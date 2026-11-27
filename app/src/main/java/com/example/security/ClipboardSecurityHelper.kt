package com.example.security

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Copies sensitive credentials (passwords, CCP/RIP identifiers) to the system clipboard
 * while tagging the clip as sensitive (Android 13+ preview masking) and scheduling
 * an automatic clipboard purge after 45 seconds if the copied secret is still present.
 */
object ClipboardSecurityHelper {
    private const val EXTRA_IS_SENSITIVE = "android.content.extra.IS_SENSITIVE"
    const val AUTO_CLEAR_DELAY_MS = 45_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Volatile
    private var lastCopiedSha256: String? = null
    private var autoClearJob: Job? = null

    fun copySensitiveText(
        context: Context,
        label: String,
        text: String,
        autoClearDelayMs: Long = AUTO_CLEAR_DELAY_MS
    ) {
        val appContext = context.applicationContext
        val cb = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        val clip = ClipData.newPlainText(label, text)
        val extras = PersistableBundle().apply {
            putBoolean(EXTRA_IS_SENSITIVE, true)
        }
        clip.description.extras = extras
        cb.setPrimaryClip(clip)

        val digest = VaultCryptoManager.sha256Hex(text)
        lastCopiedSha256 = digest
        autoClearJob?.cancel()
        if (autoClearDelayMs > 0L) {
            autoClearJob = scope.launch {
                delay(autoClearDelayMs)
                clearClipboardIfDigestMatches(appContext, digest)
            }
        }
    }

    fun clearIfSensitiveActive(context: Context) {
        val digest = lastCopiedSha256 ?: return
        autoClearJob?.cancel()
        autoClearJob = null
        clearClipboardIfDigestMatches(context.applicationContext, digest)
    }

    private fun clearClipboardIfDigestMatches(appContext: Context, expectedDigest: String) {
        try {
            val cb = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
            val currentText = cb.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()
            if (currentText != null && VaultCryptoManager.sha256Hex(currentText) == expectedDigest) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    cb.clearPrimaryClip()
                } else {
                    cb.setPrimaryClip(ClipData.newPlainText("", ""))
                }
            }
        } catch (_: Exception) {
        } finally {
            if (lastCopiedSha256 == expectedDigest) {
                lastCopiedSha256 = null
            }
        }
    }
}
