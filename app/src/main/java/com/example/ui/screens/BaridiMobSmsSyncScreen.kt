package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.data.ArchivedAlgPosteSmsEntity
import com.example.data.TransactionType
import com.example.data.UserSettings
import com.example.sms.AlgPosteSmsAnalyzer
import com.example.sms.ArchivedSmsStudyReport
import com.example.sms.SmsBalanceSnapshot
import com.example.sms.SmsSuggestionEntry
import com.example.sms.SmsTaxAnomalyEntry
import com.example.ui.components.GlassCard
import com.example.ui.components.LiquidEmptyStateCard
import com.example.ui.components.SmsAnnualTaxSummaryCard
import com.example.ui.components.SmsCleanArchiveRow
import com.example.ui.components.SmsCleanBalanceTimelineRow
import com.example.ui.components.SmsHeroBalanceCard
import com.example.ui.components.SmsManualTestAccordion
import com.example.ui.components.SmsTaxFeeCompactCard
import com.example.ui.components.SmsWorkspaceSegmentedTabs
import com.example.ui.components.SmsWorkspaceTab
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.ArabicStrings
import com.example.ui.i18n.LocalStealthMode
import com.example.ui.i18n.formatDate
import com.example.ui.i18n.formatDzd
import com.example.ui.i18n.isolateLtr
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.LocalAdaptiveDimens
import com.example.ui.theme.LocalAdaptiveLayoutInfo
import com.example.ui.theme.LocalAdaptiveTypography
import com.example.ui.theme.RoseNeon

@Composable
fun BaridiMobSmsSyncScreen(
    userSettings: UserSettings,
    pendingSuggestions: List<SmsSuggestionEntry>,
    detectedTaxes: List<SmsTaxAnomalyEntry> = emptyList(),
    balanceHistory: List<SmsBalanceSnapshot> = emptyList(),
    archivedSmsList: List<ArchivedAlgPosteSmsEntity> = emptyList(),
    archiveStudyReport: ArchivedSmsStudyReport = ArchivedSmsStudyReport(),
    onToggleSyncEnabled: (Boolean) -> Unit,
    onScanInboxNow: () -> Unit,
    onRefreshCurrentBalanceOnly: (showFeedback: Boolean) -> Unit = {},
    onClearBalanceHistory: () -> Unit = {},
    onReanalyzePermanentArchive: () -> Unit = {},
    onSkipOldSmsAndStartFresh: () -> Unit = {},
    onResetBaselineAndRescan: () -> Unit = {},
    onAnalyzeManualSms: (sender: String, body: String) -> Unit,
    onAcceptSuggestion: (suggestionId: String, overrideType: TransactionType?, customCategory: String?, customReasonOrSource: String?) -> Unit,
    onDismissSuggestion: (suggestionId: String) -> Unit,
    onClearAllSuggestions: () -> Unit,
    onRecordTaxAsExpense: (anomalyId: String) -> Unit = {},
    onDismissTaxAnomaly: (anomalyId: String) -> Unit = {},
    onClearAllTaxes: () -> Unit = {},
    onToggleAutoAddTaxes: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors
    val isArabic = strings is ArabicStrings
    val isStealth = LocalStealthMode.current
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current
    val layoutInfo = LocalAdaptiveLayoutInfo.current

    var hasReadSmsPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasReadSmsPerm = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_SMS
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val smsPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val readGranted = result[Manifest.permission.READ_SMS] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
                PackageManager.PERMISSION_GRANTED
        hasReadSmsPerm = readGranted
        if (readGranted) {
            onToggleSyncEnabled(true)
            onRefreshCurrentBalanceOnly(true)
            onScanInboxNow()
        }
    }

    LaunchedEffect(hasReadSmsPerm, userSettings.baridiMobSmsSyncEnabled) {
        if (hasReadSmsPerm) {
            onRefreshCurrentBalanceOnly(false)
            if (userSettings.baridiMobSmsSyncEnabled) {
                onScanInboxNow()
            }
        }
    }

    var activeTab by remember { mutableStateOf(SmsWorkspaceTab.TAXES) }
    var controlsExpanded by remember { mutableStateOf(false) }
    var manualSmsText by remember { mutableStateOf("") }
    var showAllBalanceHistory by remember { mutableStateOf(false) }
    var showAllPermanentArchive by remember { mutableStateOf(false) }

    val hasFreshBaseline = userSettings.smsSyncBaselineTimestampMillis > 0L

    val annualTaxesSummary = remember(detectedTaxes) {
        AlgPosteSmsAnalyzer.computeAnnualTaxesSummary(detectedTaxes)
    }
    val archiveTotalCount = archiveStudyReport.totalArchived.takeIf { it > 0 } ?: archivedSmsList.size
    val totalTransactionsCount = pendingSuggestions.size + balanceHistory.size

    val chronologicalHistory = remember(balanceHistory) {
        balanceHistory.sortedBy { it.timestampMillis }
    }
    val deltaBySnapshotId = remember(chronologicalHistory) {
        buildMap {
            for (i in chronologicalHistory.indices) {
                val curr = chronologicalHistory[i]
                val prev = chronologicalHistory.getOrNull(i - 1)
                put(curr.id, prev?.let { curr.balance - it.balance })
            }
        }
    }
    val newestHistoryFirst = remember(chronologicalHistory) {
        chronologicalHistory.asReversed()
    }
    val latestSnapshot = newestHistoryFirst.firstOrNull()
    val effectiveLatestBalance = latestSnapshot?.balance ?: userSettings.latestSmsBalanceAmount
    val effectiveUpdatedAt = latestSnapshot?.timestampMillis ?: userSettings.latestSmsBalanceUpdatedAt
    val latestDelta = latestSnapshot?.let { deltaBySnapshotId[it.id] }

    fun requestPermOrRun(action: () -> Unit) {
        if (!hasReadSmsPerm) {
            smsPermLauncher.launch(
                arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
            )
        } else {
            action()
        }
    }

    @Composable
    fun HeroControlSection() {
        SmsHeroBalanceCard(
            latestBalance = effectiveLatestBalance,
            latestDelta = latestDelta,
            updatedAtMillis = effectiveUpdatedAt,
            currencyCode = strings.currencyCode,
            isStealth = isStealth,
            hasSmsPerm = hasReadSmsPerm,
            autoSyncEnabled = userSettings.baridiMobSmsSyncEnabled,
            hasFreshBaseline = hasFreshBaseline,
            baselineMillis = userSettings.smsSyncBaselineTimestampMillis,
            controlsExpanded = controlsExpanded,
            isArabic = isArabic,
            colors = colors,
            onUnifiedSyncClick = {
                requestPermOrRun {
                    onRefreshCurrentBalanceOnly(false)
                    onScanInboxNow()
                }
            },
            onToggleControls = { controlsExpanded = !controlsExpanded },
            onToggleAutoSync = { checked ->
                if (checked && !hasReadSmsPerm) {
                    requestPermOrRun { onToggleSyncEnabled(true) }
                } else {
                    onToggleSyncEnabled(checked)
                }
            },
            onSkipOldSms = onSkipOldSmsAndStartFresh,
            onResetBaseline = onResetBaselineAndRescan
        )
    }

    @Composable
    fun SegmentedTabsSection() {
        SmsWorkspaceSegmentedTabs(
            selectedTab = activeTab,
            taxesCount = detectedTaxes.size,
            transactionsCount = totalTransactionsCount,
            archiveCount = archiveTotalCount,
            isArabic = isArabic,
            colors = colors,
            onSelectTab = { activeTab = it }
        )
    }

    @Composable
    fun TransactionsTimelineCardSection() {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("sms_transactions_ledger_card"),
            backgroundColor = colors.glassCard,
            borderColor = colors.glassBorderSubtle,
            shape = RoundedCornerShape(dimens.cornerRadiusLg),
            cornerRadius = dimens.cornerRadiusLg,
            breathingGlow = false
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimens.cardPadding, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = EmeraldNeon,
                            modifier = Modifier.size(dimens.iconSizeSm)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isArabic) "سجل المعاملات والأرصدة (${newestHistoryFirst.size})"
                            else "Transactions & Balance Log (${newestHistoryFirst.size})",
                            color = colors.textPrimary,
                            fontSize = typo.titleSm,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (balanceHistory.isNotEmpty()) {
                        IconButton(
                            onClick = onClearBalanceHistory,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = if (isArabic) "مسح السجل" else "Clear history",
                                tint = colors.textMuted,
                                modifier = Modifier.size(dimens.iconSizeSm)
                            )
                        }
                    }
                }

                if (newestHistoryFirst.isEmpty()) {
                    HorizontalDivider(color = colors.glassBorderSubtle.copy(alpha = 0.45f))
                    Text(
                        text = if (isArabic) "لا توجد أرصدة مسجلة بعد — اضغط تحديث في الأعلى لقراءة رسائلك."
                        else "No balances logged yet — tap Sync above to scan your inbox.",
                        color = colors.textMuted,
                        fontSize = typo.bodySm,
                        modifier = Modifier.padding(dimens.cardPadding)
                    )
                } else {
                    val visible = if (showAllBalanceHistory) newestHistoryFirst else newestHistoryFirst.take(8)
                    visible.forEach { snap ->
                        HorizontalDivider(
                            color = colors.glassBorderSubtle.copy(alpha = 0.4f),
                            thickness = 0.6.dp
                        )
                        SmsCleanBalanceTimelineRow(
                            snapshot = snap,
                            delta = deltaBySnapshotId[snap.id],
                            isArabic = isArabic,
                            isStealth = isStealth,
                            currency = strings.currencyCode,
                            colors = colors
                        )
                    }
                    if (newestHistoryFirst.size > 8) {
                        HorizontalDivider(color = colors.glassBorderSubtle.copy(alpha = 0.4f))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .clickable { showAllBalanceHistory = !showAllBalanceHistory }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (showAllBalanceHistory) {
                                    if (isArabic) "عرض أحدث 8 فقط" else "Show Latest 8"
                                } else {
                                    if (isArabic) "عرض الكل (${newestHistoryFirst.size})" else "Show All (${newestHistoryFirst.size})"
                                },
                                color = CyanNeon,
                                fontSize = typo.bodySm,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun ArchiveVaultCardSection() {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("permanent_alg_poste_archive_card"),
            backgroundColor = colors.glassCard,
            borderColor = colors.glassBorderSubtle,
            shape = RoundedCornerShape(dimens.cornerRadiusLg),
            cornerRadius = dimens.cornerRadiusLg,
            breathingGlow = false
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimens.cardPadding, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = CyanNeon,
                            modifier = Modifier.size(dimens.iconSizeSm)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (isArabic) "الخزنة الدائمة للرسائل" else "Permanent SMS Vault",
                                    color = colors.textPrimary,
                                    fontSize = typo.titleSm,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(EmeraldNeon.copy(alpha = 0.14f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = EmeraldNeon,
                                            modifier = Modifier.size(9.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "${archivedSmsList.size}",
                                            color = EmeraldNeon,
                                            fontSize = typo.caption,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Text(
                                text = if (isArabic) "محفوظة بأمان حتى لو حُذفت من الهاتف"
                                else "Safely kept even if deleted from phone",
                                color = colors.textMuted,
                                fontSize = typo.bodyXs,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CyanNeon.copy(alpha = 0.15f))
                            .border(0.8.dp, CyanNeon.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .clickable(onClick = onReanalyzePermanentArchive)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("reanalyze_permanent_archive_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = CyanNeon,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isArabic) "تحليل" else "Re-study",
                                color = CyanNeon,
                                fontSize = typo.bodyXs,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }

                if (archivedSmsList.isEmpty()) {
                    HorizontalDivider(color = colors.glassBorderSubtle.copy(alpha = 0.45f))
                    Text(
                        text = if (isArabic) "سيتم حفظ رسائل بريد الجزائر هنا تلقائياً عند فحصها."
                        else "Detected ALG Poste messages are securely archived here.",
                        color = colors.textMuted,
                        fontSize = typo.bodySm,
                        modifier = Modifier.padding(dimens.cardPadding)
                    )
                } else {
                    val visibleArchive = if (showAllPermanentArchive) archivedSmsList else archivedSmsList.take(8)
                    visibleArchive.forEach { entity ->
                        HorizontalDivider(
                            color = colors.glassBorderSubtle.copy(alpha = 0.4f),
                            thickness = 0.6.dp
                        )
                        SmsCleanArchiveRow(
                            item = entity,
                            isArabic = isArabic,
                            isStealth = isStealth,
                            currency = strings.currencyCode,
                            colors = colors
                        )
                    }
                    if (archivedSmsList.size > 8) {
                        HorizontalDivider(color = colors.glassBorderSubtle.copy(alpha = 0.4f))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .clickable { showAllPermanentArchive = !showAllPermanentArchive }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (showAllPermanentArchive) {
                                    if (isArabic) "عرض أحدث 8 فقط" else "Show Latest 8"
                                } else {
                                    if (isArabic) "عرض الكل (${archivedSmsList.size})" else "Show All (${archivedSmsList.size})"
                                },
                                color = CyanNeon,
                                fontSize = typo.bodySm,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    fun androidx.compose.foundation.lazy.LazyListScope.renderActiveTabItems() {
        when (activeTab) {
            SmsWorkspaceTab.TAXES -> {
                item {
                    SmsAnnualTaxSummaryCard(
                        summary = annualTaxesSummary,
                        currencyCode = strings.currencyCode,
                        isStealth = isStealth,
                        isArabic = isArabic,
                        colors = colors
                    )
                }

                item {
                    AutoAddTaxesPreferenceCard(
                        enabled = userSettings.autoAddTaxesEnabled,
                        baselineMillis = userSettings.autoAddTaxesBaselineMillis,
                        isArabic = isArabic,
                        onToggle = onToggleAutoAddTaxes
                    )
                }

                if (detectedTaxes.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(2.dp))
                        SectionActionHeader(
                            title = if (isArabic) "الضرائب والاقتطاعات المرصودة (${detectedTaxes.size})"
                            else "Detected Fees & Taxes (${detectedTaxes.size})",
                            actionLabel = if (isArabic) "مسح الكل" else "Clear",
                            actionColor = AmberNeon,
                            onActionClick = onClearAllTaxes
                        )
                    }

                    items(detectedTaxes, key = { "tax_${it.id}" }) { anomaly ->
                        SmsTaxFeeCompactCard(
                            anomaly = anomaly,
                            isArabic = isArabic,
                            isStealth = isStealth,
                            currency = strings.currencyCode,
                            colors = colors,
                            onRecordAsExpense = { onRecordTaxAsExpense(anomaly.id) },
                            onDismiss = { onDismissTaxAnomaly(anomaly.id) }
                        )
                    }
                } else {
                    item {
                        LiquidEmptyStateCard(
                            icon = Icons.Default.MarkEmailRead,
                            title = if (isArabic) "لا توجد ضرائب أو اقتطاعات مرصودة ✓" else "No Caught Taxes or Fees ✓",
                            description = if (isArabic) {
                                "أي اقتطاع ضريبي أو رسوم بنكية خفية منذ ${annualTaxesSummary.referenceStartDateLabel} ستُحسب تلقائياً في المجموع السنوي وتظهر هنا."
                            } else {
                                "Any hidden fees or bank taxes caught since ${annualTaxesSummary.referenceStartDateLabel} will be tallied above and listed here."
                            },
                            iconColor = AmberNeon
                        )
                    }
                }
            }

            SmsWorkspaceTab.TRANSACTIONS -> {
                if (pendingSuggestions.isNotEmpty()) {
                    item {
                        SectionActionHeader(
                            title = if (isArabic) "معاملات بانتظار الإضافة (${pendingSuggestions.size})"
                            else "Pending SMS Transactions (${pendingSuggestions.size})",
                            actionLabel = if (isArabic) "تجاهل الكل" else "Clear All",
                            actionColor = RoseNeon,
                            onActionClick = onClearAllSuggestions
                        )
                    }

                    items(pendingSuggestions, key = { it.id }) { suggestion ->
                        SmsSuggestionCard(
                            suggestion = suggestion,
                            isArabic = isArabic,
                            onAccept = { chosenType, chosenCategory, chosenReason ->
                                onAcceptSuggestion(suggestion.id, chosenType, chosenCategory, chosenReason)
                            },
                            onDismiss = { onDismissSuggestion(suggestion.id) }
                        )
                    }
                }

                if (newestHistoryFirst.isNotEmpty() || pendingSuggestions.isEmpty()) {
                    item { TransactionsTimelineCardSection() }
                }
            }

            SmsWorkspaceTab.ARCHIVE -> {
                item { ArchiveVaultCardSection() }
                if (layoutInfo.columns < 2) {
                    item {
                        SmsManualTestAccordion(
                            latestBalance = effectiveLatestBalance,
                            manualSmsText = manualSmsText,
                            onManualSmsTextChange = { manualSmsText = it },
                            onAnalyzeManualSms = onAnalyzeManualSms,
                            isArabic = isArabic,
                            colors = colors
                        )
                    }
                }
            }
        }
    }

    if (layoutInfo.columns >= 2) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = dimens.screenPaddingH),
            horizontalArrangement = Arrangement.spacedBy(dimens.spacingLg)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(0.92f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(dimens.spacingMd)
            ) {
                item {
                    Spacer(modifier = Modifier.height(dimens.spacingXs))
                    HeroControlSection()
                }
                item { SegmentedTabsSection() }
                item {
                    SmsManualTestAccordion(
                        latestBalance = effectiveLatestBalance,
                        manualSmsText = manualSmsText,
                        onManualSmsTextChange = { manualSmsText = it },
                        onAnalyzeManualSms = onAnalyzeManualSms,
                        isArabic = isArabic,
                        colors = colors
                    )
                }
                item { Spacer(modifier = Modifier.height(dimens.spacingXxl)) }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1.08f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(dimens.spacingMd)
            ) {
                item { Spacer(modifier = Modifier.height(dimens.spacingXs)) }
                renderActiveTabItems()
                item { Spacer(modifier = Modifier.height(dimens.spacingXxl)) }
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = dimens.screenPaddingH),
            verticalArrangement = Arrangement.spacedBy(dimens.spacingMd)
        ) {
            item {
                Spacer(modifier = Modifier.height(dimens.spacingXs))
                HeroControlSection()
            }
            item { SegmentedTabsSection() }
            renderActiveTabItems()
            item { Spacer(modifier = Modifier.height(dimens.spacingXxl)) }
        }
    }
}


@Composable
private fun SectionActionHeader(
    title: String,
    actionLabel: String,
    actionColor: androidx.compose.ui.graphics.Color,
    onActionClick: () -> Unit
) {
    val colors = LiquidGlassTheme.colors
    val typo = LocalAdaptiveTypography.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = colors.textPrimary,
            fontSize = typo.bodyLg,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(actionColor.copy(alpha = 0.13f))
                .clickable(onClick = onActionClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = actionLabel,
                color = actionColor,
                fontSize = typo.bodyXs,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun SmsSuggestionCard(
    suggestion: SmsSuggestionEntry,
    isArabic: Boolean,
    onAccept: (TransactionType, String?, String?) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LiquidGlassTheme.colors
    var selectedType by remember(suggestion.id, suggestion.isExpense) {
        mutableStateOf(suggestion.transactionType)
    }
    var selectedPresetKey by remember(suggestion.id, selectedType) {
        mutableStateOf(
            com.example.ui.components.SmsSuggestionReasonCatalog.defaultPresetKey(
                isExpense = selectedType == TransactionType.EXPENSE,
                suggestedCategory = if (selectedType == suggestion.transactionType) {
                    suggestion.suggestedCategory
                } else {
                    if (selectedType == TransactionType.EXPENSE) "other" else "transfer"
                }
            )
        )
    }
    var customReasonText by remember(suggestion.id, selectedType) { mutableStateOf("") }
    var showRawText by remember(suggestion.id) { mutableStateOf(false) }

    val isExpense = selectedType == TransactionType.EXPENSE
    val accent = if (isExpense) RoseNeon else EmeraldNeon

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sms_suggestion_card"),
        backgroundColor = colors.glassCard,
        borderColor = accent.copy(alpha = 0.38f),
        shape = RoundedCornerShape(20.dp),
        cornerRadius = 20.dp,
        breathingGlow = false
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isExpense) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isArabic) suggestion.suggestedTitleAr else suggestion.suggestedTitleEn,
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(accent.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (selectedType == suggestion.transactionType) {
                                    if (isExpense) {
                                        if (isArabic) "تصنيف تلقائي: مصروف (سحب)" else "Auto: Expense (Withdrawal)"
                                    } else {
                                        if (isArabic) "تصنيف تلقائي: دخل (رصيد وارد)" else "Auto: Income (Credit)"
                                    }
                                } else {
                                    if (isExpense) {
                                        if (isArabic) "كمصروف" else "As Expense"
                                    } else {
                                        if (isArabic) "كدخل" else "As Income"
                                    }
                                },
                                color = accent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                        Text(
                            text = "${suggestion.sender} • ${isolateLtr(formatDate(suggestion.receivedAtMillis, isArabic))}",
                            color = colors.textMuted,
                            fontSize = 10.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(
                            text = (if (isExpense) "-" else "+") + formatDzd(suggestion.amount),
                            color = accent,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            style = TextStyle(fontFeatureSettings = "tnum")
                        )
                    }
                    if (suggestion.extractedBalance != null) {
                        Text(
                            text = if (isArabic) "رصيد: ${isolateLtr(formatDzd(suggestion.extractedBalance))}"
                            else "Bal: ${formatDzd(suggestion.extractedBalance)}",
                            color = colors.textMuted,
                            fontSize = 10.sp,
                            style = TextStyle(fontFeatureSettings = "tnum")
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

            // Optional discreet toggle for raw SMS body (clean by default!)
            if (suggestion.rawBody.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (showRawText) {
                        if (isArabic) "إخفاء نص الرسالة ▲" else "Hide SMS text ▲"
                    } else {
                        if (isArabic) "عرض نص الرسالة ▼" else "Show SMS text ▼"
                    },
                    color = colors.textMuted,
                    fontSize = 10.5.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { showRawText = !showRawText }
                        .padding(vertical = 6.dp, horizontal = 4.dp)
                )
                AnimatedVisibility(visible = showRawText) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(
                            text = suggestion.rawBody,
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.glassSurface)
                                .padding(10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            com.example.ui.components.SmsSuggestionReasonPicker(
                isExpense = isExpense,
                isArabic = isArabic,
                selectedPresetKey = selectedPresetKey,
                onSelectPreset = { preset ->
                    selectedPresetKey = if (selectedPresetKey == preset.key) null else preset.key
                },
                customReasonText = customReasonText,
                onCustomReasonTextChange = { customReasonText = it },
                accent = accent
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.glassSurface)
                        .border(0.8.dp, colors.glassBorderSubtle, RoundedCornerShape(12.dp))
                        .clickable {
                            selectedType = if (selectedType == TransactionType.EXPENSE) {
                                TransactionType.INCOME
                            } else {
                                TransactionType.EXPENSE
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isExpense) {
                                if (isArabic) "كدخل" else "Income"
                            } else {
                                if (isArabic) "كمصروف" else "Expense"
                            },
                            color = colors.textSecondary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.20f))
                        .border(0.9.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                        .clickable {
                            val chosenPreset = com.example.ui.components.SmsSuggestionReasonCatalog.findPreset(
                                isExpense = isExpense,
                                key = selectedPresetKey
                            )
                            val resolvedReason = customReasonText.trim().ifBlank {
                                chosenPreset?.localizedLabel(isArabic)
                            }
                            onAccept(
                                selectedType,
                                chosenPreset?.categoryId,
                                resolvedReason
                            )
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("accept_sms_suggestion_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isExpense) {
                                if (isArabic) "إضافة في المصاريف" else "Add Expense"
                            } else {
                                if (isArabic) "إضافة في الدخل" else "Add Income"
                            },
                            color = accent,
                            fontSize = 12.5.sp,
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
private fun AutoAddTaxesPreferenceCard(
    enabled: Boolean,
    baselineMillis: Long,
    isArabic: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val colors = LiquidGlassTheme.colors
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("baridimob_auto_add_taxes_card"),
        backgroundColor = colors.glassCard,
        borderColor = if (enabled) EmeraldNeon.copy(alpha = 0.45f) else colors.glassBorderSubtle,
        shape = RoundedCornerShape(dimens.cornerRadiusLg),
        cornerRadius = dimens.cornerRadiusLg,
        breathingGlow = false
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle(!enabled) }
                .padding(horizontal = dimens.cardPadding, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(
                            if (enabled) EmeraldNeon.copy(alpha = 0.16f)
                            else AmberNeon.copy(alpha = 0.14f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = null,
                        tint = if (enabled) EmeraldNeon else AmberNeon,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isArabic) "إضافة الضرائب تلقائياً"
                        else "Auto Add Taxes",
                        color = colors.textPrimary,
                        fontSize = typo.bodyLg,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when {
                            enabled && baselineMillis > 0L -> if (isArabic) {
                                "مفعّل منذ ${formatDate(baselineMillis, true)} • تُضاف المستقبلية فقط (بدون السابقة)"
                            } else {
                                "Active since ${formatDate(baselineMillis, false)} • Future taxes only (past skipped)"
                            }
                            enabled -> if (isArabic) {
                                "مفعّل • تُضاف الضرائب المستقبلية فقط إلى المصاريف"
                            } else {
                                "Enabled • Only future taxes are logged to Expenses"
                            }
                            else -> if (isArabic) {
                                "معطّل • عند التفعيل تُسجّل الضرائب المستقبلية فقط"
                            } else {
                                "Disabled • Enabling auto-logs future taxes only"
                            }
                        },
                        color = colors.textMuted,
                        fontSize = typo.bodyXs
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                modifier = Modifier.testTag("baridimob_auto_add_taxes_switch"),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = EmeraldNeon,
                    checkedTrackColor = EmeraldNeon.copy(alpha = 0.35f)
                )
            )
        }
    }
}
