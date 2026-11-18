package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.BackupPayload
import com.example.sms.AlgPosteSmsAnalyzer
import com.example.ui.i18n.APP_VERSION
import com.example.ui.i18n.DEVELOPER_NAME
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import android.app.Application
import com.example.data.PreferencesManager
import com.example.ui.i18n.ArabicStrings
import com.example.ui.i18n.EnglishStrings
import com.example.viewmodel.SaleryPelseViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PermanentSmsArchiveRobolectricTest {

    @Test
    fun backupPayload_roundTripsPermanentAlgPosteSmsArchive() {
        val msg1 = AlgPosteSmsAnalyzer.toArchivedEntity(
            sender = "ALG Poste",
            body = "Virement reçu +10 000,00 DA. Nouveau solde: 30 000,00 DA",
            timestampMillis = 10_000_000L,
            captureSource = "RECEIVER"
        )!!
        val msg2 = AlgPosteSmsAnalyzer.toArchivedEntity(
            sender = "ALG Poste",
            body = "Retrait GAB -2 000,00 DA. Nouveau solde: 27 970,00 DA",
            timestampMillis = 20_000_000L,
            captureSource = "RECEIVER"
        )!!
        val msgOtp = AlgPosteSmsAnalyzer.toArchivedEntity(
            sender = "ALG Poste",
            body = "Votre code OTP de confirmation est 123456.",
            timestampMillis = 25_000_000L,
            captureSource = "RECEIVER"
        )!!

        val backup = BackupPayload(
            exportTimestamp = 1_763_120_000_000L,
            appVersion = APP_VERSION,
            developer = DEVELOPER_NAME,
            transactions = emptyList(),
            debts = emptyList(),
            salaryAmount = 85000.0,
            salaryPayday = 25,
            salaryEmployer = "Enterprise Algiers",
            archivedAlgPosteSms = listOf(msg1, msg2, msgOtp)
        )
        val restored = BackupPayload.fromJsonString(backup.toJsonString())
        assertNotNull(restored)
        assertEquals(3, restored!!.archivedAlgPosteSms.size)
        assertEquals(msg2.rawBody, restored.archivedAlgPosteSms[1].rawBody)
        assertEquals(27970.0, restored.archivedAlgPosteSms[1].extractedBalance!!, 0.01)
    }

    @Test
    fun roomPermanentArchive_persistsAppendOnlyRecordsAndSupportsReanalysis() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val dao = db.appDao()
            val msg1 = AlgPosteSmsAnalyzer.toArchivedEntity(
                sender = "ALG Poste",
                body = "Virement reçu +10 000,00 DA. Nouveau solde: 30 000,00 DA",
                timestampMillis = 10_000_000L,
                captureSource = "RECEIVER"
            )!!
            val msg2 = AlgPosteSmsAnalyzer.toArchivedEntity(
                sender = "ALG Poste",
                body = "Retrait GAB -2 000,00 DA. Nouveau solde: 27 970,00 DA",
                timestampMillis = 20_000_000L,
                captureSource = "INBOX"
            )!!

            dao.insertArchivedSmsIfMissing(listOf(msg1, msg2))
            // Duplicate insert must be ignored cleanly without losing original captureSource
            dao.insertArchivedSmsIfMissing(listOf(msg1.copy(captureSource = "INBOX")))

            val stored = dao.getAllArchivedAlgPosteSmsList()
            assertEquals(2, stored.size)
            assertEquals("RECEIVER", stored.first { it.id == msg1.id }.captureSource)

            val reanalyzed = stored.map { AlgPosteSmsAnalyzer.reanalyzeArchivedEntity(it) }
            dao.updateArchivedSmsAnalysis(reanalyzed)

            val afterStudy = dao.getAllArchivedAlgPosteSmsList()
            assertEquals(2, afterStudy.size)
            val snaps = AlgPosteSmsAnalyzer.reconstructSnapshotsFromArchive(afterStudy)
            val taxes = AlgPosteSmsAnalyzer.detectTaxAnomalies(snaps)
            assertTrue(taxes.any { kotlin.math.abs(it.taxAmount - 30.0) < 0.01 })
        } finally {
            db.close()
        }
    }

    @Test
    fun dashboardBalance_computesIncomeMinusExpense_unlinkedFromAlgPostBalance() = runBlocking {
        assertEquals("Balance", EnglishStrings.totalBalance)
        assertEquals("الرصيد", ArabicStrings.totalBalance)

        val app = ApplicationProvider.getApplicationContext<Application>()
        val externalPrefs = PreferencesManager(app)
        externalPrefs.updateSettings {
            it.copy(
                salaryAutoCredit = false,
                latestSmsBalanceAmount = 99999.75,
                savingsWalletBalance = 12000.0
            )
        }

        val db = AppDatabase.getDatabase(app)
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            db.clearAllTables()
            db.appDao().insertTransaction(
                com.example.data.TransactionEntity(
                    type = com.example.data.TransactionType.INCOME,
                    amount = 65000.0,
                    category = "salary",
                    title = "Salary"
                )
            )
            db.appDao().insertTransaction(
                com.example.data.TransactionEntity(
                    type = com.example.data.TransactionType.EXPENSE,
                    amount = 15500.0,
                    category = "shopping",
                    title = "Groceries"
                )
            )
        }

        val vm = SaleryPelseViewModel(app)
        val collectJob = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            vm.financialSummary.collect { }
        }
        try {
            val deadline = System.currentTimeMillis() + 5000L
            while (System.currentTimeMillis() < deadline) {
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                val cur = vm.financialSummary.value
                if (kotlin.math.abs(cur.totalBalance - 37500.0) < 0.01 &&
                    kotlin.math.abs(cur.savingsBalance - 12000.0) < 0.01
                ) {
                    break
                }
                kotlinx.coroutines.delay(50)
            }
            val syncedSummary = vm.financialSummary.value
            assertEquals(37500.0, syncedSummary.totalBalance, 0.01)
            assertEquals(49500.0, syncedSummary.grossBalance, 0.01)
            assertEquals(12000.0, syncedSummary.savingsBalance, 0.01)
            assertEquals(99999.75, externalPrefs.settingsFlow.value.latestSmsBalanceAmount, 0.01)

            // Withdraw from savings adds back to Dashboard totalBalance
            vm.withdrawFromSavings(5000.0)
            val withdrawDeadline = System.currentTimeMillis() + 3000L
            while (System.currentTimeMillis() < withdrawDeadline) {
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                if (kotlin.math.abs(vm.financialSummary.value.totalBalance - 42500.0) < 0.01) break
                kotlinx.coroutines.delay(25)
            }
            assertEquals(42500.0, vm.financialSummary.value.totalBalance, 0.01)
            assertEquals(7000.0, vm.financialSummary.value.savingsBalance, 0.01)
            assertEquals(49500.0, vm.financialSummary.value.grossBalance, 0.01)

            // Deposit to savings deducts from Dashboard totalBalance
            vm.transferToSavings(10000.0)
            val depositDeadline = System.currentTimeMillis() + 3000L
            while (System.currentTimeMillis() < depositDeadline) {
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                if (kotlin.math.abs(vm.financialSummary.value.totalBalance - 32500.0) < 0.01) break
                kotlinx.coroutines.delay(25)
            }
            assertEquals(32500.0, vm.financialSummary.value.totalBalance, 0.01)
            assertEquals(17000.0, vm.financialSummary.value.savingsBalance, 0.01)
            assertEquals(49500.0, vm.financialSummary.value.grossBalance, 0.01)
        } finally {
            collectJob.cancel()
        }
    }

    @Test
    fun algPosteManualSms_preservesNegativeBalanceInSettingsAndHistory() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val prefs = PreferencesManager(app)
        prefs.updateSettings {
            it.copy(
                salaryAutoCredit = false,
                latestSmsBalanceAmount = -1.0,
                latestSmsBalanceUpdatedAt = 0L,
                smsBalanceHistoryJson = "[]"
            )
        }

        val vm = SaleryPelseViewModel(app)
        vm.analyzeManualSmsSample("ALG Poste", "Frais -500.00 DA. Nouveau solde: -1 250,00 DA")

        val deadline = System.currentTimeMillis() + 4000L
        while (System.currentTimeMillis() < deadline) {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            if (vm.settings.value.latestSmsBalanceUpdatedAt > 0L) break
            kotlinx.coroutines.delay(25)
        }
        assertTrue(vm.settings.value.latestSmsBalanceUpdatedAt > 0L)
        assertEquals(-1250.0, vm.settings.value.latestSmsBalanceAmount, 0.01)
    }

    @Test
    fun savingsWallet_transfersGoalsAndHistory_persistAndClampAccurately() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val prefs = PreferencesManager(app)
        prefs.updateSettings {
            it.copy(
                salaryAutoCredit = false,
                savingsWalletBalance = 0.0,
                savingsGoalAmount = 0.0,
                savingsHistoryJson = "[]"
            )
        }

        val vm = SaleryPelseViewModel(app)
        vm.setSavingsGoal(100000.0)
        assertEquals(100000.0, vm.settings.value.savingsGoalAmount, 0.01)

        vm.transferToSavings(25000.0)
        assertEquals(25000.0, vm.settings.value.savingsWalletBalance, 0.01)

        vm.withdrawFromSavings(7500.0)
        assertEquals(17500.0, vm.settings.value.savingsWalletBalance, 0.01)

        // Withdrawing more than balance clamps cleanly without going negative
        vm.withdrawFromSavings(50000.0)
        assertEquals(0.0, vm.settings.value.savingsWalletBalance, 0.01)

        val history = com.example.data.SavingsHistoryCodec.parse(vm.settings.value.savingsHistoryJson)
        assertEquals(3, history.size)
        assertEquals(com.example.data.SavingsMovementType.WITHDRAW, history[0].type)
        assertEquals(17500.0, history[0].amount, 0.01)
        assertEquals(com.example.data.SavingsMovementType.WITHDRAW, history[1].type)
        assertEquals(7500.0, history[1].amount, 0.01)
        assertEquals(com.example.data.SavingsMovementType.DEPOSIT, history[2].type)
        assertEquals(25000.0, history[2].amount, 0.01)

        vm.clearSavingsHistory()
        assertEquals("[]", vm.settings.value.savingsHistoryJson)
    }

    @Test
    fun algPostBalanceMode_computesAlgPostPlusManualDiff_andPausesRestoresSalaryScheduler() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val prefs = PreferencesManager(app)
        prefs.updateSettings {
            it.copy(
                salaryAmount = 92000.0,
                salaryPayday = 18,
                salaryEmployer = "Sonatrach",
                salaryAutoCredit = true,
                lastCreditedMonthYear = "2026-11",
                algPostBalanceModeEnabled = false,
                savedSalaryAutoCreditBeforeAlgPostMode = false,
                latestSmsBalanceAmount = 50000.0,
                latestSmsBalanceUpdatedAt = 1_763_120_000_000L,
                savingsWalletBalance = 10000.0
            )
        }

        val db = AppDatabase.getDatabase(app)
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            db.clearAllTables()
            // Manual income (+12,000) & manual expense (-4,000) -> manual diff = +8,000
            db.appDao().insertTransaction(
                com.example.data.TransactionEntity(
                    type = com.example.data.TransactionType.INCOME,
                    amount = 12000.0,
                    category = "bonus",
                    title = "Manual Side Job",
                    isAutoAlgPoste = false
                )
            )
            db.appDao().insertTransaction(
                com.example.data.TransactionEntity(
                    type = com.example.data.TransactionType.EXPENSE,
                    amount = 4000.0,
                    category = "food",
                    title = "Manual Groceries",
                    isAutoAlgPoste = false
                )
            )
            // Auto ALG Post transaction MUST be excluded from manual diff when ALG Post mode is ON
            db.appDao().insertTransaction(
                com.example.data.TransactionEntity(
                    type = com.example.data.TransactionType.EXPENSE,
                    amount = 9000.0,
                    category = "shopping",
                    title = "Auto BaridiMob",
                    isAutoAlgPoste = true,
                    smsKey = "sms_key_1"
                )
            )
        }

        val vm = SaleryPelseViewModel(app)
        val collectJob = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            vm.financialSummary.collect { }
        }
        try {
            // Manual Mode first: Total Income (12,000) - Total Expense (13,000) - Savings (10,000) = -11,000
            val manualDeadline = System.currentTimeMillis() + 4000L
            while (System.currentTimeMillis() < manualDeadline) {
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                if (!vm.financialSummary.value.isAlgPostBalanceMode &&
                    kotlin.math.abs(vm.financialSummary.value.totalBalance - (-11000.0)) < 0.01
                ) break
                kotlinx.coroutines.delay(25)
            }
            assertEquals(-11000.0, vm.financialSummary.value.totalBalance, 0.01)
            assertTrue(vm.settings.value.salaryAutoCredit)

            // Enable ALG Post Balance Mode:
            // - Salary scheduler auto-pauses (salaryAutoCredit = false), keeping amount/payday/employer intact
            // - Balance = ALG Post (50,000) + Manual Diff (12,000 - 4,000 = 8,000) - Savings (10,000) = 48,000
            vm.setAlgPostBalanceModeEnabled(true)
            val algDeadline = System.currentTimeMillis() + 4000L
            while (System.currentTimeMillis() < algDeadline) {
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                val cur = vm.financialSummary.value
                if (cur.isAlgPostBalanceMode && kotlin.math.abs(cur.totalBalance - 48000.0) < 0.01) break
                kotlinx.coroutines.delay(25)
            }

            val algSummary = vm.financialSummary.value
            assertTrue(algSummary.isAlgPostBalanceMode)
            assertEquals(50000.0, algSummary.algPostBalanceComponent, 0.01)
            assertEquals(8000.0, algSummary.manualNetDiffComponent, 0.01)
            assertEquals(58000.0, algSummary.grossBalance, 0.01)
            assertEquals(48000.0, algSummary.totalBalance, 0.01)

            // Verify Smart Salary Scheduler was auto-disabled while all info remained saved
            val pausedSettings = vm.settings.value
            org.junit.Assert.assertFalse(pausedSettings.salaryAutoCredit)
            assertTrue(pausedSettings.savedSalaryAutoCreditBeforeAlgPostMode)
            assertEquals(92000.0, pausedSettings.salaryAmount, 0.01)
            assertEquals(18, pausedSettings.salaryPayday)
            assertEquals("Sonatrach", pausedSettings.salaryEmployer)
            assertEquals("2026-11", pausedSettings.lastCreditedMonthYear)

            // Disable ALG Post Balance Mode -> returns to Manual Mode and restores salaryAutoCredit = true
            vm.setAlgPostBalanceModeEnabled(false)
            val revertDeadline = System.currentTimeMillis() + 4000L
            while (System.currentTimeMillis() < revertDeadline) {
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                val cur = vm.financialSummary.value
                if (!cur.isAlgPostBalanceMode && kotlin.math.abs(cur.totalBalance - (-11000.0)) < 0.01) break
                kotlinx.coroutines.delay(25)
            }
            assertEquals(-11000.0, vm.financialSummary.value.totalBalance, 0.01)
            assertTrue(vm.settings.value.salaryAutoCredit)
            assertEquals(92000.0, vm.settings.value.salaryAmount, 0.01)
        } finally {
            collectJob.cancel()
        }
    }

    @Test
    fun autoIncomeAndExpenseToggles_convertMatchingPendingSuggestions_withoutDuplicates() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val db = AppDatabase.getDatabase(app)
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            db.clearAllTables()
        }

        val incSug = AlgPosteSmsAnalyzer.parseSms(
            sender = "ALG Poste",
            body = "Virement reçu +14 500,00 DA. Nouveau solde: 64 500,00 DA",
            timestampMillis = 100_000_000L
        )!!
        val expSug = AlgPosteSmsAnalyzer.parseSms(
            sender = "ALG Poste",
            body = "Retrait GAB -3 200,00 DA. Nouveau solde: 61 300,00 DA",
            timestampMillis = 100_500_000L
        )!!

        val prefs = PreferencesManager(app)
        prefs.updateSettings {
            it.copy(
                salaryAutoCredit = false,
                autoIncomeFromAlgPostEnabled = false,
                autoExpenseFromAlgPostEnabled = false,
                pendingSmsSuggestionsJson = com.example.sms.AlgPosteSmsCodec.encodeSuggestions(listOf(incSug, expSug)),
                processedSmsKeysJson = "[]"
            )
        }

        val vm = SaleryPelseViewModel(app)
        val collectJob = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            vm.pendingSmsSuggestions.collect { }
        }
        try {
            // Enable Auto Income only -> converts incSug, leaves expSug pending
            vm.setAutoIncomeFromAlgPostEnabled(true)
            val incDeadline = System.currentTimeMillis() + 4000L
            while (System.currentTimeMillis() < incDeadline) {
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                val txCount = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    db.appDao().getAllTransactionsList().size
                }
                if (txCount == 1 && vm.pendingSmsSuggestions.value.size == 1) break
                kotlinx.coroutines.delay(25)
            }

            val afterIncTx = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                db.appDao().getAllTransactionsList()
            }
            assertEquals(1, afterIncTx.size)
            assertEquals(14500.0, afterIncTx[0].amount, 0.01)
            assertTrue(afterIncTx[0].isAutoAlgPoste)
            assertEquals(1, vm.pendingSmsSuggestions.value.size)
            assertEquals(expSug.id, vm.pendingSmsSuggestions.value[0].id)

            // Enable Auto Expenses -> converts expSug, pending becomes empty
            vm.setAutoExpenseFromAlgPostEnabled(true)
            val expDeadline = System.currentTimeMillis() + 4000L
            while (System.currentTimeMillis() < expDeadline) {
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                val txCount = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    db.appDao().getAllTransactionsList().size
                }
                if (txCount == 2 && vm.pendingSmsSuggestions.value.isEmpty()) break
                kotlinx.coroutines.delay(25)
            }

            // Toggle again should NOT create duplicates
            vm.setAutoIncomeFromAlgPostEnabled(true)
            kotlinx.coroutines.delay(150)

            val finalTx = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                db.appDao().getAllTransactionsList()
            }
            assertEquals(2, finalTx.size)
            assertTrue(finalTx.all { it.isAutoAlgPoste && it.smsKey.isNotBlank() })
            assertTrue(vm.pendingSmsSuggestions.value.isEmpty())
        } finally {
            collectJob.cancel()
        }
    }
}
