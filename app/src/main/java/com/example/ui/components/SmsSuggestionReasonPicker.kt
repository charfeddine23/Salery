package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Atm
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.LocalAdaptiveTypography

data class SmsReasonPreset(
    val key: String,
    val categoryId: String,
    val labelAr: String,
    val labelEn: String,
    val icon: ImageVector
) {
    fun localizedLabel(isArabic: Boolean): String = if (isArabic) labelAr else labelEn
}

object SmsSuggestionReasonCatalog {
    val withdrawalReasons: List<SmsReasonPreset> = listOf(
        SmsReasonPreset("atm_cash", "other", "سحب نقدي (ATM)", "Cash Withdrawal", Icons.Default.Atm),
        SmsReasonPreset("shopping", "shopping", "مشتريات وتسوق", "Shopping", Icons.Default.ShoppingCart),
        SmsReasonPreset("food", "food", "طعام ومطاعم", "Food & Dining", Icons.Default.Fastfood),
        SmsReasonPreset("bills", "utilities", "فواتير وخدمات", "Bills & Utilities", Icons.Default.Bolt),
        SmsReasonPreset("telecom", "internet", "شحن وانترنت", "Recharge & Internet", Icons.Default.Wifi),
        SmsReasonPreset("transport", "transport", "نقل ومواصلات", "Transport", Icons.Default.DirectionsCar),
        SmsReasonPreset("housing", "housing", "كراء وسكن", "Rent & Housing", Icons.Default.Home),
        SmsReasonPreset("health", "health", "صحة ودواء", "Healthcare", Icons.Default.MedicalServices),
        SmsReasonPreset("person_transfer", "other", "تحويل لشخص", "Sent to Person", Icons.AutoMirrored.Filled.Send),
        SmsReasonPreset("taxes", "taxes_fees", "ضرائب ورسوم", "Taxes & Fees", Icons.Default.ReceiptLong)
    )

    val incomeSources: List<SmsReasonPreset> = listOf(
        SmsReasonPreset("salary", "salary", "راتب شهري", "Monthly Salary", Icons.Default.Paid),
        SmsReasonPreset("person_income", "transfer", "تحويل من شخص / جهة", "Transfer from Person", Icons.AutoMirrored.Filled.Send),
        SmsReasonPreset("freelance", "freelance", "عمل حر / مشروع", "Freelance / Project", Icons.Default.Work),
        SmsReasonPreset("sales", "investment", "تجارة / مبيعات", "Sales / Business", Icons.Default.Storefront),
        SmsReasonPreset("grant_bonus", "gift", "منحة / مكافأة", "Bonus / Grant", Icons.Default.CardGiftcard),
        SmsReasonPreset("refund", "other_income", "استرجاع مبلغ", "Refund", Icons.Default.Replay),
        SmsReasonPreset("other_src", "other_income", "مصدر دخل آخر", "Other Source", Icons.Default.AccountBalanceWallet)
    )

    fun presetsFor(isExpense: Boolean): List<SmsReasonPreset> =
        if (isExpense) withdrawalReasons else incomeSources

    fun defaultPresetKey(isExpense: Boolean, suggestedCategory: String): String? {
        val list = presetsFor(isExpense)
        return list.firstOrNull { it.categoryId == suggestedCategory }?.key
    }

    fun findPreset(isExpense: Boolean, key: String?): SmsReasonPreset? {
        if (key.isNullOrBlank()) return null
        return presetsFor(isExpense).firstOrNull { it.key == key }
    }
}

@Composable
fun SmsSuggestionReasonPicker(
    isExpense: Boolean,
    isArabic: Boolean,
    selectedPresetKey: String?,
    onSelectPreset: (SmsReasonPreset) -> Unit,
    customReasonText: String,
    onCustomReasonTextChange: (String) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val colors = LiquidGlassTheme.colors
    val typo = LocalAdaptiveTypography.current
    val presets = remember(isExpense) { SmsSuggestionReasonCatalog.presetsFor(isExpense) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.glassSurface.copy(alpha = 0.72f))
            .border(0.75.dp, colors.glassBorderSubtle, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.EditNote,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = if (isExpense) {
                        if (isArabic) "سبب السحب" else "Withdrawal Reason"
                    } else {
                        if (isArabic) "جهة الدخل أو سببه" else "Income Source / Reason"
                    },
                    color = colors.textPrimary,
                    fontSize = typo.caption,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = if (isArabic) "اختياري" else "Optional",
                color = colors.textMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(7.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(presets, key = { it.key }) { preset ->
                val isSelected = selectedPresetKey == preset.key
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .defaultMinSize(minHeight = 36.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(
                            if (isSelected) accent.copy(alpha = 0.22f)
                            else colors.glassCard.copy(alpha = 0.75f)
                        )
                        .border(
                            width = if (isSelected) 1.dp else 0.7.dp,
                            color = if (isSelected) accent.copy(alpha = 0.7f) else colors.glassBorderSubtle,
                            shape = RoundedCornerShape(9.dp)
                        )
                        .clickable { onSelectPreset(preset) }
                        .testTag("sms_reason_chip_${preset.key}")
                        .padding(horizontal = 9.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = preset.icon,
                        contentDescription = null,
                        tint = if (isSelected) accent else colors.textMuted,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = preset.localizedLabel(isArabic),
                        color = if (isSelected) accent else colors.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(colors.glassCard)
                .border(
                    width = if (customReasonText.isNotBlank()) 1.dp else 0.7.dp,
                    color = if (customReasonText.isNotBlank()) accent.copy(alpha = 0.55f) else colors.glassBorderSubtle,
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (customReasonText.isEmpty()) {
                    Text(
                        text = if (isExpense) {
                            if (isArabic) "اكتب سبب السحب (مثال: شراء دواء، فليكسي...)"
                            else "Write withdrawal reason (e.g. Groceries, Rent...)"
                        } else {
                            if (isArabic) "اكتب جهة الدخل أو سببه (مثال: راتب، تحويل من...)"
                            else "Write income source or reason (e.g. Employer, Client...)"
                        },
                        color = colors.textMuted.copy(alpha = 0.65f),
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = customReasonText,
                    onValueChange = { raw ->
                        if (raw.length <= 64) {
                            onCustomReasonTextChange(raw)
                        }
                    },
                    textStyle = TextStyle(
                        color = colors.textPrimary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sms_reason_custom_input"),
                    cursorBrush = SolidColor(accent)
                )
            }
            if (customReasonText.isNotEmpty()) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.glassSurface)
                        .clickable { onCustomReasonTextChange("") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = if (isArabic) "مسح" else "Clear",
                        tint = colors.textMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
