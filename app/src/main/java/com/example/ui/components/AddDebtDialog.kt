package com.example.ui.components

import android.app.DatePickerDialog
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.DebtEntity
import com.example.data.DebtType
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.formatDate
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.RoseNeon
import java.util.Calendar

@Composable
fun AddDebtDialog(
    initialType: DebtType = DebtType.OWED_BY_ME,
    existingDebt: DebtEntity? = null,
    onDismiss: () -> Unit,
    onSave: (
        type: DebtType,
        personName: String,
        amount: Double,
        note: String,
        dueDateMillis: Long,
        autoPay: Boolean,
        isRecurring: Boolean,
        monthlyInstallment: Double,
        installmentDay: Int
    ) -> Unit
) {
    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors
    val context = LocalContext.current

    var selectedType by remember { mutableStateOf(existingDebt?.type ?: initialType) }
    var personNameText by remember { mutableStateOf(existingDebt?.personName ?: "") }
    var amountText by remember {
        val initialAmt = existingDebt?.amount?.let { a ->
            if (a % 1.0 == 0.0) a.toLong().toString() else a.toString()
        } ?: ""
        mutableStateOf(initialAmt)
    }
    var noteText by remember { mutableStateOf(existingDebt?.note ?: "") }
    var dueDateMillis by remember { mutableLongStateOf(existingDebt?.dueDateMillis ?: (System.currentTimeMillis() + 30 * 86400000L)) }
    var autoPayOnDueDate by remember { mutableStateOf(existingDebt?.autoPayOnDueDate ?: true) }

    // Recurring Monthly Installments
    var isRecurringInstallment by remember { mutableStateOf(existingDebt?.isRecurringMonthlyInstallment ?: false) }
    var installmentAmountText by remember {
        val initialInst = existingDebt?.let { d ->
            if (d.monthlyInstallmentAmount > 0) {
                if (d.monthlyInstallmentAmount % 1.0 == 0.0) d.monthlyInstallmentAmount.toLong().toString()
                else d.monthlyInstallmentAmount.toString()
            } else ""
        } ?: ""
        mutableStateOf(initialInst)
    }
    var installmentDay by remember { mutableIntStateOf(existingDebt?.installmentDayOfMonth ?: 15) }

    fun openInstallmentDayPicker() {
        val now = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            context,
            { _, _, _, dayOfMonth ->
                installmentDay = dayOfMonth
            },
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH),
            installmentDay.coerceIn(1, 28)
        )
        datePicker.show()
    }

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
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.datePicker.minDate = System.currentTimeMillis()
        datePicker.show()
    }

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
                    Text(
                        text = if (existingDebt != null) strings.editDebt else strings.addDebt,
                        color = colors.textPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
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

                Spacer(modifier = Modifier.height(16.dp))

                // Type Toggle (I Owe / Owed to Me)
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
                                if (selectedType == DebtType.OWED_BY_ME) RoseNeon.copy(alpha = 0.25f)
                                else Color.Transparent
                            )
                            .clickable { selectedType = DebtType.OWED_BY_ME }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.debtsOwedByMe,
                            color = if (selectedType == DebtType.OWED_BY_ME) RoseNeon else colors.textMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selectedType == DebtType.OWED_TO_ME) CyanNeon.copy(alpha = 0.25f)
                                else Color.Transparent
                            )
                            .clickable { selectedType = DebtType.OWED_TO_ME }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.debtsOwedToMe,
                            color = if (selectedType == DebtType.OWED_TO_ME) CyanNeon else colors.textMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Person Name
                OutlinedTextField(
                    value = personNameText,
                    onValueChange = { personNameText = it },
                    label = { Text(strings.titleOrPerson) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = colors.primaryNeon,
                        unfocusedBorderColor = colors.glassBorderSubtle,
                        focusedLabelColor = colors.primaryNeon,
                        unfocusedLabelColor = colors.textSecondary
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("${strings.amount} (${strings.currencyCode})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = colors.primaryNeon,
                        unfocusedBorderColor = colors.glassBorderSubtle,
                        focusedLabelColor = colors.primaryNeon,
                        unfocusedLabelColor = colors.textSecondary
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Payment Plan Selector (Full Payment vs Monthly Installments)
                Text(
                    text = strings.debtPaymentMode,
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.glassCard)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(11.dp))
                            .background(
                                if (!isRecurringInstallment) colors.primaryNeon.copy(alpha = 0.25f)
                                else Color.Transparent
                            )
                            .clickable { isRecurringInstallment = false }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.debtPayInFull,
                            color = if (!isRecurringInstallment) colors.primaryNeon else colors.textMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(11.dp))
                            .background(
                                if (isRecurringInstallment) AmberNeon.copy(alpha = 0.25f)
                                else Color.Transparent
                            )
                            .clickable { isRecurringInstallment = true }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.debtMonthlyInstallments,
                            color = if (isRecurringInstallment) AmberNeon else colors.textMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (!isRecurringInstallment) {
                    // Due Date Picker for One-Time Settlement
                    Text(
                        text = strings.debtDueDate,
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.glassCard)
                            .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(16.dp))
                            .clickable { openCalendarPicker() }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = strings.pickFromCalendar,
                                tint = colors.primaryNeon,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = formatDate(dueDateMillis),
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.primaryNeon.copy(alpha = 0.2f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = strings.pickFromCalendar,
                                color = colors.primaryNeon,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Quick Day Shortcuts
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(7, 14, 30, 60).forEach { days ->
                            val targetMillis = System.currentTimeMillis() + days * 86400000L
                            val isSelected = Math.abs(dueDateMillis - targetMillis) < 86400000L
                            GlassPill(
                                text = "+$days d",
                                isSelected = isSelected,
                                onClick = { dueDateMillis = targetMillis }
                            )
                        }
                    }
                } else {
                    // Recurring Installments: Monthly Amount & Custom Day of Month
                    OutlinedTextField(
                        value = installmentAmountText,
                        onValueChange = { installmentAmountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text(strings.monthlyInstallmentAmount) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            focusedBorderColor = AmberNeon,
                            unfocusedBorderColor = colors.glassBorderSubtle,
                            focusedLabelColor = AmberNeon,
                            unfocusedLabelColor = colors.textSecondary
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Compact Single-Line Day Picker
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.glassCard)
                            .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(16.dp))
                            .clickable { openInstallmentDayPicker() }
                            .padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = strings.pickFromCalendar,
                                tint = AmberNeon,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = strings.installmentDayOfMonth,
                                color = colors.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(AmberNeon.copy(alpha = 0.2f))
                                .border(1.dp, AmberNeon.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "Day $installmentDay",
                                color = AmberNeon,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Auto-Payment Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.glassCard)
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.debtAutoPay,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (isRecurringInstallment) "Auto-deducts monthly installment until settled"
                            else "Auto-executes transaction when due",
                            color = colors.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = autoPayOnDueDate,
                        onCheckedChange = { autoPayOnDueDate = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = if (isRecurringInstallment) AmberNeon else colors.primaryNeon,
                            uncheckedThumbColor = colors.textMuted,
                            uncheckedTrackColor = colors.glassCard
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Note
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text(strings.note) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = colors.primaryNeon,
                        unfocusedBorderColor = colors.glassBorderSubtle,
                        focusedLabelColor = colors.primaryNeon,
                        unfocusedLabelColor = colors.textSecondary
                    ),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(22.dp))

                val totalAmount = amountText.toDoubleOrNull() ?: 0.0
                val installmentAmount = installmentAmountText.toDoubleOrNull() ?: (totalAmount / 3.0)
                val isValid = totalAmount > 0.0 && personNameText.isNotBlank() &&
                        (!isRecurringInstallment || installmentAmount > 0.0)

                GlassButton(
                    text = strings.save,
                    enabled = isValid,
                    containerColor = if (selectedType == DebtType.OWED_BY_ME) AmberNeon else colors.primaryNeon,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onSave(
                            selectedType,
                            personNameText.trim(),
                            totalAmount,
                            noteText.trim(),
                            dueDateMillis,
                            autoPayOnDueDate,
                            isRecurringInstallment,
                            if (isRecurringInstallment) installmentAmount else 0.0,
                            installmentDay
                        )
                        onDismiss()
                    }
                )
            }
        }
    }
}
