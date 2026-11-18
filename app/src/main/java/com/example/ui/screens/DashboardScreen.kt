package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HistoryToggleOff
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DebtEntity
import com.example.data.ExpenseCategories
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.data.UserSettings
import com.example.sms.AlgPosteSmsInboxScanner
import com.example.sms.SmsSuggestionEntry
import com.example.sms.SmsTaxAnomalyEntry
import com.example.ui.components.GlassCard
import com.example.ui.components.LiquidEmptyStateCard
import com.example.ui.components.QuickActionButton
import com.example.ui.components.TotalBalanceVaultCard
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.ArabicStrings
import com.example.ui.i18n.LocalStealthMode
import com.example.ui.i18n.StringsBundle
import com.example.ui.i18n.formatDate
import com.example.ui.i18n.formatDzd
import com.example.ui.i18n.formatDzdAmount
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    summary: FinancialSummary,
    transactions: List<TransactionEntity>,
    debts: List<DebtEntity> = emptyList(),
    userSettings: UserSettings,
    pendingSmsSuggestions: List<SmsSuggestionEntry> = emptyList(),
    detectedSmsTaxes: List<SmsTaxAnomalyEntry> = emptyList(),
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
    onRefreshCcpBalanceFromSms: (showFeedback: Boolean) -> Unit = {},
    onToggleAlgPostBalanceMode: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors
    val isStealth = LocalStealthMode.current
    val layoutInfo = LocalAdaptiveLayoutInfo.current
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current
    val context = LocalContext.current

    // Auto-read latest Alg Post balance from ALG Poste / BaridiMob SMS once on dashboard load if permission is granted
    LaunchedEffect(Unit) {
        if (AlgPosteSmsInboxScanner.hasSmsPermissions(context)) {
            onRefreshCcpBalanceFromSms(false)
        }
    }

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
                        onUnlockFromVault = onUnlockFromVault,
                        onOpenSalaryScheduler = onOpenSalaryScheduler,
                        onToggleAlgPostBalanceMode = onToggleAlgPostBalanceMode
                    )
                }

                item {
                    CcpBalanceCard(
                        userSettings = userSettings,
                        pendingCount = pendingSmsSuggestions.size,
                        detectedTaxes = detectedSmsTaxes,
                        isStealth = isStealth,
                        strings = strings,
                        colors = colors,
                        onOpenBaridiMobSync = onOpenBaridiMobSync
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
                    onUnlockFromVault = onUnlockFromVault,
                    onOpenSalaryScheduler = onOpenSalaryScheduler,
                    onToggleAlgPostBalanceMode = onToggleAlgPostBalanceMode
                )
            }

            item {
                CcpBalanceCard(
                    userSettings = userSettings,
                    pendingCount = pendingSmsSuggestions.size,
                    detectedTaxes = detectedSmsTaxes,
                    isStealth = isStealth,
                    strings = strings,
                    colors = colors,
                    onOpenBaridiMobSync = onOpenBaridiMobSync
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
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
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(dimens.spacingSm))

            val isArabic = strings is ArabicStrings
            Text(
                text = (if (isArabic) "ديون عليّ: " else "I Owe: ") + formatDzd(summary.pendingDebtsOwedByMe, strings.currencyCode),
                color = AmberNeon,
                fontSize = typo.bodySm,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = strings.seeAll,
            color = colors.primaryNeon,
            fontSize = typo.body,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onSeeAllClick() }
                .padding(horizontal = 8.dp, vertical = 6.dp)
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
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${strings.categoryName(transaction.category)} • ${formatDate(transaction.dateMillis)}",
                    color = colors.textMuted,
                    fontSize = typo.caption,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (transaction.note.isNotBlank()) {
                    Text(
                        text = transaction.note,
                        color = colors.textMuted.copy(alpha = 0.8f),
                        fontSize = typo.caption,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(dimens.spacingSm))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$prefix ${formatDzd(transaction.amount, currencyCode)}",
                    color = amountColor,
                    fontSize = typo.body,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (transaction.isSalaryCredit) {
                    Text(
                        text = "Salary Credit",
                        color = colors.primaryNeon,
                        fontSize = typo.caption,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
                if (transaction.isAutoAlgPoste) {
                    Text(
                        text = strings.autoAlgPostBadge,
                        color = CyanNeon,
                        fontSize = typo.caption,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
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
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(40.dp)
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
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current
    val isExpense = latest.isExpense
    val accent = if (isExpense) RoseNeon else EmeraldNeon
    val oppositeType = if (isExpense) TransactionType.INCOME else TransactionType.EXPENSE
    val oppositeAccent = if (isExpense) EmeraldNeon else RoseNeon

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard_sms_suggestion_banner"),
        backgroundColor = colors.glassCard,
        borderColor = accent.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(dimens.cardPadding)) {
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
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sms,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isArabic) {
                                "عملية في Alg Post Balance (${suggestions.size})"
                            } else {
                                "Alg Post Balance Operation (${suggestions.size})"
                            },
                            color = colors.textPrimary,
                            fontSize = typo.bodySm,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isArabic) latest.suggestedTitleAr else latest.suggestedTitleEn,
                            color = colors.textMuted,
                            fontSize = typo.caption,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.glassSurface)
                        .clickable(onClick = onOpenSync)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isArabic) "عرض الكل" else "All",
                        color = Color(0xFF818CF8),
                        fontSize = typo.caption,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.glassSurface)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = latest.rawBody,
                        color = colors.textSecondary,
                        fontSize = typo.caption,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (latest.previousBalance != null && latest.extractedBalance != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${formatDzd(latest.previousBalance, strings.currencyCode)} → ${formatDzd(latest.extractedBalance, strings.currencyCode)}",
                            color = CyanNeon,
                            fontSize = typo.caption,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = (if (isExpense) "-" else "+") + formatDzd(latest.amount, strings.currencyCode),
                    color = accent,
                    fontSize = typo.body,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(9.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Primary button: Add directly to detected Income or Expense
                Box(
                    modifier = Modifier
                        .weight(1.35f)
                        .defaultMinSize(minHeight = 44.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(accent.copy(alpha = 0.2f))
                        .border(0.8.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(11.dp))
                        .clickable { onAccept(latest.id, latest.transactionType) }
                        .padding(vertical = 8.dp, horizontal = 8.dp)
                        .testTag("sms_suggestion_accept_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isExpense) {
                            if (isArabic) "إضافة في المصاريف" else "Add to Expense"
                        } else {
                            if (isArabic) "إضافة في الدخل" else "Add to Income"
                        },
                        color = accent,
                        fontSize = typo.bodyXs,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Quick switch button: allow filing under the other side (Income <-> Expense)
                Box(
                    modifier = Modifier
                        .weight(0.95f)
                        .defaultMinSize(minHeight = 44.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(oppositeAccent.copy(alpha = 0.12f))
                        .border(0.8.dp, oppositeAccent.copy(alpha = 0.3f), RoundedCornerShape(11.dp))
                        .clickable { onAccept(latest.id, oppositeType) }
                        .padding(vertical = 8.dp, horizontal = 6.dp)
                        .testTag("sms_suggestion_switch_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isExpense) {
                            if (isArabic) "أو الدخل" else "As Income"
                        } else {
                            if (isArabic) "أو مصروف" else "As Expense"
                        },
                        color = oppositeAccent,
                        fontSize = typo.caption,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Explicit Dismiss / Ignore button
                Box(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 44.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(colors.glassSurface)
                        .border(0.8.dp, colors.glassBorderSubtle, RoundedCornerShape(11.dp))
                        .clickable { onDismiss(latest.id) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("sms_suggestion_dismiss_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isArabic) "تجاهل" else "Dismiss",
                        color = colors.textMuted,
                        fontSize = typo.bodyXs,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CcpBalanceCard(
    userSettings: UserSettings,
    pendingCount: Int,
    detectedTaxes: List<SmsTaxAnomalyEntry>,
    isStealth: Boolean,
    strings: StringsBundle,
    colors: LiquidGlassColors,
    onOpenBaridiMobSync: () -> Unit
) {
    val isArabic = strings is ArabicStrings
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current

    val hasBalance = userSettings.latestSmsBalanceUpdatedAt > 0L || userSettings.latestSmsBalanceAmount >= 0.0
    val isNegativeBalance = hasBalance && userSettings.latestSmsBalanceAmount < 0.0
    val balanceAccentColor = when {
        isNegativeBalance -> RoseNeon
        hasBalance -> EmeraldNeon
        else -> Color(0xFFFACC15)
    }
    val hasBaseline = userSettings.smsSyncBaselineTimestampMillis > 0L
    val totalTaxes = detectedTaxes.sumOf { it.taxAmount }
    val unrecordedTaxes = detectedTaxes.count { !it.isRecordedAsExpense }

    val posteGold = Color(0xFFFACC15)
    val posteNavy = Color(0xFF1E3A8A)

    val updatedText = remember(userSettings.latestSmsBalanceUpdatedAt, isArabic) {
        if (userSettings.latestSmsBalanceUpdatedAt > 0L) {
            SimpleDateFormat("dd/MM/yyyy • HH:mm", Locale.getDefault())
                .format(Date(userSettings.latestSmsBalanceUpdatedAt))
        } else ""
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard_ccp_balance_card"),
        backgroundColor = colors.glassCard,
        borderColor = posteGold.copy(alpha = 0.45f),
        shape = RoundedCornerShape(dimens.cornerRadiusLg),
        cornerRadius = dimens.cornerRadiusLg,
        onClick = onOpenBaridiMobSync
    ) {
        if (!colors.isNeumorphic) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val glowCenter = Offset(size.width * 0.85f, size.height * 0.25f)
                val glowRadius = size.maxDimension * 0.52f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            posteGold.copy(alpha = 0.16f),
                            posteNavy.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = glowCenter,
                        radius = glowRadius
                    ),
                    center = glowCenter,
                    radius = glowRadius
                )
            }
        }

        Column(modifier = Modifier.padding(dimens.cardPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(dimens.cornerRadiusSm))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(posteGold, Color(0xFFD97706))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = "Alg Post Balance",
                        tint = Color(0xFF0B1F3A),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(dimens.spacingMd))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Alg Post Balance",
                            color = colors.textPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = typo.titleSm,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(balanceAccentColor)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isArabic) {
                            "بريدي موب SMS • رادار الضرائب والأرصدة"
                        } else {
                            "BaridiMob SMS & Tax Radar • ALG Poste"
                        },
                        color = colors.textMuted,
                        fontSize = typo.caption,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.glassSurface.copy(alpha = 0.72f))
                    .border(
                        width = 0.8.dp,
                        color = if (hasBalance) balanceAccentColor.copy(alpha = 0.34f) else colors.glassBorder,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isArabic) "الرصيد الحالي في الحساب البريدي" else "Current ALG Poste Balance",
                        color = colors.textSecondary,
                        fontSize = typo.caption,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (hasBalance) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(
                                text = formatDzdAmount(
                                    amount = userSettings.latestSmsBalanceAmount,
                                    currencyText = strings.currencyCode,
                                    isStealth = isStealth
                                ),
                                color = balanceAccentColor,
                                fontSize = typo.titleLg,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Text(
                            text = if (isArabic) "--- دج • في انتظار رسالة أو اضغط للفتح" else "--- ${strings.currencyCode} • Waiting for SMS",
                            color = posteGold,
                            fontSize = typo.body,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = if (isArabic) "فتح تفاصيل بريدي موب" else "Open BaridiMob Sync",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (totalTaxes > 0.0 || pendingCount > 0 || hasBaseline) {
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (totalTaxes > 0.0) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(RoseNeon.copy(alpha = 0.14f))
                                .border(0.8.dp, RoseNeon.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Policy,
                                contentDescription = null,
                                tint = RoseNeon,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            val taxText = formatDzdAmount(totalTaxes, strings.currencyCode, isStealth = isStealth)
                            Text(
                                text = if (isArabic) {
                                    "ضرائب متصيدة: $taxText" + if (unrecordedTaxes > 0) " ($unrecordedTaxes)" else ""
                                } else {
                                    "Taxes caught: $taxText"
                                },
                                color = RoseNeon,
                                fontSize = typo.caption,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                        }
                    }

                    if (pendingCount > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF818CF8).copy(alpha = 0.18f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isArabic) "$pendingCount رسائل جديدة" else "$pendingCount new SMS",
                                color = Color(0xFF818CF8),
                                fontSize = typo.caption,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }

                    if (hasBaseline) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(CyanNeon.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.HistoryToggleOff,
                                contentDescription = null,
                                tint = CyanNeon,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isArabic) "عدّ من الصفر" else "From 0",
                                color = CyanNeon,
                                fontSize = typo.caption,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            if (hasBalance && (updatedText.isNotEmpty() || userSettings.latestSmsBalanceRawSnippet.isNotBlank())) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isStealth) {
                            if (isArabic) "نص الرسالة مخفي (الوضع الخفي)" else "SMS preview hidden (Stealth)"
                        } else {
                            userSettings.latestSmsBalanceRawSnippet.trim()
                        },
                        color = colors.textMuted,
                        fontSize = typo.caption,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (updatedText.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = updatedText,
                            color = CyanNeon,
                            fontSize = typo.caption,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
