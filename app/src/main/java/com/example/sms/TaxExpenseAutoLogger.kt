package com.example.sms

import com.example.data.AppDao
import com.example.data.PreferencesManager
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.abs

data class AutoTaxLogResult(
    val addedCount: Int = 0,
    val addedTotalDzd: Double = 0.0,
    val newlyLoggedIds: Set<String> = emptySet()
)

object TaxExpenseAutoLogger {

    private val mutex = Mutex()

    fun isTaxAnomalyAlreadyLogged(
        anomaly: SmsTaxAnomalyEntry,
        existingTransactions: List<TransactionEntity>
    ): Boolean {
        if (anomaly.isRecordedAsExpense) return true
        val refTag = "[tax_ref:${anomaly.id}]"
        return existingTransactions.any { tx ->
            tx.type == TransactionType.EXPENSE && (
                tx.note.contains(refTag) ||
                    (tx.category.equals("taxes_fees", ignoreCase = true) &&
                        abs(tx.amount - anomaly.taxAmount) < 0.01 &&
                        abs(tx.dateMillis - anomaly.detectedAtMillis) <= 5_000L)
                )
        }
    }

    /**
     * Filters unlogged, valid (<= MAX_CAUGHT_TAX_AMOUNT_DZD) tax anomalies.
     * When [minTimestampMillis] > 0, strictly excludes all historical/previous taxes
     * whose [SmsTaxAnomalyEntry.detectedAtMillis] is older than [minTimestampMillis].
     */
    fun findUnloggedTaxAnomalies(
        anomalies: List<SmsTaxAnomalyEntry>,
        existingTransactions: List<TransactionEntity>,
        minTimestampMillis: Long = 0L
    ): List<SmsTaxAnomalyEntry> {
        return anomalies
            .asSequence()
            .filter { entry ->
                entry.taxAmount.isFinite() &&
                    entry.taxAmount > 0.0 &&
                    entry.taxAmount <= AlgPosteSmsAnalyzer.MAX_CAUGHT_TAX_AMOUNT_DZD &&
                    (minTimestampMillis <= 0L || entry.detectedAtMillis >= minTimestampMillis) &&
                    !isTaxAnomalyAlreadyLogged(entry, existingTransactions)
            }
            .distinctBy { it.id }
            .toList()
    }

    /**
     * Automatically inserts unlogged taxes ONLY when Auto Add Taxes is enabled AND
     * a valid activation baseline timestamp is set. Taxes detected before the activation
     * baseline are never added automatically.
     */
    suspend fun logFutureTaxesIfEnabled(
        dao: AppDao,
        prefsManager: PreferencesManager,
        isArabic: Boolean,
        candidateAnomalies: List<SmsTaxAnomalyEntry>? = null
    ): AutoTaxLogResult = mutex.withLock {
        val current = prefsManager.settingsFlow.value
        val baselineMillis = current.autoAddTaxesBaselineMillis
        if (!current.autoAddTaxesEnabled || baselineMillis <= 0L) {
            return@withLock AutoTaxLogResult()
        }

        val currentTaxes = candidateAnomalies
            ?: AlgPosteSmsCodec.parseTaxAnomalies(current.detectedSmsTaxesJson)
        if (currentTaxes.isEmpty()) {
            return@withLock AutoTaxLogResult()
        }

        val existingTransactions = dao.getAllTransactionsList()
        val futureUnlogged = findUnloggedTaxAnomalies(
            anomalies = currentTaxes,
            existingTransactions = existingTransactions,
            minTimestampMillis = baselineMillis
        )

        val newlyLoggedIds = mutableSetOf<String>()
        var addedTotal = 0.0
        for (tax in futureUnlogged) {
            val rawExplanation = (if (isArabic) tax.explanationAr else tax.explanationEn)
                .ifBlank { tax.rawBody }
                .trim()
            val refTag = "[tax_ref:${tax.id}]"
            val composedNote = if (rawExplanation.isBlank()) {
                refTag
            } else {
                "${rawExplanation.take((240 - refTag.length - 1).coerceAtLeast(0))} $refTag"
            }
            dao.insertTransaction(
                TransactionEntity(
                    type = TransactionType.EXPENSE,
                    amount = tax.taxAmount,
                    category = "taxes_fees",
                    title = if (isArabic) tax.titleAr.ifBlank { "ضرائب ورسوم بريدي موب" }
                    else tax.titleEn.ifBlank { "BaridiMob Tax / Fee" },
                    note = composedNote.take(240),
                    dateMillis = tax.detectedAtMillis
                )
            )
            newlyLoggedIds.add(tax.id)
            addedTotal += tax.taxAmount
        }

        val matchedFutureIds = currentTaxes
            .filter {
                it.detectedAtMillis >= baselineMillis &&
                    (it.id in newlyLoggedIds || isTaxAnomalyAlreadyLogged(it, existingTransactions))
            }
            .map { it.id }
            .toSet()

        if (matchedFutureIds.isNotEmpty()) {
            prefsManager.updateSettings { latest ->
                val updated = AlgPosteSmsCodec.parseTaxAnomalies(latest.detectedSmsTaxesJson).map { item ->
                    if (item.id in matchedFutureIds) item.copy(isRecordedAsExpense = true) else item
                }
                latest.copy(detectedSmsTaxesJson = AlgPosteSmsCodec.encodeTaxAnomalies(updated))
            }
        }

        AutoTaxLogResult(
            addedCount = futureUnlogged.size,
            addedTotalDzd = addedTotal,
            newlyLoggedIds = newlyLoggedIds
        )
    }
}
