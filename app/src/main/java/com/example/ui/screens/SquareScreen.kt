package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.market.SquareMarketCodec
import com.example.market.SquareMarketSnapshot
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.SquareCategoryTabStrip
import com.example.ui.components.SquareExchangeTickerRow
import com.example.ui.components.SquareGoldAccent
import com.example.ui.components.SquarePrimaryTab
import com.example.ui.components.SquareQuickConverterCard
import com.example.ui.components.SquareSortColumn
import com.example.ui.components.SquareSortDirection
import com.example.ui.components.SquareSyncIntervalSettingsDialog
import com.example.ui.components.SquareTableHeaderRow
import com.example.ui.components.SquareTickerRowModel
import com.example.ui.components.SquareTopAnchoredPullToRefresh
import com.example.ui.components.SquareVenueFilter
import com.example.ui.components.SquareVenueSubFilterStrip
import com.example.ui.components.buildSquareTickerRows
import com.example.ui.components.formatSquareIntervalLabel
import com.example.ui.components.sortSquareTickerRows
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.ArabicStrings
import com.example.ui.i18n.isolateLtr
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.LocalAdaptiveDimens
import com.example.ui.theme.LocalAdaptiveLayoutInfo
import com.example.ui.theme.LocalAdaptiveTypography
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SquareScreen(
    snapshot: SquareMarketSnapshot?,
    lastSyncMillis: Long,
    syncIntervalMinutes: Int = 60,
    pinnedIdsJson: String = SquareMarketCodec.DEFAULT_PINNED_JSON,
    isRefreshing: Boolean,
    errorMessage: String?,
    onRefresh: (force: Boolean) -> Unit,
    onUpdateSyncIntervalMinutes: (Int) -> Unit = {},
    onTogglePin: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors
    val isArabic = strings is ArabicStrings
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current
    val layoutInfo = LocalAdaptiveLayoutInfo.current

    var showSyncSettingsDialog by remember { mutableStateOf(false) }

    // Initial check & continuous configurable auto-refresh timer while viewing Square
    LaunchedEffect(Unit) {
        onRefresh(false)
    }

    LaunchedEffect(lastSyncMillis, syncIntervalMinutes) {
        if (syncIntervalMinutes <= 0) return@LaunchedEffect
        val intervalMillis = syncIntervalMinutes.toLong() * 60_000L
        while (isActive) {
            val now = System.currentTimeMillis()
            val waitMillis = if (lastSyncMillis <= 0L) {
                intervalMillis
            } else {
                val elapsed = now - lastSyncMillis
                if (elapsed >= intervalMillis) 1_000L
                else (intervalMillis - elapsed).coerceAtLeast(1_000L)
            }
            delay(waitMillis)
            onRefresh(false)
        }
    }

    var primaryTab by remember { mutableStateOf(SquarePrimaryTab.ALL) }
    var venueFilter by remember { mutableStateOf(SquareVenueFilter.ALL) }
    var sortColumn by remember { mutableStateOf(SquareSortColumn.DEFAULT) }
    var sortDirection by remember { mutableStateOf(SquareSortDirection.NONE) }
    var searchExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var expandedRowId by remember { mutableStateOf<String?>(null) }

    // Converter state
    var converterIsGold by remember { mutableStateOf(true) }
    var converterInput by remember { mutableStateOf("10") }
    var selectedKarat by remember { mutableIntStateOf(18) }
    var selectedCurrencyCode by remember { mutableStateOf("EUR") }

    val pinnedIds = remember(pinnedIdsJson) {
        SquareMarketCodec.parsePinnedIds(pinnedIdsJson)
    }

    val effectiveLastSync = remember(lastSyncMillis, snapshot) {
        maxOf(lastSyncMillis, snapshot?.updatedAtMillis ?: 0L)
    }
    val lastSyncLabel = remember(effectiveLastSync, isArabic) {
        if (effectiveLastSync > 0L) {
            isolateLtr(
                SimpleDateFormat("dd/MM • HH:mm", if (isArabic) Locale("ar") else Locale.US)
                    .format(Date(effectiveLastSync))
            )
        } else {
            if (isArabic) "بانتظار أول مزامنة" else "Pending initial sync"
        }
    }
    val remainingMins = remember(effectiveLastSync, syncIntervalMinutes) {
        if (effectiveLastSync <= 0L || syncIntervalMinutes <= 0) 0L
        else {
            val intervalMillis = syncIntervalMinutes.toLong() * 60_000L
            ((effectiveLastSync + intervalMillis - System.currentTimeMillis()) / 60_000L)
                .coerceAtLeast(0L)
        }
    }

    val effectiveTabForTable = if (layoutInfo.columns >= 2 && primaryTab == SquarePrimaryTab.CALC) {
        SquarePrimaryTab.ALL
    } else {
        primaryTab
    }

    val tickerRows = remember(snapshot, effectiveTabForTable, venueFilter, pinnedIds, searchQuery, sortColumn, sortDirection) {
        if (snapshot == null || effectiveTabForTable == SquarePrimaryTab.CALC) {
            emptyList()
        } else {
            val base = buildSquareTickerRows(
                snapshot = snapshot,
                primaryTab = effectiveTabForTable,
                venueFilter = venueFilter,
                pinnedIds = pinnedIds,
                searchQuery = searchQuery
            )
            sortSquareTickerRows(
                rows = base,
                sortColumn = sortColumn,
                sortDirection = sortDirection,
                pinnedIds = pinnedIds
            )
        }
    }

    val onToggleSortColumn: (SquareSortColumn) -> Unit = { clickedCol ->
        if (sortColumn != clickedCol) {
            sortColumn = clickedCol
            sortDirection = if (clickedCol == SquareSortColumn.NAME) {
                SquareSortDirection.ASC
            } else {
                SquareSortDirection.DESC
            }
        } else {
            when (sortDirection) {
                SquareSortDirection.DESC -> sortDirection = SquareSortDirection.ASC
                SquareSortDirection.ASC -> {
                    sortColumn = SquareSortColumn.DEFAULT
                    sortDirection = SquareSortDirection.NONE
                }
                SquareSortDirection.NONE -> sortDirection = SquareSortDirection.DESC
            }
        }
    }

    val sourcesSummary = remember(snapshot, isArabic) {
        if (snapshot == null) ""
        else listOfNotNull(
            snapshot.goldSource.takeIf { it.isNotBlank() }?.let {
                (if (isArabic) "الذهب: " else "Gold: ") + if (isArabic) localizeSquareSource(it) else it
            },
            snapshot.parallelSource.takeIf { it.isNotBlank() }?.let {
                (if (isArabic) "سكوار: " else "Square: ") + if (isArabic) localizeSquareSource(it) else it
            },
            snapshot.bankSource.takeIf { it.isNotBlank() }?.let {
                (if (isArabic) "البنك: " else "Bank: ") + if (isArabic) localizeSquareSource(it) else it
            }
        ).joinToString(" • ")
    }

    val intervalLabel = remember(syncIntervalMinutes, isArabic) {
        formatSquareIntervalLabel(syncIntervalMinutes, isArabic)
    }
    val syncStatusFooter = remember(effectiveLastSync, lastSyncLabel, intervalLabel, remainingMins, syncIntervalMinutes, isArabic) {
        when {
            effectiveLastSync <= 0L -> lastSyncLabel
            syncIntervalMinutes <= 0 -> "$intervalLabel • $lastSyncLabel"
            else -> if (isArabic) {
                "$intervalLabel • $lastSyncLabel (بعد ${remainingMins}د)"
            } else {
                "$intervalLabel • $lastSyncLabel (${remainingMins}m left)"
            }
        }
    }

    @Composable
    fun InlineSyncErrorNotice() {
        if (errorMessage != null && snapshot != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = AmberNeon,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = errorMessage,
                    color = AmberNeon,
                    fontSize = typo.bodyXs
                )
            }
        }
    }

    @Composable
    fun ConverterSection() {
        if (snapshot == null) return
        SquareQuickConverterCard(
            snapshot = snapshot,
            isGoldMode = converterIsGold,
            onModeChange = { converterIsGold = it },
            amountInput = converterInput,
            onAmountChange = { converterInput = it },
            selectedKarat = selectedKarat,
            onSelectKarat = { selectedKarat = it },
            selectedCurrencyCode = selectedCurrencyCode,
            onSelectCurrency = { selectedCurrencyCode = it },
            isArabic = isArabic,
            colors = colors
        )
    }

    @Composable
    fun TickerSheetSection() {
        if (tickerRows.isEmpty()) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = colors.glassCard,
                shape = RoundedCornerShape(dimens.cornerRadiusLg),
                cornerRadius = dimens.cornerRadiusLg
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(dimens.cardPaddingLarge),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(dimens.iconSizeXl)
                    )
                    Spacer(modifier = Modifier.height(dimens.spacingSm))
                    Text(
                        text = if (effectiveTabForTable == SquarePrimaryTab.FAVORITES) {
                            if (isArabic) "لا توجد عناصر مثبتة في المفضلة بعد — اضغط على أي سطر لتثبيته" else "No favorites pinned yet — tap any row to pin"
                        } else {
                            if (isArabic) "لا توجد نتائج مطابقة للبحث" else "No matching gold or currency tickers"
                        },
                        color = colors.textSecondary,
                        fontSize = typo.body,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = colors.glassCard,
                borderColor = colors.glassBorderSubtle,
                shape = RoundedCornerShape(dimens.cornerRadiusLg),
                cornerRadius = dimens.cornerRadiusLg,
                breathingGlow = false
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    tickerRows.forEachIndexed { index, row ->
                        SquareExchangeTickerRow(
                            row = row,
                            venueFilter = venueFilter,
                            isPinned = row.id in pinnedIds,
                            isExpanded = expandedRowId == row.id,
                            isArabic = isArabic,
                            colors = colors,
                            onRowClick = {
                                expandedRowId = if (expandedRowId == row.id) null else row.id
                            },
                            onTogglePin = { onTogglePin(row.id) },
                            onOpenInCalculator = {
                                when (row) {
                                    is SquareTickerRowModel.SpotGold -> {
                                        converterIsGold = true
                                        selectedKarat = 24
                                    }
                                    is SquareTickerRowModel.GoldKarat -> {
                                        converterIsGold = true
                                        selectedKarat = row.item.karat
                                    }
                                    is SquareTickerRowModel.CurrencyFx -> {
                                        converterIsGold = false
                                        selectedCurrencyCode = row.rate.code
                                    }
                                }
                                if (layoutInfo.columns < 2) {
                                    primaryTab = SquarePrimaryTab.CALC
                                }
                            }
                        )
                        if (index < tickerRows.lastIndex) {
                            HorizontalDivider(
                                color = colors.glassBorderSubtle.copy(alpha = 0.42f),
                                thickness = 0.6.dp,
                                modifier = Modifier.padding(horizontal = dimens.cardPaddingSmall)
                            )
                        }
                    }
                }
            }
        }
    }

    SquareTopAnchoredPullToRefresh(
        isRefreshing = isRefreshing,
        isArabic = isArabic,
        onRefresh = { onRefresh(true) },
        onOpenSyncSettings = { showSyncSettingsDialog = true },
        modifier = modifier
    ) {
        if (snapshot == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = dimens.screenPaddingH),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = dimens.contentMaxWidth),
                    verticalArrangement = Arrangement.spacedBy(dimens.spacingMd)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(dimens.spacingXs))
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = colors.glassCard,
                            shape = RoundedCornerShape(dimens.cornerRadiusLg),
                            cornerRadius = dimens.cornerRadiusLg
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(dimens.cardPaddingLarge),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = if (isRefreshing) Icons.Default.AutoGraph else Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = if (isRefreshing) SquareGoldAccent else AmberNeon,
                                    modifier = Modifier.size(dimens.iconSizeXl)
                                )
                                Spacer(modifier = Modifier.height(dimens.spacingSm))
                                Text(
                                    text = when {
                                        isRefreshing -> if (isArabic) "جاري جلب أسعار الذهب والعملات المباشرة..." else "Fetching live gold & FX market rates..."
                                        errorMessage != null -> errorMessage
                                        else -> if (isArabic) "لا توجد بيانات سوق مخزنة بعد" else "No market data cached yet"
                                    },
                                    color = colors.textPrimary,
                                    fontSize = typo.bodyLg,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(dimens.spacingMd))
                                GlassButton(
                                    text = if (isArabic) "إعادة المحاولة" else "Retry Sync",
                                    onClick = { onRefresh(true) },
                                    modifier = Modifier.testTag("square_retry_button")
                                )
                            }
                        }
                    }
                }
            }
        } else if (layoutInfo.columns >= 2) {
            // Adaptive 2-column Tablet Layout: Live Market Table + Always-Docked Converter
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = dimens.screenPaddingH),
                horizontalArrangement = Arrangement.spacedBy(dimens.spacingLg)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1.22f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(dimens.spacingSm)
                ) {
                    item {
                        InlineSyncErrorNotice()
                        SquareCategoryTabStrip(
                            selectedTab = primaryTab,
                            favoritesCount = pinnedIds.size,
                            onSelectTab = { nextTab ->
                                primaryTab = nextTab
                                expandedRowId = null
                            },
                            isArabic = isArabic,
                            colors = colors
                        )
                    }

                    item {
                        SquareVenueSubFilterStrip(
                            selectedVenue = venueFilter,
                            onSelectVenue = { venueFilter = it },
                            searchExpanded = searchExpanded,
                            onToggleSearch = {
                                searchExpanded = !searchExpanded
                                if (!searchExpanded) searchQuery = ""
                            },
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            isArabic = isArabic,
                            colors = colors
                        )
                    }

                    item {
                        SquareTableHeaderRow(
                            sortColumn = sortColumn,
                            sortDirection = sortDirection,
                            selectedVenue = venueFilter,
                            onToggleSort = onToggleSortColumn,
                            isArabic = isArabic,
                            colors = colors
                        )
                    }

                    item { TickerSheetSection() }

                    item { Spacer(modifier = Modifier.height(dimens.spacingXxl)) }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(0.78f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(dimens.spacingMd)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(dimens.spacingXs))
                        ConverterSection()
                    }

                    if (sourcesSummary.isNotBlank() || syncStatusFooter.isNotBlank()) {
                        item {
                            val combinedInfo = listOf(sourcesSummary, syncStatusFooter)
                                .filter { it.isNotBlank() }
                                .joinToString(" • ")
                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showSyncSettingsDialog = true },
                                backgroundColor = colors.glassCard,
                                shape = RoundedCornerShape(dimens.cornerRadiusMd),
                                cornerRadius = dimens.cornerRadiusMd,
                                breathingGlow = false
                            ) {
                                Text(
                                    text = combinedInfo,
                                    color = EmeraldNeon.copy(alpha = 0.88f),
                                    fontSize = typo.bodyXs,
                                    modifier = Modifier.padding(dimens.cardPaddingSmall)
                                )
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(dimens.spacingXxl)) }
                }
            }
        } else {
            // Single-column smooth vertical flow for Phones
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = dimens.screenPaddingH),
                verticalArrangement = Arrangement.spacedBy(dimens.spacingSm)
            ) {
                item {
                    InlineSyncErrorNotice()
                    SquareCategoryTabStrip(
                        selectedTab = primaryTab,
                        favoritesCount = pinnedIds.size,
                        onSelectTab = { nextTab ->
                            primaryTab = nextTab
                            expandedRowId = null
                        },
                        isArabic = isArabic,
                        colors = colors
                    )
                }

                if (primaryTab == SquarePrimaryTab.CALC) {
                    item { ConverterSection() }
                } else {
                    item {
                        SquareVenueSubFilterStrip(
                            selectedVenue = venueFilter,
                            onSelectVenue = { venueFilter = it },
                            searchExpanded = searchExpanded,
                            onToggleSearch = {
                                searchExpanded = !searchExpanded
                                if (!searchExpanded) searchQuery = ""
                            },
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            isArabic = isArabic,
                            colors = colors
                        )
                    }

                    item {
                        SquareTableHeaderRow(
                            sortColumn = sortColumn,
                            sortDirection = sortDirection,
                            selectedVenue = venueFilter,
                            onToggleSort = onToggleSortColumn,
                            isArabic = isArabic,
                            colors = colors
                        )
                    }

                    item { TickerSheetSection() }

                    if (sourcesSummary.isNotBlank() || syncStatusFooter.isNotBlank()) {
                        item {
                            val footerLine = buildString {
                                if (sourcesSummary.isNotBlank()) {
                                    append(
                                        if (isArabic) "تجميع موثق من عدة مواقع • $sourcesSummary"
                                        else "Multi-source verified feed • $sourcesSummary"
                                    )
                                }
                                if (syncStatusFooter.isNotBlank()) {
                                    if (isNotEmpty()) append(" • ")
                                    append(syncStatusFooter)
                                }
                            }
                            Text(
                                text = footerLine,
                                color = EmeraldNeon.copy(alpha = 0.85f),
                                fontSize = typo.bodyXs,
                                modifier = Modifier
                                    .clickable { showSyncSettingsDialog = true }
                                    .padding(horizontal = 4.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(dimens.spacingXxl)) }
            }
        }
    }

    if (showSyncSettingsDialog) {
        SquareSyncIntervalSettingsDialog(
            currentIntervalMinutes = syncIntervalMinutes,
            lastSyncLabel = lastSyncLabel,
            isArabic = isArabic,
            onSaveInterval = onUpdateSyncIntervalMinutes,
            onSyncNow = { onRefresh(true) },
            onDismiss = { showSyncSettingsDialog = false }
        )
    }
}

private fun localizeSquareSource(source: String): String {
    return source
        .replace("Yahoo Finance", "ياهو المالية", ignoreCase = true)
        .replace("Binance", "بينانس", ignoreCase = true)
        .replace("CoinGecko", "كوين غيكو", ignoreCase = true)
        .replace("ExchangeRate-API", "أسعار الصرف", ignoreCase = true)
        .replace("Official Bank", "البنك الرسمي", ignoreCase = true)
        .replace("Parallel", "الموازي", ignoreCase = true)
        .replace("Square", "سكوار", ignoreCase = true)
        .replace("sites", "مواقع", ignoreCase = true)
}
