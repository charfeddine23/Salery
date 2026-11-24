package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import com.example.data.DebtEntity
import com.example.data.DebtType
import com.example.ui.components.AddDebtDialog
import com.example.ui.components.AddMoreDebtDialog
import com.example.ui.components.DebtBreathingEmptyState
import com.example.ui.components.DebtBreathingHeroCard
import com.example.ui.components.DebtControlsBar
import com.example.ui.components.DebtSortMode
import com.example.ui.components.DebtStatusFilter
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.ModernDebtCard
import com.example.ui.components.PartialPaymentDialog
import com.example.ui.components.edgeFadeVerticalContentPadding
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.ArabicStrings
import com.example.ui.i18n.formatDzd
import com.example.ui.i18n.isolateLtr
import com.example.ui.theme.DebtColors
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.LocalAdaptiveDimens
import com.example.ui.theme.LocalAdaptiveLayoutInfo
import com.example.ui.theme.LocalAdaptiveTypography
import com.example.ui.theme.LocalScreenshotProtection

@Composable
fun DebtScreen(
    debts: List<DebtEntity>,
    onAddDebtClick: (DebtType) -> Unit,
    onEditDebt: (DebtEntity) -> Unit,
    onPartialPay: (DebtEntity, Double, Boolean) -> Unit,
    onAddToExistingDebt: (DebtEntity, Double, String, Long?) -> Unit = { _, _, _, _ -> },
    onPayDebtNow: (DebtEntity) -> Unit,
    onDeleteDebt: (DebtEntity) -> Unit,
    onClearSettledDebts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors
    val layoutInfo = LocalAdaptiveLayoutInfo.current
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current
    val isDark = colors.isDark
    val isArabic = strings is ArabicStrings

    var activeTab by remember { mutableStateOf(DebtType.OWED_BY_ME) }
    var statusFilter by remember { mutableStateOf(DebtStatusFilter.ALL) }
    var sortMode by remember { mutableStateOf(DebtSortMode.SOONEST) }
    var searchQuery by remember { mutableStateOf("") }
    var expandedDebtIds by remember { mutableStateOf(setOf<Long>()) }

    // Dialog States
    var partialPaymentDebt by remember { mutableStateOf<DebtEntity?>(null) }
    var addMoreDebtTarget by remember { mutableStateOf<DebtEntity?>(null) }
    var editingDebt by remember { mutableStateOf<DebtEntity?>(null) }
    var debtToDelete by remember { mutableStateOf<DebtEntity?>(null) }
    var showClearSettledConfirm by remember { mutableStateOf(false) }

    val now = System.currentTimeMillis()

    // Global totals for both ledgers so user always sees the full picture
    val payableActive = remember(debts) { debts.filter { it.type == DebtType.OWED_BY_ME && !it.isPaid } }
    val receivableActive = remember(debts) { debts.filter { it.type == DebtType.OWED_TO_ME && !it.isPaid } }
    val payableRemaining = remember(payableActive) {
        payableActive.sumOf { if (it.remainingAmount > 0) it.remainingAmount else it.amount }
    }
    val receivableRemaining = remember(receivableActive) {
        receivableActive.sumOf { if (it.remainingAmount > 0) it.remainingAmount else it.amount }
    }

    val debtsInTab = remember(debts, activeTab) { debts.filter { it.type == activeTab } }
    val activeInTab = remember(debtsInTab) { debtsInTab.filter { !it.isPaid } }
    val overdueInTab = remember(activeInTab, now) {
        activeInTab.filter { !it.isRecurringMonthlyInstallment && it.dueDateMillis < now }
    }
    val settledInTab = remember(debtsInTab) { debtsInTab.filter { it.isPaid } }

    val totalPaidInTab = remember(debtsInTab) {
        debtsInTab.sumOf { (it.amount - it.remainingAmount).coerceAtLeast(0.0) }
    }
    val totalOriginalInTab = remember(debtsInTab) { debtsInTab.sumOf { it.amount } }
    val tabProgress = if (totalOriginalInTab > 0) {
        (totalPaidInTab / totalOriginalInTab).toFloat().coerceIn(0f, 1f)
    } else 1f

    // Reset filter to ALL if OVERDUE filter selected and there are no longer any overdue items
    val effectiveFilter = if (statusFilter == DebtStatusFilter.OVERDUE && overdueInTab.isEmpty()) {
        DebtStatusFilter.ALL
    } else statusFilter

    val displayedDebts = remember(debtsInTab, effectiveFilter, searchQuery, sortMode, now) {
        val statusFiltered = when (effectiveFilter) {
            DebtStatusFilter.ALL -> debtsInTab
            DebtStatusFilter.ACTIVE -> debtsInTab.filter { !it.isPaid }
            DebtStatusFilter.OVERDUE -> debtsInTab.filter {
                !it.isPaid && !it.isRecurringMonthlyInstallment && it.dueDateMillis < now
            }
            DebtStatusFilter.SETTLED -> debtsInTab.filter { it.isPaid }
        }
        val query = searchQuery.trim().lowercase()
        val searched = if (query.isEmpty()) {
            statusFiltered
        } else {
            statusFiltered.filter {
                it.personName.lowercase().contains(query) || it.note.lowercase().contains(query)
            }
        }
        when (sortMode) {
            DebtSortMode.SOONEST -> searched.sortedWith(
                compareBy<DebtEntity> { it.isPaid }.thenBy { it.dueDateMillis }
            )
            DebtSortMode.HIGHEST -> searched.sortedWith(
                compareBy<DebtEntity> { it.isPaid }.thenByDescending { it.remainingAmount }
            )
            DebtSortMode.RECENT -> searched.sortedWith(
                compareBy<DebtEntity> { it.isPaid }.thenByDescending { it.createdDateMillis }
            )
        }
    }

    val activeAccent = if (activeTab == DebtType.OWED_BY_ME) {
        DebtColors.payable(isDark)
    } else {
        DebtColors.receivable(isDark)
    }

    val onCycleSort = {
        sortMode = when (sortMode) {
            DebtSortMode.SOONEST -> DebtSortMode.HIGHEST
            DebtSortMode.HIGHEST -> DebtSortMode.RECENT
            DebtSortMode.RECENT -> DebtSortMode.SOONEST
        }
    }

    val toggleCardExpand: (Long) -> Unit = { id ->
        expandedDebtIds = if (id in expandedDebtIds) expandedDebtIds - id else expandedDebtIds + id
    }

    val hasActiveFilterOrSearch = searchQuery.isNotBlank() || effectiveFilter != DebtStatusFilter.ALL

    if (layoutInfo.columns >= 2) {
        // Adaptive 2-column layout for Tablets & Landscape
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = dimens.screenPaddingH),
            horizontalArrangement = Arrangement.spacedBy(dimens.spacingLg)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(0.92f)
                    .fillMaxHeight(),
                contentPadding = edgeFadeVerticalContentPadding(),
                verticalArrangement = Arrangement.spacedBy(dimens.spacingMd)
            ) {
                item { Spacer(modifier = Modifier.height(dimens.spacingXs)) }

                item {
                    DebtBreathingHeroCard(
                        activeTab = activeTab,
                        onTabSelect = { activeTab = it },
                        payableRemaining = payableRemaining,
                        payableCount = payableActive.size,
                        receivableRemaining = receivableRemaining,
                        receivableCount = receivableActive.size,
                        totalPaidInTab = totalPaidInTab,
                        totalOriginalInTab = totalOriginalInTab,
                        tabProgress = tabProgress,
                        overdueCountInTab = overdueInTab.size,
                        settledCountInTab = settledInTab.size,
                        strings = strings,
                        colors = colors,
                        dimens = dimens,
                        typo = typo,
                        onAddDebtClick = { onAddDebtClick(activeTab) },
                        onClearSettledClick = { showClearSettledConfirm = true }
                    )
                }

                item { Spacer(modifier = Modifier.height(dimens.spacingXl)) }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1.08f)
                    .fillMaxHeight(),
                contentPadding = edgeFadeVerticalContentPadding(),
                verticalArrangement = Arrangement.spacedBy(dimens.spacingMd)
            ) {
                item { Spacer(modifier = Modifier.height(dimens.spacingXs)) }

                item {
                    DebtControlsBar(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        statusFilter = effectiveFilter,
                        onStatusFilterChange = { statusFilter = it },
                        sortMode = sortMode,
                        onCycleSortMode = onCycleSort,
                        totalCount = debtsInTab.size,
                        activeCount = activeInTab.size,
                        overdueCount = overdueInTab.size,
                        settledCount = settledInTab.size,
                        strings = strings,
                        colors = colors
                    )
                }

                if (displayedDebts.isEmpty()) {
                    item {
                        DebtBreathingEmptyState(
                            title = if (hasActiveFilterOrSearch) {
                                if (isArabic) "لا توجد نتائج مطابقة" else "No Matching Debts"
                            } else strings.emptyDebtsTitle,
                            description = if (hasActiveFilterOrSearch) {
                                if (isArabic) "جرّب مسح البحث أو اختيار فلتر آخر لعرض الديون."
                                else "Try clearing your search or switching filters."
                            } else strings.emptyDebtsDesc,
                            accentColor = activeAccent,
                            actionText = if (hasActiveFilterOrSearch) {
                                if (isArabic) "إظهار الكل" else "Show All"
                            } else strings.addFirstDebt,
                            onActionClick = {
                                if (hasActiveFilterOrSearch) {
                                    searchQuery = ""
                                    statusFilter = DebtStatusFilter.ALL
                                } else {
                                    onAddDebtClick(activeTab)
                                }
                            }
                        )
                    }
                } else {
                    items(displayedDebts, key = { it.id }) { debt ->
                        ModernDebtCard(
                            debt = debt,
                            currencyCode = strings.currencyCode,
                            isExpanded = debt.id in expandedDebtIds,
                            onToggleExpand = { toggleCardExpand(debt.id) },
                            onPartialPayClick = { partialPaymentDebt = debt },
                            onAddMoreClick = { addMoreDebtTarget = debt },
                            onPayNow = { onPayDebtNow(debt) },
                            onEdit = { editingDebt = debt },
                            onDelete = { debtToDelete = debt }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(dimens.spacingXl)) }
            }
        }
    } else {
        // Single-column smooth vertical flow for Phones
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = dimens.screenPaddingH),
            contentPadding = edgeFadeVerticalContentPadding(),
            verticalArrangement = Arrangement.spacedBy(dimens.spacingMd)
        ) {
            item { Spacer(modifier = Modifier.height(dimens.spacingXs)) }

            item {
                DebtBreathingHeroCard(
                    activeTab = activeTab,
                    onTabSelect = { activeTab = it },
                    payableRemaining = payableRemaining,
                    payableCount = payableActive.size,
                    receivableRemaining = receivableRemaining,
                    receivableCount = receivableActive.size,
                    totalPaidInTab = totalPaidInTab,
                    totalOriginalInTab = totalOriginalInTab,
                    tabProgress = tabProgress,
                    overdueCountInTab = overdueInTab.size,
                    settledCountInTab = settledInTab.size,
                    strings = strings,
                    colors = colors,
                    dimens = dimens,
                    typo = typo,
                    onAddDebtClick = { onAddDebtClick(activeTab) },
                    onClearSettledClick = { showClearSettledConfirm = true }
                )
            }

            item {
                DebtControlsBar(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    statusFilter = effectiveFilter,
                    onStatusFilterChange = { statusFilter = it },
                    sortMode = sortMode,
                    onCycleSortMode = onCycleSort,
                    totalCount = debtsInTab.size,
                    activeCount = activeInTab.size,
                    overdueCount = overdueInTab.size,
                    settledCount = settledInTab.size,
                    strings = strings,
                    colors = colors
                )
            }

            if (displayedDebts.isEmpty()) {
                item {
                    DebtBreathingEmptyState(
                        title = if (hasActiveFilterOrSearch) {
                            if (isArabic) "لا توجد نتائج مطابقة" else "No Matching Debts"
                        } else strings.emptyDebtsTitle,
                        description = if (hasActiveFilterOrSearch) {
                            if (isArabic) "جرّب مسح البحث أو اختيار فلتر آخر لعرض الديون."
                            else "Try clearing your search or switching filters."
                        } else strings.emptyDebtsDesc,
                        accentColor = activeAccent,
                        actionText = if (hasActiveFilterOrSearch) {
                            if (isArabic) "إظهار الكل" else "Show All"
                        } else strings.addFirstDebt,
                        onActionClick = {
                            if (hasActiveFilterOrSearch) {
                                searchQuery = ""
                                statusFilter = DebtStatusFilter.ALL
                            } else {
                                onAddDebtClick(activeTab)
                            }
                        }
                    )
                }
            } else {
                items(displayedDebts, key = { it.id }) { debt ->
                    ModernDebtCard(
                        debt = debt,
                        currencyCode = strings.currencyCode,
                        isExpanded = debt.id in expandedDebtIds,
                        onToggleExpand = { toggleCardExpand(debt.id) },
                        onPartialPayClick = { partialPaymentDebt = debt },
                        onAddMoreClick = { addMoreDebtTarget = debt },
                        onPayNow = { onPayDebtNow(debt) },
                        onEdit = { editingDebt = debt },
                        onDelete = { debtToDelete = debt }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(dimens.spacingXl)) }
        }
    }

    // Dialog 1: Partial Payment
    partialPaymentDebt?.let { targetDebt ->
        PartialPaymentDialog(
            debt = targetDebt,
            currencyCode = strings.currencyCode,
            onDismiss = { partialPaymentDebt = null },
            onConfirmPayment = { amount, recordTransaction ->
                onPartialPay(targetDebt, amount, recordTransaction)
                partialPaymentDebt = null
            }
        )
    }

    // Dialog 1B: Add More Debt onto Existing Debt
    addMoreDebtTarget?.let { targetDebt ->
        AddMoreDebtDialog(
            debt = targetDebt,
            currencyCode = strings.currencyCode,
            onDismiss = { addMoreDebtTarget = null },
            onConfirmAdd = { addedAmount, extraNote, updatedDueDateMillis ->
                onAddToExistingDebt(targetDebt, addedAmount, extraNote, updatedDueDateMillis)
                addMoreDebtTarget = null
            }
        )
    }

    // Dialog 2: Edit Debt
    editingDebt?.let { targetDebt ->
        AddDebtDialog(
            initialType = targetDebt.type,
            existingDebt = targetDebt,
            onDismiss = { editingDebt = null },
            onSave = { type, personName, amount, note, dueDateMillis, autoPay, isRecurring, monthlyInstallment, installmentDay, paidAmount ->
                val updatedRemaining = (amount - paidAmount).coerceIn(0.0, amount)
                val isSettled = updatedRemaining <= 0.001

                val updatedEntity = targetDebt.copy(
                    type = type,
                    personName = personName,
                    amount = amount,
                    remainingAmount = updatedRemaining,
                    note = note,
                    dueDateMillis = dueDateMillis,
                    autoPayOnDueDate = autoPay,
                    isRecurringMonthlyInstallment = isRecurring,
                    monthlyInstallmentAmount = monthlyInstallment,
                    installmentDayOfMonth = installmentDay,
                    isPaid = isSettled
                )
                onEditDebt(updatedEntity)
                editingDebt = null
            }
        )
    }

    // Dialog 3: Delete Single Debt Confirmation
    val isScreenshotProtected = LocalScreenshotProtection.current
    debtToDelete?.let { targetDebt ->
        Dialog(
            onDismissRequest = { debtToDelete = null },
            properties = DialogProperties(
                securePolicy = if (isScreenshotProtected) SecureFlagPolicy.SecureOn else SecureFlagPolicy.SecureOff
            )
        ) {
            CompositionLocalProvider(
                LocalLayoutDirection provides if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr
            ) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    shape = RoundedCornerShape(22.dp),
                    backgroundColor = colors.dialogBackground
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(DebtColors.dangerContainer(isDark))
                                    .border(1.dp, DebtColors.dangerBorder(isDark), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = DebtColors.danger(isDark),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = strings.confirmDelete,
                                color = colors.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "${targetDebt.personName} • ${isolateLtr(formatDzd(targetDebt.amount, strings.currencyCode))}",
                            color = colors.textSecondary,
                            fontSize = 13.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GlassButton(
                                text = strings.cancel,
                                containerColor = colors.glassCard,
                                modifier = Modifier.weight(1f),
                                onClick = { debtToDelete = null }
                            )
                            GlassButton(
                                text = strings.delete,
                                icon = Icons.Default.Delete,
                                containerColor = DebtColors.danger(isDark),
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    onDeleteDebt(targetDebt)
                                    debtToDelete = null
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog 4: Clear All Settled Debts Confirmation
    if (showClearSettledConfirm) {
        Dialog(
            onDismissRequest = { showClearSettledConfirm = false },
            properties = DialogProperties(
                securePolicy = if (isScreenshotProtected) SecureFlagPolicy.SecureOn else SecureFlagPolicy.SecureOff
            )
        ) {
            CompositionLocalProvider(
                LocalLayoutDirection provides if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr
            ) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    shape = RoundedCornerShape(22.dp),
                    backgroundColor = colors.dialogBackground
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(DebtColors.dangerContainer(isDark))
                                    .border(1.dp, DebtColors.dangerBorder(isDark), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = null,
                                    tint = DebtColors.danger(isDark),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = strings.deleteSettledDebts,
                                color = colors.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = strings.clearSettledConfirm,
                            color = colors.textSecondary,
                            fontSize = 13.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GlassButton(
                                text = strings.cancel,
                                containerColor = colors.glassCard,
                                modifier = Modifier.weight(1f),
                                onClick = { showClearSettledConfirm = false }
                            )
                            GlassButton(
                                text = strings.delete,
                                icon = Icons.Default.Delete,
                                containerColor = DebtColors.danger(isDark),
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    onClearSettledDebts()
                                    showClearSettledConfirm = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
