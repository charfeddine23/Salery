package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ai.FinancialInsightsEngine
import com.example.data.DebtEntity
import com.example.data.ExpenseCategories
import com.example.data.TransactionEntity
import com.example.data.UserSettings
import com.example.ui.components.BalanceProjectionChart
import com.example.ui.components.DynamicMoneyBag
import com.example.ui.components.FinancialInsightsCard
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.SmartBudgetAlertCard
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.StringsBundle
import com.example.ui.i18n.LocalStealthMode
import com.example.ui.i18n.formatDzd
import com.example.ui.i18n.formatDzdAmount
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.LocalAdaptiveDimens
import com.example.ui.theme.LocalAdaptiveLayoutInfo
import com.example.ui.theme.LocalAdaptiveTypography
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
    onTransferToSavings: (Double) -> Unit = {},
    onWithdrawFromSavings: (Double) -> Unit = {},
    onSpendFromSavings: (Double, String, String, String) -> Unit = { _, _, _, _ -> },
    onLockToVault: (Double) -> Unit = onTransferToSavings,
    onUnlockFromVault: (Double) -> Unit = onWithdrawFromSavings,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)

    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors
    val layoutInfo = LocalAdaptiveLayoutInfo.current
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current
    val isArabic = userSettings.language == "ar"
    val isStealth = LocalStealthMode.current

    var showDepositDialog by remember { mutableStateOf(false) }
    var showWithdrawDialog by remember { mutableStateOf(false) }
    var showSpendDialog by remember { mutableStateOf(false) }

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
                    .padding(horizontal = dimens.screenPaddingH, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassCard(
                    modifier = Modifier.size(dimens.buttonHeight),
                    shape = RoundedCornerShape(dimens.cornerRadiusSm),
                    backgroundColor = colors.glassCard,
                    onClick = onBackClick
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(dimens.iconSizeMd)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(dimens.spacingMd))

                Column {
                    Text(
                        text = if (isArabic) "الادخار" else "Savings",
                        color = colors.textPrimary,
                        fontSize = typo.titleLg,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.3.sp
                    )
                    Text(
                        text = if (isArabic) "إدارة المدخرات والمحفظة الاحتياطية" else "Savings & Reserve Management",
                        color = colors.primaryNeon,
                        fontSize = typo.bodySm,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (layoutInfo.columns >= 2) {
                // Adaptive 2-column layout for tablets and landscape
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = dimens.screenPaddingH),
                    horizontalArrangement = Arrangement.spacedBy(dimens.spacingLg)
                ) {
                    // Left Column: Savings Wallet & Smart Budget Alerts
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(dimens.spacingMd)
                    ) {
                        item { Spacer(modifier = Modifier.height(dimens.spacingXs)) }

                        item {
                            SavingsWalletCard(
                                savingsBalance = summary.savingsBalance,
                                availableBalance = summary.totalBalance,
                                grossBalance = summary.grossBalance,
                                isArabic = isArabic,
                                isStealth = isStealth,
                                onOpenDeposit = { showDepositDialog = true },
                                onOpenWithdraw = { showWithdrawDialog = true },
                                onOpenSpend = { showSpendDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            SmartBudgetAlertCard(
                                transactions = transactions,
                                isArabic = isArabic,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item { Spacer(modifier = Modifier.height(dimens.spacingXl)) }
                    }

                    // Right Column: AI Insights, Balance Projection & Deep Dive Metrics
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(dimens.spacingMd)
                    ) {
                        item { Spacer(modifier = Modifier.height(dimens.spacingXs)) }

                        item {
                            FinancialInsightsCard(
                                insights = insights,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            BalanceProjectionChart(
                                currentBalance = summary.totalBalance,
                                transactions = transactions,
                                debts = debts,
                                userSettings = userSettings,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            DeepDiveMetricsContent(
                                summary = summary,
                                userSettings = userSettings,
                                strings = strings,
                                colors = colors,
                                isArabic = isArabic,
                                isStealth = isStealth,
                                typo = typo
                            )
                        }

                        item { Spacer(modifier = Modifier.height(dimens.spacingXl)) }
                    }
                }
            } else {
                // Single-column smooth vertical flow for Phones
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = dimens.screenPaddingH),
                    verticalArrangement = Arrangement.spacedBy(dimens.spacingMd)
                ) {
                    item { Spacer(modifier = Modifier.height(dimens.spacingXs)) }

                    item {
                        SavingsWalletCard(
                            savingsBalance = summary.savingsBalance,
                            availableBalance = summary.totalBalance,
                            grossBalance = summary.grossBalance,
                            isArabic = isArabic,
                            isStealth = isStealth,
                            onOpenDeposit = { showDepositDialog = true },
                            onOpenWithdraw = { showWithdrawDialog = true },
                            onOpenSpend = { showSpendDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        SmartBudgetAlertCard(
                            transactions = transactions,
                            isArabic = isArabic,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        FinancialInsightsCard(
                            insights = insights,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        BalanceProjectionChart(
                            currentBalance = summary.totalBalance,
                            transactions = transactions,
                            debts = debts,
                            userSettings = userSettings,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        DeepDiveMetricsContent(
                            summary = summary,
                            userSettings = userSettings,
                            strings = strings,
                            colors = colors,
                            isArabic = isArabic,
                            isStealth = isStealth,
                            typo = typo
                        )
                    }

                    item { Spacer(modifier = Modifier.height(dimens.spacingXl)) }
                }
            }
        }

        // Dialog 1: Deposit to Savings (تحويل للادخار)
        if (showDepositDialog) {
            DepositToSavingsDialog(
                availableBalance = summary.totalBalance,
                isArabic = isArabic,
                onDismiss = { showDepositDialog = false },
                onConfirm = { amount ->
                    onTransferToSavings(amount)
                    showDepositDialog = false
                }
            )
        }

        // Dialog 2: Withdraw from Savings (سحب للمتاح)
        if (showWithdrawDialog) {
            WithdrawFromSavingsDialog(
                savingsBalance = summary.savingsBalance,
                isArabic = isArabic,
                onDismiss = { showWithdrawDialog = false },
                onConfirm = { amount ->
                    onWithdrawFromSavings(amount)
                    showWithdrawDialog = false
                }
            )
        }

        // Dialog 3: Spend directly from Savings (صرف من الادخار)
        if (showSpendDialog) {
            SpendFromSavingsDialog(
                savingsBalance = summary.savingsBalance,
                isArabic = isArabic,
                onDismiss = { showSpendDialog = false },
                onConfirm = { amount, title, category, note ->
                    onSpendFromSavings(amount, title, category, note)
                    showSpendDialog = false
                }
            )
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
private fun DeepDiveMetricsContent(
    summary: FinancialSummary,
    userSettings: UserSettings,
    strings: StringsBundle,
    colors: com.example.ui.theme.LiquidGlassColors,
    isArabic: Boolean,
    isStealth: Boolean,
    typo: com.example.ui.theme.AdaptiveTypography
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = if (isArabic) "مؤشرات الذكاء المالي التفصيلية" else "Deep-Dive Financial Metrics",
            color = colors.textPrimary,
            fontSize = typo.titleSm,
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
            subtitle = if (isArabic) "${userSettings.salaryEmployer} • يوم ${userSettings.salaryPayday}" else "${userSettings.salaryEmployer} • Day ${userSettings.salaryPayday}"
        )

        // Debt Liquidity Ratio Metric
        val debtRatio = if (summary.totalBalance > 0) (summary.pendingDebtsOwedByMe / summary.totalBalance) * 100.0 else 0.0
        InsightMetricTile(
            icon = Icons.Default.CreditCard,
            iconTint = if (debtRatio > 35.0) RoseNeon else AmberNeon,
            title = strings.debtPressure,
            value = formatDzd(summary.pendingDebtsOwedByMe, strings.currencyCode, isStealth),
            subtitle = if (isArabic) "تمثل ${debtRatio.toInt()}٪ من رصيدك الحالي" else "Represents ${debtRatio.toInt()}% of available balance"
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

/**
 * Dedicated Savings Wallet Card (محفظة الادخار).
 * Shows segregated savings balance, contextual wealth breakdown, and provides
 * instant actions to deposit from balance, withdraw to balance, or spend directly.
 */
@Composable
fun SavingsWalletCard(
    savingsBalance: Double,
    availableBalance: Double,
    grossBalance: Double,
    isArabic: Boolean,
    isStealth: Boolean,
    onOpenDeposit: () -> Unit,
    onOpenWithdraw: () -> Unit,
    onOpenSpend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LiquidGlassTheme.colors
    val strings = AppStrings.current

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        backgroundColor = colors.glassCard
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(AmberNeon.copy(alpha = 0.20f))
                            .border(1.dp, AmberNeon.copy(alpha = 0.45f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = AmberNeon,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = strings.savingsWalletTitle,
                            color = colors.textPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.3.sp
                        )
                        Text(
                            text = if (isArabic) "مدخراتك المحمية بمعزل عن الصرف اليومي" else "Segregated & protected reserve savings",
                            color = colors.textMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                // Security Shield Tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(AmberNeon.copy(alpha = 0.15f))
                        .border(1.dp, AmberNeon.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isArabic) "محمية 🛡️" else "Protected 🛡️",
                        color = AmberNeon,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Savings Balance display
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.glassSurface)
                    .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = if (isArabic) "رصيد محفظة الادخار الحالية" else "Current Savings Wallet Balance",
                    color = colors.textMuted,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatDzd(savingsBalance, strings.currencyCode, isStealth),
                    color = AmberNeon,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Available vs Gross context
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = (if (isArabic) "الرصيد المتاح للصرف: " else "Spendable: ") +
                                formatDzd(availableBalance, strings.currencyCode, isStealth),
                        color = colors.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = (if (isArabic) "إجمالي الثروة: " else "Net Wealth: ") +
                                formatDzd(grossBalance, strings.currencyCode, isStealth),
                        color = colors.textMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons Row: 3 Core Operations
            // 1. تحويل للادخار (Deposit)
            // 2. سحب للمتاح (Withdraw)
            // 3. صرف من الادخار (Spend)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Button 1: Transfer into Savings
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.primaryNeon)
                        .clickable { onOpenDeposit() }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = if (colors.isDark) Color(0xFF0F172A) else Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = strings.transferToSavings,
                            color = if (colors.isDark) Color(0xFF0F172A) else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Button 2: Withdraw to Available
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.glassSurface)
                        .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(14.dp))
                        .clickable { onOpenWithdraw() }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = colors.textPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = strings.withdrawFromSavings,
                            color = colors.textPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Button 3: Spend from Savings
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(RoseNeon.copy(alpha = 0.16f))
                        .border(1.dp, RoseNeon.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                        .clickable { onOpenSpend() }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = null,
                            tint = RoseNeon,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = strings.spendFromSavings,
                            color = RoseNeon,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Modal to transfer money from available balance to Savings Wallet.
 */
@Composable
private fun DepositToSavingsDialog(
    availableBalance: Double,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    val colors = LiquidGlassTheme.colors
    val strings = AppStrings.current
    var amountInput by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            backgroundColor = colors.glassCard
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.primaryNeon.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = colors.primaryNeon,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isArabic) "تحويل مبالغ إلى محفظة الادخار" else "Deposit to Savings Wallet",
                            color = colors.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = (if (isArabic) "الرصيد المتاح حالياً: " else "Available: ") +
                                    formatDzdAmount(availableBalance) + " " + strings.currencyCode,
                            color = colors.textMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick presets
                Text(
                    text = if (isArabic) "مبالغ سريعة:" else "Quick Presets:",
                    color = colors.textSecondary,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val presets = listOf(2000.0, 5000.0, 10000.0, 20000.0)
                    items(presets) { preset ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.glassSurface)
                                .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(10.dp))
                                .clickable { amountInput = preset.toInt().toString() }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "+${formatDzdAmount(preset)}",
                                color = colors.primaryNeon,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (availableBalance > 0) {
                        item {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(EmeraldNeon.copy(alpha = 0.16f))
                                    .border(1.dp, EmeraldNeon.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    .clickable { amountInput = availableBalance.toLong().toString() }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (isArabic) "كامل المتاح" else "All Available",
                                    color = EmeraldNeon,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text(if (isArabic) "المبلغ المراد تحويله (دج)" else "Amount to Deposit (DZD)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = colors.primaryNeon,
                        unfocusedBorderColor = colors.glassBorderSubtle
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (isArabic)
                        "💡 ينقص هذا المبلغ من الرصيد الإجمالي المتاح ويضاف لمحفظة الادخار المحمية."
                    else
                        "💡 This amount is deducted from available balance and added to your protected savings.",
                    color = colors.textMuted,
                    fontSize = 10.5.sp,
                    lineHeight = 14.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassButton(
                        text = strings.cancel,
                        containerColor = colors.glassCard,
                        contentColor = colors.textMuted,
                        modifier = Modifier.weight(1f),
                        onClick = onDismiss
                    )
                    GlassButton(
                        text = if (isArabic) "تأكيد التحويل" else "Deposit",
                        containerColor = colors.primaryNeon,
                        contentColor = if (colors.isDark) Color(0xFF0F172A) else Color.White,
                        modifier = Modifier.weight(1.2f),
                        onClick = {
                            val amt = amountInput.toDoubleOrNull() ?: 0.0
                            if (amt > 0) {
                                onConfirm(amt)
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Modal to withdraw money from Savings Wallet back to available spendable balance.
 */
@Composable
private fun WithdrawFromSavingsDialog(
    savingsBalance: Double,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    val colors = LiquidGlassTheme.colors
    val strings = AppStrings.current
    var amountInput by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            backgroundColor = colors.glassCard
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AmberNeon.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = AmberNeon,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isArabic) "سحب مبالغ من محفظة الادخار" else "Withdraw from Savings",
                            color = colors.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = (if (isArabic) "رصيد الادخار الحالي: " else "Savings Balance: ") +
                                    formatDzdAmount(savingsBalance) + " " + strings.currencyCode,
                            color = AmberNeon,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Presets
                Text(
                    text = if (isArabic) "مبالغ سريعة:" else "Quick Presets:",
                    color = colors.textSecondary,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val presets = listOf(2000.0, 5000.0, 10000.0)
                    items(presets) { preset ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.glassSurface)
                                .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(10.dp))
                                .clickable { amountInput = preset.toInt().toString() }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = formatDzdAmount(preset),
                                color = colors.textPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (savingsBalance > 0) {
                        item {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AmberNeon.copy(alpha = 0.16f))
                                    .border(1.dp, AmberNeon.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    .clickable { amountInput = savingsBalance.toLong().toString() }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (isArabic) "كامل المدخرات" else "All Savings",
                                    color = AmberNeon,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text(if (isArabic) "المبلغ المراد سحبه للمتاح (دج)" else "Amount to Withdraw (DZD)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = AmberNeon,
                        unfocusedBorderColor = colors.glassBorderSubtle
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (isArabic)
                        "🔄 يتم استرجاع هذا المبلغ وإضافته تلقائياً إلى الرصيد الإجمالي المتاح للصرف اليومي."
                    else
                        "🔄 This amount is returned and added back to your daily spendable balance.",
                    color = colors.textMuted,
                    fontSize = 10.5.sp,
                    lineHeight = 14.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassButton(
                        text = strings.cancel,
                        containerColor = colors.glassCard,
                        contentColor = colors.textMuted,
                        modifier = Modifier.weight(1f),
                        onClick = onDismiss
                    )
                    GlassButton(
                        text = if (isArabic) "تأكيد السحب للمتاح" else "Withdraw",
                        containerColor = AmberNeon,
                        contentColor = Color(0xFF0F172A),
                        modifier = Modifier.weight(1.2f),
                        onClick = {
                            val amt = amountInput.toDoubleOrNull() ?: 0.0
                            if (amt > 0) {
                                onConfirm(amt)
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Modal to spend directly from Savings Wallet.
 * Deducts from savings balance AND automatically records the transaction in Expenses page.
 */
@Composable
private fun SpendFromSavingsDialog(
    savingsBalance: Double,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, title: String, category: String, note: String) -> Unit
) {
    val colors = LiquidGlassTheme.colors
    val strings = AppStrings.current

    var amountInput by remember { mutableStateOf("") }
    var titleInput by remember { mutableStateOf("") }
    var categorySelected by remember { mutableStateOf("shopping") }
    var noteInput by remember { mutableStateOf("") }

    val categories = remember { ExpenseCategories.getExpenseCategories() }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            backgroundColor = colors.glassCard
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(RoseNeon.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = null,
                            tint = RoseNeon,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isArabic) "صرف مباشر من محفظة الادخار 💳" else "Spend from Savings 💳",
                            color = colors.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = (if (isArabic) "رصيد الادخار المتاح: " else "Savings Available: ") +
                                    formatDzdAmount(savingsBalance) + " " + strings.currencyCode,
                            color = AmberNeon,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text(if (isArabic) "مبلغ المصروف (دج)*" else "Expense Amount (DZD)*") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = RoseNeon,
                        unfocusedBorderColor = colors.glassBorderSubtle
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text(if (isArabic) "بيان المصروف (السبب / الغرض)*" else "Expense Title / Reason*") },
                    placeholder = { Text(if (isArabic) "مثال: شراء جهاز، طوارئ صحية..." else "e.g. Laptop, Medical emergency...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = RoseNeon,
                        unfocusedBorderColor = colors.glassBorderSubtle
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isArabic) "التصنيف:" else "Category:",
                        color = colors.textSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = strings.categoryName(categorySelected),
                        color = RoseNeon,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { cat ->
                        val isSelected = cat.id == categorySelected
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) RoseNeon.copy(alpha = 0.22f) else colors.glassSurface)
                                .border(
                                    1.dp,
                                    if (isSelected) RoseNeon else colors.glassBorderSubtle,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    val prev = strings.categoryName(categorySelected)
                                    categorySelected = cat.id
                                    if (titleInput.isBlank() || titleInput == prev) {
                                        titleInput = strings.categoryName(cat.id)
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) RoseNeon else colors.textPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = strings.categoryName(cat.id),
                                    color = if (isSelected) RoseNeon else colors.textPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = { Text(if (isArabic) "ملاحظة إضافية (اختياري)" else "Note (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = colors.primaryNeon,
                        unfocusedBorderColor = colors.glassBorderSubtle
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Important policy banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(RoseNeon.copy(alpha = 0.12f))
                        .border(1.dp, RoseNeon.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = if (isArabic)
                            "⚠️ ينقص هذا المبلغ مباشرة من رصيد محفظة الادخار، ويسجل تلقائياً في صفحة المصاريف مع وسم 'محفظة الادخار'."
                        else
                            "⚠️ This amount will be deducted directly from your savings wallet and recorded in the Expenses page.",
                        color = RoseNeon,
                        fontSize = 10.5.sp,
                        lineHeight = 14.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassButton(
                        text = strings.cancel,
                        containerColor = colors.glassCard,
                        contentColor = colors.textMuted,
                        modifier = Modifier.weight(1f),
                        onClick = onDismiss
                    )
                    GlassButton(
                        text = if (isArabic) "تسجيل وصرف" else "Spend & Record",
                        containerColor = RoseNeon,
                        contentColor = Color.White,
                        modifier = Modifier.weight(1.2f),
                        onClick = {
                            val amt = amountInput.toDoubleOrNull() ?: 0.0
                            if (amt > 0) {
                                onConfirm(amt, titleInput, categorySelected, noteInput)
                            }
                        }
                    )
                }
            }
        }
    }
}

