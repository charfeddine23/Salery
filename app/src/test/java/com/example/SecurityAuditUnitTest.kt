package com.example

import com.example.data.BackupPayload
import com.example.data.DebtEntity
import com.example.data.DebtType
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.security.VaultCryptoManager
import com.example.update.AppUpdateManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SecurityAuditUnitTest {

    @Test
    fun patternHash_usesUniqueSaltAndVerifiesConstantTime() {
        val hashA = VaultCryptoManager.hashPattern("0-1-2-5-8")
        val hashB = VaultCryptoManager.hashPattern("0-1-2-5-8")

        assertTrue(VaultCryptoManager.isSaltedPatternHash(hashA))
        assertNotEquals(hashA, hashB)
        assertTrue(VaultCryptoManager.verifyPattern("0-1-2-5-8", hashA))
        assertTrue(VaultCryptoManager.verifyPattern("0-1-2-5-8", hashB))
        assertFalse(VaultCryptoManager.verifyPattern("0-1-2-4-8", hashA))
    }

    @Test
    fun patternVerification_supportsLegacyUnsaltedPatternForUpgradeMigration() {
        assertFalse(VaultCryptoManager.isSaltedPatternHash("0-1-2-5-8"))
        assertTrue(VaultCryptoManager.verifyPattern("0-1-2-5-8", "0-1-2-5-8"))
        assertFalse(VaultCryptoManager.verifyPattern("0-1-2-5-7", "0-1-2-5-8"))
    }

    @Test
    fun backupExport_redactsCardNumberAndCvcFromPlaintextJson() {
        val payload = BackupPayload(
            exportTimestamp = 1_763_200_000_000L,
            appVersion = "1.0.3",
            developer = "Charfeddine Ka",
            transactions = listOf(
                TransactionEntity(
                    type = TransactionType.EXPENSE,
                    amount = 2500.0,
                    category = "food",
                    title = "Lunch",
                    note = "",
                    dateMillis = 1_763_200_000_000L
                )
            ),
            debts = emptyList(),
            salaryAmount = 85000.0,
            salaryPayday = 25,
            salaryEmployer = "Algiers Corp",
            classicCardHolderName = "CHARFEDDINE KA",
            classicCardNumber = "6280123456789012",
            classicCardExpiry = "11/29",
            classicCardCvc = "987"
        )

        val exportedJson = payload.toJsonString()
        assertFalse(exportedJson.contains("6280123456789012"))
        assertFalse(exportedJson.contains("\"cvc\": \"987\""))

        val parsed = BackupPayload.fromJsonString(exportedJson)
        assertNotNull(parsed)
        assertEquals("", parsed!!.classicCardNumber)
        assertEquals("", parsed.classicCardCvc)
        assertEquals("CHARFEDDINE KA", parsed.classicCardHolderName)
        assertEquals(1, parsed.transactions.size)
    }

    @Test
    fun backupRestore_rejectsOversizedPayloadsAndNegativeAmounts() {
        val hugeJson = "{ \"a\": \"" + "x".repeat(10 * 1024 * 1024 + 32) + "\" }"
        assertNull(BackupPayload.fromJsonString(hugeJson))

        val negativeJson = """
            {
              "salarySettings": { "amount": -500.0, "payday": 99, "employer": "Corp" },
              "transactions": [
                { "type": "EXPENSE", "amount": -100.0, "category": "food", "title": "Bad" },
                { "type": "INCOME", "amount": 1500.0, "category": "salary", "title": "Valid" }
              ],
              "debts": [
                { "type": "OWED_BY_ME", "personName": "Invalid", "amount": -20.0, "dueDateMillis": 1000 }
              ]
            }
        """.trimIndent()
        val parsed = BackupPayload.fromJsonString(negativeJson)
        assertNotNull(parsed)
        assertEquals(85000.0, parsed!!.salaryAmount, 0.01)
        assertEquals(31, parsed.salaryPayday)
        assertEquals(1, parsed.transactions.size)
        assertEquals(1500.0, parsed.transactions[0].amount, 0.01)
        assertEquals(0, parsed.debts.size)
    }

    @Test
    fun otaUpdateUrlAllowlist_enforcesHttpsAndOfficialGitHubHostsOnly() {
        assertTrue(AppUpdateManager.isTrustedUpdateUrl("https://github.com/charfeddine23/Salery/releases/download/v1.0.5/app.apk"))
        assertTrue(AppUpdateManager.isTrustedUpdateUrl("https://objects.githubusercontent.com/github-production-release-asset/app.apk"))
        assertTrue(AppUpdateManager.isTrustedUpdateUrl("https://release-assets.githubusercontent.com/app.apk"))

        assertFalse(AppUpdateManager.isTrustedUpdateUrl("http://github.com/charfeddine23/Salery/app.apk"))
        assertFalse(AppUpdateManager.isTrustedUpdateUrl("https://github.com.evil.example/app.apk"))
        assertFalse(AppUpdateManager.isTrustedUpdateUrl("https://evil-githubusercontent.com/app.apk"))
        assertFalse(AppUpdateManager.isTrustedUpdateUrl("http://169.254.169.254/latest/meta-data/"))
        assertFalse(AppUpdateManager.isTrustedUpdateUrl("file:///sdcard/Download/update.apk"))
        assertFalse(AppUpdateManager.isTrustedUpdateUrl(""))
    }

    @Test
    fun cryptoHelper_preservesEmptyAndLegacyValuesSafelyOnJvm() {
        assertEquals("", VaultCryptoManager.encryptSensitive(""))
        assertEquals("", VaultCryptoManager.decryptSensitive(""))
        assertEquals("[{\"id\":\"k1\"}]", VaultCryptoManager.decryptSensitive("[{\"id\":\"k1\"}]"))
        assertEquals(30, com.example.data.UserSettings().autoLockTimeoutSeconds)
    }

    @Test
    fun encryptedBackupEnvelope_roundTripsWithCorrectPassphrase_andRejectsWrongOrTampered() {
        val payload = BackupPayload(
            exportTimestamp = 1_763_200_000_000L,
            appVersion = "1.0.3",
            developer = "Charfeddine Ka",
            transactions = emptyList(),
            debts = emptyList(),
            salaryAmount = 95000.0,
            salaryPayday = 20,
            salaryEmployer = "Algiers Tech",
            classicCardHolderName = "CHARFEDDINE KA",
            classicCardNumber = "6280998877665544",
            classicCardExpiry = "12/30",
            classicCardCvc = "456"
        )

        val plainWithSecrets = payload.toJsonString(includeSensitiveCardFields = true)
        val envelope = VaultCryptoManager.encryptBackupWithPassphrase(plainWithSecrets, "StrongVaultPass#2026")

        assertTrue(VaultCryptoManager.isEncryptedBackupEnvelope(envelope))
        assertFalse(VaultCryptoManager.isEncryptedBackupEnvelope(plainWithSecrets))
        assertFalse(envelope.contains("6280998877665544"))
        assertFalse(envelope.contains("Algiers Tech"))

        // Wrong password returns null
        assertNull(VaultCryptoManager.decryptBackupWithPassphrase(envelope, "WrongPassword"))
        assertNull(VaultCryptoManager.decryptBackupWithPassphrase(envelope, ""))

        // Tampered ciphertext returns null (AES-GCM authentication fails)
        val tampered = envelope.replaceFirst("\"ciphertextB64\": \"", "\"ciphertextB64\": \"AA")
        assertNull(VaultCryptoManager.decryptBackupWithPassphrase(tampered, "StrongVaultPass#2026"))

        // Correct password restores full payload including encrypted card details
        val decrypted = VaultCryptoManager.decryptBackupWithPassphrase(envelope, "StrongVaultPass#2026")
        assertNotNull(decrypted)
        val restored = BackupPayload.fromJsonString(decrypted!!)
        assertNotNull(restored)
        assertEquals("6280998877665544", restored!!.classicCardNumber)
        assertEquals("456", restored.classicCardCvc)
        assertEquals("Algiers Tech", restored.salaryEmployer)
        assertEquals(
            VaultCryptoManager.sha256Hex("secret-copy"),
            VaultCryptoManager.sha256Hex("secret-copy")
        )
    }

    @Test
    fun stepUpAuth_failsClosedWhenBiometricRequiredAndHostActivityUnavailable() {
        val appContext = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        var verified = false
        var rejected = false

        com.example.security.StepUpAuthHelper.verifyIfEnabled(
            context = appContext,
            requireBiometric = true,
            title = "Verify",
            subtitle = "Confirm",
            cancelText = "Cancel",
            onVerified = { verified = true },
            onRejected = { rejected = true }
        )

        assertFalse(verified)
        assertTrue(rejected)

        // When requireBiometric is false, action proceeds cleanly
        var unguardedVerified = false
        com.example.security.StepUpAuthHelper.verifyIfEnabled(
            context = appContext,
            requireBiometric = false,
            title = "Verify",
            subtitle = "Confirm",
            cancelText = "Cancel",
            onVerified = { unguardedVerified = true },
            onRejected = { }
        )
        assertTrue(unguardedVerified)
    }

    @Test
    fun preferencesReadLanguageFast_readsStoredLanguageWithoutFullSettingsLoad() {
        val appContext = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val prefs = com.example.data.PreferencesManager(appContext)
        prefs.updateSettings { it.copy(language = "ar") }
        assertEquals("ar", com.example.data.PreferencesManager.readLanguageFast(appContext))
        prefs.updateSettings { it.copy(language = "en") }
        assertEquals("en", com.example.data.PreferencesManager.readLanguageFast(appContext))
    }

    @Test
    fun backupRestore_skipsUnknownEnumsWithoutDiscardingValidRecords() {
        val mixedJson = """
            {
              "transactions": [
                { "type": "FUTURE_TX_TYPE", "amount": 500.0, "category": "other", "title": "Future" },
                { "type": "INCOME", "amount": 2500.0, "category": "salary", "title": "Valid Income" }
              ],
              "debts": [
                { "type": "FUTURE_DEBT_TYPE", "personName": "Unknown", "amount": 100.0 },
                { "type": "OWED_TO_ME", "personName": "Karim", "amount": 3000.0, "dueDateMillis": 2000000 }
              ]
            }
        """.trimIndent()
        val parsed = BackupPayload.fromJsonString(mixedJson)
        assertNotNull(parsed)
        assertEquals(1, parsed!!.transactions.size)
        assertEquals("Valid Income", parsed.transactions[0].title)
        assertEquals(1, parsed.debts.size)
        assertEquals("Karim", parsed.debts[0].personName)
    }

    @Test
    fun fullVaultBackup_redactsAllCardsAndVaultSecretsInPlaintext_andPreservesWhenEncrypted() {
        val payload = BackupPayload(
            exportTimestamp = 1_763_200_000_000L,
            appVersion = "1.0.5",
            developer = "Vaultix",
            transactions = emptyList(),
            debts = emptyList(),
            salaryAmount = 90000.0,
            salaryPayday = 25,
            salaryEmployer = "Algiers Corp",
            classicCardHolderName = "CARDHOLDER",
            classicCardNumber = "6280000011112222",
            classicCardExpiry = "11/29",
            classicCardCvc = "111",
            visaCardProvider = "REDOTPAY",
            visaCardHolderName = "CARDHOLDER",
            visaCardNumber = "4152000033334444",
            visaCardExpiry = "08/28",
            visaCardCvc = "222",
            thirdCardEnabled = true,
            thirdCardProvider = "BYBIT",
            thirdCardHolderName = "CARDHOLDER",
            thirdCardNumber = "5316000055556666",
            thirdCardExpiry = "09/29",
            thirdCardCvc = "333",
            vaultKeysJson = "[{\"id\":\"vk1\",\"secret\":\"super-secret-pass\"}]",
            ccpAccountsJson = "[{\"id\":\"ccp1\",\"rip\":\"00799999001234567890\"}]",
            savingsWalletBalance = 50000.0,
            localUsername = "Amine",
            localWilayaCode = "16",
            localWilayaName = "Alger",
            localCommuneName = "Hydra"
        )

        val plaintext = payload.toJsonString(includeSensitiveCardFields = false)
        assertFalse(plaintext.contains("6280000011112222"))
        assertFalse(plaintext.contains("4152000033334444"))
        assertFalse(plaintext.contains("5316000055556666"))
        assertFalse(plaintext.contains("super-secret-pass"))
        assertFalse(plaintext.contains("00799999001234567890"))
        assertTrue(plaintext.contains("Amine"))

        val plainParsed = BackupPayload.fromJsonString(plaintext)
        assertNotNull(plainParsed)
        assertEquals("", plainParsed!!.visaCardNumber)
        assertEquals("", plainParsed.thirdCardNumber)
        assertEquals("[]", plainParsed.vaultKeysJson)
        assertEquals("[]", plainParsed.ccpAccountsJson)
        assertEquals(50000.0, plainParsed.savingsWalletBalance!!, 0.01)
        assertEquals("16", plainParsed.localWilayaCode)

        val encrypted = VaultCryptoManager.encryptBackupWithPassphrase(
            payload.toJsonString(includeSensitiveCardFields = true),
            "StrongPass2026!"
        )
        val decrypted = VaultCryptoManager.decryptBackupWithPassphrase(encrypted, "StrongPass2026!")
        assertNotNull(decrypted)
        val encParsed = BackupPayload.fromJsonString(decrypted!!)
        assertNotNull(encParsed)
        assertEquals("4152000033334444", encParsed!!.visaCardNumber)
        assertEquals("222", encParsed.visaCardCvc)
        assertEquals("5316000055556666", encParsed.thirdCardNumber)
        assertTrue(encParsed.vaultKeysJson.contains("super-secret-pass"))
        assertTrue(encParsed.ccpAccountsJson.contains("00799999001234567890"))
        assertEquals("", VaultCryptoManager.decryptSensitiveOrNull(""))
    }

    @Test
    fun cloudSyncSettingsMerge_preservesLocalEncryptedSecretsAndUpdatesSafePreferences() {
        val local = com.example.data.UserSettings(
            salaryAmount = 85000.0,
            classicCardNumber = "6280111122223333",
            classicCardCvc = "321",
            visaCardNumber = "4152444455556666",
            visaCardCvc = "654",
            thirdCardNumber = "5316777788889999",
            thirdCardCvc = "987",
            vaultKeysJson = "[{\"id\":\"k1\",\"secret\":\"top-secret\"}]",
            ccpAccountsJson = "[{\"id\":\"c1\",\"rip\":\"00799999000011112222\"}]",
            latestSmsBalanceRawSnippet = "Solde: 50000 DA"
        )
        val remote = com.example.cloud.CloudVaultSettings(
            firstName = "Charfeddine",
            lastName = "Kaddouri",
            salaryAmount = 120000.0,
            salaryPayday = 27,
            salaryEmployer = "TechDZ",
            lastCreditedMonthYear = "2026-11",
            biometricEnabled = true,
            autoLockTimeoutSeconds = 60,
            hasCompletedPermissionsOnboarding = true,
            savingsWalletBalance = 35000.0,
            savingsGoalAmount = 100000.0,
            language = "ar",
            themeMode = "LIGHT",
            localUsername = "Charfeddine"
        )
        val remoteAssets = com.example.cloud.CloudVaultAssets(
            goldAssetHoldingsJson = "[{\"id\":\"g1\",\"grams\":10.0}]",
            binanceHoldingsJson = "[{\"symbol\":\"BTC\",\"amount\":0.05}]",
            binancePricesJson = "[{\"symbol\":\"BTC\",\"priceUsd\":92000.0}]",
            binanceLastPriceSyncMillis = 1_763_200_123_000L,
            squareMarketSnapshotJson = "{\"usdParallel\":245.0}",
            squareLastPriceSyncMillis = 1_763_200_456_000L,
            squareSyncIntervalMinutes = 30
        )
        val remoteSmsSync = com.example.cloud.CloudVaultSmsSync(
            baridiMobSmsSyncEnabled = true,
            latestSmsBalanceAmount = 74500.0,
            latestSmsBalanceUpdatedAt = 1_763_200_789_000L,
            smsBalanceHistoryJson = "[{\"amount\":74500.0}]",
            detectedSmsTaxesJson = "[{\"id\":\"t1\",\"amount\":10.0}]",
            autoAddTaxesEnabled = true
        )

        val merged = com.example.viewmodel.applyCloudVaultSettingsSafely(
            current = local,
            cloud = remote,
            overwritePreferences = true,
            cloudAssets = remoteAssets,
            cloudSmsSync = remoteSmsSync
        )
        assertTrue(merged.hasLocalAccount)
        assertEquals("Charfeddine", merged.firstName)
        assertEquals("Kaddouri", merged.lastName)
        assertEquals(120000.0, merged.salaryAmount, 0.01)
        assertEquals(27, merged.salaryPayday)
        assertEquals("TechDZ", merged.salaryEmployer)
        assertEquals("2026-11", merged.lastCreditedMonthYear)
        assertTrue(merged.biometricEnabled)
        assertEquals(60, merged.autoLockTimeoutSeconds)
        assertTrue(merged.hasCompletedPermissionsOnboarding)
        assertEquals(35000.0, merged.savingsWalletBalance, 0.01)
        assertEquals("ar", merged.language)
        assertEquals("LIGHT", merged.themeMode)
        assertEquals("Charfeddine", merged.localUsername)
        assertEquals("[{\"id\":\"g1\",\"grams\":10.0}]", merged.goldAssetHoldingsJson)
        assertEquals("[{\"symbol\":\"BTC\",\"amount\":0.05}]", merged.binanceHoldingsJson)
        assertEquals("[{\"symbol\":\"BTC\",\"priceUsd\":92000.0}]", merged.binancePricesJson)
        assertEquals("{\"usdParallel\":245.0}", merged.squareMarketSnapshotJson)
        assertEquals(30, merged.squareSyncIntervalMinutes)
        assertEquals(74500.0, merged.latestSmsBalanceAmount, 0.01)
        assertEquals("[{\"amount\":74500.0}]", merged.smsBalanceHistoryJson)
        assertEquals("[{\"id\":\"t1\",\"amount\":10.0}]", merged.detectedSmsTaxesJson)
        assertTrue(merged.autoAddTaxesEnabled)

        // Encrypted on-device secrets remain 100% intact
        assertEquals("6280111122223333", merged.classicCardNumber)
        assertEquals("321", merged.classicCardCvc)
        assertEquals("4152444455556666", merged.visaCardNumber)
        assertEquals("654", merged.visaCardCvc)
        assertEquals("5316777788889999", merged.thirdCardNumber)
        assertEquals("987", merged.thirdCardCvc)
        assertEquals("[{\"id\":\"k1\",\"secret\":\"top-secret\"}]", merged.vaultKeysJson)
        assertEquals("[{\"id\":\"c1\",\"rip\":\"00799999000011112222\"}]", merged.ccpAccountsJson)
        assertEquals("Solde: 50000 DA", merged.latestSmsBalanceRawSnippet)
    }

    @Test
    fun cloudSyncDedupeKeys_matchEquivalentRecordsDeterministically() {
        val txKeyA = com.example.viewmodel.buildTransactionDedupeKey(
            typeName = "EXPENSE",
            amount = 1500.0,
            category = "Food",
            title = "Lunch ",
            dateMillis = 1_763_200_000_000L
        )
        val txKeyB = com.example.viewmodel.buildTransactionDedupeKey(
            typeName = "EXPENSE",
            amount = 1500.00,
            category = "food",
            title = "lunch",
            dateMillis = 1_763_200_000_000L
        )
        assertEquals(txKeyA, txKeyB)

        val debtKeyA = com.example.viewmodel.buildDebtDedupeKey(
            typeName = "OWED_TO_ME",
            personName = "Karim ",
            amount = 4200.0,
            createdDateMillis = 1_763_100_000_000L
        )
        val debtKeyB = com.example.viewmodel.buildDebtDedupeKey(
            typeName = "OWED_TO_ME",
            personName = "karim",
            amount = 4200.0,
            createdDateMillis = 1_763_100_000_000L
        )
        assertEquals(debtKeyA, debtKeyB)
    }

    @Test
    fun cloudVaultSecretsAndText_roundTripPerUid_andRejectWrongUidOrTampering() {
        val uidA = "user_alpha_123"
        val uidB = "user_beta_999"
        val plainSecrets = """{"classicCardNumber":"6280123456789012","vaultKeysJson":"[{\"id\":\"vk1\"}]"}"""

        val secretsEnvelope = VaultCryptoManager.encryptCloudVaultSecrets(plainSecrets, uidA)
        assertTrue(VaultCryptoManager.isCloudSecretsEnvelope(secretsEnvelope))
        assertFalse(secretsEnvelope.contains("6280123456789012"))
        assertEquals(plainSecrets, VaultCryptoManager.decryptCloudVaultSecrets(secretsEnvelope, uidA))
        assertNull(VaultCryptoManager.decryptCloudVaultSecrets(secretsEnvelope, uidB))
        assertNull(VaultCryptoManager.decryptCloudVaultSecrets(secretsEnvelope.replace("a", "b"), uidA))

        val smsBody = "Votre compte CCP a ete debite de 4500.00 DA. Nouveau solde: 51200.00 DA"
        val encryptedSms = VaultCryptoManager.encryptCloudText(smsBody, uidA)
        assertFalse(encryptedSms.contains("51200.00"))
        assertEquals(smsBody, VaultCryptoManager.decryptCloudText(encryptedSms, uidA))
        assertNull(VaultCryptoManager.decryptCloudText(encryptedSms, uidB))
    }

    @Test
    fun applyCloudVaultSettingsSafely_restoresDecryptedSecretsAndOverridesAllPreferences() {
        val previous = com.example.data.UserSettings(
            firstName = "Old",
            language = "ar",
            themeMode = "LIGHT",
            designStyle = "NEUMORPHISM",
            classicCardNumber = "6280000000000000",
            vaultKeysJson = "[{\"id\":\"old_key\"}]"
        )
        val cloud = com.example.cloud.CloudVaultSettings(
            firstName = "Amine",
            lastName = "Belaid",
            language = "en",
            themeMode = "DARK",
            designStyle = "LIQUID_GLASS",
            lastBackupTimestamp = 1_764_000_000_000L
        )
        val secrets = com.example.cloud.DecryptedCloudVaultSecrets(
            classicCardNumber = "6280111122223333",
            classicCardCvc = "901",
            visaCardNumber = "4152111122223333",
            visaCardCvc = "402",
            thirdCardNumber = "5316111122223333",
            thirdCardCvc = "703",
            vaultKeysJson = "[{\"id\":\"cloud_k1\",\"secret\":\"s1\"}]",
            ccpAccountsJson = "[{\"id\":\"ccp_1\",\"rip\":\"00799999000111222333\"}]",
            latestSmsBalanceRawSnippet = "Solde: 82500.00 DA"
        )

        val restored = com.example.viewmodel.applyCloudVaultSettingsSafely(
            current = previous,
            cloud = cloud,
            overwritePreferences = true,
            cloudSecrets = secrets
        )
        assertEquals("Amine", restored.firstName)
        assertEquals("en", restored.language)
        assertEquals("DARK", restored.themeMode)
        assertEquals("LIQUID_GLASS", restored.designStyle)
        assertEquals(1_764_000_000_000L, restored.lastBackupTimestamp)
        assertEquals("6280111122223333", restored.classicCardNumber)
        assertEquals("901", restored.classicCardCvc)
        assertEquals("4152111122223333", restored.visaCardNumber)
        assertEquals("703", restored.thirdCardCvc)
        assertEquals("[{\"id\":\"cloud_k1\",\"secret\":\"s1\"}]", restored.vaultKeysJson)
        assertEquals("[{\"id\":\"ccp_1\",\"rip\":\"00799999000111222333\"}]", restored.ccpAccountsJson)
        assertEquals("Solde: 82500.00 DA", restored.latestSmsBalanceRawSnippet)
    }

    @Test
    fun preferencesManager_isolatesCloudMemoryFromGuestPrefsAndWipesOnSignOut() {
        val appContext = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        com.example.data.SessionMetaStore.setSessionRealm(appContext, com.example.data.SessionMetaStore.REALM_GUEST)
        val prefs = com.example.data.PreferencesManager(appContext)
        prefs.switchToGuestMode(activateAccount = true)
        prefs.updateSettings {
            it.copy(
                localUsername = "GuestUser",
                language = "en",
                themeMode = "DARK",
                salaryAmount = 42000.0
            )
        }

        // Switch into Cloud RAM mode with completely different settings & theme
        com.example.data.SessionMetaStore.setSessionRealm(appContext, com.example.data.SessionMetaStore.REALM_CLOUD, "cloud_uid_1")
        prefs.switchToCloudMemoryMode(
            com.example.data.UserSettings(
                hasLocalAccount = true,
                localUsername = "CloudUserA",
                language = "ar",
                themeMode = "LIGHT",
                salaryAmount = 195000.0,
                classicCardNumber = "6280999988887777"
            )
        )
        prefs.updateSettings { it.copy(savingsWalletBalance = 75000.0) }
        assertEquals("CloudUserA", prefs.settingsFlow.value.localUsername)
        assertEquals(75000.0, prefs.settingsFlow.value.savingsWalletBalance, 0.01)

        // Verify Guest SharedPreferences on disk remained completely untouched by Cloud writes
        val guestDisk = appContext.getSharedPreferences("vaultix_guest_prefs", android.content.Context.MODE_PRIVATE)
        assertEquals("GuestUser", guestDisk.getString("local_username_v1", ""))
        assertEquals("en", guestDisk.getString("app_language", ""))
        assertFalse(guestDisk.all.values.any { it.toString().contains("CloudUserA") })
        assertFalse(guestDisk.all.values.any { it.toString().contains("6280999988887777") })

        // Sign out wipes in-memory Cloud state cleanly back to clean defaults
        com.example.data.SessionMetaStore.setSessionRealm(appContext, com.example.data.SessionMetaStore.REALM_NONE)
        prefs.switchToSignedOutMode(resetAuthUiPreferences = true)
        assertFalse(prefs.settingsFlow.value.hasLocalAccount)
        assertEquals("", prefs.settingsFlow.value.localUsername)
        assertEquals("", prefs.settingsFlow.value.classicCardNumber)
        assertEquals(0.0, prefs.settingsFlow.value.savingsWalletBalance, 0.01)
        assertEquals("en", prefs.settingsFlow.value.language)
        assertEquals("DARK", prefs.settingsFlow.value.themeMode)

        // Guest data on disk stays intact for Guest mode
        prefs.switchToGuestMode()
        assertEquals("GuestUser", prefs.settingsFlow.value.localUsername)
        assertEquals(42000.0, prefs.settingsFlow.value.salaryAmount, 0.01)
    }

    @Test
    fun resolveStartupRealm_defaultsToNoneOnFreshInstallAndGuestOnlyWhenGuestAccountExists() {
        val appContext = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val metaPrefs = appContext.getSharedPreferences("vaultix_session_meta", android.content.Context.MODE_PRIVATE)
        val guestPrefs = appContext.getSharedPreferences("vaultix_guest_prefs", android.content.Context.MODE_PRIVATE)
        metaPrefs.edit().clear().commit()
        guestPrefs.edit().clear().commit()

        assertEquals(
            com.example.data.SessionMetaStore.REALM_NONE,
            com.example.data.SessionMetaStore.resolveStartupRealm(appContext)
        )

        guestPrefs.edit().putBoolean("has_local_account_v1", true).commit()
        assertEquals(
            com.example.data.SessionMetaStore.REALM_GUEST,
            com.example.data.SessionMetaStore.resolveStartupRealm(appContext)
        )

        com.example.data.SessionMetaStore.setSessionRealm(appContext, com.example.data.SessionMetaStore.REALM_CLOUD, "uid_a")
        assertEquals(
            com.example.data.SessionMetaStore.REALM_CLOUD,
            com.example.data.SessionMetaStore.resolveStartupRealm(appContext)
        )
    }

    @Test
    fun cloudSessionSignOut_immediatelyWipesCloudRamRecordsAndPreservesGuest() {
        kotlinx.coroutines.runBlocking {
            val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
            val metaPrefs = app.getSharedPreferences("vaultix_session_meta", android.content.Context.MODE_PRIVATE)
            val guestPrefs = app.getSharedPreferences("vaultix_guest_prefs", android.content.Context.MODE_PRIVATE)
            metaPrefs.edit().clear().commit()
            guestPrefs.edit().clear().commit()

            val cloudDb = com.example.data.AppDatabase.getCloudSessionDatabase(app)
            val guestDb = com.example.data.AppDatabase.getDatabase(app)
            cloudDb.appDao().clearAllTransactions()
            guestDb.appDao().clearAllTransactions()

            // Seed Guest transaction that MUST survive Cloud sign-outs
            guestDb.appDao().insertTransaction(
                TransactionEntity(
                    type = TransactionType.INCOME,
                    amount = 5000.0,
                    category = "salary",
                    title = "Guest Tx",
                    note = "",
                    dateMillis = 1_763_200_000_000L
                )
            )
            // Seed Cloud RAM transaction representing Account A
            cloudDb.appDao().insertTransaction(
                TransactionEntity(
                    type = TransactionType.EXPENSE,
                    amount = 12345.0,
                    category = "shopping",
                    title = "Account A Secret Purchase",
                    note = "",
                    dateMillis = 1_763_200_000_000L
                )
            )

            com.example.data.SessionMetaStore.setSessionRealm(
                app,
                com.example.data.SessionMetaStore.REALM_NONE
            )
            val vm = com.example.viewmodel.VaultixViewModel(app)
            // Wait briefly for ViewModel init coroutine
            kotlinx.coroutines.delay(150L)

            // Sign out to auth screen & sign out completely
            vm.signOutToAuthScreen()
            kotlinx.coroutines.delay(150L)
            assertEquals(0, vm.allTransactions.value.size)
            assertEquals(0, cloudDb.appDao().getAllTransactionsList().size)
            assertFalse(vm.settings.value.hasLocalAccount)

            // Re-seed Cloud RAM and verify signOutGoogleAccount also wipes Cloud RAM immediately
            cloudDb.appDao().insertTransaction(
                TransactionEntity(
                    type = TransactionType.EXPENSE,
                    amount = 9999.0,
                    category = "food",
                    title = "Account A Second Purchase",
                    note = "",
                    dateMillis = 1_763_200_000_000L
                )
            )
            vm.signOutGoogleAccount(app, clearQuickResume = true)
            kotlinx.coroutines.delay(150L)
            assertEquals(0, vm.allTransactions.value.size)
            assertEquals(0, cloudDb.appDao().getAllTransactionsList().size)
            assertEquals(1, guestDb.appDao().getAllTransactionsList().size)
        }
    }
}
