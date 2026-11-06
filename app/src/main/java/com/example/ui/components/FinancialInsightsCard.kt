package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.FinancialInsight
import com.example.ai.InsightIconType
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.NeuBurntCoral
import com.example.ui.theme.NeuRoyalBlue
import com.example.ui.theme.NeuVibrantGreen
import com.example.ui.theme.RoseNeon

@Composable
fun FinancialInsightsCard(
    insights: List<FinancialInsight>,
    modifier: Modifier = Modifier
) {
    if (insights.isEmpty()) return

    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors
    var selectedIndex by remember { mutableIntStateOf(0) }
    val safeIndex = selectedIndex.coerceIn(0, insights.size - 1)
    val activeInsight = insights[safeIndex]

    val accentColor = when (activeInsight.iconType) {
        InsightIconType.VELOCITY -> if (activeInsight.isPositive) EmeraldNeon else RoseNeon
        InsightIconType.RUNWAY -> CyanNeon
        InsightIconType.DEBT -> if (activeInsight.isPositive) CyanNeon else RoseNeon
        InsightIconType.SAVINGS -> EmeraldNeon
        InsightIconType.TIP -> colors.primaryNeon
    }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        backgroundColor = if (colors.isNeumorphic) colors.glassCard else colors.glassCard.copy(alpha = 0.92f)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            // Top Header: AI Badge + Category Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(accentColor.copy(alpha = 0.35f), accentColor.copy(alpha = 0.10f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = strings.aiInsightsTitle,
                            color = colors.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strings.aiInsightsSubtitle,
                            color = colors.textMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                // Metric Tag if available
                if (activeInsight.metricDelta != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentColor.copy(alpha = 0.18f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = activeInsight.metricDelta,
                            color = accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Animated Insight Content
            AnimatedContent(
                targetState = activeInsight,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                },
                label = "insight_content"
            ) { targetInsight ->
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = getInsightIcon(targetInsight.iconType),
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = targetInsight.title,
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = targetInsight.description,
                        color = colors.textSecondary,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Insight Indicator Dots / Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                insights.forEachIndexed { index, insight ->
                    val isSelected = index == safeIndex
                    val dotColor = if (isSelected) accentColor else colors.textMuted.copy(alpha = 0.35f)

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(6.dp)
                            .width(if (isSelected) 22.dp else 6.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                            .clickable { selectedIndex = index }
                    )
                }
            }
        }
    }
}

private fun getInsightIcon(type: InsightIconType): ImageVector {
    return when (type) {
        InsightIconType.VELOCITY -> Icons.Default.Speed
        InsightIconType.RUNWAY -> Icons.Default.AccountBalanceWallet
        InsightIconType.DEBT -> Icons.Default.CreditCard
        InsightIconType.SAVINGS -> Icons.Default.Savings
        InsightIconType.TIP -> Icons.Default.Lightbulb
    }
}
