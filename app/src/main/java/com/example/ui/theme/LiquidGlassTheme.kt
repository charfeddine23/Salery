package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Immutable
data class LiquidGlassColors(
    val isDark: Boolean,
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
)

val DarkLiquidGlassColors = LiquidGlassColors(
    isDark = true,
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

val LocalLiquidGlass = staticCompositionLocalOf { DarkLiquidGlassColors }

object LiquidGlassTheme {
    val colors: LiquidGlassColors
        @Composable
        get() = LocalLiquidGlass.current
}
