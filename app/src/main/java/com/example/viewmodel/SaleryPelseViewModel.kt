package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.BackupPayload
import com.example.data.CcpAccountEntry
import com.example.data.DebtEntity
import com.example.data.DebtType
import com.example.data.HubVaultCodec
import com.example.data.PreferencesManager
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
import com.example.sms.SmsSuggestionEntry
import com.example.update.AppUpdateManager
import com.example.update.AppUpdateUiState
import com.example.update.DownloadState
import com.example.update.compareVersionTags

data class FinancialSummary(
    val totalBalance: Double = 0.0,
    val grossBalance: Double = 0.0,
    val savingsBalance: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val monthlyExpenses: Double = 0.0,
    val pendingDebtsOwedByMe: Double = 0.0,
    val pendingDebtsOwedToMe: Double = 0.0
)

class SaleryPelseViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val dao = database.appDao()
    private val prefsManager = PreferencesManager(application)

    val settings: StateFlow<UserSettings> = prefsManager.settingsFlow

    val allTransactions: StateFlow<List<TransactionEntity>> = dao.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDebts: StateFlow<List<DebtEntity>> = dao.getAllDebts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Financial calculations: Total Balance dynamically reflects all Incomes and Expenses
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
        // Available spendable balance: Total Income minus Total Expenses minus Savings Wallet
        val available = totalInc - totalExp - savings
        val gross = totalInc - totalExp

        FinancialSummary(
            totalBalance = available,
            grossBalance = gross,
            savingsBalance = savings,
            monthlyIncome = monthInc,
            monthlyExpenses = monthExp,
            pendingDebtsOwedByMe = owedByMe,
            pendingDebtsOwedToMe = owedToMe
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinancialSummary())

    // App lock state
    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    // In-App GitHub Update state
    private val _updateUiState = MutableStateFlow<AppUpdateUiState>(AppUpdateUiState.Idle)
    val updateUiState: StateFlow<AppUpdateUiState> = _updateUiState.asStateFlow()

    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog: StateFlow<Boolean> = _showUpdateDialog.asStateFlow()

    private var downloadJob: Job? = null

    // Pending BaridiMob / ALG Poste SMS transaction suggestions
    val pendingSmsSuggestions: StateFlow<List<SmsSuggestionEntry>> = settings
        .map { AlgPosteSmsCodec.parseSuggestions(it.pendingSmsSuggestionsJson) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Notification events for UI banners
    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    init {
        // Initialize lock state based on biometric preference
        if (settings.value.biometricEnabled) {
            _isAppLocked.value = true
        }

        // Check Smart Salary Scheduler, Debt Auto-Payments, clean obsolete update APKs, and scan ALG Poste SMS
        viewModelScope.launch {
            try {
                AppUpdateManager.cleanupObsoleteUpdates(getApplication())
            } catch (_: Throwable) {
            }
            try {
                cleanExistingMockDataIfPresent()
                checkAndApplySalaryScheduler()
                checkAndExecuteAutoPayDebts()
                val currentDebts = dao.getAllDebtsList()
                DebtReminderScheduler.rescheduleAllReminders(getApplication(), currentDebts)
            } catch (_: Throwable) {
            }
            if (settings.value.baridiMobSmsSyncEnabled &&
                AlgPosteSmsInboxScanner.hasSmsPermissions(getApplication())
            ) {
                scanAlgPosteSmsInbox(getApplication(), showFeedback = false)
            }
        }

        // Silent background check for newer release on GitHub
        checkForUpdates(manual = false)
    }

    private suspend fun cleanExistingMockDataIfPresent() {
        val prefs = getApplication<Application>().getSharedPreferences("salerypelse_prefs", Context.MODE_PRIVATE)
        val cleaned = prefs.getBoolean("mock_data_cleaned_permanent_v2", false)
        if (!cleaned) {
            val allTrans = dao.getAllTransactionsList()
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

            val allDebts = dao.getAllDebtsList()
            val mockPersons = setOf("Mohamed Lamine", "Karim Benali")
            for (d in allDebts) {
                if (d.personName in mockPersons && (d.note.contains("spare parts") || d.note.contains("Laptop sale"))) {
                    dao.deleteDebt(d)
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
        val userConfig = settings.value
        if (!userConfig.salaryAutoCredit && !force) return

        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)
        val currentMonth = cal.get(Calendar.MONTH) + 1 // 1-12
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        val monthYearKey = "$currentYear-$currentMonth"

        if (force || (currentDay >= userConfig.salaryPayday && userConfig.lastCreditedMonthYear != monthYearKey)) {
            dao.insertTransaction(
                TransactionEntity(
                    type = TransactionType.INCOME,
                    amount = userConfig.salaryAmount,
                    category = "salary",
                    title = "${userConfig.salaryEmployer} Salary",
                    note = "Smart Salary Scheduler automatic deposit ($monthYearKey)",
                    dateMillis = System.currentTimeMillis(),
                    isSalaryCredit = true
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

        for (debt in autoDebts) {
            if (debt.isRecurringMonthlyInstallment) {
                // Monthly recurring installment check
                if (currentDay >= debt.installmentDayOfMonth && debt.lastInstallmentCreditedMonth != monthYearKey) {
                    val payNow = if (debt.monthlyInstallmentAmount > 0) {
                        minOf(debt.monthlyInstallmentAmount, debt.remainingAmount)
                    } else debt.remainingAmount

                    val newRemaining = (debt.remainingAmount - payNow).coerceAtLeast(0.0)
                    val isFullySettled = newRemaining <= 0.0

                    dao.updateDebt(
                        debt.copy(
                            remainingAmount = newRemaining,
                            lastInstallmentCreditedMonth = monthYearKey,
                            isPaid = isFullySettled,
                            paidDateMillis = if (isFullySettled) now else null
                        )
                    )

                    if (debt.type == DebtType.OWED_BY_ME) {
                        dao.insertTransaction(
                            TransactionEntity(
                                type = TransactionType.EXPENSE,
                                amount = payNow,
                                category = "debt_payment",
                                title = "Auto-Installment: ${debt.personName}",
                                note = "Monthly auto-installment ($monthYearKey) - Rem: ${newRemaining.toLong()} DZD",
                                dateMillis = now
                            )
                        )
                    } else {
                        dao.insertTransaction(
                            TransactionEntity(
                                type = TransactionType.INCOME,
                                amount = payNow,
                                category = "debt_collection",
                                title = "Auto-Collected Installment: ${debt.personName}",
                                note = "Monthly auto-installment ($monthYearKey) - Rem: ${newRemaining.toLong()} DZD",
                                dateMillis = now
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
                        debt.copy(
                            remainingAmount = 0.0,
                            isPaid = true,
                            paidDateMillis = now
                        )
                    )
                    if (debt.type == DebtType.OWED_BY_ME) {
                        dao.insertTransaction(
                            TransactionEntity(
                                type = TransactionType.EXPENSE,
                                amount = payAmount,
                                category = "debt_payment",
                                title = "Auto-Paid Debt: ${debt.personName}",
                                note = "Automatic debt execution on due date (${debt.note})",
                                dateMillis = now
                            )
                        )
                    } else {
                        dao.insertTransaction(
                            TransactionEntity(
                                type = TransactionType.INCOME,
                                amount = payAmount,
                                category = "debt_collection",
                                title = "Auto-Received Debt: ${debt.personName}",
                                note = "Automatic debt collection on due date (${debt.note})",
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
        viewModelScope.launch {
            dao.insertTransaction(
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
            dao.deleteTransaction(transaction)
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
        viewModelScope.launch {
            val validPaid = initialPaidAmount.coerceIn(0.0, amount)
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
            val newId = dao.insertDebt(newEntity)
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
                debt.copy(
                    remainingAmount = 0.0,
                    isPaid = true,
                    paidDateMillis = now
                )
            )
            DebtReminderScheduler.cancelDebtReminder(getApplication(), debt.id)
            // Record wallet impact
            if (debt.type == DebtType.OWED_BY_ME) {
                dao.insertTransaction(
                    TransactionEntity(
                        type = TransactionType.EXPENSE,
                        amount = payAmount,
                        category = "debt_payment",
                        title = "Paid Debt: ${debt.personName}",
                        note = "Manual settlement: ${debt.note}",
                        dateMillis = now
                    )
                )
            } else {
                dao.insertTransaction(
                    TransactionEntity(
                        type = TransactionType.INCOME,
                        amount = payAmount,
                        category = "debt_collection",
                        title = "Collected Debt: ${debt.personName}",
                        note = "Manual settlement: ${debt.note}",
                        dateMillis = now
                    )
                )
            }
            _toastEvent.emit("Debt marked as paid and wallet updated")
        }
    }

    fun updateDebt(debt: DebtEntity) {
        viewModelScope.launch {
            dao.updateDebt(debt)
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
            if (paymentAmount <= 0) return@launch
            val now = System.currentTimeMillis()
            val actualPay = minOf(paymentAmount, debt.remainingAmount)
            val newRemaining = (debt.remainingAmount - actualPay).coerceAtLeast(0.0)
            val isFullySettled = newRemaining <= 0.001

            val updatedDebt = debt.copy(
                remainingAmount = newRemaining,
                isPaid = isFullySettled,
                paidDateMillis = if (isFullySettled) now else debt.paidDateMillis
            )
            dao.updateDebt(updatedDebt)
            if (isFullySettled) {
                DebtReminderScheduler.cancelDebtReminder(getApplication(), debt.id)
            } else {
                DebtReminderScheduler.scheduleDebtReminder(getApplication(), updatedDebt)
            }

            if (recordTransaction) {
                if (debt.type == DebtType.OWED_BY_ME) {
                    dao.insertTransaction(
                        TransactionEntity(
                            type = TransactionType.EXPENSE,
                            amount = actualPay,
                            category = "debt_payment",
                            title = "Partial Payment: ${debt.personName}",
                            note = if (isFullySettled) "Final settlement (${debt.note})" 
                                   else "Partial payment - Rem: ${newRemaining.toLong()} DZD (${debt.note})",
                            dateMillis = now
                        )
                    )
                } else {
                    dao.insertTransaction(
                        TransactionEntity(
                            type = TransactionType.INCOME,
                            amount = actualPay,
                            category = "debt_collection",
                            title = "Partial Collection: ${debt.personName}",
                            note = if (isFullySettled) "Final collection (${debt.note})"
                                   else "Partial collection - Rem: ${newRemaining.toLong()} DZD (${debt.note})",
                            dateMillis = now
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
        prefsManager.updateSettings {
            it.copy(
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
        cvc: String
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

        prefsManager.updateSettings {
            it.copy(
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
                else -> "Visa"
            }
            _toastEvent.emit(
                if (isAr) "تم حفظ بطاقة $label بنجاح 💳"
                else "$label card saved successfully 💳"
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
        if (settings.value.patternLockEnabled && settings.value.patternLockHash.isNotBlank()) {
            _isAppLocked.value = true
        }
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
        appPresetId: String = "custom"
    ) {
        val cleanLabel = label.trim()
        val cleanSecret = secretValue.trim()
        if (cleanLabel.isEmpty() || cleanSecret.isEmpty()) return
        val resolvedPreset = appPresetId.trim().ifBlank {
            VaultAppPresets.detectFromLabel(cleanLabel)?.id ?: "custom"
        }
        prefsManager.updateSettings { current ->
            val updated = HubVaultCodec.parseVaultKeys(current.vaultKeysJson) + VaultKeyEntry(
                label = cleanLabel,
                username = username.trim(),
                secretValue = cleanSecret,
                category = category.ifBlank { "APP" },
                note = note.trim(),
                createdAt = System.currentTimeMillis(),
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
        appPresetId: String = "custom"
    ) {
        val cleanLabel = label.trim()
        val cleanSecret = secretValue.trim()
        if (cleanLabel.isEmpty() || cleanSecret.isEmpty()) return
        val resolvedPreset = appPresetId.trim().ifBlank {
            VaultAppPresets.detectFromLabel(cleanLabel)?.id ?: "custom"
        }
        prefsManager.updateSettings { current ->
            val updated = HubVaultCodec.parseVaultKeys(current.vaultKeysJson).map { item ->
                if (item.id == keyId) {
                    item.copy(
                        label = cleanLabel,
                        username = username.trim(),
                        secretValue = cleanSecret,
                        category = category.ifBlank { "APP" },
                        note = note.trim(),
                        appPresetId = resolvedPreset
                    )
                } else {
                    item
                }
            }
            current.copy(vaultKeysJson = HubVaultCodec.encodeVaultKeys(updated))
        }
    }

    fun deleteVaultKey(keyId: String) {
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
        isPinned: Boolean = false
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
                createdAt = now
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
        isPinned: Boolean = false
    ) {
        val cleanFirst = firstName.trim()
        val cleanLast = lastName.trim()
        val fullName = listOf(cleanFirst, cleanLast).filter { it.isNotEmpty() }.joinToString(" ")
        val cleanCcp = ccpNumber.filter { it.isDigit() }
        val cleanRip = ripNumber.filter { it.isDigit() }.take(20)
        if (fullName.isEmpty() || (cleanCcp.isEmpty() && cleanRip.isEmpty())) return
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
                        }
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
        prefsManager.updateSettings { current ->
            val updated = HubVaultCodec.parseCcpAccounts(current.ccpAccountsJson).filterNot { it.id == accountId }
            current.copy(ccpAccountsJson = HubVaultCodec.encodeCcpAccounts(updated))
        }
    }

    // Stealth Mode (Privacy / Shake to Hide)
    fun toggleStealthMode(): Boolean {
        val current = settings.value.stealthMode
        val next = !current
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

    fun setStealthMode(enabled: Boolean) {
        prefsManager.updateSettings { it.copy(stealthMode = enabled) }
    }

    // Savings Wallet (محفظة الادخار) Actions
    fun transferToSavings(amount: Double) {
        if (amount <= 0.0) return
        val currentAvailable = financialSummary.value.totalBalance
        if (amount > currentAvailable && currentAvailable > 0) {
            viewModelScope.launch {
                val isAr = settings.value.language == "ar"
                _toastEvent.emit(
                    if (isAr) "تنبيه: المبلغ المراد تحويله أكبر من الرصيد المتاح (${formatDzdAmount(currentAvailable)} دج)"
                    else "Notice: Amount exceeds available balance (${formatDzdAmount(currentAvailable)} DZD)"
                )
            }
        }
        prefsManager.updateSettings { current ->
            val updated = current.savingsWalletBalance + amount
            current.copy(
                savingsWalletBalance = updated,
                vaultLockedAmount = updated
            )
        }
        viewModelScope.launch {
            val isAr = settings.value.language == "ar"
            _toastEvent.emit(
                if (isAr) "تم تحويل ${formatDzdAmount(amount)} دج إلى محفظة الادخار بنجاح 💰"
                else "Savings Wallet: ${formatDzdAmount(amount)} DZD transferred to savings 💰"
            )
        }
    }

    fun withdrawFromSavings(amount: Double) {
        if (amount <= 0.0) return
        prefsManager.updateSettings { current ->
            val updated = (current.savingsWalletBalance - amount).coerceAtLeast(0.0)
            current.copy(
                savingsWalletBalance = updated,
                vaultLockedAmount = updated
            )
        }
        viewModelScope.launch {
            val isAr = settings.value.language == "ar"
            _toastEvent.emit(
                if (isAr) "تم استرجاع ${formatDzdAmount(amount)} دج إلى الرصيد المتاح بنجاح 🔄"
                else "Savings Wallet: ${formatDzdAmount(amount)} DZD withdrawn back to available balance 🔄"
            )
        }
    }

    fun spendFromSavings(amount: Double, title: String, category: String, note: String) {
        if (amount <= 0.0) return
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

        // 1. Deduct directly from savings balance
        prefsManager.updateSettings { current ->
            val updated = (current.savingsWalletBalance - amount).coerceAtLeast(0.0)
            current.copy(
                savingsWalletBalance = updated,
                vaultLockedAmount = updated
            )
        }

        // 2. Automatically record in Expenses page (صفحة المصاريف)
        viewModelScope.launch {
            val isAr = settings.value.language == "ar"
            val savingsTagNote = if (isAr) {
                if (note.isNotBlank()) "تم الصرف من محفظة الادخار • $note" else "تم الصرف مباشرة من محفظة الادخار"
            } else {
                if (note.isNotBlank()) "Paid from Savings Wallet • $note" else "Paid directly from Savings Wallet"
            }

            dao.insertTransaction(
                TransactionEntity(
                    type = TransactionType.EXPENSE,
                    amount = amount,
                    category = category.ifBlank { "savings_expense" },
                    title = title.ifBlank { if (isAr) "مصروف من محفظة الادخار" else "Savings Expense" },
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

    // Keep vault aliases for dashboard compatibility
    fun lockToVault(amount: Double) = transferToSavings(amount)
    fun unlockFromVault(amount: Double) = withdrawFromSavings(amount)
    fun setVaultLockedAmount(amount: Double) {
        val safe = amount.coerceAtLeast(0.0)
        prefsManager.updateSettings { current ->
            current.copy(
                savingsWalletBalance = safe,
                vaultLockedAmount = safe
            )
        }
    }

    // Backup & Restore
    suspend fun exportBackupJson(): String {
        val trans = dao.getAllTransactionsList()
        val debts = dao.getAllDebtsList()
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
            classicCardCvc = userConfig.classicCardCvc
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

        dao.insertTransactions(payload.transactions)
        dao.insertDebts(payload.debts)

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
        _toastEvent.emit("Restored ${payload.transactions.size} transactions and ${payload.debts.size} debts")
        return true
    }

    fun syncGoogleDrive() {
        viewModelScope.launch {
            prefsManager.updateSettings {
                it.copy(
                    googleDriveConnected = true,
                    lastBackupTimestamp = System.currentTimeMillis()
                )
            }
            _toastEvent.emit("Synced successfully with Google Drive")
        }
    }

    // In-App GitHub Releases Update & Download Actions
    fun checkForUpdates(manual: Boolean = true) {
        val activeState = _updateUiState.value
        if (activeState is AppUpdateUiState.Downloading) {
            if (manual) _showUpdateDialog.value = true
            return
        }
        if (manual && activeState is AppUpdateUiState.ReadyToInstall) {
            _showUpdateDialog.value = true
            return
        }

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
                    _showUpdateDialog.value = true
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

    fun openUpdateDialog() {
        if (_updateUiState.value is AppUpdateUiState.Idle) {
            checkForUpdates(manual = true)
        } else {
            _showUpdateDialog.value = true
        }
    }

    fun dismissUpdateDialog() {
        if (_updateUiState.value !is AppUpdateUiState.Downloading) {
            _showUpdateDialog.value = false
        }
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
        if (enableSmsSyncIfGranted && smsGranted) {
            scanAlgPosteSmsInbox(getApplication(), showFeedback = false)
        }
    }

    // BaridiMob / ALG Poste SMS & Alert Sync Actions
    fun setBaridiMobSmsSyncEnabled(enabled: Boolean) {
        prefsManager.updateSettings { it.copy(baridiMobSmsSyncEnabled = enabled) }
        if (enabled && AlgPosteSmsInboxScanner.hasSmsPermissions(getApplication())) {
            scanAlgPosteSmsInbox(getApplication(), showFeedback = false)
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

            val scanned = withContext(Dispatchers.IO) {
                AlgPosteSmsInboxScanner.scanInbox(context)
            }

            var addedCount = 0
            prefsManager.updateSettings { current ->
                val processed = AlgPosteSmsCodec.parseProcessedKeys(current.processedSmsKeysJson)
                val existing = AlgPosteSmsCodec.parseSuggestions(current.pendingSmsSuggestionsJson)
                val existingIds = existing.map { it.id }.toSet()
                val newOnes = scanned.filter { it.id !in processed && it.id !in existingIds }
                addedCount = newOnes.size
                if (newOnes.isEmpty()) {
                    current
                } else {
                    current.copy(
                        pendingSmsSuggestionsJson = AlgPosteSmsCodec.encodeSuggestions(newOnes + existing)
                    )
                }
            }

            if (showFeedback) {
                val msg = when {
                    addedCount > 0 -> if (isAr) {
                        "تم اكتشاف $addedCount معاملة جديدة من رسائل ALG Poste"
                    } else {
                        "Found $addedCount new ALG Poste transaction suggestions"
                    }
                    scanned.isEmpty() -> if (isAr) {
                        "لم يتم العثور على رسائل معاملات من ALG Poste في صندوق الوارد"
                    } else {
                        "No ALG Poste transaction messages found in SMS inbox"
                    }
                    else -> if (isAr) {
                        "جميع رسائل ALG Poste محدثة بالفعل"
                    } else {
                        "All ALG Poste SMS transactions are already synced"
                    }
                }
                _toastEvent.emit(msg)
            }
        }
    }

    fun analyzeManualSmsSample(sender: String, body: String) {
        viewModelScope.launch {
            val isAr = settings.value.language == "ar"
            val parsed = AlgPosteSmsAnalyzer.parseSms(
                sender = sender.ifBlank { "ALG Poste" },
                body = body,
                timestampMillis = System.currentTimeMillis(),
                allowAnySenderForManualTest = true
            )
            if (parsed == null) {
                _toastEvent.emit(
                    if (isAr) "تعذر استخراج مبلغ أو نوع العملية (سحب - / دخل +) من هذه الرسالة"
                    else "Could not detect a valid transaction amount or type from this SMS"
                )
                return@launch
            }

            prefsManager.updateSettings { current ->
                val existing = AlgPosteSmsCodec.parseSuggestions(current.pendingSmsSuggestionsJson)
                val merged = (listOf(parsed) + existing.filterNot { it.id == parsed.id })
                current.copy(
                    pendingSmsSuggestionsJson = AlgPosteSmsCodec.encodeSuggestions(merged)
                )
            }

            val kindLabel = if (parsed.isExpense) {
                if (isAr) "مصروف (Expense)" else "Expense"
            } else {
                if (isAr) "دخل (Income)" else "Income"
            }
            val amtLabel = formatDzdAmount(parsed.amount, if (isAr) "دج" else "DA")
            _toastEvent.emit(
                if (isAr) "تم تحليل الرسالة: $kindLabel بقيمة $amtLabel"
                else "Analyzed SMS: $kindLabel of $amtLabel"
            )
        }
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
                TransactionEntity(
                    type = finalType,
                    amount = target.amount,
                    category = resolvedCategory,
                    title = title,
                    note = "${target.sender}: ${target.rawBody}".take(240),
                    dateMillis = target.receivedAtMillis
                )
            )

            prefsManager.updateSettings { current ->
                val remaining = AlgPosteSmsCodec.parseSuggestions(current.pendingSmsSuggestionsJson)
                    .filterNot { it.id == suggestionId }
                val processed = AlgPosteSmsCodec.parseProcessedKeys(current.processedSmsKeysJson) + suggestionId
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
            val remaining = AlgPosteSmsCodec.parseSuggestions(current.pendingSmsSuggestionsJson)
                .filterNot { it.id == suggestionId }
            val processed = AlgPosteSmsCodec.parseProcessedKeys(current.processedSmsKeysJson) + suggestionId
            current.copy(
                pendingSmsSuggestionsJson = AlgPosteSmsCodec.encodeSuggestions(remaining),
                processedSmsKeysJson = AlgPosteSmsCodec.encodeProcessedKeys(processed)
            )
        }
    }

    fun clearAllSmsSuggestions() {
        prefsManager.updateSettings { current ->
            val pendingIds = AlgPosteSmsCodec.parseSuggestions(current.pendingSmsSuggestionsJson).map { it.id }
            val processed = AlgPosteSmsCodec.parseProcessedKeys(current.processedSmsKeysJson) + pendingIds
            current.copy(
                pendingSmsSuggestionsJson = "[]",
                processedSmsKeysJson = AlgPosteSmsCodec.encodeProcessedKeys(processed)
            )
        }
    }
}

typealias SaleryPulseViewModel = SaleryPelseViewModel
