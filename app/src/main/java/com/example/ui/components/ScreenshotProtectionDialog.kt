package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.ArabicStrings
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassTheme
import kotlin.math.PI
import kotlin.math.sin

private val ShieldIndigo = Color(0xFF6366F1)

@Composable
fun ScreenshotProtectionDialog(
    isProtected: Boolean,
    onToggleProtection: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors
    val isArabic = strings is ArabicStrings
    val isDark = colors.isDark

    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }

    val cardScale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.88f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "shield_dialog_scale"
    )

    val ambientTransition = rememberInfiniteTransition(label = "shield_dialog_ambient")
    val ambientPulse by ambientTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shield_ambient_pulse"
    )

    val accentTint by animateColorAsState(
        targetValue = if (isProtected) EmeraldNeon else colors.textMuted,
        animationSpec = tween(350),
        label = "shield_accent_tint"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            securePolicy = if (isProtected) SecureFlagPolicy.SecureOn else SecureFlagPolicy.SecureOff
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 376.dp)
                    .scale(cardScale)
                    .testTag("screenshot_protection_dialog"),
                shape = RoundedCornerShape(30.dp),
                backgroundColor = if (isDark) {
                    Color(0xFF0E1420).copy(alpha = 0.96f)
                } else {
                    colors.dialogBackground
                },
                borderColor = if (isProtected) CyanNeon.copy(alpha = 0.55f) else colors.glassBorder
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            if (isProtected) {
                                val topCenter = Offset(size.width * 0.5f, size.height * 0.24f)
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            EmeraldNeon.copy(alpha = (if (isDark) 0.16f else 0.10f) * ambientPulse),
                                            CyanNeon.copy(alpha = (if (isDark) 0.12f else 0.06f) * ambientPulse),
                                            ShieldIndigo.copy(alpha = if (isDark) 0.06f else 0.03f),
                                            Color.Transparent
                                        ),
                                        center = topCenter,
                                        radius = size.width * 0.62f * ambientPulse
                                    )
                                )
                            }
                        }
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top Bar: Close Button + Status Pill
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("close_shield_dialog_button")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(colors.glassCard)
                                        .border(0.8.dp, colors.glassBorderSubtle, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = strings.cancel,
                                        tint = colors.textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isProtected) EmeraldNeon.copy(alpha = 0.16f)
                                        else colors.glassCard
                                    )
                                    .border(
                                        width = 0.9.dp,
                                        color = if (isProtected) EmeraldNeon.copy(alpha = 0.45f) else colors.glassBorderSubtle,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(accentTint)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isProtected) {
                                            if (isArabic) "الحماية مفعلة" else "ACTIVE SHIELD"
                                        } else {
                                            if (isArabic) "الحماية معطلة" else "PAUSED"
                                        },
                                        color = accentTint,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }

                        // Hero Shield Medallion (luminous multi-color neon aura when active, calm neutral when disabled)
                        ShieldHeroMedallion(
                            isProtected = isProtected,
                            isDark = isDark
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Bold Title
                        Text(
                            text = if (isProtected) {
                                if (isArabic) "حماية قصوى • منع التصوير مفعل"
                                else "Strict Shield • Screenshots Blocked"
                            } else {
                                if (isArabic) "منع لقطات الشاشة معطل"
                                else "No-Screenshot Disabled"
                            },
                            color = colors.textPrimary,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            lineHeight = 25.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Frosted Detailed Explanation Box (originally at bottom of Cards Hub)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    if (isDark) Color.White.copy(alpha = 0.05f)
                                    else Color(0xFFF1F5F9).copy(alpha = 0.85f)
                                )
                                .border(
                                    width = 0.9.dp,
                                    color = if (isProtected) CyanNeon.copy(alpha = 0.30f) else colors.glassBorderSubtle,
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = if (isProtected) {
                                    if (isArabic) {
                                        "لقطات الشاشة وتسجيل الشاشة ممنوعان إطلاقاً في صفحة البطاقات لحماية بياناتك ومعلوماتك المصرفية ورمز الأمان CVC."
                                    } else {
                                        "Screenshots and screen recording are strictly disabled on the Cards Hub to protect your card details and sensitive banking information."
                                    }
                                } else {
                                    if (isArabic) {
                                        "تم تعطيل حظر التصوير مؤقتاً في صفحة البطاقات؛ يمكنك التقاط لقطات الشاشة الآن أو إعادة تفعيل الحماية في أي وقت."
                                    } else {
                                        "Screenshot blocking is currently paused on the Cards Hub. You can capture screenshots or re-enable protection anytime."
                                    }
                                },
                                color = colors.textSecondary,
                                fontSize = 12.5.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 19.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Interactive Toggle Row inside Popup
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.glassCard)
                                .border(
                                    width = 1.dp,
                                    color = if (isProtected) EmeraldNeon.copy(alpha = 0.38f) else colors.glassBorderSubtle,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { onToggleProtection(!isProtected) }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .testTag("dialog_screenshot_toggle_row"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isProtected) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = if (isProtected) EmeraldNeon else colors.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.screenshotProtectionTitle,
                                    color = colors.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isProtected) strings.screenshotProtectionActive else strings.screenshotProtectionDisabled,
                                    color = if (isProtected) EmeraldNeon else colors.textMuted,
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = isProtected,
                                onCheckedChange = { onToggleProtection(it) },
                                modifier = Modifier.testTag("dialog_screenshot_switch"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = EmeraldNeon,
                                    checkedTrackColor = EmeraldNeon.copy(alpha = 0.35f),
                                    uncheckedThumbColor = colors.textMuted,
                                    uncheckedTrackColor = colors.glassSurface
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Signature Capsule Close/Confirm Button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .shadow(
                                    elevation = if (isProtected) 10.dp else 3.dp,
                                    shape = RoundedCornerShape(50),
                                    spotColor = if (isProtected) CyanNeon.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.2f)
                                )
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (isProtected) {
                                        Brush.horizontalGradient(
                                            colors = listOf(EmeraldNeon, CyanNeon, ShieldIndigo)
                                        )
                                    } else {
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                colors.textMuted.copy(alpha = 0.45f),
                                                colors.textMuted.copy(alpha = 0.35f)
                                            )
                                        )
                                    }
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(color = Color.White),
                                    onClick = onDismiss
                                )
                                .testTag("shield_dialog_done_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isArabic) "حسناً" else "Done",
                                color = Color.White,
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.3.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShieldHeroMedallion(
    isProtected: Boolean,
    isDark: Boolean
) {
    val infinite = rememberInfiniteTransition(label = "shield_medallion")
    val orbitAngle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shield_orbit"
    )
    val bobPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shield_bob"
    )
    val sonarExpand by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shield_sonar"
    )

    Box(
        modifier = Modifier.size(124.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val baseR = size.minDimension * 0.35f

            if (isProtected) {
                // Sonar expanding protective wave
                drawCircle(
                    color = CyanNeon.copy(alpha = (1f - sonarExpand) * 0.38f),
                    radius = baseR + (size.minDimension * 0.14f * sonarExpand),
                    center = Offset(cx, cy),
                    style = Stroke(width = 2.dp.toPx())
                )

                // Soft glowing outer halo
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            EmeraldNeon.copy(alpha = if (isDark) 0.24f else 0.16f),
                            CyanNeon.copy(alpha = if (isDark) 0.14f else 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = baseR * 1.45f
                    ),
                    radius = baseR * 1.45f,
                    center = Offset(cx, cy)
                )

                // Deep cosmic inner disc
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF0E2238), Color(0xFF090F1D)),
                        center = Offset(cx, cy),
                        radius = baseR
                    ),
                    radius = baseR,
                    center = Offset(cx, cy)
                )

                // Rotating multi-color luminous ring
                rotate(degrees = orbitAngle, pivot = Offset(cx, cy)) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                EmeraldNeon,
                                CyanNeon,
                                ShieldIndigo,
                                Color(0xFF38BDF8),
                                EmeraldNeon
                            )
                        ),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(cx - baseR * 1.07f, cy - baseR * 1.07f),
                        size = Size(baseR * 2.14f, baseR * 2.14f),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Twinkling protection sparks
                val pulse = 0.55f + 0.45f * sin(bobPhase)
                drawCircle(
                    color = CyanNeon.copy(alpha = pulse),
                    radius = 2.8.dp.toPx(),
                    center = Offset(cx - baseR * 0.62f, cy - baseR * 0.46f)
                )
                drawCircle(
                    color = EmeraldNeon.copy(alpha = 1.1f - pulse * 0.4f),
                    radius = 2.4.dp.toPx(),
                    center = Offset(cx + baseR * 0.60f, cy - baseR * 0.42f)
                )
            } else {
                // Calm muted state when disabled
                drawCircle(
                    color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.65f) else Color(0xFFE2E8F0),
                    radius = baseR,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = Color(0xFF64748B).copy(alpha = 0.45f),
                    radius = baseR * 1.06f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.8.dp.toPx())
                )
            }

            // Central Shield Vector Emblem
            val sw = baseR * 0.86f
            val sh = baseR * 1.04f
            val shieldPath = Path().apply {
                moveTo(cx, cy - sh * 0.52f)
                lineTo(cx + sw * 0.52f, cy - sh * 0.30f)
                cubicTo(
                    cx + sw * 0.52f, cy + sh * 0.14f,
                    cx + sw * 0.32f, cy + sh * 0.40f,
                    cx, cy + sh * 0.56f
                )
                cubicTo(
                    cx - sw * 0.32f, cy + sh * 0.40f,
                    cx - sw * 0.52f, cy + sh * 0.14f,
                    cx - sw * 0.52f, cy - sh * 0.30f
                )
                close()
            }

            if (isProtected) {
                drawPath(
                    path = shieldPath,
                    brush = Brush.linearGradient(
                        colors = listOf(EmeraldNeon, CyanNeon, ShieldIndigo),
                        start = Offset(cx - sw, cy - sh),
                        end = Offset(cx + sw, cy + sh)
                    )
                )
                // Inner checkmark seal
                val checkPath = Path().apply {
                    moveTo(cx - sw * 0.22f, cy + sh * 0.02f)
                    lineTo(cx - sw * 0.04f, cy + sh * 0.18f)
                    lineTo(cx + sw * 0.24f, cy - sh * 0.14f)
                }
                drawPath(
                    path = checkPath,
                    color = Color.White,
                    style = Stroke(
                        width = 3.2.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            } else {
                drawPath(
                    path = shieldPath,
                    color = Color(0xFF64748B).copy(alpha = 0.55f),
                    style = Stroke(width = 2.4.dp.toPx(), join = StrokeJoin.Round)
                )
            }
        }
    }
}
