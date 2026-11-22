package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.market.CurrencySquareRate
import com.example.market.GoldKaratPrice
import com.example.market.SquareMarketCatalog
import com.example.market.SquareMarketSnapshot
import com.example.market.TROY_OUNCE_IN_GRAMS
import com.example.ui.i18n.isolateLtr
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassColors
import com.example.ui.theme.LocalAdaptiveDimens
import com.example.ui.theme.LocalAdaptiveLayoutInfo
import com.example.ui.theme.LocalAdaptiveTypography
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

enum class SquarePrimaryTab {
    ALL,
    FAVORITES,
    GOLD,
    CURRENCIES,
    CALC
}

enum class SquareVenueFilter {
    ALL,
    SQUARE,
    BANK,
    USD
}

enum class SquareSortColumn {
    DEFAULT,
    NAME,
    PRICE,
    CHANGE
}

enum class SquareSortDirection {
    NONE,
    DESC,
    ASC
}

sealed interface SquareTickerRowModel {
    val id: String
    val sortName: String
    val primaryNumericPrice: Double
    val changeOrSpreadValue: Double
    val testTagId: String

    data class SpotGold(
        override val id: String = "XAU_SPOT",
        val goldOzUsd: Double,
        val change24hPct: Double,
        val previousGoldOzUsd: Double = 0.0,
        val hasBaseline: Boolean = false,
        val usdPerGram24K: Double,
        val dzdPerGram24KParallel: Double,
        val dzdPerGram24KBank: Double,
        val sourceName: String,
        val sourceCount: Int,
        override val primaryNumericPrice: Double,
        override val changeOrSpreadValue: Double,
        override val sortName: String = "XAU",
        override val testTagId: String = "square_spot_xau"
    ) : SquareTickerRowModel

    data class GoldKarat(
        override val id: String,
        val item: GoldKaratPrice,
        val change24hPct: Double,
        val previousPrice: Double = 0.0,
        val hasBaseline: Boolean = false,
        val sourceName: String,
        override val primaryNumericPrice: Double,
        override val changeOrSpreadValue: Double,
        override val sortName: String,
        override val testTagId: String = "square_karat_${item.karat}"
    ) : SquareTickerRowModel

    data class CurrencyFx(
        override val id: String,
        val rate: CurrencySquareRate,
        val spreadPct: Double,
        val spreadDzd: Double,
        val change24hPct: Double = 0.0,
        val previousPriceDzd: Double = 0.0,
        val hasBaseline: Boolean = false,
        val accentColor: Color,
        override val primaryNumericPrice: Double,
        override val changeOrSpreadValue: Double,
        override val sortName: String = rate.code,
        override val testTagId: String = "square_currency_${rate.code}"
    ) : SquareTickerRowModel
}

val SquareGoldAccent = Color(0xFFF5B700)
private val SquareGoldDeep = Color(0xFFD97706)
private val TickerGreenPill = Color(0xFF2EBD85)
private val TickerRedPill = Color(0xFFF6465D)
private val TickerNeutralPill = Color(0xFF475569)
private val PinHighlightColor = Color(0xFFF97316)

@Composable
fun SquareCategoryTabStrip(
    selectedTab: SquarePrimaryTab,
    favoritesCount: Int,
    onSelectTab: (SquarePrimaryTab) -> Unit,
    isArabic: Boolean,
    colors: LiquidGlassColors
) {
    val tabs = listOf(
        Triple(SquarePrimaryTab.ALL, if (isArabic) "الكل" else "All", "square_filter_all"),
        Triple(
            SquarePrimaryTab.FAVORITES,
            if (isArabic) "المفضلة ($favoritesCount)" else "Favorites ($favoritesCount)",
            "square_filter_favorites"
        ),
        Triple(SquarePrimaryTab.GOLD, if (isArabic) "الذهب" else "Gold", "square_filter_gold"),
        Triple(SquarePrimaryTab.CURRENCIES, if (isArabic) "العملات" else "Currencies", "square_filter_currencies"),
        Triple(SquarePrimaryTab.CALC, if (isArabic) "الحاسبة" else "Converter", "square_filter_calc")
    )

    val typo = LocalAdaptiveTypography.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { (tab, label, tag) ->
                val isSelected = selectedTab == tab
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = SquareGoldAccent),
                            onClick = { onSelectTab(tab) }
                        )
                        .padding(horizontal = 4.dp, vertical = 8.dp)
                        .testTag(tag)
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) colors.textPrimary else colors.textMuted,
                        fontSize = typo.bodyLg,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) SquareGoldAccent else Color.Transparent)
                    )
                }
            }
        }
        HorizontalDivider(
            color = colors.glassBorderSubtle.copy(alpha = 0.45f),
            thickness = 0.8.dp
        )
    }
}

@Composable
fun SquareVenueSubFilterStrip(
    selectedVenue: SquareVenueFilter,
    onSelectVenue: (SquareVenueFilter) -> Unit,
    searchExpanded: Boolean,
    onToggleSearch: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isArabic: Boolean,
    colors: LiquidGlassColors
) {
    val venues = listOf(
        SquareVenueFilter.ALL to if (isArabic) "الكل" else "All",
        SquareVenueFilter.SQUARE to if (isArabic) "سكوار" else "Square",
        SquareVenueFilter.BANK to if (isArabic) "البنك" else "Bank",
        SquareVenueFilter.USD to if (isArabic) "دولار $" else "USD"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                venues.forEach { (venue, label) ->
                    val active = venue == selectedVenue
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(color = colors.primaryNeon),
                                onClick = { onSelectVenue(venue) }
                            )
                            .padding(horizontal = 4.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val typo = LocalAdaptiveTypography.current
                        Text(
                            text = label,
                            color = if (active) colors.textPrimary else colors.textMuted,
                            fontSize = typo.body,
                            fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }

            IconButton(
                onClick = onToggleSearch,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("square_search_toggle")
            ) {
                Icon(
                    imageVector = if (searchExpanded) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = if (isArabic) "بحث" else "Search",
                    tint = if (searchExpanded || searchQuery.isNotBlank()) SquareGoldAccent else colors.textSecondary,
                    modifier = Modifier.size(19.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = searchExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.glassSurface)
                    .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("square_currency_search")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = if (isArabic) "ابحث عن عيار أو عملة (عيار 18، يورو، دولار...)" else "Filter gold or currency (18K, EUR, USD...)",
                                color = colors.textMuted,
                                fontSize = 12.sp
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            singleLine = true,
                            textStyle = TextStyle(
                                color = colors.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(SquareGoldAccent),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = if (isArabic) "مسح البحث" else "Clear search",
                            tint = colors.textMuted,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { onSearchQueryChange("") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SquareTableHeaderRow(
    sortColumn: SquareSortColumn,
    sortDirection: SquareSortDirection,
    selectedVenue: SquareVenueFilter,
    onToggleSort: (SquareSortColumn) -> Unit,
    isArabic: Boolean,
    colors: LiquidGlassColors
) {
    val nameLabel = if (isArabic) "الأصل / المصدر" else "Name / Source"
    val priceLabel = when (selectedVenue) {
        SquareVenueFilter.BANK -> if (isArabic) "سعر البنك" else "Bank Price"
        SquareVenueFilter.USD -> if (isArabic) "السعر $" else "Price USD"
        else -> if (isArabic) "آخر سعر" else "Last Price"
    }
    val changeLabel = if (isArabic) "التغير (24سا)" else "Change(%)"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SortableHeaderCell(
            text = nameLabel,
            active = sortColumn == SquareSortColumn.NAME,
            direction = if (sortColumn == SquareSortColumn.NAME) sortDirection else SquareSortDirection.NONE,
            alignment = Alignment.CenterStart,
            modifier = Modifier.weight(1.42f),
            colors = colors,
            onClick = { onToggleSort(SquareSortColumn.NAME) }
        )
        SortableHeaderCell(
            text = priceLabel,
            active = sortColumn == SquareSortColumn.PRICE,
            direction = if (sortColumn == SquareSortColumn.PRICE) sortDirection else SquareSortDirection.NONE,
            alignment = Alignment.CenterEnd,
            modifier = Modifier.weight(1.05f),
            colors = colors,
            onClick = { onToggleSort(SquareSortColumn.PRICE) }
        )
        Spacer(modifier = Modifier.width(8.dp))
        SortableHeaderCell(
            text = changeLabel,
            active = sortColumn == SquareSortColumn.CHANGE,
            direction = if (sortColumn == SquareSortColumn.CHANGE) sortDirection else SquareSortDirection.NONE,
            alignment = Alignment.CenterEnd,
            modifier = Modifier.widthIn(min = 72.dp, max = 94.dp),
            colors = colors,
            onClick = { onToggleSort(SquareSortColumn.CHANGE) }
        )
    }
}

@Composable
private fun SortableHeaderCell(
    text: String,
    active: Boolean,
    direction: SquareSortDirection,
    alignment: Alignment,
    modifier: Modifier,
    colors: LiquidGlassColors,
    onClick: () -> Unit
) {
    val typo = LocalAdaptiveTypography.current
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 36.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        contentAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = text,
                color = if (active) SquareGoldAccent else colors.textMuted,
                fontSize = typo.bodyXs,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            SortTrianglesGlyph(
                activeDirection = if (active) direction else SquareSortDirection.NONE,
                activeColor = SquareGoldAccent,
                idleColor = colors.textMuted.copy(alpha = 0.65f)
            )
        }
    }
}

@Composable
private fun SortTrianglesGlyph(
    activeDirection: SquareSortDirection,
    activeColor: Color,
    idleColor: Color
) {
    Canvas(modifier = Modifier.size(width = 7.dp, height = 11.dp)) {
        val w = size.width
        val h = size.height
        val upColor = if (activeDirection == SquareSortDirection.ASC) activeColor else idleColor
        val downColor = if (activeDirection == SquareSortDirection.DESC) activeColor else idleColor

        val upPath = Path().apply {
            moveTo(w / 2f, 0.5f)
            lineTo(w, h * 0.40f)
            lineTo(0f, h * 0.40f)
            close()
        }
        drawPath(path = upPath, color = upColor)

        val downPath = Path().apply {
            moveTo(0f, h * 0.60f)
            lineTo(w, h * 0.60f)
            lineTo(w / 2f, h - 0.5f)
            close()
        }
        drawPath(path = downPath, color = downColor)
    }
}

@Composable
fun SquareExchangeTickerRow(
    row: SquareTickerRowModel,
    venueFilter: SquareVenueFilter,
    isPinned: Boolean,
    isExpanded: Boolean,
    isArabic: Boolean,
    colors: LiquidGlassColors,
    onRowClick: () -> Unit,
    onTogglePin: () -> Unit,
    onOpenInCalculator: () -> Unit
) {
    val typo = LocalAdaptiveTypography.current
    val layoutInfo = LocalAdaptiveLayoutInfo.current
    val tnumStyle = TextStyle(fontFeatureSettings = "tnum")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = SquareGoldAccent),
                onClick = onRowClick
            )
            .padding(horizontal = if (layoutInfo.isCompactPhone) 10.dp else 12.dp, vertical = 11.dp)
            .testTag(row.testTagId)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Icon + Symbol / Quote / Tag + Pin & Subtitle
            Row(
                modifier = Modifier.weight(1.42f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TickerLeadingEmblem(row = row, isArabic = isArabic, colors = colors)
                Spacer(modifier = Modifier.width(if (layoutInfo.isCompactPhone) 8.dp else 10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val (baseText, quoteText, tagText) = when (row) {
                            is SquareTickerRowModel.SpotGold -> Triple(
                                if (isArabic) "ذهب" else "XAU",
                                if (isArabic) "/ دولار" else "/USD",
                                if (isArabic) "فوري" else "Spot"
                            )
                            is SquareTickerRowModel.GoldKarat -> Triple(
                                if (isArabic) "عيار ${row.item.karat}" else "${row.item.karat}K",
                                if (isArabic) {
                                    if (venueFilter == SquareVenueFilter.USD) "/ دولار" else "/ دج"
                                } else {
                                    if (venueFilter == SquareVenueFilter.USD) "/USD" else "/DZD"
                                },
                                when (venueFilter) {
                                    SquareVenueFilter.BANK -> if (isArabic) "بنك" else "Bank"
                                    SquareVenueFilter.USD -> if (isArabic) "1 غ" else "1g"
                                    else -> if (isArabic) "سكوار" else "Square"
                                }
                            )
                            is SquareTickerRowModel.CurrencyFx -> Triple(
                                if (isArabic) row.rate.nameAr else row.rate.code,
                                if (isArabic) "" else "/DZD",
                                when {
                                    venueFilter == SquareVenueFilter.BANK -> if (isArabic) "بنك" else "Bank"
                                    row.rate.parallelSellDzd > 0.0 -> if (isArabic) "سكوار" else "Square"
                                    else -> if (isArabic) "بنك" else "Bank"
                                }
                            )
                        }

                        Text(
                            text = baseText,
                            color = colors.textPrimary,
                            fontSize = typo.bodyLg,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (quoteText.isNotEmpty()) {
                            Text(
                                text = quoteText,
                                color = colors.textMuted,
                                fontSize = typo.bodyXs,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors.glassSurface.copy(alpha = 0.9f))
                                .border(0.6.dp, colors.glassBorderSubtle, RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 1.5.dp)
                        ) {
                            Text(
                                text = tagText,
                                color = colors.textSecondary,
                                fontSize = typo.caption,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isPinned) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = if (isArabic) "مثبت" else "Pinned",
                                tint = PinHighlightColor,
                                modifier = Modifier
                                    .size(11.dp)
                                    .rotate(35f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        val subtitle = when (row) {
                            is SquareTickerRowModel.SpotGold -> {
                                val label = if (isArabic) "أونصة ذهب" else "Gold Oz"
                                val src = if (isArabic) {
                                    row.sourceName.ifBlank { "سوق عالمي" }
                                        .replace("Yahoo Finance", "ياهو المالية", ignoreCase = true)
                                        .replace("ExchangeRate-API", "أسعار الصرف", ignoreCase = true)
                                } else {
                                    row.sourceName.ifBlank { "Global" }
                                }
                                "$label | $src"
                            }
                            is SquareTickerRowModel.GoldKarat -> {
                                val bankShort = if (row.item.pricePerGramDzdBank > 0.0) {
                                    isolateLtr(formatTickerCompact(row.item.pricePerGramDzdBank))
                                } else "--"
                                if (isArabic) "${isolateLtr(row.item.purityLabel)} | بنك $bankShort"
                                else "${row.item.purityLabel} | Bank $bankShort"
                            }
                            is SquareTickerRowModel.CurrencyFx -> {
                                val localizedLabel = if (isArabic) "مقابل الدينار" else row.rate.nameEn
                                if (row.rate.sourceCount > 0) {
                                    if (isArabic) "$localizedLabel | ${isolateLtr("${row.rate.sourceCount}×")}"
                                    else "$localizedLabel | ${row.rate.sourceCount}×"
                                } else {
                                    localizedLabel
                                }
                            }
                        }
                        Text(
                            text = subtitle,
                            color = colors.textMuted,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Center-right: Last Price & Secondary Line (tabular)
            Column(
                modifier = Modifier.weight(1.05f),
                horizontalAlignment = Alignment.End
            ) {
                val perGramSuffix = if (isArabic) "/غ" else "/g"
                val dzdSuffix = if (isArabic) "دج" else "DZD"
                val (mainPriceText, subPriceText) = when (row) {
                    is SquareTickerRowModel.SpotGold -> {
                        val main = isolateLtr(formatTickerPrice(row.goldOzUsd))
                        val sub = isolateLtr("$${formatTickerPrice(row.usdPerGram24K)}$perGramSuffix")
                        main to sub
                    }
                    is SquareTickerRowModel.GoldKarat -> {
                        when (venueFilter) {
                            SquareVenueFilter.BANK -> {
                                val main = if (row.item.pricePerGramDzdBank > 0.0) {
                                    isolateLtr(formatTickerPrice(row.item.pricePerGramDzdBank))
                                } else "---"
                                val sub = isolateLtr("$${formatTickerPrice(row.item.pricePerGramUsd)}$perGramSuffix")
                                main to sub
                            }
                            SquareVenueFilter.USD -> {
                                val main = isolateLtr("$${formatTickerPrice(row.item.pricePerGramUsd)}")
                                val sub = "${isolateLtr(formatTickerCompact(row.item.pricePerGramDzdParallel))} $dzdSuffix"
                                main to sub
                            }
                            else -> {
                                val main = if (row.item.pricePerGramDzdParallel > 0.0) {
                                    isolateLtr(formatTickerPrice(row.item.pricePerGramDzdParallel))
                                } else "---"
                                val sub = isolateLtr("$${formatTickerPrice(row.item.pricePerGramUsd)}$perGramSuffix")
                                main to sub
                            }
                        }
                    }
                    is SquareTickerRowModel.CurrencyFx -> {
                        when (venueFilter) {
                            SquareVenueFilter.BANK -> {
                                val main = if (row.rate.bankRateDzd > 0.0) isolateLtr(formatTickerPrice(row.rate.bankRateDzd)) else "---"
                                val sub = if (row.rate.parallelSellDzd > 0.0) {
                                    "${if (isArabic) "سكوار" else "Sq"} ${isolateLtr(formatTickerCompact(row.rate.parallelSellDzd))}"
                                } else "---"
                                main to sub
                            }
                            else -> {
                                val main = when {
                                    row.rate.parallelSellDzd > 0.0 -> isolateLtr(formatTickerPrice(row.rate.parallelSellDzd))
                                    row.rate.bankRateDzd > 0.0 -> isolateLtr(formatTickerPrice(row.rate.bankRateDzd))
                                    else -> "---"
                                }
                                val sub = when {
                                    row.rate.parallelBuyDzd > 0.0 && row.rate.bankRateDzd > 0.0 -> {
                                        if (isArabic) "شراء ${isolateLtr(formatTickerCompact(row.rate.parallelBuyDzd))}"
                                        else "Buy ${formatTickerCompact(row.rate.parallelBuyDzd)}"
                                    }
                                    row.rate.bankRateDzd > 0.0 -> "${if (isArabic) "بنك" else "Bank"} ${isolateLtr(formatTickerCompact(row.rate.bankRateDzd))}"
                                    else -> "---"
                                }
                                main to sub
                            }
                        }
                    }
                }

                Text(
                    text = mainPriceText,
                    color = colors.textPrimary,
                    fontSize = typo.bodyLg,
                    fontWeight = FontWeight.ExtraBold,
                    style = tnumStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subPriceText,
                    color = colors.textMuted,
                    fontSize = typo.bodyXs,
                    fontWeight = FontWeight.Medium,
                    style = tnumStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Far-right: 24h % price change vs previous day's closing price
            val pillValue = row.changeOrSpreadValue
            val hasValidMetric = when (row) {
                is SquareTickerRowModel.SpotGold -> row.goldOzUsd > 0.0
                is SquareTickerRowModel.GoldKarat -> row.primaryNumericPrice > 0.0
                is SquareTickerRowModel.CurrencyFx -> row.primaryNumericPrice > 0.0 && (row.hasBaseline || abs(pillValue) > 0.0001)
            }

            val isPositive = pillValue > 0.005
            val isNegative = pillValue < -0.005
            val pillColor = when {
                !hasValidMetric -> colors.glassSurface
                isPositive -> TickerGreenPill
                isNegative -> TickerRedPill
                else -> TickerNeutralPill
            }
            val pctChar = if (isArabic) "٪" else "%"
            val pillLabel = when {
                !hasValidMetric -> "--"
                !isPositive && !isNegative -> isolateLtr("0.00$pctChar")
                else -> {
                    val sign = if (isPositive) "+" else ""
                    val decimals = if (abs(pillValue) >= 100.0) 1 else 2
                    isolateLtr("$sign${String.format(Locale.US, "%.${decimals}f", pillValue)}$pctChar")
                }
            }

            Box(
                modifier = Modifier
                    .widthIn(min = 72.dp, max = 94.dp)
                    .defaultMinSize(minHeight = 32.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(pillColor)
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = pillLabel,
                    color = if (hasValidMetric) Color.White else colors.textMuted,
                    fontSize = typo.bodySm,
                    fontWeight = FontWeight.Bold,
                    style = tnumStyle,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Smooth expandable breakdown & actions tray
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            TickerExpandedTray(
                row = row,
                isPinned = isPinned,
                isArabic = isArabic,
                colors = colors,
                onTogglePin = onTogglePin,
                onOpenInCalculator = onOpenInCalculator
            )
        }
    }
}

@Composable
private fun TickerLeadingEmblem(
    row: SquareTickerRowModel,
    isArabic: Boolean,
    colors: LiquidGlassColors
) {
    when (row) {
        is SquareTickerRowModel.SpotGold -> {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Color(0xFFFDE047), SquareGoldAccent, SquareGoldDeep))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Diamond,
                    contentDescription = if (isArabic) "ذهب" else "XAU",
                    tint = Color(0xFF1F1600),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        is SquareTickerRowModel.GoldKarat -> {
            val isPrime = row.item.karat == 24 || row.item.karat == 18
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                SquareGoldAccent.copy(alpha = if (isPrime) 0.28f else 0.16f),
                                SquareGoldDeep.copy(alpha = 0.18f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = SquareGoldAccent.copy(alpha = if (isPrime) 0.65f else 0.35f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isArabic) "ع${row.item.karat}" else "${row.item.karat}K",
                    color = SquareGoldAccent,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
        is SquareTickerRowModel.CurrencyFx -> {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(row.accentColor.copy(alpha = 0.16f))
                    .border(1.dp, row.accentColor.copy(alpha = 0.38f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = row.rate.flag,
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
private fun TickerExpandedTray(
    row: SquareTickerRowModel,
    isPinned: Boolean,
    isArabic: Boolean,
    colors: LiquidGlassColors,
    onTogglePin: () -> Unit,
    onOpenInCalculator: () -> Unit
) {
    val unitDzd = if (isArabic) "دج" else "DZD"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.glassSurface.copy(alpha = 0.88f))
            .border(0.8.dp, colors.glassBorderSubtle, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        when (row) {
            is SquareTickerRowModel.SpotGold -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TrayMetricBox(
                        label = if (isArabic) "أونصة (31.1غ)" else "1 Troy Oz",
                        value = "$${formatTickerPrice(row.goldOzUsd)}",
                        valueColor = SquareGoldAccent,
                        modifier = Modifier.weight(1f),
                        colors = colors
                    )
                    TrayMetricBox(
                        label = if (isArabic) "إغلاق الأمس $" else "Prev Close $",
                        value = if (row.previousGoldOzUsd > 0.0) "$${formatTickerPrice(row.previousGoldOzUsd)}" else "---",
                        valueColor = CyanNeon,
                        modifier = Modifier.weight(1f),
                        colors = colors
                    )
                    TrayMetricBox(
                        label = if (isArabic) "عيار 24 سكوار/غ" else "24K Square/g",
                        value = "${formatTickerCompact(row.dzdPerGram24KParallel)} $unitDzd",
                        valueColor = EmeraldNeon,
                        modifier = Modifier.weight(1f),
                        colors = colors
                    )
                }
            }
            is SquareTickerRowModel.GoldKarat -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TrayMetricBox(
                        label = if (isArabic) "غرام (سكوار)" else "1g Square",
                        value = "${formatTickerCompact(row.item.pricePerGramDzdParallel)} $unitDzd",
                        valueColor = SquareGoldAccent,
                        modifier = Modifier.weight(1f),
                        colors = colors
                    )
                    TrayMetricBox(
                        label = if (isArabic) "إغلاق الأمس" else "Prev Close",
                        value = if (row.previousPrice > 0.0) "${formatTickerCompact(row.previousPrice)} $unitDzd" else "---",
                        valueColor = CyanNeon,
                        modifier = Modifier.weight(1f),
                        colors = colors
                    )
                    TrayMetricBox(
                        label = if (isArabic) "غرام (البنك)" else "1g Bank",
                        value = "${formatTickerCompact(row.item.pricePerGramDzdBank)} $unitDzd",
                        valueColor = colors.textPrimary,
                        modifier = Modifier.weight(1f),
                        colors = colors
                    )
                }
            }
            is SquareTickerRowModel.CurrencyFx -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TrayMetricBox(
                        label = if (isArabic) "شراء سكوار" else "Square Buy",
                        value = if (row.rate.parallelBuyDzd > 0.0) "${formatTickerPrice(row.rate.parallelBuyDzd)} $unitDzd" else "---",
                        valueColor = EmeraldNeon,
                        modifier = Modifier.weight(1f),
                        colors = colors
                    )
                    TrayMetricBox(
                        label = if (isArabic) "إغلاق الأمس" else "Prev Close",
                        value = if (row.previousPriceDzd > 0.0) "${formatTickerPrice(row.previousPriceDzd)} $unitDzd" else "---",
                        valueColor = CyanNeon,
                        modifier = Modifier.weight(1f),
                        colors = colors
                    )
                    TrayMetricBox(
                        label = if (isArabic) "سعر البنك" else "Bank Rate",
                        value = if (row.rate.bankRateDzd > 0.0) "${formatTickerPrice(row.rate.bankRateDzd)} $unitDzd" else "---",
                        valueColor = colors.textPrimary,
                        modifier = Modifier.weight(1f),
                        colors = colors
                    )
                }
                val gapText = if (row.rate.parallelSellDzd > 0.0 && row.rate.bankRateDzd > 0.0) {
                    val gapSign = if (row.spreadPct >= 0.0) "+" else ""
                    val gapPctStr = isolateLtr("$gapSign${String.format(Locale.US, "%.1f", row.spreadPct)}${if (isArabic) "٪" else "%"}")
                    if (isArabic) "فارق السكوار عن البنك: $gapPctStr" else "Square-Bank Gap: $gapPctStr"
                } else ""
                val srcText = if (row.rate.parallelSource.isNotBlank()) {
                    (if (isArabic) "المصادر: " else "Sources: ") +
                        if (isArabic) row.rate.parallelSource.replace("sites", "مواقع", ignoreCase = true) else row.rate.parallelSource
                } else ""
                val footerLine = listOf(gapText, srcText).filter { it.isNotBlank() }.joinToString(" • ")
                if (footerLine.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = footerLine,
                        color = colors.textMuted,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isPinned) PinHighlightColor.copy(alpha = 0.16f) else colors.glassCard)
                    .border(
                        0.8.dp,
                        if (isPinned) PinHighlightColor.copy(alpha = 0.55f) else colors.glassBorderSubtle,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable(onClick = onTogglePin)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = null,
                        tint = if (isPinned) PinHighlightColor else colors.textSecondary,
                        modifier = Modifier
                            .size(14.dp)
                            .rotate(35f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isPinned) {
                            if (isArabic) "إزالة من المفضلة" else "Unpin Favorite"
                        } else {
                            if (isArabic) "تثبيت في المفضلة" else "Pin to Favorites"
                        },
                        color = if (isPinned) PinHighlightColor else colors.textPrimary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SquareGoldAccent.copy(alpha = 0.18f))
                    .border(0.8.dp, SquareGoldAccent.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .clickable(onClick = onOpenInCalculator)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = null,
                        tint = SquareGoldAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isArabic) "احسب في الحاسبة" else "Convert",
                        color = SquareGoldAccent,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun TrayMetricBox(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier,
    colors: LiquidGlassColors
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(colors.glassCard)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = colors.textMuted,
            fontSize = 9.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = isolateLtr(value),
            color = valueColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            style = TextStyle(fontFeatureSettings = "tnum"),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SquareQuickConverterCard(
    snapshot: SquareMarketSnapshot,
    isGoldMode: Boolean,
    onModeChange: (Boolean) -> Unit,
    amountInput: String,
    onAmountChange: (String) -> Unit,
    selectedKarat: Int,
    onSelectKarat: (Int) -> Unit,
    selectedCurrencyCode: String,
    onSelectCurrency: (String) -> Unit,
    isArabic: Boolean,
    colors: LiquidGlassColors
) {
    val qty = amountInput.replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val unitDzd = if (isArabic) "دج" else "DZD"
    val parallelAccent = CyanNeon
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("square_quick_converter"),
        backgroundColor = colors.glassCard,
        borderColor = SquareGoldAccent.copy(alpha = 0.35f),
        shape = RoundedCornerShape(dimens.cornerRadiusLg),
        cornerRadius = dimens.cornerRadiusLg
    ) {
        Column(modifier = Modifier.padding(dimens.cardPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = null,
                        tint = SquareGoldAccent,
                        modifier = Modifier.size(dimens.iconSizeSm)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isArabic) "حاسبة الذهب والعملات" else "Gold & FX Market Converter",
                        color = colors.textPrimary,
                        fontSize = typo.bodyLg,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minHeight = 36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isGoldMode) SquareGoldAccent.copy(alpha = 0.22f) else colors.glassSurface)
                            .clickable { onModeChange(true) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isArabic) "ذهب" else "Gold",
                            color = if (isGoldMode) SquareGoldAccent else colors.textMuted,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minHeight = 36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!isGoldMode) parallelAccent.copy(alpha = 0.22f) else colors.glassSurface)
                            .clickable { onModeChange(false) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isArabic) "عملة" else "FX",
                            color = if (!isGoldMode) parallelAccent else colors.textMuted,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Option Selector Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isGoldMode) {
                    snapshot.goldKarats.forEach { k ->
                        val active = k.karat == selectedKarat
                        Box(
                            modifier = Modifier
                                .defaultMinSize(minHeight = 36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) SquareGoldAccent.copy(alpha = 0.22f) else colors.glassSurface)
                                .border(
                                    0.8.dp,
                                    if (active) SquareGoldAccent.copy(alpha = 0.6f) else colors.glassBorderSubtle,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { onSelectKarat(k.karat) }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isArabic) "عيار ${k.karat}" else "${k.karat}K",
                                color = if (active) SquareGoldAccent else colors.textSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    snapshot.currencies.forEach { fx ->
                        val active = fx.code == selectedCurrencyCode
                        Box(
                            modifier = Modifier
                                .defaultMinSize(minHeight = 36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) parallelAccent.copy(alpha = 0.22f) else colors.glassSurface)
                                .border(
                                    0.8.dp,
                                    if (active) parallelAccent.copy(alpha = 0.6f) else colors.glassBorderSubtle,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { onSelectCurrency(fx.code) }
                                .padding(horizontal = 11.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isArabic) "${fx.flag} ${fx.nameAr}" else "${fx.flag} ${fx.code}",
                                color = if (active) parallelAccent else colors.textSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick amount presets
            val presets = if (isGoldMode) listOf("1", "5", "10", "31.1", "100") else listOf("10", "50", "100", "500", "1000")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                presets.forEach { preset ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.glassSurface)
                            .border(0.6.dp, colors.glassBorderSubtle, RoundedCornerShape(8.dp))
                            .clickable { onAmountChange(preset) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = if (isGoldMode) (if (isArabic) "${isolateLtr(preset)} غ" else "${preset}g") else isolateLtr(preset),
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            style = TextStyle(fontFeatureSettings = "tnum")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Input Field
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.glassSurface)
                    .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 11.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicTextField(
                        value = amountInput,
                        onValueChange = { raw ->
                            if (raw.length <= 12) onAmountChange(raw)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = TextStyle(
                            color = colors.textPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFeatureSettings = "tnum",
                            textDirection = TextDirection.Ltr
                        ),
                        cursorBrush = SolidColor(SquareGoldAccent),
                        modifier = Modifier.weight(1f)
                    )
                    val selectedCurrencyLabel = if (isArabic) {
                        snapshot.currencies.firstOrNull { it.code == selectedCurrencyCode }?.nameAr ?: selectedCurrencyCode
                    } else {
                        selectedCurrencyCode
                    }
                    Text(
                        text = if (isGoldMode) (if (isArabic) "غرام" else "Grams (g)") else selectedCurrencyLabel,
                        color = colors.textMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Breakdown Result Boxes
            if (isGoldMode) {
                val k = snapshot.goldKarats.firstOrNull { it.karat == selectedKarat }
                    ?: snapshot.goldKarats.firstOrNull()
                val totalSquare = (k?.pricePerGramDzdParallel ?: 0.0) * qty
                val totalBank = (k?.pricePerGramDzdBank ?: 0.0) * qty
                val totalUsd = (k?.pricePerGramUsd ?: 0.0) * qty

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TrayMetricBox(
                        label = if (isArabic) "القيمة (سكوار)" else "Square Value",
                        value = if (totalSquare > 0.0) "${formatTickerCompact(totalSquare)} $unitDzd" else "---",
                        valueColor = SquareGoldAccent,
                        modifier = Modifier.weight(1.1f),
                        colors = colors
                    )
                    TrayMetricBox(
                        label = if (isArabic) "القيمة (البنك)" else "Bank Value",
                        value = if (totalBank > 0.0) "${formatTickerCompact(totalBank)} $unitDzd" else "---",
                        valueColor = colors.textPrimary,
                        modifier = Modifier.weight(1f),
                        colors = colors
                    )
                    TrayMetricBox(
                        label = if (isArabic) "بالدولار $" else "USD Spot",
                        value = "$${formatTickerPrice(totalUsd)}",
                        valueColor = EmeraldNeon,
                        modifier = Modifier.weight(0.95f),
                        colors = colors
                    )
                }
            } else {
                val fx = snapshot.currencies.firstOrNull { it.code == selectedCurrencyCode }
                    ?: snapshot.currencies.firstOrNull()
                val totalSell = (fx?.parallelSellDzd ?: 0.0) * qty
                val totalBuy = (fx?.parallelBuyDzd ?: 0.0) * qty
                val totalBank = (fx?.bankRateDzd ?: 0.0) * qty

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TrayMetricBox(
                        label = if (isArabic) "بيع سكوار" else "Square Sell",
                        value = if (totalSell > 0.0) "${formatTickerCompact(totalSell)} $unitDzd" else "---",
                        valueColor = parallelAccent,
                        modifier = Modifier.weight(1.05f),
                        colors = colors
                    )
                    TrayMetricBox(
                        label = if (isArabic) "شراء سكوار" else "Square Buy",
                        value = if (totalBuy > 0.0) "${formatTickerCompact(totalBuy)} $unitDzd" else "---",
                        valueColor = EmeraldNeon,
                        modifier = Modifier.weight(1f),
                        colors = colors
                    )
                    TrayMetricBox(
                        label = if (isArabic) "البنك الرسمي" else "Bank Official",
                        value = if (totalBank > 0.0) "${formatTickerCompact(totalBank)} $unitDzd" else "---",
                        valueColor = colors.textPrimary,
                        modifier = Modifier.weight(1f),
                        colors = colors
                    )
                }
            }
        }
    }
}

fun buildSquareTickerRows(
    snapshot: SquareMarketSnapshot,
    primaryTab: SquarePrimaryTab,
    venueFilter: SquareVenueFilter,
    pinnedIds: Set<String>,
    searchQuery: String
): List<SquareTickerRowModel> {
    val rows = mutableListOf<SquareTickerRowModel>()

    val goldOz = snapshot.goldOzUsd
    if (goldOz > 0.0 && (primaryTab == SquarePrimaryTab.ALL || primaryTab == SquarePrimaryTab.FAVORITES || primaryTab == SquarePrimaryTab.GOLD)) {
        val pure24 = snapshot.goldKarats.firstOrNull { it.karat == 24 }
        val usdPerGram = pure24?.pricePerGramUsd ?: (goldOz / TROY_OUNCE_IN_GRAMS)
        rows.add(
            SquareTickerRowModel.SpotGold(
                goldOzUsd = goldOz,
                change24hPct = snapshot.gold24hChangePercent,
                previousGoldOzUsd = snapshot.previousGoldOzUsd,
                hasBaseline = snapshot.hasGold24hBaseline,
                usdPerGram24K = usdPerGram,
                dzdPerGram24KParallel = pure24?.pricePerGramDzdParallel ?: 0.0,
                dzdPerGram24KBank = pure24?.pricePerGramDzdBank ?: 0.0,
                sourceName = snapshot.goldSource,
                sourceCount = snapshot.goldSourceCount,
                primaryNumericPrice = goldOz,
                changeOrSpreadValue = snapshot.gold24hChangePercent
            )
        )
    }

    if (primaryTab == SquarePrimaryTab.ALL || primaryTab == SquarePrimaryTab.FAVORITES || primaryTab == SquarePrimaryTab.GOLD) {
        snapshot.goldKarats.forEach { k ->
            val numericPrice = when (venueFilter) {
                SquareVenueFilter.BANK -> k.pricePerGramDzdBank
                SquareVenueFilter.USD -> k.pricePerGramUsd
                else -> if (k.pricePerGramDzdParallel > 0.0) k.pricePerGramDzdParallel else k.pricePerGramDzdBank
            }
            val (changePct, prevKaratPrice, hasKaratBase) = when (venueFilter) {
                SquareVenueFilter.BANK -> if (k.has24hBaseline && k.previousGramDzdBank > 0.0) {
                    Triple(k.change24hBankPercent, k.previousGramDzdBank, true)
                } else {
                    Triple(snapshot.gold24hChangePercent, 0.0, false)
                }
                SquareVenueFilter.USD -> if (k.has24hBaseline && k.previousGramUsd > 0.0) {
                    Triple(k.change24hUsdPercent, k.previousGramUsd, true)
                } else {
                    Triple(snapshot.gold24hChangePercent, 0.0, false)
                }
                else -> if (k.has24hBaseline && k.previousGramDzdParallel > 0.0 && k.pricePerGramDzdParallel > 0.0) {
                    Triple(k.change24hParallelPercent, k.previousGramDzdParallel, true)
                } else if (k.has24hBaseline && k.previousGramDzdBank > 0.0) {
                    Triple(k.change24hBankPercent, k.previousGramDzdBank, true)
                } else {
                    Triple(snapshot.gold24hChangePercent, 0.0, false)
                }
            }
            rows.add(
                SquareTickerRowModel.GoldKarat(
                    id = "XAU_${k.karat}K",
                    item = k,
                    change24hPct = changePct,
                    previousPrice = prevKaratPrice,
                    hasBaseline = hasKaratBase,
                    sourceName = snapshot.goldSource,
                    primaryNumericPrice = numericPrice,
                    changeOrSpreadValue = changePct,
                    sortName = "XAU${k.karat.toString().padStart(2, '0')}K"
                )
            )
        }
    }

    if (primaryTab == SquarePrimaryTab.ALL || primaryTab == SquarePrimaryTab.FAVORITES || primaryTab == SquarePrimaryTab.CURRENCIES) {
        snapshot.currencies.forEach { fx ->
            val hasBoth = fx.parallelSellDzd > 0.0 && fx.bankRateDzd > 0.0
            val spreadDzd = if (hasBoth) fx.parallelSellDzd - fx.bankRateDzd else 0.0
            val spreadPct = if (hasBoth && fx.bankRateDzd > 0.0) (spreadDzd / fx.bankRateDzd) * 100.0 else 0.0
            val isBankVenue = venueFilter == SquareVenueFilter.BANK
            val numericPrice = if (isBankVenue) {
                fx.bankRateDzd
            } else {
                if (fx.parallelSellDzd > 0.0) fx.parallelSellDzd else fx.bankRateDzd
            }
            val (changePct, prevFxPrice) = when {
                isBankVenue -> fx.change24hBankPercent to fx.previousBankRateDzd
                fx.parallelSellDzd > 0.0 -> fx.change24hParallelPercent to fx.previousParallelSellDzd
                else -> fx.change24hBankPercent to fx.previousBankRateDzd
            }
            val hasFxBaseline = fx.has24hBaseline && prevFxPrice > 0.0
            val accentHex = SquareMarketCatalog.findCurrencySpec(fx.code)?.accentHex ?: 0xFF38BDF8
            rows.add(
                SquareTickerRowModel.CurrencyFx(
                    id = fx.code,
                    rate = fx,
                    spreadPct = spreadPct,
                    spreadDzd = spreadDzd,
                    change24hPct = changePct,
                    previousPriceDzd = prevFxPrice,
                    hasBaseline = hasFxBaseline,
                    accentColor = Color(accentHex),
                    primaryNumericPrice = numericPrice,
                    changeOrSpreadValue = changePct
                )
            )
        }
    }

    val afterFavFilter = if (primaryTab == SquarePrimaryTab.FAVORITES) {
        rows.filter { it.id in pinnedIds }
    } else {
        rows
    }

    val cleanQuery = searchQuery.trim().lowercase(Locale.US)
    if (cleanQuery.isEmpty()) return afterFavFilter

    return afterFavFilter.filter { row ->
        when (row) {
            is SquareTickerRowModel.SpotGold ->
                "xau".contains(cleanQuery) || "gold".contains(cleanQuery) || "ذهب".contains(cleanQuery) || "اونصة".contains(cleanQuery)
            is SquareTickerRowModel.GoldKarat ->
                "${row.item.karat}k".contains(cleanQuery) ||
                    row.item.titleEn.lowercase(Locale.US).contains(cleanQuery) ||
                    row.item.titleAr.contains(cleanQuery)
            is SquareTickerRowModel.CurrencyFx ->
                row.rate.code.lowercase(Locale.US).contains(cleanQuery) ||
                    row.rate.nameEn.lowercase(Locale.US).contains(cleanQuery) ||
                    row.rate.nameAr.contains(cleanQuery)
        }
    }
}

fun sortSquareTickerRows(
    rows: List<SquareTickerRowModel>,
    sortColumn: SquareSortColumn,
    sortDirection: SquareSortDirection,
    pinnedIds: Set<String>
): List<SquareTickerRowModel> {
    if (sortColumn == SquareSortColumn.DEFAULT || sortDirection == SquareSortDirection.NONE) {
        return rows.sortedByDescending { it.id in pinnedIds }
    }
    return when (sortColumn) {
        SquareSortColumn.NAME -> {
            if (sortDirection == SquareSortDirection.ASC) rows.sortedBy { it.sortName }
            else rows.sortedByDescending { it.sortName }
        }
        SquareSortColumn.PRICE -> {
            if (sortDirection == SquareSortDirection.DESC) rows.sortedByDescending { it.primaryNumericPrice }
            else rows.sortedBy { it.primaryNumericPrice }
        }
        SquareSortColumn.CHANGE -> {
            if (sortDirection == SquareSortDirection.DESC) rows.sortedByDescending { it.changeOrSpreadValue }
            else rows.sortedBy { it.changeOrSpreadValue }
        }
        SquareSortColumn.DEFAULT -> rows
    }
}

fun formatTickerPrice(amount: Double): String {
    if (amount <= 0.0) return "---"
    val nf = NumberFormat.getNumberInstance(Locale.US).apply {
        val decimals = when {
            amount >= 10_000.0 -> 0
            amount >= 100.0 -> 2
            else -> 2
        }
        minimumFractionDigits = decimals
        maximumFractionDigits = decimals
    }
    return nf.format(amount)
}

fun formatTickerCompact(amount: Double): String {
    if (amount <= 0.0) return "---"
    val nf = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = if (amount < 100.0) 2 else 0
        maximumFractionDigits = if (amount < 100.0) 2 else 1
    }
    return nf.format(amount)
}
