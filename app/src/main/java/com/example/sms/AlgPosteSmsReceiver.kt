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
import com.example.data.PreferencesManager
import com.example.ui.i18n.formatDzdAmount

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

        val prefsManager = PreferencesManager(context.applicationContext)
        val currentSettings = prefsManager.settingsFlow.value
        val baselineMillis = currentSettings.smsSyncBaselineTimestampMillis
        if (baselineMillis > 0L && latestTimestamp <= baselineMillis) return

        val processedKeys = AlgPosteSmsCodec.parseProcessedKeys(currentSettings.processedSmsKeysJson)
        val existingPending = AlgPosteSmsCodec.parseSuggestions(currentSettings.pendingSmsSuggestionsJson)
        val existingIds = existingPending.map { it.id }.toSet()
        val existingHistory = AlgPosteSmsCodec.parseBalanceHistory(currentSettings.smsBalanceHistoryJson)
        val existingTaxes = AlgPosteSmsCodec.parseTaxAnomalies(currentSettings.detectedSmsTaxesJson)

        val newlyDetected = mutableListOf<SmsSuggestionEntry>()
        val newlyExtractedSnapshots = mutableListOf<SmsBalanceSnapshot>()

        for ((sender, bodyBuilder) in groupedBySender) {
            if (!AlgPosteSmsAnalyzer.isAlgPosteSender(sender)) continue
            val rawText = bodyBuilder.toString()

            AlgPosteSmsAnalyzer.parseBalanceSnapshot(
                sender = sender,
                body = rawText,
                timestampMillis = latestTimestamp,
                allowAnySenderForManualTest = false
            )?.let { snap ->
                newlyExtractedSnapshots.add(snap)
            }

            val parsed = AlgPosteSmsAnalyzer.parseSms(
                sender = sender,
                body = rawText,
                timestampMillis = latestTimestamp,
                allowAnySenderForManualTest = false
            ) ?: continue

            if (parsed.id !in processedKeys && parsed.id !in existingIds) {
                newlyDetected.add(parsed)
            }
        }

        if (newlyDetected.isEmpty() && newlyExtractedSnapshots.isEmpty()) return

        val mergedHistory = (existingHistory + newlyExtractedSnapshots)
            .distinctBy { it.id }
            .sortedBy { it.timestampMillis }
        val latestSnap = mergedHistory.maxByOrNull { it.timestampMillis }
        val updatedTaxes = AlgPosteSmsAnalyzer.detectTaxAnomalies(
            snapshots = mergedHistory,
            existingAnomalies = existingTaxes
        )

        val updatedList = (newlyDetected + existingPending).distinctBy { it.id }
        prefsManager.updateSettings {
            it.copy(
                baridiMobSmsSyncEnabled = true,
                pendingSmsSuggestionsJson = AlgPosteSmsCodec.encodeSuggestions(updatedList),
                smsBalanceHistoryJson = AlgPosteSmsCodec.encodeBalanceHistory(mergedHistory),
                detectedSmsTaxesJson = AlgPosteSmsCodec.encodeTaxAnomalies(updatedTaxes),
                latestSmsBalanceAmount = latestSnap?.balance ?: it.latestSmsBalanceAmount,
                latestSmsBalanceUpdatedAt = latestSnap?.timestampMillis ?: it.latestSmsBalanceUpdatedAt,
                latestSmsBalanceRawSnippet = latestSnap?.rawBody ?: it.latestSmsBalanceRawSnippet
            )
        }

        val primarySuggestion = newlyDetected.firstOrNull()
        if (primarySuggestion != null) {
            postSuggestionNotification(
                context = context.applicationContext,
                suggestion = primarySuggestion,
                isArabic = currentSettings.language == "ar"
            )
        }
    }

    private fun postSuggestionNotification(
        context: Context,
        suggestion: SmsSuggestionEntry,
        isArabic: Boolean
    ) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val channelId = "baridimob_sms_sync_channel"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    if (isArabic) "مزامنة رسائل ALG Poste" else "BaridiMob SMS Sync",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                nm.createNotificationChannel(channel)
            }

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
                if (isArabic) "ALG Poste: اقتراح مصروف (-$formattedAmt)" else "ALG Poste: Expense Suggested (-$formattedAmt)"
            } else {
                if (isArabic) "ALG Poste: اقتراح دخل (+$formattedAmt)" else "ALG Poste: Income Suggested (+$formattedAmt)"
            }
            val bodyText = if (suggestion.isExpense) {
                if (isArabic) "اضغط لمراجعة الرسالة وإضافتها إلى المصاريف" else "Tap to review and add to Expense"
            } else {
                if (isArabic) "اضغط لمراجعة الرسالة وإضافتها إلى الدخل" else "Tap to review and add to Income"
            }

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(bodyText)
                .setStyle(NotificationCompat.BigTextStyle().bigText("$bodyText\n${suggestion.rawBody}"))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            nm.notify(suggestion.id.hashCode(), notification)
        } catch (_: Throwable) {
        }
    }
}
