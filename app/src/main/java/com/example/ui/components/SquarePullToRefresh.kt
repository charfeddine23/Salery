package com.example.ui.components

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import com.example.market.SQUARE_SYNC_INTERVAL_OPTIONS_MINUTES
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.isolateLtr
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.LocalScreenshotProtection
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.min

private const val HOLD_TO_SETTINGS_DURATION_NANOS = 2_000_000_000L // 2.0 seconds

@Composable
fun SquareTopAnchoredPullToRefresh(
    isRefreshing: Boolean,
    isArabic: Boolean,
    onRefresh: () -> Unit,
    onOpenSyncSettings: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = LiquidGlassTheme.colors
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val refreshThresholdPx = remember(density) { with(density) { 82.dp.toPx() } }
    val maxPullPx = remember(density) { with(density) { 164.dp.toPx() } }
    val refreshingVisualPx = remember(density) { with(density) { 54.dp.toPx() } }

    val pullOffset = remember { Animatable(0f) }
    var isFingerDown by remember { mutableStateOf(false) }
    var hasCrossedThreshold by remember { mutableStateOf(false) }
    var holdTriggeredSettings by remember { mutableStateOf(false) }
    var holdProgress by remember { mutableFloatStateOf(0f) }

    val latestOnRefresh by rememberUpdatedState(onRefresh)
    val latestOnOpenSyncSettings by rememberUpdatedState(onOpenSyncSettings)
    val latestIsRefreshing by rememberUpdatedState(isRefreshing)

    val isPastThreshold by remember(refreshThresholdPx) {
        derivedStateOf { pullOffset.value >= refreshThresholdPx }
    }

    // Animate top aurora depth when refresh starts/stops programmatically
    val visualRefreshingHeightPx by animateFloatAsState(
        targetValue = if (isRefreshing) refreshingVisualPx else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "square_refreshing_glow_height"
    )

    // 2-second continuous pull-and-hold timer -> opens Sync Interval Settings
    LaunchedEffect(isFingerDown, isPastThreshold, holdTriggeredSettings, isRefreshing) {
        if (isFingerDown && isPastThreshold && !holdTriggeredSettings && !isRefreshing) {
            val startNanos = System.nanoTime()
            while (isActive) {
                var reachedGoal = false
                withFrameNanos { frameNanos ->
                    val elapsed = (frameNanos - startNanos).coerceAtLeast(0L)
                    val progress = (elapsed.toFloat() / HOLD_TO_SETTINGS_DURATION_NANOS.toFloat()).coerceIn(0f, 1f)
                    holdProgress = progress
                    if (progress >= 1f) {
                        reachedGoal = true
                    }
                }
                if (reachedGoal) {
                    holdTriggeredSettings = true
                    holdProgress = 0f
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    latestOnOpenSyncSettings()
                    pullOffset.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                    break
                }
            }
        } else if (!isFingerDown || !isPastThreshold) {
            holdProgress = 0f
        }
    }

    // Unified release handler when finger lifts from screen
    LaunchedEffect(isFingerDown) {
        if (!isFingerDown) {
            val currentPull = pullOffset.value
            val shouldTriggerRefresh =
                !holdTriggeredSettings &&
                    !latestIsRefreshing &&
                    currentPull >= refreshThresholdPx
            if (shouldTriggerRefresh) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                latestOnRefresh()
            }
            if (currentPull > 0f) {
                pullOffset.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
            hasCrossedThreshold = false
            holdTriggeredSettings = false
            holdProgress = 0f
        }
    }

    val nestedScrollConnection = remember(refreshThresholdPx, maxPullPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y < 0f && pullOffset.value > 0f) {
                    val consumedY = available.y.coerceAtLeast(-pullOffset.value)
                    val nextVal = (pullOffset.value + consumedY).coerceAtLeast(0f)
                    scope.launch { pullOffset.snapTo(nextVal) }
                    return Offset(0f, consumedY)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source == NestedScrollSource.UserInput && available.y > 0f && !holdTriggeredSettings) {
                    val current = pullOffset.value
                    val progressRatio = (current / maxPullPx).coerceIn(0f, 0.94f)
                    val dragMultiplier = (0.58f * (1f - progressRatio)).coerceAtLeast(0.10f)
                    val nextVal = (current + available.y * dragMultiplier).coerceIn(0f, maxPullPx)
                    scope.launch { pullOffset.snapTo(nextVal) }
                    if (!hasCrossedThreshold && nextVal >= refreshThresholdPx) {
                        hasCrossedThreshold = true
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pullOffset.value > 0f) {
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    val effectiveTopGlowHeightPx by remember {
        derivedStateOf {
            maxOf(pullOffset.value, visualRefreshingHeightPx)
        }
    }
    val pullProgress by remember(refreshThresholdPx) {
        derivedStateOf {
            (effectiveTopGlowHeightPx / refreshThresholdPx).coerceIn(0f, 1.5f)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "square_top_blur_pulse")
    val ambientWave by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "square_blur_wave"
    )
    val refreshSpin by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "square_refresh_spin"
    )
    val glidePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "square_pull_hint_glide"
    )

    val activeAccent by animateColorAsState(
        targetValue = when {
            holdProgress > 0.05f -> CyanNeon
            isPastThreshold -> EmeraldNeon
            else -> SquareGoldAccent
        },
        animationSpec = tween(220),
        label = "square_pull_glow_accent"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .nestedScroll(nestedScrollConnection)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val anyPressed = event.changes.any { it.pressed }
                        if (anyPressed != isFingerDown) {
                            isFingerDown = anyPressed
                        }
                    }
                }
            }
            .testTag("square_pull_refresh_container")
    ) {
        // 1. Main scrollable Square content slides downward elastically with pull
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 30.dp)
                .graphicsLayer {
                    translationY = effectiveTopGlowHeightPx * 0.78f
                },
            content = content
        )

        // 2. Top-glued glowing blur aura (anchored strictly at y = 0 and stretches downward on pull)
        val glowBandHeightDp = with(density) {
            val baseRestingPx = 14.dp.toPx()
            val dynamicPx = baseRestingPx + effectiveTopGlowHeightPx * 1.28f
            dynamicPx.toDp()
        }
        val activeIntensity = when {
            effectiveTopGlowHeightPx > 2f -> (0.28f + pullProgress * 0.68f).coerceIn(0.28f, 1f)
            else -> 0.16f + 0.06f * ambientWave
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(glowBandHeightDp)
                .testTag("square_top_blur_glow"),
            contentAlignment = Alignment.TopCenter
        ) {
            val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Modifier
                    .fillMaxSize()
                    .blur(
                        radius = (18f + 16f * min(pullProgress, 1.2f)).dp,
                        edgeTreatment = BlurredEdgeTreatment.Unbounded
                    )
            } else {
                Modifier.fillMaxSize()
            }

            // Atmospheric blurred glow mass glued along y = 0
            Canvas(modifier = blurModifier) {
                val w = size.width
                val h = size.height
                if (w <= 0f || h <= 0f) return@Canvas

                val stretchDepth = (h * (0.48f + 0.44f * min(pullProgress, 1.3f)))
                    .coerceAtMost(h)
                val edgeDepth = h * 0.14f

                // Curved elastic aurora curtain: edges remain tethered along y = 0
                val curtainPath = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(w, 0f)
                    lineTo(w, edgeDepth)
                    cubicTo(
                        w * 0.74f, stretchDepth * 0.96f,
                        w * 0.26f, stretchDepth * 0.96f,
                        0f, edgeDepth
                    )
                    close()
                }

                drawPath(
                    path = curtainPath,
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to activeAccent.copy(alpha = (0.72f * activeIntensity).coerceIn(0f, 0.85f)),
                            0.36f to SquareGoldAccent.copy(alpha = (0.46f * activeIntensity).coerceIn(0f, 0.65f)),
                            0.74f to CyanNeon.copy(alpha = (0.20f * activeIntensity).coerceIn(0f, 0.35f)),
                            1.0f to Color.Transparent
                        ),
                        startY = 0f,
                        endY = stretchDepth
                    )
                )

                // Soft central top bloom anchored at (w/2, 0)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = (0.34f * activeIntensity).coerceIn(0f, 0.42f)),
                            activeAccent.copy(alpha = (0.42f * activeIntensity).coerceIn(0f, 0.52f)),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.5f, 0f),
                        radius = (w * 0.46f).coerceAtLeast(h * 0.85f)
                    ),
                    center = Offset(w * 0.5f, 0f),
                    radius = (w * 0.46f).coerceAtLeast(h * 0.85f)
                )
            }

            // Sharp top-seam luminous highlight line glued at y = 0
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .align(Alignment.TopCenter)
            ) {
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            activeAccent.copy(alpha = (0.45f + 0.50f * activeIntensity).coerceIn(0f, 0.95f)),
                            Color.White.copy(alpha = (0.35f + 0.50f * activeIntensity).coerceIn(0f, 0.90f)),
                            activeAccent.copy(alpha = (0.45f + 0.50f * activeIntensity).coerceIn(0f, 0.95f)),
                            Color.Transparent
                        )
                    )
                )
            }

            // Sleek, fluid micro-indicator pill aligned cleanly above header/list when idle
            val idleHintAlpha = if (isRefreshing) {
                0f
            } else {
                (1f - (effectiveTopGlowHeightPx / 22f)).coerceIn(0f, 1f)
            }

            if (idleHintAlpha > 0.02f) {
                val trackShiftPx = with(density) { ((glidePhase - 0.5f) * 5.dp.toPx()) }
                val shimmerPulse = 0.55f + 0.45f * ambientWave

                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 5.dp)
                        .alpha(idleHintAlpha)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (colors.isDark) colors.glassCard.copy(alpha = 0.56f)
                            else colors.glassCard.copy(alpha = 0.78f)
                        )
                        .border(
                            width = 0.8.dp,
                            color = colors.glassBorderSubtle.copy(alpha = 0.75f),
                            shape = RoundedCornerShape(50)
                        )
                        .padding(horizontal = 11.dp, vertical = 3.5.dp)
                        .testTag("square_pull_hint_cue"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Micro vertical glide track with smooth animated bead
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(11.dp)
                            .clip(CircleShape)
                            .background(colors.glassBorderSubtle.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(3.5.dp)
                                .graphicsLayer { translationY = trackShiftPx * 0.55f }
                                .clip(CircleShape)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            SquareGoldAccent.copy(alpha = shimmerPulse),
                                            CyanNeon.copy(alpha = shimmerPulse)
                                        )
                                    )
                                )
                        )
                    }

                    Text(
                        text = if (isArabic) "اسحب للتحديث" else "Pull to refresh",
                        color = colors.textSecondary.copy(alpha = 0.85f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.2.sp,
                        maxLines = 1
                    )
                }
            }

            // Pull feedback HUD capsule (appears organically when pulling or refreshing)
            val hudAlpha = when {
                isRefreshing -> 1f
                effectiveTopGlowHeightPx > 14f -> ((effectiveTopGlowHeightPx - 14f) / (refreshThresholdPx * 0.65f)).coerceIn(0f, 1f)
                else -> 0f
            }

            if (hudAlpha > 0.02f) {
                val secsLeftText = remember(holdProgress) {
                    val left = ((1f - holdProgress) * 2f).coerceIn(0f, 2f)
                    isolateLtr(String.format(Locale.US, "%.1f", left))
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = ((effectiveTopGlowHeightPx / density.density) * 0.22f).coerceIn(8f, 22f).dp)
                        .alpha(hudAlpha)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (colors.isDark) Color(0xFF0A1220).copy(alpha = 0.72f)
                            else Color.White.copy(alpha = 0.85f)
                        )
                        .border(
                            width = 1.dp,
                            color = activeAccent.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(50)
                        )
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                        .testTag("square_pull_status_pill"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Indicator glyph + 2s hold progress ring
                    Box(
                        modifier = Modifier.size(26.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeW = 2.4.dp.toPx()
                            drawCircle(
                                color = activeAccent.copy(alpha = 0.22f),
                                style = Stroke(width = strokeW)
                            )
                            val arcSweep = when {
                                isRefreshing -> 240f
                                holdProgress > 0f -> holdProgress * 360f
                                else -> min(pullProgress, 1f) * 360f
                            }
                            drawArc(
                                color = if (holdProgress > 0f) CyanNeon else activeAccent,
                                startAngle = -90f + if (isRefreshing) refreshSpin else 0f,
                                sweepAngle = arcSweep,
                                useCenter = false,
                                size = Size(size.width, size.height),
                                style = Stroke(width = strokeW, cap = StrokeCap.Round)
                            )
                        }
                        Icon(
                            imageVector = when {
                                isRefreshing -> Icons.Default.Refresh
                                holdProgress > 0.08f -> Icons.Default.Tune
                                isPastThreshold -> Icons.Default.Refresh
                                else -> Icons.Default.ArrowDownward
                            },
                            contentDescription = null,
                            tint = activeAccent,
                            modifier = Modifier
                                .size(14.dp)
                                .rotate(
                                    when {
                                        isRefreshing -> refreshSpin
                                        isPastThreshold -> 180f
                                        else -> pullProgress * 140f
                                    }
                                )
                        )
                    }

                    Text(
                        text = when {
                            isRefreshing -> {
                                if (isArabic) "جاري تحديث الأسعار..." else "Updating live rates..."
                            }
                            holdProgress > 0.06f -> {
                                if (isArabic) "أفلت للتحديث • انتظر ${secsLeftText} ث لوقت المزامنة"
                                else "Release to refresh • Hold ${secsLeftText}s for sync settings"
                            }
                            isPastThreshold -> {
                                if (isArabic) "أفلت للتحديث أو انتظر ${isolateLtr("2")} ث للإعدادات"
                                else "Release to refresh • Hold 2s for sync settings"
                            }
                            else -> {
                                if (isArabic) "اسحب للأسفل للتحديث" else "Pull down to refresh"
                            }
                        },
                        color = colors.textPrimary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SquareSyncIntervalSettingsDialog(
    currentIntervalMinutes: Int,
    lastSyncLabel: String,
    isArabic: Boolean,
    onSaveInterval: (Int) -> Unit,
    onSyncNow: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors
    val isDark = colors.isDark

    var selectedMinutes by remember(currentIntervalMinutes) {
        mutableStateOf(currentIntervalMinutes)
    }
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }

    val cardScale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.90f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "square_sync_dialog_scale"
    )

    val isScreenshotProtected = LocalScreenshotProtection.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            securePolicy = if (isScreenshotProtected) SecureFlagPolicy.SecureOn else SecureFlagPolicy.SecureOff
        )
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr
        ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 400.dp)
                    .scale(cardScale)
                    .testTag("square_sync_settings_dialog"),
                shape = RoundedCornerShape(28.dp),
                backgroundColor = if (isDark) Color(0xFF0E1522).copy(alpha = 0.97f) else colors.dialogBackground,
                borderColor = SquareGoldAccent.copy(alpha = 0.50f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        SquareGoldAccent.copy(alpha = if (isDark) 0.14f else 0.09f),
                                        CyanNeon.copy(alpha = if (isDark) 0.08f else 0.05f),
                                        Color.Transparent
                                    ),
                                    center = Offset(size.width * 0.5f, 0f),
                                    radius = size.width * 0.72f
                                )
                            )
                        }
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("square_sync_close_button")
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
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            SquareGoldAccent.copy(alpha = 0.22f),
                                            CyanNeon.copy(alpha = 0.16f)
                                        )
                                    )
                                )
                                .border(1.dp, SquareGoldAccent.copy(alpha = 0.45f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = SquareGoldAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.size(48.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isArabic) "إعدادات وقت المزامنة" else "Sync Interval Settings",
                        color = colors.textPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (isArabic) {
                            "اختر كل كم يتم تحديث أسعار الذهب والعملات تلقائياً • آخر مزامنة: $lastSyncLabel"
                        } else {
                            "Choose how often Square gold & FX rates auto-sync • Last sync: $lastSyncLabel"
                        },
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SQUARE_SYNC_INTERVAL_OPTIONS_MINUTES.forEach { mins ->
                            val selected = selectedMinutes == mins
                            val label = formatSquareIntervalLabel(mins, isArabic)
                            Row(
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (selected) SquareGoldAccent.copy(alpha = 0.20f)
                                        else colors.glassCard
                                    )
                                    .border(
                                        width = if (selected) 1.3.dp else 0.9.dp,
                                        color = if (selected) SquareGoldAccent else colors.glassBorderSubtle,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(color = SquareGoldAccent),
                                        onClick = { selectedMinutes = mins }
                                    )
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                    .testTag("square_sync_option_$mins"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = SquareGoldAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = label,
                                    color = if (selected) colors.textPrimary else colors.textSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Helpful mode summary pill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.glassSurface.copy(alpha = 0.55f))
                            .border(0.8.dp, colors.glassBorderSubtle, RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = if (selectedMinutes <= 0) {
                                if (isArabic) {
                                    "الوضع اليدوي: لن يتم التحديث تلقائياً إلا عند السحب للأسفل."
                                } else {
                                    "Manual mode: Rates only refresh when you pull down."
                                }
                            } else {
                                val intervalLabel = formatSquareIntervalLabel(selectedMinutes, isArabic)
                                if (isArabic) {
                                    "المزامنة التلقائية: سيتم تحديث الأسعار $intervalLabel مع إمكانية التحديث بالسحب في أي وقت."
                                } else {
                                    "Auto-sync: Rates refresh $intervalLabel, plus anytime you pull down."
                                }
                            },
                            color = colors.textSecondary,
                            fontSize = 11.5.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Sync Now secondary action
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(50))
                                .background(colors.glassCard)
                                .border(1.dp, CyanNeon.copy(alpha = 0.45f), RoundedCornerShape(50))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(color = CyanNeon),
                                    onClick = {
                                        onSaveInterval(selectedMinutes)
                                        onSyncNow()
                                        onDismiss()
                                    }
                                )
                                .testTag("square_sync_now_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = CyanNeon,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isArabic) "تحديث الآن" else "Sync Now",
                                    color = CyanNeon,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Save primary action
                        Box(
                            modifier = Modifier
                                .weight(1.25f)
                                .height(48.dp)
                                .shadow(
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(50),
                                    spotColor = SquareGoldAccent.copy(alpha = 0.45f)
                                )
                                .clip(RoundedCornerShape(50))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(SquareGoldAccent, EmeraldNeon)
                                    )
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(color = Color.White),
                                    onClick = {
                                        onSaveInterval(selectedMinutes)
                                        onDismiss()
                                    }
                                )
                                .testTag("square_sync_save_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isArabic) "حفظ المدة" else "Save",
                                color = Color(0xFF0A1220),
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
        }
    }
}

fun formatSquareIntervalLabel(minutes: Int, isArabic: Boolean): String {
    return when {
        minutes <= 0 -> if (isArabic) "يدوي فقط" else "Manual only"
        minutes < 60 -> if (isArabic) "كل $minutes د" else "Every ${minutes}m"
        else -> {
            val hours = minutes / 60
            if (isArabic) "كل $hours سا" else "Every ${hours}h"
        }
    }
}
