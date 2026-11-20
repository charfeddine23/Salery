package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.data.UserSettings
import com.example.data.VaultAppPresets
import com.example.data.VaultKeyEntry
import com.example.ui.components.toPatternHash
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
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import com.example.notification.DebtReminderScheduler
import com.example.sms.AlgPosteSmsAnalyzer
import com.example.sms.AlgPosteSmsCodec
import com.example.sms.AlgPosteSmsInboxScanner
import com.example.sms.ArchivedSmsStudyReport
import com.example.sms.SmsBalanceSnapshot
import com.example.sms.SmsSuggestionEntry
import com.example.sms.SmsTaxAnomalyEntry
import com.example.update.AppUpdateManager
import com.example.update.AppUpdateUiState
import com.example.update.DownloadState
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
import com.example.security.CryptoIssue
import com.example.security.CryptoIssueType
import com.example.security.DecryptOutcome
import com.example.security.VaultCryptoManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

data class FinancialSummary(
    val totalBalance: Double = 0.0,
    val grossBalance: Double = 0.0,
    val savingsBalance: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val monthlyExpenses: Double = 0.0,
    val pendingDebtsOwedByMe: Double = 0.0,
    val pendingDebtsOwedToMe: Double = 0.0
)

internal fun isTaxAnomalyAlreadyLoggedInTransactions(
    anomaly: SmsTaxAnomalyEntry,
    existingTransactions: List<TransactionEntity>
): Boolean {
    if (anomaly.isRecordedAsExpense) return true
    val refTag = "[tax_ref:${anomaly.id}]"
    return existingTransactions.any { tx ->
        tx.type == TransactionType.EXPENSE && (
            tx.note.contains(refTag) ||
                (tx.category.equals("taxes_fees", ignoreCase = true) &&
                    kotlin.math.abs(tx.amount - anomaly.taxAmount) < 0.01 &&
                    kotlin.math.abs(tx.dateMillis - anomaly.detectedAtMillis) <= 5_000L)
            )
    }
}

internal fun findUnloggedTaxAnomaliesForExpenses(
    anomalies: List<SmsTaxAnomalyEntry>,
    existingTransactions: List<TransactionEntity>
): List<SmsTaxAnomalyEntry> {
    return anomalies
        .asSequence()
        .filter { entry ->
            entry.taxAmount.isFinite() &&
                entry.taxAmount > 0.0 &&
                entry.taxAmount <= AlgPosteSmsAnalyzer.MAX_CAUGHT_TAX_AMOUNT_DZD &&
                !isTaxAnomalyAlreadyLoggedInTransactions(entry, existingTransactions)
        }
        .distinctBy { it.id }
        .toList()
}

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

class SaleryPelseViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val dao = database.appDao()
    private val crypto = VaultCryptoManager.getInstance(application)
    private val prefsManager = PreferencesManager(application, crypto)

    private val _roomCryptoIssue = MutableStateFlow<CryptoIssue?>(null)

    val cryptoIssue: StateFlow<CryptoIssue?> = combine(
        prefsManager.cryptoIssue,
        _roomCryptoIssue
    ) { prefIssue, roomIssue ->
        when {
            prefIssue == null && roomIssue == null -> null
            prefIssue != null && roomIssue == null -> prefIssue
            prefIssue == null && roomIssue != null -> roomIssue
            else -> {
                val mergedType = listOf(prefIssue!!.type, roomIssue!!.type).minByOrNull { it.ordinal }
                    ?: prefIssue.type
                CryptoIssue(
                    type = mergedType,
                    affectedScopes = prefIssue.affectedScopes + roomIssue.affectedScopes,
                    timestampMillis = maxOf(prefIssue.timestampMillis, roomIssue.timestampMillis)
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), prefsManager.cryptoIssue.value)

    private fun reportRoomCryptoIssue(type: CryptoIssueType, scope: String) {
        val current = _roomCryptoIssue.value
        val nextType = if (current == null || type.ordinal < current.type.ordinal) type else current.type
        val nextScopes = (current?.affectedScopes ?: emptySet()) + scope
        _roomCryptoIssue.value = CryptoIssue(type = nextType, affectedScopes = nextScopes)
    }

    val settings: StateFlow<UserSettings> = prefsManager.settingsFlow

    val allTransactions: StateFlow<List<TransactionEntity>> = dao.getAllTransactions()
        .map { list -> list.map { crypto.decryptTransaction(it, ::reportRoomCryptoIssue) } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDebts: StateFlow<List<DebtEntity>> = dao.getAllDebts()
        .map { list -> list.map { crypto.decryptDebt(it, ::reportRoomCryptoIssue) } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinancialSummary())

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

    private val _revealedCcpIds = MutableStateFlow<Set<String>>(emptySet())
    val revealedCcpIds: StateFlow<Set<String>> = _revealedCcpIds.asStateFlow()

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

    // Extracted balance history timeline from ALG Poste / BaridiMob SMS messages
    val smsBalanceHistory: StateFlow<List<SmsBalanceSnapshot>> = settings
        .map { AlgPosteSmsCodec.parseBalanceHistory(it.smsBalanceHistoryJson) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Permanent non-deletable archive of every ALG Poste SMS (survives phone SMS deletion)
    val archivedAlgPosteSms: StateFlow<List<ArchivedAlgPosteSmsEntity>> = dao.getAllArchivedAlgPosteSms()
        .map { list -> list.map { crypto.decryptArchivedSms(it, ::reportRoomCryptoIssue) } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    init {
        // Initialize lock state based on biometric preference
        if (settings.value.biometricEnabled) {
            _isAppLocked.value = true
        }

        // Check Smart Salary Scheduler, Debt Auto-Payments, clean obsolete update APKs, and scan ALG Poste SMS
        // off the Main Thread after a brief yield so Frame 1 completes within the SurfaceSyncGroup budget.
        viewModelScope.launch(Dispatchers.IO) {
            delay(250L)
            try {
                AppUpdateManager.cleanupObsoleteUpdates(getApplication())
            } catch (_: Throwable) {
            }
            try {
                cleanExistingMockDataIfPresent()
                migrateLegacyPlaintextRoomRecords()
                checkAndApplySalaryScheduler()
                checkAndExecuteAutoPayDebts()
                val currentDebts = dao.getAllDebtsList()
                    .map { crypto.decryptDebt(it, ::reportRoomCryptoIssue) }
                DebtReminderScheduler.rescheduleAllReminders(getApplication(), currentDebts)
            } catch (_: Throwable) {
            }
            if (AlgPosteSmsInboxScanner.hasSmsPermissions(getApplication())) {
                val cfg = settings.value
                if (cfg.baridiMobSmsSyncEnabled) {
                    scanAlgPosteSmsInbox(getApplication(), showFeedback = false)
                } else {
                    refreshCurrentBalanceOnlyFromInbox(getApplication(), showFeedback = false)
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
                delay(CRYPTO_PRICE_REFRESH_INTERVAL_MILLIS)
            }
        }
    }

    private suspend fun cleanExistingMockDataIfPresent() {
        val prefs = getApplication<Application>().getSharedPreferences("salerypelse_prefs", Context.MODE_PRIVATE)
        val cleaned = prefs.getBoolean("mock_data_cleaned_permanent_v2", false)
        if (!cleaned) {
            val allTrans = dao.getAllTransactionsList().map { crypto.decryptTransaction(it) }
            val mockTitles = setOf(
                "Monthly Salary Deposit",
                "Supermarket & Groceries",
                "Sonelgaz Electricity & Gas",
                "UI/UX Mobile Design"
            )
            for (t in allTrans) {
                if (t.title in mockTitles) {
                    dao.deleteTransaction(t)
                }
            }

            val allDebts = dao.getAllDebtsList().map { crypto.decryptDebt(it) }
            val mockPersons = setOf("Mohamed Lamine", "Karim Benali")
            for (d in allDebts) {
                if (d.personName in mockPersons && (d.note.contains("spare parts") || d.note.contains("Laptop sale"))) {
                    dao.deleteDebt(d)
                }
            }
            prefs.edit().putBoolean("mock_data_cleaned_permanent_v2", true).apply()
        }
    }

    private suspend fun migrateLegacyPlaintextRoomRecords() {
        val transactionsToUpgrade = dao.getAllTransactionsList().mapNotNull { tx ->
            val needsTitle = tx.title.isNotBlank() && !crypto.isEncryptedEnvelope(tx.title)
            val needsNote = tx.note.isNotBlank() && !crypto.isEncryptedEnvelope(tx.note)
            if (needsTitle || needsNote) {
                crypto.encryptTransaction(tx, ::reportRoomCryptoIssue)
            } else {
                null
            }
        }
        if (transactionsToUpgrade.isNotEmpty()) {
            dao.insertTransactions(transactionsToUpgrade)
        }

        val debtsToUpgrade = dao.getAllDebtsList().mapNotNull { debt ->
            val needsPerson = debt.personName.isNotBlank() && !crypto.isEncryptedEnvelope(debt.personName)
            val needsNote = debt.note.isNotBlank() && !crypto.isEncryptedEnvelope(debt.note)
            if (needsPerson || needsNote) {
                crypto.encryptDebt(debt, ::reportRoomCryptoIssue)
            } else {
                null
            }
        }
        if (debtsToUpgrade.isNotEmpty()) {
            dao.insertDebts(debtsToUpgrade)
        }

        val smsToUpgrade = dao.getAllArchivedAlgPosteSmsList().mapNotNull { sms ->
            if (sms.rawBody.isNotBlank() && !crypto.isEncryptedEnvelope(sms.rawBody)) {
                crypto.encryptArchivedSms(sms, ::reportRoomCryptoIssue)
            } else {
                null
            }
        }
        if (smsToUpgrade.isNotEmpty()) {
            dao.updateArchivedSmsAnalysis(smsToUpgrade)
        }
    }

    fun retryVaultCryptoRecovery() {
        viewModelScope.launch(Dispatchers.IO) {
            _roomCryptoIssue.value = null
            prefsManager.retryCryptoRecovery()
            migrateLegacyPlaintextRoomRecords()
            dao.getAllTransactionsList().forEach { crypto.decryptTransaction(it, ::reportRoomCryptoIssue) }
            dao.getAllDebtsList().forEach { crypto.decryptDebt(it, ::reportRoomCryptoIssue) }
            dao.getAllArchivedAlgPosteSmsList().forEach { crypto.decryptArchivedSms(it, ::reportRoomCryptoIssue) }
            val isAr = settings.value.language == "ar"
            val stillHasIssue = cryptoIssue.value != null
            _toastEvent.emit(
                if (!stillHasIssue) {
                    if (isAr) "تمت استعادة مفاتيح الخزنة بنجاح ✓" else "Vault protection restored ✓"
                } else {
                    if (isAr) "لا تزال بعض السجلات متعذرة القراءة" else "Some protected items still could not be unlocked"
                }
            )
        }
    }

    fun resetCorruptedVaultData() {
        viewModelScope.launch(Dispatchers.IO) {
            prefsManager.resetCorruptedEncryptedEntries(resetMasterKeyIfInvalidated = true)

            val repairedTxs = dao.getAllTransactionsList().mapNotNull { tx ->
                val titleOut = crypto.decryptField(tx.title, VaultCryptoManager.AAD_ROOM_TX_TITLE)
                val noteOut = crypto.decryptField(tx.note, VaultCryptoManager.AAD_ROOM_TX_NOTE)
                val badTitle = titleOut is DecryptOutcome.Failure
                val badNote = noteOut is DecryptOutcome.Failure
                if (badTitle || badNote) {
                    val fixedTitle = if (badTitle) "" else (titleOut as DecryptOutcome.Success).plaintext
                    val fixedNote = if (badNote) "" else (noteOut as DecryptOutcome.Success).plaintext
                    crypto.encryptTransaction(tx.copy(title = fixedTitle, note = fixedNote), ::reportRoomCryptoIssue)
                } else {
                    null
                }
            }
            if (repairedTxs.isNotEmpty()) {
                dao.insertTransactions(repairedTxs)
            }

            val repairedDebts = dao.getAllDebtsList().mapNotNull { debt ->
                val personOut = crypto.decryptField(debt.personName, VaultCryptoManager.AAD_ROOM_DEBT_PERSON)
                val noteOut = crypto.decryptField(debt.note, VaultCryptoManager.AAD_ROOM_DEBT_NOTE)
                val badPerson = personOut is DecryptOutcome.Failure
                val badNote = noteOut is DecryptOutcome.Failure
                if (badPerson || badNote) {
                    val fixedPerson = if (badPerson) "" else (personOut as DecryptOutcome.Success).plaintext
                    val fixedNote = if (badNote) "" else (noteOut as DecryptOutcome.Success).plaintext
                    crypto.encryptDebt(debt.copy(personName = fixedPerson, note = fixedNote), ::reportRoomCryptoIssue)
                } else {
                    null
                }
            }
            if (repairedDebts.isNotEmpty()) {
                dao.insertDebts(repairedDebts)
            }

            val repairedSms = dao.getAllArchivedAlgPosteSmsList().mapNotNull { sms ->
                val bodyOut = crypto.decryptField(sms.rawBody, VaultCryptoManager.AAD_ROOM_SMS_BODY)
                if (bodyOut is DecryptOutcome.Failure) {
                    sms.copy(rawBody = "")
                } else {
                    null
                }
            }
            if (repairedSms.isNotEmpty()) {
                dao.updateArchivedSmsAnalysis(repairedSms)
            }

            _roomCryptoIssue.value = null
            val isAr = settings.value.language == "ar"
            _toastEvent.emit(
                if (isAr) "تمت تصفية العناصر غير القابلة للقراءة بأمان ✓"
                else "Unreadable entries cleared safely ✓"
            )
        }
    }

    /**
     * Smart Salary Scheduler:
     * Checks if current day >= salary payday for the current month.
     * If not already credited this month, credits wallet balance automatically.
     */
    suspend fun checkAndApplySalaryScheduler(force: Boolean = false) {
        val userConfig = settings.value
        if (!userConfig.salaryAutoCredit && !force) return

        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)
        val currentMonth = cal.get(Calendar.MONTH) + 1 // 1-12
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        val monthYearKey = "$currentYear-$currentMonth"

        if (force || (currentDay >= userConfig.salaryPayday && userConfig.lastCreditedMonthYear != monthYearKey)) {
            dao.insertTransaction(
                crypto.encryptTransaction(
                    TransactionEntity(
                        type = TransactionType.INCOME,
                        amount = userConfig.salaryAmount,
                        category = "salary",
                        title = "${userConfig.salaryEmployer} Salary",
                        note = "Smart Salary Scheduler automatic deposit ($monthYearKey)",
                        dateMillis = System.currentTimeMillis(),
                        isSalaryCredit = true
                    ),
                    ::reportRoomCryptoIssue
                )
            )
            prefsManager.updateSettings {
                it.copy(lastCreditedMonthYear = monthYearKey)
            }
            _toastEvent.emit("Salary of ${userConfig.salaryAmount} DZD credited automatically!")
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

        val autoDebts = dao.getPendingAutoPayDebts()
            .map { crypto.decryptDebt(it, ::reportRoomCryptoIssue) }

        for (debt in autoDebts) {
            if (debt.isRecurringMonthlyInstallment) {
                // Monthly recurring installment check
                if (currentDay >= debt.installmentDayOfMonth && debt.lastInstallmentCreditedMonth != monthYearKey) {
                    val payNow = if (debt.monthlyInstallmentAmount > 0) {
                        minOf(debt.monthlyInstallmentAmount, debt.remainingAmount)
                    } else debt.remainingAmount

                    val newRemaining = (debt.remainingAmount - payNow).coerceAtLeast(0.0)
                    val isFullySettled = newRemaining <= 0.001

                    dao.updateDebt(
                        crypto.encryptDebt(
                            debt.copy(
                                remainingAmount = newRemaining,
                                lastInstallmentCreditedMonth = monthYearKey,
                                isPaid = isFullySettled,
                                paidDateMillis = if (isFullySettled) now else null
                            ),
                            ::reportRoomCryptoIssue
                        )
                    )

                    if (debt.type == DebtType.OWED_BY_ME) {
                        dao.insertTransaction(
                            crypto.encryptTransaction(
                                TransactionEntity(
                                    type = TransactionType.EXPENSE,
                                    amount = payNow,
                                    category = "debt_payment",
                                    title = "Auto-Installment: ${debt.personName}",
                                    note = "Monthly auto-installment ($monthYearKey) - Rem: ${newRemaining.toLong()} DZD",
                                    dateMillis = now
                                ),
                                ::reportRoomCryptoIssue
                            )
                        )
                    } else {
                        dao.insertTransaction(
                            crypto.encryptTransaction(
                                TransactionEntity(
                                    type = TransactionType.INCOME,
                                    amount = payNow,
                                    category = "debt_collection",
                                    title = "Auto-Collected Installment: ${debt.personName}",
                                    note = "Monthly auto-installment ($monthYearKey) - Rem: ${newRemaining.toLong()} DZD",
                                    dateMillis = now
                                ),
                                ::reportRoomCryptoIssue
                            )
                        )
                    }
                    _toastEvent.emit("Auto-installment of $payNow DZD processed for ${debt.personName}")
                }
            } else {
                // One-time full debt settlement on due date
                if (debt.dueDateMillis <= now) {
                    val payAmount = if (debt.remainingAmount > 0) debt.remainingAmount else debt.amount
                    dao.updateDebt(
                        crypto.encryptDebt(
                            debt.copy(
                                remainingAmount = 0.0,
                                isPaid = true,
                                paidDateMillis = now
                            ),
                            ::reportRoomCryptoIssue
                        )
                    )
                    if (debt.type == DebtType.OWED_BY_ME) {
                        dao.insertTransaction(
                            crypto.encryptTransaction(
                                TransactionEntity(
                                    type = TransactionType.EXPENSE,
                                    amount = payAmount,
                                    category = "debt_payment",
                                    title = "Auto-Paid Debt: ${debt.personName}",
                                    note = "Automatic debt execution on due date (${debt.note})",
                                    dateMillis = now
                                ),
                                ::reportRoomCryptoIssue
                            )
                        )
                    } else {
                        dao.insertTransaction(
                            crypto.encryptTransaction(
                                TransactionEntity(
                                    type = TransactionType.INCOME,
                                    amount = payAmount,
                                    category = "debt_collection",
                                    title = "Auto-Received Debt: ${debt.personName}",
                                    note = "Automatic debt collection on due date (${debt.note})",
                                    dateMillis = now
                                ),
                                ::reportRoomCryptoIssue
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
            dao.insertTransaction(
                crypto.encryptTransaction(
                    TransactionEntity(
                        type = type,
                        amount = amount,
                        category = category,
                        title = title,
                        note = note,
                        dateMillis = dateMillis
                    ),
                    ::reportRoomCryptoIssue
                )
            )
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            dao.deleteTransaction(transaction)
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
                        vaultLockedAmount = updated,
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
            val newId = dao.insertDebt(crypto.encryptDebt(newEntity, ::reportRoomCryptoIssue))
            if (!isSettled) {
                DebtReminderScheduler.scheduleDebtReminder(getApplication(), newEntity.copy(id = newId))
            }
            _toastEvent.emit("Debt recorded successfully")
        }
    }

    fun markDebtAsPaid(debt: DebtEntity) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val payAmount = if (debt.remainingAmount > 0) debt.remainingAmount else debt.amount
            dao.updateDebt(
                crypto.encryptDebt(
                    debt.copy(
                        remainingAmount = 0.0,
                        isPaid = true,
                        paidDateMillis = now
                    ),
                    ::reportRoomCryptoIssue
                )
            )
            DebtReminderScheduler.cancelDebtReminder(getApplication(), debt.id)
            // Record wallet impact
            if (debt.type == DebtType.OWED_BY_ME) {
                dao.insertTransaction(
                    crypto.encryptTransaction(
                        TransactionEntity(
                            type = TransactionType.EXPENSE,
                            amount = payAmount,
                            category = "debt_payment",
                            title = "Paid Debt: ${debt.personName}",
                            note = "Manual settlement: ${debt.note}",
                            dateMillis = now
                        ),
                        ::reportRoomCryptoIssue
                    )
                )
            } else {
                dao.insertTransaction(
                    crypto.encryptTransaction(
                        TransactionEntity(
                            type = TransactionType.INCOME,
                            amount = payAmount,
                            category = "debt_collection",
                            title = "Collected Debt: ${debt.personName}",
                            note = "Manual settlement: ${debt.note}",
                            dateMillis = now
                        ),
                        ::reportRoomCryptoIssue
                    )
                )
            }
            _toastEvent.emit("Debt marked as paid and wallet updated")
        }
    }

    fun updateDebt(debt: DebtEntity) {
        viewModelScope.launch {
            dao.updateDebt(crypto.encryptDebt(debt, ::reportRoomCryptoIssue))
            if (debt.isPaid || debt.remainingAmount <= 0.001) {
                DebtReminderScheduler.cancelDebtReminder(getApplication(), debt.id)
            } else {
                DebtReminderScheduler.scheduleDebtReminder(getApplication(), debt)
            }
            _toastEvent.emit("Debt updated successfully")
        }
    }

    fun makePartialPayment(debt: DebtEntity, paymentAmount: Double, recordTransaction: Boolean = true) {
        viewModelScope.launch {
            if (!paymentAmount.isFinite() || paymentAmount <= 0.0) return@launch
            val now = System.currentTimeMillis()
            val actualPay = minOf(paymentAmount, debt.remainingAmount)
            val newRemaining = (debt.remainingAmount - actualPay).coerceAtLeast(0.0)
            val isFullySettled = newRemaining <= 0.001

            val updatedDebt = debt.copy(
                remainingAmount = newRemaining,
                isPaid = isFullySettled,
                paidDateMillis = if (isFullySettled) now else debt.paidDateMillis
            )
            dao.updateDebt(crypto.encryptDebt(updatedDebt, ::reportRoomCryptoIssue))
            if (isFullySettled) {
                DebtReminderScheduler.cancelDebtReminder(getApplication(), debt.id)
            } else {
                DebtReminderScheduler.scheduleDebtReminder(getApplication(), updatedDebt)
            }

            if (recordTransaction) {
                if (debt.type == DebtType.OWED_BY_ME) {
                    dao.insertTransaction(
                        crypto.encryptTransaction(
                            TransactionEntity(
                                type = TransactionType.EXPENSE,
                                amount = actualPay,
                                category = "debt_payment",
                                title = "Partial Payment: ${debt.personName}",
                                note = if (isFullySettled) "Final settlement (${debt.note})" 
                                       else "Partial payment - Rem: ${newRemaining.toLong()} DZD (${debt.note})",
                                dateMillis = now
                            ),
                            ::reportRoomCryptoIssue
                        )
                    )
                } else {
                    dao.insertTransaction(
                        crypto.encryptTransaction(
                            TransactionEntity(
                                type = TransactionType.INCOME,
                                amount = actualPay,
                                category = "debt_collection",
                                title = "Partial Collection: ${debt.personName}",
                                note = if (isFullySettled) "Final collection (${debt.note})"
                                       else "Partial collection - Rem: ${newRemaining.toLong()} DZD (${debt.note})",
                                dateMillis = now
                            ),
                            ::reportRoomCryptoIssue
                        )
                    )
                }
            }

            if (isFullySettled) {
                _toastEvent.emit("Debt with ${debt.personName} fully settled!")
            } else {
                _toastEvent.emit("Payment of ${actualPay.toLong()} DZD recorded. Remaining: ${newRemaining.toLong()} DZD")
            }
        }
    }

    fun deleteDebt(debt: DebtEntity) {
        viewModelScope.launch {
            dao.deleteDebt(debt)
            DebtReminderScheduler.cancelDebtReminder(getApplication(), debt.id)
            _toastEvent.emit("Debt record removed")
        }
    }

    fun clearSettledDebts() {
        viewModelScope.launch {
            val count = dao.clearSettledDebts()
            _toastEvent.emit("$count settled debts deleted")
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
        val cleanName = holderName.trim().ifBlank { "CHARFEDDINE KA" }
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
        val cleanName = holderName.trim().ifBlank { "CHARFEDDINE KA" }.uppercase()
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
                "REDOTPAY" -> "RedotPay"
                "BYBIT" -> "Bybit"
                "BINANCE" -> "Binance"
                else -> "Visa"
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
        val cleanName = holderName.trim().ifBlank { "CHARFEDDINE KA" }.uppercase()
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
                "REDOTPAY" -> "RedotPay"
                "BYBIT" -> "Bybit"
                "BINANCE" -> "Binance"
                else -> "Visa"
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

    // Pattern Lock Security
    fun unlockApp(pattern: List<Int>): Boolean {
        val hash = pattern.toPatternHash()
        if (hash == settings.value.patternLockHash) {
            _isAppLocked.value = false
            return true
        }
        return false
    }

    fun lockApp() {
        collapseAllVaultContainersForSecurity()
        if (settings.value.patternLockEnabled && settings.value.patternLockHash.isNotBlank()) {
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
        _revealedCcpIds.value = emptySet()
    }

    fun toggleCcpRevealed(accountId: String) {
        _revealedCcpIds.value = if (accountId in _revealedCcpIds.value) {
            _revealedCcpIds.value - accountId
        } else {
            _revealedCcpIds.value + accountId
        }
    }

    fun collapseAllVaultContainersForSecurity() {
        _expandedKeyIds.value = emptySet()
        _revealedKeyIds.value = emptySet()
        _expandedCcpIds.value = emptySet()
        _revealedCcpIds.value = emptySet()
    }

    fun saveNewPattern(pattern: List<Int>) {
        val hash = pattern.toPatternHash()
        prefsManager.updateSettings {
            it.copy(
                patternLockEnabled = true,
                patternLockHash = hash
            )
        }
        _isAppLocked.value = false
    }

    fun disablePatternLock() {
        prefsManager.updateSettings {
            it.copy(
                patternLockEnabled = false,
                patternLockHash = ""
            )
        }
        _isAppLocked.value = false
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefsManager.updateSettings { it.copy(biometricEnabled = enabled) }
    }

    fun unlockAppByBiometrics() {
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
        _revealedCcpIds.value = _revealedCcpIds.value - accountId
        prefsManager.updateSettings { current ->
            val updated = HubVaultCodec.parseCcpAccounts(current.ccpAccountsJson).filterNot { it.id == accountId }
            current.copy(ccpAccountsJson = HubVaultCodec.encodeCcpAccounts(updated))
        }
    }

    // Stealth Mode (Privacy / Shake to Hide)
    fun toggleStealthMode(): Boolean {
        val current = settings.value.stealthMode
        val next = !current
        if (next) {
            collapseAllVaultContainersForSecurity()
        }
        prefsManager.updateSettings { it.copy(stealthMode = next) }
        viewModelScope.launch {
            if (next) {
                _toastEvent.emit("Stealth Mode Active • Financial amounts masked")
            } else {
                _toastEvent.emit("Stealth Mode Disabled • Amounts revealed")
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
                vaultLockedAmount = updated,
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
                vaultLockedAmount = updated,
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
                vaultLockedAmount = updated,
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

            dao.insertTransaction(
                crypto.encryptTransaction(
                    TransactionEntity(
                        type = TransactionType.EXPENSE,
                        amount = amount,
                        category = category.ifBlank { "savings_expense" },
                        title = resolvedTitle,
                        note = savingsTagNote,
                        dateMillis = System.currentTimeMillis(),
                        isPaidFromSavings = true
                    ),
                    ::reportRoomCryptoIssue
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

    // Backup & Restore
    suspend fun exportBackupJson(): String {
        val trans = dao.getAllTransactionsList()
            .map { crypto.decryptTransaction(it, ::reportRoomCryptoIssue) }
        val debts = dao.getAllDebtsList()
            .map { crypto.decryptDebt(it, ::reportRoomCryptoIssue) }
        val archivedSms = dao.getAllArchivedAlgPosteSmsList()
            .map { crypto.decryptArchivedSms(it, ::reportRoomCryptoIssue) }
        val userConfig = settings.value

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
            archivedAlgPosteSms = archivedSms
        )

        prefsManager.updateSettings {
            it.copy(lastBackupTimestamp = System.currentTimeMillis())
        }
        return payload.toJsonString()
    }

    suspend fun restoreFromJson(jsonStr: String): Boolean {
        val payload = BackupPayload.fromJsonString(jsonStr) ?: return false
        dao.clearAllTransactions()
        dao.clearAllDebts()

        dao.insertTransactions(payload.transactions.map { crypto.encryptTransaction(it, ::reportRoomCryptoIssue) })
        dao.insertDebts(payload.debts.map { crypto.encryptDebt(it, ::reportRoomCryptoIssue) })
        if (payload.archivedAlgPosteSms.isNotEmpty()) {
            // Merge into permanent archive without deleting existing permanent SMS records
            dao.insertArchivedSmsIfMissing(
                payload.archivedAlgPosteSms.map { crypto.encryptArchivedSms(it, ::reportRoomCryptoIssue) }
            )
        }

        prefsManager.updateSettings {
            it.copy(
                salaryAmount = payload.salaryAmount,
                salaryPayday = payload.salaryPayday,
                salaryEmployer = payload.salaryEmployer,
                classicCardHolderName = payload.classicCardHolderName,
                classicCardNumber = payload.classicCardNumber,
                classicCardExpiry = payload.classicCardExpiry,
                classicCardCvc = payload.classicCardCvc,
                lastBackupTimestamp = System.currentTimeMillis()
            )
        }
        reanalyzePermanentSmsArchive(showFeedback = false)
        _toastEvent.emit("Restored ${payload.transactions.size} transactions and ${payload.debts.size} debts")
        return true
    }

    private var lastUpdateCheckElapsedMillis: Long = 0L
    private var dismissedUpdateVersionTag: String? = null

    // In-App GitHub Releases Optional Update & Download Actions
    fun checkForUpdates(manual: Boolean = true) {
        if (manual) {
            dismissedUpdateVersionTag = null
        }

        lastUpdateCheckElapsedMillis = System.currentTimeMillis()
        viewModelScope.launch {
            if (manual) {
                _updateUiState.value = AppUpdateUiState.Checking
                _showUpdateDialog.value = true
            }

            val result = AppUpdateManager.fetchLatestRelease()
            result.onSuccess { release ->
                val isNewer = compareVersionTags(release.versionName, APP_VERSION) > 0
                if (isNewer) {
                    val alreadyDownloadedApk = AppUpdateManager.findReadyUpdateApk(getApplication(), release)
                    _updateUiState.value = if (alreadyDownloadedApk != null) {
                        AppUpdateUiState.ReadyToInstall(release, alreadyDownloadedApk)
                    } else {
                        AppUpdateUiState.Available(release)
                    }
                    if (manual || dismissedUpdateVersionTag != release.tagName) {
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
                        ?: if (isAr) "تعذر الاتصال بـ GitHub" else "Unable to reach GitHub"
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

    fun startUpdateDownload() {
        val release = when (val current = _updateUiState.value) {
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
            _updateUiState.value = AppUpdateUiState.Error(msg, release)
            return
        }

        downloadJob?.cancel()
        downloadJob = viewModelScope.launch {
            val appCtx = getApplication<Application>()
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
                        _updateUiState.value = AppUpdateUiState.ReadyToInstall(
                            release = release,
                            apkFile = dlState.file
                        )
                    }
                    is DownloadState.Failed -> {
                        _updateUiState.value = AppUpdateUiState.Error(
                            message = dlState.errorMessage,
                            release = release
                        )
                    }
                }
            }
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
        _updateUiState.value = AppUpdateUiState.Error(msg, currentRelease)
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
            _updateUiState.value = AppUpdateUiState.Error(msg, current.release)
        }
        return launched
    }

    fun dismissUpdateDialog() {
        val releaseTag = when (val current = _updateUiState.value) {
            is AppUpdateUiState.Available -> current.release.tagName
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
        prefsManager.updateSettings { it.copy(smsSyncBaselineTimestampMillis = 0L) }
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
                        if (isAr) "يرجى منح صلاحية قراءة الرسائل SMS أولاً"
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
                    dao.insertArchivedSmsIfMissing(
                        toArchive.map { crypto.encryptArchivedSms(it, ::reportRoomCryptoIssue) }
                    )
                }
                val liveSnaps = AlgPosteSmsInboxScanner.scanAllBalanceSnapshots(context)
                val permanentSnaps = AlgPosteSmsAnalyzer.reconstructSnapshotsFromArchive(
                    dao.getAllArchivedAlgPosteSmsList()
                        .map { crypto.decryptArchivedSms(it, ::reportRoomCryptoIssue) }
                )
                liveSnaps to permanentSnaps
            }
            if (snapshots.isEmpty() && archivedSnapshots.isEmpty()) {
                if (showFeedback) {
                    _toastEvent.emit(
                        if (isAr) "لم يتم العثور على أرصدة في رسائل ALG Poste / BaridiMob"
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

                    val existingTaxes = AlgPosteSmsCodec.parseTaxAnomalies(current.detectedSmsTaxesJson)
                    val updatedTaxes = AlgPosteSmsAnalyzer.detectTaxAnomalies(
                        snapshots = mergedHistory,
                        existingAnomalies = existingTaxes
                    )

                    val updatedPendingJson = if (current.baridiMobSmsSyncEnabled) {
                        val processed = AlgPosteSmsCodec.parseProcessedKeys(current.processedSmsKeysJson)
                        val existingPending = AlgPosteSmsCodec.parseSuggestions(current.pendingSmsSuggestionsJson)
                        val existingBaseIds = existingPending.map { it.id.removePrefix("bal_op_") }.toSet()
                        val deltaOps = AlgPosteSmsAnalyzer.detectBalanceOperationSuggestions(
                            snapshots = mergedHistory,
                            minTimestampMillis = current.smsSyncBaselineTimestampMillis
                        ).filter { op ->
                            val baseKey = op.id.removePrefix("bal_op_")
                            op.id !in processed &&
                                baseKey !in processed &&
                                "bal_op_$baseKey" !in processed &&
                                baseKey !in existingBaseIds
                        }
                        if (deltaOps.isEmpty()) {
                            current.pendingSmsSuggestionsJson
                        } else {
                            AlgPosteSmsCodec.encodeSuggestions(
                                (deltaOps + existingPending).distinctBy { it.id.removePrefix("bal_op_") }
                            )
                        }
                    } else {
                        current.pendingSmsSuggestionsJson
                    }

                    current.copy(
                        pendingSmsSuggestionsJson = updatedPendingJson,
                        latestSmsBalanceAmount = latestSnap?.balance ?: current.latestSmsBalanceAmount,
                        latestSmsBalanceUpdatedAt = latestSnap?.timestampMillis ?: current.latestSmsBalanceUpdatedAt,
                        latestSmsBalanceRawSnippet = latestSnap?.rawBody ?: current.latestSmsBalanceRawSnippet,
                        smsBalanceHistoryJson = AlgPosteSmsCodec.encodeBalanceHistory(mergedHistory),
                        detectedSmsTaxesJson = AlgPosteSmsCodec.encodeTaxAnomalies(updatedTaxes)
                    )
                }
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
                val existing = dao.getAllArchivedAlgPosteSmsList()
                    .map { crypto.decryptArchivedSms(it, ::reportRoomCryptoIssue) }
                val updated = existing.map { AlgPosteSmsAnalyzer.reanalyzeArchivedEntity(it) }
                if (updated.isNotEmpty()) {
                    dao.updateArchivedSmsAnalysis(
                        updated.map { crypto.encryptArchivedSms(it, ::reportRoomCryptoIssue) }
                    )
                }
                updated
            }
            if (reanalyzed.isEmpty()) {
                if (showFeedback) {
                    _toastEvent.emit(
                        if (isAr) "السجل الدائم لرسائل ALG Poste فارغ حالياً"
                        else "Permanent ALG Poste SMS archive is currently empty"
                    )
                }
                return@launch
            }

            val totalTaxes = withContext(Dispatchers.IO) {
                val reconstructedSnaps = AlgPosteSmsAnalyzer.reconstructSnapshotsFromArchive(reanalyzed)
                var taxesCount = 0
                prefsManager.updateSettings { current ->
                    val existingHistory = AlgPosteSmsCodec.parseBalanceHistory(current.smsBalanceHistoryJson)
                    val mergedHistory = (existingHistory + reconstructedSnaps)
                        .distinctBy { it.id }
                        .sortedBy { it.timestampMillis }
                    val latestSnap = mergedHistory.maxByOrNull { it.timestampMillis }
                    val existingTaxes = AlgPosteSmsCodec.parseTaxAnomalies(current.detectedSmsTaxesJson)
                    val updatedTaxes = AlgPosteSmsAnalyzer.detectTaxAnomalies(
                        snapshots = mergedHistory,
                        existingAnomalies = existingTaxes
                    )
                    taxesCount = updatedTaxes.size

                    current.copy(
                        smsBalanceHistoryJson = AlgPosteSmsCodec.encodeBalanceHistory(mergedHistory),
                        detectedSmsTaxesJson = AlgPosteSmsCodec.encodeTaxAnomalies(updatedTaxes),
                        latestSmsBalanceAmount = latestSnap?.balance ?: current.latestSmsBalanceAmount,
                        latestSmsBalanceUpdatedAt = latestSnap?.timestampMillis ?: current.latestSmsBalanceUpdatedAt,
                        latestSmsBalanceRawSnippet = latestSnap?.rawBody ?: current.latestSmsBalanceRawSnippet
                    )
                }
                taxesCount
            }

            if (showFeedback) {
                _toastEvent.emit(
                    if (isAr) "تمت إعادة دراسة ${reanalyzed.size} رسالة دائمة • رصد $totalTaxes ضرائب (≤ 1,000 دج)"
                    else "Re-studied ${reanalyzed.size} permanent SMS • $totalTaxes caught taxes (≤ 1,000 DA)"
                )
            }
        }
    }

    fun scanAlgPosteSmsInbox(context: Context = getApplication(), showFeedback: Boolean = true) {
        viewModelScope.launch {
            val isAr = settings.value.language == "ar"
            if (!AlgPosteSmsInboxScanner.hasSmsPermissions(context)) {
                if (showFeedback) {
                    _toastEvent.emit(
                        if (isAr) "يرجى منح صلاحية قراءة الرسائل SMS أولاً"
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
                    dao.insertArchivedSmsIfMissing(
                        toArchive.map { crypto.encryptArchivedSms(it, ::reportRoomCryptoIssue) }
                    )
                }
                val permanentSnaps = AlgPosteSmsAnalyzer.reconstructSnapshotsFromArchive(
                    dao.getAllArchivedAlgPosteSmsList()
                        .map { crypto.decryptArchivedSms(it, ::reportRoomCryptoIssue) }
                )
                res to permanentSnaps
            }

            var addedCount = 0
            var newTaxCount = 0
            var newSnapshotCount = 0
            withContext(Dispatchers.IO) {
                prefsManager.updateSettings { current ->
                    val processed = AlgPosteSmsCodec.parseProcessedKeys(current.processedSmsKeysJson)
                    val existing = AlgPosteSmsCodec.parseSuggestions(current.pendingSmsSuggestionsJson)
                    val existingBaseIds = existing.map { it.id.removePrefix("bal_op_") }.toSet()

                    val existingHistory = AlgPosteSmsCodec.parseBalanceHistory(current.smsBalanceHistoryJson)
                    val mergedHistory = (existingHistory + archivedSnapshots + scanResult.snapshots)
                        .distinctBy { it.id }
                        .sortedBy { it.timestampMillis }
                    newSnapshotCount = (mergedHistory.size - existingHistory.size).coerceAtLeast(0)
                    val latestSnap = mergedHistory.maxByOrNull { it.timestampMillis }

                    val newPending = if (current.baridiMobSmsSyncEnabled) {
                        val deltaOperations = AlgPosteSmsAnalyzer.detectBalanceOperationSuggestions(
                            snapshots = mergedHistory,
                            minTimestampMillis = current.smsSyncBaselineTimestampMillis
                        )
                        (scanResult.suggestions + deltaOperations)
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
                        existingAnomalies = existingTaxes
                    )
                    newTaxCount = (updatedTaxes.size - existingTaxes.size).coerceAtLeast(0)

                    current.copy(
                        pendingSmsSuggestionsJson = if (newPending.isEmpty()) current.pendingSmsSuggestionsJson
                        else AlgPosteSmsCodec.encodeSuggestions(
                            (newPending + existing).distinctBy { it.id.removePrefix("bal_op_") }
                        ),
                        smsBalanceHistoryJson = AlgPosteSmsCodec.encodeBalanceHistory(mergedHistory),
                        detectedSmsTaxesJson = AlgPosteSmsCodec.encodeTaxAnomalies(updatedTaxes),
                        latestSmsBalanceAmount = latestSnap?.balance ?: current.latestSmsBalanceAmount,
                        latestSmsBalanceUpdatedAt = latestSnap?.timestampMillis ?: current.latestSmsBalanceUpdatedAt,
                        latestSmsBalanceRawSnippet = latestSnap?.rawBody ?: current.latestSmsBalanceRawSnippet
                    )
                }
            }

            if (showFeedback) {
                val msg = when {
                    addedCount > 0 && newTaxCount > 0 -> if (isAr) {
                        "تم اكتشاف $addedCount معاملة و $newTaxCount فارق ضرائب/رسوم!"
                    } else {
                        "Found $addedCount transactions & $newTaxCount tax/fee discrepancies!"
                    }
                    addedCount > 0 -> if (isAr) {
                        "تم اكتشاف $addedCount معاملة جديدة من رسائل ALG Poste"
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
                        else "لم يتم العثور على رسائل معاملات من ALG Poste في صندوق الوارد"
                    } else {
                        if (baselineMillis > 0L) "No new ALG Poste SMS since Fresh Start"
                        else "No ALG Poste transaction messages found in SMS inbox"
                    }
                    else -> if (isAr) {
                        "جميع رسائل وأرصدة ALG Poste محدثة بالفعل"
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
                    dao.insertArchivedSmsIfMissing(
                        listOf(crypto.encryptArchivedSms(archivedEntity, ::reportRoomCryptoIssue))
                    )
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

            val caughtNewTaxAmount = withContext(Dispatchers.IO) {
                var taxAmt = 0.0
                prefsManager.updateSettings { current ->
                    val existing = AlgPosteSmsCodec.parseSuggestions(current.pendingSmsSuggestionsJson)
                    val existingHistory = AlgPosteSmsCodec.parseBalanceHistory(current.smsBalanceHistoryJson)
                    val mergedHistory = if (snapshot != null) {
                        (existingHistory + snapshot).distinctBy { it.id }.sortedBy { it.timestampMillis }
                    } else existingHistory
                    val latestSnap = mergedHistory.maxByOrNull { it.timestampMillis }

                    val manualDeltaSuggestion = if (parsed == null && snapshot != null) {
                        AlgPosteSmsAnalyzer.detectBalanceOperationSuggestions(mergedHistory)
                            .firstOrNull { it.id.removePrefix("bal_op_") == snapshot.id }
                    } else null
                    val effectiveSuggestion = parsed ?: manualDeltaSuggestion

                    val updatedPending = if (effectiveSuggestion != null) {
                        val base = effectiveSuggestion.id.removePrefix("bal_op_")
                        listOf(effectiveSuggestion) + existing.filterNot { it.id.removePrefix("bal_op_") == base }
                    } else existing

                    val existingTaxes = AlgPosteSmsCodec.parseTaxAnomalies(current.detectedSmsTaxesJson)
                    val updatedTaxes = AlgPosteSmsAnalyzer.detectTaxAnomalies(
                        snapshots = mergedHistory,
                        existingAnomalies = existingTaxes
                    )
                    val prevTaxIds = existingTaxes.map { it.id }.toSet()
                    taxAmt = updatedTaxes.firstOrNull { it.id !in prevTaxIds }?.taxAmount ?: 0.0

                    current.copy(
                        pendingSmsSuggestionsJson = AlgPosteSmsCodec.encodeSuggestions(updatedPending),
                        smsBalanceHistoryJson = AlgPosteSmsCodec.encodeBalanceHistory(mergedHistory),
                        detectedSmsTaxesJson = AlgPosteSmsCodec.encodeTaxAnomalies(updatedTaxes),
                        latestSmsBalanceAmount = latestSnap?.balance ?: current.latestSmsBalanceAmount,
                        latestSmsBalanceUpdatedAt = latestSnap?.timestampMillis ?: current.latestSmsBalanceUpdatedAt,
                        latestSmsBalanceRawSnippet = latestSnap?.rawBody ?: current.latestSmsBalanceRawSnippet
                    )
                }
                taxAmt
            }

            val msg = buildString {
                if (parsed != null) {
                    val kindLabel = if (parsed.isExpense) {
                        if (isAr) "مصروف (Expense)" else "Expense"
                    } else {
                        if (isAr) "دخل (Income)" else "Income"
                    }
                    val amtLabel = formatDzdAmount(parsed.amount, if (isAr) "دج" else "DA")
                    append(if (isAr) "$kindLabel: $amtLabel" else "$kindLabel: $amtLabel")
                } else if (snapshot != null) {
                    val balLabel = formatDzdAmount(snapshot.balance, if (isAr) "دج" else "DA")
                    append(if (isAr) "الرصيد المستخرج: $balLabel" else "Balance extracted: $balLabel")
                }
                if (caughtNewTaxAmount > 0.0) {
                    val taxLabel = formatDzdAmount(caughtNewTaxAmount, if (isAr) "دج" else "DA")
                    append(if (isAr) " • تم تصيّد ضرائب/فارق: $taxLabel!" else " • Caught fee/tax: $taxLabel!")
                }
            }
            _toastEvent.emit(msg)
        }
    }

    private val isAutoAddingTaxes = java.util.concurrent.atomic.AtomicBoolean(false)

    fun autoAddUnloggedTaxes(context: Context = getApplication()) {
        if (!isAutoAddingTaxes.compareAndSet(false, true)) return
        viewModelScope.launch {
            try {
                val isAr = settings.value.language == "ar"
                val summaryOutcome = withContext(Dispatchers.IO) {
                    // 1. Refresh snapshots from live SMS inbox (if granted) + permanent archive
                    val liveSnaps = if (AlgPosteSmsInboxScanner.hasSmsPermissions(context)) {
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
                            dao.insertArchivedSmsIfMissing(
                                toArchive.map { crypto.encryptArchivedSms(it, ::reportRoomCryptoIssue) }
                            )
                        }
                        AlgPosteSmsInboxScanner.scanAllBalanceSnapshots(context)
                    } else {
                        emptyList()
                    }
                    val archivedSnaps = AlgPosteSmsAnalyzer.reconstructSnapshotsFromArchive(
                        dao.getAllArchivedAlgPosteSmsList()
                            .map { crypto.decryptArchivedSms(it, ::reportRoomCryptoIssue) }
                    )

                    // 2. Detect & merge tax anomalies against balance timeline
                    var mergedTaxes: List<SmsTaxAnomalyEntry> = emptyList()
                    prefsManager.updateSettings { current ->
                        val existingHistory = AlgPosteSmsCodec.parseBalanceHistory(current.smsBalanceHistoryJson)
                        val mergedHistory = (existingHistory + archivedSnaps + liveSnaps)
                            .distinctBy { it.id }
                            .sortedBy { it.timestampMillis }
                        val latestSnap = mergedHistory.maxByOrNull { it.timestampMillis }
                        val existingTaxes = AlgPosteSmsCodec.parseTaxAnomalies(current.detectedSmsTaxesJson)
                        mergedTaxes = AlgPosteSmsAnalyzer.detectTaxAnomalies(
                            snapshots = mergedHistory,
                            existingAnomalies = existingTaxes
                        )
                        current.copy(
                            smsBalanceHistoryJson = AlgPosteSmsCodec.encodeBalanceHistory(mergedHistory),
                            detectedSmsTaxesJson = AlgPosteSmsCodec.encodeTaxAnomalies(mergedTaxes),
                            latestSmsBalanceAmount = latestSnap?.balance ?: current.latestSmsBalanceAmount,
                            latestSmsBalanceUpdatedAt = latestSnap?.timestampMillis ?: current.latestSmsBalanceUpdatedAt,
                            latestSmsBalanceRawSnippet = latestSnap?.rawBody ?: current.latestSmsBalanceRawSnippet
                        )
                    }

                    // 3. Deduplicate against existing expense transactions
                    val existingTransactions = dao.getAllTransactionsList()
                        .map { crypto.decryptTransaction(it, ::reportRoomCryptoIssue) }
                    val unlogged = findUnloggedTaxAnomaliesForExpenses(
                        anomalies = mergedTaxes,
                        existingTransactions = existingTransactions
                    )

                    val newlyLoggedIds = mutableSetOf<String>()
                    var addedTotal = 0.0
                    for (tax in unlogged) {
                        val rawExplanation = (if (isAr) tax.explanationAr else tax.explanationEn)
                            .ifBlank { tax.rawBody }
                            .trim()
                        val refTag = "[tax_ref:${tax.id}]"
                        val composedNote = if (rawExplanation.isBlank()) {
                            refTag
                        } else {
                            "${rawExplanation.take((240 - refTag.length - 1).coerceAtLeast(0))} $refTag"
                        }
                        dao.insertTransaction(
                            crypto.encryptTransaction(
                                TransactionEntity(
                                    type = TransactionType.EXPENSE,
                                    amount = tax.taxAmount,
                                    category = "taxes_fees",
                                    title = if (isAr) tax.titleAr.ifBlank { "ضرائب ورسوم بريدي موب" }
                                    else tax.titleEn.ifBlank { "BaridiMob Tax / Fee" },
                                    note = composedNote.take(240),
                                    dateMillis = tax.detectedAtMillis
                                ),
                                ::reportRoomCryptoIssue
                            )
                        )
                        newlyLoggedIds.add(tax.id)
                        addedTotal += tax.taxAmount
                    }

                    // 4. Mark newly added + already-matched taxes as recorded so UI count drops cleanly
                    val allMatchedIds = mergedTaxes
                        .filter { it.id in newlyLoggedIds || isTaxAnomalyAlreadyLoggedInTransactions(it, existingTransactions) }
                        .map { it.id }
                        .toSet()

                    if (allMatchedIds.isNotEmpty()) {
                        prefsManager.updateSettings { current ->
                            val updated = AlgPosteSmsCodec.parseTaxAnomalies(current.detectedSmsTaxesJson).map { item ->
                                if (item.id in allMatchedIds) item.copy(isRecordedAsExpense = true) else item
                            }
                            current.copy(detectedSmsTaxesJson = AlgPosteSmsCodec.encodeTaxAnomalies(updated))
                        }
                    }

                    Triple(unlogged.size, addedTotal, mergedTaxes.size)
                }

                val (addedCount, addedTotal, totalDetectedCount) = summaryOutcome
                val msg = when {
                    addedCount > 0 -> {
                        val formattedTotal = formatDzdAmount(addedTotal, if (isAr) "دج" else "DA")
                        if (isAr) "تمت إضافة $addedCount ضرائب ($formattedTotal) إلى المصاريف ✓"
                        else "Added $addedCount tax entries ($formattedTotal) to Expenses ✓"
                    }
                    totalDetectedCount > 0 -> {
                        if (isAr) "جميع الضرائب المرصودة ($totalDetectedCount) مسجلة مسبقاً ✓"
                        else "All $totalDetectedCount detected bank taxes are already logged ✓"
                    }
                    else -> {
                        if (isAr) "لم يتم العثور على ضرائب أو رسوم بنكية غير مسجلة"
                        else "No unlogged bank fees or taxes detected"
                    }
                }
                _toastEvent.emit(msg)
            } finally {
                isAutoAddingTaxes.set(false)
            }
        }
    }

    fun recordTaxAnomalyAsExpense(anomalyId: String) {
        viewModelScope.launch {
            val currentSettings = settings.value
            val isAr = currentSettings.language == "ar"
            val taxes = AlgPosteSmsCodec.parseTaxAnomalies(currentSettings.detectedSmsTaxesJson)
            val target = taxes.firstOrNull { it.id == anomalyId } ?: return@launch
            if (target.isRecordedAsExpense) return@launch
            val existingTransactions = withContext(Dispatchers.IO) {
                dao.getAllTransactionsList().map { crypto.decryptTransaction(it, ::reportRoomCryptoIssue) }
            }
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
            dao.insertTransaction(
                crypto.encryptTransaction(
                    TransactionEntity(
                        type = TransactionType.EXPENSE,
                        amount = target.taxAmount,
                        category = "taxes_fees",
                        title = if (isAr) target.titleAr.ifBlank { "ضرائب ورسوم بريدي موب" }
                        else target.titleEn.ifBlank { "BaridiMob Tax / Fee" },
                        note = composedNote.take(240),
                        dateMillis = target.detectedAtMillis
                    ),
                    ::reportRoomCryptoIssue
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

    fun acceptSmsSuggestion(suggestionId: String, overrideType: TransactionType? = null) {
        viewModelScope.launch {
            val currentSettings = settings.value
            val isAr = currentSettings.language == "ar"
            val pending = AlgPosteSmsCodec.parseSuggestions(currentSettings.pendingSmsSuggestionsJson)
            val target = pending.firstOrNull { it.id == suggestionId } ?: return@launch

            val finalType = overrideType ?: target.transactionType
            val resolvedCategory = when {
                overrideType != null && overrideType != target.transactionType -> {
                    if (finalType == TransactionType.EXPENSE) "other" else "transfer"
                }
                else -> target.suggestedCategory
            }
            val title = if (isAr) {
                if (finalType == TransactionType.EXPENSE) {
                    target.suggestedTitleAr.ifBlank { "مصروف بريدي موب (ALG Poste)" }
                } else {
                    target.suggestedTitleAr.ifBlank { "دخل بريدي موب (ALG Poste)" }
                }
            } else {
                if (finalType == TransactionType.EXPENSE) {
                    target.suggestedTitleEn.ifBlank { "BaridiMob Expense (ALG Poste)" }
                } else {
                    target.suggestedTitleEn.ifBlank { "BaridiMob Income (ALG Poste)" }
                }
            }
            dao.insertTransaction(
                crypto.encryptTransaction(
                    TransactionEntity(
                        type = finalType,
                        amount = target.amount,
                        category = resolvedCategory,
                        title = title,
                        note = "${target.sender}: ${target.rawBody}".take(240),
                        dateMillis = target.receivedAtMillis
                    ),
                    ::reportRoomCryptoIssue
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

    fun upsertCryptoHolding(symbol: String, quantity: Double) {
        val cleanSym = symbol.trim().uppercase()
        if (cleanSym.isBlank() || !quantity.isFinite()) return
        val isAr = settings.value.language == "ar"

        prefsManager.updateSettings { current ->
            val list = BinanceCryptoCodec.parseHoldings(current.binanceHoldingsJson)
                .filterNot { it.symbol.equals(cleanSym, ignoreCase = true) }
                .toMutableList()
            if (quantity > 0.0) {
                list.add(
                    0,
                    CryptoHoldingEntry(
                        symbol = cleanSym,
                        quantity = quantity,
                        updatedAtMillis = System.currentTimeMillis()
                    )
                )
            }
            current.copy(binanceHoldingsJson = BinanceCryptoCodec.encodeHoldings(list))
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
                    if (isAr) "تم حفظ رصيد $cleanSym في محفظة Binance ✅"
                    else "Saved $cleanSym holding to Binance Wallet ✅"
                } else {
                    if (isAr) "تمت إزالة $cleanSym من المحفظة"
                    else "Removed $cleanSym from Binance Wallet"
                }
            )
        }
    }

    fun removeCryptoHolding(symbol: String) {
        upsertCryptoHolding(symbol, 0.0)
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

        viewModelScope.launch {
            _isRefreshingSquareMarket.value = true
            _squareMarketError.value = null
            val result = SquareMarketFetcher.fetchLiveSnapshot()
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
                    if (isAr) "تم دمج الأصول (الذهب + الكريبتو) مع إجمالي المدخرات ✓"
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
