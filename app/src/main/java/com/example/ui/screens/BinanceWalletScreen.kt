package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import com.example.crypto.BinanceCryptoCatalog
import com.example.crypto.CRYPTO_PRICE_REFRESH_INTERVAL_MILLIS
import com.example.crypto.CryptoAssetSpec
import com.example.crypto.CryptoHoldingEntry
import com.example.crypto.CryptoMarketPriceEntry
import com.example.data.UserSettings
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.edgeFadeVerticalContentPadding
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.ArabicStrings
import com.example.ui.i18n.LocalStealthMode
import com.example.ui.i18n.isolateLtr
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.LocalAdaptiveDimens
import com.example.ui.theme.LocalAdaptiveLayoutInfo
import com.example.ui.theme.LocalAdaptiveTypography
import com.example.ui.theme.RoseNeon
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private data class CryptoDialogTarget(
    val holdingId: String?,
    val symbol: String,
    val initialQuantity: Double,
    val initialNote: String
)

@Composable
fun BinanceWalletScreen(
    userSettings: UserSettings,
    holdings: List<CryptoHoldingEntry>,
    prices: Map<String, CryptoMarketPriceEntry>,
    isRefreshingPrices: Boolean,
    priceError: String?,
    onBackClick: () -> Unit,
    onRefreshPrices: (force: Boolean) -> Unit,
    onSaveHolding: (holdingId: String?, symbol: String, quantity: Double, note: String) -> Unit,
    onRemoveHolding: (holdingId: String) -> Unit,
    onConvertHoldingToUsdt: (holdingId: String, convertQuantity: Double) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    BackHandler { onBackClick() }

    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors
    val isArabic = strings is ArabicStrings
    val isStealth = LocalStealthMode.current
    val layoutInfo = LocalAdaptiveLayoutInfo.current
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current

    val binanceGold = Color(0xFFF0B90B)
    val binanceBrightGold = Color(0xFFFCD535)

    // Auto-check hourly price freshness on entering the Binance card
    LaunchedEffect(Unit) {
        onRefreshPrices(false)
    }

    var dialogTarget by remember { mutableStateOf<CryptoDialogTarget?>(null) }
    var convertTarget by remember { mutableStateOf<CryptoHoldingEntry?>(null) }
    var refreshSpins by remember { mutableIntStateOf(0) }
    val spinAngle by animateFloatAsState(
        targetValue = refreshSpins * 360f,
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "binance_price_refresh_spin"
    )

    val symbolCounts = remember(holdings) {
        holdings.groupingBy { it.symbol.uppercase(Locale.US) }.eachCount()
    }
    val holdingPositionsById = remember(holdings) {
        val counters = HashMap<String, Int>()
        val result = HashMap<String, Int>(holdings.size)
        for (item in holdings) {
            val sym = item.symbol.uppercase(Locale.US)
            val next = (counters[sym] ?: 0) + 1
            counters[sym] = next
            result[item.id] = next
        }
        result
    }

    val totalPortfolioUsd = remember(holdings, prices) {
        BinanceCryptoCatalog.calculatePortfolioTotalUsd(holdings, prices)
    }

    val lastSyncFormatted = remember(userSettings.binanceLastPriceSyncMillis, isArabic) {
        if (userSettings.binanceLastPriceSyncMillis > 0L) {
            isolateLtr(
                SimpleDateFormat("dd/MM • HH:mm", if (isArabic) Locale("ar") else Locale.US)
                    .format(Date(userSettings.binanceLastPriceSyncMillis))
            )
        } else {
            if (isArabic) "لم يتم التحديث بعد" else "Not synced yet"
        }
    }

    val nextSyncRemainingMins = remember(userSettings.binanceLastPriceSyncMillis, prices) {
        if (userSettings.binanceLastPriceSyncMillis <= 0L) 0L
        else {
            val remaining = (userSettings.binanceLastPriceSyncMillis + CRYPTO_PRICE_REFRESH_INTERVAL_MILLIS) -
                System.currentTimeMillis()
            (remaining / 60_000L).coerceAtLeast(0L)
        }
    }

    val priceSource = remember(prices, isArabic) {
        val raw = prices.values.firstOrNull { it.source.isNotBlank() }?.source ?: "Binance"
        if (!isArabic) raw
        else raw
            .replace("Binance", "بينانس", ignoreCase = true)
            .replace("CoinGecko", "كوين غيكو", ignoreCase = true)
            .replace("CryptoCompare", "كريبتو كومبير", ignoreCase = true)
    }

    dialogTarget?.let { target ->
        val matchingHolding = remember(target.holdingId, holdings) {
            target.holdingId?.let { id -> holdings.firstOrNull { it.id == id } }
        }
        AddOrEditCryptoDialog(
            isEditing = target.holdingId != null,
            initialSymbol = target.symbol,
            initialQuantity = target.initialQuantity,
            initialNote = target.initialNote,
            existingSymbolCounts = symbolCounts,
            prices = prices,
            secureScreenshots = userSettings.screenshotProtectionEnabled,
            onDismiss = { dialogTarget = null },
            onSave = { sym, qty, note ->
                onSaveHolding(target.holdingId, sym, qty, note)
                dialogTarget = null
            },
            onDelete = if (target.holdingId != null) {
                {
                    onRemoveHolding(target.holdingId)
                    dialogTarget = null
                }
            } else null,
            onConvertToUsdt = if (matchingHolding != null && !matchingHolding.symbol.equals("USDT", ignoreCase = true)) {
                {
                    dialogTarget = null
                    convertTarget = matchingHolding
                }
            } else null
        )
    }

    convertTarget?.let { targetHolding ->
        val latestHolding = remember(targetHolding.id, holdings) {
            holdings.firstOrNull { it.id == targetHolding.id } ?: targetHolding
        }
        val assetSpec = remember(latestHolding.symbol) {
            BinanceCryptoCatalog.findBySymbol(latestHolding.symbol)
                ?: CryptoAssetSpec(
                    symbol = latestHolding.symbol,
                    nameEn = latestHolding.symbol,
                    nameAr = latestHolding.symbol,
                    binancePair = "${latestHolding.symbol}USDT",
                    coingeckoId = latestHolding.symbol.lowercase(Locale.US),
                    accentHex = 0xFFF0B90B
                )
        }
        ConvertCryptoToUsdtDialog(
            holding = latestHolding,
            asset = assetSpec,
            prices = prices,
            isRefreshingPrices = isRefreshingPrices,
            secureScreenshots = userSettings.screenshotProtectionEnabled,
            onRefreshLivePrice = { onRefreshPrices(true) },
            onDismiss = { convertTarget = null },
            onConfirmConvert = { convertQty ->
                onConvertHoldingToUsdt(latestHolding.id, convertQty)
                convertTarget = null
            }
        )
    }

    val headerActionBar: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (isArabic) "تحديث الأسعار تلقائياً كل ساعة" else "Live hourly market sync • Crypto Portfolio",
                color = binanceGold,
                fontSize = typo.bodySm,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(dimens.spacingSm))
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(14.dp))
                    .background(binanceGold)
                    .clickable {
                        dialogTarget = CryptoDialogTarget(
                            holdingId = null,
                            symbol = "XRP",
                            initialQuantity = 0.0,
                            initialNote = ""
                        )
                    }
                    .testTag("binance_add_crypto_btn")
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = if (isArabic) "إضافة عملة" else "Add Coin",
                        tint = Color(0xFF0B0E11),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isArabic) "إضافة عملة" else "Add Coin",
                        color = Color(0xFF0B0E11),
                        fontSize = typo.bodySm,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                }
            }
        }
    }

    val portfolioHeroCard: @Composable () -> Unit = {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("binance_portfolio_hero_card"),
            backgroundColor = Color(0xFF12151B),
            borderColor = binanceGold.copy(alpha = 0.55f),
            shape = RoundedCornerShape(dimens.cornerRadiusLg),
            cornerRadius = dimens.cornerRadiusLg
        ) {
            val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
            Canvas(modifier = Modifier.matchParentSize()) {
                val cx = size.width * (if (isRtl) 0.15f else 0.85f)
                val cy = size.height * 0.35f
                val r = size.height * 0.45f
                val outer = Path().apply {
                    moveTo(cx, cy - r)
                    lineTo(cx + r, cy)
                    lineTo(cx, cy + r)
                    lineTo(cx - r, cy)
                    close()
                }
                drawPath(outer, binanceGold.copy(alpha = 0.14f), style = Stroke(width = 6f))
                val inner = Path().apply {
                    moveTo(cx, cy - r * 0.45f)
                    lineTo(cx + r * 0.45f, cy)
                    lineTo(cx, cy + r * 0.45f)
                    lineTo(cx - r * 0.45f, cy)
                    close()
                }
                drawPath(inner, binanceBrightGold.copy(alpha = 0.12f))
            }

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
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(binanceGold.copy(alpha = 0.18f))
                                .border(1.dp, binanceGold.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CurrencyBitcoin,
                                contentDescription = null,
                                tint = binanceGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isArabic) "إجمالي قيمة المحفظة الرقمية" else "Estimated Portfolio Value",
                                color = Color(0xFFCBD5E1),
                                fontSize = typo.bodyXs,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isArabic) "${holdings.size} عملات مضافة" else "${holdings.size} Assets Held",
                                color = binanceGold,
                                fontSize = typo.caption,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(12.dp))
                            .background(binanceGold.copy(alpha = 0.16f))
                            .border(1.dp, binanceGold.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                            .clickable(enabled = !isRefreshingPrices) {
                                refreshSpins += 1
                                onRefreshPrices(true)
                            }
                            .testTag("binance_refresh_prices_btn")
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = if (isArabic) "تحديث الأسعار" else "Refresh Prices",
                                tint = binanceGold,
                                modifier = Modifier
                                    .size(16.dp)
                                    .rotate(spinAngle)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isArabic) "تحديث الآن" else "Sync",
                                color = binanceGold,
                                fontSize = typo.caption,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(dimens.spacingMd))

                Text(
                    text = if (isStealth) (if (isArabic) "•••• دولار" else "$ ••••••") else isolateLtr(formatUsdCurrency(totalPortfolioUsd)),
                    color = Color.White,
                    fontSize = typo.hero,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (isRefreshingPrices) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = binanceGold,
                        trackColor = Color.White.copy(alpha = 0.12f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = binanceGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isArabic) {
                                "تحديث كل ساعة ($priceSource) • $lastSyncFormatted"
                            } else {
                                "Hourly sync ($priceSource) • $lastSyncFormatted"
                            },
                            color = Color(0xFFE2E8F0),
                            fontSize = typo.caption,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (userSettings.binanceLastPriceSyncMillis > 0L) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isArabic) "التالي: ${isolateLtr(nextSyncRemainingMins.toString())} د" else "Next: ${nextSyncRemainingMins}m",
                            color = EmeraldNeon,
                            fontSize = typo.caption,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                if (!priceError.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = priceError,
                        color = RoseNeon,
                        fontSize = typo.caption,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    val quickAddCarousel: @Composable () -> Unit = {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = if (isArabic) "العملات الرقمية الأساسية (اضغط لإضافة الكمية)" else "Major Cryptocurrencies (Tap to Add Quantity)",
                color = colors.textPrimary,
                fontSize = typo.body,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(BinanceCryptoCatalog.majorAssets, key = { it.symbol }) { asset ->
                    val livePrice = prices[asset.symbol]
                    val ownedCount = symbolCounts[asset.symbol.uppercase(Locale.US)] ?: 0
                    val accent = Color(asset.accentHex)
                    GlassCard(
                        modifier = Modifier
                            .width(136.dp)
                            .testTag("crypto_quick_card_${asset.symbol.lowercase()}"),
                        backgroundColor = colors.glassCard,
                        borderColor = if (ownedCount > 0) accent.copy(alpha = 0.7f) else colors.glassBorderSubtle,
                        shape = RoundedCornerShape(16.dp),
                        cornerRadius = 16.dp,
                        onClick = {
                            dialogTarget = CryptoDialogTarget(
                                holdingId = null,
                                symbol = asset.symbol,
                                initialQuantity = 0.0,
                                initialNote = ""
                            )
                        }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(accent.copy(alpha = 0.20f))
                                        .border(1.dp, accent.copy(alpha = 0.65f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = asset.symbol.take(3),
                                        color = accent,
                                        fontSize = typo.caption,
                                        fontWeight = FontWeight.Black,
                                        maxLines = 1
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (ownedCount > 0) EmeraldNeon.copy(alpha = 0.16f)
                                            else binanceGold.copy(alpha = 0.16f)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (ownedCount > 0) {
                                            if (isArabic) "×$ownedCount +" else "×$ownedCount +"
                                        } else {
                                            "+"
                                        },
                                        color = if (ownedCount > 0) EmeraldNeon else binanceGold,
                                        fontSize = typo.caption,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = asset.symbol,
                                color = colors.textPrimary,
                                fontSize = typo.body,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                            Text(
                                text = if (isArabic) asset.nameAr else asset.nameEn,
                                color = colors.textMuted,
                                fontSize = typo.caption,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            val priceText = when {
                                asset.isStablecoin -> "$1.00"
                                livePrice != null -> formatCryptoUnitPrice(livePrice.priceUsd)
                                else -> "---"
                            }
                            Text(
                                text = isolateLtr(priceText),
                                color = colors.textPrimary,
                                fontSize = typo.bodySm,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }

    if (layoutInfo.columns >= 2) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = dimens.screenPaddingH),
            horizontalArrangement = Arrangement.spacedBy(dimens.spacingLg)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(0.95f)
                    .fillMaxHeight(),
                contentPadding = edgeFadeVerticalContentPadding(),
                verticalArrangement = Arrangement.spacedBy(dimens.spacingMd)
            ) {
                item {
                    Spacer(modifier = Modifier.height(dimens.spacingXs))
                    headerActionBar()
                }
                item { portfolioHeroCard() }
                item { quickAddCarousel() }
                item { Spacer(modifier = Modifier.height(dimens.spacingXl)) }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1.05f)
                    .fillMaxHeight(),
                contentPadding = edgeFadeVerticalContentPadding(),
                verticalArrangement = Arrangement.spacedBy(dimens.spacingMd)
            ) {
                item {
                    Spacer(modifier = Modifier.height(dimens.spacingXs))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isArabic) "رصيدي من العملات الرقمية" else "My Crypto Holdings",
                            color = colors.textPrimary,
                            fontSize = typo.titleSm,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (holdings.isNotEmpty()) {
                            Text(
                                text = if (isArabic) "اضغط للتعديل" else "Tap to edit",
                                color = colors.textMuted,
                                fontSize = typo.caption,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                if (holdings.isEmpty()) {
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = colors.glassCard,
                            borderColor = binanceGold.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(18.dp),
                            onClick = {
                                dialogTarget = CryptoDialogTarget(
                                    holdingId = null,
                                    symbol = "XRP",
                                    initialQuantity = 0.0,
                                    initialNote = ""
                                )
                            }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(22.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .background(binanceGold.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = binanceGold,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (isArabic) "لم تضف أي عملة رقمية بعد" else "No Crypto Holdings Yet",
                                    color = colors.textPrimary,
                                    fontSize = typo.bodyLg,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isArabic) {
                                        "اضغط لإضافة أي عملة رقمية أساسية وتحديد الكمية التي تملكها لمتابعة قيمتها كل ساعة"
                                    } else {
                                        "Tap to add XRP, BTC, ETH or other major coins and track live hourly market value"
                                    },
                                    color = colors.textMuted,
                                    fontSize = typo.caption
                                )
                            }
                        }
                    }
                } else {
                    items(holdings, key = { it.id }) { holding ->
                        val spec = BinanceCryptoCatalog.findBySymbol(holding.symbol)
                            ?: CryptoAssetSpec(
                                symbol = holding.symbol,
                                nameEn = holding.symbol,
                                nameAr = holding.symbol,
                                binancePair = "${holding.symbol}USDT",
                                coingeckoId = holding.symbol.lowercase(Locale.US),
                                accentHex = 0xFFF0B90B
                            )
                        val marketPrice = prices[holding.symbol.uppercase(Locale.US)]
                        val unitPrice = if (spec.isStablecoin) marketPrice?.priceUsd ?: 1.0 else marketPrice?.priceUsd ?: 0.0
                        val totalUsd = holding.quantity * unitPrice
                        val changePct = marketPrice?.change24hPercent ?: 0.0
                        val accent = Color(spec.accentHex)
                        val totalSameCoin = symbolCounts[holding.symbol.uppercase(Locale.US)] ?: 1
                        val entryIndex = holdingPositionsById[holding.id] ?: 1

                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("crypto_holding_row_${spec.symbol.lowercase()}_${holding.id}"),
                            backgroundColor = colors.glassCard,
                            borderColor = accent.copy(alpha = 0.40f),
                            shape = RoundedCornerShape(18.dp),
                            onClick = {
                                dialogTarget = CryptoDialogTarget(
                                    holdingId = holding.id,
                                    symbol = spec.symbol,
                                    initialQuantity = holding.quantity,
                                    initialNote = holding.note
                                )
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(dimens.cardPadding),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(accent.copy(alpha = 0.18f))
                                            .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(14.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = spec.symbol,
                                            color = accent,
                                            fontSize = if (spec.symbol.length >= 4) typo.caption else typo.bodySm,
                                            fontWeight = FontWeight.Black,
                                            maxLines = 1
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = if (isArabic) spec.nameAr else spec.nameEn,
                                                color = colors.textPrimary,
                                                fontSize = typo.body,
                                                fontWeight = FontWeight.ExtraBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            val tagLabel = when {
                                                holding.note.isNotBlank() -> holding.note
                                                totalSameCoin > 1 -> "#$entryIndex"
                                                else -> null
                                            }
                                            if (tagLabel != null) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(accent.copy(alpha = 0.16f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = tagLabel,
                                                        color = accent,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (isStealth) {
                                                if (isArabic) "•••••• ${spec.nameAr}" else "•••••• ${spec.symbol}"
                                            } else {
                                                isolateLtr("${formatCryptoQuantity(holding.quantity)} ${spec.symbol}")
                                            },
                                            color = colors.textSecondary,
                                            fontSize = typo.bodySm,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = if (unitPrice > 0.0) isolateLtr("1 ${spec.symbol} = ${formatCryptoUnitPrice(unitPrice)}")
                                                else if (isArabic) "بانتظار السعر..." else "Fetching price...",
                                                color = colors.textMuted,
                                                fontSize = typo.caption,
                                                fontFamily = FontFamily.Monospace,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            if (marketPrice != null && !spec.isStablecoin) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                val isPositive = changePct >= 0.0
                                                val pctColor = if (isPositive) EmeraldNeon else RoseNeon
                                                Icon(
                                                    imageVector = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                                    contentDescription = null,
                                                    tint = pctColor,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(
                                                    text = isolateLtr(String.format(Locale.US, "%+.2f", changePct) + if (isArabic) "٪" else "%"),
                                                    color = pctColor,
                                                    fontSize = typo.caption,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (isStealth) (if (isArabic) "•••• دولار" else "$ ••••") else if (unitPrice > 0.0) isolateLtr(formatUsdCurrency(totalUsd)) else "---",
                                        color = if (unitPrice > 0.0) EmeraldNeon else colors.textMuted,
                                        fontSize = typo.bodyLg,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                dialogTarget = CryptoDialogTarget(
                                                    holdingId = holding.id,
                                                    symbol = spec.symbol,
                                                    initialQuantity = holding.quantity,
                                                    initialNote = holding.note
                                                )
                                            },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = if (isArabic) "تعديل الكمية" else "Edit Quantity",
                                                tint = CyanNeon,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { onRemoveHolding(holding.id) },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .testTag("delete_crypto_${spec.symbol.lowercase()}_${holding.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = if (isArabic) "حذف العملة" else "Delete Coin",
                                                tint = RoseNeon,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(dimens.spacingXl)) }
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = dimens.screenPaddingH),
            contentPadding = edgeFadeVerticalContentPadding(),
            verticalArrangement = Arrangement.spacedBy(dimens.spacingMd)
        ) {
            item {
                Spacer(modifier = Modifier.height(dimens.spacingXs))
                headerActionBar()
            }
            item { portfolioHeroCard() }
            item { quickAddCarousel() }

        // My Added Holdings Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isArabic) "رصيدي من العملات الرقمية" else "My Crypto Holdings",
                    color = colors.textPrimary,
                    fontSize = typo.titleSm,
                    fontWeight = FontWeight.Bold
                )
                if (holdings.isNotEmpty()) {
                    Text(
                        text = if (isArabic) "اضغط على العملة للتعديل" else "Tap asset to edit",
                        color = colors.textMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        if (holdings.isEmpty()) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = colors.glassCard,
                    borderColor = binanceGold.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(18.dp),
                    onClick = {
                        dialogTarget = CryptoDialogTarget(
                            holdingId = null,
                            symbol = "XRP",
                            initialQuantity = 0.0,
                            initialNote = ""
                        )
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(binanceGold.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = binanceGold,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (isArabic) "لم تضف أي عملة رقمية بعد" else "No Crypto Holdings Yet",
                            color = colors.textPrimary,
                            fontSize = typo.bodyLg,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isArabic) {
                                "اضغط لإضافة أي عملة رقمية أساسية وتحديد الكمية التي تملكها لمتابعة قيمتها كل ساعة"
                            } else {
                                "Tap to add XRP, BTC, ETH or other major coins and track live hourly market value"
                            },
                            color = colors.textMuted,
                            fontSize = typo.caption
                        )
                    }
                }
            }
        } else {
            items(holdings, key = { it.id }) { holding ->
                val spec = BinanceCryptoCatalog.findBySymbol(holding.symbol)
                    ?: CryptoAssetSpec(
                        symbol = holding.symbol,
                        nameEn = holding.symbol,
                        nameAr = holding.symbol,
                        binancePair = "${holding.symbol}USDT",
                        coingeckoId = holding.symbol.lowercase(Locale.US),
                        accentHex = 0xFFF0B90B
                    )
                val marketPrice = prices[holding.symbol.uppercase(Locale.US)]
                val unitPrice = when {
                    spec.isStablecoin -> marketPrice?.priceUsd ?: 1.0
                    else -> marketPrice?.priceUsd ?: 0.0
                }
                val totalUsd = holding.quantity * unitPrice
                val changePct = marketPrice?.change24hPercent ?: 0.0
                val accent = Color(spec.accentHex)
                val totalSameCoin = symbolCounts[holding.symbol.uppercase(Locale.US)] ?: 1
                val entryIndex = holdingPositionsById[holding.id] ?: 1

                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("crypto_holding_row_${spec.symbol.lowercase()}_${holding.id}"),
                    backgroundColor = colors.glassCard,
                    borderColor = accent.copy(alpha = 0.40f),
                    shape = RoundedCornerShape(18.dp),
                    onClick = {
                        dialogTarget = CryptoDialogTarget(
                            holdingId = holding.id,
                            symbol = spec.symbol,
                            initialQuantity = holding.quantity,
                            initialNote = holding.note
                        )
                    }
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(accent.copy(alpha = 0.18f))
                                        .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = spec.symbol,
                                        color = accent,
                                        fontSize = if (spec.symbol.length >= 4) 11.sp else 13.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = if (isArabic) spec.nameAr else spec.nameEn,
                                            color = colors.textPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        val tagLabel = when {
                                            holding.note.isNotBlank() -> holding.note
                                            totalSameCoin > 1 -> "#$entryIndex"
                                            else -> null
                                        }
                                        if (tagLabel != null) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(accent.copy(alpha = 0.16f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = tagLabel,
                                                    color = accent,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isStealth) {
                                            if (isArabic) "•••••• ${spec.nameAr}" else "•••••• ${spec.symbol}"
                                        } else {
                                            isolateLtr("${formatCryptoQuantity(holding.quantity)} ${spec.symbol}")
                                        },
                                        color = colors.textSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (unitPrice > 0.0) {
                                                isolateLtr("1 ${spec.symbol} = ${formatCryptoUnitPrice(unitPrice)}")
                                            } else {
                                                if (isArabic) "بانتظار السعر..." else "Fetching price..."
                                            },
                                            color = colors.textMuted,
                                            fontSize = 10.5.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        if (marketPrice != null && !spec.isStablecoin) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            val isPositive = changePct >= 0.0
                                            val pctColor = if (isPositive) EmeraldNeon else RoseNeon
                                            Icon(
                                                imageVector = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                                contentDescription = null,
                                                tint = pctColor,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                text = isolateLtr(String.format(Locale.US, "%+.2f", changePct) + if (isArabic) "٪" else "%"),
                                                color = pctColor,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (isStealth) {
                                        if (isArabic) "•••• دولار" else "$ ••••"
                                    } else if (unitPrice > 0.0) {
                                        isolateLtr(formatUsdCurrency(totalUsd))
                                    } else {
                                        "---"
                                    },
                                    color = if (unitPrice > 0.0) EmeraldNeon else colors.textMuted,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            dialogTarget = CryptoDialogTarget(
                                                holdingId = holding.id,
                                                symbol = spec.symbol,
                                                initialQuantity = holding.quantity,
                                                initialNote = holding.note
                                            )
                                        },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = if (isArabic) "تعديل الكمية" else "Edit Quantity",
                                            tint = CyanNeon,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onRemoveHolding(holding.id) },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("delete_crypto_${spec.symbol.lowercase()}_${holding.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = if (isArabic) "حذف العملة" else "Delete Coin",
                                            tint = RoseNeon,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (!spec.isStablecoin && !spec.symbol.equals("USDT", ignoreCase = true)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 14.dp, end = 14.dp, bottom = 12.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(EmeraldNeon.copy(alpha = 0.12f))
                                    .border(1.dp, EmeraldNeon.copy(alpha = 0.34f), RoundedCornerShape(12.dp))
                                    .clickable { convertTarget = holding }
                                    .testTag("convert_crypto_${spec.symbol.lowercase()}_${holding.id}")
                                    .padding(horizontal = 12.dp, vertical = 9.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CurrencyExchange,
                                            contentDescription = null,
                                            tint = EmeraldNeon,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = if (isArabic) "تحويل إلى USDT بالسعر اللحظي" else "Convert to USDT at Live Price",
                                            color = EmeraldNeon,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (!isStealth && unitPrice > 0.0) {
                                        Text(
                                            text = isolateLtr("≈ ${formatCryptoQuantity(totalUsd)} USDT"),
                                            color = colors.textSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

            item {
                Spacer(modifier = Modifier.height(dimens.spacingXl))
            }
        }
    }
}

@Composable
private fun AddOrEditCryptoDialog(
    isEditing: Boolean,
    initialSymbol: String,
    initialQuantity: Double,
    initialNote: String,
    existingSymbolCounts: Map<String, Int>,
    prices: Map<String, CryptoMarketPriceEntry>,
    secureScreenshots: Boolean,
    onDismiss: () -> Unit,
    onSave: (symbol: String, quantity: Double, note: String) -> Unit,
    onDelete: (() -> Unit)? = null,
    onConvertToUsdt: (() -> Unit)? = null
) {
    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors
    val isArabic = strings is ArabicStrings
    val binanceGold = Color(0xFFF0B90B)

    var selectedSymbol by remember { mutableStateOf(initialSymbol.uppercase(Locale.US)) }
    var isAddMoreMode by remember(isEditing) { mutableStateOf(false) }
    var quantityText by remember(initialSymbol, initialQuantity) {
        mutableStateOf(
            if (initialQuantity > 0.0) formatCryptoQuantityInput(initialQuantity) else ""
        )
    }
    var addMoreText by remember(initialSymbol, initialQuantity) { mutableStateOf("") }
    var noteText by remember(initialNote) { mutableStateOf(initialNote) }

    val selectedAsset = remember(selectedSymbol) {
        BinanceCryptoCatalog.findBySymbol(selectedSymbol) ?: BinanceCryptoCatalog.majorAssets.first()
    }
    val existingCountForSelected = existingSymbolCounts[selectedAsset.symbol.uppercase(Locale.US)] ?: 0
    val unitPrice = remember(selectedSymbol, prices) {
        if (selectedAsset.isStablecoin) prices[selectedAsset.symbol]?.priceUsd ?: 1.0
        else prices[selectedAsset.symbol]?.priceUsd ?: 0.0
    }

    val parsedQuantity = quantityText.replace(',', '.').trim().toDoubleOrNull()
    val parsedAddMore = addMoreText.replace(',', '.').trim().toDoubleOrNull()
    val isValidAddMore = parsedAddMore != null && parsedAddMore > 0.0 && !parsedAddMore.isNaN() && !parsedAddMore.isInfinite()
    val effectiveQuantity = if (isEditing && isAddMoreMode) {
        if (isValidAddMore) initialQuantity + parsedAddMore!! else null
    } else {
        parsedQuantity
    }
    val isValidQty = effectiveQuantity != null && effectiveQuantity > 0.0 && !effectiveQuantity.isNaN() && !effectiveQuantity.isInfinite()

    val quickAddPresets = remember(unitPrice) {
        when {
            unitPrice >= 1_000.0 -> listOf(0.01, 0.05, 0.1, 1.0)
            unitPrice >= 20.0 -> listOf(0.5, 1.0, 5.0, 10.0)
            else -> listOf(10.0, 50.0, 100.0, 500.0)
        }
    }

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
                .widthIn(max = 500.dp)
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            shape = RoundedCornerShape(24.dp),
            backgroundColor = colors.dialogBackground
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isEditing) {
                                if (isArabic) "تعديل رصيد العملة" else "Edit Crypto Holding"
                            } else {
                                if (isArabic) "إضافة عملة رقمية" else "Add Cryptocurrency"
                            },
                            color = colors.textPrimary,
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isArabic) {
                                "أدخل الكمية ويمكنك إضافة نفس العملة أكثر من مرة في محافظ مختلفة"
                            } else {
                                "Enter coin quantity — you can add the same coin multiple times"
                            },
                            color = colors.textMuted,
                            fontSize = 11.sp
                        )
                    }
                    if (onDelete != null) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("dialog_delete_crypto_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = if (isArabic) "حذف" else "Delete",
                                tint = RoseNeon
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = strings.cancel,
                            tint = colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (isArabic) "اختر العملة الرقمية" else "Select Cryptocurrency",
                    color = colors.textSecondary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(BinanceCryptoCatalog.majorAssets, key = { it.symbol }) { asset ->
                        val isSel = asset.symbol == selectedAsset.symbol
                        val accent = Color(asset.accentHex)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) accent.copy(alpha = 0.24f) else colors.glassCard)
                                .border(
                                    width = if (isSel) 1.5.dp else 1.dp,
                                    color = if (isSel) accent else colors.glassBorderSubtle,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedSymbol = asset.symbol }
                                .testTag("dialog_select_coin_${asset.symbol.lowercase()}")
                                .padding(horizontal = 12.dp, vertical = 8.dp)
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
                                    text = if (isArabic) asset.nameAr else asset.symbol,
                                    color = if (isSel) colors.textPrimary else colors.textSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                if (!isEditing && existingCountForSelected > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isArabic) {
                            "يوجد حالياً $existingCountForSelected رصيد مسجل لـ ${selectedAsset.nameAr} — سيتم حفظ هذا الرصيد كإضافة مستقلة"
                        } else {
                            "You already have $existingCountForSelected ${selectedAsset.symbol} entry — this will be added separately"
                        },
                        color = binanceGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Current Market Rate Pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.glassCard)
                        .border(1.dp, binanceGold.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isArabic) "السعر الحالي لـ ${selectedAsset.nameAr}" else "Current ${selectedAsset.symbol} Market Price",
                        color = colors.textSecondary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (unitPrice > 0.0) isolateLtr(formatCryptoUnitPrice(unitPrice)) else (if (isArabic) "سيتم جلبه" else "Syncing..."),
                        color = binanceGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (isEditing) {
                    Spacer(modifier = Modifier.height(12.dp))
                    // Mode Switcher: Edit Total vs Add More
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.glassCard)
                            .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(14.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val editSelected = !isAddMoreMode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(11.dp))
                                .background(if (editSelected) binanceGold.copy(alpha = 0.22f) else Color.Transparent)
                                .border(
                                    width = if (editSelected) 1.dp else 0.dp,
                                    color = if (editSelected) binanceGold.copy(alpha = 0.7f) else Color.Transparent,
                                    shape = RoundedCornerShape(11.dp)
                                )
                                .clickable { isAddMoreMode = false }
                                .testTag("crypto_mode_edit_total")
                                .padding(horizontal = 10.dp, vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = if (editSelected) binanceGold else colors.textSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (isArabic) "تعديل الإجمالي" else "Edit Total",
                                    color = if (editSelected) colors.textPrimary else colors.textSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = if (editSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        }

                        val addMoreSelected = isAddMoreMode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(11.dp))
                                .background(if (addMoreSelected) EmeraldNeon.copy(alpha = 0.20f) else Color.Transparent)
                                .border(
                                    width = if (addMoreSelected) 1.dp else 0.dp,
                                    color = if (addMoreSelected) EmeraldNeon.copy(alpha = 0.7f) else Color.Transparent,
                                    shape = RoundedCornerShape(11.dp)
                                )
                                .clickable { isAddMoreMode = true }
                                .testTag("crypto_mode_add_more")
                                .padding(horizontal = 10.dp, vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = if (addMoreSelected) EmeraldNeon else colors.textSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = if (isArabic) "إضافة المزيد" else "Add More",
                                    color = if (addMoreSelected) colors.textPrimary else colors.textSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = if (addMoreSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isEditing && isAddMoreMode) {
                    // Add More Panel: Current Holding + Amount to Add + Quick Chips + Live New Total
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.glassCard)
                            .border(1.dp, EmeraldNeon.copy(alpha = 0.42f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isArabic) "الرصيد الحالي" else "Current Holding",
                                color = colors.textSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = isolateLtr("${formatCryptoQuantity(initialQuantity)} ${selectedAsset.symbol}"),
                                color = colors.textPrimary,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (isArabic) "الكمية المراد إضافتها (+ ${selectedAsset.nameAr})" else "Amount to Add (+ ${selectedAsset.symbol})",
                            color = EmeraldNeon,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (addMoreText.isEmpty()) {
                                Text(
                                    text = if (isArabic) "+ أدخل الكمية الإضافية" else "+ Enter amount to add",
                                    color = colors.textMuted.copy(alpha = 0.55f),
                                    fontSize = 15.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            BasicTextField(
                                value = addMoreText,
                                onValueChange = { raw ->
                                    val filtered = raw.filter { it.isDigit() || it == '.' || it == ',' }
                                    if (filtered.length <= 18) {
                                        addMoreText = filtered
                                    }
                                },
                                textStyle = TextStyle(
                                    color = EmeraldNeon,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    textDirection = TextDirection.Ltr
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("crypto_add_more_input"),
                                cursorBrush = SolidColor(EmeraldNeon)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick Increment Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            quickAddPresets.forEach { preset ->
                                val presetLabel = "+${formatCryptoQuantityInput(preset)}"
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(EmeraldNeon.copy(alpha = 0.14f))
                                        .border(1.dp, EmeraldNeon.copy(alpha = 0.38f), RoundedCornerShape(10.dp))
                                        .clickable {
                                            val currentAdd = addMoreText.replace(',', '.').trim().toDoubleOrNull() ?: 0.0
                                            addMoreText = formatCryptoQuantityInput(currentAdd + preset)
                                        }
                                        .testTag("crypto_add_more_preset_${formatCryptoQuantityInput(preset)}")
                                        .padding(vertical = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = isolateLtr(presetLabel),
                                        color = EmeraldNeon,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        if (isValidAddMore && effectiveQuantity != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(EmeraldNeon.copy(alpha = 0.12f))
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(
                                        text = if (isArabic) {
                                            "الإجمالي الجديد: ${isolateLtr("${formatCryptoQuantity(initialQuantity)} + ${formatCryptoQuantity(parsedAddMore!!)} = ${formatCryptoQuantity(effectiveQuantity)} ${selectedAsset.symbol}")}"
                                        } else {
                                            "New Total: ${formatCryptoQuantity(initialQuantity)} + ${formatCryptoQuantity(parsedAddMore!!)} = ${formatCryptoQuantity(effectiveQuantity)} ${selectedAsset.symbol}"
                                        },
                                        color = colors.textPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    if (unitPrice > 0.0) {
                                        Text(
                                            text = if (isArabic) {
                                                "القيمة المضافة: +${isolateLtr(formatUsdCurrency(parsedAddMore * unitPrice))} • القيمة الكلية: ${isolateLtr(formatUsdCurrency(effectiveQuantity * unitPrice))}"
                                            } else {
                                                "Added: +${formatUsdCurrency(parsedAddMore * unitPrice)} • New Value: ${formatUsdCurrency(effectiveQuantity * unitPrice)}"
                                            },
                                            color = EmeraldNeon,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Quantity Input Box
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.glassCard)
                            .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = if (isArabic) "الكمية التي تملكها (${selectedAsset.nameAr})" else "Quantity Held (${selectedAsset.symbol})",
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (quantityText.isEmpty()) {
                                Text(
                                    text = if (isArabic) {
                                        if (selectedAsset.symbol == "XRP") "مثال: 500.0" else "0.00"
                                    } else {
                                        if (selectedAsset.symbol == "XRP") "e.g. 500.0" else "0.00"
                                    },
                                    color = colors.textMuted.copy(alpha = 0.55f),
                                    fontSize = 16.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            BasicTextField(
                                value = quantityText,
                                onValueChange = { raw ->
                                    val filtered = raw.filter { it.isDigit() || it == '.' || it == ',' }
                                    if (filtered.length <= 18) {
                                        quantityText = filtered
                                    }
                                },
                                textStyle = TextStyle(
                                    color = colors.textPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    textDirection = TextDirection.Ltr
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("crypto_quantity_input"),
                                cursorBrush = SolidColor(binanceGold)
                            )
                        }
                        if (isValidQty && unitPrice > 0.0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isArabic) {
                                    "القيمة المقدرة: ${isolateLtr(formatUsdCurrency(effectiveQuantity!! * unitPrice))}"
                                } else {
                                    "Estimated Value: ${formatUsdCurrency(effectiveQuantity!! * unitPrice)}"
                                },
                                color = EmeraldNeon,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Optional label / wallet tag input (helpful when adding same coin multiple times)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.glassCard)
                        .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = if (isArabic) "التسمية / المحفظة (اختياري)" else "Label / Wallet (Optional)",
                        color = colors.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (noteText.isEmpty()) {
                            Text(
                                text = if (isArabic) "مثال: بينانس Spot، تخزين بارد، ادخار..." else "e.g. Binance Spot, Cold Wallet, Savings...",
                                color = colors.textMuted.copy(alpha = 0.55f),
                                fontSize = 13.sp
                            )
                        }
                        BasicTextField(
                            value = noteText,
                            onValueChange = { raw ->
                                if (raw.length <= 36) {
                                    noteText = raw
                                }
                            },
                            textStyle = TextStyle(
                                color = colors.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("crypto_note_input"),
                            cursorBrush = SolidColor(binanceGold)
                        )
                    }
                }

                if (isEditing && onConvertToUsdt != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(14.dp))
                            .background(EmeraldNeon.copy(alpha = 0.14f))
                            .border(1.dp, EmeraldNeon.copy(alpha = 0.42f), RoundedCornerShape(14.dp))
                            .clickable(onClick = onConvertToUsdt)
                            .testTag("dialog_convert_to_usdt_btn")
                            .padding(horizontal = 14.dp, vertical = 11.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CurrencyExchange,
                                    contentDescription = null,
                                    tint = EmeraldNeon,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (isArabic) "تحويل (كلي أو جزئي) إلى USDT" else "Convert (Full or Partial) to USDT",
                                    color = EmeraldNeon,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "USDT",
                                color = colors.textPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
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
                        text = if (isEditing && isAddMoreMode && isValidAddMore) {
                            if (isArabic) "إضافة وحفظ" else "Add & Save"
                        } else {
                            strings.save
                        },
                        enabled = isValidQty,
                        containerColor = if (isEditing && isAddMoreMode) EmeraldNeon else binanceGold,
                        contentColor = Color(0xFF0B0E11),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("save_crypto_holding_btn"),
                        onClick = {
                            if (isValidQty) {
                                onSave(selectedAsset.symbol, effectiveQuantity!!, noteText.trim())
                            }
                        }
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun ConvertCryptoToUsdtDialog(
    holding: CryptoHoldingEntry,
    asset: CryptoAssetSpec,
    prices: Map<String, CryptoMarketPriceEntry>,
    isRefreshingPrices: Boolean,
    secureScreenshots: Boolean,
    onRefreshLivePrice: () -> Unit,
    onDismiss: () -> Unit,
    onConfirmConvert: (convertQuantity: Double) -> Unit
) {
    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors
    val isArabic = strings is ArabicStrings
    val binanceGold = Color(0xFFF0B90B)

    // Automatically fetch live spot price when conversion dialog opens
    LaunchedEffect(holding.id) {
        onRefreshLivePrice()
    }

    val marketEntry = prices[asset.symbol.uppercase(Locale.US)]
    val unitPrice = marketEntry?.priceUsd ?: 0.0
    val changePct = marketEntry?.change24hPercent ?: 0.0

    var convertAmountText by remember(holding.id, holding.quantity) {
        mutableStateOf(formatCryptoQuantityInput(holding.quantity))
    }

    val parsedConvertQty = convertAmountText.replace(',', '.').trim().toDoubleOrNull()
    val balanceTolerance = maxOf(1e-8, holding.quantity * 1e-6)
    val exceedsBalance = parsedConvertQty != null && parsedConvertQty > holding.quantity + balanceTolerance
    val isValidQty = parsedConvertQty != null &&
        parsedConvertQty > 0.0 &&
        parsedConvertQty <= holding.quantity + balanceTolerance &&
        !parsedConvertQty.isNaN() &&
        !parsedConvertQty.isInfinite()

    val clampedConvertQty = if (isValidQty) parsedConvertQty!!.coerceAtMost(holding.quantity) else 0.0
    val sliderFraction = if (holding.quantity > 0.0) {
        (clampedConvertQty / holding.quantity).toFloat().coerceIn(0f, 1f)
    } else 0f
    val remainingQty = (holding.quantity - clampedConvertQty).let {
        if (it <= 1e-9 || it <= holding.quantity * 1e-7) 0.0 else it
    }
    val usdtReceived = clampedConvertQty * unitPrice
    val canConvert = isValidQty && unitPrice > 0.0

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
                    .widthIn(max = 500.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                shape = RoundedCornerShape(24.dp),
                backgroundColor = colors.dialogBackground
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isArabic) "تحويل ${asset.nameAr} إلى USDT" else "Convert ${asset.symbol} to USDT",
                                color = colors.textPrimary,
                                fontSize = 16.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isArabic) {
                                    "تحويل كلي أو جزئي حسب السعر اللحظي في السوق"
                                } else {
                                    "Convert full or partial balance at live spot price"
                                },
                                color = colors.textMuted,
                                fontSize = 11.sp
                            )
                        }
                        IconButton(
                            onClick = onRefreshLivePrice,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("convert_usdt_refresh_price_btn")
                        ) {
                            if (isRefreshingPrices) {
                                CircularProgressIndicator(
                                    color = EmeraldNeon,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = if (isArabic) "تحديث السعر اللحظي" else "Refresh Live Price",
                                    tint = EmeraldNeon
                                )
                            }
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = strings.cancel,
                                tint = colors.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Live Spot Price Pill
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.glassCard)
                            .border(1.dp, EmeraldNeon.copy(alpha = 0.42f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isArabic) "السعر اللحظي الآن" else "Live Spot Price",
                                color = colors.textSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (marketEntry != null) {
                                Text(
                                    text = if (isArabic) "المصدر: ${marketEntry.source}" else "Source: ${marketEntry.source}",
                                    color = colors.textMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (unitPrice > 0.0) {
                                    isolateLtr("1 ${asset.symbol} = ${formatCryptoUnitPrice(unitPrice)}")
                                } else {
                                    if (isArabic) "جاري جلب السعر..." else "Syncing price..."
                                },
                                color = if (unitPrice > 0.0) binanceGold else colors.textMuted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                            if (marketEntry != null) {
                                val pctPositive = changePct >= 0.0
                                Text(
                                    text = isolateLtr(String.format(Locale.US, "%+.2f%% (24h)", changePct)),
                                    color = if (pctPositive) EmeraldNeon else RoseNeon,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Available Coin Balance
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isArabic) "الرصيد المتاح للتحويل" else "Available Balance",
                            color = colors.textSecondary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = isolateLtr("${formatCryptoQuantity(holding.quantity)} ${asset.symbol}"),
                            color = colors.textPrimary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quantity to Convert Input + MAX pill + Slider + Percentage Chips
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.glassCard)
                            .border(
                                width = 1.dp,
                                color = if (exceedsBalance) RoseNeon.copy(alpha = 0.7f) else colors.glassBorderSubtle,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isArabic) "الكمية المراد تحويلها (${asset.symbol})" else "Amount to Convert (${asset.symbol})",
                                color = colors.textSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(binanceGold.copy(alpha = 0.18f))
                                    .clickable {
                                        convertAmountText = formatCryptoQuantityInput(holding.quantity)
                                    }
                                    .testTag("convert_usdt_max_btn")
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isArabic) "الكل MAX" else "MAX",
                                    color = binanceGold,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (convertAmountText.isEmpty()) {
                                Text(
                                    text = "0.00",
                                    color = colors.textMuted.copy(alpha = 0.55f),
                                    fontSize = 16.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            BasicTextField(
                                value = convertAmountText,
                                onValueChange = { raw ->
                                    val filtered = raw.filter { it.isDigit() || it == '.' || it == ',' }
                                    if (filtered.length <= 18) {
                                        convertAmountText = filtered
                                    }
                                },
                                textStyle = TextStyle(
                                    color = if (exceedsBalance) RoseNeon else colors.textPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    textDirection = TextDirection.Ltr
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("convert_usdt_amount_input"),
                                cursorBrush = SolidColor(EmeraldNeon)
                            )
                        }

                        if (exceedsBalance) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isArabic) "الكمية المطلوبة أكبر من الرصيد المتاح" else "Amount exceeds available coin balance",
                                color = RoseNeon,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Slider(
                            value = sliderFraction,
                            onValueChange = { fraction ->
                                val target = if (fraction >= 0.995f) {
                                    holding.quantity
                                } else {
                                    holding.quantity * fraction.toDouble()
                                }
                                convertAmountText = if (target <= 0.0) "" else formatCryptoQuantityInput(target)
                            },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = EmeraldNeon,
                                activeTrackColor = EmeraldNeon,
                                inactiveTrackColor = colors.textMuted.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("convert_usdt_slider")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(0.25, 0.50, 0.75, 1.0).forEach { pct ->
                                val pctLabel = "${(pct * 100).toInt()}%"
                                val isExactPct = holding.quantity > 0.0 &&
                                    abs(clampedConvertQty - holding.quantity * pct) <= holding.quantity * 0.005
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isExactPct) EmeraldNeon.copy(alpha = 0.22f)
                                            else colors.glassSurface.copy(alpha = 0.55f)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isExactPct) EmeraldNeon.copy(alpha = 0.7f) else colors.glassBorderSubtle,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            val qty = if (pct >= 1.0) holding.quantity else holding.quantity * pct
                                            convertAmountText = formatCryptoQuantityInput(qty)
                                        }
                                        .testTag("convert_pct_${(pct * 100).toInt()}")
                                        .padding(vertical = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = isolateLtr(pctLabel),
                                        color = if (isExactPct) EmeraldNeon else colors.textSecondary,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Live Conversion Output Preview
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(EmeraldNeon.copy(alpha = 0.11f))
                            .border(1.dp, EmeraldNeon.copy(alpha = 0.36f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isArabic) "ستحصل على (USDT)" else "You Receive (USDT)",
                                color = colors.textSecondary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (unitPrice > 0.0) {
                                    isolateLtr("+${formatCryptoQuantity(usdtReceived)} USDT")
                                } else "---",
                                color = EmeraldNeon,
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isArabic) "المتبقي من ${asset.nameAr}" else "Remaining ${asset.symbol}",
                                color = colors.textSecondary,
                                fontSize = 11.sp
                            )
                            Text(
                                text = if (remainingQty <= 0.0 && isValidQty) {
                                    if (isArabic) "0 ${asset.symbol} (تحويل كامل)" else "0 ${asset.symbol} (Full)"
                                } else {
                                    isolateLtr("${formatCryptoQuantity(remainingQty)} ${asset.symbol}")
                                },
                                color = colors.textPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
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
                            text = if (isArabic) "تأكيد التحويل" else "Convert",
                            enabled = canConvert,
                            containerColor = EmeraldNeon,
                            contentColor = Color(0xFF0B0E11),
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("confirm_convert_to_usdt_btn"),
                            onClick = {
                                if (canConvert) {
                                    onConfirmConvert(clampedConvertQty)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun formatUsdCurrency(value: Double): String {
    val safe = if (value.isNaN() || value.isInfinite()) 0.0 else value
    return String.format(Locale.US, "$%,.2f", safe)
}

private fun formatCryptoUnitPrice(price: Double): String {
    val absPrice = abs(price)
    return when {
        absPrice >= 100.0 -> String.format(Locale.US, "$%,.2f", price)
        absPrice >= 1.0 -> String.format(Locale.US, "$%.4f", price)
        absPrice >= 0.01 -> String.format(Locale.US, "$%.4f", price)
        else -> String.format(Locale.US, "$%.7f", price)
    }
}

private fun formatCryptoQuantity(qty: Double): String {
    return if (qty % 1.0 == 0.0) {
        String.format(Locale.US, "%,.0f", qty)
    } else if (abs(qty) >= 1.0) {
        String.format(Locale.US, "%,.4f", qty).trimEnd('0').trimEnd('.')
    } else {
        String.format(Locale.US, "%.6f", qty).trimEnd('0').trimEnd('.')
    }
}

private fun formatCryptoQuantityInput(qty: Double): String {
    return if (qty % 1.0 == 0.0) {
        String.format(Locale.US, "%.0f", qty)
    } else {
        String.format(Locale.US, "%.8f", qty).trimEnd('0').trimEnd('.')
    }
}
