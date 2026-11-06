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

val NeuDarkBackground = Color(0xFF1A1D24)  // Deep matte charcoal
val NeuDarkCard = Color(0xFF1A1D24)
val NeuDarkDarkShadow = Color(0xFF0C0E12)  // Deep ambient shadow
val NeuDarkHighlight = Color(0xFF282C37)   // Soft matte light highlight

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
    elevation: Dp = 5.dp,
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
            // 1. Top-left light highlight
            paint.color = backgroundColor.toArgb()
            paint.setShadowLayer(elevPx * 1.4f, -elevPx * 0.75f, -elevPx * 0.75f, lightShadowColor.toArgb())
            canvas.nativeCanvas.drawRoundRect(0f, 0f, size.width, size.height, cornerPx, cornerPx, paint)

            // 2. Bottom-right dark shadow
            paint.setShadowLayer(elevPx * 1.4f, elevPx * 0.75f, elevPx * 0.75f, darkShadowColor.toArgb())
            canvas.nativeCanvas.drawRoundRect(0f, 0f, size.width, size.height, cornerPx, cornerPx, paint)

            // 3. Flat solid surface on top
            paint.clearShadowLayer()
            paint.color = backgroundColor.toArgb()
            canvas.nativeCanvas.drawRoundRect(0f, 0f, size.width, size.height, cornerPx, cornerPx, paint)
        } else {
            // Concave / Inset effect: Solid base + inner shadow appearance
            paint.clearShadowLayer()
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
    val shape = RoundedCornerShape(cornerRadius)
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
            .clip(shape)
            .then(clickModifier),
        content = content
    )
}
