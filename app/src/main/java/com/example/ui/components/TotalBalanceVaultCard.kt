package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.UserSettings
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.LocalStealthMode
import com.example.ui.i18n.formatDzd
import com.example.ui.i18n.formatDzdAmount
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.NeuBurntCoral
import com.example.ui.theme.RoseNeon
import com.example.viewmodel.FinancialSummary
import java.util.Calendar

/**
 * Advanced Total Balance & Money Vault Card.
 * Incorporates:
 * 1. Dynamic Cash Flow Glow: ambient shifting background aura (Surplus = Emerald/Cyan, Balanced = Sapphire/Violet, Deficit = Amber/Coral).
 * 2. Burn Rate & Days Remaining Indicator: calculates daily living expenses burn rate and balance runway.
 * 3. Quick Vault / Safe Pocket: allows locking away a designated slice of funds so they are excluded from daily spendable balance.
 */
@Composable
fun TotalBalanceVaultCard(
    summary: FinancialSummary,
    userSettings: UserSettings,
    onLockToVault: (Double) -> Unit,
    onUnlockFromVault: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LiquidGlassTheme.colors
    val strings = AppStrings.current
    val isArabic = userSettings.language == "ar"
    val isStealth = LocalStealthMode.current

    val lockedAmount = userSettings.vaultLockedAmount
    val grossBalance = summary.totalBalance
    val spendableBalance = (grossBalance - lockedAmount).coerceAtLeast(0.0)

    // Calculate Daily Burn Rate & Days Remaining
    val cal = Calendar.getInstance()
    val daysElapsed = cal.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
    val dailyBurnRate = if (summary.monthlyExpenses > 0) summary.monthlyExpenses / daysElapsed else 0.0
    val daysRemaining = if (dailyBurnRate > 0) (spendableBalance / dailyBurnRate).toInt() else 999

    // Cash flow ratio calculations for ambient dynamic glow
    // Surplus: monthlyIncome > monthlyExpenses * 1.25
    // Deficit: monthlyExpenses > monthlyIncome
    // Equilibrium: roughly equal
    val isSurplus = summary.monthlyIncome > summary.monthlyExpenses * 1.20
    val isDeficit = summary.monthlyExpenses > summary.monthlyIncome && summary.monthlyExpenses > 0

    val glowColor1 = when {
        isSurplus -> EmeraldNeon
        isDeficit -> RoseNeon
        else -> CyanNeon
    }
    val glowColor2 = when {
        isSurplus -> CyanNeon
        isDeficit -> AmberNeon
        else -> Color(0xFF6366F1) // Indigo/Violet
    }

    // Infinite breathing animation for ambient glow
    val infiniteTransition = rememberInfiniteTransition(label = "cashflow_glow")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.70f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    // Safe Pocket expansion state & custom deposit modal
    var isVaultExpanded by remember { mutableStateOf(false) }
    var showCustomDepositDialog by remember { mutableStateOf(false) }
    var customDepositInput by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        // 3. Dynamic Cash Flow Ambient Glow (Rendered behind the card in Liquid Glass mode only)
        if (!colors.isNeumorphic) {
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        alpha = glowPulse
                        scaleX = 1.05f
                        scaleY = 1.05f
                    }
            ) {
                val centerOffset = Offset(size.width / 2f, size.height / 2f)
                val glowRadius = size.maxDimension * 0.65f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor1.copy(alpha = 0.40f),
                            glowColor2.copy(alpha = 0.22f),
                            Color.Transparent
                        ),
                        center = centerOffset,
                        radius = glowRadius
                    ),
                    radius = glowRadius,
                    center = centerOffset
                )
            }
        }

        // Main Total Balance Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = colors.glassCard,
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Top Header Row with Cash Flow Status Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (lockedAmount > 0) {
                                if (isArabic) "الرصيد المتاح للصرف" else "Daily Spendable Balance"
                            } else {
                                strings.totalBalance
                            },
                            color = colors.textSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (lockedAmount > 0) {
                            Text(
                                text = if (isArabic) "مستثنى منه مدخرات الجيب الآمن" else "Safe Pocket reserve excluded",
                                color = EmeraldNeon,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Cash Flow Phase Indicator Badge
                    val flowTagLabel = when {
                        isSurplus -> if (isArabic) "تدفق مالي إيجابي ↗" else "Surplus Flow ↗"
                        isDeficit -> if (isArabic) "حرق رصيد مرتفع ↘" else "High Burn ↘"
                        else -> if (isArabic) "تدفق متوازن ⇄" else "Balanced Flow ⇄"
                    }
                    val flowTagColor = when {
                        isSurplus -> EmeraldNeon
                        isDeficit -> RoseNeon
                        else -> CyanNeon
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(flowTagColor.copy(alpha = 0.16f))
                            .border(1.dp, flowTagColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = flowTagLabel,
                            color = flowTagColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Prominent Active Balance (Spendable Amount)
                Text(
                    text = formatDzd(spendableBalance, strings.currencyCode, isStealth),
                    color = colors.textPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )

                // If Safe Pocket is locked, show net gross breakdown chip
                if (lockedAmount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.glassSurface)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = AmberNeon,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = (if (isArabic) "إجمالي المحفظة: " else "Gross Vault Net Worth: ") +
                                    formatDzd(grossBalance, strings.currencyCode, isStealth),
                            color = colors.textMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 1. Burn Rate & Days Remaining Indicator Widget
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.glassSurface)
                        .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                daysRemaining < 7 -> RoseNeon.copy(alpha = 0.2f)
                                                daysRemaining < 15 -> AmberNeon.copy(alpha = 0.2f)
                                                else -> CyanNeon.copy(alpha = 0.2f)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = when {
                                            daysRemaining < 7 -> RoseNeon
                                            daysRemaining < 15 -> AmberNeon
                                            else -> CyanNeon
                                        },
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (isArabic) "معدل الحرق اليومي" else "Daily Burn Rate",
                                        color = colors.textMuted,
                                        fontSize = 10.5.sp
                                    )
                                    Text(
                                        text = if (dailyBurnRate > 0) {
                                            "${formatDzdAmount(dailyBurnRate)} " + (if (isArabic) "دج/يوم" else "DZD/day")
                                        } else {
                                            if (isArabic) "0 دج/يوم" else "0 DZD/day"
                                        },
                                        color = colors.textPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Days Remaining Badge
                            val runwayColor = when {
                                daysRemaining >= 30 -> EmeraldNeon
                                daysRemaining >= 15 -> CyanNeon
                                daysRemaining >= 7 -> AmberNeon
                                else -> RoseNeon
                            }
                            val runwayText = when {
                                daysRemaining >= 90 -> if (isArabic) "يكفي 90+ يوماً" else "90+ Days Runway"
                                daysRemaining > 0 -> if (isArabic) "يكفي $daysRemaining يوماً" else "$daysRemaining Days Runway"
                                else -> if (isArabic) "يحتاج تعزيز" else "Needs Reload"
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(runwayColor.copy(alpha = 0.16f))
                                    .border(1.dp, runwayColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = runwayText,
                                    color = runwayColor,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Visual Runway Meter
                        val runwayFraction = (daysRemaining.toFloat() / 35f).coerceIn(0.05f, 1f)
                        LinearProgressIndicator(
                            progress = { runwayFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = when {
                                daysRemaining >= 30 -> EmeraldNeon
                                daysRemaining >= 15 -> CyanNeon
                                daysRemaining >= 7 -> AmberNeon
                                else -> RoseNeon
                            },
                            trackColor = colors.glassCard
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Monthly Income vs Expense Mini Stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Income
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.glassSurface)
                            .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldNeon.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = EmeraldNeon,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = strings.monthlyIncome,
                                    color = colors.textMuted,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = formatDzd(summary.monthlyIncome, strings.currencyCode, isStealth),
                                    color = EmeraldNeon,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Expense
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.glassSurface)
                            .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(RoseNeon.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingDown,
                                    contentDescription = null,
                                    tint = RoseNeon,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = strings.monthlyExpense,
                                    color = colors.textMuted,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = formatDzd(summary.monthlyExpenses, strings.currencyCode, isStealth),
                                    color = RoseNeon,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Quick Vault / Reserve Pocket Section (الجيب الآمن)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (lockedAmount > 0) AmberNeon.copy(alpha = 0.10f) else colors.glassSurface
                        )
                        .border(
                            1.dp,
                            if (lockedAmount > 0) AmberNeon.copy(alpha = 0.45f) else colors.glassBorderSubtle,
                            RoundedCornerShape(18.dp)
                        )
                        .clickable { isVaultExpanded = !isVaultExpanded }
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (lockedAmount > 0) AmberNeon.copy(alpha = 0.22f) else colors.primaryNeon.copy(alpha = 0.16f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (lockedAmount > 0) Icons.Default.Lock else Icons.Default.Savings,
                                        contentDescription = null,
                                        tint = if (lockedAmount > 0) AmberNeon else colors.primaryNeon,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isArabic) "الجيب الآمن / الخزنة السريعة" else "Safe Pocket / Quick Vault",
                                        color = colors.textPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (lockedAmount > 0) {
                                            (if (isArabic) "محجوز عن الصرف اليومي: " else "Locked from spending: ") +
                                                    formatDzd(lockedAmount, strings.currencyCode, isStealth)
                                        } else {
                                            if (isArabic) "احجز جزءاً من مدخراتك لمنع الصرف غير المقصود" else "Lock savings to prevent accidental spending"
                                        },
                                        color = if (lockedAmount > 0) AmberNeon else colors.textMuted,
                                        fontSize = 11.sp,
                                        fontWeight = if (lockedAmount > 0) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }

                            // Expand / Collapse Chevron Tag
                            Text(
                                text = if (isVaultExpanded) (if (isArabic) "إغلاق ▲" else "Hide ▲") else (if (isArabic) "إدارة ▼" else "Manage ▼"),
                                color = colors.primaryNeon,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Expandable Vault Management Drawer
                        AnimatedVisibility(
                            visible = isVaultExpanded,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(modifier = Modifier.padding(top = 14.dp)) {
                                Text(
                                    text = if (isArabic) "إيداع سريع في الجيب الآمن (خصم من الرصيد المتاح)" else "Quick Lock Presets (Hides from daily spendable)",
                                    color = colors.textSecondary,
                                    fontSize = 11.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Quick Deposit Preset Chips
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(5000.0, 10000.0, 20000.0).forEach { amount ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(AmberNeon.copy(alpha = 0.16f))
                                                .border(1.dp, AmberNeon.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                                .clickable { onLockToVault(amount) }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "+${formatDzdAmount(amount)}",
                                                color = AmberNeon,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    // Custom amount button
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(colors.primaryNeon.copy(alpha = 0.16f))
                                            .border(1.dp, colors.primaryNeon.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                            .clickable { showCustomDepositDialog = true }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (isArabic) "مخصص" else "Custom",
                                            color = colors.primaryNeon,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                if (lockedAmount > 0) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    // Withdraw / Release buttons
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Release 5000 DZD
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(colors.glassCard)
                                                .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(12.dp))
                                                .clickable { onUnlockFromVault(5000.0) }
                                                .padding(vertical = 7.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (isArabic) "سحب 5,000 دج للمتاح" else "Release 5,000 DZD",
                                                color = colors.textPrimary,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        // Release All
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(EmeraldNeon.copy(alpha = 0.15f))
                                                .border(1.dp, EmeraldNeon.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                                .clickable { onUnlockFromVault(lockedAmount) }
                                                .padding(vertical = 7.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (isArabic) "سحب الكل للمتاح" else "Release All to Spend",
                                                color = EmeraldNeon,
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
            }
        }
    }

    // Custom Amount Deposit Modal
    if (showCustomDepositDialog) {
        Dialog(onDismissRequest = { showCustomDepositDialog = false }) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(24.dp),
                backgroundColor = colors.dialogBackground
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AmberNeon.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = AmberNeon,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isArabic) "حجز مبلغ في الجيب الآمن" else "Lock Savings in Safe Pocket",
                            color = colors.textPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isArabic) "أدخل المبلغ المراد حجزه بعيداً عن الرصيد المتاح للصرف لتفادي الإنفاق العفوي"
                        else "Enter amount to isolate from your daily spendable balance to prevent impulse buys",
                        color = colors.textMuted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = customDepositInput,
                        onValueChange = { customDepositInput = it.filter { char -> char.isDigit() } },
                        label = { Text(if (isArabic) "المبلغ (دج)" else "Amount (DZD)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmberNeon,
                            unfocusedBorderColor = colors.glassBorderSubtle,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        GlassButton(
                            text = strings.cancel,
                            containerColor = colors.glassCard,
                            contentColor = colors.textSecondary,
                            onClick = { showCustomDepositDialog = false }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        GlassButton(
                            text = if (isArabic) "حجز في الجيب الآمن 🔒" else "Lock in Pocket 🔒",
                            containerColor = AmberNeon,
                            contentColor = Color(0xFF0F172A),
                            onClick = {
                                val amount = customDepositInput.toDoubleOrNull() ?: 0.0
                                if (amount > 0) {
                                    onLockToVault(amount)
                                }
                                customDepositInput = ""
                                showCustomDepositDialog = false
                            }
                        )
                    }
                }
            }
        }
    }
}
