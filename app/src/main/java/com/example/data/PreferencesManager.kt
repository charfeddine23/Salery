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
    val classicCardHolderName: String = "CARDHOLDER",
    val classicCardNumber: String = "",
    val classicCardExpiry: String = "11/29",
    val classicCardCvc: String = "",
    val breathingGlowEnabled: Boolean = true,
    val screenshotProtectionEnabled: Boolean = false,
    val vaultKeysJson: String = "[]",
    val ccpAccountsJson: String = "[]",
    val visaCardProvider: String = "DEFAULT", // "DEFAULT", "REDOTPAY", "BYBIT", "BINANCE"
    val visaCardHolderName: String = "CARDHOLDER",
    val visaCardNumber: String = "",
    val visaCardExpiry: String = "08/28",
    val visaCardCvc: String = "",
    val thirdCardEnabled: Boolean = false,
    val thirdCardProvider: String = "BINANCE", // "DEFAULT", "REDOTPAY", "BYBIT", "BINANCE"
    val thirdCardHolderName: String = "CARDHOLDER",
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
    val localCommuneName: String = "",
    val cardHubOrderJson: String = CardHubOrder.DEFAULT_JSON,
    val firstName: String = "",
    val lastName: String = "",
    val profileAvatarPath: String = ""
)

enum class PrefsPersistenceMode {
    GUEST_LOCAL,
    CLOUD_MEMORY,
    SIGNED_OUT
}

object SessionMetaStore {
    private const val META_PREFS_NAME = "vaultix_session_meta"
    private const val AUTH_UI_PREFS_NAME = "vaultix_auth_ui_prefs"
    private const val LEGACY_SHARED_PREFS_NAME = "salerypelse_prefs"

    const val REALM_NONE = "NONE"
    const val REALM_GUEST = "GUEST"
    const val REALM_CLOUD = "CLOUD"

    private fun metaPrefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(META_PREFS_NAME, Context.MODE_PRIVATE)

    private fun authUiPrefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(AUTH_UI_PREFS_NAME, Context.MODE_PRIVATE)

    fun getSessionRealm(context: Context): String =
        metaPrefs(context).getString("session_realm", "").orEmpty()

    fun setSessionRealm(context: Context, realm: String, cloudUid: String = "") {
        metaPrefs(context).edit()
            .putString("session_realm", realm)
            .putString("cloud_uid", if (realm == REALM_CLOUD) cloudUid else "")
            .apply()
    }

    fun getLastCloudUid(context: Context): String =
        metaPrefs(context).getString("cloud_uid", "").orEmpty()

    fun isGuestSessionActive(context: Context): Boolean =
        getSessionRealm(context) == REALM_GUEST

    fun isExplicitGoogleSignedOut(context: Context): Boolean =
        metaPrefs(context).getBoolean("google_explicit_signed_out", false)

    fun setExplicitGoogleSignedOut(context: Context, value: Boolean) {
        metaPrefs(context).edit().putBoolean("google_explicit_signed_out", value).apply()
    }

    fun getLastCloudSyncMillis(context: Context): Long =
        metaPrefs(context).getLong("last_cloud_sync_millis", 0L)

    fun setLastCloudSyncMillis(context: Context, millis: Long) {
        metaPrefs(context).edit().putLong("last_cloud_sync_millis", millis).apply()
    }

    fun getQuickAccountActive(context: Context): Boolean =
        metaPrefs(context).getBoolean("saved_quick_account_active", false)

    fun getQuickAccountName(context: Context): String =
        metaPrefs(context).getString("saved_quick_account_name", "").orEmpty()

    fun getQuickAccountEmail(context: Context): String =
        metaPrefs(context).getString("saved_quick_account_email", "").orEmpty()

    fun getQuickAccountIsCloud(context: Context): Boolean =
        metaPrefs(context).getBoolean("saved_quick_account_is_cloud", false)

    fun getQuickAccountUid(context: Context): String =
        metaPrefs(context).getString("saved_quick_account_uid", "").orEmpty()

    fun saveQuickAccount(
        context: Context,
        displayName: String,
        email: String = "",
        isCloudLinked: Boolean = false,
        uid: String = ""
    ) {
        metaPrefs(context).edit()
            .putBoolean("saved_quick_account_active", true)
            .putString("saved_quick_account_name", displayName)
            .putString("saved_quick_account_email", email)
            .putBoolean("saved_quick_account_is_cloud", isCloudLinked)
            .putString("saved_quick_account_uid", uid)
            .apply()
    }

    fun clearQuickAccount(context: Context) {
        metaPrefs(context).edit()
            .putBoolean("saved_quick_account_active", false)
            .remove("saved_quick_account_name")
            .remove("saved_quick_account_email")
            .remove("saved_quick_account_is_cloud")
            .remove("saved_quick_account_uid")
            .apply()
    }

    fun readAuthUiLanguage(context: Context): String =
        authUiPrefs(context).getString("auth_ui_language", "en") ?: "en"

    fun readAuthUiThemeMode(context: Context): String =
        authUiPrefs(context).getString("auth_ui_theme_mode", "DARK") ?: "DARK"

    fun writeAuthUiPreferences(context: Context, language: String? = null, themeMode: String? = null) {
        authUiPrefs(context).edit().apply {
            if (language != null) putString("auth_ui_language", language)
            if (themeMode != null) putString("auth_ui_theme_mode", themeMode)
        }.apply()
    }

    fun resetAuthUiPreferences(context: Context) {
        authUiPrefs(context).edit().clear().apply()
    }

    fun purgeLegacySharedStorageOnce(context: Context) {
        val meta = metaPrefs(context)
        if (meta.getBoolean("legacy_shared_storage_purged_v1", false)) return
        val appCtx = context.applicationContext
        val legacyPrefs = appCtx.getSharedPreferences(LEGACY_SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        if (!meta.contains("google_explicit_signed_out") && legacyPrefs.contains("google_explicit_signed_out")) {
            meta.edit()
                .putBoolean(
                    "google_explicit_signed_out",
                    legacyPrefs.getBoolean("google_explicit_signed_out", false)
                )
                .apply()
        }
        legacyPrefs.edit().clear().apply()
        AppDatabase.deleteLegacySharedDatabaseIfPresent(appCtx)
        runCatching { java.io.File(appCtx.filesDir, "profile_avatar.jpg").delete() }
        meta.edit().putBoolean("legacy_shared_storage_purged_v1", true).apply()
    }
}

class PreferencesManager(context: Context) {
    private val appContext: Context = context.applicationContext
    private val guestPrefs: SharedPreferences =
        appContext.getSharedPreferences(GUEST_PREFS_NAME, Context.MODE_PRIVATE)

    @Volatile
    private var persistenceMode: PrefsPersistenceMode = when (SessionMetaStore.getSessionRealm(appContext)) {
        SessionMetaStore.REALM_CLOUD -> PrefsPersistenceMode.CLOUD_MEMORY
        SessionMetaStore.REALM_NONE -> PrefsPersistenceMode.SIGNED_OUT
        else -> PrefsPersistenceMode.GUEST_LOCAL
    }

    @Volatile
    private var preservedEncryptedBlobs: Map<String, String> = emptyMap()

    @Volatile
    private var secretsWarmed: Boolean = false

    private val _settingsFlow = MutableStateFlow(
        when (persistenceMode) {
            PrefsPersistenceMode.GUEST_LOCAL -> loadSettings(decryptSecrets = false)
            PrefsPersistenceMode.CLOUD_MEMORY -> UserSettings(
                hasLocalAccount = true,
                language = SessionMetaStore.readAuthUiLanguage(appContext),
                themeMode = SessionMetaStore.readAuthUiThemeMode(appContext)
            )
            PrefsPersistenceMode.SIGNED_OUT -> buildCleanSignedOutSettings()
        }
    )
    val settingsFlow: StateFlow<UserSettings> = _settingsFlow.asStateFlow()

    @Volatile
    private var isSelfUpdating = false

    private val prefChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        if (!isSelfUpdating && persistenceMode == PrefsPersistenceMode.GUEST_LOCAL) {
            _settingsFlow.value = loadSettings(decryptSecrets = secretsWarmed)
        }
    }

    init {
        guestPrefs.registerOnSharedPreferenceChangeListener(prefChangeListener)
    }

    fun getPersistenceMode(): PrefsPersistenceMode = persistenceMode

    private fun buildCleanSignedOutSettings(): UserSettings {
        return UserSettings(
            hasLocalAccount = false,
            language = SessionMetaStore.readAuthUiLanguage(appContext),
            themeMode = SessionMetaStore.readAuthUiThemeMode(appContext)
        )
    }

    @Synchronized
    fun switchToGuestMode(activateAccount: Boolean? = null) {
        persistenceMode = PrefsPersistenceMode.GUEST_LOCAL
        secretsWarmed = true
        val loaded = loadSettings(decryptSecrets = true)
        if (activateAccount != null && loaded.hasLocalAccount != activateAccount) {
            writeGuestSettings(loaded.copy(hasLocalAccount = activateAccount))
        } else {
            _settingsFlow.value = loaded
        }
    }

    @Synchronized
    fun switchToCloudMemoryMode(initialSettings: UserSettings) {
        persistenceMode = PrefsPersistenceMode.CLOUD_MEMORY
        secretsWarmed = true
        preservedEncryptedBlobs = emptyMap()
        _settingsFlow.value = initialSettings
    }

    @Synchronized
    fun switchToSignedOutMode(resetAuthUiPreferences: Boolean = true) {
        persistenceMode = PrefsPersistenceMode.SIGNED_OUT
        secretsWarmed = true
        preservedEncryptedBlobs = emptyMap()
        if (resetAuthUiPreferences) {
            SessionMetaStore.resetAuthUiPreferences(appContext)
        }
        _settingsFlow.value = buildCleanSignedOutSettings()
    }

    /**
     * Decrypts Android Keystore-protected secrets off the Main Thread after Frame 1 completes
     * (only applicable for Guest local storage; Cloud sessions hold secrets strictly in RAM).
     */
    @Synchronized
    fun warmDecryptedSecrets() {
        if (secretsWarmed) return
        secretsWarmed = true
        if (persistenceMode == PrefsPersistenceMode.GUEST_LOCAL && preservedEncryptedBlobs.isNotEmpty()) {
            _settingsFlow.value = loadSettings(decryptSecrets = true)
        }
    }

    private fun safeDouble(all: Map<String, *>, key: String, default: Double): Double {
        return try {
            when (val raw = all[key]) {
                is Double -> raw
                is Number -> raw.toDouble()
                is String -> raw.toDoubleOrNull() ?: default
                else -> default
            }
        } catch (_: Exception) {
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
        } catch (_: Exception) {
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
        } catch (_: Exception) {
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
        } catch (_: Exception) {
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
        } catch (_: Exception) {
            default
        }
    }

    private fun safeDecryptedString(
        all: Map<String, *>,
        key: String,
        default: String,
        preserved: MutableMap<String, String>,
        decryptSecrets: Boolean
    ): String {
        val raw = safeString(all, key, default)
        if (raw == default && raw.isEmpty()) return default
        if (!decryptSecrets && raw.startsWith("v1_gcm$")) {
            preserved[key] = raw
            return default
        }
        val decrypted = VaultCryptoManager.decryptSensitiveOrNull(raw)
        if (decrypted == null) {
            preserved[key] = raw
            return default
        }
        return decrypted.ifEmpty { default }
    }

    private fun encodeSensitiveOrPreserve(
        key: String,
        value: String,
        default: String,
        preserved: Map<String, String>
    ): String {
        val failedBlob = preserved[key]
        if (failedBlob != null && value == default) {
            return failedBlob
        }
        return VaultCryptoManager.encryptSensitive(value)
    }

    private fun loadSettings(decryptSecrets: Boolean = true): UserSettings {
        val all = guestPrefs.all
        val vaultFallback = safeDouble(all, "vault_locked_amount", 0.0)
        val savedSavings = safeDouble(all, "savings_wallet_balance", vaultFallback)
        val failedEncrypted = HashMap<String, String>()
        val loaded = UserSettings(
            salaryAmount = safeDouble(all, "salary_amount", 85000.0),
            salaryPayday = safeInt(all, "salary_payday", 25),
            salaryEmployer = safeString(all, "salary_employer", "Enterprise Algiers"),
            salaryAutoCredit = safeBoolean(all, "salary_auto_credit", true),
            lastCreditedMonthYear = safeString(all, "last_credited_month", ""),
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
            classicCardHolderName = safeString(all, "classic_card_holder", "CARDHOLDER"),
            classicCardNumber = safeDecryptedString(all, "classic_card_number", "", failedEncrypted, decryptSecrets),
            classicCardExpiry = safeString(all, "classic_card_expiry", "11/29"),
            classicCardCvc = safeDecryptedString(all, "classic_card_cvc", "", failedEncrypted, decryptSecrets),
            breathingGlowEnabled = safeBoolean(all, "breathing_glow_enabled", true),
            screenshotProtectionEnabled = safeBoolean(all, "screenshot_protection_enabled", false),
            vaultKeysJson = safeDecryptedString(all, "vault_keys_json", "[]", failedEncrypted, decryptSecrets),
            ccpAccountsJson = safeDecryptedString(all, "ccp_accounts_json", "[]", failedEncrypted, decryptSecrets),
            visaCardProvider = safeString(all, "visa_card_provider", "DEFAULT"),
            visaCardHolderName = safeString(all, "visa_card_holder", "CARDHOLDER"),
            visaCardNumber = safeDecryptedString(all, "visa_card_number", "", failedEncrypted, decryptSecrets),
            visaCardExpiry = safeString(all, "visa_card_expiry", "08/28"),
            visaCardCvc = safeDecryptedString(all, "visa_card_cvc", "", failedEncrypted, decryptSecrets),
            thirdCardEnabled = safeBoolean(all, "third_card_enabled", false),
            thirdCardProvider = safeString(all, "third_card_provider", "BINANCE"),
            thirdCardHolderName = safeString(all, "third_card_holder", "CARDHOLDER"),
            thirdCardNumber = safeDecryptedString(all, "third_card_number", "", failedEncrypted, decryptSecrets),
            thirdCardExpiry = safeString(all, "third_card_expiry", "08/28"),
            thirdCardCvc = safeDecryptedString(all, "third_card_cvc", "", failedEncrypted, decryptSecrets),
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
            localCommuneName = safeString(all, "local_commune_name_v1", ""),
            cardHubOrderJson = safeString(all, "card_hub_order_json_v1", CardHubOrder.DEFAULT_JSON),
            firstName = safeString(all, "local_first_name_v1", ""),
            lastName = safeString(all, "local_last_name_v1", ""),
            profileAvatarPath = safeString(all, "profile_avatar_path_v1", "")
        )
        preservedEncryptedBlobs = failedEncrypted
        return loaded
    }

    @Synchronized
    fun updateSettings(transform: (UserSettings) -> UserSettings) {
        when (persistenceMode) {
            PrefsPersistenceMode.GUEST_LOCAL -> {
                secretsWarmed = true
                val currentLatest = loadSettings(decryptSecrets = true)
                val finalUpdated = transform(currentLatest)
                writeGuestSettings(finalUpdated)
            }
            PrefsPersistenceMode.CLOUD_MEMORY -> {
                // Strictly in-memory: Cloud accounts never persist user settings to local SharedPreferences.
                val finalUpdated = transform(_settingsFlow.value)
                _settingsFlow.value = finalUpdated
            }
            PrefsPersistenceMode.SIGNED_OUT -> {
                val prev = _settingsFlow.value
                val finalUpdated = transform(prev)
                if (finalUpdated.language != prev.language || finalUpdated.themeMode != prev.themeMode) {
                    SessionMetaStore.writeAuthUiPreferences(
                        context = appContext,
                        language = finalUpdated.language,
                        themeMode = finalUpdated.themeMode
                    )
                }
                _settingsFlow.value = finalUpdated
            }
        }
    }

    private fun writeGuestSettings(finalUpdated: UserSettings) {
        val preserved = preservedEncryptedBlobs
        isSelfUpdating = true
        try {
            guestPrefs.edit()
                .putString("salary_amount", finalUpdated.salaryAmount.toString())
                .putInt("salary_payday", finalUpdated.salaryPayday)
                .putString("salary_employer", finalUpdated.salaryEmployer)
                .putBoolean("salary_auto_credit", finalUpdated.salaryAutoCredit)
                .putString("last_credited_month", finalUpdated.lastCreditedMonthYear)
                .remove("pattern_lock_enabled")
                .remove("pattern_lock_hash")
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
                .putString(
                    "classic_card_number",
                    encodeSensitiveOrPreserve("classic_card_number", finalUpdated.classicCardNumber, "", preserved)
                )
                .putString("classic_card_expiry", finalUpdated.classicCardExpiry)
                .putString(
                    "classic_card_cvc",
                    encodeSensitiveOrPreserve("classic_card_cvc", finalUpdated.classicCardCvc, "", preserved)
                )
                .putBoolean("breathing_glow_enabled", finalUpdated.breathingGlowEnabled)
                .putBoolean("screenshot_protection_enabled", finalUpdated.screenshotProtectionEnabled)
                .putString(
                    "vault_keys_json",
                    encodeSensitiveOrPreserve("vault_keys_json", finalUpdated.vaultKeysJson, "[]", preserved)
                )
                .putString(
                    "ccp_accounts_json",
                    encodeSensitiveOrPreserve("ccp_accounts_json", finalUpdated.ccpAccountsJson, "[]", preserved)
                )
                .putString("visa_card_provider", finalUpdated.visaCardProvider)
                .putString("visa_card_holder", finalUpdated.visaCardHolderName)
                .putString(
                    "visa_card_number",
                    encodeSensitiveOrPreserve("visa_card_number", finalUpdated.visaCardNumber, "", preserved)
                )
                .putString("visa_card_expiry", finalUpdated.visaCardExpiry)
                .putString(
                    "visa_card_cvc",
                    encodeSensitiveOrPreserve("visa_card_cvc", finalUpdated.visaCardCvc, "", preserved)
                )
                .putBoolean("third_card_enabled", finalUpdated.thirdCardEnabled)
                .putString("third_card_provider", finalUpdated.thirdCardProvider)
                .putString("third_card_holder", finalUpdated.thirdCardHolderName)
                .putString(
                    "third_card_number",
                    encodeSensitiveOrPreserve("third_card_number", finalUpdated.thirdCardNumber, "", preserved)
                )
                .putString("third_card_expiry", finalUpdated.thirdCardExpiry)
                .putString(
                    "third_card_cvc",
                    encodeSensitiveOrPreserve("third_card_cvc", finalUpdated.thirdCardCvc, "", preserved)
                )
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
                .putString("card_hub_order_json_v1", finalUpdated.cardHubOrderJson)
                .putString("local_first_name_v1", finalUpdated.firstName)
                .putString("local_last_name_v1", finalUpdated.lastName)
                .putString("profile_avatar_path_v1", finalUpdated.profileAvatarPath)
                .apply()
            _settingsFlow.value = finalUpdated
        } finally {
            isSelfUpdating = false
        }
    }

    companion object {
        private const val GUEST_PREFS_NAME = "vaultix_guest_prefs"

        fun readLanguageFast(context: Context): String {
            return runCatching {
                val appCtx = context.applicationContext
                if (SessionMetaStore.isGuestSessionActive(appCtx) || SessionMetaStore.getSessionRealm(appCtx).isBlank()) {
                    appCtx.getSharedPreferences(GUEST_PREFS_NAME, Context.MODE_PRIVATE)
                        .getString("app_language", null)
                        ?: SessionMetaStore.readAuthUiLanguage(appCtx)
                } else {
                    SessionMetaStore.readAuthUiLanguage(appCtx)
                }
            }.getOrDefault("en")
        }
    }
}
