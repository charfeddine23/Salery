package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HistoryToggleOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ArchivedAlgPosteSmsEntity
import com.example.sms.AnnualSmsTaxesSummary
import com.example.sms.SmsBalanceSnapshot
import com.example.sms.SmsFailedTransactionEntry
import com.example.sms.SmsTaxAnomalyEntry
import com.example.ui.i18n.formatDate
import com.example.ui.i18n.formatDzdAmount
import com.example.ui.i18n.isolateLtr
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassColors
import com.example.ui.theme.LocalAdaptiveDimens
import com.example.ui.theme.LocalAdaptiveTypography
import com.example.ui.theme.LocalBreathingGlowEnabled
import com.example.ui.theme.RoseNeon
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

enum class SmsWorkspaceTab {
    TAXES,
    TRANSACTIONS,
    FAILED,
    ARCHIVE
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SmsHeroBalanceCard(
    latestBalance: Double,
    latestDelta: Double?,
    updatedAtMillis: Long,
    currencyCode: String,
    isStealth: Boolean,
    hasSmsPerm: Boolean,
    autoSyncEnabled: Boolean,
    hasFreshBaseline: Boolean,
    baselineMillis: Long,
    controlsExpanded: Boolean,
    isArabic: Boolean,
    colors: LiquidGlassColors,
    onUnifiedSyncClick: () -> Unit,
    onToggleControls: () -> Unit,
    onToggleAutoSync: (Boolean) -> Unit,
    onSkipOldSms: () -> Unit,
    onResetBaseline: () -> Unit
) {
    val hasBalance = updatedAtMillis > 0L || latestBalance >= 0.0
    val isNegativeBalance = hasBalance && latestBalance < 0.0
    val breathing = LocalBreathingGlowEnabled.current
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current
    val tnum = TextStyle(fontFeatureSettings = "tnum")
    val heroAccent = if (isNegativeBalance) RoseNeon else CyanNeon

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sms_extracted_balance_card"),
        backgroundColor = colors.glassCard,
        borderColor = heroAccent.copy(alpha = 0.42f),
        shape = RoundedCornerShape(dimens.cornerRadiusXl),
        cornerRadius = dimens.cornerRadiusXl,
        breathingGlow = breathing
    ) {
        val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                heroAccent.copy(alpha = if (colors.isDark) 0.16f else 0.09f),
                                EmeraldNeon.copy(alpha = if (colors.isDark) 0.07f else 0.04f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * (if (isRtl) 0.15f else 0.85f), size.height * 0.12f),
                            radius = size.width * 0.78f
                        )
                    )
                    // Subtle telemetry horizontal hairline grid
                    drawLine(
                        color = heroAccent.copy(alpha = 0.08f),
                        start = Offset(0f, size.height * 0.48f),
                        end = Offset(size.width, size.height * 0.48f),
                        strokeWidth = 1f
                    )
                }
        ) {
            Column(modifier = Modifier.padding(dimens.cardPadding)) {
                // Cockpit Header Strip: Live Status Pill + Controls Toggle
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
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            CyanNeon.copy(alpha = 0.24f),
                                            EmeraldNeon.copy(alpha = 0.18f)
                                        )
                                    )
                                )
                                .border(1.dp, CyanNeon.copy(alpha = 0.40f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = CyanNeon,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isArabic) "مركز مزامنة بريدي موب" else "BaridiMob SMS Hub",
                                color = colors.textPrimary,
                                fontSize = typo.bodyLg,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                !hasSmsPerm -> RoseNeon
                                                autoSyncEnabled -> EmeraldNeon
                                                else -> AmberNeon
                                            }
                                        )
                                )
                                val statusText = when {
                                    !hasSmsPerm -> if (isArabic) "يتطلب إذن الرسائل" else "SMS permission required"
                                    autoSyncEnabled -> if (isArabic) "مراقبة فورية نشطة" else "Live auto-sync active"
                                    else -> if (isArabic) "وضع المزامنة اليدوية" else "Manual sync mode"
                                }
                                Text(
                                    text = statusText,
                                    color = if (hasSmsPerm && autoSyncEnabled) EmeraldNeon else colors.textMuted,
                                    fontSize = typo.bodyXs,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (controlsExpanded) CyanNeon.copy(alpha = 0.20f) else colors.glassSurface)
                            .border(
                                0.9.dp,
                                if (controlsExpanded) CyanNeon.copy(alpha = 0.55f) else colors.glassBorderSubtle,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable(onClick = onToggleControls)
                            .testTag("sms_controls_toggle_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = if (isArabic) "إعدادات المزامنة" else "Sync controls",
                            tint = if (controlsExpanded) CyanNeon else colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Current Balance + Delta Display Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.glassSurface.copy(alpha = 0.60f))
                        .border(0.8.dp, colors.glassBorderSubtle, RoundedCornerShape(18.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = if (isArabic) "رصيد بريدي موب المستخرج" else "EXTRACTED BARIDIMOB BALANCE",
                        color = colors.textMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = if (isArabic) 0.sp else 0.6.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (hasBalance) {
                            isolateLtr(formatDzdAmount(latestBalance, currencyCode, isStealth))
                        } else {
                            isolateLtr("--- $currencyCode")
                        },
                        color = when {
                            !hasBalance -> colors.textMuted
                            isNegativeBalance -> RoseNeon
                            else -> colors.textPrimary
                        },
                        fontSize = typo.display,
                        fontWeight = FontWeight.ExtraBold,
                        style = tnum,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (hasBalance && updatedAtMillis > 0L) {
                                (if (isArabic) "آخر تحديث: " else "Updated: ") + isolateLtr(formatDate(updatedAtMillis, isArabic))
                            } else {
                                if (isArabic) "اضغط تحديث لقراءة رصيدك تلقائياً" else "Tap Sync to read your latest Solde"
                            },
                            color = colors.textMuted,
                            fontSize = typo.bodyXs,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (latestDelta != null && abs(latestDelta) > 0.01 && !isStealth) {
                            val isUp = latestDelta > 0
                            val dColor = if (isUp) EmeraldNeon else RoseNeon
                            val sign = if (isUp) "+" else "-"
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(dColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = isolateLtr("$sign${formatDzdAmount(abs(latestDelta), currencyCode)}"),
                                    color = dColor,
                                    fontSize = typo.bodyXs,
                                    fontWeight = FontWeight.Bold,
                                    style = tnum,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Expandable Sync Controls Drawer (opened via Settings button)
                AnimatedVisibility(
                    visible = controlsExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.glassSurface.copy(alpha = 0.88f))
                            .border(0.8.dp, colors.glassBorderSubtle, RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GlassButton(
                            text = if (hasSmsPerm) {
                                if (isArabic) "مزامنة وفحص الرسائل الآن" else "Sync Balance & Inbox"
                            } else {
                                if (isArabic) "تفعيل قراءة رسائل بريد الجزائر" else "Enable ALG Poste SMS Sync"
                            },
                            icon = Icons.Default.Refresh,
                            containerColor = EmeraldNeon,
                            contentColor = Color(0xFF042419),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("scan_alg_poste_sms_button"),
                            onClick = onUnifiedSyncClick
                        )

                        HorizontalDivider(color = colors.glassBorderSubtle.copy(alpha = 0.45f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isArabic) "المراقبة التلقائية" else "Auto-Detect New SMS",
                                    color = colors.textPrimary,
                                    fontSize = typo.body,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isArabic) "رصد المعاملات والضرائب فور وصول الرسالة" else "Detect operations & taxes as SMS arrive",
                                    color = colors.textMuted,
                                    fontSize = typo.bodyXs
                                )
                            }
                            Switch(
                                checked = autoSyncEnabled && hasSmsPerm,
                                onCheckedChange = onToggleAutoSync,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = EmeraldNeon,
                                    checkedTrackColor = EmeraldNeon.copy(alpha = 0.35f)
                                )
                            )
                        }

                        HorizontalDivider(color = colors.glassBorderSubtle.copy(alpha = 0.45f))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.HistoryToggleOff,
                                    contentDescription = null,
                                    tint = if (hasFreshBaseline) CyanNeon else colors.textMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (hasFreshBaseline) {
                                            if (isArabic) "البدء من الصفر مفعّل" else "Fresh Start Active"
                                        } else {
                                            if (isArabic) "تصفية الرسائل القديمة" else "Old Messages Filter"
                                        },
                                        color = colors.textPrimary,
                                        fontSize = typo.bodySm,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (hasFreshBaseline && baselineMillis > 0L) {
                                        val dateFmt = remember(baselineMillis, isArabic) {
                                            isolateLtr(
                                                SimpleDateFormat("dd/MM HH:mm", if (isArabic) Locale("ar") else Locale.US)
                                                    .format(Date(baselineMillis))
                                            )
                                        }
                                        Text(
                                            text = if (isArabic) "تجاهل ما قبل $dateFmt" else "Ignoring before $dateFmt",
                                            color = CyanNeon,
                                            fontSize = typo.bodyXs
                                        )
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(CyanNeon.copy(alpha = 0.16f))
                                        .border(0.8.dp, CyanNeon.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                                        .clickable(onClick = onSkipOldSms)
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                        .testTag("skip_old_sms_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.RestartAlt,
                                            contentDescription = null,
                                            tint = CyanNeon,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isArabic) "من الصفر" else "Start 0",
                                            color = CyanNeon,
                                            fontSize = typo.bodyXs,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                }

                                if (hasFreshBaseline) {
                                    Box(
                                        modifier = Modifier
                                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(colors.glassCard)
                                            .border(0.8.dp, colors.glassBorderSubtle, RoundedCornerShape(10.dp))
                                            .clickable(onClick = onResetBaseline)
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                            .testTag("reset_sms_baseline_button"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (isArabic) "فحص الكل" else "All",
                                            color = colors.textSecondary,
                                            fontSize = typo.bodyXs,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
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

@Composable
fun SmsWorkspaceSegmentedTabs(
    selectedTab: SmsWorkspaceTab,
    taxesCount: Int,
    transactionsCount: Int,
    failedCount: Int = 0,
    archiveCount: Int,
    isArabic: Boolean,
    colors: LiquidGlassColors,
    onSelectTab: (SmsWorkspaceTab) -> Unit
) {
    val typo = LocalAdaptiveTypography.current

    data class TabSpec(
        val tab: SmsWorkspaceTab,
        val label: String,
        val count: Int,
        val icon: ImageVector,
        val accent: Color,
        val tag: String
    )

    val tabs = listOf(
        TabSpec(
            tab = SmsWorkspaceTab.TAXES,
            label = if (isArabic) "الضرائب" else "Taxes",
            count = taxesCount,
            icon = Icons.Default.Policy,
            accent = AmberNeon,
            tag = "sms_tab_taxes"
        ),
        TabSpec(
            tab = SmsWorkspaceTab.TRANSACTIONS,
            label = if (isArabic) "المعاملات" else "Activity",
            count = transactionsCount,
            icon = Icons.Default.SwapHoriz,
            accent = EmeraldNeon,
            tag = "sms_tab_transactions"
        ),
        TabSpec(
            tab = SmsWorkspaceTab.FAILED,
            label = if (isArabic) "الفاشلة" else "Failed",
            count = failedCount,
            icon = Icons.Default.Replay,
            accent = RoseNeon,
            tag = "sms_tab_failed"
        ),
        TabSpec(
            tab = SmsWorkspaceTab.ARCHIVE,
            label = if (isArabic) "الأرشيف" else "Archive",
            count = archiveCount,
            icon = Icons.Default.VerifiedUser,
            accent = CyanNeon,
            tag = "sms_tab_archive"
        )
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.glassSurface)
            .border(0.9.dp, colors.glassBorderSubtle, RoundedCornerShape(18.dp))
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tabs.forEach { item ->
            val selected = selectedTab == item.tab
            val bgColor by animateColorAsState(
                targetValue = if (selected) item.accent.copy(alpha = 0.20f) else Color.Transparent,
                animationSpec = tween(200),
                label = "sms_tab_bg_${item.tab.name}"
            )
            val borderColor by animateColorAsState(
                targetValue = if (selected) item.accent.copy(alpha = 0.55f) else Color.Transparent,
                animationSpec = tween(200),
                label = "sms_tab_border_${item.tab.name}"
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(bgColor)
                    .border(
                        width = if (selected) 1.dp else 0.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = item.accent),
                        onClick = { onSelectTab(item.tab) }
                    )
                    .padding(horizontal = 4.dp, vertical = 7.dp)
                    .testTag(item.tag),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = if (selected) item.accent else colors.textMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (item.count > 0 || selected) item.accent.copy(alpha = 0.16f)
                                else colors.glassCard.copy(alpha = 0.65f)
                            )
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "${item.count}",
                            color = if (selected || item.count > 0) item.accent else colors.textMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            style = TextStyle(fontFeatureSettings = "tnum"),
                            maxLines = 1
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = item.label,
                    color = if (selected) item.accent else colors.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SmsAnnualTaxSummaryCard(
    summary: AnnualSmsTaxesSummary,
    currencyCode: String,
    isStealth: Boolean,
    isArabic: Boolean,
    colors: LiquidGlassColors
) {
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current
    val tnum = TextStyle(fontFeatureSettings = "tnum")

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sms_annual_taxes_summary_card"),
        backgroundColor = colors.glassCard,
        borderColor = AmberNeon.copy(alpha = 0.52f),
        shape = RoundedCornerShape(dimens.cornerRadiusLg),
        cornerRadius = dimens.cornerRadiusLg,
        breathingGlow = false
    ) {
        val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                AmberNeon.copy(alpha = if (colors.isDark) 0.20f else 0.12f),
                                RoseNeon.copy(alpha = if (colors.isDark) 0.08f else 0.04f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * (if (isRtl) 0.85f else 0.15f), size.height * 0.2f),
                            radius = size.width * 0.72f
                        )
                    )
                }
                .padding(dimens.cardPadding)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(AmberNeon.copy(alpha = 0.18f))
                                .border(0.9.dp, AmberNeon.copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = AmberNeon,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isArabic) "مجموع الضرائب للسنة الحالية" else "Current Year Total Taxes",
                                color = colors.textPrimary,
                                fontSize = typo.bodyLg,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = if (isArabic) "إجمالي الرسوم والاقتطاعات لسنة ${summary.year}"
                                else "Cumulative deductions for ${summary.year}",
                                color = colors.textMuted,
                                fontSize = typo.bodyXs
                            )
                        }
                    }

                    // Reference Date Pill: 1.1.xxxx
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(CyanNeon.copy(alpha = 0.14f))
                            .border(0.9.dp, CyanNeon.copy(alpha = 0.42f), RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("sms_annual_taxes_reference_badge"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = CyanNeon,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (isArabic) {
                                "بداية العد ${isolateLtr(summary.referenceStartDateLabel)}"
                            } else {
                                "Since ${summary.referenceStartDateLabel}"
                            },
                            color = CyanNeon,
                            fontSize = typo.caption,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = isolateLtr(formatDzdAmount(summary.totalYearTaxesDzd, currencyCode, isStealth)),
                    color = AmberNeon,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    style = tnum,
                    modifier = Modifier.testTag("sms_annual_taxes_total_amount")
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.glassSurface)
                            .border(0.7.dp, colors.glassBorderSubtle, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isArabic) "${summary.yearTaxCount} عملية اقتطاع" else "${summary.yearTaxCount} entries",
                            color = colors.textSecondary,
                            fontSize = typo.bodyXs,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(EmeraldNeon.copy(alpha = 0.12f))
                            .border(0.7.dp, EmeraldNeon.copy(alpha = 0.30f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isArabic) {
                                "مسجلة: ${isolateLtr(formatDzdAmount(summary.recordedYearTaxesDzd, currencyCode, isStealth))}"
                            } else {
                                "Logged: ${formatDzdAmount(summary.recordedYearTaxesDzd, currencyCode, isStealth)}"
                            },
                            color = EmeraldNeon,
                            fontSize = typo.bodyXs,
                            fontWeight = FontWeight.Bold,
                            style = tnum,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SmsTaxFeeCompactCard(
    anomaly: SmsTaxAnomalyEntry,
    isArabic: Boolean,
    isStealth: Boolean,
    currency: String,
    colors: LiquidGlassColors,
    onRecordAsExpense: () -> Unit,
    onDismiss: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tax_discrepancy_hunter_card"),
        backgroundColor = colors.glassCard,
        borderColor = AmberNeon.copy(alpha = 0.42f),
        shape = RoundedCornerShape(dimens.cornerRadiusLg),
        cornerRadius = dimens.cornerRadiusLg
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Distinct Amber/Rose vertical edge ribbon
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(listOf(AmberNeon, RoseNeon))
                    )
            )
            Column(modifier = Modifier.weight(1f).padding(dimens.cardPaddingSmall)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(AmberNeon.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Policy,
                            contentDescription = null,
                            tint = AmberNeon,
                            modifier = Modifier.size(dimens.iconSizeSm)
                        )
                    }
                    Spacer(modifier = Modifier.width(dimens.spacingSm))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isArabic) anomaly.titleAr else anomaly.titleEn,
                            color = colors.textPrimary,
                            fontSize = typo.body,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = isolateLtr(formatDate(anomaly.detectedAtMillis, isArabic)),
                            color = colors.textMuted,
                            fontSize = typo.bodyXs,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(RoseNeon.copy(alpha = 0.14f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isStealth) "••••" else isolateLtr("-" + formatDzdAmount(anomaly.taxAmount, currency, false)),
                            color = RoseNeon,
                            fontSize = typo.body,
                            fontWeight = FontWeight.ExtraBold,
                            style = TextStyle(fontFeatureSettings = "tnum"),
                            maxLines = 1
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = if (isArabic) "تجاهل" else "Dismiss",
                            tint = colors.textMuted,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                AnimatedVisibility(visible = expanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .clip(RoundedCornerShape(dimens.cornerRadiusSm))
                            .background(colors.glassSurface)
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (isStealth) "••••••" else if (isArabic) anomaly.explanationAr else anomaly.explanationEn,
                            color = colors.textSecondary,
                            fontSize = typo.bodySm
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (anomaly.isRecordedAsExpense) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(EmeraldNeon.copy(alpha = 0.14f))
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldNeon,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isArabic) "تم تسجيل الاقتطاع في المصاريف" else "Fee logged to Expenses",
                            color = EmeraldNeon,
                            fontSize = typo.bodySm,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = dimens.buttonHeight)
                            .clip(RoundedCornerShape(dimens.cornerRadiusSm))
                            .background(AmberNeon.copy(alpha = 0.18f))
                            .border(0.8.dp, AmberNeon.copy(alpha = 0.5f), RoundedCornerShape(dimens.cornerRadiusSm))
                            .clickable(onClick = onRecordAsExpense)
                            .padding(horizontal = 12.dp, vertical = 9.dp)
                            .testTag("record_tax_expense_button"),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = null,
                            tint = AmberNeon,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isArabic) "تسجيل كرسوم في المصاريف" else "Record Fee as Expense",
                            color = AmberNeon,
                            fontSize = typo.bodySm,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SmsCleanBalanceTimelineRow(
    snapshot: SmsBalanceSnapshot,
    delta: Double?,
    isArabic: Boolean,
    isStealth: Boolean,
    currency: String,
    colors: LiquidGlassColors,
    failedStatusLabel: String? = null,
    failedStatusColor: Color = RoseNeon
) {
    var peekRaw by remember { mutableStateOf(false) }
    val typo = LocalAdaptiveTypography.current
    val dimens = LocalAdaptiveDimens.current

    val effectiveDelta = when {
        snapshot.hasTransaction && snapshot.transactionAmount > 0.0 -> {
            if (snapshot.isExpense) -snapshot.transactionAmount else snapshot.transactionAmount
        }
        else -> delta
    }
    val deltaColor = when {
        effectiveDelta == null -> colors.textMuted
        effectiveDelta > 0.005 -> EmeraldNeon
        effectiveDelta < -0.005 -> RoseNeon
        else -> colors.textMuted
    }
    val deltaLabel = when {
        isStealth -> "••••"
        effectiveDelta == null -> if (isArabic) "رصيد افتتاحي" else "Opening"
        effectiveDelta > 0.005 -> isolateLtr("+" + formatDzdAmount(effectiveDelta, currency))
        effectiveDelta < -0.005 -> isolateLtr("-" + formatDzdAmount(abs(effectiveDelta), currency))
        else -> if (isArabic) "مزامنة" else "Sync"
    }
    val operationLabel = when {
        snapshot.hasExplicitFeeKeyword -> if (isArabic) "رسوم / ضريبة" else "Bank Fee / Tax"
        snapshot.hasTransaction && snapshot.isExpense -> if (isArabic) "سحب / دفع (مصروف)" else "Debit / Payment"
        snapshot.hasTransaction && !snapshot.isExpense -> if (isArabic) "إيداع / تحويل وارد" else "Credit / Deposit"
        delta != null && delta > 0.005 -> if (isArabic) "زيادة رصيد" else "Balance Credit"
        delta != null && delta < -0.005 -> if (isArabic) "نقص رصيد" else "Balance Debit"
        else -> if (isArabic) "تحديث رصيد" else "Balance Check"
    }
    val opIcon = when {
        effectiveDelta != null && effectiveDelta > 0.005 -> Icons.AutoMirrored.Filled.TrendingUp
        effectiveDelta != null && effectiveDelta < -0.005 -> Icons.AutoMirrored.Filled.TrendingDown
        else -> Icons.Default.AccountBalanceWallet
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (snapshot.rawBody.isNotBlank()) peekRaw = !peekRaw }
            .padding(horizontal = dimens.cardPadding, vertical = 12.dp)
    ) {
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
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(deltaColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = opIcon,
                        contentDescription = null,
                        tint = deltaColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = operationLabel,
                        color = colors.textPrimary,
                        fontSize = typo.body,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${if (isArabic) snapshot.sender.ifBlank { "بريد الجزائر" } else snapshot.sender.ifBlank { "ALG Poste" }} • ${isolateLtr(formatDate(snapshot.timestampMillis, isArabic))}",
                        color = colors.textMuted,
                        fontSize = typo.bodyXs,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!failedStatusLabel.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(failedStatusColor.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = failedStatusLabel,
                                color = failedStatusColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(dimens.cornerRadiusXs))
                        .background(deltaColor.copy(alpha = 0.14f))
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = deltaLabel,
                        color = deltaColor,
                        fontSize = typo.bodySm,
                        fontWeight = FontWeight.ExtraBold,
                        style = TextStyle(fontFeatureSettings = "tnum"),
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${if (isArabic) "الرصيد:" else "Solde:"} ${isolateLtr(formatDzdAmount(snapshot.balance, currency, isStealth))}",
                    color = if (snapshot.balance < 0.0) RoseNeon else colors.textMuted,
                    fontSize = typo.caption,
                    style = TextStyle(fontFeatureSettings = "tnum"),
                    maxLines = 1
                )
            }
        }

        AnimatedVisibility(visible = peekRaw && snapshot.rawBody.isNotBlank()) {
            Text(
                text = if (isStealth) "••••••" else isolateLtr(snapshot.rawBody),
                color = colors.textMuted,
                fontSize = typo.bodyXs,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.glassSurface)
                    .padding(10.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SmsFailedTransactionsSummaryCard(
    failedEntries: List<SmsFailedTransactionEntry>,
    conflictingMap: Map<String, List<Long>>,
    currencyCode: String,
    isStealth: Boolean,
    isArabic: Boolean,
    colors: LiquidGlassColors
) {
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current
    val tnum = TextStyle(fontFeatureSettings = "tnum")

    val totalRecovered = failedEntries.sumOf { it.recoveredAmount }
    val totalUnrefundedFees = failedEntries.sumOf { it.unrefundedFeeAmount }
    val conflictingCount = failedEntries.count { (conflictingMap[it.id]?.size ?: 0) > 0 }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sms_failed_transactions_summary_card"),
        backgroundColor = colors.glassCard,
        borderColor = RoseNeon.copy(alpha = 0.48f),
        shape = RoundedCornerShape(dimens.cornerRadiusLg),
        cornerRadius = dimens.cornerRadiusLg,
        breathingGlow = false
    ) {
        val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                RoseNeon.copy(alpha = if (colors.isDark) 0.18f else 0.10f),
                                EmeraldNeon.copy(alpha = if (colors.isDark) 0.08f else 0.04f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * (if (isRtl) 0.85f else 0.15f), size.height * 0.2f),
                            radius = size.width * 0.72f
                        )
                    )
                }
                .padding(dimens.cardPadding)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(RoseNeon.copy(alpha = 0.16f))
                                .border(0.9.dp, RoseNeon.copy(alpha = 0.45f), RoundedCornerShape(11.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay,
                                contentDescription = null,
                                tint = RoseNeon,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isArabic) "التعاملات الفاشلة (الوهمية)" else "Failed / Ghost Transactions",
                                color = colors.textPrimary,
                                fontSize = typo.bodyLg,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = if (isArabic) {
                                    "رصد المبالغ المخصومة خطأً والمسترجعة تلقائياً أو برسالة"
                                } else {
                                    "Catches failed debits recovered on later SMS or refund alerts"
                                },
                                color = colors.textMuted,
                                fontSize = typo.bodyXs
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background((if (conflictingCount > 0) AmberNeon else EmeraldNeon).copy(alpha = 0.14f))
                            .border(
                                0.9.dp,
                                (if (conflictingCount > 0) AmberNeon else EmeraldNeon).copy(alpha = 0.42f),
                                RoundedCornerShape(50)
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (conflictingCount > 0) AmberNeon else EmeraldNeon,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (conflictingCount > 0) {
                                if (isArabic) "تضارب مسجل ($conflictingCount)" else "Conflict ($conflictingCount)"
                            } else {
                                if (isArabic) "الحساب محمي ✓" else "Ledger Protected ✓"
                            },
                            color = if (conflictingCount > 0) AmberNeon else EmeraldNeon,
                            fontSize = typo.caption,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = isolateLtr(formatDzdAmount(totalRecovered, currencyCode, isStealth)),
                    color = EmeraldNeon,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    style = tnum,
                    modifier = Modifier.testTag("sms_failed_total_recovered")
                )
                Text(
                    text = if (isArabic) "إجمالي المبالغ الفاشلة المسترجعة للرصيد" else "Total failed debit funds returned to balance",
                    color = colors.textMuted,
                    fontSize = typo.bodyXs
                )

                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.glassSurface)
                            .border(0.7.dp, colors.glassBorderSubtle, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isArabic) "${failedEntries.size} عملية مرصودة" else "${failedEntries.size} caught ops",
                            color = colors.textSecondary,
                            fontSize = typo.bodyXs,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (totalUnrefundedFees >= 0.01) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(AmberNeon.copy(alpha = 0.14f))
                                .border(0.7.dp, AmberNeon.copy(alpha = 0.32f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isArabic) {
                                    "رسوم بقيت مقتطعة: ${isolateLtr(formatDzdAmount(totalUnrefundedFees, currencyCode, isStealth))}"
                                } else {
                                    "Unrefunded fees: ${formatDzdAmount(totalUnrefundedFees, currencyCode, isStealth)}"
                                },
                                color = AmberNeon,
                                fontSize = typo.bodyXs,
                                fontWeight = FontWeight.Bold,
                                style = tnum
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SmsFailedTransactionCard(
    entry: SmsFailedTransactionEntry,
    conflictingLedgerCount: Int,
    isArabic: Boolean,
    isStealth: Boolean,
    currency: String,
    colors: LiquidGlassColors,
    onResolveConflicting: () -> Unit,
    onDismiss: () -> Unit
) {
    var showRawMessages by remember(entry.id) { mutableStateOf(false) }
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current
    val tnum = TextStyle(fontFeatureSettings = "tnum")

    val kindLabel = when (entry.detectionKind) {
        "EXPLICIT_REFUND_REVERSAL" -> if (isArabic) "استرجاع برسالة إلغاء" else "Explicit Reversal"
        "FAILED_WITH_FEE_KEPT" -> if (isArabic) "استرجاع صامت (رسوم مقتطعة)" else "Silent Return (Fee Kept)"
        "MULTI_DEBIT_RECOVERY" -> if (isArabic) "استرجاع مجمع" else "Batch Recovery"
        "EXPLICIT_FAILED_NO_DEDUCTION" -> if (isArabic) "فشل بدون خصم" else "Failed (No Debit)"
        "EXPLICIT_REVERSAL_UNLINKED" -> if (isArabic) "إشعار إلغاء / استرجاع" else "Reversal Notice"
        else -> if (isArabic) "رصد صامت بالرسالة القادمة" else "Silent Next-SMS Catch"
    }
    val kindAccent = when (entry.detectionKind) {
        "FAILED_WITH_FEE_KEPT" -> AmberNeon
        "EXPLICIT_REFUND_REVERSAL", "SILENT_BALANCE_JUMP", "MULTI_DEBIT_RECOVERY" -> EmeraldNeon
        else -> RoseNeon
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sms_failed_transaction_card"),
        backgroundColor = colors.glassCard,
        borderColor = RoseNeon.copy(alpha = 0.42f),
        shape = RoundedCornerShape(dimens.cornerRadiusLg),
        cornerRadius = dimens.cornerRadiusLg
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(listOf(RoseNeon, AmberNeon, EmeraldNeon))
                    )
            )
            Column(modifier = Modifier.weight(1f).padding(dimens.cardPaddingSmall)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(RoseNeon.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = null,
                            tint = RoseNeon,
                            modifier = Modifier.size(dimens.iconSizeSm)
                        )
                    }
                    Spacer(modifier = Modifier.width(dimens.spacingSm))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isArabic) entry.titleAr else entry.titleEn,
                            color = colors.textPrimary,
                            fontSize = typo.body,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(kindAccent.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$kindLabel • ${entry.confidencePercent}%",
                                    color = kindAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                            Text(
                                text = isolateLtr(formatDate(entry.recoveredTimestampMillis, isArabic)),
                                color = colors.textMuted,
                                fontSize = typo.bodyXs,
                                maxLines = 1
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(EmeraldNeon.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isStealth) "••••" else isolateLtr("+" + formatDzdAmount(entry.recoveredAmount, currency, false)),
                                color = EmeraldNeon,
                                fontSize = typo.bodySm,
                                fontWeight = FontWeight.ExtraBold,
                                style = tnum,
                                maxLines = 1
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = if (isArabic) "تجاهل" else "Dismiss",
                            tint = colors.textMuted,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 4-step Balance Proof Grid
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.glassSurface)
                        .border(0.7.dp, colors.glassBorderSubtle, RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FailedProofCell(
                            label = if (isArabic) "قبل الخصم" else "Before Debit",
                            value = isolateLtr(formatDzdAmount(entry.balanceBeforeDebit, currency, isStealth)),
                            valueColor = colors.textSecondary,
                            mutedColor = colors.textMuted,
                            modifier = Modifier.weight(1f)
                        )
                        FailedProofCell(
                            label = if (isArabic) "بعد الخصم الوهمي" else "After Ghost Debit",
                            value = isolateLtr(formatDzdAmount(entry.balanceAfterDebit, currency, isStealth)),
                            valueColor = RoseNeon,
                            mutedColor = colors.textMuted,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    HorizontalDivider(color = colors.glassBorderSubtle.copy(alpha = 0.35f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FailedProofCell(
                            label = if (isArabic) "المتوقع لاحقاً" else "Expected Next",
                            value = isolateLtr(formatDzdAmount(entry.expectedBalanceBeforeRecovery, currency, isStealth)),
                            valueColor = colors.textSecondary,
                            mutedColor = colors.textMuted,
                            modifier = Modifier.weight(1f)
                        )
                        FailedProofCell(
                            label = if (isArabic) "الفعلي عند الرجوع" else "Actual Returned",
                            value = isolateLtr(formatDzdAmount(entry.actualBalanceAtRecovery, currency, isStealth)),
                            valueColor = EmeraldNeon,
                            mutedColor = colors.textMuted,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isStealth) "••••••" else if (isArabic) entry.explanationAr else entry.explanationEn,
                    color = colors.textSecondary,
                    fontSize = typo.bodyXs,
                    lineHeight = 16.sp
                )

                if (entry.debitRawBody.isNotBlank() || entry.recoveryRawBody.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (showRawMessages) {
                            if (isArabic) "إخفاء نصوص الرسائل ▲" else "Hide SMS evidence ▲"
                        } else {
                            if (isArabic) "عرض نصوص رسالتي الخصم والاسترجاع ▼" else "Show debit & recovery SMS ▼"
                        },
                        color = CyanNeon,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { showRawMessages = !showRawMessages }
                            .padding(vertical = 6.dp, horizontal = 4.dp)
                    )
                    AnimatedVisibility(visible = showRawMessages) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (entry.debitRawBody.isNotBlank()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(RoseNeon.copy(alpha = 0.08f))
                                        .border(0.6.dp, RoseNeon.copy(alpha = 0.28f), RoundedCornerShape(10.dp))
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = if (isArabic) "1) رسالة الخصم الفاشلة:" else "1) Failed Debit SMS:",
                                        color = RoseNeon,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isStealth) "••••••" else isolateLtr(entry.debitRawBody),
                                        color = colors.textSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            if (entry.recoveryRawBody.isNotBlank() && entry.recoverySmsId != entry.failedDebitSmsId) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(EmeraldNeon.copy(alpha = 0.08f))
                                        .border(0.6.dp, EmeraldNeon.copy(alpha = 0.28f), RoundedCornerShape(10.dp))
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = if (isArabic) "2) الرسالة الكاشفة للاسترجاع:" else "2) Recovery Reveal SMS:",
                                        color = EmeraldNeon,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isStealth) "••••••" else isolateLtr(entry.recoveryRawBody),
                                        color = colors.textSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (conflictingLedgerCount > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = dimens.buttonHeight)
                            .clip(RoundedCornerShape(dimens.cornerRadiusSm))
                            .background(RoseNeon.copy(alpha = 0.18f))
                            .border(0.8.dp, RoseNeon.copy(alpha = 0.55f), RoundedCornerShape(dimens.cornerRadiusSm))
                            .clickable(onClick = onResolveConflicting)
                            .padding(horizontal = 12.dp, vertical = 9.dp)
                            .testTag("resolve_failed_tx_button"),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            tint = RoseNeon,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isArabic) {
                                "حذف المعاملة الوهمية المسجلة لمنع التناقض ($conflictingLedgerCount)"
                            } else {
                                "Delete Ghost Ledger Entry to Fix Balance ($conflictingLedgerCount)"
                            },
                            color = RoseNeon,
                            fontSize = typo.bodySm,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(EmeraldNeon.copy(alpha = 0.13f))
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldNeon,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isArabic) {
                                "مستبعدة تلقائياً من المصاريف والدخل لمنع الخلط ✓"
                            } else {
                                "Auto-excluded from Expenses & Income ✓"
                            },
                            color = EmeraldNeon,
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
private fun FailedProofCell(
    label: String,
    value: String,
    valueColor: Color,
    mutedColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            color = mutedColor,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.ExtraBold,
            style = TextStyle(fontFeatureSettings = "tnum"),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SmsCleanArchiveRow(
    item: ArchivedAlgPosteSmsEntity,
    isArabic: Boolean,
    isStealth: Boolean,
    currency: String,
    colors: LiquidGlassColors
) {
    var showRaw by remember { mutableStateOf(false) }
    val typo = LocalAdaptiveTypography.current
    val dimens = LocalAdaptiveDimens.current
    val amountText = item.extractedTxAmount?.let { amt ->
        val isExp = item.extractedIsExpense ?: true
        val prefix = if (isExp) "-" else "+"
        val color = if (isExp) RoseNeon else EmeraldNeon
        (if (isStealth) "••••" else isolateLtr("$prefix${formatDzdAmount(amt, currency)}")) to color
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showRaw = !showRaw }
            .padding(horizontal = dimens.cardPadding, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = CyanNeon,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isArabic) item.sender.ifBlank { "بريد الجزائر" } else item.sender.ifBlank { "ALG Poste" },
                        color = colors.textPrimary,
                        fontSize = typo.body,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = isolateLtr(formatDate(item.receivedAtMillis, isArabic)),
                        color = colors.textMuted,
                        fontSize = typo.bodyXs,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                if (amountText != null) {
                    Text(
                        text = amountText.first,
                        color = amountText.second,
                        fontSize = typo.body,
                        fontWeight = FontWeight.ExtraBold,
                        style = TextStyle(fontFeatureSettings = "tnum"),
                        maxLines = 1
                    )
                }
                item.extractedBalance?.let { bal ->
                    Text(
                        text = if (isStealth) "••••" else "${if (isArabic) "رصيد:" else "Solde:"} ${isolateLtr(formatDzdAmount(bal, currency))}",
                        color = if (bal < 0.0) RoseNeon else colors.textMuted,
                        fontSize = typo.bodyXs,
                        style = TextStyle(fontFeatureSettings = "tnum"),
                        maxLines = 1
                    )
                }
            }
        }

        AnimatedVisibility(visible = showRaw) {
            Text(
                text = if (isStealth) "••••••" else isolateLtr(item.rawBody),
                color = colors.textSecondary,
                fontSize = typo.bodyXs,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun SmsManualTestAccordion(
    latestBalance: Double,
    manualSmsText: String,
    onManualSmsTextChange: (String) -> Unit,
    onAnalyzeManualSms: (sender: String, body: String) -> Unit,
    isArabic: Boolean,
    colors: LiquidGlassColors
) {
    var expanded by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = colors.glassCard,
        borderColor = colors.glassBorderSubtle,
        shape = RoundedCornerShape(18.dp),
        cornerRadius = 18.dp,
        breathingGlow = false
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = CyanNeon,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isArabic) "اختبار تحليل رسالة يدوياً" else "Manual SMS Analyzer",
                        color = colors.textPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = colors.textMuted,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(if (expanded) 180f else 0f)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.glassSurface)
                            .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        if (manualSmsText.isEmpty()) {
                            Text(
                                text = if (isArabic) "ألصق رسالة بريد الجزائر هنا..." else "Paste an ALG Poste SMS here...",
                                color = colors.textMuted.copy(alpha = 0.65f),
                                fontSize = 12.sp
                            )
                        }
                        BasicTextField(
                            value = manualSmsText,
                            onValueChange = onManualSmsTextChange,
                            textStyle = TextStyle(
                                color = colors.textPrimary,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                textDirection = TextDirection.Ltr
                            ),
                            cursorBrush = SolidColor(CyanNeon),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("manual_sms_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(RoseNeon.copy(alpha = 0.14f))
                                .clickable {
                                    onManualSmsTextChange("ALG Poste: Retrait GAB -300.00 DA le 15/11/2026. Nouveau solde: 24500.00 DA")
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isArabic) "نموذج سحب" else "Sample Withdraw",
                                color = RoseNeon,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(EmeraldNeon.copy(alpha = 0.14f))
                                .clickable {
                                    val base = if (latestBalance >= 1000.0) latestBalance else 24500.0
                                    val nextFmt = String.format(Locale.US, "%.2f", base + 15000.0)
                                    onManualSmsTextChange("ALG Poste: Virement reçu +15000.00 DA. Nouveau solde: $nextFmt DA")
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isArabic) "نموذج دخل" else "Sample Income",
                                color = EmeraldNeon,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    GlassButton(
                        text = if (isArabic) "تحليل الرسالة" else "Analyze SMS",
                        enabled = manualSmsText.isNotBlank(),
                        containerColor = CyanNeon,
                        contentColor = Color(0xFF04202C),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("analyze_manual_sms_button"),
                        onClick = {
                            onAnalyzeManualSms("ALG Poste", manualSmsText)
                            onManualSmsTextChange("")
                        }
                    )
                }
            }
        }
    }
}
