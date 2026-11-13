package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.i18n.APP_NAME
import com.example.ui.i18n.APP_VERSION
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.ArabicStrings
import com.example.ui.i18n.formatPercent
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.RoseNeon
import com.example.update.AppUpdateUiState
import com.example.update.GITHUB_RELEASES_WEB_URL
import com.example.update.formatBytesToMb
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val UpdateCrimsonStart = Color(0xFFE11D48)
private val UpdateCrimsonEnd = Color(0xFFDC2626)

@Composable
fun AppUpdateDialog(
    state: AppUpdateUiState,
    canInstallPackages: Boolean,
    onDismiss: () -> Unit,
    onStartDownload: () -> Unit,
    onInstallApk: () -> Unit,
    onRequestInstallPermission: () -> Unit,
    onRetryCheck: () -> Unit,
    onOpenReleaseBrowser: (String) -> Unit
) {
    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors
    val isArabic = strings is ArabicStrings
    val isDark = colors.isDark

    val isDownloading = state is AppUpdateUiState.Downloading
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }

    val cardScale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.88f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "update_dialog_scale"
    )

    val ambientTransition = rememberInfiniteTransition(label = "update_ambient")
    val ambientPulse by ambientTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_pulse"
    )
    val sheenProgress by ambientTransition.animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "button_sheen"
    )

    val release = when (state) {
        is AppUpdateUiState.Available -> state.release
        is AppUpdateUiState.Downloading -> state.release
        is AppUpdateUiState.ReadyToInstall -> state.release
        is AppUpdateUiState.Error -> state.release
        else -> null
    }

    Dialog(
        onDismissRequest = { if (!isDownloading) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isDownloading,
            dismissOnClickOutside = !isDownloading
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
                    .widthIn(max = 370.dp)
                    .scale(cardScale)
                    .testTag("app_update_dialog"),
                shape = RoundedCornerShape(30.dp),
                backgroundColor = if (isDark) {
                    Color(0xFF10131C).copy(alpha = 0.96f)
                } else {
                    colors.dialogBackground
                },
                borderColor = colors.glassBorder
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            // Subtle dynamic cosmic/aurora aura behind the rocket medallion
                            val topCenter = Offset(size.width * 0.5f, size.height * 0.25f)
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        UpdateCrimsonStart.copy(alpha = (if (isDark) 0.16f else 0.10f) * ambientPulse),
                                        colors.primaryNeon.copy(alpha = (if (isDark) 0.08f else 0.05f)),
                                        Color.Transparent
                                    ),
                                    center = topCenter,
                                    radius = size.width * 0.58f * ambientPulse
                                )
                            )
                        }
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top Row: Subtle Version Chip & Circular X Close Button (48dp touch target)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onDismiss,
                                enabled = !isDownloading,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("close_update_dialog_button")
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
                                        tint = if (isDownloading) colors.textMuted.copy(alpha = 0.4f) else colors.textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            if (release != null) {
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(colors.primaryNeon.copy(alpha = 0.12f))
                                            .border(0.8.dp, colors.primaryNeon.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "v$APP_VERSION → ${release.tagName}",
                                            color = colors.primaryNeon,
                                            fontSize = 10.5.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.size(48.dp))
                            }
                        }

                        // Artistic Dynamic Animated Rocket Emblem (inspired by reference image)
                        UpdateRocketEmblem(
                            isLaunching = isDownloading || state is AppUpdateUiState.ReadyToInstall,
                            isUpToDate = state is AppUpdateUiState.UpToDate,
                            isChecking = state is AppUpdateUiState.Checking,
                            isDark = isDark
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Main Bold Title
                        val mainTitle = when (state) {
                            is AppUpdateUiState.Checking -> strings.checkingForUpdates
                            is AppUpdateUiState.UpToDate -> strings.alreadyUpToDate
                            is AppUpdateUiState.Downloading -> strings.downloadingUpdate
                            is AppUpdateUiState.ReadyToInstall -> if (isArabic) "التحديث جاهز للتثبيت" else "Update Ready to Install"
                            is AppUpdateUiState.Error -> strings.updateCheckFailed
                            else -> strings.updateAvailableTitle
                        }
                        Text(
                            text = mainTitle,
                            color = colors.textPrimary,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            lineHeight = 27.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Vibrant Accent Subtitle (e.g. "تحديث جديد • 14.2 MB")
                        val accentSub = when (state) {
                            is AppUpdateUiState.UpToDate -> "v${state.currentVersion}"
                            is AppUpdateUiState.Checking -> "GitHub • $APP_NAME"
                            is AppUpdateUiState.Error -> state.message
                            else -> {
                                val verText = release?.tagName ?: ""
                                val sizeText = release?.apkSizeBytes?.takeIf { it > 0L }?.let { formatBytesToMb(it) }
                                listOfNotNull(
                                    strings.updateBadgeLabel,
                                    verText.takeIf { it.isNotBlank() },
                                    sizeText
                                ).joinToString(" • ")
                            }
                        }
                        Text(
                            text = accentSub,
                            color = when (state) {
                                is AppUpdateUiState.UpToDate -> EmeraldNeon
                                is AppUpdateUiState.ReadyToInstall -> EmeraldNeon
                                is AppUpdateUiState.Error -> RoseNeon
                                else -> Color(0xFFEF4444)
                            },
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Concise Subtitle / Permission Guidance / Release Summary
                        val bodyText = when (state) {
                            is AppUpdateUiState.ReadyToInstall -> {
                                if (!canInstallPackages) strings.updateInstallPermNote
                                else if (isArabic) "اكتمل التحميل بنجاح. اضغط لتثبيت الإصدار الجديد."
                                else "Download complete. Tap below to install the new version."
                            }
                            is AppUpdateUiState.UpToDate -> {
                                if (isArabic) "كل المميزات والتحسينات محدثة على جهازك."
                                else "All latest SaleryPulse features are already installed."
                            }
                            is AppUpdateUiState.Checking -> {
                                if (isArabic) "يتم الاتصال بـ GitHub Releases للتحقق من أحدث نسخة..."
                                else "Connecting to GitHub Releases..."
                            }
                            is AppUpdateUiState.Error -> {
                                if (isArabic) "تأكد من اتصالك بالإنترنت وحاول مرة أخرى."
                                else "Check your internet connection and try again."
                            }
                            else -> {
                                val cleanedNotes = release?.releaseNotes
                                    ?.lines()
                                    ?.map { it.trim().removePrefix("-").removePrefix("*").removePrefix("#").trim() }
                                    ?.firstOrNull { it.isNotBlank() && !it.startsWith("###") }
                                cleanedNotes ?: strings.updateAvailableDesc
                            }
                        }
                        Text(
                            text = bodyText,
                            color = colors.textSecondary,
                            fontSize = 12.5.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )

                        // Animated Live Download Progress Section
                        AnimatedVisibility(
                            visible = state is AppUpdateUiState.Downloading,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            if (state is AppUpdateUiState.Downloading) {
                                val pct = (state.progress * 100f).toInt().coerceIn(0, 100)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Smooth glowing track bar
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(10.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(colors.glassCard)
                                            .border(0.7.dp, colors.glassBorderSubtle, RoundedCornerShape(50))
                                    ) {
                                        val animatedProgress by animateFloatAsState(
                                            targetValue = state.progress,
                                            animationSpec = tween(220),
                                            label = "dl_bar"
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(animatedProgress.coerceAtLeast(0.04f))
                                                .clip(RoundedCornerShape(50))
                                                .background(
                                                    Brush.horizontalGradient(
                                                        colors = listOf(UpdateCrimsonStart, AmberNeon, CyanNeon)
                                                    )
                                                )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                            Text(
                                                text = if (state.totalBytes > 0L) {
                                                    "${formatBytesToMb(state.bytesDownloaded)} / ${formatBytesToMb(state.totalBytes)}"
                                                } else {
                                                    formatBytesToMb(state.bytesDownloaded)
                                                },
                                                color = colors.textMuted,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        Text(
                                            text = formatPercent(pct, isArabic),
                                            color = CyanNeon,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Signature Wide Capsule Action Button (with Glass Shimmer Sweep)
                        when (state) {
                            is AppUpdateUiState.Checking -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(colors.glassCard)
                                        .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(50)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = colors.primaryNeon,
                                        strokeWidth = 2.4.dp
                                    )
                                }
                            }
                            is AppUpdateUiState.UpToDate -> {
                                CapsuleShimmerButton(
                                    text = strings.backToCards.takeIf { false } ?: (if (isArabic) "حسناً" else "Done"),
                                    gradientColors = listOf(EmeraldNeon, CyanNeon),
                                    sheenProgress = sheenProgress,
                                    onClick = onDismiss,
                                    testTag = "update_done_button"
                                )
                            }
                            is AppUpdateUiState.Downloading -> {
                                val pct = (state.progress * 100f).toInt().coerceIn(0, 100)
                                CapsuleShimmerButton(
                                    text = "${strings.downloadingUpdate} ${formatPercent(pct, isArabic)}",
                                    gradientColors = listOf(
                                        UpdateCrimsonStart.copy(alpha = 0.72f),
                                        Color(0xFF2563EB).copy(alpha = 0.85f)
                                    ),
                                    sheenProgress = sheenProgress,
                                    enabled = false,
                                    onClick = {},
                                    testTag = "update_downloading_button"
                                )
                            }
                            is AppUpdateUiState.ReadyToInstall -> {
                                if (!canInstallPackages) {
                                    CapsuleShimmerButton(
                                        text = strings.grantInstallPermButton,
                                        gradientColors = listOf(AmberNeon, UpdateCrimsonStart),
                                        sheenProgress = sheenProgress,
                                        onClick = onRequestInstallPermission,
                                        testTag = "grant_install_perm_button"
                                    )
                                } else {
                                    CapsuleShimmerButton(
                                        text = strings.installUpdateButton,
                                        gradientColors = listOf(EmeraldNeon, Color(0xFF0EA5E9)),
                                        sheenProgress = sheenProgress,
                                        onClick = onInstallApk,
                                        testTag = "install_update_button"
                                    )
                                }
                            }
                            is AppUpdateUiState.Error -> {
                                CapsuleShimmerButton(
                                    text = strings.retryUpdateButton,
                                    gradientColors = listOf(UpdateCrimsonStart, UpdateCrimsonEnd),
                                    sheenProgress = sheenProgress,
                                    onClick = {
                                        if (state.release != null) onStartDownload() else onRetryCheck()
                                    },
                                    testTag = "retry_update_button"
                                )
                            }
                            else -> {
                                val hasDirectApk = !release?.apkDownloadUrl.isNullOrBlank()
                                CapsuleShimmerButton(
                                    text = strings.updateActionButton,
                                    gradientColors = listOf(UpdateCrimsonStart, UpdateCrimsonEnd),
                                    sheenProgress = sheenProgress,
                                    onClick = {
                                        if (hasDirectApk) {
                                            onStartDownload()
                                        } else {
                                            onOpenReleaseBrowser(release?.htmlUrl ?: GITHUB_RELEASES_WEB_URL)
                                        }
                                    },
                                    testTag = "start_update_button"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CapsuleShimmerButton(
    text: String,
    gradientColors: List<Color>,
    sheenProgress: Float,
    onClick: () -> Unit,
    enabled: Boolean = true,
    testTag: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(
                elevation = if (enabled) 10.dp else 2.dp,
                shape = RoundedCornerShape(50),
                spotColor = gradientColors.first().copy(alpha = 0.55f),
                ambientColor = gradientColors.last().copy(alpha = 0.35f)
            )
            .clip(RoundedCornerShape(50))
            .background(
                Brush.horizontalGradient(colors = gradientColors)
            )
            .drawBehind {
                if (enabled) {
                    val sheenWidth = size.width * 0.35f
                    val startX = (size.width + sheenWidth) * sheenProgress - sheenWidth
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.28f),
                                Color.Transparent
                            ),
                            start = Offset(startX, 0f),
                            end = Offset(startX + sheenWidth, size.height)
                        )
                    )
                }
            }
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = Color.White),
                onClick = onClick
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.3.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Artistic Animated Rocket Emblem inspired by the update screenshot with concentric glowing rings,
 * orbiting stars, pulsing thruster flames, and liquid glass highlights.
 */
@Composable
private fun UpdateRocketEmblem(
    isLaunching: Boolean,
    isUpToDate: Boolean,
    isChecking: Boolean,
    isDark: Boolean
) {
    val infinite = rememberInfiniteTransition(label = "rocket_emblem")

    val bobPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isLaunching) 700 else 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rocket_bob"
    )
    val orbitAngle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isLaunching) 4500 else 11000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_ring"
    )
    val flamePulse by infinite.animateFloat(
        initialValue = 0.78f,
        targetValue = if (isLaunching) 1.38f else 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isLaunching) 160 else 380, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flame_pulse"
    )
    val rippleExpand by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sonar_ring"
    )

    val ringAccent = if (isUpToDate) EmeraldNeon else UpdateCrimsonStart

    Box(
        modifier = Modifier.size(134.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val baseR = size.minDimension * 0.35f

            // Expanding sonar wave ring
            drawCircle(
                color = ringAccent.copy(alpha = (1f - rippleExpand) * 0.35f),
                radius = baseR + (size.minDimension * 0.14f * rippleExpand),
                center = Offset(cx, cy),
                style = Stroke(width = 2.dp.toPx())
            )

            // Outer translucent glowing halo ring (matching reference screenshot)
            drawCircle(
                color = ringAccent.copy(alpha = if (isDark) 0.22f else 0.14f),
                radius = baseR * 1.24f,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = ringAccent.copy(alpha = 0.9f),
                radius = baseR * 1.07f,
                center = Offset(cx, cy),
                style = Stroke(width = 2.6.dp.toPx())
            )

            // Inner deep cosmos core circle
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1A1025),
                        Color(0xFF0E0B16)
                    ),
                    center = Offset(cx, cy),
                    radius = baseR
                ),
                radius = baseR,
                center = Offset(cx, cy)
            )

            // Rotating dotted orbital glow arc inside border
            rotate(degrees = orbitAngle, pivot = Offset(cx, cy)) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            CyanNeon.copy(alpha = 0.7f),
                            Color.White.copy(alpha = 0.85f),
                            Color.Transparent
                        )
                    ),
                    startAngle = 0f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = Offset(cx - baseR * 1.07f, cy - baseR * 1.07f),
                    size = Size(baseR * 2.14f, baseR * 2.14f),
                    style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Twinkling cosmic stars & pixel sparks inside cosmos circle
            val starOffsets = listOf(
                Triple(-0.56f, -0.32f, Color(0xFFEF4444)),
                Triple(0.58f, -0.40f, Color.White),
                Triple(-0.50f, 0.44f, Color(0xFFFACC15)),
                Triple(0.56f, 0.34f, CyanNeon)
            )
            starOffsets.forEachIndexed { idx, (ox, oy, starCol) ->
                val twinkle = 0.45f + 0.55f * (0.5f + 0.5f * sin(bobPhase + idx * 1.6f))
                val sx = cx + baseR * ox
                val sy = cy + baseR * oy
                val sq = (3.6f + (idx % 2) * 1.6f) * twinkle
                drawRoundRect(
                    color = starCol.copy(alpha = twinkle),
                    topLeft = Offset(sx - sq / 2f, sy - sq / 2f),
                    size = Size(sq, sq),
                    cornerRadius = CornerRadius(1.5f, 1.5f)
                )
            }

            if (isUpToDate) {
                // Smooth Checkmark Crest when already up to date
                val checkPath = Path().apply {
                    moveTo(cx - baseR * 0.36f, cy + baseR * 0.02f)
                    lineTo(cx - baseR * 0.08f, cy + baseR * 0.28f)
                    lineTo(cx + baseR * 0.40f, cy - baseR * 0.24f)
                }
                drawPath(
                    path = checkPath,
                    color = EmeraldNeon,
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                )
            } else {
                // Bobbing & Launching Rocket Vector Artwork
                val hoverDy = sin(bobPhase) * (if (isLaunching) 3.5f else 5.5f)
                val jitterDx = if (isLaunching) cos(bobPhase * 3f) * 1.8f else 0f

                translate(left = jitterDx, top = hoverDy) {
                    val rw = baseR * 0.50f
                    val rh = baseR * 1.18f
                    val rTop = cy - rh * 0.52f
                    val rBottom = cy + rh * 0.34f

                    // Dynamic Thruster Exhaust Flame
                    val flameLen = baseR * 0.44f * flamePulse
                    val outerFlame = Path().apply {
                        moveTo(cx - rw * 0.32f, rBottom)
                        lineTo(cx, rBottom + flameLen)
                        lineTo(cx + rw * 0.32f, rBottom)
                        close()
                    }
                    drawPath(
                        path = outerFlame,
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFF97316), Color(0xFFFACC15), Color(0xFFFFEDD5)),
                            startY = rBottom,
                            endY = rBottom + flameLen
                        )
                    )
                    val innerFlame = Path().apply {
                        moveTo(cx - rw * 0.16f, rBottom)
                        lineTo(cx, rBottom + flameLen * 0.62f)
                        lineTo(cx + rw * 0.16f, rBottom)
                        close()
                    }
                    drawPath(
                        path = innerFlame,
                        color = if (isLaunching) CyanNeon else Color(0xFFFEF08A)
                    )

                    // Left Swept Wing Fin
                    val leftFin = Path().apply {
                        moveTo(cx - rw * 0.45f, rBottom - rh * 0.26f)
                        lineTo(cx - rw * 1.08f, rBottom + rh * 0.04f)
                        lineTo(cx - rw * 0.42f, rBottom + rh * 0.02f)
                        close()
                    }
                    drawPath(leftFin, Color(0xFFDC2626))

                    // Right Swept Wing Fin
                    val rightFin = Path().apply {
                        moveTo(cx + rw * 0.45f, rBottom - rh * 0.26f)
                        lineTo(cx + rw * 1.08f, rBottom + rh * 0.04f)
                        lineTo(cx + rw * 0.42f, rBottom + rh * 0.02f)
                        close()
                    }
                    drawPath(rightFin, Color(0xFFB91C1C))

                    // Rocket White Main Body Fuselage
                    val bodyPath = Path().apply {
                        moveTo(cx, rTop)
                        cubicTo(
                            cx + rw * 0.68f, rTop + rh * 0.25f,
                            cx + rw * 0.64f, rBottom - rh * 0.10f,
                            cx + rw * 0.34f, rBottom
                        )
                        lineTo(cx - rw * 0.34f, rBottom)
                        cubicTo(
                            cx - rw * 0.64f, rBottom - rh * 0.10f,
                            cx - rw * 0.68f, rTop + rh * 0.25f,
                            cx, rTop
                        )
                        close()
                    }
                    drawPath(
                        path = bodyPath,
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFFFFFFFF), Color(0xFFF1F5F9), Color(0xFFCBD5E1))
                        )
                    )

                    // Rocket Crimson Red Nose Cone Tip (clipped visually to upper nose section)
                    val noseSplitY = rTop + rh * 0.26f
                    val nosePath = Path().apply {
                        moveTo(cx, rTop)
                        cubicTo(
                            cx + rw * 0.38f, rTop + rh * 0.11f,
                            cx + rw * 0.42f, noseSplitY,
                            cx + rw * 0.42f, noseSplitY
                        )
                        lineTo(cx - rw * 0.42f, noseSplitY)
                        cubicTo(
                            cx - rw * 0.42f, noseSplitY,
                            cx - rw * 0.38f, rTop + rh * 0.11f,
                            cx, rTop
                        )
                        close()
                    }
                    drawPath(nosePath, Color(0xFFDC2626))

                    // Bottom Center Exhaust Nozzle Collar
                    drawRoundRect(
                        color = Color(0xFF475569),
                        topLeft = Offset(cx - rw * 0.34f, rBottom - 4f),
                        size = Size(rw * 0.68f, 6f),
                        cornerRadius = CornerRadius(2f, 2f)
                    )

                    // Center Glowing Cyan Porthole Window
                    val windowCy = rTop + rh * 0.49f
                    val windowR = rw * 0.34f
                    drawCircle(
                        color = Color(0xFF1E293B),
                        radius = windowR * 1.24f,
                        center = Offset(cx, windowCy)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF67E8F9), CyanNeon, Color(0xFF0284C7)),
                            center = Offset(cx - windowR * 0.25f, windowCy - windowR * 0.25f),
                            radius = windowR * 1.2f
                        ),
                        radius = windowR,
                        center = Offset(cx, windowCy)
                    )
                    // Crisp white lens highlight inside window
                    drawCircle(
                        color = Color.White.copy(alpha = 0.85f),
                        radius = windowR * 0.28f,
                        center = Offset(cx - windowR * 0.32f, windowCy - windowR * 0.32f)
                    )
                }
            }
        }
    }
}
