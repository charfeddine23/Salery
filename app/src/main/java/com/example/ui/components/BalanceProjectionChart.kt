package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.analytics.BalanceProjectionEngine
import com.example.analytics.ForecastPeriod
import com.example.analytics.ProjectionPoint
import com.example.analytics.ProjectionResult
import com.example.data.DebtEntity
import com.example.data.TransactionEntity
import com.example.data.UserSettings
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.LocalStealthMode
import com.example.ui.i18n.formatDzd
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.NeuBurntCoral
import com.example.ui.theme.NeuRoyalBlue
import com.example.ui.theme.NeuVibrantGreen
import com.example.ui.theme.RoseNeon
import kotlin.math.max
import kotlin.math.min

@Composable
fun BalanceProjectionChart(
    currentBalance: Double,
    transactions: List<TransactionEntity>,
    debts: List<DebtEntity>,
    userSettings: UserSettings,
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors
    val isStealth = LocalStealthMode.current

    var selectedPeriod by remember { mutableStateOf(ForecastPeriod.DAYS_30) }
    var hoveredIndex by remember { mutableIntStateOf(-1) }

    val projectionResult: ProjectionResult = remember(currentBalance, transactions, debts, userSettings, selectedPeriod) {
        BalanceProjectionEngine.calculateProjection(
            currentBalance = currentBalance,
            transactions = transactions,
            debts = debts,
            userSettings = userSettings,
            period = selectedPeriod
        )
    }

    val primaryLineColor = if (colors.isNeumorphic) {
        if (colors.isDark) Color(0xFF60A5FA) else NeuRoyalBlue
    } else {
        CyanNeon
    }

    val gradientFillColor = if (colors.isNeumorphic) {
        if (colors.isDark) Color(0xFF2563EB).copy(alpha = 0.25f) else NeuRoyalBlue.copy(alpha = 0.15f)
    } else {
        CyanNeon.copy(alpha = 0.25f)
    }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        backgroundColor = if (colors.isNeumorphic) colors.glassCard else colors.glassCard.copy(alpha = 0.92f)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            // Header Row: Title + Period Tabs
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
                            .background(primaryLineColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = null,
                            tint = primaryLineColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = strings.balanceProjectionTitle,
                            color = colors.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strings.balanceProjectionSubtitle,
                            color = colors.textMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Timeframe Segmented Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (colors.isDark) Color(0xFF0F172A).copy(alpha = 0.8f) else Color(0xFFF1F5F9))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ProjectionTab(
                    label = strings.forecast7Days,
                    isSelected = selectedPeriod == ForecastPeriod.DAYS_7,
                    onClick = {
                        selectedPeriod = ForecastPeriod.DAYS_7
                        hoveredIndex = -1
                    },
                    modifier = Modifier.weight(1f)
                )
                ProjectionTab(
                    label = strings.forecast30Days,
                    isSelected = selectedPeriod == ForecastPeriod.DAYS_30,
                    onClick = {
                        selectedPeriod = ForecastPeriod.DAYS_30
                        hoveredIndex = -1
                    },
                    modifier = Modifier.weight(1f)
                )
                ProjectionTab(
                    label = strings.forecast6Months,
                    isSelected = selectedPeriod == ForecastPeriod.MONTHS_6,
                    onClick = {
                        selectedPeriod = ForecastPeriod.MONTHS_6
                        hoveredIndex = -1
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Metrics Summary Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = strings.projectedEndingBalance,
                        color = colors.textMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = formatDzd(projectionResult.endBalance, strings.currencyCode, isStealth),
                        color = colors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                val netPositive = projectionResult.netChange >= 0
                val netColor = if (netPositive) EmeraldNeon else RoseNeon

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(netColor.copy(alpha = 0.16f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (netPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = netColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isStealth) "••••••" else "${if (netPositive) "+" else ""}${formatDzd(projectionResult.netChange, strings.currencyCode)}",
                            color = netColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive Canvas Curve
            val points = projectionResult.points
            val activePoint = if (hoveredIndex in points.indices) points[hoveredIndex] else points.lastOrNull()

            // Active Inspector Banner
            if (activePoint != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(primaryLineColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = activePoint.label,
                            color = primaryLineColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (activePoint.eventDescription != null) {
                            Text(
                                text = " • ${activePoint.eventDescription}",
                                color = colors.textMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Text(
                        text = formatDzd(activePoint.balance, strings.currencyCode, isStealth),
                        color = colors.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .pointerInput(points) {
                        detectTapGestures(
                            onTap = { offset ->
                                val stepW = size.width / (points.size - 1).coerceAtLeast(1)
                                val idx = ((offset.x + stepW / 2) / stepW).toInt().coerceIn(0, points.size - 1)
                                hoveredIndex = idx
                            }
                        )
                    }
                    .pointerInput(points) {
                        detectDragGestures(
                            onDrag = { change, _ ->
                                val stepW = size.width / (points.size - 1).coerceAtLeast(1)
                                val idx = ((change.position.x + stepW / 2) / stepW).toInt().coerceIn(0, points.size - 1)
                                hoveredIndex = idx
                            },
                            onDragEnd = { /* Keep last touched point visible */ }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                    if (points.size < 2) return@Canvas

                    val maxVal = points.maxOf { it.balance }.coerceAtLeast(1.0)
                    val minVal = points.minOf { it.balance }.coerceAtLeast(0.0)
                    val range = max(1.0, maxVal - minVal)

                    val w = size.width
                    val h = size.height
                    val paddingBottom = 16f
                    val paddingTop = 16f
                    val usableHeight = h - paddingTop - paddingBottom
                    val stepWidth = w / (points.size - 1)

                    // Draw subtle grid lines
                    val gridLines = 3
                    val gridColor = colors.glassBorderSubtle.copy(alpha = 0.35f)
                    val strokeEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

                    for (i in 0..gridLines) {
                        val y = paddingTop + (usableHeight / gridLines) * i
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1f,
                            pathEffect = strokeEffect
                        )
                    }

                    // Compute pixel coordinates for each point
                    val coords = points.mapIndexed { index, pt ->
                        val x = index * stepWidth
                        val normalized = (pt.balance - minVal) / range
                        val y = paddingTop + usableHeight * (1f - normalized.toFloat())
                        Offset(x, y)
                    }

                    // Construct Cubic Bézier Path
                    val strokePath = Path().apply {
                        moveTo(coords.first().x, coords.first().y)
                        for (i in 0 until coords.size - 1) {
                            val p0 = coords[i]
                            val p1 = coords[i + 1]
                            val ctrlX = (p0.x + p1.x) / 2
                            cubicTo(ctrlX, p0.y, ctrlX, p1.y, p1.x, p1.y)
                        }
                    }

                    // Construct Under-Fill Path
                    val fillPath = Path().apply {
                        addPath(strokePath)
                        lineTo(coords.last().x, h)
                        lineTo(coords.first().x, h)
                        close()
                    }

                    // Draw Gradient Fill
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(gradientFillColor, Color.Transparent),
                            startY = paddingTop,
                            endY = h
                        )
                    )

                    // Draw Smooth Line
                    drawPath(
                        path = strokePath,
                        color = primaryLineColor,
                        style = Stroke(
                            width = 3.2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )

                    // Draw Dots on Points
                    coords.forEachIndexed { idx, offset ->
                        val isHovered = idx == hoveredIndex
                        val pt = points[idx]

                        val dotColor = when {
                            pt.isDepositDay -> EmeraldNeon
                            pt.isDebtDay -> RoseNeon
                            else -> primaryLineColor
                        }

                        // Outer ring on deposit/debt
                        if (pt.isDepositDay || pt.isDebtDay || isHovered) {
                            drawCircle(
                                color = dotColor.copy(alpha = 0.35f),
                                radius = if (isHovered) 8.dp.toPx() else 6.dp.toPx(),
                                center = offset
                            )
                        }

                        drawCircle(
                            color = if (colors.isDark) Color(0xFF0F172A) else Color.White,
                            radius = if (isHovered) 5.dp.toPx() else 3.5.dp.toPx(),
                            center = offset
                        )

                        drawCircle(
                            color = dotColor,
                            radius = if (isHovered) 3.5.dp.toPx() else 2.5.dp.toPx(),
                            center = offset
                        )

                        // Vertical guide line for hovered index
                        if (isHovered) {
                            drawLine(
                                color = primaryLineColor.copy(alpha = 0.5f),
                                start = Offset(offset.x, paddingTop),
                                end = Offset(offset.x, h),
                                strokeWidth = 1.5.dp.toPx(),
                                pathEffect = strokeEffect
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Scrubbing Instruction Hint
            Text(
                text = strings.tapToScrub,
                color = colors.textMuted.copy(alpha = 0.8f),
                fontSize = 10.5.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun ProjectionTab(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LiquidGlassTheme.colors
    val isDark = colors.isDark

    val bg = if (isSelected) {
        if (isDark) Color(0xFF1E293B) else Color.White
    } else {
        Color.Transparent
    }

    val textColor = if (isSelected) {
        if (isDark) Color.White else Color(0xFF0F172A)
    } else {
        colors.textMuted
    }

    Box(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
