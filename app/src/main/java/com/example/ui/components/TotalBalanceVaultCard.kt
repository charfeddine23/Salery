package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.ui.theme.RoseNeon
import com.example.viewmodel.FinancialSummary

/**
 * Clean & Harmonized Total Balance Card for Dashboard.
 * Incorporates:
 * 1. Neumorphic Dual-Shadow & Surface Contrast matching Smart Salary Scheduler.
 * 2. Dynamic Cash Flow Ambient Glow shifting with recent spending/income ratio.
 * 3. Daily spendable balance and gross wealth indicator.
 * 4. Monthly Income vs Monthly Expense summary pillars.
 */
@Composable
fun TotalBalanceVaultCard(
    summary: FinancialSummary,
    userSettings: UserSettings,
    onLockToVault: (Double) -> Unit = {},
    onUnlockFromVault: (Double) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = LiquidGlassTheme.colors
    val strings = AppStrings.current
    val isArabic = userSettings.language == "ar"
    val isStealth = LocalStealthMode.current

    val lockedAmount = summary.savingsBalance
    val grossBalance = summary.grossBalance
    val spendableBalance = summary.totalBalance

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
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        backgroundColor = colors.glassCard,
        cornerRadius = 20.dp
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
            modifier = Modifier.padding(16.dp)
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
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (lockedAmount > 0) {
                        Text(
                            text = if (isArabic) "مستثنى منه رصيد محفظة الادخار" else "Savings wallet reserve excluded",
                            color = EmeraldNeon,
                            fontSize = 10.5.sp,
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
                        .clip(RoundedCornerShape(12.dp))
                        .background(flowTagColor.copy(alpha = 0.16f))
                        .border(1.dp, flowTagColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = flowTagLabel,
                        color = flowTagColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Prominent Active Balance (Spendable Amount)
            Text(
                text = formatDzd(spendableBalance, strings.currencyCode, isStealth),
                color = colors.textPrimary,
                fontSize = 32.sp,
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
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.glassSurface)
                        .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(EmeraldNeon.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = EmeraldNeon,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = strings.monthlyIncome,
                                color = colors.textMuted,
                                fontSize = 10.sp
                            )
                            Text(
                                text = formatDzd(summary.monthlyIncome, strings.currencyCode, isStealth),
                                color = EmeraldNeon,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Expense
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.glassSurface)
                        .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(RoseNeon.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = RoseNeon,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = strings.monthlyExpense,
                                color = colors.textMuted,
                                fontSize = 10.sp
                            )
                            Text(
                                text = formatDzd(summary.monthlyExpenses, strings.currencyCode, isStealth),
                                color = RoseNeon,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
