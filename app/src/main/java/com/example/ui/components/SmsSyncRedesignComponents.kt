package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HistoryToggleOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ArchivedAlgPosteSmsEntity
import com.example.sms.SmsBalanceSnapshot
import com.example.sms.SmsTaxAnomalyEntry
import com.example.ui.i18n.formatDate
import com.example.ui.i18n.formatDzdAmount
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
    ACTIVITY,
    BALANCES,
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
    val hasBalance = latestBalance >= 0.0
    val breathing = LocalBreathingGlowEnabled.current
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current
    val tnum = TextStyle(fontFeatureSettings = "tnum")

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sms_extracted_balance_card"),
        backgroundColor = colors.glassCard,
        borderColor = EmeraldNeon.copy(alpha = 0.42f),
        shape = RoundedCornerShape(dimens.cornerRadiusXl),
        cornerRadius = dimens.cornerRadiusXl,
        breathingGlow = breathing
    ) {
        Column(modifier = Modifier.padding(dimens.cardPadding)) {
            // Top bar: emblem + title + live status pill
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
                            .clip(RoundedCornerShape(dimens.cornerRadiusMd))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        EmeraldNeon.copy(alpha = 0.25f),
                                        CyanNeon.copy(alpha = 0.18f)
                                    )
                                )
                            )
                            .border(1.dp, EmeraldNeon.copy(alpha = 0.35f), RoundedCornerShape(dimens.cornerRadiusMd)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = EmeraldNeon,
                            modifier = Modifier.size(dimens.iconSizeMd)
                        )
                    }
                    Spacer(modifier = Modifier.width(dimens.spacingMd))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isArabic) "رصيد بريدي موب" else "BaridiMob Balance",
                            color = colors.textPrimary,
                            fontSize = typo.bodyLg,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val statusText = when {
                            !hasSmsPerm -> if (isArabic) "يتطلب إذن الرسائل" else "SMS access needed"
                            autoSyncEnabled -> if (isArabic) "مزامنة تلقائية نشطة" else "Live auto-sync active"
                            else -> if (isArabic) "المزامنة اليدوية" else "Manual sync mode"
                        }
                        Text(
                            text = statusText,
                            color = if (hasSmsPerm && autoSyncEnabled) EmeraldNeon else colors.textMuted,
                            fontSize = typo.bodyXs,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(dimens.spacingSm))

                // Subtle Control Drawer Toggle
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (controlsExpanded) CyanNeon.copy(alpha = 0.18f) else colors.glassSurface)
                        .border(
                            0.8.dp,
                            if (controlsExpanded) CyanNeon.copy(alpha = 0.5f) else colors.glassBorderSubtle,
                            CircleShape
                        )
                        .clickable(onClick = onToggleControls),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = if (isArabic) "إعدادات المزامنة" else "Sync controls",
                        tint = if (controlsExpanded) CyanNeon else colors.textSecondary,
                        modifier = Modifier.size(dimens.iconSizeMd)
                    )
                }
            }

            Spacer(modifier = Modifier.height(dimens.spacingLg))

            // Primary Hero Balance Value
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(
                    text = if (hasBalance) {
                        formatDzdAmount(latestBalance, currencyCode, isStealth)
                    } else {
                        "--- $currencyCode"
                    },
                    color = if (hasBalance) colors.textPrimary else colors.textMuted,
                    fontSize = typo.display,
                    fontWeight = FontWeight.ExtraBold,
                    style = tnum,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(dimens.spacingSm))

            // Clean metadata line: last updated + delta chip (FlowRow prevents overlap on compact phones)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (hasBalance && updatedAtMillis > 0L) {
                        (if (isArabic) "آخر تحديث: " else "Updated: ") + formatDate(updatedAtMillis)
                    } else {
                        if (isArabic) "اضغط تحديث لقراءة رصيدك تلقائياً" else "Tap Sync to read your latest Solde"
                    },
                    color = colors.textMuted,
                    fontSize = typo.bodySm,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (latestDelta != null && abs(latestDelta) > 0.01 && !isStealth) {
                    val isUp = latestDelta > 0
                    val dColor = if (isUp) EmeraldNeon else RoseNeon
                    val sign = if (isUp) "+" else "-"
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(dimens.cornerRadiusXs))
                                .background(dColor.copy(alpha = 0.14f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "$sign${formatDzdAmount(abs(latestDelta), currencyCode)}",
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

            Spacer(modifier = Modifier.height(dimens.spacingLg))

            // Single Unified Primary Sync CTA
            GlassButton(
                text = if (hasSmsPerm) {
                    if (isArabic) "تحديث وفحص الرسائل الآن" else "Sync Balance & Inbox"
                } else {
                    if (isArabic) "تفعيل قراءة رسائل ALG Poste" else "Enable ALG Poste SMS Sync"
                },
                icon = Icons.Default.Refresh,
                containerColor = EmeraldNeon,
                contentColor = Color(0xFF042419),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scan_alg_poste_sms_button"),
                onClick = onUnifiedSyncClick
            )

            // Smooth Expandable Sync Controls Drawer (Tucked away cleanly until needed!)
            AnimatedVisibility(
                visible = controlsExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.glassSurface.copy(alpha = 0.85f))
                        .border(0.8.dp, colors.glassBorderSubtle, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Auto-monitor toggle
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
                                text = if (isArabic) "اقتراح المعاملات فور وصول الرسالة" else "Suggest operations as SMS arrive",
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

                    // Baseline / Fresh Start controls (FlowRow avoids squishing on narrow phones)
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
                                    val dateFmt = remember(baselineMillis) {
                                        SimpleDateFormat("dd/MM HH:mm", Locale.US).format(Date(baselineMillis))
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
                                    .defaultMinSize(minHeight = 36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CyanNeon.copy(alpha = 0.16f))
                                    .border(0.8.dp, CyanNeon.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                                    .clickable(onClick = onSkipOldSms)
                                    .padding(horizontal = 10.dp, vertical = 7.dp)
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
                                        .defaultMinSize(minHeight = 36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(colors.glassCard)
                                        .border(0.8.dp, colors.glassBorderSubtle, RoundedCornerShape(10.dp))
                                        .clickable(onClick = onResetBaseline)
                                        .padding(horizontal = 10.dp, vertical = 7.dp)
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

@Composable
fun SmsWorkspaceSegmentedTabs(
    selectedTab: SmsWorkspaceTab,
    activityCount: Int,
    balancesCount: Int,
    archiveCount: Int,
    isArabic: Boolean,
    colors: LiquidGlassColors,
    onSelectTab: (SmsWorkspaceTab) -> Unit
) {
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current
    val tabs = listOf(
        Triple(
            SmsWorkspaceTab.ACTIVITY,
            if (isArabic) "النشاط ($activityCount)" else "Activity ($activityCount)",
            "sms_tab_activity"
        ),
        Triple(
            SmsWorkspaceTab.BALANCES,
            if (isArabic) "الأرصدة ($balancesCount)" else "Balances ($balancesCount)",
            "sms_tab_balances"
        ),
        Triple(
            SmsWorkspaceTab.ARCHIVE,
            if (isArabic) "الأرشيف ($archiveCount)" else "Archive ($archiveCount)",
            "sms_tab_archive"
        )
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimens.cornerRadiusMd))
            .background(colors.glassSurface)
            .border(0.8.dp, colors.glassBorderSubtle, RoundedCornerShape(dimens.cornerRadiusMd))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tabs.forEach { (tab, label, tag) ->
            val selected = selectedTab == tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 44.dp)
                    .clip(RoundedCornerShape(dimens.cornerRadiusSm))
                    .background(if (selected) EmeraldNeon.copy(alpha = 0.20f) else Color.Transparent)
                    .border(
                        width = if (selected) 0.9.dp else 0.dp,
                        color = if (selected) EmeraldNeon.copy(alpha = 0.50f) else Color.Transparent,
                        shape = RoundedCornerShape(dimens.cornerRadiusSm)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = EmeraldNeon),
                        onClick = { onSelectTab(tab) }
                    )
                    .padding(horizontal = 6.dp, vertical = 8.dp)
                    .testTag(tag),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (selected) EmeraldNeon else colors.textSecondary,
                    fontSize = typo.bodySm,
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
        Column(modifier = Modifier.padding(dimens.cardPaddingSmall)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
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
                        text = formatDate(anomaly.detectedAtMillis),
                        color = colors.textMuted,
                        fontSize = typo.bodyXs,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(
                        text = "-" + formatDzdAmount(anomaly.taxAmount, currency, isStealth),
                        color = RoseNeon,
                        fontSize = typo.bodyLg,
                        fontWeight = FontWeight.ExtraBold,
                        style = TextStyle(fontFeatureSettings = "tnum"),
                        maxLines = 1
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(44.dp)
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

@Composable
fun SmsCleanBalanceTimelineRow(
    snapshot: SmsBalanceSnapshot,
    delta: Double?,
    isArabic: Boolean,
    isStealth: Boolean,
    currency: String,
    colors: LiquidGlassColors
) {
    var peekRaw by remember { mutableStateOf(false) }
    val typo = LocalAdaptiveTypography.current
    val dimens = LocalAdaptiveDimens.current
    val deltaColor = when {
        delta == null -> colors.textMuted
        delta > 0.005 -> EmeraldNeon
        delta < -0.005 -> RoseNeon
        else -> colors.textMuted
    }
    val deltaLabel = when {
        isStealth -> "••••"
        delta == null -> if (isArabic) "بداية" else "Initial"
        delta > 0.005 -> "+" + formatDzdAmount(delta, currency)
        delta < -0.005 -> "-" + formatDzdAmount(abs(delta), currency)
        else -> if (isArabic) "ثابت" else "Same"
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
            Column(modifier = Modifier.weight(1f)) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(
                        text = formatDzdAmount(snapshot.balance, currency, isStealth),
                        color = colors.textPrimary,
                        fontSize = typo.titleSm,
                        fontWeight = FontWeight.ExtraBold,
                        style = TextStyle(fontFeatureSettings = "tnum"),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatDate(snapshot.timestampMillis),
                    color = colors.textMuted,
                    fontSize = typo.bodyXs,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(dimens.cornerRadiusXs))
                        .background(deltaColor.copy(alpha = 0.14f))
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = deltaLabel,
                        color = deltaColor,
                        fontSize = typo.bodySm,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(fontFeatureSettings = "tnum"),
                        maxLines = 1
                    )
                }
            }
        }

        AnimatedVisibility(visible = peekRaw && snapshot.rawBody.isNotBlank()) {
            Text(
                text = if (isStealth) "••••••" else snapshot.rawBody,
                color = colors.textMuted,
                fontSize = typo.bodyXs,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
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
        (if (isStealth) "••••" else "$prefix${formatDzdAmount(amt, currency)}") to color
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
                        text = item.sender.ifBlank { "ALG Poste" },
                        color = colors.textPrimary,
                        fontSize = typo.body,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatDate(item.receivedAtMillis),
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
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(
                            text = amountText.first,
                            color = amountText.second,
                            fontSize = typo.body,
                            fontWeight = FontWeight.ExtraBold,
                            style = TextStyle(fontFeatureSettings = "tnum"),
                            maxLines = 1
                        )
                    }
                }
                item.extractedBalance?.let { bal ->
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(
                            text = if (isStealth) "••••" else "${if (isArabic) "رصيد:" else "Solde:"} ${formatDzdAmount(bal, currency)}",
                            color = colors.textMuted,
                            fontSize = typo.bodyXs,
                            style = TextStyle(fontFeatureSettings = "tnum"),
                            maxLines = 1
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = showRaw) {
            Text(
                text = if (isStealth) "••••••" else item.rawBody,
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
                    .defaultMinSize(minHeight = 40.dp)
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
                                text = if (isArabic) "ألصق رسالة ALG Poste هنا..." else "Paste an ALG Poste SMS here...",
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
                                fontFamily = FontFamily.Monospace
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
