package com.example.sms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.ArchivedAlgPosteSmsEntity
import com.example.data.PreferencesManager
import com.example.ui.i18n.formatDzdAmount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AlgPosteSmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = try {
            Telephony.Sms.Intents.getMessagesFromIntent(intent)
        } catch (_: Throwable) {
            null
        } ?: return

        if (messages.isEmpty()) return

        // Reassemble multi-part SMS grouped by originating address
        val groupedBySender = linkedMapOf<String, StringBuilder>()
        var latestTimestamp = System.currentTimeMillis()
        for (sms in messages) {
            val addr = sms.displayOriginatingAddress ?: sms.originatingAddress ?: ""
            val body = sms.displayMessageBody ?: sms.messageBody ?: ""
            if (sms.timestampMillis > 0L) {
                latestTimestamp = sms.timestampMillis
            }
            groupedBySender.getOrPut(addr) { StringBuilder() }.append(body)
        }

        val appContext = context.applicationContext
        val realm = com.example.data.SessionMetaStore.getSessionRealm(appContext)
        if (realm == com.example.data.SessionMetaStore.REALM_NONE || realm == com.example.data.SessionMetaStore.REALM_CLOUD) {
            return
        }
        val prefsManager = PreferencesManager(appContext)
        val currentSettings = prefsManager.settingsFlow.value
        val baselineMillis = currentSettings.smsSyncBaselineTimestampMillis
        val allowSuggestionForTimestamp = baselineMillis <= 0L || latestTimestamp > baselineMillis

        val processedKeys = AlgPosteSmsCodec.parseProcessedKeys(currentSettings.processedSmsKeysJson)
        val existingPending = AlgPosteSmsCodec.parseSuggestions(currentSettings.pendingSmsSuggestionsJson)
        val existingIds = existingPending.map { it.id }.toSet()
        val existingHistory = AlgPosteSmsCodec.parseBalanceHistory(currentSettings.smsBalanceHistoryJson)
        val existingTaxes = AlgPosteSmsCodec.parseTaxAnomalies(currentSettings.detectedSmsTaxesJson)
        val existingFailed = AlgPosteSmsCodec.parseFailedTransactions(currentSettings.detectedFailedSmsTransactionsJson)
        val dismissedFailedIds = AlgPosteSmsCodec.parseProcessedKeys(currentSettings.dismissedFailedSmsIdsJson)

        val newlyDetected = mutableListOf<SmsSuggestionEntry>()
        val newlyExtractedSnapshots = mutableListOf<SmsBalanceSnapshot>()
        val newlyArchivedEntities = mutableListOf<ArchivedAlgPosteSmsEntity>()

        for ((sender, bodyBuilder) in groupedBySender) {
            val rawText = bodyBuilder.toString()
            if (!AlgPosteSmsAnalyzer.isAlgPosteMessage(sender, rawText)) continue

            // Always archive every incoming ALG Poste SMS permanently (even OTPs or unrecognized formats)
            AlgPosteSmsAnalyzer.toArchivedEntity(
                sender = sender,
                body = rawText,
                timestampMillis = latestTimestamp,
                captureSource = "RECEIVER",
                allowAnySenderForManualTest = false
            )?.let { newlyArchivedEntities.add(it) }

            AlgPosteSmsAnalyzer.parseBalanceSnapshot(
                sender = sender,
                body = rawText,
                timestampMillis = latestTimestamp,
                allowAnySenderForManualTest = false
            )?.let { snap ->
                newlyExtractedSnapshots.add(snap)
            }

            if (currentSettings.baridiMobSmsSyncEnabled && allowSuggestionForTimestamp) {
                val parsed = AlgPosteSmsAnalyzer.parseSms(
                    sender = sender,
                    body = rawText,
                    timestampMillis = latestTimestamp,
                    allowAnySenderForManualTest = false
                )
                if (parsed != null && parsed.id !in processedKeys && parsed.id !in existingIds) {
                    newlyDetected.add(parsed)
                }
            }
        }

        if (newlyDetected.isEmpty() && newlyExtractedSnapshots.isEmpty() && newlyArchivedEntities.isEmpty()) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getDatabase(appContext).appDao()
                if (newlyArchivedEntities.isNotEmpty()) {
                    dao.insertArchivedSmsIfMissing(newlyArchivedEntities)
                }
                val archivedEntities = runCatching { dao.getAllArchivedAlgPosteSmsList() }
                    .getOrDefault(emptyList())
                val archivedSnapshots = AlgPosteSmsAnalyzer.reconstructSnapshotsFromArchive(archivedEntities)

                val inboxBaselineSnapshots = try {
                    if (AlgPosteSmsInboxScanner.hasSmsPermissions(appContext)) {
                        val rawInbox = AlgPosteSmsInboxScanner.scanAllRawAlgPosteSms(
                            context = appContext,
                            maxRowsToInspect = 120
                        )
                        val inboxArchived = rawInbox.mapNotNull { raw ->
                            AlgPosteSmsAnalyzer.toArchivedEntity(
                                sender = raw.sender,
                                body = raw.body,
                                timestampMillis = raw.dateMillis,
                                captureSource = "INBOX",
                                allowAnySenderForManualTest = false
                            )
                        }
                        if (inboxArchived.isNotEmpty()) {
                            dao.insertArchivedSmsIfMissing(inboxArchived)
                        }
                        AlgPosteSmsInboxScanner.scanAllBalanceSnapshots(
                            context = appContext,
                            maxRowsToInspect = 80
                        )
                    } else {
                        emptyList()
                    }
                } catch (_: Throwable) {
                    emptyList()
                }

                val mergedHistory = (archivedSnapshots + existingHistory + inboxBaselineSnapshots + newlyExtractedSnapshots)
                    .distinctBy { it.id }
                    .sortedBy { it.timestampMillis }
                val latestSnap = mergedHistory.maxByOrNull { it.timestampMillis }
                val existingFailedIds = existingFailed.map { it.id }.toSet()
                val failedAnalysis = AlgPosteSmsAnalyzer.analyzeFailedTransactions(
                    snapshots = mergedHistory,
                    existingEntries = existingFailed,
                    dismissedIds = dismissedFailedIds
                )
                val newSnapIds = newlyExtractedSnapshots.map { it.id }.toSet()
                val newlyCaughtFailed = failedAnalysis.failedTransactions.firstOrNull {
                    it.recoverySmsId in newSnapIds && it.id !in existingFailedIds
                }

                val existingTaxIds = existingTaxes.map { it.id }.toSet()
                val updatedTaxes = AlgPosteSmsAnalyzer.detectTaxAnomalies(
                    snapshots = mergedHistory,
                    existingAnomalies = existingTaxes,
                    failedAnalysis = failedAnalysis
                )
                val newlyCaughtTax = updatedTaxes.firstOrNull {
                    it.smsId in newSnapIds && it.id !in existingTaxIds
                }

                // Suggest any new explicit SMS operation OR balance change (Income / Outcome) in Alg Post Balance,
                // strictly excluding failed debits, explicit reversals, and silent-recovery balance jumps.
                val excludedIds = failedAnalysis.excludedSuggestionSmsIds
                val filteredNewlyDetected = newlyDetected.filterNot {
                    it.id.removePrefix("bal_op_") in excludedIds
                }
                val newDeltaSuggestions = if (currentSettings.baridiMobSmsSyncEnabled && allowSuggestionForTimestamp) {
                    AlgPosteSmsAnalyzer.detectBalanceOperationSuggestions(
                        snapshots = mergedHistory,
                        minTimestampMillis = baselineMillis,
                        excludedSmsIds = excludedIds
                    ).filter { op ->
                        val baseKey = op.id.removePrefix("bal_op_")
                        (baseKey in newSnapIds) &&
                            op.id !in processedKeys &&
                            baseKey !in processedKeys &&
                            "bal_op_$baseKey" !in processedKeys &&
                            op.id !in existingIds &&
                            baseKey !in existingIds
                    }
                } else {
                    emptyList()
                }

                val combinedNewSuggestions = (filteredNewlyDetected + newDeltaSuggestions)
                    .distinctBy { it.id.removePrefix("bal_op_") }

                val pendingToAdd = if (currentSettings.baridiMobSmsSyncEnabled) {
                    combinedNewSuggestions
                } else {
                    emptyList()
                }
                val cleanedExistingPending = existingPending.filterNot {
                    it.id.removePrefix("bal_op_") in excludedIds
                }
                val updatedList = (pendingToAdd + cleanedExistingPending)
                    .distinctBy { it.id.removePrefix("bal_op_") }

                prefsManager.updateSettings {
                    it.copy(
                        pendingSmsSuggestionsJson = AlgPosteSmsCodec.encodeSuggestions(updatedList),
                        smsBalanceHistoryJson = AlgPosteSmsCodec.encodeBalanceHistory(mergedHistory),
                        detectedSmsTaxesJson = AlgPosteSmsCodec.encodeTaxAnomalies(updatedTaxes),
                        detectedFailedSmsTransactionsJson = AlgPosteSmsCodec.encodeFailedTransactions(failedAnalysis.failedTransactions),
                        latestSmsBalanceAmount = latestSnap?.balance ?: it.latestSmsBalanceAmount,
                        latestSmsBalanceUpdatedAt = latestSnap?.timestampMillis ?: it.latestSmsBalanceUpdatedAt,
                        latestSmsBalanceRawSnippet = latestSnap?.rawBody ?: it.latestSmsBalanceRawSnippet
                    )
                }

                val isAr = currentSettings.language == "ar"
                val autoTaxLogResult = TaxExpenseAutoLogger.logFutureTaxesIfEnabled(
                    dao = dao,
                    prefsManager = prefsManager,
                    isArabic = isAr,
                    candidateAnomalies = updatedTaxes
                )
                val wasCaughtTaxAutoLogged = newlyCaughtTax != null &&
                    newlyCaughtTax.id in autoTaxLogResult.newlyLoggedIds

                val primarySuggestion = pendingToAdd.firstOrNull()
                if (primarySuggestion != null) {
                    postSuggestionNotification(
                        context = appContext,
                        suggestion = primarySuggestion,
                        caughtTax = newlyCaughtTax,
                        wasTaxAutoLogged = wasCaughtTaxAutoLogged,
                        isArabic = isAr
                    )
                } else if (newlyCaughtTax != null) {
                    postTaxDetectedNotification(
                        context = appContext,
                        caughtTax = newlyCaughtTax,
                        wasTaxAutoLogged = wasCaughtTaxAutoLogged,
                        isArabic = isAr
                    )
                }
                if (newlyCaughtFailed != null) {
                    postFailedTransactionNotification(
                        context = appContext,
                        failedEntry = newlyCaughtFailed,
                        isArabic = isAr
                    )
                }
            } catch (_: Throwable) {
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun ensureSmsChannel(nm: NotificationManager, isArabic: Boolean): String {
        val channelId = "baridimob_sms_sync_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                if (isArabic) "مزامنة رسائل بريد الجزائر" else "BaridiMob SMS Sync",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            nm.createNotificationChannel(channel)
        }
        return channelId
    }

    private fun postSuggestionNotification(
        context: Context,
        suggestion: SmsSuggestionEntry,
        caughtTax: SmsTaxAnomalyEntry?,
        wasTaxAutoLogged: Boolean = false,
        isArabic: Boolean
    ) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val channelId = ensureSmsChannel(nm, isArabic)

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                suggestion.id.hashCode(),
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val formattedAmt = formatDzdAmount(suggestion.amount, if (isArabic) "دج" else "DA")
            val title = if (suggestion.isExpense) {
                if (isArabic) "بريد الجزائر: اقتراح مصروف (-$formattedAmt)" else "ALG Poste: Expense Suggested (-$formattedAmt)"
            } else {
                if (isArabic) "بريد الجزائر: اقتراح دخل (+$formattedAmt)" else "ALG Poste: Income Suggested (+$formattedAmt)"
            }
            val taxNote = caughtTax?.let {
                val formattedTax = formatDzdAmount(it.taxAmount, if (isArabic) "دج" else "DA")
                if (wasTaxAutoLogged) {
                    if (isArabic) "\n✓ تمت إضافة ضريبة تلقائياً للمصاريف: $formattedTax"
                    else "\n✓ Auto-logged bank tax to Expenses: $formattedTax"
                } else {
                    if (isArabic) "\n⚠ تم تصيّد ضريبة/فارق: $formattedTax"
                    else "\n⚠ Hidden fee/tax caught: $formattedTax"
                }
            } ?: ""
            val bodyText = (if (suggestion.isExpense) {
                if (isArabic) "اضغط لمراجعة الرسالة وإضافتها إلى المصاريف" else "Tap to review and add to Expense"
            } else {
                if (isArabic) "اضغط لمراجعة الرسالة وإضافتها إلى الدخل" else "Tap to review and add to Income"
            }) + taxNote

            val publicNotification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(if (isArabic) "بريد الجزائر" else "ALG Poste")
                .setContentText(if (isArabic) "تم رصد حركة مالية جديدة — افتح التطبيق للمراجعة" else "New financial activity detected — open app to review")
                .setAutoCancel(true)
                .build()

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(bodyText)
                .setStyle(NotificationCompat.BigTextStyle().bigText("$bodyText\n${suggestion.rawBody}"))
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPublicVersion(publicNotification)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            nm.notify(suggestion.id.hashCode(), notification)
        } catch (_: Throwable) {
        }
    }

    private fun postTaxDetectedNotification(
        context: Context,
        caughtTax: SmsTaxAnomalyEntry,
        wasTaxAutoLogged: Boolean = false,
        isArabic: Boolean
    ) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val channelId = ensureSmsChannel(nm, isArabic)

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                caughtTax.id.hashCode(),
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val formattedTax = formatDzdAmount(caughtTax.taxAmount, if (isArabic) "دج" else "DA")
            val title = if (wasTaxAutoLogged) {
                if (isArabic) "إضافة ضريبة تلقائياً: -$formattedTax ✓"
                else "Tax Auto-Logged: -$formattedTax ✓"
            } else {
                if (isArabic) "كاشف الضرائب: فارق -$formattedTax"
                else "Tax Radar: Difference -$formattedTax"
            }
            val summary = if (isArabic) caughtTax.explanationAr else caughtTax.explanationEn

            val publicNotification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(if (isArabic) "تنبيه بريد الجزائر" else "ALG Poste Alert")
                .setContentText(if (isArabic) "تم رصد رسوم أو فارق رصيد — افتح التطبيق للتفاصيل" else "Fee or balance discrepancy detected — open app for details")
                .setAutoCancel(true)
                .build()

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(summary)
                .setStyle(NotificationCompat.BigTextStyle().bigText(summary))
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPublicVersion(publicNotification)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            nm.notify(caughtTax.id.hashCode(), notification)
        } catch (_: Throwable) {
        }
    }

    private fun postFailedTransactionNotification(
        context: Context,
        failedEntry: SmsFailedTransactionEntry,
        isArabic: Boolean
    ) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val channelId = ensureSmsChannel(nm, isArabic)

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                failedEntry.id.hashCode(),
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val formattedAmt = formatDzdAmount(failedEntry.recoveredAmount, if (isArabic) "دج" else "DA")
            val title = if (isArabic) "التعاملات الفاشلة: استرجاع $formattedAmt ✓"
            else "Failed Transaction Reversed: +$formattedAmt ✓"
            val summary = if (isArabic) failedEntry.explanationAr else failedEntry.explanationEn

            val publicNotification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(if (isArabic) "تحديث بريد الجزائر" else "ALG Poste Update")
                .setContentText(if (isArabic) "تم رصد عملية مسترجعة — افتح التطبيق للتفاصيل" else "Reversed transaction detected — open app for details")
                .setAutoCancel(true)
                .build()

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(summary)
                .setStyle(NotificationCompat.BigTextStyle().bigText(summary))
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPublicVersion(publicNotification)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            nm.notify(failedEntry.id.hashCode(), notification)
        } catch (_: Throwable) {
        }
    }
}
