package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.GlassBackground
import com.example.ui.i18n.APP_NAME
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.breathingGlow
import com.example.viewmodel.AuthTransitionKind
import com.example.viewmodel.AuthTransitionState

private val TransitionScriptFont = FontFamily(
    Font(R.font.pacifico, FontWeight.Normal)
)

private val TransitionRoundedFont = FontFamily(
    Font(R.font.nunito, FontWeight.Normal)
)

@Composable
fun AuthTransitionLuxuryScreen(
    state: AuthTransitionState.Active,
    isArabic: Boolean,
    modifier: Modifier = Modifier
) {
    // Prevent accidental back dismissal during the 2-second transition sequence
    BackHandler(enabled = true) {}

    val appColors = LiquidGlassTheme.colors
    val isDark = appColors.isDark
    val isCompleted = state.isCompleted
    val isSignUp = state.kind == AuthTransitionKind.SIGN_UP

    val progressAnim = remember(state.kind, state.displayName) { Animatable(0f) }
    val checkDrawAnim = remember(state.kind, state.displayName) { Animatable(0f) }

    LaunchedEffect(state.kind, state.displayName) {
        progressAnim.snapTo(0f)
        progressAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1950, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(state.kind, state.displayName, isCompleted) {
        if (isCompleted) {
            checkDrawAnim.snapTo(0f)
            checkDrawAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing)
            )
        }
    }

    // Subtle ambient drift matching AuthWelcomeScreen
    val infiniteTransition = rememberInfiniteTransition(label = "authTransitionAmbient")
    val driftPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "driftPhase"
    )

    val activeAccent by animateColorAsState(
        targetValue = if (isCompleted) appColors.successColor else appColors.primaryNeon,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "activeAccent"
    )
    val secondaryAccent by animateColorAsState(
        targetValue = if (isCompleted) appColors.primaryNeon else appColors.secondaryNeon,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "secondaryAccent"
    )
    val cardBorderColor by animateColorAsState(
        targetValue = if (isCompleted) {
            appColors.successColor.copy(alpha = 0.55f)
        } else {
            appColors.glassBorder
        },
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "cardBorderColor"
    )

    // Smooth morphing behind-card blob position (glides gently from loading -> success)
    val blobSpec = tween<Float>(durationMillis = 720, easing = FastOutSlowInEasing)
    val blobCenterXFraction by animateFloatAsState(
        targetValue = if (isCompleted) 0.84f else 0.14f,
        animationSpec = blobSpec,
        label = "blobCenterX"
    )
    val blobCenterYFraction by animateFloatAsState(
        targetValue = if (isCompleted) 0.78f else 0.22f,
        animationSpec = blobSpec,
        label = "blobCenterY"
    )
    val blobRotation by animateFloatAsState(
        targetValue = if (isCompleted) 20f else -14f,
        animationSpec = blobSpec,
        label = "blobRotation"
    )
    val emblemScale by animateFloatAsState(
        targetValue = if (isCompleted) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "emblemScale"
    )

    val mainTitle = remember(isSignUp, isCompleted, isArabic) {
        when {
            !isCompleted && isSignUp -> if (isArabic) "جاري إنشاء الحساب..." else "Creating your account..."
            !isCompleted && !isSignUp -> if (isArabic) "جاري تسجيل الدخول..." else "Signing you in..."
            isCompleted && isSignUp -> if (isArabic) "تم إنشاء الحساب بنجاح" else "Account created successfully"
            else -> if (isArabic) "تم تسجيل الدخول بنجاح" else "Signed in successfully"
        }
    }

    val subtitleText = remember(isCompleted, isArabic, state.displayName) {
        if (!isCompleted) {
            if (isArabic) "يرجى الانتظار لحظة..." else "Please wait a moment..."
        } else {
            val name = state.displayName.trim()
            if (name.isNotBlank()) {
                if (isArabic) "أهلاً بك، $name" else "Welcome, $name"
            } else {
                if (isArabic) "مرحباً بك في $APP_NAME" else "Welcome to $APP_NAME"
            }
        }
    }

    val avgAccentLuminance = (activeAccent.luminance() + secondaryAccent.luminance()) * 0.5f
    val onAccentText = if (avgAccentLuminance > 0.48f) Color(0xFF07111F) else Color.White

    GlassBackground(
        modifier = modifier
            .fillMaxSize()
            .testTag("auth_transition_luxury_screen")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            // Centered card + behind-card morphing visual layer matching AuthWelcomeScreen
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val w = size.width
                    val h = size.height
                    val driftX = (driftPhase - 0.5f) * 12f
                    val driftY = (0.5f - driftPhase) * 10f
                    val cx = w * blobCenterXFraction + driftX
                    val cy = h * blobCenterYFraction + driftY
                    val rx = w * 0.56f
                    val ry = h * 0.44f

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                activeAccent.copy(alpha = if (isDark) 0.22f else 0.14f),
                                Color.Transparent
                            ),
                            center = Offset(cx, cy),
                            radius = maxOf(rx, ry) * 1.4f
                        ),
                        radius = maxOf(rx, ry) * 1.4f,
                        center = Offset(cx, cy)
                    )

                    rotate(degrees = blobRotation, pivot = Offset(cx, cy)) {
                        drawOval(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    activeAccent.copy(alpha = if (isDark) 0.42f else 0.30f),
                                    secondaryAccent.copy(alpha = if (isDark) 0.36f else 0.24f)
                                ),
                                start = Offset(cx - rx * 0.5f, cy - ry * 0.5f),
                                end = Offset(cx + rx * 0.5f, cy + ry * 0.5f)
                            ),
                            topLeft = Offset(cx - rx * 0.5f, cy - ry * 0.5f),
                            size = Size(rx, ry)
                        )
                    }
                }

                val cardShape = RoundedCornerShape(28.dp)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .breathingGlow(
                            enabled = true,
                            glowColor = activeAccent,
                            secondaryGlowColor = secondaryAccent,
                            cornerRadius = 28.dp,
                            minAlpha = if (isDark) 0.06f else 0.04f,
                            maxAlpha = if (isDark) 0.18f else 0.10f,
                            animateContainerScale = false
                        )
                        .shadow(
                            elevation = if (isDark) 24.dp else 16.dp,
                            shape = cardShape,
                            ambientColor = activeAccent.copy(alpha = if (isDark) 0.18f else 0.12f),
                            spotColor = Color.Black.copy(alpha = if (isDark) 0.50f else 0.12f)
                        )
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = 0.85f,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ),
                    shape = cardShape,
                    color = appColors.glassCard,
                    border = BorderStroke(1.dp, cardBorderColor)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(cardShape)
                            .background(appColors.glassHighlightBrush)
                    ) {
                        // Signature top-left organic cloud accent matching AuthWelcomeScreen's welcome card
                        Canvas(modifier = Modifier.matchParentSize()) {
                            val w = size.width
                            val floatY = (driftPhase - 0.5f) * 5f
                            val cloudBrush = Brush.linearGradient(
                                colors = listOf(activeAccent, secondaryAccent)
                            )
                            drawCircle(
                                brush = cloudBrush,
                                radius = w * 0.18f,
                                center = Offset(w * 0.08f, w * 0.055f + floatY),
                                alpha = 0.92f
                            )
                            drawCircle(
                                brush = cloudBrush,
                                radius = w * 0.115f,
                                center = Offset(w * 0.21f, w * 0.042f + floatY),
                                alpha = 0.92f
                            )
                            drawCircle(
                                brush = cloudBrush,
                                radius = w * 0.125f,
                                center = Offset(w * 0.035f, w * 0.17f + floatY),
                                alpha = 0.92f
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 28.dp, vertical = 42.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(12.dp))

                            // Clean minimalist status emblem with single precision arc
                            Box(
                                modifier = Modifier
                                    .size(92.dp)
                                    .graphicsLayer {
                                        scaleX = emblemScale
                                        scaleY = emblemScale
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val strokeWidth = 3.dp.toPx()
                                    val radius = size.minDimension * 0.46f
                                    val center = Offset(size.width * 0.5f, size.height * 0.5f)

                                    drawCircle(
                                        color = activeAccent.copy(alpha = 0.14f),
                                        radius = radius,
                                        center = center,
                                        style = Stroke(width = strokeWidth)
                                    )

                                    val sweep = if (isCompleted) 360f else (progressAnim.value * 360f)
                                    drawArc(
                                        brush = Brush.sweepGradient(
                                            colors = listOf(secondaryAccent, activeAccent, secondaryAccent),
                                            center = center
                                        ),
                                        startAngle = -90f,
                                        sweepAngle = sweep,
                                        useCenter = false,
                                        topLeft = Offset(center.x - radius, center.y - radius),
                                        size = Size(radius * 2f, radius * 2f),
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isCompleted) {
                                                Brush.linearGradient(listOf(activeAccent, secondaryAccent))
                                            } else {
                                                Brush.linearGradient(
                                                    listOf(
                                                        activeAccent.copy(alpha = if (isDark) 0.16f else 0.11f),
                                                        secondaryAccent.copy(alpha = if (isDark) 0.12f else 0.08f)
                                                    )
                                                )
                                            }
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = activeAccent.copy(alpha = if (isCompleted) 0.85f else 0.35f),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!isCompleted) {
                                        Icon(
                                            imageVector = if (isSignUp) Icons.Outlined.Person else Icons.Outlined.LockOpen,
                                            contentDescription = null,
                                            tint = activeAccent,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    } else {
                                        Canvas(modifier = Modifier.size(30.dp)) {
                                            val checkPath = Path().apply {
                                                moveTo(size.width * 0.18f, size.height * 0.52f)
                                                lineTo(size.width * 0.42f, size.height * 0.74f)
                                                lineTo(size.width * 0.84f, size.height * 0.28f)
                                            }
                                            val measure = PathMeasure()
                                            measure.setPath(checkPath, false)
                                            val animatedSegment = Path()
                                            measure.getSegment(
                                                startDistance = 0f,
                                                stopDistance = measure.length * checkDrawAnim.value,
                                                destination = animatedSegment,
                                                startWithMoveTo = true
                                            )
                                            drawPath(
                                                path = animatedSegment,
                                                color = onAccentText,
                                                style = Stroke(
                                                    width = 3.4.dp.toPx(),
                                                    cap = StrokeCap.Round,
                                                    join = StrokeJoin.Round
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(26.dp))

                            // Clean brand whisper
                            Text(
                                text = APP_NAME,
                                color = activeAccent,
                                fontFamily = TransitionScriptFont,
                                fontSize = 26.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Main status headline
                            AnimatedContent(
                                targetState = mainTitle,
                                transitionSpec = {
                                    (fadeIn(tween(320)) +
                                        slideInVertically(tween(320, easing = FastOutSlowInEasing)) { it / 3 } +
                                        scaleIn(initialScale = 0.97f, animationSpec = tween(320)))
                                        .togetherWith(
                                            fadeOut(tween(200)) +
                                                slideOutVertically(tween(200)) { -it / 4 } +
                                                scaleOut(targetScale = 0.97f, animationSpec = tween(200))
                                        )
                                },
                                label = "transitionTitle"
                            ) { titleText ->
                                Text(
                                    text = titleText,
                                    color = appColors.textPrimary,
                                    fontFamily = TransitionRoundedFont,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.testTag("auth_transition_title")
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Calm, minimal subtitle
                            AnimatedContent(
                                targetState = subtitleText,
                                transitionSpec = {
                                    (fadeIn(tween(260)) + slideInVertically(tween(260)) { it / 4 })
                                        .togetherWith(fadeOut(tween(180)) + slideOutVertically(tween(180)) { -it / 4 })
                                },
                                label = "transitionSubtitle"
                            ) { subText ->
                                Text(
                                    text = subText,
                                    color = if (isCompleted) activeAccent else appColors.textSecondary,
                                    fontFamily = TransitionRoundedFont,
                                    fontSize = 14.5.sp,
                                    fontWeight = if (isCompleted) FontWeight.Bold else FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(26.dp))

                            // Sleek minimal progress indicator
                            Box(
                                modifier = Modifier
                                    .width(164.dp)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(appColors.glassSurface)
                            ) {
                                val barFraction = if (isCompleted) 1f else progressAnim.value.coerceIn(0.06f, 1f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(barFraction)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(activeAccent, secondaryAccent)
                                            )
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
