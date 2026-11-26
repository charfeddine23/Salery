package com.example.cloud

import android.content.Context
import com.example.R
import com.example.data.DebtEntity
import com.example.data.TransactionEntity
import com.example.data.UserSettings
import com.example.security.VaultCryptoManager
import com.example.ui.i18n.APP_VERSION
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
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
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun userDocRef(uid: String) = db.collection("users").document(uid)

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

            val mutableFields = buildMap<String, Any> {
                user.displayName?.trim()?.take(120)?.takeIf { it.isNotEmpty() }?.let { put("displayName", it) }
                user.email?.trim()?.take(254)?.takeIf { it.isNotEmpty() }?.let { put("email", it) }
                user.photoUrl?.toString()?.take(500)?.takeIf { it.isNotEmpty() }?.let { put("photoUrl", it) }
                settings.localUsername.trim().take(80).takeIf { it.isNotEmpty() }?.let { put("localUsername", it) }
                settings.localWilayaCode.trim().take(10).takeIf { it.isNotEmpty() }?.let { put("localWilayaCode", it) }
                settings.localWilayaName.trim().take(100).takeIf { it.isNotEmpty() }?.let { put("localWilayaName", it) }
                settings.localCommuneName.trim().take(100).takeIf { it.isNotEmpty() }?.let { put("localCommuneName", it) }
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
     * Pushes non-secret settings, portfolio holdings, transactions, and debts strictly under users/{uid}/...
     * Never writes raw card numbers, CVCs, vaultKeysJson, ccpAccountsJson, or SMS snippets.
     */
    suspend fun pushVaultToCloud(
        user: FirebaseUser,
        settings: UserSettings,
        transactions: List<TransactionEntity>,
        debts: List<DebtEntity>
    ): Result<CloudSyncSummary> {
        val uid = try {
            requireUserId().also {
                check(it == user.uid) { "Signed-in user changed; refusing cross-user write." }
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/${user.uid}")
            return Result.failure(e)
        }

        upsertUserProfile(user, settings).onFailure { return Result.failure(it) }
        pushNonSensitiveSettings(uid, settings).onFailure { return Result.failure(it) }

        val userRef = userDocRef(uid)
        val existingTxIds = try {
            userRef.collection("transactions").get().await().documents.mapTo(HashSet()) { it.id }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "users/$uid/transactions")
            return Result.failure(e)
        }

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
            try {
                if (docId in existingTxIds) {
                    txRef.update(mutableFields).await()
                } else {
                    txRef.set(
                        mutableFields + mapOf(
                            "userId" to uid,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.WRITE, txRef.path)
                return Result.failure(e)
            }
        }

        // Remove stale transaction docs deleted locally
        for (staleTxId in existingTxIds) {
            if (staleTxId !in currentTxDocIds) {
                val staleRef = userRef.collection("transactions").document(staleTxId)
                try {
                    staleRef.delete().await()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.DELETE, staleRef.path)
                    return Result.failure(e)
                }
            }
        }

        val existingDebtIds = try {
            userRef.collection("debts").get().await().documents.mapTo(HashSet()) { it.id }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "users/$uid/debts")
            return Result.failure(e)
        }

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
            try {
                if (docId in existingDebtIds) {
                    debtRef.update(mutableFields).await()
                } else {
                    debtRef.set(
                        mutableFields + mapOf(
                            "userId" to uid,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.WRITE, debtRef.path)
                return Result.failure(e)
            }
        }

        // Remove stale debt docs deleted locally
        for (staleDebtId in existingDebtIds) {
            if (staleDebtId !in currentDebtDocIds) {
                val staleRef = userRef.collection("debts").document(staleDebtId)
                try {
                    staleRef.delete().await()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.DELETE, staleRef.path)
                    return Result.failure(e)
                }
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

    private suspend fun pushNonSensitiveSettings(
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
                put("savingsWalletBalance", settings.savingsWalletBalance.coerceAtLeast(0.0).let { if (it.isFinite()) it else 0.0 })
                put("savingsGoalAmount", settings.savingsGoalAmount.coerceAtLeast(0.0).let { if (it.isFinite()) it else 0.0 })
                put("savingsHistoryJson", settings.savingsHistoryJson.take(200_000))
                put("includeAssetsInSavingsTotal", settings.includeAssetsInSavingsTotal)
                put("goldAssetHoldingsJson", settings.goldAssetHoldingsJson.take(100_000))
                put("binanceHoldingsJson", settings.binanceHoldingsJson.take(100_000))
                put("cardHubOrderJson", settings.cardHubOrderJson.take(2_000))
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
                settings.localUsername.trim().take(80).takeIf { it.isNotEmpty() }?.let { put("localUsername", it) }
                settings.localWilayaCode.trim().take(10).takeIf { it.isNotEmpty() }?.let { put("localWilayaCode", it) }
                settings.localWilayaName.trim().take(100).takeIf { it.isNotEmpty() }?.let { put("localWilayaName", it) }
                settings.localCommuneName.trim().take(100).takeIf { it.isNotEmpty() }?.let { put("localCommuneName", it) }
                put("language", if (settings.language == "ar") "ar" else "en")
                put("themeMode", settings.themeMode.takeIf { it in setOf("DARK", "LIGHT", "SYSTEM") } ?: "DARK")
                put("designStyle", settings.designStyle.take(64).ifBlank { "LIQUID_GLASS" })
                put("stealthMode", settings.stealthMode)
                put("breathingGlowEnabled", settings.breathingGlowEnabled)
                put("screenshotProtectionEnabled", settings.screenshotProtectionEnabled)
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
            val transactions = userRef.collection("transactions").get().await()
                .toObjects(CloudTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            val debts = userRef.collection("debts").get().await()
                .toObjects(CloudDebt::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)

            Result.success(
                CloudPulledVault(
                    profile = profile,
                    settings = settings,
                    transactions = transactions,
                    debts = debts
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
