package com.example.ui.components

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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
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

/**
 * Clickable "Assets: Gold & Crypto" entry card displayed on Assets & Saving.
 * Tapping navigates into the dedicated AssetsManagementScreen page where users add/manage assets.
 */
@Composable
fun AssetsLauncherCard(
    goldHoldings: List<GoldAssetHolding>,
    squareSnapshot: SquareMarketSnapshot?,
    binanceHoldings: List<CryptoHoldingEntry>,
    binancePrices: Map<String, CryptoMarketPriceEntry>,
    isArabic: Boolean,
    isStealth: Boolean,
    onOpenAssetsScreen: () -> Unit,
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
        GoldValuationEngine.convertUsdToSquareParallelDzd(cryptoTotalUsd, squareSnapshot) ?: 0.0
    }
    val combinedAssetsDzd = goldSummary.totalValueDzdParallel + cryptoDzdApprox

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("assets_card_navigate_button"),
        shape = RoundedCornerShape(dimens.cornerRadiusXl),
        backgroundColor = colors.glassCard,
        cornerRadius = dimens.cornerRadiusXl,
        borderColor = AmberNeon.copy(alpha = 0.42f),
        onClick = onOpenAssetsScreen
    ) {
        Column(modifier = Modifier.padding(dimens.cardPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
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

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isArabic) "الأصول: الذهب والكريبتو" else "Assets: Gold & Crypto",
                        color = colors.textPrimary,
                        fontSize = typo.titleSm,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isArabic) "اضغط لفتح صفحة إضافة وإدارة الأصول"
                        else "Tap to open & manage your Gold & Crypto",
                        color = colors.textMuted,
                        fontSize = typo.caption,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(AmberNeon.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = if (isArabic) "فتح الأصول" else "Open Assets",
                        tint = AmberNeon,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(dimens.spacingSm))

            // Concise Gold + Read-only Crypto summary pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimens.spacingSm)
            ) {
                // Gold mini badge
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(dimens.cornerRadiusMd))
                        .background(colors.glassSurface)
                        .border(1.dp, AmberNeon.copy(alpha = 0.28f), RoundedCornerShape(dimens.cornerRadiusMd))
                        .padding(dimens.cardPaddingSmall)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = AmberNeon,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isArabic) "الذهب (Square)" else "Gold (Square)",
                            color = colors.textMuted,
                            fontSize = typo.caption,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    val gramsFmt = String.format(Locale.US, "%.2f", goldSummary.totalGrams)
                        .trimEnd('0').trimEnd('.')
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(
                            text = if (goldHoldings.isEmpty()) {
                                if (isArabic) "0 غ • أضف ذهب" else "0g • Tap to add"
                            } else if (goldSummary.hasLivePrices) {
                                formatDzd(goldSummary.totalValueDzdParallel, strings.currencyCode, isStealth)
                            } else {
                                "${gramsFmt}g"
                            },
                            color = if (goldHoldings.isEmpty()) colors.textSecondary else AmberNeon,
                            fontSize = typo.bodySm,
                            fontWeight = FontWeight.ExtraBold,
                            style = tnumStyle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Crypto mini badge (display-only)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(dimens.cornerRadiusMd))
                        .background(colors.glassSurface)
                        .border(1.dp, CyanNeon.copy(alpha = 0.28f), RoundedCornerShape(dimens.cornerRadiusMd))
                        .padding(dimens.cardPaddingSmall)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CurrencyBitcoin,
                            contentDescription = null,
                            tint = CyanNeon,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isArabic) "كريبتو • للعرض" else "Crypto • View",
                            color = colors.textMuted,
                            fontSize = typo.caption,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        val usdStr = if (isStealth) "$ ••••" else String.format(Locale.US, "$%,.2f", cryptoTotalUsd)
                        Text(
                            text = usdStr,
                            color = CyanNeon,
                            fontSize = typo.bodySm,
                            fontWeight = FontWeight.ExtraBold,
                            style = tnumStyle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (combinedAssetsDzd > 0.0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isArabic) "إجمالي الأصول المقدّر:" else "Estimated Assets Total:",
                        color = colors.textSecondary,
                        fontSize = typo.caption
                    )
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(
                            text = formatDzd(combinedAssetsDzd, strings.currencyCode, isStealth),
                            color = EmeraldNeon,
                            fontSize = typo.bodySm,
                            fontWeight = FontWeight.ExtraBold,
                            style = tnumStyle
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CryptoReadOnlySummaryCard(
    totalUsd: Double,
    approxDzd: Double?,
    holdingsCount: Int,
    lastSyncMillis: Long,
    isArabic: Boolean,
    isStealth: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LiquidGlassTheme.colors
    val strings = AppStrings.current
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current
    val tnumStyle = remember { TextStyle(fontFeatureSettings = "tnum") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimens.cornerRadiusLg))
            .background(colors.glassSurface.copy(alpha = 0.78f))
            .border(1.dp, CyanNeon.copy(alpha = 0.32f), RoundedCornerShape(dimens.cornerRadiusLg))
            .padding(dimens.cardPadding)
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
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyanNeon.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CurrencyBitcoin,
                        contentDescription = null,
                        tint = CyanNeon,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (isArabic) "إجمالي العملات الرقمية" else "Binance Crypto Total",
                        color = colors.textPrimary,
                        fontSize = typo.titleSm,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatAssetsSyncTimeCompact(lastSyncMillis, isArabic),
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

        Spacer(modifier = Modifier.height(12.dp))

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
fun GoldHoldingRowItem(
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

fun formatAssetsSyncTimeCompact(millis: Long, isArabic: Boolean): String {
    if (millis <= 0L) {
        return if (isArabic) "لم تتم المزامنة بعد" else "Not synced yet"
    }
    val fmt = SimpleDateFormat("dd MMM HH:mm", if (isArabic) Locale("ar") else Locale.US)
    return fmt.format(Date(millis))
}
