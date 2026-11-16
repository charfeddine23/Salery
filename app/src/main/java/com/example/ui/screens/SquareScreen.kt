package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.market.CurrencySquareRate
import com.example.market.GoldKaratPrice
import com.example.market.SQUARE_REFRESH_INTERVAL_MILLIS
import com.example.market.SquareMarketCatalog
import com.example.market.SquareMarketSnapshot
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.ArabicStrings
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassColors
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.LocalAdaptiveDimens
import com.example.ui.theme.LocalAdaptiveTypography
import com.example.ui.theme.RoseNeon
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private enum class SquareSectionFilter {
    ALL, GOLD, CURRENCIES
}

@Composable
fun SquareScreen(
    snapshot: SquareMarketSnapshot?,
    lastSyncMillis: Long,
    isRefreshing: Boolean,
    errorMessage: String?,
    onRefresh: (force: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors
    val isArabic = strings is ArabicStrings
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current

    val goldAccent = Color(0xFFFACC15)
    val goldDeep = Color(0xFFD97706)
    val parallelAccent = Color(0xFF38BDF8)

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

    var sectionFilter by remember { mutableStateOf(SquareSectionFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var converterIsGold by remember { mutableStateOf(true) }
    var converterInput by remember { mutableStateOf("10") }
    var selectedKarat by remember { mutableIntStateOf(18) }
    var selectedCurrencyCode by remember { mutableStateOf("EUR") }
    var refreshSpins by remember { mutableIntStateOf(0) }

    val spinAngle by animateFloatAsState(
        targetValue = refreshSpins * 360f,
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "square_refresh_spin"
    )

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

    val filteredCurrencies = remember(snapshot, searchQuery) {
        val list = snapshot?.currencies.orEmpty()
        val q = searchQuery.trim().lowercase(Locale.US)
        if (q.isEmpty()) list
        else list.filter { item ->
            item.code.lowercase(Locale.US).contains(q) ||
                item.nameEn.lowercase(Locale.US).contains(q) ||
                item.nameAr.contains(q)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = dimens.screenPaddingH),
        verticalArrangement = Arrangement.spacedBy(dimens.spacingMd)
    ) {
        // 1. Hero Spot Gold + Auto Hourly Sync Status Card
        item {
            Spacer(modifier = Modifier.height(dimens.spacingXs))
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("square_hero_card"),
                backgroundColor = colors.glassCard,
                borderColor = goldAccent.copy(alpha = 0.48f),
                shape = RoundedCornerShape(dimens.cornerRadiusXl),
                cornerRadius = dimens.cornerRadiusXl
            ) {
                Column(modifier = Modifier.padding(dimens.cardPaddingLarge)) {
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
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.linearGradient(listOf(goldAccent, goldDeep))
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CurrencyExchange,
                                    contentDescription = "Square Market",
                                    tint = Color(0xFF1F1600),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isArabic) "سكوار — الذهب والعملة الصعبة" else "Square — Gold & FX",
                                    color = colors.textPrimary,
                                    fontSize = typo.titleSm,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (isArabic) "سعر السوق الموازية والبنك المباشر" else "Parallel Square & Official Bank Live",
                                    color = colors.textMuted,
                                    fontSize = typo.caption,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                refreshSpins++
                                onRefresh(true)
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(goldAccent.copy(alpha = 0.16f))
                                .border(1.dp, goldAccent.copy(alpha = 0.45f), CircleShape)
                                .testTag("square_refresh_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = if (isArabic) "تحديث الآن" else "Refresh now",
                                tint = goldAccent,
                                modifier = Modifier
                                    .size(22.dp)
                                    .rotate(spinAngle)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Spot Gold Ounce USD Banner inside Hero
                    val goldOz = snapshot?.goldOzUsd ?: 0.0
                    val goldChange = snapshot?.gold24hChangePercent ?: 0.0
                    val isGoldUp = goldChange >= 0.0
                    val changeColor = if (isGoldUp) EmeraldNeon else RoseNeon

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.glassSurface.copy(alpha = 0.78f))
                            .border(1.dp, goldAccent.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isArabic) "أونصة الذهب العالمية (XAU / USD)" else "Global Gold Spot (XAU / USD)",
                                color = colors.textSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = if (goldOz > 0.0) {
                                    "${formatMarketUsd(goldOz)} / oz"
                                } else {
                                    if (isArabic) "--- $ / أونصة" else "--- USD / oz"
                                },
                                color = goldAccent,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        if (goldOz > 0.0) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(changeColor.copy(alpha = 0.16f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isGoldUp) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                    contentDescription = null,
                                    tint = changeColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${if (isGoldUp) "+" else ""}${String.format(Locale.US, "%.2f", goldChange)}%",
                                    color = changeColor,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Auto-refresh every 1 hour info chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(CyanNeon.copy(alpha = 0.14f))
                                .padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = CyanNeon,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isArabic) "تحديث تلقائي كل 1 ساعة" else "Auto-refresh every 1h",
                                color = CyanNeon,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = if (effectiveLastSync > 0L) {
                                if (isArabic) "$lastSyncLabel • بعد ${remainingMins}د"
                                else "$lastSyncLabel • ${remainingMins}m left"
                            } else {
                                lastSyncLabel
                            },
                            color = colors.textMuted,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (snapshot != null && (snapshot.goldSource.isNotBlank() || snapshot.bankSource.isNotBlank())) {
                        Spacer(modifier = Modifier.height(6.dp))
                        val sourcesText = listOfNotNull(
                            snapshot.goldSource.takeIf { it.isNotBlank() },
                            snapshot.bankSource.takeIf { it.isNotBlank() },
                            snapshot.parallelSource.takeIf { it.isNotBlank() }
                        ).joinToString(" • ")
                        Text(
                            text = if (isArabic) "المصادر: $sourcesText" else "Sources: $sourcesText",
                            color = colors.textMuted,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (isRefreshing) {
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(CircleShape),
                            color = goldAccent,
                            trackColor = colors.glassSurface
                        )
                    }

                    if (errorMessage != null && snapshot != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(AmberNeon.copy(alpha = 0.14f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = AmberNeon,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = errorMessage,
                                color = AmberNeon,
                                fontSize = 11.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Empty / Error State when no cached snapshot exists
        if (snapshot == null) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = colors.glassCard,
                    shape = RoundedCornerShape(dimens.cornerRadiusLg)
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
                            tint = if (isRefreshing) goldAccent else AmberNeon,
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
                            fontWeight = FontWeight.Bold
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

        // 2. Section Filter Pills (All / Gold / Hard Currencies)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SquareFilterPill(
                    label = if (isArabic) "الكل" else "All",
                    selected = sectionFilter == SquareSectionFilter.ALL,
                    accentColor = CyanNeon,
                    colors = colors,
                    modifier = Modifier.testTag("square_filter_all"),
                    onClick = { sectionFilter = SquareSectionFilter.ALL }
                )
                SquareFilterPill(
                    label = if (isArabic) "الذهب (كل العيارات)" else "Gold (All Karats)",
                    selected = sectionFilter == SquareSectionFilter.GOLD,
                    accentColor = goldAccent,
                    colors = colors,
                    modifier = Modifier.testTag("square_filter_gold"),
                    onClick = { sectionFilter = SquareSectionFilter.GOLD }
                )
                SquareFilterPill(
                    label = if (isArabic) "العملة الصعبة" else "Hard Currencies",
                    selected = sectionFilter == SquareSectionFilter.CURRENCIES,
                    accentColor = EmeraldNeon,
                    colors = colors,
                    modifier = Modifier.testTag("square_filter_currencies"),
                    onClick = { sectionFilter = SquareSectionFilter.CURRENCIES }
                )
            }
        }

        // 3. Interactive Quick Converter (Gold Grams / Currencies)
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
                colors = colors,
                goldAccent = goldAccent,
                parallelAccent = parallelAccent
            )
        }

        // 4. Gold Section — All Karats (24K, 22K, 21K, 18K, 14K, 9K) in DZD & USD
        if (sectionFilter == SquareSectionFilter.ALL || sectionFilter == SquareSectionFilter.GOLD) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Diamond,
                            contentDescription = null,
                            tint = goldAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isArabic) "أسعار الذهب — كل العيارات" else "Gold Prices — All Karats",
                            color = colors.textPrimary,
                            fontSize = typo.titleSm,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Text(
                        text = if (isArabic) "سعر الغرام (دج و $)" else "Per Gram (DZD & USD)",
                        color = goldAccent,
                        fontSize = typo.caption,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (snapshot.goldKarats.isEmpty()) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = colors.glassCard,
                        shape = RoundedCornerShape(dimens.cornerRadiusMd)
                    ) {
                        Text(
                            text = if (isArabic) "تعذر جلب أسعار الذهب حالياً — اضغط تحديث" else "Gold spot rates unavailable — tap refresh",
                            color = colors.textMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            } else {
                items(snapshot.goldKarats, key = { "karat_${it.karat}" }) { karatItem ->
                    GoldKaratRowCard(
                        item = karatItem,
                        isArabic = isArabic,
                        colors = colors,
                        goldAccent = goldAccent
                    )
                }
            }
        }

        // 5. Hard Currencies Section — Official Bank vs Parallel Market (Square)
        if (sectionFilter == SquareSectionFilter.ALL || sectionFilter == SquareSectionFilter.CURRENCIES) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = parallelAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isArabic) "العملة الصعبة — البنك والموازية" else "Hard Currencies — Bank vs Square",
                            color = colors.textPrimary,
                            fontSize = typo.titleSm,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Text(
                        text = "${snapshot.currencies.size} ${if (isArabic) "عملات" else "FX"}",
                        color = parallelAccent,
                        fontSize = typo.caption,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Currency Search Input
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.glassSurface)
                        .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(14.dp))
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
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = if (isArabic) "ابحث عن عملة (EUR, USD, GBP...)" else "Search currency (EUR, USD, GBP...)",
                                    color = colors.textMuted,
                                    fontSize = 12.5.sp
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = colors.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                cursorBrush = SolidColor(colors.primaryNeon),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            items(filteredCurrencies, key = { "fx_${it.code}" }) { rate ->
                CurrencySquareCard(
                    rate = rate,
                    isArabic = isArabic,
                    colors = colors,
                    parallelAccent = parallelAccent
                )
            }
        }

        item { Spacer(modifier = Modifier.height(dimens.spacingXxl)) }
    }
}

@Composable
private fun SquareFilterPill(
    label: String,
    selected: Boolean,
    accentColor: Color,
    colors: LiquidGlassColors,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) accentColor.copy(alpha = 0.2f) else colors.glassCard)
            .border(
                width = 1.dp,
                color = if (selected) accentColor.copy(alpha = 0.65f) else colors.glassBorderSubtle,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) accentColor else colors.textSecondary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun GoldKaratRowCard(
    item: GoldKaratPrice,
    isArabic: Boolean,
    colors: LiquidGlassColors,
    goldAccent: Color
) {
    val highlight = item.karat == 24 || item.karat == 18
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("square_karat_${item.karat}"),
        backgroundColor = colors.glassCard,
        borderColor = if (highlight) goldAccent.copy(alpha = 0.45f) else colors.glassBorderSubtle,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                            .size(44.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        goldAccent.copy(alpha = 0.28f),
                                        Color(0xFFD97706).copy(alpha = 0.18f)
                                    )
                                )
                            )
                            .border(1.dp, goldAccent.copy(alpha = 0.45f), RoundedCornerShape(13.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${item.karat}K",
                            color = goldAccent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isArabic) item.titleAr else item.titleEn,
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isArabic) "نسبة النقاء: ${item.purityLabel}" else "Purity: ${item.purityLabel}",
                            color = colors.textMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                // Price per Gram in USD
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(EmeraldNeon.copy(alpha = 0.14f))
                        .border(0.8.dp, EmeraldNeon.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${formatMarketUsd(item.pricePerGramUsd)} / g",
                        color = EmeraldNeon,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // DZD Valuation Columns: Parallel Square vs Official Bank
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.glassSurface)
                        .border(0.8.dp, goldAccent.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Column {
                        Text(
                            text = if (isArabic) "بالدينار (السوق الموازية)" else "DZD (Parallel Square)",
                            color = colors.textMuted,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (item.pricePerGramDzdParallel > 0.0) {
                                "${formatMarketDzd(item.pricePerGramDzdParallel)} ${if (isArabic) "دج/غ" else "DZD/g"}"
                            } else "---",
                            color = goldAccent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.glassSurface)
                        .border(0.8.dp, colors.glassBorderSubtle, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Column {
                        Text(
                            text = if (isArabic) "بالدينار (سعر البنك)" else "DZD (Official Bank)",
                            color = colors.textMuted,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (item.pricePerGramDzdBank > 0.0) {
                                "${formatMarketDzd(item.pricePerGramDzdBank)} ${if (isArabic) "دج/غ" else "DZD/g"}"
                            } else "---",
                            color = colors.textPrimary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrencySquareCard(
    rate: CurrencySquareRate,
    isArabic: Boolean,
    colors: LiquidGlassColors,
    parallelAccent: Color
) {
    val accentHex = SquareMarketCatalog.findCurrencySpec(rate.code)?.accentHex ?: 0xFF38BDF8
    val accent = Color(accentHex)
    val unitLabel = if (isArabic) "دج" else "DZD"
    val hasParallel = rate.parallelSellDzd > 0.0 && rate.bankRateDzd > 0.0
    val spreadDzd = if (hasParallel) rate.parallelSellDzd - rate.bankRateDzd else 0.0
    val spreadPct = if (hasParallel && rate.bankRateDzd > 0.0) (spreadDzd / rate.bankRateDzd) * 100.0 else 0.0

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("square_currency_${rate.code}"),
        backgroundColor = colors.glassCard,
        borderColor = accent.copy(alpha = 0.35f),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = rate.flag, fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "${rate.code} • ${if (isArabic) rate.nameAr else rate.nameEn}",
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "1 ${rate.code} (${rate.symbol})",
                            color = colors.textMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                if (hasParallel) {
                    val spreadPositive = spreadDzd >= 0.0
                    val spreadColor = if (spreadPositive) AmberNeon else EmeraldNeon
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(spreadColor.copy(alpha = 0.14f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${if (spreadPositive) "+" else ""}${formatMarketDzd(spreadDzd)} (${String.format(Locale.US, "%.0f", abs(spreadPct))}%)",
                            color = spreadColor,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Parallel Market (Square: Buy & Sell)
                Column(
                    modifier = Modifier
                        .weight(1.35f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.glassSurface)
                        .border(0.8.dp, parallelAccent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = parallelAccent,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isArabic) "السوق الموازية (سكوار)" else "Parallel Market (Square)",
                            color = parallelAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    if (rate.parallelSellDzd > 0.0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = if (isArabic) "شراء" else "Buy",
                                    color = colors.textMuted,
                                    fontSize = 9.5.sp
                                )
                                Text(
                                    text = "${formatMarketDzd(rate.parallelBuyDzd)} $unitLabel",
                                    color = EmeraldNeon,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (isArabic) "بيع" else "Sell",
                                    color = colors.textMuted,
                                    fontSize = 9.5.sp
                                )
                                Text(
                                    text = "${formatMarketDzd(rate.parallelSellDzd)} $unitLabel",
                                    color = parallelAccent,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    } else {
                        Text(
                            text = if (isArabic) "غير متوفر حالياً" else "Unavailable",
                            color = colors.textMuted,
                            fontSize = 11.5.sp
                        )
                    }
                }

                // Official Bank Rate
                Column(
                    modifier = Modifier
                        .weight(0.95f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.glassSurface)
                        .border(0.8.dp, colors.glassBorderSubtle, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isArabic) "البنك الرسمي" else "Official Bank",
                            color = colors.textSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (rate.bankRateDzd > 0.0) {
                            "${formatMarketDzd(rate.bankRateDzd)} $unitLabel"
                        } else "---",
                        color = colors.textPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
private fun SquareQuickConverterCard(
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
    colors: LiquidGlassColors,
    goldAccent: Color,
    parallelAccent: Color
) {
    val qty = amountInput.replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val unitDzd = if (isArabic) "دج" else "DZD"

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("square_quick_converter"),
        backgroundColor = colors.glassCard,
        borderColor = colors.glassBorderSubtle,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = null,
                        tint = colors.primaryNeon,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isArabic) "حاسبة السوق السريعة" else "Quick Market Converter",
                        color = colors.textPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isGoldMode) goldAccent.copy(alpha = 0.22f) else colors.glassSurface)
                            .clickable { onModeChange(true) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = if (isArabic) "ذهب" else "Gold",
                            color = if (isGoldMode) goldAccent else colors.textMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!isGoldMode) parallelAccent.copy(alpha = 0.22f) else colors.glassSurface)
                            .clickable { onModeChange(false) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = if (isArabic) "عملة" else "FX",
                            color = if (!isGoldMode) parallelAccent else colors.textMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Option Selector Row (Karats or Currencies)
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
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) goldAccent.copy(alpha = 0.22f) else colors.glassSurface)
                                .border(
                                    0.8.dp,
                                    if (active) goldAccent.copy(alpha = 0.6f) else colors.glassBorderSubtle,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { onSelectKarat(k.karat) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${k.karat}K",
                                color = if (active) goldAccent else colors.textSecondary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    snapshot.currencies.forEach { fx ->
                        val active = fx.code == selectedCurrencyCode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) parallelAccent.copy(alpha = 0.22f) else colors.glassSurface)
                                .border(
                                    0.8.dp,
                                    if (active) parallelAccent.copy(alpha = 0.6f) else colors.glassBorderSubtle,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { onSelectCurrency(fx.code) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${fx.flag} ${fx.code}",
                                color = if (active) parallelAccent else colors.textSecondary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(0.9f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.glassSurface)
                        .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
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
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            cursorBrush = SolidColor(colors.primaryNeon),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = if (isGoldMode) (if (isArabic) "غ" else "g") else selectedCurrencyCode,
                            color = colors.textMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Column(modifier = Modifier.weight(1.2f)) {
                    if (isGoldMode) {
                        val k = snapshot.goldKarats.firstOrNull { it.karat == selectedKarat }
                            ?: snapshot.goldKarats.firstOrNull()
                        val totalDzdParallel = (k?.pricePerGramDzdParallel ?: 0.0) * qty
                        val totalUsd = (k?.pricePerGramUsd ?: 0.0) * qty
                        Text(
                            text = if (totalDzdParallel > 0.0) "${formatMarketDzd(totalDzdParallel)} $unitDzd" else "---",
                            color = goldAccent,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "${formatMarketUsd(totalUsd)} • ${if (isArabic) "سكوار" else "Square"}",
                            color = EmeraldNeon,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        val fx = snapshot.currencies.firstOrNull { it.code == selectedCurrencyCode }
                            ?: snapshot.currencies.firstOrNull()
                        val totalParallelSell = (fx?.parallelSellDzd ?: 0.0) * qty
                        val totalBank = (fx?.bankRateDzd ?: 0.0) * qty
                        Text(
                            text = if (totalParallelSell > 0.0) "${formatMarketDzd(totalParallelSell)} $unitDzd" else "---",
                            color = parallelAccent,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "${if (isArabic) "البنك" else "Bank"}: ${formatMarketDzd(totalBank)} $unitDzd",
                            color = colors.textMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

private fun formatMarketUsd(amount: Double): String {
    val nf = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return "$${nf.format(amount)}"
}

private fun formatMarketDzd(amount: Double): String {
    val nf = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = if (amount < 100.0) 2 else 0
        maximumFractionDigits = if (amount < 100.0) 2 else 2
    }
    return nf.format(amount)
}
