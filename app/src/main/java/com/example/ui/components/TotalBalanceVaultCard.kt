package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserSettings
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.LocalStealthMode
import com.example.ui.i18n.formatDzd
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.LocalAdaptiveDimens
import com.example.ui.theme.LocalAdaptiveTypography
import com.example.ui.theme.RoseNeon
import com.example.viewmodel.FinancialSummary
import kotlinx.coroutines.delay

/**
 * Clean & Harmonized Total Balance Card for Dashboard.
 * Incorporates:
 * 1. Neumorphic Dual-Shadow & Surface Contrast matching Smart Salary Scheduler.
 * 2. Dynamic Cash Flow Ambient Glow shifting with recent spending/income ratio.
 * 3. Monthly Income vs Monthly Expense summary pillars.
 * 4. Standardized 0.5s hold feedback with visual progress indicator for Smart Salary Scheduler.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TotalBalanceVaultCard(
    summary: FinancialSummary,
    userSettings: UserSettings,
    onLockToVault: (Double) -> Unit = {},
    onUnlockFromVault: (Double) -> Unit = {},
    onOpenSalaryScheduler: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = LiquidGlassTheme.colors
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current
    val strings = AppStrings.current
    val isArabic = userSettings.language == "ar"
    val isStealth = LocalStealthMode.current
    val haptic = LocalHapticFeedback.current

    val lockedAmount = summary.savingsBalance
    val spendableBalance = summary.totalBalance

    val isSurplus = summary.monthlyIncome > summary.monthlyExpenses * 1.20
    val isDeficit = summary.monthlyExpenses > summary.monthlyIncome && summary.monthlyExpenses > 0

    val glowColor1 = when {
        isSurplus -> EmeraldNeon
        isDeficit -> RoseNeon
        else -> CyanNeon
    }
    val glowColor2 = when {
        isSurplus -> CyanNeon
        isDeficit -> AmberNeon
        else -> Color(0xFF6366F1)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "cashflow_glow")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.70f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    // Unified 0.5s hold interaction (~100ms TapTimeout + 400ms progress animation)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val holdAnim = remember { Animatable(0f) }
    var didTriggerHold by remember { mutableStateOf(false) }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            didTriggerHold = false
            holdAnim.snapTo(0f)
            holdAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 400, easing = LinearEasing)
            )
            didTriggerHold = true
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onOpenSalaryScheduler()
        } else {
            holdAnim.snapTo(0f)
            if (didTriggerHold) {
                delay(140)
                didTriggerHold = false
            }
        }
    }

    val cardScale = 1f - (holdAnim.value * 0.015f)
    val schedulerSemanticLabel = if (isArabic) "تعديل مجدول الراتب الذكي" else "Edit Smart Salary Scheduler"

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .scale(cardScale)
            .clip(RoundedCornerShape(dimens.cornerRadiusLg))
            .semantics {
                onLongClick(label = schedulerSemanticLabel) {
                    onOpenSalaryScheduler()
                    true
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = colors.primaryNeon),
                onClick = {
                    if (didTriggerHold) {
                        didTriggerHold = false
                    }
                }
            )
            .testTag("total_balance_vault_card"),
        shape = RoundedCornerShape(dimens.cornerRadiusLg),
        backgroundColor = colors.glassCard,
        borderColor = if (holdAnim.value > 0.05f) colors.primaryNeon.copy(alpha = 0.75f) else colors.glassBorder,
        cornerRadius = dimens.cornerRadiusLg
    ) {
        if (!colors.isNeumorphic) {
            Canvas(
                modifier = Modifier.matchParentSize()
            ) {
                val centerOffset = Offset(size.width * 0.5f, size.height * 0.35f)
                val glowRadius = size.maxDimension * 0.55f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor1.copy(alpha = 0.22f * glowPulse),
                            glowColor2.copy(alpha = 0.12f * glowPulse),
                            Color.Transparent
                        ),
                        center = centerOffset,
                        radius = glowRadius
                    ),
                    radius = glowRadius,
                    center = centerOffset
                )
            }
        }

        Column(
            modifier = Modifier.padding(dimens.cardPadding)
        ) {
            // Top Header Row with clear 0.5s salary scheduler hold cue
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.totalBalance,
                        color = colors.textSecondary,
                        fontSize = typo.body,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (lockedAmount > 0) {
                            if (isArabic) "الرصيد المتاح (بعد الادخار)" else "Available (After Savings)"
                        } else {
                            if (isArabic) "الفرق بين الدخل والمصاريف" else "Income − Expense"
                        },
                        color = colors.textMuted,
                        fontSize = typo.caption,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(colors.glassSurface)
                        .border(0.8.dp, colors.glassBorderSubtle, RoundedCornerShape(50))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = colors.primaryNeon,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isArabic) "0.5ث للراتب" else "Hold 0.5s • Salary",
                        color = colors.textSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(dimens.spacingSm))

            // Prominent Active Balance (Spendable Amount) with tabular numerals
            Text(
                text = formatDzd(spendableBalance, strings.currencyCode, isStealth),
                color = colors.textPrimary,
                fontSize = typo.hero,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp,
                style = TextStyle(fontFeatureSettings = "tnum"),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (lockedAmount > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(dimens.cornerRadiusXs + 2.dp))
                            .background(colors.glassSurface)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = AmberNeon,
                            modifier = Modifier.size(dimens.iconSizeXs)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = (if (isArabic) "محفظة الادخار: " else "Savings Wallet: ") +
                                formatDzd(lockedAmount, strings.currencyCode, isStealth),
                            color = colors.textSecondary,
                            fontSize = typo.bodyXs,
                            fontWeight = FontWeight.Medium,
                            style = TextStyle(fontFeatureSettings = "tnum"),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(dimens.spacingMd))

            // Monthly Income vs Expense Mini Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimens.spacingSm + 2.dp)
            ) {
                // Income
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(dimens.cornerRadiusMd))
                        .background(colors.glassSurface)
                        .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(dimens.cornerRadiusMd))
                        .padding(dimens.cardPaddingSmall)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(dimens.iconSizeXl)
                                .clip(CircleShape)
                                .background(EmeraldNeon.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = EmeraldNeon,
                                modifier = Modifier.size(dimens.iconSizeSm)
                            )
                        }
                        Spacer(modifier = Modifier.width(dimens.spacingSm))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.monthlyIncome,
                                color = colors.textMuted,
                                fontSize = typo.caption,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = formatDzd(summary.monthlyIncome, strings.currencyCode, isStealth),
                                color = EmeraldNeon,
                                fontSize = typo.body,
                                fontWeight = FontWeight.Bold,
                                style = TextStyle(fontFeatureSettings = "tnum"),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Expense
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(dimens.cornerRadiusMd))
                        .background(colors.glassSurface)
                        .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(dimens.cornerRadiusMd))
                        .padding(dimens.cardPaddingSmall)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(dimens.iconSizeXl)
                                .clip(CircleShape)
                                .background(RoseNeon.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = RoseNeon,
                                modifier = Modifier.size(dimens.iconSizeSm)
                            )
                        }
                        Spacer(modifier = Modifier.width(dimens.spacingSm))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.monthlyExpense,
                                color = colors.textMuted,
                                fontSize = typo.caption,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = formatDzd(summary.monthlyExpenses, strings.currencyCode, isStealth),
                                color = RoseNeon,
                                fontSize = typo.body,
                                fontWeight = FontWeight.Bold,
                                style = TextStyle(fontFeatureSettings = "tnum"),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Top hold progress feedback bar during 0.5s press
        if (holdAnim.value > 0.04f) {
            LinearProgressIndicator(
                progress = { holdAnim.value },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.TopCenter),
                color = colors.primaryNeon,
                trackColor = colors.glassBorderSubtle.copy(alpha = 0.4f)
            )
        }
    }
}
