package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassTheme

/**
 * Top header brand icon with persistent, dynamic breathing ambient glow.
 *
 * Designed with a strictly fixed footprint to guarantee zero layout shifts,
 * jitter, or movements across different screen routes, while presenting
 * an organic, living halo pulse in both Light and Dark modes.
 */
@Composable
fun BreathingSpLogo(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    val colors = LiquidGlassTheme.colors
    val isDark = colors.isDark

    val transition = rememberInfiniteTransition(label = "SpBreathing")

    // Breathing pulse scale (organic expansion and contraction)
    val pulseScale by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.16f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    // Breathing glow opacity
    val glowAlpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowAlpha"
    )

    // Gentle shifting aura rotation
    val auraRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "AuraRotation"
    )

    // Ambient glow gradient adapted to theme
    val glowBrush = if (isDark) {
        Brush.sweepGradient(
            listOf(
                CyanNeon.copy(alpha = 0.8f),
                Color(0xFF38BDF8).copy(alpha = 0.7f),
                EmeraldNeon.copy(alpha = 0.75f),
                CyanNeon.copy(alpha = 0.8f)
            )
        )
    } else {
        Brush.sweepGradient(
            listOf(
                Color(0xFF0284C7).copy(alpha = 0.7f),
                Color(0xFFF59E0B).copy(alpha = 0.65f), // Golden accent matching the 'P'
                Color(0xFF38BDF8).copy(alpha = 0.7f),
                Color(0xFF0284C7).copy(alpha = 0.7f)
            )
        )
    }

    // Fixed bounding box container (strictly fixed footprint prevents any jitter)
    Box(
        modifier = modifier.size(size + 8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Living Ambient Glow Background Layer
        Box(
            modifier = Modifier
                .size(size + 4.dp)
                .scale(pulseScale)
                .alpha(glowAlpha)
                .rotate(auraRotation)
                .clip(CircleShape)
                .background(glowBrush)
                .blur(8.dp)
        )

        // Center SP Logo Container (Crisp, high contrast emblem)
        SpLogoContainer(
            size = size,
            cornerRadius = 11.dp,
            padding = 3.dp
        )
    }
}
