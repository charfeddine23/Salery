package com.example.ui.screens

import android.content.ContextWrapper
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.R
import com.example.security.BiometricAuthManager
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.PatternLockView
import com.example.ui.components.PatternState
import com.example.ui.components.SpLogoContainer
import com.example.ui.i18n.APP_NAME
import com.example.ui.i18n.APP_VERSION
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.DEVELOPER_NAME
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.RoseNeon

@Composable
fun LockScreen(
    onUnlockAttempt: (List<Int>) -> Boolean,
    onBiometricUnlock: () -> Unit = {},
    isBiometricEnabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors

    val activity = remember(context) {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is FragmentActivity) return@remember ctx
            ctx = ctx.baseContext
        }
        null
    }

    val canUseBiometrics = isBiometricEnabled && activity != null && BiometricAuthManager.isBiometricAvailable(activity)

    var patternState by remember { mutableStateOf(PatternState.INPUT) }
    var statusText by remember { mutableStateOf(strings.drawPattern) }

    fun launchBiometricPrompt() {
        if (!canUseBiometrics || activity == null) return
        BiometricAuthManager.authenticate(
            activity = activity,
            title = strings.biometricPromptTitle,
            subtitle = strings.biometricPromptSubtitle,
            negativeButtonText = strings.biometricUsePattern,
            onSuccess = {
                patternState = PatternState.SUCCESS
                statusText = "Verified"
                onBiometricUnlock()
            },
            onError = { errString ->
                statusText = errString
            },
            onFailed = {
                statusText = "Biometrics unverified. Use pattern."
            }
        )
    }

    LaunchedEffect(canUseBiometrics) {
        if (canUseBiometrics) {
            launchBiometricPrompt()
        }
    }

    GlassBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Official SaleryPulse "SP" Brand Logo in Dynamic Container
            SpLogoContainer(
                size = 80.dp,
                cornerRadius = 22.dp,
                padding = 8.dp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // App Name (always Latin)
            Text(
                text = APP_NAME,
                color = colors.textPrimary,
                fontSize = 25.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )

            Text(
                text = "SaleryPulse Security",
                color = colors.primaryNeon,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Instruction / Error Text
            Text(
                text = statusText,
                color = when (patternState) {
                    PatternState.INPUT -> colors.textSecondary
                    PatternState.SUCCESS -> EmeraldNeon
                    PatternState.ERROR -> RoseNeon
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 3x3 Pattern Gesture Lock
            PatternLockView(
                modifier = Modifier.size(280.dp),
                patternState = patternState,
                onPatternComplete = { pattern ->
                    val success = onUnlockAttempt(pattern)
                    if (success) {
                        patternState = PatternState.SUCCESS
                        statusText = "Unlocked"
                    } else {
                        patternState = PatternState.ERROR
                        statusText = strings.incorrectPattern
                    }
                }
            )

            if (canUseBiometrics) {
                Spacer(modifier = Modifier.height(18.dp))
                GlassCard(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { launchBiometricPrompt() },
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = colors.glassSurface
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = colors.primaryNeon,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = strings.biometricUnlockAction,
                            color = colors.primaryNeon,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Developer Credit Watermark
            Text(
                text = "Developer: $DEVELOPER_NAME",
                color = colors.textMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Version $APP_VERSION",
                color = colors.textMuted.copy(alpha = 0.7f),
                fontSize = 11.sp
            )
        }
    }
}
