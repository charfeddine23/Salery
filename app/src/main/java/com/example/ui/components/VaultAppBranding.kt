package com.example.ui.components

import android.content.pm.PackageManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import com.example.data.VaultAppMotif
import com.example.data.VaultAppPreset
import com.example.data.VaultAppPresets
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.RoseNeon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
private fun rememberInstalledAppIconBitmap(packageNames: List<String>): ImageBitmap? {
    val context = LocalContext.current
    val bitmapState by produceState<ImageBitmap?>(initialValue = null, packageNames, context) {
        value = if (packageNames.isEmpty()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                val pm = context.packageManager
                var loaded: ImageBitmap? = null
                for (pkg in packageNames) {
                    try {
                        val drawable = pm.getApplicationIcon(pkg)
                        val w = drawable.intrinsicWidth.takeIf { it in 24..256 } ?: 96
                        val h = drawable.intrinsicHeight.takeIf { it in 24..256 } ?: 96
                        loaded = drawable.toBitmap(width = w, height = h).asImageBitmap()
                        break
                    } catch (_: PackageManager.NameNotFoundException) {
                        // Try next package candidate or fall back to authentic vector emblem
                    } catch (_: Throwable) {
                        // Fall back cleanly in headless / test environments
                    }
                }
                loaded
            }
        }
    }
    return bitmapState
}

@Composable
fun VaultAppEmblemBadge(
    preset: VaultAppPreset,
    size: Dp = 44.dp
) {
    val installedIcon = rememberInstalledAppIconBitmap(preset.packageNames)
    val primary by animateColorAsState(preset.primaryColor(), tween(380), label = "emblem_p")
    val secondary by animateColorAsState(preset.secondaryColor(), tween(380), label = "emblem_s")
    val accent by animateColorAsState(preset.accentColor(), tween(380), label = "emblem_a")
    val cornerRadius = (size.value * 0.30f).coerceIn(6f, 15f).dp

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                if (installedIcon != null) {
                    Brush.linearGradient(listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.04f)))
                } else {
                    authenticBrandBackgroundBrush(preset.id, primary, secondary)
                }
            )
            .border(
                width = if (size < 28.dp) 0.9.dp else 1.2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(Color.White.copy(alpha = 0.72f), accent.copy(alpha = 0.75f))
                ),
                shape = RoundedCornerShape(cornerRadius)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (installedIcon != null) {
            Image(
                bitmap = installedIcon,
                contentDescription = preset.nameEn,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(this.size.width * 0.22f, this.size.height * 0.18f),
                        radius = this.size.width * 0.78f
                    )
                )
                drawAuthenticBrandIcon(preset.id, primary, secondary, accent)
            }
        }
    }
}

private fun authenticBrandBackgroundBrush(id: String, primary: Color, secondary: Color): Brush {
    return when (id) {
        "instagram" -> Brush.linearGradient(
            colors = listOf(Color(0xFF833AB4), Color(0xFFE1306C), Color(0xFFF77737), Color(0xFFFCAF45)),
            start = Offset(0f, 140f),
            end = Offset(140f, 0f)
        )
        "tiktok", "x_twitter" -> Brush.linearGradient(
            colors = listOf(Color(0xFF0B0F17), Color(0xFF181C28))
        )
        "netflix" -> Brush.linearGradient(
            colors = listOf(Color(0xFF140708), Color(0xFF280B0E))
        )
        "binance", "bybit" -> Brush.linearGradient(
            colors = listOf(Color(0xFF14171F), Color(0xFF1F242D))
        )
        "google" -> Brush.linearGradient(
            colors = listOf(Color(0xFFFFFFFF), Color(0xFFF1F5F9))
        )
        "snapchat" -> Brush.linearGradient(
            colors = listOf(Color(0xFFFFFC00), Color(0xFFFACC15))
        )
        "youtube" -> Brush.linearGradient(
            colors = listOf(Color(0xFFFF0000), Color(0xFFCC0000))
        )
        "whatsapp" -> Brush.linearGradient(
            colors = listOf(Color(0xFF25D366), Color(0xFF128C7E))
        )
        "telegram" -> Brush.linearGradient(
            colors = listOf(Color(0xFF37AEE2), Color(0xFF1E96C8))
        )
        "spotify" -> Brush.linearGradient(
            colors = listOf(Color(0xFF1ED760), Color(0xFF15803D))
        )
        "discord" -> Brush.linearGradient(
            colors = listOf(Color(0xFF5865F2), Color(0xFF4752C4))
        )
        "github" -> Brush.linearGradient(
            colors = listOf(Color(0xFF171515), Color(0xFF2B3137))
        )
        "facebook" -> Brush.linearGradient(
            colors = listOf(Color(0xFF1877F2), Color(0xFF0E5ACE))
        )
        "paypal" -> Brush.linearGradient(
            colors = listOf(Color(0xFF003087), Color(0xFF0070BA))
        )
        "wise" -> Brush.linearGradient(
            colors = listOf(Color(0xFF9FE870), Color(0xFF7AD94A))
        )
        "linkedin" -> Brush.linearGradient(
            colors = listOf(Color(0xFF0A66C2), Color(0xFF004182))
        )
        "baridimob" -> Brush.linearGradient(
            colors = listOf(Color(0xFF0B2559), Color(0xFF1D4ED8))
        )
        "algerie_poste" -> Brush.linearGradient(
            colors = listOf(Color(0xFF0D224E), Color(0xFF1E40AF))
        )
        "redotpay" -> Brush.linearGradient(
            colors = listOf(Color(0xFF1E0B10), Color(0xFFB91C1C))
        )
        else -> Brush.linearGradient(colors = listOf(primary, secondary))
    }
}

private fun DrawScope.drawAuthenticBrandIcon(
    id: String,
    primary: Color,
    secondary: Color,
    accent: Color
) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f
    val s = minOf(w, h)

    when (id) {
        "instagram" -> {
            val box = s * 0.56f
            val sw = (s * 0.075f).coerceAtLeast(1.5f)
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(cx - box / 2f, cy - box / 2f),
                size = Size(box, box),
                cornerRadius = CornerRadius(box * 0.28f, box * 0.28f),
                style = Stroke(width = sw)
            )
            drawCircle(
                color = Color.White,
                radius = s * 0.14f,
                center = Offset(cx, cy),
                style = Stroke(width = sw)
            )
            drawCircle(
                color = Color.White,
                radius = (s * 0.042f).coerceAtLeast(1.3f),
                center = Offset(cx + box * 0.26f, cy - box * 0.26f)
            )
        }
        "facebook" -> {
            val sw = (s * 0.13f).coerceAtLeast(2.2f)
            val p = Path().apply {
                moveTo(cx + s * 0.12f, cy - s * 0.25f)
                lineTo(cx + s * 0.02f, cy - s * 0.25f)
                quadraticTo(cx - s * 0.06f, cy - s * 0.25f, cx - s * 0.06f, cy - s * 0.13f)
                lineTo(cx - s * 0.06f, cy + s * 0.30f)
            }
            drawPath(p, color = Color.White, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawLine(
                color = Color.White,
                start = Offset(cx - s * 0.16f, cy - s * 0.01f),
                end = Offset(cx + s * 0.12f, cy - s * 0.01f),
                strokeWidth = sw * 0.9f,
                cap = StrokeCap.Round
            )
        }
        "whatsapp" -> {
            val sw = (s * 0.07f).coerceAtLeast(1.5f)
            drawCircle(
                color = Color.White,
                radius = s * 0.27f,
                center = Offset(cx, cy - s * 0.02f),
                style = Stroke(width = sw)
            )
            val tail = Path().apply {
                moveTo(cx - s * 0.17f, cy + s * 0.16f)
                lineTo(cx - s * 0.27f, cy + s * 0.28f)
                lineTo(cx - s * 0.06f, cy + s * 0.23f)
                close()
            }
            drawPath(tail, color = Color.White)
            val phone = Path().apply {
                moveTo(cx - s * 0.11f, cy - s * 0.11f)
                quadraticTo(cx - s * 0.09f, cy + s * 0.08f, cx + s * 0.10f, cy + s * 0.10f)
            }
            drawPath(phone, color = Color.White, style = Stroke(width = sw * 1.15f, cap = StrokeCap.Round))
        }
        "telegram" -> {
            val plane = Path().apply {
                moveTo(cx - s * 0.28f, cy + s * 0.01f)
                lineTo(cx + s * 0.26f, cy - s * 0.21f)
                lineTo(cx + s * 0.15f, cy + s * 0.24f)
                lineTo(cx - s * 0.02f, cy + s * 0.10f)
                lineTo(cx - s * 0.11f, cy + s * 0.19f)
                lineTo(cx - s * 0.10f, cy + s * 0.05f)
                close()
            }
            drawPath(plane, color = Color.White)
            drawLine(
                color = Color(0xFF1E96C8),
                start = Offset(cx - s * 0.06f, cy + s * 0.06f),
                end = Offset(cx + s * 0.20f, cy - s * 0.15f),
                strokeWidth = (s * 0.045f).coerceAtLeast(1.1f),
                cap = StrokeCap.Round
            )
        }
        "tiktok" -> {
            val sw = (s * 0.095f).coerceAtLeast(1.8f)
            val drawNote: (Color, Offset) -> Unit = { col, off ->
                drawCircle(
                    color = col,
                    radius = s * 0.11f,
                    center = Offset(cx - s * 0.08f + off.x, cy + s * 0.14f + off.y),
                    style = Stroke(width = sw)
                )
                drawLine(
                    color = col,
                    start = Offset(cx + s * 0.03f + off.x, cy + s * 0.14f + off.y),
                    end = Offset(cx + s * 0.03f + off.x, cy - s * 0.24f + off.y),
                    strokeWidth = sw,
                    cap = StrokeCap.Round
                )
                val arc = Path().apply {
                    moveTo(cx + s * 0.03f + off.x, cy - s * 0.24f + off.y)
                    quadraticTo(
                        cx + s * 0.07f + off.x, cy - s * 0.09f + off.y,
                        cx + s * 0.20f + off.x, cy - s * 0.09f + off.y
                    )
                }
                drawPath(arc, color = col, style = Stroke(width = sw, cap = StrokeCap.Round))
            }
            drawNote(Color(0xFF25F4EE), Offset(-s * 0.03f, -s * 0.025f))
            drawNote(Color(0xFFFE2C55), Offset(s * 0.03f, s * 0.025f))
            drawNote(Color.White, Offset.Zero)
        }
        "snapchat" -> {
            val ghost = Path().apply {
                moveTo(cx, cy - s * 0.27f)
                cubicTo(cx + s * 0.18f, cy - s * 0.27f, cx + s * 0.18f, cy - s * 0.05f, cx + s * 0.16f, cy + s * 0.04f)
                lineTo(cx + s * 0.27f, cy + s * 0.14f)
                lineTo(cx + s * 0.16f, cy + s * 0.18f)
                lineTo(cx + s * 0.22f, cy + s * 0.26f)
                lineTo(cx + s * 0.08f, cy + s * 0.24f)
                lineTo(cx, cy + s * 0.29f)
                lineTo(cx - s * 0.08f, cy + s * 0.24f)
                lineTo(cx - s * 0.22f, cy + s * 0.26f)
                lineTo(cx - s * 0.16f, cy + s * 0.18f)
                lineTo(cx - s * 0.27f, cy + s * 0.14f)
                lineTo(cx - s * 0.16f, cy + s * 0.04f)
                cubicTo(cx - s * 0.18f, cy - s * 0.05f, cx - s * 0.18f, cy - s * 0.27f, cx, cy - s * 0.27f)
                close()
            }
            drawPath(ghost, color = Color.White)
            drawPath(ghost, color = Color(0xFF0F172A), style = Stroke(width = (s * 0.045f).coerceAtLeast(1.1f), join = StrokeJoin.Round))
        }
        "google" -> {
            val r = s * 0.26f
            val sw = (s * 0.115f).coerceAtLeast(2.1f)
            val rectTopLeft = Offset(cx - r, cy - r)
            val rectSize = Size(r * 2f, r * 2f)
            drawArc(Color(0xFFEA4335), 210f, 110f, false, rectTopLeft, rectSize, style = Stroke(sw, cap = StrokeCap.Butt))
            drawArc(Color(0xFFFBBC05), 140f, 75f, false, rectTopLeft, rectSize, style = Stroke(sw, cap = StrokeCap.Butt))
            drawArc(Color(0xFF34A853), 45f, 100f, false, rectTopLeft, rectSize, style = Stroke(sw, cap = StrokeCap.Butt))
            drawArc(Color(0xFF4285F4), -10f, 62f, false, rectTopLeft, rectSize, style = Stroke(sw, cap = StrokeCap.Butt))
            drawLine(
                color = Color(0xFF4285F4),
                start = Offset(cx, cy),
                end = Offset(cx + r * 1.02f, cy),
                strokeWidth = sw,
                cap = StrokeCap.Butt
            )
        }
        "youtube" -> {
            val boxW = s * 0.62f
            val boxH = s * 0.44f
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(cx - boxW / 2f, cy - boxH / 2f),
                size = Size(boxW, boxH),
                cornerRadius = CornerRadius(s * 0.11f, s * 0.11f)
            )
            val tri = Path().apply {
                moveTo(cx - s * 0.08f, cy - s * 0.12f)
                lineTo(cx + s * 0.14f, cy)
                lineTo(cx - s * 0.08f, cy + s * 0.12f)
                close()
            }
            drawPath(tri, color = Color(0xFFFF0000))
        }
        "x_twitter" -> {
            val sw = (s * 0.085f).coerceAtLeast(1.7f)
            drawLine(
                color = Color.White,
                start = Offset(cx - s * 0.22f, cy - s * 0.22f),
                end = Offset(cx + s * 0.22f, cy + s * 0.22f),
                strokeWidth = sw,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color.White,
                start = Offset(cx + s * 0.22f, cy - s * 0.22f),
                end = Offset(cx - s * 0.22f, cy + s * 0.22f),
                strokeWidth = sw * 0.72f,
                cap = StrokeCap.Round
            )
        }
        "netflix" -> {
            val barW = s * 0.11f
            val barH = s * 0.54f
            drawRect(Color(0xFFB91C1C), Offset(cx - s * 0.20f, cy - barH / 2f), Size(barW, barH))
            drawRect(Color(0xFFB91C1C), Offset(cx + s * 0.09f, cy - barH / 2f), Size(barW, barH))
            val diag = Path().apply {
                moveTo(cx - s * 0.20f, cy - barH / 2f)
                lineTo(cx - s * 0.09f, cy - barH / 2f)
                lineTo(cx + s * 0.20f, cy + barH / 2f)
                lineTo(cx + s * 0.09f, cy + barH / 2f)
                close()
            }
            drawPath(diag, color = Color(0xFFE50914))
        }
        "spotify" -> {
            val sw = (s * 0.075f).coerceAtLeast(1.5f)
            listOf(-0.08f to 0.36f, 0.04f to 0.29f, 0.15f to 0.22f).forEach { (yOff, span) ->
                val p = Path().apply {
                    moveTo(cx - s * span, cy + s * yOff)
                    quadraticTo(cx, cy + s * (yOff - 0.11f), cx + s * span, cy + s * (yOff + 0.02f))
                }
                drawPath(p, color = Color(0xFF0A1F12), style = Stroke(width = sw, cap = StrokeCap.Round))
            }
        }
        "discord" -> {
            val headW = s * 0.56f
            val headH = s * 0.40f
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(cx - headW / 2f, cy - headH / 2f),
                size = Size(headW, headH),
                cornerRadius = CornerRadius(s * 0.14f, s * 0.14f)
            )
            drawCircle(Color(0xFF5865F2), radius = (s * 0.055f).coerceAtLeast(1.4f), center = Offset(cx - s * 0.10f, cy))
            drawCircle(Color(0xFF5865F2), radius = (s * 0.055f).coerceAtLeast(1.4f), center = Offset(cx + s * 0.10f, cy))
        }
        "github" -> {
            drawCircle(color = Color.White, radius = s * 0.24f, center = Offset(cx, cy + s * 0.02f))
            val earL = Path().apply {
                moveTo(cx - s * 0.18f, cy - s * 0.10f)
                lineTo(cx - s * 0.20f, cy - s * 0.27f)
                lineTo(cx - s * 0.05f, cy - s * 0.18f)
                close()
            }
            val earR = Path().apply {
                moveTo(cx + s * 0.18f, cy - s * 0.10f)
                lineTo(cx + s * 0.20f, cy - s * 0.27f)
                lineTo(cx + s * 0.05f, cy - s * 0.18f)
                close()
            }
            drawPath(earL, color = Color.White)
            drawPath(earR, color = Color.White)
            drawRoundRect(
                color = Color(0xFF2B3137),
                topLeft = Offset(cx - s * 0.14f, cy - s * 0.03f),
                size = Size(s * 0.28f, s * 0.14f),
                cornerRadius = CornerRadius(s * 0.07f, s * 0.07f)
            )
        }
        "paypal" -> {
            val sw = (s * 0.09f).coerceAtLeast(1.7f)
            val backP = Path().apply {
                moveTo(cx - s * 0.12f, cy + s * 0.22f)
                lineTo(cx - s * 0.06f, cy - s * 0.22f)
                lineTo(cx + s * 0.12f, cy - s * 0.22f)
                cubicTo(cx + s * 0.24f, cy - s * 0.22f, cx + s * 0.22f, cy + s * 0.01f, cx + s * 0.07f, cy + s * 0.01f)
                lineTo(cx - s * 0.08f, cy + s * 0.01f)
            }
            drawPath(backP, color = Color(0xFF38BDF8), style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
            val frontP = Path().apply {
                moveTo(cx - s * 0.04f, cy + s * 0.26f)
                lineTo(cx + s * 0.02f, cy - s * 0.12f)
                lineTo(cx + s * 0.18f, cy - s * 0.12f)
                cubicTo(cx + s * 0.28f, cy - s * 0.12f, cx + s * 0.25f, cy + s * 0.08f, cx + s * 0.11f, cy + s * 0.08f)
                lineTo(cx - s * 0.01f, cy + s * 0.08f)
            }
            drawPath(frontP, color = Color.White, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        "binance" -> {
            val gold = Color(0xFFF3BA2F)
            val dOuter = s * 0.24f
            val dInner = s * 0.10f
            val top = Path().apply {
                moveTo(cx, cy - dOuter)
                lineTo(cx + dOuter, cy)
                lineTo(cx, cy + dOuter)
                lineTo(cx - dOuter, cy)
                close()
            }
            drawPath(top, color = gold, style = Stroke(width = (s * 0.075f).coerceAtLeast(1.5f), join = StrokeJoin.Round))
            val mid = Path().apply {
                moveTo(cx, cy - dInner)
                lineTo(cx + dInner, cy)
                lineTo(cx, cy + dInner)
                lineTo(cx - dInner, cy)
                close()
            }
            drawPath(mid, color = gold)
        }
        "bybit" -> {
            val barW = s * 0.10f
            val barH = s * 0.44f
            drawRect(Color.White, Offset(cx - s * 0.22f, cy - barH / 2f), Size(barW, barH))
            drawRect(Color(0xFFF7A600), Offset(cx - s * 0.04f, cy - barH / 2f), Size(barW, barH))
            drawRect(Color.White, Offset(cx + s * 0.12f, cy - barH / 2f), Size(barW, barH))
        }
        "redotpay" -> {
            val cardW = s * 0.56f
            val cardH = s * 0.38f
            drawRoundRect(
                color = Color.White.copy(alpha = 0.22f),
                topLeft = Offset(cx - cardW / 2f, cy - cardH / 2f),
                size = Size(cardW, cardH),
                cornerRadius = CornerRadius(s * 0.09f, s * 0.09f),
                style = Stroke(width = (s * 0.06f).coerceAtLeast(1.4f))
            )
            drawCircle(color = Color(0xFFEF4444), radius = s * 0.11f, center = Offset(cx, cy))
            drawCircle(color = Color.White, radius = s * 0.045f, center = Offset(cx, cy))
        }
        "baridimob" -> {
            val cardW = s * 0.58f
            val cardH = s * 0.40f
            drawRoundRect(
                color = Color(0xFFFACC15),
                topLeft = Offset(cx - cardW / 2f, cy - cardH / 2f),
                size = Size(cardW, cardH),
                cornerRadius = CornerRadius(s * 0.09f, s * 0.09f)
            )
            drawRect(
                color = Color(0xFF0B2559),
                topLeft = Offset(cx - cardW / 2f, cy - s * 0.08f),
                size = Size(cardW, s * 0.10f)
            )
            drawCircle(
                color = Color(0xFF34D399),
                radius = (s * 0.06f).coerceAtLeast(1.4f),
                center = Offset(cx + cardW * 0.26f, cy + cardH * 0.22f)
            )
        }
        "algerie_poste" -> {
            val gold = Color(0xFFFACC15)
            val envW = s * 0.58f
            val envH = s * 0.38f
            drawRoundRect(
                color = gold,
                topLeft = Offset(cx - envW / 2f, cy - envH / 2f),
                size = Size(envW, envH),
                cornerRadius = CornerRadius(s * 0.07f, s * 0.07f),
                style = Stroke(width = (s * 0.065f).coerceAtLeast(1.5f))
            )
            val vLine = Path().apply {
                moveTo(cx - envW / 2f, cy - envH / 2f + s * 0.03f)
                lineTo(cx, cy + s * 0.04f)
                lineTo(cx + envW / 2f, cy - envH / 2f + s * 0.03f)
            }
            drawPath(vLine, color = gold, style = Stroke(width = (s * 0.06f).coerceAtLeast(1.4f), join = StrokeJoin.Round))
        }
        "wise" -> {
            val flag = Path().apply {
                moveTo(cx - s * 0.18f, cy - s * 0.24f)
                lineTo(cx + s * 0.24f, cy - s * 0.24f)
                lineTo(cx + s * 0.12f, cy - s * 0.04f)
                lineTo(cx + s * 0.22f, cy + s * 0.10f)
                lineTo(cx - s * 0.08f, cy + s * 0.10f)
                lineTo(cx - s * 0.18f, cy + s * 0.26f)
                close()
            }
            drawPath(flag, color = Color(0xFF163300))
        }
        "linkedin" -> {
            val sw = (s * 0.095f).coerceAtLeast(1.8f)
            drawCircle(Color.White, radius = (s * 0.05f).coerceAtLeast(1.3f), center = Offset(cx - s * 0.16f, cy - s * 0.18f))
            drawLine(Color.White, Offset(cx - s * 0.16f, cy - s * 0.05f), Offset(cx - s * 0.16f, cy + s * 0.22f), strokeWidth = sw, cap = StrokeCap.Round)
            val nPath = Path().apply {
                moveTo(cx - s * 0.02f, cy + s * 0.22f)
                lineTo(cx - s * 0.02f, cy - s * 0.05f)
                quadraticTo(cx + s * 0.18f, cy - s * 0.14f, cx + s * 0.18f, cy + s * 0.05f)
                lineTo(cx + s * 0.18f, cy + s * 0.22f)
            }
            drawPath(nPath, color = Color.White, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        else -> {
            // Custom App key/shield icon
            val sw = (s * 0.075f).coerceAtLeast(1.5f)
            drawCircle(
                color = Color.White,
                radius = s * 0.13f,
                center = Offset(cx - s * 0.08f, cy - s * 0.08f),
                style = Stroke(width = sw)
            )
            drawLine(
                color = Color.White,
                start = Offset(cx + s * 0.01f, cy + s * 0.01f),
                end = Offset(cx + s * 0.23f, cy + s * 0.23f),
                strokeWidth = sw,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color.White,
                start = Offset(cx + s * 0.14f, cy + s * 0.14f),
                end = Offset(cx + s * 0.21f, cy + s * 0.07f),
                strokeWidth = sw,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun VaultHoldActionDialog(
    title: String,
    subtitle: String,
    preset: VaultAppPreset,
    isPinned: Boolean,
    isArabic: Boolean,
    onPinToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LiquidGlassTheme.colors
    val primary = preset.primaryColor()
    var confirmingDelete by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .testTag("vault_hold_action_dialog"),
            shape = RoundedCornerShape(26.dp),
            backgroundColor = colors.dialogBackground,
            borderColor = primary.copy(alpha = 0.50f)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VaultAppEmblemBadge(preset = preset, size = 46.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            color = colors.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subtitle,
                            color = primary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = if (isArabic) "إغلاق" else "Close",
                            tint = colors.textMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (!confirmingDelete) {
                    // 1. Pin / Unpin Action
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(AmberNeon.copy(alpha = 0.14f))
                            .border(1.dp, AmberNeon.copy(alpha = 0.42f), RoundedCornerShape(14.dp))
                            .clickable {
                                onPinToggle()
                                onDismiss()
                            }
                            .padding(horizontal = 14.dp, vertical = 13.dp)
                            .testTag("hold_action_pin"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isPinned) Icons.Outlined.PushPin else Icons.Default.PushPin,
                            contentDescription = null,
                            tint = AmberNeon,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isPinned) {
                                if (isArabic) "إلغاء التثبيت من الأعلى" else "Unpin from Top"
                            } else {
                                if (isArabic) "تثبيت التطبيق في الأعلى" else "Pin to Top"
                            },
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. Edit Action
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(primary.copy(alpha = 0.15f))
                            .border(1.dp, primary.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                            .clickable {
                                onDismiss()
                                onEdit()
                            }
                            .padding(horizontal = 14.dp, vertical = 13.dp)
                            .testTag("hold_action_edit"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isArabic) "تعديل البيانات" else "Edit Details",
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3. Delete Action
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(RoseNeon.copy(alpha = 0.14f))
                            .border(1.dp, RoseNeon.copy(alpha = 0.42f), RoundedCornerShape(14.dp))
                            .clickable { confirmingDelete = true }
                            .padding(horizontal = 14.dp, vertical = 13.dp)
                            .testTag("hold_action_delete"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = RoseNeon,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isArabic) "حذف الحساب نهائياً" else "Delete Account",
                            color = RoseNeon,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(RoseNeon.copy(alpha = 0.12f))
                            .border(1.dp, RoseNeon.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = if (isArabic) "هل أنت متأكد من حذف \"$title\" نهائياً؟"
                            else "Permanently delete \"$title\"?",
                            color = colors.textPrimary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GlassButton(
                                text = if (isArabic) "تراجع" else "Back",
                                containerColor = colors.textMuted.copy(alpha = 0.25f),
                                contentColor = colors.textPrimary,
                                modifier = Modifier.weight(1f),
                                onClick = { confirmingDelete = false }
                            )
                            GlassButton(
                                text = if (isArabic) "تأكيد الحذف" else "Delete",
                                containerColor = RoseNeon,
                                contentColor = Color.White,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("hold_action_confirm_delete"),
                                onClick = {
                                    onDelete()
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }
        }
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
