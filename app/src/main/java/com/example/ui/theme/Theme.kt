package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

@Composable
fun VaultixTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    designStyle: String = "LIQUID_GLASS",
    content: @Composable () -> Unit
) {
    val glassColors = ThemeEngineCatalog.resolveColors(
        designStyle = designStyle,
        isDark = darkTheme
    )

    val onPrimaryColor = if (glassColors.primaryNeon.luminance() > 0.42f) {
        Color(0xFF0A1220)
    } else {
        Color.White
    }
    val onSecondaryColor = if (glassColors.secondaryNeon.luminance() > 0.42f) {
        Color(0xFF0A1220)
    } else {
        Color.White
    }

    val materialColors = if (darkTheme) {
        darkColorScheme(
            primary = glassColors.primaryNeon,
            onPrimary = onPrimaryColor,
            primaryContainer = glassColors.primaryNeon.copy(alpha = 0.24f),
            onPrimaryContainer = glassColors.textPrimary,
            secondary = glassColors.secondaryNeon,
            onSecondary = onSecondaryColor,
            background = glassColors.bgStart,
            onBackground = glassColors.textPrimary,
            surface = glassColors.glassSurface,
            onSurface = glassColors.textPrimary,
            surfaceVariant = glassColors.glassCard,
            onSurfaceVariant = glassColors.textSecondary,
            outline = glassColors.glassBorderSubtle,
            error = glassColors.errorColor,
            onError = Color.White
        )
    } else {
        lightColorScheme(
            primary = glassColors.primaryNeon,
            onPrimary = onPrimaryColor,
            primaryContainer = glassColors.primaryNeon.copy(alpha = 0.16f),
            onPrimaryContainer = glassColors.textPrimary,
            secondary = glassColors.secondaryNeon,
            onSecondary = onSecondaryColor,
            background = glassColors.bgStart,
            onBackground = glassColors.textPrimary,
            surface = glassColors.glassSurface,
            onSurface = glassColors.textPrimary,
            surfaceVariant = glassColors.glassCard,
            onSurfaceVariant = glassColors.textSecondary,
            outline = glassColors.glassBorderSubtle,
            error = glassColors.errorColor,
            onError = Color.White
        )
    }

    ProvideAdaptiveDesign {
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
}
