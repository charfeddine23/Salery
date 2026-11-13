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
    val classicCardHolderName: String = "CHARFEDDINE KA",
    val classicCardNumber: String = "",
    val classicCardExpiry: String = "11/29",
    val classicCardCvc: String = "",
    val breathingGlowEnabled: Boolean = true
)

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("salerypelse_prefs", Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<UserSettings> = _settingsFlow.asStateFlow()

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
            classicCardHolderName = safeString("classic_card_holder", "CHARFEDDINE KA"),
            classicCardNumber = safeString("classic_card_number", ""),
            classicCardExpiry = safeString("classic_card_expiry", "11/29"),
            classicCardCvc = safeString("classic_card_cvc", ""),
            breathingGlowEnabled = safeBoolean("breathing_glow_enabled", true)
        )
    }

    fun updateSettings(transform: (UserSettings) -> UserSettings) {
        val updated = transform(_settingsFlow.value)
        val finalUpdated = if (updated.savingsWalletBalance != updated.vaultLockedAmount) {
            updated.copy(vaultLockedAmount = updated.savingsWalletBalance)
        } else {
            updated
        }
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
            .putString("classic_card_holder", finalUpdated.classicCardHolderName)
            .putString("classic_card_number", finalUpdated.classicCardNumber)
            .putString("classic_card_expiry", finalUpdated.classicCardExpiry)
            .putString("classic_card_cvc", finalUpdated.classicCardCvc)
            .putBoolean("breathing_glow_enabled", finalUpdated.breathingGlowEnabled)
            .apply()
        _settingsFlow.value = finalUpdated
    }
}
