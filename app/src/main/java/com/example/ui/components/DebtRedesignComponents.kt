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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DebtEntity
import com.example.data.DebtType
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.ArabicStrings
import com.example.ui.i18n.StringsBundle
import com.example.ui.i18n.formatDate
import com.example.ui.i18n.formatDzd
import com.example.ui.i18n.isolateLtr
import com.example.ui.theme.AdaptiveDimens
import com.example.ui.theme.AdaptiveTypography
import com.example.ui.theme.DebtColors
import com.example.ui.theme.LiquidGlassColors
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.LocalBreathingGlowEnabled
import com.example.ui.theme.breathingGlow
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

enum class DebtStatusFilter {
    ALL,
    ACTIVE,
    OVERDUE,
    SETTLED
}

enum class DebtSortMode {
    SOONEST,
    HIGHEST,
    RECENT
}

@Composable
fun DebtBreathingHeroCard(
    activeTab: DebtType,
    onTabSelect: (DebtType) -> Unit,
    payableRemaining: Double,
    payableCount: Int,
    receivableRemaining: Double,
    receivableCount: Int,
    totalPaidInTab: Double,
    totalOriginalInTab: Double,
    tabProgress: Float,
    overdueCountInTab: Int,
    settledCountInTab: Int,
    strings: StringsBundle,
    colors: LiquidGlassColors,
    dimens: AdaptiveDimens,
    typo: AdaptiveTypography,
    onAddDebtClick: () -> Unit,
    onClearSettledClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = colors.isDark
    val isArabic = strings is ArabicStrings
    val breathingEnabled = LocalBreathingGlowEnabled.current

    val payableColor = DebtColors.payable(isDark)
    val receivableColor = DebtColors.receivable(isDark)
    val settledColor = DebtColors.settled(isDark)
    val dangerColor = DebtColors.danger(isDark)

    val targetAccent = if (activeTab == DebtType.OWED_BY_ME) payableColor else receivableColor
    val accentColor by animateColorAsState(
        targetValue = targetAccent,
        animationSpec = tween(320),
        label = "hero_accent"
    )

    // Organic dual-phase breathing cycle
    val breathPhase: Float
    val secondaryPhase: Float
    if (breathingEnabled) {
        val transition = rememberInfiniteTransition(label = "debt_hero_breathing")
        val primary by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "primary_breath"
        )
        val secondary by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "secondary_breath"
        )
        breathPhase = primary
        secondaryPhase = secondary
    } else {
        breathPhase = 0.45f
        secondaryPhase = 0.45f
    }

    val activeRemaining = if (activeTab == DebtType.OWED_BY_ME) payableRemaining else receivableRemaining
    val netBalance = receivableRemaining - payableRemaining

    val animatedProgress by animateFloatAsState(
        targetValue = tabProgress.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = 280f, dampingRatio = 0.85f),
        label = "tab_progress"
    )

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .breathingGlow(
                enabled = true,
                glowColor = accentColor,
                secondaryGlowColor = settledColor,
                cornerRadius = 24.dp,
                minAlpha = if (isDark) 0.08f else 0.05f,
                maxAlpha = if (isDark) 0.25f else 0.14f,
                minScale = 0.985f,
                maxScale = 1.04f,
                animateContainerScale = false,
                durationMillis = 3000
            ),
        shape = RoundedCornerShape(24.dp),
        backgroundColor = colors.glassCard,
        borderColor = accentColor.copy(alpha = if (isDark) 0.28f + breathPhase * 0.20f else 0.20f + breathPhase * 0.14f),
        breathingGlow = false
    ) {
        val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    // Ambient breathing aura orbs inside the hero card
                    val orb1Radius = size.minDimension * (0.65f + 0.16f * breathPhase)
                    val orb2Radius = size.minDimension * (0.52f + 0.14f * (1f - secondaryPhase))

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = (if (isDark) 0.16f else 0.10f) * (0.7f + 0.3f * breathPhase)),
                                Color.Transparent
                            ),
                            center = Offset(size.width * (if (isRtl) 0.14f else 0.86f), size.height * 0.18f),
                            radius = orb1Radius.coerceAtLeast(1f)
                        )
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                settledColor.copy(alpha = (if (isDark) 0.10f else 0.06f) * (0.6f + 0.4f * secondaryPhase)),
                                Color.Transparent
                            ),
                            center = Offset(size.width * (if (isRtl) 0.86f else 0.14f), size.height * 0.85f),
                            radius = orb2Radius.coerceAtLeast(1f)
                        )
                    )
                }
        ) {
            Column(modifier = Modifier.padding(dimens.cardPadding)) {
                // Top Row: Breathing Live Pill + Net Debt Position
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Live breathing title pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(accentColor.copy(alpha = if (isDark) 0.14f else 0.10f))
                            .border(1.dp, accentColor.copy(alpha = 0.28f), RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .graphicsLayer {
                                        val s = 0.85f + breathPhase * 0.75f
                                        scaleX = s
                                        scaleY = s
                                        alpha = 0.55f - breathPhase * 0.35f
                                    }
                                    .clip(CircleShape)
                                    .background(accentColor)
                            )
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (activeTab == DebtType.OWED_BY_ME) strings.debtsOwedByMe else strings.debtsOwedToMe,
                            color = accentColor,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        if (overdueCountInTab > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(dangerColor.copy(alpha = 0.18f))
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = if (isArabic) "$overdueCountInTab متأخر" else "$overdueCountInTab late",
                                    color = dangerColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    // Net Position Pill
                    val netColor = when {
                        netBalance > 0.5 -> receivableColor
                        netBalance < -0.5 -> payableColor
                        else -> settledColor
                    }
                    val netLabel = when {
                        abs(netBalance) <= 0.5 -> if (isArabic) "صافي: متوازن" else "Net: Balanced"
                        netBalance > 0 -> {
                            val amt = isolateLtr(formatDzd(netBalance, strings.currencyCode))
                            if (isArabic) "صافي لك: +$amt" else "Net: +$amt"
                        }
                        else -> {
                            val amt = isolateLtr(formatDzd(abs(netBalance), strings.currencyCode))
                            if (isArabic) "صافي عليك: -$amt" else "Net: -$amt"
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(netColor.copy(alpha = if (isDark) 0.12f else 0.08f))
                            .border(1.dp, netColor.copy(alpha = 0.24f), RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = netLabel,
                            color = netColor,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Main Balance + Breathing Progress Ring
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.remainingBalance,
                            color = colors.textMuted,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = isolateLtr(formatDzd(activeRemaining, strings.currencyCode)),
                            color = colors.textPrimary,
                            fontSize = typo.display,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${strings.paidAmount}: ${isolateLtr(formatDzd(totalPaidInTab, strings.currencyCode))}",
                                color = settledColor,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "•",
                                color = colors.textMuted,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "${strings.totalAmount}: ${isolateLtr(formatDzd(totalOriginalInTab, strings.currencyCode))}",
                                color = colors.textMuted,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    DebtBreathingProgressRing(
                        progress = animatedProgress,
                        accentColor = accentColor,
                        settledColor = settledColor,
                        trackColor = colors.glassBorderSubtle,
                        textColor = colors.textPrimary,
                        subTextColor = colors.textMuted,
                        breathPhase = breathPhase,
                        isArabic = isArabic,
                        isDark = isDark
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Dual-Ledger Instant Switcher (both sides visible at once)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LedgerSwitchTile(
                        title = strings.debtsOwedByMe,
                        amountText = isolateLtr(formatDzd(payableRemaining, strings.currencyCode)),
                        count = payableCount,
                        selected = activeTab == DebtType.OWED_BY_ME,
                        accentColor = payableColor,
                        breathPhase = breathPhase,
                        colors = colors,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("debt_tab_owed_by_me"),
                        onClick = { onTabSelect(DebtType.OWED_BY_ME) }
                    )

                    LedgerSwitchTile(
                        title = strings.debtsOwedToMe,
                        amountText = isolateLtr(formatDzd(receivableRemaining, strings.currencyCode)),
                        count = receivableCount,
                        selected = activeTab == DebtType.OWED_TO_ME,
                        accentColor = receivableColor,
                        breathPhase = breathPhase,
                        colors = colors,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("debt_tab_owed_to_me"),
                        onClick = { onTabSelect(DebtType.OWED_TO_ME) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassButton(
                        text = strings.addDebt,
                        icon = Icons.Default.Add,
                        containerColor = accentColor,
                        modifier = Modifier
                            .weight(1f)
                            .breathingGlow(
                                enabled = breathingEnabled,
                                glowColor = accentColor,
                                cornerRadius = 16.dp,
                                minAlpha = 0.08f,
                                maxAlpha = 0.24f,
                                animateContainerScale = false,
                                durationMillis = 2800
                            )
                            .testTag("debt_add_button"),
                        onClick = onAddDebtClick
                    )

                    if (settledCountInTab > 0) {
                        Box(
                            modifier = Modifier
                                .height(dimens.buttonHeight)
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(16.dp))
                                .background(dangerColor.copy(alpha = if (isDark) 0.16f else 0.10f))
                                .border(1.dp, dangerColor.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(color = dangerColor),
                                    onClick = onClearSettledClick
                                )
                                .padding(horizontal = 14.dp)
                                .testTag("debt_clear_settled_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = strings.deleteSettledDebts,
                                    tint = dangerColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$settledCountInTab",
                                    color = dangerColor,
                                    fontSize = 13.sp,
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

@Composable
private fun DebtBreathingProgressRing(
    progress: Float,
    accentColor: Color,
    settledColor: Color,
    trackColor: Color,
    textColor: Color,
    subTextColor: Color,
    breathPhase: Float,
    isArabic: Boolean,
    isDark: Boolean
) {
    val pct = (progress * 100).toInt()
    val pctLabel = if (isArabic) isolateLtr("$pct٪") else "$pct%"
    val doneLabel = if (isArabic) "مسدد" else "Paid"

    Box(
        modifier = Modifier.size(76.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(76.dp)) {
            val strokePx = 6.5.dp.toPx()
            val inset = strokePx / 2f + 4.dp.toPx()
            val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
            val center = Offset(size.width / 2f, size.height / 2f)
            val ringRadius = arcSize.width / 2f

            // Ambient breathing halo behind the ring
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = (if (isDark) 0.22f else 0.14f) * (0.55f + 0.45f * breathPhase)),
                        Color.Transparent
                    ),
                    center = center,
                    radius = (size.minDimension * 0.56f * (0.94f + 0.10f * breathPhase)).coerceAtLeast(1f)
                )
            )

            // Track arc
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Active progress arc
            val sweep = (progress * 360f).coerceIn(0f, 360f)
            if (sweep > 0.5f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(accentColor, settledColor, accentColor)
                    ),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )

                // Breathing glowing tip dot
                val angleRad = ((-90f + sweep) * PI / 180.0)
                val tipX = center.x + (ringRadius * cos(angleRad)).toFloat()
                val tipY = center.y + (ringRadius * sin(angleRad)).toFloat()
                drawCircle(
                    color = settledColor.copy(alpha = 0.28f + 0.30f * breathPhase),
                    radius = (5.dp.toPx() + 2.dp.toPx() * breathPhase),
                    center = Offset(tipX, tipY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.2.dp.toPx(),
                    center = Offset(tipX, tipY)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = pctLabel,
                color = textColor,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
            Text(
                text = doneLabel,
                color = subTextColor,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun LedgerSwitchTile(
    title: String,
    amountText: String,
    count: Int,
    selected: Boolean,
    accentColor: Color,
    breathPhase: Float,
    colors: LiquidGlassColors,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = colors.isDark
    val bg = if (selected) {
        accentColor.copy(alpha = if (isDark) 0.18f else 0.11f)
    } else {
        colors.glassSurface.copy(alpha = if (isDark) 0.55f else 0.72f)
    }
    val borderCol = if (selected) {
        accentColor.copy(alpha = 0.42f + breathPhase * 0.28f)
    } else {
        colors.glassBorderSubtle
    }

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.dp, borderCol, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = accentColor),
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = if (selected) accentColor else colors.textSecondary,
                    fontSize = 11.5.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (selected) accentColor.copy(alpha = 0.20f) else colors.glassBorderSubtle)
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "$count",
                        color = if (selected) accentColor else colors.textMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = amountText,
                color = if (selected) colors.textPrimary else colors.textSecondary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun DebtControlsBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    statusFilter: DebtStatusFilter,
    onStatusFilterChange: (DebtStatusFilter) -> Unit,
    sortMode: DebtSortMode,
    onCycleSortMode: () -> Unit,
    totalCount: Int,
    activeCount: Int,
    overdueCount: Int,
    settledCount: Int,
    strings: StringsBundle,
    colors: LiquidGlassColors,
    modifier: Modifier = Modifier
) {
    val isDark = colors.isDark
    val isArabic = strings is ArabicStrings
    val dangerColor = DebtColors.danger(isDark)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Search + Sort Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sleek Search Pill
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.glassCard)
                    .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = colors.textMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = if (isArabic) "ابحث بالاسم أو الملاحظة..." else "Search name or note...",
                            color = colors.textMuted,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        singleLine = true,
                        textStyle = TextStyle(
                            color = colors.textPrimary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(colors.primaryNeon),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("debt_search_input")
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = if (isArabic) "مسح البحث" else "Clear search",
                            tint = colors.textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Sort Pill
            val sortLabel = when (sortMode) {
                DebtSortMode.SOONEST -> if (isArabic) "الأقرب" else "Due Soon"
                DebtSortMode.HIGHEST -> if (isArabic) "الأكبر" else "Highest"
                DebtSortMode.RECENT -> if (isArabic) "الأحدث" else "Recent"
            }
            Box(
                modifier = Modifier
                    .height(46.dp)
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.glassCard)
                    .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(14.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = colors.primaryNeon),
                        onClick = onCycleSortMode
                    )
                    .padding(horizontal = 12.dp)
                    .testTag("debt_sort_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = if (isArabic) "ترتيب" else "Sort",
                        tint = colors.primaryNeon,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = sortLabel,
                        color = colors.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }

        // Scrollable Status Filter Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GlassPill(
                text = "${strings.filterAll} ($totalCount)",
                isSelected = statusFilter == DebtStatusFilter.ALL,
                onClick = { onStatusFilterChange(DebtStatusFilter.ALL) },
                modifier = Modifier.testTag("debt_filter_all")
            )
            GlassPill(
                text = "${strings.filterActive} ($activeCount)",
                isSelected = statusFilter == DebtStatusFilter.ACTIVE,
                onClick = { onStatusFilterChange(DebtStatusFilter.ACTIVE) },
                modifier = Modifier.testTag("debt_filter_active")
            )
            if (overdueCount > 0) {
                val overdueText = if (isArabic) "متأخر ($overdueCount)" else "Overdue ($overdueCount)"
                val selected = statusFilter == DebtStatusFilter.OVERDUE
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .border(
                            1.dp,
                            if (selected) dangerColor else dangerColor.copy(alpha = 0.35f),
                            RoundedCornerShape(24.dp)
                        )
                        .background(
                            if (selected) dangerColor.copy(alpha = 0.24f)
                            else dangerColor.copy(alpha = 0.08f)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = dangerColor),
                            onClick = { onStatusFilterChange(DebtStatusFilter.OVERDUE) }
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("debt_filter_overdue"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = overdueText,
                        color = dangerColor,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                    )
                }
            }
            GlassPill(
                text = "${strings.filterSettled} ($settledCount)",
                isSelected = statusFilter == DebtStatusFilter.SETTLED,
                onClick = { onStatusFilterChange(DebtStatusFilter.SETTLED) },
                modifier = Modifier.testTag("debt_filter_settled")
            )
        }
    }
}

@Composable
fun ModernDebtCard(
    debt: DebtEntity,
    currencyCode: String,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onPartialPayClick: () -> Unit,
    onAddMoreClick: () -> Unit = {},
    onPayNow: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LiquidGlassTheme.colors
    val strings = AppStrings.current
    val isDark = colors.isDark
    val isArabic = strings is ArabicStrings
    val breathingEnabled = LocalBreathingGlowEnabled.current

    val now = System.currentTimeMillis()
    val isOverdue = !debt.isPaid && !debt.isRecurringMonthlyInstallment && debt.dueDateMillis < now
    val daysDiff = ((debt.dueDateMillis - now) / 86400000L)
    val isUrgentDueSoon = !debt.isPaid && !isOverdue && !debt.isRecurringMonthlyInstallment && daysDiff in 0L..2L

    val totalAmount = debt.amount
    val remainingAmount = debt.remainingAmount.coerceAtLeast(0.0)
    val paidAmount = (totalAmount - remainingAmount).coerceAtLeast(0.0)
    val paidRatio = if (totalAmount > 0) (paidAmount / totalAmount).toFloat().coerceIn(0f, 1f) else 1f
    val paidPercent = (paidRatio * 100).toInt()

    val animatedPaidRatio by animateFloatAsState(
        targetValue = paidRatio,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "card_progress"
    )
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(220),
        label = "chevron_rot"
    )

    val statusColor = when {
        debt.isPaid -> DebtColors.settled(isDark)
        isOverdue -> DebtColors.danger(isDark)
        debt.type == DebtType.OWED_BY_ME -> DebtColors.payable(isDark)
        else -> DebtColors.receivable(isDark)
    }

    // Subtle localized breathing pulse for active/urgent avatars & urgent card borders
    val cardBreath: Float = if (breathingEnabled && !debt.isPaid) {
        val t = rememberInfiniteTransition(label = "debt_card_breath_${debt.id}")
        val v by t.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = if (isOverdue || isUrgentDueSoon) 2000 else 3200,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "card_breath_val"
        )
        v
    } else {
        0.35f
    }

    val cardBorderColor = when {
        debt.isPaid -> colors.glassBorderSubtle
        isOverdue -> statusColor.copy(alpha = 0.35f + cardBreath * 0.40f)
        isExpanded -> statusColor.copy(alpha = 0.50f)
        isUrgentDueSoon -> statusColor.copy(alpha = 0.25f + cardBreath * 0.25f)
        else -> colors.glassBorder
    }

    val initials = remember(debt.personName) {
        val parts = debt.personName.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        when {
            parts.isEmpty() -> "?"
            parts.size == 1 -> parts[0].take(2).uppercase()
            else -> "${parts[0].first()}${parts[1].first()}".uppercase()
        }
    }

    val scheduleSummary = when {
        debt.isPaid -> {
            val dStr = isolateLtr(formatDate(debt.paidDateMillis ?: debt.dueDateMillis, isArabic))
            if (isArabic) "مسدد في $dStr" else "Settled $dStr"
        }
        debt.isRecurringMonthlyInstallment -> {
            val moAmt = isolateLtr(formatDzd(debt.monthlyInstallmentAmount, currencyCode))
            if (isArabic) "قسط شهري $moAmt • يوم ${debt.installmentDayOfMonth}"
            else "$moAmt / mo • Day ${debt.installmentDayOfMonth}"
        }
        isOverdue -> {
            val overdueDays = abs(daysDiff).coerceAtLeast(1)
            if (isArabic) "متأخر منذ $overdueDays يوم" else "Overdue by ${overdueDays}d"
        }
        daysDiff <= 0L -> if (isArabic) "مستحق اليوم" else "Due today"
        daysDiff == 1L -> if (isArabic) "مستحق غداً" else "Due tomorrow"
        else -> {
            val dStr = isolateLtr(formatDate(debt.dueDateMillis, isArabic))
            if (isArabic) "بعد $daysDiff يوم • $dStr" else "In ${daysDiff}d • $dStr"
        }
    }

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .breathingGlow(
                enabled = breathingEnabled && !debt.isPaid && (isOverdue || isUrgentDueSoon || isExpanded),
                glowColor = statusColor,
                cornerRadius = 20.dp,
                minAlpha = if (isOverdue) 0.08f else 0.04f,
                maxAlpha = if (isOverdue) 0.26f else 0.16f,
                animateContainerScale = false,
                durationMillis = if (isOverdue) 2000 else 2800
            )
            .testTag("debt_card_${debt.id}"),
        shape = RoundedCornerShape(20.dp),
        backgroundColor = colors.glassCard,
        borderColor = cardBorderColor,
        breathingGlow = !debt.isPaid && !isOverdue && !isUrgentDueSoon,
        onClick = onToggleExpand
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Collapsed Core Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar with Breathing Ring
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (!debt.isPaid) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .graphicsLayer {
                                    val s = 0.94f + cardBreath * 0.14f
                                    scaleX = s
                                    scaleY = s
                                    alpha = if (isOverdue) 0.55f + cardBreath * 0.35f else 0.25f + cardBreath * 0.25f
                                }
                                .border(1.5.dp, statusColor, CircleShape)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = if (isDark) 0.18f else 0.12f))
                            .border(1.dp, statusColor.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (debt.isPaid) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = strings.debtPaid,
                                tint = statusColor,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(
                                text = initials,
                                color = statusColor,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Person Name & Single Clear Context Line
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = debt.personName,
                            color = colors.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isOverdue) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(statusColor.copy(alpha = 0.16f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isArabic) "متأخر" else "Late",
                                    color = statusColor,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        } else if (debt.isRecurringMonthlyInstallment && !debt.isPaid) {
                            Icon(
                                imageVector = Icons.Default.Autorenew,
                                contentDescription = strings.debtMonthlyInstallments,
                                tint = DebtColors.autoPay(isDark),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = scheduleSummary,
                        color = if (isOverdue) statusColor else colors.textSecondary,
                        fontSize = 11.5.sp,
                        fontWeight = if (isOverdue) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (debt.note.isNotBlank() && !isExpanded) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = debt.note,
                            color = colors.textMuted,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Right Column: Remaining Balance + Expand Indicator
                Column(horizontalAlignment = Alignment.End) {
                    if (debt.isPaid) {
                        Text(
                            text = isolateLtr(formatDzd(debt.amount, currencyCode)),
                            color = colors.textMuted,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = TextDecoration.LineThrough,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = strings.debtPaid,
                                color = statusColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = colors.textMuted,
                                modifier = Modifier
                                    .size(16.dp)
                                    .graphicsLayer { rotationZ = chevronRotation }
                            )
                        }
                    } else {
                        Text(
                            text = isolateLtr(formatDzd(remainingAmount, currencyCode)),
                            color = colors.textPrimary,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val pctText = if (isArabic) isolateLtr("$paidPercent٪") else "$paidPercent%"
                            Text(
                                text = if (paidAmount > 0.5) {
                                    if (isArabic) "مدفوع $pctText" else "$pctText paid"
                                } else {
                                    if (isArabic) "من ${isolateLtr(formatDzd(totalAmount, currencyCode))}"
                                    else "of ${formatDzd(totalAmount, currencyCode)}"
                                },
                                color = statusColor,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = colors.textMuted,
                                modifier = Modifier
                                    .size(16.dp)
                                    .graphicsLayer { rotationZ = chevronRotation }
                            )
                        }
                    }
                }
            }

            // Ultra-slim integrated progress bar at bottom edge (for active partially/unpaid debts)
            if (!debt.isPaid) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .padding(bottom = if (isExpanded) 10.dp else 12.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(colors.glassBorderSubtle)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedPaidRatio.coerceAtLeast(0.03f))
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(statusColor, DebtColors.settled(isDark))
                                )
                            )
                    )
                }
            }

            // Expandable Practical Detail & Action Tray
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(220)) + fadeIn(tween(180)),
                exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(tween(140))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .padding(bottom = 14.dp)
                ) {
                    HorizontalDivider(color = colors.glassBorderSubtle, thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // 3-Column Clean Breakdown
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.glassSurface.copy(alpha = if (isDark) 0.55f else 0.72f))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MiniStatCol(
                            label = strings.totalAmount,
                            value = isolateLtr(formatDzd(totalAmount, currencyCode)),
                            valueColor = colors.textPrimary,
                            labelColor = colors.textMuted,
                            modifier = Modifier.weight(1f)
                        )
                        MiniStatCol(
                            label = strings.paidAmount,
                            value = isolateLtr(formatDzd(paidAmount, currencyCode)),
                            valueColor = DebtColors.settled(isDark),
                            labelColor = colors.textMuted,
                            modifier = Modifier.weight(1f)
                        )
                        MiniStatCol(
                            label = if (debt.isPaid) (if (isArabic) "تاريخ السداد" else "Settled") else strings.debtDueDate,
                            value = isolateLtr(formatDate(if (debt.isPaid) (debt.paidDateMillis ?: debt.dueDateMillis) else debt.dueDateMillis, isArabic)),
                            valueColor = if (isOverdue) statusColor else colors.textSecondary,
                            labelColor = colors.textMuted,
                            modifier = Modifier.weight(1f),
                            alignEnd = true
                        )
                    }

                    if (debt.note.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = debt.note,
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.glassSurface.copy(alpha = 0.4f))
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Practical Action Rows (48.dp touch height)
                    val typeAccentColor = if (debt.type == DebtType.OWED_BY_ME) {
                        DebtColors.payable(isDark)
                    } else {
                        DebtColors.receivable(isDark)
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!debt.isPaid) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CompactDebtActionPill(
                                    text = strings.makePartialPayment,
                                    icon = Icons.Default.Payments,
                                    accentColor = statusColor,
                                    isDark = isDark,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("debt_partial_pay_${debt.id}"),
                                    onClick = onPartialPayClick
                                )
                                CompactDebtActionPill(
                                    text = strings.executePayment,
                                    icon = Icons.Default.Payment,
                                    accentColor = DebtColors.settled(isDark),
                                    isDark = isDark,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("debt_settle_${debt.id}"),
                                    onClick = onPayNow
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CompactDebtActionPill(
                                    text = if (isArabic) "إضافة على الدين" else "Add to Debt",
                                    icon = Icons.Default.Add,
                                    accentColor = colors.primaryNeon,
                                    isDark = isDark,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("debt_add_more_${debt.id}"),
                                    onClick = onAddMoreClick
                                )

                                IconButton(
                                    onClick = onEdit,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(colors.glassSurface)
                                        .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(14.dp))
                                        .testTag("debt_edit_${debt.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = strings.editDebt,
                                        tint = colors.primaryNeon,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = onDelete,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(DebtColors.dangerContainer(isDark))
                                        .border(1.dp, DebtColors.dangerBorder(isDark), RoundedCornerShape(14.dp))
                                        .testTag("debt_delete_${debt.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = strings.delete,
                                        tint = DebtColors.danger(isDark),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(DebtColors.settledContainer(isDark))
                                        .border(1.dp, DebtColors.settledBorder(isDark), RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = DebtColors.settled(isDark),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = strings.fullySettled,
                                            color = DebtColors.settled(isDark),
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = onEdit,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(colors.glassSurface)
                                        .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(14.dp))
                                        .testTag("debt_edit_${debt.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = strings.editDebt,
                                        tint = colors.primaryNeon,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = onDelete,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(DebtColors.dangerContainer(isDark))
                                        .border(1.dp, DebtColors.dangerBorder(isDark), RoundedCornerShape(14.dp))
                                        .testTag("debt_delete_${debt.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = strings.delete,
                                        tint = DebtColors.danger(isDark),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            CompactDebtActionPill(
                                text = if (isArabic) "إضافة دين جديد عليه" else "Add New Debt on This",
                                icon = Icons.Default.Add,
                                accentColor = typeAccentColor,
                                isDark = isDark,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("debt_add_more_${debt.id}"),
                                onClick = onAddMoreClick
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniStatCol(
    label: String,
    value: String,
    valueColor: Color,
    labelColor: Color,
    modifier: Modifier = Modifier,
    alignEnd: Boolean = false
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start
    ) {
        Text(
            text = label,
            color = labelColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = valueColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CompactDebtActionPill(
    text: String,
    icon: ImageVector,
    accentColor: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(accentColor.copy(alpha = if (isDark) 0.18f else 0.11f))
            .border(1.dp, accentColor.copy(alpha = 0.42f), RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = accentColor),
                onClick = onClick
            )
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                color = accentColor,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun DebtBreathingEmptyState(
    title: String,
    description: String,
    accentColor: Color,
    actionText: String?,
    onActionClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val colors = LiquidGlassTheme.colors
    val breathingEnabled = LocalBreathingGlowEnabled.current

    val pulse: Float = if (breathingEnabled) {
        val transition = rememberInfiniteTransition(label = "debt_empty_breathing")
        val v by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "empty_pulse"
        )
        v
    } else {
        0.4f
    }

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .breathingGlow(
                enabled = breathingEnabled,
                glowColor = accentColor,
                cornerRadius = 22.dp,
                minAlpha = 0.05f,
                maxAlpha = 0.16f,
                animateContainerScale = false
            ),
        shape = RoundedCornerShape(22.dp),
        backgroundColor = colors.glassCard,
        breathingGlow = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(84.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .graphicsLayer {
                            val s = 0.90f + pulse * 0.20f
                            scaleX = s
                            scaleY = s
                            alpha = 0.35f - pulse * 0.20f
                        }
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.14f))
                        .border(1.dp, accentColor.copy(alpha = 0.38f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = title,
                color = colors.textPrimary,
                fontSize = 16.5.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = description,
                color = colors.textSecondary,
                fontSize = 12.5.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            if (actionText != null && onActionClick != null) {
                Spacer(modifier = Modifier.height(20.dp))
                GlassButton(
                    text = actionText,
                    icon = Icons.Default.Add,
                    containerColor = accentColor,
                    onClick = onActionClick,
                    modifier = Modifier.fillMaxWidth(0.75f)
                )
            }
        }
    }
}
