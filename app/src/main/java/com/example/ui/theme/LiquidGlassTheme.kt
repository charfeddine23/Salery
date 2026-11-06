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
    glassSurface = NeuDarkCard,
    glassCard = NeuDarkCard,
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

object LiquidGlassTheme {
    val colors: LiquidGlassColors
        @Composable
        get() = LocalLiquidGlass.current
}
