package com.example.security

import android.content.Context
import android.content.ContextWrapper
import androidx.fragment.app.FragmentActivity

/**
 * Step-up authentication helper for high-sensitivity vault actions
 * (revealing stored secrets, inspecting/editing card PAN & CVC, exporting backups).
 */
object StepUpAuthHelper {

    fun verifyIfEnabled(
        context: Context,
        requireBiometric: Boolean,
        title: String,
        subtitle: String,
        cancelText: String,
        onVerified: () -> Unit,
        onRejected: (String?) -> Unit = {}
    ) {
        if (!requireBiometric) {
            onVerified()
            return
        }
        val activity = findFragmentActivity(context)
        if (activity == null || !BiometricAuthManager.isBiometricAvailable(activity)) {
            onVerified()
            return
        }
        BiometricAuthManager.authenticate(
            activity = activity,
            title = title,
            subtitle = subtitle,
            negativeButtonText = cancelText,
            onSuccess = onVerified,
            onError = { err -> onRejected(err) },
            onFailed = { /* BiometricPrompt shows built-in retry feedback until user cancels or succeeds */ }
        )
    }

    private tailrec fun findFragmentActivity(context: Context): FragmentActivity? = when (context) {
        is FragmentActivity -> context
        is ContextWrapper -> findFragmentActivity(context.baseContext)
        else -> null
    }
}
