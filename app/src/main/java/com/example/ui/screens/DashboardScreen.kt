package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.data.UserSettings
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.LiquidEmptyStateCard
import com.example.ui.components.QuickActionButton
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.formatDate
import com.example.ui.i18n.formatDzd
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DebtBtnGradient
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.ExpenseBtnGradient
import com.example.ui.theme.IncomeBtnGradient
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.RoseNeon
import com.example.viewmodel.FinancialSummary
import java.util.Calendar

@Composable
fun DashboardScreen(
    summary: FinancialSummary,
    transactions: List<TransactionEntity>,
    userSettings: UserSettings,
    onAddIncomeClick: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onAddDebtClick: () -> Unit,
    onAddTransactionClick: () -> Unit = {},
    onSeeAllClick: () -> Unit,
    onOpenSalaryScheduler: () -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Main Liquid Glass Wallet Balance Card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = colors.glassCard,
                shape = RoundedCornerShape(26.dp)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.totalBalance,
                            color = colors.textSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.primaryNeon.copy(alpha = 0.18f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = strings.currencyCode,
                                color = colors.primaryNeon,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Prominent Balance
                    Text(
                        text = formatDzd(summary.totalBalance, strings.currencyCode),
                        color = colors.textPrimary,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Monthly Income vs Expense Mini Stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Income
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.glassSurface)
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldNeon.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingUp,
                                        contentDescription = null,
                                        tint = EmeraldNeon,
                                        modifier = Modifier.size(18.dp)
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
                                        text = formatDzd(summary.monthlyIncome, strings.currencyCode),
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
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.glassSurface)
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(RoseNeon.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingDown,
                                        contentDescription = null,
                                        tint = RoseNeon,
                                        modifier = Modifier.size(18.dp)
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
                                        text = formatDzd(summary.monthlyExpenses, strings.currencyCode),
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

        // Smart Salary Scheduler Card Banner
        item {
            val cal = Calendar.getInstance()
            val currentDay = cal.get(Calendar.DAY_OF_MONTH)
            val daysUntilPayday = (userSettings.salaryPayday - currentDay).let {
                if (it < 0) it + 30 else it
            }

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = colors.glassSurface,
                shape = RoundedCornerShape(20.dp),
                onClick = onOpenSalaryScheduler
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(CyanNeon, colors.secondaryNeon)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.salarySchedulerTitle,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = formatDzd(userSettings.salaryAmount, strings.currencyCode),
                                color = EmeraldNeon,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "•",
                                color = colors.textMuted,
                                fontSize = 12.sp
                            )
                            val paydayStatus = if (currentDay == userSettings.salaryPayday) {
                                strings.salaryDepositedToday
                            } else {
                                "Day ${userSettings.salaryPayday} ($daysUntilPayday d)"
                            }
                            Text(
                                text = paydayStatus,
                                color = colors.primaryNeon,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Quick Actions Row (+ Income, + Expense, + Debt)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionButton(
                    text = strings.addIncome,
                    icon = Icons.Default.Add,
                    brush = IncomeBtnGradient,
                    borderColor = Color(0x6034D399),
                    modifier = Modifier.weight(1f),
                    onClick = onAddIncomeClick
                )
                QuickActionButton(
                    text = strings.addExpense,
                    icon = Icons.Default.Add,
                    brush = ExpenseBtnGradient,
                    borderColor = Color(0x60FB7185),
                    modifier = Modifier.weight(1f),
                    onClick = onAddExpenseClick
                )
                QuickActionButton(
                    text = strings.addDebt,
                    icon = Icons.Default.CreditCard,
                    brush = DebtBtnGradient,
                    borderColor = Color(0x6038BDF8),
                    modifier = Modifier.weight(1f),
                    onClick = onAddDebtClick
                )
            }
        }

        // Active Debts Mini Banner
        item {
            if (summary.pendingDebtsOwedByMe > 0 || summary.pendingDebtsOwedToMe > 0) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = colors.glassCard,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = null,
                                tint = AmberNeon,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.activeDebts,
                                color = colors.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Text(
                            text = "I Owe: ${formatDzd(summary.pendingDebtsOwedByMe, strings.currencyCode)}",
                            color = AmberNeon,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Recent Transactions Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.recentTransactions,
                    color = colors.textPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = strings.seeAll,
                    color = colors.primaryNeon,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onSeeAllClick() }
                )
            }
        }

        // Recent Transactions List (first 6 items)
        val recentList = transactions.take(6)
        if (recentList.isEmpty()) {
            item {
                LiquidEmptyStateCard(
                    title = strings.emptyTransactionsTitle,
                    description = strings.emptyTransactionsDesc,
                    icon = Icons.Default.Receipt,
                    iconColor = CyanNeon,
                    actionText = strings.addFirstTransaction,
                    onActionClick = onAddTransactionClick
                )
            }
        } else {
            items(recentList, key = { it.id }) { item ->
                TransactionRowItem(
                    transaction = item,
                    currencyCode = strings.currencyCode,
                    onDelete = { onDeleteTransaction(item) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun TransactionRowItem(
    transaction: TransactionEntity,
    currencyCode: String,
    onDelete: () -> Unit
) {
    val colors = LiquidGlassTheme.colors
    val strings = AppStrings.current

    val isIncome = transaction.type == TransactionType.INCOME
    val amountColor = if (isIncome) EmeraldNeon else RoseNeon
    val prefix = if (isIncome) "+" else "-"

    val icon: ImageVector = when (transaction.category.lowercase()) {
        "salary" -> Icons.Default.Paid
        "freelance" -> Icons.Default.Work
        "internet", "telecom" -> Icons.Default.Wifi
        "transfer" -> Icons.Default.Send
        "withdrawal" -> Icons.Default.LocalAtm
        "food" -> Icons.Default.Fastfood
        "shopping" -> Icons.Default.ShoppingCart
        "bills" -> Icons.Default.Receipt
        "debt_payment", "debt_collection" -> Icons.Default.CreditCard
        else -> Icons.Default.AccountBalanceWallet
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = colors.glassCard,
        shape = RoundedCornerShape(18.dp)
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
                    .background(amountColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = amountColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.title,
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${strings.categoryName(transaction.category)} • ${formatDate(transaction.dateMillis)}",
                    color = colors.textMuted,
                    fontSize = 11.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$prefix ${formatDzd(transaction.amount, currencyCode)}",
                    color = amountColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                if (transaction.isSalaryCredit) {
                    Text(
                        text = "Salary Credit",
                        color = colors.primaryNeon,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = colors.textMuted.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
