package com.example.data

import android.content.Context
import android.content.SharedPreferences
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

    private fun safeFloat(key: String, default: Float): Float {
        return try {
            when (val raw = prefs.all[key]) {
                is Float -> raw
                is Number -> raw.toFloat()
                is String -> raw.toFloatOrNull() ?: default
                else -> default
            }
        } catch (_: Throwable) {
            default
        }
    }

    private fun safeDouble(key: String, default: Double): Double {
        return try {
            when (val raw = prefs.all[key]) {
                is Double -> raw
                is Number -> raw.toDouble()
                is String -> raw.toDoubleOrNull() ?: default
                else -> default
            }
        } catch (_: Throwable) {
            default
        }
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

    private fun loadSettings(): UserSettings {
        val vaultFallback = safeFloat("vault_locked_amount", 0.0f)
        val savedSavings = safeFloat("savings_wallet_balance", vaultFallback).toDouble()
        return UserSettings(
            salaryAmount = safeFloat("salary_amount", 85000.0f).toDouble(),
            salaryPayday = safeInt("salary_payday", 25),
            salaryEmployer = safeString("salary_employer", "Enterprise Algiers"),
            salaryAutoCredit = safeBoolean("salary_auto_credit", true),
            lastCreditedMonthYear = safeString("last_credited_month", ""),
            patternLockEnabled = safeBoolean("pattern_lock_enabled", false),
            patternLockHash = safeString("pattern_lock_hash", ""),
            biometricEnabled = safeBoolean("biometric_enabled", false),
            language = safeString("app_language", "en"),
            themeMode = safeString("theme_mode", "DARK"),
            designStyle = safeString("design_style", "LIQUID_GLASS"),
            googleDriveConnected = safeBoolean("gdrive_connected", true),
            lastBackupTimestamp = safeLong("last_backup_time", 0L),
            stealthMode = safeBoolean("stealth_mode", false),
            vaultLockedAmount = savedSavings,
            savingsWalletBalance = savedSavings,
            classicCardBankStyle = safeString("classic_card_bank_style", "EDAHABIA"),
            classicCardHolderName = safeString("classic_card_holder", "CHARFEDDINE KA"),
            classicCardNumber = safeString("classic_card_number", ""),
            classicCardExpiry = safeString("classic_card_expiry", "11/29"),
            classicCardCvc = safeString("classic_card_cvc", ""),
            breathingGlowEnabled = safeBoolean("breathing_glow_enabled", true),
            screenshotProtectionEnabled = safeBoolean("screenshot_protection_enabled", false),
            vaultKeysJson = safeString("vault_keys_json", "[]"),
            ccpAccountsJson = safeString("ccp_accounts_json", "[]"),
            visaCardProvider = safeString("visa_card_provider", "DEFAULT"),
            visaCardHolderName = safeString("visa_card_holder", "CHARFEDDINE KA"),
            visaCardNumber = safeString("visa_card_number", ""),
            visaCardExpiry = safeString("visa_card_expiry", "08/28"),
            visaCardCvc = safeString("visa_card_cvc", ""),
            thirdCardEnabled = safeBoolean("third_card_enabled", false),
            thirdCardProvider = safeString("third_card_provider", "BINANCE"),
            thirdCardHolderName = safeString("third_card_holder", "CHARFEDDINE KA"),
            thirdCardNumber = safeString("third_card_number", ""),
            thirdCardExpiry = safeString("third_card_expiry", "08/28"),
            thirdCardCvc = safeString("third_card_cvc", ""),
            binanceHoldingsJson = safeString("binance_holdings_json", "[]"),
            binancePricesJson = safeString("binance_prices_json", "[]"),
            binanceLastPriceSyncMillis = safeLong("binance_last_price_sync_millis", 0L),
            squareMarketSnapshotJson = safeString("square_market_snapshot_json", ""),
            squareLastPriceSyncMillis = safeLong("square_last_price_sync_millis", 0L),
            baridiMobSmsSyncEnabled = safeBoolean("baridimob_sms_sync_enabled", true),
            pendingSmsSuggestionsJson = safeString("pending_sms_suggestions_json", "[]"),
            processedSmsKeysJson = safeString("processed_sms_keys_json", "[]"),
            smsSyncBaselineTimestampMillis = safeLong("sms_sync_baseline_millis", 0L),
            latestSmsBalanceAmount = safeDouble("latest_sms_balance_amount", -1.0),
            latestSmsBalanceUpdatedAt = safeLong("latest_sms_balance_updated_at", 0L),
            latestSmsBalanceRawSnippet = safeString("latest_sms_balance_snippet", ""),
            smsBalanceHistoryJson = safeString("sms_balance_history_json", "[]"),
            detectedSmsTaxesJson = safeString("detected_sms_taxes_json", "[]"),
            hasCompletedPermissionsOnboarding = safeBoolean("has_completed_permissions_onboarding", false)
        )
    }

    @Synchronized
    fun updateSettings(transform: (UserSettings) -> UserSettings) {
        val currentLatest = loadSettings()
        val updated = transform(currentLatest)
        val finalUpdated = if (updated.savingsWalletBalance != updated.vaultLockedAmount) {
            updated.copy(vaultLockedAmount = updated.savingsWalletBalance)
        } else {
            updated
        }
        isSelfUpdating = true
        try {
            prefs.edit()
                .putFloat("salary_amount", finalUpdated.salaryAmount.toFloat())
                .putInt("salary_payday", finalUpdated.salaryPayday)
                .putString("salary_employer", finalUpdated.salaryEmployer)
                .putBoolean("salary_auto_credit", finalUpdated.salaryAutoCredit)
                .putString("last_credited_month", finalUpdated.lastCreditedMonthYear)
                .putBoolean("pattern_lock_enabled", finalUpdated.patternLockEnabled)
                .putString("pattern_lock_hash", finalUpdated.patternLockHash)
                .putBoolean("biometric_enabled", finalUpdated.biometricEnabled)
                .putString("app_language", finalUpdated.language)
                .putString("theme_mode", finalUpdated.themeMode)
                .putString("design_style", finalUpdated.designStyle)
                .putBoolean("gdrive_connected", finalUpdated.googleDriveConnected)
                .putLong("last_backup_time", finalUpdated.lastBackupTimestamp)
                .putBoolean("stealth_mode", finalUpdated.stealthMode)
                .putFloat("vault_locked_amount", finalUpdated.savingsWalletBalance.toFloat())
                .putFloat("savings_wallet_balance", finalUpdated.savingsWalletBalance.toFloat())
                .putString("classic_card_bank_style", finalUpdated.classicCardBankStyle)
                .putString("classic_card_holder", finalUpdated.classicCardHolderName)
                .putString("classic_card_number", finalUpdated.classicCardNumber)
                .putString("classic_card_expiry", finalUpdated.classicCardExpiry)
                .putString("classic_card_cvc", finalUpdated.classicCardCvc)
                .putBoolean("breathing_glow_enabled", finalUpdated.breathingGlowEnabled)
                .putBoolean("screenshot_protection_enabled", finalUpdated.screenshotProtectionEnabled)
                .putString("vault_keys_json", finalUpdated.vaultKeysJson)
                .putString("ccp_accounts_json", finalUpdated.ccpAccountsJson)
                .putString("visa_card_provider", finalUpdated.visaCardProvider)
                .putString("visa_card_holder", finalUpdated.visaCardHolderName)
                .putString("visa_card_number", finalUpdated.visaCardNumber)
                .putString("visa_card_expiry", finalUpdated.visaCardExpiry)
                .putString("visa_card_cvc", finalUpdated.visaCardCvc)
                .putBoolean("third_card_enabled", finalUpdated.thirdCardEnabled)
                .putString("third_card_provider", finalUpdated.thirdCardProvider)
                .putString("third_card_holder", finalUpdated.thirdCardHolderName)
                .putString("third_card_number", finalUpdated.thirdCardNumber)
                .putString("third_card_expiry", finalUpdated.thirdCardExpiry)
                .putString("third_card_cvc", finalUpdated.thirdCardCvc)
                .putString("binance_holdings_json", finalUpdated.binanceHoldingsJson)
                .putString("binance_prices_json", finalUpdated.binancePricesJson)
                .putLong("binance_last_price_sync_millis", finalUpdated.binanceLastPriceSyncMillis)
                .putString("square_market_snapshot_json", finalUpdated.squareMarketSnapshotJson)
                .putLong("square_last_price_sync_millis", finalUpdated.squareLastPriceSyncMillis)
                .putBoolean("baridimob_sms_sync_enabled", finalUpdated.baridiMobSmsSyncEnabled)
                .putString("pending_sms_suggestions_json", finalUpdated.pendingSmsSuggestionsJson)
                .putString("processed_sms_keys_json", finalUpdated.processedSmsKeysJson)
                .putLong("sms_sync_baseline_millis", finalUpdated.smsSyncBaselineTimestampMillis)
                .putString("latest_sms_balance_amount", finalUpdated.latestSmsBalanceAmount.toString())
                .putLong("latest_sms_balance_updated_at", finalUpdated.latestSmsBalanceUpdatedAt)
                .putString("latest_sms_balance_snippet", finalUpdated.latestSmsBalanceRawSnippet)
                .putString("sms_balance_history_json", finalUpdated.smsBalanceHistoryJson)
                .putString("detected_sms_taxes_json", finalUpdated.detectedSmsTaxesJson)
                .putBoolean("has_completed_permissions_onboarding", finalUpdated.hasCompletedPermissionsOnboarding)
                .apply()
            _settingsFlow.value = finalUpdated
        } finally {
            isSelfUpdating = false
        }
    }
}
