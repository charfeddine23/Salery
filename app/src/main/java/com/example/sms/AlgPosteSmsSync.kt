package com.example.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.example.data.ArchivedAlgPosteSmsEntity
import com.example.data.TransactionType
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.security.MessageDigest
import java.util.Locale
import kotlin.math.roundToInt
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

data class RawAlgPosteSms(
    val sender: String,
    val body: String,
    val dateMillis: Long
)

data class InboxScanResult(
    val suggestions: List<SmsSuggestionEntry>,
    val snapshots: List<SmsBalanceSnapshot>,
    val rawMessages: List<RawAlgPosteSms> = emptyList()
)

data class FeePatternStat(
    val amountDzd: Double,
    val occurrences: Int
)

data class ArchivedSmsStudyReport(
    val totalArchived: Int = 0,
    val fullMatchCount: Int = 0,
    val balanceExtractedCount: Int = 0,
    val amountOnlyCount: Int = 0,
    val needsStudyCount: Int = 0,
    val otpIgnoredCount: Int = 0,
    val recognitionAccuracyPercent: Int = 100,
    val learnedFeePatterns: List<FeePatternStat> = emptyList(),
    val totalValidCaughtTaxesDzd: Double = 0.0
)

object AlgPosteSmsAnalyzer {

    /**
     * Caught Tax ignores any balance discrepancy or explicit fee above 1,000 DZD,
     * since larger jumps indicate unlogged transfers/withdrawals or missing SMS rather than taxes.
     */
    const val MAX_CAUGHT_TAX_AMOUNT_DZD = 1000.0

    private val debitKeywords = listOf(
        "retrait", "retire", "retiré", "débit", "debit", "débité", "debite",
        "prélevé", "preleve", "prélèvement", "prelevement", "paiement", "payement",
        "tpe", "achat", "virement emis", "virement émis", "virement effectue",
        "virement effectué", "émis", "emis", "frais", "commission", "facture",
        "cotisation", "transfert sortant", "taxe", "taxes", "timbre", "agios", "redevance",
        "سحب", "خصم", "حسم", "اقتطاع", "دفع", "شراء", "صرف", "تحويل صادر", "فاتورة",
        "ضريبة", "ضرائب", "رسوم", "رسم", "عمولة"
    )

    private val creditKeywords = listOf(
        "crédit", "credit", "crédité", "credite", "versement", "alimentation",
        "virement reçu", "virement recu", "reçu", "recu", "salaire", "dépôt",
        "depot", "remboursement", "virement entrant", "bonification", "encaissement",
        "إيداع", "تحويل وارد", "وارد", "استلام", "راتب", "مستحقات", "إضافة", "رصيد مضاف", "صب"
    )

    private val explicitFeeKeywords = listOf(
        "frais", "taxe", "taxes", "commission", "timbre", "tva", "agios",
        "cotisation", "redevance", "prelevement fiscal", "prélèvement fiscal",
        "ضريبة", "الضريبة", "ضرائب", "رسم", "رسوم", "عمولة", "اقتطاع", "طابع", "مصاريف بنكية"
    )

    private val balanceContextMarkers = listOf(
        "nouveau solde", "solde disponible", "solde actuel", "solde comptable",
        "solde courant", "solde ccp", "solde final", "solde restant",
        "n.solde", "solde", "balance", "avoir",
        "الرصيد الجديد", "الرصيد الحالي", "الرصيد المتاح", "الرصيد المتوفر",
        "الرصيد البريدي", "الرصيد المتبقي", "الرصيد", "رصيدك الحالي", "رصيدكم", "رصيدك", "رصيد"
    )

    private const val CURRENCY_PATTERN = """(?:DA|DZD|DZ|د\.?\s*ج|دج|دينار)"""
    private const val NUMBER_PATTERN =
        """([0-9]+(?:[ \t\u00A0.,][0-9]{3})*(?:[.,][0-9]{1,2})?|[0-9]+(?:[.,][0-9]{1,2})?)"""
    private const val PRIORITIZED_BALANCE_LABELS =
        """(?:nouveau\s+solde|solde\s+(?:disponible|actuel|comptable|courant|final|net|restant|apr[eè]s\s+op[eé]ration)|n\.?\s*solde|الرصيد\s+(?:الجديد|الحالي|المتاح|المتوفر|المتبقي|البريدي|النهائي)|رصيدك\s+(?:الحالي|الجديد|المتاح)|رصيد\s+(?:الحساب|حسابك|حسابكم|البطاقة))"""
    private const val GENERIC_BALANCE_LABELS =
        """(?:(?<!ancien\s)(?<!ancien\s{2})solde(?!\s+(?:pr[eé]c[eé]dent|precedent|initial|avant|ant[eé]rieur|anterieur))(?:\s+ccp)?|balance|avoir|الرصيد(?!\s+(?:السابق|القديم))|رصيدكم|رصيدك|رصيد)"""
    private const val PREVIOUS_BALANCE_LABELS =
        """(?:ancien\s+solde|solde\s+(?:pr[eé]c[eé]dent|precedent|initial|avant|ant[eé]rieur|anterieur)|الرصيد\s+(?:السابق|القديم)|رصيدك\s+السابق)"""
    private const val IMMEDIATE_CONNECTORS =
        """(?:\s*(?:est\s+de|est|de|du|au|le|à|a|هو|الحالي|الجديد|السابق|المتاح|بـ|ب|:|=|-|،)\s*){0,4}"""

    private val prioritizedBalanceCurrencyRegex = Regex(
        """$PRIORITIZED_BALANCE_LABELS[^\r\n]{0,60}?(?<![0-9/:\-])$NUMBER_PATTERN(?![ \t]*[/:\-]\d)\s*$CURRENCY_PATTERN(?![\p{L}])""",
        RegexOption.IGNORE_CASE
    )

    private val prioritizedBalanceNoCurrencyRegex = Regex(
        """$PRIORITIZED_BALANCE_LABELS$IMMEDIATE_CONNECTORS(?<![0-9/:\-])$NUMBER_PATTERN(?![ \t]*[/:\-]\d)(?:\s*$CURRENCY_PATTERN)?""",
        RegexOption.IGNORE_CASE
    )

    private val genericBalanceCurrencyRegex = Regex(
        """$GENERIC_BALANCE_LABELS[^\r\n]{0,60}?(?<![0-9/:\-])$NUMBER_PATTERN(?![ \t]*[/:\-]\d)\s*$CURRENCY_PATTERN(?![\p{L}])""",
        RegexOption.IGNORE_CASE
    )

    private val genericBalanceNoCurrencyRegex = Regex(
        """$GENERIC_BALANCE_LABELS$IMMEDIATE_CONNECTORS(?<![0-9/:\-])$NUMBER_PATTERN(?![ \t]*[/:\-]\d)(?:\s*$CURRENCY_PATTERN)?""",
        RegexOption.IGNORE_CASE
    )

    private val reversedBalanceRegex = Regex(
        """(?<![0-9/:\-])$NUMBER_PATTERN\s*$CURRENCY_PATTERN\s*(?:est\s+(?:votre\s+)?(?:nouveau\s+)?solde|هو\s*رصيدك)""",
        RegexOption.IGNORE_CASE
    )

    private val previousBalanceCurrencyRegex = Regex(
        """$PREVIOUS_BALANCE_LABELS[^\r\n]{0,50}?(?<![0-9/:\-])$NUMBER_PATTERN(?![ \t]*[/:\-]\d)\s*$CURRENCY_PATTERN(?![\p{L}])""",
        RegexOption.IGNORE_CASE
    )

    private val previousBalanceNoCurrencyRegex = Regex(
        """$PREVIOUS_BALANCE_LABELS$IMMEDIATE_CONNECTORS(?<![0-9/:\-])$NUMBER_PATTERN(?![ \t]*[/:\-]\d)(?:\s*$CURRENCY_PATTERN)?""",
        RegexOption.IGNORE_CASE
    )

    private val otpNonTransactionMarkers = listOf(
        "code otp", "otp", "code de confirmation", "code de verification",
        "code de vérification", "mot de passe", "رمز التحقق", "كود التفعيل", "كلمة السر"
    )

    private val signedAmountRegex = Regex(
        """([+-])[ \t\u00A0]*$NUMBER_PATTERN(?![ \t]*[/:\-]\d)(?:[ \t\u00A0]*($CURRENCY_PATTERN))?""",
        RegexOption.IGNORE_CASE
    )

    private val currencyAmountRegex = Regex(
        """(?<![0-9/:\-])$NUMBER_PATTERN(?![ \t]*[/:\-]\d)[ \t\u00A0]*($CURRENCY_PATTERN)(?![\p{L}])""",
        RegexOption.IGNORE_CASE
    )

    private data class ExtractedBalanceMatch(
        val amount: Double,
        val range: IntRange
    )

    fun isAlgPosteSender(sender: String): Boolean {
        val norm = normalizeArabicDigits(sender)
            .lowercase(Locale.US)
            .replace(Regex("[\\s._-]+"), "")
        if (norm.isBlank()) return false
        return norm.contains("algpost") ||
            norm.contains("algeriepost") ||
            norm.contains("algériepost") ||
            norm.contains("algeriapost") ||
            norm.contains("baridi") ||
            norm.contains("edahabia") ||
            norm.contains("dahabia") ||
            norm.contains("postedz") ||
            norm.contains("postealgerie") ||
            norm == "poste" ||
            norm == "ccp" ||
            norm == "eccp" ||
            norm.contains("بريدالجزائر") ||
            norm.contains("بريدي") ||
            norm.contains("الذهبية")
    }

    fun isAlgPosteMessage(sender: String, body: String): Boolean {
        if (isAlgPosteSender(sender)) return true
        val normSender = normalizeArabicDigits(sender)
            .lowercase(Locale.US)
            .replace(Regex("[\\s._-]+"), "")
        val normBody = normalizeArabicDigits(body).lowercase(Locale.US)
        val compactBody = normBody.replace(Regex("[\\s._-]+"), "")
        if (compactBody.contains("algpost") ||
            compactBody.contains("algeriepost") ||
            compactBody.contains("algériepost") ||
            compactBody.contains("baridimob") ||
            compactBody.contains("barididz") ||
            compactBody.contains("edahabia") ||
            compactBody.contains("eccp") ||
            compactBody.contains("compteccp") ||
            compactBody.contains("soldeccp") ||
            compactBody.contains("بريدالجزائر") ||
            compactBody.contains("بريدي") ||
            compactBody.contains("البطاقةالذهبية") ||
            compactBody.contains("حسابكمالبريدي") ||
            compactBody.contains("حسابكالبريدي")
        ) {
            return true
        }
        // Carrier short numeric codes (e.g. 600 / 777 / 1530) with explicit Solde + Currency
        val isShortNumericSender = normSender.isNotBlank() &&
            normSender.all { it.isDigit() || it == '+' } &&
            normSender.length <= 6
        if (isShortNumericSender && extractBalanceFromBody(body) != null) {
            val hasCurrency = normBody.contains("da") || normBody.contains("dzd") ||
                normBody.contains("دج") || normBody.contains("د.ج") || normBody.contains("دينار")
            if (hasCurrency) return true
        }
        return false
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
        return extractBalanceMatchFromBody(normalizedBody)?.amount
    }

    private fun extractBalanceMatchFromBody(normalizedBody: String): ExtractedBalanceMatch? {
        val patterns = listOf(
            prioritizedBalanceCurrencyRegex,
            prioritizedBalanceNoCurrencyRegex,
            genericBalanceCurrencyRegex,
            genericBalanceNoCurrencyRegex,
            reversedBalanceRegex
        )
        for (regex in patterns) {
            val match = regex.findAll(normalizedBody)
                .mapNotNull { res ->
                    val parsed = parseFlexibleAmount(res.groupValues[1]) ?: return@mapNotNull null
                    if (parsed < 0.0) return@mapNotNull null
                    ExtractedBalanceMatch(roundTwoDecimals(parsed), res.range)
                }
                .lastOrNull()
            if (match != null) return match
        }
        return null
    }

    private fun extractPreviousBalanceMatchFromBody(normalizedBody: String): ExtractedBalanceMatch? {
        for (regex in listOf(previousBalanceCurrencyRegex, previousBalanceNoCurrencyRegex)) {
            val match = regex.findAll(normalizedBody)
                .mapNotNull { res ->
                    val parsed = parseFlexibleAmount(res.groupValues[1]) ?: return@mapNotNull null
                    if (parsed < 0.0) return@mapNotNull null
                    ExtractedBalanceMatch(roundTwoDecimals(parsed), res.range)
                }
                .firstOrNull()
            if (match != null) return match
        }
        return null
    }

    private fun extractUnsignedCandidateAmountOutsideBalances(normalizedBody: String): Double? {
        val currentBalRange = extractBalanceMatchFromBody(normalizedBody)?.range
        val prevBalRange = extractPreviousBalanceMatchFromBody(normalizedBody)?.range
        val lowerBody = normalizedBody.lowercase(Locale.US)

        for (match in currencyAmountRegex.findAll(normalizedBody)) {
            if (currentBalRange != null && rangesOverlap(match.range, currentBalRange)) continue
            if (prevBalRange != null && rangesOverlap(match.range, prevBalRange)) continue
            if (isInsideBalanceLabel(lowerBody, match.range.first)) continue
            val parsed = parseFlexibleAmount(match.groupValues[1]) ?: continue
            if (parsed > 0.0) return roundTwoDecimals(parsed)
        }
        return null
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

        val isKnownPoste = isAlgPosteMessage(trimmedSender, trimmedBody)
        val senderMatches = isKnownPoste ||
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
            sender = if (isAlgPosteSender(trimmedSender)) trimmedSender else if (isKnownPoste) trimmedSender else "ALG Poste",
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

        val isKnownPoste = isAlgPosteMessage(trimmedSender, trimmedBody)
        val senderMatches = isKnownPoste ||
            (allowAnySenderForManualTest && (trimmedSender.isNotBlank() || trimmedBody.isNotBlank()))
        if (!senderMatches) return null

        val normalizedBody = normalizeArabicDigits(trimmedBody)
        val lowerBody = normalizedBody.lowercase(Locale.US)

        // Ignore pure OTP / authentication codes that do not contain currency amounts
        val hasCurrencyToken = lowerBody.contains(" da") ||
            lowerBody.endsWith("da") ||
            lowerBody.contains("dzd") ||
            lowerBody.contains("دج") ||
            lowerBody.contains("د.ج") ||
            lowerBody.contains("د ج") ||
            lowerBody.contains("دينار")
        if (!hasCurrencyToken && otpNonTransactionMarkers.any { lowerBody.contains(it) }) {
            return null
        }

        val hasDebitKeyword = debitKeywords.any { lowerBody.contains(it) }
        val hasCreditKeyword = creditKeywords.any { lowerBody.contains(it) }
        val balanceMatch = extractBalanceMatchFromBody(normalizedBody)
        val prevBalanceMatch = extractPreviousBalanceMatchFromBody(normalizedBody)
        val extractedBalance = balanceMatch?.amount
        val isExplicitFee = explicitFeeKeywords.any { lowerBody.contains(it) }

        // 1. Prioritize explicitly signed amounts (e.g. -300.00 DA or +5000.00 DA), ignoring Solde/Balance spans
        var pickedAmount: Double? = null
        var pickedIsExpense: Boolean? = null
        var matchedReason = ""

        val signedMatches = signedAmountRegex.findAll(normalizedBody).toList()
        for (match in signedMatches) {
            if (balanceMatch != null && rangesOverlap(match.range, balanceMatch.range)) continue
            if (prevBalanceMatch != null && rangesOverlap(match.range, prevBalanceMatch.range)) continue
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
                if (balanceMatch != null && rangesOverlap(match.range, balanceMatch.range)) continue
                if (prevBalanceMatch != null && rangesOverlap(match.range, prevBalanceMatch.range)) continue
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
            sender = if (isAlgPosteSender(trimmedSender)) trimmedSender else if (isKnownPoste) trimmedSender else "ALG Poste",
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
     * Analyzes chronological ALG Poste / BaridiMob balance snapshots to detect:
     * 1. Message-to-message comparison (Current SMS vs Previous SMS):
     *    - Previous New Balance (`prev.balance`, or explicit old balance in same SMS if first)
     *    - Withdrawn / Credited Amount (`curr.transactionAmount`, or inferred from unsigned amount + balance delta)
     *    - Expected New Balance = prevBalance - withdrawn (or + credited)
     *    - Actual Current New Balance = `curr.balance`
     *    - Caught Tax / Hidden Fee Discrepancy = expectedBalance - curr.balance
     * 2. Explicit fee/tax SMS debits when directly billed as a standalone fee message.
     */
    fun detectTaxAnomalies(
        snapshots: List<SmsBalanceSnapshot>,
        existingAnomalies: List<SmsTaxAnomalyEntry> = emptyList()
    ): List<SmsTaxAnomalyEntry> {
        if (snapshots.isEmpty()) return existingAnomalies

        val ordered = snapshots
            .distinctBy { it.id }
            .sortedWith(compareBy<SmsBalanceSnapshot> { it.timestampMillis }.thenBy { it.id })
        val evaluatedSmsIds = ordered.map { it.id }.toSet()
        val existingById = existingAnomalies.associateBy { it.id }
        val resultMap = LinkedHashMap<String, SmsTaxAnomalyEntry>()
        // Keep historical anomalies from pruned/external SMS or already recorded expenses,
        // ignoring unrecorded entries that exceed MAX_CAUGHT_TAX_AMOUNT_DZD (1,000 DZD)
        existingAnomalies
            .filter {
                (it.smsId !in evaluatedSmsIds && it.taxAmount <= MAX_CAUGHT_TAX_AMOUNT_DZD) ||
                    it.isRecordedAsExpense
            }
            .forEach { resultMap[it.id] = it }

        for (i in ordered.indices) {
            val curr = ordered[i]
            val prev = if (i > 0) ordered[i - 1] else null

            val inlinePrevBalance = if (prev == null && curr.rawBody.isNotBlank()) {
                extractPreviousBalanceMatchFromBody(normalizeArabicDigits(curr.rawBody))?.amount
            } else null

            val referencePrevBalance = prev?.balance ?: inlinePrevBalance

            var effectiveHasTx = curr.hasTransaction && curr.transactionAmount > 0.0
            var effectiveTxAmount = curr.transactionAmount
            var effectiveIsExpense = curr.isExpense

            // Recover unsigned transaction amount & infer direction by comparing against previous SMS balance
            if (!effectiveHasTx && referencePrevBalance != null && curr.rawBody.isNotBlank()) {
                val candidateAmt = extractUnsignedCandidateAmountOutsideBalances(normalizeArabicDigits(curr.rawBody))
                if (candidateAmt != null && candidateAmt > 0.0) {
                    val debitExpected = roundTwoDecimals(referencePrevBalance - candidateAmt)
                    val creditExpected = roundTwoDecimals(referencePrevBalance + candidateAmt)
                    val debitDiscrepancy = roundTwoDecimals(debitExpected - curr.balance)
                    val creditDiscrepancy = roundTwoDecimals(creditExpected - curr.balance)

                    val debitFits = curr.balance <= referencePrevBalance + 0.01 &&
                        debitDiscrepancy >= -0.01 &&
                        debitDiscrepancy <= maxOf(1000.0, candidateAmt * 0.35)
                    val creditFits = curr.balance >= referencePrevBalance - 0.01 &&
                        creditDiscrepancy >= -0.01 &&
                        creditDiscrepancy <= maxOf(1000.0, candidateAmt * 0.35)

                    if (debitFits || creditFits) {
                        effectiveHasTx = true
                        effectiveTxAmount = candidateAmt
                        effectiveIsExpense = if (debitFits && !creditFits) true
                        else if (creditFits && !debitFits) false
                        else curr.balance <= referencePrevBalance
                    }
                }
            }

            if (referencePrevBalance != null) {
                val expectedBalance = roundTwoDecimals(
                    if (effectiveHasTx) {
                        if (effectiveIsExpense) referencePrevBalance - effectiveTxAmount
                        else referencePrevBalance + effectiveTxAmount
                    } else {
                        referencePrevBalance
                    }
                )
                val discrepancy = roundTwoDecimals(expectedBalance - curr.balance)

                if (discrepancy >= 0.01 && discrepancy <= MAX_CAUGHT_TAX_AMOUNT_DZD) {
                    val anomalyId = "tax_diff_${curr.id}"
                    val alreadyRecorded = existingById[anomalyId]?.isRecordedAsExpense == true
                    val isLikelyFee = curr.hasExplicitFeeKeyword ||
                        !effectiveHasTx ||
                        discrepancy <= maxOf(500.0, effectiveTxAmount * 0.25)

                    val signStr = if (effectiveHasTx) {
                        if (effectiveIsExpense) " − المسحوب ${formatCompactAmount(effectiveTxAmount)}"
                        else " + المضاف ${formatCompactAmount(effectiveTxAmount)}"
                    } else ""
                    val signStrEn = if (effectiveHasTx) {
                        if (effectiveIsExpense) " − Withdrawn ${formatCompactAmount(effectiveTxAmount)}"
                        else " + Credited ${formatCompactAmount(effectiveTxAmount)}"
                    } else ""

                    val titleAr = when {
                        !effectiveHasTx -> "فارق رصيد / اقتطاع ضرائب مقارنة بالرسالة السابقة"
                        isLikelyFee -> "رصد ضريبة / عمولة مقتطعة بين الرسالتين"
                        else -> "فارق رصيد / خصم إضافي مكتشف"
                    }
                    val titleEn = when {
                        !effectiveHasTx -> "Tax / Deduction vs Previous SMS Balance"
                        isLikelyFee -> "Hidden Bank Fee / Tax Caught Between SMS"
                        else -> "Balance Discrepancy / Deduction Caught"
                    }
                    val explanationAr = if (effectiveHasTx) {
                        "الرصيد السابق ${formatCompactAmount(referencePrevBalance)}$signStr = المتوقع ${formatCompactAmount(expectedBalance)} • الجديد الحالي ${formatCompactAmount(curr.balance)} ⟵ الفرق (ضريبة): ${formatCompactAmount(discrepancy)} دج"
                    } else {
                        "الرصيد الجديد السابق ${formatCompactAmount(referencePrevBalance)} • الرصيد الجديد الحالي ${formatCompactAmount(curr.balance)} ⟵ الفرق المقتطع: ${formatCompactAmount(discrepancy)} دج"
                    }
                    val explanationEn = if (effectiveHasTx) {
                        "Prev New ${formatCompactAmount(referencePrevBalance)}$signStrEn = Expected ${formatCompactAmount(expectedBalance)} • Current New ${formatCompactAmount(curr.balance)} ⟵ Difference (Tax): ${formatCompactAmount(discrepancy)} DA"
                    } else {
                        "Prev New ${formatCompactAmount(referencePrevBalance)} • Current New ${formatCompactAmount(curr.balance)} ⟵ Caught Difference: ${formatCompactAmount(discrepancy)} DA"
                    }

                    resultMap[anomalyId] = SmsTaxAnomalyEntry(
                        id = anomalyId,
                        smsId = curr.id,
                        previousBalance = referencePrevBalance,
                        transactionAmount = if (effectiveHasTx) effectiveTxAmount else 0.0,
                        isExpense = effectiveIsExpense,
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
                }
            }

            // Explicit standalone fee/tax message (e.g. Frais de tenue de compte -250 DA)
            if (curr.hasExplicitFeeKeyword &&
                effectiveHasTx &&
                effectiveIsExpense &&
                effectiveTxAmount > 0.0 &&
                effectiveTxAmount <= MAX_CAUGHT_TAX_AMOUNT_DZD
            ) {
                val anomalyId = "tax_fee_${curr.id}"
                val alreadyRecorded = existingById[anomalyId]?.isRecordedAsExpense == true
                resultMap[anomalyId] = SmsTaxAnomalyEntry(
                    id = anomalyId,
                    smsId = curr.id,
                    previousBalance = referencePrevBalance ?: roundTwoDecimals(curr.balance + effectiveTxAmount),
                    transactionAmount = effectiveTxAmount,
                    isExpense = true,
                    expectedBalance = curr.balance,
                    actualBalance = curr.balance,
                    taxAmount = effectiveTxAmount,
                    detectionKind = "EXPLICIT_TAX_FEE",
                    detectedAtMillis = curr.timestampMillis,
                    titleAr = "رسوم / ضريبة صريحة من بريد الجزائر",
                    titleEn = "Explicit ALG Poste Fee / Tax",
                    explanationAr = "اقتطاع مباشر مصنف كرسوم أو ضريبة بقيمة ${formatCompactAmount(effectiveTxAmount)} دج (الرصيد الحالي: ${formatCompactAmount(curr.balance)} دج)",
                    explanationEn = "Direct fee/tax debit detected for ${formatCompactAmount(effectiveTxAmount)} DA (Current balance: ${formatCompactAmount(curr.balance)} DA)",
                    rawBody = curr.rawBody,
                    isRecordedAsExpense = alreadyRecorded
                )
            }
        }

        return resultMap.values
            .sortedByDescending { it.detectedAtMillis }
    }

    fun toArchivedEntity(
        sender: String,
        body: String,
        timestampMillis: Long = System.currentTimeMillis(),
        captureSource: String = "INBOX",
        allowAnySenderForManualTest: Boolean = false
    ): ArchivedAlgPosteSmsEntity? {
        val trimmedSender = sender.trim().ifBlank { "ALG Poste" }
        val trimmedBody = body.trim()
        if (trimmedBody.isBlank()) return null

        val isKnownPoste = isAlgPosteMessage(trimmedSender, trimmedBody)
        if (!isKnownPoste && !allowAnySenderForManualTest) return null

        val normalizedBody = normalizeArabicDigits(trimmedBody)
        val lowerBody = normalizedBody.lowercase(Locale.US)
        val parsedTx = parseSms(
            sender = trimmedSender,
            body = trimmedBody,
            timestampMillis = timestampMillis,
            allowAnySenderForManualTest = allowAnySenderForManualTest
        )
        val extractedBalance = parsedTx?.extractedBalance ?: extractBalanceFromBody(normalizedBody)
        val isFeeKeyword = parsedTx?.hasExplicitFeeKeyword ?: hasExplicitFeeKeyword(trimmedBody)
        val isOtpMessage = parsedTx == null &&
            extractedBalance == null &&
            otpNonTransactionMarkers.any { lowerBody.contains(it) }

        val matchedRule = when {
            parsedTx != null && parsedTx.matchedReason.isNotBlank() -> parsedTx.matchedReason
            extractedBalance != null -> "BALANCE_ONLY"
            isOtpMessage -> "OTP_AUTH"
            else -> "UNRECOGNIZED"
        }
        val studyStatus = when {
            parsedTx != null && extractedBalance != null -> "FULL_MATCH"
            parsedTx == null && extractedBalance != null -> "BALANCE_ONLY"
            parsedTx != null -> "AMOUNT_ONLY"
            isOtpMessage -> "OTP_IGNORED"
            else -> "NEEDS_STUDY"
        }

        return ArchivedAlgPosteSmsEntity(
            id = buildSuggestionId(trimmedSender, trimmedBody, timestampMillis),
            sender = if (isAlgPosteSender(trimmedSender) || isKnownPoste) trimmedSender else "ALG Poste",
            rawBody = trimmedBody,
            receivedAtMillis = timestampMillis,
            capturedAtMillis = System.currentTimeMillis(),
            captureSource = captureSource,
            extractedBalance = extractedBalance,
            extractedTxAmount = parsedTx?.amount,
            extractedIsExpense = parsedTx?.isExpense,
            hasExplicitFeeKeyword = isFeeKeyword,
            matchedRule = matchedRule,
            studyStatus = studyStatus
        )
    }

    fun reanalyzeArchivedEntity(existing: ArchivedAlgPosteSmsEntity): ArchivedAlgPosteSmsEntity {
        val refreshed = toArchivedEntity(
            sender = existing.sender,
            body = existing.rawBody,
            timestampMillis = existing.receivedAtMillis,
            captureSource = existing.captureSource,
            allowAnySenderForManualTest = true
        ) ?: return existing
        return existing.copy(
            extractedBalance = refreshed.extractedBalance,
            extractedTxAmount = refreshed.extractedTxAmount,
            extractedIsExpense = refreshed.extractedIsExpense,
            hasExplicitFeeKeyword = refreshed.hasExplicitFeeKeyword,
            matchedRule = refreshed.matchedRule,
            studyStatus = refreshed.studyStatus
        )
    }

    fun reconstructSnapshotsFromArchive(
        archived: List<ArchivedAlgPosteSmsEntity>
    ): List<SmsBalanceSnapshot> {
        if (archived.isEmpty()) return emptyList()
        return archived
            .sortedWith(compareBy<ArchivedAlgPosteSmsEntity> { it.receivedAtMillis }.thenBy { it.id })
            .mapNotNull { entry ->
                parseBalanceSnapshot(
                    sender = entry.sender,
                    body = entry.rawBody,
                    timestampMillis = entry.receivedAtMillis,
                    allowAnySenderForManualTest = true
                )?.copy(id = entry.id)
            }
            .distinctBy { it.id }
            .sortedBy { it.timestampMillis }
    }

    fun buildStudyReport(
        archived: List<ArchivedAlgPosteSmsEntity>,
        caughtTaxes: List<SmsTaxAnomalyEntry>
    ): ArchivedSmsStudyReport {
        val total = archived.size
        val fullCount = archived.count { it.studyStatus == "FULL_MATCH" }
        val balCount = archived.count { it.extractedBalance != null }
        val amtOnlyCount = archived.count { it.studyStatus == "AMOUNT_ONLY" }
        val otpCount = archived.count { it.studyStatus == "OTP_IGNORED" }
        val needsStudyCount = archived.count { it.studyStatus == "NEEDS_STUDY" }
        val nonOtpTotal = (total - otpCount).coerceAtLeast(0)
        val recognizedNonOtp = archived.count {
            it.studyStatus == "FULL_MATCH" ||
                it.studyStatus == "BALANCE_ONLY" ||
                it.studyStatus == "AMOUNT_ONLY"
        }
        val accuracy = if (nonOtpTotal == 0) 100
        else ((recognizedNonOtp * 100.0) / nonOtpTotal).roundToInt().coerceIn(0, 100)

        val validTaxes = caughtTaxes.filter { it.taxAmount > 0.0 && it.taxAmount <= MAX_CAUGHT_TAX_AMOUNT_DZD }
        val feeStats = validTaxes
            .groupBy { roundTwoDecimals(it.taxAmount) }
            .map { (amt, list) -> FeePatternStat(amountDzd = amt, occurrences = list.size) }
            .sortedWith(compareByDescending<FeePatternStat> { it.occurrences }.thenByDescending { it.amountDzd })
            .take(5)

        return ArchivedSmsStudyReport(
            totalArchived = total,
            fullMatchCount = fullCount,
            balanceExtractedCount = balCount,
            amountOnlyCount = amtOnlyCount,
            needsStudyCount = needsStudyCount,
            otpIgnoredCount = otpCount,
            recognitionAccuracyPercent = accuracy,
            learnedFeePatterns = feeStats,
            totalValidCaughtTaxesDzd = roundTwoDecimals(validTaxes.sumOf { it.taxAmount })
        )
    }

    private fun rangesOverlap(a: IntRange, b: IntRange): Boolean =
        a.first <= b.last && b.first <= a.last

    private fun isInsideBalanceLabel(lowerText: String, matchStart: Int): Boolean {
        val windowStart = (matchStart - 45).coerceAtLeast(0)
        var prefix = lowerText.substring(windowStart, matchStart)
        val cutIdx = listOf(". ", ";", "\n", "\r", " da", " dzd", "دج", "د.ج", "دينار")
            .mapNotNull { sep -> prefix.lastIndexOf(sep).takeIf { it >= 0 }?.plus(sep.length) }
            .maxOrNull()
        if (cutIdx != null && cutIdx <= prefix.length) {
            prefix = prefix.substring(cutIdx)
        }
        val lastMarkerEnd = balanceContextMarkers
            .mapNotNull { marker ->
                prefix.lastIndexOf(marker).takeIf { it >= 0 }?.plus(marker.length)
            }
            .maxOrNull() ?: return false
        val tailAfterMarker = prefix.substring(lastMarkerEnd)
        if (debitKeywords.any { tailAfterMarker.contains(it) } ||
            creditKeywords.any { tailAfterMarker.contains(it) }
        ) {
            return false
        }
        return true
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
            compact.count { it == '.' } > 1 -> compact.replace(".", "")
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
                ?.filter {
                    it.id.isNotBlank() &&
                        it.taxAmount > 0.0 &&
                        (it.isRecordedAsExpense || it.taxAmount <= AlgPosteSmsAnalyzer.MAX_CAUGHT_TAX_AMOUNT_DZD)
                }
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
        maxRowsToInspect: Int = 500,
        minTimestampMillis: Long = 0L
    ): List<SmsSuggestionEntry> =
        scanInboxWithBalances(context, maxRowsToInspect, minTimestampMillis).suggestions

    private fun readIncomingSmsMessages(
        context: Context,
        maxRowsToInspect: Int
    ): List<RawAlgPosteSms> {
        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )
        val result = mutableListOf<RawAlgPosteSms>()
        val dedup = HashSet<String>()

        fun readUri(uri: android.net.Uri, selection: String?, selectionArgs: Array<String>?) {
            if (result.size >= maxRowsToInspect) return
            try {
                context.contentResolver.query(
                    uri,
                    projection,
                    selection,
                    selectionArgs,
                    "${Telephony.Sms.DATE} DESC"
                )?.use { cursor ->
                    val addrIdx = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
                    val bodyIdx = cursor.getColumnIndex(Telephony.Sms.BODY)
                    val dateIdx = cursor.getColumnIndex(Telephony.Sms.DATE)

                    var inspected = 0
                    while (cursor.moveToNext() && inspected < maxRowsToInspect && result.size < maxRowsToInspect) {
                        inspected++
                        val sender = if (addrIdx != -1) cursor.getString(addrIdx).orEmpty() else ""
                        val body = if (bodyIdx != -1) cursor.getString(bodyIdx).orEmpty() else ""
                        val dateMillis = if (dateIdx != -1) cursor.getLong(dateIdx) else System.currentTimeMillis()
                        if (body.isBlank()) continue
                        val key = "${sender.trim().lowercase(Locale.US)}|${dateMillis / 1000L}|${body.trim()}"
                        if (dedup.add(key)) {
                            result.add(RawAlgPosteSms(sender, body, dateMillis))
                        }
                    }
                }
            } catch (_: Throwable) {
            }
        }

        readUri(Telephony.Sms.Inbox.CONTENT_URI, null, null)
        if (result.isEmpty()) {
            // Fallback for OEM ROMs where Inbox URI returns empty: query main SMS provider excluding Sent/Draft/Outbox
            readUri(
                Telephony.Sms.CONTENT_URI,
                "${Telephony.Sms.TYPE} NOT IN (2, 3, 4, 5, 6)",
                null
            )
        }
        return result
    }

    fun scanAllRawAlgPosteSms(
        context: Context,
        maxRowsToInspect: Int = 500
    ): List<RawAlgPosteSms> {
        if (!hasSmsPermissions(context)) return emptyList()
        return readIncomingSmsMessages(context, maxRowsToInspect)
            .filter { AlgPosteSmsAnalyzer.isAlgPosteMessage(it.sender, it.body) }
    }

    fun scanInboxWithBalances(
        context: Context,
        maxRowsToInspect: Int = 500,
        minTimestampMillis: Long = 0L,
        applyMinTimestampToSnapshots: Boolean = false
    ): InboxScanResult {
        if (!hasSmsPermissions(context)) {
            return InboxScanResult(emptyList(), emptyList(), emptyList())
        }

        val suggestions = mutableListOf<SmsSuggestionEntry>()
        val snapshots = mutableListOf<SmsBalanceSnapshot>()
        val matchedRawMessages = mutableListOf<RawAlgPosteSms>()
        val rawMessages = readIncomingSmsMessages(context, maxRowsToInspect)

        for (sms in rawMessages) {
            if (!AlgPosteSmsAnalyzer.isAlgPosteMessage(sms.sender, sms.body)) continue
            matchedRawMessages.add(sms)

            val includeSuggestion = minTimestampMillis <= 0L || sms.dateMillis > minTimestampMillis
            if (includeSuggestion) {
                AlgPosteSmsAnalyzer.parseSms(
                    sender = sms.sender,
                    body = sms.body,
                    timestampMillis = sms.dateMillis,
                    allowAnySenderForManualTest = false
                )?.let { suggestions.add(it) }
            }

            val includeSnapshot = !applyMinTimestampToSnapshots ||
                minTimestampMillis <= 0L ||
                sms.dateMillis > minTimestampMillis
            if (includeSnapshot) {
                AlgPosteSmsAnalyzer.parseBalanceSnapshot(
                    sender = sms.sender,
                    body = sms.body,
                    timestampMillis = sms.dateMillis,
                    allowAnySenderForManualTest = false
                )?.let { snapshots.add(it) }
            }
        }

        return InboxScanResult(
            suggestions = suggestions.distinctBy { it.id }.sortedByDescending { it.receivedAtMillis },
            snapshots = snapshots.distinctBy { it.id }.sortedBy { it.timestampMillis },
            rawMessages = matchedRawMessages.sortedBy { it.dateMillis }
        )
    }

    fun scanAllBalanceSnapshots(
        context: Context,
        maxRowsToInspect: Int = 500
    ): List<SmsBalanceSnapshot> {
        if (!hasSmsPermissions(context)) return emptyList()
        val rawMessages = readIncomingSmsMessages(context, maxRowsToInspect)
        val snapshots = mutableListOf<SmsBalanceSnapshot>()
        for (sms in rawMessages) {
            if (!AlgPosteSmsAnalyzer.isAlgPosteMessage(sms.sender, sms.body)) continue
            AlgPosteSmsAnalyzer.parseBalanceSnapshot(
                sender = sms.sender,
                body = sms.body,
                timestampMillis = sms.dateMillis,
                allowAnySenderForManualTest = false
            )?.let { snapshots.add(it) }
        }
        return snapshots.distinctBy { it.id }.sortedBy { it.timestampMillis }
    }

    fun scanLatestBalanceSnapshot(context: Context, maxRowsToInspect: Int = 500): SmsBalanceSnapshot? {
        return scanAllBalanceSnapshots(context, maxRowsToInspect).maxByOrNull { it.timestampMillis }
    }
}
