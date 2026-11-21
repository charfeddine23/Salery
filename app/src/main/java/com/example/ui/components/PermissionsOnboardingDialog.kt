package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.ArabicStrings
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.LocalScreenshotProtection
import com.example.update.AppUpdateManager
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val SecurityIndigo = Color(0xFF6366F1)

@Composable
fun PermissionsOnboardingDialog(
    onRequestInstallSourcesSettings: () -> Unit,
    onCompleted: (enableSmsSync: Boolean) -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors
    val isArabic = strings is ArabicStrings
    val isDark = colors.isDark

    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }

    var refreshTick by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshTick++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val contactsGranted = remember(refreshTick) {
        hasPerm(context, Manifest.permission.READ_CONTACTS)
    }
    val smsGranted = remember(refreshTick) {
        hasPerm(context, Manifest.permission.READ_SMS) &&
            hasPerm(context, Manifest.permission.RECEIVE_SMS)
    }
    val notificationsGranted = remember(refreshTick) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPerm(context, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true
        }
    }
    val installSourcesAllowed = remember(refreshTick) {
        AppUpdateManager.canRequestPackageInstalls(context)
    }

    val permissionsToRequest = remember {
        buildList {
            add(Manifest.permission.READ_CONTACTS)
            add(Manifest.permission.READ_SMS)
            add(Manifest.permission.RECEIVE_SMS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }

    val multiPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshTick++
        onCompleted(hasPerm(context, Manifest.permission.READ_SMS))
    }

    val cardScale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.88f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "onboarding_card_scale"
    )

    val infinite = rememberInfiniteTransition(label = "onboarding_security_anim")
    val ambientPulse by infinite.animateFloat(
        initialValue = 0.86f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "onboarding_ambient_pulse"
    )
    val sheenProgress by infinite.animateFloat(
        initialValue = -0.35f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "onboarding_btn_sheen"
    )

    val isScreenshotProtected = LocalScreenshotProtection.current
    Dialog(
        onDismissRequest = onSkip,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            securePolicy = if (isScreenshotProtected) SecureFlagPolicy.SecureOn else SecureFlagPolicy.SecureOff
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 410.dp)
                    .scale(cardScale)
                    .testTag("permissions_onboarding_dialog"),
                shape = RoundedCornerShape(30.dp),
                backgroundColor = if (isDark) {
                    Color(0xFF0A111D).copy(alpha = 0.97f)
                } else {
                    colors.dialogBackground
                },
                borderColor = CyanNeon.copy(alpha = 0.55f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            val topCenter = Offset(size.width * 0.5f, size.height * 0.16f)
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        EmeraldNeon.copy(alpha = (if (isDark) 0.16f else 0.10f) * ambientPulse),
                                        CyanNeon.copy(alpha = (if (isDark) 0.12f else 0.08f) * ambientPulse),
                                        SecurityIndigo.copy(alpha = 0.07f),
                                        Color.Transparent
                                    ),
                                    center = topCenter,
                                    radius = size.width * 0.62f * ambientPulse
                                )
                            )
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top privacy shield tag
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(EmeraldNeon.copy(alpha = 0.14f))
                                .border(0.9.dp, EmeraldNeon.copy(alpha = 0.45f), RoundedCornerShape(50))
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = EmeraldNeon,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isArabic) "تشفير محلي 100% • خصوصية مطلقة"
                                    else "100% On-Device • Zero Cloud Tracking",
                                    color = EmeraldNeon,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Animated High-Craft Security Shield Vault Emblem
                        SecurityVaultHeroEmblem()

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = if (isArabic) "إعداد الأمان والصلاحيات الذكية"
                            else "Smart Security & Permissions Setup",
                            color = colors.textPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            lineHeight = 26.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (isArabic) {
                                "لتفعيل مزامنة رسائل ALG Poste وجهات الاتصال والتنبيهات الآمنة، يرجى تفعيل الصلاحيات التالية:"
                            } else {
                                "Enable these essential permissions for ALG Poste SMS sync, contact linking, and instant alerts:"
                            },
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Permission Cards
                        PermissionHighlightRow(
                            icon = Icons.Default.Sms,
                            accent = SecurityIndigo,
                            title = if (isArabic) "رسائل BaridiMob / ALG Poste" else "ALG Poste / BaridiMob SMS",
                            desc = if (isArabic) "قراءة رسائل السحب والدخل واقتراح إضافتها لحسابك"
                            else "Analyze ALG Poste alerts and suggest Expenses & Income",
                            isGranted = smsGranted
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        PermissionHighlightRow(
                            icon = Icons.Default.ContactPhone,
                            accent = CyanNeon,
                            title = if (isArabic) "الوصول إلى جهات الاتصال" else "Contacts Access",
                            desc = if (isArabic) "ربط ملفات الـ CCP والديون بأسماء وأرقام جهات اتصالك"
                            else "Link CCP profiles and debts directly from phone contacts",
                            isGranted = contactsGranted
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        PermissionHighlightRow(
                            icon = Icons.Default.NotificationsActive,
                            accent = EmeraldNeon,
                            title = if (isArabic) "التنبيهات المالية الفورية" else "Instant Financial Alerts",
                            desc = if (isArabic) "تذكير بمواعيد الديون واقتراحات المعاملات الواردة"
                            else "Reminders for due debts and newly detected SMS alerts",
                            isGranted = notificationsGranted
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Optional direct shortcut to enable Install Unknown Apps for seamless in-app updates
                        PermissionHighlightRow(
                            icon = Icons.Default.SystemUpdate,
                            accent = AmberNeon,
                            title = if (isArabic) "تثبيت التحديثات داخل التطبيق" else "In-App Update Installer",
                            desc = if (isArabic) {
                                if (installSourcesAllowed) "مفعّل — التحديثات تثبّت تلقائياً من داخل التطبيق"
                                else "اضغط هنا لتفعيل (Install unknown apps) للتحديث السلس"
                            } else {
                                if (installSourcesAllowed) "Enabled — ready for seamless in-app APK updates"
                                else "Tap here to allow 'Install unknown apps' in Settings"
                            },
                            isGranted = installSourcesAllowed,
                            onClick = if (!installSourcesAllowed) onRequestInstallSourcesSettings else null
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Primary Shimmer CTA Button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .shadow(
                                    elevation = 12.dp,
                                    shape = RoundedCornerShape(50),
                                    spotColor = CyanNeon.copy(alpha = 0.55f),
                                    ambientColor = EmeraldNeon.copy(alpha = 0.45f)
                                )
                                .clip(RoundedCornerShape(50))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(EmeraldNeon, CyanNeon, SecurityIndigo)
                                    )
                                )
                                .drawBehind {
                                    val sheenW = size.width * 0.34f
                                    val startX = (size.width + sheenW) * sheenProgress - sheenW
                                    drawRect(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.White.copy(alpha = 0.30f),
                                                Color.Transparent
                                            ),
                                            start = Offset(startX, 0f),
                                            end = Offset(startX + sheenW, size.height)
                                        )
                                    )
                                }
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(color = Color.White),
                                    onClick = {
                                        val allGranted = smsGranted && contactsGranted && notificationsGranted
                                        if (allGranted) {
                                            onCompleted(true)
                                        } else {
                                            multiPermissionLauncher.launch(permissionsToRequest)
                                        }
                                    }
                                )
                                .testTag("onboarding_grant_permissions_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = Color(0xFF041C24),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isArabic) "تفعيل الصلاحيات الآن" else "Enable Permissions Now",
                                    color = Color(0xFF041C24),
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(50))
                                .clickable(onClick = onSkip)
                                .testTag("onboarding_skip_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isArabic) "متابعة الآن وتفعيلها لاحقاً" else "Continue and set up later",
                                color = colors.textMuted,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionHighlightRow(
    icon: ImageVector,
    accent: Color,
    title: String,
    desc: String,
    isGranted: Boolean,
    onClick: (() -> Unit)? = null
) {
    val colors = LiquidGlassTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isGranted) EmeraldNeon.copy(alpha = 0.08f) else colors.glassCard
            )
            .border(
                width = 1.dp,
                color = if (isGranted) EmeraldNeon.copy(alpha = 0.45f) else accent.copy(alpha = 0.30f),
                shape = RoundedCornerShape(16.dp)
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = desc,
                color = colors.textMuted,
                fontSize = 10.8.sp,
                lineHeight = 14.sp
            )
        }
        if (isGranted) {
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Granted",
                tint = EmeraldNeon,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * High-craft animated Cyber-Security Vault Shield Emblem:
 * - Expanding sonar radar waves
 * - Rotating orbital cyber-ring with glowing energy arc
 * - Sculpted multi-stop neon shield with live laser scanner sweep & orbiting security particles
 */
@Composable
private fun SecurityVaultHeroEmblem() {
    val infinite = rememberInfiniteTransition(label = "security_vault_hero")
    val sonarExpand by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sonar_wave"
    )
    val orbitDeg by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_ring"
    )
    val scanPhase by infinite.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_scan"
    )
    val pulse by infinite.animateFloat(
        initialValue = 0.80f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emblem_pulse"
    )

    Box(
        modifier = Modifier.size(126.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = size.minDimension * 0.34f

            // Expanding sonar security pulses
            drawCircle(
                color = EmeraldNeon.copy(alpha = (1f - sonarExpand) * 0.42f),
                radius = r + (size.minDimension * 0.17f * sonarExpand),
                center = Offset(cx, cy),
                style = Stroke(width = 2.dp.toPx())
            )
            val secondWave = (sonarExpand + 0.5f) % 1f
            drawCircle(
                color = CyanNeon.copy(alpha = (1f - secondWave) * 0.30f),
                radius = r + (size.minDimension * 0.17f * secondWave),
                center = Offset(cx, cy),
                style = Stroke(width = 1.6.dp.toPx())
            )

            // Backlit aurora glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        CyanNeon.copy(alpha = 0.48f * pulse),
                        EmeraldNeon.copy(alpha = 0.28f * pulse),
                        SecurityIndigo.copy(alpha = 0.18f * pulse),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = r * 1.48f
                ),
                radius = r * 1.48f,
                center = Offset(cx, cy)
            )

            // Rotating orbital security arc + orbiting node particles
            rotate(degrees = orbitDeg, pivot = Offset(cx, cy)) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            EmeraldNeon,
                            CyanNeon,
                            Color.White,
                            Color.Transparent
                        )
                    ),
                    startAngle = 0f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(cx - r * 1.18f, cy - r * 1.18f),
                    size = Size(r * 2.36f, r * 2.36f),
                    style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Orbiting particles
            val particleCount = 4
            for (i in 0 until particleCount) {
                val angleRad = ((orbitDeg + i * (360f / particleCount)) * PI / 180.0)
                val px = cx + (r * 1.18f) * cos(angleRad).toFloat()
                val py = cy + (r * 1.18f) * sin(angleRad).toFloat()
                drawCircle(
                    color = if (i % 2 == 0) CyanNeon else EmeraldNeon,
                    radius = 2.6.dp.toPx(),
                    center = Offset(px, py)
                )
            }

            // Core shield geometry
            val shieldW = r * 1.16f
            val shieldH = r * 1.38f
            val top = cy - shieldH * 0.50f
            val shieldPath = Path().apply {
                moveTo(cx, top)
                lineTo(cx + shieldW * 0.52f, top + shieldH * 0.18f)
                cubicTo(
                    cx + shieldW * 0.52f, top + shieldH * 0.56f,
                    cx + shieldW * 0.34f, top + shieldH * 0.82f,
                    cx, top + shieldH
                )
                cubicTo(
                    cx - shieldW * 0.34f, top + shieldH * 0.82f,
                    cx - shieldW * 0.52f, top + shieldH * 0.56f,
                    cx - shieldW * 0.52f, top + shieldH * 0.18f
                )
                close()
            }

            drawPath(
                path = shieldPath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        EmeraldNeon,
                        CyanNeon,
                        SecurityIndigo
                    ),
                    start = Offset(cx - shieldW * 0.4f, top),
                    end = Offset(cx + shieldW * 0.4f, top + shieldH)
                )
            )

            drawPath(
                path = shieldPath,
                brush = Brush.linearGradient(
                    colors = listOf(Color.White.copy(alpha = 0.92f), CyanNeon, EmeraldNeon)
                ),
                style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round)
            )

            // Center crisp checkmark emblem inside the security shield
            val check = Path().apply {
                moveTo(cx - shieldW * 0.20f, cy + shieldH * 0.02f)
                lineTo(cx - shieldW * 0.04f, cy + shieldH * 0.16f)
                lineTo(cx + shieldW * 0.22f, cy - shieldH * 0.12f)
            }
            drawPath(
                path = check,
                color = Color(0xFF04202C),
                style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawPath(
                path = check,
                color = Color.White,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Live cyber laser scan beam sweeping vertically across the shield
            val laserY = top + shieldH * scanPhase
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.88f),
                        CyanNeon,
                        Color.White.copy(alpha = 0.88f),
                        Color.Transparent
                    ),
                    startX = cx - shieldW * 0.48f,
                    endX = cx + shieldW * 0.48f
                ),
                start = Offset(cx - shieldW * 0.46f, laserY),
                end = Offset(cx + shieldW * 0.46f, laserY),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

private fun hasPerm(context: Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
