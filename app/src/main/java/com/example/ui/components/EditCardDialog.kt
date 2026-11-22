package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import com.example.data.AlgerianBankCardCatalog
import com.example.data.AlgerianBankMotif
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.ArabicStrings
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.RoseNeon

@Composable
fun EditCardDialog(
    currentBankStyle: String = "EDAHABIA",
    currentHolderName: String,
    currentCardNumber: String,
    currentExpiry: String,
    currentCvc: String,
    secureScreenshots: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (holderName: String, cardNumber: String, expiryDate: String, cvc: String, bankStyle: String) -> Unit
) {
    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors
    val isArabic = strings is ArabicStrings

    var selectedBankStyle by remember {
        mutableStateOf(AlgerianBankCardCatalog.findById(currentBankStyle).id)
    }
    val bankSpec = remember(selectedBankStyle) {
        AlgerianBankCardCatalog.findById(selectedBankStyle)
    }
    var holderName by remember { mutableStateOf(currentHolderName) }
    var cardNumberDigits by remember {
        mutableStateOf(currentCardNumber.filter { it.isDigit() }.take(16))
    }
    var expiryDigits by remember {
        mutableStateOf(currentExpiry.filter { it.isDigit() }.take(4))
    }
    var cvcDigits by remember {
        mutableStateOf(currentCvc.filter { it.isDigit() }.take(4))
    }

    DisposableEffect(Unit) {
        onDispose {
            holderName = ""
            cardNumberDigits = ""
            expiryDigits = ""
            cvcDigits = ""
        }
    }

    val isNameValid = holderName.trim().isNotEmpty()
    val isCardNumberValid = cardNumberDigits.isEmpty() || cardNumberDigits.length == 16
    val expiryMonth = expiryDigits.take(2).toIntOrNull() ?: 0
    val isExpiryValid = expiryDigits.length == 4 && expiryMonth in 1..12
    val isCvcValid = cvcDigits.isEmpty() || cvcDigits.length in 3..4
    val canSave = isNameValid && isCardNumberValid && isExpiryValid && isCvcValid

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            securePolicy = if (secureScreenshots) SecureFlagPolicy.SecureOn else SecureFlagPolicy.SecureOff
        )
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr
        ) {
        GlassCard(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            shape = RoundedCornerShape(26.dp),
            backgroundColor = colors.dialogBackground
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF2563EB).copy(alpha = 0.22f))
                                .border(1.dp, CyanNeon.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = null,
                                tint = CyanNeon,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = strings.editCardTitle,
                                color = colors.textPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = strings.editCardSubtitle,
                                color = colors.textMuted,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("close_edit_card_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = strings.cancel,
                            tint = colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Algerian Banks Selector
                Text(
                    text = if (isArabic) "اختر تصميم البنك الجزائري (${AlgerianBankCardCatalog.all.size} بطاقة)" else "Choose Algerian Bank Card (${AlgerianBankCardCatalog.all.size} styles)",
                    color = colors.textSecondary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(AlgerianBankCardCatalog.all, key = { it.id }) { bank ->
                        val isSelected = bank.id == bankSpec.id
                        val primaryColor = Color(bank.gradientHexes.first())
                        val accentColor = Color(bank.accentHex)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) primaryColor.copy(alpha = 0.30f)
                                    else colors.glassCard
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) accentColor else colors.glassBorderSubtle,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedBankStyle = bank.id }
                                .testTag("select_dz_bank_${bank.id.lowercase()}")
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(listOf(primaryColor, accentColor))
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = when {
                                        isArabic && bank.id == "EDAHABIA" -> "الذهبية"
                                        isArabic -> bank.nameAr
                                        bank.id == "EDAHABIA" -> "Edahabia"
                                        else -> bank.shortCode
                                    },
                                    color = if (isSelected) colors.textPrimary else colors.textSecondary,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    maxLines = 1
                                )
                                Text(
                                    text = if (isArabic) bank.cardTypeLabel else bank.nameEn,
                                    color = colors.textMuted,
                                    fontSize = 9.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Live Mini Preview of the Selected Algerian Bank Card
                val bankAccent = Color(bankSpec.accentHex)
                val bankSecondaryText = Color(bankSpec.secondaryTextHex)
                val isEdahabia = bankSpec.motif == AlgerianBankMotif.EDAHABIA_ZELLIGE
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = bankSpec.gradientHexes.map { Color(it) },
                                start = Offset(0f, 0f),
                                end = Offset(700f, 350f)
                            )
                        )
                        .drawBehind {
                            if (isEdahabia) {
                                val boundary = Path().apply {
                                    moveTo(size.width * 0.85f, 0f)
                                    quadraticTo(
                                        size.width * 0.90f, size.height * 0.45f,
                                        size.width * 0.86f, size.height
                                    )
                                }
                                val clipP = Path().apply {
                                    addPath(boundary)
                                    lineTo(size.width, size.height)
                                    lineTo(size.width, 0f)
                                    close()
                                }
                                clipPath(clipP) {
                                    drawRect(
                                        color = Color(0xFF1E3A8A),
                                        topLeft = Offset(size.width * 0.83f, 0f),
                                        size = Size(size.width * 0.19f, size.height)
                                    )
                                    val miniColors = listOf(
                                        Color(0xFFFACC15),
                                        Color.White,
                                        Color(0xFF38BDF8),
                                        Color(0xFF1D4ED8)
                                    )
                                    val cw = size.width * 0.065f
                                    val ch = size.height / 3.8f
                                    for (r in -1..5) {
                                        for (c in 0..3) {
                                            val cx = size.width * 0.84f + c * cw + (if (r % 2 == 0) 0f else cw * 0.5f)
                                            val cy = r * ch
                                            val tile = Path().apply {
                                                moveTo(cx, cy - ch * 0.5f)
                                                lineTo(cx + cw * 0.5f, cy)
                                                lineTo(cx, cy + ch * 0.5f)
                                                lineTo(cx - cw * 0.5f, cy)
                                                close()
                                            }
                                            drawPath(tile, miniColors[(r + c + 4) % miniColors.size])
                                        }
                                    }
                                }
                                drawPath(
                                    path = boundary,
                                    color = Color(0xFF93C5FD).copy(alpha = 0.45f),
                                    style = Stroke(width = 1.2f)
                                )
                            } else {
                                drawCircle(
                                    color = bankAccent.copy(alpha = 0.14f),
                                    radius = size.height * 0.55f,
                                    center = Offset(size.width * 0.88f, size.height * 0.42f),
                                    style = Stroke(width = 14f)
                                )
                                val ribbon = Path().apply {
                                    moveTo(size.width * 0.52f, 0f)
                                    lineTo(size.width * 0.72f, 0f)
                                    lineTo(size.width * 0.44f, size.height)
                                    lineTo(size.width * 0.24f, size.height)
                                    close()
                                }
                                drawPath(ribbon, bankAccent.copy(alpha = 0.09f))
                            }
                        }
                        .border(
                            1.dp,
                            bankAccent.copy(alpha = 0.55f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(
                            start = 14.dp,
                            end = if (isEdahabia) 42.dp else 14.dp,
                            top = 12.dp,
                            bottom = 12.dp
                        )
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text(
                                        text = if (isEdahabia) "الذهبية" else bankSpec.nameAr,
                                        color = bankSecondaryText,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 9.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = bankSpec.cardTypeLabel,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.1.sp,
                                        lineHeight = 13.sp
                                    )
                                }
                                Text(
                                    text = if (isEdahabia) "بريد الجزائر" else "CIB • ${bankSpec.shortCode}",
                                    color = bankAccent,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            val previewNumber = if (cardNumberDigits.isBlank()) {
                                bankSpec.defaultNumberPrefix
                            } else {
                                formatCardDigitsPreview(cardNumberDigits)
                            }
                            Text(
                                text = previewNumber,
                                color = Color.White,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.1.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = holderName.trim().ifBlank { "CHARFEDDINE KA" }.uppercase(),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                val previewExp = if (expiryDigits.length >= 2) {
                                    val m = expiryDigits.take(2)
                                    val y = expiryDigits.drop(2).padEnd(2, '•')
                                    "$m / $y"
                                } else {
                                    "MM / YY"
                                }
                                val previewCvc = cvcDigits.ifBlank { "•••" }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "EXP $previewExp",
                                        color = bankSecondaryText,
                                        fontSize = 9.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "CVC $previewCvc",
                                        color = bankSecondaryText,
                                        fontSize = 9.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 1. Cardholder Full Name (الاسم واللقب)
                CardInputBox(
                    label = strings.cardHolderNameLabel,
                    value = holderName,
                    onValueChange = { if (it.length <= 32) holderName = it },
                    placeholder = "CHARFEDDINE KA",
                    icon = Icons.Default.Person,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        keyboardType = KeyboardType.Text
                    ),
                    isError = !isNameValid,
                    testTag = "card_holder_input"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Card Number (رقم البطاقة - 16 digits)
                CardInputBox(
                    label = strings.cardNumberLabel,
                    value = cardNumberDigits,
                    onValueChange = { input ->
                        cardNumberDigits = input.filter { it.isDigit() }.take(16)
                    },
                    placeholder = "6280 2600 0000 9842",
                    icon = Icons.Default.CreditCard,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = CardNumberVisualTransformation(),
                    trailingBadge = "${cardNumberDigits.length}/16",
                    trailingBadgeColor = when {
                        cardNumberDigits.isEmpty() -> colors.textMuted
                        cardNumberDigits.length == 16 -> EmeraldNeon
                        else -> AmberNeon
                    },
                    isError = !isCardNumberValid,
                    useMonospace = true,
                    ltrInput = true,
                    testTag = "card_number_input"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Expiry Date (تاريخ نهاية الصلاحية) & 4. CVC Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.weight(1.15f)) {
                        CardInputBox(
                            label = strings.cardExpiryLabel,
                            value = expiryDigits,
                            onValueChange = { input ->
                                expiryDigits = input.filter { it.isDigit() }.take(4)
                            },
                            placeholder = "11/29",
                            icon = Icons.Default.Event,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = ExpiryDateVisualTransformation(),
                            isError = !isExpiryValid,
                            useMonospace = true,
                            ltrInput = true,
                            testTag = "card_expiry_input"
                        )
                    }

                    Box(modifier = Modifier.weight(0.85f)) {
                        CardInputBox(
                            label = strings.cardCvcLabel,
                            value = cvcDigits,
                            onValueChange = { input ->
                                cvcDigits = input.filter { it.isDigit() }.take(4)
                            },
                            placeholder = "842",
                            icon = Icons.Default.Lock,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = !isCvcValid,
                            useMonospace = true,
                            ltrInput = true,
                            testTag = "card_cvc_input"
                        )
                    }
                }

                // Helpful inline validation text if needed
                if (!canSave) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val errorMsg = when {
                        !isNameValid -> if (isArabic) "يرجى إدخال الاسم واللقب" else "Please enter cardholder name"
                        !isCardNumberValid -> if (isArabic) "رقم البطاقة يجب أن يتكون من 16 رقماً أو يُترك فارغاً" else "Card number must be 16 digits (or left blank)"
                        !isExpiryValid -> if (isArabic) "صيغة الصلاحية غير صحيحة (مثال: 11/29)" else "Enter valid expiry date (MM/YY, month 01-12)"
                        else -> if (isArabic) "رمز التحقق يجب أن يكون 3 أو 4 أرقام" else "CVC must be 3 or 4 digits"
                    }
                    Text(
                        text = errorMsg,
                        color = RoseNeon,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassButton(
                        text = strings.cancel,
                        containerColor = colors.textMuted.copy(alpha = 0.25f),
                        contentColor = colors.textPrimary,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("cancel_edit_card_button"),
                        onClick = onDismiss
                    )

                    GlassButton(
                        text = strings.save,
                        enabled = canSave,
                        containerColor = colors.primaryNeon,
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("save_edit_card_button"),
                        onClick = {
                            val formattedExp = "${expiryDigits.substring(0, 2)}/${expiryDigits.substring(2, 4)}"
                            onSave(
                                holderName.trim(),
                                cardNumberDigits,
                                formattedExp,
                                cvcDigits,
                                bankSpec.id
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

@Composable
private fun CardInputBox(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingBadge: String? = null,
    trailingBadgeColor: Color = Color.Unspecified,
    isError: Boolean = false,
    useMonospace: Boolean = false,
    ltrInput: Boolean = false,
    testTag: String
) {
    val colors = LiquidGlassTheme.colors
    val borderColor = when {
        isError -> RoseNeon.copy(alpha = 0.85f)
        else -> colors.glassBorderSubtle
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.glassCard)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isError) RoseNeon else colors.primaryNeon,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    color = colors.textSecondary,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (trailingBadge != null) {
                Text(
                    text = trailingBadge,
                    color = trailingBadgeColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        val inputDirection = if (ltrInput) LayoutDirection.Ltr else LocalLayoutDirection.current
        CompositionLocalProvider(LocalLayoutDirection provides inputDirection) {
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = colors.textMuted.copy(alpha = 0.55f),
                        fontSize = 14.5.sp,
                        fontFamily = if (useMonospace) FontFamily.Monospace else FontFamily.Default,
                        fontWeight = FontWeight.Medium
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = TextStyle(
                        color = colors.textPrimary,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = if (useMonospace) FontFamily.Monospace else FontFamily.Default
                    ),
                    keyboardOptions = keyboardOptions,
                    visualTransformation = visualTransformation,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(testTag),
                    cursorBrush = SolidColor(colors.primaryNeon)
                )
            }
        }
    }
}

fun formatCardDigitsDisplay(rawDigits: String, isStealth: Boolean): String {
    val digits = rawDigits.filter { it.isDigit() }
    if (digits.isEmpty()) {
        return if (isStealth) "••••  ••••  ••••  ••••" else "6280  26••  ••••  9842"
    }
    if (isStealth) {
        val last4 = if (digits.length >= 4) digits.takeLast(4) else digits.padStart(4, '•')
        return "••••  ••••  ••••  $last4"
    }
    return digits.chunked(4).joinToString("  ")
}

private fun formatCardDigitsPreview(digits: String): String {
    val padded = digits.padEnd(16, '•')
    return padded.chunked(4).joinToString("  ")
}

private class CardNumberVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val out = StringBuilder()
        for (i in raw.indices) {
            out.append(raw[i])
            if (i % 4 == 3 && i != raw.lastIndex) {
                out.append(' ')
            }
        }
        val transformed = out.toString()
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val safe = offset.coerceIn(0, raw.length)
                val spaces = when {
                    safe <= 4 -> 0
                    safe <= 8 -> 1
                    safe <= 12 -> 2
                    else -> 3
                }
                return (safe + spaces).coerceIn(0, transformed.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                val safe = offset.coerceIn(0, transformed.length)
                val spaces = when {
                    safe <= 4 -> 0
                    safe <= 9 -> 1
                    safe <= 14 -> 2
                    else -> 3
                }
                return (safe - spaces).coerceIn(0, raw.length)
            }
        }
        return TransformedText(AnnotatedString(transformed), offsetMapping)
    }
}

private class ExpiryDateVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val out = StringBuilder()
        for (i in raw.indices) {
            out.append(raw[i])
            if (i == 1 && i != raw.lastIndex) {
                out.append('/')
            }
        }
        val transformed = out.toString()
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val safe = offset.coerceIn(0, raw.length)
                val slash = if (safe > 2) 1 else 0
                return (safe + slash).coerceIn(0, transformed.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                val safe = offset.coerceIn(0, transformed.length)
                val slash = if (safe > 2) 1 else 0
                return (safe - slash).coerceIn(0, raw.length)
            }
        }
        return TransformedText(AnnotatedString(transformed), offsetMapping)
    }
}
