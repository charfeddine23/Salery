package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.data.AppDatabase
import com.example.data.ArchivedAlgPosteSmsEntity
import com.example.data.BackupPayload
import com.example.data.CcpAccountEntry
import com.example.data.DebtEntity
import com.example.data.DebtType
import com.example.data.HubVaultCodec
import com.example.data.PreferencesManager
import com.example.data.SavingsHistoryCodec
import com.example.data.SavingsMovementEntry
import com.example.data.SavingsMovementType
import com.example.data.SessionMetaStore
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.data.UserSettings
import com.example.data.VaultAppPresets
import com.example.data.VaultKeyEntry
import com.example.security.VaultCryptoManager
import com.example.ui.i18n.APP_VERSION
import com.example.ui.i18n.DEVELOPER_NAME
import com.example.ui.i18n.formatDzdAmount
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Calendar
import java.util.Locale
import com.example.notification.DebtReminderScheduler
import com.example.sms.AlgPosteSmsAnalyzer
import com.example.sms.AlgPosteSmsCodec
import com.example.sms.AlgPosteSmsInboxScanner
import com.example.sms.ArchivedSmsStudyReport
import com.example.sms.SmsBalanceSnapshot
import com.example.sms.SmsFailedTransactionEntry
import com.example.sms.SmsSuggestionEntry
import com.example.sms.SmsTaxAnomalyEntry
import com.example.sms.TaxExpenseAutoLogger
import com.example.update.AppUpdateManager
import com.example.update.AppUpdateUiState
import com.example.update.DownloadState
import com.example.update.GitHubReleaseInfo
import com.example.update.compareVersionTags
import com.example.crypto.BinanceCryptoCatalog
import com.example.crypto.BinanceCryptoCodec
import com.example.crypto.BinanceMarketPriceFetcher
import com.example.crypto.CRYPTO_PRICE_REFRESH_INTERVAL_MILLIS
import com.example.crypto.CryptoHoldingEntry
import com.example.crypto.CryptoMarketPriceEntry
import com.example.market.GoldAssetCodec
import com.example.market.GoldAssetHolding
import com.example.market.SQUARE_SNAPSHOT_VERSION
import com.example.market.SquareMarketCatalog
import com.example.market.SquareMarketCodec
import com.example.market.SquareMarketFetcher
import com.example.market.SquareMarketSnapshot
import com.example.cloud.CloudDebt
import com.example.cloud.CloudPulledVault
import com.example.cloud.CloudSyncUiState
import com.example.cloud.CloudTransaction
import com.example.cloud.CloudUserProfile
import com.example.cloud.CloudVaultAssets
import com.example.cloud.CloudVaultSettings
import com.example.cloud.CloudVaultSmsSync
import com.example.cloud.DecryptedCloudVaultSecrets
import com.example.cloud.EmailAuthResult
import com.example.cloud.GoogleAuthHelper
import com.example.cloud.GoogleAuthUiState
import com.example.cloud.VaultixCloudRepository
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.isActive
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToLong

data class FinancialSummary(
    val totalBalance: Double = 0.0,
    val grossBalance: Double = 0.0,
    val savingsBalance: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val monthlyExpenses: Double = 0.0,
    val pendingDebtsOwedByMe: Double = 0.0,
    val pendingDebtsOwedToMe: Double = 0.0
)

enum class BackupRestoreOutcome {
    SUCCESS,
    WRONG_PASSWORD_OR_TAMPERED,
    INVALID_FORMAT
}

internal fun isTaxAnomalyAlreadyLoggedInTransactions(
    anomaly: SmsTaxAnomalyEntry,
    existingTransactions: List<TransactionEntity>
): Boolean = TaxExpenseAutoLogger.isTaxAnomalyAlreadyLogged(anomaly, existingTransactions)

internal fun findUnloggedTaxAnomaliesForExpenses(
    anomalies: List<SmsTaxAnomalyEntry>,
    existingTransactions: List<TransactionEntity>,
    minTimestampMillis: Long = 0L
): List<SmsTaxAnomalyEntry> = TaxExpenseAutoLogger.findUnloggedTaxAnomalies(
    anomalies = anomalies,
    existingTransactions = existingTransactions,
    minTimestampMillis = minTimestampMillis
)

internal fun computeSavingsBalanceAfterTransactionDelete(
    transaction: TransactionEntity,
    currentSavings: Double
): Double {
    val safeSavings = if (currentSavings.isFinite()) currentSavings.coerceAtLeast(0.0) else 0.0
    return if (
        transaction.type == TransactionType.EXPENSE &&
        transaction.isPaidFromSavings &&
        transaction.amount.isFinite() &&
        transaction.amount > 0.0
    ) {
        safeSavings + transaction.amount
    } else {
        safeSavings
    }
}

enum class AuthTransitionKind {
    SIGN_IN,
    SIGN_UP
}

sealed interface AuthTransitionState {
    data object None : AuthTransitionState
    data class Active(
        val kind: AuthTransitionKind,
        val displayName: String,
        val isCompleted: Boolean = false
    ) : AuthTransitionState
}

enum class StorageRealm {
    GUEST,
    CLOUD_MEMORY,
    SIGNED_OUT
}

data class SavedAuthAccount(
    val displayName: String,
    val email: String = "",
    val photoUrl: String? = null,
    val isCloudLinked: Boolean = false
)

internal fun buildTransactionDedupeKey(
    typeName: String,
    amount: Double,
    category: String,
    title: String,
    dateMillis: Long
): String {
    val cents = (amount.coerceAtLeast(0.0) * 100.0).roundToLong()
    return "$typeName|$cents|${category.trim().lowercase(Locale.US)}|${title.trim().lowercase(Locale.US)}|$dateMillis"
}

internal fun buildDebtDedupeKey(
    typeName: String,
    personName: String,
    amount: Double,
    createdDateMillis: Long
): String {
    val cents = (amount.coerceAtLeast(0.0) * 100.0).roundToLong()
    return "$typeName|${personName.trim().lowercase(Locale.US)}|$cents|$createdDateMillis"
}

internal fun applyCloudVaultSettingsSafely(
    current: UserSettings,
    cloud: CloudVaultSettings,
    overwritePreferences: Boolean = true,
    cloudAssets: CloudVaultAssets? = null,
    cloudSmsSync: CloudVaultSmsSync? = null,
    cloudSecrets: DecryptedCloudVaultSecrets? = null
): UserSettings {
    if (!overwritePreferences) return current
    val resolvedFirstName = cloud.firstName?.trim() ?: current.firstName
    val resolvedLastName = cloud.lastName?.trim() ?: current.lastName
    val resolvedUsername = cloud.localUsername?.trim() ?: current.localUsername
    val resolvedWilayaCode = cloud.localWilayaCode?.trim() ?: current.localWilayaCode
    val resolvedWilayaName = cloud.localWilayaName?.trim() ?: current.localWilayaName
    val resolvedCommuneName = cloud.localCommuneName?.trim() ?: current.localCommuneName

    return current.copy(
        hasLocalAccount = true,
        firstName = resolvedFirstName,
        lastName = resolvedLastName,
        salaryAmount = cloud.salaryAmount.takeIf { it.isFinite() && it >= 0.0 } ?: current.salaryAmount,
        salaryPayday = cloud.salaryPayday.coerceIn(1, 31),
        salaryEmployer = cloud.salaryEmployer.ifBlank { current.salaryEmployer },
        salaryAutoCredit = cloud.salaryAutoCredit,
        lastCreditedMonthYear = cloud.lastCreditedMonthYear.ifBlank { current.lastCreditedMonthYear },
        biometricEnabled = cloud.biometricEnabled,
        autoLockTimeoutSeconds = cloud.autoLockTimeoutSeconds.coerceIn(-1, 3600),
        hasCompletedPermissionsOnboarding = cloud.hasCompletedPermissionsOnboarding || current.hasCompletedPermissionsOnboarding,
        savingsWalletBalance = cloud.savingsWalletBalance.takeIf { it.isFinite() && it >= 0.0 }
            ?: current.savingsWalletBalance,
        savingsGoalAmount = cloud.savingsGoalAmount.takeIf { it.isFinite() && it >= 0.0 }
            ?: current.savingsGoalAmount,
        savingsHistoryJson = cloud.savingsHistoryJson.ifBlank { current.savingsHistoryJson },
        includeAssetsInSavingsTotal = cloud.includeAssetsInSavingsTotal,
        goldAssetHoldingsJson = cloudAssets?.goldAssetHoldingsJson?.ifBlank { null }
            ?: cloud.goldAssetHoldingsJson.ifBlank { current.goldAssetHoldingsJson },
        binanceHoldingsJson = cloudAssets?.binanceHoldingsJson?.ifBlank { null }
            ?: cloud.binanceHoldingsJson.ifBlank { current.binanceHoldingsJson },
        binancePricesJson = cloudAssets?.binancePricesJson?.ifBlank { null } ?: current.binancePricesJson,
        binanceLastPriceSyncMillis = cloudAssets?.binanceLastPriceSyncMillis?.takeIf { it > 0L }
            ?: current.binanceLastPriceSyncMillis,
        squareMarketSnapshotJson = cloudAssets?.squareMarketSnapshotJson?.ifBlank { null }
            ?: current.squareMarketSnapshotJson,
        squareLastPriceSyncMillis = cloudAssets?.squareLastPriceSyncMillis?.takeIf { it > 0L }
            ?: current.squareLastPriceSyncMillis,
        squareDailyBaselineJson = cloudAssets?.squareDailyBaselineJson?.ifBlank { null }
            ?: current.squareDailyBaselineJson,
        squarePinnedIdsJson = cloudAssets?.squarePinnedIdsJson?.ifBlank { null }
            ?: current.squarePinnedIdsJson,
        squareSyncIntervalMinutes = cloudAssets?.squareSyncIntervalMinutes?.coerceIn(0, 1440)
            ?: current.squareSyncIntervalMinutes,
        squareEnabledSourceIdsJson = cloudAssets?.squareEnabledSourceIdsJson?.ifBlank { null }
            ?: current.squareEnabledSourceIdsJson,
        baridiMobSmsSyncEnabled = cloudSmsSync?.baridiMobSmsSyncEnabled ?: current.baridiMobSmsSyncEnabled,
        pendingSmsSuggestionsJson = cloudSmsSync?.pendingSmsSuggestionsJson?.ifBlank { null }
            ?: current.pendingSmsSuggestionsJson,
        processedSmsKeysJson = cloudSmsSync?.processedSmsKeysJson?.ifBlank { null }
            ?: current.processedSmsKeysJson,
        smsSyncBaselineTimestampMillis = cloudSmsSync?.smsSyncBaselineTimestampMillis?.coerceAtLeast(0L)
            ?: current.smsSyncBaselineTimestampMillis,
        latestSmsBalanceAmount = cloudSmsSync?.latestSmsBalanceAmount?.takeIf { it.isFinite() && it >= -1.0 }
            ?: current.latestSmsBalanceAmount,
        latestSmsBalanceUpdatedAt = cloudSmsSync?.latestSmsBalanceUpdatedAt?.coerceAtLeast(0L)
            ?: current.latestSmsBalanceUpdatedAt,
        smsBalanceHistoryJson = cloudSmsSync?.smsBalanceHistoryJson?.ifBlank { null }
            ?: current.smsBalanceHistoryJson,
        detectedSmsTaxesJson = cloudSmsSync?.detectedSmsTaxesJson?.ifBlank { null }
            ?: current.detectedSmsTaxesJson,
        detectedFailedSmsTransactionsJson = cloudSmsSync?.detectedFailedSmsTransactionsJson?.ifBlank { null }
            ?: current.detectedFailedSmsTransactionsJson,
        dismissedFailedSmsIdsJson = cloudSmsSync?.dismissedFailedSmsIdsJson?.ifBlank { null }
            ?: current.dismissedFailedSmsIdsJson,
        autoAddTaxesEnabled = cloudSmsSync?.autoAddTaxesEnabled ?: current.autoAddTaxesEnabled,
        autoAddTaxesBaselineMillis = cloudSmsSync?.autoAddTaxesBaselineMillis?.coerceAtLeast(0L)
            ?: current.autoAddTaxesBaselineMillis,
        cardHubOrderJson = cloud.cardHubOrderJson.ifBlank { current.cardHubOrderJson },
        classicCardBankStyle = cloud.classicCardBankStyle.ifBlank { current.classicCardBankStyle },
        classicCardHolderName = cloud.classicCardHolderName.ifBlank { current.classicCardHolderName },
        classicCardExpiry = cloud.classicCardExpiry.ifBlank { current.classicCardExpiry },
        visaCardProvider = cloud.visaCardProvider.ifBlank { current.visaCardProvider },
        visaCardHolderName = cloud.visaCardHolderName.ifBlank { current.visaCardHolderName },
        visaCardExpiry = cloud.visaCardExpiry.ifBlank { current.visaCardExpiry },
        thirdCardEnabled = cloud.thirdCardEnabled,
        thirdCardProvider = cloud.thirdCardProvider.ifBlank { current.thirdCardProvider },
        thirdCardHolderName = cloud.thirdCardHolderName.ifBlank { current.thirdCardHolderName },
        thirdCardExpiry = cloud.thirdCardExpiry.ifBlank { current.thirdCardExpiry },
        localUsername = resolvedUsername,
        localWilayaCode = resolvedWilayaCode,
        localWilayaName = resolvedWilayaName,
        localCommuneName = resolvedCommuneName,
        language = if (cloud.language == "ar") "ar" else "en",
        themeMode = cloud.themeMode.takeIf { it in setOf("DARK", "LIGHT", "SYSTEM") } ?: current.themeMode,
        designStyle = cloud.designStyle.ifBlank { current.designStyle },
        stealthMode = cloud.stealthMode,
        breathingGlowEnabled = cloud.breathingGlowEnabled,
        screenshotProtectionEnabled = cloud.screenshotProtectionEnabled,
        lastBackupTimestamp = if (cloud.lastBackupTimestamp > 0L) cloud.lastBackupTimestamp else current.lastBackupTimestamp,
        classicCardNumber = cloudSecrets?.classicCardNumber ?: current.classicCardNumber,
        classicCardCvc = cloudSecrets?.classicCardCvc ?: current.classicCardCvc,
        visaCardNumber = cloudSecrets?.visaCardNumber ?: current.visaCardNumber,
        visaCardCvc = cloudSecrets?.visaCardCvc ?: current.visaCardCvc,
        thirdCardNumber = cloudSecrets?.thirdCardNumber ?: current.thirdCardNumber,
        thirdCardCvc = cloudSecrets?.thirdCardCvc ?: current.thirdCardCvc,
        vaultKeysJson = cloudSecrets?.vaultKeysJson ?: current.vaultKeysJson,
        ccpAccountsJson = cloudSecrets?.ccpAccountsJson ?: current.ccpAccountsJson,
        latestSmsBalanceRawSnippet = cloudSecrets?.latestSmsBalanceRawSnippet ?: current.latestSmsBalanceRawSnippet
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
class VaultixViewModel(application: Application) : AndroidViewModel(application) {
    private val guestDatabase = AppDatabase.getDatabase(application)
    private val cloudSessionDatabase = AppDatabase.getCloudSessionDatabase(application)
    private val prefsManager = PreferencesManager(application)

    private val _storageRealm = MutableStateFlow(
        when (SessionMetaStore.resolveStartupRealm(application)) {
            SessionMetaStore.REALM_CLOUD -> StorageRealm.CLOUD_MEMORY
            SessionMetaStore.REALM_GUEST -> StorageRealm.GUEST
            else -> StorageRealm.SIGNED_OUT
        }
    )

    private fun activeDatabase(): AppDatabase =
        if (_storageRealm.value == StorageRealm.GUEST) guestDatabase else cloudSessionDatabase

    private fun activeDao() = activeDatabase().appDao()

    val settings: StateFlow<UserSettings> = prefsManager.settingsFlow

    val allTransactions: StateFlow<List<TransactionEntity>> = _storageRealm
        .flatMapLatest { realm ->
            when (realm) {
                StorageRealm.GUEST -> guestDatabase.appDao().getAllTransactions()
                StorageRealm.CLOUD_MEMORY -> cloudSessionDatabase.appDao().getAllTransactions()
                StorageRealm.SIGNED_OUT -> flowOf(emptyList())
            }
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allDebts: StateFlow<List<DebtEntity>> = _storageRealm
        .flatMapLatest { realm ->
            when (realm) {
                StorageRealm.GUEST -> guestDatabase.appDao().getAllDebts()
                StorageRealm.CLOUD_MEMORY -> cloudSessionDatabase.appDao().getAllDebts()
                StorageRealm.SIGNED_OUT -> flowOf(emptyList())
            }
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Live financial calculations: Balance = Total Income - Total Expenses - Savings Wallet
    val financialSummary: StateFlow<FinancialSummary> = combine(
        allTransactions,
        allDebts,
        settings
    ) { transactions, debts, userSettings ->
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)

        var totalInc = 0.0
        var totalExp = 0.0
        var monthInc = 0.0
        var monthExp = 0.0

        for (t in transactions) {
            val transCal = Calendar.getInstance().apply { timeInMillis = t.dateMillis }
            val isCurrentMonth = transCal.get(Calendar.YEAR) == currentYear &&
                    transCal.get(Calendar.MONTH) == currentMonth

            if (t.type == TransactionType.INCOME) {
                totalInc += t.amount
                if (isCurrentMonth) monthInc += t.amount
            } else {
                totalExp += t.amount
                if (isCurrentMonth) monthExp += t.amount
            }
        }

        var owedByMe = 0.0
        var owedToMe = 0.0
        for (d in debts) {
            if (!d.isPaid) {
                val rem = if (d.remainingAmount > 0) d.remainingAmount else d.amount
                if (d.type == DebtType.OWED_BY_ME) owedByMe += rem
                else owedToMe += rem
            }
        }

        val savings = userSettings.savingsWalletBalance
        val grossBalance = totalInc - totalExp
        // Dashboard Balance reflects available spendable funds after Savings Wallet
        val availableBalance = grossBalance - savings

        FinancialSummary(
            totalBalance = availableBalance,
            grossBalance = grossBalance,
            savingsBalance = savings,
            monthlyIncome = monthInc,
            monthlyExpenses = monthExp,
            pendingDebtsOwedByMe = owedByMe,
            pendingDebtsOwedToMe = owedToMe
        )
    }.flowOn(Dispatchers.Default)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, FinancialSummary())

    // App lock state
    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    // Session-scoped expand & reveal states for Keys and CCPs:
    // Preserved across screen switches inside the app, and auto-collapsed when exiting the app.
    private val _expandedKeyIds = MutableStateFlow<Set<String>>(emptySet())
    val expandedKeyIds: StateFlow<Set<String>> = _expandedKeyIds.asStateFlow()

    private val _revealedKeyIds = MutableStateFlow<Set<String>>(emptySet())
    val revealedKeyIds: StateFlow<Set<String>> = _revealedKeyIds.asStateFlow()

    private val _expandedCcpIds = MutableStateFlow<Set<String>>(emptySet())
    val expandedCcpIds: StateFlow<Set<String>> = _expandedCcpIds.asStateFlow()

    private val _hiddenCcpIds = MutableStateFlow<Set<String>>(emptySet())
    val hiddenCcpIds: StateFlow<Set<String>> = _hiddenCcpIds.asStateFlow()

    // In-App GitHub Update state
    private val _updateUiState = MutableStateFlow<AppUpdateUiState>(AppUpdateUiState.Idle)
    val updateUiState: StateFlow<AppUpdateUiState> = _updateUiState.asStateFlow()

    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog: StateFlow<Boolean> = _showUpdateDialog.asStateFlow()

    private var downloadJob: Job? = null

    // Pending BaridiMob / ALG Poste SMS transaction suggestions
    val pendingSmsSuggestions: StateFlow<List<SmsSuggestionEntry>> = settings
        .map { AlgPosteSmsCodec.parseSuggestions(it.pendingSmsSuggestionsJson) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Caught hidden bank fees / tax discrepancies from ALG Poste SMS balance diffs
    val detectedSmsTaxes: StateFlow<List<SmsTaxAnomalyEntry>> = settings
        .map { AlgPosteSmsCodec.parseTaxAnomalies(it.detectedSmsTaxesJson) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Caught Failed / Ghost Transactions ("التعاملات الفاشلة") reversed silently or via cancellation SMS
    val detectedFailedSmsTransactions: StateFlow<List<SmsFailedTransactionEntry>> = settings
        .map { AlgPosteSmsCodec.parseFailedTransactions(it.detectedFailedSmsTransactionsJson) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Room ledger TransactionEntity IDs that contradict a detected Failed Transaction (if already logged prior to reversal)
    val conflictingLedgerTransactionIdsByFailedId: StateFlow<Map<String, List<Long>>> = combine(
        detectedFailedSmsTransactions,
        allTransactions
    ) { failedList, transactions ->
        buildMap {
            for (entry in failedList) {
                val matchedIds = findMatchingConflictingTransactions(entry, transactions).map { it.id }
                if (matchedIds.isNotEmpty()) {
                    put(entry.id, matchedIds)
                }
            }
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Extracted balance history timeline from ALG Poste / BaridiMob SMS messages
    val smsBalanceHistory: StateFlow<List<SmsBalanceSnapshot>> = settings
        .map { AlgPosteSmsCodec.parseBalanceHistory(it.smsBalanceHistoryJson) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Permanent non-deletable archive of every ALG Poste SMS (survives phone SMS deletion)
    val archivedAlgPosteSms: StateFlow<List<ArchivedAlgPosteSmsEntity>> = _storageRealm
        .flatMapLatest { realm ->
            when (realm) {
                StorageRealm.GUEST -> guestDatabase.appDao().getAllArchivedAlgPosteSms()
                StorageRealm.CLOUD_MEMORY -> cloudSessionDatabase.appDao().getAllArchivedAlgPosteSms()
                StorageRealm.SIGNED_OUT -> flowOf(emptyList())
            }
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Live algorithm study metrics computed from the permanent archive & caught taxes
    val archiveStudyReport: StateFlow<ArchivedSmsStudyReport> = combine(
        archivedAlgPosteSms,
        detectedSmsTaxes
    ) { archived, taxes ->
        AlgPosteSmsAnalyzer.buildStudyReport(archived, taxes)
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ArchivedSmsStudyReport())

    // Binance Card Crypto Portfolio & Live Hourly Market Prices
    val binanceHoldings: StateFlow<List<CryptoHoldingEntry>> = settings
        .map { BinanceCryptoCodec.parseHoldings(it.binanceHoldingsJson) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val binancePrices: StateFlow<Map<String, CryptoMarketPriceEntry>> = settings
        .map { BinanceCryptoCodec.parsePrices(it.binancePricesJson) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _isRefreshingCryptoPrices = MutableStateFlow(false)
    val isRefreshingCryptoPrices: StateFlow<Boolean> = _isRefreshingCryptoPrices.asStateFlow()

    private val _cryptoPriceError = MutableStateFlow<String?>(null)
    val cryptoPriceError: StateFlow<String?> = _cryptoPriceError.asStateFlow()

    // Square Market (Gold All Karats in DZD & USD + Hard Currencies Bank vs Parallel)
    val squareSnapshot: StateFlow<SquareMarketSnapshot?> = settings
        .map { SquareMarketCodec.parseSnapshot(it.squareMarketSnapshotJson) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Savings Wallet Movement History
    val savingsHistory: StateFlow<List<SavingsMovementEntry>> = settings
        .map { SavingsHistoryCodec.parse(it.savingsHistoryJson) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Stored Physical Gold Assets (paired with live Square rates)
    val goldHoldings: StateFlow<List<GoldAssetHolding>> = settings
        .map { GoldAssetCodec.parseHoldings(it.goldAssetHoldingsJson) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isRefreshingSquareMarket = MutableStateFlow(false)
    val isRefreshingSquareMarket: StateFlow<Boolean> = _isRefreshingSquareMarket.asStateFlow()

    private val _squareMarketError = MutableStateFlow<String?>(null)
    val squareMarketError: StateFlow<String?> = _squareMarketError.asStateFlow()

    // Notification events for UI banners
    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    // Firebase Auth & Cloud-Only vs Guest-Only Session Metadata
    private val _googleAuthState = MutableStateFlow<GoogleAuthUiState>(GoogleAuthUiState.SignedOut)
    val googleAuthState: StateFlow<GoogleAuthUiState> = _googleAuthState.asStateFlow()

    private val _savedQuickAccountActive = MutableStateFlow(
        SessionMetaStore.getQuickAccountActive(application)
    )
    private val _savedQuickAccountName = MutableStateFlow(
        SessionMetaStore.getQuickAccountName(application)
    )
    private val _savedQuickAccountEmail = MutableStateFlow(
        SessionMetaStore.getQuickAccountEmail(application)
    )
    private val _savedQuickAccountIsCloud = MutableStateFlow(
        SessionMetaStore.getQuickAccountIsCloud(application)
    )
    private val _savedQuickAccountUid = MutableStateFlow(
        SessionMetaStore.getQuickAccountUid(application)
    )

    val savedAuthAccount: StateFlow<SavedAuthAccount?> = combine(
        googleAuthState,
        settings,
        _savedQuickAccountActive,
        _savedQuickAccountName,
        _savedQuickAccountEmail,
        _savedQuickAccountIsCloud
    ) { values ->
        val googleState = values[0] as GoogleAuthUiState
        val userSettings = values[1] as UserSettings
        val quickActive = values[2] as Boolean
        val quickName = values[3] as String
        val quickEmail = values[4] as String
        val quickIsCloud = values[5] as Boolean
        when (googleState) {
            is GoogleAuthUiState.SignedIn -> {
                val fullLocalName = listOf(userSettings.firstName, userSettings.lastName)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                    .ifBlank { userSettings.localUsername }
                SavedAuthAccount(
                    displayName = googleState.displayName.ifBlank { fullLocalName },
                    email = googleState.email,
                    photoUrl = googleState.photoUrl,
                    isCloudLinked = true
                )
            }
            else -> {
                if (!quickActive) {
                    null
                } else {
                    val settingsName = listOf(userSettings.firstName, userSettings.lastName)
                        .filter { it.isNotBlank() }
                        .joinToString(" ")
                        .ifBlank { userSettings.localUsername }
                    val resolvedName = quickName.ifBlank { settingsName }
                        .ifBlank { quickEmail.substringBefore('@') }
                    if (resolvedName.isBlank()) {
                        null
                    } else {
                        SavedAuthAccount(
                            displayName = resolvedName,
                            email = quickEmail,
                            photoUrl = null,
                            isCloudLinked = quickIsCloud
                        )
                    }
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private fun persistSavedQuickAccount(
        displayName: String,
        email: String = "",
        isCloudLinked: Boolean = false,
        uid: String = ""
    ) {
        val cleanName = displayName.trim()
        val cleanEmail = email.trim()
        if (cleanName.isBlank() && cleanEmail.isBlank()) return
        val resolvedName = cleanName.ifBlank { cleanEmail.substringBefore('@') }
        _savedQuickAccountName.value = resolvedName
        _savedQuickAccountEmail.value = cleanEmail
        _savedQuickAccountIsCloud.value = isCloudLinked
        _savedQuickAccountUid.value = uid
        _savedQuickAccountActive.value = true
        SessionMetaStore.saveQuickAccount(
            context = getApplication(),
            displayName = resolvedName,
            email = cleanEmail,
            isCloudLinked = isCloudLinked,
            uid = uid
        )
    }

    private fun clearSavedQuickAccount() {
        _savedQuickAccountActive.value = false
        _savedQuickAccountName.value = ""
        _savedQuickAccountEmail.value = ""
        _savedQuickAccountIsCloud.value = false
        _savedQuickAccountUid.value = ""
        SessionMetaStore.clearQuickAccount(getApplication())
    }

    private val _authTransitionState = MutableStateFlow<AuthTransitionState>(AuthTransitionState.None)
    val authTransitionState: StateFlow<AuthTransitionState> = _authTransitionState.asStateFlow()
    private var authTransitionJob: Job? = null

    private fun triggerAuthSuccessTransition(
        kind: AuthTransitionKind,
        displayName: String
    ) {
        authTransitionJob?.cancel()
        val cleanName = displayName.trim()
        _authTransitionState.value = AuthTransitionState.Active(
            kind = kind,
            displayName = cleanName,
            isCompleted = false
        )
        authTransitionJob = viewModelScope.launch {
            delay(2000L)
            _authTransitionState.value = AuthTransitionState.Active(
                kind = kind,
                displayName = cleanName,
                isCompleted = true
            )
            delay(1350L)
            _authTransitionState.value = AuthTransitionState.None
        }
    }

    private fun clearAuthTransition() {
        authTransitionJob?.cancel()
        authTransitionJob = null
        _authTransitionState.value = AuthTransitionState.None
    }

    private val _cloudSyncState = MutableStateFlow<CloudSyncUiState>(CloudSyncUiState.Idle)
    val cloudSyncState: StateFlow<CloudSyncUiState> = _cloudSyncState.asStateFlow()

    private val _isRestoringCloudSession = MutableStateFlow(
        _storageRealm.value == StorageRealm.CLOUD_MEMORY
    )
    val isRestoringCloudSession: StateFlow<Boolean> = _isRestoringCloudSession.asStateFlow()

    private val _lastCloudSyncMillis = MutableStateFlow(
        SessionMetaStore.getLastCloudSyncMillis(application)
    )
    val lastCloudSyncMillis: StateFlow<Long> = _lastCloudSyncMillis.asStateFlow()

    private val _cloudUserProfile = MutableStateFlow<CloudUserProfile?>(null)
    val cloudUserProfile: StateFlow<CloudUserProfile?> = _cloudUserProfile.asStateFlow()

    private val cloudSessionMutex = Mutex()
    @Volatile
    private var activeCloudTransitionJob: Job? = null
    private var cloudProfileObserverJob: Job? = null
    private var cloudAutoSyncJob: Job? = null
    @Volatile
    private var silentGoogleSignInAttempted = false
    @Volatile
    private var isApplyingCloudPull: Boolean = false
    @Volatile
    private var initialCloudHandshakeDoneForUid: String? = null
    @Volatile
    private var lastSyncedSettingsHash: Int = 0
    @Volatile
    private var lastSyncedTxHash: Int = 0
    @Volatile
    private var lastSyncedDebtHash: Int = 0
    @Volatile
    private var lastSyncedArchivedSmsHash: Int = 0
    @Volatile
    private var lastSyncedAvatarBase64: String = ""

    private val firebaseAuth: FirebaseAuth? by lazy {
        runCatching {
            val appCtx = getApplication<Application>()
            if (FirebaseApp.getApps(appCtx).isEmpty()) {
                FirebaseApp.initializeApp(appCtx)
            }
            Firebase.auth
        }.getOrNull()
    }

    private val cloudRepository: VaultixCloudRepository? by lazy {
        val auth = firebaseAuth ?: return@lazy null
        runCatching {
            VaultixCloudRepository(
                db = VaultixCloudRepository.resolveFirestore(getApplication()),
                auth = auth
            )
        }.getOrNull()
    }

    private val firebaseAuthStateListener = FirebaseAuth.AuthStateListener { auth ->
        updateGoogleAuthFromFirebaseUser(auth.currentUser)
    }

    init {
        val initialRealm = _storageRealm.value
        if (initialRealm == StorageRealm.GUEST && settings.value.biometricEnabled) {
            _isAppLocked.value = true
        }

        // Safely clean legacy shared storage, warm Guest secrets, or restore Cloud-only RAM session off Main Thread
        viewModelScope.launch {
            kotlinx.coroutines.yield()
            val appCtx = getApplication<Application>()
            withContext(Dispatchers.IO) {
                runCatching { SessionMetaStore.purgeLegacySharedStorageOnce(appCtx) }
                if (initialRealm == StorageRealm.GUEST) {
                    runCatching { prefsManager.warmDecryptedSecrets() }
                }
            }
            val auth = withContext(Dispatchers.IO) {
                runCatching { firebaseAuth }.getOrNull()
            }
            if (initialRealm == StorageRealm.CLOUD_MEMORY) {
                val user = auth?.currentUser
                if (user != null) {
                    updateGoogleAuthFromFirebaseUser(user)
                    withTimeoutOrNull(12_000L) {
                        restoreCloudSessionOnLaunch(user)
                    }
                    _isRestoringCloudSession.value = false
                    if (settings.value.biometricEnabled) {
                        _isAppLocked.value = true
                    }
                    viewModelScope.launch(Dispatchers.IO) {
                        runActiveRealmMaintenanceAndSmsScan()
                    }
                } else {
                    clearCloudSessionInMemoryStorage(appCtx)
                    SessionMetaStore.setSessionRealm(appCtx, SessionMetaStore.REALM_NONE)
                    _storageRealm.value = StorageRealm.SIGNED_OUT
                    prefsManager.switchToSignedOutMode(resetAuthUiPreferences = true)
                    _isRestoringCloudSession.value = false
                }
            } else {
                _isRestoringCloudSession.value = false
            }

            if (auth != null) {
                runCatching {
                    updateGoogleAuthFromFirebaseUser(auth.currentUser)
                    auth.addAuthStateListener(firebaseAuthStateListener)
                }
            }
        }

        // Check Smart Salary Scheduler, Debt Auto-Payments, clean obsolete update APKs, and scan ALG Poste SMS
        // off the Main Thread after a brief yield so Frame 1 completes within the SurfaceSyncGroup budget.
        viewModelScope.launch(Dispatchers.IO) {
            delay(350L)
            try {
                DebtReminderScheduler.createNotificationChannel(getApplication())
            } catch (_: Exception) {
            }
            try {
                AppUpdateManager.cleanupObsoleteUpdates(getApplication())
            } catch (_: Exception) {
            }
            if (_storageRealm.value == StorageRealm.GUEST) {
                try {
                    cleanExistingMockDataIfPresent()
                    runActiveRealmMaintenanceAndSmsScan()
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                }
            }
            delay(900L)
            // Silent background check for newer release on GitHub
            checkForUpdates(manual = false)
        }

        // Hourly market price synchronization loop for Binance Crypto Card
        viewModelScope.launch(Dispatchers.IO) {
            delay(800L)
            while (isActive) {
                if (_storageRealm.value != StorageRealm.SIGNED_OUT) {
                    val cfg = settings.value
                    val hasGoldHoldings = GoldAssetCodec.parseHoldings(cfg.goldAssetHoldingsJson).isNotEmpty()
                    val usesBinance = cfg.visaCardProvider.equals("BINANCE", ignoreCase = true) ||
                        (cfg.thirdCardEnabled && cfg.thirdCardProvider.equals("BINANCE", ignoreCase = true)) ||
                        BinanceCryptoCodec.parseHoldings(cfg.binanceHoldingsJson).isNotEmpty() ||
                        hasGoldHoldings ||
                        cfg.includeAssetsInSavingsTotal
                    if (usesBinance) {
                        refreshCryptoMarketPrices(force = false, showFeedback = false)
                    }
                    if (hasGoldHoldings || cfg.includeAssetsInSavingsTotal) {
                        refreshSquareMarketData(force = false, showFeedback = false)
                    }
                }
                delay(CRYPTO_PRICE_REFRESH_INTERVAL_MILLIS)
            }
        }
    }

    private suspend fun runActiveRealmMaintenanceAndSmsScan() {
        if (_storageRealm.value == StorageRealm.SIGNED_OUT) return
        try {
            checkAndApplySalaryScheduler()
            checkAndExecuteAutoPayDebts()
            val currentDebts = activeDao().getAllDebtsList()
            DebtReminderScheduler.rescheduleAllReminders(getApplication(), currentDebts)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
        val cfg = settings.value
        if (cfg.hasCompletedPermissionsOnboarding && AlgPosteSmsInboxScanner.hasSmsPermissions(getApplication())) {
            if (cfg.baridiMobSmsSyncEnabled) {
                scanAlgPosteSmsInbox(getApplication(), showFeedback = false)
            } else {
                refreshCurrentBalanceOnlyFromInbox(getApplication(), showFeedback = false)
            }
        }
    }

    private suspend fun cleanExistingMockDataIfPresent() {
        val prefs = getApplication<Application>().getSharedPreferences("vaultix_guest_prefs", Context.MODE_PRIVATE)
        val cleaned = prefs.getBoolean("mock_data_cleaned_permanent_v2", false)
        if (!cleaned) {
            val guestDao = guestDatabase.appDao()
            val allTrans = guestDao.getAllTransactionsList()
            val mockTitles = setOf(
                "Monthly Salary Deposit",
                "Supermarket & Groceries",
                "Sonelgaz Electricity & Gas",
                "UI/UX Mobile Design"
            )
            for (t in allTrans) {
                if (t.title in mockTitles) {
                    guestDao.deleteTransaction(t)
                }
            }

            val allDebts = guestDao.getAllDebtsList()
            val mockPersons = setOf("Mohamed Lamine", "Karim Benali")
            for (d in allDebts) {
                if (d.personName in mockPersons && (d.note.contains("spare parts") || d.note.contains("Laptop sale"))) {
                    guestDao.deleteDebt(d)
                }
            }
            prefs.edit().putBoolean("mock_data_cleaned_permanent_v2", true).apply()
        }
    }

    /**
     * Smart Salary Scheduler:
     * Checks if current day >= salary payday for the current month.
     * If not already credited this month, credits wallet balance automatically.
     */
    suspend fun checkAndApplySalaryScheduler(force: Boolean = false) {
        if (_storageRealm.value == StorageRealm.SIGNED_OUT) return
        val userConfig = settings.value
        if (!userConfig.salaryAutoCredit && !force) return

        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)
        val currentMonth = cal.get(Calendar.MONTH) + 1 // 1-12
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        val monthYearKey = "$currentYear-$currentMonth"

        if (force || (currentDay >= userConfig.salaryPayday && userConfig.lastCreditedMonthYear != monthYearKey)) {
            val isAr = userConfig.language == "ar"
            val formattedSalary = formatDzdAmount(userConfig.salaryAmount, if (isAr) "د.ج" else "DZD")
            activeDao().insertTransaction(
                TransactionEntity(
                    type = TransactionType.INCOME,
                    amount = userConfig.salaryAmount,
                    category = "salary",
                    title = if (isAr) "راتب ${userConfig.salaryEmployer}" else "${userConfig.salaryEmployer} Salary",
                    note = if (isAr) "إيداع تلقائي من مجدول الراتب الذكي ($monthYearKey)"
                    else "Smart Salary Scheduler automatic deposit ($monthYearKey)",
                    dateMillis = System.currentTimeMillis(),
                    isSalaryCredit = true
                )
            )
            prefsManager.updateSettings {
                it.copy(lastCreditedMonthYear = monthYearKey)
            }
            _toastEvent.emit(
                if (isAr) "تم إيداع راتب $formattedSalary تلقائياً!"
                else "Salary of ${userConfig.salaryAmount} DZD credited automatically!"
            )
        }
    }

    /**
     * Debt Management with Auto-Payment:
     * Scans for pending debts with autoPayOnDueDate = true whose due date has arrived.
     */
    /**
     * Debt Management with Auto-Payment:
     * Handles both one-time due date settlements and recurring monthly installments.
     */
    suspend fun checkAndExecuteAutoPayDebts() {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)
        val currentMonth = cal.get(Calendar.MONTH) + 1
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        val monthYearKey = "$currentYear-$currentMonth"

        val autoDebts = activeDao().getPendingAutoPayDebts()
        val isAr = settings.value.language == "ar"

        for (debt in autoDebts) {
            if (debt.isRecurringMonthlyInstallment) {
                // Monthly recurring installment check
                if (currentDay >= debt.installmentDayOfMonth && debt.lastInstallmentCreditedMonth != monthYearKey) {
                    val payNow = if (debt.monthlyInstallmentAmount > 0) {
                        minOf(debt.monthlyInstallmentAmount, debt.remainingAmount)
                    } else debt.remainingAmount

                    val newRemaining = (debt.remainingAmount - payNow).coerceAtLeast(0.0)
                    val isFullySettled = newRemaining <= 0.001

                    activeDao().updateDebt(
                        debt.copy(
                            remainingAmount = newRemaining,
                            lastInstallmentCreditedMonth = monthYearKey,
                            isPaid = isFullySettled,
                            paidDateMillis = if (isFullySettled) now else null
                        )
                    )

                    if (debt.type == DebtType.OWED_BY_ME) {
                        activeDao().insertTransaction(
                            TransactionEntity(
                                type = TransactionType.EXPENSE,
                                amount = payNow,
                                category = "debt_payment",
                                title = if (isAr) "قسط تلقائي: ${debt.personName}" else "Auto-Installment: ${debt.personName}",
                                note = if (isAr) "قسط شهري تلقائي ($monthYearKey) - المتبقي: ${newRemaining.toLong()} د.ج"
                                else "Monthly auto-installment ($monthYearKey) - Rem: ${newRemaining.toLong()} DZD",
                                dateMillis = now
                            )
                        )
                    } else {
                        activeDao().insertTransaction(
                            TransactionEntity(
                                type = TransactionType.INCOME,
                                amount = payNow,
                                category = "debt_collection",
                                title = if (isAr) "تحصيل قسط تلقائي: ${debt.personName}" else "Auto-Collected Installment: ${debt.personName}",
                                note = if (isAr) "قسط شهري تلقائي ($monthYearKey) - المتبقي: ${newRemaining.toLong()} د.ج"
                                else "Monthly auto-installment ($monthYearKey) - Rem: ${newRemaining.toLong()} DZD",
                                dateMillis = now
                            )
                        )
                    }
                    _toastEvent.emit(
                        if (isAr) "تمت معالجة قسط تلقائي بقيمة ${formatDzdAmount(payNow, "د.ج")} لـ ${debt.personName}"
                        else "Auto-installment of $payNow DZD processed for ${debt.personName}"
                    )
                }
            } else {
                // One-time full debt settlement on due date
                if (debt.dueDateMillis <= now) {
                    val payAmount = if (debt.remainingAmount > 0) debt.remainingAmount else debt.amount
                    activeDao().updateDebt(
                        debt.copy(
                            remainingAmount = 0.0,
                            isPaid = true,
                            paidDateMillis = now
                        )
                    )
                    if (debt.type == DebtType.OWED_BY_ME) {
                        activeDao().insertTransaction(
                            TransactionEntity(
                                type = TransactionType.EXPENSE,
                                amount = payAmount,
                                category = "debt_payment",
                                title = if (isAr) "سداد دين تلقائي: ${debt.personName}" else "Auto-Paid Debt: ${debt.personName}",
                                note = if (isAr) "تنفيذ تلقائي لسداد الدين عند الاستحقاق (${debt.note})"
                                else "Automatic debt execution on due date (${debt.note})",
                                dateMillis = now
                            )
                        )
                    } else {
                        activeDao().insertTransaction(
                            TransactionEntity(
                                type = TransactionType.INCOME,
                                amount = payAmount,
                                category = "debt_collection",
                                title = if (isAr) "تحصيل دين تلقائي: ${debt.personName}" else "Auto-Received Debt: ${debt.personName}",
                                note = if (isAr) "تحصيل تلقائي للدين عند الاستحقاق (${debt.note})"
                                else "Automatic debt collection on due date (${debt.note})",
                                dateMillis = now
                            )
                        )
                    }
                }
            }
        }
    }

    // Transaction Actions
    fun addTransaction(type: TransactionType, amount: Double, category: String, title: String, note: String, dateMillis: Long) {
        if (!amount.isFinite() || amount <= 0.0) return
        viewModelScope.launch {
            activeDao().insertTransaction(
                TransactionEntity(
                    type = type,
                    amount = amount,
                    category = category,
                    title = title,
                    note = note,
                    dateMillis = dateMillis
                )
            )
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            activeDao().deleteTransaction(transaction)
            if (
                transaction.type == TransactionType.EXPENSE &&
                transaction.isPaidFromSavings &&
                transaction.amount.isFinite() &&
                transaction.amount > 0.0
            ) {
                val isAr = settings.value.language == "ar"
                val refundTitle = if (isAr) {
                    "استرجاع مصروف ملغى: ${transaction.title}".take(80)
                } else {
                    "Refunded Deleted Expense: ${transaction.title}".take(80)
                }
                val entry = SavingsMovementEntry(
                    type = SavingsMovementType.DEPOSIT,
                    amount = transaction.amount,
                    title = refundTitle,
                    note = transaction.note
                )
                prefsManager.updateSettings { current ->
                    val updated = computeSavingsBalanceAfterTransactionDelete(transaction, current.savingsWalletBalance)
                    current.copy(
                        savingsWalletBalance = updated,
                        savingsHistoryJson = SavingsHistoryCodec.prepend(current.savingsHistoryJson, entry)
                    )
                }
                _toastEvent.emit(
                    if (isAr) "تم حذف المصروف وإرجاع ${formatDzdAmount(transaction.amount)} دج إلى محفظة الادخار ✓"
                    else "Deleted expense and refunded ${formatDzdAmount(transaction.amount)} DZD to Savings Wallet ✓"
                )
            }
        }
    }

    // Debt Actions
    fun addDebt(
        type: DebtType,
        personName: String,
        amount: Double,
        note: String,
        dueDateMillis: Long,
        autoPay: Boolean,
        isRecurring: Boolean = false,
        monthlyInstallment: Double = 0.0,
        installmentDay: Int = 1,
        initialPaidAmount: Double = 0.0
    ) {
        if (!amount.isFinite() || amount <= 0.0) return
        viewModelScope.launch {
            val safePaidInput = if (initialPaidAmount.isFinite()) initialPaidAmount else 0.0
            val validPaid = safePaidInput.coerceIn(0.0, amount)
            val remaining = (amount - validPaid).coerceAtLeast(0.0)
            val isSettled = remaining <= 0.001
            val now = System.currentTimeMillis()

            val newEntity = DebtEntity(
                type = type,
                personName = personName,
                amount = amount,
                remainingAmount = remaining,
                note = note,
                dueDateMillis = dueDateMillis,
                autoPayOnDueDate = autoPay,
                isRecurringMonthlyInstallment = isRecurring,
                monthlyInstallmentAmount = monthlyInstallment,
                installmentDayOfMonth = installmentDay,
                isPaid = isSettled,
                paidDateMillis = if (isSettled) now else null
            )
            val newId = activeDao().insertDebt(newEntity)
            if (!isSettled) {
                DebtReminderScheduler.scheduleDebtReminder(getApplication(), newEntity.copy(id = newId))
            }
            val isAr = settings.value.language == "ar"
            _toastEvent.emit(
                if (isAr) "تم تسجيل الدين بنجاح" else "Debt recorded successfully"
            )
        }
    }

    fun markDebtAsPaid(debt: DebtEntity) {
        viewModelScope.launch {
            val isAr = settings.value.language == "ar"
            val now = System.currentTimeMillis()
            val payAmount = if (debt.remainingAmount > 0) debt.remainingAmount else debt.amount
            activeDao().updateDebt(
                debt.copy(
                    remainingAmount = 0.0,
                    isPaid = true,
                    paidDateMillis = now
                )
            )
            DebtReminderScheduler.cancelDebtReminder(getApplication(), debt.id)
            // Record wallet impact
            if (debt.type == DebtType.OWED_BY_ME) {
                activeDao().insertTransaction(
                    TransactionEntity(
                        type = TransactionType.EXPENSE,
                        amount = payAmount,
                        category = "debt_payment",
                        title = if (isAr) "دين مسدّد: ${debt.personName}" else "Paid Debt: ${debt.personName}",
                        note = if (isAr) "تسوية يدوية: ${debt.note}" else "Manual settlement: ${debt.note}",
                        dateMillis = now
                    )
                )
            } else {
                activeDao().insertTransaction(
                    TransactionEntity(
                        type = TransactionType.INCOME,
                        amount = payAmount,
                        category = "debt_collection",
                        title = if (isAr) "دين محصّل: ${debt.personName}" else "Collected Debt: ${debt.personName}",
                        note = if (isAr) "تسوية يدوية: ${debt.note}" else "Manual settlement: ${debt.note}",
                        dateMillis = now
                    )
                )
            }
            _toastEvent.emit(
                if (isAr) "تم تحديد الدين كمدفوع وتحديث المحفظة"
                else "Debt marked as paid and wallet updated"
            )
        }
    }

    fun updateDebt(debt: DebtEntity) {
        viewModelScope.launch {
            val isAr = settings.value.language == "ar"
            activeDao().updateDebt(debt)
            if (debt.isPaid || debt.remainingAmount <= 0.001) {
                DebtReminderScheduler.cancelDebtReminder(getApplication(), debt.id)
            } else {
                DebtReminderScheduler.scheduleDebtReminder(getApplication(), debt)
            }
            _toastEvent.emit(
                if (isAr) "تم تحديث الدين بنجاح" else "Debt updated successfully"
            )
        }
    }

    fun addAmountToExistingDebt(
        debt: DebtEntity,
        addedAmount: Double,
        extraNote: String = "",
        updatedDueDateMillis: Long? = null
    ) {
        if (!addedAmount.isFinite() || addedAmount <= 0.0) return
        viewModelScope.launch {
            val isAr = settings.value.language == "ar"
            val newTotal = debt.amount + addedAmount
            val newRemaining = debt.remainingAmount.coerceAtLeast(0.0) + addedAmount
            val cleanExtra = extraNote.trim()
            val mergedNote = when {
                cleanExtra.isBlank() -> debt.note
                debt.note.isBlank() -> cleanExtra
                else -> "${debt.note}\n+ ${addedAmount.toLong()}: $cleanExtra"
            }
            val nextDue = updatedDueDateMillis ?: debt.dueDateMillis
            val updatedDebt = debt.copy(
                amount = newTotal,
                remainingAmount = newRemaining,
                isPaid = false,
                paidDateMillis = null,
                dueDateMillis = nextDue,
                note = mergedNote
            )
            activeDao().updateDebt(updatedDebt)
            DebtReminderScheduler.scheduleDebtReminder(getApplication(), updatedDebt)
            _toastEvent.emit(
                if (isAr) {
                    "تمت إضافة ${addedAmount.toLong()} د.ج إلى دين ${debt.personName} • المتبقي: ${newRemaining.toLong()} د.ج"
                } else {
                    "Added ${addedAmount.toLong()} DZD to ${debt.personName} • Remaining: ${newRemaining.toLong()} DZD"
                }
            )
        }
    }

    fun makePartialPayment(debt: DebtEntity, paymentAmount: Double, recordTransaction: Boolean = true) {
        viewModelScope.launch {
            if (!paymentAmount.isFinite() || paymentAmount <= 0.0) return@launch
            val isAr = settings.value.language == "ar"
            val now = System.currentTimeMillis()
            val actualPay = minOf(paymentAmount, debt.remainingAmount)
            val newRemaining = (debt.remainingAmount - actualPay).coerceAtLeast(0.0)
            val isFullySettled = newRemaining <= 0.001

            val updatedDebt = debt.copy(
                remainingAmount = newRemaining,
                isPaid = isFullySettled,
                paidDateMillis = if (isFullySettled) now else debt.paidDateMillis
            )
            activeDao().updateDebt(updatedDebt)
            if (isFullySettled) {
                DebtReminderScheduler.cancelDebtReminder(getApplication(), debt.id)
            } else {
                DebtReminderScheduler.scheduleDebtReminder(getApplication(), updatedDebt)
            }

            if (recordTransaction) {
                if (debt.type == DebtType.OWED_BY_ME) {
                    activeDao().insertTransaction(
                        TransactionEntity(
                            type = TransactionType.EXPENSE,
                            amount = actualPay,
                            category = "debt_payment",
                            title = if (isAr) "دفعة جزئية: ${debt.personName}" else "Partial Payment: ${debt.personName}",
                            note = if (isAr) {
                                if (isFullySettled) "تسوية نهائية (${debt.note})"
                                else "دفعة جزئية - المتبقي: ${newRemaining.toLong()} د.ج (${debt.note})"
                            } else {
                                if (isFullySettled) "Final settlement (${debt.note})"
                                else "Partial payment - Rem: ${newRemaining.toLong()} DZD (${debt.note})"
                            },
                            dateMillis = now
                        )
                    )
                } else {
                    activeDao().insertTransaction(
                        TransactionEntity(
                            type = TransactionType.INCOME,
                            amount = actualPay,
                            category = "debt_collection",
                            title = if (isAr) "تحصيل جزئي: ${debt.personName}" else "Partial Collection: ${debt.personName}",
                            note = if (isAr) {
                                if (isFullySettled) "تحصيل نهائي (${debt.note})"
                                else "تحصيل جزئي - المتبقي: ${newRemaining.toLong()} د.ج (${debt.note})"
                            } else {
                                if (isFullySettled) "Final collection (${debt.note})"
                                else "Partial collection - Rem: ${newRemaining.toLong()} DZD (${debt.note})"
                            },
                            dateMillis = now
                        )
                    )
                }
            }

            if (isFullySettled) {
                _toastEvent.emit(
                    if (isAr) "تمت تسوية الدين مع ${debt.personName} بالكامل!"
                    else "Debt with ${debt.personName} fully settled!"
                )
            } else {
                _toastEvent.emit(
                    if (isAr) "تم تسجيل دفعة بقيمة ${actualPay.toLong()} د.ج • المتبقي: ${newRemaining.toLong()} د.ج"
                    else "Payment of ${actualPay.toLong()} DZD recorded. Remaining: ${newRemaining.toLong()} DZD"
                )
            }
        }
    }

    fun deleteDebt(debt: DebtEntity) {
        viewModelScope.launch {
            val isAr = settings.value.language == "ar"
            activeDao().deleteDebt(debt)
            DebtReminderScheduler.cancelDebtReminder(getApplication(), debt.id)
            _toastEvent.emit(
                if (isAr) "تم حذف سجل الدين" else "Debt record removed"
            )
        }
    }

    fun clearSettledDebts() {
        viewModelScope.launch {
            val isAr = settings.value.language == "ar"
            val count = activeDao().clearSettledDebts()
            _toastEvent.emit(
                if (isAr) "تم حذف $count ديون مسددة" else "$count settled debts deleted"
            )
        }
    }

    // Salary Settings Actions
    fun updateSalarySettings(amount: Double, payday: Int, employer: String, autoCredit: Boolean) {
        prefsManager.updateSettings { current ->
            current.copy(
                salaryAmount = amount,
                salaryPayday = payday.coerceIn(1, 31),
                salaryEmployer = employer,
                salaryAutoCredit = autoCredit
            )
        }
        viewModelScope.launch {
            checkAndApplySalaryScheduler()
        }
    }

    fun depositSalaryNowManually() {
        viewModelScope.launch {
            checkAndApplySalaryScheduler(force = true)
        }
    }

    // Classic Card Info Update
    fun updateClassicCardDetails(
        holderName: String,
        cardNumber: String,
        expiryDate: String,
        cvc: String,
        bankStyle: String = settings.value.classicCardBankStyle
    ) {
        val cleanName = holderName.trim().ifBlank {
            settings.value.localUsername.trim().ifBlank { "CARDHOLDER" }
        }
        val cleanDigits = cardNumber.filter { it.isDigit() }.take(16)
        val cleanExpiryDigits = expiryDate.filter { it.isDigit() }.take(4)
        val formattedExpiry = if (cleanExpiryDigits.length == 4) {
            "${cleanExpiryDigits.substring(0, 2)}/${cleanExpiryDigits.substring(2, 4)}"
        } else {
            expiryDate.trim().ifBlank { "11/29" }
        }
        val cleanCvc = cvc.filter { it.isDigit() }.take(4)
        val cleanBankStyle = com.example.data.AlgerianBankCardCatalog.findById(bankStyle).id

        prefsManager.updateSettings {
            it.copy(
                classicCardBankStyle = cleanBankStyle,
                classicCardHolderName = cleanName,
                classicCardNumber = cleanDigits,
                classicCardExpiry = formattedExpiry,
                classicCardCvc = cleanCvc
            )
        }
        viewModelScope.launch {
            val isAr = settings.value.language == "ar"
            _toastEvent.emit(
                if (isAr) "تم تحديث معلومات البطاقة بنجاح 💳"
                else "Card details updated successfully 💳"
            )
        }
    }

    fun updateVisaCardDetails(
        provider: String,
        holderName: String,
        cardNumber: String,
        expiryDate: String,
        cvc: String
    ) {
        val cleanProvider = when (provider.uppercase()) {
            "REDOTPAY" -> "REDOTPAY"
            "BYBIT" -> "BYBIT"
            "BINANCE" -> "BINANCE"
            else -> "DEFAULT"
        }
        val cleanName = holderName.trim().ifBlank {
            settings.value.localUsername.trim().ifBlank { "CARDHOLDER" }
        }.uppercase()
        val cleanDigits = cardNumber.filter { it.isDigit() }.take(16)
        val cleanExpiryDigits = expiryDate.filter { it.isDigit() }.take(4)
        val formattedExpiry = if (cleanExpiryDigits.length == 4) {
            "${cleanExpiryDigits.substring(0, 2)}/${cleanExpiryDigits.substring(2, 4)}"
        } else {
            expiryDate.trim().ifBlank { "08/28" }
        }
        val cleanCvc = cvc.filter { it.isDigit() }.take(4)

        prefsManager.updateSettings {
            it.copy(
                visaCardProvider = cleanProvider,
                visaCardHolderName = cleanName,
                visaCardNumber = cleanDigits,
                visaCardExpiry = formattedExpiry,
                visaCardCvc = cleanCvc
            )
        }
        viewModelScope.launch {
            val isAr = settings.value.language == "ar"
            val label = when (cleanProvider) {
                "REDOTPAY" -> if (isAr) "ريدوت باي" else "RedotPay"
                "BYBIT" -> if (isAr) "بايبت" else "Bybit"
                "BINANCE" -> if (isAr) "بينانس" else "Binance"
                else -> if (isAr) "فيزا" else "Visa"
            }
            _toastEvent.emit(
                if (isAr) "تم حفظ بطاقة $label بنجاح 💳"
                else "$label card saved successfully 💳"
            )
        }
    }

    fun updateThirdCardDetails(
        provider: String,
        holderName: String,
        cardNumber: String,
        expiryDate: String,
        cvc: String
    ) {
        val cleanProvider = when (provider.uppercase()) {
            "REDOTPAY" -> "REDOTPAY"
            "BYBIT" -> "BYBIT"
            "BINANCE" -> "BINANCE"
            else -> "DEFAULT"
        }
        val cleanName = holderName.trim().ifBlank {
            settings.value.localUsername.trim().ifBlank { "CARDHOLDER" }
        }.uppercase()
        val cleanDigits = cardNumber.filter { it.isDigit() }.take(16)
        val cleanExpiryDigits = expiryDate.filter { it.isDigit() }.take(4)
        val formattedExpiry = if (cleanExpiryDigits.length == 4) {
            "${cleanExpiryDigits.substring(0, 2)}/${cleanExpiryDigits.substring(2, 4)}"
        } else {
            expiryDate.trim().ifBlank { "08/28" }
        }
        val cleanCvc = cvc.filter { it.isDigit() }.take(4)

        prefsManager.updateSettings {
            it.copy(
                thirdCardEnabled = true,
                thirdCardProvider = cleanProvider,
                thirdCardHolderName = cleanName,
                thirdCardNumber = cleanDigits,
                thirdCardExpiry = formattedExpiry,
                thirdCardCvc = cleanCvc
            )
        }
        viewModelScope.launch {
            val isAr = settings.value.language == "ar"
            val label = when (cleanProvider) {
                "REDOTPAY" -> if (isAr) "ريدوت باي" else "RedotPay"
                "BYBIT" -> if (isAr) "بايبت" else "Bybit"
                "BINANCE" -> if (isAr) "بينانس" else "Binance"
                else -> if (isAr) "فيزا" else "Visa"
            }
            _toastEvent.emit(
                if (isAr) "تم حفظ البطاقة الثالثة ($label) بنجاح 💳"
                else "Third card ($label) saved successfully 💳"
            )
        }
    }

    fun removeThirdCard() {
        prefsManager.updateSettings {
            it.copy(
                thirdCardEnabled = false,
                thirdCardNumber = "",
                thirdCardCvc = ""
            )
        }
        viewModelScope.launch {
            val isAr = settings.value.language == "ar"
            _toastEvent.emit(
                if (isAr) "تمت إزالة البطاقة الثالثة 💳"
                else "Third card removed 💳"
            )
        }
    }

    fun updateCardHubOrder(order: List<com.example.data.CardHubSlot>) {
        val encoded = com.example.data.CardHubOrder.encode(order)
        prefsManager.updateSettings {
            it.copy(cardHubOrderJson = encoded)
        }
    }

    // App Lock & Auto-Lock Lifecycle Security
    private var backgroundedAtMillis: Long = 0L
    @Volatile
    private var suppressNextAutoLockOnce: Boolean = false

    fun suppressNextAutoLockForExternalFlow() {
        suppressNextAutoLockOnce = true
    }

    fun onAppBackgrounded() {
        collapseAllVaultContainersForSecurity()
        backgroundedAtMillis = System.currentTimeMillis()
    }

    fun onAppForegrounded() {
        if (suppressNextAutoLockOnce) {
            suppressNextAutoLockOnce = false
            backgroundedAtMillis = 0L
            return
        }
        val cfg = settings.value
        val hasActiveLock = cfg.biometricEnabled
        val timeoutSeconds = cfg.autoLockTimeoutSeconds
        val bgAt = backgroundedAtMillis
        backgroundedAtMillis = 0L
        if (hasActiveLock && timeoutSeconds >= 0 && bgAt > 0L) {
            val elapsedMs = System.currentTimeMillis() - bgAt
            if (elapsedMs >= timeoutSeconds * 1000L) {
                lockApp()
            }
        }
    }

    fun lockApp() {
        collapseAllVaultContainersForSecurity()
        com.example.security.ClipboardSecurityHelper.clearIfSensitiveActive(getApplication())
        val cfg = settings.value
        if (cfg.biometricEnabled) {
            _isAppLocked.value = true
        }
    }

    fun toggleKeyExpanded(keyId: String) {
        _expandedKeyIds.value = if (keyId in _expandedKeyIds.value) {
            _expandedKeyIds.value - keyId
        } else {
            _expandedKeyIds.value + keyId
        }
    }

    fun collapseAllKeys() {
        _expandedKeyIds.value = emptySet()
        _revealedKeyIds.value = emptySet()
    }

    fun toggleKeyRevealed(keyId: String) {
        _revealedKeyIds.value = if (keyId in _revealedKeyIds.value) {
            _revealedKeyIds.value - keyId
        } else {
            _revealedKeyIds.value + keyId
        }
    }

    fun toggleCcpExpanded(accountId: String) {
        _expandedCcpIds.value = if (accountId in _expandedCcpIds.value) {
            _expandedCcpIds.value - accountId
        } else {
            _expandedCcpIds.value + accountId
        }
    }

    fun collapseAllCcps() {
        _expandedCcpIds.value = emptySet()
    }

    fun toggleCcpHidden(accountId: String) {
        _hiddenCcpIds.value = if (accountId in _hiddenCcpIds.value) {
            _hiddenCcpIds.value - accountId
        } else {
            _hiddenCcpIds.value + accountId
        }
    }

    fun collapseAllVaultContainersForSecurity() {
        _expandedKeyIds.value = emptySet()
        _revealedKeyIds.value = emptySet()
        _expandedCcpIds.value = emptySet()
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefsManager.updateSettings { it.copy(biometricEnabled = enabled) }
    }

    fun setAutoLockTimeoutSeconds(seconds: Int) {
        val normalized = when (seconds) {
            -1, 0, 30, 60, 300 -> seconds
            else -> 30
        }
        prefsManager.updateSettings { it.copy(autoLockTimeoutSeconds = normalized) }
    }

    fun unlockAppByBiometrics() {
        backgroundedAtMillis = 0L
        _isAppLocked.value = false
    }

    fun disableBiometricsAndUnlock() {
        backgroundedAtMillis = 0L
        prefsManager.updateSettings { it.copy(biometricEnabled = false) }
        _isAppLocked.value = false
    }

    // Appearance & Localization
    fun setLanguage(lang: String) {
        prefsManager.updateSettings { it.copy(language = lang) }
    }

    fun setThemeMode(mode: String) {
        prefsManager.updateSettings { it.copy(themeMode = mode) }
    }

    fun setDesignStyle(style: String) {
        prefsManager.updateSettings { it.copy(designStyle = style) }
    }

    fun setBreathingGlowEnabled(enabled: Boolean) {
        prefsManager.updateSettings { it.copy(breathingGlowEnabled = enabled) }
    }

    fun setScreenshotProtectionEnabled(enabled: Boolean) {
        prefsManager.updateSettings { it.copy(screenshotProtectionEnabled = enabled) }
    }

    fun addVaultKey(
        label: String,
        username: String,
        secretValue: String,
        category: String,
        note: String,
        isPinned: Boolean = false,
        appPresetId: String = "custom"
    ) {
        val cleanLabel = label.trim()
        val cleanSecret = secretValue.trim()
        if (cleanLabel.isEmpty() || cleanSecret.isEmpty()) return
        val resolvedPreset = appPresetId.trim().ifBlank {
            VaultAppPresets.detectFromLabel(cleanLabel)?.id ?: "custom"
        }
        val now = System.currentTimeMillis()
        prefsManager.updateSettings { current ->
            val updated = HubVaultCodec.parseVaultKeys(current.vaultKeysJson) + VaultKeyEntry(
                label = cleanLabel,
                username = username.trim(),
                secretValue = cleanSecret,
                category = category.ifBlank { "APP" },
                note = note.trim(),
                isPinned = isPinned,
                pinnedAt = if (isPinned) now else 0L,
                createdAt = now,
                appPresetId = resolvedPreset
            )
            current.copy(vaultKeysJson = HubVaultCodec.encodeVaultKeys(updated))
        }
    }

    fun updateVaultKey(
        keyId: String,
        label: String,
        username: String,
        secretValue: String,
        category: String,
        note: String,
        isPinned: Boolean = false,
        appPresetId: String = "custom"
    ) {
        val cleanLabel = label.trim()
        val cleanSecret = secretValue.trim()
        if (cleanLabel.isEmpty() || cleanSecret.isEmpty()) return
        val resolvedPreset = appPresetId.trim().ifBlank {
            VaultAppPresets.detectFromLabel(cleanLabel)?.id ?: "custom"
        }
        val now = System.currentTimeMillis()
        prefsManager.updateSettings { current ->
            val updated = HubVaultCodec.parseVaultKeys(current.vaultKeysJson).map { item ->
                if (item.id == keyId) {
                    item.copy(
                        label = cleanLabel,
                        username = username.trim(),
                        secretValue = cleanSecret,
                        category = category.ifBlank { "APP" },
                        note = note.trim(),
                        isPinned = isPinned,
                        pinnedAt = when {
                            isPinned && !item.isPinned -> now
                            isPinned -> item.pinnedAt.takeIf { it > 0L } ?: now
                            else -> 0L
                        },
                        appPresetId = resolvedPreset
                    )
                } else {
                    item
                }
            }
            current.copy(vaultKeysJson = HubVaultCodec.encodeVaultKeys(updated))
        }
    }

    fun togglePinVaultKey(keyId: String) {
        val now = System.currentTimeMillis()
        prefsManager.updateSettings { current ->
            val updated = HubVaultCodec.parseVaultKeys(current.vaultKeysJson).map { item ->
                if (item.id == keyId) {
                    val nextPinned = !item.isPinned
                    item.copy(
                        isPinned = nextPinned,
                        pinnedAt = if (nextPinned) now else 0L
                    )
                } else {
                    item
                }
            }
            current.copy(vaultKeysJson = HubVaultCodec.encodeVaultKeys(updated))
        }
    }

    fun deleteVaultKey(keyId: String) {
        _expandedKeyIds.value = _expandedKeyIds.value - keyId
        _revealedKeyIds.value = _revealedKeyIds.value - keyId
        prefsManager.updateSettings { current ->
            val updated = HubVaultCodec.parseVaultKeys(current.vaultKeysJson).filterNot { it.id == keyId }
            current.copy(vaultKeysJson = HubVaultCodec.encodeVaultKeys(updated))
        }
    }

    fun addCcpAccount(
        firstName: String,
        lastName: String,
        ccpNumber: String,
        ccpKey: String,
        ripNumber: String,
        agencyNote: String,
        contactName: String = "",
        contactUri: String = "",
        contactPhone: String = "",
        isPinned: Boolean = false,
        appPresetId: String = "algerie_poste"
    ) {
        val cleanFirst = firstName.trim()
        val cleanLast = lastName.trim()
        val fullName = listOf(cleanFirst, cleanLast).filter { it.isNotEmpty() }.joinToString(" ")
        val cleanCcp = ccpNumber.filter { it.isDigit() }
        val cleanRip = ripNumber.filter { it.isDigit() }.take(20)
        if (fullName.isEmpty() || (cleanCcp.isEmpty() && cleanRip.isEmpty())) return

        val existing = HubVaultCodec.parseCcpAccounts(settings.value.ccpAccountsJson)
        if (existing.size >= HubVaultCodec.MAX_CCP_PROFILES) {
            viewModelScope.launch {
                val isAr = settings.value.language == "ar"
                _toastEvent.emit(
                    if (isAr) "تم بلوغ الحد الأقصى (${HubVaultCodec.MAX_CCP_PROFILES} ملف)"
                    else "Maximum limit reached (${HubVaultCodec.MAX_CCP_PROFILES} profiles)"
                )
            }
            return
        }

        val resolvedPreset = appPresetId.trim().ifBlank { "algerie_poste" }
        val now = System.currentTimeMillis()
        prefsManager.updateSettings { current ->
            val currentList = HubVaultCodec.parseCcpAccounts(current.ccpAccountsJson)
            if (currentList.size >= HubVaultCodec.MAX_CCP_PROFILES) return@updateSettings current
            val updated = currentList + CcpAccountEntry(
                firstName = cleanFirst,
                lastName = cleanLast,
                holderName = fullName,
                ccpNumber = cleanCcp,
                ccpKey = ccpKey.filter { it.isDigit() }.take(2),
                ripFull = cleanRip,
                agencyNote = agencyNote.trim(),
                contactName = contactName.trim(),
                contactUri = contactUri.trim(),
                contactPhone = contactPhone.trim(),
                isPinned = isPinned,
                pinnedAt = if (isPinned) now else 0L,
                createdAt = now,
                appPresetId = resolvedPreset
            )
            current.copy(ccpAccountsJson = HubVaultCodec.encodeCcpAccounts(updated))
        }
    }

    fun updateCcpAccount(
        accountId: String,
        firstName: String,
        lastName: String,
        ccpNumber: String,
        ccpKey: String,
        ripNumber: String,
        agencyNote: String,
        contactName: String = "",
        contactUri: String = "",
        contactPhone: String = "",
        isPinned: Boolean = false,
        appPresetId: String = "algerie_poste"
    ) {
        val cleanFirst = firstName.trim()
        val cleanLast = lastName.trim()
        val fullName = listOf(cleanFirst, cleanLast).filter { it.isNotEmpty() }.joinToString(" ")
        val cleanCcp = ccpNumber.filter { it.isDigit() }
        val cleanRip = ripNumber.filter { it.isDigit() }.take(20)
        if (fullName.isEmpty() || (cleanCcp.isEmpty() && cleanRip.isEmpty())) return
        val resolvedPreset = appPresetId.trim().ifBlank { "algerie_poste" }
        val now = System.currentTimeMillis()

        prefsManager.updateSettings { current ->
            val updated = HubVaultCodec.parseCcpAccounts(current.ccpAccountsJson).map { item ->
                if (item.id == accountId) {
                    item.copy(
                        firstName = cleanFirst,
                        lastName = cleanLast,
                        holderName = fullName,
                        ccpNumber = cleanCcp,
                        ccpKey = ccpKey.filter { it.isDigit() }.take(2),
                        ripFull = cleanRip,
                        agencyNote = agencyNote.trim(),
                        contactName = contactName.trim(),
                        contactUri = contactUri.trim(),
                        contactPhone = contactPhone.trim(),
                        isPinned = isPinned,
                        pinnedAt = when {
                            isPinned && !item.isPinned -> now
                            isPinned -> item.pinnedAt.takeIf { it > 0L } ?: now
                            else -> 0L
                        },
                        appPresetId = resolvedPreset
                    )
                } else {
                    item
                }
            }
            current.copy(ccpAccountsJson = HubVaultCodec.encodeCcpAccounts(updated))
        }
    }

    fun togglePinCcpAccount(accountId: String) {
        val now = System.currentTimeMillis()
        prefsManager.updateSettings { current ->
            val updated = HubVaultCodec.parseCcpAccounts(current.ccpAccountsJson).map { item ->
                if (item.id == accountId) {
                    val nextPinned = !item.isPinned
                    item.copy(
                        isPinned = nextPinned,
                        pinnedAt = if (nextPinned) now else 0L
                    )
                } else {
                    item
                }
            }
            current.copy(ccpAccountsJson = HubVaultCodec.encodeCcpAccounts(updated))
        }
    }

    fun deleteCcpAccount(accountId: String) {
        _expandedCcpIds.value = _expandedCcpIds.value - accountId
        _hiddenCcpIds.value = _hiddenCcpIds.value - accountId
        prefsManager.updateSettings { current ->
            val updated = HubVaultCodec.parseCcpAccounts(current.ccpAccountsJson).filterNot { it.id == accountId }
            current.copy(ccpAccountsJson = HubVaultCodec.encodeCcpAccounts(updated))
        }
    }

    // Stealth Mode (Privacy / Shake to Hide)
    fun toggleStealthMode(): Boolean {
        val current = settings.value.stealthMode
        val next = !current
        val isAr = settings.value.language == "ar"
        prefsManager.updateSettings { it.copy(stealthMode = next) }
        viewModelScope.launch {
            if (next) {
                _toastEvent.emit(
                    if (isAr) "وضع التخفي نشط • المبالغ المالية مخفية"
                    else "Stealth Mode Active • Financial amounts masked"
                )
            } else {
                _toastEvent.emit(
                    if (isAr) "تم إيقاف وضع التخفي • المبالغ ظاهرة"
                    else "Stealth Mode Disabled • Amounts revealed"
                )
            }
        }
        return next
    }

    // Savings Wallet (محفظة الادخار) Actions
    fun transferToSavings(amount: Double, note: String = "") {
        if (!amount.isFinite() || amount <= 0.0) return
        val isAr = settings.value.language == "ar"
        val currentAvailable = financialSummary.value.totalBalance
        if (amount > currentAvailable && currentAvailable > 0) {
            viewModelScope.launch {
                _toastEvent.emit(
                    if (isAr) "تنبيه: المبلغ المراد تحويله أكبر من الرصيد المتاح (${formatDzdAmount(currentAvailable)} دج)"
                    else "Notice: Amount exceeds available balance (${formatDzdAmount(currentAvailable)} DZD)"
                )
            }
        }
        val entry = SavingsMovementEntry(
            type = SavingsMovementType.DEPOSIT,
            amount = amount,
            title = if (isAr) "إيداع إلى الادخار" else "Deposit to Savings",
            note = note
        )
        prefsManager.updateSettings { current ->
            val updated = current.savingsWalletBalance + amount
            current.copy(
                savingsWalletBalance = updated,
                savingsHistoryJson = SavingsHistoryCodec.prepend(current.savingsHistoryJson, entry)
            )
        }
        viewModelScope.launch {
            _toastEvent.emit(
                if (isAr) "تم تحويل ${formatDzdAmount(amount)} دج إلى محفظة الادخار بنجاح 💰"
                else "Savings Wallet: ${formatDzdAmount(amount)} DZD transferred to savings 💰"
            )
        }
    }

    fun withdrawFromSavings(amount: Double, note: String = "") {
        if (!amount.isFinite() || amount <= 0.0) return
        val isAr = settings.value.language == "ar"
        val currentSavings = settings.value.savingsWalletBalance
        val actualWithdraw = amount.coerceAtMost(currentSavings)
        if (actualWithdraw <= 0.0) return
        val entry = SavingsMovementEntry(
            type = SavingsMovementType.WITHDRAW,
            amount = actualWithdraw,
            title = if (isAr) "سحب إلى الرصيد المتاح" else "Withdraw to Available",
            note = note
        )
        prefsManager.updateSettings { current ->
            val updated = (current.savingsWalletBalance - actualWithdraw).coerceAtLeast(0.0)
            current.copy(
                savingsWalletBalance = updated,
                savingsHistoryJson = SavingsHistoryCodec.prepend(current.savingsHistoryJson, entry)
            )
        }
        viewModelScope.launch {
            _toastEvent.emit(
                if (isAr) "تم استرجاع ${formatDzdAmount(actualWithdraw)} دج إلى الرصيد المتاح بنجاح 🔄"
                else "Savings Wallet: ${formatDzdAmount(actualWithdraw)} DZD withdrawn back to available balance 🔄"
            )
        }
    }

    fun spendFromSavings(amount: Double, title: String, category: String, note: String) {
        if (!amount.isFinite() || amount <= 0.0) return
        val currentSavings = settings.value.savingsWalletBalance
        if (amount > currentSavings) {
            viewModelScope.launch {
                val isAr = settings.value.language == "ar"
                _toastEvent.emit(
                    if (isAr) "رصيد محفظة الادخار غير كافٍ (${formatDzdAmount(currentSavings)} دج)"
                    else "Insufficient savings wallet balance (${formatDzdAmount(currentSavings)} DZD)"
                )
            }
            return
        }

        val isAr = settings.value.language == "ar"
        val resolvedTitle = title.ifBlank { if (isAr) "مصروف من محفظة الادخار" else "Savings Expense" }
        val entry = SavingsMovementEntry(
            type = SavingsMovementType.SPEND,
            amount = amount,
            title = resolvedTitle,
            note = note
        )

        // 1. Deduct directly from savings balance & record in history
        prefsManager.updateSettings { current ->
            val updated = (current.savingsWalletBalance - amount).coerceAtLeast(0.0)
            current.copy(
                savingsWalletBalance = updated,
                savingsHistoryJson = SavingsHistoryCodec.prepend(current.savingsHistoryJson, entry)
            )
        }

        // 2. Automatically record in Expenses page (صفحة المصاريف)
        viewModelScope.launch {
            val savingsTagNote = if (isAr) {
                if (note.isNotBlank()) "تم الصرف من محفظة الادخار • $note" else "تم الصرف مباشرة من محفظة الادخار"
            } else {
                if (note.isNotBlank()) "Paid from Savings Wallet • $note" else "Paid directly from Savings Wallet"
            }

            activeDao().insertTransaction(
                TransactionEntity(
                    type = TransactionType.EXPENSE,
                    amount = amount,
                    category = category.ifBlank { "savings_expense" },
                    title = resolvedTitle,
                    note = savingsTagNote,
                    dateMillis = System.currentTimeMillis(),
                    isPaidFromSavings = true
                )
            )

            _toastEvent.emit(
                if (isAr) "تم تسجيل المصروف بنجاح (${formatDzdAmount(amount)} دج) وخصمه من محفظة الادخار 💳"
                else "Expense of ${formatDzdAmount(amount)} DZD recorded and deducted from Savings Wallet 💳"
            )
        }
    }

    fun setSavingsGoal(goalAmount: Double) {
        val safe = goalAmount.coerceAtLeast(0.0)
        prefsManager.updateSettings { it.copy(savingsGoalAmount = safe) }
    }

    fun clearSavingsHistory() {
        prefsManager.updateSettings { it.copy(savingsHistoryJson = "[]") }
    }

    // Keep vault aliases for dashboard compatibility
    fun lockToVault(amount: Double) = transferToSavings(amount)
    fun unlockFromVault(amount: Double) = withdrawFromSavings(amount)

    // Backup & Restore (Supports both sanitized JSON and AES-256-GCM passphrase-encrypted backups)
    suspend fun exportBackupJson(passphrase: String = ""): String {
        val trans = activeDao().getAllTransactionsList()
        val debts = activeDao().getAllDebtsList()
        val archivedSms = activeDao().getAllArchivedAlgPosteSmsList()
        val userConfig = settings.value
        val usePassphraseEncryption = passphrase.isNotBlank()

        val payload = BackupPayload(
            exportTimestamp = System.currentTimeMillis(),
            appVersion = APP_VERSION,
            developer = DEVELOPER_NAME,
            transactions = trans,
            debts = debts,
            salaryAmount = userConfig.salaryAmount,
            salaryPayday = userConfig.salaryPayday,
            salaryEmployer = userConfig.salaryEmployer,
            classicCardHolderName = userConfig.classicCardHolderName,
            classicCardNumber = userConfig.classicCardNumber,
            classicCardExpiry = userConfig.classicCardExpiry,
            classicCardCvc = userConfig.classicCardCvc,
            archivedAlgPosteSms = archivedSms,
            classicCardBankStyle = userConfig.classicCardBankStyle,
            visaCardProvider = userConfig.visaCardProvider,
            visaCardHolderName = userConfig.visaCardHolderName,
            visaCardNumber = userConfig.visaCardNumber,
            visaCardExpiry = userConfig.visaCardExpiry,
            visaCardCvc = userConfig.visaCardCvc,
            thirdCardEnabled = userConfig.thirdCardEnabled,
            thirdCardProvider = userConfig.thirdCardProvider,
            thirdCardHolderName = userConfig.thirdCardHolderName,
            thirdCardNumber = userConfig.thirdCardNumber,
            thirdCardExpiry = userConfig.thirdCardExpiry,
            thirdCardCvc = userConfig.thirdCardCvc,
            vaultKeysJson = userConfig.vaultKeysJson,
            ccpAccountsJson = userConfig.ccpAccountsJson,
            savingsWalletBalance = userConfig.savingsWalletBalance,
            savingsGoalAmount = userConfig.savingsGoalAmount,
            savingsHistoryJson = userConfig.savingsHistoryJson,
            includeAssetsInSavingsTotal = userConfig.includeAssetsInSavingsTotal,
            goldAssetHoldingsJson = userConfig.goldAssetHoldingsJson,
            binanceHoldingsJson = userConfig.binanceHoldingsJson,
            cardHubOrderJson = userConfig.cardHubOrderJson,
            localUsername = userConfig.localUsername,
            localWilayaCode = userConfig.localWilayaCode,
            localWilayaName = userConfig.localWilayaName,
            localCommuneName = userConfig.localCommuneName
        )

        val output = if (usePassphraseEncryption) {
            withContext(Dispatchers.Default) {
                val fullJson = payload.toJsonString(includeSensitiveCardFields = true)
                VaultCryptoManager.encryptBackupWithPassphrase(fullJson, passphrase)
            }
        } else {
            payload.toJsonString(includeSensitiveCardFields = false)
        }

        prefsManager.updateSettings {
            it.copy(lastBackupTimestamp = System.currentTimeMillis())
        }
        return output
    }

    suspend fun restoreFromBackupWithOutcome(
        rawContent: String,
        passphrase: String = ""
    ): BackupRestoreOutcome {
        val plainJson = if (VaultCryptoManager.isEncryptedBackupEnvelope(rawContent)) {
            if (passphrase.isBlank()) return BackupRestoreOutcome.WRONG_PASSWORD_OR_TAMPERED
            val decrypted = withContext(Dispatchers.Default) {
                VaultCryptoManager.decryptBackupWithPassphrase(rawContent, passphrase)
            } ?: return BackupRestoreOutcome.WRONG_PASSWORD_OR_TAMPERED
            decrypted
        } else {
            rawContent
        }

        val payload = BackupPayload.fromJsonString(plainJson)
            ?: return BackupRestoreOutcome.INVALID_FORMAT

        activeDatabase().withTransaction {
            activeDao().clearAllTransactions()
            activeDao().clearAllDebts()

            activeDao().insertTransactions(payload.transactions)
            activeDao().insertDebts(payload.debts)
            if (payload.archivedAlgPosteSms.isNotEmpty()) {
                // Merge into permanent archive without deleting existing permanent SMS records
                activeDao().insertArchivedSmsIfMissing(payload.archivedAlgPosteSms)
            }
        }

        prefsManager.updateSettings {
            it.copy(
                salaryAmount = payload.salaryAmount,
                salaryPayday = payload.salaryPayday,
                salaryEmployer = payload.salaryEmployer,
                classicCardBankStyle = payload.classicCardBankStyle.ifBlank { it.classicCardBankStyle },
                classicCardHolderName = payload.classicCardHolderName,
                classicCardNumber = payload.classicCardNumber.ifBlank { it.classicCardNumber },
                classicCardExpiry = payload.classicCardExpiry,
                classicCardCvc = payload.classicCardCvc.ifBlank { it.classicCardCvc },
                visaCardProvider = payload.visaCardProvider.ifBlank { it.visaCardProvider },
                visaCardHolderName = payload.visaCardHolderName.ifBlank { it.visaCardHolderName },
                visaCardNumber = payload.visaCardNumber.ifBlank { it.visaCardNumber },
                visaCardExpiry = payload.visaCardExpiry.ifBlank { it.visaCardExpiry },
                visaCardCvc = payload.visaCardCvc.ifBlank { it.visaCardCvc },
                thirdCardEnabled = payload.thirdCardEnabled || it.thirdCardEnabled,
                thirdCardProvider = payload.thirdCardProvider.ifBlank { it.thirdCardProvider },
                thirdCardHolderName = payload.thirdCardHolderName.ifBlank { it.thirdCardHolderName },
                thirdCardNumber = payload.thirdCardNumber.ifBlank { it.thirdCardNumber },
                thirdCardExpiry = payload.thirdCardExpiry.ifBlank { it.thirdCardExpiry },
                thirdCardCvc = payload.thirdCardCvc.ifBlank { it.thirdCardCvc },
                vaultKeysJson = if (payload.vaultKeysJson != "[]" && payload.vaultKeysJson.isNotBlank()) payload.vaultKeysJson else it.vaultKeysJson,
                ccpAccountsJson = if (payload.ccpAccountsJson != "[]" && payload.ccpAccountsJson.isNotBlank()) payload.ccpAccountsJson else it.ccpAccountsJson,
                savingsWalletBalance = payload.savingsWalletBalance ?: it.savingsWalletBalance,
                savingsGoalAmount = payload.savingsGoalAmount ?: it.savingsGoalAmount,
                savingsHistoryJson = payload.savingsHistoryJson?.ifBlank { null } ?: it.savingsHistoryJson,
                includeAssetsInSavingsTotal = payload.includeAssetsInSavingsTotal ?: it.includeAssetsInSavingsTotal,
                goldAssetHoldingsJson = payload.goldAssetHoldingsJson?.ifBlank { null } ?: it.goldAssetHoldingsJson,
                binanceHoldingsJson = payload.binanceHoldingsJson?.ifBlank { null } ?: it.binanceHoldingsJson,
                cardHubOrderJson = payload.cardHubOrderJson?.ifBlank { null } ?: it.cardHubOrderJson,
                localUsername = payload.localUsername?.ifBlank { null } ?: it.localUsername,
                localWilayaCode = payload.localWilayaCode?.ifBlank { null } ?: it.localWilayaCode,
                localWilayaName = payload.localWilayaName?.ifBlank { null } ?: it.localWilayaName,
                localCommuneName = payload.localCommuneName?.ifBlank { null } ?: it.localCommuneName,
                lastBackupTimestamp = System.currentTimeMillis()
            )
        }
        reanalyzePermanentSmsArchive(showFeedback = false)
        val isAr = settings.value.language == "ar"
        _toastEvent.emit(
            if (isAr) "تمت استعادة ${payload.transactions.size} معاملة و ${payload.debts.size} ديون"
            else "Restored ${payload.transactions.size} transactions and ${payload.debts.size} debts"
        )
        return BackupRestoreOutcome.SUCCESS
    }

    private var lastUpdateCheckElapsedMillis: Long = 0L
    private var dismissedUpdateVersionTag: String? = null
    private var activeDownloadTag: String? = null

    // In-App GitHub Releases Optional Update & Silent Auto-Download + Auto-Retry Actions
    fun checkForUpdates(manual: Boolean = true) {
        if (manual) {
            dismissedUpdateVersionTag = null
        }

        lastUpdateCheckElapsedMillis = System.currentTimeMillis()
        viewModelScope.launch {
            val currentState = _updateUiState.value
            val hasOngoingOrReadyUpdate = when (currentState) {
                is AppUpdateUiState.Downloading -> true
                is AppUpdateUiState.ReadyToInstall -> true
                is AppUpdateUiState.Error -> currentState.release != null && downloadJob?.isActive == true
                else -> false
            }

            if (manual) {
                _showUpdateDialog.value = true
                if (hasOngoingOrReadyUpdate) {
                    return@launch
                }
                _updateUiState.value = AppUpdateUiState.Checking
            }

            val result = AppUpdateManager.fetchLatestRelease()
            result.onSuccess { release ->
                val isNewer = compareVersionTags(release.versionName, APP_VERSION) > 0
                if (isNewer) {
                    val existing = _updateUiState.value
                    val sameTagActive = (
                        (existing is AppUpdateUiState.Downloading && existing.release.tagName == release.tagName) ||
                            (existing is AppUpdateUiState.ReadyToInstall && existing.release.tagName == release.tagName) ||
                            (existing is AppUpdateUiState.Error && existing.release?.tagName == release.tagName && downloadJob?.isActive == true)
                        )
                    if (sameTagActive) {
                        return@onSuccess
                    }

                    val alreadyDownloadedApk = AppUpdateManager.findReadyUpdateApk(getApplication(), release)
                    if (alreadyDownloadedApk != null) {
                        downloadJob?.cancel()
                        downloadJob = null
                        activeDownloadTag = null
                        _updateUiState.value = AppUpdateUiState.ReadyToInstall(release, alreadyDownloadedApk)
                    } else {
                        _updateUiState.value = AppUpdateUiState.Available(release)
                        if (!release.apkDownloadUrl.isNullOrBlank()) {
                            startUpdateDownload(releaseOverride = release, forceRestart = false)
                        }
                    }
                    // Automatic background checks stay silent — the luxurious top-bar arrow appears without popping a dialog.
                    if (manual) {
                        _showUpdateDialog.value = true
                    }
                } else if (manual) {
                    _updateUiState.value = AppUpdateUiState.UpToDate(APP_VERSION)
                    _showUpdateDialog.value = true
                }
            }.onFailure { err ->
                if (manual) {
                    val isAr = settings.value.language == "ar"
                    val msg = err.localizedMessage?.takeIf { it.isNotBlank() }
                        ?: if (isAr) "تعذر الاتصال بجيت هب" else "Unable to reach GitHub"
                    _updateUiState.value = AppUpdateUiState.Error(msg)
                    _showUpdateDialog.value = true
                }
            }
        }
    }

    fun checkForUpdatesIfDueOnResume() {
        val now = System.currentTimeMillis()
        if (now - lastUpdateCheckElapsedMillis >= 5 * 60 * 1000L) {
            checkForUpdates(manual = false)
        }
    }

    fun openUpdateDialogFromIndicator() {
        val current = _updateUiState.value
        _showUpdateDialog.value = true
        when (current) {
            is AppUpdateUiState.Available -> {
                if (!current.release.apkDownloadUrl.isNullOrBlank() && downloadJob?.isActive != true) {
                    startUpdateDownload(releaseOverride = current.release, forceRestart = false)
                }
            }
            is AppUpdateUiState.Error -> {
                val rel = current.release
                if (rel != null && !rel.apkDownloadUrl.isNullOrBlank() && downloadJob?.isActive != true) {
                    startUpdateDownload(releaseOverride = rel, forceRestart = true)
                }
            }
            else -> Unit
        }
    }

    fun startUpdateDownload(
        releaseOverride: GitHubReleaseInfo? = null,
        forceRestart: Boolean = true
    ) {
        val release = releaseOverride ?: when (val current = _updateUiState.value) {
            is AppUpdateUiState.Available -> current.release
            is AppUpdateUiState.Error -> current.release
            else -> null
        } ?: run {
            checkForUpdates(manual = true)
            return
        }

        if (release.apkDownloadUrl.isNullOrBlank()) {
            val isAr = settings.value.language == "ar"
            val msg = if (isAr) {
                "ملف التحديث غير مرفق بعد في هذا الإصدار. أعد المحاولة بعد قليل."
            } else {
                "No downloadable APK asset is available for this release yet."
            }
            _updateUiState.value = AppUpdateUiState.Error(msg, release, isRetrying = false)
            return
        }

        if (!forceRestart && downloadJob?.isActive == true && activeDownloadTag == release.tagName) {
            return
        }

        downloadJob?.cancel()
        activeDownloadTag = release.tagName
        downloadJob = viewModelScope.launch {
            val appCtx = getApplication<Application>()
            var attempt = 0
            while (isActive) {
                val cachedValidApk = AppUpdateManager.findReadyUpdateApk(appCtx, release)
                if (cachedValidApk != null) {
                    _updateUiState.value = AppUpdateUiState.ReadyToInstall(release, cachedValidApk)
                    break
                }

                var completed = false
                var failedMessage: String? = null
                _updateUiState.value = AppUpdateUiState.Downloading(release, 0L, release.apkSizeBytes)

                AppUpdateManager.downloadApk(appCtx, release).collect { dlState ->
                    when (dlState) {
                        is DownloadState.Progress -> {
                            _updateUiState.value = AppUpdateUiState.Downloading(
                                release = release,
                                bytesDownloaded = dlState.bytesDownloaded,
                                totalBytes = dlState.totalBytes
                            )
                        }
                        is DownloadState.Completed -> {
                            completed = true
                            _updateUiState.value = AppUpdateUiState.ReadyToInstall(
                                release = release,
                                apkFile = dlState.file
                            )
                        }
                        is DownloadState.Failed -> {
                            failedMessage = dlState.errorMessage
                        }
                    }
                }

                if (completed) break

                attempt++
                val isAr = settings.value.language == "ar"
                val detail = failedMessage?.takeIf { it.isNotBlank() }
                val retryMsg = if (isAr) {
                    "تعذر إكمال التحميل، سيتم إعادة المحاولة تلقائياً..."
                } else {
                    if (detail != null) "Download failed ($detail). Retrying automatically..."
                    else "Download failed. Retrying automatically..."
                }
                _updateUiState.value = AppUpdateUiState.Error(
                    message = retryMsg,
                    release = release,
                    isRetrying = true
                )
                delay(AppUpdateManager.computeRetryBackoffMillis(attempt))
            }
            activeDownloadTag = null
        }
    }

    fun reportInstallPermissionUnavailable() {
        val currentRelease = when (val current = _updateUiState.value) {
            is AppUpdateUiState.ReadyToInstall -> current.release
            is AppUpdateUiState.Error -> current.release
            else -> null
        }
        val isAr = settings.value.language == "ar"
        val msg = if (isAr) {
            "يرجى تفعيل إذن تثبيت التطبيقات من مصادر غير معروفة من إعدادات الجهاز."
        } else {
            "Please enable 'Install unknown apps' permission in device settings."
        }
        _updateUiState.value = AppUpdateUiState.Error(msg, currentRelease, isRetrying = false)
    }

    fun installReadyUpdate(launcherContext: Context = getApplication()): Boolean {
        val current = _updateUiState.value as? AppUpdateUiState.ReadyToInstall ?: return false
        val launched = AppUpdateManager.installApk(launcherContext, current.apkFile)
        if (!launched) {
            val isAr = settings.value.language == "ar"
            val msg = if (isAr) {
                "تعذر تشغيل مثبت التحديثات على جهازك. يرجى إعادة المحاولة."
            } else {
                "Could not launch the package installer. Please try again."
            }
            _updateUiState.value = AppUpdateUiState.Error(msg, current.release, isRetrying = false)
        }
        return launched
    }

    fun dismissUpdateDialog() {
        val releaseTag = when (val current = _updateUiState.value) {
            is AppUpdateUiState.Available -> current.release.tagName
            is AppUpdateUiState.Downloading -> current.release.tagName
            is AppUpdateUiState.ReadyToInstall -> current.release.tagName
            is AppUpdateUiState.Error -> current.release?.tagName
            else -> null
        }
        if (!releaseTag.isNullOrBlank()) {
            dismissedUpdateVersionTag = releaseTag
        }
        _showUpdateDialog.value = false
    }

    // First-Launch Luxury Permissions Onboarding
    fun completePermissionsOnboarding(enableSmsSyncIfGranted: Boolean = true) {
        val smsGranted = AlgPosteSmsInboxScanner.hasSmsPermissions(getApplication())
        prefsManager.updateSettings {
            it.copy(
                hasCompletedPermissionsOnboarding = true,
                baridiMobSmsSyncEnabled = if (enableSmsSyncIfGranted && smsGranted) true else it.baridiMobSmsSyncEnabled
            )
        }
        if (smsGranted) {
            if (enableSmsSyncIfGranted) {
                scanAlgPosteSmsInbox(getApplication(), showFeedback = false)
            } else {
                refreshCurrentBalanceOnlyFromInbox(getApplication(), showFeedback = false)
            }
        }
    }

    private data class CloudVaultSnapshotForSync(
        val settings: UserSettings,
        val transactions: List<TransactionEntity>,
        val debts: List<DebtEntity>,
        val archivedSms: List<ArchivedAlgPosteSmsEntity>
    )

    private suspend fun clearCloudSessionInMemoryStorage(appCtx: Context = getApplication()) {
        withContext(Dispatchers.IO) {
            runCatching {
                cloudSessionDatabase.withTransaction {
                    cloudSessionDatabase.appDao().clearAllTransactions()
                    cloudSessionDatabase.appDao().clearAllDebts()
                    cloudSessionDatabase.appDao().clearAllArchivedAlgPosteSms()
                }
            }
            runCatching {
                appCtx.filesDir.listFiles()?.forEach { file ->
                    if (file.name.startsWith("session_avatar_") && file.name.endsWith(".jpg")) {
                        file.delete()
                    }
                }
            }
        }
        lastSyncedSettingsHash = 0
        lastSyncedTxHash = 0
        lastSyncedDebtHash = 0
        lastSyncedArchivedSmsHash = 0
        lastSyncedAvatarBase64 = ""
    }

    private fun encodeAvatarFileToBase64(avatarPath: String): String {
        if (avatarPath.isBlank()) return ""
        return runCatching {
            val file = File(avatarPath)
            if (!file.exists() || !file.isFile) return ""
            val rawBytes = file.readBytes()
            if (rawBytes.isEmpty()) return ""
            val finalBytes = if (rawBytes.size <= 220_000) {
                rawBytes
            } else {
                val bmp = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size) ?: return ""
                val out = java.io.ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.JPEG, 70, out)
                bmp.recycle()
                out.toByteArray()
            }
            if (finalBytes.size > 230_000) return ""
            Base64.encodeToString(finalBytes, Base64.NO_WRAP)
        }.getOrDefault("")
    }

    private fun restoreAvatarFromBase64(context: Context, uid: String, avatarBase64: String): String {
        if (avatarBase64.isBlank() || uid.isBlank()) return ""
        return runCatching {
            val decoded = Base64.decode(avatarBase64, Base64.DEFAULT)
            if (decoded.isEmpty()) return ""
            val targetFile = File(context.applicationContext.filesDir, "session_avatar_${uid}.jpg")
            FileOutputStream(targetFile).use { it.write(decoded) }
            targetFile.absolutePath
        }.getOrDefault("")
    }

    private fun resetSessionUiEphemeralState() {
        _expandedKeyIds.value = emptySet()
        _revealedKeyIds.value = emptySet()
        _expandedCcpIds.value = emptySet()
        _hiddenCcpIds.value = emptySet()
    }

    private suspend fun prepareCleanCloudSessionForUser(user: FirebaseUser) {
        val appCtx = getApplication<Application>()
        cloudSessionMutex.withLock {
            stopCloudProfileListener()
            resetSessionUiEphemeralState()
            clearCloudSessionInMemoryStorage(appCtx)
            val uiLang = SessionMetaStore.readAuthUiLanguage(appCtx)
            val uiTheme = SessionMetaStore.readAuthUiThemeMode(appCtx)
            val cleanCloudSettings = UserSettings(
                hasLocalAccount = true,
                salaryAutoCredit = false,
                language = uiLang,
                themeMode = uiTheme
            )
            SessionMetaStore.setSessionRealm(appCtx, SessionMetaStore.REALM_CLOUD, user.uid)
            SessionMetaStore.resetAuthUiPreferences(appCtx)
            _storageRealm.value = StorageRealm.CLOUD_MEMORY
            prefsManager.switchToCloudMemoryMode(cleanCloudSettings)
            _cloudUserProfile.value = null
            _cloudSyncState.value = CloudSyncUiState.Idle
            _lastCloudSyncMillis.value = 0L
            SessionMetaStore.setLastCloudSyncMillis(appCtx, 0L)
        }
    }

    fun activateLocalAccount(username: String = "") {
        val cleanUsername = username.trim()
        val appCtx = getApplication<Application>()

        stopCloudProfileListener()
        resetSessionUiEphemeralState()
        _cloudUserProfile.value = null
        _cloudSyncState.value = CloudSyncUiState.Idle
        if (runCatching { firebaseAuth?.currentUser != null }.getOrDefault(false)) {
            runCatching { firebaseAuth?.signOut() }
        }
        _googleAuthState.value = GoogleAuthUiState.SignedOut

        viewModelScope.launch {
            withTimeoutOrNull(4_000L) { activeCloudTransitionJob?.join() }
            cloudSessionMutex.withLock {
                clearCloudSessionInMemoryStorage(appCtx)
            }
        }

        SessionMetaStore.setSessionRealm(appCtx, SessionMetaStore.REALM_GUEST)
        _storageRealm.value = StorageRealm.GUEST
        prefsManager.switchToGuestMode(activateAccount = null)

        val wasAlreadyActive = settings.value.hasLocalAccount
        if (!wasAlreadyActive) {
            triggerAuthSuccessTransition(
                kind = AuthTransitionKind.SIGN_UP,
                displayName = cleanUsername
            )
        }
        val savedName = cleanUsername.ifEmpty { settings.value.localUsername }
        if (savedName.isNotBlank()) {
            persistSavedQuickAccount(
                displayName = savedName,
                email = "",
                isCloudLinked = false,
                uid = ""
            )
        }
        prefsManager.updateSettings {
            val isDefaultClassic = it.classicCardHolderName.isBlank() ||
                it.classicCardHolderName.equals("CARDHOLDER", ignoreCase = true) ||
                it.classicCardHolderName.equals("CHARFEDDINE KA", ignoreCase = true)
            val isDefaultVisa = it.visaCardHolderName.isBlank() ||
                it.visaCardHolderName.equals("CARDHOLDER", ignoreCase = true) ||
                it.visaCardHolderName.equals("CHARFEDDINE KA", ignoreCase = true)
            val isDefaultThird = it.thirdCardHolderName.isBlank() ||
                it.thirdCardHolderName.equals("CARDHOLDER", ignoreCase = true) ||
                it.thirdCardHolderName.equals("CHARFEDDINE KA", ignoreCase = true)
            it.copy(
                hasLocalAccount = true,
                localUsername = cleanUsername.ifEmpty { it.localUsername },
                classicCardHolderName = if (cleanUsername.isNotEmpty() && isDefaultClassic) cleanUsername.uppercase() else it.classicCardHolderName,
                visaCardHolderName = if (cleanUsername.isNotEmpty() && isDefaultVisa) cleanUsername.uppercase() else it.visaCardHolderName,
                thirdCardHolderName = if (cleanUsername.isNotEmpty() && isDefaultThird) cleanUsername.uppercase() else it.thirdCardHolderName
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            cleanExistingMockDataIfPresent()
            runActiveRealmMaintenanceAndSmsScan()
        }
    }

    fun signOutToAuthScreen() {
        clearAuthTransition()
        val appCtx = getApplication<Application>()
        val currentSettings = settings.value
        val wasCloudRealm = _storageRealm.value == StorageRealm.CLOUD_MEMORY
        val activeUser = runCatching { firebaseAuth?.currentUser }.getOrNull()

        if (wasCloudRealm && activeUser != null) {
            val resolvedName = listOf(currentSettings.firstName, currentSettings.lastName)
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .ifBlank { currentSettings.localUsername }
                .ifBlank { activeUser.displayName?.trim().orEmpty() }
                .ifBlank { activeUser.email?.substringBefore('@').orEmpty() }
            persistSavedQuickAccount(
                displayName = resolvedName,
                email = activeUser.email.orEmpty(),
                isCloudLinked = true,
                uid = activeUser.uid
            )
        } else {
            val localDisplayName = listOf(currentSettings.firstName, currentSettings.lastName)
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .ifBlank { currentSettings.localUsername }
            if (localDisplayName.isNotBlank()) {
                persistSavedQuickAccount(
                    displayName = localDisplayName,
                    email = "",
                    isCloudLinked = false,
                    uid = ""
                )
            }
        }

        val repo = cloudRepository
        val shouldFlushCloud = wasCloudRealm &&
            activeUser != null &&
            repo != null &&
            initialCloudHandshakeDoneForUid == activeUser.uid

        stopCloudProfileListener()
        resetSessionUiEphemeralState()
        _cloudUserProfile.value = null
        _cloudSyncState.value = CloudSyncUiState.Idle
        _isAppLocked.value = false
        _isRestoringCloudSession.value = false
        _lastCloudSyncMillis.value = 0L
        SessionMetaStore.setLastCloudSyncMillis(appCtx, 0L)
        SessionMetaStore.setSessionRealm(appCtx, SessionMetaStore.REALM_NONE)
        _storageRealm.value = StorageRealm.SIGNED_OUT
        prefsManager.switchToSignedOutMode(resetAuthUiPreferences = true)

        activeCloudTransitionJob = viewModelScope.launch {
            withContext(NonCancellable + Dispatchers.IO) {
                cloudSessionMutex.withLock {
                    val txList = if (shouldFlushCloud) {
                        runCatching { cloudSessionDatabase.appDao().getAllTransactionsList() }.getOrDefault(emptyList())
                    } else emptyList()
                    val debtList = if (shouldFlushCloud) {
                        runCatching { cloudSessionDatabase.appDao().getAllDebtsList() }.getOrDefault(emptyList())
                    } else emptyList()
                    val smsList = if (shouldFlushCloud) {
                        runCatching { cloudSessionDatabase.appDao().getAllArchivedAlgPosteSmsList() }.getOrDefault(emptyList())
                    } else emptyList()
                    val avatarB64 = if (shouldFlushCloud) {
                        encodeAvatarFileToBase64(currentSettings.profileAvatarPath)
                    } else ""

                    // Immediately clear RAM records & session avatars before any network I/O
                    clearCloudSessionInMemoryStorage(appCtx)

                    if (shouldFlushCloud && activeUser != null && repo != null) {
                        val stillSameUser = runCatching {
                            firebaseAuth?.currentUser?.uid == activeUser.uid
                        }.getOrDefault(false)
                        if (stillSameUser) {
                            withTimeoutOrNull(3_500L) {
                                runCatching {
                                    repo.pushVaultToCloud(
                                        user = activeUser,
                                        settings = currentSettings,
                                        transactions = txList,
                                        debts = debtList,
                                        archivedSms = smsList,
                                        avatarBase64 = avatarB64,
                                        syncSettings = true,
                                        syncTransactions = true,
                                        syncDebts = true,
                                        syncSecrets = true,
                                        syncArchivedSms = true
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun updateAccountProfile(
        firstName: String,
        lastName: String,
        username: String,
        pickedPhotoUri: Uri? = null,
        removeCustomPhoto: Boolean = false
    ) {
        val cleanFirst = firstName.trim().take(80)
        val cleanLast = lastName.trim().take(80)
        val cleanUser = username.trim().take(80)
        val isAr = settings.value.language == "ar"
        val currentRealm = _storageRealm.value
        val activeUser = firebaseAuth?.currentUser

        viewModelScope.launch {
            val appCtx = getApplication<Application>()
            val resolvedAvatarPath = withContext(Dispatchers.IO) {
                val avatarFileName = if (currentRealm == StorageRealm.CLOUD_MEMORY && activeUser != null) {
                    "session_avatar_${activeUser.uid}.jpg"
                } else {
                    "guest_profile_avatar.jpg"
                }
                val targetFile = File(appCtx.filesDir, avatarFileName)
                when {
                    pickedPhotoUri != null -> {
                        saveDownscaledAvatar(appCtx, pickedPhotoUri, targetFile)
                            ?: settings.value.profileAvatarPath
                    }
                    removeCustomPhoto -> {
                        runCatching { targetFile.delete() }
                        ""
                    }
                    else -> settings.value.profileAvatarPath
                }
            }

            prefsManager.updateSettings { current ->
                current.copy(
                    firstName = cleanFirst,
                    lastName = cleanLast,
                    localUsername = cleanUser.ifBlank { current.localUsername },
                    profileAvatarPath = resolvedAvatarPath
                )
            }

            val fullDisplayName = listOf(cleanFirst, cleanLast)
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .ifBlank { cleanUser }

            val isCloud = currentRealm == StorageRealm.CLOUD_MEMORY && activeUser != null
            if (fullDisplayName.isNotBlank()) {
                persistSavedQuickAccount(
                    displayName = fullDisplayName,
                    email = activeUser?.email.orEmpty(),
                    isCloudLinked = isCloud,
                    uid = if (isCloud) activeUser?.uid.orEmpty() else ""
                )
            }
            if (isCloud && activeUser != null) {
                if (fullDisplayName.isNotBlank()) {
                    runCatching {
                        activeUser.updateProfile(
                            UserProfileChangeRequest.Builder()
                                .setDisplayName(fullDisplayName)
                                .build()
                        ).await()
                    }
                    _googleAuthState.value = GoogleAuthUiState.SignedIn(
                        uid = activeUser.uid,
                        displayName = fullDisplayName,
                        email = activeUser.email.orEmpty(),
                        photoUrl = activeUser.photoUrl?.toString()
                    )
                }
                val repo = cloudRepository
                if (repo != null) {
                    val latestSettings = settings.value
                    val avatarB64 = withContext(Dispatchers.IO) { encodeAvatarFileToBase64(resolvedAvatarPath) }
                    withContext(Dispatchers.IO) {
                        repo.pushVaultToCloud(
                            user = activeUser,
                            settings = latestSettings,
                            transactions = emptyList(),
                            debts = emptyList(),
                            avatarBase64 = avatarB64,
                            syncSettings = true,
                            syncTransactions = false,
                            syncDebts = false,
                            syncSecrets = true,
                            syncArchivedSms = false
                        )
                        lastSyncedAvatarBase64 = avatarB64
                    }
                }
            }

            _toastEvent.emit(
                if (isAr) "تم تحديث الملف الشخصي ✓"
                else "Profile updated ✓"
            )
        }
    }

    private fun saveDownscaledAvatar(context: Context, uri: Uri, destFile: File): String? {
        return try {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            }
            var sampleSize = 1
            val maxDim = maxOf(boundsOpts.outWidth, boundsOpts.outHeight)
            if (maxDim > 512) {
                while (maxDim / (sampleSize * 2) >= 256) {
                    sampleSize *= 2
                }
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return null

            val scaled = if (decoded.width > 512 || decoded.height > 512) {
                val ratio = minOf(512f / decoded.width, 512f / decoded.height)
                Bitmap.createScaledBitmap(
                    decoded,
                    (decoded.width * ratio).toInt().coerceAtLeast(1),
                    (decoded.height * ratio).toInt().coerceAtLeast(1),
                    true
                ).also {
                    if (it !== decoded) decoded.recycle()
                }
            } else {
                decoded
            }

            FileOutputStream(destFile).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 88, out)
            }
            scaled.recycle()
            destFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    // ==================== Google Sign-In & Cloud Firestore Sync (Cloud-Only Realm) ====================

    private fun updateGoogleAuthFromFirebaseUser(user: FirebaseUser?) {
        if (user != null) {
            val resolvedName = user.displayName?.trim()?.takeIf { it.isNotEmpty() }
                ?: user.email?.substringBefore('@')?.takeIf { it.isNotBlank() }
                ?: settings.value.localUsername.ifBlank { "Vaultix User" }
            _googleAuthState.value = GoogleAuthUiState.SignedIn(
                uid = user.uid,
                displayName = resolvedName,
                email = user.email.orEmpty(),
                photoUrl = user.photoUrl?.toString()
            )
            if (_storageRealm.value == StorageRealm.CLOUD_MEMORY && settings.value.hasLocalAccount) {
                startCloudProfileListener(user.uid)
            } else {
                stopCloudProfileListener()
            }
        } else {
            stopCloudProfileListener()
            _cloudUserProfile.value = null
            if (_googleAuthState.value is GoogleAuthUiState.SignedIn) {
                _googleAuthState.value = GoogleAuthUiState.SignedOut
            }
            if (_cloudSyncState.value !is CloudSyncUiState.Idle) {
                _cloudSyncState.value = CloudSyncUiState.Idle
            }
        }
    }

    private fun startCloudProfileListener(uid: String) {
        if (_storageRealm.value != StorageRealm.CLOUD_MEMORY) return
        cloudProfileObserverJob?.cancel()
        cloudProfileObserverJob = viewModelScope.launch(Dispatchers.IO) {
            val repo = cloudRepository ?: return@launch
            try {
                repo.observeUserProfile(uid).collect { profile ->
                    _cloudUserProfile.value = profile
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }
        startCloudAutoSyncObserver(uid)
    }

    @OptIn(FlowPreview::class)
    private fun startCloudAutoSyncObserver(uid: String) {
        if (_storageRealm.value != StorageRealm.CLOUD_MEMORY) return
        cloudAutoSyncJob?.cancel()
        cloudAutoSyncJob = viewModelScope.launch(Dispatchers.IO) {
            val repo = cloudRepository ?: return@launch
            combine(
                settings,
                allTransactions,
                allDebts,
                archivedAlgPosteSms
            ) { currentSettings, currentTxs, currentDebts, currentArchived ->
                CloudVaultSnapshotForSync(currentSettings, currentTxs, currentDebts, currentArchived)
            }
                .debounce(1200L)
                .collect { (latestSettings, txList, debtList, archivedList) ->
                    if (_storageRealm.value != StorageRealm.CLOUD_MEMORY) return@collect
                    val currentUser = firebaseAuth?.currentUser ?: return@collect
                    if (currentUser.uid != uid) return@collect
                    if (!latestSettings.hasLocalAccount) return@collect
                    if (isApplyingCloudPull) return@collect
                    if (initialCloudHandshakeDoneForUid != uid) return@collect

                    val avatarB64 = encodeAvatarFileToBase64(latestSettings.profileAvatarPath)
                    val sHash = computeCloudSyncedSettingsFingerprint(latestSettings)
                    val tHash = txList.hashCode()
                    val dHash = debtList.hashCode()
                    val aHash = archivedList.hashCode()

                    val settingsChanged = sHash != lastSyncedSettingsHash
                    val avatarChanged = avatarB64 != lastSyncedAvatarBase64
                    val txChanged = tHash != lastSyncedTxHash
                    val debtChanged = dHash != lastSyncedDebtHash
                    val archivedChanged = aHash != lastSyncedArchivedSmsHash
                    if (!settingsChanged && !avatarChanged && !txChanged && !debtChanged && !archivedChanged) {
                        return@collect
                    }

                    _cloudSyncState.value = CloudSyncUiState.Syncing
                    val result = repo.pushVaultToCloud(
                        user = currentUser,
                        settings = latestSettings,
                        transactions = txList,
                        debts = debtList,
                        archivedSms = archivedList,
                        avatarBase64 = avatarB64,
                        syncSettings = settingsChanged,
                        syncTransactions = txChanged,
                        syncDebts = debtChanged,
                        syncSecrets = settingsChanged || avatarChanged,
                        syncArchivedSms = archivedChanged
                    )
                    result.onSuccess { summary ->
                        lastSyncedSettingsHash = sHash
                        lastSyncedTxHash = tHash
                        lastSyncedDebtHash = dHash
                        lastSyncedArchivedSmsHash = aHash
                        lastSyncedAvatarBase64 = avatarB64
                        recordLastCloudSyncMillis(summary.syncedAtMillis)
                        _cloudSyncState.value = CloudSyncUiState.Success(
                            syncedAtMillis = summary.syncedAtMillis,
                            transactionCount = summary.transactionCount,
                            debtCount = summary.debtCount
                        )
                    }.onFailure { err ->
                        val isAr = settings.value.language == "ar"
                        _cloudSyncState.value = CloudSyncUiState.Error(friendlyCloudErrorMessage(err, isAr))
                    }
                }
        }
    }

    private fun computeCloudSyncedSettingsFingerprint(s: UserSettings): Int {
        var h = s.salaryAmount.hashCode()
        h = 31 * h + s.salaryPayday
        h = 31 * h + s.salaryEmployer.hashCode()
        h = 31 * h + s.salaryAutoCredit.hashCode()
        h = 31 * h + s.lastCreditedMonthYear.hashCode()
        h = 31 * h + s.biometricEnabled.hashCode()
        h = 31 * h + s.autoLockTimeoutSeconds
        h = 31 * h + s.language.hashCode()
        h = 31 * h + s.themeMode.hashCode()
        h = 31 * h + s.designStyle.hashCode()
        h = 31 * h + s.lastBackupTimestamp.hashCode()
        h = 31 * h + s.stealthMode.hashCode()
        h = 31 * h + s.savingsWalletBalance.hashCode()
        h = 31 * h + s.savingsGoalAmount.hashCode()
        h = 31 * h + s.savingsHistoryJson.hashCode()
        h = 31 * h + s.includeAssetsInSavingsTotal.hashCode()
        h = 31 * h + s.goldAssetHoldingsJson.hashCode()
        h = 31 * h + s.classicCardBankStyle.hashCode()
        h = 31 * h + s.classicCardHolderName.hashCode()
        h = 31 * h + s.classicCardNumber.hashCode()
        h = 31 * h + s.classicCardExpiry.hashCode()
        h = 31 * h + s.classicCardCvc.hashCode()
        h = 31 * h + s.breathingGlowEnabled.hashCode()
        h = 31 * h + s.screenshotProtectionEnabled.hashCode()
        h = 31 * h + s.vaultKeysJson.hashCode()
        h = 31 * h + s.ccpAccountsJson.hashCode()
        h = 31 * h + s.visaCardProvider.hashCode()
        h = 31 * h + s.visaCardHolderName.hashCode()
        h = 31 * h + s.visaCardNumber.hashCode()
        h = 31 * h + s.visaCardExpiry.hashCode()
        h = 31 * h + s.visaCardCvc.hashCode()
        h = 31 * h + s.thirdCardEnabled.hashCode()
        h = 31 * h + s.thirdCardProvider.hashCode()
        h = 31 * h + s.thirdCardHolderName.hashCode()
        h = 31 * h + s.thirdCardNumber.hashCode()
        h = 31 * h + s.thirdCardExpiry.hashCode()
        h = 31 * h + s.thirdCardCvc.hashCode()
        h = 31 * h + s.binanceHoldingsJson.hashCode()
        h = 31 * h + s.binancePricesJson.hashCode()
        h = 31 * h + s.binanceLastPriceSyncMillis.hashCode()
        h = 31 * h + s.squareMarketSnapshotJson.hashCode()
        h = 31 * h + s.squareLastPriceSyncMillis.hashCode()
        h = 31 * h + s.squareDailyBaselineJson.hashCode()
        h = 31 * h + s.squarePinnedIdsJson.hashCode()
        h = 31 * h + s.squareSyncIntervalMinutes
        h = 31 * h + s.squareEnabledSourceIdsJson.hashCode()
        h = 31 * h + s.baridiMobSmsSyncEnabled.hashCode()
        h = 31 * h + s.pendingSmsSuggestionsJson.hashCode()
        h = 31 * h + s.processedSmsKeysJson.hashCode()
        h = 31 * h + s.smsSyncBaselineTimestampMillis.hashCode()
        h = 31 * h + s.latestSmsBalanceAmount.hashCode()
        h = 31 * h + s.latestSmsBalanceUpdatedAt.hashCode()
        h = 31 * h + s.latestSmsBalanceRawSnippet.hashCode()
        h = 31 * h + s.smsBalanceHistoryJson.hashCode()
        h = 31 * h + s.detectedSmsTaxesJson.hashCode()
        h = 31 * h + s.detectedFailedSmsTransactionsJson.hashCode()
        h = 31 * h + s.dismissedFailedSmsIdsJson.hashCode()
        h = 31 * h + s.autoAddTaxesEnabled.hashCode()
        h = 31 * h + s.autoAddTaxesBaselineMillis.hashCode()
        h = 31 * h + s.hasCompletedPermissionsOnboarding.hashCode()
        h = 31 * h + s.firstName.hashCode()
        h = 31 * h + s.lastName.hashCode()
        h = 31 * h + s.localUsername.hashCode()
        h = 31 * h + s.localWilayaCode.hashCode()
        h = 31 * h + s.localWilayaName.hashCode()
        h = 31 * h + s.localCommuneName.hashCode()
        h = 31 * h + s.cardHubOrderJson.hashCode()
        h = 31 * h + s.profileAvatarPath.hashCode()
        return h
    }

    private fun stopCloudProfileListener() {
        cloudProfileObserverJob?.cancel()
        cloudProfileObserverJob = null
        cloudAutoSyncJob?.cancel()
        cloudAutoSyncJob = null
        initialCloudHandshakeDoneForUid = null
    }

    fun clearAuthError() {
        if (_googleAuthState.value is GoogleAuthUiState.Error) {
            val currentUser = firebaseAuth?.currentUser
            if (currentUser != null) {
                updateGoogleAuthFromFirebaseUser(currentUser)
            } else {
                _googleAuthState.value = GoogleAuthUiState.SignedOut
            }
        }
    }

    fun attemptSilentGoogleSignIn(context: Context) {
        viewModelScope.launch {
            val appCtx = getApplication<Application>()
            val auth = withContext(Dispatchers.IO) {
                runCatching { firebaseAuth }.getOrNull()
            }
            val currentUser = auth?.currentUser
            if (currentUser != null) {
                updateGoogleAuthFromFirebaseUser(currentUser)
                val resolvedName = currentUser.displayName?.trim()?.takeIf { it.isNotEmpty() }
                    ?: currentUser.email?.substringBefore('@')?.trim().orEmpty()
                persistSavedQuickAccount(
                    displayName = resolvedName,
                    email = currentUser.email.orEmpty(),
                    isCloudLinked = true,
                    uid = currentUser.uid
                )
                return@launch
            }
            if (silentGoogleSignInAttempted) return@launch
            silentGoogleSignInAttempted = true
            delay(500L)
            val explicitSignedOut = withContext(Dispatchers.IO) {
                SessionMetaStore.isExplicitGoogleSignedOut(appCtx)
            }
            if (explicitSignedOut || auth == null) return@launch
            GoogleAuthHelper.attemptAutoSignIn(
                context = context,
                scope = viewModelScope,
                onAuthSuccess = { user ->
                    viewModelScope.launch {
                        _isRestoringCloudSession.value = true
                        withTimeoutOrNull(4_000L) { activeCloudTransitionJob?.join() }
                        prepareCleanCloudSessionForUser(user)
                        activateGoogleConnectedAccount(user)
                        updateGoogleAuthFromFirebaseUser(user)
                        syncAfterCloudSignIn(user, showFeedback = false)
                        _isRestoringCloudSession.value = false
                        withContext(Dispatchers.IO) {
                            runActiveRealmMaintenanceAndSmsScan()
                        }
                    }
                },
                onUnauthenticated = {
                    // Stay SignedOut silently until user taps Sign In
                }
            )
        }
    }

    fun signInWithGoogle(context: Context) {
        if (_googleAuthState.value is GoogleAuthUiState.Loading) return
        val isAr = settings.value.language == "ar"
        suppressNextAutoLockForExternalFlow()
        SessionMetaStore.setExplicitGoogleSignedOut(getApplication(), false)
        _googleAuthState.value = GoogleAuthUiState.Loading

        GoogleAuthHelper.onGoogleSignInClicked(
            context = context,
            scope = viewModelScope,
            isArabic = isAr,
            onAuthSuccess = { user ->
                val resolvedName = user.displayName?.trim()?.takeIf { it.isNotEmpty() }
                    ?: user.email?.substringBefore('@')?.trim().orEmpty()
                if (!settings.value.hasLocalAccount) {
                    triggerAuthSuccessTransition(
                        kind = AuthTransitionKind.SIGN_IN,
                        displayName = resolvedName
                    )
                }
                viewModelScope.launch {
                    _isRestoringCloudSession.value = true
                    withTimeoutOrNull(4_000L) { activeCloudTransitionJob?.join() }
                    prepareCleanCloudSessionForUser(user)
                    activateGoogleConnectedAccount(user)
                    updateGoogleAuthFromFirebaseUser(user)
                    syncAfterCloudSignIn(user, showFeedback = false)
                    _isRestoringCloudSession.value = false
                    withContext(Dispatchers.IO) {
                        runActiveRealmMaintenanceAndSmsScan()
                    }
                }
            },
            onAuthCancelled = {
                val existingUser = firebaseAuth?.currentUser
                if (existingUser != null) {
                    updateGoogleAuthFromFirebaseUser(existingUser)
                } else {
                    _googleAuthState.value = GoogleAuthUiState.SignedOut
                }
                viewModelScope.launch {
                    _toastEvent.emit(
                        if (isAr) "تم إلغاء تسجيل الدخول بحساب Google"
                        else "Google Sign-In cancelled"
                    )
                }
            },
            onAuthError = { errorMsg ->
                _googleAuthState.value = GoogleAuthUiState.Error(errorMsg)
                viewModelScope.launch {
                    _toastEvent.emit(
                        if (isAr) "فشل تسجيل الدخول عبر Google: $errorMsg"
                        else "Google Sign-In failed: $errorMsg"
                    )
                }
            }
        )
    }

    fun signInWithEmailAccount(
        context: Context,
        email: String,
        password: String
    ) {
        if (_googleAuthState.value is GoogleAuthUiState.Loading) return
        val isAr = settings.value.language == "ar"
        val cleanEmail = email.trim()
        if (cleanEmail.isEmpty() || !cleanEmail.contains("@") || password.isEmpty()) {
            _googleAuthState.value = GoogleAuthUiState.Error(
                if (isAr) "يرجى إدخال البريد الإلكتروني وكلمة السر"
                else "Please enter your email and password"
            )
            return
        }

        SessionMetaStore.setExplicitGoogleSignedOut(getApplication(), false)
        _googleAuthState.value = GoogleAuthUiState.Loading

        viewModelScope.launch {
            withTimeoutOrNull(4_000L) { activeCloudTransitionJob?.join() }
            when (val res = GoogleAuthHelper.signInWithEmailPassword(context, cleanEmail, password, isAr)) {
                is EmailAuthResult.Success -> {
                    val resolvedName = res.user.displayName?.trim()?.takeIf { it.isNotEmpty() }
                        ?: res.user.email?.substringBefore('@')?.trim().orEmpty()
                    triggerAuthSuccessTransition(
                        kind = AuthTransitionKind.SIGN_IN,
                        displayName = resolvedName
                    )
                    _isRestoringCloudSession.value = true
                    prepareCleanCloudSessionForUser(res.user)
                    activateGoogleConnectedAccount(res.user)
                    updateGoogleAuthFromFirebaseUser(res.user)
                    syncAfterCloudSignIn(res.user, showFeedback = false)
                    _isRestoringCloudSession.value = false
                    withContext(Dispatchers.IO) {
                        runActiveRealmMaintenanceAndSmsScan()
                    }
                }
                is EmailAuthResult.Error -> {
                    _googleAuthState.value = GoogleAuthUiState.Error(
                        message = res.message,
                        accountNotFound = res.accountNotFound
                    )
                }
            }
        }
    }

    fun signUpWithEmailAccount(
        context: Context,
        firstName: String,
        lastName: String,
        username: String,
        email: String,
        password: String
    ) {
        if (_googleAuthState.value is GoogleAuthUiState.Loading) return
        val isAr = settings.value.language == "ar"
        val cleanFirst = firstName.trim()
        val cleanLast = lastName.trim()
        val cleanUser = username.trim()
        val cleanEmail = email.trim()
        if (cleanFirst.isEmpty() || cleanLast.isEmpty() || cleanUser.isEmpty() || cleanEmail.isEmpty()) {
            _googleAuthState.value = GoogleAuthUiState.Error(
                if (isAr) "يرجى ملء جميع الحقول المطلوبة"
                else "Please complete all required fields"
            )
            return
        }
        if (password.length < 6) {
            _googleAuthState.value = GoogleAuthUiState.Error(
                if (isAr) "كلمة السر يجب أن تتكون من 6 أحرف على الأقل"
                else "Password must be at least 6 characters"
            )
            return
        }

        SessionMetaStore.setExplicitGoogleSignedOut(getApplication(), false)
        _googleAuthState.value = GoogleAuthUiState.Loading

        viewModelScope.launch {
            withTimeoutOrNull(4_000L) { activeCloudTransitionJob?.join() }
            when (
                val res = GoogleAuthHelper.signUpWithEmailPassword(
                    context = context,
                    firstName = cleanFirst,
                    lastName = cleanLast,
                    email = cleanEmail,
                    password = password,
                    isArabic = isAr
                )
            ) {
                is EmailAuthResult.Success -> {
                    val fullName = listOf(cleanFirst, cleanLast)
                        .filter { it.isNotBlank() }
                        .joinToString(" ")
                        .ifBlank { cleanUser }
                    triggerAuthSuccessTransition(
                        kind = AuthTransitionKind.SIGN_UP,
                        displayName = fullName
                    )
                    _isRestoringCloudSession.value = true
                    prepareCleanCloudSessionForUser(res.user)
                    activateRegisteredCloudProfile(
                        user = res.user,
                        firstName = cleanFirst,
                        lastName = cleanLast,
                        username = cleanUser
                    )
                    updateGoogleAuthFromFirebaseUser(res.user)
                    syncAfterCloudSignIn(res.user, showFeedback = false)
                    _isRestoringCloudSession.value = false
                    withContext(Dispatchers.IO) {
                        runActiveRealmMaintenanceAndSmsScan()
                    }
                }
                is EmailAuthResult.Error -> {
                    _googleAuthState.value = GoogleAuthUiState.Error(res.message)
                }
            }
        }
    }

    fun sendPasswordResetEmail(
        context: Context,
        email: String,
        onResult: (success: Boolean, message: String) -> Unit
    ) {
        val isAr = settings.value.language == "ar"
        val cleanEmail = email.trim()
        if (cleanEmail.isEmpty() || !cleanEmail.contains("@")) {
            onResult(
                false,
                if (isAr) "يرجى إدخال بريد إلكتروني صالح" else "Please enter a valid email address"
            )
            return
        }
        viewModelScope.launch {
            val result = GoogleAuthHelper.sendPasswordResetEmail(context, cleanEmail, isAr)
            result.onSuccess {
                val msg = if (isAr) {
                    "تم إرسال رابط إعادة تعيين كلمة السر إلى $cleanEmail"
                } else {
                    "Password reset link sent to $cleanEmail"
                }
                _toastEvent.emit(msg)
                onResult(true, msg)
            }.onFailure { err ->
                val msg = err.message ?: if (isAr) {
                    "تعذر إرسال رابط إعادة التعيين"
                } else {
                    "Unable to send password reset email"
                }
                onResult(false, msg)
            }
        }
    }

    private fun activateRegisteredCloudProfile(
        user: FirebaseUser,
        firstName: String,
        lastName: String,
        username: String
    ) {
        val fullName = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")
        val resolvedHolder = (fullName.ifBlank { username }).uppercase()
        persistSavedQuickAccount(
            displayName = fullName.ifBlank { username },
            email = user.email.orEmpty(),
            isCloudLinked = true,
            uid = user.uid
        )
        prefsManager.updateSettings { current ->
            val isDefaultClassic = current.classicCardHolderName.isBlank() ||
                current.classicCardHolderName.equals("CARDHOLDER", ignoreCase = true)
            val isDefaultVisa = current.visaCardHolderName.isBlank() ||
                current.visaCardHolderName.equals("CARDHOLDER", ignoreCase = true)
            val isDefaultThird = current.thirdCardHolderName.isBlank() ||
                current.thirdCardHolderName.equals("CARDHOLDER", ignoreCase = true)
            current.copy(
                hasLocalAccount = true,
                firstName = firstName,
                lastName = lastName,
                localUsername = username,
                classicCardHolderName = if (resolvedHolder.isNotEmpty() && isDefaultClassic) resolvedHolder else current.classicCardHolderName,
                visaCardHolderName = if (resolvedHolder.isNotEmpty() && isDefaultVisa) resolvedHolder else current.visaCardHolderName,
                thirdCardHolderName = if (resolvedHolder.isNotEmpty() && isDefaultThird) resolvedHolder else current.thirdCardHolderName
            )
        }
    }

    fun continueWithSignedInGoogleAccount() {
        val currentUserSnapshot = firebaseAuth?.currentUser
        if (currentUserSnapshot != null) {
            val resolvedName = currentUserSnapshot.displayName?.trim()?.takeIf { it.isNotEmpty() }
                ?: currentUserSnapshot.email?.substringBefore('@')?.trim().orEmpty()
            triggerAuthSuccessTransition(
                kind = AuthTransitionKind.SIGN_IN,
                displayName = resolvedName
            )
            viewModelScope.launch {
                _isRestoringCloudSession.value = true
                withTimeoutOrNull(4_000L) { activeCloudTransitionJob?.join() }
                val activeUser = firebaseAuth?.currentUser
                if (activeUser != null) {
                    prepareCleanCloudSessionForUser(activeUser)
                    activateGoogleConnectedAccount(activeUser)
                    updateGoogleAuthFromFirebaseUser(activeUser)
                    syncAfterCloudSignIn(activeUser, showFeedback = false)
                    _isRestoringCloudSession.value = false
                    withContext(Dispatchers.IO) {
                        runActiveRealmMaintenanceAndSmsScan()
                    }
                } else {
                    _isRestoringCloudSession.value = false
                    clearAuthTransition()
                }
            }
            return
        }

        val saved = savedAuthAccount.value ?: return
        if (!saved.isCloudLinked) {
            activateLocalAccount(saved.displayName)
        } else {
            val isAr = settings.value.language == "ar"
            viewModelScope.launch {
                _toastEvent.emit(
                    if (isAr) "يرجى تسجيل الدخول مجدداً لاستعادة حسابك السحابي"
                    else "Please sign in again to restore your Cloud account"
                )
            }
        }
    }

    private fun activateGoogleConnectedAccount(user: FirebaseUser) {
        val googleFullName = user.displayName?.trim().orEmpty()
        val googleFirst = googleFullName.substringBefore(' ').trim()
        val googleLast = googleFullName.substringAfter(' ', "").trim()
        val googleName = googleFullName.takeIf { it.isNotEmpty() }
            ?: user.email?.substringBefore('@')?.trim().orEmpty()
        persistSavedQuickAccount(
            displayName = googleName,
            email = user.email.orEmpty(),
            isCloudLinked = true,
            uid = user.uid
        )
        prefsManager.updateSettings { current ->
            val resolvedFirst = current.firstName.ifBlank { googleFirst }
            val resolvedLast = current.lastName.ifBlank { googleLast }
            val resolvedName = current.localUsername.ifBlank { googleName }
            val isDefaultClassic = current.classicCardHolderName.isBlank() ||
                current.classicCardHolderName.equals("CARDHOLDER", ignoreCase = true)
            val isDefaultVisa = current.visaCardHolderName.isBlank() ||
                current.visaCardHolderName.equals("CARDHOLDER", ignoreCase = true)
            val isDefaultThird = current.thirdCardHolderName.isBlank() ||
                current.thirdCardHolderName.equals("CARDHOLDER", ignoreCase = true)
            current.copy(
                hasLocalAccount = true,
                firstName = resolvedFirst,
                lastName = resolvedLast,
                localUsername = resolvedName,
                classicCardHolderName = if (resolvedName.isNotEmpty() && isDefaultClassic) resolvedName.uppercase() else current.classicCardHolderName,
                visaCardHolderName = if (resolvedName.isNotEmpty() && isDefaultVisa) resolvedName.uppercase() else current.visaCardHolderName,
                thirdCardHolderName = if (resolvedName.isNotEmpty() && isDefaultThird) resolvedName.uppercase() else current.thirdCardHolderName
            )
        }
    }

    fun signOutGoogleAccount(
        context: Context,
        clearQuickResume: Boolean = false
    ) {
        clearAuthTransition()
        val appCtx = getApplication<Application>()
        val isAr = settings.value.language == "ar"
        val wasCloudRealm = _storageRealm.value == StorageRealm.CLOUD_MEMORY
        val activeUser = runCatching { firebaseAuth?.currentUser }.getOrNull()
        val currentSettings = settings.value
        val shouldFlushCloud = wasCloudRealm &&
            activeUser != null &&
            cloudRepository != null &&
            initialCloudHandshakeDoneForUid == activeUser.uid

        if (clearQuickResume || wasCloudRealm) {
            clearSavedQuickAccount()
        }

        // Immediately stop all listeners & wipe active session UI state before any network call
        stopCloudProfileListener()
        resetSessionUiEphemeralState()
        _cloudUserProfile.value = null
        _cloudSyncState.value = CloudSyncUiState.Idle
        _isAppLocked.value = false
        _isRestoringCloudSession.value = false
        _lastCloudSyncMillis.value = 0L
        SessionMetaStore.setLastCloudSyncMillis(appCtx, 0L)
        SessionMetaStore.setSessionRealm(appCtx, SessionMetaStore.REALM_NONE)
        _storageRealm.value = StorageRealm.SIGNED_OUT
        prefsManager.switchToSignedOutMode(resetAuthUiPreferences = true)

        if (activeUser == null) {
            _googleAuthState.value = GoogleAuthUiState.SignedOut
            activeCloudTransitionJob = viewModelScope.launch {
                withContext(NonCancellable + Dispatchers.IO) {
                    cloudSessionMutex.withLock {
                        clearCloudSessionInMemoryStorage(appCtx)
                    }
                }
            }
            if (clearQuickResume) {
                viewModelScope.launch {
                    _toastEvent.emit(
                        if (isAr) "تمت إزالة الحساب من شاشة الدخول"
                        else "Account removed from quick login"
                    )
                }
            }
            return
        }

        SessionMetaStore.setExplicitGoogleSignedOut(appCtx, true)
        activeCloudTransitionJob = viewModelScope.launch {
            withContext(NonCancellable + Dispatchers.IO) {
                cloudSessionMutex.withLock {
                    val txList = if (shouldFlushCloud) {
                        runCatching { cloudSessionDatabase.appDao().getAllTransactionsList() }.getOrDefault(emptyList())
                    } else emptyList()
                    val debtList = if (shouldFlushCloud) {
                        runCatching { cloudSessionDatabase.appDao().getAllDebtsList() }.getOrDefault(emptyList())
                    } else emptyList()
                    val smsList = if (shouldFlushCloud) {
                        runCatching { cloudSessionDatabase.appDao().getAllArchivedAlgPosteSmsList() }.getOrDefault(emptyList())
                    } else emptyList()
                    val avatarB64 = if (shouldFlushCloud) {
                        encodeAvatarFileToBase64(currentSettings.profileAvatarPath)
                    } else ""

                    // Immediately clear RAM records & session avatars before any network I/O
                    clearCloudSessionInMemoryStorage(appCtx)

                    if (shouldFlushCloud) {
                        val repo = cloudRepository
                        val stillSameUser = runCatching {
                            firebaseAuth?.currentUser?.uid == activeUser.uid
                        }.getOrDefault(false)
                        if (repo != null && stillSameUser) {
                            withTimeoutOrNull(3_500L) {
                                runCatching {
                                    repo.pushVaultToCloud(
                                        user = activeUser,
                                        settings = currentSettings,
                                        transactions = txList,
                                        debts = debtList,
                                        archivedSms = smsList,
                                        avatarBase64 = avatarB64,
                                        syncSettings = true,
                                        syncTransactions = true,
                                        syncDebts = true,
                                        syncSecrets = true,
                                        syncArchivedSms = true
                                    )
                                }
                            }
                        }
                    }

                    GoogleAuthHelper.signOutSuspend(
                        context = context,
                        beforeFirebaseSignOut = {
                            clearCloudSessionInMemoryStorage(appCtx)
                        },
                        onSignOutComplete = {
                            _googleAuthState.value = GoogleAuthUiState.SignedOut
                            viewModelScope.launch {
                                _toastEvent.emit(
                                    if (isAr) "تم تسجيل الخروج بالكامل ومحو الجلسة من الجهاز • بياناتك محفوظة في السحابة فقط"
                                    else "Signed out & cleared session from device • Data stays safely in Cloud only"
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    private suspend fun restoreCloudSessionOnLaunch(user: FirebaseUser) {
        val repo = cloudRepository ?: return
        val isAr = settings.value.language == "ar"
        _cloudSyncState.value = CloudSyncUiState.Syncing

        val pullResult = withContext(Dispatchers.IO) {
            repo.pullVaultFromCloud()
        }
        pullResult.onSuccess { pulled ->
            if (pulled.hasExistingCloudData()) {
                replaceActiveRealmWithCloudPull(user.uid, pulled, forceApplySettings = true)
            } else {
                activateGoogleConnectedAccount(user)
            }
            initialCloudHandshakeDoneForUid = user.uid
            val now = System.currentTimeMillis()
            recordLastCloudSyncMillis(now)
            val txCount = withContext(Dispatchers.IO) { activeDao().getAllTransactionsList().size }
            val debtCount = withContext(Dispatchers.IO) { activeDao().getAllDebtsList().size }
            _cloudSyncState.value = CloudSyncUiState.Success(
                syncedAtMillis = now,
                transactionCount = txCount,
                debtCount = debtCount
            )
            startCloudProfileListener(user.uid)
        }.onFailure { err ->
            val msg = friendlyCloudErrorMessage(err, isAr)
            _cloudSyncState.value = CloudSyncUiState.Error(msg)
            startCloudProfileListener(user.uid)
            _toastEvent.emit(
                if (isAr) "تعذّر جلب البيانات السحابية — تحقّق من الاتصال: $msg"
                else "Unable to fetch Cloud data — check connection: $msg"
            )
        }
    }

    private suspend fun syncAfterCloudSignIn(
        user: FirebaseUser,
        showFeedback: Boolean
    ) {
        val repo = cloudRepository ?: return
        val isAr = settings.value.language == "ar"
        _cloudSyncState.value = CloudSyncUiState.Syncing

        val pullResult = withContext(Dispatchers.IO) {
            repo.pullVaultFromCloud()
        }
        pullResult.onFailure { err ->
            val msg = friendlyCloudErrorMessage(err, isAr)
            _cloudSyncState.value = CloudSyncUiState.Error(msg)
            if (showFeedback) {
                _toastEvent.emit(
                    if (isAr) "تعذّر الاتصال بالسحابة لجلب بيانات الحساب: $msg"
                    else "Failed to fetch Cloud account data: $msg"
                )
            }
            return
        }

        val pulled = pullResult.getOrNull() ?: return
        if (pulled.hasExistingCloudData()) {
            val (restoredTxCount, restoredDebtCount) = replaceActiveRealmWithCloudPull(
                uid = user.uid,
                pulled = pulled,
                forceApplySettings = true
            )
            val resolvedDisplayName = listOf(settings.value.firstName, settings.value.lastName)
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .ifBlank { settings.value.localUsername }
                .ifBlank { user.displayName?.trim().orEmpty() }
            if (resolvedDisplayName.isNotBlank()) {
                persistSavedQuickAccount(
                    displayName = resolvedDisplayName,
                    email = user.email.orEmpty(),
                    isCloudLinked = true,
                    uid = user.uid
                )
            }
            initialCloudHandshakeDoneForUid = user.uid
            val now = System.currentTimeMillis()
            recordLastCloudSyncMillis(now)
            _cloudSyncState.value = CloudSyncUiState.Success(
                syncedAtMillis = now,
                transactionCount = restoredTxCount,
                debtCount = restoredDebtCount
            )
            if (showFeedback) {
                _toastEvent.emit(
                    if (isAr) "تمت استعادة الحساب السحابي بالكامل ($restoredTxCount معاملة) ✓"
                    else "Cloud account restored ($restoredTxCount transactions) ✓"
                )
            }
            return
        }

        // Brand-new Cloud account with no remote vault yet -> push initial clean Cloud state ONLY
        val latestSettings = settings.value
        val txList = emptyList<TransactionEntity>()
        val debtList = emptyList<DebtEntity>()
        val smsList = emptyList<ArchivedAlgPosteSmsEntity>()
        clearCloudSessionInMemoryStorage(getApplication())
        val avatarB64 = withContext(Dispatchers.IO) { encodeAvatarFileToBase64(latestSettings.profileAvatarPath) }
        val pushResult = withContext(Dispatchers.IO) {
            repo.pushVaultToCloud(
                user = user,
                settings = latestSettings,
                transactions = txList,
                debts = debtList,
                archivedSms = smsList,
                avatarBase64 = avatarB64,
                syncSettings = true,
                syncTransactions = false,
                syncDebts = false,
                syncSecrets = true,
                syncArchivedSms = false
            )
        }
        pushResult.onSuccess { summary ->
            lastSyncedSettingsHash = computeCloudSyncedSettingsFingerprint(latestSettings)
            lastSyncedTxHash = txList.hashCode()
            lastSyncedDebtHash = debtList.hashCode()
            lastSyncedArchivedSmsHash = smsList.hashCode()
            lastSyncedAvatarBase64 = avatarB64
            initialCloudHandshakeDoneForUid = user.uid
            recordLastCloudSyncMillis(summary.syncedAtMillis)
            _cloudSyncState.value = CloudSyncUiState.Success(
                syncedAtMillis = summary.syncedAtMillis,
                transactionCount = summary.transactionCount,
                debtCount = summary.debtCount
            )
            if (showFeedback) {
                _toastEvent.emit(
                    if (isAr) "تم إنشاء ومزامنة الحساب السحابي الجديد ✓"
                    else "New Cloud account initialized & synced ✓"
                )
            }
        }.onFailure { err ->
            val msg = friendlyCloudErrorMessage(err, isAr)
            _cloudSyncState.value = CloudSyncUiState.Error(msg)
            if (showFeedback) {
                _toastEvent.emit(
                    if (isAr) "تعذرت المزامنة السحابية: $msg"
                    else "Cloud sync failed: $msg"
                )
            }
        }
    }

    fun syncNowWithCloud(showFeedback: Boolean = true) {
        val user = firebaseAuth?.currentUser
        val repo = cloudRepository
        val isAr = settings.value.language == "ar"
        if (user == null || repo == null || _storageRealm.value != StorageRealm.CLOUD_MEMORY) {
            val msg = if (isAr) "سجّل الدخول بحساب سحابي أولاً للمزامنة" else "Sign in with a Cloud account first to sync"
            _cloudSyncState.value = CloudSyncUiState.Error(msg)
            if (showFeedback) {
                viewModelScope.launch { _toastEvent.emit(msg) }
            }
            return
        }
        if (_cloudSyncState.value is CloudSyncUiState.Syncing) return

        viewModelScope.launch {
            if (initialCloudHandshakeDoneForUid != user.uid) {
                syncAfterCloudSignIn(user, showFeedback = showFeedback)
                return@launch
            }
            _cloudSyncState.value = CloudSyncUiState.Syncing
            val (latestSettings, txList, debtList, smsList) = withContext(Dispatchers.IO) {
                CloudVaultSnapshotForSync(
                    settings = settings.value,
                    transactions = activeDao().getAllTransactionsList(),
                    debts = activeDao().getAllDebtsList(),
                    archivedSms = activeDao().getAllArchivedAlgPosteSmsList()
                )
            }
            val avatarB64 = withContext(Dispatchers.IO) { encodeAvatarFileToBase64(latestSettings.profileAvatarPath) }
            val result = withContext(Dispatchers.IO) {
                repo.pushVaultToCloud(
                    user = user,
                    settings = latestSettings,
                    transactions = txList,
                    debts = debtList,
                    archivedSms = smsList,
                    avatarBase64 = avatarB64,
                    syncSettings = true,
                    syncTransactions = true,
                    syncDebts = true,
                    syncSecrets = true,
                    syncArchivedSms = true
                )
            }
            result.onSuccess { summary ->
                lastSyncedSettingsHash = computeCloudSyncedSettingsFingerprint(latestSettings)
                lastSyncedTxHash = txList.hashCode()
                lastSyncedDebtHash = debtList.hashCode()
                lastSyncedArchivedSmsHash = smsList.hashCode()
                lastSyncedAvatarBase64 = avatarB64
                recordLastCloudSyncMillis(summary.syncedAtMillis)
                _cloudSyncState.value = CloudSyncUiState.Success(
                    syncedAtMillis = summary.syncedAtMillis,
                    transactionCount = summary.transactionCount,
                    debtCount = summary.debtCount
                )
                if (showFeedback) {
                    _toastEvent.emit(
                        if (isAr) "تمت المزامنة السحابية الكاملة (${summary.transactionCount} معاملة • ${summary.debtCount} ديون) ☁️"
                        else "Full Cloud sync complete (${summary.transactionCount} tx • ${summary.debtCount} debts) ☁️"
                    )
                }
            }.onFailure { err ->
                val msg = friendlyCloudErrorMessage(err, isAr)
                _cloudSyncState.value = CloudSyncUiState.Error(msg)
                if (showFeedback) {
                    _toastEvent.emit(
                        if (isAr) "تعذرت المزامنة السحابية: $msg"
                        else "Cloud sync failed: $msg"
                    )
                }
            }
        }
    }

    fun restoreFromCloud(showFeedback: Boolean = true) {
        val user = firebaseAuth?.currentUser
        val repo = cloudRepository
        val isAr = settings.value.language == "ar"
        if (user == null || repo == null || _storageRealm.value != StorageRealm.CLOUD_MEMORY) {
            val msg = if (isAr) "سجّل الدخول بحساب سحابي أولاً للاستعادة" else "Sign in with a Cloud account first to restore"
            _cloudSyncState.value = CloudSyncUiState.Error(msg)
            if (showFeedback) {
                viewModelScope.launch { _toastEvent.emit(msg) }
            }
            return
        }
        if (_cloudSyncState.value is CloudSyncUiState.Syncing) return

        viewModelScope.launch {
            _cloudSyncState.value = CloudSyncUiState.Syncing
            val pullResult = withContext(Dispatchers.IO) {
                repo.pullVaultFromCloud()
            }
            pullResult.onSuccess { pulled ->
                val (totalTx, totalDebts) = replaceActiveRealmWithCloudPull(
                    uid = user.uid,
                    pulled = pulled,
                    forceApplySettings = true
                )
                initialCloudHandshakeDoneForUid = user.uid
                val now = System.currentTimeMillis()
                recordLastCloudSyncMillis(now)
                _cloudSyncState.value = CloudSyncUiState.Success(
                    syncedAtMillis = now,
                    transactionCount = totalTx,
                    debtCount = totalDebts
                )
                if (showFeedback) {
                    _toastEvent.emit(
                        if (isAr) "تمت الاستعادة من السحابة ($totalTx معاملة • $totalDebts ديون) ✓"
                        else "Restored from Cloud ($totalTx tx • $totalDebts debts) ✓"
                    )
                }
            }.onFailure { err ->
                val msg = friendlyCloudErrorMessage(err, isAr)
                _cloudSyncState.value = CloudSyncUiState.Error(msg)
                if (showFeedback) {
                    _toastEvent.emit(
                        if (isAr) "تعذرت الاستعادة من السحابة: $msg"
                        else "Cloud restore failed: $msg"
                    )
                }
            }
        }
    }

    suspend fun uploadEncryptedBackupToCloud(passphrase: String): Result<Unit> {
        val isAr = settings.value.language == "ar"
        val repo = cloudRepository ?: return Result.failure(
            IllegalStateException(if (isAr) "الخدمة السحابية غير مهيأة" else "Cloud repository unavailable")
        )
        if (passphrase.length < 6) {
            return Result.failure(
                IllegalArgumentException(
                    if (isAr) "كلمة المرور يجب أن تكون 6 أحرف على الأقل"
                    else "Passphrase must be at least 6 characters"
                )
            )
        }
        val encryptedEnvelope = exportBackupJson(passphrase)
        return withContext(Dispatchers.IO) {
            repo.pushEncryptedBackupEnvelope(encryptedEnvelope)
        }.onSuccess {
            val now = System.currentTimeMillis()
            recordLastCloudSyncMillis(now)
        }
    }

    suspend fun restoreEncryptedBackupFromCloud(passphrase: String): Result<BackupRestoreOutcome> {
        val isAr = settings.value.language == "ar"
        val repo = cloudRepository ?: return Result.failure(
            IllegalStateException(if (isAr) "الخدمة السحابية غير مهيأة" else "Cloud repository unavailable")
        )
        val pullResult = withContext(Dispatchers.IO) {
            repo.pullEncryptedBackupEnvelope()
        }
        return pullResult.mapCatching { envelope ->
            if (envelope.isNullOrBlank()) {
                throw IllegalStateException(
                    if (isAr) "لا توجد نسخة سحابية مشفرة محفوظة بعد"
                    else "No encrypted cloud backup found yet"
                )
            }
            restoreFromBackupWithOutcome(envelope, passphrase)
        }
    }

    private suspend fun replaceActiveRealmWithCloudPull(
        uid: String,
        pulled: CloudPulledVault,
        forceApplySettings: Boolean = true
    ): Pair<Int, Int> {
        var restoredTxCount = 0
        var restoredDebtCount = 0
        isApplyingCloudPull = true
        try {
            val appCtx = getApplication<Application>()
            val db = activeDatabase()
            val daoRef = activeDao()

            val seenTxIds = HashSet<Long>()
            val txEntities = mutableListOf<TransactionEntity>()
            for (cloudTx in pulled.transactions) {
                val txType = runCatching { TransactionType.valueOf(cloudTx.type) }.getOrNull() ?: continue
                if (!cloudTx.amount.isFinite() || cloudTx.amount < 0.0) continue
                val parsedId = cloudTx.id.removePrefix("tx_").toLongOrNull()?.takeIf { it > 0L && seenTxIds.add(it) } ?: 0L
                txEntities.add(
                    TransactionEntity(
                        id = parsedId,
                        type = txType,
                        amount = cloudTx.amount,
                        category = cloudTx.category.ifBlank { "other" },
                        title = cloudTx.title.ifBlank { cloudTx.category.ifBlank { "Transaction" } },
                        note = cloudTx.note,
                        dateMillis = cloudTx.dateMillis.coerceAtLeast(0L),
                        isSalaryCredit = cloudTx.salaryCredit,
                        isPaidFromSavings = cloudTx.paidFromSavings
                    )
                )
            }

            val seenDebtIds = HashSet<Long>()
            val debtEntities = mutableListOf<DebtEntity>()
            for (cloudDebt in pulled.debts) {
                val debtType = runCatching { DebtType.valueOf(cloudDebt.type) }.getOrNull() ?: continue
                if (!cloudDebt.amount.isFinite() || cloudDebt.amount < 0.0) continue
                val parsedId = cloudDebt.id.removePrefix("debt_").toLongOrNull()?.takeIf { it > 0L && seenDebtIds.add(it) } ?: 0L
                debtEntities.add(
                    DebtEntity(
                        id = parsedId,
                        type = debtType,
                        personName = cloudDebt.personName.trim().ifBlank { "Contact" },
                        amount = cloudDebt.amount,
                        note = cloudDebt.note,
                        dueDateMillis = cloudDebt.dueDateMillis.coerceAtLeast(0L),
                        autoPayOnDueDate = cloudDebt.autoPayOnDueDate,
                        isPaid = cloudDebt.paid,
                        paidDateMillis = cloudDebt.paidDateMillis,
                        createdDateMillis = cloudDebt.createdDateMillis.coerceAtLeast(0L),
                        isRecurringMonthlyInstallment = cloudDebt.recurringMonthlyInstallment,
                        monthlyInstallmentAmount = cloudDebt.monthlyInstallmentAmount.coerceAtLeast(0.0),
                        installmentDayOfMonth = cloudDebt.installmentDayOfMonth.coerceIn(1, 31),
                        remainingAmount = cloudDebt.remainingAmount.coerceAtLeast(0.0),
                        lastInstallmentCreditedMonth = cloudDebt.lastInstallmentCreditedMonth
                    )
                )
            }

            withContext(Dispatchers.IO) {
                db.withTransaction {
                    daoRef.clearAllTransactions()
                    daoRef.clearAllDebts()
                    daoRef.clearAllArchivedAlgPosteSms()
                    if (txEntities.isNotEmpty()) {
                        daoRef.insertTransactions(txEntities)
                    }
                    if (debtEntities.isNotEmpty()) {
                        daoRef.insertDebts(debtEntities)
                    }
                    if (pulled.archivedSms.isNotEmpty()) {
                        daoRef.insertArchivedSmsIfMissing(pulled.archivedSms)
                    }
                }
            }

            val restoredAvatarPath = withContext(Dispatchers.IO) {
                val b64 = pulled.secrets?.avatarBase64.orEmpty()
                if (b64.isNotBlank()) {
                    restoreAvatarFromBase64(appCtx, uid, b64)
                } else {
                    ""
                }
            }

            val effectiveCloudSettings = pulled.settings ?: CloudVaultSettings(
                salaryAutoCredit = false,
                hasCompletedPermissionsOnboarding = false
            )
            prefsManager.updateSettings { current ->
                val merged = applyCloudVaultSettingsSafely(
                    current = current,
                    cloud = effectiveCloudSettings,
                    overwritePreferences = forceApplySettings,
                    cloudAssets = pulled.assets,
                    cloudSmsSync = pulled.smsSync,
                    cloudSecrets = pulled.secrets
                )
                val pFirst = pulled.profile?.firstName?.trim().orEmpty()
                val pLast = pulled.profile?.lastName?.trim().orEmpty()
                val pUser = pulled.profile?.localUsername?.trim().orEmpty()
                merged.copy(
                    firstName = merged.firstName.ifBlank { pFirst },
                    lastName = merged.lastName.ifBlank { pLast },
                    localUsername = merged.localUsername.ifBlank { pUser },
                    profileAvatarPath = restoredAvatarPath
                )
            }

            val (latestTx, latestDebts, latestSms) = withContext(Dispatchers.IO) {
                Triple(
                    daoRef.getAllTransactionsList(),
                    daoRef.getAllDebtsList(),
                    daoRef.getAllArchivedAlgPosteSmsList()
                )
            }
            restoredTxCount = latestTx.size
            restoredDebtCount = latestDebts.size
            lastSyncedSettingsHash = computeCloudSyncedSettingsFingerprint(settings.value)
            lastSyncedTxHash = latestTx.hashCode()
            lastSyncedDebtHash = latestDebts.hashCode()
            lastSyncedArchivedSmsHash = latestSms.hashCode()
            lastSyncedAvatarBase64 = pulled.secrets?.avatarBase64.orEmpty()
        } finally {
            isApplyingCloudPull = false
        }
        return restoredTxCount to restoredDebtCount
    }

    private fun recordLastCloudSyncMillis(timestamp: Long) {
        _lastCloudSyncMillis.value = timestamp
        SessionMetaStore.setLastCloudSyncMillis(getApplication(), timestamp)
    }

    private fun friendlyCloudErrorMessage(err: Throwable, isAr: Boolean): String {
        val raw = err.localizedMessage?.takeIf { it.isNotBlank() } ?: ""
        return when {
            raw.contains("PERMISSION_DENIED", ignoreCase = true) ->
                if (isAr) "تم رفض الصلاحية في قواعد Firestore" else "Permission denied by Firestore rules"
            raw.contains("UNAVAILABLE", ignoreCase = true) || raw.contains("network", ignoreCase = true) ->
                if (isAr) "لا يوجد اتصال بالسحابة حالياً • البيانات محفوظة محلياً" else "Offline — local vault remains up to date"
            raw.isNotBlank() -> raw
            else -> if (isAr) "خطأ غير متوقع في المزامنة" else "Unexpected cloud sync error"
        }
    }

    override fun onCleared() {
        stopCloudProfileListener()
        runCatching {
            firebaseAuth?.removeAuthStateListener(firebaseAuthStateListener)
        }
        super.onCleared()
    }

    // BaridiMob / ALG Poste SMS & Alert Sync Actions
    fun setBaridiMobSmsSyncEnabled(enabled: Boolean) {
        prefsManager.updateSettings { it.copy(baridiMobSmsSyncEnabled = enabled) }
        if (enabled && AlgPosteSmsInboxScanner.hasSmsPermissions(getApplication())) {
            scanAlgPosteSmsInbox(getApplication(), showFeedback = false)
        }
    }

    /**
     * Skip all historical ALG Poste SMS and start counting from zero.
     * Every future SMS arriving after this timestamp will immediately be suggested
     * in Expense (-) or Income (+), extract balance, and check tax variance.
     */
    fun skipOldSmsAndStartFresh() {
        val now = System.currentTimeMillis()
        val hasPerm = AlgPosteSmsInboxScanner.hasSmsPermissions(getApplication())
        prefsManager.updateSettings { current ->
            current.copy(
                smsSyncBaselineTimestampMillis = now,
                baridiMobSmsSyncEnabled = if (hasPerm) true else current.baridiMobSmsSyncEnabled,
                pendingSmsSuggestionsJson = "[]",
                processedSmsKeysJson = "[]"
            )
        }
        viewModelScope.launch {
            val isAr = settings.value.language == "ar"
            _toastEvent.emit(
                if (isAr) "تم تخطي الاقتراحات القديمة والبدء من الصفر — الأرصدة محفوظة وأي رسالة جديدة ستُقترح فوراً"
                else "Skipped old SMS suggestions & started fresh — balances kept and new alerts suggest immediately"
            )
        }
    }

    fun resetSmsBaselineAndRescan(context: Context = getApplication()) {
        prefsManager.updateSettings {
            it.copy(
                smsSyncBaselineTimestampMillis = 0L,
                dismissedFailedSmsIdsJson = "[]"
            )
        }
        scanAlgPosteSmsInbox(context, showFeedback = true)
    }

    fun refreshCurrentBalanceOnlyFromInbox(
        context: Context = getApplication(),
        showFeedback: Boolean = true
    ) {
        viewModelScope.launch {
            val isAr = settings.value.language == "ar"
            if (!AlgPosteSmsInboxScanner.hasSmsPermissions(context)) {
                if (showFeedback) {
                    _toastEvent.emit(
                        if (isAr) "يرجى منح صلاحية قراءة الرسائل النصية أولاً"
                        else "Please grant SMS read permission first"
                    )
                }
                return@launch
            }
            val (snapshots, archivedSnapshots) = withContext(Dispatchers.IO) {
                val rawList = AlgPosteSmsInboxScanner.scanAllRawAlgPosteSms(context)
                val toArchive = rawList.mapNotNull { raw ->
                    AlgPosteSmsAnalyzer.toArchivedEntity(
                        sender = raw.sender,
                        body = raw.body,
                        timestampMillis = raw.dateMillis,
                        captureSource = "INBOX",
                        allowAnySenderForManualTest = false
                    )
                }
                if (toArchive.isNotEmpty()) {
                    activeDao().insertArchivedSmsIfMissing(toArchive)
                }
                val liveSnaps = AlgPosteSmsInboxScanner.scanAllBalanceSnapshots(context)
                val permanentSnaps = AlgPosteSmsAnalyzer.reconstructSnapshotsFromArchive(
                    activeDao().getAllArchivedAlgPosteSmsList()
                )
                liveSnaps to permanentSnaps
            }
            if (snapshots.isEmpty() && archivedSnapshots.isEmpty()) {
                if (showFeedback) {
                    _toastEvent.emit(
                        if (isAr) "لم يتم العثور على أرصدة في رسائل بريد الجزائر أو بريدي موب"
                        else "No balances found in ALG Poste / BaridiMob SMS inbox"
                    )
                }
                return@launch
            }

            var totalHistoryCount = 0
            var latestBalance: Double? = null
            withContext(Dispatchers.IO) {
                prefsManager.updateSettings { current ->
                    val existingHistory = AlgPosteSmsCodec.parseBalanceHistory(current.smsBalanceHistoryJson)
                    val mergedHistory = (existingHistory + archivedSnapshots + snapshots)
                        .distinctBy { it.id }
                        .sortedBy { it.timestampMillis }
                    val latestSnap = mergedHistory.maxByOrNull { it.timestampMillis }
                    totalHistoryCount = mergedHistory.size
                    latestBalance = latestSnap?.balance ?: current.latestSmsBalanceAmount.takeIf {
                        current.latestSmsBalanceUpdatedAt > 0L || it >= 0.0
                    }

                    val existingFailed = AlgPosteSmsCodec.parseFailedTransactions(current.detectedFailedSmsTransactionsJson)
                    val dismissedFailedIds = AlgPosteSmsCodec.parseProcessedKeys(current.dismissedFailedSmsIdsJson)
                    val failedAnalysis = AlgPosteSmsAnalyzer.analyzeFailedTransactions(
                        snapshots = mergedHistory,
                        existingEntries = existingFailed,
                        dismissedIds = dismissedFailedIds
                    )
                    val excludedIds = failedAnalysis.excludedSuggestionSmsIds

                    val existingTaxes = AlgPosteSmsCodec.parseTaxAnomalies(current.detectedSmsTaxesJson)
                    val updatedTaxes = AlgPosteSmsAnalyzer.detectTaxAnomalies(
                        snapshots = mergedHistory,
                        existingAnomalies = existingTaxes,
                        failedAnalysis = failedAnalysis
                    )

                    val existingPending = AlgPosteSmsCodec.parseSuggestions(current.pendingSmsSuggestionsJson)
                    val cleanedExistingPending = existingPending.filterNot {
                        it.id.removePrefix("bal_op_") in excludedIds
                    }
                    val updatedPendingJson = if (current.baridiMobSmsSyncEnabled) {
                        val processed = AlgPosteSmsCodec.parseProcessedKeys(current.processedSmsKeysJson)
                        val existingBaseIds = cleanedExistingPending.map { it.id.removePrefix("bal_op_") }.toSet()
                        val deltaOps = AlgPosteSmsAnalyzer.detectBalanceOperationSuggestions(
                            snapshots = mergedHistory,
                            minTimestampMillis = current.smsSyncBaselineTimestampMillis,
                            excludedSmsIds = excludedIds
                        ).filter { op ->
                            val baseKey = op.id.removePrefix("bal_op_")
                            op.id !in processed &&
                                baseKey !in processed &&
                                "bal_op_$baseKey" !in processed &&
                                baseKey !in existingBaseIds
                        }
                        AlgPosteSmsCodec.encodeSuggestions(
                            (deltaOps + cleanedExistingPending).distinctBy { it.id.removePrefix("bal_op_") }
                        )
                    } else {
                        AlgPosteSmsCodec.encodeSuggestions(cleanedExistingPending)
                    }

                    current.copy(
                        pendingSmsSuggestionsJson = updatedPendingJson,
                        latestSmsBalanceAmount = latestSnap?.balance ?: current.latestSmsBalanceAmount,
                        latestSmsBalanceUpdatedAt = latestSnap?.timestampMillis ?: current.latestSmsBalanceUpdatedAt,
                        latestSmsBalanceRawSnippet = latestSnap?.rawBody ?: current.latestSmsBalanceRawSnippet,
                        smsBalanceHistoryJson = AlgPosteSmsCodec.encodeBalanceHistory(mergedHistory),
                        detectedSmsTaxesJson = AlgPosteSmsCodec.encodeTaxAnomalies(updatedTaxes),
                        detectedFailedSmsTransactionsJson = AlgPosteSmsCodec.encodeFailedTransactions(failedAnalysis.failedTransactions)
                    )
                }
                TaxExpenseAutoLogger.logFutureTaxesIfEnabled(
                    dao = activeDao(),
                    prefsManager = prefsManager,
                    isArabic = isAr
                )
            }
            val extractedBal = latestBalance
            if (showFeedback && extractedBal != null) {
                val formatted = formatDzdAmount(extractedBal, if (isAr) "دج" else "DA")
                _toastEvent.emit(
                    if (isAr) "تم استخراج $totalHistoryCount أرصدة • الحالي: $formatted"
                    else "Extracted $totalHistoryCount balances • Latest: $formatted"
                )
            }
        }
    }

    fun clearSmsBalanceHistory() {
        // Clears only the display timeline; the permanent ALG Poste archive in Room is NEVER deleted.
        prefsManager.updateSettings { current ->
            current.copy(
                smsBalanceHistoryJson = "[]",
                latestSmsBalanceAmount = -1.0,
                latestSmsBalanceUpdatedAt = 0L,
                latestSmsBalanceRawSnippet = ""
            )
        }
    }

    fun reanalyzePermanentSmsArchive(showFeedback: Boolean = true) {
        viewModelScope.launch {
            val isAr = settings.value.language == "ar"
            val reanalyzed = withContext(Dispatchers.IO) {
                val existing = activeDao().getAllArchivedAlgPosteSmsList()
                val updated = existing.map { AlgPosteSmsAnalyzer.reanalyzeArchivedEntity(it) }
                if (updated.isNotEmpty()) {
                    activeDao().updateArchivedSmsAnalysis(updated)
                }
                updated
            }
            if (reanalyzed.isEmpty()) {
                if (showFeedback) {
                    _toastEvent.emit(
                        if (isAr) "السجل الدائم لرسائل بريد الجزائر فارغ حالياً"
                        else "Permanent ALG Poste SMS archive is currently empty"
                    )
                }
                return@launch
            }

            val (totalTaxes, totalFailed) = withContext(Dispatchers.IO) {
                val reconstructedSnaps = AlgPosteSmsAnalyzer.reconstructSnapshotsFromArchive(reanalyzed)
                var taxesCount = 0
                var failedCount = 0
                prefsManager.updateSettings { current ->
                    val existingHistory = AlgPosteSmsCodec.parseBalanceHistory(current.smsBalanceHistoryJson)
                    val mergedHistory = (existingHistory + reconstructedSnaps)
                        .distinctBy { it.id }
                        .sortedBy { it.timestampMillis }
                    val latestSnap = mergedHistory.maxByOrNull { it.timestampMillis }

                    val existingFailed = AlgPosteSmsCodec.parseFailedTransactions(current.detectedFailedSmsTransactionsJson)
                    val dismissedFailedIds = AlgPosteSmsCodec.parseProcessedKeys(current.dismissedFailedSmsIdsJson)
                    val failedAnalysis = AlgPosteSmsAnalyzer.analyzeFailedTransactions(
                        snapshots = mergedHistory,
                        existingEntries = existingFailed,
                        dismissedIds = dismissedFailedIds
                    )
                    failedCount = failedAnalysis.failedTransactions.size

                    val existingTaxes = AlgPosteSmsCodec.parseTaxAnomalies(current.detectedSmsTaxesJson)
                    val updatedTaxes = AlgPosteSmsAnalyzer.detectTaxAnomalies(
                        snapshots = mergedHistory,
                        existingAnomalies = existingTaxes,
                        failedAnalysis = failedAnalysis
                    )
                    taxesCount = updatedTaxes.size

                    val cleanedPending = AlgPosteSmsCodec.parseSuggestions(current.pendingSmsSuggestionsJson)
                        .filterNot { it.id.removePrefix("bal_op_") in failedAnalysis.excludedSuggestionSmsIds }

                    current.copy(
                        pendingSmsSuggestionsJson = AlgPosteSmsCodec.encodeSuggestions(cleanedPending),
                        smsBalanceHistoryJson = AlgPosteSmsCodec.encodeBalanceHistory(mergedHistory),
                        detectedSmsTaxesJson = AlgPosteSmsCodec.encodeTaxAnomalies(updatedTaxes),
                        detectedFailedSmsTransactionsJson = AlgPosteSmsCodec.encodeFailedTransactions(failedAnalysis.failedTransactions),
                        latestSmsBalanceAmount = latestSnap?.balance ?: current.latestSmsBalanceAmount,
                        latestSmsBalanceUpdatedAt = latestSnap?.timestampMillis ?: current.latestSmsBalanceUpdatedAt,
                        latestSmsBalanceRawSnippet = latestSnap?.rawBody ?: current.latestSmsBalanceRawSnippet
                    )
                }
                TaxExpenseAutoLogger.logFutureTaxesIfEnabled(
                    dao = activeDao(),
                    prefsManager = prefsManager,
                    isArabic = isAr
                )
                taxesCount to failedCount
            }

            if (showFeedback) {
                _toastEvent.emit(
                    if (isAr) "تمت إعادة دراسة ${reanalyzed.size} رسالة دائمة • $totalTaxes ضرائب • $totalFailed تعاملات فاشلة"
                    else "Re-studied ${reanalyzed.size} permanent SMS • $totalTaxes taxes • $totalFailed failed ops"
                )
            }
        }
    }

    fun scanAlgPosteSmsInbox(context: Context = getApplication(), showFeedback: Boolean = true) {
        viewModelScope.launch {
            val realmAtStart = _storageRealm.value
            val uidAtStart = runCatching { firebaseAuth?.currentUser?.uid }.getOrNull()
            if (realmAtStart == StorageRealm.SIGNED_OUT) return@launch
            val isAr = settings.value.language == "ar"
            if (!AlgPosteSmsInboxScanner.hasSmsPermissions(context)) {
                if (showFeedback) {
                    _toastEvent.emit(
                        if (isAr) "يرجى منح صلاحية قراءة الرسائل النصية أولاً"
                        else "Please grant SMS read permission first"
                    )
                }
                return@launch
            }

            val baselineMillis = settings.value.smsSyncBaselineTimestampMillis
            val (scanResult, archivedSnapshots) = withContext(Dispatchers.IO) {
                val res = AlgPosteSmsInboxScanner.scanInboxWithBalances(
                    context = context,
                    minTimestampMillis = baselineMillis,
                    applyMinTimestampToSnapshots = false
                )
                if (_storageRealm.value != realmAtStart ||
                    (realmAtStart == StorageRealm.CLOUD_MEMORY && firebaseAuth?.currentUser?.uid != uidAtStart)
                ) {
                    return@withContext res to emptyList()
                }
                val toArchive = res.rawMessages.mapNotNull { raw ->
                    AlgPosteSmsAnalyzer.toArchivedEntity(
                        sender = raw.sender,
                        body = raw.body,
                        timestampMillis = raw.dateMillis,
                        captureSource = "INBOX",
                        allowAnySenderForManualTest = false
                    )
                }
                if (toArchive.isNotEmpty()) {
                    activeDao().insertArchivedSmsIfMissing(toArchive)
                }
                val permanentSnaps = AlgPosteSmsAnalyzer.reconstructSnapshotsFromArchive(
                    activeDao().getAllArchivedAlgPosteSmsList()
                )
                res to permanentSnaps
            }
            if (_storageRealm.value != realmAtStart ||
                (realmAtStart == StorageRealm.CLOUD_MEMORY && firebaseAuth?.currentUser?.uid != uidAtStart)
            ) {
                return@launch
            }

            var addedCount = 0
            var newTaxCount = 0
            var newFailedCount = 0
            var newSnapshotCount = 0
            var autoLoggedTaxCount = 0
            withContext(Dispatchers.IO) {
                prefsManager.updateSettings { current ->
                    if (_storageRealm.value != realmAtStart) return@updateSettings current
                    val processed = AlgPosteSmsCodec.parseProcessedKeys(current.processedSmsKeysJson)
                    val existing = AlgPosteSmsCodec.parseSuggestions(current.pendingSmsSuggestionsJson)

                    val existingHistory = AlgPosteSmsCodec.parseBalanceHistory(current.smsBalanceHistoryJson)
                    val mergedHistory = (existingHistory + archivedSnapshots + scanResult.snapshots)
                        .distinctBy { it.id }
                        .sortedBy { it.timestampMillis }
                    newSnapshotCount = (mergedHistory.size - existingHistory.size).coerceAtLeast(0)
                    val latestSnap = mergedHistory.maxByOrNull { it.timestampMillis }

                    val existingFailed = AlgPosteSmsCodec.parseFailedTransactions(current.detectedFailedSmsTransactionsJson)
                    val dismissedFailedIds = AlgPosteSmsCodec.parseProcessedKeys(current.dismissedFailedSmsIdsJson)
                    val failedAnalysis = AlgPosteSmsAnalyzer.analyzeFailedTransactions(
                        snapshots = mergedHistory,
                        existingEntries = existingFailed,
                        dismissedIds = dismissedFailedIds
                    )
                    newFailedCount = (failedAnalysis.failedTransactions.size - existingFailed.size).coerceAtLeast(0)
                    val excludedIds = failedAnalysis.excludedSuggestionSmsIds
                    val cleanedExisting = existing.filterNot {
                        it.id.removePrefix("bal_op_") in excludedIds
                    }
                    val existingBaseIds = cleanedExisting.map { it.id.removePrefix("bal_op_") }.toSet()

                    val newPending = if (current.baridiMobSmsSyncEnabled) {
                        val deltaOperations = AlgPosteSmsAnalyzer.detectBalanceOperationSuggestions(
                            snapshots = mergedHistory,
                            minTimestampMillis = current.smsSyncBaselineTimestampMillis,
                            excludedSmsIds = excludedIds
                        )
                        (scanResult.suggestions.filterNot { it.id.removePrefix("bal_op_") in excludedIds } + deltaOperations)
                            .distinctBy { it.id.removePrefix("bal_op_") }
                            .filter { candidate ->
                                val baseKey = candidate.id.removePrefix("bal_op_")
                                candidate.id !in processed &&
                                    baseKey !in processed &&
                                    "bal_op_$baseKey" !in processed &&
                                    baseKey !in existingBaseIds
                            }
                    } else {
                        emptyList()
                    }
                    addedCount = newPending.size

                    val existingTaxes = AlgPosteSmsCodec.parseTaxAnomalies(current.detectedSmsTaxesJson)
                    val updatedTaxes = AlgPosteSmsAnalyzer.detectTaxAnomalies(
                        snapshots = mergedHistory,
                        existingAnomalies = existingTaxes,
                        failedAnalysis = failedAnalysis
                    )
                    newTaxCount = (updatedTaxes.size - existingTaxes.size).coerceAtLeast(0)

                    current.copy(
                        pendingSmsSuggestionsJson = AlgPosteSmsCodec.encodeSuggestions(
                            (newPending + cleanedExisting).distinctBy { it.id.removePrefix("bal_op_") }
                        ),
                        smsBalanceHistoryJson = AlgPosteSmsCodec.encodeBalanceHistory(mergedHistory),
                        detectedSmsTaxesJson = AlgPosteSmsCodec.encodeTaxAnomalies(updatedTaxes),
                        detectedFailedSmsTransactionsJson = AlgPosteSmsCodec.encodeFailedTransactions(failedAnalysis.failedTransactions),
                        latestSmsBalanceAmount = latestSnap?.balance ?: current.latestSmsBalanceAmount,
                        latestSmsBalanceUpdatedAt = latestSnap?.timestampMillis ?: current.latestSmsBalanceUpdatedAt,
                        latestSmsBalanceRawSnippet = latestSnap?.rawBody ?: current.latestSmsBalanceRawSnippet
                    )
                }
                autoLoggedTaxCount = TaxExpenseAutoLogger.logFutureTaxesIfEnabled(
                    dao = activeDao(),
                    prefsManager = prefsManager,
                    isArabic = isAr
                ).addedCount
            }

            if (showFeedback) {
                val msg = when {
                    newFailedCount > 0 && addedCount > 0 -> if (isAr) {
                        "تم اكتشاف $addedCount معاملة و رصد $newFailedCount تعاملات فاشلة!"
                    } else {
                        "Found $addedCount transactions & detected $newFailedCount failed/reversed ops!"
                    }
                    newFailedCount > 0 -> if (isAr) {
                        "⚠ تم رصد $newFailedCount تعاملات فاشلة واستبعادها لمنع خلط الحساب!"
                    } else {
                        "⚠ Detected $newFailedCount failed/reversed transactions & excluded from ledger!"
                    }
                    addedCount > 0 && newTaxCount > 0 -> if (isAr) {
                        "تم اكتشاف $addedCount معاملة و $newTaxCount فارق ضرائب/رسوم!"
                    } else {
                        "Found $addedCount transactions & $newTaxCount tax/fee discrepancies!"
                    }
                    autoLoggedTaxCount > 0 -> if (isAr) {
                        "تم تسجيل $autoLoggedTaxCount ضرائب مستقبلية تلقائياً في المصاريف ✓"
                    } else {
                        "Auto-added $autoLoggedTaxCount future taxes to Expenses ✓"
                    }
                    addedCount > 0 -> if (isAr) {
                        "تم اكتشاف $addedCount معاملة جديدة من رسائل بريد الجزائر"
                    } else {
                        "Found $addedCount new ALG Poste transaction suggestions"
                    }
                    newSnapshotCount > 0 -> if (isAr) {
                        "تم تحويل $newSnapshotCount رصيد جديد إلى سجل بريدي موب"
                    } else {
                        "Logged $newSnapshotCount new SMS balances into BaridiMob history"
                    }
                    newTaxCount > 0 -> if (isAr) {
                        "تم رصد $newTaxCount فارق ضرائب أو رسوم من الرصيد"
                    } else {
                        "Detected $newTaxCount balance tax/fee discrepancies"
                    }
                    scanResult.suggestions.isEmpty() && scanResult.snapshots.isEmpty() -> if (isAr) {
                        if (baselineMillis > 0L) "لا توجد رسائل جديدة منذ البدء من الصفر"
                        else "لم يتم العثور على رسائل معاملات من بريد الجزائر في صندوق الوارد"
                    } else {
                        if (baselineMillis > 0L) "No new ALG Poste SMS since Fresh Start"
                        else "No ALG Poste transaction messages found in SMS inbox"
                    }
                    else -> if (isAr) {
                        "جميع رسائل وأرصدة بريد الجزائر محدثة بالفعل"
                    } else {
                        "All ALG Poste SMS & balances are already synced"
                    }
                }
                _toastEvent.emit(msg)
            }
        }
    }

    fun analyzeManualSmsSample(sender: String, body: String) {
        viewModelScope.launch {
            val isAr = settings.value.language == "ar"
            val now = System.currentTimeMillis()
            val effectiveSender = sender.ifBlank { "ALG Poste" }
            val (parsed, snapshot) = withContext(Dispatchers.IO) {
                val p = AlgPosteSmsAnalyzer.parseSms(
                    sender = effectiveSender,
                    body = body,
                    timestampMillis = now,
                    allowAnySenderForManualTest = true
                )
                val s = AlgPosteSmsAnalyzer.parseBalanceSnapshot(
                    sender = effectiveSender,
                    body = body,
                    timestampMillis = now,
                    allowAnySenderForManualTest = true
                )
                // Always archive into the permanent study log even if unrecognized so algorithm can learn it
                val archivedEntity = AlgPosteSmsAnalyzer.toArchivedEntity(
                    sender = effectiveSender,
                    body = body,
                    timestampMillis = now,
                    captureSource = "MANUAL",
                    allowAnySenderForManualTest = true
                )
                if (archivedEntity != null) {
                    activeDao().insertArchivedSmsIfMissing(listOf(archivedEntity))
                }
                p to s
            }
            if (parsed == null && snapshot == null) {
                _toastEvent.emit(
                    if (isAr) "تم حفظ الرسالة في السجل الدائم لدراسة الخوارزمية (تعذر استخراج مبلغ تلقائياً)"
                    else "Archived SMS for algorithm study (could not auto-extract amount or balance)"
                )
                return@launch
            }

            val (caughtNewTaxAmount, caughtFailedRecoveredAmt, autoAddedFutureTaxCount) = withContext(Dispatchers.IO) {
                var taxAmt = 0.0
                var failedRecAmt = 0.0
                prefsManager.updateSettings { current ->
                    val existing = AlgPosteSmsCodec.parseSuggestions(current.pendingSmsSuggestionsJson)
                    val existingHistory = AlgPosteSmsCodec.parseBalanceHistory(current.smsBalanceHistoryJson)
                    val mergedHistory = if (snapshot != null) {
                        (existingHistory + snapshot).distinctBy { it.id }.sortedBy { it.timestampMillis }
                    } else existingHistory
                    val latestSnap = mergedHistory.maxByOrNull { it.timestampMillis }

                    val existingFailed = AlgPosteSmsCodec.parseFailedTransactions(current.detectedFailedSmsTransactionsJson)
                    val dismissedFailedIds = AlgPosteSmsCodec.parseProcessedKeys(current.dismissedFailedSmsIdsJson)
                    val failedAnalysis = AlgPosteSmsAnalyzer.analyzeFailedTransactions(
                        snapshots = mergedHistory,
                        existingEntries = existingFailed,
                        dismissedIds = dismissedFailedIds
                    )
                    val prevFailedIds = existingFailed.map { it.id }.toSet()
                    failedRecAmt = failedAnalysis.failedTransactions
                        .firstOrNull { it.id !in prevFailedIds }
                        ?.recoveredAmount ?: 0.0

                    val excludedIds = failedAnalysis.excludedSuggestionSmsIds
                    val manualDeltaSuggestion = if (parsed == null && snapshot != null && snapshot.id !in excludedIds) {
                        AlgPosteSmsAnalyzer.detectBalanceOperationSuggestions(
                            snapshots = mergedHistory,
                            excludedSmsIds = excludedIds
                        ).firstOrNull { it.id.removePrefix("bal_op_") == snapshot.id }
                    } else null
                    val parsedIfNotExcluded = parsed?.takeIf {
                        it.id.removePrefix("bal_op_") !in excludedIds
                    }
                    val effectiveSuggestion = parsedIfNotExcluded ?: manualDeltaSuggestion

                    val cleanedExisting = existing.filterNot {
                        it.id.removePrefix("bal_op_") in excludedIds
                    }
                    val updatedPending = if (effectiveSuggestion != null) {
                        val base = effectiveSuggestion.id.removePrefix("bal_op_")
                        listOf(effectiveSuggestion) + cleanedExisting.filterNot { it.id.removePrefix("bal_op_") == base }
                    } else cleanedExisting

                    val existingTaxes = AlgPosteSmsCodec.parseTaxAnomalies(current.detectedSmsTaxesJson)
                    val updatedTaxes = AlgPosteSmsAnalyzer.detectTaxAnomalies(
                        snapshots = mergedHistory,
                        existingAnomalies = existingTaxes,
                        failedAnalysis = failedAnalysis
                    )
                    val prevTaxIds = existingTaxes.map { it.id }.toSet()
                    taxAmt = updatedTaxes.firstOrNull { it.id !in prevTaxIds }?.taxAmount ?: 0.0

                    current.copy(
                        pendingSmsSuggestionsJson = AlgPosteSmsCodec.encodeSuggestions(updatedPending),
                        smsBalanceHistoryJson = AlgPosteSmsCodec.encodeBalanceHistory(mergedHistory),
                        detectedSmsTaxesJson = AlgPosteSmsCodec.encodeTaxAnomalies(updatedTaxes),
                        detectedFailedSmsTransactionsJson = AlgPosteSmsCodec.encodeFailedTransactions(failedAnalysis.failedTransactions),
                        latestSmsBalanceAmount = latestSnap?.balance ?: current.latestSmsBalanceAmount,
                        latestSmsBalanceUpdatedAt = latestSnap?.timestampMillis ?: current.latestSmsBalanceUpdatedAt,
                        latestSmsBalanceRawSnippet = latestSnap?.rawBody ?: current.latestSmsBalanceRawSnippet
                    )
                }
                val autoResult = TaxExpenseAutoLogger.logFutureTaxesIfEnabled(
                    dao = activeDao(),
                    prefsManager = prefsManager,
                    isArabic = isAr
                )
                Triple(taxAmt, failedRecAmt, autoResult.addedCount)
            }

            val msg = buildString {
                if (parsed != null) {
                    val kindLabel = if (parsed.isExpense) {
                        if (isAr) "مصروف" else "Expense"
                    } else {
                        if (isAr) "دخل" else "Income"
                    }
                    val amtLabel = formatDzdAmount(parsed.amount, if (isAr) "دج" else "DA")
                    append(if (isAr) "$kindLabel: $amtLabel" else "$kindLabel: $amtLabel")
                } else if (snapshot != null) {
                    val balLabel = formatDzdAmount(snapshot.balance, if (isAr) "دج" else "DA")
                    append(if (isAr) "الرصيد المستخرج: $balLabel" else "Balance extracted: $balLabel")
                }
                if (caughtFailedRecoveredAmt > 0.0) {
                    val recLabel = formatDzdAmount(caughtFailedRecoveredAmt, if (isAr) "دج" else "DA")
                    append(
                        if (isAr) " • ⚠ معاملة فاشلة مسترجعة: $recLabel!"
                        else " • ⚠ Caught Failed Op Reversal: $recLabel!"
                    )
                }
                if (caughtNewTaxAmount > 0.0) {
                    val taxLabel = formatDzdAmount(caughtNewTaxAmount, if (isAr) "دج" else "DA")
                    append(if (isAr) " • تم تصيّد ضرائب/فارق: $taxLabel!" else " • Caught fee/tax: $taxLabel!")
                }
                if (autoAddedFutureTaxCount > 0) {
                    append(
                        if (isAr) " • أُضيفت للمصاريف تلقائياً ✓"
                        else " • Auto-added to Expenses ✓"
                    )
                }
            }
            _toastEvent.emit(msg)
        }
    }

    /**
     * Enables or disables automatic logging of detected BaridiMob/ALG Poste taxes into Expenses.
     * When enabled, records a fresh activation baseline timestamp so ONLY future detected taxes
     * are automatically added — previously detected/historical taxes are never swept in.
     */
    fun setAutoAddTaxesEnabled(
        enabled: Boolean,
        activationTimestampMillis: Long = System.currentTimeMillis()
    ) {
        val isAr = settings.value.language == "ar"
        prefsManager.updateSettings { current ->
            if (enabled) {
                current.copy(
                    autoAddTaxesEnabled = true,
                    autoAddTaxesBaselineMillis = activationTimestampMillis.coerceAtLeast(1L)
                )
            } else {
                current.copy(
                    autoAddTaxesEnabled = false,
                    autoAddTaxesBaselineMillis = 0L
                )
            }
        }
        viewModelScope.launch {
            _toastEvent.emit(
                if (enabled) {
                    if (isAr) "تم تفعيل الإضافة التلقائية للضرائب — المستقبلية فقط (بدون السابقة) ✓"
                    else "Auto Add Taxes enabled — future taxes only (past skipped) ✓"
                } else {
                    if (isAr) "تم تعطيل الإضافة التلقائية للضرائب"
                    else "Auto Add Taxes disabled"
                }
            )
        }
    }

    fun recordTaxAnomalyAsExpense(anomalyId: String) {
        viewModelScope.launch {
            val currentSettings = settings.value
            val isAr = currentSettings.language == "ar"
            val taxes = AlgPosteSmsCodec.parseTaxAnomalies(currentSettings.detectedSmsTaxesJson)
            val target = taxes.firstOrNull { it.id == anomalyId } ?: return@launch
            if (target.isRecordedAsExpense) return@launch
            val existingTransactions = withContext(Dispatchers.IO) { activeDao().getAllTransactionsList() }
            if (isTaxAnomalyAlreadyLoggedInTransactions(target, existingTransactions)) {
                prefsManager.updateSettings { current ->
                    val updated = AlgPosteSmsCodec.parseTaxAnomalies(current.detectedSmsTaxesJson).map { item ->
                        if (item.id == anomalyId) item.copy(isRecordedAsExpense = true) else item
                    }
                    current.copy(detectedSmsTaxesJson = AlgPosteSmsCodec.encodeTaxAnomalies(updated))
                }
                return@launch
            }

            val refTag = "[tax_ref:${target.id}]"
            val explanation = if (isAr) target.explanationAr else target.explanationEn
            val composedNote = if (explanation.isBlank()) {
                refTag
            } else {
                "${explanation.take((240 - refTag.length - 1).coerceAtLeast(0))} $refTag"
            }
            activeDao().insertTransaction(
                TransactionEntity(
                    type = TransactionType.EXPENSE,
                    amount = target.taxAmount,
                    category = "taxes_fees",
                    title = if (isAr) target.titleAr.ifBlank { "ضرائب ورسوم بريدي موب" }
                    else target.titleEn.ifBlank { "BaridiMob Tax / Fee" },
                    note = composedNote.take(240),
                    dateMillis = target.detectedAtMillis
                )
            )

            prefsManager.updateSettings { current ->
                val updated = AlgPosteSmsCodec.parseTaxAnomalies(current.detectedSmsTaxesJson).map { item ->
                    if (item.id == anomalyId) item.copy(isRecordedAsExpense = true) else item
                }
                current.copy(detectedSmsTaxesJson = AlgPosteSmsCodec.encodeTaxAnomalies(updated))
            }

            val formattedTax = formatDzdAmount(target.taxAmount, if (isAr) "دج" else "DA")
            _toastEvent.emit(
                if (isAr) "تم تسجيل الضريبة/الفارق ($formattedTax) في المصاريف ✓"
                else "Recorded caught tax/fee ($formattedTax) to Expenses ✓"
            )
        }
    }

    fun dismissTaxAnomaly(anomalyId: String) {
        prefsManager.updateSettings { current ->
            val updated = AlgPosteSmsCodec.parseTaxAnomalies(current.detectedSmsTaxesJson)
                .filterNot { it.id == anomalyId }
            current.copy(detectedSmsTaxesJson = AlgPosteSmsCodec.encodeTaxAnomalies(updated))
        }
    }

    fun clearAllTaxAnomalies() {
        prefsManager.updateSettings { it.copy(detectedSmsTaxesJson = "[]") }
    }

    private fun findMatchingConflictingTransactions(
        entry: SmsFailedTransactionEntry,
        transactions: List<TransactionEntity>
    ): List<TransactionEntity> {
        val debitSnippet = entry.debitRawBody.trim().take(32)
        val recoverySnippet = entry.recoveryRawBody.trim().take(32)
        val isExplicitReversal = entry.detectionKind in setOf(
            "EXPLICIT_REFUND_REVERSAL",
            "EXPLICIT_REVERSAL_UNLINKED"
        )
        return transactions.filter { tx ->
            val matchesFailedDebit = tx.type == TransactionType.EXPENSE &&
                entry.debitAmount > 0.0 &&
                kotlin.math.abs(tx.amount - entry.debitAmount) < 0.01 &&
                kotlin.math.abs(tx.dateMillis - entry.debitTimestampMillis) <= 120_000L &&
                (debitSnippet.isBlank() || tx.note.contains(debitSnippet, ignoreCase = true))

            val matchesExplicitRefundIncome = isExplicitReversal &&
                tx.type == TransactionType.INCOME &&
                entry.recoveredAmount > 0.0 &&
                kotlin.math.abs(tx.amount - entry.recoveredAmount) < 0.01 &&
                kotlin.math.abs(tx.dateMillis - entry.recoveredTimestampMillis) <= 120_000L &&
                (recoverySnippet.isBlank() || tx.note.contains(recoverySnippet, ignoreCase = true))

            matchesFailedDebit || matchesExplicitRefundIncome
        }
    }

    fun dismissFailedSmsTransaction(failedId: String) {
        prefsManager.updateSettings { current ->
            val updated = AlgPosteSmsCodec.parseFailedTransactions(current.detectedFailedSmsTransactionsJson)
                .filterNot { it.id == failedId }
            val dismissed = AlgPosteSmsCodec.parseProcessedKeys(current.dismissedFailedSmsIdsJson) + failedId
            current.copy(
                detectedFailedSmsTransactionsJson = AlgPosteSmsCodec.encodeFailedTransactions(updated),
                dismissedFailedSmsIdsJson = AlgPosteSmsCodec.encodeProcessedKeys(dismissed)
            )
        }
    }

    fun clearAllFailedSmsTransactions() {
        prefsManager.updateSettings { current ->
            val existingIds = AlgPosteSmsCodec.parseFailedTransactions(current.detectedFailedSmsTransactionsJson)
                .map { it.id }
            val dismissed = AlgPosteSmsCodec.parseProcessedKeys(current.dismissedFailedSmsIdsJson) + existingIds
            current.copy(
                detectedFailedSmsTransactionsJson = "[]",
                dismissedFailedSmsIdsJson = AlgPosteSmsCodec.encodeProcessedKeys(dismissed)
            )
        }
    }

    fun resolveFailedTransactionAndDeleteConflictingLedgerEntries(failedId: String) {
        viewModelScope.launch {
            val isAr = settings.value.language == "ar"
            val failedList = AlgPosteSmsCodec.parseFailedTransactions(
                settings.value.detectedFailedSmsTransactionsJson
            )
            val target = failedList.firstOrNull { it.id == failedId } ?: return@launch
            val deletedCount = withContext(Dispatchers.IO) {
                val allTx = activeDao().getAllTransactionsList()
                val matches = findMatchingConflictingTransactions(target, allTx)
                for (tx in matches) {
                    activeDao().deleteTransaction(tx)
                }
                prefsManager.updateSettings { current ->
                    val updated = AlgPosteSmsCodec.parseFailedTransactions(current.detectedFailedSmsTransactionsJson)
                        .map { item -> if (item.id == failedId) item.copy(isResolved = true) else item }
                    current.copy(
                        detectedFailedSmsTransactionsJson = AlgPosteSmsCodec.encodeFailedTransactions(updated)
                    )
                }
                matches.size
            }
            _toastEvent.emit(
                if (deletedCount > 0) {
                    if (isAr) "تم حذف $deletedCount معاملة وهمية من الحسابات لمنع التناقض ✓"
                    else "Removed $deletedCount ghost entry from ledger to keep balances accurate ✓"
                } else {
                    if (isAr) "تمت معالجة الحالة — لا توجد معاملات متضاربة في السجل ✓"
                    else "Marked resolved — no conflicting ledger entries found ✓"
                }
            )
        }
    }

    fun acceptSmsSuggestion(
        suggestionId: String,
        overrideType: TransactionType? = null,
        customCategory: String? = null,
        customReasonOrSource: String? = null
    ) {
        viewModelScope.launch {
            val currentSettings = settings.value
            val isAr = currentSettings.language == "ar"
            val pending = AlgPosteSmsCodec.parseSuggestions(currentSettings.pendingSmsSuggestionsJson)
            val target = pending.firstOrNull { it.id == suggestionId } ?: return@launch

            val finalType = overrideType ?: target.transactionType
            val cleanCustomCat = customCategory?.trim()?.takeIf { it.isNotBlank() }
            val cleanReasonOrSource = customReasonOrSource?.trim()?.takeIf { it.isNotBlank() }
            val resolvedCategory = when {
                cleanCustomCat != null -> cleanCustomCat
                overrideType != null && overrideType != target.transactionType -> {
                    if (finalType == TransactionType.EXPENSE) "other" else "transfer"
                }
                else -> target.suggestedCategory
            }
            val baseFallbackTitle = if (isAr) {
                if (finalType == TransactionType.EXPENSE) {
                    if (overrideType == TransactionType.EXPENSE && target.transactionType != TransactionType.EXPENSE) {
                        "مصروف بريدي موب (بريد الجزائر)"
                    } else {
                        target.suggestedTitleAr.ifBlank { "مصروف بريدي موب (بريد الجزائر)" }
                    }
                } else {
                    if (overrideType == TransactionType.INCOME && target.transactionType != TransactionType.INCOME) {
                        "دخل بريدي موب (بريد الجزائر)"
                    } else {
                        target.suggestedTitleAr.ifBlank { "دخل بريدي موب (بريد الجزائر)" }
                    }
                }
            } else {
                if (finalType == TransactionType.EXPENSE) {
                    if (overrideType == TransactionType.EXPENSE && target.transactionType != TransactionType.EXPENSE) {
                        "BaridiMob Expense (ALG Poste)"
                    } else {
                        target.suggestedTitleEn.ifBlank { "BaridiMob Expense (ALG Poste)" }
                    }
                } else {
                    if (overrideType == TransactionType.INCOME && target.transactionType != TransactionType.INCOME) {
                        "BaridiMob Income (ALG Poste)"
                    } else {
                        target.suggestedTitleEn.ifBlank { "BaridiMob Income (ALG Poste)" }
                    }
                }
            }
            val title = cleanReasonOrSource ?: baseFallbackTitle
            val composedNote = if (cleanReasonOrSource != null) {
                val prefix = if (finalType == TransactionType.EXPENSE) {
                    if (isAr) "سبب السحب: $cleanReasonOrSource" else "Reason: $cleanReasonOrSource"
                } else {
                    if (isAr) "جهة/سبب الدخل: $cleanReasonOrSource" else "Source/Reason: $cleanReasonOrSource"
                }
                "$prefix • ${target.sender}: ${target.rawBody}".take(240)
            } else {
                "${target.sender}: ${target.rawBody}".take(240)
            }
            activeDao().insertTransaction(
                TransactionEntity(
                    type = finalType,
                    amount = target.amount,
                    category = resolvedCategory,
                    title = title,
                    note = composedNote,
                    dateMillis = target.receivedAtMillis
                )
            )

            prefsManager.updateSettings { current ->
                val baseKey = suggestionId.removePrefix("bal_op_")
                val remaining = AlgPosteSmsCodec.parseSuggestions(current.pendingSmsSuggestionsJson)
                    .filterNot { it.id == suggestionId || it.id.removePrefix("bal_op_") == baseKey }
                val processed = AlgPosteSmsCodec.parseProcessedKeys(current.processedSmsKeysJson) +
                    listOf(suggestionId, baseKey, "bal_op_$baseKey")
                current.copy(
                    pendingSmsSuggestionsJson = AlgPosteSmsCodec.encodeSuggestions(remaining),
                    processedSmsKeysJson = AlgPosteSmsCodec.encodeProcessedKeys(processed)
                )
            }

            val formattedAmt = formatDzdAmount(target.amount, if (isAr) "دج" else "DA")
            _toastEvent.emit(
                if (finalType == TransactionType.EXPENSE) {
                    if (isAr) "تمت إضافة $formattedAmt إلى المصاريف" else "Added $formattedAmt to Expenses"
                } else {
                    if (isAr) "تمت إضافة $formattedAmt إلى الدخل" else "Added $formattedAmt to Income"
                }
            )
        }
    }

    fun dismissSmsSuggestion(suggestionId: String) {
        prefsManager.updateSettings { current ->
            val baseKey = suggestionId.removePrefix("bal_op_")
            val remaining = AlgPosteSmsCodec.parseSuggestions(current.pendingSmsSuggestionsJson)
                .filterNot { it.id == suggestionId || it.id.removePrefix("bal_op_") == baseKey }
            val processed = AlgPosteSmsCodec.parseProcessedKeys(current.processedSmsKeysJson) +
                listOf(suggestionId, baseKey, "bal_op_$baseKey")
            current.copy(
                pendingSmsSuggestionsJson = AlgPosteSmsCodec.encodeSuggestions(remaining),
                processedSmsKeysJson = AlgPosteSmsCodec.encodeProcessedKeys(processed)
            )
        }
    }

    fun clearAllSmsSuggestions() {
        prefsManager.updateSettings { current ->
            val pendingKeys = AlgPosteSmsCodec.parseSuggestions(current.pendingSmsSuggestionsJson)
                .flatMap { s ->
                    val base = s.id.removePrefix("bal_op_")
                    listOf(s.id, base, "bal_op_$base")
                }
            val processed = AlgPosteSmsCodec.parseProcessedKeys(current.processedSmsKeysJson) + pendingKeys
            current.copy(
                pendingSmsSuggestionsJson = "[]",
                processedSmsKeysJson = AlgPosteSmsCodec.encodeProcessedKeys(processed)
            )
        }
    }

    // ==================== Binance Crypto Card & Hourly Market Prices ====================

    fun refreshCryptoMarketPrices(force: Boolean = false, showFeedback: Boolean = false) {
        if (_isRefreshingCryptoPrices.value) return
        val lastSync = settings.value.binanceLastPriceSyncMillis
        if (!force && !BinanceCryptoCatalog.shouldRefreshMarketPrices(lastSync)) {
            return
        }

        viewModelScope.launch {
            _isRefreshingCryptoPrices.value = true
            _cryptoPriceError.value = null
            val result = BinanceMarketPriceFetcher.fetchLivePrices()
            _isRefreshingCryptoPrices.value = false

            val isAr = settings.value.language == "ar"
            result.onSuccess { fetchedMap ->
                val now = System.currentTimeMillis()
                prefsManager.updateSettings { current ->
                    val merged = BinanceCryptoCodec.parsePrices(current.binancePricesJson).toMutableMap()
                    merged.putAll(fetchedMap)
                    current.copy(
                        binancePricesJson = BinanceCryptoCodec.encodePrices(merged),
                        binanceLastPriceSyncMillis = now
                    )
                }
                if (showFeedback) {
                    _toastEvent.emit(
                        if (isAr) "تم تحديث أسعار العملات الرقمية من السوق ✅"
                        else "Crypto market prices updated ✅"
                    )
                }
            }.onFailure {
                val msg = if (isAr) {
                    "تعذر الاتصال بالسوق لتحديث الأسعار"
                } else {
                    "Unable to fetch live market prices"
                }
                _cryptoPriceError.value = msg
                if (showFeedback) {
                    _toastEvent.emit(msg)
                }
            }
        }
    }

    fun saveCryptoHolding(
        holdingId: String?,
        symbol: String,
        quantity: Double,
        note: String = ""
    ) {
        val cleanSym = symbol.trim().uppercase()
        val cleanNote = note.trim()
        if (cleanSym.isBlank() || !quantity.isFinite()) return
        val isAr = settings.value.language == "ar"
        val now = System.currentTimeMillis()

        prefsManager.updateSettings { current ->
            val currentList = BinanceCryptoCodec.parseHoldings(current.binanceHoldingsJson)
                .toMutableList()
            if (holdingId.isNullOrBlank()) {
                if (quantity > 0.0) {
                    currentList.add(
                        0,
                        CryptoHoldingEntry(
                            id = java.util.UUID.randomUUID().toString(),
                            symbol = cleanSym,
                            quantity = quantity,
                            updatedAtMillis = now,
                            note = cleanNote
                        )
                    )
                }
            } else {
                val targetIndex = currentList.indexOfFirst { it.id == holdingId }
                if (targetIndex >= 0) {
                    if (quantity > 0.0) {
                        currentList[targetIndex] = currentList[targetIndex].copy(
                            symbol = cleanSym,
                            quantity = quantity,
                            updatedAtMillis = now,
                            note = cleanNote
                        )
                    } else {
                        currentList.removeAt(targetIndex)
                    }
                } else if (quantity > 0.0) {
                    currentList.add(
                        0,
                        CryptoHoldingEntry(
                            id = holdingId,
                            symbol = cleanSym,
                            quantity = quantity,
                            updatedAtMillis = now,
                            note = cleanNote
                        )
                    )
                }
            }
            current.copy(binanceHoldingsJson = BinanceCryptoCodec.encodeHoldings(currentList))
        }

        // Ensure we have fresh prices if stale or if coin price isn't cached yet
        val currentPrices = BinanceCryptoCodec.parsePrices(settings.value.binancePricesJson)
        if (quantity > 0.0 && (currentPrices[cleanSym] == null ||
                BinanceCryptoCatalog.shouldRefreshMarketPrices(settings.value.binanceLastPriceSyncMillis))
        ) {
            refreshCryptoMarketPrices(force = true, showFeedback = false)
        }

        viewModelScope.launch {
            _toastEvent.emit(
                if (quantity > 0.0) {
                    if (isAr) "تم حفظ رصيد $cleanSym في محفظة بينانس ✅"
                    else "Saved $cleanSym holding to Binance Wallet ✅"
                } else {
                    if (isAr) "تمت إزالة $cleanSym من المحفظة"
                    else "Removed $cleanSym from Binance Wallet"
                }
            )
        }
    }

    fun removeCryptoHoldingById(holdingId: String) {
        if (holdingId.isBlank()) return
        val isAr = settings.value.language == "ar"
        var removedSymbol: String? = null

        prefsManager.updateSettings { current ->
            val currentList = BinanceCryptoCodec.parseHoldings(current.binanceHoldingsJson)
            val target = currentList.firstOrNull { it.id == holdingId }
            removedSymbol = target?.symbol
            val filtered = currentList.filterNot { it.id == holdingId }
            current.copy(binanceHoldingsJson = BinanceCryptoCodec.encodeHoldings(filtered))
        }

        val symLabel = removedSymbol ?: ""
        if (symLabel.isNotBlank()) {
            viewModelScope.launch {
                _toastEvent.emit(
                    if (isAr) "تمت إزالة $symLabel من المحفظة"
                    else "Removed $symLabel from Binance Wallet"
                )
            }
        }
    }

    fun convertCryptoHoldingToUsdt(
        holdingId: String,
        convertQuantity: Double,
        targetStablecoin: String = "USDT"
    ) {
        if (holdingId.isBlank() || !convertQuantity.isFinite() || convertQuantity <= 0.0) return
        val targetSym = targetStablecoin.trim().uppercase(Locale.US).let {
            if (it == "USDC") "USDC" else "USDT"
        }
        val isAr = settings.value.language == "ar"
        val currentPrices = BinanceCryptoCodec.parsePrices(settings.value.binancePricesJson)
        val currentList = BinanceCryptoCodec.parseHoldings(settings.value.binanceHoldingsJson)
        val sourceEntry = currentList.firstOrNull { it.id == holdingId } ?: return
        val sourceSym = sourceEntry.symbol.trim().uppercase(Locale.US)
        if (sourceSym == targetSym) return

        val unitPriceUsd = when {
            sourceSym == "USDT" || sourceSym == "USDC" -> currentPrices[sourceSym]?.priceUsd ?: 1.0
            else -> currentPrices[sourceSym]?.priceUsd ?: 0.0
        }
        val targetPriceUsd = (currentPrices[targetSym]?.priceUsd ?: 1.0).takeIf { it > 0.0 } ?: 1.0
        if (unitPriceUsd <= 0.0 || !unitPriceUsd.isFinite()) {
            viewModelScope.launch {
                _toastEvent.emit(
                    if (isAr) "يرجى تحديث السعر اللحظي لـ $sourceSym قبل التحويل"
                    else "Please sync live $sourceSym price before converting"
                )
            }
            return
        }

        val actualConvertQty = convertQuantity.coerceAtMost(sourceEntry.quantity)
        val targetReceived = (actualConvertQty * unitPriceUsd) / targetPriceUsd
        if (actualConvertQty <= 0.0 || targetReceived <= 0.0 || !targetReceived.isFinite()) return

        val now = System.currentTimeMillis()
        var convertedFully = false

        prefsManager.updateSettings { current ->
            val mutable = BinanceCryptoCodec.parseHoldings(current.binanceHoldingsJson).toMutableList()
            val srcIdx = mutable.indexOfFirst { it.id == holdingId }
            if (srcIdx < 0) return@updateSettings current
            val latestSource = mutable[srcIdx]
            val clampedQty = actualConvertQty.coerceAtMost(latestSource.quantity)
            val remainingQty = latestSource.quantity - clampedQty

            if (remainingQty <= 1e-9 || remainingQty <= latestSource.quantity * 1e-7) {
                mutable.removeAt(srcIdx)
                convertedFully = true
            } else {
                mutable[srcIdx] = latestSource.copy(
                    quantity = remainingQty,
                    updatedAtMillis = now
                )
            }

            // Credit target stablecoin (USDT/USDC) received into existing general holding or create a new entry
            val generalTargetIdx = mutable.indexOfFirst {
                it.symbol.equals(targetSym, ignoreCase = true) && it.note.isBlank()
            }
            val targetIdx = if (generalTargetIdx >= 0) {
                generalTargetIdx
            } else {
                mutable.indexOfFirst { it.symbol.equals(targetSym, ignoreCase = true) }
            }

            if (targetIdx >= 0) {
                val existingTarget = mutable[targetIdx]
                mutable[targetIdx] = existingTarget.copy(
                    quantity = existingTarget.quantity + targetReceived,
                    updatedAtMillis = now
                )
            } else {
                mutable.add(
                    0,
                    CryptoHoldingEntry(
                        id = java.util.UUID.randomUUID().toString(),
                        symbol = targetSym,
                        quantity = targetReceived,
                        updatedAtMillis = now,
                        note = ""
                    )
                )
            }

            current.copy(binanceHoldingsJson = BinanceCryptoCodec.encodeHoldings(mutable))
        }

        val qtyLabel = String.format(Locale.US, "%.4f", actualConvertQty).trimEnd('0').trimEnd('.')
        val receivedLabel = String.format(Locale.US, "%,.2f", targetReceived)
        viewModelScope.launch {
            _toastEvent.emit(
                if (isAr) {
                    if (convertedFully) "تم تحويل كامل رصيد $sourceSym ($qtyLabel) إلى $receivedLabel $targetSym ✅"
                    else "تم تحويل $qtyLabel $sourceSym إلى $receivedLabel $targetSym ✅"
                } else {
                    if (convertedFully) "Converted all $qtyLabel $sourceSym to $receivedLabel $targetSym ✅"
                    else "Converted $qtyLabel $sourceSym to $receivedLabel $targetSym ✅"
                }
            )
        }
    }

    // ==================== Square Market (Gold Karats + Bank/Parallel FX, Hourly) ====================

    fun refreshSquareMarketData(force: Boolean = false, showFeedback: Boolean = false) {
        if (_isRefreshingSquareMarket.value) return
        val currentSettings = settings.value
        val lastSync = currentSettings.squareLastPriceSyncMillis
        val syncIntervalMinutes = currentSettings.squareSyncIntervalMinutes.coerceIn(0, 1440)
        val cachedSnapshot = SquareMarketCodec.parseSnapshot(currentSettings.squareMarketSnapshotJson)
        val hasCurrentVersionCache = cachedSnapshot != null && cachedSnapshot.version >= SQUARE_SNAPSHOT_VERSION
        if (!force && hasCurrentVersionCache && syncIntervalMinutes <= 0) {
            return
        }
        val needsDailyRollover = SquareMarketCatalog.needsDailyRollover(
            storedBaselineJson = currentSettings.squareDailyBaselineJson
        )
        if (!force && hasCurrentVersionCache && !needsDailyRollover && !SquareMarketCatalog.shouldRefresh(
                lastSyncMillis = lastSync,
                intervalMinutes = syncIntervalMinutes
            )
        ) {
            return
        }

        val enabledSourceIds = SquareMarketCodec.parseEnabledSourceIds(currentSettings.squareEnabledSourceIdsJson)

        viewModelScope.launch {
            _isRefreshingSquareMarket.value = true
            _squareMarketError.value = null
            val result = SquareMarketFetcher.fetchLiveSnapshot(enabledSourceIds)
            _isRefreshingSquareMarket.value = false

            val isAr = settings.value.language == "ar"
            result.onSuccess { rawSnapshot ->
                val now = rawSnapshot.updatedAtMillis
                withContext(Dispatchers.IO) {
                    prefsManager.updateSettings { current ->
                        val (comparedSnapshot, nextBaselineJson) = SquareMarketCatalog.apply24hPriceComparison(
                            fetched = rawSnapshot,
                            storedBaselineJson = current.squareDailyBaselineJson,
                            nowMillis = now
                        )
                        val encoded = SquareMarketCodec.encodeSnapshot(comparedSnapshot)
                        if (encoded.isNotBlank()) {
                            current.copy(
                                squareMarketSnapshotJson = encoded,
                                squareDailyBaselineJson = nextBaselineJson,
                                squareLastPriceSyncMillis = now
                            )
                        } else {
                            current
                        }
                    }
                }
                if (showFeedback) {
                    _toastEvent.emit(
                        if (isAr) "تم تحديث أسعار الذهب والعملة الصعبة ✅"
                        else "Square gold & currency rates updated ✅"
                    )
                }
            }.onFailure {
                val msg = if (isAr) {
                    "تعذر تحديث أسعار الذهب والعملة حالياً"
                } else {
                    "Unable to fetch live Square market rates"
                }
                _squareMarketError.value = msg
                if (showFeedback) {
                    _toastEvent.emit(msg)
                }
            }
        }
    }

    fun updateSquareSyncIntervalMinutes(minutes: Int) {
        val safeMinutes = minutes.coerceIn(0, 1440)
        prefsManager.updateSettings { current ->
            current.copy(squareSyncIntervalMinutes = safeMinutes)
        }
        val isAr = settings.value.language == "ar"
        viewModelScope.launch {
            val label = when {
                safeMinutes <= 0 -> if (isAr) "يدوي فقط" else "Manual only"
                safeMinutes < 60 -> if (isAr) "كل $safeMinutes دقيقة" else "Every $safeMinutes min"
                else -> {
                    val hours = safeMinutes / 60
                    if (isAr) "كل $hours ساعة" else "Every ${hours}h"
                }
            }
            _toastEvent.emit(
                if (isAr) "تم حفظ وقت مزامنة سكوار: $label ⏱️"
                else "Square sync interval saved: $label ⏱️"
            )
        }
    }

    fun toggleSquarePinnedId(tickerId: String) {
        val cleanId = tickerId.trim()
        if (cleanId.isEmpty()) return
        prefsManager.updateSettings { current ->
            val currentPins = SquareMarketCodec.parsePinnedIds(current.squarePinnedIdsJson).toMutableSet()
            if (!currentPins.add(cleanId)) {
                currentPins.remove(cleanId)
            }
            current.copy(squarePinnedIdsJson = SquareMarketCodec.encodePinnedIds(currentPins))
        }
    }

    fun updateSquareEnabledSourceIds(sourceIds: Set<String>) {
        val cleanIds = sourceIds.filter { it in SquareMarketCatalog.ALL_SOURCE_IDS }.toSet()
        if (cleanIds.isEmpty()) return
        val encoded = SquareMarketCodec.encodeEnabledSourceIds(cleanIds)
        prefsManager.updateSettings { current ->
            current.copy(squareEnabledSourceIdsJson = encoded)
        }
        val isAr = settings.value.language == "ar"
        viewModelScope.launch {
            _toastEvent.emit(
                if (isAr) "تم اعتماد ${cleanIds.size} موقع لتحديث الأسعار ✓"
                else "Saved ${cleanIds.size} price update websites ✓"
            )
        }
        refreshSquareMarketData(force = true, showFeedback = false)
    }

    // ==================== Assets & Saving (Gold via Square + Crypto via Binance) ====================

    fun setIncludeAssetsInSavingsTotal(enabled: Boolean) {
        val isAr = settings.value.language == "ar"
        prefsManager.updateSettings { current ->
            current.copy(includeAssetsInSavingsTotal = enabled)
        }
        if (enabled) {
            syncAssetsMarketPrices(force = false, showFeedback = false)
        }
        viewModelScope.launch {
            _toastEvent.emit(
                if (enabled) {
                    if (isAr) "تم دمج الأصول (الذهب والعملات الرقمية) مع إجمالي المدخرات ✓"
                    else "Combined Gold & Crypto assets into Total Reserved Savings ✓"
                } else {
                    if (isAr) "عرض الرصيد النقدي المدّخر فقط"
                    else "Showing Cash Savings balance only"
                }
            )
        }
    }

    fun syncAssetsMarketPrices(force: Boolean = true, showFeedback: Boolean = true) {
        refreshSquareMarketData(force = force, showFeedback = showFeedback)
        refreshCryptoMarketPrices(force = force, showFeedback = false)
    }

    /**
     * Triggered every time the user enters the Assets & Saving screen:
     * forces a fresh background sync across Square (Gold + FX), Binance Crypto,
     * BaridiMob / ALG Poste SMS Inbox, Salary Scheduler, Auto-Pay Debts, and App Updates.
     */
    fun syncEverythingOnAssetsSavingEnter(context: Context = getApplication()) {
        refreshSquareMarketData(force = true, showFeedback = false)
        refreshCryptoMarketPrices(force = true, showFeedback = false)
        if (AlgPosteSmsInboxScanner.hasSmsPermissions(context)) {
            if (settings.value.baridiMobSmsSyncEnabled) {
                scanAlgPosteSmsInbox(context = context, showFeedback = false)
            } else {
                refreshCurrentBalanceOnlyFromInbox(context = context, showFeedback = false)
            }
        }
        viewModelScope.launch {
            checkAndApplySalaryScheduler(force = false)
            checkAndExecuteAutoPayDebts()
        }
        checkForUpdates(manual = false)
    }

    fun upsertGoldHolding(
        karat: Int,
        grams: Double,
        label: String = "",
        existingId: String? = null
    ) {
        if (grams <= 0.0 || !grams.isFinite()) return
        val allowedKarats = SquareMarketCatalog.karats.map { it.first }.toSet()
        val safeKarat = if (karat in allowedKarats) karat else 18
        val cleanLabel = label.trim()
        val now = System.currentTimeMillis()
        val isAr = settings.value.language == "ar"

        prefsManager.updateSettings { current ->
            val existingList = GoldAssetCodec.parseHoldings(current.goldAssetHoldingsJson)
            val updatedList = if (!existingId.isNullOrBlank() && existingList.any { it.id == existingId }) {
                existingList.map { item ->
                    if (item.id == existingId) {
                        item.copy(
                            karat = safeKarat,
                            grams = grams,
                            label = cleanLabel,
                            updatedAtMillis = now
                        )
                    } else item
                }
            } else {
                listOf(
                    GoldAssetHolding(
                        karat = safeKarat,
                        grams = grams,
                        label = cleanLabel,
                        updatedAtMillis = now
                    )
                ) + existingList
            }
            current.copy(goldAssetHoldingsJson = GoldAssetCodec.encodeHoldings(updatedList))
        }

        if (squareSnapshot.value == null || SquareMarketCatalog.shouldRefresh(
                lastSyncMillis = settings.value.squareLastPriceSyncMillis,
                intervalMinutes = settings.value.squareSyncIntervalMinutes
            )
        ) {
            refreshSquareMarketData(force = true, showFeedback = false)
        }

        viewModelScope.launch {
            val formattedGrams = String.format(java.util.Locale.US, "%.2f", grams).trimEnd('0').trimEnd('.')
            _toastEvent.emit(
                if (isAr) "تم حفظ $formattedGrams غ ذهب عيار $safeKarat وتحديث التقييم ✓"
                else "Saved ${formattedGrams}g of ${safeKarat}K gold ✓"
            )
        }
    }

    fun removeGoldHolding(id: String) {
        if (id.isBlank()) return
        val isAr = settings.value.language == "ar"
        prefsManager.updateSettings { current ->
            val filtered = GoldAssetCodec.parseHoldings(current.goldAssetHoldingsJson)
                .filterNot { it.id == id }
            current.copy(goldAssetHoldingsJson = GoldAssetCodec.encodeHoldings(filtered))
        }
        viewModelScope.launch {
            _toastEvent.emit(
                if (isAr) "تمت إزالة سجل الذهب"
                else "Gold entry removed"
            )
        }
    }
}
