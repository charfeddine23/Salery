package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.SavingsMovementEntry
import com.example.data.SavingsMovementType
import com.example.data.UserSettings
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.formatDzd
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.LocalAdaptiveDimens
import com.example.ui.theme.LocalAdaptiveTypography
import com.example.viewmodel.FinancialSummary
import androidx.compose.material3.minimumInteractiveComponentSize
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class SavingsBottomView {
    LEDGER,
    PULSE
}

@Composable
fun SavingsLedgerPulseSection(
    entries: List<SavingsMovementEntry>,
    summary: FinancialSummary,
    userSettings: UserSettings,
    currencyCode: String,
    isArabic: Boolean,
    isStealth: Boolean,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LiquidGlassTheme.colors
    val dimens = LocalAdaptiveDimens.current
    var activeView by remember { mutableStateOf(SavingsBottomView.LEDGER) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dimens.spacingSm)
    ) {
        // Sleek 2-tab luxury pill bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(dimens.cornerRadiusLg))
                .background(colors.glassSurface)
                .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(dimens.cornerRadiusLg))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SavingsBottomTabPill(
                label = if (isArabic) "السجل (${entries.size})" else "Activity (${entries.size})",
                icon = Icons.Default.History,
                selected = activeView == SavingsBottomView.LEDGER,
                tag = "savings_tab_ledger",
                onClick = { activeView = SavingsBottomView.LEDGER },
                modifier = Modifier.weight(1f)
            )
            SavingsBottomTabPill(
                label = if (isArabic) "المؤشرات" else "Pulse",
                icon = Icons.Default.Speed,
                selected = activeView == SavingsBottomView.PULSE,
                tag = "savings_tab_pulse",
                onClick = { activeView = SavingsBottomView.PULSE },
                modifier = Modifier.weight(1f)
            )
        }

        AnimatedContent(
            targetState = activeView,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "savings_bottom_tab_content"
        ) { view ->
            when (view) {
                SavingsBottomView.LEDGER -> SavingsActivityLedgerCard(
                    entries = entries,
                    currencyCode = currencyCode,
                    isArabic = isArabic,
                    isStealth = isStealth,
                    onClearHistory = onClearHistory
                )
                SavingsBottomView.PULSE -> SavingsPulseSection(
                    summary = summary,
                    userSettings = userSettings,
                    isArabic = isArabic,
                    isStealth = isStealth,
                    showHeader = false
                )
            }
        }
    }
}

@Composable
private fun SavingsBottomTabPill(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LiquidGlassTheme.colors
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .defaultMinSize(minHeight = 44.dp)
            .clip(RoundedCornerShape(dimens.cornerRadiusMd))
            .background(
                if (selected) colors.primaryNeon.copy(alpha = 0.18f) else Color.Transparent
            )
            .then(
                if (selected) {
                    Modifier.border(
                        1.dp,
                        colors.primaryNeon.copy(alpha = 0.45f),
                        RoundedCornerShape(dimens.cornerRadiusMd)
                    )
                } else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) colors.primaryNeon else colors.textMuted,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = if (selected) colors.primaryNeon else colors.textSecondary,
                fontSize = typo.bodySm,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

/**
 * Clean chronological activity log of deposits, withdrawals, and direct expenses from Savings.
 */
@Composable
fun SavingsActivityLedgerCard(
    entries: List<SavingsMovementEntry>,
    currencyCode: String,
    isArabic: Boolean,
    isStealth: Boolean,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LiquidGlassTheme.colors
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current
    var showAll by remember { mutableStateOf(false) }

    val displayedEntries = remember(entries, showAll) {
        if (showAll) entries.take(20) else entries.take(4)
    }

    val dateFormatter = remember(isArabic) {
        SimpleDateFormat("dd MMM • HH:mm", if (isArabic) Locale("ar") else Locale.US)
    }

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("savings_activity_ledger_card"),
        shape = RoundedCornerShape(dimens.cornerRadiusXl),
        backgroundColor = colors.glassCard,
        cornerRadius = dimens.cornerRadiusXl
    ) {
        Column(modifier = Modifier.padding(dimens.cardPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AmberNeon.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = AmberNeon,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(dimens.spacingSm))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isArabic) "سجل حركات الادخار" else "Savings Ledger",
                            color = colors.textPrimary,
                            fontSize = typo.titleSm,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (entries.isEmpty()) {
                                if (isArabic) "كل تحويلاتك ستظهر هنا" else "All deposits & withdrawals appear here"
                            } else {
                                if (isArabic) "${entries.size} حركة مسجلة" else "${entries.size} recorded movements"
                            },
                            color = colors.textMuted,
                            fontSize = typo.caption,
                            maxLines = 1
                        )
                    }
                }

                if (entries.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minHeight = 36.dp)
                            .clip(RoundedCornerShape(50))
                            .background(colors.glassSurface)
                            .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(50))
                            .clickable(onClick = onClearHistory)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = if (isArabic) "مسح السجل" else "Clear History",
                                tint = colors.textMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isArabic) "مسح" else "Clear",
                                color = colors.textMuted,
                                fontSize = typo.caption,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            if (entries.isEmpty()) {
                Spacer(modifier = Modifier.height(dimens.spacingMd))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(dimens.cornerRadiusMd))
                        .background(colors.glassSurface.copy(alpha = 0.7f))
                        .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(dimens.cornerRadiusMd))
                        .padding(dimens.cardPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isArabic) {
                            "لا توجد حركات محفظة بعد • استخدم مركز التحويل السريع أعلاه"
                        } else {
                            "No transfers yet • Use Quick Transfer above to move funds"
                        },
                        color = colors.textMuted,
                        fontSize = typo.bodySm
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(dimens.spacingMd))
                displayedEntries.forEachIndexed { index, entry ->
                    SavingsMovementRow(
                        entry = entry,
                        formattedDate = dateFormatter.format(Date(entry.timestampMillis)),
                        currencyCode = currencyCode,
                        isArabic = isArabic,
                        isStealth = isStealth
                    )
                    if (index < displayedEntries.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = colors.glassBorderSubtle.copy(alpha = 0.5f)
                        )
                    }
                }

                if (entries.size > 4) {
                    Spacer(modifier = Modifier.height(dimens.spacingSm))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 40.dp)
                            .clip(RoundedCornerShape(dimens.cornerRadiusSm))
                            .background(colors.glassSurface)
                            .clickable { showAll = !showAll }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (showAll) {
                                if (isArabic) "عرض أقل" else "Show Less"
                            } else {
                                if (isArabic) "عرض الكل (${entries.size})" else "View All (${entries.size})"
                            },
                            color = colors.primaryNeon,
                            fontSize = typo.bodyXs,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SavingsMovementRow(
    entry: SavingsMovementEntry,
    formattedDate: String,
    currencyCode: String,
    isArabic: Boolean,
    isStealth: Boolean
) {
    val colors = LiquidGlassTheme.colors
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current

    val accent = when (entry.type) {
        SavingsMovementType.DEPOSIT -> colors.successColor
        SavingsMovementType.WITHDRAW -> colors.warningColor
        SavingsMovementType.SPEND -> colors.errorColor
    }
    val icon = when (entry.type) {
        SavingsMovementType.DEPOSIT -> Icons.Default.ArrowDownward
        SavingsMovementType.WITHDRAW -> Icons.Default.ArrowUpward
        SavingsMovementType.SPEND -> Icons.Default.CreditCard
    }
    val prefix = when (entry.type) {
        SavingsMovementType.DEPOSIT -> "+"
        SavingsMovementType.WITHDRAW, SavingsMovementType.SPEND -> "-"
    }
    val displayTitle = entry.title.ifBlank {
        when (entry.type) {
            SavingsMovementType.DEPOSIT -> if (isArabic) "إيداع للادخار" else "Deposit"
            SavingsMovementType.WITHDRAW -> if (isArabic) "سحب للمتاح" else "Withdraw"
            SavingsMovementType.SPEND -> if (isArabic) "صرف من الادخار" else "Expense"
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(17.dp)
            )
        }
        Spacer(modifier = Modifier.width(dimens.spacingSm))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayTitle,
                color = colors.textPrimary,
                fontSize = typo.bodySm,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (entry.note.isNotBlank()) "$formattedDate • ${entry.note}" else formattedDate,
                color = colors.textMuted,
                fontSize = typo.caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(dimens.spacingSm))
        Text(
            text = if (isStealth) "•••••" else "$prefix${formatDzd(entry.amount, currencyCode, false)}",
            color = accent,
            fontSize = typo.bodySm,
            fontWeight = FontWeight.ExtraBold,
            style = TextStyle(fontFeatureSettings = "tnum"),
            maxLines = 1
        )
    }
}

/**
 * Compact 2x2 Wealth Pulse Grid summarizing key monthly liquidity ratios without visual clutter.
 */
@Composable
fun SavingsPulseSection(
    summary: FinancialSummary,
    userSettings: UserSettings,
    isArabic: Boolean,
    isStealth: Boolean,
    showHeader: Boolean = true,
    modifier: Modifier = Modifier
) {
    val colors = LiquidGlassTheme.colors
    val strings = AppStrings.current
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current

    val daysToPayday = remember(userSettings.salaryPayday) {
        (userSettings.salaryPayday - Calendar.getInstance().get(Calendar.DAY_OF_MONTH)).let {
            if (it < 0) it + 30 else it
        }
    }
    val netSurplus = (summary.monthlyIncome - summary.monthlyExpenses).coerceAtLeast(0.0)
    val dailyBurn = summary.monthlyExpenses / 30.0
    val debtRatio = if (summary.totalBalance > 0) (summary.pendingDebtsOwedByMe / summary.totalBalance) * 100.0 else 0.0

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dimens.spacingSm)
    ) {
        if (showHeader) {
            Text(
                text = if (isArabic) "نبض السيولة الشهري" else "Monthly Liquidity Pulse",
                color = colors.textPrimary,
                fontSize = typo.titleSm,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimens.spacingSm)
        ) {
            CompactPulseTile(
                icon = Icons.Default.Savings,
                tint = colors.successColor,
                title = strings.savingsMilestone,
                value = formatDzd(netSurplus, strings.currencyCode, isStealth),
                subtitle = if (isArabic) "الفائض هذا الشهر" else "Monthly surplus",
                modifier = Modifier.weight(1f)
            )
            CompactPulseTile(
                icon = Icons.Default.Schedule,
                tint = colors.primaryNeon,
                title = strings.salaryRunway,
                value = if (isArabic) "$daysToPayday يوم" else "$daysToPayday days",
                subtitle = if (isArabic) "حتى يوم ${userSettings.salaryPayday}" else "Until payday ${userSettings.salaryPayday}",
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimens.spacingSm)
        ) {
            CompactPulseTile(
                icon = Icons.Default.Speed,
                tint = colors.warningColor,
                title = strings.spendingVelocity,
                value = formatDzd(dailyBurn, strings.currencyCode, isStealth),
                subtitle = if (isArabic) "المتوسط اليومي" else "Daily burn rate",
                modifier = Modifier.weight(1f)
            )
            CompactPulseTile(
                icon = Icons.Default.CreditCard,
                tint = if (debtRatio > 35.0) colors.errorColor else colors.secondaryNeon,
                title = strings.debtPressure,
                value = formatDzd(summary.pendingDebtsOwedByMe, strings.currencyCode, isStealth),
                subtitle = if (isArabic) "${debtRatio.toInt()}٪ من المتاح" else "${debtRatio.toInt()}% of available",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CompactPulseTile(
    icon: ImageVector,
    tint: Color,
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val colors = LiquidGlassTheme.colors
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current

    GlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(dimens.cornerRadiusLg),
        backgroundColor = colors.glassSurface,
        cornerRadius = dimens.cornerRadiusLg
    ) {
        Column(modifier = Modifier.padding(dimens.cardPaddingSmall)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(tint.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    color = colors.textMuted,
                    fontSize = typo.caption,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                color = colors.textPrimary,
                fontSize = typo.bodyLg,
                fontWeight = FontWeight.ExtraBold,
                style = TextStyle(fontFeatureSettings = "tnum"),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = colors.textMuted,
                fontSize = typo.caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
