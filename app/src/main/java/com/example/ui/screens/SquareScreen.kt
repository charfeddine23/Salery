package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.sp
import com.example.market.SQUARE_REFRESH_INTERVAL_MILLIS
import com.example.market.SquareMarketCodec
import com.example.market.SquareMarketSnapshot
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.SquareCategoryTabStrip
import com.example.ui.components.SquareCompactMarketHeader
import com.example.ui.components.SquareExchangeTickerRow
import com.example.ui.components.SquareGoldAccent
import com.example.ui.components.SquarePrimaryTab
import com.example.ui.components.SquareQuickConverterCard
import com.example.ui.components.SquareSortColumn
import com.example.ui.components.SquareSortDirection
import com.example.ui.components.SquareTableHeaderRow
import com.example.ui.components.SquareTickerRowModel
import com.example.ui.components.SquareVenueFilter
import com.example.ui.components.SquareVenueSubFilterStrip
import com.example.ui.components.buildSquareTickerRows
import com.example.ui.components.sortSquareTickerRows
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.ArabicStrings
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.LocalAdaptiveDimens
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SquareScreen(
    snapshot: SquareMarketSnapshot?,
    lastSyncMillis: Long,
    pinnedIdsJson: String = SquareMarketCodec.DEFAULT_PINNED_JSON,
    isRefreshing: Boolean,
    errorMessage: String?,
    onRefresh: (force: Boolean) -> Unit,
    onTogglePin: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors
    val isArabic = strings is ArabicStrings
    val dimens = LocalAdaptiveDimens.current

    // Initial check & continuous 1-hour auto-refresh timer while viewing Square
    LaunchedEffect(Unit) {
        onRefresh(false)
    }

    LaunchedEffect(lastSyncMillis) {
        while (isActive) {
            val now = System.currentTimeMillis()
            val waitMillis = if (lastSyncMillis <= 0L) {
                SQUARE_REFRESH_INTERVAL_MILLIS
            } else {
                val elapsed = now - lastSyncMillis
                if (elapsed >= SQUARE_REFRESH_INTERVAL_MILLIS) 1_000L
                else (SQUARE_REFRESH_INTERVAL_MILLIS - elapsed).coerceAtLeast(1_000L)
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
    var refreshSpins by remember { mutableIntStateOf(0) }

    val pinnedIds = remember(pinnedIdsJson) {
        SquareMarketCodec.parsePinnedIds(pinnedIdsJson)
    }

    val effectiveLastSync = remember(lastSyncMillis, snapshot) {
        maxOf(lastSyncMillis, snapshot?.updatedAtMillis ?: 0L)
    }
    val lastSyncLabel = remember(effectiveLastSync, isArabic) {
        if (effectiveLastSync > 0L) {
            SimpleDateFormat("dd/MM • HH:mm", Locale.getDefault()).format(Date(effectiveLastSync))
        } else {
            if (isArabic) "بانتظار أول مزامنة" else "Pending initial sync"
        }
    }
    val remainingMins = remember(effectiveLastSync) {
        if (effectiveLastSync <= 0L) 0L
        else ((effectiveLastSync + SQUARE_REFRESH_INTERVAL_MILLIS - System.currentTimeMillis()) / 60_000L)
            .coerceAtLeast(0L)
    }

    val tickerRows = remember(snapshot, primaryTab, venueFilter, pinnedIds, searchQuery, sortColumn, sortDirection) {
        if (snapshot == null || primaryTab == SquarePrimaryTab.CALC) {
            emptyList()
        } else {
            val base = buildSquareTickerRows(
                snapshot = snapshot,
                primaryTab = primaryTab,
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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = dimens.screenPaddingH),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Compact Liquid Glass Market Header
        item {
            Spacer(modifier = Modifier.height(dimens.spacingXs))
            SquareCompactMarketHeader(
                snapshot = snapshot,
                lastSyncLabel = lastSyncLabel,
                remainingMins = remainingMins,
                effectiveLastSync = effectiveLastSync,
                isRefreshing = isRefreshing,
                errorMessage = errorMessage,
                refreshSpins = refreshSpins,
                onRefreshClick = {
                    refreshSpins++
                    onRefresh(true)
                },
                isArabic = isArabic,
                colors = colors
            )
        }

        // Empty / Error State when no cached snapshot exists
        if (snapshot == null) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = colors.glassCard,
                    shape = RoundedCornerShape(dimens.cornerRadiusLg),
                    cornerRadius = dimens.cornerRadiusLg
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (isRefreshing) Icons.Default.AutoGraph else Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = if (isRefreshing) SquareGoldAccent else AmberNeon,
                            modifier = Modifier.size(38.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = when {
                                isRefreshing -> if (isArabic) "جاري جلب أسعار الذهب والعملات المباشرة..." else "Fetching live gold & FX market rates..."
                                errorMessage != null -> errorMessage
                                else -> if (isArabic) "لا توجد بيانات سوق مخزنة بعد" else "No market data cached yet"
                            },
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        GlassButton(
                            text = if (isArabic) "إعادة المحاولة" else "Retry Sync",
                            onClick = { onRefresh(true) },
                            modifier = Modifier.testTag("square_retry_button")
                        )
                    }
                }
            }
            return@LazyColumn
        }

        // 2. Primary Binance-style Category Tabs (All / Favorites / Gold / FX / Converter)
        item {
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

        // 3. Converter View OR Exchange Market Table View
        if (primaryTab == SquarePrimaryTab.CALC) {
            item {
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
        } else {
            // Secondary Venue Filter Strip + Expandable Search
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

            // Sortable 3-Column Table Header
            item {
                SquareTableHeaderRow(
                    sortColumn = sortColumn,
                    sortDirection = sortDirection,
                    selectedVenue = venueFilter,
                    onToggleSort = { clickedCol ->
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
                    },
                    isArabic = isArabic,
                    colors = colors
                )
            }

            // Ticker Sheet (GlassCard container with hairline dividers between rows)
            if (tickerRows.isEmpty()) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = colors.glassCard,
                        shape = RoundedCornerShape(18.dp),
                        cornerRadius = 18.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = colors.textMuted,
                                modifier = Modifier.size(34.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (primaryTab == SquarePrimaryTab.FAVORITES) {
                                    if (isArabic) "لا توجد عناصر مثبتة في المفضلة بعد — اضغط على أي سطر لتثبيته" else "No favorites pinned yet — tap any row to pin"
                                } else {
                                    if (isArabic) "لا توجد نتائج مطابقة للبحث" else "No matching gold or currency tickers"
                                },
                                color = colors.textSecondary,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = colors.glassCard,
                        borderColor = colors.glassBorderSubtle,
                        shape = RoundedCornerShape(18.dp),
                        cornerRadius = 18.dp,
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
                                        primaryTab = SquarePrimaryTab.CALC
                                    }
                                )
                                if (index < tickerRows.lastIndex) {
                                    HorizontalDivider(
                                        color = colors.glassBorderSubtle.copy(alpha = 0.42f),
                                        thickness = 0.6.dp,
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Discreet multi-site consensus footer
            val sourcesSummary = listOfNotNull(
                snapshot.goldSource.takeIf { it.isNotBlank() }?.let { (if (isArabic) "الذهب: " else "Gold: ") + it },
                snapshot.parallelSource.takeIf { it.isNotBlank() }?.let { (if (isArabic) "سكوار: " else "Square: ") + it },
                snapshot.bankSource.takeIf { it.isNotBlank() }?.let { (if (isArabic) "البنك: " else "Bank: ") + it }
            ).joinToString(" • ")

            if (sourcesSummary.isNotBlank()) {
                item {
                    Text(
                        text = if (isArabic) {
                            "تجميع موثق من عدة مواقع • $sourcesSummary"
                        } else {
                            "Multi-source verified feed • $sourcesSummary"
                        },
                        color = EmeraldNeon.copy(alpha = 0.85f),
                        fontSize = 10.5.sp,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(dimens.spacingXxl)) }
    }
}
