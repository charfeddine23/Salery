package com.example.ui.theme

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Neumorphism Theme Color Palette
val NeuLightBackground = Color(0xFFE8EEF5) // Soft tactile off-white/slate
val NeuLightCard = Color(0xFFE8EEF5)
val NeuLightDarkShadow = Color(0xFFB8C5D6) // Soft ambient dark shadow
val NeuLightHighlight = Color(0xFFFFFFFF)  // Pure light highlight

val NeuDarkBackground = Color(0xFF13151A)  // Deep matte obsidian backdrop
val NeuDarkCard = Color(0xFF202530)        // Tactile dark charcoal card surface with clear contrast
val NeuDarkDarkShadow = Color(0xFF06070A)  // Deep soft shadow
val NeuDarkHighlight = Color(0xFF384050)   // Subtle ambient light highlight on top-left edge

// Tactile Neumorphic Accents
val NeuBurntCoral = Color(0xFFE0564C)     // Expenses in Light
val NeuBurntCoralDark = Color(0xFFFF6B6B) // Expenses in Dark
val NeuVibrantGreen = Color(0xFF10B981)   // Income in Light
val NeuVibrantGreenDark = Color(0xFF20C997) // Income in Dark
val NeuRoyalBlue = Color(0xFF2563EB)      // Balances/Primary in Light
val NeuRoyalBlueDark = Color(0xFF3B82F6)  // Balances/Primary in Dark

/**
 * Custom Modifier for realistic dual-shadow Neumorphic depth:
 * - Extruded state: Soft dark shadow at bottom-right + soft light highlight at top-left
 * - Inset/Concave state: Inward shadows creating tactile pressed depth
 */
fun Modifier.neumorphicDepth(
    cornerRadius: Dp = 20.dp,
    elevation: Dp = 6.dp,
    isDark: Boolean = false,
    isPressed: Boolean = false,
    backgroundColor: Color = if (isDark) NeuDarkCard else NeuLightCard,
    darkShadowColor: Color = if (isDark) NeuDarkDarkShadow.copy(alpha = 0.95f) else NeuLightDarkShadow.copy(alpha = 0.70f),
    lightShadowColor: Color = if (isDark) NeuDarkHighlight.copy(alpha = 0.75f) else NeuLightHighlight.copy(alpha = 0.95f)
): Modifier = this.drawBehind {
    val cornerPx = cornerRadius.toPx()
    val elevPx = elevation.toPx()

    drawIntoCanvas { canvas ->
        val paint = Paint().asFrameworkPaint().apply {
            isAntiAlias = true
        }

        if (!isPressed) {
            val blurRadius = (elevPx * 1.5f).coerceAtLeast(1f)
            val offsetDist = (elevPx * 0.85f).coerceAtLeast(1f)

            // 1a. Ambient diffused dark shadow (larger radius, softer)
            paint.color = darkShadowColor.copy(alpha = if (isDark) 0.50f else 0.30f).toArgb()
            paint.maskFilter = BlurMaskFilter(blurRadius * 2.2f, BlurMaskFilter.Blur.NORMAL)
            canvas.nativeCanvas.drawRoundRect(
                offsetDist * 1.3f,
                offsetDist * 1.3f,
                size.width + offsetDist * 1.3f,
                size.height + offsetDist * 1.3f,
                cornerPx,
                cornerPx,
                paint
            )

            // 1b. Direct contact dark shadow
            paint.color = darkShadowColor.toArgb()
            paint.maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
            canvas.nativeCanvas.drawRoundRect(
                offsetDist,
                offsetDist,
                size.width + offsetDist,
                size.height + offsetDist,
                cornerPx,
                cornerPx,
                paint
            )

            // 2. Top-left light highlight (soft ambient light on top-left edge)
            paint.color = lightShadowColor.toArgb()
            paint.maskFilter = BlurMaskFilter(blurRadius * 1.1f, BlurMaskFilter.Blur.NORMAL)
            canvas.nativeCanvas.drawRoundRect(
                -offsetDist * 0.85f,
                -offsetDist * 0.85f,
                size.width - offsetDist * 0.85f,
                size.height - offsetDist * 0.85f,
                cornerPx,
                cornerPx,
                paint
            )

            // 3. Flat solid surface on top
            paint.maskFilter = null
            paint.color = backgroundColor.toArgb()
            canvas.nativeCanvas.drawRoundRect(0f, 0f, size.width, size.height, cornerPx, cornerPx, paint)

            // 4. Beveled specular gradient edge stroke (light top-left, shadow bottom-right)
            val strokePaint = android.graphics.Paint().apply {
                isAntiAlias = true
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 1.5f
                shader = android.graphics.LinearGradient(
                    0f, 0f, size.width, size.height,
                    intArrayOf(
                        lightShadowColor.copy(alpha = if (isDark) 0.55f else 0.80f).toArgb(),
                        lightShadowColor.copy(alpha = if (isDark) 0.20f else 0.35f).toArgb(),
                        android.graphics.Color.TRANSPARENT,
                        darkShadowColor.copy(alpha = if (isDark) 0.70f else 0.40f).toArgb()
                    ),
                    floatArrayOf(0f, 0.25f, 0.70f, 1f),
                    android.graphics.Shader.TileMode.CLAMP
                )
            }
            canvas.nativeCanvas.drawRoundRect(0.75f, 0.75f, size.width - 0.75f, size.height - 0.75f, cornerPx, cornerPx, strokePaint)
        } else {
            // Concave / Inset effect: Solid base + inner shadow appearance
            paint.clearShadowLayer()
            paint.maskFilter = null
            paint.color = backgroundColor.toArgb()
            canvas.nativeCanvas.drawRoundRect(0f, 0f, size.width, size.height, cornerPx, cornerPx, paint)

            // Inset top-left dark depression
            paint.color = darkShadowColor.copy(alpha = 0.45f).toArgb()
            paint.maskFilter = BlurMaskFilter(elevPx * 0.8f, BlurMaskFilter.Blur.INNER)
            canvas.nativeCanvas.drawRoundRect(0f, 0f, size.width, size.height, cornerPx, cornerPx, paint)
            paint.maskFilter = null
        }
    }
}

/**
 * Tactile Neumorphic Card container with dual-shadow depth.
 */
@Composable
fun NeumorphicCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    elevation: Dp = 5.dp,
    isDark: Boolean = LiquidGlassTheme.colors.isDark,
    isPressed: Boolean = false,
    backgroundColor: Color = if (isDark) NeuDarkCard else NeuLightCard,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(color = if (isDark) NeuRoyalBlueDark else NeuRoyalBlue),
            onClick = onClick
        )
    } else Modifier

    Box(
        modifier = modifier
            .neumorphicDepth(
                cornerRadius = cornerRadius,
                elevation = elevation,
                isDark = isDark,
                isPressed = isPressed,
                backgroundColor = backgroundColor
            )
            .then(clickModifier),
        content = content
    )
}
