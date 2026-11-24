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
    val previousBalance: Double? = null,
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

@JsonClass(generateAdapter = true)
data class SmsFailedTransactionEntry(
    val id: String = "",
    val failedDebitSmsId: String = "",
    val recoverySmsId: String = "",
    val debitAmount: Double = 0.0,
    val recoveredAmount: Double = 0.0,
    val unrefundedFeeAmount: Double = 0.0,
    val balanceBeforeDebit: Double = 0.0,
    val balanceAfterDebit: Double = 0.0,
    val expectedBalanceBeforeRecovery: Double = 0.0,
    val actualBalanceAtRecovery: Double = 0.0,
    val debitTimestampMillis: Long = 0L,
    val recoveredTimestampMillis: Long = System.currentTimeMillis(),
    val detectionKind: String = "SILENT_BALANCE_JUMP", // SILENT_BALANCE_JUMP, EXPLICIT_REFUND_REVERSAL, FAILED_WITH_FEE_KEPT, MULTI_DEBIT_RECOVERY, EXPLICIT_FAILED_NO_DEDUCTION, EXPLICIT_REVERSAL_UNLINKED
    val confidencePercent: Int = 99,
    val titleAr: String = "",
    val titleEn: String = "",
    val explanationAr: String = "",
    val explanationEn: String = "",
    val debitRawBody: String = "",
    val recoveryRawBody: String = "",
    val isResolved: Boolean = false
)

data class FailedTransactionAnalysisResult(
    val failedTransactions: List<SmsFailedTransactionEntry> = emptyList(),
    val excludedSuggestionSmsIds: Set<String> = emptySet(),
    val voidedTaxDebitSmsIds: Set<String> = emptySet(),
    val reversalRecoverySmsIds: Set<String> = emptySet(),
    val adjustedPrevBalanceByRecoverySmsId: Map<String, Double> = emptyMap()
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

data class AnnualSmsTaxesSummary(
    val year: Int,
    val referenceStartDateLabel: String,
    val yearStartMillis: Long,
    val totalYearTaxesDzd: Double,
    val yearTaxCount: Int,
    val recordedYearTaxesDzd: Double,
    val unrecordedYearTaxesDzd: Double
)

object AlgPosteSmsAnalyzer {

    /**
     * Caught Tax ignores any balance discrepancy or explicit fee above 1,000 DZD,
     * since larger jumps indicate unlogged transfers/withdrawals or missing SMS rather than taxes.
     */
    const val MAX_CAUGHT_TAX_AMOUNT_DZD = 1000.0

    fun computeAnnualTaxesSummary(
        taxes: List<SmsTaxAnomalyEntry>,
        referenceNowMillis: Long = System.currentTimeMillis(),
        timeZone: java.util.TimeZone = java.util.TimeZone.getDefault()
    ): AnnualSmsTaxesSummary {
        val cal = java.util.Calendar.getInstance(timeZone).apply {
            timeInMillis = referenceNowMillis
        }
        val currentYear = cal.get(java.util.Calendar.YEAR)
        val startCal = java.util.Calendar.getInstance(timeZone).apply {
            clear()
            set(currentYear, java.util.Calendar.JANUARY, 1, 0, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val endCal = java.util.Calendar.getInstance(timeZone).apply {
            clear()
            set(currentYear + 1, java.util.Calendar.JANUARY, 1, 0, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val yearStartMillis = startCal.timeInMillis
        val nextYearStartMillis = endCal.timeInMillis

        val yearEntries = taxes.filter { entry ->
            entry.taxAmount > 0.0 &&
                entry.detectedAtMillis >= yearStartMillis &&
                entry.detectedAtMillis < nextYearStartMillis
        }
        val total = yearEntries.sumOf { it.taxAmount }
        val recorded = yearEntries.filter { it.isRecordedAsExpense }.sumOf { it.taxAmount }
        val unrecorded = (total - recorded).coerceAtLeast(0.0)

        return AnnualSmsTaxesSummary(
            year = currentYear,
            referenceStartDateLabel = "1.1.$currentYear",
            yearStartMillis = yearStartMillis,
            totalYearTaxesDzd = (total * 100.0).roundToLong() / 100.0,
            yearTaxCount = yearEntries.size,
            recordedYearTaxesDzd = (recorded * 100.0).roundToLong() / 100.0,
            unrecordedYearTaxesDzd = (unrecorded * 100.0).roundToLong() / 100.0
        )
    }

    private val debitKeywords = listOf(
        "retrait", "retire", "retiré", "retirer", "gab", "dab",
        "débit", "debit", "débité", "debite",
        "prélevé", "preleve", "prélèvement", "prelevement",
        "paiement", "payement", "e-paiement", "epaiement", "tpe", "achat",
        "virement emis", "virement émis", "virement effectue", "virement effectué",
        "transfert effectue", "transfert effectué", "transfert envoye", "transfert envoyé",
        "transfert sortant", "émis", "emis", "envoyé", "envoye",
        "frais", "commission", "facture", "cotisation", "taxe", "taxes", "timbre", "agios", "redevance",
        "recharge", "flexy", "baridipay", "mobipost",
        "withdrawn", "withdrawal", "debited", "payment", "purchase", "deducted", "sent to",
        "سحب", "مسحوب", "سحبت", "تم سحب", "خصم", "تم خصم", "حسم", "اقتطاع", "تم اقتطاع",
        "دفع", "تم دفع", "تسديد", "سداد", "شراء", "صرف", "تحويل صادر", "تحويل إلى", "تحويل الى",
        "إرسال", "ارسال", "فاتورة", "ضريبة", "ضرائب", "رسوم", "رسم", "عمولة", "شحن", "تعبئة", "فليكسي"
    )

    private val outgoingTransferPhrases = listOf(
        "vers le compte", "vers rip", "vers ccp", "vers compte", "vers carte",
        "من حسابكم إلى", "من حسابك إلى", "من حسابكم الى", "من حسابك الى"
    )

    private val versWordRegex = Regex("""\bvers\b""", RegexOption.IGNORE_CASE)
    private val arabicOutgoingTransferRegex = Regex(
        """(?:تم\s+)?تحويل[^\r\n]{0,60}(?:إلى|الى|لحساب)(?!\s*(?:حسابك|حسابكم))"""
    )
    private val frenchOutgoingTransferRegex = Regex(
        """\b(?:virement|transfert|transfer)\b[^\r\n]{0,60}\b(?:vers|to)\b(?!\s*votre)"""
    )

    private val creditKeywords = listOf(
        "crédit", "credit", "crédité", "credite", "credited",
        "versement", "alimentation", "alimenté", "alimente",
        "virement reçu", "virement recu", "transfert reçu", "transfert recu",
        "reçu", "recu", "received", "incoming", "en votre faveur", "vers votre compte",
        "salaire", "paie", "salary", "pension", "allocation", "bourse",
        "dépôt", "depot", "deposit", "deposited",
        "remboursement", "refund", "virement entrant", "transfert entrant",
        "bonification", "encaissement", "top up", "top-up",
        "إيداع", "ايداع", "تم إيداع", "تم ايداع", "تحويل وارد", "وارد",
        "استلام", "تم استلام", "راتب", "أجرة", "منحة", "معاش", "تقاعد", "مستحقات",
        "إضافة", "تمت إضافة", "تمت اضافة", "رصيد مضاف", "مبلغ مضاف",
        "دخول رصيد", "دخول", "إلى حسابكم", "الى حسابكم", "إلى حسابك", "الى حسابك",
        "في حسابكم", "في حسابك",
        "استرجاع", "تعويض", "تغذية الحساب", "صب"
    )

    private val explicitFeeKeywords = listOf(
        "frais", "taxe", "taxes", "commission", "timbre", "tva", "agios",
        "cotisation", "redevance", "prelevement fiscal", "prélèvement fiscal",
        "ضريبة", "الضريبة", "ضرائب", "رسم", "رسوم", "عمولة", "اقتطاع", "طابع", "مصاريف بنكية"
    )

    private val explicitFailureKeywords = listOf(
        "echoue", "échoué", "echouee", "échouée", "echec", "échec",
        "non abouti", "non aboutie", "refuse", "refusé", "refusee", "refusée",
        "rejet", "rejete", "rejeté", "rejetee", "rejetée",
        "failed", "declined", "unsuccessful", "rejected",
        "فشل", "فشلت", "فاشل", "فاشلة", "مرفوض", "مرفوضة", "رفض",
        "لم تتم", "غير مكتمل", "غير مكتملة", "تعذر"
    )

    private val strongReversalKeywords = listOf(
        "annulation", "annule", "annulé", "annulee", "annulée",
        "extourne", "extourné", "restitution", "restitue", "restitué",
        "rejet", "rejete", "rejeté", "rejetee", "rejetée",
        "reversed", "reversal", "cancelled", "canceled", "chargeback",
        "إلغاء", "الغاء", "ملغاة", "ملغى", "ملغي", "رجوع", "إرجاع", "ارجاع"
    )

    private val softRefundKeywords = listOf(
        "remboursement", "rembourse", "remboursé", "remboursee", "remboursée",
        "refund", "refunded", "استرجاع", "تم استرجاع", "تعويض"
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
    private const val SIGN_PREFIX = """(?:(-)[ \t\u00A0]*)?"""
    private const val TRAILING_SIGN_SUFFIX = """(?<![ \t\u00A0])(-)?(?![0-9])"""
    private const val BALANCE_NUM_BOUNDARY = """(?<![0-9])(?<![0-9][:/\-])"""
    private const val PRIORITIZED_BALANCE_LABELS =
        """(?:nouveau\s+solde|solde\s+(?:disponible|actuel|comptable|courant|final|net|restant|apr[eè]s\s+op[eé]ration)|n\.?\s*solde|الرصيد\s+(?:الجديد|الحالي|المتاح|المتوفر|المتبقي|البريدي|النهائي)|رصيدك\s+(?:الحالي|الجديد|المتاح)|رصيد\s+(?:الحساب|حسابك|حسابكم|البطاقة))"""
    private const val GENERIC_BALANCE_LABELS =
        """(?:(?<!ancien\s)(?<!ancien\s{2})solde(?!\s+(?:pr[eé]c[eé]dent|precedent|initial|avant|ant[eé]rieur|anterieur))(?:\s+ccp)?|balance|avoir|الرصيد(?!\s+(?:السابق|القديم))|رصيدكم|رصيدك|رصيد)"""
    private const val PREVIOUS_BALANCE_LABELS =
        """(?:ancien\s+solde|solde\s+(?:pr[eé]c[eé]dent|precedent|initial|avant|ant[eé]rieur|anterieur)|الرصيد\s+(?:السابق|القديم)|رصيدك\s+السابق)"""
    private const val IMMEDIATE_CONNECTORS =
        """(?:\s*(?:est\s+de|est|de|du|au|le|à|a|هو|الحالي|الجديد|السابق|المتاح|بـ|ب|:|=|،)\s*){0,4}"""

    private val prioritizedBalanceCurrencyRegex = Regex(
        """$PRIORITIZED_BALANCE_LABELS[^\r\n]{0,60}?$BALANCE_NUM_BOUNDARY$SIGN_PREFIX$NUMBER_PATTERN$TRAILING_SIGN_SUFFIX(?![ \t]*[/:\-]\d)\s*$CURRENCY_PATTERN(?![\p{L}])""",
        RegexOption.IGNORE_CASE
    )

    private val prioritizedBalanceNoCurrencyRegex = Regex(
        """$PRIORITIZED_BALANCE_LABELS$IMMEDIATE_CONNECTORS$BALANCE_NUM_BOUNDARY$SIGN_PREFIX$NUMBER_PATTERN$TRAILING_SIGN_SUFFIX(?![ \t]*[/:\-]\d)(?:\s*$CURRENCY_PATTERN)?""",
        RegexOption.IGNORE_CASE
    )

    private val genericBalanceCurrencyRegex = Regex(
        """$GENERIC_BALANCE_LABELS[^\r\n]{0,60}?$BALANCE_NUM_BOUNDARY$SIGN_PREFIX$NUMBER_PATTERN$TRAILING_SIGN_SUFFIX(?![ \t]*[/:\-]\d)\s*$CURRENCY_PATTERN(?![\p{L}])""",
        RegexOption.IGNORE_CASE
    )

    private val genericBalanceNoCurrencyRegex = Regex(
        """$GENERIC_BALANCE_LABELS$IMMEDIATE_CONNECTORS$BALANCE_NUM_BOUNDARY$SIGN_PREFIX$NUMBER_PATTERN$TRAILING_SIGN_SUFFIX(?![ \t]*[/:\-]\d)(?:\s*$CURRENCY_PATTERN)?""",
        RegexOption.IGNORE_CASE
    )

    private val reversedBalanceRegex = Regex(
        """$BALANCE_NUM_BOUNDARY$SIGN_PREFIX$NUMBER_PATTERN$TRAILING_SIGN_SUFFIX\s*$CURRENCY_PATTERN\s*(?:est\s+(?:votre\s+)?(?:nouveau\s+)?solde|هو\s*رصيدك)""",
        RegexOption.IGNORE_CASE
    )

    private val previousBalanceCurrencyRegex = Regex(
        """$PREVIOUS_BALANCE_LABELS[^\r\n]{0,50}?$BALANCE_NUM_BOUNDARY$SIGN_PREFIX$NUMBER_PATTERN$TRAILING_SIGN_SUFFIX(?![ \t]*[/:\-]\d)\s*$CURRENCY_PATTERN(?![\p{L}])""",
        RegexOption.IGNORE_CASE
    )

    private val previousBalanceNoCurrencyRegex = Regex(
        """$PREVIOUS_BALANCE_LABELS$IMMEDIATE_CONNECTORS$BALANCE_NUM_BOUNDARY$SIGN_PREFIX$NUMBER_PATTERN$TRAILING_SIGN_SUFFIX(?![ \t]*[/:\-]\d)(?:\s*$CURRENCY_PATTERN)?""",
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
                    val isNegative = res.groupValues[1] == "-" || res.groupValues.getOrNull(3) == "-"
                    val mag = parseFlexibleAmount(res.groupValues[2]) ?: return@mapNotNull null
                    val signed = if (isNegative) -mag else mag
                    val rounded = roundTwoDecimals(signed).let {
                        if (kotlin.math.abs(it) < 0.005) 0.0 else it
                    }
                    ExtractedBalanceMatch(rounded, res.range)
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
                    val isNegative = res.groupValues[1] == "-" || res.groupValues.getOrNull(3) == "-"
                    val mag = parseFlexibleAmount(res.groupValues[2]) ?: return@mapNotNull null
                    val signed = if (isNegative) -mag else mag
                    val rounded = roundTwoDecimals(signed).let {
                        if (kotlin.math.abs(it) < 0.005) 0.0 else it
                    }
                    ExtractedBalanceMatch(rounded, res.range)
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

        val hasStrongReversalKeyword = strongReversalKeywords.any { lowerBody.contains(it) }
        val hasSoftRefundKeyword = softRefundKeywords.any { lowerBody.contains(it) }
        val hasCreditKeyword = creditKeywords.any { lowerBody.contains(it) } || hasStrongReversalKeyword
        val hasDebitKeyword = debitKeywords.any { lowerBody.contains(it) } ||
            outgoingTransferPhrases.any { lowerBody.contains(it) } ||
            (!hasCreditKeyword && (
                versWordRegex.containsMatchIn(lowerBody) ||
                    arabicOutgoingTransferRegex.containsMatchIn(lowerBody) ||
                    frenchOutgoingTransferRegex.containsMatchIn(lowerBody)
                ))
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
            val balRose = balanceMatch != null && prevBalanceMatch != null &&
                (balanceMatch.amount - prevBalanceMatch.amount) > 0.01
            pickedIsExpense = if ((hasStrongReversalKeyword || hasSoftRefundKeyword) && balRose) {
                false
            } else {
                (sign == "-")
            }
            matchedReason = if (pickedIsExpense == true) "SIGN_MINUS" else "SIGN_PLUS"
            break
        }

        // 2. Fallback to unsigned currency amount (e.g. "300.00 DA") + transaction keyword or balance delta direction
        if (pickedAmount == null) {
            val currencyMatches = currencyAmountRegex.findAll(normalizedBody).toList()
            for (match in currencyMatches) {
                if (balanceMatch != null && rangesOverlap(match.range, balanceMatch.range)) continue
                if (prevBalanceMatch != null && rangesOverlap(match.range, prevBalanceMatch.range)) continue
                if (isInsideBalanceLabel(lowerBody, match.range.first)) continue
                val parsed = parseFlexibleAmount(match.groupValues[1]) ?: continue
                if (parsed <= 0.0) continue

                if (hasStrongReversalKeyword || hasSoftRefundKeyword) {
                    val balDropped = balanceMatch != null && prevBalanceMatch != null &&
                        (balanceMatch.amount - prevBalanceMatch.amount) < -0.01
                    pickedAmount = parsed
                    pickedIsExpense = balDropped
                    matchedReason = if (balDropped) "DEBIT_KEYWORD" else "REVERSAL_CREDIT"
                    break
                } else if (hasDebitKeyword && !hasCreditKeyword) {
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
                } else if (balanceMatch != null && prevBalanceMatch != null) {
                    val balDelta = balanceMatch.amount - prevBalanceMatch.amount
                    if (kotlin.math.abs(balDelta) >= 0.01) {
                        pickedAmount = parsed
                        pickedIsExpense = balDelta < 0.0
                        matchedReason = if (pickedIsExpense == true) "BALANCE_DECREASE" else "BALANCE_INCREASE"
                        break
                    }
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
            previousBalance = prevBalanceMatch?.amount,
            hasExplicitFeeKeyword = isExplicitFee
        )
    }

    private const val MAX_FAILED_RECOVERY_WINDOW_MILLIS = 60L * 24L * 60L * 60L * 1000L
    private const val MAX_FEE_VARIANCE_ON_RECOVERY_DZD = 100.0

    private data class PendingDebitCandidate(
        val smsId: String,
        val principalAmount: Double,
        val totalDrop: Double,
        val keptFee: Double,
        val balanceBefore: Double,
        val balanceAfter: Double,
        val timestampMillis: Long,
        val rawBody: String
    )

    private data class EffectiveSnapshotOp(
        val hasTx: Boolean,
        val txAmount: Double,
        val isExpense: Boolean
    )

    private fun resolveEffectiveOperation(
        curr: SmsBalanceSnapshot,
        referencePrevBalance: Double?
    ): EffectiveSnapshotOp {
        var effectiveHasTx = curr.hasTransaction && curr.transactionAmount > 0.0
        var effectiveTxAmount = curr.transactionAmount
        var effectiveIsExpense = curr.isExpense

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
        return EffectiveSnapshotOp(effectiveHasTx, effectiveTxAmount, effectiveIsExpense)
    }

    /**
     * High-precision Failed / Ghost Transaction ("التعاملات الفاشلة") detector.
     *
     * Catches:
     * 1. Silent Reversals: A debit SMS reduces the balance, the operation fails silently, and a later SMS
     *    reveals an unexplained positive balance jump matching that debit (principal or principal + fee),
     *    or matching the sum of 2 recent failed debits.
     * 2. Explicit Refund / Cancellation / Rejection SMS: A subsequent SMS with cancellation/refund keywords
     *    returns the debited funds (paired with the original failed debit).
     * 3. Explicit Immediate Failures: SMS stating an operation failed/rejected with zero net balance deduction.
     *
     * Also returns exclusion sets so phantom debits, explicit refunds, and balance-only recovery jumps
     * are excluded from pending Expense/Income suggestions, while genuine subsequent transactions on the
     * recovery SMS are preserved and fee calculations remain accurate.
     */
    fun analyzeFailedTransactions(
        snapshots: List<SmsBalanceSnapshot>,
        existingEntries: List<SmsFailedTransactionEntry> = emptyList(),
        dismissedIds: Set<String> = emptySet()
    ): FailedTransactionAnalysisResult {
        if (snapshots.isEmpty()) {
            val kept = existingEntries.filterNot { it.id in dismissedIds }
            return FailedTransactionAnalysisResult(failedTransactions = kept)
        }

        val ordered = snapshots
            .distinctBy { it.id }
            .sortedWith(compareBy<SmsBalanceSnapshot> { it.timestampMillis }.thenBy { it.id })

        val existingById = existingEntries.associateBy { it.id }
        val openDebits = mutableListOf<PendingDebitCandidate>()
        val resultMap = LinkedHashMap<String, SmsFailedTransactionEntry>()
        val excludedSuggestionIds = mutableSetOf<String>()
        val voidedTaxDebitSmsIds = mutableSetOf<String>()
        val reversalRecoverySmsIds = mutableSetOf<String>()
        val adjustedPrevByRecoverySmsId = mutableMapOf<String, Double>()

        // Preserve previously detected entries whose underlying debit SMS is no longer in current window
        val evaluatedSmsIds = ordered.map { it.id }.toSet()
        existingEntries
            .filter { entry ->
                entry.id !in dismissedIds &&
                    entry.failedDebitSmsId.isNotBlank() &&
                    entry.failedDebitSmsId !in evaluatedSmsIds &&
                    entry.recoverySmsId !in evaluatedSmsIds
            }
            .forEach { resultMap[it.id] = it }

        for (i in ordered.indices) {
            val curr = ordered[i]
            val prev = if (i > 0) ordered[i - 1] else null
            val inlinePrevBalance = if (prev == null && curr.rawBody.isNotBlank()) {
                extractPreviousBalanceMatchFromBody(normalizeArabicDigits(curr.rawBody))?.amount
            } else null
            val refPrevBalance = prev?.balance ?: inlinePrevBalance

            val lowerBody = normalizeArabicDigits(curr.rawBody).lowercase(Locale.US)
            val hasExplicitFailure = explicitFailureKeywords.any { lowerBody.contains(it) }
            val hasStrongReversal = strongReversalKeywords.any { lowerBody.contains(it) }
            val hasSoftRefund = softRefundKeywords.any { lowerBody.contains(it) }
            val isDuplicatePrev = prev != null &&
                kotlin.math.abs(curr.balance - prev.balance) <= 0.01 &&
                prev.hasTransaction == curr.hasTransaction &&
                kotlin.math.abs(curr.transactionAmount - prev.transactionAmount) <= 0.01 &&
                kotlin.math.abs(curr.timestampMillis - prev.timestampMillis) <= 180_000L

            val op = resolveEffectiveOperation(curr, refPrevBalance)
            var matchedAnyRecoveryHere = false
            var consumedAsPureFailureOrReversal = false

            // 1. Immediate explicit failed attempt where no funds were actually deducted
            if ((hasExplicitFailure || (hasStrongReversal && op.isExpense)) &&
                refPrevBalance != null &&
                kotlin.math.abs(curr.balance - refPrevBalance) <= 0.05
            ) {
                val entryId = "fail_nodeduct_${curr.id}"
                excludedSuggestionIds.add(curr.id)
                consumedAsPureFailureOrReversal = true
                if (entryId !in dismissedIds) {
                    val amt = if (op.hasTx) op.txAmount else 0.0
                    val amtText = if (amt > 0.0) formatCompactAmount(amt) else ""
                    resultMap[entryId] = SmsFailedTransactionEntry(
                        id = entryId,
                        failedDebitSmsId = curr.id,
                        recoverySmsId = curr.id,
                        debitAmount = amt,
                        recoveredAmount = amt,
                        unrefundedFeeAmount = 0.0,
                        balanceBeforeDebit = refPrevBalance,
                        balanceAfterDebit = curr.balance,
                        expectedBalanceBeforeRecovery = refPrevBalance,
                        actualBalanceAtRecovery = curr.balance,
                        debitTimestampMillis = curr.timestampMillis,
                        recoveredTimestampMillis = curr.timestampMillis,
                        detectionKind = "EXPLICIT_FAILED_NO_DEDUCTION",
                        confidencePercent = 99,
                        titleAr = if (amt > 0.0) "محاولة عملية فاشلة ($amtText دج)" else "محاولة عملية فاشلة",
                        titleEn = if (amt > 0.0) "Failed Transaction ($amtText DA)" else "Failed Transaction",
                        explanationAr = if (amt > 0.0) {
                            "العملية بقيمة $amtText دج فشلت ولم يتم خصمها فعلياً من رصيدك (${formatCompactAmount(curr.balance)} دج) — تم استبعادها من المصاريف."
                        } else {
                            "تم رصد رسالة فشل عملية بدون أي خصم فعلي من الرصيد (${formatCompactAmount(curr.balance)} دج)."
                        },
                        explanationEn = if (amt > 0.0) {
                            "Operation for $amtText DA failed with no actual deduction (Balance: ${formatCompactAmount(curr.balance)} DA) — excluded from expenses."
                        } else {
                            "Detected explicit failed operation with unchanged balance (${formatCompactAmount(curr.balance)} DA)."
                        },
                        debitRawBody = curr.rawBody,
                        recoveryRawBody = curr.rawBody,
                        isResolved = existingById[entryId]?.isResolved == true
                    )
                }
            } else if ((hasStrongReversal || hasSoftRefund) && !isDuplicatePrev) {
                // 2. Explicit Refund / Cancellation / Reversal SMS matching an earlier unreversed debit
                val balGain = if (refPrevBalance != null) roundTwoDecimals(curr.balance - refPrevBalance) else 0.0
                val explicitRecoveryAmt = when {
                    balGain >= 1.0 -> balGain
                    op.hasTx && !op.isExpense && op.txAmount >= 1.0 -> op.txAmount
                    else -> 0.0
                }
                if (explicitRecoveryAmt >= 1.0) {
                    val candidateIdx = findBestMatchingOpenDebitIndex(
                        openDebits = openDebits,
                        recoveredAmt = explicitRecoveryAmt,
                        recoveryTimestampMillis = curr.timestampMillis,
                        allowSmallFeeOffset = true
                    )
                    if (candidateIdx >= 0) {
                        val matched = openDebits.removeAt(candidateIdx)
                        matchedAnyRecoveryHere = true
                        consumedAsPureFailureOrReversal = true
                        excludedSuggestionIds.add(matched.smsId)
                        excludedSuggestionIds.add(curr.id)
                        reversalRecoverySmsIds.add(curr.id)
                        val returnedFullDrop = kotlin.math.abs(explicitRecoveryAmt - matched.totalDrop) <= 0.50
                        val unrefundedFee = if (returnedFullDrop) 0.0 else roundTwoDecimals((matched.totalDrop - explicitRecoveryAmt).coerceAtLeast(0.0))
                        if (unrefundedFee < 0.01) {
                            voidedTaxDebitSmsIds.add(matched.smsId)
                        }

                        val entryId = "fail_${matched.smsId}_${curr.id}"
                        if (entryId !in dismissedIds) {
                            resultMap[entryId] = buildPairedFailedEntry(
                                id = entryId,
                                debit = matched,
                                recovery = curr,
                                recoveredAmount = explicitRecoveryAmt,
                                unrefundedFeeAmount = unrefundedFee,
                                expectedBeforeRecovery = roundTwoDecimals((refPrevBalance ?: matched.balanceAfter) + explicitRecoveryAmt),
                                detectionKind = "EXPLICIT_REFUND_REVERSAL",
                                confidencePercent = 99,
                                isResolved = existingById[entryId]?.isResolved == true
                            )
                        }
                    } else if (hasStrongReversal) {
                        consumedAsPureFailureOrReversal = true
                        excludedSuggestionIds.add(curr.id)
                        reversalRecoverySmsIds.add(curr.id)
                        val entryId = "fail_rev_${curr.id}"
                        if (entryId !in dismissedIds) {
                            val balBefore = refPrevBalance ?: roundTwoDecimals(curr.balance - explicitRecoveryAmt)
                            resultMap[entryId] = SmsFailedTransactionEntry(
                                id = entryId,
                                failedDebitSmsId = curr.id,
                                recoverySmsId = curr.id,
                                debitAmount = explicitRecoveryAmt,
                                recoveredAmount = explicitRecoveryAmt,
                                unrefundedFeeAmount = 0.0,
                                balanceBeforeDebit = balBefore,
                                balanceAfterDebit = balBefore,
                                expectedBalanceBeforeRecovery = curr.balance,
                                actualBalanceAtRecovery = curr.balance,
                                debitTimestampMillis = curr.timestampMillis,
                                recoveredTimestampMillis = curr.timestampMillis,
                                detectionKind = "EXPLICIT_REVERSAL_UNLINKED",
                                confidencePercent = 95,
                                titleAr = "إلغاء / استرجاع عملية (${formatCompactAmount(explicitRecoveryAmt)} دج)",
                                titleEn = "Cancelled / Reversed Operation (${formatCompactAmount(explicitRecoveryAmt)} DA)",
                                explanationAr = "وصل إشعار إلغاء واسترجاع مبلغ ${formatCompactAmount(explicitRecoveryAmt)} دج إلى الرصيد (${formatCompactAmount(curr.balance)} دج) — تم استبعادها من الدخل لمنع خلط الحسابات.",
                                explanationEn = "Explicit reversal SMS returned ${formatCompactAmount(explicitRecoveryAmt)} DA (Balance: ${formatCompactAmount(curr.balance)} DA) — excluded from income to avoid ledger confusion.",
                                debitRawBody = curr.rawBody,
                                recoveryRawBody = curr.rawBody,
                                isResolved = existingById[entryId]?.isResolved == true
                            )
                        }
                    }
                }
            }

            // 3. Silent Recovery Caught via Unexplained Positive Balance Jump on Next SMS
            if (!matchedAnyRecoveryHere && !isDuplicatePrev && refPrevBalance != null) {
                val expectedBalWithoutJump = roundTwoDecimals(
                    when {
                        op.hasTx && op.isExpense -> refPrevBalance - op.txAmount
                        op.hasTx && !op.isExpense -> refPrevBalance + op.txAmount
                        else -> refPrevBalance
                    }
                )
                val jump = roundTwoDecimals(curr.balance - expectedBalWithoutJump)

                if (jump >= 1.0 && openDebits.isNotEmpty()) {
                    val exactIdx = findBestMatchingOpenDebitIndex(
                        openDebits = openDebits,
                        recoveredAmt = jump,
                        recoveryTimestampMillis = curr.timestampMillis,
                        allowSmallFeeOffset = (op.hasTx && op.isExpense)
                    )
                    if (exactIdx >= 0) {
                        val matched = openDebits.removeAt(exactIdx)
                        matchedAnyRecoveryHere = true
                        excludedSuggestionIds.add(matched.smsId)
                        // If recovery SMS had NO real operation (balance-only SMS showing the money came back),
                        // exclude it from pending Income suggestions so it doesn't count as fake income!
                        if (!op.hasTx || hasExplicitFailure || hasStrongReversal) {
                            excludedSuggestionIds.add(curr.id)
                        }

                        val isExactTotalDrop = kotlin.math.abs(jump - matched.totalDrop) <= 0.50
                        val isExactPrincipal = kotlin.math.abs(jump - matched.principalAmount) <= 0.50
                        val recoveredAmt = when {
                            isExactTotalDrop -> matched.totalDrop
                            isExactPrincipal -> matched.principalAmount
                            else -> matched.principalAmount
                        }
                        val unrefundedFee = if (isExactTotalDrop) 0.0 else matched.keptFee
                        if (unrefundedFee < 0.01) {
                            voidedTaxDebitSmsIds.add(matched.smsId)
                        }
                        adjustedPrevByRecoverySmsId[curr.id] = roundTwoDecimals(refPrevBalance + recoveredAmt)

                        val kind = if (unrefundedFee >= 0.01 && !isExactTotalDrop) {
                            "FAILED_WITH_FEE_KEPT"
                        } else {
                            "SILENT_BALANCE_JUMP"
                        }
                        val confidence = if (isExactTotalDrop || isExactPrincipal) 99 else 94
                        val entryId = "fail_${matched.smsId}_${curr.id}"
                        if (entryId !in dismissedIds) {
                            resultMap[entryId] = buildPairedFailedEntry(
                                id = entryId,
                                debit = matched,
                                recovery = curr,
                                recoveredAmount = recoveredAmt,
                                unrefundedFeeAmount = unrefundedFee,
                                expectedBeforeRecovery = expectedBalWithoutJump,
                                detectionKind = kind,
                                confidencePercent = confidence,
                                isResolved = existingById[entryId]?.isResolved == true
                            )
                        }
                    } else {
                        // Try matching sum of 2 recent failed debits returned together before curr
                        val pair = findMatchingOpenDebitPair(
                            openDebits = openDebits,
                            jump = jump,
                            recoveryTimestampMillis = curr.timestampMillis
                        )
                        if (pair != null) {
                            val (idxHigh, idxLow, matchedTotalDropSum) = pair
                            val firstRemoved = openDebits.removeAt(idxHigh)
                            val secondRemoved = openDebits.removeAt(idxLow)
                            val matchedPair = listOf(secondRemoved, firstRemoved)
                            matchedAnyRecoveryHere = true
                            if (!op.hasTx || hasExplicitFailure || hasStrongReversal) {
                                excludedSuggestionIds.add(curr.id)
                            }
                            adjustedPrevByRecoverySmsId[curr.id] = roundTwoDecimals(refPrevBalance + jump)

                            for (debitItem in matchedPair) {
                                excludedSuggestionIds.add(debitItem.smsId)
                                val recAmt = if (matchedTotalDropSum) debitItem.totalDrop else debitItem.principalAmount
                                val unrefFee = if (matchedTotalDropSum) 0.0 else debitItem.keptFee
                                if (unrefFee < 0.01) voidedTaxDebitSmsIds.add(debitItem.smsId)
                                val entryId = "fail_${debitItem.smsId}_${curr.id}"
                                if (entryId !in dismissedIds) {
                                    resultMap[entryId] = buildPairedFailedEntry(
                                        id = entryId,
                                        debit = debitItem,
                                        recovery = curr,
                                        recoveredAmount = recAmt,
                                        unrefundedFeeAmount = unrefFee,
                                        expectedBeforeRecovery = expectedBalWithoutJump,
                                        detectionKind = "MULTI_DEBIT_RECOVERY",
                                        confidencePercent = 97,
                                        isResolved = existingById[entryId]?.isResolved == true
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Register genuine outgoing debit as candidate in openDebits AFTER checking recoveries on curr
            if (!consumedAsPureFailureOrReversal &&
                !isDuplicatePrev &&
                op.hasTx &&
                op.isExpense &&
                op.txAmount > 0.0
            ) {
                val basePrev = adjustedPrevByRecoverySmsId[curr.id] ?: refPrevBalance
                val actualDrop = if (basePrev != null) {
                    roundTwoDecimals(basePrev - curr.balance)
                } else {
                    op.txAmount
                }
                if (actualDrop > 0.50) {
                    val balBefore = basePrev ?: roundTwoDecimals(curr.balance + actualDrop)
                    val keptFee = roundTwoDecimals((actualDrop - op.txAmount).coerceAtLeast(0.0))
                    openDebits.add(
                        PendingDebitCandidate(
                            smsId = curr.id,
                            principalAmount = op.txAmount,
                            totalDrop = actualDrop,
                            keptFee = keptFee,
                            balanceBefore = balBefore,
                            balanceAfter = curr.balance,
                            timestampMillis = curr.timestampMillis,
                            rawBody = curr.rawBody
                        )
                    )
                    // Keep openDebits bounded to recent lookback
                    if (openDebits.size > 40) {
                        openDebits.removeAt(0)
                    }
                }
            }
        }

        val sortedList = resultMap.values
            .sortedByDescending { it.recoveredTimestampMillis }
        return FailedTransactionAnalysisResult(
            failedTransactions = sortedList,
            excludedSuggestionSmsIds = excludedSuggestionIds,
            voidedTaxDebitSmsIds = voidedTaxDebitSmsIds,
            reversalRecoverySmsIds = reversalRecoverySmsIds,
            adjustedPrevBalanceByRecoverySmsId = adjustedPrevByRecoverySmsId
        )
    }

    private fun findBestMatchingOpenDebitIndex(
        openDebits: List<PendingDebitCandidate>,
        recoveredAmt: Double,
        recoveryTimestampMillis: Long,
        allowSmallFeeOffset: Boolean
    ): Int {
        // Priority 1: Most recent exact totalDrop match (principal + fee returned)
        for (idx in openDebits.indices.reversed()) {
            val d = openDebits[idx]
            if (recoveryTimestampMillis - d.timestampMillis > MAX_FAILED_RECOVERY_WINDOW_MILLIS) continue
            if (kotlin.math.abs(recoveredAmt - d.totalDrop) <= 0.50) return idx
        }
        // Priority 2: Most recent exact principalAmount match (principal returned)
        for (idx in openDebits.indices.reversed()) {
            val d = openDebits[idx]
            if (recoveryTimestampMillis - d.timestampMillis > MAX_FAILED_RECOVERY_WINDOW_MILLIS) continue
            if (kotlin.math.abs(recoveredAmt - d.principalAmount) <= 0.50) return idx
        }
        // Priority 3: Principal returned while recovery SMS itself deducted a small bank fee (<= 100 DA)
        if (allowSmallFeeOffset && recoveredAmt >= 200.0) {
            for (idx in openDebits.indices.reversed()) {
                val d = openDebits[idx]
                if (recoveryTimestampMillis - d.timestampMillis > MAX_FAILED_RECOVERY_WINDOW_MILLIS) continue
                if (d.principalAmount >= 200.0 && recoveredAmt >= d.principalAmount * 0.85) {
                    val principalGap = roundTwoDecimals(d.principalAmount - recoveredAmt)
                    val dropGap = roundTwoDecimals(d.totalDrop - recoveredAmt)
                    if (principalGap in 0.51..MAX_FEE_VARIANCE_ON_RECOVERY_DZD ||
                        dropGap in 0.51..MAX_FEE_VARIANCE_ON_RECOVERY_DZD
                    ) {
                        return idx
                    }
                }
            }
        }
        return -1
    }

    private data class MatchedDebitPair(
        val highIndex: Int,
        val lowIndex: Int,
        val matchedTotalDropSum: Boolean
    )

    private fun findMatchingOpenDebitPair(
        openDebits: List<PendingDebitCandidate>,
        jump: Double,
        recoveryTimestampMillis: Long
    ): MatchedDebitPair? {
        if (openDebits.size < 2) return null
        val start = (openDebits.size - 12).coerceAtLeast(0)
        for (i in openDebits.indices.reversed()) {
            if (i < start) break
            val d1 = openDebits[i]
            if (recoveryTimestampMillis - d1.timestampMillis > MAX_FAILED_RECOVERY_WINDOW_MILLIS) continue
            for (j in (i - 1) downTo start) {
                val d2 = openDebits[j]
                if (recoveryTimestampMillis - d2.timestampMillis > MAX_FAILED_RECOVERY_WINDOW_MILLIS) continue
                if (kotlin.math.abs(jump - (d1.totalDrop + d2.totalDrop)) <= 0.50) {
                    return MatchedDebitPair(i, j, matchedTotalDropSum = true)
                }
                if (kotlin.math.abs(jump - (d1.principalAmount + d2.principalAmount)) <= 0.50) {
                    return MatchedDebitPair(i, j, matchedTotalDropSum = false)
                }
            }
        }
        return null
    }

    private fun buildPairedFailedEntry(
        id: String,
        debit: PendingDebitCandidate,
        recovery: SmsBalanceSnapshot,
        recoveredAmount: Double,
        unrefundedFeeAmount: Double,
        expectedBeforeRecovery: Double,
        detectionKind: String,
        confidencePercent: Int,
        isResolved: Boolean
    ): SmsFailedTransactionEntry {
        val debitFmt = formatCompactAmount(debit.principalAmount)
        val recFmt = formatCompactAmount(recoveredAmount)
        val beforeFmt = formatCompactAmount(debit.balanceBefore)
        val afterFmt = formatCompactAmount(debit.balanceAfter)
        val expFmt = formatCompactAmount(expectedBeforeRecovery)
        val actFmt = formatCompactAmount(recovery.balance)

        val (titleAr, titleEn) = when (detectionKind) {
            "EXPLICIT_REFUND_REVERSAL" ->
                "معاملة فاشلة واسترجاع صريح ($recFmt دج)" to "Failed Op & Explicit Refund ($recFmt DA)"
            "FAILED_WITH_FEE_KEPT" ->
                "معاملة فاشلة — استُرجع $recFmt دج (رسوم ${formatCompactAmount(unrefundedFeeAmount)} دج)" to
                    "Failed Op — Returned $recFmt DA (${formatCompactAmount(unrefundedFeeAmount)} DA fee kept)"
            "MULTI_DEBIT_RECOVERY" ->
                "معاملة فاشلة ضمن استرجاع مجمع ($recFmt دج)" to "Failed Op in Batch Recovery ($recFmt DA)"
            else ->
                "معاملة وهمية/فاشلة مسترجعة ($recFmt دج)" to "Ghost / Failed Debit Recovered ($recFmt DA)"
        }

        val feeSuffixAr = if (unrefundedFeeAmount >= 0.01) {
            " • ملاحظة: بقيت رسوم ${formatCompactAmount(unrefundedFeeAmount)} دج غير مسترجعة"
        } else ""
        val feeSuffixEn = if (unrefundedFeeAmount >= 0.01) {
            " • Note: ${formatCompactAmount(unrefundedFeeAmount)} DA fee remained unrefunded"
        } else ""

        val explanationAr = when (detectionKind) {
            "EXPLICIT_REFUND_REVERSAL" ->
                "خُصم $debitFmt دج ($beforeFmt ← $afterFmt دج) ثم وصل إشعار الإلغاء والاسترجاع بقيمة $recFmt دج إلى $actFmt دج — تم استبعاد العمليتين من المصاريف والدخل$feeSuffixAr."
            else ->
                "خُصم $debitFmt دج ($beforeFmt ← $afterFmt دج) دون إتمام العملية، وعند الرسالة التالية كان المتوقع $expFmt دج لكن الرصيد الفعلي قفز إلى $actFmt دج (+$recFmt دج عادت للحساب)$feeSuffixAr."
        }
        val explanationEn = when (detectionKind) {
            "EXPLICIT_REFUND_REVERSAL" ->
                "Debited $debitFmt DA ($beforeFmt → $afterFmt DA), then explicitly reversed/refunded $recFmt DA to $actFmt DA — both excluded to keep your ledger accurate$feeSuffixEn."
            else ->
                "Debited $debitFmt DA ($beforeFmt → $afterFmt DA) on an incomplete operation; next SMS expected $expFmt DA but actual balance jumped to $actFmt DA (+$recFmt DA silently returned)$feeSuffixEn."
        }

        return SmsFailedTransactionEntry(
            id = id,
            failedDebitSmsId = debit.smsId,
            recoverySmsId = recovery.id,
            debitAmount = debit.principalAmount,
            recoveredAmount = recoveredAmount,
            unrefundedFeeAmount = unrefundedFeeAmount,
            balanceBeforeDebit = debit.balanceBefore,
            balanceAfterDebit = debit.balanceAfter,
            expectedBalanceBeforeRecovery = expectedBeforeRecovery,
            actualBalanceAtRecovery = recovery.balance,
            debitTimestampMillis = debit.timestampMillis,
            recoveredTimestampMillis = recovery.timestampMillis,
            detectionKind = detectionKind,
            confidencePercent = confidencePercent,
            titleAr = titleAr,
            titleEn = titleEn,
            explanationAr = explanationAr,
            explanationEn = explanationEn,
            debitRawBody = debit.rawBody,
            recoveryRawBody = recovery.rawBody,
            isResolved = isResolved
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
        existingAnomalies: List<SmsTaxAnomalyEntry> = emptyList(),
        failedAnalysis: FailedTransactionAnalysisResult = analyzeFailedTransactions(snapshots)
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
            // Skip tax detection on fully refunded failed debits, immediate no-deduct failures, or reversal SMS
            if (curr.id in failedAnalysis.voidedTaxDebitSmsIds ||
                curr.id in failedAnalysis.reversalRecoverySmsIds
            ) continue

            val prev = if (i > 0) ordered[i - 1] else null

            val inlinePrevBalance = if (prev == null && curr.rawBody.isNotBlank()) {
                extractPreviousBalanceMatchFromBody(normalizeArabicDigits(curr.rawBody))?.amount
            } else null

            val rawPrevBalance = prev?.balance ?: inlinePrevBalance
            val referencePrevBalance = failedAnalysis.adjustedPrevBalanceByRecoverySmsId[curr.id] ?: rawPrevBalance

            val resolvedOp = resolveEffectiveOperation(curr, rawPrevBalance)
            val effectiveHasTx = resolvedOp.hasTx
            val effectiveTxAmount = resolvedOp.txAmount
            val effectiveIsExpense = resolvedOp.isExpense

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

    /**
     * Detects Income (+) or Outcome (-) operation suggestions whenever Alg Post Balance changes,
     * covering both explicit transaction SMS and balance-only SMS where only the new balance changed.
     */
    fun detectBalanceOperationSuggestions(
        snapshots: List<SmsBalanceSnapshot>,
        minTimestampMillis: Long = 0L,
        excludedSmsIds: Set<String> = analyzeFailedTransactions(snapshots).excludedSuggestionSmsIds
    ): List<SmsSuggestionEntry> {
        if (snapshots.isEmpty()) return emptyList()

        val ordered = snapshots
            .distinctBy { it.id }
            .sortedWith(compareBy<SmsBalanceSnapshot> { it.timestampMillis }.thenBy { it.id })

        val results = mutableListOf<SmsSuggestionEntry>()
        for (i in ordered.indices) {
            val curr = ordered[i]
            val prev = if (i > 0) ordered[i - 1] else null
            val inlinePrev = if (prev == null && curr.rawBody.isNotBlank()) {
                extractPreviousBalanceMatchFromBody(normalizeArabicDigits(curr.rawBody))?.amount
            } else null
            val refPrevBalance = prev?.balance ?: inlinePrev

            if (minTimestampMillis > 0L && curr.timestampMillis <= minTimestampMillis) {
                continue
            }
            if (curr.id in excludedSmsIds) {
                continue
            }

            val explicitParsed = if (curr.rawBody.isNotBlank()) {
                parseSms(
                    sender = curr.sender,
                    body = curr.rawBody,
                    timestampMillis = curr.timestampMillis,
                    allowAnySenderForManualTest = true
                )
            } else null

            if (explicitParsed != null) {
                var resolved = explicitParsed
                if (refPrevBalance != null &&
                    explicitParsed.matchedReason != "SIGN_MINUS" &&
                    explicitParsed.matchedReason != "SIGN_PLUS"
                ) {
                    val balDelta = roundTwoDecimals(curr.balance - refPrevBalance)
                    val shouldBeExpense = when {
                        balDelta < -0.01 && kotlin.math.abs(balDelta) >= explicitParsed.amount * 0.5 -> true
                        balDelta > 0.01 && balDelta >= explicitParsed.amount * 0.5 -> false
                        else -> explicitParsed.isExpense
                    }
                    if (shouldBeExpense != explicitParsed.isExpense) {
                        val lower = normalizeArabicDigits(curr.rawBody).lowercase(Locale.US)
                        val (cat, titleAr, titleEn) = classifyMetadata(
                            lower,
                            shouldBeExpense,
                            explicitParsed.hasExplicitFeeKeyword
                        )
                        resolved = explicitParsed.copy(
                            isExpense = shouldBeExpense,
                            suggestedCategory = cat,
                            suggestedTitleAr = titleAr,
                            suggestedTitleEn = titleEn,
                            matchedReason = if (shouldBeExpense) "BALANCE_DECREASE" else "BALANCE_INCREASE"
                        )
                    }
                }
                results.add(
                    resolved.copy(
                        extractedBalance = resolved.extractedBalance ?: curr.balance,
                        previousBalance = refPrevBalance ?: resolved.previousBalance
                    )
                )
                continue
            }

            if (refPrevBalance != null) {
                val delta = roundTwoDecimals(curr.balance - refPrevBalance)
                if (kotlin.math.abs(delta) >= 0.01) {
                    val isOutcome = delta < 0.0
                    val candidateAmt = if (curr.rawBody.isNotBlank()) {
                        extractUnsignedCandidateAmountOutsideBalances(normalizeArabicDigits(curr.rawBody))
                    } else null
                    val finalAmt = candidateAmt?.takeIf { it > 0.0 } ?: roundTwoDecimals(kotlin.math.abs(delta))
                    val lower = normalizeArabicDigits(curr.rawBody).lowercase(Locale.US)
                    val (cat, titleAr, titleEn) = if (candidateAmt != null) {
                        classifyMetadata(lower, isOutcome, curr.hasExplicitFeeKeyword)
                    } else {
                        Triple(
                            if (isOutcome) "other" else "transfer",
                            if (isOutcome) "سحب / عملية خصم في بريد الجزائر" else "دخول رصيد / إيداع في بريد الجزائر",
                            if (isOutcome) "Alg Post Balance Withdrawal" else "Alg Post Balance Income"
                        )
                    }
                    results.add(
                        SmsSuggestionEntry(
                            id = if (candidateAmt != null) curr.id else "bal_op_${curr.id}",
                            sender = curr.sender.ifBlank { "ALG Poste" },
                            rawBody = curr.rawBody,
                            amount = finalAmt,
                            isExpense = isOutcome,
                            suggestedCategory = cat,
                            suggestedTitleAr = titleAr,
                            suggestedTitleEn = titleEn,
                            receivedAtMillis = curr.timestampMillis,
                            matchedReason = if (isOutcome) "BALANCE_DELTA_EXPENSE" else "BALANCE_DELTA_INCOME",
                            extractedBalance = curr.balance,
                            previousBalance = refPrevBalance,
                            hasExplicitFeeKeyword = curr.hasExplicitFeeKeyword
                        )
                    )
                }
            }
        }
        return results.sortedByDescending { it.receivedAtMillis }
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
                    Triple("taxes_fees", "ضرائب ورسوم (بريد الجزائر)", "Bank Tax / Fee (ALG Poste)")

                lowerBody.contains("mobilis") || lowerBody.contains("djezzy") ||
                    lowerBody.contains("ooredoo") || lowerBody.contains("internet") ||
                    lowerBody.contains("idoom") || lowerBody.contains("recharge") ||
                    lowerBody.contains("flexy") || lowerBody.contains("فليكسي") ||
                    lowerBody.contains("شحن") || lowerBody.contains("تعبئة") ->
                    Triple("internet", "شحن واتصالات (بريدي موب)", "Telecom & Recharge (BaridiMob)")

                lowerBody.contains("sonelgaz") || lowerBody.contains("ade") ||
                    lowerBody.contains("seaal") || lowerBody.contains("facture") ||
                    lowerBody.contains("فاتورة") || lowerBody.contains("تسديد") ->
                    Triple("utilities", "دفع فاتورة (بريدي موب)", "Bill Payment (BaridiMob)")

                lowerBody.contains("tpe") || lowerBody.contains("achat") ||
                    lowerBody.contains("paiement") || lowerBody.contains("payement") ||
                    lowerBody.contains("baridipay") || lowerBody.contains("شراء") ->
                    Triple("shopping", "دفع / شراء بالبطاقة (بريدي موب)", "Card Payment (BaridiMob)")

                lowerBody.contains("retrait") || lowerBody.contains("gab") ||
                    lowerBody.contains("dab") || lowerBody.contains("سحب") ->
                    Triple("other", "سحب نقدي (بريدي موب)", "ATM Withdrawal (BaridiMob)")

                lowerBody.contains("virement") || lowerBody.contains("transfert") ||
                    lowerBody.contains("تحويل") ->
                    Triple("other", "تحويل صادر (بريدي موب)", "Outgoing Transfer (BaridiMob)")

                else ->
                    Triple("other", "سحب / مصروف بريدي موب", "BaridiMob Expense (ALG Poste)")
            }
        } else {
            when {
                lowerBody.contains("salaire") || lowerBody.contains("راتب") ||
                    lowerBody.contains("paie") || lowerBody.contains("salary") ||
                    lowerBody.contains("أجرة") ->
                    Triple("salary", "راتب وارد (بريدي موب)", "Salary Credit (BaridiMob)")

                lowerBody.contains("bourse") || lowerBody.contains("allocation") ||
                    lowerBody.contains("pension") || lowerBody.contains("منحة") ||
                    lowerBody.contains("معاش") || lowerBody.contains("تقاعد") ->
                    Triple("other_income", "منحة / معاش وارد (بريدي موب)", "Grant / Pension Income (BaridiMob)")

                lowerBody.contains("remboursement") || lowerBody.contains("refund") ||
                    lowerBody.contains("استرجاع") || lowerBody.contains("تعويض") ->
                    Triple("other_income", "استرجاع مبلغ (بريدي موب)", "Refund Received (BaridiMob)")

                lowerBody.contains("virement") || lowerBody.contains("transfert") ||
                    lowerBody.contains("تحويل") ->
                    Triple("transfer", "تحويل مالي وارد (بريدي موب)", "Incoming Transfer (BaridiMob)")

                lowerBody.contains("versement") || lowerBody.contains("dépôt") ||
                    lowerBody.contains("depot") || lowerBody.contains("إيداع") ||
                    lowerBody.contains("ايداع") || lowerBody.contains("صب") ->
                    Triple("transfer", "إيداع في الحساب (بريدي موب)", "Account Deposit (BaridiMob)")

                else ->
                    Triple("other_income", "دخول رصيد / دخل بريدي موب", "BaridiMob Income (ALG Poste)")
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
            if (ch == '\u200E' || ch == '\u200F' || ch == '\u061C' ||
                ch in '\u202A'..'\u202E' || ch in '\u2066'..'\u2069'
            ) {
                continue
            }
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
                ?.filter { it.id.isNotBlank() && it.balance.isFinite() }
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

    private const val MAX_FAILED_ITEMS = 120

    private val failedListAdapter by lazy {
        val type = Types.newParameterizedType(List::class.java, SmsFailedTransactionEntry::class.java)
        moshi.adapter<List<SmsFailedTransactionEntry>>(type)
    }

    fun parseFailedTransactions(json: String): List<SmsFailedTransactionEntry> {
        if (json.isBlank()) return emptyList()
        return try {
            failedListAdapter.fromJson(json)
                ?.filter { it.id.isNotBlank() }
                ?.sortedByDescending { it.recoveredTimestampMillis }
                ?.take(MAX_FAILED_ITEMS)
                ?: emptyList()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun encodeFailedTransactions(list: List<SmsFailedTransactionEntry>): String {
        return try {
            failedListAdapter.toJson(
                list.distinctBy { it.id }
                    .sortedByDescending { it.recoveredTimestampMillis }
                    .take(MAX_FAILED_ITEMS)
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
