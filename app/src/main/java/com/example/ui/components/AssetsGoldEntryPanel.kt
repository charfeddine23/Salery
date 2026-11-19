package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.market.GoldAssetHolding
import com.example.market.GoldValuationEngine
import com.example.market.SquareMarketCatalog
import com.example.market.SquareMarketSnapshot
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.formatDzd
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.LocalAdaptiveDimens
import com.example.ui.theme.LocalAdaptiveTypography
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AssetsGoldEntryPanel(
    editingHolding: GoldAssetHolding?,
    squareSnapshot: SquareMarketSnapshot?,
    isRefreshingSquare: Boolean,
    isArabic: Boolean,
    isStealth: Boolean,
    onRequestSyncSquare: () -> Unit,
    onSave: (karat: Int, grams: Double, label: String, existingId: String?) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LiquidGlassTheme.colors
    val strings = AppStrings.current
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current
    val tnumStyle = remember { TextStyle(fontFeatureSettings = "tnum") }

    var gramsText by remember(editingHolding?.id) {
        mutableStateOf(
            if (editingHolding != null && editingHolding.grams > 0.0) {
                String.format(Locale.US, "%.2f", editingHolding.grams).trimEnd('0').trimEnd('.')
            } else ""
        )
    }
    var selectedKarat by remember(editingHolding?.id) {
        mutableIntStateOf(editingHolding?.karat ?: 18)
    }
    var labelText by remember(editingHolding?.id) {
        mutableStateOf(editingHolding?.label ?: "")
    }

    val parsedGrams = gramsText.replace(',', '.').trim().toDoubleOrNull()?.takeIf { it > 0.0 && it.isFinite() } ?: 0.0
    val previewValuation = remember(selectedKarat, parsedGrams, squareSnapshot) {
        GoldValuationEngine.evaluateSingle(
            holding = GoldAssetHolding(karat = selectedKarat, grams = parsedGrams),
            snapshot = squareSnapshot
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimens.cornerRadiusLg))
            .background(colors.glassSurface.copy(alpha = 0.85f))
            .border(1.dp, AmberNeon.copy(alpha = 0.35f), RoundedCornerShape(dimens.cornerRadiusLg))
            .padding(dimens.cardPaddingSmall)
            .testTag("gold_input_panel")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = AmberNeon,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (editingHolding != null) {
                        if (isArabic) "تعديل مخزون الذهب" else "Edit Gold Holding"
                    } else {
                        if (isArabic) "إضافة ذهب وتقدير قيمته" else "Add Gold & Live Value"
                    },
                    color = colors.textPrimary,
                    fontSize = typo.body,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onCancel),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = if (isArabic) "إغلاق" else "Close",
                    tint = colors.textMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // 1. Grams Input + Optional Label
        OutlinedTextField(
            value = gramsText,
            onValueChange = { raw ->
                val filtered = raw.filter { it.isDigit() || it == '.' || it == ',' }
                if (filtered.count { it == '.' || it == ',' } <= 1) {
                    gramsText = filtered
                }
            },
            label = {
                Text(
                    text = if (isArabic) "كمية الذهب بالغرام (مثال: 15.5)" else "Gold Weight in Grams (e.g. 15.5)",
                    fontSize = typo.bodyXs
                )
            },
            suffix = {
                Text(
                    text = if (isArabic) "غ" else "g",
                    color = AmberNeon,
                    fontWeight = FontWeight.Bold
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AmberNeon,
                unfocusedBorderColor = colors.glassBorderSubtle,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary,
                focusedLabelColor = AmberNeon,
                unfocusedLabelColor = colors.textMuted
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("gold_grams_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Karat Selector (24K, 22K, 21K, 18K, 14K, 9K)
        Text(
            text = if (isArabic) "اختر نوع / عيار الذهب:" else "Select Gold Purity / Type:",
            color = colors.textSecondary,
            fontSize = typo.bodyXs,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SquareMarketCatalog.karats.forEach { (karat, purity, _) ->
                val selected = selectedKarat == karat
                val chipTitle = when {
                    karat == 24 && isArabic -> "24K خالص"
                    karat == 18 && isArabic -> "18K محلي"
                    else -> "${karat}K"
                }
                Box(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (selected) AmberNeon.copy(alpha = 0.22f)
                            else colors.glassCard
                        )
                        .border(
                            width = if (selected) 1.4.dp else 1.dp,
                            color = if (selected) AmberNeon else colors.glassBorderSubtle,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { selectedKarat = karat }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                        .testTag("gold_karat_chip_$karat"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = chipTitle,
                            color = if (selected) AmberNeon else colors.textPrimary,
                            fontSize = typo.bodyXs,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = purity,
                            color = colors.textMuted,
                            fontSize = typo.caption
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = labelText,
            onValueChange = { if (it.length <= 40) labelText = it },
            label = {
                Text(
                    text = if (isArabic) "الوصف أو المكان (اختياري: سبيكة، خاتم...)" else "Note / Label (Optional: Bar, Ring...)",
                    fontSize = typo.bodyXs
                )
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.primaryNeon,
                unfocusedBorderColor = colors.glassBorderSubtle,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary,
                focusedLabelColor = colors.primaryNeon,
                unfocusedLabelColor = colors.textMuted
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("gold_label_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Live Instant Valuation Preview synced from Square
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(dimens.cornerRadiusMd))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            AmberNeon.copy(alpha = if (colors.isDark) 0.14f else 0.10f),
                            EmeraldNeon.copy(alpha = if (colors.isDark) 0.10f else 0.06f)
                        )
                    )
                )
                .border(1.dp, AmberNeon.copy(alpha = 0.32f), RoundedCornerShape(dimens.cornerRadiusMd))
                .padding(dimens.cardPaddingSmall)
                .testTag("gold_live_preview_box")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                val perGramDzd = previewValuation.pricePerGramDzdParallel
                val perGramUsd = previewValuation.pricePerGramUsd
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isArabic) "التقييم الفوري حسب Square" else "Live Square Valuation",
                        color = AmberNeon,
                        fontSize = typo.caption,
                        fontWeight = FontWeight.Bold
                    )
                    if (perGramDzd > 0.0) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(
                                text = "1g ${selectedKarat}K ≈ ${formatDzd(perGramDzd, strings.currencyCode, isStealth)}",
                                color = colors.textMuted,
                                fontSize = typo.caption,
                                style = tnumStyle
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                when {
                    squareSnapshot == null || perGramDzd <= 0.0 -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isRefreshingSquare) {
                                    if (isArabic) "جاري مزامنة أسعار الذهب من Square..." else "Syncing Square gold prices..."
                                } else {
                                    if (isArabic) "اضغط للمزامنة لحساب القيمة بالـ DZD" else "Tap sync to fetch live DZD gold rate"
                                },
                                color = colors.textSecondary,
                                fontSize = typo.bodyXs,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 36.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(AmberNeon.copy(alpha = 0.2f))
                                    .clickable(enabled = !isRefreshingSquare, onClick = onRequestSyncSquare)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = AmberNeon,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isArabic) "مزامنة" else "Sync",
                                        color = AmberNeon,
                                        fontSize = typo.caption,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    parsedGrams <= 0.0 -> {
                        Text(
                            text = if (isArabic) "أدخل الوزن بالغرام لتظهر القيمة الإجمالية مباشرة"
                            else "Enter weight in grams to preview total worth",
                            color = colors.textMuted,
                            fontSize = typo.bodyXs
                        )
                    }
                    else -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = if (isArabic) "يساوي حالياً:" else "Worth Today:",
                                    color = colors.textSecondary,
                                    fontSize = typo.caption
                                )
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Text(
                                        text = formatDzd(previewValuation.totalValueDzdParallel, strings.currencyCode, isStealth),
                                        color = EmeraldNeon,
                                        fontSize = typo.titleSm,
                                        fontWeight = FontWeight.ExtraBold,
                                        style = tnumStyle
                                    )
                                }
                            }
                            if (perGramUsd > 0.0) {
                                val usdStr = if (isStealth) "•••• USD"
                                else String.format(Locale.US, "$%,.2f", previewValuation.totalValueUsd)
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Text(
                                        text = "≈ $usdStr",
                                        color = CyanNeon,
                                        fontSize = typo.bodySm,
                                        fontWeight = FontWeight.Bold,
                                        style = tnumStyle
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Save Action Button (min 48dp height)
        val canSave = parsedGrams > 0.0
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (canSave) {
                        Brush.horizontalGradient(listOf(AmberNeon, EmeraldNeon))
                    } else {
                        Brush.horizontalGradient(listOf(colors.glassCard, colors.glassCard))
                    }
                )
                .border(
                    width = 1.dp,
                    color = if (canSave) Color.Transparent else colors.glassBorderSubtle,
                    shape = RoundedCornerShape(14.dp)
                )
                .clickable(enabled = canSave) {
                    onSave(selectedKarat, parsedGrams, labelText, editingHolding?.id)
                }
                .testTag("gold_save_button"),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = if (canSave) Color(0xFF0A0F1A) else colors.textMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (editingHolding != null) {
                        if (isArabic) "تحديث رصيد الذهب" else "Update Gold Holding"
                    } else {
                        if (isArabic) "حفظ الذهب في الأصول" else "Save Gold Asset"
                    },
                    color = if (canSave) Color(0xFF0A0F1A) else colors.textMuted,
                    fontSize = typo.body,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}
