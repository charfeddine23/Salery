package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassCard
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.ArabicStrings
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.LocalAdaptiveDimens
import com.example.ui.theme.LocalAdaptiveTypography
import com.example.ui.theme.RoseNeon

/**
 * Modern Neumorphic "Coming Soon" screen for the Visa International Card.
 * Adheres strictly to Neumorphism / Liquid Glass v3 design standards.
 */
@Composable
fun VisaComingSoonScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBackClick() }

    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors
    val isArabic = strings is ArabicStrings
    val isDark = colors.isDark
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current

    // Breathing glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "VisaComingSoonGlow")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloScale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloAlpha"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = dimens.screenPaddingH),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimens.spacingLg)
    ) {
        item {
            // Header with Back Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(colors.glassCard)
                        .border(1.dp, colors.glassBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = strings.visaCardTitle,
                        color = colors.textPrimary,
                        fontSize = typo.titleLg,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.visaCardSubtitle,
                        color = colors.textMuted,
                        fontSize = typo.caption
                    )
                }
            }
        }

        // Hero Card Visual with Breathing Aura
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                // Breathing radial halo behind the card
                Box(
                    modifier = Modifier
                        .size(width = 320.dp, height = 210.dp)
                        .scale(glowScale)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF2563EB).copy(alpha = glowAlpha * 0.45f),
                                    Color(0xFF06B6D4).copy(alpha = glowAlpha * 0.25f),
                                    Color.Transparent
                                )
                            ),
                            shape = RoundedCornerShape(32.dp)
                        )
                )

                // The 3D Visa Card Container
                Box(
                    modifier = Modifier
                        .widthIn(max = 420.dp)
                        .fillMaxWidth()
                        .height(205.dp)
                        .shadow(
                            elevation = if (isDark) 16.dp else 12.dp,
                            shape = RoundedCornerShape(22.dp),
                            spotColor = Color(0xFF1D4ED8).copy(alpha = 0.5f),
                            ambientColor = if (isDark) Color(0xFF000000) else Color(0xFF94A3B8)
                        )
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF0B192C),
                                    Color(0xFF1E3E62),
                                    Color(0xFF000000)
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(600f, 400f)
                            )
                        )
                        .border(
                            width = 1.2.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF60A5FA).copy(alpha = 0.8f),
                                    Color(0xFF38BDF8).copy(alpha = 0.4f),
                                    Color(0xFF1E293B).copy(alpha = 0.6f)
                                )
                            ),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top row: Brand & Status Tag
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SaleryPulse",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )

                            // Coming soon pill badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF2563EB), Color(0xFF06B6D4))
                                        )
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isArabic) "قريباً" else "COMING SOON",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.8.sp
                                )
                            }
                        }

                        // Middle: Card Number Mask & EMV Chip placeholder
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "4532  ••••  ••••  8829",
                                color = Color(0xFFE2E8F0),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 2.sp
                            )
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Bottom row: Cardholder Name & Official Visa Logo
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = "CARDHOLDER",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "CHARFEDDINE KA",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                )
                            }

                            Text(
                                text = "VISA",
                                color = Color.White,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                fontStyle = FontStyle.Italic,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                }
            }
        }

        // Announcement & Status Badge
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = colors.glassCard,
                borderColor = Color(0xFF2563EB).copy(alpha = 0.4f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF2563EB), Color(0xFF06B6D4))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (isArabic) "بطاقة فيزا الدولية قريباً" else "Visa International Coming Soon",
                        color = colors.textPrimary,
                        fontSize = typo.title,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = strings.visaComingSoonDesc,
                        color = colors.textMuted,
                        fontSize = typo.body,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }
            }
        }

        // Future Features Preview
        item {
            Text(
                text = if (isArabic) "المميزات القادمة" else "Upcoming Capabilities",
                color = colors.textPrimary,
                fontSize = typo.titleSm,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FeaturePreviewItem(
                    icon = Icons.Default.CurrencyExchange,
                    iconTint = EmeraldNeon,
                    title = if (isArabic) "محافظ متعددة العملات (USD / EUR)" else "Multi-Currency Wallets (USD / EUR)",
                    subtitle = if (isArabic) "شحن مباشر بالدينار الجزائري والدفع بالدولار واليورو" else "Direct top-up with DZD and spend in USD / EUR without hidden fees"
                )

                FeaturePreviewItem(
                    icon = Icons.Default.ShoppingBag,
                    iconTint = CyanNeon,
                    title = if (isArabic) "الدفع الإلكتروني والاشتراكات" else "Online Subscriptions & Purchases",
                    subtitle = if (isArabic) "تفعيل اشتراكات Netflix و Spotify والشراء من AliExpress و Google Play" else "Seamless payments on Google Play, AliExpress, Spotify & Netflix"
                )

                FeaturePreviewItem(
                    icon = Icons.Default.Security,
                    iconTint = Color(0xFF60A5FA),
                    title = if (isArabic) "حماية ثلاثية الأبعاد 3D Secure" else "3D Secure Fraud Protection",
                    subtitle = if (isArabic) "تأكيد فوري للعمليات عبر إشعار الهاتف مع إمكانية التجميد اللحظي" else "Instant OTP notifications and one-tap in-app card freeze"
                )
            }
        }

        // Return Action Button
        item {
            Button(
                onClick = onBackClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(vertical = 2.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primaryNeon,
                    contentColor = if (isDark) Color(0xFF0F172A) else Color.White
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = strings.backToCards,
                    fontSize = typo.bodyLg,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FeaturePreviewItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String
) {
    val colors = LiquidGlassTheme.colors
    val typo = LocalAdaptiveTypography.current

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = colors.glassCard
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = colors.textPrimary,
                    fontSize = typo.body,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = colors.textMuted,
                    fontSize = typo.caption,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
