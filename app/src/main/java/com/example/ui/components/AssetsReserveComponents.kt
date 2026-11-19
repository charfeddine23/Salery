package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.crypto.BinanceCryptoCatalog
import com.example.crypto.CryptoHoldingEntry
import com.example.crypto.CryptoMarketPriceEntry
import com.example.market.GoldAssetHolding
import com.example.market.GoldValuationEngine
import com.example.market.GoldValuationItem
import com.example.market.SquareMarketSnapshot
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.formatDzd
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.LocalAdaptiveDimens
import com.example.ui.theme.LocalAdaptiveTypography
import com.example.ui.theme.RoseNeon
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AssetsReserveSection(
    isEnabled: Boolean,
    cashSavingsBalance: Double,
    goldHoldings: List<GoldAssetHolding>,
    squareSnapshot: SquareMarketSnapshot?,
    squareLastSyncMillis: Long,
    binanceHoldings: List<CryptoHoldingEntry>,
    binancePrices: Map<String, CryptoMarketPriceEntry>,
    binanceLastSyncMillis: Long,
    isRefreshingSquare: Boolean,
    isRefreshingCrypto: Boolean,
    isArabic: Boolean,
    isStealth: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onSyncNow: () -> Unit,
    onUpsertGoldHolding: (karat: Int, grams: Double, label: String, existingId: String?) -> Unit,
    onRemoveGoldHolding: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LiquidGlassTheme.colors
    val strings = AppStrings.current
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current
    val tnumStyle = remember { TextStyle(fontFeatureSettings = "tnum") }

    val goldSummary = remember(goldHoldings, squareSnapshot) {
        GoldValuationEngine.evaluatePortfolio(goldHoldings, squareSnapshot)
    }
    val cryptoTotalUsd = remember(binanceHoldings, binancePrices) {
        BinanceCryptoCatalog.calculatePortfolioTotalUsd(binanceHoldings, binancePrices)
    }
    val cryptoDzdApprox = remember(cryptoTotalUsd, squareSnapshot) {
        GoldValuationEngine.convertUsdToSquareParallelDzd(cryptoTotalUsd, squareSnapshot)
    }

    var showGoldEditor by remember(goldHoldings.isEmpty()) { mutableStateOf(goldHoldings.isEmpty()) }
    var editingGoldItem by remember { mutableStateOf<GoldAssetHolding?>(null) }

    val isSyncingAny = isRefreshingSquare || isRefreshingCrypto

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("assets_reserve_section"),
        shape = RoundedCornerShape(dimens.cornerRadiusXl),
        backgroundColor = colors.glassCard,
        cornerRadius = dimens.cornerRadiusXl,
        borderColor = if (isEnabled) AmberNeon.copy(alpha = 0.36f) else colors.glassBorderSubtle
    ) {
        Column(modifier = Modifier.padding(dimens.cardPadding)) {
            // Header Row: Title + Auto-Sync Toggle + Refresh Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(AmberNeon.copy(alpha = 0.24f), CyanNeon.copy(alpha = 0.20f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = AmberNeon,
                        modifier = Modifier.size(21.dp)
                    )
                }

                Spacer(modifier = Modifier.width(dimens.spacingSm))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isArabic) "الأصول والذهب والكريبتو" else "Assets: Gold & Crypto",
                        color = colors.textPrimary,
                        fontSize = typo.titleSm,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isEnabled) {
                            if (isArabic) "مزامنة تلقائية للأسعار من Square و Binance"
                            else "Auto-synced live from Square & Binance"
                        } else {
                            if (isArabic) "فعّل الميزة لإضافة الذهب وعرض إجمالي الكريبتو"
                            else "Enable to store Gold & view synced Crypto"
                        },
                        color = if (isEnabled) EmeraldNeon else colors.textMuted,
                        fontSize = typo.caption,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isEnabled) {
                    IconButton(
                        onClick = onSyncNow,
                        enabled = !isSyncingAny,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("assets_sync_refresh_button")
                    ) {
                        if (isSyncingAny) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = CyanNeon
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = if (isArabic) "تحديث أسعار السوق" else "Refresh Market Prices",
                                tint = CyanNeon,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggleEnabled,
                    modifier = Modifier.testTag("assets_sync_toggle_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AmberNeon,
                        checkedTrackColor = AmberNeon.copy(alpha = 0.35f),
                        uncheckedThumbColor = colors.textMuted,
                        uncheckedTrackColor = colors.glassSurface
                    )
                )
            }

            AnimatedVisibility(
                visible = isEnabled,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(dimens.spacingMd))

                    // Combined Wealth Breakdown Banner
                    val combinedAssetsDzd = cashSavingsBalance + goldSummary.totalValueDzdParallel + (cryptoDzdApprox ?: 0.0)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(dimens.cornerRadiusMd))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        AmberNeon.copy(alpha = if (colors.isDark) 0.14f else 0.09f),
                                        CyanNeon.copy(alpha = if (colors.isDark) 0.12f else 0.08f)
                                    )
                                )
                            )
                            .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(dimens.cornerRadiusMd))
                            .padding(dimens.cardPaddingSmall)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = if (isArabic) "إجمالي الثروة (الادخار + الأصول المفعّلة)"
                                else "Combined Reserve (Savings + Synced Assets)",
                                color = colors.textSecondary,
                                fontSize = typo.caption,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Text(
                                    text = formatDzd(combinedAssetsDzd, strings.currencyCode, isStealth),
                                    color = colors.textPrimary,
                                    fontSize = typo.title,
                                    fontWeight = FontWeight.ExtraBold,
                                    style = tnumStyle,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(dimens.spacingMd))

                    // 1. BINANCE CRYPTO SUMMARY — STRICTLY READ-ONLY (للعرض فقط)
                    CryptoReadOnlySummaryCard(
                        totalUsd = cryptoTotalUsd,
                        approxDzd = cryptoDzdApprox,
                        holdingsCount = binanceHoldings.size,
                        lastSyncMillis = binanceLastSyncMillis,
                        isArabic = isArabic,
                        isStealth = isStealth
                    )

                    Spacer(modifier = Modifier.height(dimens.spacingMd))

                    // 2. GOLD VAULT — INTERACTIVE INPUTS & LIVE SQUARE PRICING
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(dimens.cornerRadiusLg))
                            .background(colors.glassSurface.copy(alpha = 0.72f))
                            .border(1.dp, AmberNeon.copy(alpha = 0.28f), RoundedCornerShape(dimens.cornerRadiusLg))
                            .padding(dimens.cardPaddingSmall)
                            .testTag("assets_gold_storage_card")
                    ) {
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
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(AmberNeon.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WorkspacePremium,
                                        contentDescription = null,
                                        tint = AmberNeon,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (isArabic) "مخزون الذهب • Square" else "Gold Reserve • Square",
                                        color = colors.textPrimary,
                                        fontSize = typo.body,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val syncStr = formatSyncTimeCompact(squareLastSyncMillis, isArabic)
                                    Text(
                                        text = if (isArabic) "مزامنة تلقائية للأسعار • $syncStr"
                                        else "Auto-priced from Square • $syncStr",
                                        color = colors.textMuted,
                                        fontSize = typo.caption,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 40.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(AmberNeon.copy(alpha = 0.18f))
                                    .border(1.dp, AmberNeon.copy(alpha = 0.45f), RoundedCornerShape(50))
                                    .clickable {
                                        editingGoldItem = null
                                        showGoldEditor = !showGoldEditor
                                    }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                                    .testTag("gold_add_open_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = AmberNeon,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isArabic) "إضافة ذهب" else "Add Gold",
                                        color = AmberNeon,
                                        fontSize = typo.caption,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (showGoldEditor) {
                            Spacer(modifier = Modifier.height(dimens.spacingSm))
                            AssetsGoldEntryPanel(
                                editingHolding = editingGoldItem,
                                squareSnapshot = squareSnapshot,
                                isRefreshingSquare = isRefreshingSquare,
                                isArabic = isArabic,
                                isStealth = isStealth,
                                onRequestSyncSquare = onSyncNow,
                                onSave = { karat, grams, label, existingId ->
                                    onUpsertGoldHolding(karat, grams, label, existingId)
                                    editingGoldItem = null
                                    showGoldEditor = false
                                },
                                onCancel = {
                                    editingGoldItem = null
                                    showGoldEditor = false
                                }
                            )
                        }

                        if (goldSummary.items.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(dimens.spacingSm))
                            goldSummary.items.forEachIndexed { index, item ->
                                GoldHoldingRowItem(
                                    item = item,
                                    isArabic = isArabic,
                                    isStealth = isStealth,
                                    onEdit = {
                                        editingGoldItem = item.holding
                                        showGoldEditor = true
                                    },
                                    onDelete = { onRemoveGoldHolding(item.holding.id) }
                                )
                                if (index < goldSummary.items.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        color = colors.glassBorderSubtle.copy(alpha = 0.55f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Gold Portfolio Total Strip
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(dimens.cornerRadiusMd))
                                    .background(AmberNeon.copy(alpha = if (colors.isDark) 0.14f else 0.10f))
                                    .border(1.dp, AmberNeon.copy(alpha = 0.30f), RoundedCornerShape(dimens.cornerRadiusMd))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (isArabic) "إجمالي الذهب المخزّن" else "Total Stored Gold",
                                        color = colors.textSecondary,
                                        fontSize = typo.caption
                                    )
                                    val gramsFmt = String.format(Locale.US, "%.2f", goldSummary.totalGrams)
                                        .trimEnd('0').trimEnd('.')
                                    Text(
                                        text = if (isArabic) "$gramsFmt غرام" else "${gramsFmt}g total",
                                        color = AmberNeon,
                                        fontSize = typo.bodySm,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                        Text(
                                            text = if (goldSummary.hasLivePrices) {
                                                formatDzd(goldSummary.totalValueDzdParallel, strings.currencyCode, isStealth)
                                            } else {
                                                "— ${strings.currencyCode}"
                                            },
                                            color = EmeraldNeon,
                                            fontSize = typo.titleSm,
                                            fontWeight = FontWeight.ExtraBold,
                                            style = tnumStyle
                                        )
                                        if (goldSummary.hasLivePrices && goldSummary.totalValueUsd > 0.0) {
                                            val usdText = if (isStealth) "•••• USD"
                                            else String.format(Locale.US, "$%,.2f", goldSummary.totalValueUsd)
                                            Text(
                                                text = "≈ $usdText",
                                                color = CyanNeon,
                                                fontSize = typo.caption,
                                                fontWeight = FontWeight.SemiBold,
                                                style = tnumStyle
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CryptoReadOnlySummaryCard(
    totalUsd: Double,
    approxDzd: Double?,
    holdingsCount: Int,
    lastSyncMillis: Long,
    isArabic: Boolean,
    isStealth: Boolean
) {
    val colors = LiquidGlassTheme.colors
    val strings = AppStrings.current
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current
    val tnumStyle = remember { TextStyle(fontFeatureSettings = "tnum") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimens.cornerRadiusLg))
            .background(colors.glassSurface.copy(alpha = 0.72f))
            .border(1.dp, CyanNeon.copy(alpha = 0.30f), RoundedCornerShape(dimens.cornerRadiusLg))
            .padding(dimens.cardPaddingSmall)
            .testTag("assets_crypto_readonly_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyanNeon.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CurrencyBitcoin,
                        contentDescription = null,
                        tint = CyanNeon,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (isArabic) "إجمالي العملات الرقمية" else "Binance Crypto Total",
                        color = colors.textPrimary,
                        fontSize = typo.body,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatSyncTimeCompact(lastSyncMillis, isArabic),
                        color = colors.textMuted,
                        fontSize = typo.caption
                    )
                }
            }

            // Strictly Display-Only Badge (للعرض فقط)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(CyanNeon.copy(alpha = 0.14f))
                    .border(1.dp, CyanNeon.copy(alpha = 0.35f), RoundedCornerShape(50))
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    tint = CyanNeon,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isArabic) "للعرض فقط" else "Display Only",
                    color = CyanNeon,
                    fontSize = typo.caption,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    val formattedUsd = if (isStealth) "$ ••••••"
                    else String.format(Locale.US, "$%,.2f", totalUsd)
                    Text(
                        text = formattedUsd,
                        color = CyanNeon,
                        fontSize = typo.titleLg,
                        fontWeight = FontWeight.ExtraBold,
                        style = tnumStyle
                    )
                }
                if (approxDzd != null && approxDzd > 0.0) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(
                            text = "≈ ${formatDzd(approxDzd, strings.currencyCode, isStealth)}",
                            color = EmeraldNeon,
                            fontSize = typo.bodySm,
                            fontWeight = FontWeight.Bold,
                            style = tnumStyle
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.glassCard)
                    .border(0.8.dp, colors.glassBorderSubtle, RoundedCornerShape(10.dp))
                    .padding(horizontal = 9.dp, vertical = 5.dp)
            ) {
                Text(
                    text = if (isArabic) "$holdingsCount عملات في Binance"
                    else "$holdingsCount Binance assets",
                    color = colors.textSecondary,
                    fontSize = typo.caption,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun GoldHoldingRowItem(
    item: GoldValuationItem,
    isArabic: Boolean,
    isStealth: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LiquidGlassTheme.colors
    val strings = AppStrings.current
    val typo = LocalAdaptiveTypography.current
    val tnumStyle = remember { TextStyle(fontFeatureSettings = "tnum") }

    val h = item.holding
    val gramsStr = String.format(Locale.US, "%.2f", h.grams).trimEnd('0').trimEnd('.')

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("gold_holding_item_${h.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Karat Badge
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(AmberNeon.copy(alpha = 0.16f))
                .border(1.dp, AmberNeon.copy(alpha = 0.42f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${h.karat}K",
                color = AmberNeon,
                fontSize = typo.bodySm,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isArabic) "$gramsStr غ • عيار ${h.karat}" else "${gramsStr}g • ${h.karat}K Gold",
                    color = colors.textPrimary,
                    fontSize = typo.bodySm,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (h.label.isNotBlank()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• ${h.label}",
                        color = colors.textMuted,
                        fontSize = typo.caption,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (item.pricePerGramDzdParallel > 0.0) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(
                        text = "${formatDzd(item.totalValueDzdParallel, strings.currencyCode, isStealth)}  (1g ≈ ${formatDzd(item.pricePerGramDzdParallel, strings.currencyCode, isStealth)})",
                        color = EmeraldNeon,
                        fontSize = typo.bodyXs,
                        fontWeight = FontWeight.Bold,
                        style = tnumStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Text(
                    text = if (isArabic) "بانتظار مزامنة سعر Square..." else "Waiting for Square rate sync...",
                    color = colors.textMuted,
                    fontSize = typo.caption
                )
            }
        }

        IconButton(
            onClick = onEdit,
            modifier = Modifier
                .size(48.dp)
                .testTag("edit_gold_button_${h.id}")
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = if (isArabic) "تعديل" else "Edit Gold",
                tint = colors.primaryNeon,
                modifier = Modifier.size(18.dp)
            )
        }
        IconButton(
            onClick = onDelete,
            modifier = Modifier
                .size(48.dp)
                .testTag("delete_gold_button_${h.id}")
        ) {
            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = if (isArabic) "حذف" else "Delete Gold",
                tint = RoseNeon,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun formatSyncTimeCompact(millis: Long, isArabic: Boolean): String {
    if (millis <= 0L) {
        return if (isArabic) "لم تتم المزامنة بعد" else "Not synced yet"
    }
    val fmt = SimpleDateFormat("dd MMM HH:mm", if (isArabic) Locale("ar") else Locale.US)
    return fmt.format(Date(millis))
}
