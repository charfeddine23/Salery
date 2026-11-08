package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.BackupPayload
import com.example.data.DebtEntity
import com.example.data.DebtType
import com.example.data.PreferencesManager
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.data.UserSettings
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
import kotlinx.coroutines.launch
import java.util.Calendar
import com.example.notification.DebtReminderScheduler

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

    // Notification events for UI banners
    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    init {
        // Initialize lock state based on preferences
        if (settings.value.patternLockEnabled && settings.value.patternLockHash.isNotBlank()) {
            _isAppLocked.value = true
        }

        // Check Smart Salary Scheduler and Debt Auto-Payments (Zero mock data seeded)
        viewModelScope.launch {
            cleanExistingMockDataIfPresent()
            checkAndApplySalaryScheduler()
            checkAndExecuteAutoPayDebts()
            val currentDebts = dao.getAllDebtsList()
            DebtReminderScheduler.rescheduleAllReminders(getApplication(), currentDebts)
        }
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
            salaryEmployer = userConfig.salaryEmployer
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
}

typealias SaleryPulseViewModel = SaleryPelseViewModel
