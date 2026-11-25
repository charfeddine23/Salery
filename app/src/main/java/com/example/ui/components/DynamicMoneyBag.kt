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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.ArabicStrings
import com.example.ui.theme.LiquidGlassTheme

/**
 * High-contrast, sculpted 3D-style Money Bag button for the top navigation bar.
 * - Responds smoothly to available balance (plumpness + side gold coins + subtle sparkle).
 * - Uses GPU-accelerated float, sway, and tactile press bounce.
 * - Crisp silhouette and bold "$" emblem stay legible at compact top-bar sizes (44dp-48dp).
 */
@Composable
fun DynamicMoneyBag(
    totalBalance: Double,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val colors = LiquidGlassTheme.colors
    val isDark = colors.isDark
    val isArabic = AppStrings.current is ArabicStrings
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val fullnessFactor = (totalBalance.coerceAtLeast(0.0) / 150000.0).toFloat().coerceIn(0f, 1f)

    val animatedFullness by animateFloatAsState(
        targetValue = fullnessFactor,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "MoneyBagFullness"
    )

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "MoneyBagPress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "MoneyBagPhysics")

    val floatY by infiniteTransition.animateFloat(
        initialValue = -1.8f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FloatY"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = -2.2f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "RotationSway"
    )

    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowPulse"
    )

    val dynamicBaseScale = 0.93f + (animatedFullness * 0.08f)
    val effectiveScale = dynamicBaseScale * pressScale
    val accessibilityLabel = if (isArabic) "تحليلات الرصيد وكيس النقود" else "Money Bag & Financial Insights"

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
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
                    colors = if (isDark) {
                        listOf(
                            Color(0xFF452A06).copy(alpha = 0.72f),
                            Color(0xFF1F1608).copy(alpha = 0.56f),
                            Color(0xFF0F172A).copy(alpha = 0.35f)
                        )
                    } else {
                        listOf(
                            Color(0xFFFFFBEB).copy(alpha = 0.96f),
                            Color(0xFFFEF3C7).copy(alpha = 0.85f),
                            Color(0xFFFDE68A).copy(alpha = 0.45f)
                        )
                    }
                )
            )
            .border(
                width = 1.1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFDE047).copy(alpha = if (isDark) 0.68f else 0.92f),
                        Color(0xFFF59E0B).copy(alpha = if (isDark) 0.34f else 0.60f),
                        Color(0xFF10B981).copy(alpha = if (isDark) 0.42f else 0.45f)
                    )
                ),
                shape = CircleShape
            )
            .semantics { contentDescription = accessibilityLabel }
            .testTag("dynamic_money_bag_button")
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = Color(0xFFFBBF24), bounded = true),
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.78f)) {
            val w = this.size.width
            val h = this.size.height
            val outlineColor = if (isDark) Color(0xFF451A03) else Color(0xFF78350F)
            val outlinePx = 1.35.dp.toPx()

            // Safe belly expansion bounded inside the canvas
            val bellyExpand = w * (0.02f + animatedFullness * 0.055f)

            // 1. Subtle ground contact shadow
            drawOval(
                color = Color(0xFF451A03).copy(alpha = if (isDark) 0.42f else 0.18f),
                topLeft = Offset(w * 0.22f, h * 0.87f),
                size = Size(w * 0.56f, h * 0.09f)
            )

            // 2. Top pleated sack crown (3 clear scalloped lobes)
            val crownPath = Path().apply {
                moveTo(w * 0.34f, h * 0.31f)
                cubicTo(w * 0.23f, h * 0.18f, w * 0.31f, h * 0.08f, w * 0.41f, h * 0.18f)
                cubicTo(w * 0.46f, h * 0.06f, w * 0.54f, h * 0.06f, w * 0.59f, h * 0.18f)
                cubicTo(w * 0.69f, h * 0.08f, w * 0.77f, h * 0.18f, w * 0.66f, h * 0.31f)
                close()
            }
            drawPath(
                path = crownPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFFBEB),
                        Color(0xFFFDE047),
                        Color(0xFFF59E0B)
                    ),
                    startY = h * 0.06f,
                    endY = h * 0.32f
                ),
                style = Fill
            )
            drawPath(
                path = crownPath,
                color = outlineColor.copy(alpha = 0.75f),
                style = Stroke(width = outlinePx * 0.85f, join = StrokeJoin.Round)
            )

            // Crisp fabric gather creases inside the crown
            drawLine(
                color = Color(0xFFB45309).copy(alpha = 0.55f),
                start = Offset(w * 0.42f, h * 0.18f),
                end = Offset(w * 0.45f, h * 0.29f),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFFB45309).copy(alpha = 0.55f),
                start = Offset(w * 0.58f, h * 0.18f),
                end = Offset(w * 0.55f, h * 0.29f),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round
            )

            // 3. Sculpted 3D Pouch Body
            val bodyPath = Path().apply {
                moveTo(w * 0.33f, h * 0.31f)
                cubicTo(
                    w * 0.14f - bellyExpand, h * 0.45f,
                    w * 0.11f - bellyExpand, h * 0.83f,
                    w * 0.33f, h * 0.90f
                )
                cubicTo(
                    w * 0.44f, h * 0.93f,
                    w * 0.56f, h * 0.93f,
                    w * 0.67f, h * 0.90f
                )
                cubicTo(
                    w * 0.89f + bellyExpand, h * 0.83f,
                    w * 0.86f + bellyExpand, h * 0.45f,
                    w * 0.67f, h * 0.31f
                )
                close()
            }

            drawPath(
                path = bodyPath,
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFEF08A),
                        Color(0xFFFBBF24),
                        Color(0xFFF59E0B),
                        Color(0xFFD97706),
                        Color(0xFFB45309)
                    ),
                    center = Offset(w * 0.39f, h * 0.47f),
                    radius = w * 0.68f
                ),
                style = Fill
            )

            // Soft 3D specular gloss on top-left shoulder
            val sheenPath = Path().apply {
                moveTo(w * 0.30f, h * 0.41f)
                cubicTo(w * 0.22f, h * 0.50f, w * 0.20f, h * 0.62f, w * 0.24f, h * 0.70f)
                cubicTo(w * 0.26f, h * 0.60f, w * 0.28f, h * 0.49f, w * 0.34f, h * 0.42f)
                close()
            }
            drawPath(
                path = sheenPath,
                color = Color.White.copy(alpha = 0.38f),
                style = Fill
            )

            // Crisp dark-bronze border around pouch
            drawPath(
                path = bodyPath,
                color = outlineColor.copy(alpha = 0.85f),
                style = Stroke(width = outlinePx, join = StrokeJoin.Round)
            )

            // 4. Curved Emerald Silk Neck Band + Knot & Tassels
            val ribbonPath = Path().apply {
                moveTo(w * 0.30f, h * 0.29f)
                cubicTo(w * 0.43f, h * 0.33f, w * 0.57f, h * 0.33f, w * 0.70f, h * 0.29f)
                lineTo(w * 0.69f, h * 0.36f)
                cubicTo(w * 0.57f, h * 0.40f, w * 0.43f, h * 0.40f, w * 0.31f, h * 0.36f)
                close()
            }
            drawPath(
                path = ribbonPath,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF047857),
                        Color(0xFF10B981),
                        Color(0xFF34D399),
                        Color(0xFF059669)
                    )
                ),
                style = Fill
            )
            drawPath(
                path = ribbonPath,
                color = Color(0xFF064E3B),
                style = Stroke(width = 0.9.dp.toPx(), join = StrokeJoin.Round)
            )

            // Dangling cord tassels
            val leftTassel = Path().apply {
                moveTo(w * 0.49f, h * 0.35f)
                quadraticTo(w * 0.43f, h * 0.40f, w * 0.40f, h * 0.45f)
            }
            val rightTassel = Path().apply {
                moveTo(w * 0.51f, h * 0.35f)
                quadraticTo(w * 0.57f, h * 0.40f, w * 0.60f, h * 0.45f)
            }
            drawPath(leftTassel, Color(0xFF059669), style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round))
            drawPath(rightTassel, Color(0xFF10B981), style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round))

            // Center silk knot
            drawCircle(
                color = Color(0xFF34D399),
                radius = w * 0.055f,
                center = Offset(w * 0.50f, h * 0.345f)
            )
            drawCircle(
                color = Color(0xFF064E3B),
                radius = w * 0.055f,
                center = Offset(w * 0.50f, h * 0.345f),
                style = Stroke(width = 0.9.dp.toPx())
            )

            // 5. Crisp Embossed Gold Medallion & Bold "$" Symbol
            val crestCenter = Offset(w * 0.50f, h * 0.64f)
            val crestRadius = w * 0.21f

            // Soft golden glow halo behind medallion
            drawCircle(
                color = Color(0xFFFFFBEB).copy(alpha = 0.48f * glowPulse),
                radius = crestRadius * 1.24f,
                center = crestCenter
            )

            // Medallion Face
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFFBEB),
                        Color(0xFFFEF08A),
                        Color(0xFFFBBF24)
                    ),
                    center = Offset(crestCenter.x - crestRadius * 0.25f, crestCenter.y - crestRadius * 0.25f),
                    radius = crestRadius * 1.2f
                ),
                radius = crestRadius,
                center = crestCenter
            )
            drawCircle(
                color = outlineColor,
                radius = crestRadius,
                center = crestCenter,
                style = Stroke(width = 1.15.dp.toPx())
            )

            // Crisp "$" S-curve + vertical bar
            val dollarColor = Color(0xFF5C2406)
            val dx = crestCenter.x
            val dy = crestCenter.y
            val rx = crestRadius * 0.42f
            val ry = crestRadius * 0.52f

            // Vertical bar of "$"
            drawLine(
                color = dollarColor,
                start = Offset(dx, dy - crestRadius * 0.66f),
                end = Offset(dx, dy + crestRadius * 0.66f),
                strokeWidth = 1.45.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Crisp S-curve of "$"
            val sPath = Path().apply {
                moveTo(dx + rx * 0.85f, dy - ry * 0.55f)
                cubicTo(
                    dx + rx * 0.55f, dy - ry * 1.05f,
                    dx - rx * 1.05f, dy - ry * 0.95f,
                    dx - rx * 0.85f, dy - ry * 0.35f
                )
                cubicTo(
                    dx - rx * 0.65f, dy + ry * 0.08f,
                    dx + rx * 0.85f, dy - ry * 0.05f,
                    dx + rx * 0.85f, dy + ry * 0.42f
                )
                cubicTo(
                    dx + rx * 0.85f, dy + ry * 1.02f,
                    dx - rx * 0.65f, dy + ry * 1.05f,
                    dx - rx * 0.88f, dy + ry * 0.52f
                )
            }
            drawPath(
                path = sPath,
                color = dollarColor,
                style = Stroke(
                    width = 1.65.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // 6. Extra gleaming gold coins peeking at bottom-right when balance is funded
            if (animatedFullness > 0.08f) {
                val coinAlpha = (animatedFullness * 2.5f).coerceIn(0.45f, 1f)
                val coinCenter = Offset(w * 0.82f, h * 0.82f)
                val coinRx = w * 0.115f
                val coinRy = h * 0.055f

                // Coin thickness rim
                drawOval(
                    color = Color(0xFFB45309).copy(alpha = coinAlpha),
                    topLeft = Offset(coinCenter.x - coinRx, coinCenter.y - coinRy + h * 0.025f),
                    size = Size(coinRx * 2f, coinRy * 2f)
                )
                // Coin top face
                drawOval(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFFFFBEB), Color(0xFFFBBF24))
                    ),
                    topLeft = Offset(coinCenter.x - coinRx, coinCenter.y - coinRy),
                    size = Size(coinRx * 2f, coinRy * 2f),
                    alpha = coinAlpha
                )
                drawOval(
                    color = outlineColor.copy(alpha = coinAlpha * 0.85f),
                    topLeft = Offset(coinCenter.x - coinRx, coinCenter.y - coinRy),
                    size = Size(coinRx * 2f, coinRy * 2f),
                    style = Stroke(width = 0.9.dp.toPx())
                )
            }

            // 7. Subtle 4-point star sparkle at upper-left
            val spX = w * 0.16f
            val spY = h * 0.22f
            val spR = w * 0.085f * glowPulse
            val sparkle = Path().apply {
                moveTo(spX, spY - spR)
                quadraticTo(spX, spY, spX + spR, spY)
                quadraticTo(spX, spY, spX, spY + spR)
                quadraticTo(spX, spY, spX - spR, spY)
                quadraticTo(spX, spY, spX, spY - spR)
                close()
            }
            drawPath(
                path = sparkle,
                color = Color(0xFFFFFBEB).copy(alpha = 0.85f * glowPulse),
                style = Fill
            )
        }
    }
}
