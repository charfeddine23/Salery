package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.VaultixLogoContainer
import kotlinx.coroutines.delay

private val AuthCreamBg = Color(0xFFFCF4E8)
private val AuthPastelBlob = Color(0xFFF9DFDC)
private val AuthRoyalIndigo = Color(0xFF334194)
private val AuthFieldGray = Color(0xFFD6D7DA)
private val AuthPlaceholderGray = Color(0xFF7B7F87)
private val AuthInkDark = Color(0xFF0F172A)

@Composable
fun AuthWelcomeScreen(
    isArabic: Boolean,
    onToggleLanguage: (String) -> Unit,
    onCreateLocalAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isSignUpMode by remember { mutableStateOf(false) }
    var soonTriggerCount by remember { mutableIntStateOf(0) }
    var showSoonPill by remember { mutableStateOf(false) }

    BackHandler(enabled = isSignUpMode) {
        isSignUpMode = false
    }

    LaunchedEffect(soonTriggerCount) {
        if (soonTriggerCount > 0) {
            showSoonPill = true
            delay(1800L)
            showSoonPill = false
        }
    }

    val triggerSoon = {
        soonTriggerCount += 1
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AuthCreamBg)
    ) {
        // Soft pastel organic curved blobs matching the reference illustration
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val topBlob = Path().apply {
                moveTo(0f, 0f)
                lineTo(w * 0.68f, 0f)
                cubicTo(
                    w * 0.78f, h * 0.14f,
                    w * 0.62f, h * 0.25f,
                    w * 0.44f, h * 0.27f
                )
                cubicTo(
                    w * 0.26f, h * 0.29f,
                    w * 0.20f, h * 0.40f,
                    0f, h * 0.44f
                )
                close()
            }
            drawPath(path = topBlob, color = AuthPastelBlob)

            val bottomBlob = Path().apply {
                moveTo(w, h)
                lineTo(w * 0.34f, h)
                cubicTo(
                    w * 0.28f, h * 0.86f,
                    w * 0.42f, h * 0.76f,
                    w * 0.62f, h * 0.72f
                )
                cubicTo(
                    w * 0.80f, h * 0.68f,
                    w * 0.88f, h * 0.58f,
                    w, h * 0.54f
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 26.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Compact language toggle pill at top-end
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.72f))
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

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Vaultix App Logo replacing reference logo
                VaultixLogoContainer(
                    size = 86.dp,
                    cornerRadius = 24.dp,
                    padding = 10.dp,
                    forceObsidianFrame = true
                )

                Spacer(modifier = Modifier.height(18.dp))

                AnimatedContent(
                    targetState = isSignUpMode,
                    transitionSpec = {
                        (fadeIn(tween(220)) + slideInVertically { it / 12 })
                            .togetherWith(fadeOut(tween(180)) + slideOutVertically { -it / 12 })
                    },
                    label = "auth_mode_transition"
                ) { signUp ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (signUp) {
                                if (isArabic) "إنشاء حساب" else "sign up"
                            } else {
                                if (isArabic) "تسجيل الدخول" else "Login"
                            },
                            color = AuthInkDark,
                            fontSize = 27.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(26.dp))

                        if (signUp) {
                            AuthMockInputField(
                                icon = Icons.Outlined.Person,
                                placeholder = if (isArabic) "اسم المستخدم" else "User name",
                                testTag = "auth_username_field",
                                onClick = triggerSoon
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        AuthMockInputField(
                            icon = Icons.Outlined.Email,
                            placeholder = if (isArabic) "البريد الإلكتروني" else "Email address",
                            testTag = "auth_email_field",
                            onClick = triggerSoon
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        AuthMockInputField(
                            icon = Icons.Outlined.Lock,
                            placeholder = if (isArabic) "كلمة المرور" else "Password",
                            testTag = "auth_password_field",
                            onClick = triggerSoon
                        )

                        if (signUp) {
                            Spacer(modifier = Modifier.height(14.dp))
                            AuthMockInputField(
                                icon = Icons.Outlined.Lock,
                                placeholder = if (isArabic) "تأكيد كلمة المرور" else "Confirm password",
                                testTag = "auth_confirm_password_field",
                                onClick = triggerSoon
                            )
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        // Primary Royal Blue Action Button (Log in / Sign up -> Soon)
                        Button(
                            onClick = triggerSoon,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag(if (signUp) "auth_signup_submit_button" else "auth_login_submit_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AuthRoyalIndigo,
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 3.dp,
                                pressedElevation = 1.dp
                            ),
                            contentPadding = PaddingValues(horizontal = 20.dp)
                        ) {
                            Text(
                                text = if (signUp) {
                                    if (isArabic) "إنشاء حساب" else "Sign up"
                                } else {
                                    if (isArabic) "تسجيل الدخول" else "Log in"
                                },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (!signUp) {
                            Box(
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(onClick = triggerSoon)
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .testTag("auth_forgot_password_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isArabic) "نسيت كلمة المرور ؟" else "Forgot password ?",
                                    color = AuthInkDark,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    thickness = 1.dp,
                                    color = AuthInkDark.copy(alpha = 0.72f)
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
                                    color = AuthInkDark.copy(alpha = 0.72f)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedButton(
                                onClick = { isSignUpMode = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("auth_switch_to_signup_button"),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.5.dp, AuthRoyalIndigo),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = AuthRoyalIndigo
                                )
                            ) {
                                Text(
                                    text = if (isArabic) "إنشاء حساب" else "sign up",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            Text(
                                text = if (isArabic) "لديك حساب بالفعل ؟" else "Already have account ?",
                                color = AuthInkDark,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedButton(
                                onClick = { isSignUpMode = false },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("auth_switch_to_login_button"),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.5.dp, AuthRoyalIndigo),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = AuthRoyalIndigo
                                )
                            ) {
                                Text(
                                    text = if (isArabic) "تسجيل الدخول" else "Log in",
                                    fontSize = 16.sp,
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
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Requested Bottom Action: Create Local Account ("انشاء حساب محلي")
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
                        .clickable(onClick = onCreateLocalAccount)
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

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Animated "قريباً / Soon" pill toast on tapping any cloud auth input or action
        AnimatedVisibility(
            visible = showSoonPill,
            enter = fadeIn(tween(150)) + scaleIn(
                initialScale = 0.88f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ),
            exit = fadeOut(tween(180)) + scaleOut(targetScale = 0.92f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 22.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = AuthInkDark,
                border = BorderStroke(1.2.dp, Color(0xFF38BDF8).copy(alpha = 0.55f)),
                shadowElevation = 10.dp,
                modifier = Modifier.testTag("auth_soon_pill")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isArabic) "قريباً" else "Soon",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthMockInputField(
    icon: ImageVector,
    placeholder: String,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AuthFieldGray)
            .clickable(onClick = onClick)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AuthRoyalIndigo),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = placeholder,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = placeholder,
            color = AuthPlaceholderGray,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
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
