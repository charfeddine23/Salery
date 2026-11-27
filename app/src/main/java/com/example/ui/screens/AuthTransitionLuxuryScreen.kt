package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

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
    // Prevent accidental back dismissal during the 2-second verification & celebration sequence
    BackHandler(enabled = true) {}

    val appColors = LiquidGlassTheme.colors
    val isCompleted = state.isCompleted
    val isSignUp = state.kind == AuthTransitionKind.SIGN_UP

    // Drive smooth 0 -> 1 progress over exact 2000ms preparing phase
    val progressAnim = remember(state.kind, state.displayName) { Animatable(0f) }
    val checkDrawAnim = remember(state.kind, state.displayName) { Animatable(0f) }
    val burstPulseAnim = remember(state.kind, state.displayName) { Animatable(0f) }
    var activeStepIndex by remember(state.kind, state.displayName) { mutableIntStateOf(0) }

    LaunchedEffect(state.kind, state.displayName) {
        progressAnim.snapTo(0f)
        progressAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1950, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(state.kind, state.displayName, isCompleted) {
        if (!isCompleted) {
            activeStepIndex = 0
            delay(650L)
            activeStepIndex = 1
            delay(650L)
            activeStepIndex = 2
        } else {
            checkDrawAnim.snapTo(0f)
            burstPulseAnim.snapTo(0f)
            checkDrawAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
            )
            burstPulseAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing)
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "luxuryAuthTransition")
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbitAngle"
    )
    val counterOrbitAngle by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "counterOrbitAngle"
    )
    val corePulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "corePulse"
    )

    val activeAccent by animateColorAsState(
        targetValue = if (isCompleted) appColors.successColor else appColors.primaryNeon,
        animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
        label = "activeAccent"
    )
    val secondaryAccent by animateColorAsState(
        targetValue = if (isCompleted) appColors.primaryNeon else appColors.secondaryNeon,
        animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
        label = "secondaryAccent"
    )
    val cardBorderColor by animateColorAsState(
        targetValue = if (isCompleted) {
            appColors.successColor.copy(alpha = 0.72f)
        } else {
            appColors.glassBorder
        },
        animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
        label = "cardBorderColor"
    )
    val medallionScale by animateFloatAsState(
        targetValue = if (isCompleted) 1.04f else corePulse,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "medallionScale"
    )

    val microStepText = remember(isSignUp, isArabic, activeStepIndex) {
        if (isSignUp) {
            if (isArabic) {
                listOf(
                    "توليد مفاتيح التشفير الآمنة...",
                    "إعداد خزنتك المالية الخاصة...",
                    "ربط وتوثيق ملفك الشخصي..."
                )[activeStepIndex.coerceIn(0, 2)]
            } else {
                listOf(
                    "Generating hardware encryption keys...",
                    "Preparing your encrypted vault...",
                    "Securing your cloud profile..."
                )[activeStepIndex.coerceIn(0, 2)]
            }
        } else {
            if (isArabic) {
                listOf(
                    "التحقق من بيانات الهوية...",
                    "فك تشفير الخزنة الآمنة...",
                    "مزامنة الأرصدة والسجلات..."
                )[activeStepIndex.coerceIn(0, 2)]
            } else {
                listOf(
                    "Verifying encrypted credentials...",
                    "Unlocking your private vault...",
                    "Syncing balances & records..."
                )[activeStepIndex.coerceIn(0, 2)]
            }
        }
    }

    val mainTitle = remember(isSignUp, isCompleted, isArabic) {
        when {
            !isCompleted && isSignUp -> if (isArabic) "جاري إنشاء الحساب..." else "Creating your account..."
            !isCompleted && !isSignUp -> if (isArabic) "جاري تسجيل الدخول..." else "Signing you in..."
            isCompleted && isSignUp -> if (isArabic) "تم إنشاء الحساب بنجاح" else "Account created successfully"
            else -> if (isArabic) "تم تسجيل الدخول بنجاح" else "Signed in successfully"
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
            // Dynamic morphing neon blobs orbiting behind the centered Liquid Glass card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val w = size.width
                    val h = size.height
                    val rad = Math.toRadians(orbitAngle.toDouble())
                    val offset1 = Offset(
                        x = w * 0.14f + (cos(rad) * w * 0.12f).toFloat(),
                        y = h * 0.14f + (sin(rad) * h * 0.08f).toFloat()
                    )
                    val offset2 = Offset(
                        x = w * 0.86f - (cos(rad) * w * 0.12f).toFloat(),
                        y = h * 0.84f - (sin(rad) * h * 0.08f).toFloat()
                    )

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                activeAccent.copy(alpha = if (appColors.isDark) 0.55f else 0.35f),
                                Color.Transparent
                            ),
                            center = offset1,
                            radius = w * 0.48f
                        ),
                        radius = w * 0.48f,
                        center = offset1
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                secondaryAccent.copy(alpha = if (appColors.isDark) 0.48f else 0.30f),
                                Color.Transparent
                            ),
                            center = offset2,
                            radius = w * 0.46f
                        ),
                        radius = w * 0.46f,
                        center = offset2
                    )

                    if (isCompleted && burstPulseAnim.value < 0.99f) {
                        val pulseProgress = burstPulseAnim.value
                        val ringRadius = w * (0.36f + pulseProgress * 0.52f)
                        drawCircle(
                            color = activeAccent.copy(alpha = (1f - pulseProgress) * 0.50f),
                            radius = ringRadius,
                            center = Offset(w * 0.5f, h * 0.5f),
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }
                }

                val cardShape = RoundedCornerShape(30.dp)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .breathingGlow(
                            enabled = true,
                            glowColor = activeAccent,
                            secondaryGlowColor = secondaryAccent,
                            cornerRadius = 30.dp,
                            minAlpha = if (appColors.isDark) 0.09f else 0.05f,
                            maxAlpha = if (appColors.isDark) 0.28f else 0.14f,
                            animateContainerScale = false
                        )
                        .shadow(
                            elevation = if (appColors.isDark) 28.dp else 18.dp,
                            shape = cardShape,
                            ambientColor = activeAccent.copy(alpha = 0.25f),
                            spotColor = Color.Black.copy(alpha = if (appColors.isDark) 0.55f else 0.14f)
                        ),
                    shape = cardShape,
                    color = appColors.glassCard,
                    border = BorderStroke(1.2.dp, cardBorderColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(cardShape)
                            .background(appColors.glassHighlightBrush)
                            .padding(horizontal = 26.dp, vertical = 34.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = APP_NAME,
                            color = activeAccent,
                            fontFamily = TransitionScriptFont,
                            fontSize = 31.sp
                        )

                        Spacer(modifier = Modifier.height(26.dp))

                        // Dynamic Orbital Hero Emblem
                        Box(
                            modifier = Modifier
                                .size(148.dp)
                                .graphicsLayer {
                                    scaleX = medallionScale
                                    scaleY = medallionScale
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val minDim = size.minDimension
                                val center = Offset(size.width * 0.5f, size.height * 0.5f)
                                val outerRadius = minDim * 0.45f
                                val innerRadius = minDim * 0.36f
                                val strokeOuter = 3.5.dp.toPx()
                                val strokeInner = 1.6.dp.toPx()

                                // Ambient center aura
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            activeAccent.copy(alpha = if (isCompleted) 0.34f else 0.22f),
                                            Color.Transparent
                                        ),
                                        center = center,
                                        radius = minDim * 0.50f
                                    ),
                                    radius = minDim * 0.50f,
                                    center = center
                                )

                                // Outer background track
                                drawCircle(
                                    color = activeAccent.copy(alpha = 0.16f),
                                    radius = outerRadius,
                                    center = center,
                                    style = Stroke(width = strokeOuter)
                                )

                                // Dynamic sweeping progress arc (fills over 2 seconds)
                                val sweep = if (isCompleted) 360f else (progressAnim.value * 360f)
                                drawArc(
                                    brush = Brush.sweepGradient(
                                        colors = listOf(
                                            secondaryAccent,
                                            activeAccent,
                                            secondaryAccent
                                        ),
                                        center = center
                                    ),
                                    startAngle = -90f,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                                    size = Size(outerRadius * 2f, outerRadius * 2f),
                                    style = Stroke(width = strokeOuter, cap = StrokeCap.Round)
                                )

                                // Orbiting shimmer arc & luminous node while verifying
                                if (!isCompleted) {
                                    rotate(degrees = orbitAngle, pivot = center) {
                                        drawArc(
                                            color = Color.White.copy(alpha = 0.55f),
                                            startAngle = -15f,
                                            sweepAngle = 42f,
                                            useCenter = false,
                                            topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                                            size = Size(outerRadius * 2f, outerRadius * 2f),
                                            style = Stroke(width = strokeOuter * 0.7f, cap = StrokeCap.Round)
                                        )
                                        drawCircle(
                                            color = activeAccent,
                                            radius = 4.dp.toPx(),
                                            center = Offset(center.x + outerRadius, center.y)
                                        )
                                    }

                                    rotate(degrees = counterOrbitAngle, pivot = center) {
                                        drawArc(
                                            color = secondaryAccent.copy(alpha = 0.50f),
                                            startAngle = 30f,
                                            sweepAngle = 110f,
                                            useCenter = false,
                                            topLeft = Offset(center.x - innerRadius, center.y - innerRadius),
                                            size = Size(innerRadius * 2f, innerRadius * 2f),
                                            style = Stroke(width = strokeInner, cap = StrokeCap.Round)
                                        )
                                        drawArc(
                                            color = activeAccent.copy(alpha = 0.45f),
                                            startAngle = 210f,
                                            sweepAngle = 95f,
                                            useCenter = false,
                                            topLeft = Offset(center.x - innerRadius, center.y - innerRadius),
                                            size = Size(innerRadius * 2f, innerRadius * 2f),
                                            style = Stroke(width = strokeInner, cap = StrokeCap.Round)
                                        )
                                    }
                                } else {
                                    // Sparkle orbital accents on completion
                                    for (i in 0 until 6) {
                                        val angle = Math.toRadians((i * 60.0) + orbitAngle * 0.15)
                                        val sparkleDist = outerRadius * (1.08f + 0.08f * burstPulseAnim.value)
                                        val sx = center.x + (cos(angle) * sparkleDist).toFloat()
                                        val sy = center.y + (sin(angle) * sparkleDist).toFloat()
                                        drawCircle(
                                            color = if (i % 2 == 0) activeAccent else secondaryAccent,
                                            radius = (2.6.dp.toPx() * checkDrawAnim.value),
                                            center = Offset(sx, sy),
                                            alpha = 0.85f
                                        )
                                    }
                                }
                            }

                            // Core Glass Emblem + Animated Checkmark
                            Box(
                                modifier = Modifier
                                    .size(84.dp)
                                    .shadow(
                                        elevation = 16.dp,
                                        shape = CircleShape,
                                        ambientColor = activeAccent,
                                        spotColor = activeAccent
                                    )
                                    .clip(CircleShape)
                                    .background(
                                        if (isCompleted) {
                                            Brush.linearGradient(listOf(activeAccent, secondaryAccent))
                                        } else {
                                            Brush.linearGradient(
                                                listOf(
                                                    activeAccent.copy(alpha = if (appColors.isDark) 0.22f else 0.16f),
                                                    secondaryAccent.copy(alpha = if (appColors.isDark) 0.18f else 0.12f)
                                                )
                                            )
                                        }
                                    )
                                    .border(
                                        width = 1.5.dp,
                                        color = activeAccent.copy(alpha = if (isCompleted) 0.9f else 0.55f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!isCompleted) {
                                    Icon(
                                        imageVector = if (isSignUp) Icons.Rounded.PersonAdd else Icons.Rounded.LockOpen,
                                        contentDescription = null,
                                        tint = activeAccent,
                                        modifier = Modifier.size(34.dp)
                                    )
                                } else {
                                    Canvas(modifier = Modifier.size(40.dp)) {
                                        val checkPath = Path().apply {
                                            moveTo(size.width * 0.20f, size.height * 0.52f)
                                            lineTo(size.width * 0.42f, size.height * 0.74f)
                                            lineTo(size.width * 0.82f, size.height * 0.28f)
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
                                                width = 4.dp.toPx(),
                                                cap = StrokeCap.Round,
                                                join = StrokeJoin.Round
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(26.dp))

                        // Animated Phase Headline
                        AnimatedContent(
                            targetState = mainTitle,
                            transitionSpec = {
                                (fadeIn(tween(340)) +
                                    slideInVertically(tween(340, easing = FastOutSlowInEasing)) { it / 3 } +
                                    scaleIn(initialScale = 0.96f, animationSpec = tween(340)))
                                    .togetherWith(
                                        fadeOut(tween(220)) +
                                            slideOutVertically(tween(220)) { -it / 4 } +
                                            scaleOut(targetScale = 0.96f, animationSpec = tween(220))
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

                        Spacer(modifier = Modifier.height(10.dp))

                        // Dynamic Micro-steps while loading -> Personalized greeting when complete
                        AnimatedContent(
                            targetState = if (isCompleted) {
                                val name = state.displayName.trim()
                                if (name.isNotBlank()) {
                                    if (isArabic) "أهلاً بك، $name" else "Welcome, $name"
                                } else {
                                    if (isArabic) "خزنتك المالية جاهزة الآن" else "Your encrypted vault is ready"
                                }
                            } else {
                                microStepText
                            },
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

                        Spacer(modifier = Modifier.height(24.dp))

                        // Bottom sleek progress bar & security status badge
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.76f)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(appColors.glassSurface)
                                .border(0.6.dp, appColors.glassBorderSubtle, CircleShape)
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

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(activeAccent.copy(alpha = if (appColors.isDark) 0.12f else 0.08f))
                                .border(1.dp, activeAccent.copy(alpha = 0.35f), CircleShape)
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isCompleted) Icons.Rounded.VerifiedUser else Icons.Rounded.Shield,
                                contentDescription = null,
                                tint = activeAccent,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isCompleted) {
                                    if (isArabic) "AES-256-GCM • الخزنة مفعّلة" else "AES-256-GCM • Vault Ready"
                                } else {
                                    if (isArabic) "تشفير عتادي متقدّم" else "Hardware-Backed Encryption"
                                },
                                color = activeAccent,
                                fontFamily = TransitionRoundedFont,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
