package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyanNeon,
    onPrimary = Color(0xFF001F29),
    primaryContainer = Color(0xFF004D61),
    onPrimaryContainer = Color(0xFFBBE9FF),
    secondary = BlueNeon,
    onSecondary = Color.White,
    background = DarkBgStart,
    onBackground = TextDarkPrimary,
    surface = GlassDarkSurface,
    onSurface = TextDarkPrimary,
    error = RoseNeon,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = Color(0xFF2563EB),
    onSecondary = Color.White,
    background = LightBgStart,
    onBackground = TextLightPrimary,
    surface = GlassLightSurface,
    onSurface = TextLightPrimary,
    error = Color(0xFFDC2626),
    onError = Color.White
)

@Composable
fun SaleryPulseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val glassColors = if (darkTheme) DarkLiquidGlassColors else LightLiquidGlassColors
    val materialColors = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(
        LocalLiquidGlass provides glassColors
    ) {
        MaterialTheme(
            colorScheme = materialColors,
            typography = Typography,
            content = content
        )
    }
}

@Composable
fun SaleryPelseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) = SaleryPulseTheme(darkTheme = darkTheme, content = content)
