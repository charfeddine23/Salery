package com.example.cloud

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class CloudUserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val firstName: String? = null,
    val lastName: String? = null,
    val displayName: String? = null,
    val email: String? = null,
    val photoUrl: String? = null,
    val localUsername: String? = null,
    val localWilayaCode: String? = null,
    val localWilayaName: String? = null,
    val localCommuneName: String? = null,
    val language: String = "en",
    val themeMode: String = "DARK",
    val designStyle: String = "LIQUID_GLASS",
    @ServerTimestamp val lastSyncedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val type: String = "EXPENSE",
    val amount: Double = 0.0,
    val category: String = "other",
    val title: String = "",
    val note: String = "",
    val dateMillis: Long = 0L,
    val salaryCredit: Boolean = false,
    val paidFromSavings: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudDebt(
    @DocumentId val id: String = "",
    val userId: String = "",
    val type: String = "OWED_BY_ME",
    val personName: String = "",
    val amount: Double = 0.0,
    val note: String = "",
    val dueDateMillis: Long = 0L,
    val autoPayOnDueDate: Boolean = true,
    val paid: Boolean = false,
    val paidDateMillis: Long? = null,
    val createdDateMillis: Long = 0L,
    val recurringMonthlyInstallment: Boolean = false,
    val monthlyInstallmentAmount: Double = 0.0,
    val installmentDayOfMonth: Int = 1,
    val remainingAmount: Double = 0.0,
    val lastInstallmentCreditedMonth: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

/**
 * Non-sensitive synced Vaultix preferences, savings, salary, and card metadata stored at
 * users/{uid}/vault_settings/primary.
 * Never includes card PANs, CVCs, vaultKeysJson, ccpAccountsJson, SMS raw snippets, or passwords.
 */
@Keep
@IgnoreExtraProperties
data class CloudVaultSettings(
    @DocumentId val id: String = "",
    val userId: String = "",
    val salaryAmount: Double = 85000.0,
    val salaryPayday: Int = 25,
    val salaryEmployer: String = "Enterprise Algiers",
    val salaryAutoCredit: Boolean = true,
    val lastCreditedMonthYear: String = "",
    val biometricEnabled: Boolean = false,
    val autoLockTimeoutSeconds: Int = 30,
    val hasCompletedPermissionsOnboarding: Boolean = false,
    val savingsWalletBalance: Double = 0.0,
    val savingsGoalAmount: Double = 0.0,
    val savingsHistoryJson: String = "[]",
    val includeAssetsInSavingsTotal: Boolean = false,
    val goldAssetHoldingsJson: String = "[]",
    val binanceHoldingsJson: String = "[]",
    val cardHubOrderJson: String = "[\"CLASSIC\",\"VISA\",\"THIRD\"]",
    val classicCardBankStyle: String = "EDAHABIA",
    val classicCardHolderName: String = "CARDHOLDER",
    val classicCardExpiry: String = "11/29",
    val visaCardProvider: String = "DEFAULT",
    val visaCardHolderName: String = "CARDHOLDER",
    val visaCardExpiry: String = "08/28",
    val thirdCardEnabled: Boolean = false,
    val thirdCardProvider: String = "BINANCE",
    val thirdCardHolderName: String = "CARDHOLDER",
    val thirdCardExpiry: String = "08/28",
    val firstName: String? = null,
    val lastName: String? = null,
    val localUsername: String? = null,
    val localWilayaCode: String? = null,
    val localWilayaName: String? = null,
    val localCommuneName: String? = null,
    val language: String = "en",
    val themeMode: String = "DARK",
    val designStyle: String = "LIQUID_GLASS",
    val stealthMode: Boolean = false,
    val breathingGlowEnabled: Boolean = true,
    val screenshotProtectionEnabled: Boolean = false,
    val lastBackupTimestamp: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

/**
 * Synced Gold, Binance Crypto, and Square Market assets & pricing state stored at
 * users/{uid}/vault_settings/assets.
 */
@Keep
@IgnoreExtraProperties
data class CloudVaultAssets(
    @DocumentId val id: String = "",
    val userId: String = "",
    val goldAssetHoldingsJson: String = "[]",
    val binanceHoldingsJson: String = "[]",
    val binancePricesJson: String = "[]",
    val binanceLastPriceSyncMillis: Long = 0L,
    val squareMarketSnapshotJson: String = "",
    val squareLastPriceSyncMillis: Long = 0L,
    val squareDailyBaselineJson: String = "",
    val squarePinnedIdsJson: String = "[\"XAU_SPOT\",\"XAU_24K\",\"XAU_18K\",\"EUR\",\"USD\"]",
    val squareSyncIntervalMinutes: Int = 60,
    val squareEnabledSourceIdsJson: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

/**
 * Synced BaridiMob SMS parser state, balance timeline, caught taxes, and failed transaction alerts
 * stored at users/{uid}/vault_settings/sms_sync (never stores raw private SMS message bodies).
 */
@Keep
@IgnoreExtraProperties
data class CloudVaultSmsSync(
    @DocumentId val id: String = "",
    val userId: String = "",
    val baridiMobSmsSyncEnabled: Boolean = true,
    val pendingSmsSuggestionsJson: String = "[]",
    val processedSmsKeysJson: String = "[]",
    val smsSyncBaselineTimestampMillis: Long = 0L,
    val latestSmsBalanceAmount: Double = -1.0,
    val latestSmsBalanceUpdatedAt: Long = 0L,
    val smsBalanceHistoryJson: String = "[]",
    val detectedSmsTaxesJson: String = "[]",
    val detectedFailedSmsTransactionsJson: String = "[]",
    val dismissedFailedSmsIdsJson: String = "[]",
    val autoAddTaxesEnabled: Boolean = false,
    val autoAddTaxesBaselineMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

/**
 * UID-scoped AES-256-GCM encrypted envelope storing sensitive account secrets (cards, keys, CCPs,
 * SMS balance raw snippet, custom profile photo bytes) at users/{uid}/vault_settings/secrets.
 */
@Keep
@IgnoreExtraProperties
data class CloudVaultSecrets(
    @DocumentId val id: String = "",
    val userId: String = "",
    val format: String = "VAULTIX_CLOUD_SECRETS_V1",
    val envelopeJson: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class DecryptedCloudVaultSecrets(
    val classicCardNumber: String = "",
    val classicCardCvc: String = "",
    val visaCardNumber: String = "",
    val visaCardCvc: String = "",
    val thirdCardNumber: String = "",
    val thirdCardCvc: String = "",
    val vaultKeysJson: String = "[]",
    val ccpAccountsJson: String = "[]",
    val latestSmsBalanceRawSnippet: String = "",
    val avatarBase64: String = ""
)

/**
 * Cloud-synced archived ALG Poste SMS record stored under users/{uid}/archived_sms/{smsDocId}.
 * Raw SMS body is encrypted per-UID via VaultCryptoManager.encryptCloudText.
 */
@Keep
@IgnoreExtraProperties
data class CloudArchivedSms(
    @DocumentId val id: String = "",
    val userId: String = "",
    val smsId: String = "",
    val sender: String = "",
    val encryptedRawBody: String = "",
    val receivedAtMillis: Long = 0L,
    val capturedAtMillis: Long = 0L,
    val captureSource: String = "INBOX",
    val extractedBalance: Double? = null,
    val extractedTxAmount: Double? = null,
    val extractedExpense: Boolean? = null,
    val hasExplicitFeeKeyword: Boolean = false,
    val matchedRule: String = "UNRECOGNIZED",
    val studyStatus: String = "NEEDS_STUDY",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudEncryptedBackup(
    @DocumentId val id: String = "",
    val userId: String = "",
    val format: String = "VAULTIX_ENC_V1",
    val envelopeJson: String = "",
    val appVersion: String = "1.0.0",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class CloudSyncSummary(
    val syncedAtMillis: Long,
    val transactionCount: Int,
    val debtCount: Int
)

data class CloudPulledVault(
    val profile: CloudUserProfile?,
    val settings: CloudVaultSettings?,
    val assets: CloudVaultAssets? = null,
    val smsSync: CloudVaultSmsSync? = null,
    val secrets: DecryptedCloudVaultSecrets? = null,
    val transactions: List<CloudTransaction>,
    val debts: List<CloudDebt>,
    val archivedSms: List<com.example.data.ArchivedAlgPosteSmsEntity> = emptyList()
) {
    fun hasExistingCloudData(): Boolean =
        settings != null ||
            assets != null ||
            smsSync != null ||
            secrets != null ||
            transactions.isNotEmpty() ||
            debts.isNotEmpty() ||
            archivedSms.isNotEmpty()
}

sealed interface GoogleAuthUiState {
    data object SignedOut : GoogleAuthUiState
    data object Loading : GoogleAuthUiState
    data class SignedIn(
        val uid: String,
        val displayName: String,
        val email: String,
        val photoUrl: String? = null
    ) : GoogleAuthUiState
    data class Error(
        val message: String,
        val accountNotFound: Boolean = false
    ) : GoogleAuthUiState
}

sealed interface CloudSyncUiState {
    data object Idle : CloudSyncUiState
    data object Syncing : CloudSyncUiState
    data class Success(
        val syncedAtMillis: Long,
        val transactionCount: Int,
        val debtCount: Int
    ) : CloudSyncUiState
    data class Error(val message: String) : CloudSyncUiState
}
