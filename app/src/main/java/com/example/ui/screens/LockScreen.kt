package com.example.ui.screens

import android.content.ContextWrapper
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.security.BiometricAuthManager
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.SpLogoContainer
import com.example.ui.i18n.APP_NAME
import com.example.ui.i18n.APP_VERSION
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.ArabicStrings
import com.example.ui.i18n.DEVELOPER_NAME
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.RoseNeon

@Composable
fun LockScreen(
    onUnlockAttempt: (List<Int>) -> Boolean = { false },
    onBiometricUnlock: () -> Unit = {},
    isBiometricEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors
    val isArabic = strings is ArabicStrings

    val activity = remember(context) {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is FragmentActivity) return@remember ctx
            ctx = ctx.baseContext
        }
        null
    }

    val canUseBiometrics = activity != null && BiometricAuthManager.isBiometricAvailable(activity)

    var isVerifying by remember { mutableStateOf(false) }
    var statusText by remember {
        mutableStateOf(if (isArabic) "المس مستشعر البصمة للمتابعة" else "Touch fingerprint sensor to continue")
    }
    var isError by remember { mutableStateOf(false) }

    fun launchBiometricPrompt() {
        if (activity == null) return
        isVerifying = true
        isError = false
        BiometricAuthManager.authenticate(
            activity = activity,
            title = strings.biometricPromptTitle,
            subtitle = strings.biometricPromptSubtitle,
            negativeButtonText = strings.cancel,
            onSuccess = {
                isVerifying = false
                isError = false
                statusText = if (isArabic) "تم التحقق بنجاح" else "Verified"
                onBiometricUnlock()
            },
            onError = { errString ->
                isVerifying = false
                isError = true
                statusText = errString
            },
            onFailed = {
                isVerifying = false
                isError = true
                statusText = if (isArabic) "لم يتم التعرف على البصمة. أعد المحاولة." else "Biometrics unverified. Tap to retry."
            }
        )
    }

    LaunchedEffect(canUseBiometrics) {
        if (canUseBiometrics) {
            launchBiometricPrompt()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

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
                size = 76.dp,
                cornerRadius = 22.dp,
                padding = 8.dp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // App Name (always Latin)
            Text(
                text = APP_NAME,
                color = colors.textPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )

            Text(
                text = strings.biometricSettingsTitle,
                color = colors.primaryNeon,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Large Fingerprint Visual Target with glowing ring
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(140.dp)
                    .scale(if (isVerifying) pulseScale else 1f)
                    .clip(CircleShape)
                    .background(colors.primaryNeon.copy(alpha = 0.12f))
                    .border(1.5.dp, colors.primaryNeon.copy(alpha = 0.45f), CircleShape)
                    .clickable { launchBiometricPrompt() }
            ) {
                Box(
                    modifier = Modifier
                        .size(105.dp)
                        .clip(CircleShape)
                        .background(colors.primaryNeon.copy(alpha = 0.20f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Fingerprint Sensor",
                        tint = if (isError) RoseNeon else colors.primaryNeon,
                        modifier = Modifier.size(60.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Instruction / Status Text
            Text(
                text = statusText,
                color = if (isError) RoseNeon else colors.textSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Primary Unlock Button
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 320.dp)
                    .testTag("biometric_unlock_button"),
                shape = RoundedCornerShape(18.dp),
                backgroundColor = colors.primaryNeon.copy(alpha = 0.22f),
                onClick = { launchBiometricPrompt() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = colors.primaryNeon,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = strings.biometricUnlockAction,
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Developer Credit & Version
            Text(
                text = "${strings.developer}: $DEVELOPER_NAME",
                color = colors.textMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "v$APP_VERSION • ${strings.updatedLabel}: ${strings.buildDate}",
                color = colors.textMuted.copy(alpha = 0.7f),
                fontSize = 11.sp
            )
        }
    }
}
