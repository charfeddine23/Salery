package com.example.data

import android.content.Context
import android.content.SharedPreferences
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
    val autoLockTimeoutSeconds: Int = 30,
    val language: String = "en", // "en" or "ar"
    val themeMode: String = "DARK", // "DARK", "LIGHT", or "SYSTEM"
    val designStyle: String = "LIQUID_GLASS", // "LIQUID_GLASS" or "NEUMORPHISM"
    val lastBackupTimestamp: Long = 0L,
    val stealthMode: Boolean = false,
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
    val squareEnabledSourceIdsJson: String = "",
    val baridiMobSmsSyncEnabled: Boolean = true,
    val pendingSmsSuggestionsJson: String = "[]",
    val processedSmsKeysJson: String = "[]",
    val smsSyncBaselineTimestampMillis: Long = 0L,
    val latestSmsBalanceAmount: Double = -1.0,
    val latestSmsBalanceUpdatedAt: Long = 0L,
    val latestSmsBalanceRawSnippet: String = "",
    val smsBalanceHistoryJson: String = "[]",
    val detectedSmsTaxesJson: String = "[]",
    val detectedFailedSmsTransactionsJson: String = "[]",
    val dismissedFailedSmsIdsJson: String = "[]",
    val autoAddTaxesEnabled: Boolean = false,
    val autoAddTaxesBaselineMillis: Long = 0L,
    val hasCompletedPermissionsOnboarding: Boolean = false,
    val hasLocalAccount: Boolean = false,
    val localUsername: String = "",
    val localWilayaCode: String = "",
    val localWilayaName: String = "",
    val localCommuneName: String = ""
)

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("salerypelse_prefs", Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<UserSettings> = _settingsFlow.asStateFlow()

    @Volatile
    private var isSelfUpdating = false

    private val prefChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        if (!isSelfUpdating) {
            _settingsFlow.value = loadSettings()
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefChangeListener)
    }

    private fun safeDouble(all: Map<String, *>, key: String, default: Double): Double {
        return try {
            when (val raw = all[key]) {
                is Double -> raw
                is Number -> raw.toDouble()
                is String -> raw.toDoubleOrNull() ?: default
                else -> default
            }
        } catch (_: Throwable) {
            default
        }
    }

    private fun safeInt(all: Map<String, *>, key: String, default: Int): Int {
        return try {
            when (val raw = all[key]) {
                is Int -> raw
                is Number -> raw.toInt()
                is String -> raw.toIntOrNull() ?: default
                else -> default
            }
        } catch (_: Throwable) {
            default
        }
    }

    private fun safeLong(all: Map<String, *>, key: String, default: Long): Long {
        return try {
            when (val raw = all[key]) {
                is Long -> raw
                is Number -> raw.toLong()
                is String -> raw.toLongOrNull() ?: default
                else -> default
            }
        } catch (_: Throwable) {
            default
        }
    }

    private fun safeBoolean(all: Map<String, *>, key: String, default: Boolean): Boolean {
        return try {
            when (val raw = all[key]) {
                is Boolean -> raw
                is String -> raw.toBooleanStrictOrNull() ?: default
                else -> default
            }
        } catch (_: Throwable) {
            default
        }
    }

    private fun safeString(all: Map<String, *>, key: String, default: String): String {
        return try {
            when (val raw = all[key]) {
                is String -> raw
                null -> default
                else -> raw.toString()
            }
        } catch (_: Throwable) {
            default
        }
    }

    private fun safeDecryptedString(all: Map<String, *>, key: String, default: String): String {
        val raw = safeString(all, key, default)
        if (raw == default && raw.isEmpty()) return default
        val decrypted = VaultCryptoManager.decryptSensitive(raw)
        return decrypted.ifEmpty { default }
    }

    private fun loadSettings(): UserSettings {
        val all = prefs.all
        val vaultFallback = safeDouble(all, "vault_locked_amount", 0.0)
        val savedSavings = safeDouble(all, "savings_wallet_balance", vaultFallback)
        return UserSettings(
            salaryAmount = safeDouble(all, "salary_amount", 85000.0),
            salaryPayday = safeInt(all, "salary_payday", 25),
            salaryEmployer = safeString(all, "salary_employer", "Enterprise Algiers"),
            salaryAutoCredit = safeBoolean(all, "salary_auto_credit", true),
            lastCreditedMonthYear = safeString(all, "last_credited_month", ""),
            patternLockEnabled = safeBoolean(all, "pattern_lock_enabled", false),
            patternLockHash = safeString(all, "pattern_lock_hash", ""),
            biometricEnabled = safeBoolean(all, "biometric_enabled", false),
            autoLockTimeoutSeconds = safeInt(all, "auto_lock_timeout_seconds", 30),
            language = safeString(all, "app_language", "en"),
            themeMode = safeString(all, "theme_mode", "DARK"),
            designStyle = safeString(all, "design_style", "LIQUID_GLASS"),
            lastBackupTimestamp = safeLong(all, "last_backup_time", 0L),
            stealthMode = safeBoolean(all, "stealth_mode", false),
            savingsWalletBalance = savedSavings,
            savingsGoalAmount = safeDouble(all, "savings_goal_amount", 0.0),
            savingsHistoryJson = safeString(all, "savings_history_json", "[]"),
            includeAssetsInSavingsTotal = safeBoolean(all, "include_assets_in_savings_total", false),
            goldAssetHoldingsJson = safeString(all, "gold_asset_holdings_json", "[]"),
            classicCardBankStyle = safeString(all, "classic_card_bank_style", "EDAHABIA"),
            classicCardHolderName = safeString(all, "classic_card_holder", "CHARFEDDINE KA"),
            classicCardNumber = safeDecryptedString(all, "classic_card_number", ""),
            classicCardExpiry = safeString(all, "classic_card_expiry", "11/29"),
            classicCardCvc = safeDecryptedString(all, "classic_card_cvc", ""),
            breathingGlowEnabled = safeBoolean(all, "breathing_glow_enabled", true),
            screenshotProtectionEnabled = safeBoolean(all, "screenshot_protection_enabled", false),
            vaultKeysJson = safeDecryptedString(all, "vault_keys_json", "[]"),
            ccpAccountsJson = safeDecryptedString(all, "ccp_accounts_json", "[]"),
            visaCardProvider = safeString(all, "visa_card_provider", "DEFAULT"),
            visaCardHolderName = safeString(all, "visa_card_holder", "CHARFEDDINE KA"),
            visaCardNumber = safeDecryptedString(all, "visa_card_number", ""),
            visaCardExpiry = safeString(all, "visa_card_expiry", "08/28"),
            visaCardCvc = safeDecryptedString(all, "visa_card_cvc", ""),
            thirdCardEnabled = safeBoolean(all, "third_card_enabled", false),
            thirdCardProvider = safeString(all, "third_card_provider", "BINANCE"),
            thirdCardHolderName = safeString(all, "third_card_holder", "CHARFEDDINE KA"),
            thirdCardNumber = safeDecryptedString(all, "third_card_number", ""),
            thirdCardExpiry = safeString(all, "third_card_expiry", "08/28"),
            thirdCardCvc = safeDecryptedString(all, "third_card_cvc", ""),
            binanceHoldingsJson = safeString(all, "binance_holdings_json", "[]"),
            binancePricesJson = safeString(all, "binance_prices_json", "[]"),
            binanceLastPriceSyncMillis = safeLong(all, "binance_last_price_sync_millis", 0L),
            squareMarketSnapshotJson = safeString(all, "square_market_snapshot_json", ""),
            squareLastPriceSyncMillis = safeLong(all, "square_last_price_sync_millis", 0L),
            squareDailyBaselineJson = safeString(all, "square_daily_baseline_json", ""),
            squarePinnedIdsJson = safeString(all, "square_pinned_ids_json", "[\"XAU_SPOT\",\"XAU_24K\",\"XAU_18K\",\"EUR\",\"USD\"]"),
            squareSyncIntervalMinutes = safeInt(all, "square_sync_interval_minutes", 60).coerceIn(0, 1440),
            squareEnabledSourceIdsJson = safeString(all, "square_enabled_source_ids_json", ""),
            baridiMobSmsSyncEnabled = safeBoolean(all, "baridimob_sms_sync_enabled", true),
            pendingSmsSuggestionsJson = safeString(all, "pending_sms_suggestions_json", "[]"),
            processedSmsKeysJson = safeString(all, "processed_sms_keys_json", "[]"),
            smsSyncBaselineTimestampMillis = safeLong(all, "sms_sync_baseline_millis", 0L),
            latestSmsBalanceAmount = safeDouble(all, "latest_sms_balance_amount", -1.0),
            latestSmsBalanceUpdatedAt = safeLong(all, "latest_sms_balance_updated_at", 0L),
            latestSmsBalanceRawSnippet = safeString(all, "latest_sms_balance_snippet", ""),
            smsBalanceHistoryJson = safeString(all, "sms_balance_history_json", "[]"),
            detectedSmsTaxesJson = safeString(all, "detected_sms_taxes_json", "[]"),
            detectedFailedSmsTransactionsJson = safeString(all, "detected_failed_sms_transactions_json", "[]"),
            dismissedFailedSmsIdsJson = safeString(all, "dismissed_failed_sms_ids_json", "[]"),
            autoAddTaxesEnabled = safeBoolean(all, "auto_add_taxes_enabled", false),
            autoAddTaxesBaselineMillis = safeLong(all, "auto_add_taxes_baseline_millis", 0L),
            hasCompletedPermissionsOnboarding = safeBoolean(all, "has_completed_permissions_onboarding", false),
            hasLocalAccount = safeBoolean(all, "has_local_account_v1", false),
            localUsername = safeString(all, "local_username_v1", ""),
            localWilayaCode = safeString(all, "local_wilaya_code_v1", ""),
            localWilayaName = safeString(all, "local_wilaya_name_v1", ""),
            localCommuneName = safeString(all, "local_commune_name_v1", "")
        )
    }

    @Synchronized
    fun updateSettings(transform: (UserSettings) -> UserSettings) {
        val currentLatest = loadSettings()
        val finalUpdated = transform(currentLatest)
        isSelfUpdating = true
        try {
            prefs.edit()
                .putString("salary_amount", finalUpdated.salaryAmount.toString())
                .putInt("salary_payday", finalUpdated.salaryPayday)
                .putString("salary_employer", finalUpdated.salaryEmployer)
                .putBoolean("salary_auto_credit", finalUpdated.salaryAutoCredit)
                .putString("last_credited_month", finalUpdated.lastCreditedMonthYear)
                .putBoolean("pattern_lock_enabled", finalUpdated.patternLockEnabled)
                .putString("pattern_lock_hash", finalUpdated.patternLockHash)
                .putBoolean("biometric_enabled", finalUpdated.biometricEnabled)
                .putInt("auto_lock_timeout_seconds", finalUpdated.autoLockTimeoutSeconds)
                .putString("app_language", finalUpdated.language)
                .putString("theme_mode", finalUpdated.themeMode)
                .putString("design_style", finalUpdated.designStyle)
                .putLong("last_backup_time", finalUpdated.lastBackupTimestamp)
                .putBoolean("stealth_mode", finalUpdated.stealthMode)
                .putString("savings_wallet_balance", finalUpdated.savingsWalletBalance.toString())
                .putString("savings_goal_amount", finalUpdated.savingsGoalAmount.toString())
                .putString("savings_history_json", finalUpdated.savingsHistoryJson)
                .putBoolean("include_assets_in_savings_total", finalUpdated.includeAssetsInSavingsTotal)
                .putString("gold_asset_holdings_json", finalUpdated.goldAssetHoldingsJson)
                .putString("classic_card_bank_style", finalUpdated.classicCardBankStyle)
                .putString("classic_card_holder", finalUpdated.classicCardHolderName)
                .putString("classic_card_number", VaultCryptoManager.encryptSensitive(finalUpdated.classicCardNumber))
                .putString("classic_card_expiry", finalUpdated.classicCardExpiry)
                .putString("classic_card_cvc", VaultCryptoManager.encryptSensitive(finalUpdated.classicCardCvc))
                .putBoolean("breathing_glow_enabled", finalUpdated.breathingGlowEnabled)
                .putBoolean("screenshot_protection_enabled", finalUpdated.screenshotProtectionEnabled)
                .putString("vault_keys_json", VaultCryptoManager.encryptSensitive(finalUpdated.vaultKeysJson))
                .putString("ccp_accounts_json", VaultCryptoManager.encryptSensitive(finalUpdated.ccpAccountsJson))
                .putString("visa_card_provider", finalUpdated.visaCardProvider)
                .putString("visa_card_holder", finalUpdated.visaCardHolderName)
                .putString("visa_card_number", VaultCryptoManager.encryptSensitive(finalUpdated.visaCardNumber))
                .putString("visa_card_expiry", finalUpdated.visaCardExpiry)
                .putString("visa_card_cvc", VaultCryptoManager.encryptSensitive(finalUpdated.visaCardCvc))
                .putBoolean("third_card_enabled", finalUpdated.thirdCardEnabled)
                .putString("third_card_provider", finalUpdated.thirdCardProvider)
                .putString("third_card_holder", finalUpdated.thirdCardHolderName)
                .putString("third_card_number", VaultCryptoManager.encryptSensitive(finalUpdated.thirdCardNumber))
                .putString("third_card_expiry", finalUpdated.thirdCardExpiry)
                .putString("third_card_cvc", VaultCryptoManager.encryptSensitive(finalUpdated.thirdCardCvc))
                .putString("binance_holdings_json", finalUpdated.binanceHoldingsJson)
                .putString("binance_prices_json", finalUpdated.binancePricesJson)
                .putLong("binance_last_price_sync_millis", finalUpdated.binanceLastPriceSyncMillis)
                .putString("square_market_snapshot_json", finalUpdated.squareMarketSnapshotJson)
                .putLong("square_last_price_sync_millis", finalUpdated.squareLastPriceSyncMillis)
                .putString("square_daily_baseline_json", finalUpdated.squareDailyBaselineJson)
                .putString("square_pinned_ids_json", finalUpdated.squarePinnedIdsJson)
                .putInt("square_sync_interval_minutes", finalUpdated.squareSyncIntervalMinutes.coerceIn(0, 1440))
                .putString("square_enabled_source_ids_json", finalUpdated.squareEnabledSourceIdsJson)
                .putBoolean("baridimob_sms_sync_enabled", finalUpdated.baridiMobSmsSyncEnabled)
                .putString("pending_sms_suggestions_json", finalUpdated.pendingSmsSuggestionsJson)
                .putString("processed_sms_keys_json", finalUpdated.processedSmsKeysJson)
                .putLong("sms_sync_baseline_millis", finalUpdated.smsSyncBaselineTimestampMillis)
                .putString("latest_sms_balance_amount", finalUpdated.latestSmsBalanceAmount.toString())
                .putLong("latest_sms_balance_updated_at", finalUpdated.latestSmsBalanceUpdatedAt)
                .putString("latest_sms_balance_snippet", finalUpdated.latestSmsBalanceRawSnippet)
                .putString("sms_balance_history_json", finalUpdated.smsBalanceHistoryJson)
                .putString("detected_sms_taxes_json", finalUpdated.detectedSmsTaxesJson)
                .putString("detected_failed_sms_transactions_json", finalUpdated.detectedFailedSmsTransactionsJson)
                .putString("dismissed_failed_sms_ids_json", finalUpdated.dismissedFailedSmsIdsJson)
                .putBoolean("auto_add_taxes_enabled", finalUpdated.autoAddTaxesEnabled)
                .putLong("auto_add_taxes_baseline_millis", finalUpdated.autoAddTaxesBaselineMillis)
                .putBoolean("has_completed_permissions_onboarding", finalUpdated.hasCompletedPermissionsOnboarding)
                .putBoolean("has_local_account_v1", finalUpdated.hasLocalAccount)
                .putString("local_username_v1", finalUpdated.localUsername)
                .putString("local_wilaya_code_v1", finalUpdated.localWilayaCode)
                .putString("local_wilaya_name_v1", finalUpdated.localWilayaName)
                .putString("local_commune_name_v1", finalUpdated.localCommuneName)
                .apply()
            _settingsFlow.value = finalUpdated
        } finally {
            isSelfUpdating = false
        }
    }
}
