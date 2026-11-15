package com.example.data

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
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
    val createdAt: Long = System.currentTimeMillis(),
    val appPresetId: String = "custom"
)

enum class VaultAppMotif {
    LIQUID_AURORA,
    POSTAL_ARCS,
    CRYSTAL_FACETS,
    CRYPTO_HEX_GRID,
    CRYPTO_DIAMONDS,
    AURORA_ORBS,
    CAMERA_LENS_RINGS,
    SOCIAL_WAVES,
    SOUND_WAVES,
    CHAT_BUBBLES,
    PAPER_PLANES,
    SPARK_STARS,
    DIAGONAL_SLASHES,
    CINEMA_STRIPES,
    SOUND_RINGS,
    ORBIT_NODES,
    CODE_NODES,
    CARD_RIBBONS
}

data class VaultAppPreset(
    val id: String,
    val nameEn: String,
    val nameAr: String,
    val monogram: String,
    val defaultCategory: String,
    val primaryColorHex: Long,
    val secondaryColorHex: Long,
    val accentColorHex: Long,
    val surfaceTintDarkHex: Long,
    val surfaceTintLightHex: Long,
    val motif: VaultAppMotif,
    val keywords: List<String> = emptyList()
)

object VaultAppPresets {
    val all: List<VaultAppPreset> = listOf(
        VaultAppPreset(
            id = "baridimob",
            nameEn = "BaridiMob",
            nameAr = "بريدي موب",
            monogram = "BM",
            defaultCategory = "BANK",
            primaryColorHex = 0xFFF59E0B,
            secondaryColorHex = 0xFF1D4ED8,
            accentColorHex = 0xFF34D399,
            surfaceTintDarkHex = 0xFF0E1A33,
            surfaceTintLightHex = 0xFFFFFBEB,
            motif = VaultAppMotif.POSTAL_ARCS,
            keywords = listOf("baridi", "baridimob", "بريدي")
        ),
        VaultAppPreset(
            id = "algerie_poste",
            nameEn = "Algérie Poste / ECCP",
            nameAr = "بريد الجزائر / ECCP",
            monogram = "CCP",
            defaultCategory = "BANK",
            primaryColorHex = 0xFFEAB308,
            secondaryColorHex = 0xFF2563EB,
            accentColorHex = 0xFFFDE047,
            surfaceTintDarkHex = 0xFF0C1930,
            surfaceTintLightHex = 0xFFFEFCE8,
            motif = VaultAppMotif.POSTAL_ARCS,
            keywords = listOf("eccp", "ccp", "poste", "edahabia", "بريد", "الذهبية")
        ),
        VaultAppPreset(
            id = "redotpay",
            nameEn = "RedotPay",
            nameAr = "RedotPay",
            monogram = "RP",
            defaultCategory = "BANK",
            primaryColorHex = 0xFFEF4444,
            secondaryColorHex = 0xFF991B1B,
            accentColorHex = 0xFFFCA5A5,
            surfaceTintDarkHex = 0xFF210B10,
            surfaceTintLightHex = 0xFFFEF2F2,
            motif = VaultAppMotif.CRYSTAL_FACETS,
            keywords = listOf("redot", "redotpay")
        ),
        VaultAppPreset(
            id = "bybit",
            nameEn = "Bybit",
            nameAr = "Bybit",
            monogram = "BY",
            defaultCategory = "BANK",
            primaryColorHex = 0xFFF59E0B,
            secondaryColorHex = 0xFF334155,
            accentColorHex = 0xFFFBBF24,
            surfaceTintDarkHex = 0xFF17140B,
            surfaceTintLightHex = 0xFFFFFBEB,
            motif = VaultAppMotif.CRYPTO_HEX_GRID,
            keywords = listOf("bybit")
        ),
        VaultAppPreset(
            id = "binance",
            nameEn = "Binance",
            nameAr = "Binance",
            monogram = "BNB",
            defaultCategory = "BANK",
            primaryColorHex = 0xFFFACC15,
            secondaryColorHex = 0xFFCA8A04,
            accentColorHex = 0xFFFEF08A,
            surfaceTintDarkHex = 0xFF1A1708,
            surfaceTintLightHex = 0xFFFEFCE8,
            motif = VaultAppMotif.CRYPTO_DIAMONDS,
            keywords = listOf("binance", "bnb")
        ),
        VaultAppPreset(
            id = "google",
            nameEn = "Google / Gmail",
            nameAr = "جوجل / Gmail",
            monogram = "G",
            defaultCategory = "EMAIL",
            primaryColorHex = 0xFF4285F4,
            secondaryColorHex = 0xFF34A853,
            accentColorHex = 0xFFEA4335,
            surfaceTintDarkHex = 0xFF0E172A,
            surfaceTintLightHex = 0xFFEFF6FF,
            motif = VaultAppMotif.AURORA_ORBS,
            keywords = listOf("google", "gmail", "youtube", "جوجل", "جيميل")
        ),
        VaultAppPreset(
            id = "instagram",
            nameEn = "Instagram",
            nameAr = "إنستغرام",
            monogram = "IG",
            defaultCategory = "APP",
            primaryColorHex = 0xFFE1306C,
            secondaryColorHex = 0xFF833AB4,
            accentColorHex = 0xFFF77737,
            surfaceTintDarkHex = 0xFF210D22,
            surfaceTintLightHex = 0xFFFDF2F8,
            motif = VaultAppMotif.CAMERA_LENS_RINGS,
            keywords = listOf("instagram", "insta", "انستا", "انستغرام", "إنستغرام")
        ),
        VaultAppPreset(
            id = "facebook",
            nameEn = "Facebook",
            nameAr = "فيسبوك",
            monogram = "f",
            defaultCategory = "APP",
            primaryColorHex = 0xFF1877F2,
            secondaryColorHex = 0xFF0EA5E9,
            accentColorHex = 0xFF93C5FD,
            surfaceTintDarkHex = 0xFF0B1830,
            surfaceTintLightHex = 0xFFEFF6FF,
            motif = VaultAppMotif.SOCIAL_WAVES,
            keywords = listOf("facebook", "fb", "meta", "فيسبوك")
        ),
        VaultAppPreset(
            id = "tiktok",
            nameEn = "TikTok",
            nameAr = "تيك توك",
            monogram = "TT",
            defaultCategory = "APP",
            primaryColorHex = 0xFF22D3EE,
            secondaryColorHex = 0xFFFE2C55,
            accentColorHex = 0xFF67E8F9,
            surfaceTintDarkHex = 0xFF0F131E,
            surfaceTintLightHex = 0xFFECFEFF,
            motif = VaultAppMotif.SOUND_WAVES,
            keywords = listOf("tiktok", "تيك توك", "تيكتوك")
        ),
        VaultAppPreset(
            id = "whatsapp",
            nameEn = "WhatsApp",
            nameAr = "واتساب",
            monogram = "WA",
            defaultCategory = "APP",
            primaryColorHex = 0xFF25D366,
            secondaryColorHex = 0xFF075E54,
            accentColorHex = 0xFF86EFAC,
            surfaceTintDarkHex = 0xFF091E18,
            surfaceTintLightHex = 0xFFF0FDF4,
            motif = VaultAppMotif.CHAT_BUBBLES,
            keywords = listOf("whatsapp", "واتساب")
        ),
        VaultAppPreset(
            id = "telegram",
            nameEn = "Telegram",
            nameAr = "تيليجرام",
            monogram = "TG",
            defaultCategory = "APP",
            primaryColorHex = 0xFF2AABEE,
            secondaryColorHex = 0xFF0284C7,
            accentColorHex = 0xFFBAE6FD,
            surfaceTintDarkHex = 0xFF081B2C,
            surfaceTintLightHex = 0xFFF0F9FF,
            motif = VaultAppMotif.PAPER_PLANES,
            keywords = listOf("telegram", "تلغرام", "تيليجرام")
        ),
        VaultAppPreset(
            id = "snapchat",
            nameEn = "Snapchat",
            nameAr = "سناب شات",
            monogram = "SC",
            defaultCategory = "APP",
            primaryColorHex = 0xFFFACC15,
            secondaryColorHex = 0xFFF59E0B,
            accentColorHex = 0xFFFEF08A,
            surfaceTintDarkHex = 0xFF1F1B06,
            surfaceTintLightHex = 0xFFFEFCE8,
            motif = VaultAppMotif.SPARK_STARS,
            keywords = listOf("snapchat", "snap", "سناب")
        ),
        VaultAppPreset(
            id = "x_twitter",
            nameEn = "X (Twitter)",
            nameAr = "إكس (تويتر)",
            monogram = "X",
            defaultCategory = "APP",
            primaryColorHex = 0xFFE2E8F0,
            secondaryColorHex = 0xFF475569,
            accentColorHex = 0xFF38BDF8,
            surfaceTintDarkHex = 0xFF0D1117,
            surfaceTintLightHex = 0xFFF8FAFC,
            motif = VaultAppMotif.DIAGONAL_SLASHES,
            keywords = listOf("twitter", "x.com", "تويتر")
        ),
        VaultAppPreset(
            id = "netflix",
            nameEn = "Netflix",
            nameAr = "نتفليكس",
            monogram = "N",
            defaultCategory = "APP",
            primaryColorHex = 0xFFE50914,
            secondaryColorHex = 0xFF7F1D1D,
            accentColorHex = 0xFFFCA5A5,
            surfaceTintDarkHex = 0xFF1E080A,
            surfaceTintLightHex = 0xFFFEF2F2,
            motif = VaultAppMotif.CINEMA_STRIPES,
            keywords = listOf("netflix", "نتفليكس")
        ),
        VaultAppPreset(
            id = "spotify",
            nameEn = "Spotify",
            nameAr = "سبوتيفاي",
            monogram = "SP",
            defaultCategory = "APP",
            primaryColorHex = 0xFF1DB954,
            secondaryColorHex = 0xFF15803D,
            accentColorHex = 0xFF86EFAC,
            surfaceTintDarkHex = 0xFF091B10,
            surfaceTintLightHex = 0xFFF0FDF4,
            motif = VaultAppMotif.SOUND_RINGS,
            keywords = listOf("spotify", "سبوتيفاي")
        ),
        VaultAppPreset(
            id = "discord",
            nameEn = "Discord",
            nameAr = "ديسكورد",
            monogram = "DC",
            defaultCategory = "APP",
            primaryColorHex = 0xFF5865F2,
            secondaryColorHex = 0xFF4338CA,
            accentColorHex = 0xFFA5B4FC,
            surfaceTintDarkHex = 0xFF12142B,
            surfaceTintLightHex = 0xFFEEF2FF,
            motif = VaultAppMotif.ORBIT_NODES,
            keywords = listOf("discord", "ديسكورد")
        ),
        VaultAppPreset(
            id = "github",
            nameEn = "GitHub",
            nameAr = "GitHub",
            monogram = "GH",
            defaultCategory = "APP",
            primaryColorHex = 0xFF8B5CF6,
            secondaryColorHex = 0xFF0EA5E9,
            accentColorHex = 0xFFC4B5FD,
            surfaceTintDarkHex = 0xFF121024,
            surfaceTintLightHex = 0xFFF5F3FF,
            motif = VaultAppMotif.CODE_NODES,
            keywords = listOf("github", "git")
        ),
        VaultAppPreset(
            id = "paypal",
            nameEn = "PayPal",
            nameAr = "باي بال",
            monogram = "PP",
            defaultCategory = "BANK",
            primaryColorHex = 0xFF0070BA,
            secondaryColorHex = 0xFF003087,
            accentColorHex = 0xFF38BDF8,
            surfaceTintDarkHex = 0xFF09162D,
            surfaceTintLightHex = 0xFFEFF6FF,
            motif = VaultAppMotif.CARD_RIBBONS,
            keywords = listOf("paypal", "بايبال", "باي بال")
        ),
        VaultAppPreset(
            id = "custom",
            nameEn = "Custom App / Other",
            nameAr = "تطبيق مخصص / آخر",
            monogram = "★",
            defaultCategory = "APP",
            primaryColorHex = 0xFF38BDF8,
            secondaryColorHex = 0xFF6366F1,
            accentColorHex = 0xFF34D399,
            surfaceTintDarkHex = 0xFF0F172A,
            surfaceTintLightHex = 0xFFF8FAFC,
            motif = VaultAppMotif.LIQUID_AURORA,
            keywords = emptyList()
        )
    )

    fun findById(id: String?): VaultAppPreset {
        val clean = id?.trim()?.lowercase().orEmpty()
        return all.firstOrNull { it.id == clean } ?: all.last()
    }

    fun resolveForEntry(entry: VaultKeyEntry): VaultAppPreset {
        val explicit = entry.appPresetId.trim().lowercase()
        if (explicit.isNotEmpty() && explicit != "custom") {
            all.firstOrNull { it.id == explicit }?.let { return it }
        }
        return detectFromLabel(entry.label) ?: findById("custom")
    }

    fun resolveForCcpEntry(entry: CcpAccountEntry): VaultAppPreset {
        val explicit = entry.appPresetId.trim().lowercase()
        if (explicit.isNotEmpty() && explicit != "custom") {
            all.firstOrNull { it.id == explicit }?.let { return it }
        }
        if (explicit == "custom") {
            return detectFromLabel("${entry.displayFullName} ${entry.agencyNote}") ?: findById("custom")
        }
        return detectFromLabel("${entry.displayFullName} ${entry.agencyNote}") ?: findById("algerie_poste")
    }

    fun detectFromLabel(label: String): VaultAppPreset? {
        val norm = label.trim().lowercase()
        if (norm.isEmpty()) return null
        return all.firstOrNull { preset ->
            preset.id != "custom" && (
                norm.contains(preset.id) ||
                    norm.contains(preset.nameEn.lowercase()) ||
                    preset.keywords.any { kw -> norm.contains(kw) }
                )
        }
    }
}

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
    val createdAt: Long = System.currentTimeMillis(),
    val appPresetId: String = "algerie_poste"
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

                val cleanRip = entry.ripFull.filter { it.isDigit() }.take(20)
                if (resolvedHolder.isEmpty() || (cleanCcp.isEmpty() && cleanRip.isEmpty())) {
                    null
                } else {
                    entry.copy(
                        firstName = resolvedFirst,
                        lastName = resolvedLast,
                        holderName = resolvedHolder,
                        ccpNumber = cleanCcp,
                        ccpKey = entry.ccpKey.filter { it.isDigit() }.take(2),
                        ripFull = cleanRip,
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

    fun formatRipGrouped(rip: String): String {
        val clean = rip.filter { it.isDigit() }
        if (clean.length != 20) return rip
        return "${clean.substring(0, 3)} ${clean.substring(3, 8)} ${clean.substring(8, 18)} ${clean.substring(18, 20)}"
    }
}
