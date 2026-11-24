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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
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
fun AddDebtDialog(
    initialType: DebtType = DebtType.OWED_BY_ME,
    existingDebt: DebtEntity? = null,
    existingDebts: List<DebtEntity> = emptyList(),
    onAddToExistingDebt: ((targetDebt: DebtEntity, addedAmount: Double, extraNote: String, updatedDueDateMillis: Long?) -> Unit)? = null,
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
        installmentDay: Int,
        paidAmount: Double
    ) -> Unit
) {
    val strings = AppStrings.current
    val isArabic = strings is ArabicStrings
    val colors = LiquidGlassTheme.colors
    val isDark = colors.isDark
    val context = LocalContext.current

    var selectedType by remember { mutableStateOf(existingDebt?.type ?: initialType) }
    var personNameText by remember { mutableStateOf(existingDebt?.personName ?: "") }
    var addOnExistingMode by remember { mutableStateOf(false) }
    val matchingExistingDebts = remember(existingDebts, selectedType) {
        existingDebts
            .filter { it.type == selectedType }
            .sortedWith(compareBy<DebtEntity> { it.isPaid }.thenByDescending { it.createdDateMillis })
    }
    var selectedExistingDebtId by remember(matchingExistingDebts) {
        mutableLongStateOf(matchingExistingDebts.firstOrNull()?.id ?: -1L)
    }
    val selectedExistingDebt = remember(matchingExistingDebts, selectedExistingDebtId) {
        matchingExistingDebts.firstOrNull { it.id == selectedExistingDebtId }
            ?: matchingExistingDebts.firstOrNull()
    }
    val isStackingOnExisting = existingDebt == null &&
        onAddToExistingDebt != null &&
        addOnExistingMode &&
        selectedExistingDebt != null

    var amountText by remember {
        val initialAmt = existingDebt?.amount?.let { a ->
            if (a % 1.0 == 0.0) a.toLong().toString() else a.toString()
        } ?: ""
        mutableStateOf(initialAmt)
    }

    // New: Paid Amount input state
    var paidAmountText by remember {
        val initialPaid = existingDebt?.let { d ->
            val paid = (d.amount - d.remainingAmount).coerceAtLeast(0.0)
            if (paid > 0) {
                if (paid % 1.0 == 0.0) paid.toLong().toString() else paid.toString()
            } else ""
        } ?: ""
        mutableStateOf(initialPaid)
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

    val isScreenshotProtected = LocalScreenshotProtection.current
    val contactPicker = rememberContactPickerFlow(secureScreenshots = isScreenshotProtected) { resolvedName, _, _ ->
        if (resolvedName.isNotBlank()) {
            personNameText = resolvedName
        }
    }

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

                Spacer(modifier = Modifier.height(18.dp))

                // Debt Type Toggle
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
                                if (selectedType == DebtType.OWED_BY_ME) DebtColors.payableContainer(isDark)
                                else Color.Transparent
                            )
                            .border(
                                1.dp,
                                if (selectedType == DebtType.OWED_BY_ME) DebtColors.payableBorder(isDark)
                                else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedType = DebtType.OWED_BY_ME }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.debtsOwedByMe,
                            color = if (selectedType == DebtType.OWED_BY_ME) DebtColors.payable(isDark) else colors.textMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selectedType == DebtType.OWED_TO_ME) DebtColors.receivableContainer(isDark)
                                else Color.Transparent
                            )
                            .border(
                                1.dp,
                                if (selectedType == DebtType.OWED_TO_ME) DebtColors.receivableBorder(isDark)
                                else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedType = DebtType.OWED_TO_ME }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.debtsOwedToMe,
                            color = if (selectedType == DebtType.OWED_TO_ME) DebtColors.receivable(isDark) else colors.textMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (existingDebt == null && onAddToExistingDebt != null && matchingExistingDebts.isNotEmpty()) {
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
                                    if (!addOnExistingMode) colors.primaryNeon.copy(alpha = 0.22f)
                                    else Color.Transparent
                                )
                                .clickable { addOnExistingMode = false }
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isArabic) "دين جديد" else "New Debt",
                                color = if (!addOnExistingMode) colors.primaryNeon else colors.textMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(11.dp))
                                .background(
                                    if (addOnExistingMode) colors.primaryNeon.copy(alpha = 0.22f)
                                    else Color.Transparent
                                )
                                .clickable {
                                    addOnExistingMode = true
                                    if (selectedExistingDebt == null) {
                                        selectedExistingDebtId = matchingExistingDebts.firstOrNull()?.id ?: -1L
                                    }
                                }
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isArabic) "على دين قديم (${matchingExistingDebts.size})"
                                else "Add to Existing (${matchingExistingDebts.size})",
                                color = if (addOnExistingMode) colors.primaryNeon else colors.textMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                } else {
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (isStackingOnExisting && selectedExistingDebt != null) {
                    val accentColor = if (selectedType == DebtType.OWED_BY_ME) {
                        DebtColors.payable(isDark)
                    } else {
                        DebtColors.receivable(isDark)
                    }

                    Text(
                        text = if (isArabic) "اختر الدين القديم" else "Select existing debt",
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        matchingExistingDebts.forEach { candidate ->
                            val isSelected = candidate.id == selectedExistingDebt.id
                            val candidateRem = candidate.remainingAmount.coerceAtLeast(0.0)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (isSelected) accentColor.copy(alpha = 0.14f)
                                        else colors.glassCard
                                    )
                                    .border(
                                        width = if (isSelected) 1.3.dp else 1.dp,
                                        color = if (isSelected) accentColor else colors.glassBorderSubtle,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable { selectedExistingDebtId = candidate.id }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = candidate.personName,
                                        color = colors.textPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (candidate.isPaid) {
                                            if (isArabic) "مسدد بالكامل • الإجمالي: ${isolateLtr(formatDzd(candidate.amount, strings.currencyCode))}"
                                            else "Settled • Total: ${formatDzd(candidate.amount, strings.currencyCode)}"
                                        } else {
                                            if (isArabic) "المتبقي: ${isolateLtr(formatDzd(candidateRem, strings.currencyCode))}"
                                            else "Remaining: ${formatDzd(candidateRem, strings.currencyCode)}"
                                        },
                                        color = if (candidate.isPaid) DebtColors.settled(isDark) else accentColor,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = {
                            Text(
                                if (isArabic) "مبلغ الدين المضاف (${strings.currencyCode})"
                                else "Added Amount (${strings.currencyCode})"
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = TextStyle(textDirection = TextDirection.Ltr),
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
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(500L, 1000L, 2000L, 5000L).forEach { step ->
                            GlassPill(
                                text = "+$step",
                                isSelected = false,
                                onClick = {
                                    val nextVal = (amountText.toDoubleOrNull() ?: 0.0) + step
                                    amountText = if (nextVal % 1.0 == 0.0) nextVal.toLong().toString() else nextVal.toString()
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
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

                    val addedAmt = amountText.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 } ?: 0.0
                    if (addedAmt > 0.0) {
                        val projRem = selectedExistingDebt.remainingAmount.coerceAtLeast(0.0) + addedAmt
                        val projTot = selectedExistingDebt.amount + addedAmt
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
                                        text = if (isArabic) "النتيجة بعد الإضافة:" else "Projected outcome:",
                                        color = accentColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isArabic) {
                                            "المتبقي الجديد: ${isolateLtr(formatDzd(projRem, strings.currencyCode))}"
                                        } else {
                                            "New Remaining: ${formatDzd(projRem, strings.currencyCode)}"
                                        },
                                        color = colors.textPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = if (isArabic) {
                                            "الإجمالي الجديد: ${isolateLtr(formatDzd(projTot, strings.currencyCode))}"
                                        } else {
                                            "New Total: ${formatDzd(projTot, strings.currencyCode)}"
                                        },
                                        color = colors.textSecondary,
                                        fontSize = 11.sp
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
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    GlassButton(
                        text = if (addedAmt > 0.0) {
                            if (isArabic) "إضافة ${isolateLtr(formatDzd(addedAmt, strings.currencyCode))}"
                            else "Add ${formatDzd(addedAmt, strings.currencyCode)}"
                        } else strings.save,
                        icon = Icons.Default.Add,
                        enabled = addedAmt > 0.0,
                        containerColor = accentColor,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            onAddToExistingDebt(
                                selectedExistingDebt,
                                addedAmt,
                                noteText.trim(),
                                null
                            )
                            onDismiss()
                        }
                    )
                } else {
                // Title / Person with Contacts Picker Icon
                OutlinedTextField(
                    value = personNameText,
                    onValueChange = { personNameText = it },
                    label = { Text(strings.titleOrPerson) },
                    trailingIcon = {
                        IconButton(
                            onClick = { contactPicker.openPicker() },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("debt_pick_contact_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContactPhone,
                                contentDescription = strings.pickFromContacts,
                                tint = colors.primaryNeon
                            )
                        }
                    },
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

                if (contactPicker.permissionDeniedHint && !contactPicker.hasPermission) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DebtColors.payableContainer(isDark))
                            .border(1.dp, DebtColors.payableBorder(isDark), RoundedCornerShape(12.dp))
                            .clickable { openAppContactsPermissionSettings(context) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isArabic) {
                                "إذن جهات الاتصال مرفوض — اضغط لفتح الإعدادات وتفعيله"
                            } else {
                                "Contacts permission denied — tap to enable in Settings"
                            },
                            color = DebtColors.payable(isDark),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isArabic) "الإعدادات" else "Settings",
                            color = colors.primaryNeon,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Total Debt Amount (DZD) - No duplicated currency in label!
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text(strings.amount) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = TextStyle(textDirection = TextDirection.Ltr),
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

                // Paid Amount Field ("المبلغ المدفوع")
                OutlinedTextField(
                    value = paidAmountText,
                    onValueChange = { paidAmountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text(strings.paidAmount) },
                    placeholder = { Text("0") },
                    supportingText = {
                        val total = amountText.toDoubleOrNull() ?: 0.0
                        val paid = paidAmountText.toDoubleOrNull() ?: 0.0
                        val rem = (total - paid).coerceAtLeast(0.0)
                        val pct = if (total > 0) ((paid / total) * 100).toInt().coerceIn(0, 100) else 0
                        val pctStr = if (isArabic) "$pct٪" else "$pct%"
                        if (paid > 0) {
                            Text(
                                text = "${strings.remainingBalance}: ${isolateLtr(formatDzd(rem, strings.currencyCode))} ($pctStr ${strings.paidOutOf} ${isolateLtr(formatDzd(total, strings.currencyCode))})",
                                color = if (rem <= 0.001) DebtColors.settled(isDark)
                                else if (selectedType == DebtType.OWED_BY_ME) DebtColors.payable(isDark)
                                else DebtColors.receivable(isDark),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Text(
                                text = strings.initialPaidAmountHelp,
                                color = colors.textMuted,
                                fontSize = 10.sp
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = TextStyle(textDirection = TextDirection.Ltr),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = DebtColors.settled(isDark),
                        unfocusedBorderColor = colors.glassBorderSubtle,
                        focusedLabelColor = DebtColors.settled(isDark),
                        unfocusedLabelColor = colors.textSecondary
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

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
                                if (isRecurringInstallment) DebtColors.autoPayContainer(isDark)
                                else Color.Transparent
                            )
                            .border(
                                1.dp,
                                if (isRecurringInstallment) DebtColors.autoPayBorder(isDark)
                                else Color.Transparent,
                                RoundedCornerShape(11.dp)
                            )
                            .clickable { isRecurringInstallment = true }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.debtMonthlyInstallments,
                            color = if (isRecurringInstallment) DebtColors.autoPay(isDark) else colors.textMuted,
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
                                text = isolateLtr(formatDate(dueDateMillis, isArabic)),
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
                                text = if (isArabic) "+$days يوم" else "+$days d",
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
                        textStyle = TextStyle(textDirection = TextDirection.Ltr),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            focusedBorderColor = DebtColors.autoPay(isDark),
                            unfocusedBorderColor = colors.glassBorderSubtle,
                            focusedLabelColor = DebtColors.autoPay(isDark),
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
                                tint = DebtColors.autoPay(isDark),
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
                                .background(DebtColors.autoPayContainer(isDark))
                                .border(1.dp, DebtColors.autoPayBorder(isDark), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = if (isArabic) "يوم $installmentDay" else "Day $installmentDay",
                                color = DebtColors.autoPay(isDark),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Quick Installment Day Shortcuts
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1, 5, 10, 15, 20, 25, 28).forEach { day ->
                            GlassPill(
                                text = if (isArabic) "يوم $day" else "Day $day",
                                isSelected = installmentDay == day,
                                onClick = { installmentDay = day }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Auto Pay Switch
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
                            fontSize = 13.sp
                        )
                        Text(
                            text = if (isArabic) {
                                if (isRecurringInstallment) "تنفيذ القسط تلقائياً في يوم $installmentDay من كل شهر"
                                else "خصم المبلغ تلقائياً بتاريخ الاستحقاق (${isolateLtr(formatDate(dueDateMillis, true))})"
                            } else {
                                if (isRecurringInstallment) "Automatically execute installment on Day $installmentDay"
                                else "Automatically deduct on due date (${formatDate(dueDateMillis, false)})"
                            },
                            color = colors.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = autoPayOnDueDate,
                        onCheckedChange = { autoPayOnDueDate = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = if (selectedType == DebtType.OWED_BY_ME) DebtColors.payable(isDark) else DebtColors.receivable(isDark),
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
                val paidAmount = paidAmountText.toDoubleOrNull() ?: 0.0
                val installmentAmount = installmentAmountText.toDoubleOrNull() ?: (totalAmount / 3.0)
                val isValid = totalAmount > 0.0 && personNameText.isNotBlank() &&
                        (!isRecurringInstallment || installmentAmount > 0.0) &&
                        (paidAmount <= totalAmount)

                GlassButton(
                    text = strings.save,
                    enabled = isValid,
                    containerColor = if (selectedType == DebtType.OWED_BY_ME) DebtColors.payable(isDark) else DebtColors.receivable(isDark),
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
                            installmentDay,
                            paidAmount
                        )
                        onDismiss()
                    }
                )
                }
            }
        }
        }
    }
}
