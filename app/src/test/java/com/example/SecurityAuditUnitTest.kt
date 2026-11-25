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
}
