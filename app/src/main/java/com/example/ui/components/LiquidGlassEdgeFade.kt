package com.example.ui.components

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LiquidGlassTheme

val LocalEdgeFadeOuterPadding = compositionLocalOf { PaddingValues(0.dp) }

@Composable
fun edgeFadeVerticalContentPadding(
    extraTop: Dp = 0.dp,
    extraBottom: Dp = 0.dp
): PaddingValues {
    val outer = LocalEdgeFadeOuterPadding.current
    return PaddingValues(
        top = outer.calculateTopPadding() + extraTop,
        bottom = outer.calculateBottomPadding() + extraBottom
    )
}

/**
 * Hosts full-bleed scrollable content and renders progressive Top & Bottom
 * "Fade-to-Blur Liquid Glass" edge veils behind frameless top/bottom navigation bars.
 *
 * Uses a single-pass [GraphicsLayer] capture synchronized via [NestedScrollConnection]
 * and optional [scrollVersionProvider] in the Draw phase (zero recomposition overhead
 * during scroll), combined with hardware RenderEffect blur (API 31+) masked via
 * [BlendMode.DstIn] and a smooth frosted liquid-glass mist gradient.
 */
@Composable
fun FadeToBlurLiquidGlassHost(
    topEdgeHeight: Dp,
    bottomEdgeHeight: Dp,
    scrollVersionProvider: (() -> Int)? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val contentLayer = rememberGraphicsLayer()
    val colors = LiquidGlassTheme.colors
    val isDark = colors.isDark
    var nestedScrollTick by remember { mutableIntStateOf(0) }

    val scrollTracker = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (consumed.y != 0f || consumed.x != 0f) {
                    nestedScrollTick++
                }
                return Offset.Zero
            }
        }
    }

    val effectiveScrollVersion: () -> Int = {
        (scrollVersionProvider?.invoke() ?: 0) + nestedScrollTick
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollTracker)
    ) {
        // 1. Sharp full-bleed scrollable content recorded into shared GraphicsLayer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    effectiveScrollVersion()
                    contentLayer.record {
                        this@drawWithContent.drawContent()
                    }
                    drawLayer(contentLayer)
                }
        ) {
            content()
        }

        // 2. Top Fade-to-Blur Liquid Glass veil (behind frameless top bar)
        if (topEdgeHeight > 0.dp) {
            val topBandHeight = topEdgeHeight + 28.dp
            TopLiquidGlassFadeVeil(
                height = topBandHeight,
                contentLayer = contentLayer,
                scrollVersionProvider = effectiveScrollVersion,
                bgStart = colors.bgStart,
                bgCenter = colors.bgCenter,
                glassSurface = colors.glassSurface,
                accentTint = colors.primaryNeon,
                isDark = isDark,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .testTag("cards_hub_top_liquid_fade")
            )
        }

        // 3. Bottom Fade-to-Blur Liquid Glass veil (behind bottom dock)
        if (bottomEdgeHeight > 0.dp) {
            val bottomBandHeight = bottomEdgeHeight + 34.dp
            BottomLiquidGlassFadeVeil(
                height = bottomBandHeight,
                contentLayer = contentLayer,
                scrollVersionProvider = effectiveScrollVersion,
                bgEnd = colors.bgEnd,
                glassSurface = colors.glassSurface,
                accentTint = colors.secondaryNeon,
                isDark = isDark,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .testTag("cards_hub_bottom_liquid_fade")
            )
        }
    }
}

@Composable
private fun TopLiquidGlassFadeVeil(
    height: Dp,
    contentLayer: GraphicsLayer,
    scrollVersionProvider: () -> Int,
    bgStart: Color,
    bgCenter: Color,
    glassSurface: Color,
    accentTint: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier
            .fillMaxSize()
            .blur(radius = 22.dp, edgeTreatment = BlurredEdgeTreatment.Rectangle)
    } else {
        Modifier.fillMaxSize()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clipToBounds()
    ) {
        // Real progressively masked backdrop blur of scrolling content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to Color.Black,
                                0.56f to Color.Black.copy(alpha = 0.94f),
                                0.78f to Color.Black.copy(alpha = 0.52f),
                                0.92f to Color.Black.copy(alpha = 0.14f),
                                1.00f to Color.Transparent
                            )
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
        ) {
            Canvas(modifier = blurModifier) {
                scrollVersionProvider()
                if (contentLayer.size.width > 0 && contentLayer.size.height > 0) {
                    drawLayer(contentLayer)
                }
            }
        }

        // Frosted liquid-glass mist + subtle specular refraction sheen (no hard borders)
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.00f to bgStart.copy(alpha = if (isDark) 0.86f else 0.90f),
                        0.50f to glassSurface.copy(alpha = if (isDark) 0.58f else 0.66f),
                        0.78f to bgCenter.copy(alpha = if (isDark) 0.22f else 0.26f),
                        1.00f to Color.Transparent
                    )
                )
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.00f to accentTint.copy(alpha = if (isDark) 0.08f else 0.05f),
                        0.58f to accentTint.copy(alpha = if (isDark) 0.03f else 0.02f),
                        1.00f to Color.Transparent
                    )
                )
            )
        }
    }
}

@Composable
private fun BottomLiquidGlassFadeVeil(
    height: Dp,
    contentLayer: GraphicsLayer,
    scrollVersionProvider: () -> Int,
    bgEnd: Color,
    glassSurface: Color,
    accentTint: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier
            .fillMaxSize()
            .blur(radius = 22.dp, edgeTreatment = BlurredEdgeTreatment.Rectangle)
    } else {
        Modifier.fillMaxSize()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clipToBounds()
    ) {
        // Real progressively masked backdrop blur aligned to the bottom slice of contentLayer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to Color.Transparent,
                                0.08f to Color.Black.copy(alpha = 0.14f),
                                0.24f to Color.Black.copy(alpha = 0.52f),
                                0.46f to Color.Black.copy(alpha = 0.94f),
                                1.00f to Color.Black
                            )
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
        ) {
            Canvas(modifier = blurModifier) {
                scrollVersionProvider()
                val layerHeight = contentLayer.size.height.toFloat()
                if (contentLayer.size.width > 0 && layerHeight > 0f) {
                    val yOffset = size.height - layerHeight
                    translate(left = 0f, top = yOffset) {
                        drawLayer(contentLayer)
                    }
                }
            }
        }

        // Frosted liquid-glass mist + subtle specular refraction sheen (no hard borders)
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.00f to Color.Transparent,
                        0.22f to bgEnd.copy(alpha = if (isDark) 0.22f else 0.26f),
                        0.48f to glassSurface.copy(alpha = if (isDark) 0.62f else 0.70f),
                        1.00f to bgEnd.copy(alpha = if (isDark) 0.92f else 0.95f)
                    )
                )
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.00f to Color.Transparent,
                        0.42f to accentTint.copy(alpha = if (isDark) 0.03f else 0.02f),
                        1.00f to accentTint.copy(alpha = if (isDark) 0.08f else 0.05f)
                    )
                )
            )
        }
    }
}
