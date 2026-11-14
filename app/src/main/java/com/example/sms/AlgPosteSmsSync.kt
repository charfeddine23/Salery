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
    val matchedReason: String = ""
) {
    val transactionType: TransactionType
        get() = if (isExpense) TransactionType.EXPENSE else TransactionType.INCOME
}

object AlgPosteSmsAnalyzer {

    private val debitKeywords = listOf(
        "retrait", "débit", "debit", "débité", "debite", "paiement", "payement",
        "tpe", "achat", "virement emis", "virement émis", "émis", "emis",
        "frais", "commission", "facture", "cotisation", "transfert sortant",
        "سحب", "خصم", "اقتطاع", "دفع", "شراء", "صرف", "تحويل صادر", "فاتورة"
    )

    private val creditKeywords = listOf(
        "crédit", "credit", "crédité", "credite", "versement", "alimentation",
        "virement reçu", "virement recu", "reçu", "recu", "salaire", "dépôt",
        "depot", "remboursement", "virement entrant", "bonification",
        "إيداع", "تحويل وارد", "وارد", "استلام", "راتب", "مستحقات", "إضافة", "رصيد مضاف"
    )

    private val balanceContextMarkers = listOf(
        "nouveau solde", "solde disponible", "solde", "balance", "الرصيد", "رصيدك"
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
        // Bucket timestamp to nearest minute so BroadcastReceiver and Inbox scan generate the exact same ID
        val minuteBucket = timestampMillis / 60_000L
        val payload = "${sender.trim().lowercase(Locale.US)}|${body.trim()}|$minuteBucket"
        val digest = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(Charsets.UTF_8))
        return digest.take(16).joinToString("") { "%02x".format(it) }
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
                    // Check which keyword appears closer/earlier to the transaction start
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

        val (category, titleAr, titleEn) = classifyMetadata(lowerBody, finalIsExpense)
        val id = buildSuggestionId(trimmedSender, trimmedBody, timestampMillis)

        return SmsSuggestionEntry(
            id = id,
            sender = if (isAlgPosteSender(trimmedSender)) trimmedSender else "ALG Poste",
            rawBody = trimmedBody,
            amount = finalAmount,
            isExpense = finalIsExpense,
            suggestedCategory = category,
            suggestedTitleAr = titleAr,
            suggestedTitleEn = titleEn,
            receivedAtMillis = timestampMillis,
            matchedReason = matchedReason
        )
    }

    private fun isInsideBalanceLabel(lowerText: String, matchStart: Int): Boolean {
        val windowStart = (matchStart - 26).coerceAtLeast(0)
        val prefix = lowerText.substring(windowStart, matchStart)
        return balanceContextMarkers.any { marker -> prefix.contains(marker) }
    }

    private fun classifyMetadata(lowerBody: String, isExpense: Boolean): Triple<String, String, String> {
        return if (isExpense) {
            when {
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
                    // European: 1.500,25 -> 1500.25
                    compact.replace(".", "").replace(',', '.')
                } else {
                    // US: 1,500.25 -> 1500.25
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

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val suggestionListAdapter by lazy {
        val type = Types.newParameterizedType(List::class.java, SmsSuggestionEntry::class.java)
        moshi.adapter<List<SmsSuggestionEntry>>(type)
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

    fun scanInbox(context: Context, maxRowsToInspect: Int = 250): List<SmsSuggestionEntry> {
        if (!hasSmsPermissions(context)) return emptyList()

        val results = mutableListOf<SmsSuggestionEntry>()
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
                    val parsed = AlgPosteSmsAnalyzer.parseSms(
                        sender = sender,
                        body = body,
                        timestampMillis = dateMillis,
                        allowAnySenderForManualTest = false
                    )
                    if (parsed != null) {
                        results.add(parsed)
                    }
                }
            }
        } catch (_: Throwable) {
        }

        return results.distinctBy { it.id }.sortedByDescending { it.receivedAtMillis }
    }
}
