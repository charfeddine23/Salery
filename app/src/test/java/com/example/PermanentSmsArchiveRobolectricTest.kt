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
                if (kotlin.math.abs(cur.totalBalance - 49500.0) < 0.01 &&
                    kotlin.math.abs(cur.savingsBalance - 12000.0) < 0.01
                ) {
                    break
                }
                kotlinx.coroutines.delay(50)
            }
            val syncedSummary = vm.financialSummary.value
            assertEquals(49500.0, syncedSummary.totalBalance, 0.01)
            assertEquals(49500.0, syncedSummary.grossBalance, 0.01)
            assertEquals(12000.0, syncedSummary.savingsBalance, 0.01)
            assertEquals(99999.75, externalPrefs.settingsFlow.value.latestSmsBalanceAmount, 0.01)
        } finally {
            collectJob.cancel()
        }
    }
}
