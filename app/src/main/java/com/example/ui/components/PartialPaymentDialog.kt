package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.DebtEntity
import com.example.data.DebtType
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.ArabicStrings
import com.example.ui.i18n.formatDzd
import com.example.ui.theme.DebtColors
import com.example.ui.theme.LiquidGlassTheme

@Composable
fun PartialPaymentDialog(
    debt: DebtEntity,
    currencyCode: String,
    onDismiss: () -> Unit,
    onConfirmPayment: (amount: Double, recordTransaction: Boolean) -> Unit
) {
    val strings = AppStrings.current
    val isArabic = strings is ArabicStrings
    val colors = LiquidGlassTheme.colors
    val isDark = colors.isDark

    val totalAmount = debt.amount
    val currentRemaining = debt.remainingAmount
    val currentPaid = (totalAmount - currentRemaining).coerceAtLeast(0.0)

    var payAmountText by remember {
        val defaultSuggest = if (debt.isRecurringMonthlyInstallment && debt.monthlyInstallmentAmount > 0) {
            minOf(debt.monthlyInstallmentAmount, currentRemaining).toLong().toString()
        } else {
            (currentRemaining / 2.0).coerceAtLeast(1.0).toLong().toString()
        }
        mutableStateOf(defaultSuggest)
    }

    var recordInWallet by remember { mutableStateOf(true) }

    val enteredAmount = payAmountText.toDoubleOrNull() ?: 0.0
    val actualPay = enteredAmount.coerceIn(0.0, currentRemaining)
    val projectedRemaining = (currentRemaining - actualPay).coerceAtLeast(0.0)
    val projectedPaid = totalAmount - projectedRemaining
    val projectedProgress = if (totalAmount > 0) (projectedPaid / totalAmount).toFloat().coerceIn(0f, 1f) else 1f
    val currentProgress = if (totalAmount > 0) (currentPaid / totalAmount).toFloat().coerceIn(0f, 1f) else 1f

    val accentColor = if (debt.type == DebtType.OWED_BY_ME) DebtColors.payable(isDark) else DebtColors.receivable(isDark)

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(26.dp),
            backgroundColor = colors.dialogBackground
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = strings.makePartialPayment,
                                color = colors.textPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = debt.personName,
                                color = accentColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = strings.cancel,
                            tint = colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Current Progress Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.glassCard)
                        .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${strings.paidAmount}: ${formatDzd(currentPaid, currencyCode)}",
                                color = DebtColors.settled(isDark),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${strings.remainingBalance}: ${formatDzd(currentRemaining, currencyCode)}",
                                color = if (debt.type == DebtType.OWED_BY_ME) DebtColors.payable(isDark) else DebtColors.receivable(isDark),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { currentProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = accentColor,
                            trackColor = colors.glassBorderSubtle
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val progPct = if (isArabic) "${(currentProgress * 100).toInt()}٪" else "${(currentProgress * 100).toInt()}%"
                            Text(
                                text = "$progPct ${strings.paidOutOf} ${formatDzd(totalAmount, currencyCode)}",
                                color = colors.textMuted,
                                fontSize = 11.sp
                            )
                            Text(
                                text = if (debt.type == DebtType.OWED_BY_ME) strings.debtsOwedByMe else strings.debtsOwedToMe,
                                color = colors.textMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Payment Input Box (Compact & Sleek)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.glassCard)
                        .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column {
                        val maxLabel = if (isArabic) "الأقصى" else "Max"
                        Text(
                            text = "${strings.partialPaymentAmount} ($maxLabel: ${formatDzd(currentRemaining, currencyCode)})",
                            color = colors.textSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        BasicTextField(
                            value = payAmountText,
                            onValueChange = { payAmountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            textStyle = TextStyle(
                                color = colors.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            cursorBrush = SolidColor(accentColor)
                        )
                    }
                }

                // Quick Amount Shortcuts
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Quarter (25%)
                    val quarter = (currentRemaining * 0.25).toLong().coerceAtLeast(1)
                    GlassPill(
                        text = if (isArabic) "25٪" else "25%",
                        isSelected = actualPay.toLong() == quarter,
                        onClick = { payAmountText = quarter.toString() }
                    )

                    // Half (50%)
                    val half = (currentRemaining * 0.5).toLong().coerceAtLeast(1)
                    GlassPill(
                        text = if (isArabic) "50٪" else "50%",
                        isSelected = actualPay.toLong() == half,
                        onClick = { payAmountText = half.toString() }
                    )

                    // Installment (if recurring)
                    if (debt.isRecurringMonthlyInstallment && debt.monthlyInstallmentAmount > 0) {
                        val inst = minOf(debt.monthlyInstallmentAmount, currentRemaining).toLong()
                        GlassPill(
                            text = if (isArabic) "شهر ($inst)" else "1 Mo ($inst)",
                            isSelected = actualPay.toLong() == inst,
                            onClick = { payAmountText = inst.toString() }
                        )
                    }

                    // Full Payoff (100%)
                    val full = currentRemaining.toLong()
                    GlassPill(
                        text = if (isArabic) "الكل (100٪)" else "Full (100%)",
                        isSelected = actualPay.toLong() == full,
                        onClick = { payAmountText = full.toString() }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Real-Time Projected Outcome Banner
                if (actualPay > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentColor.copy(alpha = 0.12f))
                            .border(0.5.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (projectedRemaining <= 0.001) strings.fullySettled else if (isArabic) "بعد السداد:" else "After payment:",
                                    color = accentColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (projectedRemaining <= 0.001) {
                                        if (isArabic) "سيتم سداد الدين بالكامل!" else "Debt will be fully settled!"
                                    } else {
                                        val projPct = if (isArabic) "${(projectedProgress * 100).toInt()}٪" else "${(projectedProgress * 100).toInt()}%"
                                        if (isArabic) {
                                            "المتبقي: ${formatDzd(projectedRemaining, currencyCode)} (مسدد $projPct)"
                                        } else {
                                            "Remaining: ${formatDzd(projectedRemaining, currencyCode)} ($projPct paid)"
                                        }
                                    },
                                    color = colors.textPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Switch: Record in Wallet
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.glassCard)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.recordInWallet,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = if (debt.type == DebtType.OWED_BY_ME) "Deduct expense from wallet balance"
                            else "Credit income to wallet balance",
                            color = colors.textSecondary,
                            fontSize = 10.sp
                        )
                    }
                    Switch(
                        checked = recordInWallet,
                        onCheckedChange = { recordInWallet = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = accentColor,
                            uncheckedThumbColor = colors.textMuted,
                            uncheckedTrackColor = colors.glassCard
                        )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Confirm Button
                val isValid = actualPay > 0.0 && actualPay <= currentRemaining
                GlassButton(
                    text = if (actualPay >= currentRemaining) strings.markAsPaid else "Record ${formatDzd(actualPay, currencyCode)}",
                    icon = Icons.Default.Payments,
                    enabled = isValid,
                    containerColor = accentColor,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onConfirmPayment(actualPay, recordInWallet)
                        onDismiss()
                    }
                )
            }
        }
    }
}
