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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.LocalAdaptiveDimens
import com.example.ui.theme.LocalAdaptiveLayoutInfo
import com.example.ui.theme.LocalAdaptiveTypography
import com.example.ui.theme.RoseNeon

@Composable
fun LockScreen(
    onBiometricUnlock: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors
    val isArabic = strings is ArabicStrings
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current
    val layoutInfo = LocalAdaptiveLayoutInfo.current

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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimens.screenPaddingH, vertical = dimens.spacingLg),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = dimens.contentMaxWidth),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Official SaleryPulse "SP" Brand Logo in Dynamic Container
                SpLogoContainer(
                    size = if (layoutInfo.isCompactPhone) 64.dp else 76.dp,
                    cornerRadius = dimens.cornerRadiusXl,
                    padding = 8.dp
                )

                Spacer(modifier = Modifier.height(dimens.spacingMd))

                // App Name (always Latin)
                Text(
                    text = APP_NAME,
                    color = colors.textPrimary,
                    fontSize = typo.headline,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )

                Text(
                    text = strings.biometricSettingsTitle,
                    color = colors.primaryNeon,
                    fontSize = typo.body,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(dimens.spacingXl))

                // Large Fingerprint Visual Target with glowing ring
                val outerRingSize = if (layoutInfo.isCompactPhone) 120.dp else 140.dp
                val innerRingSize = if (layoutInfo.isCompactPhone) 90.dp else 105.dp
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(outerRingSize)
                        .scale(if (isVerifying) pulseScale else 1f)
                        .clip(CircleShape)
                        .background(colors.primaryNeon.copy(alpha = 0.12f))
                        .border(1.5.dp, colors.primaryNeon.copy(alpha = 0.45f), CircleShape)
                        .clickable { launchBiometricPrompt() }
                ) {
                    Box(
                        modifier = Modifier
                            .size(innerRingSize)
                            .clip(CircleShape)
                            .background(colors.primaryNeon.copy(alpha = 0.20f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Fingerprint Sensor",
                            tint = if (isError) RoseNeon else colors.primaryNeon,
                            modifier = Modifier.size(if (layoutInfo.isCompactPhone) 52.dp else 60.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(dimens.spacingLg))

                // Instruction / Status Text
                Text(
                    text = statusText,
                    color = if (isError) RoseNeon else colors.textSecondary,
                    fontSize = typo.bodyLg,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(dimens.spacingXl))

                // Primary Unlock Button
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 320.dp)
                        .testTag("biometric_unlock_button"),
                    shape = RoundedCornerShape(dimens.cornerRadiusLg),
                    backgroundColor = colors.primaryNeon.copy(alpha = 0.22f),
                    onClick = { launchBiometricPrompt() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = dimens.cardPaddingLarge, vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = colors.primaryNeon,
                            modifier = Modifier.size(dimens.iconSizeMd)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = strings.biometricUnlockAction,
                            color = colors.textPrimary,
                            fontSize = typo.bodyLg,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(dimens.spacingXxl))

                // Developer Credit & Version
                Text(
                    text = "${strings.developer}: $DEVELOPER_NAME",
                    color = colors.textMuted,
                    fontSize = typo.bodySm,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "v$APP_VERSION • ${strings.updatedLabel}: ${strings.buildDate}",
                    color = colors.textMuted.copy(alpha = 0.7f),
                    fontSize = typo.bodyXs,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
