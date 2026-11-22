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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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

@Composable
fun BinanceWalletScreen(
    userSettings: UserSettings,
    holdings: List<CryptoHoldingEntry>,
    prices: Map<String, CryptoMarketPriceEntry>,
    isRefreshingPrices: Boolean,
    priceError: String?,
    onBackClick: () -> Unit,
    onRefreshPrices: (force: Boolean) -> Unit,
    onUpsertHolding: (symbol: String, quantity: Double) -> Unit,
    onRemoveHolding: (symbol: String) -> Unit,
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

    var dialogCoinSymbol by remember { mutableStateOf<String?>(null) }
    var refreshSpins by remember { mutableIntStateOf(0) }
    val spinAngle by animateFloatAsState(
        targetValue = refreshSpins * 360f,
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "binance_price_refresh_spin"
    )

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

    if (dialogCoinSymbol != null) {
        val currentSymbol = dialogCoinSymbol!!
        val existingQty = holdings.firstOrNull {
            it.symbol.equals(currentSymbol, ignoreCase = true)
        }?.quantity ?: 0.0

        AddOrEditCryptoDialog(
            initialSymbol = currentSymbol,
            initialQuantity = existingQty,
            prices = prices,
            secureScreenshots = userSettings.screenshotProtectionEnabled,
            onDismiss = { dialogCoinSymbol = null },
            onSave = { sym, qty ->
                onUpsertHolding(sym, qty)
                dialogCoinSymbol = null
            },
            onDelete = if (existingQty > 0.0) {
                {
                    onRemoveHolding(currentSymbol)
                    dialogCoinSymbol = null
                }
            } else null
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
                    .clickable { dialogCoinSymbol = "XRP" }
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
                    val owned = holdings.firstOrNull { it.symbol.equals(asset.symbol, ignoreCase = true) }
                    val accent = Color(asset.accentHex)
                    GlassCard(
                        modifier = Modifier
                            .width(136.dp)
                            .testTag("crypto_quick_card_${asset.symbol.lowercase()}"),
                        backgroundColor = colors.glassCard,
                        borderColor = if (owned != null) accent.copy(alpha = 0.7f) else colors.glassBorderSubtle,
                        shape = RoundedCornerShape(16.dp),
                        cornerRadius = 16.dp,
                        onClick = { dialogCoinSymbol = asset.symbol }
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
                                            if (owned != null) EmeraldNeon.copy(alpha = 0.16f)
                                            else binanceGold.copy(alpha = 0.16f)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (owned != null) {
                                            if (isArabic) "مملوكة" else "HELD"
                                        } else {
                                            "+"
                                        },
                                        color = if (owned != null) EmeraldNeon else binanceGold,
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
                            onClick = { dialogCoinSymbol = "XRP" }
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
                    items(holdings, key = { it.symbol }) { holding ->
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

                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("crypto_holding_row_${spec.symbol.lowercase()}"),
                            backgroundColor = colors.glassCard,
                            borderColor = accent.copy(alpha = 0.40f),
                            shape = RoundedCornerShape(18.dp),
                            onClick = { dialogCoinSymbol = spec.symbol }
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
                                        Text(
                                            text = if (isArabic) spec.nameAr else spec.nameEn,
                                            color = colors.textPrimary,
                                            fontSize = typo.body,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
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
                                            onClick = { dialogCoinSymbol = spec.symbol },
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = if (isArabic) "تعديل الكمية" else "Edit Quantity",
                                                tint = CyanNeon,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { onRemoveHolding(spec.symbol) },
                                            modifier = Modifier
                                                .size(40.dp)
                                                .testTag("delete_crypto_${spec.symbol.lowercase()}")
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
                    onClick = { dialogCoinSymbol = "XRP" }
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
            items(holdings, key = { it.symbol }) { holding ->
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

                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("crypto_holding_row_${spec.symbol.lowercase()}"),
                    backgroundColor = colors.glassCard,
                    borderColor = accent.copy(alpha = 0.40f),
                    shape = RoundedCornerShape(18.dp),
                    onClick = { dialogCoinSymbol = spec.symbol }
                ) {
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isArabic) spec.nameAr else spec.nameEn,
                                        color = colors.textPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
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
                                    onClick = { dialogCoinSymbol = spec.symbol },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = if (isArabic) "تعديل الكمية" else "Edit Quantity",
                                        tint = CyanNeon,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onRemoveHolding(spec.symbol) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("delete_crypto_${spec.symbol.lowercase()}")
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

            item {
                Spacer(modifier = Modifier.height(dimens.spacingXl))
            }
        }
    }
}

@Composable
private fun AddOrEditCryptoDialog(
    initialSymbol: String,
    initialQuantity: Double,
    prices: Map<String, CryptoMarketPriceEntry>,
    secureScreenshots: Boolean,
    onDismiss: () -> Unit,
    onSave: (symbol: String, quantity: Double) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors
    val isArabic = strings is ArabicStrings
    val binanceGold = Color(0xFFF0B90B)

    var selectedSymbol by remember { mutableStateOf(initialSymbol.uppercase(Locale.US)) }
    var quantityText by remember(selectedSymbol) {
        mutableStateOf(
            if (selectedSymbol.equals(initialSymbol, ignoreCase = true) && initialQuantity > 0.0) {
                formatCryptoQuantityInput(initialQuantity)
            } else {
                ""
            }
        )
    }

    val selectedAsset = remember(selectedSymbol) {
        BinanceCryptoCatalog.findBySymbol(selectedSymbol) ?: BinanceCryptoCatalog.majorAssets.first()
    }
    val unitPrice = remember(selectedSymbol, prices) {
        if (selectedAsset.isStablecoin) prices[selectedAsset.symbol]?.priceUsd ?: 1.0
        else prices[selectedAsset.symbol]?.priceUsd ?: 0.0
    }

    val parsedQuantity = quantityText.replace(',', '.').trim().toDoubleOrNull()
    val isValidQty = parsedQuantity != null && parsedQuantity > 0.0 && !parsedQuantity.isNaN() && !parsedQuantity.isInfinite()

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
                            text = if (isArabic) "إضافة / تعديل عملة رقمية" else "Add / Edit Cryptocurrency",
                            color = colors.textPrimary,
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isArabic) "أدخل الكمية المملوكة للريبل أو أي عملة رقمية أساسية" else "Enter your coin balance (XRP & major assets)",
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

                Spacer(modifier = Modifier.height(14.dp))

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

                Spacer(modifier = Modifier.height(12.dp))

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
                                "القيمة المقدرة: ${isolateLtr(formatUsdCurrency(parsedQuantity!! * unitPrice))}"
                            } else {
                                "Estimated Value: ${formatUsdCurrency(parsedQuantity!! * unitPrice)}"
                            },
                            color = EmeraldNeon,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
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
                        text = strings.save,
                        enabled = isValidQty,
                        containerColor = binanceGold,
                        contentColor = Color(0xFF0B0E11),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("save_crypto_holding_btn"),
                        onClick = {
                            if (isValidQty) {
                                onSave(selectedAsset.symbol, parsedQuantity!!)
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
        String.format(Locale.US, "%.6f", qty).trimEnd('0').trimEnd('.')
    }
}
