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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.ArabicStrings
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.RoseNeon

@Composable
fun EditVisaCardDialog(
    currentProvider: String,
    currentHolderName: String,
    currentCardNumber: String,
    currentExpiry: String,
    currentCvc: String,
    secureScreenshots: Boolean = true,
    isThirdCard: Boolean = false,
    onDeleteThirdCard: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onSave: (
        provider: String,
        holderName: String,
        cardNumber: String,
        expiryDate: String,
        cvc: String
    ) -> Unit
) {
    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors
    val isArabic = strings is ArabicStrings

    var selectedProvider by remember {
        mutableStateOf(
            when (currentProvider.uppercase()) {
                "REDOTPAY" -> "REDOTPAY"
                "BYBIT" -> "BYBIT"
                "BINANCE" -> "BINANCE"
                else -> "DEFAULT"
            }
        )
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

    val isNameValid = holderName.trim().isNotEmpty()
    val isCardNumberValid = cardNumberDigits.isEmpty() || cardNumberDigits.length == 16
    val expiryMonth = expiryDigits.take(2).toIntOrNull() ?: 0
    val isExpiryValid = expiryDigits.isEmpty() || (expiryDigits.length == 4 && expiryMonth in 1..12)
    val isCvcValid = cvcDigits.isEmpty() || cvcDigits.length in 3..4
    val canSave = isNameValid && isCardNumberValid && isExpiryValid && isCvcValid

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            securePolicy = if (secureScreenshots) SecureFlagPolicy.SecureOn else SecureFlagPolicy.SecureOff
        )
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
                                .background(
                                    when (selectedProvider) {
                                        "REDOTPAY" -> Color(0xFFE11D48).copy(alpha = 0.22f)
                                        "BYBIT" -> Color(0xFFF7A600).copy(alpha = 0.22f)
                                        "BINANCE" -> Color(0xFFF0B90B).copy(alpha = 0.22f)
                                        else -> CyanNeon.copy(alpha = 0.20f)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = null,
                                tint = when (selectedProvider) {
                                    "REDOTPAY" -> Color(0xFFFB7185)
                                    "BYBIT" -> Color(0xFFF7A600)
                                    "BINANCE" -> Color(0xFFF0B90B)
                                    else -> CyanNeon
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = when {
                                    isThirdCard -> if (isArabic) "إعداد البطاقة الثالثة" else "Configure Third Card"
                                    else -> if (isArabic) "إعداد البطاقة الدولية (Visa)" else "Configure Virtual / Bank Card"
                                },
                                color = colors.textPrimary,
                                fontSize = 16.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isArabic) "اختر RedotPay أو Bybit أو Binance وأدخل بيانات بطاقتك" else "Choose RedotPay, Bybit, Binance, or default Visa card",
                                color = colors.textMuted,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (isThirdCard && onDeleteThirdCard != null) {
                        IconButton(
                            onClick = {
                                onDeleteThirdCard()
                                onDismiss()
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("remove_third_card_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = if (isArabic) "حذف البطاقة الثالثة" else "Remove third card",
                                tint = RoseNeon
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("close_edit_visa_card_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = strings.cancel,
                            tint = colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Provider Selection Pills: 2x2 Grid (RedotPay | Bybit / Binance | Default)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProviderChoicePill(
                            title = "RedotPay",
                            accent = Color(0xFFE11D48),
                            isSelected = selectedProvider == "REDOTPAY",
                            onClick = { selectedProvider = "REDOTPAY" },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("select_provider_redotpay")
                        )
                        ProviderChoicePill(
                            title = "Bybit",
                            accent = Color(0xFFF7A600),
                            isSelected = selectedProvider == "BYBIT",
                            onClick = { selectedProvider = "BYBIT" },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("select_provider_bybit")
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProviderChoicePill(
                            title = "Binance",
                            accent = Color(0xFFF0B90B),
                            isSelected = selectedProvider == "BINANCE",
                            onClick = { selectedProvider = "BINANCE" },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("select_provider_binance")
                        )
                        ProviderChoicePill(
                            title = if (isArabic) "افتراضي" else "Default",
                            accent = CyanNeon,
                            isSelected = selectedProvider == "DEFAULT",
                            onClick = { selectedProvider = "DEFAULT" },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("select_provider_default")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Live Mini Official Card Preview
                VisaProviderMiniPreview(
                    provider = selectedProvider,
                    holderName = holderName.trim().ifBlank { "CHARFEDDINE KA" }.uppercase(),
                    cardDigits = cardNumberDigits,
                    expiryDigits = expiryDigits,
                    cvcDigits = cvcDigits
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Holder Name
                VisaDialogFieldBox(
                    label = strings.cardHolderNameLabel,
                    icon = Icons.Default.Person,
                    value = holderName,
                    onValueChange = { holderName = it.take(32) },
                    placeholder = "CHARFEDDINE KA",
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    isError = !isNameValid,
                    testTag = "visa_holder_input"
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Card Number (16 digits)
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    VisaDialogFieldBox(
                        label = strings.cardNumberLabel,
                        icon = Icons.Default.CreditCard,
                        value = cardNumberDigits,
                        onValueChange = { input ->
                            cardNumberDigits = input.filter { it.isDigit() }.take(16)
                        },
                        placeholder = when (selectedProvider) {
                            "BYBIT" -> "5316 00•• •••• 8890"
                            "BINANCE" -> "4895 00•• •••• 2026"
                            else -> "4152 •••• •••• 3120"
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = VisaNumberVisualTransformation(),
                        isError = !isCardNumberValid,
                        helperText = if (!isCardNumberValid) {
                            if (isArabic) "أدخل 16 رقماً أو اتركه فارغاً" else "Enter 16 digits or leave empty"
                        } else null,
                        testTag = "visa_number_input"
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Expiry Date & CVC
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            VisaDialogFieldBox(
                                label = strings.cardExpiryLabel,
                                icon = Icons.Default.Event,
                                value = expiryDigits,
                                onValueChange = { input ->
                                    expiryDigits = input.filter { it.isDigit() }.take(4)
                                },
                                placeholder = "08/28",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                visualTransformation = VisaExpiryVisualTransformation(),
                                isError = !isExpiryValid,
                                testTag = "visa_expiry_input"
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            VisaDialogFieldBox(
                                label = strings.cardCvcLabel,
                                icon = Icons.Default.Lock,
                                value = cvcDigits,
                                onValueChange = { input ->
                                    cvcDigits = input.filter { it.isDigit() }.take(4)
                                },
                                placeholder = "•••",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                isError = !isCvcValid,
                                testTag = "visa_cvc_input"
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassButton(
                        text = strings.cancel,
                        containerColor = colors.textMuted.copy(alpha = 0.25f),
                        contentColor = colors.textPrimary,
                        modifier = Modifier.weight(1f),
                        onClick = onDismiss
                    )
                    GlassButton(
                        text = strings.save,
                        enabled = canSave,
                        containerColor = when (selectedProvider) {
                            "REDOTPAY" -> Color(0xFFE11D48)
                            "BYBIT" -> Color(0xFFF7A600)
                            "BINANCE" -> Color(0xFFF0B90B)
                            else -> CyanNeon
                        },
                        contentColor = if (selectedProvider == "REDOTPAY") Color.White else Color(0xFF0B0E14),
                        modifier = Modifier
                            .weight(1.25f)
                            .testTag("save_visa_card_button"),
                        onClick = {
                            val formattedExpiry = if (expiryDigits.length == 4) {
                                "${expiryDigits.substring(0, 2)}/${expiryDigits.substring(2, 4)}"
                            } else {
                                currentExpiry.ifBlank { "08/28" }
                            }
                            onSave(
                                selectedProvider,
                                holderName.trim().ifBlank { "CHARFEDDINE KA" }.uppercase(),
                                cardNumberDigits,
                                formattedExpiry,
                                cvcDigits
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderChoicePill(
    title: String,
    accent: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LiquidGlassTheme.colors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) accent.copy(alpha = 0.22f) else colors.glassCard)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) accent else colors.glassBorderSubtle,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                color = if (isSelected) colors.textPrimary else colors.textSecondary,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun VisaProviderMiniPreview(
    provider: String,
    holderName: String,
    cardDigits: String,
    expiryDigits: String,
    cvcDigits: String
) {
    val bgBrush = when (provider) {
        "REDOTPAY" -> Brush.linearGradient(
            colors = listOf(
                Color(0xFFE11D48),
                Color(0xFFBE123C),
                Color(0xFF881337),
                Color(0xFF2B0613)
            ),
            start = Offset(0f, 0f),
            end = Offset(700f, 350f)
        )
        "BYBIT" -> Brush.linearGradient(
            colors = listOf(
                Color(0xFF0B0E14),
                Color(0xFF161B26),
                Color(0xFF1F2735),
                Color(0xFF12161F)
            ),
            start = Offset(0f, 0f),
            end = Offset(700f, 350f)
        )
        "BINANCE" -> Brush.linearGradient(
            colors = listOf(
                Color(0xFF0B0E11),
                Color(0xFF181A20),
                Color(0xFF1E2329),
                Color(0xFF0B0E11)
            ),
            start = Offset(0f, 0f),
            end = Offset(700f, 350f)
        )
        else -> Brush.linearGradient(
            colors = listOf(
                Color(0xFF0F172A),
                Color(0xFF1E293B),
                Color(0xFF0F2B48),
                Color(0xFF075985)
            ),
            start = Offset(0f, 0f),
            end = Offset(700f, 350f)
        )
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(bgBrush)
                .border(
                    1.dp,
                    when (provider) {
                        "REDOTPAY" -> Color(0xFFFDA4AF).copy(alpha = 0.45f)
                        "BYBIT" -> Color(0xFFF7A600).copy(alpha = 0.55f)
                        "BINANCE" -> Color(0xFFF0B90B).copy(alpha = 0.60f)
                        else -> Color(0xFF38BDF8).copy(alpha = 0.45f)
                    },
                    RoundedCornerShape(16.dp)
                )
                .drawBehind {
                    if (provider == "REDOTPAY") {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.08f),
                            radius = size.height * 0.65f,
                            center = Offset(size.width * 0.92f, size.height * 0.45f),
                            style = Stroke(width = 18f)
                        )
                    } else if (provider == "BYBIT") {
                        val slash = Path().apply {
                            moveTo(size.width * 0.62f, 0f)
                            lineTo(size.width * 0.78f, 0f)
                            lineTo(size.width * 0.52f, size.height)
                            lineTo(size.width * 0.36f, size.height)
                            close()
                        }
                        drawPath(
                            path = slash,
                            color = Color(0xFFF7A600).copy(alpha = 0.14f)
                        )
                    } else if (provider == "BINANCE") {
                        val cx = size.width * 0.84f
                        val cy = size.height * 0.50f
                        val r = size.height * 0.42f
                        val outerDiamond = Path().apply {
                            moveTo(cx, cy - r)
                            lineTo(cx + r, cy)
                            lineTo(cx, cy + r)
                            lineTo(cx - r, cy)
                            close()
                        }
                        drawPath(
                            path = outerDiamond,
                            color = Color(0xFFF0B90B).copy(alpha = 0.14f),
                            style = Stroke(width = 5f)
                        )
                        val innerDiamond = Path().apply {
                            moveTo(cx, cy - r * 0.46f)
                            lineTo(cx + r * 0.46f, cy)
                            lineTo(cx, cy + r * 0.46f)
                            lineTo(cx - r * 0.46f, cy)
                            close()
                        }
                        drawPath(
                            path = innerDiamond,
                            color = Color(0xFFFCD535).copy(alpha = 0.16f)
                        )
                    }
                }
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (provider) {
                        "REDOTPAY" -> Text(
                            text = "RedotPay",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        "BYBIT" -> Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(color = Color.White)) { append("BYB") }
                                withStyle(SpanStyle(color = Color(0xFFF7A600))) { append("I") }
                                withStyle(SpanStyle(color = Color.White)) { append("T") }
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.4.sp
                        )
                        "BINANCE" -> Text(
                            text = "BINANCE",
                            color = Color(0xFFF0B90B),
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp
                        )
                        else -> Text(
                            text = "Vaultix",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    if (provider == "BYBIT") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEB001B))
                            )
                            Box(
                                modifier = Modifier
                                    .padding(start = 0.dp)
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF79E1B).copy(alpha = 0.88f))
                            )
                        }
                    } else {
                        Text(
                            text = "VISA",
                            color = if (provider == "BINANCE") Color(0xFFFCD535) else Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val previewDigits = if (cardDigits.isEmpty()) {
                    when (provider) {
                        "BYBIT" -> "5316  ••••  ••••  8890"
                        "BINANCE" -> "4895  ••••  ••••  2026"
                        else -> "4152  ••••  ••••  3120"
                    }
                } else {
                    cardDigits.padEnd(16, '•').chunked(4).joinToString("  ")
                }
                Text(
                    text = previewDigits,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.2.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = holderName,
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val expDisplay = when {
                            expiryDigits.length >= 3 -> "${expiryDigits.take(2)}/${expiryDigits.drop(2)}"
                            expiryDigits.isNotEmpty() -> expiryDigits
                            else -> "08/28"
                        }
                        val accentBottomText = when (provider) {
                            "BYBIT" -> Color(0xFFF7A600)
                            "BINANCE" -> Color(0xFFF0B90B)
                            else -> Color.White
                        }
                        Text(
                            text = expDisplay,
                            color = accentBottomText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "CVC ${cvcDigits.ifBlank { "•••" }}",
                            color = accentBottomText.copy(alpha = 0.9f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VisaDialogFieldBox(
    label: String,
    icon: ImageVector,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    isError: Boolean = false,
    helperText: String? = null,
    testTag: String
) {
    val colors = LiquidGlassTheme.colors
    val borderColor = if (isError) RoseNeon else colors.glassBorderSubtle

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.glassCard)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isError) RoseNeon else CyanNeon,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = if (isError) RoseNeon else colors.textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = colors.textMuted.copy(alpha = 0.55f),
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                ),
                keyboardOptions = keyboardOptions,
                visualTransformation = visualTransformation,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(testTag),
                cursorBrush = SolidColor(CyanNeon)
            )
        }
        if (!helperText.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = helperText,
                color = RoseNeon,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private class VisaNumberVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val out = StringBuilder()
        for (i in raw.indices) {
            out.append(raw[i])
            if (i % 4 == 3 && i != raw.lastIndex) out.append(' ')
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

private class VisaExpiryVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val out = StringBuilder()
        for (i in raw.indices) {
            out.append(raw[i])
            if (i == 1 && i != raw.lastIndex) out.append('/')
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
