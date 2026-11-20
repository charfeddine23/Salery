package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.WindowManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import com.example.data.AlgerianBankCardCatalog
import com.example.data.AlgerianBankCardSpec
import com.example.data.AlgerianBankMotif
import com.example.data.UserSettings
import com.example.ui.components.EditCardDialog
import com.example.ui.components.EditVisaCardDialog
import com.example.ui.components.GlassCard
import com.example.ui.components.SpLogoContainer
import com.example.ui.components.formatCardDigitsDisplay
import java.util.Locale
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.ArabicStrings
import com.example.ui.i18n.LocalStealthMode
import com.example.ui.i18n.formatDzd
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.LocalAdaptiveDimens
import com.example.ui.theme.LocalAdaptiveLayoutInfo
import com.example.ui.theme.LocalAdaptiveTypography
import com.example.ui.theme.breathingGlow
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
    binancePortfolioTotalUsd: Double = 0.0,
    binanceHoldingsCount: Int = 0,
    onOpenDashboard: () -> Unit,
    onOpenVisaDetails: () -> Unit,
    onOpenBinanceWallet: () -> Unit = {},
    onUpdateClassicCard: (holderName: String, cardNumber: String, expiryDate: String, cvc: String, bankStyle: String) -> Unit = { _, _, _, _, _ -> },
    onUpdateVisaCard: (provider: String, holderName: String, cardNumber: String, expiryDate: String, cvc: String) -> Unit = { _, _, _, _, _ -> },
    onUpdateThirdCard: (provider: String, holderName: String, cardNumber: String, expiryDate: String, cvc: String) -> Unit = { _, _, _, _, _ -> },
    onRemoveThirdCard: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors
    val isArabic = strings is ArabicStrings
    val isDark = colors.isDark
    val layoutInfo = LocalAdaptiveLayoutInfo.current
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isScreenshotBlocked = userSettings.screenshotProtectionEnabled

    // Strictly block (or allow when disabled) screenshots, screen recording, and Recents thumbnails while viewing Cards Hub
    DisposableEffect(context, lifecycleOwner, isScreenshotBlocked) {
        val activity = context.findHostActivity()
        val syncSecureFlags: () -> Unit = {
            activity?.let { act ->
                if (isScreenshotBlocked) {
                    act.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    act.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    try {
                        act.setRecentsScreenshotEnabled(!isScreenshotBlocked)
                    } catch (_: Throwable) {
                    }
                }
            }
        }
        syncSecureFlags()
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START || event == Lifecycle.Event.ON_RESUME) {
                syncSecureFlags()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            activity?.let { act ->
                act.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    try {
                        act.setRecentsScreenshotEnabled(true)
                    } catch (_: Throwable) {
                    }
                }
            }
        }
    }

    var showEditClassicCardDialog by remember { mutableStateOf(false) }
    var showEditVisaCardDialog by remember { mutableStateOf(false) }
    var showEditThirdCardDialog by remember { mutableStateOf(false) }

    if (showEditClassicCardDialog) {
        EditCardDialog(
            currentBankStyle = userSettings.classicCardBankStyle,
            currentHolderName = userSettings.classicCardHolderName,
            currentCardNumber = userSettings.classicCardNumber,
            currentExpiry = userSettings.classicCardExpiry,
            currentCvc = userSettings.classicCardCvc,
            secureScreenshots = isScreenshotBlocked,
            onDismiss = { showEditClassicCardDialog = false },
            onSave = { name, number, expiry, cvc, bankStyle ->
                onUpdateClassicCard(name, number, expiry, cvc, bankStyle)
            }
        )
    }

    if (showEditVisaCardDialog) {
        EditVisaCardDialog(
            currentProvider = userSettings.visaCardProvider,
            currentHolderName = userSettings.visaCardHolderName,
            currentCardNumber = userSettings.visaCardNumber,
            currentExpiry = userSettings.visaCardExpiry,
            currentCvc = userSettings.visaCardCvc,
            secureScreenshots = isScreenshotBlocked,
            onDismiss = { showEditVisaCardDialog = false },
            onSave = { provider, name, number, expiry, cvc ->
                onUpdateVisaCard(provider, name, number, expiry, cvc)
                showEditVisaCardDialog = false
            }
        )
    }

    if (showEditThirdCardDialog) {
        EditVisaCardDialog(
            currentProvider = userSettings.thirdCardProvider,
            currentHolderName = userSettings.thirdCardHolderName,
            currentCardNumber = userSettings.thirdCardNumber,
            currentExpiry = userSettings.thirdCardExpiry,
            currentCvc = userSettings.thirdCardCvc,
            secureScreenshots = isScreenshotBlocked,
            isThirdCard = true,
            onDeleteThirdCard = if (userSettings.thirdCardEnabled) onRemoveThirdCard else null,
            onDismiss = { showEditThirdCardDialog = false },
            onSave = { provider, name, number, expiry, cvc ->
                onUpdateThirdCard(provider, name, number, expiry, cvc)
                showEditThirdCardDialog = false
            }
        )
    }

    val primaryCardSection: @Composable (Modifier) -> Unit = { sectionMod ->
        val bankSpec = AlgerianBankCardCatalog.findById(userSettings.classicCardBankStyle)
        Column(modifier = sectionMod) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (bankSpec.id == "EDAHABIA") {
                        strings.edahabiaCardTitle
                    } else {
                        if (isArabic) "${bankSpec.nameAr} (${bankSpec.shortCode})" else bankSpec.nameEn
                    },
                    color = colors.textPrimary,
                    fontSize = typo.body,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.currentAccount,
                    color = CyanNeon,
                    fontSize = typo.caption,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            EdahabiaCardRealistic(
                summary = summary,
                userSettings = userSettings,
                bankSpec = bankSpec,
                isArabic = isArabic,
                isDark = isDark,
                onClick = onOpenDashboard,
                onLongPressEdit = { showEditClassicCardDialog = true }
            )
        }
    }

    val secondaryCardSection: @Composable (Modifier) -> Unit = { sectionMod ->
        val visaProvider = userSettings.visaCardProvider.uppercase()
        val visaSectionTitle = when (visaProvider) {
            "REDOTPAY" -> if (isArabic) "بطاقة RedotPay الدولية" else "RedotPay Visa Card"
            "BYBIT" -> if (isArabic) "بطاقة Bybit الدولية" else "Bybit Card"
            "BINANCE" -> if (isArabic) "بطاقة Binance الدولية" else "Binance Visa Card"
            else -> strings.visaCardTitle
        }
        val visaBadgeColor = when (visaProvider) {
            "REDOTPAY" -> Color(0xFFFB7185)
            "BYBIT" -> Color(0xFFF7A600)
            "BINANCE" -> Color(0xFFF0B90B)
            else -> CyanNeon
        }
        val visaBadgeText = when (visaProvider) {
            "REDOTPAY", "BYBIT", "BINANCE" -> if (isArabic) "مفعّلة" else "ACTIVE"
            else -> strings.comingSoon
        }

        Column(modifier = sectionMod) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = visaSectionTitle,
                    color = colors.textPrimary,
                    fontSize = typo.body,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(visaBadgeColor.copy(alpha = 0.15f))
                        .border(1.dp, visaBadgeColor.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = visaBadgeText,
                        color = visaBadgeColor,
                        fontSize = typo.caption,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            VisaCardRealistic(
                provider = visaProvider,
                holderName = userSettings.visaCardHolderName,
                cardNumber = userSettings.visaCardNumber,
                expiry = userSettings.visaCardExpiry,
                cvc = userSettings.visaCardCvc,
                cardTestTag = "visa_hub_card",
                binancePortfolioTotalUsd = binancePortfolioTotalUsd,
                binanceHoldingsCount = binanceHoldingsCount,
                isArabic = isArabic,
                isDark = isDark,
                onClick = {
                    if (visaProvider == "BINANCE") onOpenBinanceWallet() else onOpenVisaDetails()
                },
                onLongPressEdit = { showEditVisaCardDialog = true }
            )
        }
    }

    val thirdCardSection: @Composable (Modifier) -> Unit = { sectionMod ->
        if (userSettings.thirdCardEnabled) {
            val thirdProvider = userSettings.thirdCardProvider.uppercase()
            val thirdSectionTitle = when (thirdProvider) {
                "REDOTPAY" -> if (isArabic) "البطاقة الثالثة • RedotPay" else "Third Card • RedotPay"
                "BYBIT" -> if (isArabic) "البطاقة الثالثة • Bybit" else "Third Card • Bybit"
                "BINANCE" -> if (isArabic) "البطاقة الثالثة • Binance" else "Third Card • Binance"
                else -> if (isArabic) "البطاقة الثالثة • Visa" else "Third Card • Visa"
            }
            val thirdBadgeColor = when (thirdProvider) {
                "REDOTPAY" -> Color(0xFFFB7185)
                "BYBIT" -> Color(0xFFF7A600)
                "BINANCE" -> Color(0xFFF0B90B)
                else -> CyanNeon
            }

            Column(modifier = sectionMod) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = thirdSectionTitle,
                        color = colors.textPrimary,
                        fontSize = typo.body,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(thirdBadgeColor.copy(alpha = 0.15f))
                            .border(1.dp, thirdBadgeColor.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                            .clickable { showEditThirdCardDialog = true }
                            .testTag("edit_third_card_badge")
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isArabic) "تعديل" else "EDIT",
                            color = thirdBadgeColor,
                            fontSize = typo.caption,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                VisaCardRealistic(
                    provider = thirdProvider,
                    holderName = userSettings.thirdCardHolderName,
                    cardNumber = userSettings.thirdCardNumber,
                    expiry = userSettings.thirdCardExpiry,
                    cvc = userSettings.thirdCardCvc,
                    cardTestTag = "third_hub_card",
                    binancePortfolioTotalUsd = binancePortfolioTotalUsd,
                    binanceHoldingsCount = binanceHoldingsCount,
                    isArabic = isArabic,
                    isDark = isDark,
                    onClick = {
                        if (thirdProvider == "BINANCE") onOpenBinanceWallet() else onOpenVisaDetails()
                    },
                    onLongPressEdit = { showEditThirdCardDialog = true }
                )
            }
        } else {
            Box(modifier = sectionMod, contentAlignment = Alignment.Center) {
                AddThirdCardSlotCard(
                    isArabic = isArabic,
                    onClick = { showEditThirdCardDialog = true }
                )
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = dimens.screenPaddingH),
        verticalArrangement = Arrangement.spacedBy(dimens.spacingLg)
    ) {
        // Section Header: Contextual subtitle & Active Badges (TopAppBar already displays Cards Hub title)
        item {
            Spacer(modifier = Modifier.height(dimens.spacingXs))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.cardsHubSubtitle,
                    color = colors.textMuted,
                    fontSize = typo.bodySm,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(dimens.spacingSm))

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
                            text = if (userSettings.thirdCardEnabled) {
                                if (isArabic) "3 بطاقات نشطة" else "3 Cards Active"
                            } else {
                                strings.activeCardsCount
                            },
                            color = colors.textPrimary,
                            fontSize = typo.caption,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        if (layoutInfo.columns >= 2) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(dimens.spacingLg),
                    verticalAlignment = Alignment.Top
                ) {
                    primaryCardSection(Modifier.weight(1f))
                    secondaryCardSection(Modifier.weight(1f))
                }
            }
            item {
                thirdCardSection(
                    Modifier
                        .fillMaxWidth()
                        .widthIn(max = 540.dp)
                )
            }
        } else {
            item { primaryCardSection(Modifier.fillMaxWidth()) }
            item { secondaryCardSection(Modifier.fillMaxWidth()) }
            item { thirdCardSection(Modifier.fillMaxWidth()) }
        }

        item {
            Spacer(modifier = Modifier.height(dimens.spacingLg))
        }
    }
}

@Composable
private fun AddThirdCardSlotCard(
    isArabic: Boolean,
    onClick: () -> Unit
) {
    val colors = LiquidGlassTheme.colors
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current
    val goldAccent = Color(0xFFF0B90B)

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .testTag("add_third_card_button"),
        backgroundColor = colors.glassCard,
        borderColor = goldAccent.copy(alpha = 0.42f),
        shape = RoundedCornerShape(20.dp),
        cornerRadius = 20.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(goldAccent.copy(alpha = 0.25f), CyanNeon.copy(alpha = 0.22f))
                            )
                        )
                        .border(1.2.dp, goldAccent.copy(alpha = 0.65f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = if (isArabic) "إضافة بطاقة ثالثة" else "Add Third Card",
                        tint = goldAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isArabic) "إضافة بطاقة ثالثة" else "Add Third Card",
                        color = colors.textPrimary,
                        fontSize = typo.titleSm,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isArabic) {
                            "أضف Binance أو RedotPay أو Bybit أو Visa"
                        } else {
                            "Add Binance, RedotPay, Bybit or Visa card"
                        },
                        color = colors.textMuted,
                        fontSize = typo.caption,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(goldAccent.copy(alpha = 0.16f))
                    .border(1.dp, goldAccent.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isArabic) "+ إضافة" else "+ Add",
                    color = goldAccent,
                    fontSize = typo.caption,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
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
    bankSpec: AlgerianBankCardSpec,
    isArabic: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    onLongPressEdit: () -> Unit
) {
    val colors = LiquidGlassTheme.colors
    val strings = AppStrings.current
    val isStealth = LocalStealthMode.current
    val haptic = LocalHapticFeedback.current
    val isEdahabia = bankSpec.motif == AlgerianBankMotif.EDAHABIA_ZELLIGE
    val bankAccent = Color(bankSpec.accentHex)
    val bankSecondary = Color(bankSpec.secondaryTextHex)
    val bankGlowPrimary = Color(bankSpec.glowPrimaryHex)
    val bankGlowSecondary = Color(bankSpec.glowSecondaryHex)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val holdAnim = remember { Animatable(0f) }
    var didTriggerLongPress by remember { mutableStateOf(false) }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            didTriggerLongPress = false
            holdAnim.snapTo(0f)
            // clickable waits ~100ms TapTimeout before emitting Press inside LazyColumn,
            // so 400ms animation equals 0.5s full hold.
            holdAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 400, easing = LinearEasing)
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

    val cardScale = 1f - (holdAnim.value * 0.025f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(cardScale)
            .breathingGlow(
                glowColor = bankGlowPrimary,
                secondaryGlowColor = bankGlowSecondary,
                cornerRadius = 22.dp,
                minAlpha = if (isDark) 0.24f else 0.14f,
                maxAlpha = if (isDark) 0.52f else 0.34f,
                minScale = 0.98f,
                maxScale = 1.045f,
                durationMillis = 2600
            )
            .testTag("classic_blue_card")
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = bankAccent),
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

        // The Physical Tactile Classic Algerian Bank / Edahabia Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(222.dp)
                .shadow(
                    elevation = if (isDark) 16.dp else 12.dp,
                    shape = RoundedCornerShape(22.dp),
                    spotColor = if (isDark) Color(0xFF020617) else bankGlowPrimary.copy(alpha = 0.40f),
                    ambientColor = bankGlowPrimary.copy(alpha = 0.32f)
                )
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        colors = bankSpec.gradientHexes.map { Color(it) },
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
                                bankSecondary.copy(alpha = 0.85f),
                                bankAccent.copy(alpha = 0.55f),
                                bankGlowPrimary.copy(alpha = 0.65f),
                                bankSecondary.copy(alpha = 0.45f)
                            )
                        )
                    },
                    shape = RoundedCornerShape(22.dp)
                )
        ) {
            // Card Surface Artwork: Classic Edahabia Zellige or selected Algerian CIB Bank geometric motif
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (isEdahabia) {
                    drawClassicEdahabiaArtwork(holdProgress = holdAnim.value)
                } else {
                    drawAlgerianBankArtwork(bankSpec = bankSpec, holdProgress = holdAnim.value)
                }
            }

            // Card Inner Content (Always LTR to match physical bank card typography, kept clear of right mosaic)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = 18.dp,
                            end = if (isEdahabia) 52.dp else 18.dp,
                            top = 14.dp,
                            bottom = 14.dp
                        ),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Row: Brand Title on Left & Algérie Poste or Algerian CIB Crest on Right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = if (isEdahabia) "الذهبية" else bankSpec.nameAr,
                                color = bankSecondary,
                                fontSize = if (isEdahabia) 11.sp else 10.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = bankSpec.cardTypeLabel,
                                color = Color.White,
                                fontSize = if (isEdahabia) 20.sp else 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.4.sp,
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

                        if (isEdahabia) {
                            AlgeriePosteWhiteLogo()
                        } else {
                            AlgerianCibBankBadge(bankSpec = bankSpec)
                        }
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
                                tint = bankSecondary.copy(alpha = 0.88f)
                            )
                        }

                        // Live Total Balance Glass Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(11.dp))
                                .background(Color(0xFF090D16).copy(alpha = 0.56f))
                                .border(0.9.dp, bankAccent.copy(alpha = 0.5f), RoundedCornerShape(11.dp))
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (isArabic) "الرصيد" else "Balance",
                                    color = bankSecondary,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 10.sp
                                )
                                val hasSmsBal = userSettings.latestSmsBalanceUpdatedAt > 0L || userSettings.latestSmsBalanceAmount >= 0.0
                                val hubCardBalance = if (hasSmsBal) {
                                    userSettings.latestSmsBalanceAmount
                                } else {
                                    summary.totalBalance
                                }
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Text(
                                        text = formatDzd(hubCardBalance),
                                        color = if (hasSmsBal && hubCardBalance < 0.0) Color(0xFFFDA4AF) else Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.3.sp,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }

                    // Card Number: Embossed Pure White Monospace Digits
                    val classicDigits = userSettings.classicCardNumber.filter { it.isDigit() }
                    val classicDisplayNum = if (classicDigits.isEmpty()) {
                        if (isStealth) "••••  ••••  ••••  ••••" else bankSpec.defaultNumberPrefix
                    } else {
                        formatCardDigitsDisplay(userSettings.classicCardNumber, isStealth)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = classicDisplayNum,
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
                            val holderRaw = userSettings.classicCardHolderName.ifBlank { "CHARFEDDINE KA" }.uppercase()
                            Text(
                                text = if (isStealth) "••••  ••••" else holderRaw,
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

                        Spacer(modifier = Modifier.width(8.dp))

                        Row(
                            modifier = Modifier.padding(end = 22.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
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
                                val expFormatted = if (isStealth) {
                                    "•• / ••"
                                } else {
                                    userSettings.classicCardExpiry
                                        .ifBlank { "11/29" }
                                        .replace("/", " / ")
                                }
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
 * Premium Visa / RedotPay / Bybit Bank Card container.
 * Supports:
 * - Short Tap: Opens Visa preview details.
 * - 1-Second Press-and-Hold: Opens EditVisaCardDialog to select RedotPay, Bybit, or default Visa and customize card details.
 */
@Composable
private fun VisaCardRealistic(
    provider: String,
    holderName: String,
    cardNumber: String,
    expiry: String,
    cvc: String,
    cardTestTag: String = "visa_hub_card",
    binancePortfolioTotalUsd: Double = 0.0,
    binanceHoldingsCount: Int = 0,
    isArabic: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    onLongPressEdit: () -> Unit
) {
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
            // 400ms animation after ~100ms TapTimeout = 0.5s unified hold
            holdAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 400, easing = LinearEasing)
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

    val cardScale = 1f - (holdAnim.value * 0.025f)
    val glowPrimary = when (provider) {
        "REDOTPAY" -> Color(0xFFE11D48)
        "BYBIT" -> Color(0xFFF7A600)
        "BINANCE" -> Color(0xFFF0B90B)
        else -> Color(0xFF0284C7)
    }
    val glowSecondary = when (provider) {
        "REDOTPAY" -> Color(0xFF9F1239)
        "BYBIT" -> Color(0xFFD97706)
        "BINANCE" -> Color(0xFFB45309)
        else -> Color(0xFF0369A1)
    }

    val isDefaultLight = provider == "DEFAULT" && !isDark
    val primaryTextColor = if (isDefaultLight) Color(0xFF0F172A) else Color.White
    val secondaryTextColor = when {
        provider == "REDOTPAY" -> Color(0xFFFECDD3)
        provider == "BYBIT" -> Color(0xFF9CA3AF)
        provider == "BINANCE" -> Color(0xFFFDE68A).copy(alpha = 0.85f)
        isDark -> Color(0xFF94A3B8)
        else -> Color(0xFF64748B)
    }

    val cardBgBrush = when (provider) {
        "REDOTPAY" -> Brush.linearGradient(
            colors = listOf(
                Color(0xFFE11D48), // Iconic RedotPay vibrant crimson
                Color(0xFFBE123C), // Deep ruby red
                Color(0xFF881337), // Rich burgundy
                Color(0xFF2B0613)  // Obsidian crimson corner
            ),
            start = Offset(0f, 0f),
            end = Offset(900f, 600f)
        )
        "BYBIT" -> Brush.linearGradient(
            colors = listOf(
                Color(0xFF0B0E14), // Official Bybit matte obsidian
                Color(0xFF151B26), // Carbon slate
                Color(0xFF1E2533), // Graphite sheen
                Color(0xFF10141D)  // Deep black corner
            ),
            start = Offset(0f, 0f),
            end = Offset(900f, 600f)
        )
        "BINANCE" -> Brush.linearGradient(
            colors = listOf(
                Color(0xFF0B0E11), // Official Binance deep obsidian black
                Color(0xFF181A20), // Rich charcoal matte
                Color(0xFF1E2329), // Metallic graphite
                Color(0xFF0B0E11)  // Satin black edge
            ),
            start = Offset(0f, 0f),
            end = Offset(900f, 600f)
        )
        else -> if (isDark) {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF0F172A),
                    Color(0xFF1E293B),
                    Color(0xFF0F2B48),
                    Color(0xFF075985)
                ),
                start = Offset(0f, 0f),
                end = Offset(900f, 600f)
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFFFFFF),
                    Color(0xFFF1F5F9),
                    Color(0xFFE2E8F0),
                    Color(0xFFBAE6FD)
                ),
                start = Offset(0f, 0f),
                end = Offset(900f, 600f)
            )
        }
    }

    val cardBorderBrush = when (provider) {
        "REDOTPAY" -> Brush.linearGradient(
            colors = listOf(
                Color(0xFFFDA4AF).copy(alpha = 0.72f),
                Color(0xFF881337).copy(alpha = 0.45f),
                Color(0xFFFB7185).copy(alpha = 0.70f)
            )
        )
        "BYBIT" -> Brush.linearGradient(
            colors = listOf(
                Color(0xFFF7A600).copy(alpha = 0.78f),
                Color(0xFF1E293B).copy(alpha = 0.55f),
                Color(0xFFFBBF24).copy(alpha = 0.65f)
            )
        )
        "BINANCE" -> Brush.linearGradient(
            colors = listOf(
                Color(0xFFF0B90B).copy(alpha = 0.85f),
                Color(0xFF2B3139).copy(alpha = 0.55f),
                Color(0xFFFCD535).copy(alpha = 0.78f)
            )
        )
        else -> Brush.linearGradient(
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
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(cardScale)
            .breathingGlow(
                glowColor = glowPrimary,
                secondaryGlowColor = glowSecondary,
                cornerRadius = 22.dp,
                minAlpha = if (isDark) 0.18f else 0.12f,
                maxAlpha = if (isDark) 0.46f else 0.28f,
                minScale = 0.98f,
                maxScale = 1.04f,
                durationMillis = 2800
            )
            .testTag(cardTestTag)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = glowPrimary),
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .shadow(
                    elevation = if (isDark) 14.dp else 10.dp,
                    shape = RoundedCornerShape(22.dp),
                    spotColor = if (isDark) Color(0xFF020617) else glowPrimary.copy(alpha = 0.35f),
                    ambientColor = glowPrimary.copy(alpha = 0.25f)
                )
                .clip(RoundedCornerShape(22.dp))
                .background(cardBgBrush)
                .border(width = 1.2.dp, brush = cardBorderBrush, shape = RoundedCornerShape(22.dp))
        ) {
            // Official circulating surface artwork for RedotPay / Bybit / Default Visa
            Canvas(modifier = Modifier.fillMaxSize()) {
                when (provider) {
                    "REDOTPAY" -> {
                        // Concentric RedotPay signature rings & silky diagonal wave on right side
                        drawCircle(
                            color = Color.White.copy(alpha = 0.08f),
                            radius = size.height * 0.58f,
                            center = Offset(size.width * 0.90f, size.height * 0.44f),
                            style = Stroke(width = 24f)
                        )
                        drawCircle(
                            color = Color(0xFFFDA4AF).copy(alpha = 0.12f),
                            radius = size.height * 0.36f,
                            center = Offset(size.width * 0.90f, size.height * 0.44f),
                            style = Stroke(width = 14f)
                        )
                        val wave = Path().apply {
                            moveTo(0f, size.height * 0.65f)
                            quadraticTo(
                                size.width * 0.40f, size.height * 0.30f,
                                size.width, size.height * 0.18f
                            )
                            lineTo(size.width, 0f)
                            lineTo(0f, 0f)
                            close()
                        }
                        drawPath(
                            path = wave,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.10f),
                                    Color.Transparent
                                )
                            )
                        )
                    }
                    "BYBIT" -> {
                        // Sleek Bybit carbon grid lines & diagonal Amber-Gold light rays
                        val ray1 = Path().apply {
                            moveTo(size.width * 0.56f, 0f)
                            lineTo(size.width * 0.74f, 0f)
                            lineTo(size.width * 0.46f, size.height)
                            lineTo(size.width * 0.28f, size.height)
                            close()
                        }
                        drawPath(
                            path = ray1,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFF7A600).copy(alpha = 0.15f),
                                    Color(0xFFF7A600).copy(alpha = 0.02f)
                                )
                            )
                        )
                        val ray2 = Path().apply {
                            moveTo(size.width * 0.78f, 0f)
                            lineTo(size.width * 0.85f, 0f)
                            lineTo(size.width * 0.57f, size.height)
                            lineTo(size.width * 0.50f, size.height)
                            close()
                        }
                        drawPath(
                            path = ray2,
                            color = Color(0xFFF7A600).copy(alpha = 0.22f)
                        )
                    }
                    "BINANCE" -> {
                        // Iconic Binance interlocking geometric diamond watermark & gold filigree
                        val cx = size.width * 0.82f
                        val cy = size.height * 0.48f
                        val r = size.height * 0.48f
                        val outer = Path().apply {
                            moveTo(cx, cy - r)
                            lineTo(cx + r, cy)
                            lineTo(cx, cy + r)
                            lineTo(cx - r, cy)
                            close()
                        }
                        drawPath(
                            path = outer,
                            color = Color(0xFFF0B90B).copy(alpha = 0.14f),
                            style = Stroke(width = 8f)
                        )
                        val mid = Path().apply {
                            moveTo(cx, cy - r * 0.64f)
                            lineTo(cx + r * 0.64f, cy)
                            lineTo(cx, cy + r * 0.64f)
                            lineTo(cx - r * 0.64f, cy)
                            close()
                        }
                        drawPath(
                            path = mid,
                            color = Color(0xFFFCD535).copy(alpha = 0.11f),
                            style = Stroke(width = 4f)
                        )
                        val core = Path().apply {
                            moveTo(cx, cy - r * 0.26f)
                            lineTo(cx + r * 0.26f, cy)
                            lineTo(cx, cy + r * 0.26f)
                            lineTo(cx - r * 0.26f, cy)
                            close()
                        }
                        drawPath(
                            path = core,
                            color = Color(0xFFF0B90B).copy(alpha = 0.20f)
                        )
                    }
                    else -> {
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
                }

                // 1-second hold progress rim accent at bottom
                if (holdAnim.value > 0.02f) {
                    drawRoundRect(
                        color = glowPrimary.copy(alpha = 0.95f),
                        topLeft = Offset(0f, size.height - 6f),
                        size = Size(size.width * holdAnim.value, 6f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                }
            }

            // Card Inner Content (Always LTR like physical international cards)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Row: Provider Brand + Network Logo
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when (provider) {
                            "REDOTPAY" -> {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(Color.White),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(9.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFE11D48))
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(7.dp))
                                        Text(
                                            text = "RedotPay",
                                            color = Color.White,
                                            fontSize = 15.5.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 0.3.sp
                                        )
                                    }
                                    Text(
                                        text = "CRYPTO VISA CARD",
                                        color = secondaryTextColor,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    )
                                }
                            }
                            "BYBIT" -> {
                                Column {
                                    Text(
                                        text = buildAnnotatedString {
                                            withStyle(SpanStyle(color = Color.White)) { append("BYB") }
                                            withStyle(SpanStyle(color = Color(0xFFF7A600))) { append("I") }
                                            withStyle(SpanStyle(color = Color.White)) { append("T") }
                                        },
                                        fontSize = 17.5.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.6.sp
                                    )
                                    Text(
                                        text = "BYBIT CARD • CRYPTO DEBIT",
                                        color = Color(0xFFF7A600).copy(alpha = 0.88f),
                                        fontSize = 7.8.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.7.sp
                                    )
                                }
                            }
                            "BINANCE" -> {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Canvas(modifier = Modifier.size(18.dp)) {
                                            val cx = size.width / 2f
                                            val cy = size.height / 2f
                                            val r = size.minDimension * 0.45f
                                            val centerDiamond = Path().apply {
                                                moveTo(cx, cy - r * 0.42f)
                                                lineTo(cx + r * 0.42f, cy)
                                                lineTo(cx, cy + r * 0.42f)
                                                lineTo(cx - r * 0.42f, cy)
                                                close()
                                            }
                                            drawPath(centerDiamond, Color(0xFFF0B90B))
                                            val ringDiamond = Path().apply {
                                                moveTo(cx, cy - r)
                                                lineTo(cx + r, cy)
                                                lineTo(cx, cy + r)
                                                lineTo(cx - r, cy)
                                                close()
                                            }
                                            drawPath(ringDiamond, Color(0xFFFCD535), style = Stroke(width = 3.2f))
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "BINANCE",
                                            color = Color(0xFFF0B90B),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.6.sp
                                        )
                                    }
                                    Text(
                                        text = "BINANCE CARD • CRYPTO VISA",
                                        color = Color(0xFFFCD535).copy(alpha = 0.82f),
                                        fontSize = 7.8.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.7.sp
                                    )
                                }
                            }
                            else -> {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        SpLogoContainer(size = 20.dp, cornerRadius = 6.dp, padding = 2.dp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Vaultix",
                                            color = primaryTextColor,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 0.4.sp
                                        )
                                    }
                                    Text(
                                        text = "PREMIER DEBIT",
                                        color = secondaryTextColor,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    )
                                }
                            }
                        }

                        if (provider == "BYBIT") {
                            // Official Bybit Dual-Circle Network Mark + VISA/MC tag
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.width(34.dp).height(20.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEB001B))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .padding(start = 13.dp)
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFF79E1B).copy(alpha = 0.90f))
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "VISA",
                                color = when {
                                    provider == "BINANCE" -> Color(0xFFFCD535)
                                    isDefaultLight -> Color(0xFF1E3A8A)
                                    else -> Color.White
                                },
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                fontStyle = FontStyle.Italic,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // Middle Row: EMV Chip + Contactless waves + Status Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (provider == "BYBIT" || provider == "BINANCE") {
                                EmvChipRealistic(modifier = Modifier.size(width = 40.dp, height = 30.dp))
                            } else {
                                SilverEmvChip(modifier = Modifier.size(width = 40.dp, height = 30.dp), isDark = isDark)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            ContactlessWaves(
                                modifier = Modifier.size(18.dp),
                                tint = when (provider) {
                                    "REDOTPAY" -> Color.White.copy(alpha = 0.85f)
                                    "BYBIT" -> Color(0xFFF7A600)
                                    "BINANCE" -> Color(0xFFF0B90B)
                                    else -> if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                                }
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    when (provider) {
                                        "REDOTPAY" -> Color.White.copy(alpha = 0.16f)
                                        "BYBIT" -> Color(0xFFF7A600).copy(alpha = 0.16f)
                                        "BINANCE" -> Color(0xFFF0B90B).copy(alpha = 0.16f)
                                        else -> if (isDark) Color(0xFF0284C7).copy(alpha = 0.25f) else Color(0xFF0284C7).copy(alpha = 0.12f)
                                    }
                                )
                                .border(
                                    1.dp,
                                    when (provider) {
                                        "REDOTPAY" -> Color.White.copy(alpha = 0.45f)
                                        "BYBIT" -> Color(0xFFF7A600).copy(alpha = 0.55f)
                                        "BINANCE" -> Color(0xFFF0B90B).copy(alpha = 0.60f)
                                        else -> Color(0xFF38BDF8).copy(alpha = 0.5f)
                                    },
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (provider == "DEFAULT") {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                }
                                Text(
                                    text = when (provider) {
                                        "REDOTPAY" -> if (isArabic) "RedotPay • USD" else "RedotPay • USD Virtual"
                                        "BYBIT" -> if (isArabic) "Bybit • USDT / EUR" else "Bybit • Crypto Debit"
                                        "BINANCE" -> when {
                                            isStealth -> "Binance • $ ••••"
                                            binanceHoldingsCount > 0 -> String.format(
                                                Locale.US,
                                                "$%,.2f • %d Crypto",
                                                binancePortfolioTotalUsd,
                                                binanceHoldingsCount
                                            )
                                            else -> if (isArabic) "محفظة كريبتو • اضغط للفتح" else "Crypto Wallet • Tap to Open"
                                        }
                                        else -> if (isArabic) "قريباً • بطاقة دولية" else "Coming Soon • Multi-Currency"
                                    },
                                    color = when (provider) {
                                        "REDOTPAY" -> Color.White
                                        "BYBIT" -> Color(0xFFF7A600)
                                        "BINANCE" -> Color(0xFFF0B90B)
                                        else -> if (isDark) Color(0xFF38BDF8) else Color(0xFF0369A1)
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Card Number
                    val displayNumber = formatVisaNumberDisplay(
                        rawDigits = cardNumber,
                        provider = provider,
                        isStealth = isStealth
                    )
                    Text(
                        text = displayNumber,
                        color = primaryTextColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Bottom Row: Cardholder, Valid Thru, CVC
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "CARDHOLDER",
                                color = secondaryTextColor,
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.4.sp
                            )
                            val visaHolderRaw = holderName.ifBlank { "CHARFEDDINE KA" }.uppercase()
                            Text(
                                text = if (isStealth) "••••  ••••" else visaHolderRaw,
                                color = primaryTextColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "VALID THRU",
                                    color = secondaryTextColor,
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isStealth) "••/••" else expiry.ifBlank { "08/28" },
                                    color = primaryTextColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "CVC",
                                    color = secondaryTextColor,
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = if (isStealth || cvc.isBlank()) "•••" else cvc,
                                    color = primaryTextColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            // Floating Hold-to-Edit Progress Badge while holding 0.5s
            if (holdAnim.value > 0.10f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF0F172A).copy(alpha = 0.88f))
                        .border(1.dp, glowPrimary, RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = glowPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = AppStrings.current.holdingToEditLabel,
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
                            color = glowPrimary,
                            trackColor = Color.White.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }
    }

    // Hold-to-customize RedotPay / Bybit / Binance hint below card
    Spacer(modifier = Modifier.height(6.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onLongPressEdit)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isArabic) "اضغط مطولاً 0.5 ثانية على البطاقة لاختيار RedotPay أو Bybit أو Binance"
            else "Hold card 0.5s to choose RedotPay, Bybit or Binance",
            color = when (provider) {
                "REDOTPAY" -> Color(0xFFFB7185)
                "BYBIT" -> Color(0xFFF7A600)
                "BINANCE" -> Color(0xFFF0B90B)
                else -> CyanNeon
            },
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = when (provider) {
                "REDOTPAY" -> Color(0xFFFB7185)
                "BYBIT" -> Color(0xFFF7A600)
                "BINANCE" -> Color(0xFFF0B90B)
                else -> CyanNeon
            },
            modifier = Modifier.size(13.dp)
        )
    }
}

private fun formatVisaNumberDisplay(
    rawDigits: String,
    provider: String,
    isStealth: Boolean
): String {
    val digits = rawDigits.filter { it.isDigit() }
    if (digits.isEmpty()) {
        return when {
            isStealth -> "••••  ••••  ••••  ••••"
            provider == "BYBIT" -> "5316  ••••  ••••  8890"
            provider == "BINANCE" -> "4895  ••••  ••••  2026"
            else -> "4152  ••••  ••••  3120"
        }
    }
    if (isStealth) {
        val last4 = if (digits.length >= 4) digits.takeLast(4) else digits.padStart(4, '•')
        return "••••  ••••  ••••  $last4"
    }
    return digits.chunked(4).joinToString("  ")
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

@Composable
private fun AlgerianCibBankBadge(bankSpec: AlgerianBankCardSpec) {
    val accent = Color(bankSpec.accentHex)
    Column(horizontalAlignment = Alignment.End) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.14f))
                .border(1.dp, accent.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
                .padding(horizontal = 9.dp, vertical = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = bankSpec.shortCode,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.6.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "CIB INTERBANK",
            color = accent.copy(alpha = 0.90f),
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.7.sp
        )
    }
}

private fun DrawScope.drawAlgerianBankArtwork(
    bankSpec: AlgerianBankCardSpec,
    holdProgress: Float
) {
    val accent = Color(bankSpec.accentHex)
    val secondary = Color(bankSpec.secondaryTextHex)

    when (bankSpec.motif) {
        AlgerianBankMotif.EMERALD_ARCS,
        AlgerianBankMotif.EDAHABIA_ZELLIGE -> {
            drawCircle(
                color = accent.copy(alpha = 0.12f),
                radius = size.height * 0.62f,
                center = Offset(size.width * 0.88f, size.height * 0.42f),
                style = Stroke(width = 24f)
            )
            drawCircle(
                color = secondary.copy(alpha = 0.10f),
                radius = size.height * 0.40f,
                center = Offset(size.width * 0.88f, size.height * 0.42f),
                style = Stroke(width = 12f)
            )
        }
        AlgerianBankMotif.SOVEREIGN_RINGS -> {
            for (ring in 1..3) {
                drawCircle(
                    color = accent.copy(alpha = 0.15f - ring * 0.03f),
                    radius = size.height * (0.28f + ring * 0.18f),
                    center = Offset(size.width * 0.84f, size.height * 0.48f),
                    style = Stroke(width = 6f)
                )
            }
        }
        AlgerianBankMotif.DIAGONAL_RIBBONS -> {
            val ray1 = Path().apply {
                moveTo(size.width * 0.54f, 0f)
                lineTo(size.width * 0.74f, 0f)
                lineTo(size.width * 0.46f, size.height)
                lineTo(size.width * 0.26f, size.height)
                close()
            }
            drawPath(ray1, accent.copy(alpha = 0.11f))
            val ray2 = Path().apply {
                moveTo(size.width * 0.78f, 0f)
                lineTo(size.width * 0.88f, 0f)
                lineTo(size.width * 0.60f, size.height)
                lineTo(size.width * 0.50f, size.height)
                close()
            }
            drawPath(ray2, Color.White.copy(alpha = 0.08f))
        }
        AlgerianBankMotif.ISLAMIC_STAR_LATTICE -> {
            val cx = size.width * 0.84f
            val cy = size.height * 0.48f
            val r = size.height * 0.38f
            val star = Path()
            for (p in 0 until 16) {
                val angle = (p * PI / 8.0).toFloat()
                val rad = if (p % 2 == 0) r else r * 0.52f
                val px = cx + cos(angle) * rad
                val py = cy + sin(angle) * rad
                if (p == 0) star.moveTo(px, py) else star.lineTo(px, py)
            }
            star.close()
            drawPath(star, accent.copy(alpha = 0.16f), style = Stroke(width = 4.5f))
            drawCircle(
                color = accent.copy(alpha = 0.12f),
                radius = r * 0.34f,
                center = Offset(cx, cy),
                style = Stroke(width = 3f)
            )
        }
        AlgerianBankMotif.SPLIT_DYNAMIC -> {
            val split = Path().apply {
                moveTo(size.width * 0.58f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height)
                lineTo(size.width * 0.36f, size.height)
                close()
            }
            drawPath(split, Color.Black.copy(alpha = 0.22f))
            drawLine(
                color = accent.copy(alpha = 0.38f),
                start = Offset(size.width * 0.58f, 0f),
                end = Offset(size.width * 0.36f, size.height),
                strokeWidth = 3f
            )
        }
    }

    if (holdProgress > 0.02f) {
        drawRoundRect(
            color = accent.copy(alpha = 0.92f),
            topLeft = Offset(0f, size.height - 6f),
            size = Size(size.width * holdProgress, 6f),
            cornerRadius = CornerRadius(4f, 4f)
        )
    }
}

private tailrec fun Context.findHostActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findHostActivity()
    else -> null
}

