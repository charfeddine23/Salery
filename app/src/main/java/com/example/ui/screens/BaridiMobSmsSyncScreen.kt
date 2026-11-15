package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.HistoryToggleOff
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.TransactionType
import com.example.data.UserSettings
import com.example.sms.SmsSuggestionEntry
import com.example.sms.SmsTaxAnomalyEntry
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.LiquidEmptyStateCard
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.ArabicStrings
import com.example.ui.i18n.LocalStealthMode
import com.example.ui.i18n.formatDate
import com.example.ui.i18n.formatDzd
import com.example.ui.i18n.formatDzdAmount
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.LocalAdaptiveDimens
import com.example.ui.theme.LocalAdaptiveTypography
import com.example.ui.theme.RoseNeon
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BaridiMobSmsSyncScreen(
    userSettings: UserSettings,
    pendingSuggestions: List<SmsSuggestionEntry>,
    detectedTaxes: List<SmsTaxAnomalyEntry> = emptyList(),
    onToggleSyncEnabled: (Boolean) -> Unit,
    onScanInboxNow: () -> Unit,
    onRefreshCurrentBalanceOnly: () -> Unit = {},
    onSkipOldSmsAndStartFresh: () -> Unit = {},
    onResetBaselineAndRescan: () -> Unit = {},
    onAnalyzeManualSms: (sender: String, body: String) -> Unit,
    onAcceptSuggestion: (suggestionId: String, overrideType: TransactionType?) -> Unit,
    onDismissSuggestion: (suggestionId: String) -> Unit,
    onClearAllSuggestions: () -> Unit,
    onRecordTaxAsExpense: (anomalyId: String) -> Unit = {},
    onDismissTaxAnomaly: (anomalyId: String) -> Unit = {},
    onClearAllTaxes: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors
    val isArabic = strings is ArabicStrings
    val isStealth = LocalStealthMode.current
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current

    var hasReadSmsPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
                PackageManager.PERMISSION_GRANTED
        )
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
            onScanInboxNow()
        }
    }

    LaunchedEffect(hasReadSmsPerm, userSettings.baridiMobSmsSyncEnabled) {
        if (hasReadSmsPerm && userSettings.baridiMobSmsSyncEnabled) {
            onScanInboxNow()
        }
    }

    var manualSmsText by remember { mutableStateOf("") }
    val hasFreshBaseline = userSettings.smsSyncBaselineTimestampMillis > 0L
    val totalCaughtTaxes = remember(detectedTaxes) {
        detectedTaxes.sumOf { it.taxAmount }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = dimens.screenPaddingH),
        verticalArrangement = Arrangement.spacedBy(dimens.spacingMd)
    ) {
        // 1. Extracted Current Balance from BaridiMob SMS Card
        item {
            Spacer(modifier = Modifier.height(dimens.spacingXs))

            val hasBalance = userSettings.latestSmsBalanceAmount >= 0.0
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sms_extracted_balance_card"),
                backgroundColor = colors.glassCard,
                borderColor = EmeraldNeon.copy(alpha = 0.48f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                EmeraldNeon.copy(alpha = 0.25f),
                                                CyanNeon.copy(alpha = 0.20f)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = EmeraldNeon,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isArabic) "رصيد البطاقة المستخرج من الرسائل"
                                    else "Extracted BaridiMob Balance",
                                    color = colors.textSecondary,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Text(
                                        text = if (hasBalance) {
                                            formatDzdAmount(
                                                amount = userSettings.latestSmsBalanceAmount,
                                                currencyText = strings.currencyCode,
                                                isStealth = isStealth
                                            )
                                        } else {
                                            if (isArabic) "غير مستخرج بعد" else "Not extracted yet"
                                        },
                                        color = if (hasBalance) EmeraldNeon else colors.textMuted,
                                        fontSize = if (hasBalance) 21.sp else 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(11.dp))
                                .background(EmeraldNeon.copy(alpha = 0.16f))
                                .border(0.9.dp, EmeraldNeon.copy(alpha = 0.45f), RoundedCornerShape(11.dp))
                                .clickable {
                                    if (!hasReadSmsPerm) {
                                        smsPermLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.READ_SMS,
                                                Manifest.permission.RECEIVE_SMS
                                            )
                                        )
                                    } else {
                                        onRefreshCurrentBalanceOnly()
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                .testTag("extract_balance_sms_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = EmeraldNeon,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isArabic) "تحديث الرصيد" else "Read Solde",
                                    color = EmeraldNeon,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (hasBalance && userSettings.latestSmsBalanceUpdatedAt > 0L) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.glassSurface)
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = userSettings.latestSmsBalanceRawSnippet.ifBlank {
                                    if (isArabic) "آخر رصيد مرصود من ALG Poste" else "Latest ALG Poste Solde"
                                },
                                color = colors.textSecondary,
                                fontSize = 10.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = formatDate(userSettings.latestSmsBalanceUpdatedAt),
                                color = colors.textMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        // 2. Hero Sync Control & Skip Old Messages (Start Fresh) Card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = colors.glassCard,
                borderColor = Color(0xFF818CF8).copy(alpha = 0.45f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF818CF8).copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sms,
                                contentDescription = null,
                                tint = Color(0xFF818CF8),
                                modifier = Modifier.size(23.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "BaridiMob SMS & Alert Sync",
                                color = colors.textPrimary,
                                fontSize = typo.titleSm,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = if (isArabic) {
                                    "تحليل رسائل ALG Poste داخل البطاقة الجزائرية — السحب (-) مصروف والدخل (+) دخل"
                                } else {
                                    "Inside Algerian Card: ALG Poste (-) -> Expense, (+) -> Income & Tax radar"
                                },
                                color = colors.textMuted,
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Auto-Sync Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.glassSurface)
                            .clickable {
                                if (!hasReadSmsPerm) {
                                    smsPermLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.READ_SMS,
                                            Manifest.permission.RECEIVE_SMS
                                        )
                                    )
                                } else {
                                    onToggleSyncEnabled(!userSettings.baridiMobSmsSyncEnabled)
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isArabic) "القراءة والمراقبة التلقائية للرسائل الجديدة"
                                else "Auto-detect new ALG Poste SMS",
                                color = colors.textPrimary,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (hasReadSmsPerm) {
                                    if (isArabic) "صلاحية SMS مفعّلة ✓" else "SMS permission granted ✓"
                                } else {
                                    if (isArabic) "يتطلب صلاحية قراءة الرسائل القصيرة SMS" else "Requires SMS permission"
                                },
                                color = if (hasReadSmsPerm) EmeraldNeon else RoseNeon,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Switch(
                            checked = userSettings.baridiMobSmsSyncEnabled && hasReadSmsPerm,
                            onCheckedChange = { checked ->
                                if (checked && !hasReadSmsPerm) {
                                    smsPermLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.READ_SMS,
                                            Manifest.permission.RECEIVE_SMS
                                        )
                                    )
                                } else {
                                    onToggleSyncEnabled(checked)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = EmeraldNeon,
                                checkedTrackColor = EmeraldNeon.copy(alpha = 0.35f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Baseline Status + Skip Old SMS Button ("البدء من الصفر")
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (hasFreshBaseline) CyanNeon.copy(alpha = 0.10f)
                                else colors.glassSurface
                            )
                            .border(
                                1.dp,
                                if (hasFreshBaseline) CyanNeon.copy(alpha = 0.45f) else colors.glassBorderSubtle,
                                RoundedCornerShape(14.dp)
                            )
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                    imageVector = Icons.Default.HistoryToggleOff,
                                    contentDescription = null,
                                    tint = if (hasFreshBaseline) CyanNeon else AmberNeon,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (hasFreshBaseline) {
                                            if (isArabic) "وضع البدء من الصفر مفعّل ✓" else "Fresh Start Active ✓"
                                        } else {
                                            if (isArabic) "تصفية الرسائل القديمة" else "Historical SMS Filter"
                                        },
                                        color = if (hasFreshBaseline) CyanNeon else colors.textPrimary,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val timeFmt = remember(userSettings.smsSyncBaselineTimestampMillis) {
                                        if (hasFreshBaseline) {
                                            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US)
                                                .format(Date(userSettings.smsSyncBaselineTimestampMillis))
                                        } else ""
                                    }
                                    Text(
                                        text = if (hasFreshBaseline) {
                                            if (isArabic) "تجاهل كل ما قبل $timeFmt • أي رسالة جديدة ستُقترح فوراً"
                                            else "Ignoring SMS before $timeFmt • New SMS suggest immediately"
                                        } else {
                                            if (isArabic) "تخطي الرسائل القديمة وبدء العد من 0 للمستقبل فقط"
                                            else "Skip old inbox messages and start counting from 0"
                                        },
                                        color = colors.textMuted,
                                        fontSize = 10.5.sp,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(CyanNeon.copy(alpha = 0.18f))
                                    .border(0.9.dp, CyanNeon.copy(alpha = 0.55f), RoundedCornerShape(11.dp))
                                    .clickable { onSkipOldSmsAndStartFresh() }
                                    .padding(horizontal = 10.dp, vertical = 10.dp)
                                    .testTag("skip_old_sms_button"),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RestartAlt,
                                    contentDescription = null,
                                    tint = CyanNeon,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isArabic) "تخطي القديم والبدء من الصفر"
                                    else "Skip Old & Start From 0",
                                    color = CyanNeon,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (hasFreshBaseline) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(colors.glassCard)
                                        .border(0.8.dp, colors.glassBorderSubtle, RoundedCornerShape(11.dp))
                                        .clickable { onResetBaselineAndRescan() }
                                        .padding(horizontal = 10.dp, vertical = 10.dp)
                                        .testTag("reset_sms_baseline_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isArabic) "فحص الكل" else "Scan All",
                                        color = colors.textSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    GlassButton(
                        text = if (hasReadSmsPerm) {
                            if (isArabic) "فحص رسائل ALG Poste الآن" else "Scan ALG Poste SMS Inbox"
                        } else {
                            if (isArabic) "منح صلاحية SMS وفحص الرسائل" else "Grant SMS Access & Scan"
                        },
                        icon = Icons.Default.Refresh,
                        containerColor = Color(0xFF818CF8),
                        contentColor = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("scan_alg_poste_sms_button"),
                        onClick = {
                            if (!hasReadSmsPerm) {
                                smsPermLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.READ_SMS,
                                        Manifest.permission.RECEIVE_SMS
                                    )
                                )
                            } else {
                                onScanInboxNow()
                            }
                        }
                    )
                }
            }
        }

        // 3. Tax & Balance Discrepancy Hunter Section (كاشف الفارق وتصيد الضرائب)
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tax_discrepancy_hunter_card"),
                backgroundColor = colors.glassCard,
                borderColor = AmberNeon.copy(alpha = 0.45f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
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
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(AmberNeon.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Policy,
                                    contentDescription = null,
                                    tint = AmberNeon,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isArabic) "كاشف الفارق وتصيد الضرائب"
                                    else "Tax & Fee Discrepancy Hunter",
                                    color = colors.textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = if (isArabic) {
                                        "مقارنة الرصيد السابق ± العملية مع الرصيد الفعلي لاقتناص الضرائب والرسوم الخفية"
                                    } else {
                                        "Compares previous vs actual Solde to catch hidden taxes & commissions"
                                    },
                                    color = colors.textMuted,
                                    fontSize = 10.5.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        }

                        if (detectedTaxes.isNotEmpty()) {
                            IconButton(
                                onClick = onClearAllTaxes,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = if (isArabic) "مسح" else "Clear",
                                    tint = colors.textMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Total Caught Taxes Summary Pill
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AmberNeon.copy(alpha = 0.12f))
                            .border(0.8.dp, AmberNeon.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isArabic) "إجمالي الضرائب/الفوارق المرصودة (${detectedTaxes.size})"
                            else "Total Caught Taxes & Fees (${detectedTaxes.size})",
                            color = colors.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(
                                text = formatDzdAmount(
                                    amount = totalCaughtTaxes,
                                    currencyText = strings.currencyCode,
                                    isStealth = isStealth
                                ),
                                color = AmberNeon,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    if (detectedTaxes.isEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (isArabic) {
                                "لم يتم رصد أي فارق أو ضريبة خفية بعد. عند استلام رسالتين تحتويان على الرصيد (Solde)، يحسب المحرك الفرق تلقائياً ليكشف أي اقتطاع."
                            } else {
                                "No hidden fees detected yet. Once consecutive SMS alerts include a Solde balance, any unexplained discrepancy is caught here automatically."
                            },
                            color = colors.textMuted,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    } else {
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            detectedTaxes.forEach { anomaly ->
                                TaxAnomalyItemCard(
                                    anomaly = anomaly,
                                    isArabic = isArabic,
                                    isStealth = isStealth,
                                    currency = strings.currencyCode,
                                    onRecordAsExpense = { onRecordTaxAsExpense(anomaly.id) },
                                    onDismiss = { onDismissTaxAnomaly(anomaly.id) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Pending Suggestions Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isArabic) "المعاملات المقترحة (${pendingSuggestions.size})"
                    else "Suggested Transactions (${pendingSuggestions.size})",
                    color = colors.textPrimary,
                    fontSize = typo.titleSm,
                    fontWeight = FontWeight.Bold
                )
                if (pendingSuggestions.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(RoseNeon.copy(alpha = 0.14f))
                            .clickable(onClick = onClearAllSuggestions)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            tint = RoseNeon,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isArabic) "مسح الكل" else "Clear All",
                            color = RoseNeon,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (pendingSuggestions.isEmpty()) {
            item {
                LiquidEmptyStateCard(
                    icon = Icons.Default.MarkEmailRead,
                    title = if (isArabic) "لا توجد اقتراحات جديدة حالياً" else "No Pending SMS Suggestions",
                    description = if (isArabic) {
                        "عند وصول أي رسالة من ALG Poste (مثل -300.00 DA للسحب أو +5000.00 DA للدخل) سيحللها النظام ويقترح إضافتها هنا مباشرة."
                    } else {
                        "When any SMS arrives from ALG Poste (e.g., -300.00 DA withdrawal or incoming deposit), it appears here for 1-tap adding."
                    },
                    iconColor = Color(0xFF818CF8)
                )
            }
        } else {
            items(pendingSuggestions, key = { it.id }) { suggestion ->
                SmsSuggestionCard(
                    suggestion = suggestion,
                    isArabic = isArabic,
                    onAccept = { chosenType -> onAcceptSuggestion(suggestion.id, chosenType) },
                    onDismiss = { onDismissSuggestion(suggestion.id) }
                )
            }
        }

        // 5. Manual Paste / Quick Analyzer & Tax Discrepancy Tester
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = colors.glassCard,
                borderColor = CyanNeon.copy(alpha = 0.30f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = CyanNeon,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isArabic) "تحليل رسالة ALG Poste يدوياً" else "Analyze ALG Poste SMS Manually",
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (isArabic) "الصق نص رسالة من ALG Poste لاختبار تصنيفها، استخراج الرصيد، ورصد الفارق الضريبي:"
                        else "Paste an ALG Poste SMS to test classification, Solde extraction, and tax hunting:",
                        color = colors.textMuted,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.glassSurface)
                            .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        if (manualSmsText.isEmpty()) {
                            Text(
                                text = if (isArabic) "مثال: ALG Poste: Opération -300.00 DA • Nouveau solde: 24500.00 DA"
                                else "Example: ALG Poste: Retrait -300.00 DA • Nouveau solde: 24500.00 DA",
                                color = colors.textMuted.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        }
                        BasicTextField(
                            value = manualSmsText,
                            onValueChange = { manualSmsText = it },
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

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Test Sample Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(RoseNeon.copy(alpha = 0.14f))
                                .border(0.8.dp, RoseNeon.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                                .clickable {
                                    manualSmsText = "ALG Poste: Retrait GAB -300.00 DA le 15/11/2026. Nouveau solde: 24500.00 DA"
                                }
                                .padding(horizontal = 8.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isArabic) "سحب (-300 DA)" else "Withdraw -300",
                                color = RoseNeon,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(EmeraldNeon.copy(alpha = 0.14f))
                                .border(0.8.dp, EmeraldNeon.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                                .clickable {
                                    manualSmsText = "ALG Poste: Virement reçu +15000.00 DA. Nouveau solde: 39500.00 DA"
                                }
                                .padding(horizontal = 8.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isArabic) "دخل (+15000 DA)" else "Income +15000",
                                color = EmeraldNeon,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 3rd Preset Chip: Demonstrate tax discrepancy hunting relative to current balance
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AmberNeon.copy(alpha = 0.14f))
                            .border(0.8.dp, AmberNeon.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                            .clickable {
                                val baseBal = if (userSettings.latestSmsBalanceAmount >= 1100.0) {
                                    userSettings.latestSmsBalanceAmount
                                } else {
                                    24500.0
                                }
                                val nextAfterTax = (baseBal - 1050.0).coerceAtLeast(0.0)
                                val balFmt = String.format(Locale.US, "%.2f", nextAfterTax)
                                manualSmsText = "ALG Poste: Paiement BaridiMob -1000.00 DA. Nouveau solde: $balFmt DA"
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isArabic) "تجربة تصيّد الضرائب (سحب -1000 مع فارق -50 دج ضريبة)"
                            else "Sample Tax Hunter (-1000 DA tx with 50 DA hidden fee)",
                            color = AmberNeon,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    GlassButton(
                        text = if (isArabic) "تحليل واستخراج الرصيد والضرائب" else "Analyze SMS, Balance & Tax",
                        enabled = manualSmsText.isNotBlank(),
                        containerColor = CyanNeon,
                        contentColor = Color(0xFF04202C),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("analyze_manual_sms_button"),
                        onClick = {
                            onAnalyzeManualSms("ALG Poste", manualSmsText)
                            manualSmsText = ""
                        }
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(92.dp)) }
    }
}

@Composable
private fun TaxAnomalyItemCard(
    anomaly: SmsTaxAnomalyEntry,
    isArabic: Boolean,
    isStealth: Boolean,
    currency: String,
    onRecordAsExpense: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LiquidGlassTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.glassSurface)
            .border(0.9.dp, AmberNeon.copy(alpha = 0.42f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isArabic) anomaly.titleAr else anomaly.titleEn,
                    color = AmberNeon,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = formatDate(anomaly.detectedAtMillis),
                    color = colors.textMuted,
                    fontSize = 10.sp
                )
            }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(RoseNeon.copy(alpha = 0.16f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "-" + formatDzdAmount(anomaly.taxAmount, currency, isStealth),
                        color = RoseNeon,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = if (isArabic) "إزالة" else "Dismiss",
                    tint = colors.textMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Text(
            text = if (isArabic) anomaly.explanationAr else anomaly.explanationEn,
            color = colors.textPrimary,
            fontSize = 11.5.sp,
            lineHeight = 16.sp
        )

        if (anomaly.rawBody.isNotBlank()) {
            Text(
                text = anomaly.rawBody,
                color = colors.textMuted,
                fontSize = 10.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (anomaly.isRecordedAsExpense) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(EmeraldNeon.copy(alpha = 0.14f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = EmeraldNeon,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isArabic) "تم تسجيل الضريبة في المصاريف ✓" else "Logged in Expenses ✓",
                    color = EmeraldNeon,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(AmberNeon.copy(alpha = 0.18f))
                    .border(0.8.dp, AmberNeon.copy(alpha = 0.50f), RoundedCornerShape(10.dp))
                    .clickable(onClick = onRecordAsExpense)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .testTag("record_tax_expense_button"),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AddCircle,
                    contentDescription = null,
                    tint = AmberNeon,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isArabic) "إضافة كضريبة في المصاريف (Expense)"
                    else "Log Caught Tax to Expenses",
                    color = AmberNeon,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
fun SmsSuggestionCard(
    suggestion: SmsSuggestionEntry,
    isArabic: Boolean,
    onAccept: (TransactionType) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LiquidGlassTheme.colors
    var selectedType by remember(suggestion.id, suggestion.isExpense) {
        mutableStateOf(suggestion.transactionType)
    }
    val isExpense = selectedType == TransactionType.EXPENSE
    val accent = if (isExpense) RoseNeon else EmeraldNeon

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sms_suggestion_card"),
        backgroundColor = colors.glassCard,
        borderColor = accent.copy(alpha = 0.45f)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isExpense) Icons.Default.TrendingDown else Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = suggestion.sender,
                                color = colors.textPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(accent.copy(alpha = 0.16f))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isExpense) {
                                        if (isArabic) "مصروف (Expense)" else "Expense"
                                    } else {
                                        if (isArabic) "دخل (Income)" else "Income"
                                    },
                                    color = accent,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                        Text(
                            text = formatDate(suggestion.receivedAtMillis),
                            color = colors.textMuted,
                            fontSize = 10.5.sp
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
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Amount + SMS Body Snippet
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.glassSurface)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isArabic) suggestion.suggestedTitleAr else suggestion.suggestedTitleEn,
                        color = colors.textPrimary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = suggestion.rawBody,
                        color = colors.textSecondary,
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 15.sp
                    )
                    if (suggestion.extractedBalance != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isArabic) {
                                "الرصيد في الرسالة: ${formatDzd(suggestion.extractedBalance)}"
                            } else {
                                "Solde in SMS: ${formatDzd(suggestion.extractedBalance)}"
                            },
                            color = EmeraldNeon,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(
                        text = (if (isExpense) "-" else "+") + formatDzd(suggestion.amount),
                        color = accent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Switch classification manually if needed
                Row(
                    modifier = Modifier
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
                        .padding(horizontal = 10.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isExpense) {
                            if (isArabic) "تبديل لدخل" else "To Income"
                        } else {
                            if (isArabic) "تبديل لمصروف" else "To Expense"
                        },
                        color = colors.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // 1-Tap Confirm Add to Expense or Income
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.20f))
                        .border(1.dp, accent.copy(alpha = 0.60f), RoundedCornerShape(12.dp))
                        .clickable { onAccept(selectedType) }
                        .padding(horizontal = 12.dp, vertical = 11.dp)
                        .testTag("accept_sms_suggestion_button"),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isExpense) {
                            if (isArabic) "إضافة في المصاريف (Expense)" else "Add to Expense"
                        } else {
                            if (isArabic) "إضافة في الدخل (Income)" else "Add to Income"
                        },
                        color = accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
