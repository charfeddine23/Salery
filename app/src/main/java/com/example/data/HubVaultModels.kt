package com.example.data

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.math.BigInteger
import java.security.SecureRandom
import java.util.UUID

enum class PasswordStrengthLevel {
    WEAK,
    FAIR,
    GOOD,
    STRONG
}

data class PasswordAnalysis(
    val score: Int,
    val level: PasswordStrengthLevel,
    val isReused: Boolean,
    val tipsAr: List<String>,
    val tipsEn: List<String>
)

@JsonClass(generateAdapter = true)
data class VaultKeyEntry(
    val id: String = UUID.randomUUID().toString(),
    val label: String = "",
    val username: String = "",
    val secretValue: String = "",
    val category: String = "APP", // "APP", "BANK", "EMAIL", "PIN"
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class CcpAccountEntry(
    val id: String = UUID.randomUUID().toString(),
    val firstName: String = "",
    val lastName: String = "",
    val holderName: String = "",
    val ccpNumber: String = "",
    val ccpKey: String = "",
    val ripFull: String = "",
    val agencyNote: String = "",
    val contactName: String = "",
    val contactUri: String = "",
    val contactPhone: String = "",
    val isPinned: Boolean = false,
    val pinnedAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
) {
    val displayFullName: String
        get() = listOf(firstName.trim(), lastName.trim())
            .filter { it.isNotEmpty() }
            .joinToString(" ")
            .ifBlank { holderName.trim() }

    val hasLinkedContact: Boolean
        get() = contactName.isNotBlank() || contactUri.isNotBlank() || contactPhone.isNotBlank()
}

object PasswordStrengthAnalyzer {

    private val commonWeakTokens = setOf(
        "123456", "12345678", "123456789", "1234567890", "password", "qwerty",
        "azerty", "admin", "welcome", "letmein", "000000", "111111", "abc123",
        "iloveyou", "pass123", "123123", "654321", "0000", "1111", "1234"
    )

    fun analyze(
        password: String,
        username: String = "",
        label: String = "",
        duplicateCount: Int = 1
    ): PasswordAnalysis {
        if (password.isBlank()) {
            return PasswordAnalysis(
                score = 0,
                level = PasswordStrengthLevel.WEAK,
                isReused = false,
                tipsAr = listOf("أدخل كلمة السر لتقييم قوتها"),
                tipsEn = listOf("Enter a password to evaluate strength")
            )
        }

        var score = 0
        val tipsAr = mutableListOf<String>()
        val tipsEn = mutableListOf<String>()

        val len = password.length
        score += when {
            len >= 16 -> 35
            len >= 12 -> 28
            len >= 8 -> 18
            len >= 6 -> 8
            else -> 2
        }
        if (len < 8) {
            tipsAr.add("يفضّل أن لا تقل عن 8 أحرف (الأفضل 12+)")
            tipsEn.add("Use at least 8 characters (12+ recommended)")
        }

        val hasLower = password.any { it.isLowerCase() }
        val hasUpper = password.any { it.isUpperCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSymbol = password.any { !it.isLetterOrDigit() }

        if (hasLower) score += 12 else {
            tipsAr.add("أضف حروفاً صغيرة (a-z)")
            tipsEn.add("Add lowercase letters (a-z)")
        }
        if (hasUpper) score += 15 else {
            tipsAr.add("أضف حروفاً كبيرة (A-Z)")
            tipsEn.add("Add uppercase letters (A-Z)")
        }
        if (hasDigit) score += 13 else {
            tipsAr.add("أضف أرقاماً (0-9)")
            tipsEn.add("Add numbers (0-9)")
        }
        if (hasSymbol) score += 18 else {
            tipsAr.add("أضف رموزاً خاصة (!@#\$%)")
            tipsEn.add("Add special symbols (!@#\$%)")
        }

        val uniqueRatio = password.toSet().size.toDouble() / len.toDouble()
        if (uniqueRatio >= 0.7 && len >= 8) {
            score += 7
        }

        val lower = password.lowercase()
        if (commonWeakTokens.any { lower == it || (it.length >= 5 && lower.contains(it)) }) {
            score -= 35
            tipsAr.add("تحتوي على نمط شائع يسهل تخمينه")
            tipsEn.add("Contains a common easily-guessed pattern")
        }

        if (hasSequentialRuns(lower)) {
            score -= 18
            tipsAr.add("تجنب التسلسل المتتالي مثل 1234 أو abcd")
            tipsEn.add("Avoid sequential characters like 1234 or abcd")
        }

        if (hasRepeatedRun(password)) {
            score -= 15
            tipsAr.add("تجنب تكرار نفس الحرف أو الرقم بشكل متتالي")
            tipsEn.add("Avoid repeating the same character consecutively")
        }

        val userToken = username.trim().lowercase().substringBefore("@")
        val labelToken = label.trim().lowercase()
        if ((userToken.length >= 3 && lower.contains(userToken)) ||
            (labelToken.length >= 3 && lower.contains(labelToken))
        ) {
            score -= 22
            tipsAr.add("لا تستخدم اسم المستخدم أو اسم الخدمة داخل كلمة السر")
            tipsEn.add("Do not include your username or service name")
        }

        val isReused = duplicateCount > 1
        if (isReused) {
            score -= 20
            tipsAr.add("كلمة السر مستخدمة في $duplicateCount حسابات — يوصى بتغييرها")
            tipsEn.add("Reused across $duplicateCount accounts — use a unique password")
        }

        val clampedScore = score.coerceIn(0, 100)
        val level = when {
            clampedScore < 40 -> PasswordStrengthLevel.WEAK
            clampedScore < 65 -> PasswordStrengthLevel.FAIR
            clampedScore < 82 -> PasswordStrengthLevel.GOOD
            else -> PasswordStrengthLevel.STRONG
        }

        if (tipsAr.isEmpty()) {
            tipsAr.add("كلمة سر ممتازة ومعقدة يصعب اختراقها")
            tipsEn.add("Strong, unique password with high entropy")
        }

        return PasswordAnalysis(
            score = clampedScore,
            level = level,
            isReused = isReused,
            tipsAr = tipsAr,
            tipsEn = tipsEn
        )
    }

    fun generateStrongPassword(length: Int = 14): String {
        val lowers = "abcdefghijkmnopqrstuvwxyz"
        val uppers = "ABCDEFGHJKLMNPQRSTUVWXYZ"
        val digits = "23456789"
        val symbols = "@#\$%&*!?+-"
        val all = lowers + uppers + digits + symbols
        val rng = SecureRandom()

        val chars = mutableListOf(
            lowers[rng.nextInt(lowers.length)],
            uppers[rng.nextInt(uppers.length)],
            digits[rng.nextInt(digits.length)],
            symbols[rng.nextInt(symbols.length)]
        )
        while (chars.size < length) {
            chars.add(all[rng.nextInt(all.length)])
        }
        chars.shuffle(rng)
        return chars.joinToString("")
    }

    private fun hasSequentialRuns(s: String): Boolean {
        if (s.length < 4) return false
        var asc = 1
        var desc = 1
        for (i in 1 until s.length) {
            val diff = s[i].code - s[i - 1].code
            asc = if (diff == 1) asc + 1 else 1
            desc = if (diff == -1) desc + 1 else 1
            if (asc >= 4 || desc >= 4) return true
        }
        return false
    }

    private fun hasRepeatedRun(s: String): Boolean {
        if (s.length < 3) return false
        var run = 1
        for (i in 1 until s.length) {
            run = if (s[i] == s[i - 1]) run + 1 else 1
            if (run >= 3) return true
        }
        return false
    }
}

object HubVaultCodec {

    const val MAX_CCP_PROFILES = 1000

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val vaultKeysAdapter by lazy {
        val type = Types.newParameterizedType(List::class.java, VaultKeyEntry::class.java)
        moshi.adapter<List<VaultKeyEntry>>(type)
    }

    private val ccpAccountsAdapter by lazy {
        val type = Types.newParameterizedType(List::class.java, CcpAccountEntry::class.java)
        moshi.adapter<List<CcpAccountEntry>>(type)
    }

    fun parseVaultKeys(json: String): List<VaultKeyEntry> {
        if (json.isBlank()) return emptyList()
        return try {
            vaultKeysAdapter.fromJson(json)
                ?.filter { it.label.isNotBlank() && it.secretValue.isNotBlank() }
                ?.sortedByDescending { it.createdAt }
                ?: emptyList()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun encodeVaultKeys(list: List<VaultKeyEntry>): String {
        return try {
            vaultKeysAdapter.toJson(list)
        } catch (_: Throwable) {
            "[]"
        }
    }

    fun parseCcpAccounts(json: String): List<CcpAccountEntry> {
        if (json.isBlank()) return emptyList()
        return try {
            val rawList = ccpAccountsAdapter.fromJson(json) ?: emptyList()
            val normalized = rawList.mapNotNull { entry ->
                val cleanCcp = entry.ccpNumber.filter { it.isDigit() }
                val cleanHolder = entry.holderName.trim()
                val rawFirst = entry.firstName.trim()
                val rawLast = entry.lastName.trim()

                val (resolvedFirst, resolvedLast) = if (rawFirst.isNotEmpty() || rawLast.isNotEmpty()) {
                    rawFirst to rawLast
                } else {
                    splitFullName(cleanHolder)
                }

                val resolvedHolder = listOf(resolvedFirst, resolvedLast)
                    .filter { it.isNotEmpty() }
                    .joinToString(" ")
                    .ifBlank { cleanHolder }

                if (resolvedHolder.isEmpty() || cleanCcp.isEmpty()) {
                    null
                } else {
                    entry.copy(
                        firstName = resolvedFirst,
                        lastName = resolvedLast,
                        holderName = resolvedHolder,
                        ccpNumber = cleanCcp,
                        ccpKey = entry.ccpKey.filter { it.isDigit() }.take(2),
                        ripFull = entry.ripFull.ifBlank { computeAlgeriePosteRip(cleanCcp) },
                        agencyNote = entry.agencyNote.trim(),
                        contactName = entry.contactName.trim(),
                        contactUri = entry.contactUri.trim(),
                        contactPhone = entry.contactPhone.trim()
                    )
                }
            }
            sortCcpAccounts(normalized)
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun encodeCcpAccounts(list: List<CcpAccountEntry>): String {
        return try {
            ccpAccountsAdapter.toJson(sortCcpAccounts(list))
        } catch (_: Throwable) {
            "[]"
        }
    }

    fun sortCcpAccounts(list: List<CcpAccountEntry>): List<CcpAccountEntry> {
        return list.sortedWith(
            compareByDescending<CcpAccountEntry> { it.isPinned }
                .thenByDescending { if (it.isPinned) it.pinnedAt else 0L }
                .thenByDescending { it.createdAt }
        )
    }

    fun splitFullName(fullName: String): Pair<String, String> {
        val parts = fullName.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        return when {
            parts.isEmpty() -> "" to ""
            parts.size == 1 -> parts[0] to ""
            else -> parts.first() to parts.drop(1).joinToString(" ")
        }
    }

    /**
     * Computes the official 20-digit Algérie Poste RIP using Modulo 97:
     * Base18 = "00799999" + ccpDigits.padStart(10, '0')
     * RIP Key = 97 - ((BigInteger(Base18) * 100) % 97)
     */
    fun computeAlgeriePosteRip(rawCcpDigits: String): String {
        val digits = rawCcpDigits.filter { it.isDigit() }
        if (digits.isEmpty()) return ""
        val paddedCcp = digits.takeLast(10).padStart(10, '0')
        val base18 = "00799999$paddedCcp"
        return try {
            val nTimes100 = BigInteger(base18).multiply(BigInteger.valueOf(100L))
            val rem = nTimes100.mod(BigInteger.valueOf(97L)).toInt()
            val ripKey = 97 - rem
            val keyStr = ripKey.toString().padStart(2, '0')
            "$base18$keyStr"
        } catch (_: Throwable) {
            ""
        }
    }

    fun formatRipGrouped(rip: String): String {
        val clean = rip.filter { it.isDigit() }
        if (clean.length != 20) return rip
        return "${clean.substring(0, 3)} ${clean.substring(3, 8)} ${clean.substring(8, 18)} ${clean.substring(18, 20)}"
    }
}
