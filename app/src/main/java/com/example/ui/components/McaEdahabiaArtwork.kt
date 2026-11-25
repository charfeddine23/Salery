package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal val McaEmeraldBright = Color(0xFF009A5E)
internal val McaEmeraldMid = Color(0xFF00804C)
internal val McaEmeraldDeep = Color(0xFF005C36)
internal val McaCrimsonBright = Color(0xFFE6313E)
internal val McaCrimsonMid = Color(0xFFCE1A28)
internal val McaCrimsonDeep = Color(0xFFA80E1B)
internal val McaChampagneGold = Color(0xFFF2DF89)
internal val McaWarmGold = Color(0xFFE4C662)
internal val McaDeepGold = Color(0xFFC49E32)

/**
 * Draws the Mouloudia Club d'Alger (MCA) FAN Edition Classic Edahabia card surface:
 * - Rich emerald-green main zone on the left & bottom
 * - Curved crimson-red upper-right zone with subtle geometric weave texture
 * - Sweeping tapered champagne-gold ribbon separating green and red
 * - Optional press-and-hold bottom accent bar
 */
fun DrawScope.drawMcaFanCardBackground(
    holdProgress: Float = 0f
) {
    val w = size.width
    val h = size.height

    // 1. Emerald Green Main Base
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(
                McaEmeraldBright,
                McaEmeraldMid,
                McaEmeraldDeep
            ),
            start = Offset(0f, 0f),
            end = Offset(w * 0.85f, h)
        ),
        size = size
    )

    // Subtle diagonal silk texture lines across emerald zone
    val lineStep = (w * 0.045f).coerceAtLeast(14f)
    var diag = -h
    while (diag < w + h) {
        drawLine(
            color = Color.White.copy(alpha = 0.025f),
            start = Offset(diag, 0f),
            end = Offset(diag + h * 0.85f, h),
            strokeWidth = 1f
        )
        diag += lineStep
    }

    // 2. Crimson Red Upper-Right Sweeping Sector
    val dividerCurve = Path().apply {
        moveTo(w * 0.21f, 0f)
        cubicTo(
            w * 0.56f, h * 0.11f,
            w * 0.84f, h * 0.42f,
            w * 0.945f, h
        )
    }
    val redZonePath = Path().apply {
        addPath(dividerCurve)
        lineTo(w, h)
        lineTo(w, 0f)
        close()
    }

    clipPath(redZonePath) {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    McaCrimsonMid,
                    McaCrimsonBright,
                    McaCrimsonDeep
                ),
                start = Offset(w * 0.35f, 0f),
                end = Offset(w, h)
            ),
            size = size
        )

        // Subtle geometric weave / micro-lettering texture matching the red corner of the FAN card
        val cellW = (w * 0.055f).coerceAtLeast(10f)
        val cellH = (h * 0.11f).coerceAtLeast(8f)
        var row = 0
        var y = 0f
        while (y < h) {
            var x = w * 0.20f + (if (row % 2 == 0) 0f else cellW * 0.5f)
            while (x < w) {
                drawRect(
                    color = Color.White.copy(alpha = if ((row + (x / cellW).toInt()) % 3 == 0) 0.065f else 0.03f),
                    topLeft = Offset(x, y),
                    size = Size(cellW * 0.78f, cellH * 0.74f),
                    style = Stroke(width = 1.1f)
                )
                drawLine(
                    color = Color.Black.copy(alpha = 0.045f),
                    start = Offset(x + cellW * 0.15f, y + cellH * 0.38f),
                    end = Offset(x + cellW * 0.65f, y + cellH * 0.38f),
                    strokeWidth = 1.2f
                )
                x += cellW
            }
            y += cellH
            row++
        }
    }

    // 3. Sweeping Tapered Champagne-Gold Swoosh Ribbon along the Green/Red seam
    val swooshWidthCenter = (w * 0.022f).coerceAtLeast(4.5f)
    val swooshPath = Path().apply {
        // Outer (upper-right) edge
        moveTo(w * 0.205f, 0f)
        cubicTo(
            w * 0.568f, h * 0.105f,
            w * 0.848f, h * 0.415f,
            w * 0.948f, h
        )
        // Taper back along inner (lower-left) edge
        lineTo(w * 0.934f, h)
        cubicTo(
            w * 0.818f, h * 0.445f,
            w * 0.540f, h * 0.145f,
            w * 0.190f, 0f
        )
        close()
    }
    drawPath(
        path = swooshPath,
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFFFF4BD),
                McaChampagneGold,
                McaWarmGold,
                McaChampagneGold
            ),
            start = Offset(w * 0.20f, 0f),
            end = Offset(w * 0.95f, h)
        )
    )

    // Delicate specular highlight along the green edge of the gold swoosh
    drawPath(
        path = dividerCurve,
        color = Color(0xFFFFF8D6).copy(alpha = 0.55f),
        style = Stroke(width = (swooshWidthCenter * 0.18f).coerceAtLeast(1f))
    )

    // 4. Press-and-Hold Edit Progress Accent
    if (holdProgress > 0.02f) {
        drawRoundRect(
            color = McaChampagneGold.copy(alpha = 0.95f),
            topLeft = Offset(0f, h - 6f),
            size = Size(w * holdProgress, 6f),
            cornerRadius = CornerRadius(4f, 4f)
        )
    }
}

/**
 * Vector Mouloudia Club d'Alger (MCA) Shield Crest:
 * - Floating top gold 5-pointed star
 * - Gold-rimmed shield split vertically left Emerald Green / right Crimson Red
 * - Centered golden crescent + 5-pointed star
 * - Crisp embossed gold "MCA" lettering
 */
@Composable
fun McaShieldCrest(
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Crown 5-pointed Star above shield
            drawFivePointStar(
                center = Offset(w * 0.50f, h * 0.085f),
                outerRadius = w * 0.105f,
                innerRadius = w * 0.045f,
                fillColor = Color(0xFFFFF1A8),
                strokeColor = McaDeepGold.copy(alpha = 0.65f)
            )

            // 2. Shield Contour Path
            val shieldPath = Path().apply {
                moveTo(w * 0.11f, h * 0.22f)
                quadraticTo(w * 0.30f, h * 0.27f, w * 0.50f, h * 0.185f)
                quadraticTo(w * 0.70f, h * 0.27f, w * 0.89f, h * 0.22f)
                cubicTo(
                    w * 0.91f, h * 0.57f,
                    w * 0.76f, h * 0.82f,
                    w * 0.50f, h * 0.96f
                )
                cubicTo(
                    w * 0.24f, h * 0.82f,
                    w * 0.09f, h * 0.57f,
                    w * 0.11f, h * 0.22f
                )
                close()
            }

            // Subtle drop shadow under shield
            drawPath(
                path = shieldPath,
                color = Color.Black.copy(alpha = 0.28f),
                style = Stroke(width = if (compact) 4f else 6f)
            )

            // 3. Vertical Green/Red Split Inside Shield
            clipPath(shieldPath) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF00995C), Color(0xFF007846))
                    ),
                    topLeft = Offset(0f, 0f),
                    size = Size(w * 0.50f, h)
                )
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFE62735), Color(0xFFC21422))
                    ),
                    topLeft = Offset(w * 0.50f, 0f),
                    size = Size(w * 0.50f, h)
                )
                // Glossy upper sheen
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.16f), Color.Transparent),
                        startY = h * 0.18f,
                        endY = h * 0.50f
                    ),
                    topLeft = Offset(0f, h * 0.18f),
                    size = Size(w, h * 0.32f)
                )
            }

            // 4. Double Gold Rim Border
            drawPath(
                path = shieldPath,
                color = Color(0xFF2B1B04).copy(alpha = 0.35f),
                style = Stroke(width = if (compact) 2.8f else 4.6f, join = StrokeJoin.Round)
            )
            drawPath(
                path = shieldPath,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFFFF5C0), McaChampagneGold, McaDeepGold)
                ),
                style = Stroke(width = if (compact) 1.8f else 3.0f, join = StrokeJoin.Round)
            )

            // 5. Golden Crescent inside upper-middle of shield
            val crescentCenter = Offset(w * 0.50f, h * 0.455f)
            val outerR = w * 0.21f
            val innerR = w * 0.165f
            val outerCircle = Path().apply {
                addOval(
                    Rect(
                        left = crescentCenter.x - outerR,
                        top = crescentCenter.y - outerR,
                        right = crescentCenter.x + outerR,
                        bottom = crescentCenter.y + outerR
                    )
                )
            }
            val innerCut = Path().apply {
                addOval(
                    Rect(
                        left = crescentCenter.x - innerR + w * 0.035f,
                        top = crescentCenter.y - innerR - h * 0.048f,
                        right = crescentCenter.x + innerR + w * 0.035f,
                        bottom = crescentCenter.y + innerR - h * 0.048f
                    )
                )
            }
            val crescentPath = Path.combine(PathOperation.Difference, outerCircle, innerCut)
            drawPath(
                path = crescentPath,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFFFF4B8), McaWarmGold)
                )
            )

            // 6. Inner 5-Pointed Star cradled inside the Crescent
            drawFivePointStar(
                center = Offset(w * 0.50f, h * 0.405f),
                outerRadius = w * 0.095f,
                innerRadius = w * 0.040f,
                fillColor = Color(0xFFFFF8D6),
                strokeColor = McaDeepGold.copy(alpha = 0.50f)
            )
        }

        // Bold "MCA" lettering across lower shield
        Text(
            text = "MCA",
            color = Color(0xFFFFF2B0),
            fontSize = if (compact) 7.5.sp else 12.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = if (compact) 0.2.sp else 0.6.sp,
            modifier = Modifier.offset(y = if (compact) 7.dp else 13.dp),
            style = TextStyle(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.55f),
                    offset = Offset(0f, 1.2f),
                    blurRadius = 2f
                )
            )
        )
    }
}

/**
 * Stylized champagne-gold Arabic calligraphic club title matching the physical MCA FAN card:
 * "العميد" above "مولودية الجزائر"
 */
@Composable
fun McaClubCalligraphyMark(
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val goldShadow = Shadow(
        color = Color(0xFF013820).copy(alpha = 0.72f),
        offset = Offset(0.8f, 1.4f),
        blurRadius = 2.5f
    )
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "العميـد",
            color = McaChampagneGold,
            fontSize = if (compact) 10.sp else 17.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = if (compact) 11.sp else 19.sp,
            style = TextStyle(shadow = goldShadow)
        )
        Text(
            text = "مولودية الجزائر",
            color = McaWarmGold,
            fontSize = if (compact) 7.5.sp else 11.5.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = if (compact) 9.sp else 13.sp,
            style = TextStyle(shadow = goldShadow)
        )
    }
}

private fun DrawScope.drawFivePointStar(
    center: Offset,
    outerRadius: Float,
    innerRadius: Float,
    fillColor: Color,
    strokeColor: Color
) {
    val path = Path()
    for (i in 0 until 10) {
        val angle = (-PI / 2.0 + i * PI / 5.0).toFloat()
        val r = if (i % 2 == 0) outerRadius else innerRadius
        val x = center.x + cos(angle) * r
        val y = center.y + sin(angle) * r
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path = path, color = fillColor)
    drawPath(
        path = path,
        color = strokeColor,
        style = Stroke(width = 1.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}
