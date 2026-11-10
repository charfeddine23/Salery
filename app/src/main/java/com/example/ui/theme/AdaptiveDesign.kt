package com.example.ui.theme

import android.content.res.Configuration
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
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

/**
 * Window & device form factor classification for adaptive Jetpack Compose layouts.
 */
data class AdaptiveLayoutInfo(
    val screenWidthDp: Int,
    val screenHeightDp: Int,
    val isTablet: Boolean,
    val isLargeTablet: Boolean,
    val isLandscape: Boolean,
    val columns: Int,
    val useNavRail: Boolean
)

/**
 * Responsive dimension tokens automatically derived from dimens.xml (values, values-sw600dp, values-sw720dp).
 */
data class AdaptiveDimens(
    val screenPaddingH: Dp,
    val screenPaddingV: Dp,
    val cardPaddingSmall: Dp,
    val cardPadding: Dp,
    val cardPaddingLarge: Dp,
    val spacingXs: Dp,
    val spacingSm: Dp,
    val spacingMd: Dp,
    val spacingLg: Dp,
    val spacingXl: Dp,
    val spacingXxl: Dp,
    val cornerRadiusXs: Dp,
    val cornerRadiusSm: Dp,
    val cornerRadiusMd: Dp,
    val cornerRadiusLg: Dp,
    val cornerRadiusXl: Dp,
    val iconSizeXs: Dp,
    val iconSizeSm: Dp,
    val iconSizeMd: Dp,
    val iconSizeLg: Dp,
    val iconSizeXl: Dp,
    val buttonHeight: Dp,
    val fabSize: Dp,
    val appBarHeight: Dp,
    val bottomBarHeight: Dp,
    val dialogMaxWidth: Dp,
    val contentMaxWidth: Dp
)

/**
 * Scalable typography sizes (sp) backed by responsive Android dimension resources.
 */
data class AdaptiveTypography(
    val caption: TextUnit,
    val bodyXs: TextUnit,
    val bodySm: TextUnit,
    val body: TextUnit,
    val bodyLg: TextUnit,
    val titleSm: TextUnit,
    val title: TextUnit,
    val titleLg: TextUnit,
    val headline: TextUnit,
    val display: TextUnit,
    val hero: TextUnit
) {
    val titleLarge: TextUnit get() = titleLg
}

val LocalAdaptiveLayoutInfo = compositionLocalOf {
    AdaptiveLayoutInfo(
        screenWidthDp = 360,
        screenHeightDp = 640,
        isTablet = false,
        isLargeTablet = false,
        isLandscape = false,
        columns = 1,
        useNavRail = false
    )
}

val LocalAdaptiveDimens = compositionLocalOf {
    AdaptiveDimens(
        screenPaddingH = 16.dp,
        screenPaddingV = 12.dp,
        cardPaddingSmall = 12.dp,
        cardPadding = 16.dp,
        cardPaddingLarge = 20.dp,
        spacingXs = 4.dp,
        spacingSm = 8.dp,
        spacingMd = 12.dp,
        spacingLg = 16.dp,
        spacingXl = 20.dp,
        spacingXxl = 28.dp,
        cornerRadiusXs = 6.dp,
        cornerRadiusSm = 10.dp,
        cornerRadiusMd = 14.dp,
        cornerRadiusLg = 20.dp,
        cornerRadiusXl = 24.dp,
        iconSizeXs = 14.dp,
        iconSizeSm = 18.dp,
        iconSizeMd = 22.dp,
        iconSizeLg = 26.dp,
        iconSizeXl = 34.dp,
        buttonHeight = 48.dp,
        fabSize = 56.dp,
        appBarHeight = 64.dp,
        bottomBarHeight = 68.dp,
        dialogMaxWidth = 440.dp,
        contentMaxWidth = 600.dp
    )
}

val LocalAdaptiveTypography = compositionLocalOf {
    AdaptiveTypography(
        caption = 10.sp,
        bodyXs = 11.sp,
        bodySm = 12.sp,
        body = 13.sp,
        bodyLg = 15.sp,
        titleSm = 16.sp,
        title = 18.sp,
        titleLg = 20.sp,
        headline = 24.sp,
        display = 28.sp,
        hero = 32.sp
    )
}

@Composable
fun rememberAdaptiveLayoutInfo(): AdaptiveLayoutInfo {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isTablet = configuration.smallestScreenWidthDp >= 600 || screenWidth >= 600
    val isLargeTablet = configuration.smallestScreenWidthDp >= 720 || screenWidth >= 840

    val columns = when {
        isLargeTablet -> 2
        isTablet || (isLandscape && screenWidth >= 560) -> 2
        else -> 1
    }

    val useNavRail = isTablet && (isLandscape || screenWidth >= 720)

    return remember(screenWidth, screenHeight, isLandscape, isTablet, isLargeTablet) {
        AdaptiveLayoutInfo(
            screenWidthDp = screenWidth,
            screenHeightDp = screenHeight,
            isTablet = isTablet,
            isLargeTablet = isLargeTablet,
            isLandscape = isLandscape,
            columns = columns,
            useNavRail = useNavRail
        )
    }
}

@Composable
fun rememberAdaptiveDimens(): AdaptiveDimens {
    return AdaptiveDimens(
        screenPaddingH = dimensionResource(R.dimen.screen_padding_horizontal),
        screenPaddingV = dimensionResource(R.dimen.screen_padding_vertical),
        cardPaddingSmall = dimensionResource(R.dimen.card_padding_small),
        cardPadding = dimensionResource(R.dimen.card_padding),
        cardPaddingLarge = dimensionResource(R.dimen.card_padding_large),
        spacingXs = dimensionResource(R.dimen.spacing_xs),
        spacingSm = dimensionResource(R.dimen.spacing_sm),
        spacingMd = dimensionResource(R.dimen.spacing_md),
        spacingLg = dimensionResource(R.dimen.spacing_lg),
        spacingXl = dimensionResource(R.dimen.spacing_xl),
        spacingXxl = dimensionResource(R.dimen.spacing_xxl),
        cornerRadiusXs = dimensionResource(R.dimen.corner_radius_xs),
        cornerRadiusSm = dimensionResource(R.dimen.corner_radius_sm),
        cornerRadiusMd = dimensionResource(R.dimen.corner_radius_md),
        cornerRadiusLg = dimensionResource(R.dimen.corner_radius_lg),
        cornerRadiusXl = dimensionResource(R.dimen.corner_radius_xl),
        iconSizeXs = dimensionResource(R.dimen.icon_size_xs),
        iconSizeSm = dimensionResource(R.dimen.icon_size_sm),
        iconSizeMd = dimensionResource(R.dimen.icon_size_md),
        iconSizeLg = dimensionResource(R.dimen.icon_size_lg),
        iconSizeXl = dimensionResource(R.dimen.icon_size_xl),
        buttonHeight = dimensionResource(R.dimen.button_height),
        fabSize = dimensionResource(R.dimen.fab_size),
        appBarHeight = dimensionResource(R.dimen.app_bar_height),
        bottomBarHeight = dimensionResource(R.dimen.bottom_bar_height),
        dialogMaxWidth = dimensionResource(R.dimen.dialog_max_width),
        contentMaxWidth = dimensionResource(R.dimen.content_max_width)
    )
}

@Composable
fun rememberAdaptiveTypography(): AdaptiveTypography {
    return AdaptiveTypography(
        caption = dimensionResource(R.dimen.text_caption).value.sp,
        bodyXs = dimensionResource(R.dimen.text_body_xs).value.sp,
        bodySm = dimensionResource(R.dimen.text_body_sm).value.sp,
        body = dimensionResource(R.dimen.text_body).value.sp,
        bodyLg = dimensionResource(R.dimen.text_body_lg).value.sp,
        titleSm = dimensionResource(R.dimen.text_title_sm).value.sp,
        title = dimensionResource(R.dimen.text_title).value.sp,
        titleLg = dimensionResource(R.dimen.text_title_lg).value.sp,
        headline = dimensionResource(R.dimen.text_headline).value.sp,
        display = dimensionResource(R.dimen.text_display).value.sp,
        hero = dimensionResource(R.dimen.text_hero).value.sp
    )
}

/**
 * Top-level provider of responsive layout tokens and typography.
 */
@Composable
fun ProvideAdaptiveDesign(content: @Composable () -> Unit) {
    val layoutInfo = rememberAdaptiveLayoutInfo()
    val dimens = rememberAdaptiveDimens()
    val typography = rememberAdaptiveTypography()

    CompositionLocalProvider(
        LocalAdaptiveLayoutInfo provides layoutInfo,
        LocalAdaptiveDimens provides dimens,
        LocalAdaptiveTypography provides typography,
        content = content
    )
}

/**
 * Centers content and constrains maximum width on large screens to avoid awkward stretching.
 */
fun Modifier.adaptiveContentWidth(maxWidth: Dp = 1200.dp): Modifier = this
    .widthIn(max = maxWidth)
    .wrapContentWidth(Alignment.CenterHorizontally)
