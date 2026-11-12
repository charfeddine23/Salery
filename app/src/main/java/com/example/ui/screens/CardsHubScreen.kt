package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserSettings
import com.example.ui.components.GlassCard
import com.example.ui.components.SpLogoContainer
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.ArabicStrings
import com.example.ui.i18n.formatDzd
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.LocalAdaptiveDimens
import com.example.ui.theme.LocalAdaptiveTypography
import com.example.ui.theme.RoseNeon
import com.example.viewmodel.FinancialSummary

/**
 * Cards Hub Screen ("البطاقات").
 * Houses the ultra-tactile Neumorphic Algerian Edahabia card (navigates to full Dashboard)
 * and the secondary luxury Visa card (navigates to Coming Soon placeholder).
 */
@Composable
fun CardsHubScreen(
    summary: FinancialSummary,
    userSettings: UserSettings,
    onOpenDashboard: () -> Unit,
    onOpenVisaDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors
    val isArabic = strings is ArabicStrings
    val isDark = colors.isDark
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = dimens.screenPaddingH),
        verticalArrangement = Arrangement.spacedBy(dimens.spacingLg)
    ) {
        // Section Header: Cards Hub Title & Active Badges
        item {
            Spacer(modifier = Modifier.height(dimens.spacingSm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = strings.cardsHubTitle,
                        color = colors.textPrimary,
                        fontSize = typo.title,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.2.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = strings.cardsHubSubtitle,
                        color = colors.textMuted,
                        fontSize = typo.caption
                    )
                }

                // Security & Status Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) colors.glassCard else Color.White.copy(alpha = 0.9f))
                        .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(EmeraldNeon)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.activeCardsCount,
                            color = colors.textPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // 1. Primary Card: Algérie Poste - Edahabia (الذهبية)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.edahabiaCardTitle,
                        color = colors.textPrimary,
                        fontSize = typo.body,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.currentAccount,
                        color = AmberNeon,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                EdahabiaCardRealistic(
                    summary = summary,
                    userSettings = userSettings,
                    isArabic = isArabic,
                    isDark = isDark,
                    onClick = onOpenDashboard
                )
            }
        }

        // 2. Secondary Card: Visa Card (بطاقة فيزا)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.visaCardTitle,
                        color = colors.textPrimary,
                        fontSize = typo.body,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyanNeon.copy(alpha = 0.15f))
                            .border(1.dp, CyanNeon.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = strings.comingSoon,
                            color = CyanNeon,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                VisaCardRealistic(
                    userSettings = userSettings,
                    isArabic = isArabic,
                    isDark = isDark,
                    onClick = onOpenVisaDetails
                )
            }
        }

        // Bottom Info Card: Security & Connectivity
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = dimens.spacingLg),
                shape = RoundedCornerShape(18.dp),
                backgroundColor = colors.glassCard
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(colors.primaryNeon.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = colors.primaryNeon,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isArabic) "حماية متقدمة وتشفير مصرفي" else "Bank-Grade Encryption & Security",
                            color = colors.textPrimary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isArabic) "جميع الحسابات والبطاقات مؤمنة محلياً بتشفير AES-256 والمصادقة البيومترية."
                            else "All cards and balances are protected locally with AES-256 and biometric security.",
                            color = colors.textMuted,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Ultra-high-definition, realistic and tactile Neumorphic Algérie Poste Edahabia Card.
 */
@Composable
private fun EdahabiaCardRealistic(
    summary: FinancialSummary,
    userSettings: UserSettings,
    isArabic: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val colors = LiquidGlassTheme.colors

    // Subtle animated breathing background glow (Liquid Glass v3 breathing shaders)
    val infiniteTransition = rememberInfiniteTransition(label = "edahabia_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = if (isDark) 0.28f else 0.18f,
        targetValue = if (isDark) 0.58f else 0.38f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "edahabia_alpha"
    )
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "edahabia_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = AmberNeon),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Breathing Radial Halo behind the card
        Box(
            modifier = Modifier
                .matchParentSize()
                .scale(glowScale)
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFE5A93C).copy(alpha = glowAlpha),
                            Color(0xFFC58925).copy(alpha = glowAlpha * 0.45f),
                            Color.Transparent
                        )
                    )
                )
        )

        // The Physical Tactile Edahabia Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(216.dp)
                .shadow(
                    elevation = if (isDark) 16.dp else 12.dp,
                    shape = RoundedCornerShape(22.dp),
                    spotColor = if (isDark) Color(0xFF020617) else Color(0x66B45309),
                    ambientColor = if (isDark) Color(0xFFE5A93C).copy(alpha = 0.3f) else Color(0x33B45309)
                )
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFF9E8A2), // Champagne reflection highlight
                            Color(0xFFE5B036), // Authentic rich gold
                            Color(0xFFCA8A04), // Classic Edahabia warm amber gold
                            Color(0xFFA16207), // Deep burnished gold
                            Color(0xFF713F12)  // Dark brass depth
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(1000f, 600f)
                    )
                )
                .border(
                    width = 1.2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFFFBEB).copy(alpha = 0.8f),
                            Color(0xFFFDE68A).copy(alpha = 0.5f),
                            Color(0xFF854D0E).copy(alpha = 0.7f),
                            Color(0xFFFFFBEB).copy(alpha = 0.4f)
                        )
                    ),
                    shape = RoundedCornerShape(22.dp)
                )
        ) {
            // Subtle brushed metallic texture lines
            Canvas(modifier = Modifier.fillMaxSize()) {
                val step = 14f
                var x = -size.height
                while (x < size.width + size.height) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.035f),
                        start = Offset(x, 0f),
                        end = Offset(x + size.height, size.height),
                        strokeWidth = 1f
                    )
                    x += step
                }
            }

            // Card Inner Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Row: Algérie Poste (بريد الجزائر) Emblem & "الذهبية" (EDAHABIA)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Algérie Poste Logo & Typography
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Official Algerian Postal Envelope & Crescent Emblem
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF1E293B).copy(alpha = 0.65f))
                                .border(0.8.dp, Color(0xFFFDE047), RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = "Algérie Poste",
                                tint = Color(0xFFFDE047),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            Text(
                                text = "بريد الجزائر",
                                color = Color(0xFF1E293B),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.2.sp
                            )
                            Text(
                                text = "Algérie Poste",
                                color = Color(0xFF334155),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // Stylized "الذهبية" (EDAHABIA) Calligraphy Crest
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E293B).copy(alpha = 0.25f))
                            .border(1.dp, Color(0xFFFFFBEB).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "الذهبية",
                                color = Color(0xFF0F172A),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "EDAHABIA",
                                color = Color(0xFF1E293B),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Middle Row: EMV Chip + Contactless Symbol + Live Balance Highlight Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Ultra-realistic EMV Gold Microchip
                        EmvChipRealistic(modifier = Modifier.size(width = 42.dp, height = 32.dp))

                        Spacer(modifier = Modifier.width(10.dp))

                        // Contactless Waves Symbol
                        ContactlessWaves(modifier = Modifier.size(20.dp), tint = Color(0xFF1E293B).copy(alpha = 0.8f))
                    }

                    // Core Financial Highlight Pill on Card: Live Total Balance
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F172A).copy(alpha = 0.35f))
                            .border(1.dp, Color(0xFFFFFBEB).copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (isArabic) "الرصيد الإجمالي" else "Total Balance",
                                color = Color(0xFFFDE68A),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = formatDzd(summary.totalBalance),
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.3.sp
                            )
                        }
                    }
                }

                // Card Number: Embossed Monospace Credit Card Typeface with 3D stamping effect
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "6280  26••  ••••  9842",
                        color = Color(0xFF0F172A),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp,
                        style = androidx.compose.ui.text.TextStyle(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color(0xFFFFFBEB).copy(alpha = 0.8f),
                                offset = Offset(-0.8f, -0.8f),
                                blurRadius = 0.5f
                            )
                        )
                    )
                }

                // Bottom Row: Cardholder Name, Expiry, and CIB / SATIM Seal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "CARDHOLDER / الحائز",
                            color = Color(0xFF334155),
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.4.sp
                        )
                        Text(
                            text = "CHARFEDDINE KA",
                            color = Color(0xFF0F172A),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "EXPIRES END",
                            color = Color(0xFF334155),
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "11/29",
                            color = Color(0xFF0F172A),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Official Algerian CIB / SATIM Interbank Seal
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF0F172A).copy(alpha = 0.65f))
                            .border(0.8.dp, Color(0xFFFDE68A), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "CIB",
                            color = Color(0xFFFDE68A),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }

    // Interactive Tap Guidance Pill
    Spacer(modifier = Modifier.height(6.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isArabic) "اضغط على البطاقة الذهبية لفتح لوحة التحكم والتفاصيل"
            else "Tap Edahabia card to open full financial dashboard",
            color = colors.primaryNeon,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = colors.primaryNeon,
            modifier = Modifier.size(13.dp)
        )
    }
}

/**
 * Premium Neumorphic Visa Card container with a luxury metallic/glass finish.
 */
@Composable
private fun VisaCardRealistic(
    userSettings: UserSettings,
    isArabic: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val colors = LiquidGlassTheme.colors

    // Subtle animated breathing background glow (cyan / sapphire aura)
    val infiniteTransition = rememberInfiniteTransition(label = "visa_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = if (isDark) 0.18f else 0.12f,
        targetValue = if (isDark) 0.45f else 0.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "visa_alpha"
    )
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "visa_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = CyanNeon),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Breathing Radial Halo behind the Visa card
        Box(
            modifier = Modifier
                .matchParentSize()
                .scale(glowScale)
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF0284C7).copy(alpha = glowAlpha),
                            Color(0xFF0369A1).copy(alpha = glowAlpha * 0.4f),
                            Color.Transparent
                        )
                    )
                )
        )

        // The Physical Tactile Visa Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(205.dp)
                .shadow(
                    elevation = if (isDark) 14.dp else 10.dp,
                    shape = RoundedCornerShape(22.dp),
                    spotColor = if (isDark) Color(0xFF020617) else Color(0x400284C7),
                    ambientColor = if (isDark) Color(0xFF0284C7).copy(alpha = 0.25f) else Color(0x200284C7)
                )
                .clip(RoundedCornerShape(22.dp))
                .background(
                    if (isDark) {
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF0F172A), // Midnight obsidian
                                Color(0xFF1E293B), // Dark slate metallic
                                Color(0xFF0F2B48), // Deep sapphire
                                Color(0xFF075985)  // Electric blue accent
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(900f, 600f)
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFFFFFF), // Pure frosted crystal
                                Color(0xFFF1F5F9), // Ice platinum
                                Color(0xFFE2E8F0), // Titanium sheen
                                Color(0xFFBAE6FD)  // Light sapphire shimmer
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(900f, 600f)
                        )
                    }
                )
                .border(
                    width = 1.2.dp,
                    brush = Brush.linearGradient(
                        colors = if (isDark) {
                            listOf(
                                Color(0xFF38BDF8).copy(alpha = 0.65f),
                                Color(0xFF0F172A).copy(alpha = 0.4f),
                                Color(0xFF0284C7).copy(alpha = 0.6f)
                            )
                        } else {
                            listOf(
                                Color.White,
                                Color(0xFF93C5FD).copy(alpha = 0.5f),
                                Color(0xFFCBD5E1)
                            )
                        }
                    ),
                    shape = RoundedCornerShape(22.dp)
                )
        ) {
            // Subtle diagonal metallic reflection
            Canvas(modifier = Modifier.fillMaxSize()) {
                val path = Path().apply {
                    moveTo(size.width * 0.45f, 0f)
                    lineTo(size.width * 0.85f, 0f)
                    lineTo(size.width * 0.55f, size.height)
                    lineTo(size.width * 0.15f, size.height)
                    close()
                }
                drawPath(
                    path = path,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isDark) 0.05f else 0.15f),
                            Color.Transparent
                        )
                    )
                )
            }

            // Card Inner Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Row: Brand & Official VISA Logo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SpLogoContainer(size = 20.dp, cornerRadius = 6.dp, padding = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SaleryPulse",
                                color = if (isDark) Color.White else Color(0xFF0F172A),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.4.sp
                            )
                        }
                        Text(
                            text = "PREMIER DEBIT",
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }

                    // Official VISA Typography Emblem
                    Text(
                        text = "VISA",
                        color = if (isDark) Color.White else Color(0xFF1E3A8A),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        letterSpacing = 1.sp
                    )
                }

                // Middle Row: Silver/Platinum Chip + Contactless waves + Coming Soon status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SilverEmvChip(modifier = Modifier.size(width = 40.dp, height = 30.dp), isDark = isDark)
                        Spacer(modifier = Modifier.width(10.dp))
                        ContactlessWaves(
                            modifier = Modifier.size(18.dp),
                            tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                        )
                    }

                    // Coming Soon Glass Hologram Tag
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isDark) Color(0xFF0284C7).copy(alpha = 0.25f)
                                else Color(0xFF0284C7).copy(alpha = 0.12f)
                            )
                            .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isArabic) "قريباً • بطاقة دولية" else "Coming Soon • Multi-Currency",
                                color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0369A1),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Card Number: Masked
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "4152  ••••  ••••  3120",
                        color = if (isDark) Color.White else Color(0xFF0F172A),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                }

                // Bottom Row: Cardholder & Expiry
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "CARDHOLDER",
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.4.sp
                        )
                        Text(
                            text = "CHARFEDDINE KA",
                            color = if (isDark) Color.White else Color(0xFF0F172A),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "VALID THRU",
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "08/28",
                            color = if (isDark) Color.White else Color(0xFF0F172A),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }

    // Interactive Tap Guidance Pill
    Spacer(modifier = Modifier.height(6.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isArabic) "اضغط على بطاقة فيزا لعرض تفاصيل الإطلاق القادم"
            else "Tap Visa card to view upcoming release details",
            color = CyanNeon,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = CyanNeon,
            modifier = Modifier.size(13.dp)
        )
    }
}

/**
 * Realistic tactile EMV Gold Microchip graphic.
 */
@Composable
private fun EmvChipRealistic(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFEF08A),
                        Color(0xFFEAB308),
                        Color(0xFFCA8A04)
                    )
                )
            )
            .border(0.8.dp, Color(0xFF713F12).copy(alpha = 0.6f), RoundedCornerShape(6.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 1f)
            val strokeColor = Color(0xFF713F12).copy(alpha = 0.5f)

            // Horizontal microchip separation line
            drawLine(
                color = strokeColor,
                start = Offset(0f, size.height * 0.5f),
                end = Offset(size.width, size.height * 0.5f),
                strokeWidth = 1f
            )

            // Vertical pad lines
            drawLine(
                color = strokeColor,
                start = Offset(size.width * 0.35f, 0f),
                end = Offset(size.width * 0.35f, size.height),
                strokeWidth = 1f
            )
            drawLine(
                color = strokeColor,
                start = Offset(size.width * 0.65f, 0f),
                end = Offset(size.width * 0.65f, size.height),
                strokeWidth = 1f
            )
        }
    }
}

/**
 * Realistic tactile Silver/Platinum EMV Microchip graphic.
 */
@Composable
private fun SilverEmvChip(modifier: Modifier = Modifier, isDark: Boolean) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFF8FAFC),
                        Color(0xFFCBD5E1),
                        Color(0xFF94A3B8)
                    )
                )
            )
            .border(0.8.dp, Color(0xFF475569).copy(alpha = 0.6f), RoundedCornerShape(6.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeColor = Color(0xFF334155).copy(alpha = 0.5f)
            drawLine(
                color = strokeColor,
                start = Offset(0f, size.height * 0.5f),
                end = Offset(size.width, size.height * 0.5f),
                strokeWidth = 1f
            )
            drawLine(
                color = strokeColor,
                start = Offset(size.width * 0.35f, 0f),
                end = Offset(size.width * 0.35f, size.height),
                strokeWidth = 1f
            )
            drawLine(
                color = strokeColor,
                start = Offset(size.width * 0.65f, 0f),
                end = Offset(size.width * 0.65f, size.height),
                strokeWidth = 1f
            )
        }
    }
}

/**
 * Contactless Radio Wave curves representation.
 */
@Composable
private fun ContactlessWaves(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 1.8f)
        val cx = 0f
        val cy = size.height / 2f
        drawArc(
            color = tint,
            startAngle = -45f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(-size.width * 0.2f, cy - size.height * 0.25f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.5f, size.height * 0.5f),
            style = stroke
        )
        drawArc(
            color = tint,
            startAngle = -45f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(-size.width * 0.2f, cy - size.height * 0.45f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.9f, size.height * 0.9f),
            style = stroke
        )
    }
}
