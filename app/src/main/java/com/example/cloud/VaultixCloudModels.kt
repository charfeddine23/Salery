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
 * Non-sensitive synced Vaultix preferences and portfolio holdings.
 * Never includes card PANs, CVCs, vaultKeysJson, ccpAccountsJson, or SMS snippets.
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
    val transactions: List<CloudTransaction>,
    val debts: List<CloudDebt>
)

sealed interface GoogleAuthUiState {
    data object SignedOut : GoogleAuthUiState
    data object Loading : GoogleAuthUiState
    data class SignedIn(
        val uid: String,
        val displayName: String,
        val email: String,
        val photoUrl: String? = null
    ) : GoogleAuthUiState
    data class Error(val message: String) : GoogleAuthUiState
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
