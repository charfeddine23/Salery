package com.example.security

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import android.util.Base64
import android.util.Log
import com.example.data.ArchivedAlgPosteSmsEntity
import com.example.data.DebtEntity
import com.example.data.TransactionEntity
import java.security.InvalidKeyException
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchProviderException
import java.security.ProviderException
import java.security.UnrecoverableKeyException
import java.util.Arrays
import javax.crypto.AEADBadTagException
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec

enum class CryptoIssueType {
    KEYSTORE_UNAVAILABLE,
    KEY_INVALIDATED,
    DECRYPTION_FAILED_OR_TAMPERED,
    CORRUPTED_DATA
}

data class CryptoIssue(
    val type: CryptoIssueType,
    val affectedScopes: Set<String> = emptySet(),
    val timestampMillis: Long = System.currentTimeMillis()
)

sealed class DecryptOutcome {
    data class Success(
        val plaintext: String,
        val isLegacyPlaintext: Boolean = false
    ) : DecryptOutcome()

    data class Failure(
        val issueType: CryptoIssueType,
        val reason: String
    ) : DecryptOutcome()
}

interface VaultKeyProvider {
    fun getOrCreateSecretKey(): SecretKey
    fun resetKey(): SecretKey
    fun isHardwareBacked(): Boolean
}

class AndroidKeystoreKeyProvider(
    private val keyAlias: String = DEFAULT_KEY_ALIAS
) : VaultKeyProvider {

    companion object {
        private const val TAG = "AndroidKeystoreProvider"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val DEFAULT_KEY_ALIAS = "vaultix_master_aes_gcm_v1"
    }

    private val lock = Any()

    override fun getOrCreateSecretKey(): SecretKey = synchronized(lock) {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(keyAlias)) {
            val existing = keyStore.getKey(keyAlias, null) as? SecretKey
            if (existing != null) return@synchronized existing
            // Corrupt/wrong entry type — throw so recovery handler surfaces cleanly
            throw KeyStoreException("Existing Keystore entry for $keyAlias is not a SecretKey")
        }
        generateNewKey(useStrongBoxFirst = true)
    }

    override fun resetKey(): SecretKey = synchronized(lock) {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(keyAlias)) {
            keyStore.deleteEntry(keyAlias)
        }
        generateNewKey(useStrongBoxFirst = true)
    }

    override fun isHardwareBacked(): Boolean {
        return try {
            val key = getOrCreateSecretKey()
            val factory = SecretKeyFactory.getInstance(key.algorithm, ANDROID_KEYSTORE)
            val keyInfo = factory.getKeySpec(key, KeyInfo::class.java) as? KeyInfo ?: return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                keyInfo.securityLevel == KeyInfo.SECURITY_LEVEL_STRONGBOX ||
                    keyInfo.securityLevel == KeyInfo.SECURITY_LEVEL_TRUSTED_ENVIRONMENT
            } else {
                @Suppress("DEPRECATION")
                keyInfo.isInsideSecureHardware
            }
        } catch (_: Throwable) {
            false
        }
    }

    private fun generateNewKey(useStrongBoxFirst: Boolean): SecretKey {
        if (useStrongBoxFirst && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                return generateWithSpec(strongBox = true)
            } catch (e: StrongBoxUnavailableException) {
                Log.w(TAG, "StrongBox unavailable; falling back to TEE AndroidKeyStore", e)
            } catch (e: ProviderException) {
                Log.w(TAG, "StrongBox provider error; falling back to TEE AndroidKeyStore", e)
            }
        }
        return generateWithSpec(strongBox = false)
    }

    private fun generateWithSpec(strongBox: Boolean): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        val builder = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)

        if (strongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setIsStrongBoxBacked(true)
        }

        keyGenerator.init(builder.build())
        return keyGenerator.generateKey()
    }
}

class InMemoryTestKeyProvider : VaultKeyProvider {
    private val lock = Any()
    @Volatile
    private var secretKey: SecretKey = createKey()

    private fun createKey(): SecretKey {
        val generator = KeyGenerator.getInstance("AES")
        generator.init(256)
        return generator.generateKey()
    }

    override fun getOrCreateSecretKey(): SecretKey = synchronized(lock) { secretKey }

    override fun resetKey(): SecretKey = synchronized(lock) {
        secretKey = createKey()
        secretKey
    }

    override fun isHardwareBacked(): Boolean = false
}

class VaultCryptoManager(
    private val keyProvider: VaultKeyProvider
) {
    companion object {
        private const val TAG = "VaultCryptoManager"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS = 128
        private const val MIN_IV_BYTES = 12
        private const val MIN_GCM_TAG_BYTES = 16
        const val ENVELOPE_PREFIX = "VXENC:v1:"

        // Domain-bound AAD constants for Room tables
        const val AAD_ROOM_TX_TITLE = "room:transactions.title"
        const val AAD_ROOM_TX_NOTE = "room:transactions.note"
        const val AAD_ROOM_DEBT_PERSON = "room:debts.personName"
        const val AAD_ROOM_DEBT_NOTE = "room:debts.note"
        const val AAD_ROOM_SMS_BODY = "room:archived_alg_post_sms.rawBody"

        @Volatile
        private var sharedInstance: VaultCryptoManager? = null

        @Volatile
        private var robolectricTestProvider: InMemoryTestKeyProvider? = null

        private fun isRobolectricRuntime(): Boolean {
            return "robolectric".equals(Build.FINGERPRINT, ignoreCase = true)
        }

        fun getInstance(@Suppress("UNUSED_PARAMETER") context: Context): VaultCryptoManager {
            sharedInstance?.let { return it }
            return synchronized(this) {
                sharedInstance ?: run {
                    val provider: VaultKeyProvider = if (isRobolectricRuntime()) {
                        robolectricTestProvider ?: InMemoryTestKeyProvider().also {
                            robolectricTestProvider = it
                        }
                    } else {
                        AndroidKeystoreKeyProvider()
                    }
                    VaultCryptoManager(provider).also { sharedInstance = it }
                }
            }
        }
    }

    private val keyLock = Any()

    fun isEncryptedEnvelope(value: String): Boolean = value.startsWith(ENVELOPE_PREFIX)

    fun isHardwareBacked(): Boolean = keyProvider.isHardwareBacked()

    /**
     * Encrypts [plaintext] using AES-256-GCM with domain-bound Associated Authenticated Data ([aad])
     * and a fresh random IV generated by the key provider's CSPRNG.
     *
     * Returns null (without throwing or falling back to plaintext) if encryption fails.
     */
    fun encrypt(plaintext: String, aad: String): String? {
        val plainBytes = plaintext.toByteArray(Charsets.UTF_8)
        val aadBytes = aad.toByteArray(Charsets.UTF_8)
        return try {
            val key = synchronized(keyLock) { keyProvider.getOrCreateSecretKey() }
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            cipher.updateAAD(aadBytes)
            val cipherBytes = cipher.doFinal(plainBytes)
            val iv = cipher.iv
            if (iv == null || iv.size < MIN_IV_BYTES) {
                Log.e(TAG, "Cipher produced invalid IV length: ${iv?.size}")
                return null
            }
            val ivB64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            val ctB64 = Base64.encodeToString(cipherBytes, Base64.NO_WRAP)
            SecureMemoryUtils.wipeBytes(cipherBytes)
            "$ENVELOPE_PREFIX$ivB64:$ctB64"
        } catch (t: Throwable) {
            Log.e(TAG, "Encryption failed for scope '$aad'", t)
            null
        } finally {
            SecureMemoryUtils.wipeBytes(plainBytes)
            SecureMemoryUtils.wipeBytes(aadBytes)
        }
    }

    /**
     * Decrypts [stored] if it uses the [ENVELOPE_PREFIX] format, or returns [DecryptOutcome.Success]
     * with `isLegacyPlaintext = true` for unencrypted legacy records so migrations are lossless.
     * Never throws on corrupted or tampered data.
     */
    fun decrypt(stored: String, aad: String): DecryptOutcome {
        if (!stored.startsWith(ENVELOPE_PREFIX)) {
            return DecryptOutcome.Success(plaintext = stored, isLegacyPlaintext = true)
        }

        val payload = stored.removePrefix(ENVELOPE_PREFIX)
        val sepIndex = payload.indexOf(':')
        if (sepIndex <= 0 || sepIndex >= payload.lastIndex) {
            return DecryptOutcome.Failure(
                issueType = CryptoIssueType.CORRUPTED_DATA,
                reason = "Malformed encrypted envelope"
            )
        }

        val ivB64 = payload.substring(0, sepIndex)
        val ctB64 = payload.substring(sepIndex + 1)
        if (ctB64.contains(':')) {
            return DecryptOutcome.Failure(
                issueType = CryptoIssueType.CORRUPTED_DATA,
                reason = "Unexpected envelope segments"
            )
        }

        val ivBytes: ByteArray
        val cipherBytes: ByteArray
        try {
            ivBytes = Base64.decode(ivB64, Base64.NO_WRAP)
            cipherBytes = Base64.decode(ctB64, Base64.NO_WRAP)
        } catch (e: IllegalArgumentException) {
            return DecryptOutcome.Failure(
                issueType = CryptoIssueType.CORRUPTED_DATA,
                reason = e.message ?: "Invalid Base64 envelope"
            )
        }

        if (ivBytes.size < MIN_IV_BYTES || cipherBytes.size < MIN_GCM_TAG_BYTES) {
            SecureMemoryUtils.wipeBytes(ivBytes)
            SecureMemoryUtils.wipeBytes(cipherBytes)
            return DecryptOutcome.Failure(
                issueType = CryptoIssueType.CORRUPTED_DATA,
                reason = "Truncated ciphertext or IV"
            )
        }

        val aadBytes = aad.toByteArray(Charsets.UTF_8)
        var plainBytes: ByteArray? = null
        return try {
            val key = synchronized(keyLock) { keyProvider.getOrCreateSecretKey() }
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, ivBytes))
            cipher.updateAAD(aadBytes)
            plainBytes = cipher.doFinal(cipherBytes)
            DecryptOutcome.Success(
                plaintext = String(plainBytes, Charsets.UTF_8),
                isLegacyPlaintext = false
            )
        } catch (e: AEADBadTagException) {
            Log.w(TAG, "AEAD tag verification failed for '$aad'", e)
            DecryptOutcome.Failure(
                issueType = CryptoIssueType.DECRYPTION_FAILED_OR_TAMPERED,
                reason = e.message ?: "Authentication tag mismatch"
            )
        } catch (e: KeyPermanentlyInvalidatedException) {
            Log.e(TAG, "Keystore key permanently invalidated", e)
            DecryptOutcome.Failure(
                issueType = CryptoIssueType.KEY_INVALIDATED,
                reason = e.message ?: "Key invalidated"
            )
        } catch (e: UnrecoverableKeyException) {
            Log.e(TAG, "Keystore key unrecoverable", e)
            DecryptOutcome.Failure(
                issueType = CryptoIssueType.KEYSTORE_UNAVAILABLE,
                reason = e.message ?: "Key unrecoverable"
            )
        } catch (e: KeyStoreException) {
            Log.e(TAG, "Keystore exception", e)
            DecryptOutcome.Failure(
                issueType = CryptoIssueType.KEYSTORE_UNAVAILABLE,
                reason = e.message ?: "Keystore unavailable"
            )
        } catch (e: NoSuchProviderException) {
            Log.e(TAG, "Keystore provider missing", e)
            DecryptOutcome.Failure(
                issueType = CryptoIssueType.KEYSTORE_UNAVAILABLE,
                reason = e.message ?: "Keystore provider missing"
            )
        } catch (e: InvalidKeyException) {
            Log.e(TAG, "Invalid key for decryption", e)
            DecryptOutcome.Failure(
                issueType = CryptoIssueType.KEY_INVALIDATED,
                reason = e.message ?: "Invalid key"
            )
        } catch (e: ProviderException) {
            Log.e(TAG, "Hardware provider error", e)
            DecryptOutcome.Failure(
                issueType = CryptoIssueType.KEYSTORE_UNAVAILABLE,
                reason = e.message ?: "Security provider error"
            )
        } catch (e: BadPaddingException) {
            DecryptOutcome.Failure(
                issueType = CryptoIssueType.CORRUPTED_DATA,
                reason = e.message ?: "Corrupted ciphertext"
            )
        } catch (e: IllegalBlockSizeException) {
            DecryptOutcome.Failure(
                issueType = CryptoIssueType.CORRUPTED_DATA,
                reason = e.message ?: "Invalid block size"
            )
        } catch (t: Throwable) {
            Log.e(TAG, "Unexpected decryption error for '$aad'", t)
            DecryptOutcome.Failure(
                issueType = CryptoIssueType.DECRYPTION_FAILED_OR_TAMPERED,
                reason = t.message ?: "Decryption failed"
            )
        } finally {
            SecureMemoryUtils.wipeBytes(ivBytes)
            SecureMemoryUtils.wipeBytes(cipherBytes)
            SecureMemoryUtils.wipeBytes(aadBytes)
            SecureMemoryUtils.wipeBytes(plainBytes)
        }
    }

    fun resetMasterKey(): Boolean = synchronized(keyLock) {
        return try {
            keyProvider.resetKey()
            true
        } catch (t: Throwable) {
            Log.e(TAG, "Failed resetting master key", t)
            false
        }
    }

    fun encryptFieldIfNonBlank(
        value: String,
        aad: String,
        onFailure: ((CryptoIssueType, String) -> Unit)? = null
    ): String {
        if (value.isBlank() || isEncryptedEnvelope(value)) return value
        val encrypted = encrypt(value, aad)
        if (encrypted == null) {
            onFailure?.invoke(CryptoIssueType.KEYSTORE_UNAVAILABLE, aad)
            return value
        }
        return encrypted
    }

    fun decryptField(stored: String, aad: String, fallbackOnFailure: String = ""): DecryptOutcome {
        if (stored.isBlank()) return DecryptOutcome.Success("", isLegacyPlaintext = false)
        return when (val out = decrypt(stored, aad)) {
            is DecryptOutcome.Success -> out
            is DecryptOutcome.Failure -> if (fallbackOnFailure.isEmpty()) out else DecryptOutcome.Failure(out.issueType, out.reason)
        }
    }

    fun encryptTransaction(
        tx: TransactionEntity,
        onFailure: ((CryptoIssueType, String) -> Unit)? = null
    ): TransactionEntity {
        return tx.copy(
            title = encryptFieldIfNonBlank(tx.title, AAD_ROOM_TX_TITLE, onFailure),
            note = encryptFieldIfNonBlank(tx.note, AAD_ROOM_TX_NOTE, onFailure)
        )
    }

    fun decryptTransaction(tx: TransactionEntity, onFailure: ((CryptoIssueType, String) -> Unit)? = null): TransactionEntity {
        val titleOut = decryptField(tx.title, AAD_ROOM_TX_TITLE)
        val noteOut = decryptField(tx.note, AAD_ROOM_TX_NOTE)
        val decryptedTitle = when (titleOut) {
            is DecryptOutcome.Success -> titleOut.plaintext
            is DecryptOutcome.Failure -> {
                onFailure?.invoke(titleOut.issueType, AAD_ROOM_TX_TITLE)
                ""
            }
        }
        val decryptedNote = when (noteOut) {
            is DecryptOutcome.Success -> noteOut.plaintext
            is DecryptOutcome.Failure -> {
                onFailure?.invoke(noteOut.issueType, AAD_ROOM_TX_NOTE)
                ""
            }
        }
        return if (decryptedTitle == tx.title && decryptedNote == tx.note) tx
        else tx.copy(title = decryptedTitle, note = decryptedNote)
    }

    fun encryptDebt(
        debt: DebtEntity,
        onFailure: ((CryptoIssueType, String) -> Unit)? = null
    ): DebtEntity {
        return debt.copy(
            personName = encryptFieldIfNonBlank(debt.personName, AAD_ROOM_DEBT_PERSON, onFailure),
            note = encryptFieldIfNonBlank(debt.note, AAD_ROOM_DEBT_NOTE, onFailure)
        )
    }

    fun decryptDebt(debt: DebtEntity, onFailure: ((CryptoIssueType, String) -> Unit)? = null): DebtEntity {
        val personOut = decryptField(debt.personName, AAD_ROOM_DEBT_PERSON)
        val noteOut = decryptField(debt.note, AAD_ROOM_DEBT_NOTE)
        val decryptedPerson = when (personOut) {
            is DecryptOutcome.Success -> personOut.plaintext
            is DecryptOutcome.Failure -> {
                onFailure?.invoke(personOut.issueType, AAD_ROOM_DEBT_PERSON)
                ""
            }
        }
        val decryptedNote = when (noteOut) {
            is DecryptOutcome.Success -> noteOut.plaintext
            is DecryptOutcome.Failure -> {
                onFailure?.invoke(noteOut.issueType, AAD_ROOM_DEBT_NOTE)
                ""
            }
        }
        return if (decryptedPerson == debt.personName && decryptedNote == debt.note) debt
        else debt.copy(personName = decryptedPerson, note = decryptedNote)
    }

    fun encryptArchivedSms(
        sms: ArchivedAlgPosteSmsEntity,
        onFailure: ((CryptoIssueType, String) -> Unit)? = null
    ): ArchivedAlgPosteSmsEntity {
        return sms.copy(
            rawBody = encryptFieldIfNonBlank(sms.rawBody, AAD_ROOM_SMS_BODY, onFailure)
        )
    }

    fun decryptArchivedSms(sms: ArchivedAlgPosteSmsEntity, onFailure: ((CryptoIssueType, String) -> Unit)? = null): ArchivedAlgPosteSmsEntity {
        val bodyOut = decryptField(sms.rawBody, AAD_ROOM_SMS_BODY)
        val decryptedBody = when (bodyOut) {
            is DecryptOutcome.Success -> bodyOut.plaintext
            is DecryptOutcome.Failure -> {
                onFailure?.invoke(bodyOut.issueType, AAD_ROOM_SMS_BODY)
                ""
            }
        }
        return if (decryptedBody == sms.rawBody) sms else sms.copy(rawBody = decryptedBody)
    }
}

object SecureMemoryUtils {
    private const val EXTRA_IS_SENSITIVE_COMPAT = "android.content.extra.IS_SENSITIVE"

    /**
     * Copies [text] to the system clipboard and marks it sensitive so Android 13+ masks it
     * in the clipboard preview overlay and password managers/recorders treat it as confidential.
     */
    fun copySensitiveText(context: Context, label: String, text: String) {
        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        val clip = ClipData.newPlainText(label, text)
        val extras = PersistableBundle().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            } else {
                putBoolean(EXTRA_IS_SENSITIVE_COMPAT, true)
            }
        }
        clip.description.extras = extras
        cb.setPrimaryClip(clip)
    }

    fun wipeChars(chars: CharArray?) {
        if (chars != null) {
            Arrays.fill(chars, '\u0000')
        }
    }

    fun wipeBytes(bytes: ByteArray?) {
        if (bytes != null) {
            Arrays.fill(bytes, 0.toByte())
        }
    }
}
