package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import com.example.ui.components.edgeFadeVerticalContentPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import com.example.data.HubVaultCodec
import com.example.data.PasswordAnalysis
import com.example.data.PasswordStrengthAnalyzer
import com.example.data.PasswordStrengthLevel
import com.example.data.UserSettings
import com.example.data.VaultAppPresets
import com.example.data.VaultKeyEntry
import com.example.ui.components.AppPresetPickerDialog
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPill
import com.example.ui.components.LiquidEmptyStateCard
import com.example.ui.components.VaultAppBrandBackdrop
import com.example.ui.components.VaultAppEmblemBadge
import com.example.ui.components.VaultHoldActionDialog
import com.example.ui.components.primaryColor
import com.example.ui.components.secondaryColor
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.ArabicStrings
import com.example.ui.i18n.LocalStealthMode
import com.example.ui.i18n.formatPercent
import com.example.ui.i18n.isolateLtr
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.LocalAdaptiveDimens
import com.example.ui.theme.LocalAdaptiveLayoutInfo
import com.example.ui.theme.LocalAdaptiveTypography
import com.example.ui.theme.RoseNeon
import kotlinx.coroutines.delay

private enum class KeysFilter {
    ALL,
    PINNED,
    WEAK_OR_REUSED,
    STRONG
}

@Composable
fun KeysScreen(
    userSettings: UserSettings,
    expandedKeyIds: Set<String> = emptySet(),
    revealedKeyIds: Set<String> = emptySet(),
    onToggleExpandKey: (String) -> Unit = {},
    onToggleRevealKey: (String) -> Unit = {},
    onCollapseAllKeys: () -> Unit = {},
    onAddKey: (label: String, username: String, secretValue: String, category: String, note: String, isPinned: Boolean, appPresetId: String) -> Unit,
    onUpdateKey: (keyId: String, label: String, username: String, secretValue: String, category: String, note: String, isPinned: Boolean, appPresetId: String) -> Unit,
    onTogglePinKey: (String) -> Unit = {},
    onDeleteKey: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors
    val isArabic = strings is ArabicStrings
    val isStealth = LocalStealthMode.current
    val layoutInfo = LocalAdaptiveLayoutInfo.current
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current
    val context = LocalContext.current

    val keys = remember(userSettings.vaultKeysJson) {
        HubVaultCodec.parseVaultKeys(userSettings.vaultKeysJson)
    }

    val analyzedItems = remember(keys) {
        val counts = keys.groupingBy { it.secretValue }.eachCount()
        keys.map { entry ->
            val analysis = PasswordStrengthAnalyzer.analyze(
                password = entry.secretValue,
                username = entry.username,
                label = entry.label,
                duplicateCount = counts[entry.secretValue] ?: 1
            )
            entry to analysis
        }
    }

    val pinnedCount = remember(keys) { keys.count { it.isPinned } }
    val strongCount = remember(analyzedItems) {
        analyzedItems.count {
            it.second.level == PasswordStrengthLevel.STRONG || it.second.level == PasswordStrengthLevel.GOOD
        }
    }
    val weakCount = remember(analyzedItems) {
        analyzedItems.count {
            it.second.level == PasswordStrengthLevel.WEAK || it.second.level == PasswordStrengthLevel.FAIR
        }
    }
    val reusedCount = remember(analyzedItems) {
        analyzedItems.count { it.second.isReused }
    }
    val averageScore = remember(analyzedItems) {
        if (analyzedItems.isEmpty()) 100 else analyzedItems.map { it.second.score }.average().toInt()
    }

    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf(KeysFilter.ALL) }

    val filteredItems by remember(analyzedItems, searchQuery, activeFilter) {
        derivedStateOf {
            val q = searchQuery.trim().lowercase()
            analyzedItems.filter { (entry, analysis) ->
                val matchesSearch = q.isEmpty() ||
                    entry.label.lowercase().contains(q) ||
                    entry.username.lowercase().contains(q) ||
                    entry.category.lowercase().contains(q) ||
                    entry.note.lowercase().contains(q) ||
                    entry.appPresetId.lowercase().contains(q)
                val matchesFilter = when (activeFilter) {
                    KeysFilter.ALL -> true
                    KeysFilter.PINNED -> entry.isPinned
                    KeysFilter.WEAK_OR_REUSED ->
                        analysis.level == PasswordStrengthLevel.WEAK ||
                            analysis.level == PasswordStrengthLevel.FAIR ||
                            analysis.isReused
                    KeysFilter.STRONG ->
                        (analysis.level == PasswordStrengthLevel.STRONG ||
                            analysis.level == PasswordStrengthLevel.GOOD) && !analysis.isReused
                }
                matchesSearch && matchesFilter
            }
        }
    }

    var showAddOrEditDialog by remember { mutableStateOf(false) }
    var editingKey by remember { mutableStateOf<VaultKeyEntry?>(null) }
    var holdActionTargetKey by remember { mutableStateOf<VaultKeyEntry?>(null) }

    holdActionTargetKey?.let { targetKey ->
        val preset = remember(targetKey.appPresetId, targetKey.label) {
            VaultAppPresets.resolveForEntry(targetKey)
        }
        VaultHoldActionDialog(
            title = targetKey.label,
            subtitle = if (preset.id == "custom") localizedKeyCategory(targetKey.category, isArabic) else {
                if (isArabic) preset.nameAr else preset.nameEn
            },
            preset = preset,
            isPinned = targetKey.isPinned,
            isArabic = isArabic,
            onPinToggle = {
                onTogglePinKey(targetKey.id)
            },
            onEdit = {
                editingKey = targetKey
                showAddOrEditDialog = true
            },
            onDelete = {
                onDeleteKey(targetKey.id)
            },
            onDismiss = {
                holdActionTargetKey = null
            }
        )
    }

    if (showAddOrEditDialog) {
        AddOrEditVaultKeyDialog(
            existing = editingKey,
            secureScreenshots = userSettings.screenshotProtectionEnabled,
            onDismiss = {
                showAddOrEditDialog = false
                editingKey = null
            },
            onSave = { label, username, secret, category, note, isPinned, appPresetId ->
                val target = editingKey
                if (target == null) {
                    onAddKey(label, username, secret, category, note, isPinned, appPresetId)
                } else {
                    onUpdateKey(target.id, label, username, secret, category, note, isPinned, appPresetId)
                }
                showAddOrEditDialog = false
                editingKey = null
            }
        )
    }

    fun copyToClipboard(label: String, text: String, msg: String) {
        com.example.security.ClipboardSecurityHelper.copySensitiveText(context, label, text)
        val suffix = if (isArabic) "يُمسح بعد 45ث" else "auto-clears in 45s"
        android.widget.Toast.makeText(context, "$msg • $suffix", android.widget.Toast.LENGTH_SHORT).show()
    }

    fun toggleRevealWithStepUp(keyId: String) {
        val isCurrentlyRevealed = keyId in revealedKeyIds
        if (isCurrentlyRevealed || !userSettings.biometricEnabled) {
            onToggleRevealKey(keyId)
            return
        }
        com.example.security.StepUpAuthHelper.verifyIfEnabled(
            context = context,
            requireBiometric = true,
            title = if (isArabic) "تأكيد الهوية لعرض كلمة السر" else "Verify Identity to Reveal Password",
            subtitle = if (isArabic) "تحقق بالبصمة لكشف كلمة المرور المحفوظة" else "Confirm biometrics to reveal this stored secret",
            cancelText = strings.cancel,
            onVerified = { onToggleRevealKey(keyId) },
            onRejected = { err ->
                val msg = err?.takeIf { it.isNotBlank() } ?: if (isArabic) {
                    "تعذر التحقق بالبصمة. تحقق من بصمة الجهاز ثم أعد المحاولة."
                } else {
                    "Biometric verification unavailable. Check device biometrics and try again."
                }
                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
            }
        )
    }

    val keysHeader: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isArabic) "خزنة الحسابات • ${keys.size} محفوظ" else "Vault Accounts • ${keys.size} saved",
                    color = colors.textPrimary,
                    fontSize = typo.body,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isArabic) "اضغط مطولاً 0.5 ث على أي حاوية للتعديل أو الحذف"
                    else "Hold any app card 0.5s to edit or delete",
                    color = colors.textMuted,
                    fontSize = typo.caption,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            GlassButton(
                text = if (isArabic) "إضافة حساب" else "Add Account",
                icon = Icons.Default.Add,
                containerColor = AmberNeon,
                contentColor = Color(0xFF0F172A),
                onClick = {
                    editingKey = null
                    showAddOrEditDialog = true
                },
                modifier = Modifier.testTag("add_vault_key_button")
            )
        }
    }

    val auditSummaryCard: @Composable () -> Unit = {
        val auditAccent = when {
            keys.isEmpty() -> CyanNeon
            averageScore >= 80 && weakCount == 0 -> EmeraldNeon
            averageScore >= 55 -> AmberNeon
            else -> RoseNeon
        }

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = colors.glassCard,
            borderColor = auditAccent.copy(alpha = 0.42f)
        ) {
            Column(modifier = Modifier.padding(dimens.cardPadding)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(auditAccent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = auditAccent,
                            modifier = Modifier.size(23.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isArabic) "التحليل الذكي لأمان كلمات السر" else "Smart Password Security Audit",
                            color = colors.textPrimary,
                            fontSize = typo.bodyLg,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (keys.isEmpty()) {
                                if (isArabic) "أضف حساباتك واختر التطبيق لتتحول البطاقة بألوانه وشعاره"
                                else "Pick an app from the list to transform each vault card"
                            } else {
                                if (isArabic) "${keys.size} حساب محفوظ • معدل الأمان العام ${formatPercent(averageScore, true)}"
                                else "${keys.size} saved accounts • Overall security score ${formatPercent(averageScore, false)}"
                            },
                            color = colors.textMuted,
                            fontSize = typo.bodyXs
                        )
                    }
                    if (keys.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(auditAccent.copy(alpha = 0.16f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = formatPercent(averageScore, isArabic),
                                color = auditAccent,
                                fontSize = typo.bodySm,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                if (keys.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { (averageScore / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50)),
                        color = auditAccent,
                        trackColor = colors.glassSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AuditStatChip(
                            label = if (isArabic) "قوية: $strongCount" else "Strong: $strongCount",
                            color = EmeraldNeon,
                            modifier = Modifier.weight(1f)
                        )
                        AuditStatChip(
                            label = if (isArabic) "ضعيفة: $weakCount" else "Weak: $weakCount",
                            color = RoseNeon,
                            modifier = Modifier.weight(1f)
                        )
                        AuditStatChip(
                            label = if (isArabic) "مكررة: $reusedCount" else "Reused: $reusedCount",
                            color = AmberNeon,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    val searchAndFilterBlock: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = colors.glassCard,
                borderColor = colors.glassBorderSubtle,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = AmberNeon,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = if (isArabic) "ابحث باسم التطبيق، اسم المستخدم أو الملاحظة..."
                                else "Search app, username, or note...",
                                color = colors.textMuted,
                                fontSize = typo.bodySm,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = colors.textPrimary,
                                fontSize = typo.body,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(AmberNeon),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = if (isArabic) "مسح" else "Clear",
                                tint = colors.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassPill(
                    text = if (isArabic) "الكل (${keys.size})" else "All (${keys.size})",
                    isSelected = activeFilter == KeysFilter.ALL,
                    onClick = { activeFilter = KeysFilter.ALL }
                )
                GlassPill(
                    text = if (isArabic) "مثبتة ($pinnedCount)" else "Pinned ($pinnedCount)",
                    isSelected = activeFilter == KeysFilter.PINNED,
                    onClick = { activeFilter = KeysFilter.PINNED }
                )
                GlassPill(
                    text = if (isArabic) "ضعيفة ومكررة ($weakCount)" else "Weak / Reused ($weakCount)",
                    isSelected = activeFilter == KeysFilter.WEAK_OR_REUSED,
                    onClick = { activeFilter = KeysFilter.WEAK_OR_REUSED }
                )
                GlassPill(
                    text = if (isArabic) "قوية ($strongCount)" else "Strong ($strongCount)",
                    isSelected = activeFilter == KeysFilter.STRONG,
                    onClick = { activeFilter = KeysFilter.STRONG }
                )
                if (expandedKeyIds.isNotEmpty()) {
                    GlassPill(
                        text = if (isArabic) "طي الكل (${expandedKeyIds.size})" else "Collapse (${expandedKeyIds.size})",
                        isSelected = false,
                        onClick = onCollapseAllKeys
                    )
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
                    .weight(0.9f)
                    .fillMaxHeight(),
                contentPadding = edgeFadeVerticalContentPadding(),
                verticalArrangement = Arrangement.spacedBy(dimens.spacingMd)
            ) {
                item(key = "keys_header_tablet") {
                    Spacer(modifier = Modifier.height(dimens.spacingXs))
                    keysHeader()
                }
                item(key = "keys_smart_audit_tablet") { auditSummaryCard() }
                if (keys.isNotEmpty()) {
                    item(key = "keys_search_filter_tablet") { searchAndFilterBlock() }
                }
                item(key = "keys_left_spacer") { Spacer(modifier = Modifier.height(dimens.spacingXl)) }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight(),
                contentPadding = edgeFadeVerticalContentPadding(),
                verticalArrangement = Arrangement.spacedBy(dimens.spacingMd)
            ) {
                item { Spacer(modifier = Modifier.height(dimens.spacingXs)) }
                if (filteredItems.isEmpty()) {
                    item(key = "keys_empty_tablet") {
                        LiquidEmptyStateCard(
                            title = if (keys.isEmpty()) {
                                if (isArabic) "لا توجد حسابات محفوظة بعد" else "No saved accounts yet"
                            } else {
                                if (isArabic) "لا توجد حسابات مطابقة للتصفية" else "No matching accounts"
                            },
                            description = if (keys.isEmpty()) {
                                if (isArabic) "احفظ كلمات السر لحساباتك (بريدي موب، إنستغرام، جوجل، ريدوت باي وغيرها) مع تحوّل تلقائي فخم لشكل ولون كل تطبيق."
                                else "Store usernames & passwords locally with dynamic app-themed animated cards and instant strength analysis."
                            } else {
                                if (isArabic) "غيّر التصفية أو كلمات البحث لعرض باقي الحسابات."
                                else "Adjust your search or filter to see more entries."
                            },
                            icon = Icons.Default.Key,
                            iconColor = AmberNeon
                        )
                    }
                } else {
                    items(filteredItems, key = { it.first.id }) { (entry, analysis) ->
                        val isVisible = !isStealth && (entry.id in revealedKeyIds)
                        val isExpanded = entry.id in expandedKeyIds
                        Box(
                            modifier = Modifier.animateItem(
                                placementSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = 380f
                                )
                            )
                        ) {
                            VaultKeyRowCard(
                            entry = entry,
                            analysis = analysis,
                            isVisible = isVisible,
                            isExpanded = isExpanded,
                            isStealth = isStealth,
                            isArabic = isArabic,
                            onToggleExpand = { onToggleExpandKey(entry.id) },
                            onToggleReveal = { toggleRevealWithStepUp(entry.id) },
                            onTogglePin = { onTogglePinKey(entry.id) },
                            onHoldActions = { holdActionTargetKey = entry },
                            onCopyUsername = {
                                if (entry.username.isNotBlank()) {
                                    copyToClipboard(
                                        label = "Username",
                                        text = entry.username,
                                        msg = if (isArabic) "تم نسخ اسم المستخدم ✓" else "Username copied ✓"
                                    )
                                }
                            },
                            onCopySecret = {
                                copyToClipboard(
                                    label = entry.label,
                                    text = entry.secretValue,
                                    msg = if (isArabic) "تم نسخ كلمة السر ✓" else "Password copied ✓"
                                )
                            }
                        )
                        }
                    }
                }
                item(key = "keys_bottom_spacer_tablet") { Spacer(modifier = Modifier.height(dimens.spacingXl)) }
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = dimens.screenPaddingH),
            contentPadding = edgeFadeVerticalContentPadding(),
            verticalArrangement = Arrangement.spacedBy(dimens.spacingMd)
        ) {
            item(key = "keys_header") {
                Spacer(modifier = Modifier.height(dimens.spacingXs))
                keysHeader()
            }
            item(key = "keys_smart_audit") { auditSummaryCard() }
            if (keys.isNotEmpty()) {
                item(key = "keys_search_filter") { searchAndFilterBlock() }
            }
            if (filteredItems.isEmpty()) {
                item(key = "keys_empty") {
                    LiquidEmptyStateCard(
                        title = if (keys.isEmpty()) {
                            if (isArabic) "لا توجد حسابات محفوظة بعد" else "No saved accounts yet"
                        } else {
                            if (isArabic) "لا توجد حسابات مطابقة للتصفية" else "No matching accounts"
                        },
                        description = if (keys.isEmpty()) {
                            if (isArabic) "احفظ كلمات السر لحساباتك (بريدي موب، إنستغرام، جوجل، ريدوت باي وغيرها) مع تحوّل تلقائي فخم لشكل ولون كل تطبيق."
                            else "Store usernames & passwords locally with dynamic app-themed animated cards and instant strength analysis."
                        } else {
                            if (isArabic) "غيّر التصفية أو كلمات البحث لعرض باقي الحسابات."
                            else "Adjust your search or filter to see more entries."
                        },
                        icon = Icons.Default.Key,
                        iconColor = AmberNeon
                    )
                }
            } else {
                items(filteredItems, key = { it.first.id }) { (entry, analysis) ->
                    val isVisible = !isStealth && (entry.id in revealedKeyIds)
                    val isExpanded = entry.id in expandedKeyIds
                    Box(
                        modifier = Modifier.animateItem(
                            placementSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = 380f
                            )
                        )
                    ) {
                        VaultKeyRowCard(
                            entry = entry,
                            analysis = analysis,
                            isVisible = isVisible,
                            isExpanded = isExpanded,
                            isStealth = isStealth,
                            isArabic = isArabic,
                            onToggleExpand = { onToggleExpandKey(entry.id) },
                            onTogglePin = { onTogglePinKey(entry.id) },
                            onToggleReveal = { toggleRevealWithStepUp(entry.id) },
                            onHoldActions = { holdActionTargetKey = entry },
                            onCopyUsername = {
                                if (entry.username.isNotBlank()) {
                                    copyToClipboard(
                                        label = "Username",
                                        text = entry.username,
                                        msg = if (isArabic) "تم نسخ اسم المستخدم ✓" else "Username copied ✓"
                                    )
                                }
                            },
                            onCopySecret = {
                                copyToClipboard(
                                    label = entry.label,
                                    text = entry.secretValue,
                                    msg = if (isArabic) "تم نسخ كلمة السر ✓" else "Password copied ✓"
                                )
                            }
                        )
                    }
                }
            }

            item(key = "keys_bottom_spacer") {
                Spacer(modifier = Modifier.height(84.dp))
            }
        }
    }
}

@Composable
private fun AuditStatChip(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val typo = LocalAdaptiveTypography.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.13f))
            .border(0.8.dp, color.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = color,
            fontSize = typo.caption,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VaultKeyRowCard(
    entry: VaultKeyEntry,
    analysis: PasswordAnalysis,
    isVisible: Boolean,
    isExpanded: Boolean,
    isStealth: Boolean,
    isArabic: Boolean,
    onToggleExpand: () -> Unit,
    onToggleReveal: () -> Unit,
    onTogglePin: () -> Unit,
    onHoldActions: () -> Unit,
    onCopyUsername: () -> Unit,
    onCopySecret: () -> Unit
) {
    val colors = LiquidGlassTheme.colors
    val haptic = LocalHapticFeedback.current
    val preset = remember(entry.appPresetId, entry.label) {
        VaultAppPresets.resolveForEntry(entry)
    }
    val brandPrimary by animateColorAsState(preset.primaryColor(), tween(320), label = "card_bp")
    val (strengthColor, _) = strengthMeta(analysis.level, isArabic)

    var copiedUsernameRecently by remember { mutableStateOf(false) }
    var copiedSecretRecently by remember { mutableStateOf(false) }

    // Standardized 0.5s hold (~100ms TapTimeout + 400ms linear fill)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val holdAnim = remember { Animatable(0f) }
    var didTriggerLongPress by remember { mutableStateOf(false) }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            didTriggerLongPress = false
            holdAnim.snapTo(0f)
            holdAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 400, easing = LinearEasing)
            )
            didTriggerLongPress = true
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onHoldActions()
        } else {
            holdAnim.snapTo(0f)
            if (didTriggerLongPress) {
                delay(150)
                didTriggerLongPress = false
            }
        }
    }

    LaunchedEffect(copiedUsernameRecently) {
        if (copiedUsernameRecently) {
            delay(1400)
            copiedUsernameRecently = false
        }
    }
    LaunchedEffect(copiedSecretRecently) {
        if (copiedSecretRecently) {
            delay(1400)
            copiedSecretRecently = false
        }
    }

    val borderCol = when {
        holdAnim.value > 0.05f -> brandPrimary
        entry.isPinned -> AmberNeon.copy(alpha = if (colors.isDark) 0.65f else 0.72f)
        else -> colors.glassBorder
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                val s = 1f - (holdAnim.value * 0.015f)
                scaleX = s
                scaleY = s
            }
            .testTag("vault_key_card_${preset.id}"),
        shape = RoundedCornerShape(18.dp),
        cornerRadius = 18.dp,
        backgroundColor = colors.glassCard,
        borderColor = borderCol,
        borderWidth = if (entry.isPinned || holdAnim.value > 0.05f) 1.35.dp else 1.dp,
        breathingGlow = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(bounded = true, color = brandPrimary),
                    onClick = {
                        if (didTriggerLongPress) {
                            didTriggerLongPress = false
                        } else {
                            onToggleExpand()
                        }
                    }
                )
        ) {
            // Top 0.5s hold progress indicator
            if (holdAnim.value > 0.04f) {
                LinearProgressIndicator(
                    progress = { holdAnim.value },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = brandPrimary,
                    trackColor = colors.glassBorderSubtle.copy(alpha = 0.4f)
                )
            }

            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                // Minimal Header Row: App Icon + App Name + Strength % + Pin Icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VaultAppEmblemBadge(preset = preset, size = 40.dp, isArabic = isArabic, showBorder = false)
                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = entry.label,
                        color = colors.textPrimary,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        lineHeight = 20.sp,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(strengthColor.copy(alpha = if (colors.isDark) 0.16f else 0.13f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = isolateLtr(formatPercent(analysis.score, isArabic)),
                            color = strengthColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            style = TextStyle(fontFeatureSettings = "tnum")
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("pin_key_${entry.id}")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (entry.isPinned) AmberNeon.copy(alpha = 0.16f)
                                    else Color.Transparent
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (entry.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                contentDescription = if (isArabic) "تثبيت التطبيق في الأعلى" else "Pin app to top",
                                tint = if (entry.isPinned) AmberNeon else colors.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // 3. Structured Expanded Utility Panel (single critically damped spring for zero-jank expand/collapse)
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn(
                        animationSpec = tween(
                            durationMillis = 220,
                            delayMillis = 35,
                            easing = FastOutSlowInEasing
                        )
                    ) + expandVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = 380f,
                            visibilityThreshold = IntSize.VisibilityThreshold
                        ),
                        expandFrom = Alignment.Top
                    ),
                    exit = fadeOut(
                        animationSpec = tween(
                            durationMillis = 120,
                            easing = FastOutSlowInEasing
                        )
                    ) + shrinkVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = 420f,
                            visibilityThreshold = IntSize.VisibilityThreshold
                        ),
                        shrinkTowards = Alignment.Top
                    )
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(10.dp))

                        // Structured Username Row
                        if (entry.username.isNotBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.glassSurface)
                                    .border(
                                        width = 0.9.dp,
                                        color = if (copiedUsernameRecently) EmeraldNeon.copy(alpha = 0.75f)
                                        else colors.glassBorderSubtle,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        copiedUsernameRecently = true
                                        onCopyUsername()
                                    }
                                    .padding(start = 12.dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = brandPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isArabic) "اسم المستخدم" else "USERNAME",
                                            color = colors.textMuted,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = if (isArabic) 0.sp else 0.5.sp
                                        )
                                        Text(
                                            text = if (isStealth) "••••••••" else isolateLtr(entry.username),
                                            color = colors.textPrimary,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        copiedUsernameRecently = true
                                        onCopyUsername()
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = if (copiedUsernameRecently) Icons.Default.Check else Icons.Default.ContentCopy,
                                        contentDescription = if (isArabic) "نسخ اسم المستخدم" else "Copy username",
                                        tint = if (copiedUsernameRecently) EmeraldNeon else brandPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Structured Secret Row with 48dp Reveal & Copy Actions
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.glassSurface)
                                .border(
                                    width = 1.dp,
                                    color = when {
                                        copiedSecretRecently -> EmeraldNeon.copy(alpha = 0.80f)
                                        isVisible -> brandPrimary.copy(alpha = 0.65f)
                                        else -> colors.glassBorderSubtle
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(start = 12.dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (isVisible) EmeraldNeon else brandPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isArabic) "كلمة المرور" else "SECRET",
                                        color = colors.textMuted,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = if (isArabic) 0.sp else 0.5.sp
                                    )
                                    AnimatedContent(
                                        targetState = isVisible,
                                        transitionSpec = {
                                            fadeIn(tween(200)) togetherWith fadeOut(tween(160))
                                        },
                                        label = "secret_reveal_anim"
                                    ) { revealed ->
                                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                            Text(
                                                text = if (revealed) entry.secretValue else "•".repeat(entry.secretValue.length.coerceIn(6, 14)),
                                                color = colors.textPrimary,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                letterSpacing = 1.1.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = onToggleReveal, modifier = Modifier.size(48.dp)) {
                                    Icon(
                                        imageVector = if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (isArabic) "إظهار/إخفاء" else "Reveal/Hide",
                                        tint = brandPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        copiedSecretRecently = true
                                        onCopySecret()
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = if (copiedSecretRecently) Icons.Default.Check else Icons.Default.ContentCopy,
                                        contentDescription = if (isArabic) "نسخ كلمة السر" else "Copy password",
                                        tint = EmeraldNeon,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Strength Meter & Optional Audit Warning
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { (analysis.score / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(50)),
                            color = strengthColor,
                            trackColor = colors.glassBorderSubtle.copy(alpha = 0.5f)
                        )

                        val showAuditWarning = analysis.level == PasswordStrengthLevel.WEAK ||
                            analysis.level == PasswordStrengthLevel.FAIR ||
                            analysis.isReused
                        if (showAuditWarning) {
                            val primaryTip = if (isArabic) analysis.tipsAr.firstOrNull() else analysis.tipsEn.firstOrNull()
                            if (!primaryTip.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(strengthColor.copy(alpha = 0.13f))
                                        .border(0.7.dp, strengthColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = strengthColor,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = primaryTip,
                                        color = strengthColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        if (entry.note.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = entry.note,
                                color = colors.textSecondary,
                                fontSize = 11.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddOrEditVaultKeyDialog(
    existing: VaultKeyEntry?,
    secureScreenshots: Boolean,
    onDismiss: () -> Unit,
    onSave: (label: String, username: String, secret: String, category: String, note: String, isPinned: Boolean, appPresetId: String) -> Unit
) {
    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors
    val isArabic = strings is ArabicStrings

    val vaultTransition = rememberInfiniteTransition(label = "keys_dialog_shimmer")
    val shimmerPhase by vaultTransition.animateFloat(
        initialValue = -0.25f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_phase"
    )
    val pulsePhase by vaultTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_phase"
    )

    val initialPreset = remember(existing) {
        if (existing != null) VaultAppPresets.resolveForEntry(existing).id else "custom"
    }
    var selectedPresetId by remember(existing) { mutableStateOf(initialPreset) }
    var userExplicitlyPickedPreset by remember(existing) { mutableStateOf(existing != null && initialPreset != "custom") }
    var showAppPickerModal by remember { mutableStateOf(false) }

    var label by remember(existing) { mutableStateOf(existing?.label ?: "") }
    var username by remember(existing) { mutableStateOf(existing?.username ?: "") }
    var secret by remember(existing) { mutableStateOf(existing?.secretValue ?: "") }
    var category by remember(existing) { mutableStateOf(existing?.category ?: "APP") }
    var note by remember(existing) { mutableStateOf(existing?.note ?: "") }
    var isPinned by remember(existing) { mutableStateOf(existing?.isPinned ?: false) }
    var showPasswordInDialog by remember { mutableStateOf(true) }

    val activePreset = remember(selectedPresetId) { VaultAppPresets.findById(selectedPresetId) }
    val presetAccent = activePreset.primaryColor()

    val liveAnalysis = remember(secret, username, label) {
        PasswordStrengthAnalyzer.analyze(
            password = secret,
            username = username,
            label = label,
            duplicateCount = 1
        )
    }
    val (strengthColor, strengthLabel) = strengthMeta(liveAnalysis.level, isArabic)

    if (showAppPickerModal) {
        AppPresetPickerDialog(
            selectedPresetId = selectedPresetId,
            isArabic = isArabic,
            onSelect = { chosen ->
                val prevPreset = VaultAppPresets.findById(selectedPresetId)
                selectedPresetId = chosen.id
                userExplicitlyPickedPreset = (chosen.id != "custom")
                if (chosen.id != "custom") {
                    val chosenName = if (isArabic) chosen.nameAr else chosen.nameEn
                    if (label.isBlank() ||
                        label == prevPreset.nameEn ||
                        label == prevPreset.nameAr
                    ) {
                        label = chosenName
                    }
                    category = chosen.defaultCategory
                }
                showAppPickerModal = false
            },
            onDismiss = { showAppPickerModal = false }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            securePolicy = if (secureScreenshots) SecureFlagPolicy.SecureOn else SecureFlagPolicy.SecureOff
        )
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr
        ) {
            GlassCard(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                backgroundColor = colors.dialogBackground,
                borderColor = presetAccent.copy(alpha = 0.50f)
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                // Live Brand Transformation Preview Banner inside the Dialog
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, presetAccent.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                ) {
                    Box(modifier = Modifier.matchParentSize()) {
                        VaultAppBrandBackdrop(
                            preset = activePreset,
                            shimmerPhase = shimmerPhase,
                            pulsePhase = pulsePhase
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        VaultAppEmblemBadge(preset = activePreset, size = 44.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (existing == null) {
                                    if (isArabic) "إضافة حساب وكلمة سر" else "Add Account & Password"
                                } else {
                                    if (isArabic) "تعديل الحساب" else "Edit Account"
                                },
                                color = colors.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = if (activePreset.id == "custom") {
                                    if (isArabic) "اختر تطبيقاً لتلوين وتخصيص البطاقة تلقائياً"
                                    else "Pick an app to transform card colors & shapes"
                                } else {
                                    if (isArabic) "نمط التطبيق: ${activePreset.nameAr}"
                                    else "Theme active: ${activePreset.nameEn}"
                                },
                                color = presetAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Prominent App List Selector Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(presetAccent.copy(alpha = 0.14f))
                        .border(1.dp, presetAccent.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                        .clickable { showAppPickerModal = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .testTag("select_app_preset_button"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = null,
                            tint = presetAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isArabic) "اختيار التطبيق من القائمة" else "Select App from List",
                                color = colors.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isArabic) activePreset.nameAr else activePreset.nameEn,
                                color = presetAccent,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Text(
                        text = if (isArabic) "تغيير" else "Choose",
                        color = presetAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Popular App Chips Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val quickIds = listOf("baridimob", "instagram", "google", "facebook", "tiktok", "redotpay", "bybit")
                    quickIds.forEach { qId ->
                        val p = VaultAppPresets.findById(qId)
                        val pColor = p.primaryColor()
                        val selected = selectedPresetId == p.id
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selected) pColor.copy(alpha = 0.24f) else colors.glassCard
                                )
                                .border(
                                    width = if (selected) 1.2.dp else 0.8.dp,
                                    color = if (selected) pColor else colors.glassBorderSubtle,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    val prevPreset = VaultAppPresets.findById(selectedPresetId)
                                    selectedPresetId = p.id
                                    userExplicitlyPickedPreset = true
                                    val chosenName = if (isArabic) p.nameAr else p.nameEn
                                    if (label.isBlank() ||
                                        label == prevPreset.nameEn ||
                                        label == prevPreset.nameAr
                                    ) {
                                        label = chosenName
                                    }
                                    category = p.defaultCategory
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            VaultAppEmblemBadge(preset = p, size = 20.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = (if (isArabic) p.nameAr else p.nameEn).substringBefore(" /"),
                                color = if (selected) pColor else colors.textSecondary,
                                fontSize = 11.5.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("APP", "BANK", "EMAIL", "PIN").forEach { cat ->
                        GlassPill(
                            text = localizedKeyCategory(cat, isArabic),
                            isSelected = category == cat,
                            onClick = { category = cat }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                KeyFieldBox(
                    label = if (isArabic) "اسم التطبيق / الخدمة (مثال: بريدي موب)" else "Account / Service Name",
                    value = label,
                    onValueChange = { newText ->
                        label = newText
                        if (!userExplicitlyPickedPreset) {
                            val detected = VaultAppPresets.detectFromLabel(newText)
                            selectedPresetId = detected?.id ?: "custom"
                            if (detected != null) {
                                category = detected.defaultCategory
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                KeyFieldBox(
                    label = if (isArabic) "اسم المستخدم أو البريد الإلكتروني" else "Username / Email",
                    value = username,
                    onValueChange = { username = it }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Password Input + Generate Strong Password Action
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.glassCard)
                        .border(
                            1.dp,
                            strengthColor.copy(alpha = if (secret.isEmpty()) 0.25f else 0.55f),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isArabic) "كلمة السر / الرمز" else "Password / PIN",
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(EmeraldNeon.copy(alpha = 0.14f))
                                .clickable {
                                    secret = PasswordStrengthAnalyzer.generateStrongPassword(14)
                                    showPasswordInDialog = true
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("generate_strong_password_button"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoFixHigh,
                                contentDescription = null,
                                tint = EmeraldNeon,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isArabic) "توليد قوية" else "Generate",
                                color = EmeraldNeon,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            BasicTextField(
                                value = secret,
                                onValueChange = { secret = it },
                                singleLine = true,
                                visualTransformation = if (showPasswordInDialog) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                textStyle = TextStyle(
                                    color = colors.textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                cursorBrush = SolidColor(strengthColor),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        IconButton(
                            onClick = { showPasswordInDialog = !showPasswordInDialog },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = if (showPasswordInDialog) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = colors.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Live Smart Password Strength Meter
                if (secret.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(strengthColor.copy(alpha = 0.10f))
                            .border(0.8.dp, strengthColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = strengthColor,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isArabic) "التقييم الذكي: $strengthLabel" else "Smart Rating: $strengthLabel",
                                    color = strengthColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Text(
                                text = formatPercent(liveAnalysis.score, isArabic),
                                color = strengthColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { (liveAnalysis.score / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(50)),
                            color = strengthColor,
                            trackColor = colors.glassSurface
                        )
                        val tip = if (isArabic) liveAnalysis.tipsAr.firstOrNull() else liveAnalysis.tipsEn.firstOrNull()
                        if (!tip.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = tip,
                                color = colors.textSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                KeyFieldBox(
                    label = if (isArabic) "ملاحظة (اختياري)" else "Note (optional)",
                    value = note,
                    onValueChange = { note = it }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Pin at Top toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.glassCard)
                        .clickable { isPinned = !isPinned }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = null,
                            tint = if (isPinned) AmberNeon else colors.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isArabic) "تثبيت التطبيق في الأعلى" else "Pin app to top",
                            color = colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Switch(
                        checked = isPinned,
                        onCheckedChange = { isPinned = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AmberNeon,
                            checkedTrackColor = AmberNeon.copy(alpha = 0.35f),
                            uncheckedThumbColor = colors.textMuted,
                            uncheckedTrackColor = colors.glassSurface
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassButton(
                        text = strings.cancel,
                        containerColor = colors.textMuted.copy(alpha = 0.25f),
                        contentColor = colors.textPrimary,
                        modifier = Modifier.weight(1f),
                        onClick = onDismiss
                    )
                    GlassButton(
                        text = strings.save,
                        enabled = label.isNotBlank() && secret.isNotBlank(),
                        containerColor = presetAccent,
                        contentColor = Color(0xFF0F172A),
                        modifier = Modifier.weight(1.2f),
                        onClick = { onSave(label, username, secret, category, note, isPinned, selectedPresetId) }
                    )
                }
            }
        }
        }
    }
}

private fun localizedKeyCategory(cat: String, isArabic: Boolean): String {
    if (!isArabic) return cat
    return when (cat.uppercase()) {
        "APP" -> "تطبيق"
        "BANK" -> "بنك"
        "EMAIL" -> "بريد"
        "PIN" -> "رمز سري"
        else -> cat
    }
}

private fun strengthMeta(level: PasswordStrengthLevel, isArabic: Boolean): Pair<Color, String> {
    return when (level) {
        PasswordStrengthLevel.WEAK -> RoseNeon to (if (isArabic) "ضعيفة" else "Weak")
        PasswordStrengthLevel.FAIR -> AmberNeon to (if (isArabic) "متوسطة" else "Fair")
        PasswordStrengthLevel.GOOD -> CyanNeon to (if (isArabic) "جيدة" else "Good")
        PasswordStrengthLevel.STRONG -> EmeraldNeon to (if (isArabic) "قوية جداً" else "Strong")
    }
}

@Composable
private fun KeyFieldBox(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    val colors = LiquidGlassTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.glassCard)
            .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            color = colors.textSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                color = colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            ),
            cursorBrush = SolidColor(colors.primaryNeon),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
