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
    val vaultLockedAmount: Double = 0.0
)

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("salerypelse_prefs", Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<UserSettings> = _settingsFlow.asStateFlow()

    private fun loadSettings(): UserSettings {
        return UserSettings(
            salaryAmount = prefs.getFloat("salary_amount", 85000.0f).toDouble(),
            salaryPayday = prefs.getInt("salary_payday", 25),
            salaryEmployer = prefs.getString("salary_employer", "Enterprise Algiers") ?: "Enterprise Algiers",
            salaryAutoCredit = prefs.getBoolean("salary_auto_credit", true),
            lastCreditedMonthYear = prefs.getString("last_credited_month", "") ?: "",
            patternLockEnabled = prefs.getBoolean("pattern_lock_enabled", false),
            patternLockHash = prefs.getString("pattern_lock_hash", "") ?: "",
            biometricEnabled = prefs.getBoolean("biometric_enabled", false),
            language = prefs.getString("app_language", "en") ?: "en",
            themeMode = prefs.getString("theme_mode", "DARK") ?: "DARK",
            designStyle = prefs.getString("design_style", "LIQUID_GLASS") ?: "LIQUID_GLASS",
            googleDriveConnected = prefs.getBoolean("gdrive_connected", true),
            lastBackupTimestamp = prefs.getLong("last_backup_time", 0L),
            stealthMode = prefs.getBoolean("stealth_mode", false),
            vaultLockedAmount = prefs.getFloat("vault_locked_amount", 0.0f).toDouble()
        )
    }

    fun updateSettings(transform: (UserSettings) -> UserSettings) {
        val updated = transform(_settingsFlow.value)
        prefs.edit()
            .putFloat("salary_amount", updated.salaryAmount.toFloat())
            .putInt("salary_payday", updated.salaryPayday)
            .putString("salary_employer", updated.salaryEmployer)
            .putBoolean("salary_auto_credit", updated.salaryAutoCredit)
            .putString("last_credited_month", updated.lastCreditedMonthYear)
            .putBoolean("pattern_lock_enabled", updated.patternLockEnabled)
            .putString("pattern_lock_hash", updated.patternLockHash)
            .putBoolean("biometric_enabled", updated.biometricEnabled)
            .putString("app_language", updated.language)
            .putString("theme_mode", updated.themeMode)
            .putString("design_style", updated.designStyle)
            .putBoolean("gdrive_connected", updated.googleDriveConnected)
            .putLong("last_backup_time", updated.lastBackupTimestamp)
            .putBoolean("stealth_mode", updated.stealthMode)
            .putFloat("vault_locked_amount", updated.vaultLockedAmount.toFloat())
            .apply()
        _settingsFlow.value = updated
    }
}
