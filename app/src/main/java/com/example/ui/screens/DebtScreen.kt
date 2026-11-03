package com.example.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.DebtEntity
import com.example.data.DebtType
import com.example.ui.components.AddDebtDialog
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPill
import com.example.ui.components.PartialPaymentDialog
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.formatDate
import com.example.ui.i18n.formatDzd
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.RoseNeon

@Composable
fun DebtScreen(
    debts: List<DebtEntity>,
    onAddDebtClick: (DebtType) -> Unit,
    onEditDebt: (DebtEntity) -> Unit,
    onPartialPay: (DebtEntity, Double, Boolean) -> Unit,
    onPayDebtNow: (DebtEntity) -> Unit,
    onDeleteDebt: (DebtEntity) -> Unit,
    onClearSettledDebts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors

    var activeTab by remember { mutableStateOf(DebtType.OWED_BY_ME) }
    var statusFilter by remember { mutableStateOf("ALL") } // "ALL", "ACTIVE", "SETTLED"

    // Dialog States
    var partialPaymentDebt by remember { mutableStateOf<DebtEntity?>(null) }
    var editingDebt by remember { mutableStateOf<DebtEntity?>(null) }
    var debtToDelete by remember { mutableStateOf<DebtEntity?>(null) }
    var showClearSettledConfirm by remember { mutableStateOf(false) }

    val debtsInTab = debts.filter { it.type == activeTab }
    val pendingDebts = debtsInTab.filter { !it.isPaid }
    val settledDebts = debtsInTab.filter { it.isPaid }

    val totalPendingAmount = pendingDebts.sumOf { if (it.remainingAmount > 0) it.remainingAmount else it.amount }
    val totalPaidAmount = debtsInTab.sumOf { (it.amount - it.remainingAmount).coerceAtLeast(0.0) }
    val totalOriginalAmount = debtsInTab.sumOf { it.amount }
    val tabProgress = if (totalOriginalAmount > 0) (totalPaidAmount / totalOriginalAmount).toFloat().coerceIn(0f, 1f) else 1f

    val displayedDebts = when (statusFilter) {
        "ACTIVE" -> pendingDebts
        "SETTLED" -> settledDebts
        else -> debtsInTab
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Comprehensive Debt Management Dashboard Card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = colors.glassCard,
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (activeTab == DebtType.OWED_BY_ME) strings.debtsOwedByMe else strings.debtsOwedToMe,
                            color = colors.textSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    (if (activeTab == DebtType.OWED_BY_ME) AmberNeon else CyanNeon).copy(alpha = 0.18f)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${pendingDebts.size} ${strings.filterActive}",
                                color = if (activeTab == DebtType.OWED_BY_ME) AmberNeon else CyanNeon,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = formatDzd(totalPendingAmount, strings.currencyCode),
                        color = if (activeTab == DebtType.OWED_BY_ME) AmberNeon else CyanNeon,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Overall Tab Progress Bar
                    LinearProgressIndicator(
                        progress = { tabProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (activeTab == DebtType.OWED_BY_ME) AmberNeon else EmeraldNeon,
                        trackColor = colors.glassBorderSubtle
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${strings.paidAmount}: ${formatDzd(totalPaidAmount, strings.currencyCode)} (${(tabProgress * 100).toInt()}%)",
                            color = EmeraldNeon,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${strings.totalAmount}: ${formatDzd(totalOriginalAmount, strings.currencyCode)}",
                            color = colors.textMuted,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Controls: Add Debt & Clear Settled Debts
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GlassButton(
                            text = strings.addDebt,
                            icon = Icons.Default.Add,
                            containerColor = if (activeTab == DebtType.OWED_BY_ME) AmberNeon else colors.primaryNeon,
                            modifier = Modifier.weight(1f),
                            onClick = { onAddDebtClick(activeTab) }
                        )

                        if (settledDebts.isNotEmpty()) {
                            GlassButton(
                                text = "${strings.deleteSettledDebts} (${settledDebts.size})",
                                icon = Icons.Default.DeleteSweep,
                                containerColor = RoseNeon.copy(alpha = 0.85f),
                                modifier = Modifier.weight(1f),
                                onClick = { showClearSettledConfirm = true }
                            )
                        }
                    }
                }
            }
        }

        // Tab Selector (I Owe vs Owed to Me)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.glassCard)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (activeTab == DebtType.OWED_BY_ME) AmberNeon.copy(alpha = 0.25f)
                            else Color.Transparent
                        )
                        .clickable { activeTab = DebtType.OWED_BY_ME }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${strings.debtsOwedByMe} (${debts.count { it.type == DebtType.OWED_BY_ME }})",
                        color = if (activeTab == DebtType.OWED_BY_ME) AmberNeon else colors.textMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (activeTab == DebtType.OWED_TO_ME) CyanNeon.copy(alpha = 0.25f)
                            else Color.Transparent
                        )
                        .clickable { activeTab = DebtType.OWED_TO_ME }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${strings.debtsOwedToMe} (${debts.count { it.type == DebtType.OWED_TO_ME }})",
                        color = if (activeTab == DebtType.OWED_TO_ME) CyanNeon else colors.textMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Status Filter Chips (All, Active, Settled)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassPill(
                    text = "${strings.filterAll} (${debtsInTab.size})",
                    isSelected = statusFilter == "ALL",
                    onClick = { statusFilter = "ALL" }
                )
                GlassPill(
                    text = "${strings.filterActive} (${pendingDebts.size})",
                    isSelected = statusFilter == "ACTIVE",
                    onClick = { statusFilter = "ACTIVE" }
                )
                GlassPill(
                    text = "${strings.filterSettled} (${settledDebts.size})",
                    isSelected = statusFilter == "SETTLED",
                    onClick = { statusFilter = "SETTLED" }
                )
            }
        }

        // Debts List
        if (displayedDebts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = strings.emptyRecords,
                        color = colors.textMuted,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            items(displayedDebts, key = { it.id }) { debt ->
                DebtItemCard(
                    debt = debt,
                    currencyCode = strings.currencyCode,
                    onPartialPayClick = { partialPaymentDebt = debt },
                    onPayNow = { onPayDebtNow(debt) },
                    onEdit = { editingDebt = debt },
                    onDelete = { debtToDelete = debt }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
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

    // Dialog 2: Edit Debt
    editingDebt?.let { targetDebt ->
        AddDebtDialog(
            initialType = targetDebt.type,
            existingDebt = targetDebt,
            onDismiss = { editingDebt = null },
            onSave = { type, personName, amount, note, dueDateMillis, autoPay, isRecurring, monthlyInstallment, installmentDay ->
                val ratio = if (targetDebt.amount > 0) (targetDebt.remainingAmount / targetDebt.amount) else 1.0
                val updatedRemaining = (amount * ratio).coerceIn(0.0, amount)
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
    debtToDelete?.let { targetDebt ->
        Dialog(onDismissRequest = { debtToDelete = null }) {
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
                                .background(RoseNeon.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = RoseNeon,
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
                        text = "${targetDebt.personName} • ${formatDzd(targetDebt.amount, strings.currencyCode)}",
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
                            containerColor = RoseNeon,
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

    // Dialog 4: Clear All Settled Debts Confirmation
    if (showClearSettledConfirm) {
        Dialog(onDismissRequest = { showClearSettledConfirm = false }) {
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
                                .background(RoseNeon.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = null,
                                tint = RoseNeon,
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
                            containerColor = RoseNeon,
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

@Composable
fun DebtItemCard(
    debt: DebtEntity,
    currencyCode: String,
    onPartialPayClick: () -> Unit,
    onPayNow: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LiquidGlassTheme.colors
    val strings = AppStrings.current

    val now = System.currentTimeMillis()
    val isOverdue = !debt.isPaid && debt.dueDateMillis < now
    val daysLeft = ((debt.dueDateMillis - now) / 86400000L).coerceAtLeast(0)

    val totalAmount = debt.amount
    val remainingAmount = debt.remainingAmount
    val paidAmount = (totalAmount - remainingAmount).coerceAtLeast(0.0)
    val paidRatio = if (totalAmount > 0) (paidAmount / totalAmount).toFloat().coerceIn(0f, 1f) else 1f
    val paidPercent = (paidRatio * 100).toInt()

    val statusColor = when {
        debt.isPaid -> EmeraldNeon
        isOverdue -> RoseNeon
        else -> AmberNeon
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = colors.glassCard,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Icon, Person Name, and Total Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (debt.isPaid) Icons.Default.CheckCircle else Icons.Default.CreditCard,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = debt.personName,
                            color = colors.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (debt.note.isNotBlank()) {
                            Text(
                                text = debt.note,
                                color = colors.textSecondary,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Total Amount and Remaining Balance
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatDzd(debt.amount, currencyCode),
                        color = colors.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = if (debt.isPaid) strings.fullySettled
                        else "${strings.remainingBalance}: ${formatDzd(remainingAmount, currencyCode)}",
                        color = if (debt.isPaid) EmeraldNeon else AmberNeon,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Visual Progress Tracking: Paid Amount vs. Total Amount & Percentage
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { paidRatio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (debt.isPaid) EmeraldNeon else AmberNeon,
                trackColor = colors.glassBorderSubtle
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${formatDzd(paidAmount, currencyCode)} ${strings.paidOutOf} ${formatDzd(totalAmount, currencyCode)}",
                    color = EmeraldNeon,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "$paidPercent%",
                    color = if (debt.isPaid) EmeraldNeon else AmberNeon,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Badges & Due Date / Installment Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Paid / Pending / Overdue Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(statusColor.copy(alpha = 0.18f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (debt.isPaid) strings.debtPaid else if (isOverdue) "Overdue" else strings.debtPending,
                            color = statusColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Auto-pay or Installment badge
                    if (debt.isRecurringMonthlyInstallment) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(AmberNeon.copy(alpha = 0.18f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Autorenew,
                                    contentDescription = null,
                                    tint = AmberNeon,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "${debt.monthlyInstallmentAmount.toLong()} $currencyCode / mo • Day ${debt.installmentDayOfMonth}",
                                    color = AmberNeon,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else if (debt.autoPayOnDueDate) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyanNeon.copy(alpha = 0.18f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Autorenew,
                                    contentDescription = null,
                                    tint = CyanNeon,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Auto-Pay",
                                    color = CyanNeon,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Due Date Text
                Text(
                    text = if (debt.isPaid) "Settled on ${formatDate(debt.paidDateMillis ?: debt.dueDateMillis)}"
                    else if (debt.isRecurringMonthlyInstallment) "Monthly on Day ${debt.installmentDayOfMonth}"
                    else "Due: ${formatDate(debt.dueDateMillis)} ($daysLeft d)",
                    color = colors.textMuted,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Controls Row (Partial Pay, Settle Full, Edit, Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!debt.isPaid) {
                    // Partial Payment Button
                    GlassButton(
                        text = strings.makePartialPayment,
                        icon = Icons.Default.Payments,
                        containerColor = AmberNeon,
                        modifier = Modifier.weight(1.3f),
                        onClick = onPartialPayClick
                    )

                    // Full Payoff Button
                    GlassButton(
                        text = strings.executePayment,
                        icon = Icons.Default.Payment,
                        containerColor = EmeraldNeon,
                        modifier = Modifier.weight(1.1f),
                        onClick = onPayNow
                    )
                } else {
                    // Settled Archive / Delete Action
                    GlassButton(
                        text = strings.delete,
                        icon = Icons.Default.Delete,
                        containerColor = RoseNeon.copy(alpha = 0.8f),
                        modifier = Modifier.weight(1f),
                        onClick = onDelete
                    )
                }

                // Edit Button
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.glassCard)
                        .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = strings.editDebt,
                        tint = colors.primaryNeon,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Delete Button (if not already settled, settled has button above)
                if (!debt.isPaid) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.glassCard)
                            .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = strings.delete,
                            tint = colors.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
