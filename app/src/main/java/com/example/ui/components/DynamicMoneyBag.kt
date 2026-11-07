package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LiquidGlassTheme

/**
 * Dynamic, fluid 90fps Money Bag component.
 * - Changes its fullness, bulge, and scale dynamically based on available funds.
 * - Hardware-accelerated 90fps GPU float & sway animations via graphicsLayer.
 * - Tactile spring squish when pressed.
 * - Click navigates to the dedicated AI Financial Insights Screen.
 */
@Composable
fun DynamicMoneyBag(
    totalBalance: Double,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp
) {
    val colors = LiquidGlassTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 1. Calculate dynamic fullness factor based on available funds (0.0 to 1.0)
    // 0 = empty/minimal funds, 1.0 = 150,000+ DZD
    val fullnessFactor = (totalBalance.coerceAtLeast(0.0) / 150000.0).toFloat().coerceIn(0f, 1f)

    // Smooth spring animation for fullness changes
    val animatedFullness by animateFloatAsState(
        targetValue = fullnessFactor,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "MoneyBagFullness"
    )

    // Press scale spring animation
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "MoneyBagPress"
    )

    // 2. Continuous 90fps GPU Floating & Swaying Animation
    val infiniteTransition = rememberInfiniteTransition(label = "MoneyBag90fpsPhysics")

    // Vertical floating oscillation (±2.5dp)
    val floatY by infiniteTransition.animateFloat(
        initialValue = -2.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FloatY"
    )

    // Subtle gentle tilt sway (±2.8 degrees)
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = -2.8f,
        targetValue = 2.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "RotationSway"
    )

    // Subtle golden glow breathing pulse
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowPulse"
    )

    // Dynamic scale: leaner bag for lower balance, bulging plump bag for high balance
    val dynamicBaseScale = 0.88f + (animatedFullness * 0.28f)
    val effectiveScale = dynamicBaseScale * pressScale

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                translationY = floatY
                rotationZ = rotationAngle
                scaleX = effectiveScale
                scaleY = effectiveScale
            }
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFD700).copy(alpha = 0.22f * glowPulse),
                        Color(0xFFF59E0B).copy(alpha = 0.10f * glowPulse),
                        Color.Transparent
                    )
                )
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = Color(0xFFFFD700), bounded = true),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.82f)) {
            val w = this.size.width
            val h = this.size.height

            // Dynamic pouch width bulge based on fullness (expands outward when rich)
            val bellyBulge = w * (0.05f + animatedFullness * 0.12f)

            // Pouch Body Gradient: Rich Velvet/Gold Leather
            val bagBrush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFFE066), // Gold highlight at top neck
                    Color(0xFFF59E0B), // Vibrant amber gold
                    Color(0xFFD97706), // Deep warm leather gold
                    Color(0xFFB45309)  // Shadowed base
                ),
                startY = h * 0.2f,
                endY = h
            )

            // 1. Draw Pouch Body (Pear/Sack shape)
            val bodyPath = Path().apply {
                // Top neck pinch
                moveTo(w * 0.32f, h * 0.35f)

                // Left bulge
                cubicTo(
                    w * 0.18f - bellyBulge, h * 0.45f,
                    w * 0.12f - bellyBulge, h * 0.80f,
                    w * 0.32f, h * 0.94f
                )

                // Bottom curve
                cubicTo(
                    w * 0.40f, h * 0.98f,
                    w * 0.60f, h * 0.98f,
                    w * 0.68f, h * 0.94f
                )

                // Right bulge
                cubicTo(
                    w * 0.88f + bellyBulge, h * 0.80f,
                    w * 0.82f + bellyBulge, h * 0.45f,
                    w * 0.68f, h * 0.35f
                )

                // Neck pinch close
                close()
            }

            drawPath(path = bodyPath, brush = bagBrush, style = Fill)
            drawPath(
                path = bodyPath,
                color = Color(0xFF78350F).copy(alpha = 0.35f),
                style = Stroke(width = 1.2.dp.toPx())
            )

            // 2. Draw Top Sack Ruffles (Gathered fabric above the tie)
            val rufflePath = Path().apply {
                moveTo(w * 0.30f, h * 0.36f)
                cubicTo(w * 0.22f, h * 0.20f, w * 0.38f, h * 0.15f, w * 0.42f, h * 0.28f)
                cubicTo(w * 0.48f, h * 0.12f, w * 0.54f, h * 0.12f, w * 0.58f, h * 0.28f)
                cubicTo(w * 0.62f, h * 0.15f, w * 0.78f, h * 0.20f, w * 0.70f, h * 0.36f)
                close()
            }
            drawPath(
                path = rufflePath,
                brush = Brush.verticalGradient(
                    listOf(Color(0xFFFEF08A), Color(0xFFF59E0B)),
                    startY = 0f,
                    endY = h * 0.36f
                ),
                style = Fill
            )
            drawPath(
                path = rufflePath,
                color = Color(0xFF78350F).copy(alpha = 0.4f),
                style = Stroke(width = 1.dp.toPx())
            )

            // 3. Draw Tied Cord / Neck Band (Emerald or Ruby thread)
            val cordColor = Color(0xFF10B981) // Emerald silk cord
            drawRoundRect(
                color = cordColor,
                topLeft = Offset(w * 0.28f, h * 0.33f),
                size = Size(w * 0.44f, h * 0.08f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )

            // Cord Knot & Dangling Tassels
            drawCircle(
                color = Color(0xFF059669),
                radius = w * 0.055f,
                center = Offset(w * 0.50f, h * 0.37f)
            )
            // Left tassel
            drawLine(
                color = cordColor,
                start = Offset(w * 0.48f, h * 0.38f),
                end = Offset(w * 0.42f, h * 0.50f),
                strokeWidth = 1.8.dp.toPx(),
                cap = StrokeCap.Round
            )
            // Right tassel
            drawLine(
                color = cordColor,
                start = Offset(w * 0.52f, h * 0.38f),
                end = Offset(w * 0.58f, h * 0.50f),
                strokeWidth = 1.8.dp.toPx(),
                cap = StrokeCap.Round
            )

            // 4. Center Gold Coin Crest Emblem
            val crestCenter = Offset(w * 0.50f, h * 0.64f)
            val crestRadius = w * 0.16f + (animatedFullness * 0.04f * w)

            // Outer gold halo
            drawCircle(
                color = Color(0xFFFEF08A).copy(alpha = 0.5f * glowPulse),
                radius = crestRadius * 1.35f,
                center = crestCenter
            )

            // Gold Coin
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFFBEB), Color(0xFFFBBF24), Color(0xFFD97706)),
                    center = crestCenter,
                    radius = crestRadius
                ),
                radius = crestRadius,
                center = crestCenter
            )
            drawCircle(
                color = Color(0xFF92400E),
                radius = crestRadius,
                center = crestCenter,
                style = Stroke(width = 1.dp.toPx())
            )

            // Inner Currency Emblem Mark ("$")
            val symWidth = crestRadius * 0.6f
            val symHeight = crestRadius * 1.1f
            drawLine(
                color = Color(0xFF78350F),
                start = Offset(crestCenter.x, crestCenter.y - symHeight * 0.55f),
                end = Offset(crestCenter.x, crestCenter.y + symHeight * 0.55f),
                strokeWidth = 1.2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}
