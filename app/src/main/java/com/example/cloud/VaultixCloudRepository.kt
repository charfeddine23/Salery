package com.example.cloud

import android.content.Context
import com.example.R
import com.example.data.ArchivedAlgPosteSmsEntity
import com.example.data.DebtEntity
import com.example.data.TransactionEntity
import com.example.data.UserSettings
import com.example.security.VaultCryptoManager
import com.example.ui.i18n.APP_VERSION
import org.json.JSONObject
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class VaultixCloudRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    constructor(context: Context) : this(
        db = resolveFirestore(context),
        auth = Firebase.auth
    )

    companion object {
        private const val BATCH_LIMIT = 400

        fun resolveFirestore(context: Context): FirebaseFirestore {
            val dbId = runCatching {
                context.applicationContext.getString(R.string.firestore_database_id).trim()
            }.getOrDefault("(default)")
            return if (dbId.isBlank() || dbId == "(default)") {
                FirebaseFirestore.getInstance()
            } else {
                FirebaseFirestore.getInstance(dbId)
            }
        }
    }

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in before accessing Firestore.")
    }

    private fun userDocRef(uid: String) = db.collection("users").document(uid)

    private fun boundedJson(raw: String, maxLen: Int, fallback: String): String {
        return if (raw.length <= maxLen) raw else fallback
    }

    fun observeUserProfile(userId: String): Flow<CloudUserProfile?> = flow {
        val signedInUid = requireUserId()
        check(signedInUid == userId) { "Signed-in user mismatch for profile stream." }
        val path = "users/$userId"
        emitAll(
            userDocRef(userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        CloudUserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun upsertUserProfile(
        user: FirebaseUser,
        settings: UserSettings
    ): Result<Unit> {
        val docPath = "users/${user.uid}"
        return try {
            val uid = requireUserId()
            check(uid == user.uid) { "Signed-in user changed; refusing cross-user profile write." }
            val ref = userDocRef(uid)
            val existingSnap = ref.get().await()

            val combinedFullName = listOf(settings.firstName.trim(), settings.lastName.trim())
                .filter { it.isNotEmpty() }
                .joinToString(" ")
            val resolvedDisplayName = combinedFullName.ifBlank {
                user.displayName?.trim().orEmpty()
            }.take(120)

            val mutableFields = buildMap<String, Any> {
                put("firstName", settings.firstName.trim().take(80))
                put("lastName", settings.lastName.trim().take(80))
                put("displayName", resolvedDisplayName)
                user.email?.trim()?.take(254)?.takeIf { it.isNotEmpty() }?.let { put("email", it) }
                user.photoUrl?.toString()?.take(500)?.takeIf { it.isNotEmpty() }?.let { put("photoUrl", it) }
                put("localUsername", settings.localUsername.trim().take(80))
                put("localWilayaCode", settings.localWilayaCode.trim().take(10))
                put("localWilayaName", settings.localWilayaName.trim().take(100))
                put("localCommuneName", settings.localCommuneName.trim().take(100))
                put("language", if (settings.language == "ar") "ar" else "en")
                put("themeMode", settings.themeMode.takeIf { it in setOf("DARK", "LIGHT", "SYSTEM") } ?: "DARK")
                put("designStyle", settings.designStyle.take(64).ifBlank { "LIQUID_GLASS" })
                put("lastSyncedAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }

            if (existingSnap.exists()) {
                ref.update(mutableFields).await()
            } else {
                val createPayload = mutableFields + mapOf(
                    "userId" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                ref.set(createPayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    /**
     * Pushes ALL non-secret data across the entire app strictly under users/{uid}/...:
     * - Profile (`users/{uid}`)
     * - Core Settings, Salary, Savings, Card Styles (`vault_settings/primary`)
     * - Gold, Binance Crypto, Square Market Assets (`vault_settings/assets`)
     * - BaridiMob SMS Sync, Balance History, Caught Taxes, Failed Ops (`vault_settings/sms_sync`)
     * - Income & Expenses (`transactions/{txId}`)
     * - Debts & Installments (`debts/{debtId}`)
     *
     * Never writes passwords, raw card PANs/CVCs, vaultKeysJson, ccpAccountsJson, or raw SMS snippets.
     */
    suspend fun pushVaultToCloud(
        user: FirebaseUser,
        settings: UserSettings,
        transactions: List<TransactionEntity>,
        debts: List<DebtEntity>,
        archivedSms: List<ArchivedAlgPosteSmsEntity> = emptyList(),
        avatarBase64: String = "",
        syncSettings: Boolean = true,
        syncTransactions: Boolean = true,
        syncDebts: Boolean = true,
        syncSecrets: Boolean = syncSettings,
        syncArchivedSms: Boolean = false
    ): Result<CloudSyncSummary> {
        val uid = try {
            requireUserId().also {
                check(it == user.uid) { "Signed-in user changed; refusing cross-user write." }
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/${user.uid}")
            return Result.failure(e)
        }

        if (syncSettings) {
            upsertUserProfile(user, settings).onFailure { return Result.failure(it) }
            pushPrimarySettings(uid, settings).onFailure { return Result.failure(it) }
            pushAssetsSettings(uid, settings).onFailure { return Result.failure(it) }
            pushSmsSyncSettings(uid, settings).onFailure { return Result.failure(it) }
        }
        if (syncSecrets) {
            pushSecretsSettings(uid, settings, avatarBase64).onFailure { return Result.failure(it) }
        }

        val userRef = userDocRef(uid)
        if (syncTransactions) {
            val existingTxIds = try {
                userRef.collection("transactions").get().await().documents.mapTo(HashSet()) { it.id }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.LIST, "users/$uid/transactions")
                return Result.failure(e)
            }

            val pendingOps = mutableListOf<BatchOp>()
            val currentTxDocIds = HashSet<String>(transactions.size)
            for (tx in transactions) {
                val docId = "tx_${tx.id}"
                currentTxDocIds.add(docId)
                val txRef = userRef.collection("transactions").document(docId)
                val mutableFields = mapOf<String, Any>(
                    "type" to tx.type.name,
                    "amount" to tx.amount.coerceAtLeast(0.0).let { if (it.isFinite()) it else 0.0 },
                    "category" to tx.category.ifBlank { "other" }.take(64),
                    "title" to tx.title.ifBlank { tx.category.ifBlank { "Transaction" } }.take(160),
                    "note" to tx.note.take(500),
                    "dateMillis" to tx.dateMillis.coerceAtLeast(0L),
                    "salaryCredit" to tx.isSalaryCredit,
                    "paidFromSavings" to tx.isPaidFromSavings,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (docId in existingTxIds) {
                    pendingOps.add(BatchOp.Update(txRef, mutableFields))
                } else {
                    pendingOps.add(
                        BatchOp.Set(
                            txRef,
                            mutableFields + mapOf(
                                "userId" to uid,
                                "createdAt" to FieldValue.serverTimestamp()
                            )
                        )
                    )
                }
            }

            for (staleTxId in existingTxIds) {
                if (staleTxId !in currentTxDocIds) {
                    pendingOps.add(BatchOp.Delete(userRef.collection("transactions").document(staleTxId)))
                }
            }

            commitBatchedOps(pendingOps, "users/$uid/transactions").onFailure {
                return Result.failure(it)
            }
        }

        if (syncDebts) {
            val existingDebtIds = try {
                userRef.collection("debts").get().await().documents.mapTo(HashSet()) { it.id }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.LIST, "users/$uid/debts")
                return Result.failure(e)
            }

            val pendingOps = mutableListOf<BatchOp>()
            val currentDebtDocIds = HashSet<String>(debts.size)
            for (debt in debts) {
                val docId = "debt_${debt.id}"
                currentDebtDocIds.add(docId)
                val debtRef = userRef.collection("debts").document(docId)
                val mutableFields = buildMap<String, Any> {
                    put("type", debt.type.name)
                    put("personName", debt.personName.trim().ifBlank { "Contact" }.take(120))
                    put("amount", debt.amount.coerceAtLeast(0.0).let { if (it.isFinite()) it else 0.0 })
                    put("note", debt.note.take(500))
                    put("dueDateMillis", debt.dueDateMillis.coerceAtLeast(0L))
                    put("autoPayOnDueDate", debt.autoPayOnDueDate)
                    put("paid", debt.isPaid)
                    debt.paidDateMillis?.takeIf { it >= 0L }?.let { put("paidDateMillis", it) }
                    put("createdDateMillis", debt.createdDateMillis.coerceAtLeast(0L))
                    put("recurringMonthlyInstallment", debt.isRecurringMonthlyInstallment)
                    put(
                        "monthlyInstallmentAmount",
                        debt.monthlyInstallmentAmount.coerceAtLeast(0.0).let { if (it.isFinite()) it else 0.0 }
                    )
                    put("installmentDayOfMonth", debt.installmentDayOfMonth.coerceIn(1, 31))
                    put(
                        "remainingAmount",
                        debt.remainingAmount.coerceAtLeast(0.0).let { if (it.isFinite()) it else 0.0 }
                    )
                    put("lastInstallmentCreditedMonth", debt.lastInstallmentCreditedMonth.take(20))
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                if (docId in existingDebtIds) {
                    pendingOps.add(BatchOp.Update(debtRef, mutableFields))
                } else {
                    pendingOps.add(
                        BatchOp.Set(
                            debtRef,
                            mutableFields + mapOf(
                                "userId" to uid,
                                "createdAt" to FieldValue.serverTimestamp()
                            )
                        )
                    )
                }
            }

            for (staleDebtId in existingDebtIds) {
                if (staleDebtId !in currentDebtDocIds) {
                    pendingOps.add(BatchOp.Delete(userRef.collection("debts").document(staleDebtId)))
                }
            }

            commitBatchedOps(pendingOps, "users/$uid/debts").onFailure {
                return Result.failure(it)
            }
        }

        if (syncArchivedSms) {
            pushArchivedSms(uid, archivedSms).onFailure {
                return Result.failure(it)
            }
        }

        return Result.success(
            CloudSyncSummary(
                syncedAtMillis = System.currentTimeMillis(),
                transactionCount = transactions.size,
                debtCount = debts.size
            )
        )
    }

    private sealed interface BatchOp {
        data class Set(val ref: DocumentReference, val data: Map<String, Any>) : BatchOp
        data class Update(val ref: DocumentReference, val data: Map<String, Any>) : BatchOp
        data class Delete(val ref: DocumentReference) : BatchOp
    }

    private suspend fun commitBatchedOps(ops: List<BatchOp>, pathForError: String): Result<Unit> {
        if (ops.isEmpty()) return Result.success(Unit)
        for (chunk in ops.chunked(BATCH_LIMIT)) {
            try {
                val batch = db.batch()
                for (op in chunk) {
                    when (op) {
                        is BatchOp.Set -> batch.set(op.ref, op.data)
                        is BatchOp.Update -> batch.update(op.ref, op.data)
                        is BatchOp.Delete -> batch.delete(op.ref)
                    }
                }
                batch.commit().await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.WRITE, pathForError)
                return Result.failure(e)
            }
        }
        return Result.success(Unit)
    }

    private suspend fun pushPrimarySettings(
        uid: String,
        settings: UserSettings
    ): Result<Unit> {
        val settingsRef = userDocRef(uid).collection("vault_settings").document("primary")
        return try {
            val existingSnap = settingsRef.get().await()
            val mutableFields = buildMap<String, Any> {
                put("salaryAmount", settings.salaryAmount.coerceAtLeast(0.0).let { if (it.isFinite()) it else 0.0 })
                put("salaryPayday", settings.salaryPayday.coerceIn(1, 31))
                put("salaryEmployer", settings.salaryEmployer.take(120))
                put("salaryAutoCredit", settings.salaryAutoCredit)
                put("lastCreditedMonthYear", settings.lastCreditedMonthYear.take(20))
                put("biometricEnabled", settings.biometricEnabled)
                put("autoLockTimeoutSeconds", settings.autoLockTimeoutSeconds.coerceIn(-1, 3600))
                put("hasCompletedPermissionsOnboarding", settings.hasCompletedPermissionsOnboarding)
                put("savingsWalletBalance", settings.savingsWalletBalance.coerceAtLeast(0.0).let { if (it.isFinite()) it else 0.0 })
                put("savingsGoalAmount", settings.savingsGoalAmount.coerceAtLeast(0.0).let { if (it.isFinite()) it else 0.0 })
                put("savingsHistoryJson", boundedJson(settings.savingsHistoryJson, 200_000, "[]"))
                put("includeAssetsInSavingsTotal", settings.includeAssetsInSavingsTotal)
                put("goldAssetHoldingsJson", boundedJson(settings.goldAssetHoldingsJson, 100_000, "[]"))
                put("binanceHoldingsJson", boundedJson(settings.binanceHoldingsJson, 100_000, "[]"))
                put("cardHubOrderJson", boundedJson(settings.cardHubOrderJson, 2_000, "[\"CLASSIC\",\"VISA\",\"THIRD\"]"))
                put("classicCardBankStyle", settings.classicCardBankStyle.take(40))
                put("classicCardHolderName", settings.classicCardHolderName.take(80))
                put("classicCardExpiry", settings.classicCardExpiry.take(16))
                put("visaCardProvider", settings.visaCardProvider.take(40))
                put("visaCardHolderName", settings.visaCardHolderName.take(80))
                put("visaCardExpiry", settings.visaCardExpiry.take(16))
                put("thirdCardEnabled", settings.thirdCardEnabled)
                put("thirdCardProvider", settings.thirdCardProvider.take(40))
                put("thirdCardHolderName", settings.thirdCardHolderName.take(80))
                put("thirdCardExpiry", settings.thirdCardExpiry.take(16))
                put("firstName", settings.firstName.trim().take(80))
                put("lastName", settings.lastName.trim().take(80))
                put("localUsername", settings.localUsername.trim().take(80))
                put("localWilayaCode", settings.localWilayaCode.trim().take(10))
                put("localWilayaName", settings.localWilayaName.trim().take(100))
                put("localCommuneName", settings.localCommuneName.trim().take(100))
                put("language", if (settings.language == "ar") "ar" else "en")
                put("themeMode", settings.themeMode.takeIf { it in setOf("DARK", "LIGHT", "SYSTEM") } ?: "DARK")
                put("designStyle", settings.designStyle.take(64).ifBlank { "LIQUID_GLASS" })
                put("stealthMode", settings.stealthMode)
                put("breathingGlowEnabled", settings.breathingGlowEnabled)
                put("screenshotProtectionEnabled", settings.screenshotProtectionEnabled)
                put("lastBackupTimestamp", settings.lastBackupTimestamp.coerceAtLeast(0L))
                put("updatedAt", FieldValue.serverTimestamp())
            }

            if (existingSnap.exists()) {
                settingsRef.update(mutableFields).await()
            } else {
                settingsRef.set(
                    mutableFields + mapOf(
                        "userId" to uid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, settingsRef.path)
            Result.failure(e)
        }
    }

    private suspend fun pushAssetsSettings(
        uid: String,
        settings: UserSettings
    ): Result<Unit> {
        val assetsRef = userDocRef(uid).collection("vault_settings").document("assets")
        return try {
            val existingSnap = assetsRef.get().await()
            val mutableFields = mapOf<String, Any>(
                "goldAssetHoldingsJson" to boundedJson(settings.goldAssetHoldingsJson, 100_000, "[]"),
                "binanceHoldingsJson" to boundedJson(settings.binanceHoldingsJson, 100_000, "[]"),
                "binancePricesJson" to boundedJson(settings.binancePricesJson, 100_000, "[]"),
                "binanceLastPriceSyncMillis" to settings.binanceLastPriceSyncMillis.coerceAtLeast(0L),
                "squareMarketSnapshotJson" to boundedJson(settings.squareMarketSnapshotJson, 180_000, ""),
                "squareLastPriceSyncMillis" to settings.squareLastPriceSyncMillis.coerceAtLeast(0L),
                "squareDailyBaselineJson" to boundedJson(settings.squareDailyBaselineJson, 100_000, ""),
                "squarePinnedIdsJson" to boundedJson(settings.squarePinnedIdsJson, 5_000, "[]"),
                "squareSyncIntervalMinutes" to settings.squareSyncIntervalMinutes.coerceIn(0, 1440),
                "squareEnabledSourceIdsJson" to boundedJson(settings.squareEnabledSourceIdsJson, 5_000, ""),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (existingSnap.exists()) {
                assetsRef.update(mutableFields).await()
            } else {
                assetsRef.set(
                    mutableFields + mapOf(
                        "userId" to uid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, assetsRef.path)
            Result.failure(e)
        }
    }

    private suspend fun pushSmsSyncSettings(
        uid: String,
        settings: UserSettings
    ): Result<Unit> {
        val smsRef = userDocRef(uid).collection("vault_settings").document("sms_sync")
        return try {
            val existingSnap = smsRef.get().await()
            val mutableFields = mapOf<String, Any>(
                "baridiMobSmsSyncEnabled" to settings.baridiMobSmsSyncEnabled,
                "pendingSmsSuggestionsJson" to boundedJson(settings.pendingSmsSuggestionsJson, 180_000, "[]"),
                "processedSmsKeysJson" to boundedJson(settings.processedSmsKeysJson, 100_000, "[]"),
                "smsSyncBaselineTimestampMillis" to settings.smsSyncBaselineTimestampMillis.coerceAtLeast(0L),
                "latestSmsBalanceAmount" to settings.latestSmsBalanceAmount.let {
                    if (it.isFinite() && it >= -1.0) it else -1.0
                },
                "latestSmsBalanceUpdatedAt" to settings.latestSmsBalanceUpdatedAt.coerceAtLeast(0L),
                "smsBalanceHistoryJson" to boundedJson(settings.smsBalanceHistoryJson, 180_000, "[]"),
                "detectedSmsTaxesJson" to boundedJson(settings.detectedSmsTaxesJson, 100_000, "[]"),
                "detectedFailedSmsTransactionsJson" to boundedJson(settings.detectedFailedSmsTransactionsJson, 100_000, "[]"),
                "dismissedFailedSmsIdsJson" to boundedJson(settings.dismissedFailedSmsIdsJson, 50_000, "[]"),
                "autoAddTaxesEnabled" to settings.autoAddTaxesEnabled,
                "autoAddTaxesBaselineMillis" to settings.autoAddTaxesBaselineMillis.coerceAtLeast(0L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (existingSnap.exists()) {
                smsRef.update(mutableFields).await()
            } else {
                smsRef.set(
                    mutableFields + mapOf(
                        "userId" to uid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, smsRef.path)
            Result.failure(e)
        }
    }

    private suspend fun pushSecretsSettings(
        uid: String,
        settings: UserSettings,
        avatarBase64: String = ""
    ): Result<Unit> {
        val secretsRef = userDocRef(uid).collection("vault_settings").document("secrets")
        return try {
            val safeAvatar = if (avatarBase64.length <= 320_000) avatarBase64 else ""
            val plainSecretsJson = JSONObject().apply {
                put("classicCardNumber", settings.classicCardNumber)
                put("classicCardCvc", settings.classicCardCvc)
                put("visaCardNumber", settings.visaCardNumber)
                put("visaCardCvc", settings.visaCardCvc)
                put("thirdCardNumber", settings.thirdCardNumber)
                put("thirdCardCvc", settings.thirdCardCvc)
                put("vaultKeysJson", boundedJson(settings.vaultKeysJson, 180_000, "[]"))
                put("ccpAccountsJson", boundedJson(settings.ccpAccountsJson, 180_000, "[]"))
                put("latestSmsBalanceRawSnippet", settings.latestSmsBalanceRawSnippet.take(1_000))
                put("avatarBase64", safeAvatar)
            }.toString()
            val envelope = VaultCryptoManager.encryptCloudVaultSecrets(plainSecretsJson, uid)
            require(envelope.length <= 750_000) {
                "Encrypted cloud secrets envelope exceeds Firestore safety limit."
            }
            val existingSnap = secretsRef.get().await()
            val mutableFields = mapOf<String, Any>(
                "format" to "VAULTIX_CLOUD_SECRETS_V1",
                "envelopeJson" to envelope,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (existingSnap.exists()) {
                secretsRef.update(mutableFields).await()
            } else {
                secretsRef.set(
                    mutableFields + mapOf(
                        "userId" to uid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, secretsRef.path)
            Result.failure(e)
        }
    }

    private fun archivedSmsDocId(rawId: String): String =
        "sms_" + VaultCryptoManager.sha256Hex(rawId).take(32)

    private suspend fun pushArchivedSms(
        uid: String,
        archivedSms: List<ArchivedAlgPosteSmsEntity>
    ): Result<Unit> {
        val userRef = userDocRef(uid)
        val existingDocIds = try {
            userRef.collection("archived_sms").get().await().documents.mapTo(HashSet()) { it.id }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "users/$uid/archived_sms")
            return Result.failure(e)
        }

        val pendingOps = mutableListOf<BatchOp>()
        val currentDocIds = HashSet<String>(archivedSms.size)
        for (sms in archivedSms) {
            val docId = archivedSmsDocId(sms.id)
            currentDocIds.add(docId)
            val docRef = userRef.collection("archived_sms").document(docId)
            val encryptedBody = VaultCryptoManager.encryptCloudText(sms.rawBody.take(4_000), uid)
            val mutableFields = buildMap<String, Any> {
                put("smsId", sms.id.take(256))
                put("sender", sms.sender.take(120))
                put("encryptedRawBody", encryptedBody.take(12_000))
                put("receivedAtMillis", sms.receivedAtMillis.coerceAtLeast(0L))
                put("capturedAtMillis", sms.capturedAtMillis.coerceAtLeast(0L))
                put("captureSource", sms.captureSource.ifBlank { "INBOX" }.take(32))
                sms.extractedBalance?.takeIf { it.isFinite() && it >= 0.0 }?.let {
                    put("extractedBalance", it)
                }
                sms.extractedTxAmount?.takeIf { it.isFinite() && it >= 0.0 }?.let {
                    put("extractedTxAmount", it)
                }
                sms.extractedIsExpense?.let {
                    put("extractedExpense", it)
                }
                put("hasExplicitFeeKeyword", sms.hasExplicitFeeKeyword)
                put("matchedRule", sms.matchedRule.ifBlank { "UNRECOGNIZED" }.take(64))
                put("studyStatus", sms.studyStatus.ifBlank { "NEEDS_STUDY" }.take(32))
                put("updatedAt", FieldValue.serverTimestamp())
            }
            if (docId in existingDocIds) {
                pendingOps.add(BatchOp.Update(docRef, mutableFields))
            } else {
                pendingOps.add(
                    BatchOp.Set(
                        docRef,
                        mutableFields + mapOf(
                            "userId" to uid,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                )
            }
        }

        for (staleId in existingDocIds) {
            if (staleId !in currentDocIds) {
                pendingOps.add(BatchOp.Delete(userRef.collection("archived_sms").document(staleId)))
            }
        }

        return commitBatchedOps(pendingOps, "users/$uid/archived_sms")
    }

    private fun decryptSecretsEnvelope(
        envelopeDoc: CloudVaultSecrets?,
        uid: String
    ): DecryptedCloudVaultSecrets? {
        val rawEnvelope = envelopeDoc?.envelopeJson?.takeIf {
            it.isNotBlank() && VaultCryptoManager.isCloudSecretsEnvelope(it)
        } ?: return null
        val plain = VaultCryptoManager.decryptCloudVaultSecrets(rawEnvelope, uid) ?: return null
        return runCatching {
            val json = JSONObject(plain)
            DecryptedCloudVaultSecrets(
                classicCardNumber = json.optString("classicCardNumber", ""),
                classicCardCvc = json.optString("classicCardCvc", ""),
                visaCardNumber = json.optString("visaCardNumber", ""),
                visaCardCvc = json.optString("visaCardCvc", ""),
                thirdCardNumber = json.optString("thirdCardNumber", ""),
                thirdCardCvc = json.optString("thirdCardCvc", ""),
                vaultKeysJson = json.optString("vaultKeysJson", "[]").ifBlank { "[]" },
                ccpAccountsJson = json.optString("ccpAccountsJson", "[]").ifBlank { "[]" },
                latestSmsBalanceRawSnippet = json.optString("latestSmsBalanceRawSnippet", ""),
                avatarBase64 = json.optString("avatarBase64", "")
            )
        }.getOrNull()
    }

    suspend fun pullVaultFromCloud(): Result<CloudPulledVault> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "users/unauthenticated")
            return Result.failure(e)
        }

        val userRef = userDocRef(uid)
        return try {
            val profile = userRef.get().await()
                .toObject(CloudUserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            val settings = userRef.collection("vault_settings").document("primary").get().await()
                .toObject(CloudVaultSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            val assets = runCatching {
                userRef.collection("vault_settings").document("assets").get().await()
                    .toObject(CloudVaultAssets::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }.getOrNull()
            val smsSync = runCatching {
                userRef.collection("vault_settings").document("sms_sync").get().await()
                    .toObject(CloudVaultSmsSync::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }.getOrNull()
            val secretsDoc = runCatching {
                userRef.collection("vault_settings").document("secrets").get().await()
                    .toObject(CloudVaultSecrets::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }.getOrNull()
            val decryptedSecrets = decryptSecretsEnvelope(secretsDoc, uid)
            val transactions = userRef.collection("transactions").get().await()
                .toObjects(CloudTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            val debts = userRef.collection("debts").get().await()
                .toObjects(CloudDebt::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            val archivedSmsDocs = runCatching {
                userRef.collection("archived_sms").get().await()
                    .toObjects(CloudArchivedSms::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }.getOrDefault(emptyList())
            val archivedSms = archivedSmsDocs.mapNotNull { doc ->
                val rawBody = VaultCryptoManager.decryptCloudText(doc.encryptedRawBody, uid) ?: return@mapNotNull null
                val cleanId = doc.smsId.ifBlank { doc.id }
                if (cleanId.isBlank()) return@mapNotNull null
                ArchivedAlgPosteSmsEntity(
                    id = cleanId,
                    sender = doc.sender,
                    rawBody = rawBody,
                    receivedAtMillis = doc.receivedAtMillis.coerceAtLeast(0L),
                    capturedAtMillis = doc.capturedAtMillis.coerceAtLeast(0L),
                    captureSource = doc.captureSource.ifBlank { "INBOX" },
                    extractedBalance = doc.extractedBalance,
                    extractedTxAmount = doc.extractedTxAmount,
                    extractedIsExpense = doc.extractedExpense,
                    hasExplicitFeeKeyword = doc.hasExplicitFeeKeyword,
                    matchedRule = doc.matchedRule.ifBlank { "UNRECOGNIZED" },
                    studyStatus = doc.studyStatus.ifBlank { "NEEDS_STUDY" }
                )
            }

            Result.success(
                CloudPulledVault(
                    profile = profile,
                    settings = settings,
                    assets = assets,
                    smsSync = smsSync,
                    secrets = decryptedSecrets,
                    transactions = transactions,
                    debts = debts,
                    archivedSms = archivedSms
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, userRef.path)
            Result.failure(e)
        }
    }

    suspend fun pushEncryptedBackupEnvelope(envelopeJson: String): Result<Unit> {
        val docRef = try {
            val uid = requireUserId()
            require(VaultCryptoManager.isEncryptedBackupEnvelope(envelopeJson)) {
                "Only VAULTIX_ENC_V1 encrypted backup envelopes can be uploaded."
            }
            require(envelopeJson.length <= 750_000) {
                "Encrypted backup exceeds Firestore document safety limit."
            }
            userDocRef(uid).collection("encrypted_backups").document("latest")
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid ?: "unauthenticated"}/encrypted_backups/latest")
            return Result.failure(e)
        }

        return try {
            val uid = requireUserId()
            val existingSnap = docRef.get().await()
            val mutableFields = mapOf<String, Any>(
                "format" to "VAULTIX_ENC_V1",
                "envelopeJson" to envelopeJson,
                "appVersion" to APP_VERSION.take(32),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (existingSnap.exists()) {
                docRef.update(mutableFields).await()
            } else {
                docRef.set(
                    mutableFields + mapOf(
                        "userId" to uid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun pullEncryptedBackupEnvelope(): Result<String?> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "users/unauthenticated/encrypted_backups/latest")
            return Result.failure(e)
        }

        val docRef = userDocRef(uid).collection("encrypted_backups").document("latest")
        return try {
            val snap = docRef.get().await()
            val backup = snap.toObject(
                CloudEncryptedBackup::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            val envelope = backup?.envelopeJson?.takeIf {
                it.isNotBlank() && VaultCryptoManager.isEncryptedBackupEnvelope(it)
            }
            Result.success(envelope)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }
}
