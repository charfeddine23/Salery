package com.example.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.example.data.TransactionType
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.security.MessageDigest
import java.util.Locale
import kotlin.math.roundToLong

@JsonClass(generateAdapter = true)
data class SmsSuggestionEntry(
    val id: String = "",
    val sender: String = "ALG Poste",
    val rawBody: String = "",
    val amount: Double = 0.0,
    val isExpense: Boolean = true,
    val suggestedCategory: String = "other",
    val suggestedTitleAr: String = "",
    val suggestedTitleEn: String = "",
    val receivedAtMillis: Long = System.currentTimeMillis(),
    val matchedReason: String = "",
    val extractedBalance: Double? = null,
    val hasExplicitFeeKeyword: Boolean = false
) {
    val transactionType: TransactionType
        get() = if (isExpense) TransactionType.EXPENSE else TransactionType.INCOME
}

@JsonClass(generateAdapter = true)
data class SmsBalanceSnapshot(
    val id: String = "",
    val sender: String = "ALG Poste",
    val balance: Double = 0.0,
    val transactionAmount: Double = 0.0,
    val hasTransaction: Boolean = false,
    val isExpense: Boolean = true,
    val hasExplicitFeeKeyword: Boolean = false,
    val timestampMillis: Long = 0L,
    val rawBody: String = ""
)

@JsonClass(generateAdapter = true)
data class SmsTaxAnomalyEntry(
    val id: String = "",
    val smsId: String = "",
    val previousBalance: Double = 0.0,
    val transactionAmount: Double = 0.0,
    val isExpense: Boolean = true,
    val expectedBalance: Double = 0.0,
    val actualBalance: Double = 0.0,
    val taxAmount: Double = 0.0,
    val detectionKind: String = "BALANCE_DISCREPANCY", // "BALANCE_DISCREPANCY" or "EXPLICIT_TAX_FEE"
    val detectedAtMillis: Long = System.currentTimeMillis(),
    val titleAr: String = "",
    val titleEn: String = "",
    val explanationAr: String = "",
    val explanationEn: String = "",
    val rawBody: String = "",
    val isRecordedAsExpense: Boolean = false
)

data class InboxScanResult(
    val suggestions: List<SmsSuggestionEntry>,
    val snapshots: List<SmsBalanceSnapshot>
)

object AlgPosteSmsAnalyzer {

    private val debitKeywords = listOf(
        "retrait", "débit", "debit", "débité", "debite", "paiement", "payement",
        "tpe", "achat", "virement emis", "virement émis", "émis", "emis",
        "frais", "commission", "facture", "cotisation", "transfert sortant",
        "taxe", "taxes", "timbre", "agios", "redevance",
        "سحب", "خصم", "اقتطاع", "دفع", "شراء", "صرف", "تحويل صادر", "فاتورة",
        "ضريبة", "ضرائب", "رسوم", "رسم", "عمولة"
    )

    private val creditKeywords = listOf(
        "crédit", "credit", "crédité", "credite", "versement", "alimentation",
        "virement reçu", "virement recu", "reçu", "recu", "salaire", "dépôt",
        "depot", "remboursement", "virement entrant", "bonification",
        "إيداع", "تحويل وارد", "وارد", "استلام", "راتب", "مستحقات", "إضافة", "رصيد مضاف"
    )

    private val explicitFeeKeywords = listOf(
        "frais", "taxe", "taxes", "commission", "timbre", "tva", "agios",
        "cotisation", "redevance", "prelevement fiscal", "prélèvement fiscal",
        "ضريبة", "الضريبة", "ضرائب", "رسم", "رسوم", "عمولة", "اقتطاع", "طابع", "مصاريف بنكية"
    )

    private val balanceContextMarkers = listOf(
        "nouveau solde", "solde disponible", "solde actuel", "solde ccp",
        "n.solde", "solde", "balance",
        "الرصيد الجديد", "الرصيد الحالي", "الرصيد المتوفر", "الرصيد", "رصيدك الحالي", "رصيدك"
    )

    private val prioritizedNewBalanceRegex = Regex(
        """(?:nouveau\s+solde|solde\s+disponible|solde\s+actuel|n\.?\s*solde|الرصيد\s+الجديد|الرصيد\s+الحالي|الرصيد\s+المتوفر|رصيدك\s+الحالي)\s*(?:est|:|=|-|هو)?\s*([0-9][0-9\s.,]*)\s*(?:DA|DZD|د\.?ج)?""",
        RegexOption.IGNORE_CASE
    )

    private val genericBalanceRegex = Regex(
        """(?:solde(?:\s+ccp)?|balance|الرصيد|رصيدك)\s*(?:est|:|=|-|هو)?\s*([0-9][0-9\s.,]*)\s*(?:DA|DZD|د\.?ج)?""",
        RegexOption.IGNORE_CASE
    )

    private val otpNonTransactionMarkers = listOf(
        "code otp", "otp", "code de confirmation", "code de verification",
        "code de vérification", "mot de passe", "رمز التحقق", "كود التفعيل", "كلمة السر"
    )

    private val signedAmountRegex = Regex(
        """([+-])\s*([0-9][0-9\s.,]*)\s*(DA|DZD|د\.?ج)?""",
        RegexOption.IGNORE_CASE
    )

    private val currencyAmountRegex = Regex(
        """([0-9][0-9\s.,]*)\s*(DA|DZD|د\.?ج)""",
        RegexOption.IGNORE_CASE
    )

    fun isAlgPosteSender(sender: String): Boolean {
        val norm = normalizeArabicDigits(sender)
            .lowercase(Locale.US)
            .replace(Regex("[\\s._-]+"), "")
        if (norm.isBlank()) return false
        return norm.contains("algposte") ||
            norm.contains("algerieposte") ||
            norm.contains("algérieposte") ||
            norm.contains("baridimob") ||
            norm.contains("postedz") ||
            norm == "poste" ||
            norm.contains("بريدالجزائر") ||
            norm.contains("بريدي")
    }

    fun buildSuggestionId(sender: String, body: String, timestampMillis: Long): String {
        val minuteBucket = timestampMillis / 60_000L
        val payload = "${sender.trim().lowercase(Locale.US)}|${body.trim()}|$minuteBucket"
        val digest = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(Charsets.UTF_8))
        return digest.take(16).joinToString("") { "%02x".format(it) }
    }

    fun hasExplicitFeeKeyword(body: String): Boolean {
        val lower = normalizeArabicDigits(body).lowercase(Locale.US)
        return explicitFeeKeywords.any { lower.contains(it) }
    }

    fun extractBalanceFromBody(body: String): Double? {
        val normalizedBody = normalizeArabicDigits(body)
        if (normalizedBody.isBlank()) return null

        val prioritized = prioritizedNewBalanceRegex.findAll(normalizedBody)
            .mapNotNull { parseFlexibleAmount(it.groupValues[1]) }
            .filter { it >= 0.0 }
            .lastOrNull()
        if (prioritized != null) return roundTwoDecimals(prioritized)

        val generic = genericBalanceRegex.findAll(normalizedBody)
            .mapNotNull { parseFlexibleAmount(it.groupValues[1]) }
            .filter { it >= 0.0 }
            .lastOrNull()
        return generic?.let(::roundTwoDecimals)
    }

    fun parseBalanceSnapshot(
        sender: String,
        body: String,
        timestampMillis: Long = System.currentTimeMillis(),
        allowAnySenderForManualTest: Boolean = false
    ): SmsBalanceSnapshot? {
        val trimmedSender = sender.trim().ifBlank { "ALG Poste" }
        val trimmedBody = body.trim()
        if (trimmedBody.isBlank()) return null

        val senderMatches = isAlgPosteSender(trimmedSender) ||
            (allowAnySenderForManualTest && (trimmedSender.isNotBlank() || trimmedBody.isNotBlank()))
        if (!senderMatches) return null

        val normalizedBody = normalizeArabicDigits(trimmedBody)
        val lowerBody = normalizedBody.lowercase(Locale.US)
        val extractedBalance = extractBalanceFromBody(normalizedBody) ?: return null

        val txParsed = parseSms(
            sender = trimmedSender,
            body = trimmedBody,
            timestampMillis = timestampMillis,
            allowAnySenderForManualTest = allowAnySenderForManualTest
        )
        val id = txParsed?.id ?: buildSuggestionId(trimmedSender, trimmedBody, timestampMillis)

        return SmsBalanceSnapshot(
            id = id,
            sender = if (isAlgPosteSender(trimmedSender)) trimmedSender else "ALG Poste",
            balance = extractedBalance,
            transactionAmount = txParsed?.amount ?: 0.0,
            hasTransaction = txParsed != null,
            isExpense = txParsed?.isExpense ?: true,
            hasExplicitFeeKeyword = explicitFeeKeywords.any { lowerBody.contains(it) },
            timestampMillis = timestampMillis,
            rawBody = trimmedBody
        )
    }

    fun parseSms(
        sender: String,
        body: String,
        timestampMillis: Long = System.currentTimeMillis(),
        allowAnySenderForManualTest: Boolean = false
    ): SmsSuggestionEntry? {
        val trimmedSender = sender.trim().ifBlank { "ALG Poste" }
        val trimmedBody = body.trim()
        if (trimmedBody.isBlank()) return null

        val senderMatches = isAlgPosteSender(trimmedSender) ||
            (allowAnySenderForManualTest && (trimmedSender.isNotBlank() || trimmedBody.isNotBlank()))
        if (!senderMatches) return null

        val normalizedBody = normalizeArabicDigits(trimmedBody)
        val lowerBody = normalizedBody.lowercase(Locale.US)

        // Ignore pure OTP / authentication codes that do not contain currency amounts
        val hasCurrencyToken = lowerBody.contains(" da") ||
            lowerBody.endsWith("da") ||
            lowerBody.contains("dzd") ||
            lowerBody.contains("دج") ||
            lowerBody.contains("د.ج")
        if (!hasCurrencyToken && otpNonTransactionMarkers.any { lowerBody.contains(it) }) {
            return null
        }

        val hasDebitKeyword = debitKeywords.any { lowerBody.contains(it) }
        val hasCreditKeyword = creditKeywords.any { lowerBody.contains(it) }
        val extractedBalance = extractBalanceFromBody(normalizedBody)
        val isExplicitFee = explicitFeeKeywords.any { lowerBody.contains(it) }

        // 1. Prioritize explicitly signed amounts (e.g. -300.00 DA or +5000.00 DA), ignoring Solde/Balance lines
        var pickedAmount: Double? = null
        var pickedIsExpense: Boolean? = null
        var matchedReason = ""

        val signedMatches = signedAmountRegex.findAll(normalizedBody).toList()
        for (match in signedMatches) {
            if (isInsideBalanceLabel(lowerBody, match.range.first)) continue
            val sign = match.groupValues[1]
            val numStr = match.groupValues[2]
            val parsed = parseFlexibleAmount(numStr) ?: continue
            if (parsed <= 0.0) continue

            // Avoid matching dates like "-2026" unless currency present or number looks like an amount
            val hasCurrency = match.groupValues[3].isNotBlank()
            if (!hasCurrency && parsed in 1900.0..2100.0 && !hasDebitKeyword && !hasCreditKeyword) continue

            pickedAmount = parsed
            pickedIsExpense = (sign == "-")
            matchedReason = if (sign == "-") "SIGN_MINUS" else "SIGN_PLUS"
            break
        }

        // 2. Fallback to unsigned currency amount (e.g. "300.00 DA") + transaction keyword direction
        if (pickedAmount == null) {
            val currencyMatches = currencyAmountRegex.findAll(normalizedBody).toList()
            for (match in currencyMatches) {
                if (isInsideBalanceLabel(lowerBody, match.range.first)) continue
                val parsed = parseFlexibleAmount(match.groupValues[1]) ?: continue
                if (parsed <= 0.0) continue

                if (hasDebitKeyword && !hasCreditKeyword) {
                    pickedAmount = parsed
                    pickedIsExpense = true
                    matchedReason = "DEBIT_KEYWORD"
                    break
                } else if (hasCreditKeyword && !hasDebitKeyword) {
                    pickedAmount = parsed
                    pickedIsExpense = false
                    matchedReason = "CREDIT_KEYWORD"
                    break
                } else if (hasDebitKeyword && hasCreditKeyword) {
                    val firstDebitIdx = debitKeywords.mapNotNull { kw ->
                        lowerBody.indexOf(kw).takeIf { it >= 0 }
                    }.minOrNull() ?: Int.MAX_VALUE
                    val firstCreditIdx = creditKeywords.mapNotNull { kw ->
                        lowerBody.indexOf(kw).takeIf { it >= 0 }
                    }.minOrNull() ?: Int.MAX_VALUE
                    pickedAmount = parsed
                    pickedIsExpense = firstDebitIdx <= firstCreditIdx
                    matchedReason = if (pickedIsExpense == true) "DEBIT_KEYWORD" else "CREDIT_KEYWORD"
                    break
                }
            }
        }

        val finalAmount = pickedAmount ?: return null
        val finalIsExpense = pickedIsExpense ?: return null

        val (category, titleAr, titleEn) = classifyMetadata(lowerBody, finalIsExpense, isExplicitFee)
        val id = buildSuggestionId(trimmedSender, trimmedBody, timestampMillis)

        return SmsSuggestionEntry(
            id = id,
            sender = if (isAlgPosteSender(trimmedSender)) trimmedSender else "ALG Poste",
            rawBody = trimmedBody,
            amount = roundTwoDecimals(finalAmount),
            isExpense = finalIsExpense,
            suggestedCategory = category,
            suggestedTitleAr = titleAr,
            suggestedTitleEn = titleEn,
            receivedAtMillis = timestampMillis,
            matchedReason = matchedReason,
            extractedBalance = extractedBalance,
            hasExplicitFeeKeyword = isExplicitFee
        )
    }

    /**
     * Analyzes chronological ALG Poste balance snapshots to detect:
     * 1. Mathematical discrepancy between expected balance (prev +/- tx) and actual reported balance,
     *    catching hidden bank taxes, commissions, and unannounced deductions.
     * 2. Explicit fee/tax SMS debits when already directly billed as a standalone fee message.
     */
    fun detectTaxAnomalies(
        snapshots: List<SmsBalanceSnapshot>,
        existingAnomalies: List<SmsTaxAnomalyEntry> = emptyList()
    ): List<SmsTaxAnomalyEntry> {
        if (snapshots.isEmpty()) return existingAnomalies

        val ordered = snapshots
            .distinctBy { it.id }
            .sortedWith(compareBy<SmsBalanceSnapshot> { it.timestampMillis }.thenBy { it.id })
        val existingById = existingAnomalies.associateBy { it.id }
        val resultMap = LinkedHashMap<String, SmsTaxAnomalyEntry>()
        existingAnomalies.forEach { resultMap[it.id] = it }

        for (i in ordered.indices) {
            val curr = ordered[i]
            val prev = if (i > 0) ordered[i - 1] else null

            if (prev != null) {
                val expectedBalance = roundTwoDecimals(
                    if (curr.hasTransaction) {
                        if (curr.isExpense) prev.balance - curr.transactionAmount
                        else prev.balance + curr.transactionAmount
                    } else {
                        prev.balance
                    }
                )
                val discrepancy = roundTwoDecimals(expectedBalance - curr.balance)

                if (discrepancy >= 0.01) {
                    val anomalyId = "tax_diff_${curr.id}"
                    val alreadyRecorded = existingById[anomalyId]?.isRecordedAsExpense == true
                    val isLikelyFee = curr.hasExplicitFeeKeyword ||
                        !curr.hasTransaction ||
                        discrepancy <= maxOf(500.0, curr.transactionAmount * 0.25)

                    val signStr = if (curr.hasTransaction) {
                        if (curr.isExpense) " - ${formatCompactAmount(curr.transactionAmount)}"
                        else " + ${formatCompactAmount(curr.transactionAmount)}"
                    } else ""

                    val titleAr = when {
                        !curr.hasTransaction -> "فارق رصيد / اقتطاع ضرائب غير معلن"
                        isLikelyFee -> "رصد ضريبة / عمولة مقتطعة من الرصيد"
                        else -> "فارق رصيد / خصم إضافي مكتشف"
                    }
                    val titleEn = when {
                        !curr.hasTransaction -> "Unannounced Balance Deduction / Tax"
                        isLikelyFee -> "Hidden Bank Fee / Tax Detected"
                        else -> "Balance Discrepancy / Deduction Caught"
                    }
                    val explanationAr = if (curr.hasTransaction) {
                        "السابق ${formatCompactAmount(prev.balance)}$signStr = المتوقع ${formatCompactAmount(expectedBalance)} • الفعلي ${formatCompactAmount(curr.balance)} ⟵ الفارق المقتطع: ${formatCompactAmount(discrepancy)} دج"
                    } else {
                        "الرصيد السابق ${formatCompactAmount(prev.balance)} • الرصيد الحالي ${formatCompactAmount(curr.balance)} ⟵ الفارق المقتطع: ${formatCompactAmount(discrepancy)} دج"
                    }
                    val explanationEn = if (curr.hasTransaction) {
                        "Prev ${formatCompactAmount(prev.balance)}$signStr = Expected ${formatCompactAmount(expectedBalance)} • Actual ${formatCompactAmount(curr.balance)} ⟵ Caught tax/fee: ${formatCompactAmount(discrepancy)} DA"
                    } else {
                        "Prev ${formatCompactAmount(prev.balance)} • Actual ${formatCompactAmount(curr.balance)} ⟵ Caught deduction: ${formatCompactAmount(discrepancy)} DA"
                    }

                    resultMap[anomalyId] = SmsTaxAnomalyEntry(
                        id = anomalyId,
                        smsId = curr.id,
                        previousBalance = prev.balance,
                        transactionAmount = curr.transactionAmount,
                        isExpense = curr.isExpense,
                        expectedBalance = expectedBalance,
                        actualBalance = curr.balance,
                        taxAmount = discrepancy,
                        detectionKind = "BALANCE_DISCREPANCY",
                        detectedAtMillis = curr.timestampMillis,
                        titleAr = titleAr,
                        titleEn = titleEn,
                        explanationAr = explanationAr,
                        explanationEn = explanationEn,
                        rawBody = curr.rawBody,
                        isRecordedAsExpense = alreadyRecorded
                    )
                    continue
                }
            }

            // Explicit standalone fee/tax message (e.g. Frais de tenue de compte -250 DA)
            if (curr.hasExplicitFeeKeyword && curr.hasTransaction && curr.isExpense && curr.transactionAmount > 0.0) {
                val anomalyId = "tax_fee_${curr.id}"
                val alreadyRecorded = existingById[anomalyId]?.isRecordedAsExpense == true
                resultMap[anomalyId] = SmsTaxAnomalyEntry(
                    id = anomalyId,
                    smsId = curr.id,
                    previousBalance = prev?.balance ?: (curr.balance + curr.transactionAmount),
                    transactionAmount = curr.transactionAmount,
                    isExpense = true,
                    expectedBalance = curr.balance,
                    actualBalance = curr.balance,
                    taxAmount = curr.transactionAmount,
                    detectionKind = "EXPLICIT_TAX_FEE",
                    detectedAtMillis = curr.timestampMillis,
                    titleAr = "رسوم / ضريبة صريحة من بريد الجزائر",
                    titleEn = "Explicit ALG Poste Fee / Tax",
                    explanationAr = "اقتطاع مباشر مصنف كرسوم أو ضريبة بقيمة ${formatCompactAmount(curr.transactionAmount)} دج",
                    explanationEn = "Direct fee/tax debit detected for ${formatCompactAmount(curr.transactionAmount)} DA",
                    rawBody = curr.rawBody,
                    isRecordedAsExpense = alreadyRecorded
                )
            }
        }

        return resultMap.values
            .sortedByDescending { it.detectedAtMillis }
    }

    private fun isInsideBalanceLabel(lowerText: String, matchStart: Int): Boolean {
        val windowStart = (matchStart - 30).coerceAtLeast(0)
        val prefix = lowerText.substring(windowStart, matchStart)
        return balanceContextMarkers.any { marker -> prefix.contains(marker) }
    }

    private fun classifyMetadata(
        lowerBody: String,
        isExpense: Boolean,
        isExplicitFee: Boolean
    ): Triple<String, String, String> {
        return if (isExpense) {
            when {
                isExplicitFee ->
                    Triple("taxes_fees", "ضرائب ورسوم (ALG Poste)", "Bank Tax / Fee (ALG Poste)")

                lowerBody.contains("mobilis") || lowerBody.contains("djezzy") ||
                    lowerBody.contains("ooredoo") || lowerBody.contains("internet") ||
                    lowerBody.contains("recharge") || lowerBody.contains("فليكسي") ->
                    Triple("internet", "شحن واتصالات (ALG Poste)", "Telecom & Recharge (ALG Poste)")

                lowerBody.contains("sonelgaz") || lowerBody.contains("ade") ||
                    lowerBody.contains("facture") || lowerBody.contains("فاتورة") ->
                    Triple("utilities", "دفع فاتورة (ALG Poste)", "Bill Payment (ALG Poste)")

                lowerBody.contains("tpe") || lowerBody.contains("achat") ||
                    lowerBody.contains("paiement") || lowerBody.contains("شراء") ->
                    Triple("shopping", "دفع / شراء (ALG Poste)", "Card Payment (ALG Poste)")

                lowerBody.contains("retrait") || lowerBody.contains("gab") || lowerBody.contains("سحب") ->
                    Triple("other", "سحب نقدي (ALG Poste)", "ATM Withdrawal (ALG Poste)")

                else ->
                    Triple("other", "سحب / مصروف بريدي موب", "BaridiMob Expense (ALG Poste)")
            }
        } else {
            when {
                lowerBody.contains("salaire") || lowerBody.contains("راتب") || lowerBody.contains("paie") ->
                    Triple("salary", "راتب وارد (ALG Poste)", "Salary Credit (ALG Poste)")

                lowerBody.contains("virement") || lowerBody.contains("تحويل") ->
                    Triple("transfer", "تحويل مالي وارد (ALG Poste)", "Incoming Transfer (ALG Poste)")

                else ->
                    Triple("other_income", "إيداع / دخل بريدي موب", "BaridiMob Income (ALG Poste)")
            }
        }
    }

    internal fun parseFlexibleAmount(rawNumber: String): Double? {
        val compact = rawNumber.replace(Regex("[\\s\u00A0]+"), "").trim()
        if (compact.isEmpty()) return null

        val normalized = when {
            compact.contains(',') && compact.contains('.') -> {
                if (compact.lastIndexOf(',') > compact.lastIndexOf('.')) {
                    compact.replace(".", "").replace(',', '.')
                } else {
                    compact.replace(",", "")
                }
            }
            compact.contains(',') -> {
                val afterComma = compact.substringAfterLast(',')
                if (afterComma.length <= 2) {
                    compact.replace(',', '.')
                } else {
                    compact.replace(",", "")
                }
            }
            else -> compact
        }
        return normalized.toDoubleOrNull()
    }

    private fun roundTwoDecimals(value: Double): Double =
        (value * 100.0).roundToLong() / 100.0

    private fun formatCompactAmount(value: Double): String =
        if (value % 1.0 == 0.0) String.format(Locale.US, "%,.0f", value)
        else String.format(Locale.US, "%,.2f", value)

    private fun normalizeArabicDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            val mapped = when (ch) {
                in '٠'..'٩' -> ('0'.code + (ch.code - '٠'.code)).toChar()
                in '۰'..'۹' -> ('0'.code + (ch.code - '۰'.code)).toChar()
                '٫' -> '.'
                '٬' -> ','
                '−', '–', '—' -> '-'
                else -> ch
            }
            sb.append(mapped)
        }
        return sb.toString()
    }
}

object AlgPosteSmsCodec {
    private const val MAX_PROCESSED_KEYS = 500
    private const val MAX_PENDING_ITEMS = 120
    private const val MAX_SNAPSHOT_ITEMS = 200
    private const val MAX_TAX_ITEMS = 120

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val suggestionListAdapter by lazy {
        val type = Types.newParameterizedType(List::class.java, SmsSuggestionEntry::class.java)
        moshi.adapter<List<SmsSuggestionEntry>>(type)
    }

    private val snapshotListAdapter by lazy {
        val type = Types.newParameterizedType(List::class.java, SmsBalanceSnapshot::class.java)
        moshi.adapter<List<SmsBalanceSnapshot>>(type)
    }

    private val taxListAdapter by lazy {
        val type = Types.newParameterizedType(List::class.java, SmsTaxAnomalyEntry::class.java)
        moshi.adapter<List<SmsTaxAnomalyEntry>>(type)
    }

    private val stringListAdapter by lazy {
        val type = Types.newParameterizedType(List::class.java, String::class.java)
        moshi.adapter<List<String>>(type)
    }

    fun parseSuggestions(json: String): List<SmsSuggestionEntry> {
        if (json.isBlank()) return emptyList()
        return try {
            suggestionListAdapter.fromJson(json)
                ?.filter { it.id.isNotBlank() && it.amount > 0.0 }
                ?.sortedByDescending { it.receivedAtMillis }
                ?.take(MAX_PENDING_ITEMS)
                ?: emptyList()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun encodeSuggestions(list: List<SmsSuggestionEntry>): String {
        return try {
            suggestionListAdapter.toJson(
                list.distinctBy { it.id }
                    .sortedByDescending { it.receivedAtMillis }
                    .take(MAX_PENDING_ITEMS)
            )
        } catch (_: Throwable) {
            "[]"
        }
    }

    fun parseBalanceHistory(json: String): List<SmsBalanceSnapshot> {
        if (json.isBlank()) return emptyList()
        return try {
            snapshotListAdapter.fromJson(json)
                ?.filter { it.id.isNotBlank() && it.balance >= 0.0 }
                ?.sortedBy { it.timestampMillis }
                ?.takeLast(MAX_SNAPSHOT_ITEMS)
                ?: emptyList()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun encodeBalanceHistory(list: List<SmsBalanceSnapshot>): String {
        return try {
            snapshotListAdapter.toJson(
                list.distinctBy { it.id }
                    .sortedBy { it.timestampMillis }
                    .takeLast(MAX_SNAPSHOT_ITEMS)
            )
        } catch (_: Throwable) {
            "[]"
        }
    }

    fun parseTaxAnomalies(json: String): List<SmsTaxAnomalyEntry> {
        if (json.isBlank()) return emptyList()
        return try {
            taxListAdapter.fromJson(json)
                ?.filter { it.id.isNotBlank() && it.taxAmount > 0.0 }
                ?.sortedByDescending { it.detectedAtMillis }
                ?.take(MAX_TAX_ITEMS)
                ?: emptyList()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun encodeTaxAnomalies(list: List<SmsTaxAnomalyEntry>): String {
        return try {
            taxListAdapter.toJson(
                list.distinctBy { it.id }
                    .sortedByDescending { it.detectedAtMillis }
                    .take(MAX_TAX_ITEMS)
            )
        } catch (_: Throwable) {
            "[]"
        }
    }

    fun parseProcessedKeys(json: String): Set<String> {
        if (json.isBlank()) return emptySet()
        return try {
            stringListAdapter.fromJson(json)?.toSet() ?: emptySet()
        } catch (_: Throwable) {
            emptySet()
        }
    }

    fun encodeProcessedKeys(keys: Collection<String>): String {
        return try {
            stringListAdapter.toJson(keys.distinct().takeLast(MAX_PROCESSED_KEYS))
        } catch (_: Throwable) {
            "[]"
        }
    }
}

object AlgPosteSmsInboxScanner {

    fun hasSmsPermissions(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun scanInbox(
        context: Context,
        maxRowsToInspect: Int = 250,
        minTimestampMillis: Long = 0L
    ): List<SmsSuggestionEntry> =
        scanInboxWithBalances(context, maxRowsToInspect, minTimestampMillis).suggestions

    fun scanInboxWithBalances(
        context: Context,
        maxRowsToInspect: Int = 300,
        minTimestampMillis: Long = 0L
    ): InboxScanResult {
        if (!hasSmsPermissions(context)) {
            return InboxScanResult(emptyList(), emptyList())
        }

        val suggestions = mutableListOf<SmsSuggestionEntry>()
        val snapshots = mutableListOf<SmsBalanceSnapshot>()
        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )

        val selection = if (minTimestampMillis > 0L) "${Telephony.Sms.DATE} > ?" else null
        val selectionArgs = if (minTimestampMillis > 0L) arrayOf(minTimestampMillis.toString()) else null

        try {
            context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${Telephony.Sms.DATE} DESC"
            )?.use { cursor ->
                val addrIdx = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIdx = cursor.getColumnIndex(Telephony.Sms.BODY)
                val dateIdx = cursor.getColumnIndex(Telephony.Sms.DATE)

                var inspected = 0
                while (cursor.moveToNext() && inspected < maxRowsToInspect) {
                    inspected++
                    val sender = if (addrIdx != -1) cursor.getString(addrIdx).orEmpty() else ""
                    val body = if (bodyIdx != -1) cursor.getString(bodyIdx).orEmpty() else ""
                    val dateMillis = if (dateIdx != -1) cursor.getLong(dateIdx) else System.currentTimeMillis()

                    if (minTimestampMillis > 0L && dateMillis <= minTimestampMillis) continue
                    if (!AlgPosteSmsAnalyzer.isAlgPosteSender(sender)) continue

                    AlgPosteSmsAnalyzer.parseSms(
                        sender = sender,
                        body = body,
                        timestampMillis = dateMillis,
                        allowAnySenderForManualTest = false
                    )?.let { suggestions.add(it) }

                    AlgPosteSmsAnalyzer.parseBalanceSnapshot(
                        sender = sender,
                        body = body,
                        timestampMillis = dateMillis,
                        allowAnySenderForManualTest = false
                    )?.let { snapshots.add(it) }
                }
            }
        } catch (_: Throwable) {
        }

        return InboxScanResult(
            suggestions = suggestions.distinctBy { it.id }.sortedByDescending { it.receivedAtMillis },
            snapshots = snapshots.distinctBy { it.id }.sortedBy { it.timestampMillis }
        )
    }

    fun scanLatestBalanceSnapshot(context: Context, maxRowsToInspect: Int = 200): SmsBalanceSnapshot? {
        if (!hasSmsPermissions(context)) return null
        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )
        try {
            context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                projection,
                null,
                null,
                "${Telephony.Sms.DATE} DESC"
            )?.use { cursor ->
                val addrIdx = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIdx = cursor.getColumnIndex(Telephony.Sms.BODY)
                val dateIdx = cursor.getColumnIndex(Telephony.Sms.DATE)
                var inspected = 0
                while (cursor.moveToNext() && inspected < maxRowsToInspect) {
                    inspected++
                    val sender = if (addrIdx != -1) cursor.getString(addrIdx).orEmpty() else ""
                    val body = if (bodyIdx != -1) cursor.getString(bodyIdx).orEmpty() else ""
                    val dateMillis = if (dateIdx != -1) cursor.getLong(dateIdx) else System.currentTimeMillis()
                    if (!AlgPosteSmsAnalyzer.isAlgPosteSender(sender)) continue
                    val snap = AlgPosteSmsAnalyzer.parseBalanceSnapshot(
                        sender = sender,
                        body = body,
                        timestampMillis = dateMillis,
                        allowAnySenderForManualTest = false
                    )
                    if (snap != null) return snap
                }
            }
        } catch (_: Throwable) {
        }
        return null
    }
}
