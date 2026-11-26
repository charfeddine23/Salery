package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
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
import com.example.cloud.GoogleAuthUiState
import com.example.ui.components.VaultixLogoContainer

private val AuthCreamBg = Color(0xFFFCF4E8)
private val AuthPastelBlob = Color(0xFFF9DFDC)
private val AuthIndigoTint = Color(0xFFE8ECFA)
private val AuthRoyalIndigo = Color(0xFF334194)
private val AuthRoyalDeep = Color(0xFF1E296B)
private val AuthInkDark = Color(0xFF0F172A)
private val AuthInkMuted = Color(0xFF475569)
private val AuthErrorRed = Color(0xFFB91C1C)
private val AuthEmerald = Color(0xFF047857)
private val AuthWarmAmber = Color(0xFFB45309)

private val AUTH_EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

private fun isValidAuthEmail(email: String): Boolean = AUTH_EMAIL_REGEX.matches(email.trim())

private enum class AuthPageMode {
    LOGIN,
    SIGN_UP
}

@Composable
fun AuthWelcomeScreen(
    isArabic: Boolean,
    googleAuthState: GoogleAuthUiState = GoogleAuthUiState.SignedOut,
    onToggleLanguage: (String) -> Unit,
    onGoogleSignIn: () -> Unit = {},
    onEmailSignIn: (email: String, password: String) -> Unit = { _, _ -> },
    onEmailSignUp: (firstName: String, lastName: String, username: String, email: String, password: String) -> Unit = { _, _, _, _, _ -> },
    onClearAuthError: () -> Unit = {},
    onContinueSignedInGoogle: () -> Unit = {},
    onGoogleSignOut: () -> Unit = {},
    onCreateLocalAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLoading = googleAuthState is GoogleAuthUiState.Loading
    val signedInState = googleAuthState as? GoogleAuthUiState.SignedIn
    val errorState = googleAuthState as? GoogleAuthUiState.Error

    var pageMode by rememberSaveable { mutableStateOf(AuthPageMode.LOGIN) }

    // Login inputs
    var loginEmail by rememberSaveable { mutableStateOf("") }
    var loginPassword by rememberSaveable { mutableStateOf("") }
    var loginPasswordVisible by rememberSaveable { mutableStateOf(false) }

    // Sign-Up inputs (5 exact fields requested)
    var firstName by rememberSaveable { mutableStateOf("") }
    var lastName by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var signUpEmail by rememberSaveable { mutableStateOf("") }
    var signUpPassword by rememberSaveable { mutableStateOf("") }
    var signUpPasswordVisible by rememberSaveable { mutableStateOf(false) }

    var localValidationError by rememberSaveable { mutableStateOf<String?>(null) }
    var redirectedFromMissingAccount by rememberSaveable { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    // If login fails because email isn't registered yet, cleanly transfer user to Sign-Up with email & password prefilled
    LaunchedEffect(errorState) {
        if (errorState != null && errorState.accountNotFound && pageMode == AuthPageMode.LOGIN) {
            if (loginEmail.isNotBlank()) {
                signUpEmail = loginEmail.trim()
            }
            if (loginPassword.isNotBlank() && signUpPassword.isBlank()) {
                signUpPassword = loginPassword
            }
            redirectedFromMissingAccount = true
            localValidationError = null
            onClearAuthError()
            pageMode = AuthPageMode.SIGN_UP
        }
    }

    val switchToSignUp: () -> Unit = {
        focusManager.clearFocus()
        localValidationError = null
        redirectedFromMissingAccount = false
        onClearAuthError()
        if (loginEmail.isNotBlank()) {
            signUpEmail = loginEmail.trim()
        }
        if (loginPassword.isNotBlank() && signUpPassword.isBlank()) {
            signUpPassword = loginPassword
        }
        pageMode = AuthPageMode.SIGN_UP
    }

    val switchToLogin: () -> Unit = {
        focusManager.clearFocus()
        localValidationError = null
        redirectedFromMissingAccount = false
        onClearAuthError()
        if (signUpEmail.isNotBlank() && loginEmail.isBlank()) {
            loginEmail = signUpEmail.trim()
        }
        pageMode = AuthPageMode.LOGIN
    }

    BackHandler(enabled = pageMode == AuthPageMode.SIGN_UP) {
        switchToLogin()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AuthCreamBg)
    ) {
        // Editorial curved organic blobs & atmospheric highlight
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val topBlob = Path().apply {
                moveTo(0f, 0f)
                lineTo(w * 0.72f, 0f)
                cubicTo(
                    w * 0.80f, h * 0.13f,
                    w * 0.62f, h * 0.23f,
                    w * 0.42f, h * 0.25f
                )
                cubicTo(
                    w * 0.22f, h * 0.27f,
                    w * 0.16f, h * 0.36f,
                    0f, h * 0.40f
                )
                close()
            }
            drawPath(path = topBlob, color = AuthPastelBlob)

            drawCircle(
                color = AuthIndigoTint.copy(alpha = 0.65f),
                radius = w * 0.32f,
                center = Offset(w * 0.92f, h * 0.22f)
            )

            val bottomBlob = Path().apply {
                moveTo(w, h)
                lineTo(w * 0.28f, h)
                cubicTo(
                    w * 0.22f, h * 0.87f,
                    w * 0.38f, h * 0.78f,
                    w * 0.58f, h * 0.75f
                )
                cubicTo(
                    w * 0.78f, h * 0.72f,
                    w * 0.88f, h * 0.62f,
                    w, h * 0.58f
                )
                close()
            }
            drawPath(path = bottomBlob, color = AuthPastelBlob)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar: optional Back button when on Sign-Up + Language toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pageMode == AuthPageMode.SIGN_UP) {
                    Surface(
                        onClick = switchToLogin,
                        enabled = !isLoading,
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.86f),
                        border = BorderStroke(1.dp, AuthRoyalIndigo.copy(alpha = 0.20f)),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("auth_signup_top_back_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = if (isArabic) "العودة لتسجيل الدخول" else "Back to Login",
                                tint = AuthInkDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.84f))
                        .border(1.dp, AuthRoyalIndigo.copy(alpha = 0.22f), RoundedCornerShape(50))
                        .padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AuthLangChip(
                        label = "العربية",
                        selected = isArabic,
                        onClick = { onToggleLanguage("ar") },
                        tag = "auth_lang_ar"
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    AuthLangChip(
                        label = "EN",
                        selected = !isArabic,
                        onClick = { onToggleLanguage("en") },
                        tag = "auth_lang_en"
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                VaultixLogoContainer(
                    size = if (pageMode == AuthPageMode.LOGIN) 78.dp else 64.dp,
                    cornerRadius = 22.dp,
                    padding = 9.dp,
                    forceObsidianFrame = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = when (pageMode) {
                        AuthPageMode.LOGIN -> if (isArabic) "تسجيل الدخول إلى Vaultix" else "Sign in to Vaultix"
                        AuthPageMode.SIGN_UP -> if (isArabic) "إنشاء حساب جديد" else "Create Your Account"
                    },
                    color = AuthInkDark,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = when (pageMode) {
                        AuthPageMode.LOGIN -> if (isArabic) {
                            "تتم مزامنة الدخل، الإنفاق، الديون، الادخار، الأصول والإعدادات سحابياً بالكامل"
                        } else {
                            "Cloud sync across Income, Expenses, Debts, Savings, Assets & all Settings"
                        }
                        AuthPageMode.SIGN_UP -> if (isArabic) {
                            "أدخل بياناتك لحفظ محفظتك وإعداداتك سحابياً ومزامنتها عبر أجهزتك"
                        } else {
                            "Complete your details to store and sync everything securely in the cloud"
                        }
                    },
                    color = AuthInkMuted,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Clear 2-tab mode switcher at the top so users can jump straight to creating an account with any new email
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.88f))
                        .border(1.dp, AuthRoyalIndigo.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val isLoginTab = pageMode == AuthPageMode.LOGIN
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isLoginTab) AuthRoyalIndigo else Color.Transparent)
                            .clickable(enabled = !isLoading) {
                                if (!isLoginTab) switchToLogin()
                            }
                            .testTag("auth_tab_login"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isArabic) "تسجيل الدخول" else "Sign In",
                            color = if (isLoginTab) Color.White else AuthInkDark,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (!isLoginTab) AuthRoyalDeep else Color.Transparent)
                            .clickable(enabled = !isLoading) {
                                if (isLoginTab) switchToSignUp()
                            }
                            .testTag("auth_tab_signup"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isArabic) "إنشاء حساب جديد" else "Create Account",
                            color = if (!isLoginTab) Color.White else AuthInkDark,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                val displayedError = localValidationError ?: errorState?.message

                AnimatedContent(
                    targetState = pageMode,
                    transitionSpec = {
                        val direction = if (targetState == AuthPageMode.SIGN_UP) 1 else -1
                        (slideInHorizontally(animationSpec = tween(260)) { it / 3 * direction } +
                            fadeIn(animationSpec = tween(240))) togetherWith
                            (slideOutHorizontally(animationSpec = tween(220)) { -it / 3 * direction } +
                                fadeOut(animationSpec = tween(180)))
                    },
                    label = "auth_page_transition"
                ) { mode ->
                    when (mode) {
                        AuthPageMode.LOGIN -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Already-signed-in Firebase Session Card (if returning user)
                                if (signedInState != null) {
                                    SignedInSessionCard(
                                        isArabic = isArabic,
                                        signedInState = signedInState,
                                        onContinue = onContinueSignedInGoogle,
                                        onSignOut = onGoogleSignOut
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                }

                                // Main Cloud Login Card (Google Sign-In + Email/Password + Go to Sign-Up)
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(22.dp),
                                    color = Color.White.copy(alpha = 0.92f),
                                    border = BorderStroke(1.dp, AuthRoyalIndigo.copy(alpha = 0.16f)),
                                    shadowElevation = 6.dp
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(18.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        // Prominent Google Sign-In Button
                                        OutlinedButton(
                                            onClick = {
                                                localValidationError = null
                                                onGoogleSignIn()
                                            },
                                            enabled = !isLoading,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(52.dp)
                                                .testTag("auth_google_signin_button"),
                                            shape = RoundedCornerShape(14.dp),
                                            border = BorderStroke(1.3.dp, AuthRoyalIndigo.copy(alpha = 0.35f)),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = AuthIndigoTint.copy(alpha = 0.45f),
                                                contentColor = AuthInkDark
                                            ),
                                            contentPadding = PaddingValues(horizontal = 16.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(26.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White)
                                                    .border(1.dp, AuthRoyalIndigo.copy(alpha = 0.18f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "G",
                                                    color = AuthRoyalIndigo,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = if (isArabic) "تسجيل الدخول بواسطة Google" else "Sign in with Google",
                                                fontSize = 14.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            HorizontalDivider(
                                                modifier = Modifier.weight(1f),
                                                color = AuthInkMuted.copy(alpha = 0.22f)
                                            )
                                            Text(
                                                text = if (isArabic) "أو عبر البريد الإلكتروني" else "or with email",
                                                color = AuthInkMuted,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 10.dp)
                                            )
                                            HorizontalDivider(
                                                modifier = Modifier.weight(1f),
                                                color = AuthInkMuted.copy(alpha = 0.22f)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))

                                        AuthOutlinedField(
                                            value = loginEmail,
                                            onValueChange = {
                                                loginEmail = it
                                                localValidationError = null
                                                onClearAuthError()
                                            },
                                            label = if (isArabic) "البريد الإلكتروني" else "Email Address",
                                            leadingIcon = Icons.Default.Email,
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Email,
                                                imeAction = ImeAction.Next
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("auth_login_email")
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))

                                        AuthOutlinedField(
                                            value = loginPassword,
                                            onValueChange = {
                                                loginPassword = it
                                                localValidationError = null
                                                onClearAuthError()
                                            },
                                            label = if (isArabic) "كلمة السر" else "Password",
                                            leadingIcon = Icons.Default.Lock,
                                            isPassword = true,
                                            passwordVisible = loginPasswordVisible,
                                            onTogglePasswordVisibility = {
                                                loginPasswordVisible = !loginPasswordVisible
                                            },
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Password,
                                                imeAction = ImeAction.Done
                                            ),
                                            keyboardActions = KeyboardActions(
                                                onDone = {
                                                    focusManager.clearFocus()
                                                    val cleanEmail = loginEmail.trim()
                                                    if (!isValidAuthEmail(cleanEmail)) {
                                                        localValidationError = if (isArabic) {
                                                            "يرجى إدخال بريد إلكتروني صحيح"
                                                        } else {
                                                            "Please enter a valid email address"
                                                        }
                                                    } else if (loginPassword.isBlank()) {
                                                        localValidationError = if (isArabic) {
                                                            "يرجى إدخال كلمة السر"
                                                        } else {
                                                            "Please enter your password"
                                                        }
                                                    } else {
                                                        onEmailSignIn(cleanEmail, loginPassword)
                                                    }
                                                }
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("auth_login_password")
                                        )

                                        AuthErrorBanner(
                                            message = displayedError,
                                            testTag = "auth_google_error_banner"
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        Button(
                                            onClick = {
                                                focusManager.clearFocus()
                                                val cleanEmail = loginEmail.trim()
                                                if (!isValidAuthEmail(cleanEmail)) {
                                                    localValidationError = if (isArabic) {
                                                        "يرجى إدخال بريد إلكتروني صحيح"
                                                    } else {
                                                        "Please enter a valid email address"
                                                    }
                                                } else if (loginPassword.isBlank()) {
                                                    localValidationError = if (isArabic) {
                                                        "يرجى إدخال كلمة السر"
                                                    } else {
                                                        "Please enter your password"
                                                    }
                                                } else {
                                                    localValidationError = null
                                                    onEmailSignIn(cleanEmail, loginPassword)
                                                }
                                            },
                                            enabled = !isLoading,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(52.dp)
                                                .testTag("auth_login_submit_button"),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = AuthRoyalIndigo,
                                                contentColor = Color.White,
                                                disabledContainerColor = AuthRoyalIndigo.copy(alpha = 0.72f),
                                                disabledContentColor = Color.White
                                            ),
                                            elevation = ButtonDefaults.buttonElevation(
                                                defaultElevation = 3.dp,
                                                pressedElevation = 1.dp
                                            )
                                        ) {
                                            if (isLoading) {
                                                CircularProgressIndicator(
                                                    color = Color.White,
                                                    strokeWidth = 2.2.dp,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = if (isArabic) "جاري التحقق..." else "Signing in...",
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            } else {
                                                Text(
                                                    text = if (isArabic) "تسجيل الدخول" else "Sign In",
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))

                                        // Explicit Create Account CTA for users who don't have an account
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(14.dp),
                                            color = AuthCreamBg.copy(alpha = 0.85f),
                                            border = BorderStroke(1.dp, AuthRoyalIndigo.copy(alpha = 0.16f))
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = if (isArabic) "ليس لديك حساب؟" else "Don't have an account?",
                                                    color = AuthInkMuted,
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                OutlinedButton(
                                                    onClick = switchToSignUp,
                                                    enabled = !isLoading,
                                                    modifier = Modifier
                                                        .height(48.dp)
                                                        .testTag("auth_go_to_signup_button"),
                                                    shape = RoundedCornerShape(12.dp),
                                                    border = BorderStroke(1.2.dp, AuthRoyalIndigo),
                                                    colors = ButtonDefaults.outlinedButtonColors(
                                                        containerColor = Color.White,
                                                        contentColor = AuthRoyalIndigo
                                                    ),
                                                    contentPadding = PaddingValues(horizontal = 14.dp)
                                                ) {
                                                    Text(
                                                        text = if (isArabic) "إنشاء حساب" else "Create Account",
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.ExtraBold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        AuthPageMode.SIGN_UP -> {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(22.dp),
                                color = Color.White.copy(alpha = 0.94f),
                                border = BorderStroke(1.dp, AuthRoyalIndigo.copy(alpha = 0.16f)),
                                shadowElevation = 6.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    AnimatedVisibility(visible = redirectedFromMissingAccount) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 12.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(AuthWarmAmber.copy(alpha = 0.10f))
                                                .border(1.dp, AuthWarmAmber.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                                .padding(11.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CloudDone,
                                                contentDescription = null,
                                                tint = AuthWarmAmber,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (isArabic) {
                                                    "هذا البريد غير مسجّل بعد — يمكنك فتح حساب جديد بأي بريد إلكتروني بإكمال الاسم واللقب واسم المستخدم"
                                                } else {
                                                    "This email is new — create a new account with any email by completing your name and username below"
                                                },
                                                color = AuthInkDark,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }

                                    // 1. الاسم (First Name) & 2. اللقب (Last Name)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        AuthOutlinedField(
                                            value = firstName,
                                            onValueChange = {
                                                firstName = it
                                                localValidationError = null
                                                onClearAuthError()
                                            },
                                            label = if (isArabic) "الاسم" else "First Name",
                                            leadingIcon = Icons.Default.Person,
                                            keyboardOptions = KeyboardOptions(
                                                capitalization = KeyboardCapitalization.Words,
                                                imeAction = ImeAction.Next
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("auth_signup_first_name")
                                        )

                                        AuthOutlinedField(
                                            value = lastName,
                                            onValueChange = {
                                                lastName = it
                                                localValidationError = null
                                                onClearAuthError()
                                            },
                                            label = if (isArabic) "اللقب" else "Last Name",
                                            leadingIcon = Icons.Default.Badge,
                                            keyboardOptions = KeyboardOptions(
                                                capitalization = KeyboardCapitalization.Words,
                                                imeAction = ImeAction.Next
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("auth_signup_last_name")
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // 3. اسم المستخدم (Username)
                                    AuthOutlinedField(
                                        value = username,
                                        onValueChange = {
                                            username = it
                                            localValidationError = null
                                            onClearAuthError()
                                        },
                                        label = if (isArabic) "اسم المستخدم" else "Username",
                                        leadingIcon = Icons.Default.AlternateEmail,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Text,
                                            imeAction = ImeAction.Next
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("auth_signup_username")
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // 4. البريد الإلكتروني (Email)
                                    AuthOutlinedField(
                                        value = signUpEmail,
                                        onValueChange = {
                                            signUpEmail = it
                                            localValidationError = null
                                            onClearAuthError()
                                        },
                                        label = if (isArabic) "البريد الإلكتروني" else "Email Address",
                                        leadingIcon = Icons.Default.Email,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Email,
                                            imeAction = ImeAction.Next
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("auth_signup_email")
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // 5. كلمة السر (Password)
                                    AuthOutlinedField(
                                        value = signUpPassword,
                                        onValueChange = {
                                            signUpPassword = it
                                            localValidationError = null
                                            onClearAuthError()
                                        },
                                        label = if (isArabic) "كلمة السر (6 أحرف فأكثر)" else "Password (min 6 chars)",
                                        leadingIcon = Icons.Default.Lock,
                                        isPassword = true,
                                        passwordVisible = signUpPasswordVisible,
                                        onTogglePasswordVisibility = {
                                            signUpPasswordVisible = !signUpPasswordVisible
                                        },
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Password,
                                            imeAction = ImeAction.Done
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onDone = {
                                                focusManager.clearFocus()
                                                val cleanFirst = firstName.trim()
                                                val cleanLast = lastName.trim()
                                                val cleanUser = username.trim()
                                                val cleanEmail = signUpEmail.trim()
                                                when {
                                                    cleanFirst.isEmpty() -> {
                                                        localValidationError = if (isArabic) "يرجى إدخال الاسم" else "Please enter your first name"
                                                    }
                                                    cleanLast.isEmpty() -> {
                                                        localValidationError = if (isArabic) "يرجى إدخال اللقب" else "Please enter your last name"
                                                    }
                                                    cleanUser.isEmpty() -> {
                                                        localValidationError = if (isArabic) "يرجى إدخال اسم المستخدم" else "Please enter a username"
                                                    }
                                                    !isValidAuthEmail(cleanEmail) -> {
                                                        localValidationError = if (isArabic) "يرجى إدخال بريد إلكتروني صحيح" else "Please enter a valid email address"
                                                    }
                                                    signUpPassword.length < 6 -> {
                                                        localValidationError = if (isArabic) "كلمة السر يجب أن تكون 6 أحرف على الأقل" else "Password must be at least 6 characters"
                                                    }
                                                    else -> {
                                                        localValidationError = null
                                                        onEmailSignUp(cleanFirst, cleanLast, cleanUser, cleanEmail, signUpPassword)
                                                    }
                                                }
                                            }
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("auth_signup_password")
                                    )

                                    AuthErrorBanner(
                                        message = displayedError,
                                        testTag = "auth_signup_error_banner"
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = {
                                            focusManager.clearFocus()
                                            val cleanFirst = firstName.trim()
                                            val cleanLast = lastName.trim()
                                            val cleanUser = username.trim()
                                            val cleanEmail = signUpEmail.trim()
                                            when {
                                                cleanFirst.isEmpty() -> {
                                                    localValidationError = if (isArabic) "يرجى إدخال الاسم" else "Please enter your first name"
                                                }
                                                cleanLast.isEmpty() -> {
                                                    localValidationError = if (isArabic) "يرجى إدخال اللقب" else "Please enter your last name"
                                                }
                                                cleanUser.isEmpty() -> {
                                                    localValidationError = if (isArabic) "يرجى إدخال اسم المستخدم" else "Please enter a username"
                                                }
                                                !isValidAuthEmail(cleanEmail) -> {
                                                    localValidationError = if (isArabic) "يرجى إدخال بريد إلكتروني صحيح" else "Please enter a valid email address"
                                                }
                                                signUpPassword.length < 6 -> {
                                                    localValidationError = if (isArabic) "كلمة السر يجب أن تكون 6 أحرف على الأقل" else "Password must be at least 6 characters"
                                                }
                                                else -> {
                                                    localValidationError = null
                                                    onEmailSignUp(cleanFirst, cleanLast, cleanUser, cleanEmail, signUpPassword)
                                                }
                                            }
                                        },
                                        enabled = !isLoading,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .testTag("auth_signup_submit_button"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = AuthRoyalDeep,
                                            contentColor = Color.White,
                                            disabledContainerColor = AuthRoyalDeep.copy(alpha = 0.72f),
                                            disabledContentColor = Color.White
                                        ),
                                        elevation = ButtonDefaults.buttonElevation(
                                            defaultElevation = 3.dp,
                                            pressedElevation = 1.dp
                                        )
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(
                                                color = Color.White,
                                                strokeWidth = 2.2.dp,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = if (isArabic) "جاري إنشاء الحساب..." else "Creating account...",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        } else {
                                            Text(
                                                text = if (isArabic) "إنشاء حساب" else "Create Account",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    TextButton(
                                        onClick = switchToLogin,
                                        enabled = !isLoading,
                                        modifier = Modifier
                                            .height(48.dp)
                                            .testTag("auth_back_to_login_button")
                                    ) {
                                        Text(
                                            text = if (isArabic) {
                                                "لديك حساب بالفعل؟ العودة لتسجيل الدخول"
                                            } else {
                                                "Already have an account? Back to Sign In"
                                            },
                                            color = AuthRoyalIndigo,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        thickness = 1.dp,
                        color = AuthInkDark.copy(alpha = 0.25f)
                    )
                    Text(
                        text = if (isArabic) "أو" else "OR",
                        color = AuthInkDark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        thickness = 1.dp,
                        color = AuthInkDark.copy(alpha = 0.25f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Offline-First Local Account Button ("وفي الاسفل اضف زر انشاء حساب محلي")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(14.dp),
                            spotColor = AuthInkDark.copy(alpha = 0.25f)
                        )
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                            )
                        )
                        .border(
                            width = 1.2.dp,
                            color = Color(0xFF38BDF8).copy(alpha = 0.45f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable(enabled = !isLoading, onClick = onCreateLocalAccount)
                        .padding(horizontal = 18.dp)
                        .testTag("auth_create_local_account_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isArabic) "إنشاء حساب محلي" else "Create Local Account",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = AuthEmerald,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isArabic) {
                            "يعمل 100% دون إنترنت مع تشفير عتادي AES-256-GCM"
                        } else {
                            "Works 100% offline with hardware-backed AES-256-GCM encryption"
                        },
                        color = AuthInkMuted,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun SignedInSessionCard(
    isArabic: Boolean,
    signedInState: GoogleAuthUiState.SignedIn,
    onContinue: () -> Unit,
    onSignOut: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.94f),
        border = BorderStroke(1.2.dp, AuthEmerald.copy(alpha = 0.40f)),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(AuthEmerald.copy(alpha = 0.10f))
                    .border(1.dp, AuthEmerald.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AuthRoyalIndigo),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = signedInState.displayName.take(1).uppercase(),
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = signedInState.displayName,
                        color = AuthInkDark,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (signedInState.email.isNotBlank()) {
                        Text(
                            text = signedInState.email,
                            color = AuthInkMuted,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = AuthEmerald,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("auth_continue_google_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AuthEmerald,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = if (isArabic) "المتابعة إلى Vaultix" else "Continue to Vaultix",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("auth_google_signout_button"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, AuthInkMuted.copy(alpha = 0.35f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = AuthInkDark
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isArabic) "تبديل الحساب السحابي" else "Switch Cloud Account",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun AuthOutlinedField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePasswordVisibility: (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        label = {
            Text(
                text = label,
                fontSize = 12.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = AuthRoyalIndigo,
                modifier = Modifier.size(19.dp)
            )
        },
        trailingIcon = if (isPassword && onTogglePasswordVisibility != null) {
            {
                IconButton(
                    onClick = onTogglePasswordVisibility,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                        tint = AuthInkMuted,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !passwordVisible) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AuthRoyalIndigo,
            unfocusedBorderColor = AuthInkMuted.copy(alpha = 0.30f),
            focusedLabelColor = AuthRoyalIndigo,
            unfocusedLabelColor = AuthInkMuted,
            focusedTextColor = AuthInkDark,
            unfocusedTextColor = AuthInkDark,
            cursorColor = AuthRoyalIndigo,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = AuthCreamBg.copy(alpha = 0.45f)
        ),
        modifier = modifier
    )
}

@Composable
private fun AuthErrorBanner(
    message: String?,
    testTag: String
) {
    AnimatedVisibility(
        visible = !message.isNullOrBlank(),
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        if (!message.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AuthErrorRed.copy(alpha = 0.08f))
                    .border(1.dp, AuthErrorRed.copy(alpha = 0.28f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
                    .testTag(testTag),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = AuthErrorRed,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = message,
                    color = AuthErrorRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun AuthLangChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) AuthRoyalIndigo else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else AuthInkDark.copy(alpha = 0.72f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
