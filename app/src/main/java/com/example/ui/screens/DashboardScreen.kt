package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DebtEntity
import com.example.data.ExpenseCategories
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.data.UserSettings
import com.example.sms.SmsSuggestionEntry
import com.example.ui.components.GlassCard
import com.example.ui.components.LiquidEmptyStateCard
import com.example.ui.components.QuickActionButton
import com.example.ui.components.TotalBalanceVaultCard
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.ArabicStrings
import com.example.ui.i18n.StringsBundle
import com.example.ui.i18n.formatDate
import com.example.ui.i18n.formatDzd
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DebtBtnGradient
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.ExpenseBtnGradient
import com.example.ui.theme.IncomeBtnGradient
import com.example.ui.theme.LiquidGlassColors
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.LocalAdaptiveDimens
import com.example.ui.theme.LocalAdaptiveLayoutInfo
import com.example.ui.theme.LocalAdaptiveTypography
import com.example.ui.theme.RoseNeon
import com.example.viewmodel.FinancialSummary
import java.util.Calendar

@Composable
fun DashboardScreen(
    summary: FinancialSummary,
    transactions: List<TransactionEntity>,
    debts: List<DebtEntity> = emptyList(),
    userSettings: UserSettings,
    pendingSmsSuggestions: List<SmsSuggestionEntry> = emptyList(),
    onAddIncomeClick: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onAddDebtClick: () -> Unit,
    onAddTransactionClick: () -> Unit = {},
    onSeeAllClick: () -> Unit,
    onOpenSalaryScheduler: () -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onLockToVault: (Double) -> Unit = {},
    onUnlockFromVault: (Double) -> Unit = {},
    onAcceptSmsSuggestion: (String, TransactionType?) -> Unit = { _, _ -> },
    onDismissSmsSuggestion: (String) -> Unit = {},
    onOpenBaridiMobSync: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors
    val layoutInfo = LocalAdaptiveLayoutInfo.current
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current

    if (layoutInfo.columns >= 2) {
        // Multi-column adaptive layout for Tablets, Foldables & Large screens
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = dimens.screenPaddingH),
            horizontalArrangement = Arrangement.spacedBy(dimens.spacingLg)
        ) {
            // Left Column (Financial Health, Scheduler, Quick Actions, Debt overview)
            LazyColumn(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(dimens.spacingMd)
            ) {
                item { Spacer(modifier = Modifier.height(dimens.spacingXs)) }

                if (pendingSmsSuggestions.isNotEmpty()) {
                    item {
                        BaridiMobSmsSuggestionBanner(
                            suggestions = pendingSmsSuggestions,
                            strings = strings,
                            colors = colors,
                            onAccept = { id, type -> onAcceptSmsSuggestion(id, type) },
                            onDismiss = onDismissSmsSuggestion,
                            onOpenSync = onOpenBaridiMobSync
                        )
                    }
                }

                item {
                    TotalBalanceVaultCard(
                        summary = summary,
                        userSettings = userSettings,
                        onLockToVault = onLockToVault,
                        onUnlockFromVault = onUnlockFromVault
                    )
                }

                item {
                    SalarySchedulerCard(
                        userSettings = userSettings,
                        strings = strings,
                        colors = colors,
                        onClick = onOpenSalaryScheduler
                    )
                }

                item {
                    QuickActionsRow(
                        strings = strings,
                        onAddIncomeClick = onAddIncomeClick,
                        onAddExpenseClick = onAddExpenseClick,
                        onAddDebtClick = onAddDebtClick
                    )
                }

                if (summary.pendingDebtsOwedByMe > 0 || summary.pendingDebtsOwedToMe > 0) {
                    item {
                        ActiveDebtsBanner(
                            summary = summary,
                            strings = strings,
                            colors = colors
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(dimens.spacingXl)) }
            }

            // Right Column (Activity Stream & Recent Transactions)
            LazyColumn(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(dimens.spacingMd)
            ) {
                item { Spacer(modifier = Modifier.height(dimens.spacingXs)) }

                item {
                    RecentTransactionsHeader(
                        strings = strings,
                        colors = colors,
                        onSeeAllClick = onSeeAllClick
                    )
                }

                val recentList = transactions.take(10)
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

                item { Spacer(modifier = Modifier.height(dimens.spacingXl)) }
            }
        }
    } else {
        // Single-column smooth vertical flow for Phones (sw < 600dp portrait)
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = dimens.screenPaddingH),
            verticalArrangement = Arrangement.spacedBy(dimens.spacingMd)
        ) {
            item { Spacer(modifier = Modifier.height(dimens.spacingXs)) }

            if (pendingSmsSuggestions.isNotEmpty()) {
                item {
                    BaridiMobSmsSuggestionBanner(
                        suggestions = pendingSmsSuggestions,
                        strings = strings,
                        colors = colors,
                        onAccept = { id, type -> onAcceptSmsSuggestion(id, type) },
                        onDismiss = onDismissSmsSuggestion,
                        onOpenSync = onOpenBaridiMobSync
                    )
                }
            }

            item {
                TotalBalanceVaultCard(
                    summary = summary,
                    userSettings = userSettings,
                    onLockToVault = onLockToVault,
                    onUnlockFromVault = onUnlockFromVault
                )
            }

            item {
                SalarySchedulerCard(
                    userSettings = userSettings,
                    strings = strings,
                    colors = colors,
                    onClick = onOpenSalaryScheduler
                )
            }

            item {
                QuickActionsRow(
                    strings = strings,
                    onAddIncomeClick = onAddIncomeClick,
                    onAddExpenseClick = onAddExpenseClick,
                    onAddDebtClick = onAddDebtClick
                )
            }

            if (summary.pendingDebtsOwedByMe > 0 || summary.pendingDebtsOwedToMe > 0) {
                item {
                    ActiveDebtsBanner(
                        summary = summary,
                        strings = strings,
                        colors = colors
                    )
                }
            }

            item {
                RecentTransactionsHeader(
                    strings = strings,
                    colors = colors,
                    onSeeAllClick = onSeeAllClick
                )
            }

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

            item { Spacer(modifier = Modifier.height(dimens.spacingXl)) }
        }
    }
}

@Composable
private fun SalarySchedulerCard(
    userSettings: UserSettings,
    strings: StringsBundle,
    colors: LiquidGlassColors,
    onClick: () -> Unit
) {
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current

    val cal = Calendar.getInstance()
    val currentDay = cal.get(Calendar.DAY_OF_MONTH)
    val daysUntilPayday = (userSettings.salaryPayday - currentDay).let {
        if (it < 0) it + 30 else it
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = colors.glassCard,
        shape = RoundedCornerShape(dimens.cornerRadiusLg),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(dimens.cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(dimens.iconSizeXl + 4.dp)
                    .clip(RoundedCornerShape(dimens.cornerRadiusSm))
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
                    modifier = Modifier.size(dimens.iconSizeMd)
                )
            }

            Spacer(modifier = Modifier.width(dimens.spacingMd))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.salarySchedulerTitle,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = typo.bodyLg
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dimens.spacingSm)
                ) {
                    Text(
                        text = formatDzd(userSettings.salaryAmount, strings.currencyCode),
                        color = EmeraldNeon,
                        fontSize = typo.body,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "•",
                        color = colors.textMuted,
                        fontSize = typo.caption
                    )
                    val isArabic = strings is ArabicStrings
                    val paydayStatus = if (currentDay == userSettings.salaryPayday) {
                        strings.salaryDepositedToday
                    } else if (isArabic) {
                        "يوم ${userSettings.salaryPayday} (متبقٍ $daysUntilPayday يوم)"
                    } else {
                        "Day ${userSettings.salaryPayday} ($daysUntilPayday d)"
                    }
                    Text(
                        text = paydayStatus,
                        color = colors.primaryNeon,
                        fontSize = typo.bodySm,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(dimens.iconSizeSm)
            )
        }
    }
}

@Composable
private fun QuickActionsRow(
    strings: StringsBundle,
    onAddIncomeClick: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onAddDebtClick: () -> Unit
) {
    val dimens = LocalAdaptiveDimens.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimens.spacingSm)
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

@Composable
private fun ActiveDebtsBanner(
    summary: FinancialSummary,
    strings: StringsBundle,
    colors: LiquidGlassColors
) {
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = colors.glassCard,
        shape = RoundedCornerShape(dimens.cornerRadiusMd)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimens.cardPaddingSmall),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CreditCard,
                    contentDescription = null,
                    tint = AmberNeon,
                    modifier = Modifier.size(dimens.iconSizeMd)
                )
                Spacer(modifier = Modifier.width(dimens.spacingSm))
                Text(
                    text = strings.activeDebts,
                    color = colors.textPrimary,
                    fontSize = typo.body,
                    fontWeight = FontWeight.SemiBold
                )
            }

            val isArabic = strings is ArabicStrings
            Text(
                text = (if (isArabic) "ديون عليّ: " else "I Owe: ") + formatDzd(summary.pendingDebtsOwedByMe, strings.currencyCode),
                color = AmberNeon,
                fontSize = typo.bodySm,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun RecentTransactionsHeader(
    strings: StringsBundle,
    colors: LiquidGlassColors,
    onSeeAllClick: () -> Unit
) {
    val typo = LocalAdaptiveTypography.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = strings.recentTransactions,
            color = colors.textPrimary,
            fontSize = typo.title,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = strings.seeAll,
            color = colors.primaryNeon,
            fontSize = typo.body,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable { onSeeAllClick() }
        )
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
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current

    val isIncome = transaction.type == TransactionType.INCOME
    val amountColor = if (isIncome) EmeraldNeon else RoseNeon
    val prefix = if (isIncome) "+" else "-"

    val icon: ImageVector = ExpenseCategories.getIcon(transaction.category)

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = colors.glassCard,
        shape = RoundedCornerShape(dimens.cornerRadiusMd)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimens.cardPaddingSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(dimens.iconSizeXl + 4.dp)
                    .clip(RoundedCornerShape(dimens.cornerRadiusSm))
                    .background(amountColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = amountColor,
                    modifier = Modifier.size(dimens.iconSizeSm)
                )
            }

            Spacer(modifier = Modifier.width(dimens.spacingMd))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.title,
                    color = colors.textPrimary,
                    fontSize = typo.titleSm,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${strings.categoryName(transaction.category)} • ${formatDate(transaction.dateMillis)}",
                    color = colors.textMuted,
                    fontSize = typo.caption
                )
                if (transaction.note.isNotBlank()) {
                    Text(
                        text = transaction.note,
                        color = colors.textMuted.copy(alpha = 0.8f),
                        fontSize = typo.caption,
                        maxLines = 1
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$prefix ${formatDzd(transaction.amount, currencyCode)}",
                    color = amountColor,
                    fontSize = typo.body,
                    fontWeight = FontWeight.Bold
                )
                if (transaction.isSalaryCredit) {
                    Text(
                        text = "Salary Credit",
                        color = colors.primaryNeon,
                        fontSize = typo.caption,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (transaction.isPaidFromSavings) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .clip(RoundedCornerShape(dimens.cornerRadiusXs))
                            .background(AmberNeon.copy(alpha = 0.16f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = AmberNeon,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = strings.paidFromSavingsBadge,
                            color = AmberNeon,
                            fontSize = typo.caption,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = colors.textMuted.copy(alpha = 0.6f),
                    modifier = Modifier.size(dimens.iconSizeSm)
                )
            }
        }
    }
}

@Composable
private fun BaridiMobSmsSuggestionBanner(
    suggestions: List<SmsSuggestionEntry>,
    strings: StringsBundle,
    colors: LiquidGlassColors,
    onAccept: (id: String, type: TransactionType) -> Unit,
    onDismiss: (id: String) -> Unit,
    onOpenSync: () -> Unit
) {
    val latest = suggestions.firstOrNull() ?: return
    val isArabic = strings is ArabicStrings
    val isExpense = latest.isExpense
    val accent = if (isExpense) RoseNeon else EmeraldNeon

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = colors.glassCard,
        borderColor = Color(0xFF818CF8).copy(alpha = 0.45f)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF818CF8).copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sms,
                            contentDescription = null,
                            tint = Color(0xFF818CF8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isArabic) {
                                "رسالة جديدة من ${latest.sender} (${suggestions.size})"
                            } else {
                                "New ${latest.sender} SMS (${suggestions.size})"
                            },
                            color = colors.textPrimary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isArabic) latest.suggestedTitleAr else latest.suggestedTitleEn,
                            color = colors.textMuted,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = { onDismiss(latest.id) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = if (isArabic) "تجاهل" else "Dismiss",
                        tint = colors.textMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.glassSurface)
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = latest.rawBody,
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = (if (isExpense) "-" else "+") + formatDzd(latest.amount, strings.currencyCode),
                    color = accent,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accent.copy(alpha = 0.18f))
                        .clickable { onAccept(latest.id, latest.transactionType) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isExpense) {
                            if (isArabic) "إضافة في المصاريف" else "Add to Expense"
                        } else {
                            if (isArabic) "إضافة في الدخل" else "Add to Income"
                        },
                        color = accent,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.glassSurface)
                        .clickable(onClick = onOpenSync)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isArabic) "عرض الكل" else "View All",
                        color = Color(0xFF818CF8),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
