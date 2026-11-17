package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.ExpenseCategories
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.formatDzd
import com.example.ui.i18n.formatDzdAmount
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.LocalAdaptiveDimens
import com.example.ui.theme.LocalAdaptiveTypography
import com.example.ui.theme.LocalBreathingGlowEnabled
import com.example.ui.theme.RoseNeon
import com.example.ui.theme.breathingGlow
import kotlin.math.roundToLong

enum class SavingsTransferMode {
    DEPOSIT,
    WITHDRAW,
    SPEND
}

/**
 * Sovereign Vault Hero Card with organic breathing aurora glow, animated tabular balance,
 * Available vs Savings liquidity ratio, and interactive Savings Goal progress bar.
 */
@Composable
fun SovereignSavingsHeroCard(
    savingsBalance: Double,
    availableBalance: Double,
    savingsGoalAmount: Double,
    isArabic: Boolean,
    isStealth: Boolean,
    onEditGoalClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LiquidGlassTheme.colors
    val strings = AppStrings.current
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current
    val breathingEnabled = LocalBreathingGlowEnabled.current
    val tnumStyle = remember { TextStyle(fontFeatureSettings = "tnum") }

    val vaultGold = colors.warningColor
    val vaultEmerald = colors.successColor

    val breathPhase: Float
    if (breathingEnabled) {
        val transition = rememberInfiniteTransition(label = "savings_vault_breath")
        val anim by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "breath_val"
        )
        breathPhase = anim
    } else {
        breathPhase = 0.45f
    }

    val animatedSavingsValue by animateFloatAsState(
        targetValue = savingsBalance.toFloat().coerceAtLeast(0f),
        animationSpec = spring(stiffness = 260f, dampingRatio = 0.88f),
        label = "animated_savings"
    )

    val totalLiquidity = (availableBalance.coerceAtLeast(0.0) + savingsBalance.coerceAtLeast(0.0))
    val reserveRatio = if (totalLiquidity > 0.0) {
        (savingsBalance.coerceAtLeast(0.0) / totalLiquidity).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }

    val goalProgress = if (savingsGoalAmount > 0.0) {
        (savingsBalance / savingsGoalAmount).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }
    val animatedGoalProgress by animateFloatAsState(
        targetValue = goalProgress,
        animationSpec = spring(stiffness = 240f, dampingRatio = 0.85f),
        label = "goal_progress"
    )

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .breathingGlow(
                enabled = true,
                glowColor = vaultGold,
                secondaryGlowColor = vaultEmerald,
                cornerRadius = dimens.cornerRadiusXl,
                minAlpha = if (colors.isDark) 0.08f else 0.04f,
                maxAlpha = if (colors.isDark) 0.24f else 0.12f,
                animateContainerScale = false,
                durationMillis = 3200
            )
            .testTag("sovereign_savings_hero_card"),
        shape = RoundedCornerShape(dimens.cornerRadiusXl),
        backgroundColor = colors.glassCard,
        cornerRadius = dimens.cornerRadiusXl,
        borderColor = vaultGold.copy(alpha = if (colors.isDark) 0.30f + breathPhase * 0.18f else 0.22f + breathPhase * 0.12f),
        breathingGlow = false
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    if (!colors.isNeumorphic) {
                        val r1 = size.minDimension * (0.62f + 0.15f * breathPhase)
                        val r2 = size.minDimension * (0.48f + 0.14f * (1f - breathPhase))
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    vaultGold.copy(alpha = (if (colors.isDark) 0.18f else 0.10f) * (0.7f + 0.3f * breathPhase)),
                                    Color.Transparent
                                ),
                                center = Offset(size.width * 0.88f, size.height * 0.18f),
                                radius = r1.coerceAtLeast(1f)
                            )
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    vaultEmerald.copy(alpha = (if (colors.isDark) 0.12f else 0.06f) * (0.65f + 0.35f * breathPhase)),
                                    Color.Transparent
                                ),
                                center = Offset(size.width * 0.12f, size.height * 0.88f),
                                radius = r2.coerceAtLeast(1f)
                            )
                        )
                    }
                }
        ) {
            Column(modifier = Modifier.padding(dimens.cardPaddingLarge)) {
                // Top Row: Shield Status Pill + Goal Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(vaultGold.copy(alpha = if (colors.isDark) 0.14f else 0.10f))
                            .border(1.dp, vaultGold.copy(alpha = 0.32f), RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .graphicsLayer {
                                    val s = 0.85f + 0.30f * breathPhase
                                    scaleX = s
                                    scaleY = s
                                }
                                .clip(CircleShape)
                                .background(vaultEmerald)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = vaultGold,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isArabic) "خزنة محمية" else "Protected Vault",
                            color = vaultGold,
                            fontSize = typo.caption,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    Box(
                        modifier = Modifier
                            .defaultMinSize(minHeight = 36.dp)
                            .clip(RoundedCornerShape(50))
                            .background(colors.glassSurface)
                            .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(50))
                            .clickable(onClick = onEditGoalClick)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("savings_goal_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = if (isArabic) "تحديد هدف الادخار" else "Set Savings Goal",
                                tint = if (savingsGoalAmount > 0.0) vaultEmerald else colors.primaryNeon,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (savingsGoalAmount > 0.0) {
                                    (if (isArabic) "الهدف: " else "Goal: ") + formatDzd(savingsGoalAmount, strings.currencyCode, isStealth)
                                } else {
                                    if (isArabic) "تحديد هدف +" else "Set Goal +"
                                },
                                color = colors.textPrimary,
                                fontSize = typo.caption,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(dimens.spacingLg))

                // Centered Hero Savings Balance
                Text(
                    text = if (isArabic) "إجمالي الرصيد المدّخر" else "Total Reserved Savings",
                    color = colors.textMuted,
                    fontSize = typo.bodySm,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatDzd(animatedSavingsValue.toDouble(), strings.currencyCode, isStealth),
                    color = vaultGold,
                    fontSize = typo.hero,
                    fontWeight = FontWeight.ExtraBold,
                    style = tnumStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Goal Progress Section (if target set)
                if (savingsGoalAmount > 0.0) {
                    Spacer(modifier = Modifier.height(dimens.spacingMd))
                    val pct = (goalProgress * 100f).toInt().coerceIn(0, 100)
                    val remaining = (savingsGoalAmount - savingsBalance).coerceAtLeast(0.0)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(dimens.cornerRadiusMd))
                            .background(colors.glassSurface.copy(alpha = 0.85f))
                            .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(dimens.cornerRadiusMd))
                            .padding(dimens.cardPaddingSmall)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (remaining <= 0.0) {
                                    if (isArabic) "🎉 تم تحقيق هدف الادخار!" else "🎉 Savings Goal Reached!"
                                } else {
                                    if (isArabic) "تقدّم الهدف ($pct٪)" else "Goal Progress ($pct%)"
                                },
                                color = if (remaining <= 0.0) vaultEmerald else colors.textPrimary,
                                fontSize = typo.bodyXs,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            if (remaining > 0.0) {
                                Text(
                                    text = (if (isArabic) "المتبقي: " else "Left: ") +
                                        formatDzd(remaining, strings.currencyCode, isStealth),
                                    color = colors.textMuted,
                                    fontSize = typo.caption,
                                    style = tnumStyle,
                                    maxLines = 1
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(colors.glassBorderSubtle.copy(alpha = 0.5f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedGoalProgress.coerceAtLeast(0.02f))
                                    .height(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.horizontalGradient(listOf(vaultGold, vaultEmerald))
                                    )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(dimens.spacingMd))

                // Dual Liquidity Snapshot Strip (Available Balance + Reserve Share %)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(dimens.spacingSm)
                ) {
                    VaultMiniStatTile(
                        icon = Icons.Default.AccountBalanceWallet,
                        iconTint = CyanNeon,
                        label = if (isArabic) "المتاح للصرف" else "Available Balance",
                        value = formatDzd(availableBalance, strings.currencyCode, isStealth),
                        modifier = Modifier.weight(1f)
                    )
                    VaultMiniStatTile(
                        icon = Icons.Default.Savings,
                        iconTint = vaultEmerald,
                        label = if (isArabic) "نسبة التحصين" else "Reserve Share",
                        value = if (isArabic) "${(reserveRatio * 100).toInt()}٪ محصّن" else "${(reserveRatio * 100).toInt()}% Secured",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun VaultMiniStatTile(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val colors = LiquidGlassTheme.colors
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(dimens.cornerRadiusMd))
            .background(colors.glassSurface)
            .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(dimens.cornerRadiusMd))
            .padding(dimens.cardPaddingSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(dimens.spacingSm))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = colors.textMuted,
                fontSize = typo.caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                color = colors.textPrimary,
                fontSize = typo.bodySm,
                fontWeight = FontWeight.Bold,
                style = TextStyle(fontFeatureSettings = "tnum"),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Effortless inline Transfer Studio: Deposit to Savings, Withdraw to Available, or Spend directly.
 * Eliminates multi-step dialogs by combining mode selector, live Before->After preview, quick presets,
 * percentage slider, and numeric entry right on the main Saving workspace.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InstantSavingsTransferStudio(
    availableBalance: Double,
    savingsBalance: Double,
    isArabic: Boolean,
    isStealth: Boolean,
    onDeposit: (Double) -> Unit,
    onWithdraw: (Double) -> Unit,
    onSpend: (amount: Double, title: String, category: String, note: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LiquidGlassTheme.colors
    val strings = AppStrings.current
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current

    var activeMode by remember { mutableStateOf(SavingsTransferMode.DEPOSIT) }
    var amountInput by remember { mutableStateOf("") }
    var spendTitle by remember { mutableStateOf("") }
    var spendCategory by remember { mutableStateOf("shopping") }
    var spendNote by remember { mutableStateOf("") }

    val parsedAmount = amountInput.toDoubleOrNull() ?: 0.0
    val categories = remember { ExpenseCategories.getExpenseCategories() }

    val modeColor by animateColorAsState(
        targetValue = when (activeMode) {
            SavingsTransferMode.DEPOSIT -> colors.successColor
            SavingsTransferMode.WITHDRAW -> colors.warningColor
            SavingsTransferMode.SPEND -> colors.errorColor
        },
        animationSpec = tween(260),
        label = "transfer_mode_color"
    )

    // Source balance for percentage & slider calculations
    val sourceMax = when (activeMode) {
        SavingsTransferMode.DEPOSIT -> availableBalance.coerceAtLeast(0.0)
        SavingsTransferMode.WITHDRAW, SavingsTransferMode.SPEND -> savingsBalance.coerceAtLeast(0.0)
    }

    val sliderFraction = if (sourceMax > 0.0) {
        (parsedAmount / sourceMax).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }

    val exceedsSavings = (activeMode == SavingsTransferMode.WITHDRAW || activeMode == SavingsTransferMode.SPEND) &&
        parsedAmount > savingsBalance
    val canConfirm = parsedAmount > 0.0 && !exceedsSavings

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("instant_savings_transfer_studio"),
        shape = RoundedCornerShape(dimens.cornerRadiusXl),
        backgroundColor = colors.glassCard,
        cornerRadius = dimens.cornerRadiusXl,
        borderColor = modeColor.copy(alpha = 0.28f)
    ) {
        Column(modifier = Modifier.padding(dimens.cardPadding)) {
            // Section Title Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(modeColor.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = modeColor,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(dimens.spacingSm))
                    Column {
                        Text(
                            text = if (isArabic) "التحويل السريع" else "Quick Transfer",
                            color = colors.textPrimary,
                            fontSize = typo.titleSm,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                        Text(
                            text = when (activeMode) {
                                SavingsTransferMode.DEPOSIT -> if (isArabic) "من المتاح إلى محفظة الادخار" else "Available ➜ Savings Vault"
                                SavingsTransferMode.WITHDRAW -> if (isArabic) "من الادخار إلى الرصيد المتاح" else "Savings Vault ➜ Available"
                                SavingsTransferMode.SPEND -> if (isArabic) "صرف وتسجيل مباشر كمصروف" else "Pay & log directly to Expenses"
                            },
                            color = colors.textMuted,
                            fontSize = typo.caption,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(dimens.spacingMd))

            // 3-Mode Tactile Segmented Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(dimens.cornerRadiusMd))
                    .background(colors.glassSurface)
                    .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(dimens.cornerRadiusMd))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TransferModeSegmentTab(
                    label = if (isArabic) "إيداع" else "Deposit",
                    icon = Icons.Default.ArrowDownward,
                    selected = activeMode == SavingsTransferMode.DEPOSIT,
                    accent = colors.successColor,
                    onClick = {
                        activeMode = SavingsTransferMode.DEPOSIT
                        amountInput = ""
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("savings_deposit_tab")
                )
                TransferModeSegmentTab(
                    label = if (isArabic) "سحب" else "Withdraw",
                    icon = Icons.Default.ArrowUpward,
                    selected = activeMode == SavingsTransferMode.WITHDRAW,
                    accent = colors.warningColor,
                    onClick = {
                        activeMode = SavingsTransferMode.WITHDRAW
                        amountInput = ""
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("savings_withdraw_tab")
                )
                TransferModeSegmentTab(
                    label = if (isArabic) "صرف" else "Spend",
                    icon = Icons.Default.CreditCard,
                    selected = activeMode == SavingsTransferMode.SPEND,
                    accent = colors.errorColor,
                    onClick = {
                        activeMode = SavingsTransferMode.SPEND
                        amountInput = ""
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("savings_spend_tab")
                )
            }

            Spacer(modifier = Modifier.height(dimens.spacingMd))

            // Live Before -> After Flow Preview Bridge
            TransferFlowPreviewStrip(
                mode = activeMode,
                availableBalance = availableBalance,
                savingsBalance = savingsBalance,
                transferAmount = parsedAmount,
                currencyCode = strings.currencyCode,
                isArabic = isArabic,
                isStealth = isStealth,
                accentColor = modeColor
            )

            Spacer(modifier = Modifier.height(dimens.spacingMd))

            // Amount Input Field with Clear Action
            OutlinedTextField(
                value = amountInput,
                onValueChange = { raw ->
                    val filtered = raw.filter { ch -> ch.isDigit() || ch == '.' }
                    if (filtered.count { it == '.' } <= 1) {
                        amountInput = filtered
                    }
                },
                label = {
                    Text(
                        text = if (isArabic) "المبلغ (${strings.currencyCode})" else "Amount (${strings.currencyCode})",
                        fontSize = typo.bodySm
                    )
                },
                placeholder = {
                    Text(
                        text = "0",
                        color = colors.textMuted,
                        fontSize = typo.titleSm
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = TextStyle(
                    fontSize = typo.title,
                    fontWeight = FontWeight.ExtraBold,
                    fontFeatureSettings = "tnum"
                ),
                trailingIcon = {
                    if (amountInput.isNotEmpty()) {
                        IconButton(
                            onClick = { amountInput = "" },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = if (isArabic) "مسح" else "Clear",
                                tint = colors.textMuted
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("savings_amount_input"),
                shape = RoundedCornerShape(dimens.cornerRadiusMd),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    focusedBorderColor = modeColor,
                    unfocusedBorderColor = colors.glassBorderSubtle,
                    focusedLabelColor = modeColor,
                    unfocusedLabelColor = colors.textMuted
                )
            )

            // Interactive Slider + Quick Percentage Pills when sourceMax > 0
            if (sourceMax > 0.0) {
                Spacer(modifier = Modifier.height(dimens.spacingSm))

                Slider(
                    value = sliderFraction,
                    onValueChange = { fraction ->
                        val step = 100.0
                        val raw = sourceMax * fraction
                        val snapped = if (fraction >= 0.99f) {
                            sourceMax
                        } else {
                            ((raw / step).roundToLong() * step).coerceIn(0.0, sourceMax)
                        }
                        amountInput = if (snapped <= 0.0) "" else snapped.toLong().toString()
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = modeColor,
                        activeTrackColor = modeColor,
                        inactiveTrackColor = colors.glassBorderSubtle
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(dimens.spacingXs + 2.dp)
                ) {
                    listOf(0.10, 0.25, 0.50, 1.0).forEach { pct ->
                        val pctLabel = if (pct == 1.0) {
                            if (isArabic) "الكل" else "All"
                        } else {
                            "${(pct * 100).toInt()}%"
                        }
                        val targetAmt = (sourceMax * pct).toLong()
                        val isSelected = targetAmt > 0 && parsedAmount.toLong() == targetAmt
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 40.dp)
                                .clip(RoundedCornerShape(dimens.cornerRadiusSm))
                                .background(if (isSelected) modeColor.copy(alpha = 0.20f) else colors.glassSurface)
                                .border(
                                    1.dp,
                                    if (isSelected) modeColor else colors.glassBorderSubtle,
                                    RoundedCornerShape(dimens.cornerRadiusSm)
                                )
                                .clickable {
                                    if (targetAmt > 0) amountInput = targetAmt.toString()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = pctLabel,
                                color = if (isSelected) modeColor else colors.textSecondary,
                                fontSize = typo.bodyXs,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(dimens.spacingSm))

            // Quick-Add DZD Preset Chips (cumulative taps for effortless amount building)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(dimens.spacingXs + 2.dp)
            ) {
                val quickSteps = listOf(1000.0, 2000.0, 5000.0, 10000.0, 20000.0)
                quickSteps.forEach { step ->
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minHeight = 40.dp)
                            .clip(RoundedCornerShape(dimens.cornerRadiusSm))
                            .background(colors.glassSurface)
                            .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(dimens.cornerRadiusSm))
                            .clickable {
                                val next = parsedAmount + step
                                val capped = if (activeMode != SavingsTransferMode.DEPOSIT && savingsBalance > 0.0) {
                                    next.coerceAtMost(savingsBalance)
                                } else {
                                    next
                                }
                                amountInput = capped.toLong().toString()
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+${formatDzdAmount(step)}",
                            color = modeColor,
                            fontSize = typo.bodyXs,
                            fontWeight = FontWeight.Bold,
                            style = TextStyle(fontFeatureSettings = "tnum"),
                            maxLines = 1
                        )
                    }
                }
            }

            // Expandable Spend Details when Spend mode is active
            AnimatedVisibility(
                visible = activeMode == SavingsTransferMode.SPEND,
                enter = fadeIn(tween(220)) + expandVertically(tween(240)),
                exit = fadeOut(tween(180)) + shrinkVertically(tween(200))
            ) {
                Column(modifier = Modifier.padding(top = dimens.spacingMd)) {
                    OutlinedTextField(
                        value = spendTitle,
                        onValueChange = { spendTitle = it },
                        label = { Text(if (isArabic) "سبب الصرف (اختياري)" else "Expense Title (Optional)", fontSize = typo.bodyXs) },
                        placeholder = { Text(if (isArabic) "مثال: صيانة، شراء طارئ..." else "e.g. Repair, Emergency...", fontSize = typo.bodySm) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(dimens.cornerRadiusMd),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            focusedBorderColor = RoseNeon,
                            unfocusedBorderColor = colors.glassBorderSubtle
                        )
                    )

                    Spacer(modifier = Modifier.height(dimens.spacingSm))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories) { cat ->
                            val isSelected = cat.id == spendCategory
                            Box(
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 38.dp)
                                    .clip(RoundedCornerShape(dimens.cornerRadiusSm))
                                    .background(if (isSelected) RoseNeon.copy(alpha = 0.20f) else colors.glassSurface)
                                    .border(
                                        1.dp,
                                        if (isSelected) RoseNeon else colors.glassBorderSubtle,
                                        RoundedCornerShape(dimens.cornerRadiusSm)
                                    )
                                    .clickable { spendCategory = cat.id }
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = cat.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) RoseNeon else colors.textMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = strings.categoryName(cat.id),
                                        color = if (isSelected) RoseNeon else colors.textPrimary,
                                        fontSize = typo.bodyXs,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(dimens.spacingSm))

                    OutlinedTextField(
                        value = spendNote,
                        onValueChange = { spendNote = it },
                        label = { Text(if (isArabic) "ملاحظة (اختياري)" else "Note (Optional)", fontSize = typo.bodyXs) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(dimens.cornerRadiusMd),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            focusedBorderColor = colors.primaryNeon,
                            unfocusedBorderColor = colors.glassBorderSubtle
                        )
                    )
                }
            }

            // Inline Validation or Advisory Hint
            if (exceedsSavings) {
                Spacer(modifier = Modifier.height(dimens.spacingSm))
                Text(
                    text = if (isArabic) {
                        "رصيد الادخار الحالي (${formatDzdAmount(savingsBalance)} ${strings.currencyCode}) لا يكفي لهذا المبلغ."
                    } else {
                        "Current savings balance (${formatDzdAmount(savingsBalance)} ${strings.currencyCode}) is insufficient."
                    },
                    color = RoseNeon,
                    fontSize = typo.bodyXs,
                    fontWeight = FontWeight.Medium
                )
            } else if (activeMode == SavingsTransferMode.DEPOSIT && parsedAmount > availableBalance && availableBalance > 0) {
                Spacer(modifier = Modifier.height(dimens.spacingSm))
                Text(
                    text = if (isArabic) {
                        "ملاحظة: المبلغ أكبر من الرصيد المتاح الحالي (${formatDzdAmount(availableBalance)} ${strings.currencyCode})."
                    } else {
                        "Note: Amount exceeds current available balance (${formatDzdAmount(availableBalance)} ${strings.currencyCode})."
                    },
                    color = AmberNeon,
                    fontSize = typo.bodyXs,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(dimens.spacingMd))

            // Primary Confirm Button (minimum 50dp touch height)
            val confirmLabel = when {
                parsedAmount <= 0.0 -> when (activeMode) {
                    SavingsTransferMode.DEPOSIT -> if (isArabic) "أدخل مبلغ الإيداع" else "Enter Deposit Amount"
                    SavingsTransferMode.WITHDRAW -> if (isArabic) "أدخل مبلغ السحب" else "Enter Withdraw Amount"
                    SavingsTransferMode.SPEND -> if (isArabic) "أدخل مبلغ الصرف" else "Enter Spend Amount"
                }
                else -> {
                    val amtText = "${formatDzdAmount(parsedAmount)} ${strings.currencyCode}"
                    when (activeMode) {
                        SavingsTransferMode.DEPOSIT -> if (isArabic) "تحويل $amtText للادخار" else "Deposit $amtText to Savings"
                        SavingsTransferMode.WITHDRAW -> if (isArabic) "سحب $amtText للمتاح" else "Withdraw $amtText to Available"
                        SavingsTransferMode.SPEND -> if (isArabic) "صرف $amtText من الادخار" else "Spend $amtText from Savings"
                    }
                }
            }

            val buttonContainerColor = if (canConfirm) modeColor else colors.glassSurface
            val buttonContentColor = if (canConfirm) {
                if (activeMode == SavingsTransferMode.SPEND) Color.White else Color(0xFF0F172A)
            } else {
                colors.textMuted
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 52.dp)
                    .clip(RoundedCornerShape(dimens.cornerRadiusMd))
                    .background(buttonContainerColor)
                    .then(
                        if (!canConfirm) {
                            Modifier.border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(dimens.cornerRadiusMd))
                        } else Modifier
                    )
                    .clickable(enabled = canConfirm) {
                        when (activeMode) {
                            SavingsTransferMode.DEPOSIT -> onDeposit(parsedAmount)
                            SavingsTransferMode.WITHDRAW -> onWithdraw(parsedAmount)
                            SavingsTransferMode.SPEND -> {
                                val resolvedTitle = spendTitle.ifBlank { strings.categoryName(spendCategory) }
                                onSpend(parsedAmount, resolvedTitle, spendCategory, spendNote)
                                spendTitle = ""
                                spendNote = ""
                            }
                        }
                        amountInput = ""
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .testTag("savings_confirm_transfer_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = buttonContentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = confirmLabel,
                        color = buttonContentColor,
                        fontSize = typo.bodyLg,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun TransferModeSegmentTab(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LiquidGlassTheme.colors
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current

    val bgColor by animateColorAsState(
        targetValue = if (selected) accent.copy(alpha = 0.22f) else Color.Transparent,
        animationSpec = tween(220),
        label = "tab_bg"
    )

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 46.dp)
            .clip(RoundedCornerShape(dimens.cornerRadiusSm))
            .background(bgColor)
            .then(
                if (selected) {
                    Modifier.border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(dimens.cornerRadiusSm))
                } else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) accent else colors.textMuted,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = if (selected) accent else colors.textSecondary,
                fontSize = typo.bodyXs,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TransferFlowPreviewStrip(
    mode: SavingsTransferMode,
    availableBalance: Double,
    savingsBalance: Double,
    transferAmount: Double,
    currencyCode: String,
    isArabic: Boolean,
    isStealth: Boolean,
    accentColor: Color
) {
    val colors = LiquidGlassTheme.colors
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current

    val isDeposit = mode == SavingsTransferMode.DEPOSIT
    val fromLabel = if (isDeposit) {
        if (isArabic) "من المتاح" else "From Available"
    } else {
        if (isArabic) "من الادخار" else "From Savings"
    }
    val toLabel = when (mode) {
        SavingsTransferMode.DEPOSIT -> if (isArabic) "إلى الادخار" else "To Savings"
        SavingsTransferMode.WITHDRAW -> if (isArabic) "إلى المتاح" else "To Available"
        SavingsTransferMode.SPEND -> if (isArabic) "إلى المصاريف" else "To Expenses"
    }

    val fromBefore = if (isDeposit) availableBalance else savingsBalance
    val fromAfter = if (transferAmount > 0.0) (fromBefore - transferAmount) else fromBefore
    val toBefore = when (mode) {
        SavingsTransferMode.DEPOSIT -> savingsBalance
        SavingsTransferMode.WITHDRAW -> availableBalance
        SavingsTransferMode.SPEND -> 0.0
    }
    val toAfter = toBefore + transferAmount

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimens.cornerRadiusMd))
            .background(colors.glassSurface.copy(alpha = 0.78f))
            .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(dimens.cornerRadiusMd))
            .padding(horizontal = dimens.cardPaddingSmall, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fromLabel,
                color = colors.textMuted,
                fontSize = typo.caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (transferAmount > 0.0) {
                    formatDzd(fromAfter, currencyCode, isStealth)
                } else {
                    formatDzd(fromBefore, currencyCode, isStealth)
                },
                color = if (transferAmount > 0.0) colors.textPrimary else colors.textSecondary,
                fontSize = typo.bodySm,
                fontWeight = FontWeight.Bold,
                style = TextStyle(fontFeatureSettings = "tnum"),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(15.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = toLabel,
                color = colors.textMuted,
                fontSize = typo.caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (mode == SavingsTransferMode.SPEND) {
                    if (transferAmount > 0.0) formatDzd(transferAmount, currencyCode, isStealth) else "—"
                } else if (transferAmount > 0.0) {
                    formatDzd(toAfter, currencyCode, isStealth)
                } else {
                    formatDzd(toBefore, currencyCode, isStealth)
                },
                color = if (transferAmount > 0.0) accentColor else colors.textSecondary,
                fontSize = typo.bodySm,
                fontWeight = FontWeight.Bold,
                style = TextStyle(fontFeatureSettings = "tnum"),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Clean glass modal for setting or clearing a personal Savings Target Goal.
 */
@Composable
fun SavingsGoalDialog(
    currentGoal: Double,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    val colors = LiquidGlassTheme.colors
    val strings = AppStrings.current
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current
    var goalInput by remember {
        mutableStateOf(if (currentGoal > 0.0) currentGoal.toLong().toString() else "")
    }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(dimens.cornerRadiusXl),
            backgroundColor = colors.glassCard,
            cornerRadius = dimens.cornerRadiusXl
        ) {
            Column(modifier = Modifier.padding(dimens.cardPaddingLarge)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AmberNeon.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = null,
                            tint = AmberNeon,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isArabic) "هدف الادخار المستقبلي" else "Savings Target Goal",
                            color = colors.textPrimary,
                            fontSize = typo.titleSm,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isArabic) "تابع نسبة وصولك للهدف بوضوح" else "Track your progress visually",
                            color = colors.textMuted,
                            fontSize = typo.caption
                        )
                    }
                }

                Spacer(modifier = Modifier.height(dimens.spacingMd))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(50000.0, 100000.0, 250000.0, 500000.0, 1000000.0).forEach { preset ->
                        Box(
                            modifier = Modifier
                                .defaultMinSize(minHeight = 38.dp)
                                .clip(RoundedCornerShape(dimens.cornerRadiusSm))
                                .background(colors.glassSurface)
                                .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(dimens.cornerRadiusSm))
                                .clickable { goalInput = preset.toLong().toString() }
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = formatDzdAmount(preset),
                                color = AmberNeon,
                                fontSize = typo.bodyXs,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(dimens.spacingMd))

                OutlinedTextField(
                    value = goalInput,
                    onValueChange = { goalInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text(if (isArabic) "مبلغ الهدف (${strings.currencyCode})" else "Goal Amount (${strings.currencyCode})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(dimens.cornerRadiusMd),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = AmberNeon,
                        unfocusedBorderColor = colors.glassBorderSubtle
                    )
                )

                Spacer(modifier = Modifier.height(dimens.spacingLg))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(dimens.spacingSm)
                ) {
                    if (currentGoal > 0.0) {
                        GlassButton(
                            text = if (isArabic) "إلغاء الهدف" else "Remove",
                            containerColor = colors.glassSurface,
                            contentColor = RoseNeon,
                            modifier = Modifier.weight(1f),
                            onClick = { onConfirm(0.0) }
                        )
                    } else {
                        GlassButton(
                            text = strings.cancel,
                            containerColor = colors.glassSurface,
                            contentColor = colors.textMuted,
                            modifier = Modifier.weight(1f),
                            onClick = onDismiss
                        )
                    }
                    GlassButton(
                        text = if (isArabic) "حفظ الهدف" else "Save Goal",
                        containerColor = AmberNeon,
                        contentColor = Color(0xFF0F172A),
                        modifier = Modifier.weight(1.2f),
                        onClick = {
                            val amt = goalInput.toDoubleOrNull() ?: 0.0
                            onConfirm(amt)
                        }
                    )
                }
            }
        }
    }
}
