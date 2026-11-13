package com.example.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class LiquidGlassColors(
    val isDark: Boolean,
    val designStyle: String = "LIQUID_GLASS",
    val bgStart: Color,
    val bgCenter: Color,
    val bgEnd: Color,
    val glassSurface: Color,
    val glassCard: Color,
    val dialogBackground: Color,
    val glassBorder: Color,
    val glassBorderSubtle: Color,
    val primaryNeon: Color,
    val secondaryNeon: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val successColor: Color,
    val warningColor: Color,
    val errorColor: Color,
    val glassHighlightBrush: Brush
) {
    val isNeumorphic: Boolean
        get() = designStyle == "NEUMORPHISM"
}

val DarkLiquidGlassColors = LiquidGlassColors(
    isDark = true,
    designStyle = "LIQUID_GLASS",
    bgStart = DarkBgStart,
    bgCenter = DarkBgCenter,
    bgEnd = DarkBgEnd,
    glassSurface = GlassDarkSurface,
    glassCard = GlassDarkCard,
    dialogBackground = Color(0xF70E1726), // 97% solid opaque dark glass
    glassBorder = GlassDarkBorder,
    glassBorderSubtle = GlassDarkBorderSubtle,
    primaryNeon = CyanNeon,
    secondaryNeon = BlueNeon,
    textPrimary = TextDarkPrimary,
    textSecondary = TextDarkSecondary,
    textMuted = TextDarkMuted,
    successColor = EmeraldNeon,
    warningColor = AmberNeon,
    errorColor = RoseNeon,
    glassHighlightBrush = GlassHighlightGradient
)

val LightLiquidGlassColors = LiquidGlassColors(
    isDark = false,
    designStyle = "LIQUID_GLASS",
    bgStart = LightBgStart,
    bgCenter = LightBgCenter,
    bgEnd = LightBgEnd,
    glassSurface = GlassLightSurface,
    glassCard = GlassLightCard,
    dialogBackground = Color(0xFAFFFFFF), // 98% solid crystal white (no background bleed-through)
    glassBorder = GlassLightBorder,
    glassBorderSubtle = GlassLightBorderSubtle,
    primaryNeon = Color(0xFF0284C7),
    secondaryNeon = Color(0xFF2563EB),
    textPrimary = TextLightPrimary,
    textSecondary = TextLightSecondary,
    textMuted = TextLightMuted,
    successColor = Color(0xFF059669),
    warningColor = Color(0xFFD97706),
    errorColor = Color(0xFFDC2626),
    glassHighlightBrush = GlassHighlightGradient
)

val DarkNeumorphicColors = LiquidGlassColors(
    isDark = true,
    designStyle = "NEUMORPHISM",
    bgStart = NeuDarkBackground,
    bgCenter = NeuDarkBackground,
    bgEnd = NeuDarkBackground,
    glassSurface = Color(0xFF181C24), // Recessed inner widget surface
    glassCard = NeuDarkCard,          // 0xFF202530 extruded card surface
    dialogBackground = NeuDarkCard,
    glassBorder = NeuDarkHighlight.copy(alpha = 0.5f),
    glassBorderSubtle = NeuDarkHighlight.copy(alpha = 0.25f),
    primaryNeon = NeuRoyalBlueDark,
    secondaryNeon = Color(0xFF60A5FA),
    textPrimary = Color(0xFFF1F5F9),
    textSecondary = Color(0xFF94A3B8),
    textMuted = Color(0xFF64748B),
    successColor = NeuVibrantGreenDark,
    warningColor = Color(0xFFF59E0B),
    errorColor = NeuBurntCoralDark,
    glassHighlightBrush = GlassHighlightGradient
)

val LightNeumorphicColors = LiquidGlassColors(
    isDark = false,
    designStyle = "NEUMORPHISM",
    bgStart = NeuLightBackground,
    bgCenter = NeuLightBackground,
    bgEnd = NeuLightBackground,
    glassSurface = NeuLightCard,
    glassCard = NeuLightCard,
    dialogBackground = NeuLightCard,
    glassBorder = NeuLightDarkShadow.copy(alpha = 0.4f),
    glassBorderSubtle = NeuLightDarkShadow.copy(alpha = 0.25f),
    primaryNeon = NeuRoyalBlue,
    secondaryNeon = Color(0xFF1D4ED8),
    textPrimary = Color(0xFF1E293B),
    textSecondary = Color(0xFF475569),
    textMuted = Color(0xFF64748B),
    successColor = NeuVibrantGreen,
    warningColor = Color(0xFFD97706),
    errorColor = NeuBurntCoral,
    glassHighlightBrush = GlassHighlightGradient
)

val LocalLiquidGlass = staticCompositionLocalOf { DarkLiquidGlassColors }
val LocalBreathingGlowEnabled = compositionLocalOf { true }

object LiquidGlassTheme {
    val colors: LiquidGlassColors
        @Composable
        get() = LocalLiquidGlass.current
}

/**
 * Custom Modifier implementing the Liquid Glass v3 'breathing' glow effect for Card containers.
 * Smoothly animates both the ambient background glow alpha and scale, plus an ultra-subtle
 * container scale pulse, and cleanly disables itself when [LocalBreathingGlowEnabled] is false.
 */
fun Modifier.breathingGlow(
    enabled: Boolean = true,
    glowColor: Color = Color.Unspecified,
    secondaryGlowColor: Color = Color.Unspecified,
    cornerRadius: Dp = 20.dp,
    minAlpha: Float = 0.06f,
    maxAlpha: Float = 0.22f,
    minScale: Float = 0.985f,
    maxScale: Float = 1.035f,
    animateContainerScale: Boolean = true,
    durationMillis: Int = 2600
): Modifier = composed {
    val colors = LiquidGlassTheme.colors
    val globalEnabled = LocalBreathingGlowEnabled.current
    if (!enabled || !globalEnabled) return@composed this

    val infiniteTransition = rememberInfiniteTransition(label = "liquid_glass_breathing")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = minAlpha,
        targetValue = maxAlpha,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing_alpha"
    )
    val glowScale by infiniteTransition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing_scale"
    )

    val primaryCol = if (glowColor != Color.Unspecified) glowColor else colors.primaryNeon
    val secondaryCol = if (secondaryGlowColor != Color.Unspecified) secondaryGlowColor else colors.secondaryNeon

    val scaleRange = (maxScale - minScale).coerceAtLeast(0.001f)
    val normalizedPhase = ((glowScale - minScale) / scaleRange).coerceIn(0f, 1f)
    val containerScale = if (animateContainerScale) 0.997f + normalizedPhase * 0.006f else 1f

    this
        .graphicsLayer {
            scaleX = containerScale
            scaleY = containerScale
        }
        .drawBehind {
            val expandPx = 10.dp.toPx() * glowScale
            val radiusPx = (cornerRadius + 6.dp).toPx()
            val maxDim = maxOf(size.width, size.height)

            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryCol.copy(alpha = glowAlpha),
                        secondaryCol.copy(alpha = glowAlpha * 0.45f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.5f),
                    radius = (maxDim * 0.72f * glowScale).coerceAtLeast(1f)
                ),
                topLeft = Offset(-expandPx * 0.45f, -expandPx * 0.45f),
                size = Size(size.width + expandPx * 0.9f, size.height + expandPx * 0.9f),
                cornerRadius = CornerRadius(radiusPx, radiusPx)
            )
        }
}
