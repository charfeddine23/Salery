package com.example.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

fun List<Int>.toPatternHash(): String = joinToString("-")

/**
 * Cryptographic storage & credential-hashing helpers for Vaultix:
 * - Encrypts sensitive local credentials (Vault keys, CCP profiles, card PAN/CVC) at rest
 *   using Android Keystore AES-256-GCM when available, with seamless backward-compatibility
 *   for legacy plaintext values created before encryption was enabled.
 * - Salted SHA-256 hashing with constant-time comparison for unlock pattern verification.
 */
object VaultCryptoManager {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "SaleryPulseVaultAesKey_v1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128
    private const val GCM_IV_BYTES = 12
    private const val ENC_PREFIX = "v1_gcm$"
    private const val HASH_PREFIX = "sha256$"

    private val secureRandom = SecureRandom()

    @Volatile
    private var cachedAesKey: SecretKey? = null

    fun encryptSensitive(plaintext: String): String {
        if (plaintext.isEmpty()) return ""
        if (plaintext.startsWith(ENC_PREFIX)) return plaintext
        val secretKey = getOrCreateAesKey() ?: return plaintext
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val ciphertext = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))
            val ivB64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            val ctB64 = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
            "$ENC_PREFIX$ivB64$$ctB64"
        } catch (_: Throwable) {
            plaintext
        }
    }

    fun decryptSensitive(stored: String): String {
        if (stored.isEmpty()) return ""
        if (!stored.startsWith(ENC_PREFIX)) return stored // Legacy plaintext migration compatibility
        val payload = stored.removePrefix(ENC_PREFIX)
        val parts = payload.split('$')
        if (parts.size != 2) return ""
        val secretKey = getOrCreateAesKey() ?: return ""
        return try {
            val iv = Base64.decode(parts[0], Base64.DEFAULT)
            if (iv.size != GCM_IV_BYTES) return ""
            val ct = Base64.decode(parts[1], Base64.DEFAULT)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(ct), StandardCharsets.UTF_8)
        } catch (_: Throwable) {
            ""
        }
    }

    private fun getOrCreateAesKey(): SecretKey? {
        cachedAesKey?.let { return it }
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let {
                cachedAesKey = it
                return it
            }

            val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGen.init(spec)
            keyGen.generateKey().also { cachedAesKey = it }
        } catch (_: Throwable) {
            // Headless JVM unit test environments without AndroidKeyStore safely return null
            null
        }
    }

    fun isSaltedPatternHash(storedHash: String): Boolean = storedHash.startsWith(HASH_PREFIX)

    fun hashPattern(rawPattern: String): String {
        if (rawPattern.isBlank()) return ""
        val salt = ByteArray(16)
        secureRandom.nextBytes(salt)
        val digest = sha256PatternBytes(salt, rawPattern)
        return "$HASH_PREFIX${salt.toHex()}$${digest.toHex()}"
    }

    fun verifyPattern(rawPattern: String, storedHash: String): Boolean {
        if (rawPattern.isBlank() || storedHash.isBlank()) return false
        if (!storedHash.startsWith(HASH_PREFIX)) {
            // Constant-time check for legacy raw stored patterns ("0-1-2-5-8")
            val candidateBytes = rawPattern.toByteArray(StandardCharsets.UTF_8)
            val storedBytes = storedHash.toByteArray(StandardCharsets.UTF_8)
            return MessageDigest.isEqual(candidateBytes, storedBytes)
        }
        val payload = storedHash.removePrefix(HASH_PREFIX)
        val parts = payload.split('$')
        if (parts.size != 2) return false
        val salt = parts[0].fromHexOrNull() ?: return false
        val expectedDigest = parts[1].fromHexOrNull() ?: return false
        val actualDigest = sha256PatternBytes(salt, rawPattern)
        return MessageDigest.isEqual(actualDigest, expectedDigest)
    }

    private fun sha256PatternBytes(salt: ByteArray, rawPattern: String): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        md.update("VaultixPatternV1:".toByteArray(StandardCharsets.UTF_8))
        md.update(salt)
        md.update(rawPattern.toByteArray(StandardCharsets.UTF_8))
        return md.digest()
    }

    fun sha256Hex(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(input.toByteArray(StandardCharsets.UTF_8)).toHex()
    }

    fun isEncryptedBackupEnvelope(rawText: String): Boolean {
        val trimmed = rawText.trim()
        if (!trimmed.startsWith("{")) return false
        return try {
            org.json.JSONObject(trimmed).optString("vaultixBackupFormat") == BACKUP_FORMAT_ID
        } catch (_: Throwable) {
            false
        }
    }

    fun encryptBackupWithPassphrase(plaintextJson: String, passphrase: String): String {
        require(passphrase.isNotBlank()) { "Passphrase must not be blank" }
        val salt = ByteArray(BACKUP_SALT_BYTES)
        val iv = ByteArray(GCM_IV_BYTES)
        secureRandom.nextBytes(salt)
        secureRandom.nextBytes(iv)

        val key = deriveBackupKey(passphrase, salt, BACKUP_KDF_ITERATIONS)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.updateAAD(BACKUP_AAD.toByteArray(StandardCharsets.UTF_8))
        val ciphertext = cipher.doFinal(plaintextJson.toByteArray(StandardCharsets.UTF_8))

        return org.json.JSONObject().apply {
            put("vaultixBackupFormat", BACKUP_FORMAT_ID)
            put("kdf", BACKUP_KDF_ALGO)
            put("iterations", BACKUP_KDF_ITERATIONS)
            put("saltB64", Base64.encodeToString(salt, Base64.NO_WRAP))
            put("ivB64", Base64.encodeToString(iv, Base64.NO_WRAP))
            put("ciphertextB64", Base64.encodeToString(ciphertext, Base64.NO_WRAP))
        }.toString(2)
    }

    fun decryptBackupWithPassphrase(envelopeJson: String, passphrase: String): String? {
        if (passphrase.isBlank()) return null
        val trimmed = envelopeJson.trim()
        if (trimmed.isEmpty() || trimmed.length > MAX_ENCRYPTED_ENVELOPE_CHARS) return null
        return try {
            val root = org.json.JSONObject(trimmed)
            if (root.optString("vaultixBackupFormat") != BACKUP_FORMAT_ID) return null
            val kdf = root.optString("kdf")
            if (kdf != BACKUP_KDF_ALGO) return null
            val iterations = root.optInt("iterations", 0)
            if (iterations !in 10_000..1_000_000) return null

            val salt = Base64.decode(root.getString("saltB64"), Base64.DEFAULT)
            val iv = Base64.decode(root.getString("ivB64"), Base64.DEFAULT)
            val ciphertext = Base64.decode(root.getString("ciphertextB64"), Base64.DEFAULT)
            if (salt.size != BACKUP_SALT_BYTES || iv.size != GCM_IV_BYTES || ciphertext.isEmpty()) {
                return null
            }

            val key = deriveBackupKey(passphrase, salt, iterations)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            cipher.updateAAD(BACKUP_AAD.toByteArray(StandardCharsets.UTF_8))
            String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8)
        } catch (_: Throwable) {
            null
        }
    }

    private fun deriveBackupKey(
        passphrase: String,
        salt: ByteArray,
        iterations: Int
    ): javax.crypto.spec.SecretKeySpec {
        val factory = javax.crypto.SecretKeyFactory.getInstance(BACKUP_KDF_ALGO)
        val spec = javax.crypto.spec.PBEKeySpec(
            passphrase.toCharArray(),
            salt,
            iterations,
            BACKUP_KEY_BITS
        )
        val keyBytes = try {
            factory.generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
        return javax.crypto.spec.SecretKeySpec(keyBytes, "AES")
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.fromHexOrNull(): ByteArray? {
        if (length % 2 != 0) return null
        return try {
            ByteArray(length / 2) { idx ->
                substring(idx * 2, idx * 2 + 2).toInt(16).toByte()
            }
        } catch (_: NumberFormatException) {
            null
        }
    }

    private const val BACKUP_FORMAT_ID = "VAULTIX_ENC_V1"
    private const val BACKUP_KDF_ALGO = "PBKDF2WithHmacSHA256"
    private const val BACKUP_KDF_ITERATIONS = 120_000
    private const val BACKUP_SALT_BYTES = 16
    private const val BACKUP_KEY_BITS = 256
    private const val BACKUP_AAD = "VaultixBackupV1"
    private const val MAX_ENCRYPTED_ENVELOPE_CHARS = 14 * 1024 * 1024
}
