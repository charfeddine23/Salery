package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cloud.GoogleAuthUiState
import com.example.ui.components.VaultixLogoContainer

private val AuthCreamBg = Color(0xFFFCF4E8)
private val AuthPastelBlob = Color(0xFFF9DFDC)
private val AuthRoyalIndigo = Color(0xFF334194)
private val AuthInkDark = Color(0xFF0F172A)
private val AuthInkMuted = Color(0xFF475569)
private val AuthErrorRed = Color(0xFFB91C1C)
private val AuthEmerald = Color(0xFF047857)

@Composable
fun AuthWelcomeScreen(
    isArabic: Boolean,
    googleAuthState: GoogleAuthUiState = GoogleAuthUiState.SignedOut,
    onToggleLanguage: (String) -> Unit,
    onGoogleSignIn: () -> Unit = {},
    onContinueSignedInGoogle: () -> Unit = {},
    onGoogleSignOut: () -> Unit = {},
    onCreateLocalAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLoading = googleAuthState is GoogleAuthUiState.Loading
    val signedInState = googleAuthState as? GoogleAuthUiState.SignedIn
    val errorState = googleAuthState as? GoogleAuthUiState.Error

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AuthCreamBg)
    ) {
        // Soft pastel organic curved blobs matching the editorial illustration aesthetic
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
                        .background(Color.White.copy(alpha = 0.78f))
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

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                VaultixLogoContainer(
                    size = 86.dp,
                    cornerRadius = 24.dp,
                    padding = 10.dp,
                    forceObsidianFrame = true
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = if (isArabic) "مرحباً بك في Vaultix" else "Welcome to Vaultix",
                    color = AuthInkDark,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isArabic) {
                        "خزنتك المالية المشفّرة محلياً • مع مزامنة سحابية آمنة عبر Google"
                    } else {
                        "Offline-first encrypted vault • Secure Google Cloud Sync"
                    },
                    color = AuthInkMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Firebase Authentication (Google Sign-In) Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, AuthRoyalIndigo.copy(alpha = 0.16f)),
                    shadowElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (signedInState != null) Icons.Default.CheckCircle else Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = if (signedInState != null) AuthEmerald else AuthRoyalIndigo,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (signedInState != null) {
                                    if (isArabic) "حساب Google متصل" else "Google Account Connected"
                                } else {
                                    if (isArabic) "تسجيل الدخول والمزامنة السحابية" else "Sign in & Cloud Sync"
                                },
                                color = AuthInkDark,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (signedInState != null) {
                                signedInState.email.ifBlank { signedInState.displayName }
                            } else if (isArabic) {
                                "تتم مزامنة معاملاتك وديونك وإعداداتك بأمان دون رفع كلمات المرور أو أرقام البطاقات"
                            } else {
                                "Sync transactions, debts, and preferences privately under your account while secrets stay encrypted on-device."
                            },
                            color = AuthInkMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 17.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (signedInState != null) {
                            // Signed-in Google Profile Pill
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(AuthEmerald.copy(alpha = 0.10f))
                                    .border(1.dp, AuthEmerald.copy(alpha = 0.30f), RoundedCornerShape(14.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
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
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = onContinueSignedInGoogle,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("auth_continue_google_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AuthRoyalIndigo,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 20.dp)
                            ) {
                                Text(
                                    text = if (isArabic) "المتابعة إلى Vaultix" else "Continue to Vaultix",
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

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedButton(
                                onClick = onGoogleSignOut,
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
                                    text = if (isArabic) "تبديل حساب Google" else "Switch Google Account",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            Button(
                                onClick = onGoogleSignIn,
                                enabled = !isLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("auth_google_signin_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AuthRoyalIndigo,
                                    contentColor = Color.White,
                                    disabledContainerColor = AuthRoyalIndigo.copy(alpha = 0.72f),
                                    disabledContentColor = Color.White
                                ),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 3.dp,
                                    pressedElevation = 1.dp
                                ),
                                contentPadding = PaddingValues(horizontal = 18.dp)
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
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "G",
                                            color = AuthRoyalIndigo,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = if (isArabic) "تسجيل الدخول عبر Google" else "Continue with Google",
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
                        }

                        AnimatedVisibility(
                            visible = errorState != null,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            errorState?.let { err ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(AuthErrorRed.copy(alpha = 0.08f))
                                        .border(1.dp, AuthErrorRed.copy(alpha = 0.28f), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                        .testTag("auth_google_error_banner"),
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
                                        text = err.message,
                                        color = AuthErrorRed,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        thickness = 1.dp,
                        color = AuthInkDark.copy(alpha = 0.35f)
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
                        color = AuthInkDark.copy(alpha = 0.35f)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Offline-First Local Account Option
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

                Spacer(modifier = Modifier.height(16.dp))
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
