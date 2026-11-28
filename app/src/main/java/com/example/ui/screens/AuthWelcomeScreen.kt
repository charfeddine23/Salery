package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
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
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import com.example.R
import com.example.cloud.GoogleAuthUiState
import com.example.ui.components.GlassBackground
import com.example.ui.i18n.APP_NAME
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.LocalScreenshotProtection
import com.example.ui.theme.VaultixBrandOrange
import com.example.ui.theme.VaultixBrandOrangeLight
import com.example.ui.theme.breathingGlow
import com.example.viewmodel.SavedAuthAccount

private val VaultixScriptFont = FontFamily(
    Font(R.font.pacifico, FontWeight.Normal)
)

private val VaultixRoundedFont = FontFamily(
    Font(R.font.nunito, FontWeight.Normal)
)

private val AUTH_EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

private fun isValidAuthEmail(email: String): Boolean = AUTH_EMAIL_REGEX.matches(email.trim())

private enum class AuthPageMode {
    WELCOME,
    LOGIN,
    REGISTER,
    GUEST_MODE
}

private data class AuthVisualPalette(
    val isDark: Boolean,
    val cardSurface: Color,
    val dialogSurface: Color,
    val cardBorder: Color,
    val topControlBg: Color,
    val topControlBorder: Color,
    val fieldBg: Color,
    val fieldFocusedBg: Color,
    val fieldBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accentCyan: Color,
    val accentCyanDeep: Color,
    val brandOrange: Color,
    val onAccentText: Color,
    val googleBg: Color,
    val googleBorder: Color,
    val guestBg: Color,
    val guestBorder: Color,
    val infoBg: Color,
    val infoBorder: Color,
    val errorRed: Color,
    val successGreen: Color
)

@Composable
private fun rememberAnimatedAuthPalette(): AuthVisualPalette {
    val appColors = LiquidGlassTheme.colors
    val target = remember(appColors) {
        val avgAccentLuminance = (appColors.primaryNeon.luminance() + appColors.secondaryNeon.luminance()) * 0.5f
        val onAccent = if (avgAccentLuminance > 0.48f) Color(0xFF07111F) else Color.White

        AuthVisualPalette(
            isDark = appColors.isDark,
            cardSurface = appColors.glassCard,
            dialogSurface = appColors.dialogBackground,
            cardBorder = appColors.glassBorder,
            topControlBg = appColors.glassSurface,
            topControlBorder = appColors.glassBorder,
            fieldBg = appColors.glassSurface,
            fieldFocusedBg = appColors.glassCard,
            fieldBorder = appColors.glassBorderSubtle,
            textPrimary = appColors.textPrimary,
            textSecondary = appColors.textSecondary,
            textMuted = appColors.textMuted,
            accentCyan = appColors.primaryNeon,
            accentCyanDeep = appColors.secondaryNeon,
            brandOrange = if (appColors.isDark) VaultixBrandOrange else VaultixBrandOrangeLight,
            onAccentText = onAccent,
            googleBg = appColors.glassSurface,
            googleBorder = appColors.glassBorder,
            guestBg = appColors.primaryNeon.copy(alpha = if (appColors.isDark) 0.12f else 0.08f),
            guestBorder = appColors.primaryNeon.copy(alpha = 0.45f),
            infoBg = appColors.primaryNeon.copy(alpha = if (appColors.isDark) 0.11f else 0.07f),
            infoBorder = appColors.glassBorder,
            errorRed = appColors.errorColor,
            successGreen = appColors.successColor
        )
    }

    val colorSpec = tween<Color>(durationMillis = 360, easing = FastOutSlowInEasing)
    return AuthVisualPalette(
        isDark = appColors.isDark,
        cardSurface = animateColorAsState(target.cardSurface, colorSpec, label = "cardSurface").value,
        dialogSurface = animateColorAsState(target.dialogSurface, colorSpec, label = "dialogSurface").value,
        cardBorder = animateColorAsState(target.cardBorder, colorSpec, label = "cardBorder").value,
        topControlBg = animateColorAsState(target.topControlBg, colorSpec, label = "topControlBg").value,
        topControlBorder = animateColorAsState(target.topControlBorder, colorSpec, label = "topControlBorder").value,
        fieldBg = animateColorAsState(target.fieldBg, colorSpec, label = "fieldBg").value,
        fieldFocusedBg = animateColorAsState(target.fieldFocusedBg, colorSpec, label = "fieldFocusedBg").value,
        fieldBorder = animateColorAsState(target.fieldBorder, colorSpec, label = "fieldBorder").value,
        textPrimary = animateColorAsState(target.textPrimary, colorSpec, label = "textPrimary").value,
        textSecondary = animateColorAsState(target.textSecondary, colorSpec, label = "textSecondary").value,
        textMuted = animateColorAsState(target.textMuted, colorSpec, label = "textMuted").value,
        accentCyan = animateColorAsState(target.accentCyan, colorSpec, label = "accentCyan").value,
        accentCyanDeep = animateColorAsState(target.accentCyanDeep, colorSpec, label = "accentCyanDeep").value,
        brandOrange = animateColorAsState(target.brandOrange, colorSpec, label = "brandOrange").value,
        onAccentText = animateColorAsState(target.onAccentText, colorSpec, label = "onAccentText").value,
        googleBg = animateColorAsState(target.googleBg, colorSpec, label = "googleBg").value,
        googleBorder = animateColorAsState(target.googleBorder, colorSpec, label = "googleBorder").value,
        guestBg = animateColorAsState(target.guestBg, colorSpec, label = "guestBg").value,
        guestBorder = animateColorAsState(target.guestBorder, colorSpec, label = "guestBorder").value,
        infoBg = animateColorAsState(target.infoBg, colorSpec, label = "infoBg").value,
        infoBorder = animateColorAsState(target.infoBorder, colorSpec, label = "infoBorder").value,
        errorRed = animateColorAsState(target.errorRed, colorSpec, label = "errorRed").value,
        successGreen = animateColorAsState(target.successGreen, colorSpec, label = "successGreen").value
    )
}

@Composable
fun AuthWelcomeScreen(
    isArabic: Boolean,
    isDarkTheme: Boolean = true,
    googleAuthState: GoogleAuthUiState = GoogleAuthUiState.SignedOut,
    savedAccount: SavedAuthAccount? = null,
    onToggleLanguage: (String) -> Unit,
    onToggleTheme: () -> Unit = {},
    onGoogleSignIn: () -> Unit = {},
    onEmailSignIn: (email: String, password: String) -> Unit = { _, _ -> },
    onEmailSignUp: (firstName: String, lastName: String, username: String, email: String, password: String) -> Unit = { _, _, _, _, _ -> },
    onSendPasswordReset: (email: String, onResult: (Boolean, String) -> Unit) -> Unit = { _, cb -> cb(false, "") },
    onClearAuthError: () -> Unit = {},
    onContinueSignedInGoogle: () -> Unit = {},
    onGoogleSignOut: () -> Unit = {},
    onCreateGuestAccount: (username: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val appColors = LiquidGlassTheme.colors
    val palette = rememberAnimatedAuthPalette()
    val isLoading = googleAuthState is GoogleAuthUiState.Loading
    val signedInState = googleAuthState as? GoogleAuthUiState.SignedIn
    val resolvedSavedAccount = remember(signedInState, savedAccount) {
        signedInState?.let {
            SavedAuthAccount(
                displayName = it.displayName,
                email = it.email,
                photoUrl = it.photoUrl,
                isCloudLinked = true
            )
        } ?: savedAccount
    }
    val errorState = googleAuthState as? GoogleAuthUiState.Error

    var pageMode by rememberSaveable { mutableStateOf(AuthPageMode.WELCOME) }

    // Login inputs
    var loginEmail by rememberSaveable { mutableStateOf("") }
    var loginPassword by rememberSaveable { mutableStateOf("") }
    var loginPasswordVisible by rememberSaveable { mutableStateOf(false) }

    // Register inputs (5 exact fields)
    var firstName by rememberSaveable { mutableStateOf("") }
    var lastName by rememberSaveable { mutableStateOf("") }
    var signUpEmail by rememberSaveable { mutableStateOf("") }
    var signUpPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var signUpPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }

    // Guest input
    var guestUsername by rememberSaveable { mutableStateOf("") }

    var localValidationError by rememberSaveable { mutableStateOf<String?>(null) }
    var showForgotPasswordDialog by rememberSaveable { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    val navigateTo: (AuthPageMode) -> Unit = { target ->
        focusManager.clearFocus()
        localValidationError = null
        onClearAuthError()
        if (target == AuthPageMode.REGISTER && loginEmail.isNotBlank() && signUpEmail.isBlank()) {
            signUpEmail = loginEmail.trim()
        }
        if (target == AuthPageMode.LOGIN && signUpEmail.isNotBlank() && loginEmail.isBlank()) {
            loginEmail = signUpEmail.trim()
        }
        pageMode = target
    }

    BackHandler(enabled = pageMode != AuthPageMode.WELCOME) {
        navigateTo(AuthPageMode.WELCOME)
    }

    // Smoothly animate background morphing blob parameters on page navigation
    val animSpec = tween<Float>(durationMillis = 720, easing = FastOutSlowInEasing)
    val blobCenterXFraction by animateFloatAsState(
        targetValue = when (pageMode) {
            AuthPageMode.WELCOME -> 0.16f
            AuthPageMode.LOGIN -> 0.02f
            AuthPageMode.REGISTER -> 0.87f
            AuthPageMode.GUEST_MODE -> 0.50f
        },
        animationSpec = animSpec,
        label = "blobCenterX"
    )
    val blobCenterYFraction by animateFloatAsState(
        targetValue = when (pageMode) {
            AuthPageMode.WELCOME -> 0.14f
            AuthPageMode.LOGIN -> 0.36f
            AuthPageMode.REGISTER -> 0.72f
            AuthPageMode.GUEST_MODE -> 0.05f
        },
        animationSpec = animSpec,
        label = "blobCenterY"
    )
    val blobWidthFraction by animateFloatAsState(
        targetValue = when (pageMode) {
            AuthPageMode.WELCOME -> 0.56f
            AuthPageMode.LOGIN -> 0.52f
            AuthPageMode.REGISTER -> 0.86f
            AuthPageMode.GUEST_MODE -> 0.58f
        },
        animationSpec = animSpec,
        label = "blobWidth"
    )
    val blobHeightFraction by animateFloatAsState(
        targetValue = when (pageMode) {
            AuthPageMode.WELCOME -> 0.42f
            AuthPageMode.LOGIN -> 0.62f
            AuthPageMode.REGISTER -> 0.72f
            AuthPageMode.GUEST_MODE -> 0.28f
        },
        animationSpec = animSpec,
        label = "blobHeight"
    )
    val blobRotation by animateFloatAsState(
        targetValue = when (pageMode) {
            AuthPageMode.WELCOME -> -16f
            AuthPageMode.LOGIN -> -6f
            AuthPageMode.REGISTER -> 24f
            AuthPageMode.GUEST_MODE -> 0f
        },
        animationSpec = animSpec,
        label = "blobRotation"
    )
    val behindBlobAlpha by animateFloatAsState(
        targetValue = when (pageMode) {
            AuthPageMode.WELCOME -> 0.22f
            AuthPageMode.LOGIN -> 0.96f
            AuthPageMode.REGISTER -> 0.98f
            AuthPageMode.GUEST_MODE -> 0.96f
        },
        animationSpec = animSpec,
        label = "behindBlobAlpha"
    )
    val innerCloudProgress by animateFloatAsState(
        targetValue = if (pageMode == AuthPageMode.WELCOME) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "innerCloudProgress"
    )

    val blobColorTop by animateColorAsState(
        targetValue = palette.accentCyan,
        animationSpec = tween(680, easing = FastOutSlowInEasing),
        label = "blobColorTop"
    )
    val blobColorBottom by animateColorAsState(
        targetValue = when (pageMode) {
            AuthPageMode.WELCOME -> palette.accentCyan
            AuthPageMode.LOGIN -> palette.accentCyanDeep
            AuthPageMode.REGISTER -> palette.accentCyanDeep
            AuthPageMode.GUEST_MODE -> palette.accentCyanDeep
        },
        animationSpec = tween(680, easing = FastOutSlowInEasing),
        label = "blobColorBottom"
    )

    // Subtle continuous ambient motion
    val infiniteTransition = rememberInfiniteTransition(label = "authAmbient")
    val driftPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "driftPhase"
    )

    GlassBackground(
        modifier = modifier.fillMaxSize()
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val screenMinHeight = maxHeight

            // Full-bleed unclipped atmospheric glow layer (completely removes top/bottom color seams)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val maxDim = maxOf(w, h)
                val driftX = (driftPhase - 0.5f) * w * 0.03f
                val driftY = (0.5f - driftPhase) * h * 0.025f

                // Smooth seamless top gradient extending cleanly behind status bar & top action bar
                drawRect(
                    brush = Brush.verticalGradient(
                        0.0f to blobColorTop.copy(alpha = if (palette.isDark) 0.11f else 0.08f),
                        0.24f to blobColorBottom.copy(alpha = if (palette.isDark) 0.055f else 0.045f),
                        0.56f to Color.Transparent,
                        1.0f to Color.Transparent
                    )
                )

                // Wide unclipped soft radial aurora tracking the active auth page
                val auraCx = w * (0.18f + blobCenterXFraction * 0.64f) + driftX
                val auraCy = h * (0.24f + blobCenterYFraction * 0.48f) + driftY
                drawCircle(
                    brush = Brush.radialGradient(
                        0.0f to blobColorTop.copy(alpha = if (palette.isDark) 0.22f else 0.14f),
                        0.36f to blobColorBottom.copy(alpha = if (palette.isDark) 0.11f else 0.075f),
                        0.68f to palette.brandOrange.copy(alpha = if (palette.isDark) 0.04f else 0.025f),
                        1.0f to Color.Transparent,
                        center = Offset(auraCx, auraCy),
                        radius = maxDim * 0.78f
                    ),
                    radius = maxDim * 0.78f,
                    center = Offset(auraCx, auraCy)
                )
            }

            // Full-screen scrollable host so behind-card Canvas glow is never clipped below the top bar
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = screenMinHeight)
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 20.dp, vertical = 72.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Floating Glass Card + Smooth Feathered Behind-Card Glow Wrapper
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 440.dp)
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(
                            modifier = Modifier.matchParentSize()
                        ) {
                            val w = size.width
                            val h = size.height
                            val driftX = (driftPhase - 0.5f) * 14f
                            val driftY = (0.5f - driftPhase) * 12f
                            val cx = w * blobCenterXFraction + driftX
                            val cy = h * blobCenterYFraction + driftY
                            val rx = w * blobWidthFraction
                            val ry = h * blobHeightFraction

                            // Soft feathered radial-gradient morphing aura behind card (no hard cut edges)
                            if (behindBlobAlpha > 0.02f) {
                                val effectiveAlpha = (behindBlobAlpha * 0.48f).coerceAtMost(0.45f)
                                rotate(degrees = blobRotation, pivot = Offset(cx, cy)) {
                                    drawOval(
                                        brush = Brush.radialGradient(
                                            0.0f to blobColorTop.copy(alpha = effectiveAlpha),
                                            0.52f to blobColorBottom.copy(alpha = effectiveAlpha * 0.48f),
                                            1.0f to Color.Transparent,
                                            center = Offset(cx, cy),
                                            radius = maxOf(rx, ry) * 0.62f
                                        ),
                                        topLeft = Offset(cx - rx * 0.5f, cy - ry * 0.5f),
                                        size = Size(rx, ry)
                                    )
                                }
                            }
                        }

                // Main Rounded Glass Card matching internal GlassCard surface, specular border, and breathingGlow
                val cardShape = RoundedCornerShape(28.dp)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .breathingGlow(
                            enabled = true,
                            glowColor = palette.accentCyan,
                            secondaryGlowColor = palette.accentCyanDeep,
                            cornerRadius = 28.dp,
                            minAlpha = if (palette.isDark) 0.06f else 0.04f,
                            maxAlpha = if (palette.isDark) 0.20f else 0.11f,
                            animateContainerScale = false
                        )
                        .shadow(
                            elevation = if (palette.isDark) 24.dp else 16.dp,
                            shape = cardShape,
                            ambientColor = palette.accentCyan.copy(alpha = if (palette.isDark) 0.18f else 0.12f),
                            spotColor = Color.Black.copy(alpha = if (palette.isDark) 0.50f else 0.12f)
                        )
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = 0.85f,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ),
                    shape = cardShape,
                    color = palette.cardSurface,
                    border = BorderStroke(1.dp, palette.cardBorder)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(cardShape)
                            .background(appColors.glassHighlightBrush)
                    ) {
                        // Top-left organic cloud blob inside the card on WELCOME page (using internal app neon gradient)
                        if (innerCloudProgress > 0.01f) {
                            Canvas(modifier = Modifier.matchParentSize()) {
                                val p = innerCloudProgress
                                val w = size.width
                                val floatY = (driftPhase - 0.5f) * 6f
                                val cloudColor = Brush.linearGradient(
                                    colors = listOf(
                                        palette.accentCyan,
                                        palette.accentCyanDeep
                                    )
                                )
                                drawCircle(
                                    brush = cloudColor,
                                    radius = w * 0.22f * p,
                                    center = Offset(w * 0.10f * p, w * 0.07f * p + floatY),
                                    alpha = p
                                )
                                drawCircle(
                                    brush = cloudColor,
                                    radius = w * 0.145f * p,
                                    center = Offset(w * 0.25f * p, w * 0.055f * p + floatY),
                                    alpha = p
                                )
                                drawCircle(
                                    brush = cloudColor,
                                    radius = w * 0.155f * p,
                                    center = Offset(w * 0.05f * p, w * 0.21f * p + floatY),
                                    alpha = p
                                )
                            }
                        }

                        AnimatedContent(
                            targetState = pageMode,
                            transitionSpec = {
                                val forward = targetState.ordinal >= initialState.ordinal
                                (fadeIn(tween(320)) +
                                    slideInHorizontally(tween(340, easing = FastOutSlowInEasing)) { width ->
                                        if (forward) width / 8 else -width / 8
                                    } +
                                    scaleIn(initialScale = 0.97f, animationSpec = tween(320)))
                                    .togetherWith(
                                        fadeOut(tween(220)) +
                                            slideOutHorizontally(tween(260, easing = FastOutSlowInEasing)) { width ->
                                                if (forward) -width / 10 else width / 10
                                            } +
                                            scaleOut(targetScale = 0.98f, animationSpec = tween(220))
                                    )
                            },
                            label = "authPageContent"
                        ) { mode ->
                            when (mode) {
                                AuthPageMode.WELCOME -> {
                                    WelcomeCardContent(
                                        isArabic = isArabic,
                                        palette = palette,
                                        isLoading = isLoading,
                                        savedAccount = resolvedSavedAccount,
                                        errorMessage = errorState?.message,
                                        onRegisterClick = { navigateTo(AuthPageMode.REGISTER) },
                                        onHaveAccountClick = { navigateTo(AuthPageMode.LOGIN) },
                                        onGoogleSignIn = {
                                            localValidationError = null
                                            onClearAuthError()
                                            onGoogleSignIn()
                                        },
                                        onGuestModeClick = { navigateTo(AuthPageMode.GUEST_MODE) },
                                        onContinueSignedInGoogle = onContinueSignedInGoogle,
                                        onGoogleSignOut = onGoogleSignOut
                                    )
                                }

                                AuthPageMode.GUEST_MODE -> {
                                    GuestModeCardContent(
                                        isArabic = isArabic,
                                        palette = palette,
                                        username = guestUsername,
                                        onUsernameChange = {
                                            guestUsername = it
                                            localValidationError = null
                                        },
                                        validationError = localValidationError,
                                        onBack = { navigateTo(AuthPageMode.WELCOME) },
                                        onConfirm = {
                                            focusManager.clearFocus()
                                            val clean = guestUsername.trim()
                                            if (clean.isEmpty()) {
                                                localValidationError = if (isArabic) {
                                                    "يرجى إدخال اسم المستخدم للمتابعة"
                                                } else {
                                                    "Please enter a username to continue"
                                                }
                                            } else {
                                                onCreateGuestAccount(clean)
                                            }
                                        }
                                    )
                                }

                                AuthPageMode.REGISTER -> {
                                    RegisterCardContent(
                                        isArabic = isArabic,
                                        palette = palette,
                                        isLoading = isLoading,
                                        firstName = firstName,
                                        onFirstNameChange = {
                                            firstName = it
                                            localValidationError = null
                                            onClearAuthError()
                                        },
                                        lastName = lastName,
                                        onLastNameChange = {
                                            lastName = it
                                            localValidationError = null
                                            onClearAuthError()
                                        },
                                        email = signUpEmail,
                                        onEmailChange = {
                                            signUpEmail = it
                                            localValidationError = null
                                            onClearAuthError()
                                        },
                                        password = signUpPassword,
                                        onPasswordChange = {
                                            signUpPassword = it
                                            localValidationError = null
                                            onClearAuthError()
                                        },
                                        confirmPassword = confirmPassword,
                                        onConfirmPasswordChange = {
                                            confirmPassword = it
                                            localValidationError = null
                                            onClearAuthError()
                                        },
                                        passwordVisible = signUpPasswordVisible,
                                        onTogglePasswordVisible = { signUpPasswordVisible = !signUpPasswordVisible },
                                        confirmPasswordVisible = confirmPasswordVisible,
                                        onToggleConfirmPasswordVisible = {
                                            confirmPasswordVisible = !confirmPasswordVisible
                                        },
                                        errorMessage = localValidationError ?: errorState?.message,
                                        onSwitchToLogin = { navigateTo(AuthPageMode.LOGIN) },
                                        onSubmit = {
                                            focusManager.clearFocus()
                                            val cleanFirst = firstName.trim()
                                            val cleanLast = lastName.trim()
                                            val cleanEmail = signUpEmail.trim()
                                            when {
                                                cleanFirst.isEmpty() -> {
                                                    localValidationError = if (isArabic) {
                                                        "يرجى إدخال الاسم الأول"
                                                    } else {
                                                        "Please enter your first name"
                                                    }
                                                }
                                                cleanLast.isEmpty() -> {
                                                    localValidationError = if (isArabic) {
                                                        "يرجى إدخال اسم العائلة"
                                                    } else {
                                                        "Please enter your last name"
                                                    }
                                                }
                                                cleanEmail.isEmpty() || !isValidAuthEmail(cleanEmail) -> {
                                                    localValidationError = if (isArabic) {
                                                        "يرجى إدخال بريد إلكتروني صحيح"
                                                    } else {
                                                        "Please enter a valid email address"
                                                    }
                                                }
                                                signUpPassword.length < 6 -> {
                                                    localValidationError = if (isArabic) {
                                                        "كلمة السر يجب أن تحتوي على 6 أحرف على الأقل"
                                                    } else {
                                                        "Password must be at least 6 characters"
                                                    }
                                                }
                                                confirmPassword != signUpPassword -> {
                                                    localValidationError = if (isArabic) {
                                                        "كلمتا السر غير متطابقتين"
                                                    } else {
                                                        "Passwords do not match"
                                                    }
                                                }
                                                else -> {
                                                    val derivedUsername = cleanEmail
                                                        .substringBefore('@')
                                                        .trim()
                                                        .ifBlank { "$cleanFirst $cleanLast" }
                                                    localValidationError = null
                                                    onEmailSignUp(
                                                        cleanFirst,
                                                        cleanLast,
                                                        derivedUsername,
                                                        cleanEmail,
                                                        signUpPassword
                                                    )
                                                }
                                            }
                                        }
                                    )
                                }

                                AuthPageMode.LOGIN -> {
                                    LoginCardContent(
                                        isArabic = isArabic,
                                        palette = palette,
                                        isLoading = isLoading,
                                        email = loginEmail,
                                        onEmailChange = {
                                            loginEmail = it
                                            localValidationError = null
                                            onClearAuthError()
                                        },
                                        password = loginPassword,
                                        onPasswordChange = {
                                            loginPassword = it
                                            localValidationError = null
                                            onClearAuthError()
                                        },
                                        passwordVisible = loginPasswordVisible,
                                        onTogglePasswordVisible = { loginPasswordVisible = !loginPasswordVisible },
                                        errorMessage = localValidationError ?: errorState?.message,
                                        onForgotPasswordClick = { showForgotPasswordDialog = true },
                                        onSwitchToRegister = { navigateTo(AuthPageMode.REGISTER) },
                                        onSignInSubmit = {
                                            focusManager.clearFocus()
                                            val cleanEmail = loginEmail.trim()
                                            when {
                                                cleanEmail.isEmpty() || !isValidAuthEmail(cleanEmail) -> {
                                                    localValidationError = if (isArabic) {
                                                        "يرجى إدخال بريد إلكتروني صحيح"
                                                    } else {
                                                        "Please enter a valid email address"
                                                    }
                                                }
                                                loginPassword.isEmpty() -> {
                                                    localValidationError = if (isArabic) {
                                                        "يرجى إدخال كلمة السر"
                                                    } else {
                                                        "Please enter your password"
                                                    }
                                                }
                                                else -> {
                                                    localValidationError = null
                                                    onEmailSignIn(cleanEmail, loginPassword)
                                                }
                                            }
                                        },
                                        onGoogleSignIn = {
                                            localValidationError = null
                                            onClearAuthError()
                                            onGoogleSignIn()
                                        },
                                        onGuestModeClick = { navigateTo(AuthPageMode.GUEST_MODE) }
                                    )
                                }
                            }
                        }
                    }
                }
                }
            }
            }

            // Pinned Top Bar: Back (when in sub-page) + Theme toggle on start; Language pill on end
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedVisibility(
                        visible = pageMode != AuthPageMode.WELCOME,
                        enter = fadeIn(tween(240)) + scaleIn(initialScale = 0.8f, animationSpec = tween(240)),
                        exit = fadeOut(tween(180)) + scaleOut(targetScale = 0.8f, animationSpec = tween(180))
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .shadow(
                                    elevation = if (palette.isDark) 8.dp else 4.dp,
                                    shape = CircleShape,
                                    ambientColor = Color.Black.copy(alpha = 0.3f),
                                    spotColor = Color.Black.copy(alpha = 0.3f)
                                )
                                .clip(CircleShape)
                                .background(palette.topControlBg)
                                .border(1.dp, palette.topControlBorder, CircleShape)
                                .clickable { navigateTo(AuthPageMode.WELCOME) }
                                .testTag("auth_back_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = if (isArabic) "رجوع" else "Back",
                                tint = palette.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .shadow(
                                elevation = if (palette.isDark) 10.dp else 4.dp,
                                shape = CircleShape,
                                ambientColor = palette.accentCyan.copy(alpha = 0.25f),
                                spotColor = palette.accentCyan.copy(alpha = 0.25f)
                            )
                            .clip(CircleShape)
                            .background(palette.topControlBg)
                            .border(1.dp, palette.topControlBorder, CircleShape)
                            .clickable(onClick = onToggleTheme)
                            .testTag("auth_theme_toggle"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Rounded.WbSunny else Icons.Rounded.DarkMode,
                            contentDescription = if (isDarkTheme) {
                                if (isArabic) "التبديل إلى الوضع الفاتح" else "Switch to Light Mode"
                            } else {
                                if (isArabic) "التبديل إلى الوضع الداكن" else "Switch to Dark Mode"
                            },
                            tint = palette.accentCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Single Language pill: "🌐 عربي" / "🌐 EN"
                Row(
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .minimumInteractiveComponentSize()
                        .shadow(
                            elevation = if (palette.isDark) 10.dp else 4.dp,
                            shape = CircleShape,
                            ambientColor = palette.accentCyan.copy(alpha = 0.22f),
                            spotColor = palette.accentCyan.copy(alpha = 0.22f)
                        )
                        .clip(CircleShape)
                        .background(palette.topControlBg)
                        .border(1.dp, palette.topControlBorder, CircleShape)
                        .clickable { onToggleLanguage(if (isArabic) "en" else "ar") }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .testTag("auth_lang_toggle"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Language,
                        contentDescription = null,
                        tint = palette.accentCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isArabic) "EN" else "عربي",
                        color = palette.textPrimary,
                        fontFamily = VaultixRoundedFont,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (showForgotPasswordDialog) {
            ForgotPasswordDialog(
                isArabic = isArabic,
                palette = palette,
                initialEmail = loginEmail.trim(),
                onDismiss = { showForgotPasswordDialog = false },
                onSendReset = onSendPasswordReset
            )
        }
    }
}

@Composable
private fun WelcomeCardContent(
    isArabic: Boolean,
    palette: AuthVisualPalette,
    isLoading: Boolean,
    savedAccount: SavedAuthAccount?,
    errorMessage: String?,
    onRegisterClick: () -> Unit,
    onHaveAccountClick: () -> Unit,
    onGoogleSignIn: () -> Unit,
    onGuestModeClick: () -> Unit,
    onContinueSignedInGoogle: () -> Unit,
    onGoogleSignOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = if (isArabic) "مرحباً بك" else "Welcome",
            color = palette.textPrimary,
            fontFamily = VaultixRoundedFont,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(2.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isArabic) "في " else "To ",
                color = palette.textPrimary,
                fontFamily = VaultixRoundedFont,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$APP_NAME !",
                color = palette.brandOrange,
                fontFamily = VaultixScriptFont,
                fontSize = 31.sp
            )
        }

        if (savedAccount != null) {
            Spacer(modifier = Modifier.height(20.dp))
            SignedInQuickBanner(
                isArabic = isArabic,
                palette = palette,
                savedAccount = savedAccount,
                onContinue = onContinueSignedInGoogle,
                onSignOut = onGoogleSignOut
            )
        }

        Spacer(modifier = Modifier.height(34.dp))

        // Primary Glowing Pill Button 1: Register now !
        GlowingCyanButton(
            text = if (isArabic) "سجّل الآن !" else "Register now !",
            palette = palette,
            onClick = onRegisterClick,
            modifier = Modifier
                .fillMaxWidth(0.76f)
                .height(52.dp)
                .testTag("auth_welcome_register_button")
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Primary Glowing Pill Button 2: Have an account
        GlowingCyanButton(
            text = if (isArabic) "لدي حساب بالفعل" else "Have an account",
            palette = palette,
            onClick = onHaveAccountClick,
            modifier = Modifier
                .fillMaxWidth(0.76f)
                .height(52.dp)
                .testTag("auth_welcome_login_button")
        )

        Spacer(modifier = Modifier.height(32.dp))

        ContinueWithGooglePillButton(
            isArabic = isArabic,
            palette = palette,
            isLoading = isLoading,
            onClick = onGoogleSignIn,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("auth_google_signin_button")
        )

        Spacer(modifier = Modifier.height(14.dp))

        GuestModePillButton(
            isArabic = isArabic,
            palette = palette,
            onClick = onGuestModeClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("auth_guest_mode_button")
        )

        AuthInlineBanner(
            message = errorMessage,
            isError = true,
            palette = palette,
            testTag = "auth_welcome_error"
        )

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun GuestModeCardContent(
    isArabic: Boolean,
    palette: AuthVisualPalette,
    username: String,
    onUsernameChange: (String) -> Unit,
    validationError: String?,
    onBack: () -> Unit,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 28.dp)
    ) {
        Text(
            text = APP_NAME,
            color = palette.brandOrange,
            fontFamily = VaultixScriptFont,
            fontSize = 31.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(18.dp))

        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(68.dp)
                .clip(CircleShape)
                .background(palette.guestBg)
                .border(1.4.dp, palette.guestBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = null,
                tint = palette.accentCyan,
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        Text(
            text = if (isArabic) "وضع الضيف" else "Guest Mode",
            color = palette.textPrimary,
            fontFamily = VaultixRoundedFont,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (isArabic) {
                "أدخل اسم المستخدم للمتابعة إلى التطبيق"
            } else {
                "Enter your username to continue to the app"
            },
            color = palette.textMuted,
            fontFamily = VaultixRoundedFont,
            fontSize = 13.5.sp
        )

        Spacer(modifier = Modifier.height(22.dp))

        AuthPillTextField(
            value = username,
            onValueChange = onUsernameChange,
            placeholder = if (isArabic) "اسم المستخدم" else "Username",
            palette = palette,
            leadingIcon = Icons.Outlined.Person,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onConfirm() }),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("auth_guest_username")
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(palette.infoBg)
                .border(1.dp, palette.infoBorder, RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = if (isArabic) {
                    "تُحفظ خزنتك وسجلاتك المالية محلياً على هذا الجهاز، ويمكنك ربط حساب سحابي في أي وقت."
                } else {
                    "Your financial vault and records are saved locally on this device, and you can link an account anytime."
                },
                color = palette.textMuted,
                fontFamily = VaultixRoundedFont,
                fontSize = 12.5.sp,
                lineHeight = 18.sp
            )
        }

        AuthInlineBanner(
            message = validationError,
            isError = true,
            palette = palette,
            testTag = "auth_guest_error"
        )

        Spacer(modifier = Modifier.height(26.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onBack,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("auth_guest_back_button")
            ) {
                Text(
                    text = if (isArabic) "رجوع" else "Back",
                    color = palette.accentCyan,
                    fontFamily = VaultixRoundedFont,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            GlowingCyanButton(
                text = if (isArabic) "تأكيد والدخول" else "Confirm & Enter",
                palette = palette,
                onClick = onConfirm,
                modifier = Modifier
                    .height(48.dp)
                    .widthIn(min = 164.dp)
                    .testTag("auth_guest_confirm_button")
            )
        }
    }
}

@Composable
private fun RegisterCardContent(
    isArabic: Boolean,
    palette: AuthVisualPalette,
    isLoading: Boolean,
    firstName: String,
    onFirstNameChange: (String) -> Unit,
    lastName: String,
    onLastNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onTogglePasswordVisible: () -> Unit,
    confirmPasswordVisible: Boolean,
    onToggleConfirmPasswordVisible: () -> Unit,
    errorMessage: String?,
    onSwitchToLogin: () -> Unit,
    onSubmit: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 28.dp)
    ) {
        Text(
            text = APP_NAME,
            color = palette.brandOrange,
            fontFamily = VaultixScriptFont,
            fontSize = 31.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isArabic) "إنشاء حساب" else "Register",
            color = palette.textPrimary,
            fontFamily = VaultixRoundedFont,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(22.dp))

        AuthPillTextField(
            value = firstName,
            onValueChange = onFirstNameChange,
            placeholder = if (isArabic) "الاسم الأول" else "First name",
            palette = palette,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("auth_signup_first_name")
        )

        Spacer(modifier = Modifier.height(14.dp))

        AuthPillTextField(
            value = lastName,
            onValueChange = onLastNameChange,
            placeholder = if (isArabic) "اسم العائلة" else "Last name",
            palette = palette,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("auth_signup_last_name")
        )

        Spacer(modifier = Modifier.height(14.dp))

        AuthPillTextField(
            value = email,
            onValueChange = onEmailChange,
            placeholder = if (isArabic) "البريد الإلكتروني" else "Email address",
            palette = palette,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("auth_signup_email")
        )

        Spacer(modifier = Modifier.height(14.dp))

        AuthPillTextField(
            value = password,
            onValueChange = onPasswordChange,
            placeholder = if (isArabic) "كلمة السر" else "Password",
            palette = palette,
            isPassword = true,
            passwordVisible = passwordVisible,
            onTogglePasswordVisibility = onTogglePasswordVisible,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("auth_signup_password")
        )

        Spacer(modifier = Modifier.height(14.dp))

        AuthPillTextField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            placeholder = if (isArabic) "تأكيد كلمة السر" else "Confirm password",
            palette = palette,
            isPassword = true,
            passwordVisible = confirmPasswordVisible,
            onTogglePasswordVisibility = onToggleConfirmPasswordVisible,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { onSubmit() }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("auth_signup_confirm_password")
        )

        AuthInlineBanner(
            message = errorMessage,
            isError = true,
            palette = palette,
            testTag = "auth_signup_error"
        )

        Spacer(modifier = Modifier.height(26.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onSwitchToLogin,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("auth_switch_to_login")
            ) {
                Text(
                    text = if (isArabic) "تسجيل الدخول" else "Login",
                    color = palette.accentCyan,
                    fontFamily = VaultixRoundedFont,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            GlowingCyanButton(
                text = if (isArabic) "إنشاء حساب" else "Register",
                palette = palette,
                isLoading = isLoading,
                onClick = onSubmit,
                modifier = Modifier
                    .height(48.dp)
                    .widthIn(min = 148.dp)
                    .testTag("auth_signup_submit_button")
            )
        }
    }
}

@Composable
private fun LoginCardContent(
    isArabic: Boolean,
    palette: AuthVisualPalette,
    isLoading: Boolean,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onTogglePasswordVisible: () -> Unit,
    errorMessage: String?,
    onForgotPasswordClick: () -> Unit,
    onSwitchToRegister: () -> Unit,
    onSignInSubmit: () -> Unit,
    onGoogleSignIn: () -> Unit,
    onGuestModeClick: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 28.dp)
    ) {
        Text(
            text = APP_NAME,
            color = palette.brandOrange,
            fontFamily = VaultixScriptFont,
            fontSize = 31.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isArabic) "تسجيل الدخول" else "Login",
            color = palette.textPrimary,
            fontFamily = VaultixRoundedFont,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = if (isArabic) "سجّل الدخول للمتابعة" else "Sign in to continue",
            color = palette.textMuted,
            fontFamily = VaultixRoundedFont,
            fontSize = 13.5.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        AuthPillTextField(
            value = email,
            onValueChange = onEmailChange,
            placeholder = if (isArabic) "بريدك الإلكتروني" else "Your Email Address",
            palette = palette,
            leadingIcon = Icons.Outlined.Email,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("auth_login_email")
        )

        Spacer(modifier = Modifier.height(14.dp))

        AuthPillTextField(
            value = password,
            onValueChange = onPasswordChange,
            placeholder = if (isArabic) "كلمة السر" else "Password",
            palette = palette,
            leadingIcon = Icons.Outlined.Lock,
            isPassword = true,
            passwordVisible = passwordVisible,
            onTogglePasswordVisibility = onTogglePasswordVisible,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onSignInSubmit() }),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("auth_login_password")
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = if (isArabic) "نسيت كلمة السر؟" else "Forgot password?",
                color = palette.textMuted,
                fontFamily = VaultixRoundedFont,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onForgotPasswordClick)
                    .padding(horizontal = 6.dp, vertical = 10.dp)
                    .testTag("auth_forgot_password_button")
            )
        }

        AuthInlineBanner(
            message = errorMessage,
            isError = true,
            palette = palette,
            testTag = "auth_login_error"
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onSwitchToRegister,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("auth_switch_to_register")
            ) {
                Text(
                    text = if (isArabic) "إنشاء حساب" else "Register",
                    color = palette.accentCyan,
                    fontFamily = VaultixRoundedFont,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            GlowingCyanButton(
                text = if (isArabic) "دخول" else "Sign in",
                palette = palette,
                isLoading = isLoading,
                onClick = onSignInSubmit,
                modifier = Modifier
                    .height(48.dp)
                    .widthIn(min = 132.dp)
                    .testTag("auth_login_submit_button")
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        ContinueWithGooglePillButton(
            isArabic = isArabic,
            palette = palette,
            isLoading = isLoading,
            onClick = onGoogleSignIn,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("auth_login_google_button")
        )

        Spacer(modifier = Modifier.height(12.dp))

        GuestModePillButton(
            isArabic = isArabic,
            palette = palette,
            onClick = onGuestModeClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("auth_login_guest_button")
        )
    }
}

@Composable
private fun ForgotPasswordDialog(
    isArabic: Boolean,
    palette: AuthVisualPalette,
    initialEmail: String,
    onDismiss: () -> Unit,
    onSendReset: (email: String, onResult: (Boolean, String) -> Unit) -> Unit
) {
    var resetEmail by rememberSaveable(initialEmail) { mutableStateOf(initialEmail) }
    var isSending by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }
    val isScreenshotProtected = LocalScreenshotProtection.current

    Dialog(
        onDismissRequest = { if (!isSending) onDismiss() },
        properties = DialogProperties(
            securePolicy = if (isScreenshotProtected) SecureFlagPolicy.SecureOn else SecureFlagPolicy.SecureOff
        )
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = palette.dialogSurface,
            border = BorderStroke(1.dp, palette.cardBorder),
            shadowElevation = 20.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                Text(
                    text = if (isArabic) "استرجاع كلمة السر" else "Reset Password",
                    color = palette.textPrimary,
                    fontFamily = VaultixRoundedFont,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isArabic) {
                        "أدخل بريدك الإلكتروني وسنرسل لك رابطاً لإعادة تعيين كلمة السر."
                    } else {
                        "Enter your email address and we'll send you a password reset link."
                    },
                    color = palette.textMuted,
                    fontFamily = VaultixRoundedFont,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                AuthPillTextField(
                    value = resetEmail,
                    onValueChange = {
                        resetEmail = it
                        feedbackMessage = null
                    },
                    placeholder = if (isArabic) "بريدك الإلكتروني" else "Your Email Address",
                    palette = palette,
                    leadingIcon = Icons.Outlined.Email,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                AuthInlineBanner(
                    message = feedbackMessage,
                    isError = !isSuccess,
                    palette = palette,
                    testTag = "auth_forgot_password_feedback"
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isSending,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(
                            text = if (isArabic) "إغلاق" else "Close",
                            color = palette.textMuted,
                            fontFamily = VaultixRoundedFont,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    GlowingCyanButton(
                        text = if (isArabic) "إرسال الرابط" else "Send Link",
                        palette = palette,
                        isLoading = isSending,
                        onClick = {
                            val clean = resetEmail.trim()
                            if (clean.isEmpty() || !isValidAuthEmail(clean)) {
                                isSuccess = false
                                feedbackMessage = if (isArabic) {
                                    "يرجى إدخال بريد إلكتروني صحيح"
                                } else {
                                    "Please enter a valid email address"
                                }
                            } else {
                                isSending = true
                                feedbackMessage = null
                                onSendReset(clean) { ok, msg ->
                                    isSending = false
                                    isSuccess = ok
                                    feedbackMessage = msg
                                }
                            }
                        },
                        modifier = Modifier.height(48.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SignedInQuickBanner(
    isArabic: Boolean,
    palette: AuthVisualPalette,
    savedAccount: SavedAuthAccount,
    onContinue: () -> Unit,
    onSignOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(palette.infoBg)
            .border(1.dp, palette.successGreen.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(palette.accentCyan),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = savedAccount.displayName.take(1).uppercase(),
                    color = palette.onAccentText,
                    fontFamily = VaultixRoundedFont,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = savedAccount.displayName,
                    color = palette.textPrimary,
                    fontFamily = VaultixRoundedFont,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (savedAccount.email.isNotBlank()) {
                    Text(
                        text = savedAccount.email,
                        color = palette.textMuted,
                        fontFamily = VaultixRoundedFont,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(
                onClick = onSignOut,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("auth_google_signout_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = if (isArabic) "تبديل الحساب" else "Switch account",
                    tint = palette.textMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Button(
            onClick = onContinue,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = palette.successGreen,
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("auth_continue_google_button")
        ) {
            Text(
                text = if (isArabic) "المتابعة إلى $APP_NAME" else "Continue to $APP_NAME",
                fontFamily = VaultixRoundedFont,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun GlowingCyanButton(
    text: String,
    palette: AuthVisualPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            contentColor = palette.onAccentText,
            disabledContentColor = palette.onAccentText.copy(alpha = 0.7f)
        ),
        contentPadding = PaddingValues(0.dp),
        modifier = modifier
            .shadow(
                elevation = 16.dp,
                shape = CircleShape,
                ambientColor = palette.accentCyan,
                spotColor = palette.accentCyan
            )
            .clip(CircleShape)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        palette.accentCyan,
                        palette.accentCyanDeep
                    )
                )
            )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = palette.onAccentText,
                strokeWidth = 2.2.dp,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Text(
                text = text,
                color = palette.onAccentText,
                fontFamily = VaultixRoundedFont,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
    }
}

@Composable
private fun ContinueWithGooglePillButton(
    isArabic: Boolean,
    palette: AuthVisualPalette,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        enabled = !isLoading,
        shape = CircleShape,
        border = BorderStroke(1.dp, palette.googleBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = palette.googleBg,
            contentColor = palette.textPrimary
        ),
        contentPadding = PaddingValues(horizontal = 18.dp),
        modifier = modifier
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = palette.accentCyan,
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp)
            )
        } else {
            GoogleColorLogo(modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = if (isArabic) "المتابعة عبر Google" else "Continue with Google",
            color = palette.textPrimary,
            fontFamily = VaultixRoundedFont,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun GuestModePillButton(
    isArabic: Boolean,
    palette: AuthVisualPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        shape = CircleShape,
        border = BorderStroke(1.dp, palette.guestBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = palette.guestBg,
            contentColor = palette.accentCyan
        ),
        contentPadding = PaddingValues(horizontal = 18.dp),
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Outlined.Person,
            contentDescription = null,
            tint = palette.accentCyan,
            modifier = Modifier.size(19.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = if (isArabic) "وضع الضيف" else "Guest Mode",
            color = palette.accentCyan,
            fontFamily = VaultixRoundedFont,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun GoogleColorLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.22f
        val inset = strokeWidth * 0.5f
        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
        val topLeft = Offset(inset, inset)

        // Red (top-left to top-right)
        drawArc(
            color = Color(0xFFEA4335),
            startAngle = -148f,
            sweepAngle = 104f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        // Yellow (left)
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 148f,
            sweepAngle = 66f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        // Green (bottom)
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 38f,
            sweepAngle = 112f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        // Blue (right bottom curve)
        drawArc(
            color = Color(0xFF4285F4),
            startAngle = -12f,
            sweepAngle = 54f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        // Blue horizontal bar
        drawLine(
            color = Color(0xFF4285F4),
            start = Offset(size.width * 0.50f, size.height * 0.50f),
            end = Offset(size.width - inset * 0.6f, size.height * 0.50f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Butt
        )
    }
}

@Composable
private fun AuthPillTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    palette: AuthVisualPalette,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePasswordVisibility: (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) palette.accentCyan else palette.fieldBorder,
        animationSpec = tween(200),
        label = "fieldBorderColor"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isFocused) palette.fieldFocusedBg else palette.fieldBg,
        animationSpec = tween(200),
        label = "fieldContainerColor"
    )

    Row(
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(CircleShape)
            .background(containerColor)
            .border(if (isFocused) 1.4.dp else 1.dp, borderColor, CircleShape)
            .padding(
                start = if (leadingIcon != null) 16.dp else 20.dp,
                end = if (isPassword && onTogglePasswordVisibility != null) 4.dp else 20.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = if (isFocused) palette.accentCyan else palette.textMuted,
                modifier = Modifier.size(19.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = palette.textMuted,
                    fontFamily = VaultixRoundedFont,
                    fontSize = 14.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = palette.textPrimary,
                    fontFamily = VaultixRoundedFont,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(palette.accentCyan),
                visualTransformation = if (isPassword && !passwordVisible) {
                    PasswordVisualTransformation()
                } else {
                    VisualTransformation.None
                },
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isFocused = it.isFocused }
            )
        }

        if (isPassword && onTogglePasswordVisibility != null) {
            IconButton(
                onClick = onTogglePasswordVisibility,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                    tint = palette.textMuted,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

@Composable
private fun AuthInlineBanner(
    message: String?,
    isError: Boolean,
    palette: AuthVisualPalette,
    testTag: String
) {
    AnimatedVisibility(
        visible = !message.isNullOrBlank(),
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(150))
    ) {
        if (!message.isNullOrBlank()) {
            val tone = if (isError) palette.errorRed else palette.successGreen
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(tone.copy(alpha = 0.12f))
                    .border(1.dp, tone.copy(alpha = 0.36f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag(testTag),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = tone,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = message,
                    color = tone,
                    fontFamily = VaultixRoundedFont,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 17.sp
                )
            }
        }
    }
}
