package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.i18n.APP_NAME
import com.example.ui.theme.LiquidGlassTheme

/**
 * Luxurious dynamic container for the official Vaultix "VX" brand logo.
 *
 * Ensures flawless contrast, razor-sharp visibility, and adaptive lighting:
 * - In Light Mode: Houses the logo inside a rich obsidian-slate crystal capsule (Color 0xFF0F172A)
 *   with luminous sapphire borders, ensuring the metallic cyan & gold "VX" emblem pops vibrantly.
 * - In Dark Mode: Uses translucent specular dark glass with specular neon highlights.
 */
@Composable
fun VaultixLogoContainer(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    cornerRadius: Dp = 14.dp,
    padding: Dp = 6.dp,
    containerAlpha: Float? = null
) {
    val colors = LiquidGlassTheme.colors
    val isDark = colors.isDark
    val customAlpha = containerAlpha?.coerceIn(0f, 1f)

    val backgroundBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                colors.glassSurface.copy(alpha = customAlpha ?: 0.90f),
                colors.glassCard.copy(alpha = customAlpha ?: 0.75f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0F172A).copy(alpha = customAlpha ?: 1f), // Deep Slate Obsidian
                Color(0xFF1E293B).copy(alpha = customAlpha ?: 1f)  // Luminous Midnight Slate
            )
        )
    }

    val baseBorderColor = if (isDark) {
        colors.glassBorder
    } else {
        Color(0xFF38BDF8).copy(alpha = 0.55f)
    }
    val borderColor = when {
        customAlpha != null && customAlpha <= 0.001f -> Color.Transparent
        customAlpha != null && customAlpha <= 0.15f -> baseBorderColor.copy(alpha = (customAlpha * 2.4f).coerceAtLeast(0.05f))
        else -> baseBorderColor
    }

    val shape = RoundedCornerShape(cornerRadius)
    val shadowModifier = if (customAlpha != null && customAlpha <= 0.15f) {
        Modifier
    } else {
        Modifier.shadow(
            elevation = if (isDark) 4.dp else 6.dp,
            shape = shape,
            spotColor = Color(0xFF00E5FF).copy(alpha = 0.25f)
        )
    }

    Box(
        modifier = modifier
            .size(size)
            .then(shadowModifier)
            .clip(shape)
            .background(backgroundBrush)
            .border(1.2.dp, borderColor, shape)
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.vaultix_brand_logo),
            contentDescription = APP_NAME,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}
