package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Liquid Glass v3 - Dark Obsidian & Neon Accents
val DarkBgStart = Color(0xFF090D16)
val DarkBgCenter = Color(0xFF0E1726)
val DarkBgEnd = Color(0xFF0B1220)

val GlassDarkSurface = Color(0x281B2A4A) // Translucent dark glass
val GlassDarkCard = Color(0x351F3158)
val GlassDarkBorder = Color(0x4D64B5F6) // Specular neon cyan edge
val GlassDarkBorderSubtle = Color(0x2280D8FF)

// Liquid Glass v3 - Light Crystal & Prism Accents
val LightBgStart = Color(0xFFF1F5F9)
val LightBgCenter = Color(0xFFE2E8F0)
val LightBgEnd = Color(0xFFCBD5E1)

val GlassLightSurface = Color(0x8CF8FAFC) // Translucent frosted white
val GlassLightCard = Color(0xB8FFFFFF)
val GlassLightBorder = Color(0x8038BDF8)
val GlassLightBorderSubtle = Color(0x4094A3B8)

// Neon Accent Colors
val CyanNeon = Color(0xFF00E5FF)
val BlueNeon = Color(0xFF2979FF)
val EmeraldNeon = Color(0xFF00E676)
val AmberNeon = Color(0xFFFFAB00)
val RoseNeon = Color(0xFFFF1744)
val PurpleNeon = Color(0xFFD500F9)

// Text Colors
val TextDarkPrimary = Color(0xFFF8FAFC)
val TextDarkSecondary = Color(0xFF94A3B8)
val TextDarkMuted = Color(0xFF64748B)

val TextLightPrimary = Color(0xFF0F172A)
val TextLightSecondary = Color(0xFF475569)
val TextLightMuted = Color(0xFF64748B)

// Gradients
val LiquidCyanGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF00E5FF), Color(0xFF2979FF))
)
val LiquidEmeraldGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF00E676), Color(0xFF00B0FF))
)
val LiquidAmberGradient = Brush.linearGradient(
    colors = listOf(Color(0xFFFFAB00), Color(0xFFFF6D00))
)
val LiquidRoseGradient = Brush.linearGradient(
    colors = listOf(Color(0xFFFF5252), Color(0xFFFF1744))
)
val GlassHighlightGradient = Brush.linearGradient(
    colors = listOf(Color(0x66FFFFFF), Color(0x10FFFFFF), Color(0x00FFFFFF))
)
