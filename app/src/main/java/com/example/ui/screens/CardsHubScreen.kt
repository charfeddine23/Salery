package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import com.example.data.UserSettings
import com.example.ui.components.EditCardDialog
import com.example.ui.components.GlassCard
import com.example.ui.components.SpLogoContainer
import com.example.ui.components.formatCardDigitsDisplay
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.ArabicStrings
import com.example.ui.i18n.LocalStealthMode
import com.example.ui.i18n.formatDzd
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.LocalAdaptiveDimens
import com.example.ui.theme.LocalAdaptiveTypography
import com.example.ui.theme.RoseNeon
import com.example.viewmodel.FinancialSummary
import kotlinx.coroutines.delay

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
    onUpdateClassicCard: (holderName: String, cardNumber: String, expiryDate: String, cvc: String) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors
    val isArabic = strings is ArabicStrings
    val isDark = colors.isDark
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current

    var showEditClassicCardDialog by remember { mutableStateOf(false) }

    if (showEditClassicCardDialog) {
        EditCardDialog(
            currentHolderName = userSettings.classicCardHolderName,
            currentCardNumber = userSettings.classicCardNumber,
            currentExpiry = userSettings.classicCardExpiry,
            currentCvc = userSettings.classicCardCvc,
            onDismiss = { showEditClassicCardDialog = false },
            onSave = { name, number, expiry, cvc ->
                onUpdateClassicCard(name, number, expiry, cvc)
            }
        )
    }

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
                        color = CyanNeon,
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
                    onClick = onOpenDashboard,
                    onLongPressEdit = { showEditClassicCardDialog = true }
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
 * Ultra-high-definition, realistic and tactile Classic Blue Algérie Poste Card.
 * Supports:
 * - Short Tap: Opens the full financial Dashboard.
 * - 1-Second Press-and-Hold (1000ms): Opens EditCardDialog to customize name, card number, expiry, and CVC.
 */
@Composable
private fun EdahabiaCardRealistic(
    summary: FinancialSummary,
    userSettings: UserSettings,
    isArabic: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    onLongPressEdit: () -> Unit
) {
    val colors = LiquidGlassTheme.colors
    val strings = AppStrings.current
    val isStealth = LocalStealthMode.current
    val haptic = LocalHapticFeedback.current

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val holdAnim = remember { Animatable(0f) }
    var didTriggerLongPress by remember { mutableStateOf(false) }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            didTriggerLongPress = false
            holdAnim.snapTo(0f)
            // clickable waits ~100ms TapTimeout before emitting Press inside LazyColumn,
            // so 900ms animation equals 1.0 full second of holding.
            holdAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 900, easing = LinearEasing)
            )
            didTriggerLongPress = true
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onLongPressEdit()
        } else {
            holdAnim.snapTo(0f)
            if (didTriggerLongPress) {
                delay(150)
                didTriggerLongPress = false
            }
        }
    }

    // Subtle animated breathing background glow (Liquid Glass v3 breathing shaders)
    val infiniteTransition = rememberInfiniteTransition(label = "classic_blue_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = if (isDark) 0.28f else 0.18f,
        targetValue = if (isDark) 0.58f else 0.38f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "classic_blue_alpha"
    )
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "classic_blue_scale"
    )

    val cardScale = 1f - (holdAnim.value * 0.025f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(cardScale)
            .testTag("classic_blue_card")
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = CyanNeon),
                onClick = {
                    if (didTriggerLongPress) {
                        didTriggerLongPress = false
                    } else {
                        onClick()
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        // Breathing Radial Halo behind the card in vibrant Royal Blue
        Box(
            modifier = Modifier
                .matchParentSize()
                .scale(glowScale)
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF2563EB).copy(alpha = glowAlpha),
                            Color(0xFF0284C7).copy(alpha = glowAlpha * 0.45f),
                            Color.Transparent
                        )
                    )
                )
        )

        // The Physical Tactile Classic Blue Card (Algérie Poste Classic Reference Design)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(222.dp)
                .shadow(
                    elevation = if (isDark) 16.dp else 12.dp,
                    shape = RoundedCornerShape(22.dp),
                    spotColor = if (isDark) Color(0xFF020617) else Color(0x661D4ED8),
                    ambientColor = if (isDark) Color(0xFF2563EB).copy(alpha = 0.35f) else Color(0x331D4ED8)
                )
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF2563EB), // Vibrant royal cobalt upper-left
                            Color(0xFF1D4ED8), // Classic Algérie Poste blue
                            Color(0xFF1A42A8), // Rich sapphire midtone
                            Color(0xFF15348A), // Deep blue lower-right
                            Color(0xFF0F2566)  // Deep navy shadow edge
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(1000f, 650f)
                    )
                )
                .border(
                    width = if (holdAnim.value > 0.05f) 2.2.dp else 1.2.dp,
                    brush = if (holdAnim.value > 0.05f) {
                        Brush.linearGradient(
                            colors = listOf(
                                CyanNeon,
                                EmeraldNeon,
                                Color.White
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFBAE6FD).copy(alpha = 0.85f),
                                Color(0xFF38BDF8).copy(alpha = 0.55f),
                                Color(0xFF1D4ED8).copy(alpha = 0.65f),
                                Color(0xFFBAE6FD).copy(alpha = 0.45f)
                            )
                        )
                    },
                    shape = RoundedCornerShape(22.dp)
                )
        ) {
            // Classic Algérie Poste Artwork: Landmarks Skyline, Diagonal Gloss & Right Zellige Mosaic
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawClassicEdahabiaArtwork(holdProgress = holdAnim.value)
            }

            // Card Inner Content (Always LTR to match physical bank card typography, kept clear of right mosaic)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 18.dp, end = 52.dp, top = 14.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Row: "الذهبية / CLASSIC" on Left & White Algérie Poste Crest on Right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "الذهبية",
                                color = Color(0xFFE0F2FE),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 12.sp
                            )
                            Text(
                                text = "CLASSIC",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.6.sp,
                                lineHeight = 21.sp,
                                style = TextStyle(
                                    shadow = Shadow(
                                        color = Color(0xFF0F172A).copy(alpha = 0.45f),
                                        offset = Offset(0f, 1.5f),
                                        blurRadius = 3f
                                    )
                                )
                            )
                        }

                        AlgeriePosteWhiteLogo()
                    }

                    // Middle Row: Gold EMV Chip + Contactless Waves & Subtle Live Balance Pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            EmvChipRealistic(modifier = Modifier.size(width = 42.dp, height = 31.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            ContactlessWaves(
                                modifier = Modifier.size(18.dp),
                                tint = Color(0xFFBAE6FD).copy(alpha = 0.88f)
                            )
                        }

                        // Live Total Balance Glass Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(11.dp))
                                .background(Color(0xFF0B1F56).copy(alpha = 0.56f))
                                .border(0.9.dp, Color(0xFF7DD3FC).copy(alpha = 0.5f), RoundedCornerShape(11.dp))
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (isArabic) "الرصيد الإجمالي" else "Total Balance",
                                    color = Color(0xFF93C5FD),
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 10.sp
                                )
                                Text(
                                    text = formatDzd(summary.totalBalance),
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.3.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    // Card Number: Embossed Pure White Monospace Digits
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatCardDigitsDisplay(userSettings.classicCardNumber, isStealth),
                            color = Color.White,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.1.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = TextStyle(
                                shadow = Shadow(
                                    color = Color(0xFF0A1B4D).copy(alpha = 0.85f),
                                    offset = Offset(1f, 1.5f),
                                    blurRadius = 2f
                                )
                            )
                        )
                    }

                    // Bottom Row: Cardholder Name (Bottom Left) + EXP DATE & CVC (Center-Right)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = userSettings.classicCardHolderName.ifBlank { "CHARFEDDINE KA" }.uppercase(),
                                color = Color.White,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = TextStyle(
                                    shadow = Shadow(
                                        color = Color(0xFF0A1B4D).copy(alpha = 0.7f),
                                        offset = Offset(0.5f, 1f),
                                        blurRadius = 1.5f
                                    )
                                )
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "EXP DATE",
                                    color = Color(0xFFBAE6FD),
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                val expFormatted = userSettings.classicCardExpiry
                                    .ifBlank { "11/29" }
                                    .replace("/", " / ")
                                Text(
                                    text = expFormatted,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "CVC",
                                    color = Color(0xFFBAE6FD),
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = if (isStealth || userSettings.classicCardCvc.isBlank()) "•••" else userSettings.classicCardCvc,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            // Floating Hold-to-Edit Progress Badge while user is holding for 1 second
            if (holdAnim.value > 0.12f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF0F172A).copy(alpha = 0.88f))
                        .border(1.dp, CyanNeon, RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = CyanNeon,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.holdingToEditLabel,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { holdAnim.value },
                            modifier = Modifier
                                .width(130.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = CyanNeon,
                            trackColor = Color.White.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }
    }

    // Interactive Tap & Hold Guidance Pill
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
            text = strings.holdToEditCardHint,
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
            size = Size(size.width * 0.9f, size.height * 0.9f),
            style = stroke
        )
    }
}

/**
 * Stylized white Algérie Poste crescent-wing emblem matching the Classic Blue card.
 */
@Composable
private fun AlgeriePosteWhiteLogo(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(modifier = Modifier.size(width = 40.dp, height = 26.dp)) {
            val w = size.width
            val h = size.height

            // Outer sweeping crescent / wing arc
            val crescentPath = Path().apply {
                moveTo(w * 0.08f, h * 0.30f)
                cubicTo(
                    w * 0.36f, h * 0.02f,
                    w * 0.92f, h * 0.06f,
                    w * 0.90f, h * 0.46f
                )
                cubicTo(
                    w * 0.88f, h * 0.84f,
                    w * 0.40f, h * 0.92f,
                    w * 0.16f, h * 0.68f
                )
                cubicTo(
                    w * 0.34f, h * 0.66f,
                    w * 0.64f, h * 0.56f,
                    w * 0.64f, h * 0.36f
                )
                cubicTo(
                    w * 0.64f, h * 0.22f,
                    w * 0.36f, h * 0.24f,
                    w * 0.08f, h * 0.30f
                )
                close()
            }
            drawPath(path = crescentPath, color = Color.White)

            // Slanted postal bar beneath crest
            drawLine(
                color = Color.White,
                start = Offset(w * 0.18f, h * 0.88f),
                end = Offset(w * 0.82f, h * 0.72f),
                strokeWidth = 3.2f,
                cap = StrokeCap.Round
            )
        }
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = "بريد الجزائر",
            color = Color.White,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 9.sp
        )
        Text(
            text = "ALGÉRIE POSTE",
            color = Color(0xFFBAE6FD),
            fontSize = 5.8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp,
            lineHeight = 7.sp
        )
    }
}

/**
 * Draws the Algerian Classic Blue Edahabia surface artwork:
 * 1) Subtle satin surface lines + upper-left diagonal sapphire sheen
 * 2) Faint landmark line-art silhouettes (Maqam Echahid, Djamaa el Djazaïr minaret, curved dome)
 * 3) Iconic right-hand curved Islamic geometric zellige mosaic strip in Gold, White, Sky Blue & Navy
 * 4) 1-second press-and-hold bottom progress accent
 */
private fun DrawScope.drawClassicEdahabiaArtwork(holdProgress: Float) {
    // 1. Subtle diagonal sheen across upper-left (just above the EMV chip area)
    val glossPath = Path().apply {
        moveTo(0f, size.height * 0.56f)
        quadraticTo(
            size.width * 0.30f, size.height * 0.32f,
            size.width * 0.78f, size.height * 0.24f
        )
        lineTo(size.width * 0.84f, 0f)
        lineTo(0f, 0f)
        close()
    }
    drawPath(
        path = glossPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF60A5FA).copy(alpha = 0.20f),
                Color(0xFF93C5FD).copy(alpha = 0.04f)
            )
        )
    )

    // Subtle brushed diagonal lines
    val step = 16f
    var diagX = -size.height
    while (diagX < size.width + size.height) {
        drawLine(
            color = Color.White.copy(alpha = 0.025f),
            start = Offset(diagX, 0f),
            end = Offset(diagX + size.height, size.height),
            strokeWidth = 1f
        )
        diagX += step
    }

    // 2. Faint Algerian Landmarks Wireframe Silhouettes along bottom
    val watermarkStroke = Stroke(
        width = 1.6f,
        cap = StrokeCap.Round,
        join = StrokeJoin.Round
    )
    val landmarkTint = Color(0xFF93C5FD).copy(alpha = 0.22f)
    val landmarkFaint = Color(0xFFBAE6FD).copy(alpha = 0.14f)

    // Maqam Echahid (Martyrs' Memorial) bottom-left
    val maqamBaseY = size.height * 0.97f
    val maqamCenterX = size.width * 0.13f
    val maqamTopY = size.height * 0.56f
    // Left curved palm-rib
    drawPath(
        path = Path().apply {
            moveTo(maqamCenterX - size.width * 0.055f, maqamBaseY)
            quadraticTo(
                maqamCenterX - size.width * 0.015f, size.height * 0.72f,
                maqamCenterX - size.width * 0.008f, maqamTopY
            )
            lineTo(maqamCenterX + size.width * 0.008f, maqamTopY)
            quadraticTo(
                maqamCenterX + size.width * 0.015f, size.height * 0.72f,
                maqamCenterX + size.width * 0.055f, maqamBaseY
            )
        },
        color = landmarkTint,
        style = watermarkStroke
    )
    // Inner spire line
    drawLine(
        color = landmarkFaint,
        start = Offset(maqamCenterX, maqamTopY + 4f),
        end = Offset(maqamCenterX, maqamBaseY),
        strokeWidth = 1.4f
    )

    // Arched Colonnade & Djamaa el Djazaïr Minaret in center
    val minaretX = size.width * 0.42f
    val minaretW = size.width * 0.042f
    val minaretTopY = size.height * 0.28f
    drawRect(
        color = landmarkTint,
        topLeft = Offset(minaretX, minaretTopY),
        size = Size(minaretW, size.height - minaretTopY),
        style = watermarkStroke
    )
    // Minaret spire cap & inner geometric ribs
    drawPath(
        path = Path().apply {
            moveTo(minaretX, minaretTopY)
            lineTo(minaretX + minaretW / 2f, minaretTopY - size.height * 0.06f)
            lineTo(minaretX + minaretW, minaretTopY)
        },
        color = landmarkTint,
        style = watermarkStroke
    )
    for (tier in 1..4) {
        val ty = minaretTopY + (size.height - minaretTopY) * (tier * 0.2f)
        drawLine(
            color = landmarkFaint,
            start = Offset(minaretX, ty),
            end = Offset(minaretX + minaretW, ty),
            strokeWidth = 1.2f
        )
    }

    // Colonnade arches between Maqam and Minaret
    val archStartX = size.width * 0.22f
    val archWidth = size.width * 0.032f
    for (i in 0..4) {
        val ax = archStartX + i * (archWidth + 4f)
        val ay = size.height * 0.78f
        drawPath(
            path = Path().apply {
                moveTo(ax, size.height)
                lineTo(ax, ay + 10f)
                quadraticTo(ax + archWidth / 2f, ay, ax + archWidth, ay + 10f)
                lineTo(ax + archWidth, size.height)
            },
            color = landmarkFaint,
            style = watermarkStroke
        )
    }

    // Wide Mosque Dome silhouette on bottom-right (before mosaic)
    val domeLeftX = size.width * 0.56f
    val domeRightX = size.width * 0.80f
    drawPath(
        path = Path().apply {
            moveTo(domeLeftX, size.height * 0.94f)
            quadraticTo(
                (domeLeftX + domeRightX) / 2f, size.height * 0.70f,
                domeRightX, size.height * 0.94f
            )
        },
        color = landmarkTint,
        style = watermarkStroke
    )
    drawPath(
        path = Path().apply {
            moveTo(domeLeftX + 18f, size.height * 0.94f)
            quadraticTo(
                (domeLeftX + domeRightX) / 2f, size.height * 0.76f,
                domeRightX - 18f, size.height * 0.94f
            )
        },
        color = landmarkFaint,
        style = watermarkStroke
    )

    // 3. Curved Right-Edge Islamic Geometric Zellige Mosaic Strip
    val mosaicBoundaryPath = Path().apply {
        moveTo(size.width * 0.835f, 0f)
        quadraticTo(
            size.width * 0.895f, size.height * 0.45f,
            size.width * 0.845f, size.height
        )
    }
    val mosaicClipPath = Path().apply {
        addPath(mosaicBoundaryPath)
        lineTo(size.width, size.height)
        lineTo(size.width, 0f)
        close()
    }

    // Soft curve shadow behind the mosaic seam
    drawPath(
        path = mosaicBoundaryPath,
        color = Color(0xFF09163A).copy(alpha = 0.55f),
        style = Stroke(width = 7f)
    )

    clipPath(mosaicClipPath) {
        // Deep royal navy grout background
        drawRect(
            color = Color(0xFF1E3A8A),
            topLeft = Offset(size.width * 0.81f, 0f),
            size = Size(size.width * 0.22f, size.height)
        )

        val tilePalette = listOf(
            Color(0xFFFACC15), // Classic Algerian Gold Yellow
            Color(0xFFFFFFFF), // Pure Ceramic White
            Color(0xFF38BDF8), // Vibrant Sky Blue
            Color(0xFF1D4ED8), // Cobalt Blue
            Color(0xFFFDE047), // Warm Bright Gold
            Color(0xFFE0F2FE), // Ice White-Blue
            Color(0xFF0284C7), // Azure Blue
            Color(0xFF1E3A8A)  // Deep Zellige Navy
        )

        val cellW = size.width * 0.068f
        val cellH = size.height / 5.4f
        val startX = size.width * 0.81f
        val cols = 4
        val rows = 7

        for (r in -1..rows) {
            for (c in 0..cols) {
                val cx = startX + c * cellW + (if (r % 2 == 0) 0f else cellW * 0.5f)
                val cy = r * cellH
                val baseIdx = (r * 3 + c * 5 + 10).let { if (it < 0) -it else it }

                // Primary diamond/kite zellige tile
                val diamond = Path().apply {
                    moveTo(cx, cy - cellH * 0.5f)
                    lineTo(cx + cellW * 0.5f, cy)
                    lineTo(cx, cy + cellH * 0.5f)
                    lineTo(cx - cellW * 0.5f, cy)
                    close()
                }
                val diamondColor = tilePalette[baseIdx % tilePalette.size]
                drawPath(path = diamond, color = diamondColor)
                drawPath(
                    path = diamond,
                    color = Color(0xFF0F2566).copy(alpha = 0.45f),
                    style = Stroke(width = 1.2f)
                )

                // Interlocking 8-pointed star / rosette accents on selected intersections
                if ((r + c) % 2 == 0) {
                    val starColor = when ((r + c * 2) % 3) {
                        0 -> Color(0xFFFACC15) // Gold star
                        1 -> Color.White       // White star
                        else -> Color(0xFF38BDF8) // Sky star
                    }
                    val outerR = cellW * 0.36f
                    val innerR = cellW * 0.18f
                    val starPath = Path()
                    val points = 16
                    for (p in 0 until points) {
                        val angle = (p * PI / 8.0).toFloat()
                        val rad = if (p % 2 == 0) outerR else innerR
                        val sx = cx + cos(angle) * rad
                        val sy = cy + sin(angle) * rad
                        if (p == 0) starPath.moveTo(sx, sy) else starPath.lineTo(sx, sy)
                    }
                    starPath.close()
                    drawPath(path = starPath, color = starColor)
                    drawPath(
                        path = starPath,
                        color = Color(0xFF1E3A8A).copy(alpha = 0.55f),
                        style = Stroke(width = 1.0f)
                    )
                }
            }
        }
    }

    // Crisp luminous border highlight along the left edge of the mosaic band
    drawPath(
        path = mosaicBoundaryPath,
        color = Color(0xFF93C5FD).copy(alpha = 0.45f),
        style = Stroke(width = 1.4f)
    )

    // 4. 1-second Hold Progress Rim Accent at bottom of card
    if (holdProgress > 0.02f) {
        drawRoundRect(
            color = CyanNeon.copy(alpha = 0.9f),
            topLeft = Offset(0f, size.height - 6f),
            size = Size(size.width * holdProgress, 6f),
            cornerRadius = CornerRadius(4f, 4f)
        )
    }
}

