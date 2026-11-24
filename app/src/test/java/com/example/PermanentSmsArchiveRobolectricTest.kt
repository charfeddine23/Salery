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
import com.example.viewmodel.VaultixViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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

        val vm = VaultixViewModel(app)
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

        val vm = VaultixViewModel(app)
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

        val vm = VaultixViewModel(app)
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
    fun autoAddTaxes_toggleOnLogsOnlyFutureTaxesAndSkipsPreviousTaxes() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val prefs = PreferencesManager(app)
        val db = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val dao = db.appDao()
            val pastTime = 1_760_000_000_000L
            val activationTime = 1_765_000_000_000L
            val futureTime = 1_766_000_000_000L

            val pastTax = com.example.sms.SmsTaxAnomalyEntry(
                id = "past_fee_1",
                taxAmount = 30.0,
                detectedAtMillis = pastTime,
                titleEn = "Old Bank Fee",
                explanationEn = "Before activation",
                isRecordedAsExpense = false
            )
            val futureTax = com.example.sms.SmsTaxAnomalyEntry(
                id = "future_fee_2",
                taxAmount = 55.0,
                detectedAtMillis = futureTime,
                titleEn = "Future Bank Fee",
                explanationEn = "After activation",
                isRecordedAsExpense = false
            )

            // Initially disabled: logs nothing even if taxes exist
            prefs.updateSettings {
                it.copy(
                    autoAddTaxesEnabled = false,
                    autoAddTaxesBaselineMillis = 0L,
                    detectedSmsTaxesJson = com.example.sms.AlgPosteSmsCodec.encodeTaxAnomalies(
                        listOf(pastTax, futureTax)
                    )
                )
            }
            val disabledResult = com.example.sms.TaxExpenseAutoLogger.logFutureTaxesIfEnabled(
                dao = dao,
                prefsManager = prefs,
                isArabic = false
            )
            assertEquals(0, disabledResult.addedCount)
            assertTrue(dao.getAllTransactionsList().isEmpty())

            // Enable Auto Add Taxes at activationTime -> MUST skip past_fee_1 and log ONLY future_fee_2
            val vm = VaultixViewModel(app)
            vm.setAutoAddTaxesEnabled(true, activationTimestampMillis = activationTime)
            assertTrue(prefs.settingsFlow.value.autoAddTaxesEnabled)
            assertEquals(activationTime, prefs.settingsFlow.value.autoAddTaxesBaselineMillis)

            val enabledResult = com.example.sms.TaxExpenseAutoLogger.logFutureTaxesIfEnabled(
                dao = dao,
                prefsManager = prefs,
                isArabic = false
            )
            assertEquals(1, enabledResult.addedCount)
            assertEquals(55.0, enabledResult.addedTotalDzd, 0.01)

            val txs = dao.getAllTransactionsList()
            assertEquals(1, txs.size)
            assertEquals(55.0, txs[0].amount, 0.01)
            assertTrue(txs[0].note.contains("[tax_ref:future_fee_2]"))

            val updatedAnomalies = com.example.sms.AlgPosteSmsCodec.parseTaxAnomalies(
                prefs.settingsFlow.value.detectedSmsTaxesJson
            )
            assertEquals(false, updatedAnomalies.first { it.id == "past_fee_1" }.isRecordedAsExpense)
            assertEquals(true, updatedAnomalies.first { it.id == "future_fee_2" }.isRecordedAsExpense)

            // Re-running must be idempotent (zero duplicates)
            val duplicateRun = com.example.sms.TaxExpenseAutoLogger.logFutureTaxesIfEnabled(
                dao = dao,
                prefsManager = prefs,
                isArabic = false
            )
            assertEquals(0, duplicateRun.addedCount)
            assertEquals(1, dao.getAllTransactionsList().size)

            // Disabling must block subsequent future taxes until re-enabled
            vm.setAutoAddTaxesEnabled(false)
            assertEquals(false, prefs.settingsFlow.value.autoAddTaxesEnabled)
            assertEquals(0L, prefs.settingsFlow.value.autoAddTaxesBaselineMillis)

            val evenLaterTax = futureTax.copy(id = "even_later_fee_3", detectedAtMillis = futureTime + 60_000L)
            prefs.updateSettings {
                it.copy(
                    detectedSmsTaxesJson = com.example.sms.AlgPosteSmsCodec.encodeTaxAnomalies(
                        updatedAnomalies + evenLaterTax
                    )
                )
            }
            val afterDisableResult = com.example.sms.TaxExpenseAutoLogger.logFutureTaxesIfEnabled(
                dao = dao,
                prefsManager = prefs,
                isArabic = false
            )
            assertEquals(0, afterDisableResult.addedCount)
            assertEquals(1, dao.getAllTransactionsList().size)
        } finally {
            db.close()
        }
    }
}
