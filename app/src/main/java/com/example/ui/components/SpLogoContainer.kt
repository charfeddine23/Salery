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
 * Luxurious dynamic container for the official "SP" brand logo.
 * 
 * Ensures flawless contrast, razor-sharp visibility, and adaptive lighting:
 * - In Light Mode: Houses the logo inside a rich obsidian-slate crystal capsule (Color 0xFF0F172A)
 *   with luminous sapphire borders, ensuring the white/silver 'S' and golden 'P' pop vibrantly.
 * - In Dark Mode: Uses translucent specular dark glass with specular neon highlights.
 */
@Composable
fun SpLogoContainer(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    cornerRadius: Dp = 14.dp,
    padding: Dp = 6.dp
) {
    val colors = LiquidGlassTheme.colors
    val isDark = colors.isDark

    val backgroundBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                colors.glassSurface.copy(alpha = 0.90f),
                colors.glassCard.copy(alpha = 0.75f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0F172A), // Deep Slate Obsidian
                Color(0xFF1E293B)  // Luminous Midnight Slate
            )
        )
    }

    val borderColor = if (isDark) {
        colors.glassBorder
    } else {
        Color(0xFF38BDF8).copy(alpha = 0.55f)
    }

    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .size(size)
            .shadow(elevation = if (isDark) 4.dp else 6.dp, shape = shape, spotColor = Color(0xFF00E5FF).copy(alpha = 0.25f))
            .clip(shape)
            .background(backgroundBrush)
            .border(1.2.dp, borderColor, shape)
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.sp_brand_logo),
            contentDescription = APP_NAME,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}
