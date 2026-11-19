package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.example.data.UserSettings
import com.example.market.GoldAssetHolding
import com.example.market.GoldPortfolioSummary
import com.example.market.GoldValuationEngine
import com.example.market.SquareMarketSnapshot
import com.example.ui.components.AssetsGoldEntryPanel
import com.example.ui.components.CryptoReadOnlySummaryCard
import com.example.ui.components.GlassCard
import com.example.ui.components.GoldHoldingRowItem
import com.example.ui.components.formatAssetsSyncTimeCompact
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.LocalStealthMode
import com.example.ui.i18n.formatDzd
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.LocalAdaptiveDimens
import com.example.ui.theme.LocalAdaptiveLayoutInfo
import com.example.ui.theme.LocalAdaptiveTypography
import java.util.Locale

@Composable
fun AssetsManagementScreen(
    userSettings: UserSettings,
    goldHoldings: List<GoldAssetHolding>,
    squareSnapshot: SquareMarketSnapshot?,
    binanceHoldings: List<CryptoHoldingEntry>,
    binancePrices: Map<String, CryptoMarketPriceEntry>,
    isRefreshingSquare: Boolean,
    isRefreshingCrypto: Boolean,
    onBackClick: () -> Unit,
    onSyncPricesNow: (Boolean) -> Unit,
    onUpsertGoldHolding: (Int, Double, String, String?) -> Unit,
    onRemoveGoldHolding: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)

    val isArabic = userSettings.language == "ar"
    val isStealth = LocalStealthMode.current
    val layoutInfo = LocalAdaptiveLayoutInfo.current
    val dimens = LocalAdaptiveDimens.current

    LaunchedEffect(Unit) {
        onSyncPricesNow(false)
    }

    val goldSummary = remember(goldHoldings, squareSnapshot) {
        GoldValuationEngine.evaluatePortfolio(goldHoldings, squareSnapshot)
    }
    val cryptoTotalUsd = remember(binanceHoldings, binancePrices) {
        BinanceCryptoCatalog.calculatePortfolioTotalUsd(binanceHoldings, binancePrices)
    }
    val cryptoDzdApprox = remember(cryptoTotalUsd, squareSnapshot) {
        GoldValuationEngine.convertUsdToSquareParallelDzd(cryptoTotalUsd, squareSnapshot) ?: 0.0
    }
    val combinedAssetsDzd = goldSummary.totalValueDzdParallel + cryptoDzdApprox
    val isSyncingAny = isRefreshingSquare || isRefreshingCrypto

    var showGoldEditor by remember { mutableStateOf(goldHoldings.isEmpty()) }
    var editingGoldHolding by remember { mutableStateOf<GoldAssetHolding?>(null) }

    LaunchedEffect(goldHoldings.size) {
        if (goldHoldings.isEmpty()) {
            showGoldEditor = true
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (layoutInfo.columns >= 2) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = dimens.screenPaddingH),
                horizontalArrangement = Arrangement.spacedBy(dimens.spacingLg)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(dimens.spacingMd)
                ) {
                    item { Spacer(modifier = Modifier.height(dimens.spacingXs)) }
                    item {
                        AssetsSummaryHeaderCard(
                            combinedAssetsDzd = combinedAssetsDzd,
                            goldValueDzd = goldSummary.totalValueDzdParallel,
                            cryptoValueDzd = cryptoDzdApprox,
                            squareLastSyncMillis = userSettings.squareLastPriceSyncMillis,
                            isSyncingAny = isSyncingAny,
                            isArabic = isArabic,
                            isStealth = isStealth,
                            onSyncNow = { onSyncPricesNow(true) }
                        )
                    }
                    item {
                        CryptoReadOnlySummaryCard(
                            totalUsd = cryptoTotalUsd,
                            approxDzd = cryptoDzdApprox.takeIf { it > 0.0 },
                            holdingsCount = binanceHoldings.size,
                            lastSyncMillis = userSettings.binanceLastPriceSyncMillis,
                            isArabic = isArabic,
                            isStealth = isStealth
                        )
                    }
                    item { Spacer(modifier = Modifier.height(dimens.spacingXl)) }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(dimens.spacingMd)
                ) {
                    item { Spacer(modifier = Modifier.height(dimens.spacingXs)) }
                    item {
                        GoldVaultManagementCard(
                            goldSummary = goldSummary,
                            squareSnapshot = squareSnapshot,
                            isRefreshingSquare = isRefreshingSquare,
                            isArabic = isArabic,
                            isStealth = isStealth,
                            showGoldEditor = showGoldEditor,
                            editingGoldHolding = editingGoldHolding,
                            onToggleEditorOpen = {
                                if (showGoldEditor && editingGoldHolding == null) {
                                    showGoldEditor = false
                                } else {
                                    editingGoldHolding = null
                                    showGoldEditor = true
                                }
                            },
                            onRequestSyncSquare = { onSyncPricesNow(true) },
                            onSaveGold = { karat, grams, label, existingId ->
                                onUpsertGoldHolding(karat, grams, label, existingId)
                                editingGoldHolding = null
                                showGoldEditor = false
                            },
                            onCancelEditor = {
                                editingGoldHolding = null
                                showGoldEditor = false
                            },
                            onEditItem = { holding ->
                                editingGoldHolding = holding
                                showGoldEditor = true
                            },
                            onDeleteItem = { id ->
                                if (editingGoldHolding?.id == id) editingGoldHolding = null
                                onRemoveGoldHolding(id)
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(dimens.spacingXl)) }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = dimens.screenPaddingH),
                verticalArrangement = Arrangement.spacedBy(dimens.spacingMd)
            ) {
                item { Spacer(modifier = Modifier.height(dimens.spacingXs)) }

                item {
                    AssetsSummaryHeaderCard(
                        combinedAssetsDzd = combinedAssetsDzd,
                        goldValueDzd = goldSummary.totalValueDzdParallel,
                        cryptoValueDzd = cryptoDzdApprox,
                        squareLastSyncMillis = userSettings.squareLastPriceSyncMillis,
                        isSyncingAny = isSyncingAny,
                        isArabic = isArabic,
                        isStealth = isStealth,
                        onSyncNow = { onSyncPricesNow(true) }
                    )
                }

                item {
                    GoldVaultManagementCard(
                        goldSummary = goldSummary,
                        squareSnapshot = squareSnapshot,
                        isRefreshingSquare = isRefreshingSquare,
                        isArabic = isArabic,
                        isStealth = isStealth,
                        showGoldEditor = showGoldEditor,
                        editingGoldHolding = editingGoldHolding,
                        onToggleEditorOpen = {
                            if (showGoldEditor && editingGoldHolding == null) {
                                showGoldEditor = false
                            } else {
                                editingGoldHolding = null
                                showGoldEditor = true
                            }
                        },
                        onRequestSyncSquare = { onSyncPricesNow(true) },
                        onSaveGold = { karat, grams, label, existingId ->
                            onUpsertGoldHolding(karat, grams, label, existingId)
                            editingGoldHolding = null
                            showGoldEditor = false
                        },
                        onCancelEditor = {
                            editingGoldHolding = null
                            showGoldEditor = false
                        },
                        onEditItem = { holding ->
                            editingGoldHolding = holding
                            showGoldEditor = true
                        },
                        onDeleteItem = { id ->
                            if (editingGoldHolding?.id == id) editingGoldHolding = null
                            onRemoveGoldHolding(id)
                        }
                    )
                }

                item {
                    CryptoReadOnlySummaryCard(
                        totalUsd = cryptoTotalUsd,
                        approxDzd = cryptoDzdApprox.takeIf { it > 0.0 },
                        holdingsCount = binanceHoldings.size,
                        lastSyncMillis = userSettings.binanceLastPriceSyncMillis,
                        isArabic = isArabic,
                        isStealth = isStealth
                    )
                }

                item { Spacer(modifier = Modifier.height(dimens.spacingXl)) }
            }
        }
    }
}

@Composable
private fun AssetsSummaryHeaderCard(
    combinedAssetsDzd: Double,
    goldValueDzd: Double,
    cryptoValueDzd: Double,
    squareLastSyncMillis: Long,
    isSyncingAny: Boolean,
    isArabic: Boolean,
    isStealth: Boolean,
    onSyncNow: () -> Unit
) {
    val colors = LiquidGlassTheme.colors
    val strings = AppStrings.current
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current
    val tnumStyle = remember { TextStyle(fontFeatureSettings = "tnum") }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("assets_vault_header_card"),
        shape = RoundedCornerShape(dimens.cornerRadiusXl),
        backgroundColor = colors.glassCard,
        cornerRadius = dimens.cornerRadiusXl,
        borderColor = AmberNeon.copy(alpha = 0.42f)
    ) {
        Column(modifier = Modifier.padding(dimens.cardPaddingLarge)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(AmberNeon.copy(alpha = 0.25f), CyanNeon.copy(alpha = 0.22f))
                                )
                            )
                            .border(1.dp, AmberNeon.copy(alpha = 0.45f), RoundedCornerShape(13.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = AmberNeon,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(dimens.spacingSm))
                    Column {
                        Text(
                            text = if (isArabic) "إجمالي قيمة الأصول" else "Total Assets Value",
                            color = colors.textMuted,
                            fontSize = typo.bodySm,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                        Text(
                            text = if (isArabic) "آخر تحديث: ${formatAssetsSyncTimeCompact(squareLastSyncMillis, true)}"
                            else "Synced: ${formatAssetsSyncTimeCompact(squareLastSyncMillis, false)}",
                            color = colors.textMuted,
                            fontSize = typo.caption,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(EmeraldNeon.copy(alpha = 0.16f))
                        .border(1.dp, EmeraldNeon.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                        .clickable(enabled = !isSyncingAny, onClick = onSyncNow)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("assets_sync_refresh_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isSyncingAny) {
                            CircularProgressIndicator(
                                color = EmeraldNeon,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = if (isArabic) "مزامنة الأسعار" else "Sync Prices",
                                tint = EmeraldNeon,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isArabic) "مزامنة" else "Sync",
                            color = EmeraldNeon,
                            fontSize = typo.bodyXs,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(dimens.spacingMd))

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(
                    text = formatDzd(combinedAssetsDzd, strings.currencyCode, isStealth),
                    color = AmberNeon,
                    fontSize = typo.hero,
                    fontWeight = FontWeight.ExtraBold,
                    style = tnumStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(dimens.spacingSm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimens.spacingSm)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(dimens.cornerRadiusMd))
                        .background(colors.glassSurface)
                        .border(1.dp, AmberNeon.copy(alpha = 0.25f), RoundedCornerShape(dimens.cornerRadiusMd))
                        .padding(dimens.cardPaddingSmall)
                ) {
                    Text(
                        text = if (isArabic) "الذهب (Square)" else "Gold (Square)",
                        color = colors.textMuted,
                        fontSize = typo.caption
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(
                            text = formatDzd(goldValueDzd, strings.currencyCode, isStealth),
                            color = AmberNeon,
                            fontSize = typo.bodySm,
                            fontWeight = FontWeight.Bold,
                            style = tnumStyle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(dimens.cornerRadiusMd))
                        .background(colors.glassSurface)
                        .border(1.dp, CyanNeon.copy(alpha = 0.25f), RoundedCornerShape(dimens.cornerRadiusMd))
                        .padding(dimens.cardPaddingSmall)
                ) {
                    Text(
                        text = if (isArabic) "الكريبتو (Square eq)" else "Crypto (Square eq)",
                        color = colors.textMuted,
                        fontSize = typo.caption
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(
                            text = formatDzd(cryptoValueDzd, strings.currencyCode, isStealth),
                            color = CyanNeon,
                            fontSize = typo.bodySm,
                            fontWeight = FontWeight.Bold,
                            style = tnumStyle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GoldVaultManagementCard(
    goldSummary: GoldPortfolioSummary,
    squareSnapshot: SquareMarketSnapshot?,
    isRefreshingSquare: Boolean,
    isArabic: Boolean,
    isStealth: Boolean,
    showGoldEditor: Boolean,
    editingGoldHolding: GoldAssetHolding?,
    onToggleEditorOpen: () -> Unit,
    onRequestSyncSquare: () -> Unit,
    onSaveGold: (karat: Int, grams: Double, label: String, existingId: String?) -> Unit,
    onCancelEditor: () -> Unit,
    onEditItem: (GoldAssetHolding) -> Unit,
    onDeleteItem: (String) -> Unit
) {
    val colors = LiquidGlassTheme.colors
    val strings = AppStrings.current
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current
    val tnumStyle = remember { TextStyle(fontFeatureSettings = "tnum") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimens.cornerRadiusXl))
            .background(colors.glassCard)
            .border(1.dp, AmberNeon.copy(alpha = 0.36f), RoundedCornerShape(dimens.cornerRadiusXl))
            .padding(dimens.cardPadding)
            .testTag("assets_gold_storage_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(AmberNeon.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = AmberNeon,
                        modifier = Modifier.size(21.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (isArabic) "خزنة الذهب (Square)" else "Gold Vault (Square)",
                        color = colors.textPrimary,
                        fontSize = typo.titleSm,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isArabic) "أدخل وزن الذهب ونوع العيار لحساب قيمته"
                        else "Add gold weight & karat for live Square worth",
                        color = colors.textMuted,
                        fontSize = typo.caption,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(AmberNeon.copy(alpha = 0.18f))
                    .border(1.dp, AmberNeon.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                    .clickable(onClick = onToggleEditorOpen)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("gold_add_open_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = if (isArabic) "إضافة ذهب" else "Add Gold",
                        tint = AmberNeon,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isArabic) "إضافة ذهب" else "Add Gold",
                        color = AmberNeon,
                        fontSize = typo.bodyXs,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showGoldEditor,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(12.dp))
                AssetsGoldEntryPanel(
                    editingHolding = editingGoldHolding,
                    squareSnapshot = squareSnapshot,
                    isRefreshingSquare = isRefreshingSquare,
                    isArabic = isArabic,
                    isStealth = isStealth,
                    onRequestSyncSquare = onRequestSyncSquare,
                    onSave = onSaveGold,
                    onCancel = onCancelEditor
                )
            }
        }

        if (goldSummary.items.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                goldSummary.items.forEachIndexed { index, valuationItem ->
                    GoldHoldingRowItem(
                        item = valuationItem,
                        isArabic = isArabic,
                        isStealth = isStealth,
                        onEdit = { onEditItem(valuationItem.holding) },
                        onDelete = { onDeleteItem(valuationItem.holding.id) }
                    )
                    if (index < goldSummary.items.lastIndex) {
                        HorizontalDivider(color = colors.glassBorderSubtle, thickness = 0.6.dp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = AmberNeon.copy(alpha = 0.25f), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(10.dp))

            val totalGramsFmt = String.format(Locale.US, "%.2f", goldSummary.totalGrams)
                .trimEnd('0').trimEnd('.')

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isArabic) "إجمالي الذهب ($totalGramsFmt غ)" else "Total Gold (${totalGramsFmt}g)",
                        color = colors.textSecondary,
                        fontSize = typo.caption,
                        fontWeight = FontWeight.SemiBold
                    )
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(
                            text = if (goldSummary.hasLivePrices) {
                                formatDzd(goldSummary.totalValueDzdParallel, strings.currencyCode, isStealth)
                            } else {
                                if (isArabic) "بانتظار مزامنة الأسعار" else "Awaiting price sync"
                            },
                            color = AmberNeon,
                            fontSize = typo.title,
                            fontWeight = FontWeight.ExtraBold,
                            style = tnumStyle
                        )
                    }
                }

                if (goldSummary.totalValueUsd > 0.0) {
                    val usdFormatted = if (isStealth) "•••• USD"
                    else String.format(Locale.US, "$%,.2f", goldSummary.totalValueUsd)
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(
                            text = "≈ $usdFormatted",
                            color = EmeraldNeon,
                            fontSize = typo.bodySm,
                            fontWeight = FontWeight.Bold,
                            style = tnumStyle
                        )
                    }
                }
            }
        } else if (!showGoldEditor) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (isArabic) "لم تضف أي ذهب بعد. اضغط على (إضافة ذهب) لإدخال الوزن والعيار."
                else "No gold entries yet. Tap Add Gold to enter weight & karat.",
                color = colors.textMuted,
                fontSize = typo.bodyXs
            )
        }
    }
}
