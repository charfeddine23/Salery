package com.example.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import com.example.data.DebtEntity
import com.example.data.DebtType
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.ArabicStrings
import com.example.ui.i18n.formatDate
import com.example.ui.i18n.formatDzd
import com.example.ui.i18n.isolateLtr
import com.example.ui.theme.DebtColors
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.LocalScreenshotProtection
import java.util.Calendar

@Composable
fun AddMoreDebtDialog(
    debt: DebtEntity,
    currencyCode: String,
    onDismiss: () -> Unit,
    onConfirmAdd: (addedAmount: Double, extraNote: String, updatedDueDateMillis: Long?) -> Unit
) {
    val strings = AppStrings.current
    val isArabic = strings is ArabicStrings
    val colors = LiquidGlassTheme.colors
    val isDark = colors.isDark
    val context = LocalContext.current

    val currentTotal = debt.amount
    val currentRemaining = debt.remainingAmount.coerceAtLeast(0.0)
    val currentPaid = (currentTotal - currentRemaining).coerceAtLeast(0.0)
    val currentProgress = if (currentTotal > 0) (currentPaid / currentTotal).toFloat().coerceIn(0f, 1f) else 1f

    var addAmountText by remember { mutableStateOf("") }
    var extraNoteText by remember { mutableStateOf("") }

    val defaultDueMillis = remember(debt.id) {
        val now = System.currentTimeMillis()
        if (debt.isPaid && debt.dueDateMillis < now) {
            now + 30 * 86400000L
        } else {
            debt.dueDateMillis
        }
    }
    var dueDateMillis by remember(debt.id) { mutableLongStateOf(defaultDueMillis) }
    var didChangeDueDate by remember(debt.id) { mutableStateOf(debt.isPaid && debt.dueDateMillis < System.currentTimeMillis()) }

    val addedAmount = addAmountText.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 } ?: 0.0
    val projectedTotal = currentTotal + addedAmount
    val projectedRemaining = currentRemaining + addedAmount
    val projectedProgress = if (projectedTotal > 0) (currentPaid / projectedTotal).toFloat().coerceIn(0f, 1f) else 0f

    val accentColor = if (debt.type == DebtType.OWED_BY_ME) {
        DebtColors.payable(isDark)
    } else {
        DebtColors.receivable(isDark)
    }

    val isScreenshotProtected = LocalScreenshotProtection.current

    fun openCalendarPicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = dueDateMillis }
        val datePicker = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val chosen = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, 12)
                    set(Calendar.MINUTE, 0)
                }
                dueDateMillis = chosen.timeInMillis
                didChangeDueDate = true
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.datePicker.minDate = System.currentTimeMillis()
        datePicker.applyScreenshotProtection(isScreenshotProtected)
        datePicker.show()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            securePolicy = if (isScreenshotProtected) SecureFlagPolicy.SecureOn else SecureFlagPolicy.SecureOff
        )
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr
        ) {
            GlassCard(
                modifier = Modifier
                    .widthIn(max = 520.dp)
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(accentColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isArabic) "إضافة دين على دين قديم" else "Add to Existing Debt",
                                    color = colors.textPrimary,
                                    fontSize = 16.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = debt.personName,
                                    color = accentColor,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = strings.cancel,
                                tint = colors.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Current Debt Summary Card
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
                                    text = "${strings.paidAmount}: ${isolateLtr(formatDzd(currentPaid, currencyCode))}",
                                    color = DebtColors.settled(isDark),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${strings.remainingBalance}: ${isolateLtr(formatDzd(currentRemaining, currencyCode))}",
                                    color = accentColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LinearProgressIndicator(
                                progress = { currentProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = accentColor,
                                trackColor = colors.glassBorderSubtle
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${strings.totalAmount}: ${isolateLtr(formatDzd(currentTotal, currencyCode))}",
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

                    // New Added Amount Input
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.glassCard)
                            .border(1.dp, accentColor.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Column {
                            Text(
                                text = if (isArabic) "مبلغ الدين الجديد المضاف (${strings.currencyCode})"
                                else "New Added Amount (${strings.currencyCode})",
                                color = colors.textSecondary,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (addAmountText.isEmpty()) {
                                    Text(
                                        text = "0",
                                        color = colors.textMuted,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                BasicTextField(
                                    value = addAmountText,
                                    onValueChange = { addAmountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                                    textStyle = TextStyle(
                                        color = colors.textPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        textDirection = TextDirection.Ltr
                                    ),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("add_more_debt_amount_input"),
                                    cursorBrush = SolidColor(accentColor)
                                )
                            }
                        }
                    }

                    // Quick Increment Shortcuts
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(500L, 1000L, 2000L, 5000L, 10000L).forEach { step ->
                            GlassPill(
                                text = "+$step",
                                isSelected = false,
                                onClick = {
                                    val nextVal = (addAmountText.toDoubleOrNull() ?: 0.0) + step
                                    addAmountText = if (nextVal % 1.0 == 0.0) {
                                        nextVal.toLong().toString()
                                    } else {
                                        nextVal.toString()
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Optional Note for this addition
                    OutlinedTextField(
                        value = extraNoteText,
                        onValueChange = { extraNoteText = it },
                        label = {
                            Text(
                                if (isArabic) "ملاحظة الإضافة (اختياري)"
                                else "Addition note (optional)"
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = colors.glassBorderSubtle,
                            focusedLabelColor = accentColor,
                            unfocusedLabelColor = colors.textSecondary
                        ),
                        maxLines = 2
                    )

                    if (!debt.isRecurringMonthlyInstallment) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.glassCard)
                                .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(14.dp))
                                .clickable { openCalendarPicker() }
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = strings.debtDueDate,
                                    tint = accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = strings.debtDueDate,
                                        color = colors.textMuted,
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        text = isolateLtr(formatDate(dueDateMillis, isArabic)),
                                        color = colors.textPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = if (isArabic) "تغيير" else "Change",
                                color = accentColor,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Live Projected Totals Banner
                    if (addedAmount > 0.0) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(accentColor.copy(alpha = 0.12f))
                                .border(0.8.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isArabic) "بعد الإضافة:" else "After adding:",
                                        color = accentColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isArabic) {
                                            "المتبقي الجديد: ${isolateLtr(formatDzd(projectedRemaining, currencyCode))}"
                                        } else {
                                            "New Remaining: ${formatDzd(projectedRemaining, currencyCode)}"
                                        },
                                        color = colors.textPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    val projPct = (projectedProgress * 100).toInt()
                                    val pctStr = if (isArabic) "$projPct٪" else "$projPct%"
                                    Text(
                                        text = if (isArabic) {
                                            "الإجمالي الجديد: ${isolateLtr(formatDzd(projectedTotal, currencyCode))} (مسدد $pctStr)"
                                        } else {
                                            "New Total: ${formatDzd(projectedTotal, currencyCode)} ($pctStr paid)"
                                        },
                                        color = colors.textSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    GlassButton(
                        text = if (addedAmount > 0.0) {
                            if (isArabic) "إضافة ${isolateLtr(formatDzd(addedAmount, currencyCode))}"
                            else "Add ${formatDzd(addedAmount, currencyCode)}"
                        } else {
                            if (isArabic) "تأكيد الإضافة" else "Confirm Addition"
                        },
                        icon = Icons.Default.Add,
                        enabled = addedAmount > 0.0,
                        containerColor = accentColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("confirm_add_more_debt_button"),
                        onClick = {
                            onConfirmAdd(
                                addedAmount,
                                extraNoteText.trim(),
                                if (didChangeDueDate) dueDateMillis else null
                            )
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}
