package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.VaultAppMotif
import com.example.data.VaultAppPreset
import com.example.data.VaultAppPresets
import com.example.ui.theme.LiquidGlassTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun VaultAppPreset.primaryColor(): Color = Color(primaryColorHex)
fun VaultAppPreset.secondaryColor(): Color = Color(secondaryColorHex)
fun VaultAppPreset.accentColor(): Color = Color(accentColorHex)
fun VaultAppPreset.surfaceTint(isDark: Boolean): Color =
    Color(if (isDark) surfaceTintDarkHex else surfaceTintLightHex)

@Composable
fun VaultAppBrandBackdrop(
    preset: VaultAppPreset,
    shimmerPhase: Float,
    pulsePhase: Float,
    modifier: Modifier = Modifier
) {
    val colors = LiquidGlassTheme.colors
    val isDark = colors.isDark

    val primary by animateColorAsState(
        targetValue = preset.primaryColor(),
        animationSpec = tween(420),
        label = "brand_primary"
    )
    val secondary by animateColorAsState(
        targetValue = preset.secondaryColor(),
        animationSpec = tween(420),
        label = "brand_secondary"
    )
    val accent by animateColorAsState(
        targetValue = preset.accentColor(),
        animationSpec = tween(420),
        label = "brand_accent"
    )
    val surfaceTint by animateColorAsState(
        targetValue = preset.surfaceTint(isDark),
        animationSpec = tween(420),
        label = "brand_surface"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // 1. Rich brand-tinted glass base wash
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    surfaceTint.copy(alpha = if (isDark) 0.94f else 0.96f),
                    primary.copy(alpha = if (isDark) 0.16f else 0.11f),
                    secondary.copy(alpha = if (isDark) 0.20f else 0.09f)
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        )

        // 2. Ambient corner radial glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primary.copy(alpha = (if (isDark) 0.28f else 0.16f) * (0.85f + 0.15f * pulsePhase)),
                    secondary.copy(alpha = if (isDark) 0.12f else 0.06f),
                    Color.Transparent
                ),
                center = Offset(w * 0.86f, h * 0.18f),
                radius = w * 0.62f
            ),
            radius = w * 0.62f,
            center = Offset(w * 0.86f, h * 0.18f)
        )

        // 3. Signature Brand Shapes & Geometry
        val shapeAlpha = if (isDark) 0.24f else 0.18f
        when (preset.motif) {
            VaultAppMotif.CAMERA_LENS_RINGS -> drawCameraLensMotif(w, h, primary, secondary, accent, pulsePhase, shapeAlpha)
            VaultAppMotif.POSTAL_ARCS -> drawPostalArcsMotif(w, h, primary, secondary, accent, pulsePhase, shapeAlpha)
            VaultAppMotif.CRYSTAL_FACETS -> drawCrystalFacetsMotif(w, h, primary, secondary, accent, shapeAlpha)
            VaultAppMotif.CRYPTO_HEX_GRID -> drawCryptoHexMotif(w, h, primary, accent, pulsePhase, shapeAlpha)
            VaultAppMotif.CRYPTO_DIAMONDS -> drawCryptoDiamondsMotif(w, h, primary, accent, pulsePhase, shapeAlpha)
            VaultAppMotif.AURORA_ORBS -> drawGoogleAuroraOrbsMotif(w, h, pulsePhase, shapeAlpha)
            VaultAppMotif.SOCIAL_WAVES -> drawSocialWavesMotif(w, h, primary, secondary, pulsePhase, shapeAlpha)
            VaultAppMotif.SOUND_WAVES -> drawTikTokSoundWavesMotif(w, h, primary, secondary, pulsePhase, shapeAlpha)
            VaultAppMotif.CHAT_BUBBLES -> drawChatBubblesMotif(w, h, primary, accent, pulsePhase, shapeAlpha)
            VaultAppMotif.PAPER_PLANES -> drawPaperPlaneMotif(w, h, primary, accent, pulsePhase, shapeAlpha)
            VaultAppMotif.SPARK_STARS -> drawSparkStarsMotif(w, h, primary, accent, pulsePhase, shapeAlpha)
            VaultAppMotif.DIAGONAL_SLASHES -> drawDiagonalSlashesMotif(w, h, primary, accent, shapeAlpha)
            VaultAppMotif.CINEMA_STRIPES -> drawCinemaStripesMotif(w, h, primary, secondary, shapeAlpha)
            VaultAppMotif.SOUND_RINGS -> drawSpotifySoundRingsMotif(w, h, primary, accent, pulsePhase, shapeAlpha)
            VaultAppMotif.ORBIT_NODES -> drawOrbitNodesMotif(w, h, primary, accent, pulsePhase, shapeAlpha)
            VaultAppMotif.CODE_NODES -> drawCodeNodesMotif(w, h, primary, secondary, shapeAlpha)
            VaultAppMotif.CARD_RIBBONS -> drawCardRibbonsMotif(w, h, primary, secondary, accent, shapeAlpha)
            VaultAppMotif.LIQUID_AURORA -> drawLiquidAuroraMotif(w, h, primary, secondary, pulsePhase, shapeAlpha)
        }

        // 4. Diagonal metallic liquid shimmer sweep
        val beamW = w * 0.36f
        val beamX = (w + beamW * 2f) * shimmerPhase - beamW
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = if (isDark) 0.055f else 0.20f),
                    Color.Transparent
                ),
                start = Offset(beamX, 0f),
                end = Offset(beamX + beamW, h)
            )
        )
    }
}

@Composable
fun VaultAppEmblemBadge(
    preset: VaultAppPreset,
    size: Dp = 44.dp
) {
    val primary by animateColorAsState(preset.primaryColor(), tween(380), label = "emblem_p")
    val secondary by animateColorAsState(preset.secondaryColor(), tween(380), label = "emblem_s")
    val accent by animateColorAsState(preset.accentColor(), tween(380), label = "emblem_a")

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(primary, secondary)
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(Color.White.copy(alpha = 0.72f), accent.copy(alpha = 0.75f))
                ),
                shape = RoundedCornerShape(14.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.26f), Color.Transparent),
                    center = Offset(this.size.width * 0.25f, this.size.height * 0.2f),
                    radius = this.size.width * 0.75f
                )
            )
        }
        Text(
            text = preset.monogram,
            color = if (preset.id == "binance" || preset.id == "snapchat" || preset.id == "x_twitter") {
                Color(0xFF0F172A)
            } else {
                Color.White
            },
            fontSize = when {
                preset.monogram.length >= 3 -> 11.5.sp
                preset.monogram.length == 2 -> 14.sp
                else -> 17.sp
            },
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.4.sp
        )
    }
}

@Composable
fun AppPresetPickerDialog(
    selectedPresetId: String,
    isArabic: Boolean,
    onSelect: (VaultAppPreset) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LiquidGlassTheme.colors
    var query by remember { mutableStateOf("") }

    val filtered = remember(query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) {
            VaultAppPresets.all
        } else {
            VaultAppPresets.all.filter { preset ->
                preset.nameEn.lowercase().contains(q) ||
                    preset.nameAr.lowercase().contains(q) ||
                    preset.keywords.any { it.contains(q) }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .testTag("app_preset_picker_dialog"),
            shape = RoundedCornerShape(26.dp),
            backgroundColor = colors.dialogBackground,
            borderColor = colors.primaryNeon.copy(alpha = 0.45f)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isArabic) "اختر التطبيق أو الخدمة" else "Choose App Theme",
                            color = colors.textPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = if (isArabic) "سيتحول شكل ولون الوعاء بالكامل حسب التطبيق"
                            else "Card transforms into that app's colors & signature shapes",
                            color = colors.textMuted,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(44.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = if (isArabic) "إغلاق" else "Close",
                            tint = colors.textMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.glassCard)
                        .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = colors.primaryNeon,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(
                                text = if (isArabic) "ابحث: Instagram، BaridiMob، Google..."
                                else "Search: Instagram, BaridiMob, Google...",
                                color = colors.textMuted,
                                fontSize = 12.5.sp
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = colors.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(colors.primaryNeon),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 390.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.id }) { preset ->
                        val isSelected = preset.id == selectedPresetId
                        val primary = preset.primaryColor()
                        val secondary = preset.secondaryColor()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            primary.copy(alpha = if (isSelected) 0.26f else 0.13f),
                                            secondary.copy(alpha = if (isSelected) 0.22f else 0.08f)
                                        )
                                    )
                                )
                                .border(
                                    width = if (isSelected) 1.4.dp else 0.9.dp,
                                    color = if (isSelected) primary else primary.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { onSelect(preset) }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .testTag("app_preset_item_${preset.id}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            VaultAppEmblemBadge(preset = preset, size = 40.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isArabic) preset.nameAr else preset.nameEn,
                                    color = colors.textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = preset.defaultCategory,
                                    color = primary,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Brand-Specific Shape Motif Painters ---

private fun DrawScope.drawCameraLensMotif(
    w: Float, h: Float, primary: Color, secondary: Color, accent: Color, pulse: Float, alpha: Float
) {
    val cx = w * 0.84f
    val cy = h * 0.34f
    drawRoundRect(
        color = primary.copy(alpha = alpha * 0.85f),
        topLeft = Offset(cx - 34.dp.toPx(), cy - 34.dp.toPx()),
        size = Size(68.dp.toPx(), 68.dp.toPx()),
        cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx()),
        style = Stroke(width = 2.dp.toPx())
    )
    drawCircle(
        color = accent.copy(alpha = alpha * (0.8f + 0.2f * pulse)),
        radius = (17f + 2.5f * pulse).dp.toPx(),
        center = Offset(cx, cy),
        style = Stroke(width = 2.2.dp.toPx())
    )
    drawCircle(
        color = secondary.copy(alpha = alpha),
        radius = 4.dp.toPx(),
        center = Offset(cx + 21.dp.toPx(), cy - 21.dp.toPx())
    )
}

private fun DrawScope.drawPostalArcsMotif(
    w: Float, h: Float, primary: Color, secondary: Color, accent: Color, pulse: Float, alpha: Float
) {
    val center = Offset(w * 0.88f, h * 0.86f)
    listOf(28f, 48f, 68f).forEachIndexed { idx, rad ->
        drawCircle(
            color = (if (idx % 2 == 0) primary else accent).copy(alpha = alpha * (0.65f + 0.25f * pulse)),
            radius = rad.dp.toPx(),
            center = center,
            style = Stroke(width = 1.6.dp.toPx())
        )
    }
    drawLine(
        color = secondary.copy(alpha = alpha * 0.65f),
        start = Offset(w * 0.08f, h * 0.88f),
        end = Offset(w * 0.56f, h * 0.88f),
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawCrystalFacetsMotif(
    w: Float, h: Float, primary: Color, secondary: Color, accent: Color, alpha: Float
) {
    val facet1 = Path().apply {
        moveTo(w * 0.72f, 0f)
        lineTo(w, 0f)
        lineTo(w, h * 0.62f)
        lineTo(w * 0.58f, h * 0.28f)
        close()
    }
    drawPath(facet1, color = primary.copy(alpha = alpha * 0.6f))
    drawPath(facet1, color = accent.copy(alpha = alpha * 0.7f), style = Stroke(width = 1.3.dp.toPx()))
    val facet2 = Path().apply {
        moveTo(w * 0.82f, h * 0.38f)
        lineTo(w, h * 0.68f)
        lineTo(w * 0.70f, h)
        close()
    }
    drawPath(facet2, color = secondary.copy(alpha = alpha * 0.75f))
}

private fun DrawScope.drawCryptoHexMotif(
    w: Float, h: Float, primary: Color, accent: Color, pulse: Float, alpha: Float
) {
    val cx = w * 0.85f
    val cy = h * 0.42f
    listOf(24f, 40f).forEach { radiusDp ->
        val rad = radiusDp.dp.toPx()
        val hex = Path().apply {
            for (i in 0 until 6) {
                val angle = (i * 60.0) * PI / 180.0
                val px = cx + rad * cos(angle).toFloat()
                val py = cy + rad * sin(angle).toFloat()
                if (i == 0) moveTo(px, py) else lineTo(px, py)
            }
            close()
        }
        drawPath(
            path = hex,
            color = (if (radiusDp < 30f) accent else primary).copy(alpha = alpha * (0.75f + 0.25f * pulse)),
            style = Stroke(width = 1.6.dp.toPx(), join = StrokeJoin.Round)
        )
    }
}

private fun DrawScope.drawCryptoDiamondsMotif(
    w: Float, h: Float, primary: Color, accent: Color, pulse: Float, alpha: Float
) {
    val cx = w * 0.84f
    val cy = h * 0.44f
    rotate(degrees = 45f, pivot = Offset(cx, cy)) {
        val outer = 38.dp.toPx()
        val inner = 20.dp.toPx()
        drawRect(
            color = primary.copy(alpha = alpha * 0.85f),
            topLeft = Offset(cx - outer / 2f, cy - outer / 2f),
            size = Size(outer, outer),
            style = Stroke(width = 1.8.dp.toPx())
        )
        drawRect(
            color = accent.copy(alpha = alpha * (0.65f + 0.35f * pulse)),
            topLeft = Offset(cx - inner / 2f, cy - inner / 2f),
            size = Size(inner, inner)
        )
    }
}

private fun DrawScope.drawGoogleAuroraOrbsMotif(
    w: Float, h: Float, pulse: Float, alpha: Float
) {
    val orbs = listOf(
        Triple(Offset(w * 0.78f, h * 0.24f), 26.dp.toPx(), Color(0xFF4285F4)),
        Triple(Offset(w * 0.90f, h * 0.44f), 20.dp.toPx(), Color(0xFFEA4335)),
        Triple(Offset(w * 0.74f, h * 0.62f), 18.dp.toPx(), Color(0xFFFBBC05)),
        Triple(Offset(w * 0.88f, h * 0.76f), 22.dp.toPx(), Color(0xFF34A853))
    )
    orbs.forEach { (center, r, col) ->
        drawCircle(
            color = col.copy(alpha = alpha * (0.75f + 0.2f * pulse)),
            radius = r * (0.94f + 0.08f * pulse),
            center = center
        )
    }
}

private fun DrawScope.drawSocialWavesMotif(
    w: Float, h: Float, primary: Color, secondary: Color, pulse: Float, alpha: Float
) {
    val wave = Path().apply {
        moveTo(0f, h * 0.78f)
        cubicTo(w * 0.32f, h * 0.56f, w * 0.66f, h * 0.96f, w, h * 0.62f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(wave, color = primary.copy(alpha = alpha * 0.65f))
    drawCircle(
        color = secondary.copy(alpha = alpha * (0.7f + 0.25f * pulse)),
        radius = 30.dp.toPx(),
        center = Offset(w * 0.88f, h * 0.22f),
        style = Stroke(width = 2.dp.toPx())
    )
}

private fun DrawScope.drawTikTokSoundWavesMotif(
    w: Float, h: Float, primary: Color, secondary: Color, pulse: Float, alpha: Float
) {
    // Dual offset cyan + pink sonar bars
    val baseX = w * 0.74f
    val heights = listOf(16f, 28f, 40f, 24f, 34f)
    heights.forEachIndexed { idx, barDp ->
        val x = baseX + idx * 10.dp.toPx()
        val barH = barDp.dp.toPx() * (0.75f + 0.3f * ((pulse + idx * 0.2f) % 1f))
        drawLine(
            color = primary.copy(alpha = alpha),
            start = Offset(x - 1.5.dp.toPx(), h * 0.52f - barH / 2f),
            end = Offset(x - 1.5.dp.toPx(), h * 0.52f + barH / 2f),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = secondary.copy(alpha = alpha),
            start = Offset(x + 1.5.dp.toPx(), h * 0.52f - barH / 2f),
            end = Offset(x + 1.5.dp.toPx(), h * 0.52f + barH / 2f),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawChatBubblesMotif(
    w: Float, h: Float, primary: Color, accent: Color, pulse: Float, alpha: Float
) {
    val bx = w * 0.76f
    val by = h * 0.24f
    drawRoundRect(
        color = primary.copy(alpha = alpha * 0.9f),
        topLeft = Offset(bx, by),
        size = Size(52.dp.toPx(), 36.dp.toPx()),
        cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
        style = Stroke(width = 2.dp.toPx())
    )
    drawCircle(
        color = accent.copy(alpha = alpha * (0.7f + 0.3f * pulse)),
        radius = 4.dp.toPx(),
        center = Offset(bx + 15.dp.toPx(), by + 18.dp.toPx())
    )
    drawCircle(
        color = accent.copy(alpha = alpha * (0.7f + 0.3f * pulse)),
        radius = 4.dp.toPx(),
        center = Offset(bx + 26.dp.toPx(), by + 18.dp.toPx())
    )
    drawCircle(
        color = accent.copy(alpha = alpha * (0.7f + 0.3f * pulse)),
        radius = 4.dp.toPx(),
        center = Offset(bx + 37.dp.toPx(), by + 18.dp.toPx())
    )
}

private fun DrawScope.drawPaperPlaneMotif(
    w: Float, h: Float, primary: Color, accent: Color, pulse: Float, alpha: Float
) {
    val plane = Path().apply {
        moveTo(w * 0.74f, h * 0.36f)
        lineTo(w * 0.94f, h * 0.18f)
        lineTo(w * 0.86f, h * 0.48f)
        close()
    }
    drawPath(plane, color = primary.copy(alpha = alpha * (0.85f + 0.15f * pulse)))
    drawLine(
        color = accent.copy(alpha = alpha * 0.8f),
        start = Offset(w * 0.54f, h * 0.56f),
        end = Offset(w * 0.72f, h * 0.39f),
        strokeWidth = 1.8.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
    )
}

private fun DrawScope.drawSparkStarsMotif(
    w: Float, h: Float, primary: Color, accent: Color, pulse: Float, alpha: Float
) {
    val stars = listOf(
        Triple(Offset(w * 0.84f, h * 0.28f), 18.dp.toPx(), primary),
        Triple(Offset(w * 0.72f, h * 0.48f), 11.dp.toPx(), accent)
    )
    stars.forEach { (center, r, col) ->
        val p = Path().apply {
            moveTo(center.x, center.y - r)
            quadraticTo(center.x, center.y, center.x + r, center.y)
            quadraticTo(center.x, center.y, center.x, center.y + r)
            quadraticTo(center.x, center.y, center.x - r, center.y)
            quadraticTo(center.x, center.y, center.x, center.y - r)
            close()
        }
        drawPath(p, color = col.copy(alpha = alpha * (0.8f + 0.2f * pulse)))
    }
}

private fun DrawScope.drawDiagonalSlashesMotif(
    w: Float, h: Float, primary: Color, accent: Color, alpha: Float
) {
    drawLine(
        color = primary.copy(alpha = alpha * 0.7f),
        start = Offset(w * 0.72f, 0f),
        end = Offset(w * 0.96f, h),
        strokeWidth = 4.dp.toPx()
    )
    drawLine(
        color = accent.copy(alpha = alpha * 0.75f),
        start = Offset(w * 0.96f, 0f),
        end = Offset(w * 0.72f, h),
        strokeWidth = 2.dp.toPx()
    )
}

private fun DrawScope.drawCinemaStripesMotif(
    w: Float, h: Float, primary: Color, secondary: Color, alpha: Float
) {
    listOf(0.74f, 0.82f, 0.90f).forEachIndexed { i, xFrac ->
        drawRect(
            color = (if (i == 1) primary else secondary).copy(alpha = alpha * 0.65f),
            topLeft = Offset(w * xFrac, 0f),
            size = Size(10.dp.toPx(), h)
        )
    }
}

private fun DrawScope.drawSpotifySoundRingsMotif(
    w: Float, h: Float, primary: Color, accent: Color, pulse: Float, alpha: Float
) {
    val cx = w * 0.88f
    val cy = h * 0.56f
    listOf(20f, 34f, 48f).forEachIndexed { idx, rDp ->
        drawArc(
            color = (if (idx == 0) accent else primary).copy(alpha = alpha * (0.75f + 0.25f * pulse)),
            startAngle = 200f,
            sweepAngle = 100f,
            useCenter = false,
            topLeft = Offset(cx - rDp.dp.toPx(), cy - rDp.dp.toPx()),
            size = Size(rDp.dp.toPx() * 2f, rDp.dp.toPx() * 2f),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

private fun DrawScope.drawOrbitNodesMotif(
    w: Float, h: Float, primary: Color, accent: Color, pulse: Float, alpha: Float
) {
    val c = Offset(w * 0.85f, h * 0.42f)
    drawCircle(
        color = primary.copy(alpha = alpha * 0.7f),
        radius = 28.dp.toPx(),
        center = c,
        style = Stroke(width = 1.6.dp.toPx())
    )
    listOf(0f, 120f, 240f).forEach { deg ->
        val rad = deg * PI / 180.0
        val pt = Offset(c.x + 28.dp.toPx() * cos(rad).toFloat(), c.y + 28.dp.toPx() * sin(rad).toFloat())
        drawCircle(color = accent.copy(alpha = alpha * (0.8f + 0.2f * pulse)), radius = 4.5.dp.toPx(), center = pt)
    }
}

private fun DrawScope.drawCodeNodesMotif(
    w: Float, h: Float, primary: Color, secondary: Color, alpha: Float
) {
    val p1 = Offset(w * 0.78f, h * 0.22f)
    val p2 = Offset(w * 0.78f, h * 0.72f)
    val p3 = Offset(w * 0.90f, h * 0.45f)
    drawLine(primary.copy(alpha = alpha), p1, p2, strokeWidth = 2.dp.toPx())
    drawLine(secondary.copy(alpha = alpha), p1, p3, strokeWidth = 2.dp.toPx())
    listOf(p1, p2, p3).forEach { pt ->
        drawCircle(primary.copy(alpha = alpha * 1.1f), radius = 4.5.dp.toPx(), center = pt)
    }
}

private fun DrawScope.drawCardRibbonsMotif(
    w: Float, h: Float, primary: Color, secondary: Color, accent: Color, alpha: Float
) {
    drawRoundRect(
        color = primary.copy(alpha = alpha * 0.55f),
        topLeft = Offset(w * 0.72f, h * 0.16f),
        size = Size(38.dp.toPx(), 46.dp.toPx()),
        cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
    )
    drawRoundRect(
        color = accent.copy(alpha = alpha * 0.65f),
        topLeft = Offset(w * 0.79f, h * 0.26f),
        size = Size(38.dp.toPx(), 46.dp.toPx()),
        cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
        style = Stroke(width = 1.6.dp.toPx())
    )
    drawCircle(color = secondary.copy(alpha = alpha * 0.5f), radius = 8.dp.toPx(), center = Offset(w * 0.88f, h * 0.74f))
}

private fun DrawScope.drawLiquidAuroraMotif(
    w: Float, h: Float, primary: Color, secondary: Color, pulse: Float, alpha: Float
) {
    drawCircle(
        color = primary.copy(alpha = alpha * 0.5f),
        radius = 40.dp.toPx() * (0.9f + 0.1f * pulse),
        center = Offset(w * 0.90f, h * 0.25f)
    )
    drawCircle(
        color = secondary.copy(alpha = alpha * 0.4f),
        radius = 28.dp.toPx(),
        center = Offset(w * 0.76f, h * 0.75f)
    )
}
