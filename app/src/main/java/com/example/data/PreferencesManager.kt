package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.security.CryptoIssue
import com.example.security.CryptoIssueType
import com.example.security.DecryptOutcome
import com.example.security.VaultCryptoManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserSettings(
    val salaryAmount: Double = 85000.0,
    val salaryPayday: Int = 25,
    val salaryEmployer: String = "Enterprise Algiers",
    val salaryAutoCredit: Boolean = true,
    val lastCreditedMonthYear: String = "",
    val patternLockEnabled: Boolean = false,
    val patternLockHash: String = "",
    val biometricEnabled: Boolean = false,
    val language: String = "en", // "en" or "ar"
    val themeMode: String = "DARK", // "DARK", "LIGHT", or "SYSTEM"
    val designStyle: String = "LIQUID_GLASS", // "LIQUID_GLASS" or "NEUMORPHISM"
    val googleDriveConnected: Boolean = true,
    val lastBackupTimestamp: Long = 0L,
    val stealthMode: Boolean = false,
    val vaultLockedAmount: Double = 0.0,
    val savingsWalletBalance: Double = 0.0,
    val savingsGoalAmount: Double = 0.0,
    val savingsHistoryJson: String = "[]",
    val includeAssetsInSavingsTotal: Boolean = false,
    val goldAssetHoldingsJson: String = "[]",
    val classicCardBankStyle: String = "EDAHABIA",
    val classicCardHolderName: String = "CHARFEDDINE KA",
    val classicCardNumber: String = "",
    val classicCardExpiry: String = "11/29",
    val classicCardCvc: String = "",
    val breathingGlowEnabled: Boolean = true,
    val screenshotProtectionEnabled: Boolean = false,
    val vaultKeysJson: String = "[]",
    val ccpAccountsJson: String = "[]",
    val visaCardProvider: String = "DEFAULT", // "DEFAULT", "REDOTPAY", "BYBIT", "BINANCE"
    val visaCardHolderName: String = "CHARFEDDINE KA",
    val visaCardNumber: String = "",
    val visaCardExpiry: String = "08/28",
    val visaCardCvc: String = "",
    val thirdCardEnabled: Boolean = false,
    val thirdCardProvider: String = "BINANCE", // "DEFAULT", "REDOTPAY", "BYBIT", "BINANCE"
    val thirdCardHolderName: String = "CHARFEDDINE KA",
    val thirdCardNumber: String = "",
    val thirdCardExpiry: String = "08/28",
    val thirdCardCvc: String = "",
    val binanceHoldingsJson: String = "[]",
    val binancePricesJson: String = "[]",
    val binanceLastPriceSyncMillis: Long = 0L,
    val squareMarketSnapshotJson: String = "",
    val squareLastPriceSyncMillis: Long = 0L,
    val squareDailyBaselineJson: String = "",
    val squarePinnedIdsJson: String = "[\"XAU_SPOT\",\"XAU_24K\",\"XAU_18K\",\"EUR\",\"USD\"]",
    val squareSyncIntervalMinutes: Int = 60,
    val baridiMobSmsSyncEnabled: Boolean = true,
    val pendingSmsSuggestionsJson: String = "[]",
    val processedSmsKeysJson: String = "[]",
    val smsSyncBaselineTimestampMillis: Long = 0L,
    val latestSmsBalanceAmount: Double = -1.0,
    val latestSmsBalanceUpdatedAt: Long = 0L,
    val latestSmsBalanceRawSnippet: String = "",
    val smsBalanceHistoryJson: String = "[]",
    val detectedSmsTaxesJson: String = "[]",
    val hasCompletedPermissionsOnboarding: Boolean = false
)

class PreferencesManager(
    context: Context,
    private val crypto: VaultCryptoManager = VaultCryptoManager.getInstance(context)
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("salerypelse_prefs", Context.MODE_PRIVATE)

    private val _cryptoIssue = MutableStateFlow<CryptoIssue?>(null)
    val cryptoIssue: StateFlow<CryptoIssue?> = _cryptoIssue.asStateFlow()

    @Volatile
    private var unreadableKeysSnapshot: Map<String, CryptoIssueType> = emptyMap()

    @Volatile
    private var isSelfUpdating = false

    private val _settingsFlow = MutableStateFlow(initializeAndMigrate())
    val settingsFlow: StateFlow<UserSettings> = _settingsFlow.asStateFlow()

    private val prefChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        if (!isSelfUpdating) {
            val loaded = loadSettingsInternal()
            unreadableKeysSnapshot = loaded.unreadableKeys
            _cryptoIssue.value = buildCryptoIssue(loaded.unreadableKeys)
            _settingsFlow.value = loaded.settings
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefChangeListener)
    }

    private data class LoadResult(
        val settings: UserSettings,
        val unreadableKeys: Map<String, CryptoIssueType>,
        val legacyPlaintextValues: Map<String, String>
    )

    private fun aadFor(key: String): String = "pref:$key"

    private fun initializeAndMigrate(): UserSettings {
        val loaded = loadSettingsInternal()
        unreadableKeysSnapshot = loaded.unreadableKeys
        if (loaded.legacyPlaintextValues.isNotEmpty()) {
            val writeIssues = mutableMapOf<String, CryptoIssueType>()
            isSelfUpdating = true
            try {
                val editor = prefs.edit()
                var wroteAny = false
                for ((key, plainValue) in loaded.legacyPlaintextValues) {
                    val encrypted = crypto.encrypt(plainValue, aadFor(key))
                    if (encrypted != null) {
                        editor.putString(key, encrypted)
                        wroteAny = true
                    } else {
                        writeIssues[key] = CryptoIssueType.KEYSTORE_UNAVAILABLE
                    }
                }
                if (wroteAny) {
                    editor.apply()
                }
            } finally {
                isSelfUpdating = false
            }
            val mergedIssues = loaded.unreadableKeys + writeIssues
            unreadableKeysSnapshot = mergedIssues
            _cryptoIssue.value = buildCryptoIssue(mergedIssues)
        } else {
            _cryptoIssue.value = buildCryptoIssue(loaded.unreadableKeys)
        }
        return loaded.settings
    }

    private fun buildCryptoIssue(unreadable: Map<String, CryptoIssueType>): CryptoIssue? {
        if (unreadable.isEmpty()) return null
        val priorityType = when {
            unreadable.values.any { it == CryptoIssueType.KEY_INVALIDATED } -> CryptoIssueType.KEY_INVALIDATED
            unreadable.values.any { it == CryptoIssueType.KEYSTORE_UNAVAILABLE } -> CryptoIssueType.KEYSTORE_UNAVAILABLE
            unreadable.values.any { it == CryptoIssueType.DECRYPTION_FAILED_OR_TAMPERED } -> CryptoIssueType.DECRYPTION_FAILED_OR_TAMPERED
            else -> CryptoIssueType.CORRUPTED_DATA
        }
        return CryptoIssue(
            type = priorityType,
            affectedScopes = unreadable.keys.toSet()
        )
    }

    private fun safeInt(key: String, default: Int): Int {
        return try {
            when (val raw = prefs.all[key]) {
                is Int -> raw
                is Number -> raw.toInt()
                is String -> raw.toIntOrNull() ?: default
                else -> default
            }
        } catch (_: Throwable) {
            default
        }
    }

    private fun safeLong(key: String, default: Long): Long {
        return try {
            when (val raw = prefs.all[key]) {
                is Long -> raw
                is Number -> raw.toLong()
                is String -> raw.toLongOrNull() ?: default
                else -> default
            }
        } catch (_: Throwable) {
            default
        }
    }

    private fun safeBoolean(key: String, default: Boolean): Boolean {
        return try {
            when (val raw = prefs.all[key]) {
                is Boolean -> raw
                is String -> raw.toBooleanStrictOrNull() ?: default
                else -> default
            }
        } catch (_: Throwable) {
            default
        }
    }

    private fun safeString(key: String, default: String): String {
        return try {
            when (val raw = prefs.all[key]) {
                is String -> raw
                null -> default
                else -> raw.toString()
            }
        } catch (_: Throwable) {
            default
        }
    }

    private fun readEncryptedString(
        key: String,
        default: String,
        unreadableOut: MutableMap<String, CryptoIssueType>,
        legacyOut: MutableMap<String, String>
    ): String {
        val raw = try {
            prefs.all[key]
        } catch (_: Throwable) {
            null
        } ?: return default

        val rawStr = when (raw) {
            is String -> raw
            else -> raw.toString()
        }

        return when (val outcome = crypto.decrypt(rawStr, aadFor(key))) {
            is DecryptOutcome.Success -> {
                if (outcome.isLegacyPlaintext) {
                    legacyOut[key] = outcome.plaintext
                }
                outcome.plaintext
            }
            is DecryptOutcome.Failure -> {
                unreadableOut[key] = outcome.issueType
                default
            }
        }
    }

    private fun readEncryptedDouble(
        key: String,
        default: Double,
        unreadableOut: MutableMap<String, CryptoIssueType>,
        legacyOut: MutableMap<String, String>
    ): Double {
        val raw = try {
            prefs.all[key]
        } catch (_: Throwable) {
            null
        } ?: return default

        if (raw is Number) {
            val dbl = raw.toDouble()
            legacyOut[key] = dbl.toString()
            return dbl
        }

        val rawStr = raw.toString()
        return when (val outcome = crypto.decrypt(rawStr, aadFor(key))) {
            is DecryptOutcome.Success -> {
                val parsed = outcome.plaintext.toDoubleOrNull()
                if (parsed == null) {
                    if (crypto.isEncryptedEnvelope(rawStr)) {
                        unreadableOut[key] = CryptoIssueType.CORRUPTED_DATA
                    }
                    default
                } else {
                    if (outcome.isLegacyPlaintext) {
                        legacyOut[key] = parsed.toString()
                    }
                    parsed
                }
            }
            is DecryptOutcome.Failure -> {
                unreadableOut[key] = outcome.issueType
                default
            }
        }
    }

    private fun loadSettingsInternal(): LoadResult {
        val unreadable = mutableMapOf<String, CryptoIssueType>()
        val legacy = mutableMapOf<String, String>()

        val vaultFallback = readEncryptedDouble("vault_locked_amount", 0.0, unreadable, legacy)
        val savedSavings = readEncryptedDouble("savings_wallet_balance", vaultFallback, unreadable, legacy)

        val settings = UserSettings(
            salaryAmount = readEncryptedDouble("salary_amount", 85000.0, unreadable, legacy),
            salaryPayday = safeInt("salary_payday", 25),
            salaryEmployer = readEncryptedString("salary_employer", "Enterprise Algiers", unreadable, legacy),
            salaryAutoCredit = safeBoolean("salary_auto_credit", true),
            lastCreditedMonthYear = safeString("last_credited_month", ""),
            patternLockEnabled = safeBoolean("pattern_lock_enabled", false),
            patternLockHash = readEncryptedString("pattern_lock_hash", "", unreadable, legacy),
            biometricEnabled = safeBoolean("biometric_enabled", false),
            language = safeString("app_language", "en"),
            themeMode = safeString("theme_mode", "DARK"),
            designStyle = safeString("design_style", "LIQUID_GLASS"),
            googleDriveConnected = safeBoolean("gdrive_connected", true),
            lastBackupTimestamp = safeLong("last_backup_time", 0L),
            stealthMode = safeBoolean("stealth_mode", false),
            vaultLockedAmount = savedSavings,
            savingsWalletBalance = savedSavings,
            savingsGoalAmount = readEncryptedDouble("savings_goal_amount", 0.0, unreadable, legacy),
            savingsHistoryJson = readEncryptedString("savings_history_json", "[]", unreadable, legacy),
            includeAssetsInSavingsTotal = safeBoolean("include_assets_in_savings_total", false),
            goldAssetHoldingsJson = readEncryptedString("gold_asset_holdings_json", "[]", unreadable, legacy),
            classicCardBankStyle = safeString("classic_card_bank_style", "EDAHABIA"),
            classicCardHolderName = readEncryptedString("classic_card_holder", "CHARFEDDINE KA", unreadable, legacy),
            classicCardNumber = readEncryptedString("classic_card_number", "", unreadable, legacy),
            classicCardExpiry = readEncryptedString("classic_card_expiry", "11/29", unreadable, legacy),
            classicCardCvc = readEncryptedString("classic_card_cvc", "", unreadable, legacy),
            breathingGlowEnabled = safeBoolean("breathing_glow_enabled", true),
            screenshotProtectionEnabled = safeBoolean("screenshot_protection_enabled", false),
            vaultKeysJson = readEncryptedString("vault_keys_json", "[]", unreadable, legacy),
            ccpAccountsJson = readEncryptedString("ccp_accounts_json", "[]", unreadable, legacy),
            visaCardProvider = safeString("visa_card_provider", "DEFAULT"),
            visaCardHolderName = readEncryptedString("visa_card_holder", "CHARFEDDINE KA", unreadable, legacy),
            visaCardNumber = readEncryptedString("visa_card_number", "", unreadable, legacy),
            visaCardExpiry = readEncryptedString("visa_card_expiry", "08/28", unreadable, legacy),
            visaCardCvc = readEncryptedString("visa_card_cvc", "", unreadable, legacy),
            thirdCardEnabled = safeBoolean("third_card_enabled", false),
            thirdCardProvider = safeString("third_card_provider", "BINANCE"),
            thirdCardHolderName = readEncryptedString("third_card_holder", "CHARFEDDINE KA", unreadable, legacy),
            thirdCardNumber = readEncryptedString("third_card_number", "", unreadable, legacy),
            thirdCardExpiry = readEncryptedString("third_card_expiry", "08/28", unreadable, legacy),
            thirdCardCvc = readEncryptedString("third_card_cvc", "", unreadable, legacy),
            binanceHoldingsJson = readEncryptedString("binance_holdings_json", "[]", unreadable, legacy),
            binancePricesJson = safeString("binance_prices_json", "[]"),
            binanceLastPriceSyncMillis = safeLong("binance_last_price_sync_millis", 0L),
            squareMarketSnapshotJson = safeString("square_market_snapshot_json", ""),
            squareLastPriceSyncMillis = safeLong("square_last_price_sync_millis", 0L),
            squareDailyBaselineJson = safeString("square_daily_baseline_json", ""),
            squarePinnedIdsJson = safeString("square_pinned_ids_json", "[\"XAU_SPOT\",\"XAU_24K\",\"XAU_18K\",\"EUR\",\"USD\"]"),
            squareSyncIntervalMinutes = safeInt("square_sync_interval_minutes", 60).coerceIn(0, 1440),
            baridiMobSmsSyncEnabled = safeBoolean("baridimob_sms_sync_enabled", true),
            pendingSmsSuggestionsJson = readEncryptedString("pending_sms_suggestions_json", "[]", unreadable, legacy),
            processedSmsKeysJson = readEncryptedString("processed_sms_keys_json", "[]", unreadable, legacy),
            smsSyncBaselineTimestampMillis = safeLong("sms_sync_baseline_millis", 0L),
            latestSmsBalanceAmount = readEncryptedDouble("latest_sms_balance_amount", -1.0, unreadable, legacy),
            latestSmsBalanceUpdatedAt = safeLong("latest_sms_balance_updated_at", 0L),
            latestSmsBalanceRawSnippet = readEncryptedString("latest_sms_balance_snippet", "", unreadable, legacy),
            smsBalanceHistoryJson = readEncryptedString("sms_balance_history_json", "[]", unreadable, legacy),
            detectedSmsTaxesJson = readEncryptedString("detected_sms_taxes_json", "[]", unreadable, legacy),
            hasCompletedPermissionsOnboarding = safeBoolean("has_completed_permissions_onboarding", false)
        )
        return LoadResult(
            settings = settings,
            unreadableKeys = unreadable,
            legacyPlaintextValues = legacy
        )
    }

    private fun putEncrypted(
        editor: SharedPreferences.Editor,
        key: String,
        plainValue: String,
        defaultValue: String,
        priorUnreadable: Map<String, CryptoIssueType>,
        writeIssuesOut: MutableMap<String, CryptoIssueType>
    ) {
        // Preserve potentially recoverable ciphertext on disk if the caller didn't intentionally replace this field
        if (key in priorUnreadable && plainValue == defaultValue) {
            writeIssuesOut[key] = priorUnreadable.getValue(key)
            return
        }
        val encrypted = crypto.encrypt(plainValue, aadFor(key))
        if (encrypted != null) {
            editor.putString(key, encrypted)
        } else {
            writeIssuesOut[key] = CryptoIssueType.KEYSTORE_UNAVAILABLE
        }
    }

    private fun persistSettings(
        finalUpdated: UserSettings,
        priorUnreadable: Map<String, CryptoIssueType>
    ): Map<String, CryptoIssueType> {
        val writeIssues = mutableMapOf<String, CryptoIssueType>()
        isSelfUpdating = true
        try {
            val editor = prefs.edit()
            putEncrypted(editor, "salary_amount", finalUpdated.salaryAmount.toString(), "85000.0", priorUnreadable, writeIssues)
            editor.putInt("salary_payday", finalUpdated.salaryPayday)
            putEncrypted(editor, "salary_employer", finalUpdated.salaryEmployer, "Enterprise Algiers", priorUnreadable, writeIssues)
            editor.putBoolean("salary_auto_credit", finalUpdated.salaryAutoCredit)
            editor.putString("last_credited_month", finalUpdated.lastCreditedMonthYear)
            editor.putBoolean("pattern_lock_enabled", finalUpdated.patternLockEnabled)
            putEncrypted(editor, "pattern_lock_hash", finalUpdated.patternLockHash, "", priorUnreadable, writeIssues)
            editor.putBoolean("biometric_enabled", finalUpdated.biometricEnabled)
            editor.putString("app_language", finalUpdated.language)
            editor.putString("theme_mode", finalUpdated.themeMode)
            editor.putString("design_style", finalUpdated.designStyle)
            editor.putBoolean("gdrive_connected", finalUpdated.googleDriveConnected)
            editor.putLong("last_backup_time", finalUpdated.lastBackupTimestamp)
            editor.putBoolean("stealth_mode", finalUpdated.stealthMode)
            putEncrypted(editor, "vault_locked_amount", finalUpdated.savingsWalletBalance.toString(), "0.0", priorUnreadable, writeIssues)
            putEncrypted(editor, "savings_wallet_balance", finalUpdated.savingsWalletBalance.toString(), "0.0", priorUnreadable, writeIssues)
            putEncrypted(editor, "savings_goal_amount", finalUpdated.savingsGoalAmount.toString(), "0.0", priorUnreadable, writeIssues)
            putEncrypted(editor, "savings_history_json", finalUpdated.savingsHistoryJson, "[]", priorUnreadable, writeIssues)
            editor.putBoolean("include_assets_in_savings_total", finalUpdated.includeAssetsInSavingsTotal)
            putEncrypted(editor, "gold_asset_holdings_json", finalUpdated.goldAssetHoldingsJson, "[]", priorUnreadable, writeIssues)
            editor.putString("classic_card_bank_style", finalUpdated.classicCardBankStyle)
            putEncrypted(editor, "classic_card_holder", finalUpdated.classicCardHolderName, "CHARFEDDINE KA", priorUnreadable, writeIssues)
            putEncrypted(editor, "classic_card_number", finalUpdated.classicCardNumber, "", priorUnreadable, writeIssues)
            putEncrypted(editor, "classic_card_expiry", finalUpdated.classicCardExpiry, "11/29", priorUnreadable, writeIssues)
            putEncrypted(editor, "classic_card_cvc", finalUpdated.classicCardCvc, "", priorUnreadable, writeIssues)
            editor.putBoolean("breathing_glow_enabled", finalUpdated.breathingGlowEnabled)
            editor.putBoolean("screenshot_protection_enabled", finalUpdated.screenshotProtectionEnabled)
            putEncrypted(editor, "vault_keys_json", finalUpdated.vaultKeysJson, "[]", priorUnreadable, writeIssues)
            putEncrypted(editor, "ccp_accounts_json", finalUpdated.ccpAccountsJson, "[]", priorUnreadable, writeIssues)
            editor.putString("visa_card_provider", finalUpdated.visaCardProvider)
            putEncrypted(editor, "visa_card_holder", finalUpdated.visaCardHolderName, "CHARFEDDINE KA", priorUnreadable, writeIssues)
            putEncrypted(editor, "visa_card_number", finalUpdated.visaCardNumber, "", priorUnreadable, writeIssues)
            putEncrypted(editor, "visa_card_expiry", finalUpdated.visaCardExpiry, "08/28", priorUnreadable, writeIssues)
            putEncrypted(editor, "visa_card_cvc", finalUpdated.visaCardCvc, "", priorUnreadable, writeIssues)
            editor.putBoolean("third_card_enabled", finalUpdated.thirdCardEnabled)
            editor.putString("third_card_provider", finalUpdated.thirdCardProvider)
            putEncrypted(editor, "third_card_holder", finalUpdated.thirdCardHolderName, "CHARFEDDINE KA", priorUnreadable, writeIssues)
            putEncrypted(editor, "third_card_number", finalUpdated.thirdCardNumber, "", priorUnreadable, writeIssues)
            putEncrypted(editor, "third_card_expiry", finalUpdated.thirdCardExpiry, "08/28", priorUnreadable, writeIssues)
            putEncrypted(editor, "third_card_cvc", finalUpdated.thirdCardCvc, "", priorUnreadable, writeIssues)
            putEncrypted(editor, "binance_holdings_json", finalUpdated.binanceHoldingsJson, "[]", priorUnreadable, writeIssues)
            editor.putString("binance_prices_json", finalUpdated.binancePricesJson)
            editor.putLong("binance_last_price_sync_millis", finalUpdated.binanceLastPriceSyncMillis)
            editor.putString("square_market_snapshot_json", finalUpdated.squareMarketSnapshotJson)
            editor.putLong("square_last_price_sync_millis", finalUpdated.squareLastPriceSyncMillis)
            editor.putString("square_daily_baseline_json", finalUpdated.squareDailyBaselineJson)
            editor.putString("square_pinned_ids_json", finalUpdated.squarePinnedIdsJson)
            editor.putInt("square_sync_interval_minutes", finalUpdated.squareSyncIntervalMinutes.coerceIn(0, 1440))
            editor.putBoolean("baridimob_sms_sync_enabled", finalUpdated.baridiMobSmsSyncEnabled)
            putEncrypted(editor, "pending_sms_suggestions_json", finalUpdated.pendingSmsSuggestionsJson, "[]", priorUnreadable, writeIssues)
            putEncrypted(editor, "processed_sms_keys_json", finalUpdated.processedSmsKeysJson, "[]", priorUnreadable, writeIssues)
            editor.putLong("sms_sync_baseline_millis", finalUpdated.smsSyncBaselineTimestampMillis)
            putEncrypted(editor, "latest_sms_balance_amount", finalUpdated.latestSmsBalanceAmount.toString(), "-1.0", priorUnreadable, writeIssues)
            editor.putLong("latest_sms_balance_updated_at", finalUpdated.latestSmsBalanceUpdatedAt)
            putEncrypted(editor, "latest_sms_balance_snippet", finalUpdated.latestSmsBalanceRawSnippet, "", priorUnreadable, writeIssues)
            putEncrypted(editor, "sms_balance_history_json", finalUpdated.smsBalanceHistoryJson, "[]", priorUnreadable, writeIssues)
            putEncrypted(editor, "detected_sms_taxes_json", finalUpdated.detectedSmsTaxesJson, "[]", priorUnreadable, writeIssues)
            editor.putBoolean("has_completed_permissions_onboarding", finalUpdated.hasCompletedPermissionsOnboarding)
            editor.apply()
        } finally {
            isSelfUpdating = false
        }
        return writeIssues
    }

    @Synchronized
    fun updateSettings(transform: (UserSettings) -> UserSettings) {
        val loaded = loadSettingsInternal()
        val updated = transform(loaded.settings)
        val finalUpdated = if (updated.savingsWalletBalance != updated.vaultLockedAmount) {
            updated.copy(vaultLockedAmount = updated.savingsWalletBalance)
        } else {
            updated
        }
        val writeIssues = persistSettings(finalUpdated, loaded.unreadableKeys)
        unreadableKeysSnapshot = writeIssues
        _cryptoIssue.value = buildCryptoIssue(writeIssues)
        _settingsFlow.value = finalUpdated
    }

    @Synchronized
    fun retryCryptoRecovery(): Boolean {
        val refreshed = initializeAndMigrate()
        _settingsFlow.value = refreshed
        return unreadableKeysSnapshot.isEmpty()
    }

    @Synchronized
    fun resetCorruptedEncryptedEntries(resetMasterKeyIfInvalidated: Boolean = true) {
        val loaded = loadSettingsInternal()
        val hasKeystoreLevelFailure = loaded.unreadableKeys.values.any {
            it == CryptoIssueType.KEY_INVALIDATED || it == CryptoIssueType.KEYSTORE_UNAVAILABLE
        }
        isSelfUpdating = true
        try {
            val editor = prefs.edit()
            for (badKey in loaded.unreadableKeys.keys) {
                editor.remove(badKey)
            }
            editor.apply()
        } finally {
            isSelfUpdating = false
        }
        if (hasKeystoreLevelFailure && resetMasterKeyIfInvalidated) {
            crypto.resetMasterKey()
        }
        val remainingIssues = persistSettings(loaded.settings, emptyMap())
        unreadableKeysSnapshot = remainingIssues
        _cryptoIssue.value = buildCryptoIssue(remainingIssues)
        _settingsFlow.value = loaded.settings
    }

    fun isHardwareBackedKeystore(): Boolean = crypto.isHardwareBacked()
}
