package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.data.DebtEntity
import com.example.data.DebtType
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
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
    onPayDebtNow: (DebtEntity) -> Unit,
    onDeleteDebt: (DebtEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors

    var activeTab by remember { mutableStateOf(DebtType.OWED_BY_ME) }

    val currentDebts = debts.filter { it.type == activeTab }
    val pendingDebts = currentDebts.filter { !it.isPaid }
    val totalPendingAmount = pendingDebts.sumOf { it.amount }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Summary Card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = colors.glassCard,
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = if (activeTab == DebtType.OWED_BY_ME) strings.debtsOwedByMe else strings.debtsOwedToMe,
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = formatDzd(totalPendingAmount, strings.currencyCode),
                        color = if (activeTab == DebtType.OWED_BY_ME) AmberNeon else CyanNeon,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    GlassButton(
                        text = strings.addDebt,
                        icon = Icons.Default.Add,
                        containerColor = if (activeTab == DebtType.OWED_BY_ME) AmberNeon else colors.primaryNeon,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onAddDebtClick(activeTab) }
                    )
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
                        text = strings.debtsOwedByMe,
                        color = if (activeTab == DebtType.OWED_BY_ME) AmberNeon else colors.textMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
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
                        text = strings.debtsOwedToMe,
                        color = if (activeTab == DebtType.OWED_TO_ME) CyanNeon else colors.textMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        if (currentDebts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
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
            items(currentDebts, key = { it.id }) { debt ->
                DebtItemCard(
                    debt = debt,
                    currencyCode = strings.currencyCode,
                    onPayNow = { onPayDebtNow(debt) },
                    onDelete = { onDeleteDebt(debt) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun DebtItemCard(
    debt: DebtEntity,
    currencyCode: String,
    onPayNow: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LiquidGlassTheme.colors
    val strings = AppStrings.current

    val now = System.currentTimeMillis()
    val isOverdue = !debt.isPaid && debt.dueDateMillis < now
    val daysLeft = ((debt.dueDateMillis - now) / 86400000L).coerceAtLeast(0)

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                    Column {
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
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Text(
                    text = formatDzd(debt.amount, currencyCode),
                    color = colors.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Badges & Due Date Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Paid / Pending Badge
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

                    // Auto-pay badge
                    if (debt.autoPayOnDueDate) {
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
                    else "Due: ${formatDate(debt.dueDateMillis)} ($daysLeft d)",
                    color = colors.textMuted,
                    fontSize = 11.sp
                )
            }

            // Actions row (Pay Now if pending)
            if (!debt.isPaid) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GlassButton(
                        text = if (debt.type == DebtType.OWED_BY_ME) strings.executePayment else strings.markAsPaid,
                        icon = Icons.Default.Payment,
                        containerColor = EmeraldNeon,
                        modifier = Modifier.weight(1f),
                        onClick = onPayNow
                    )
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = colors.textMuted
                        )
                    }
                }
            }
        }
    }
}
