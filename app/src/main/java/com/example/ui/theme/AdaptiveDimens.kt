package com.example.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

enum class AdaptiveWindowSize {
    COMPACT,   // Standard smartphones (< 600dp)
    MEDIUM,    // Small tablets, foldables unfolded (600dp - 839dp)
    EXPANDED   // Full-size tablets, desktops, large foldables (>= 840dp)
}

/**
 * App-wide responsive dimensions that automatically adjust based on
 * screen configuration (sw600dp, sw720dp) and scalable font preferences.
 */
data class AppDimensions(
    val screenPaddingHorizontal: Dp,
    val screenPaddingVertical: Dp,
    val contentSpacing: Dp,
    val contentMaxWidth: Dp,
    val cardPadding: Dp,
    val cardCornerRadius: Dp,
    val heroCardPadding: Dp,
    val heroCardCornerRadius: Dp,
    val dialogPadding: Dp,
    val dialogCornerRadius: Dp,
    val dialogMaxWidth: Dp,
    val minTouchTarget: Dp,
    val iconSizeSmall: Dp,
    val iconSizeMedium: Dp,
    val iconSizeLarge: Dp,
    val iconSizeHero: Dp,
    val buttonHeight: Dp,
    val fabSize: Dp,
    val bottomNavHeight: Dp,
    val bottomNavMaxWidth: Dp,
    val gridMinColumnWidth: Dp,
    val gridItemSpacing: Dp,
    val fontSizeDisplay: TextUnit,
    val fontSizeHero: TextUnit,
    val fontSizeTitle: TextUnit,
    val fontSizeSubtitle: TextUnit,
    val fontSizeBody: TextUnit,
    val fontSizeCaption: TextUnit,
    val fontSizeMicro: TextUnit,
    val windowSize: AdaptiveWindowSize,
    val isTablet: Boolean
)

val LocalAppDimens = compositionLocalOf {
    AppDimensions(
        screenPaddingHorizontal = 16.dp,
        screenPaddingVertical = 12.dp,
        contentSpacing = 16.dp,
        contentMaxWidth = 600.dp,
        cardPadding = 16.dp,
        cardCornerRadius = 20.dp,
        heroCardPadding = 20.dp,
        heroCardCornerRadius = 24.dp,
        dialogPadding = 20.dp,
        dialogCornerRadius = 24.dp,
        dialogMaxWidth = 440.dp,
        minTouchTarget = 48.dp,
        iconSizeSmall = 16.dp,
        iconSizeMedium = 22.dp,
        iconSizeLarge = 32.dp,
        iconSizeHero = 48.dp,
        buttonHeight = 52.dp,
        fabSize = 56.dp,
        bottomNavHeight = 72.dp,
        bottomNavMaxWidth = 600.dp,
        gridMinColumnWidth = 300.dp,
        gridItemSpacing = 14.dp,
        fontSizeDisplay = 32.sp,
        fontSizeHero = 28.sp,
        fontSizeTitle = 19.sp,
        fontSizeSubtitle = 14.sp,
        fontSizeBody = 13.sp,
        fontSizeCaption = 11.sp,
        fontSizeMicro = 9.5.sp,
        windowSize = AdaptiveWindowSize.COMPACT,
        isTablet = false
    )
}

/**
 * Access responsive dimensions easily in any Composable:
 * val dimens = AppDimens.current
 */
object AppDimens {
    val current: AppDimensions
        @Composable
        @ReadOnlyComposable
        get() = LocalAppDimens.current
}

/**
 * Dynamically resolves Android dimension resources (dimens.xml, values-sw600dp, values-sw720dp)
 * and calculates current adaptive window classification.
 */
@Composable
fun rememberAppDimensions(): AppDimensions {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val density = LocalDensity.current

    val windowSize = when {
        screenWidth >= 840 -> AdaptiveWindowSize.EXPANDED
        screenWidth >= 600 -> AdaptiveWindowSize.MEDIUM
        else -> AdaptiveWindowSize.COMPACT
    }
    val isTablet = screenWidth >= 600

    // Resolve text sizes from resources using density conversion so sp scaling works automatically
    fun dimenToSp(resId: Int): TextUnit {
        val px = with(density) { dimensionResource(resId).toPx() }
        return with(density) { (px / fontScale).toSp() }
    }

    return AppDimensions(
        screenPaddingHorizontal = dimensionResource(R.dimen.screen_padding_horizontal),
        screenPaddingVertical = dimensionResource(R.dimen.screen_padding_vertical),
        contentSpacing = dimensionResource(R.dimen.content_spacing),
        contentMaxWidth = dimensionResource(R.dimen.content_max_width),
        cardPadding = dimensionResource(R.dimen.card_padding),
        cardCornerRadius = dimensionResource(R.dimen.card_corner_radius),
        heroCardPadding = dimensionResource(R.dimen.hero_card_padding),
        heroCardCornerRadius = dimensionResource(R.dimen.hero_card_corner_radius),
        dialogPadding = dimensionResource(R.dimen.dialog_padding),
        dialogCornerRadius = dimensionResource(R.dimen.dialog_corner_radius),
        dialogMaxWidth = dimensionResource(R.dimen.dialog_max_width),
        minTouchTarget = dimensionResource(R.dimen.min_touch_target),
        iconSizeSmall = dimensionResource(R.dimen.icon_size_small),
        iconSizeMedium = dimensionResource(R.dimen.icon_size_medium),
        iconSizeLarge = dimensionResource(R.dimen.icon_size_large),
        iconSizeHero = dimensionResource(R.dimen.icon_size_hero),
        buttonHeight = dimensionResource(R.dimen.button_height),
        fabSize = dimensionResource(R.dimen.fab_size),
        bottomNavHeight = dimensionResource(R.dimen.bottom_nav_height),
        bottomNavMaxWidth = dimensionResource(R.dimen.bottom_nav_max_width),
        gridMinColumnWidth = dimensionResource(R.dimen.grid_min_column_width),
        gridItemSpacing = dimensionResource(R.dimen.grid_item_spacing),
        fontSizeDisplay = dimenToSp(R.dimen.font_size_display),
        fontSizeHero = dimenToSp(R.dimen.font_size_hero),
        fontSizeTitle = dimenToSp(R.dimen.font_size_title),
        fontSizeSubtitle = dimenToSp(R.dimen.font_size_subtitle),
        fontSizeBody = dimenToSp(R.dimen.font_size_body),
        fontSizeCaption = dimenToSp(R.dimen.font_size_caption),
        fontSizeMicro = dimenToSp(R.dimen.font_size_micro),
        windowSize = windowSize,
        isTablet = isTablet
    )
}

/**
 * Modifier to keep tablet content horizontally constrained and centered
 * without stretching indefinitely across wide monitors or tablet screens.
 */
fun Modifier.adaptiveContent(maxWidth: Dp? = null): Modifier = this.then(
    Modifier
        .fillMaxWidth()
        .widthIn(max = maxWidth ?: 900.dp)
)
