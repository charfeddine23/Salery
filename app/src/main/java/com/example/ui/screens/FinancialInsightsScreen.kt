package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.FinancialInsightsEngine
import com.example.data.DebtEntity
import com.example.data.TransactionEntity
import com.example.data.UserSettings
import com.example.ui.components.BalanceProjectionChart
import com.example.ui.components.DynamicMoneyBag
import com.example.ui.components.FinancialInsightsCard
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.SmartBudgetAlertCard
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.LocalStealthMode
import com.example.ui.i18n.formatDzd
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.NeuRoyalBlue
import com.example.ui.theme.RoseNeon
import com.example.viewmodel.FinancialSummary

/**
 * Dedicated AI Financial Insights & Wealth Analytics Screen.
 * Opened by tapping the dynamic 90fps Money Bag in the top header app bar.
 */
@Composable
fun FinancialInsightsScreen(
    summary: FinancialSummary,
    transactions: List<TransactionEntity>,
    debts: List<DebtEntity>,
    userSettings: UserSettings,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)

    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors
    val isArabic = userSettings.language == "ar"
    val isStealth = LocalStealthMode.current

    val insights = remember(transactions, debts, summary, userSettings, isArabic) {
        FinancialInsightsEngine.generateInsights(
            transactions = transactions,
            debts = debts,
            summary = summary,
            userSettings = userSettings,
            isArabic = isArabic
        )
    }

    GlassBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Bar with Back Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassCard(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    backgroundColor = colors.glassCard,
                    onClick = onBackClick
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = strings.aiInsightsTitle,
                        color = colors.textPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.3.sp
                    )
                    Text(
                        text = strings.aiInsightsSubtitle,
                        color = colors.primaryNeon,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // 1. Hero Wealth Status Card with Dynamic 90fps Money Bag
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = colors.glassCard,
                        shape = RoundedCornerShape(26.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Large interactive Money Bag visualizer
                            DynamicMoneyBag(
                                totalBalance = summary.totalBalance,
                                onClick = { /* Already on insights screen */ },
                                size = 72.dp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = strings.totalBalance,
                                color = colors.textMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = formatDzd(summary.totalBalance, strings.currencyCode, isStealth),
                                color = colors.textPrimary,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Wealth Status Tier Badge
                            val tierLabel = when {
                                summary.totalBalance >= 120000.0 -> if (isArabic) "سيولة ممتازة واحتياطي وفير" else "Abundant Capital & Surplus"
                                summary.totalBalance >= 40000.0 -> if (isArabic) "احتياطي مالي صحي ومتوازن" else "Healthy Capital Reserves"
                                else -> if (isArabic) "مرحلة تعزيز السيولة والادخار" else "Capital Building Phase"
                            }
                            val tierColor = when {
                                summary.totalBalance >= 120000.0 -> EmeraldNeon
                                summary.totalBalance >= 40000.0 -> CyanNeon
                                else -> AmberNeon
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(tierColor.copy(alpha = 0.16f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = tierColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = tierLabel,
                                        color = tierColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Cashflow Ratio Pillars
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CashflowPillar(
                                    title = strings.monthlyIncome,
                                    amount = formatDzd(summary.monthlyIncome, strings.currencyCode, isStealth),
                                    color = EmeraldNeon,
                                    icon = Icons.Default.TrendingUp,
                                    modifier = Modifier.weight(1f)
                                )
                                CashflowPillar(
                                    title = strings.monthlyExpense,
                                    amount = formatDzd(summary.monthlyExpenses, strings.currencyCode, isStealth),
                                    color = RoseNeon,
                                    icon = Icons.Default.TrendingDown,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // 2. Smart Budget Threshold Alerts & Local Notifications (80% Spending Cap Warning & Deficit Advice)
                item {
                    SmartBudgetAlertCard(
                        transactions = transactions,
                        isArabic = isArabic,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 3. Main AI Financial Insights Engine Carousel / Cards
                item {
                    FinancialInsightsCard(
                        insights = insights,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 4. Future Balance Projection Chart (توقع الرصيد المستقبلي)
                item {
                    BalanceProjectionChart(
                        currentBalance = summary.totalBalance,
                        transactions = transactions,
                        debts = debts,
                        userSettings = userSettings,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 5. Deep-Dive Metrics Cards
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = if (isArabic) "مؤشرات الذكاء المالي التفصيلية" else "Deep-Dive Financial Metrics",
                            color = colors.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        // Spending Velocity Metric
                        InsightMetricTile(
                            icon = Icons.Default.Speed,
                            iconTint = CyanNeon,
                            title = strings.spendingVelocity,
                            value = formatDzd(summary.monthlyExpenses / 30.0, strings.currencyCode, isStealth),
                            subtitle = if (isArabic) "متوسط الحرق اليومي لمصاريفك" else "Average daily living expenditure burn rate"
                        )

                        // Salary Runway Metric
                        val daysToPayday = (userSettings.salaryPayday - java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH)).let {
                            if (it < 0) it + 30 else it
                        }
                        InsightMetricTile(
                            icon = Icons.Default.Schedule,
                            iconTint = EmeraldNeon,
                            title = strings.salaryRunway,
                            value = if (isArabic) "$daysToPayday يوماً متبقية" else "$daysToPayday Days remaining",
                            subtitle = "${userSettings.salaryEmployer} • Day ${userSettings.salaryPayday}"
                        )

                        // Debt Liquidity Ratio Metric
                        val debtRatio = if (summary.totalBalance > 0) (summary.pendingDebtsOwedByMe / summary.totalBalance) * 100.0 else 0.0
                        InsightMetricTile(
                            icon = Icons.Default.CreditCard,
                            iconTint = if (debtRatio > 35.0) RoseNeon else AmberNeon,
                            title = strings.debtPressure,
                            value = formatDzd(summary.pendingDebtsOwedByMe, strings.currencyCode, isStealth),
                            subtitle = if (isArabic) "تمثل ${debtRatio.toInt()}% من رصيدك الحالي" else "Represents ${debtRatio.toInt()}% of available balance"
                        )

                        // Net Savings Cushion Metric
                        val netSavings = (summary.monthlyIncome - summary.monthlyExpenses).coerceAtLeast(0.0)
                        InsightMetricTile(
                            icon = Icons.Default.Savings,
                            iconTint = EmeraldNeon,
                            title = strings.savingsMilestone,
                            value = formatDzd(netSavings, strings.currencyCode, isStealth),
                            subtitle = if (isArabic) "الفائض المالي الصافي للشهر الحالي" else "Current net monthly surplus retained"
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun CashflowPillar(
    title: String,
    amount: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    val colors = LiquidGlassTheme.colors

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(colors.glassSurface)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    color = colors.textMuted,
                    fontSize = 10.5.sp
                )
                Text(
                    text = amount,
                    color = color,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun InsightMetricTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val colors = LiquidGlassTheme.colors

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        backgroundColor = colors.glassSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconTint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = colors.textPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    color = colors.textMuted,
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = value,
                color = iconTint,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
