package com.example.security

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

enum class BiometricStatus {
    AVAILABLE,
    NO_HARDWARE,
    NOT_ENROLLED,
    UNAVAILABLE
}

object BiometricAuthManager {

    private const val BIOMETRIC_ONLY_AUTHENTICATORS =
        BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK

    private const val BIOMETRIC_OR_CREDENTIAL_AUTHENTICATORS =
        BIOMETRIC_ONLY_AUTHENTICATORS or BiometricManager.Authenticators.DEVICE_CREDENTIAL

    fun getBiometricStatus(context: Context): BiometricStatus {
        val manager = BiometricManager.from(context)
        return when (manager.canAuthenticate(BIOMETRIC_ONLY_AUTHENTICATORS)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NOT_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.NO_HARDWARE
            else -> BiometricStatus.UNAVAILABLE
        }
    }

    fun isBiometricAvailable(context: Context): Boolean {
        return getBiometricStatus(context) == BiometricStatus.AVAILABLE
    }

    fun canAuthenticateWithDeviceFallback(context: Context): Boolean {
        val manager = BiometricManager.from(context)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            manager.canAuthenticate(BIOMETRIC_OR_CREDENTIAL_AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS
        } else {
            isBiometricAvailable(context) || hasLegacyDeviceCredential(context)
        }
    }

    private fun hasLegacyDeviceCredential(context: Context): Boolean {
        return try {
            val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
            km?.isDeviceSecure == true
        } catch (_: Exception) {
            false
        }
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        negativeButtonText: String,
        allowDeviceCredential: Boolean = false,
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {},
        onFailed: () -> Unit = {}
    ) {
        val canAuth = if (allowDeviceCredential) {
            canAuthenticateWithDeviceFallback(activity)
        } else {
            isBiometricAvailable(activity)
        }
        if (!canAuth) {
            onError("Biometrics not available")
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // User canceled, negative button pressed, or lock out
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFailed()
                }
            }
        )

        val promptBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)

        if (allowDeviceCredential) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                promptBuilder.setAllowedAuthenticators(BIOMETRIC_OR_CREDENTIAL_AUTHENTICATORS)
            } else {
                @Suppress("DEPRECATION")
                promptBuilder.setDeviceCredentialAllowed(true)
            }
        } else {
            promptBuilder
                .setNegativeButtonText(negativeButtonText)
                .setAllowedAuthenticators(BIOMETRIC_ONLY_AUTHENTICATORS)
        }

        try {
            prompt.authenticate(promptBuilder.build())
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Biometric prompt unavailable")
        }
    }
}
