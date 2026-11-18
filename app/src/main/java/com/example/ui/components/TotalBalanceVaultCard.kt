package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PauseCircleOutline
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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

/**
 * Clean & Harmonized Total Balance Card for Dashboard.
 * Incorporates:
 * 1. Neumorphic Dual-Shadow & Surface Contrast matching Smart Salary Scheduler.
 * 2. Dynamic Cash Flow Ambient Glow shifting with recent spending/income ratio.
 * 3. Toggleable ALG Post Auto Balance Mode (ALG Post Balance + Manual In - Manual Out - Savings).
 * 4. Monthly Income vs Monthly Expense summary pillars.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun TotalBalanceVaultCard(
    summary: FinancialSummary,
    userSettings: UserSettings,
    onLockToVault: (Double) -> Unit = {},
    onUnlockFromVault: (Double) -> Unit = {},
    onOpenSalaryScheduler: () -> Unit = {},
    onToggleAlgPostBalanceMode: (Boolean) -> Unit = {},
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
    val isAlgPostMode = summary.isAlgPostBalanceMode

    // Cash flow ratio calculations for ambient dynamic glow
    val isSurplus = summary.monthlyIncome > summary.monthlyExpenses * 1.20
    val isDeficit = summary.monthlyExpenses > summary.monthlyIncome && summary.monthlyExpenses > 0

    val glowColor1 = when {
        isAlgPostMode -> CyanNeon
        isSurplus -> EmeraldNeon
        isDeficit -> RoseNeon
        else -> CyanNeon
    }
    val glowColor2 = when {
        isAlgPostMode -> EmeraldNeon
        isSurplus -> CyanNeon
        isDeficit -> AmberNeon
        else -> Color(0xFF6366F1) // Indigo/Violet
    }

    // Infinite breathing animation for ambient glow
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

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimens.cornerRadiusLg))
            .combinedClickable(
                onClick = {},
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onOpenSalaryScheduler()
                },
                onLongClickLabel = if (isArabic) "تعديل مجدول الراتب الذكي" else "Edit Smart Salary Scheduler"
            )
            .testTag("total_balance_vault_card"),
        shape = RoundedCornerShape(dimens.cornerRadiusLg),
        backgroundColor = colors.glassCard,
        cornerRadius = dimens.cornerRadiusLg
    ) {
        // Subtle Dynamic Cash Flow Ambient Glow (Rendered safely inside clipped card bounds)
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
            // Top Header Row with Cash Flow Status Badge + ALG Post Balance Mode Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.totalBalance,
                        color = colors.textSecondary,
                        fontSize = typo.body,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = when {
                            isAlgPostMode -> strings.algPostBalanceModeFormulaHint
                            lockedAmount > 0 -> if (isArabic) "الرصيد المتاح (بعد الادخار)" else "Available (After Savings)"
                            else -> if (isArabic) "الفرق بين الدخل والمصاريف" else "Income − Expense"
                        },
                        color = if (isAlgPostMode) CyanNeon else colors.textMuted,
                        fontSize = typo.caption,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(dimens.spacingSm))

                // Mode Switch Button inside the Balance Container (min 48dp accessible touch target)
                Box(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(RoundedCornerShape(dimens.cornerRadiusMd))
                        .background(
                            if (isAlgPostMode) CyanNeon.copy(alpha = 0.22f)
                            else colors.glassSurface
                        )
                        .border(
                            1.dp,
                            if (isAlgPostMode) CyanNeon.copy(alpha = 0.65f) else colors.glassBorderSubtle,
                            RoundedCornerShape(dimens.cornerRadiusMd)
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleAlgPostBalanceMode(!isAlgPostMode)
                        }
                        .padding(horizontal = dimens.spacingSm + 4.dp, vertical = 6.dp)
                        .testTag("toggle_alg_post_balance_mode_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isAlgPostMode) Icons.Default.AccountBalance else Icons.Default.AutoAwesome,
                            contentDescription = strings.algPostBalanceModeLabel,
                            tint = if (isAlgPostMode) CyanNeon else colors.textSecondary,
                            modifier = Modifier.size(dimens.iconSizeXs + 2.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isAlgPostMode) strings.algPostBalanceModeLabel else strings.manualBalanceModeLabel,
                            color = if (isAlgPostMode) CyanNeon else colors.textPrimary,
                            fontSize = typo.bodyXs,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(dimens.spacingSm))

            // Prominent Active Balance (Spendable Amount)
            Text(
                text = formatDzd(spendableBalance, strings.currencyCode, isStealth),
                color = colors.textPrimary,
                fontSize = typo.hero,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            // Breakdown Chips (ALG Post + Manual Diff, Salary Scheduler Auto-Pause notice, Savings Wallet)
            if (isAlgPostMode || lockedAmount > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isAlgPostMode) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(dimens.cornerRadiusXs + 2.dp))
                                .background(CyanNeon.copy(alpha = 0.14f))
                                .border(1.dp, CyanNeon.copy(alpha = 0.30f), RoundedCornerShape(dimens.cornerRadiusXs + 2.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = (if (isArabic) "ALG Post: " else "ALG Post: ") +
                                    formatDzd(summary.algPostBalanceComponent, strings.currencyCode, isStealth),
                                color = CyanNeon,
                                fontSize = typo.bodyXs,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }

                        val manualDiff = summary.manualNetDiffComponent
                        val diffSign = if (manualDiff >= 0.0) "+" else ""
                        val diffColor = if (manualDiff >= 0.0) EmeraldNeon else RoseNeon
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(dimens.cornerRadiusXs + 2.dp))
                                .background(diffColor.copy(alpha = 0.14f))
                                .border(1.dp, diffColor.copy(alpha = 0.30f), RoundedCornerShape(dimens.cornerRadiusXs + 2.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = (if (isArabic) "يدوي: " else "Manual: ") +
                                    "$diffSign${formatDzd(manualDiff, strings.currencyCode, isStealth)}",
                                color = diffColor,
                                fontSize = typo.bodyXs,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(dimens.cornerRadiusXs + 2.dp))
                                .background(AmberNeon.copy(alpha = 0.14f))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PauseCircleOutline,
                                contentDescription = null,
                                tint = AmberNeon,
                                modifier = Modifier.size(dimens.iconSizeXs)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = strings.salarySchedulerPausedByAlgPost,
                                color = AmberNeon,
                                fontSize = typo.bodyXs,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (lockedAmount > 0) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(dimens.cornerRadiusXs + 2.dp))
                                .background(colors.glassSurface)
                                .padding(horizontal = 8.dp, vertical = 3.dp),
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
                                color = colors.textMuted,
                                fontSize = typo.bodyXs,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
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
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Text(
                                text = formatDzd(summary.monthlyIncome, strings.currencyCode, isStealth),
                                color = EmeraldNeon,
                                fontSize = typo.body,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Text(
                                text = formatDzd(summary.monthlyExpenses, strings.currencyCode, isStealth),
                                color = RoseNeon,
                                fontSize = typo.body,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
