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
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Schedule
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
import java.util.Calendar

/**
 * Clean & Harmonized Total Balance Card for Dashboard.
 * Incorporates:
 * 1. Neumorphic Dual-Shadow & Surface Contrast matching Smart Salary Scheduler.
 * 2. Dynamic Cash Flow Ambient Glow shifting with recent spending/income ratio.
 * 3. Daily spendable balance and gross wealth indicator.
 * 4. Monthly Income vs Monthly Expense summary pillars.
 */
@OptIn(ExperimentalFoundationApi::class)
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
    val grossBalance = summary.grossBalance
    val spendableBalance = summary.totalBalance

    val cal = Calendar.getInstance()
    val currentDay = cal.get(Calendar.DAY_OF_MONTH)
    val daysUntilPayday = (userSettings.salaryPayday - currentDay).let {
        if (it < 0) it + 30 else it
    }
    val paydayStatus = when {
        currentDay == userSettings.salaryPayday -> strings.salaryDepositedToday
        isArabic -> "يوم ${userSettings.salaryPayday} (متبقٍ $daysUntilPayday يوم)"
        else -> "Day ${userSettings.salaryPayday} (${daysUntilPayday}d left)"
    }

    // Cash flow ratio calculations for ambient dynamic glow
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
            // Top Header Row with Cash Flow Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (lockedAmount > 0) {
                            if (isArabic) "الرصيد المتاح للصرف" else "Daily Spendable Balance"
                        } else {
                            strings.totalBalance
                        },
                        color = colors.textSecondary,
                        fontSize = typo.body,
                        fontWeight = FontWeight.Medium
                    )
                    if (lockedAmount > 0) {
                        Text(
                            text = if (isArabic) "مستثنى منه رصيد محفظة الادخار" else "Savings wallet reserve excluded",
                            color = EmeraldNeon,
                            fontSize = typo.caption,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Cash Flow Phase Indicator Badge
                val flowTagLabel = when {
                    isSurplus -> if (isArabic) "تدفق مالي إيجابي ↗" else "Surplus Flow ↗"
                    isDeficit -> if (isArabic) "حرق رصيد مرتفع ↘" else "High Burn ↘"
                    else -> if (isArabic) "تدفق متوازن ⇄" else "Balanced Flow ⇄"
                }
                val flowTagColor = when {
                    isSurplus -> EmeraldNeon
                    isDeficit -> RoseNeon
                    else -> CyanNeon
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(dimens.cornerRadiusSm))
                        .background(flowTagColor.copy(alpha = 0.16f))
                        .border(1.dp, flowTagColor.copy(alpha = 0.35f), RoundedCornerShape(dimens.cornerRadiusSm))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = flowTagLabel,
                        color = flowTagColor,
                        fontSize = typo.bodyXs,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(dimens.spacingSm))

            // Prominent Active Balance (Spendable Amount)
            Text(
                text = formatDzd(spendableBalance, strings.currencyCode, isStealth),
                color = colors.textPrimary,
                fontSize = typo.hero,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )

            // If Safe Pocket is locked, show net gross breakdown chip
            if (lockedAmount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.glassSurface)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Savings,
                        contentDescription = null,
                        tint = AmberNeon,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = (if (isArabic) "إجمالي الثروة (المتاح + الادخار): " else "Gross Wealth (Available + Savings): ") +
                                formatDzd(grossBalance, strings.currencyCode, isStealth),
                        color = colors.textMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Monthly Income vs Expense Mini Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                        Column {
                            Text(
                                text = strings.monthlyIncome,
                                color = colors.textMuted,
                                fontSize = typo.caption
                            )
                            Text(
                                text = formatDzd(summary.monthlyIncome, strings.currencyCode, isStealth),
                                color = EmeraldNeon,
                                fontSize = typo.body,
                                fontWeight = FontWeight.Bold
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
                        Column {
                            Text(
                                text = strings.monthlyExpense,
                                color = colors.textMuted,
                                fontSize = typo.caption
                            )
                            Text(
                                text = formatDzd(summary.monthlyExpenses, strings.currencyCode, isStealth),
                                color = RoseNeon,
                                fontSize = typo.body,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Smart Salary Scheduler embedded status bar (Hold anywhere on Total Balance to edit)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(dimens.cornerRadiusMd))
                    .background(colors.glassSurface.copy(alpha = 0.78f))
                    .border(0.8.dp, CyanNeon.copy(alpha = 0.25f), RoundedCornerShape(dimens.cornerRadiusMd))
                    .padding(horizontal = 12.dp, vertical = 9.dp)
                    .testTag("total_balance_salary_scheduler_strip"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(CyanNeon.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = CyanNeon,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${strings.salarySchedulerTitle} • ${formatDzd(userSettings.salaryAmount, strings.currencyCode, isStealth)}",
                            color = colors.textPrimary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = paydayStatus,
                            color = colors.primaryNeon,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyanNeon.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isArabic) "اضغط مطولاً للتعديل" else "Hold to edit",
                        color = CyanNeon,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
