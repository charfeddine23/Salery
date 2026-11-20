package com.example.ui.screens

import com.example.security.SecureMemoryUtils
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import com.example.data.CcpAccountEntry
import com.example.data.HubVaultCodec
import com.example.data.UserSettings
import com.example.data.VaultAppPresets
import com.example.ui.components.AppPresetPickerDialog
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPill
import com.example.ui.components.LiquidEmptyStateCard
import com.example.ui.components.VaultAppBrandBackdrop
import com.example.ui.components.VaultAppEmblemBadge
import com.example.ui.components.VaultHoldActionDialog
import com.example.ui.components.openAppContactsPermissionSettings
import com.example.ui.components.primaryColor
import com.example.ui.components.rememberContactPickerFlow
import com.example.ui.components.secondaryColor
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.ArabicStrings
import com.example.ui.i18n.LocalStealthMode
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.LocalAdaptiveDimens
import com.example.ui.theme.LocalAdaptiveTypography
import com.example.ui.theme.RoseNeon

private enum class CcpFilter {
    ALL,
    PINNED,
    LINKED_CONTACTS,
    WITH_RIP
}

@Composable
fun CcpsScreen(
    userSettings: UserSettings,
    expandedCcpIds: Set<String> = emptySet(),
    revealedCcpIds: Set<String> = emptySet(),
    onToggleExpandCcp: (String) -> Unit = {},
    onToggleRevealCcp: (String) -> Unit = {},
    onCollapseAllCcps: () -> Unit = {},
    onAddCcpAccount: (
        firstName: String,
        lastName: String,
        ccpNumber: String,
        ccpKey: String,
        ripNumber: String,
        agencyNote: String,
        contactName: String,
        contactUri: String,
        contactPhone: String,
        isPinned: Boolean,
        appPresetId: String
    ) -> Unit,
    onUpdateCcpAccount: (
        accountId: String,
        firstName: String,
        lastName: String,
        ccpNumber: String,
        ccpKey: String,
        ripNumber: String,
        agencyNote: String,
        contactName: String,
        contactUri: String,
        contactPhone: String,
        isPinned: Boolean,
        appPresetId: String
    ) -> Unit,
    onTogglePinCcpAccount: (String) -> Unit,
    onDeleteCcpAccount: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors
    val isArabic = strings is ArabicStrings
    val isStealth = LocalStealthMode.current
    val layoutInfo = com.example.ui.theme.LocalAdaptiveLayoutInfo.current
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current
    val context = LocalContext.current

    val accounts = remember(userSettings.ccpAccountsJson) {
        HubVaultCodec.parseCcpAccounts(userSettings.ccpAccountsJson)
    }

    val pinnedCount = remember(accounts) { accounts.count { it.isPinned } }
    val linkedCount = remember(accounts) { accounts.count { it.hasLinkedContact } }
    val withRipCount = remember(accounts) { accounts.count { it.ripFull.isNotBlank() } }

    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf(CcpFilter.ALL) }

    val filteredAccounts by remember(accounts, searchQuery, activeFilter) {
        derivedStateOf {
            val q = searchQuery.trim().lowercase()
            accounts.filter { acc ->
                val matchesSearch = q.isEmpty() ||
                    acc.displayFullName.lowercase().contains(q) ||
                    acc.ccpNumber.contains(q) ||
                    acc.ripFull.contains(q) ||
                    acc.agencyNote.lowercase().contains(q) ||
                    acc.contactName.lowercase().contains(q) ||
                    acc.contactPhone.contains(q) ||
                    acc.appPresetId.lowercase().contains(q)
                val matchesFilter = when (activeFilter) {
                    CcpFilter.ALL -> true
                    CcpFilter.PINNED -> acc.isPinned
                    CcpFilter.LINKED_CONTACTS -> acc.hasLinkedContact
                    CcpFilter.WITH_RIP -> acc.ripFull.isNotBlank()
                }
                matchesSearch && matchesFilter
            }
        }
    }

    // Shared infinite animations matching KeysScreen silky-smooth 90fps luxury vault containers
    val ccpTransition = rememberInfiniteTransition(label = "ccps_vault_shimmer")
    val shimmerPhase by ccpTransition.animateFloat(
        initialValue = -0.25f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ccp_shimmer_phase"
    )
    val pulsePhase by ccpTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ccp_pulse_phase"
    )

    var showAddOrEditDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<CcpAccountEntry?>(null) }
    var holdActionTargetAccount by remember { mutableStateOf<CcpAccountEntry?>(null) }

    holdActionTargetAccount?.let { targetAcc ->
        val preset = remember(targetAcc.appPresetId, targetAcc.displayFullName, targetAcc.agencyNote) {
            VaultAppPresets.resolveForCcpEntry(targetAcc)
        }
        VaultHoldActionDialog(
            title = if (isStealth) "••••••" else targetAcc.displayFullName,
            subtitle = buildString {
                append(if (isArabic) preset.nameAr else preset.nameEn)
                if (targetAcc.ccpNumber.isNotBlank()) {
                    val maskedCcp = if (isStealth) "••••••" else "••••${targetAcc.ccpNumber.takeLast(4)}"
                    append(" • $maskedCcp")
                }
            },
            preset = preset,
            isPinned = targetAcc.isPinned,
            isArabic = isArabic,
            onPinToggle = {
                onTogglePinCcpAccount(targetAcc.id)
            },
            onEdit = {
                editingAccount = targetAcc
                showAddOrEditDialog = true
            },
            onDelete = {
                onDeleteCcpAccount(targetAcc.id)
            },
            onDismiss = {
                holdActionTargetAccount = null
            }
        )
    }

    if (showAddOrEditDialog) {
        AddOrEditCcpAccountDialog(
            existing = editingAccount,
            secureScreenshots = userSettings.screenshotProtectionEnabled,
            isAtCapacity = editingAccount == null && accounts.size >= HubVaultCodec.MAX_CCP_PROFILES,
            shimmerPhase = shimmerPhase,
            pulsePhase = pulsePhase,
            onDismiss = {
                showAddOrEditDialog = false
                editingAccount = null
            },
            onSave = { first, last, ccp, key, rip, note, cName, cUri, cPhone, pinned, appPresetId ->
                val target = editingAccount
                if (target == null) {
                    onAddCcpAccount(first, last, ccp, key, rip, note, cName, cUri, cPhone, pinned, appPresetId)
                } else {
                    onUpdateCcpAccount(target.id, first, last, ccp, key, rip, note, cName, cUri, cPhone, pinned, appPresetId)
                }
                showAddOrEditDialog = false
                editingAccount = null
            }
        )
    }

    fun copyText(label: String, text: String, @Suppress("UNUSED_PARAMETER") toastMessage: String) {
        SecureMemoryUtils.copySensitiveText(context, label, text)
    }

    val ccpHeader: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyanNeon.copy(alpha = 0.14f))
                        .border(1.dp, CyanNeon.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${accounts.size} / ${HubVaultCodec.MAX_CCP_PROFILES}",
                        color = CyanNeon,
                        fontSize = typo.caption,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isArabic) "اضغط مطولاً 0.5 ث على أي بطاقة للتعديل أو الحذف"
                    else "Hold any CCP card 0.5s to edit or delete",
                    color = colors.textMuted,
                    fontSize = typo.caption,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            GlassButton(
                text = if (isArabic) "إضافة شخص" else "Add Person",
                icon = Icons.Default.Add,
                containerColor = CyanNeon,
                contentColor = Color(0xFF04202C),
                enabled = accounts.size < HubVaultCodec.MAX_CCP_PROFILES,
                onClick = {
                    editingAccount = null
                    showAddOrEditDialog = true
                },
                modifier = Modifier.testTag("add_ccp_button")
            )
        }
    }

    val ccpSummaryCard: @Composable () -> Unit = {
        val dirAccent = when {
            accounts.isEmpty() -> CyanNeon
            pinnedCount > 0 -> AmberNeon
            else -> EmeraldNeon
        }
        val ripRatio = if (accounts.isEmpty()) 0f else (withRipCount.toFloat() / accounts.size.toFloat()).coerceIn(0f, 1f)

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = colors.glassCard,
            borderColor = dirAccent.copy(alpha = 0.42f)
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
                            .background(dirAccent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = dirAccent,
                            modifier = Modifier.size(23.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isArabic) "دليل الحسابات البريدية التفاعلي" else "Smart CCP Vault & Profiles",
                            color = colors.textPrimary,
                            fontSize = typo.bodyLg,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (accounts.isEmpty()) {
                                if (isArabic) "أضف حسابات CCP واختر نمط البطاقة لتتحول بأشكال وألوان متناسقة"
                                else "Pick a card theme to transform each CCP container dynamically"
                            } else {
                                if (isArabic) "${accounts.size} ملف محفوظ • تغطية RIP الكاملة ${(ripRatio * 100).toInt()}%"
                                else "${accounts.size} saved profiles • ${(ripRatio * 100).toInt()}% complete RIP"
                            },
                            color = colors.textMuted,
                            fontSize = typo.bodyXs
                        )
                    }
                }

                if (accounts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { ripRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50)),
                        color = dirAccent,
                        trackColor = colors.glassSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CcpSummaryChip(
                            label = if (isArabic) "مثبتة: $pinnedCount" else "Pinned: $pinnedCount",
                            color = AmberNeon,
                            modifier = Modifier.weight(1f)
                        )
                        CcpSummaryChip(
                            label = if (isArabic) "جهات اتصال: $linkedCount" else "Contacts: $linkedCount",
                            color = CyanNeon,
                            modifier = Modifier.weight(1f)
                        )
                        CcpSummaryChip(
                            label = if (isArabic) "مع RIP: $withRipCount" else "RIP: $withRipCount",
                            color = EmeraldNeon,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    val ccpSearchAndFilter: @Composable () -> Unit = {
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
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = CyanNeon,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = if (isArabic) "ابحث بالاسم، اللقب، CCP، RIP أو الملاحظة..."
                                else "Search name, CCP, RIP, or note...",
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
                            cursorBrush = SolidColor(CyanNeon),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ccp_search_input")
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = if (isArabic) "مسح البحث" else "Clear search",
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
                    text = if (isArabic) "الكل (${accounts.size})" else "All (${accounts.size})",
                    isSelected = activeFilter == CcpFilter.ALL,
                    onClick = { activeFilter = CcpFilter.ALL }
                )
                GlassPill(
                    text = if (isArabic) "مثبتة ($pinnedCount)" else "Pinned ($pinnedCount)",
                    isSelected = activeFilter == CcpFilter.PINNED,
                    onClick = { activeFilter = CcpFilter.PINNED }
                )
                GlassPill(
                    text = if (isArabic) "جهات اتصال ($linkedCount)" else "Contacts ($linkedCount)",
                    isSelected = activeFilter == CcpFilter.LINKED_CONTACTS,
                    onClick = { activeFilter = CcpFilter.LINKED_CONTACTS }
                )
                GlassPill(
                    text = if (isArabic) "مع RIP ($withRipCount)" else "With RIP ($withRipCount)",
                    isSelected = activeFilter == CcpFilter.WITH_RIP,
                    onClick = { activeFilter = CcpFilter.WITH_RIP }
                )
                if (expandedCcpIds.isNotEmpty()) {
                    GlassPill(
                        text = if (isArabic) "طي الكل (${expandedCcpIds.size})" else "Collapse (${expandedCcpIds.size})",
                        isSelected = false,
                        onClick = onCollapseAllCcps
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
                verticalArrangement = Arrangement.spacedBy(dimens.spacingMd)
            ) {
                item(key = "ccp_header_tablet") {
                    Spacer(modifier = Modifier.height(dimens.spacingXs))
                    ccpHeader()
                }
                item(key = "ccp_smart_summary_tablet") { ccpSummaryCard() }
                if (accounts.isNotEmpty()) {
                    item(key = "ccp_search_tablet") { ccpSearchAndFilter() }
                }
                item(key = "ccp_left_spacer") { Spacer(modifier = Modifier.height(dimens.spacingXl)) }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(dimens.spacingMd)
            ) {
                item { Spacer(modifier = Modifier.height(dimens.spacingXs)) }
                if (filteredAccounts.isEmpty()) {
                    item(key = "ccp_empty_tablet") {
                        LiquidEmptyStateCard(
                            title = if (accounts.isEmpty()) {
                                if (isArabic) "لا توجد ملفات CCP محفوظة" else "No saved CCP profiles"
                            } else {
                                if (isArabic) "لا توجد نتائج مطابقة" else "No matching profiles"
                            },
                            description = if (accounts.isEmpty()) {
                                if (isArabic) "أضف حتى 1000 شخص (الاسم، ربط بجهة اتصال، CCP والمفتاح Clé، و RIP) مع تحوّل تلقائي فخم لشكل ولون كل وعاء."
                                else "Store up to 1,000 CCP & RIP profiles with app-themed animated cards, linked contacts, and one-tap copy."
                            } else {
                                if (isArabic) "جرّب تغيير التصفية أو البحث باسم آخر."
                                else "Adjust your filter or search terms."
                            },
                            icon = Icons.Default.AccountBalance,
                            iconColor = CyanNeon
                        )
                    }
                } else {
                    items(filteredAccounts, key = { it.id }) { acc ->
                        val isExpanded = acc.id in expandedCcpIds
                        val isDigitsVisible = !isStealth && (acc.id in revealedCcpIds)
                        CcpAccountCard(
                            account = acc,
                            isExpanded = isExpanded,
                            isDigitsVisible = isDigitsVisible,
                            isStealth = isStealth,
                            isArabic = isArabic,
                            onToggleExpand = { onToggleExpandCcp(acc.id) },
                            onToggleDigitsVisibility = { onToggleRevealCcp(acc.id) },
                            onTogglePin = { onTogglePinCcpAccount(acc.id) },
                            onHoldActions = { holdActionTargetAccount = acc },
                            onCopyName = {
                                copyText(
                                    label = "Name",
                                    text = acc.displayFullName,
                                    toastMessage = if (isArabic) "تم نسخ الاسم: ${acc.displayFullName}" else "Name copied ✓"
                                )
                            },
                            onCopyCcp = {
                                val value = if (acc.ccpKey.isNotBlank()) "${acc.ccpNumber} ${acc.ccpKey}" else acc.ccpNumber
                                copyText(
                                    label = "CCP",
                                    text = value,
                                    toastMessage = if (isArabic) "تم نسخ رقم CCP والمفتاح ✓" else "CCP & Clé copied ✓"
                                )
                            },
                            onCopyRip = {
                                copyText(
                                    label = "RIP",
                                    text = acc.ripFull,
                                    toastMessage = if (isArabic) "تم نسخ رقم RIP ✓" else "RIP copied ✓"
                                )
                            },
                            onCopyPhone = {
                                if (acc.contactPhone.isNotBlank()) {
                                    copyText(
                                        label = "Phone",
                                        text = acc.contactPhone,
                                        toastMessage = if (isArabic) "تم نسخ رقم الهاتف ✓" else "Phone copied ✓"
                                    )
                                }
                            },
                            onCopyAll = {
                                val fullBlock = buildString {
                                    append(acc.displayFullName)
                                    if (acc.ccpNumber.isNotBlank()) {
                                        append("\nCCP: ${acc.ccpNumber}")
                                        if (acc.ccpKey.isNotBlank()) append(" | Clé: ${acc.ccpKey}")
                                    }
                                    if (acc.ripFull.isNotBlank()) append("\nRIP: ${acc.ripFull}")
                                    if (acc.contactPhone.isNotBlank()) append("\nTel: ${acc.contactPhone}")
                                    if (acc.agencyNote.isNotBlank()) append("\nNote: ${acc.agencyNote}")
                                }
                                copyText(
                                    label = "CCP Profile",
                                    text = fullBlock,
                                    toastMessage = if (isArabic) "تم نسخ جميع معطيات الملف ✓" else "All profile data copied ✓"
                                )
                            }
                        )
                    }
                }
                item(key = "ccp_bottom_spacer_tablet") { Spacer(modifier = Modifier.height(dimens.spacingXl)) }
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = dimens.screenPaddingH),
            verticalArrangement = Arrangement.spacedBy(dimens.spacingMd)
        ) {
            item(key = "ccp_header") {
                Spacer(modifier = Modifier.height(dimens.spacingXs))
                ccpHeader()
            }
            item(key = "ccp_smart_summary") { ccpSummaryCard() }
            if (accounts.isNotEmpty()) {
                item(key = "ccp_search") { ccpSearchAndFilter() }
            }
            if (filteredAccounts.isEmpty()) {
                item(key = "ccp_empty") {
                    LiquidEmptyStateCard(
                        title = if (accounts.isEmpty()) {
                            if (isArabic) "لا توجد ملفات CCP محفوظة" else "No saved CCP profiles"
                        } else {
                            if (isArabic) "لا توجد نتائج مطابقة" else "No matching profiles"
                        },
                        description = if (accounts.isEmpty()) {
                            if (isArabic) "أضف حتى 1000 شخص (الاسم، ربط بجهة اتصال، CCP والمفتاح Clé، و RIP) مع تحوّل تلقائي فخم لشكل ولون كل وعاء."
                            else "Store up to 1,000 CCP & RIP profiles with app-themed animated cards, linked contacts, and one-tap copy."
                        } else {
                            if (isArabic) "جرّب تغيير التصفية أو البحث باسم آخر."
                            else "Adjust your filter or search terms."
                        },
                        icon = Icons.Default.AccountBalance,
                        iconColor = CyanNeon
                    )
                }
            } else {
                items(filteredAccounts, key = { it.id }) { acc ->
                    val isExpanded = acc.id in expandedCcpIds
                    val isDigitsVisible = !isStealth && (acc.id in revealedCcpIds)
                    CcpAccountCard(
                        account = acc,
                        isExpanded = isExpanded,
                        isDigitsVisible = isDigitsVisible,
                        isStealth = isStealth,
                        isArabic = isArabic,
                        onToggleExpand = { onToggleExpandCcp(acc.id) },
                        onToggleDigitsVisibility = { onToggleRevealCcp(acc.id) },
                        onTogglePin = { onTogglePinCcpAccount(acc.id) },
                        onHoldActions = { holdActionTargetAccount = acc },
                        onCopyName = {
                            copyText(
                                label = "Name",
                                text = acc.displayFullName,
                                toastMessage = if (isArabic) "تم نسخ الاسم: ${acc.displayFullName}" else "Name copied ✓"
                            )
                        },
                        onCopyCcp = {
                            val value = if (acc.ccpKey.isNotBlank()) "${acc.ccpNumber} ${acc.ccpKey}" else acc.ccpNumber
                            copyText(
                                label = "CCP",
                                text = value,
                                toastMessage = if (isArabic) "تم نسخ رقم CCP والمفتاح ✓" else "CCP & Clé copied ✓"
                            )
                        },
                        onCopyRip = {
                            copyText(
                                label = "RIP",
                                text = acc.ripFull,
                                toastMessage = if (isArabic) "تم نسخ رقم RIP ✓" else "RIP copied ✓"
                            )
                        },
                        onCopyPhone = {
                            if (acc.contactPhone.isNotBlank()) {
                                copyText(
                                    label = "Phone",
                                    text = acc.contactPhone,
                                    toastMessage = if (isArabic) "تم نسخ رقم الهاتف ✓" else "Phone copied ✓"
                                )
                            }
                        },
                        onCopyAll = {
                            val fullBlock = buildString {
                                append(acc.displayFullName)
                                if (acc.ccpNumber.isNotBlank()) {
                                    append("\nCCP: ${acc.ccpNumber}")
                                    if (acc.ccpKey.isNotBlank()) append(" | Clé: ${acc.ccpKey}")
                                }
                                if (acc.ripFull.isNotBlank()) append("\nRIP: ${acc.ripFull}")
                                if (acc.contactPhone.isNotBlank()) append("\nTel: ${acc.contactPhone}")
                                if (acc.agencyNote.isNotBlank()) append("\nNote: ${acc.agencyNote}")
                            }
                            copyText(
                                label = "CCP Profile",
                                text = fullBlock,
                                toastMessage = if (isArabic) "تم نسخ جميع معطيات الملف ✓" else "All profile data copied ✓"
                            )
                        }
                    )
                }
            }

            item(key = "ccp_bottom_spacer") {
                Spacer(modifier = Modifier.height(84.dp))
            }
        }
    }
}

@Composable
private fun CcpSummaryChip(
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

@Composable
private fun CcpAccountCard(
    account: CcpAccountEntry,
    isExpanded: Boolean,
    isDigitsVisible: Boolean,
    isStealth: Boolean,
    isArabic: Boolean,
    onToggleExpand: () -> Unit,
    onToggleDigitsVisibility: () -> Unit,
    onTogglePin: () -> Unit,
    onHoldActions: () -> Unit,
    onCopyName: () -> Unit,
    onCopyCcp: () -> Unit,
    onCopyRip: () -> Unit,
    onCopyPhone: () -> Unit,
    onCopyAll: () -> Unit
) {
    val colors = LiquidGlassTheme.colors
    val haptic = LocalHapticFeedback.current
    val preset = remember(account.appPresetId, account.displayFullName, account.agencyNote) {
        VaultAppPresets.resolveForCcpEntry(account)
    }
    val brandPrimary by animateColorAsState(preset.primaryColor(), tween(320), label = "ccp_bp")

    var copiedCcpRecently by remember { mutableStateOf(false) }
    var copiedRipRecently by remember { mutableStateOf(false) }
    var copiedAllRecently by remember { mutableStateOf(false) }

    // Standardized 0.5s hold (~100ms TapTimeout + 400ms linear progress)
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

    LaunchedEffect(copiedCcpRecently) {
        if (copiedCcpRecently) {
            delay(1400)
            copiedCcpRecently = false
        }
    }
    LaunchedEffect(copiedRipRecently) {
        if (copiedRipRecently) {
            delay(1400)
            copiedRipRecently = false
        }
    }
    LaunchedEffect(copiedAllRecently) {
        if (copiedAllRecently) {
            delay(1400)
            copiedAllRecently = false
        }
    }

    val cardScale = 1f - (holdAnim.value * 0.015f)
    val borderCol = when {
        holdAnim.value > 0.05f -> brandPrimary
        account.isPinned -> AmberNeon.copy(alpha = if (colors.isDark) 0.65f else 0.72f)
        else -> colors.glassBorder
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .scale(cardScale)
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
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            .testTag("ccp_vault_card_${account.id}"),
        shape = RoundedCornerShape(18.dp),
        cornerRadius = 18.dp,
        backgroundColor = colors.glassCard,
        borderColor = borderCol,
        borderWidth = if (account.isPinned || holdAnim.value > 0.05f) 1.4.dp else 1.dp,
        breathingGlow = false
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
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

            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                // 1. Clean Utility Header Row: Accent Rail + Brand Badge + Name + Status Badges + 48dp Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.5.dp)
                            .height(38.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (account.isPinned) AmberNeon else brandPrimary)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    VaultAppEmblemBadge(preset = preset, size = 40.dp)
                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = account.displayFullName,
                            color = colors.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(brandPrimary.copy(alpha = if (colors.isDark) 0.16f else 0.12f))
                                    .border(0.6.dp, brandPrimary.copy(alpha = 0.36f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isArabic) preset.nameAr else preset.nameEn,
                                    color = brandPrimary,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (account.isPinned) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(AmberNeon.copy(alpha = 0.18f))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isArabic) "مثبت" else "PIN",
                                        color = AmberNeon,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }

                            if (account.ripFull.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(EmeraldNeon.copy(alpha = if (colors.isDark) 0.16f else 0.13f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "RIP",
                                        color = EmeraldNeon,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (account.hasLinkedContact) {
                            Spacer(modifier = Modifier.height(3.dp))
                            val contactMeta = buildString {
                                append(if (isStealth) "••••••" else account.contactName)
                                if (account.contactPhone.isNotBlank()) {
                                    append(" • ${if (!isDigitsVisible) "••••••" else account.contactPhone}")
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = if (isExpanded && account.contactPhone.isNotBlank()) {
                                    Modifier.clickable { onCopyPhone() }
                                } else Modifier
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContactPhone,
                                    contentDescription = null,
                                    tint = EmeraldNeon,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = contactMeta,
                                    color = EmeraldNeon,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onTogglePin,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("pin_ccp_${account.id}")
                        ) {
                            Icon(
                                imageVector = if (account.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                contentDescription = if (isArabic) "تثبيت في الأعلى" else "Pin to top",
                                tint = if (account.isPinned) AmberNeon else colors.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onToggleExpand,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("expand_ccp_${account.id}")
                        ) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isArabic) "توسيع/طي" else "Expand/Collapse",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // 2. Scannable Utility Summary Strip & 0.5s Hold Cue
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(
                    color = colors.glassBorderSubtle.copy(alpha = 0.55f),
                    thickness = 0.75.dp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val quickPreview = when {
                        isStealth -> "CCP ••••••"
                        account.ccpNumber.isNotBlank() -> {
                            val tail = account.ccpNumber.takeLast(4)
                            if (account.ccpKey.isNotBlank()) "CCP ••••$tail (${account.ccpKey})" else "CCP ••••$tail"
                        }
                        account.ripFull.isNotBlank() -> "RIP ••••${account.ripFull.takeLast(4)}"
                        else -> if (isArabic) "اضغط لعرض التفاصيل" else "Tap to view details"
                    }
                    Text(
                        text = quickPreview,
                        color = colors.textSecondary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        style = TextStyle(fontFeatureSettings = "tnum"),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    Text(
                        text = if (isArabic) "اضغط 0.5ث للتعديل" else "Hold 0.5s • Edit",
                        color = colors.textMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }

                // 3. Expanded Structured Utility Rows
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn(tween(200)) + expandVertically(tween(240)),
                    exit = fadeOut(tween(160)) + shrinkVertically(tween(200))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(10.dp))

                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            if (account.ccpNumber.isNotBlank()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.glassSurface)
                                        .border(
                                            width = 0.9.dp,
                                            color = if (copiedCcpRecently) EmeraldNeon.copy(alpha = 0.80f)
                                            else colors.glassBorderSubtle,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            copiedCcpRecently = true
                                            onCopyCcp()
                                        }
                                        .padding(start = 12.dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "CCP • CLÉ",
                                            color = brandPrimary,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 0.5.sp
                                        )
                                        val ccpDisplay = if (!isDigitsVisible) "••••••••••" else buildString {
                                            append(account.ccpNumber)
                                            if (account.ccpKey.isNotBlank()) append("   Clé ${account.ccpKey}")
                                        }
                                        AnimatedContent(
                                            targetState = ccpDisplay,
                                            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(160)) },
                                            label = "ccp_digits_anim"
                                        ) { textVal ->
                                            Text(
                                                text = textVal,
                                                color = colors.textPrimary,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                style = TextStyle(fontFeatureSettings = "tnum")
                                            )
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = onToggleDigitsVisibility, modifier = Modifier.size(48.dp)) {
                                            Icon(
                                                imageVector = if (isDigitsVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = if (isArabic) "إظهار/إخفاء" else "Show/Hide",
                                                tint = brandPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                copiedCcpRecently = true
                                                onCopyCcp()
                                            },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (copiedCcpRecently) Icons.Default.Check else Icons.Default.ContentCopy,
                                                contentDescription = "Copy CCP",
                                                tint = if (copiedCcpRecently) EmeraldNeon else brandPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (account.ripFull.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.glassSurface)
                                        .border(
                                            width = 0.9.dp,
                                            color = if (copiedRipRecently) EmeraldNeon.copy(alpha = 0.85f)
                                            else colors.glassBorderSubtle,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            copiedRipRecently = true
                                            onCopyRip()
                                        }
                                        .padding(start = 12.dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "RIP (ALGÉRIE POSTE)",
                                            color = EmeraldNeon,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 0.5.sp
                                        )
                                        val ripDisplay = if (!isDigitsVisible) "007 99999 •••••••••• ••" else {
                                            HubVaultCodec.formatRipGrouped(account.ripFull)
                                        }
                                        AnimatedContent(
                                            targetState = ripDisplay,
                                            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(160)) },
                                            label = "rip_digits_anim"
                                        ) { textVal ->
                                            Text(
                                                text = textVal,
                                                color = colors.textPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                style = TextStyle(fontFeatureSettings = "tnum"),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            copiedRipRecently = true
                                            onCopyRip()
                                        },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (copiedRipRecently) Icons.Default.Check else Icons.Default.ContentCopy,
                                            contentDescription = "Copy RIP",
                                            tint = EmeraldNeon,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Note + Quick Utility Footer Actions (48dp touch targets)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (account.agencyNote.isNotBlank()) {
                                Text(
                                    text = if (isStealth) "••••••" else account.agencyNote,
                                    color = colors.textSecondary,
                                    fontSize = 11.5.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp)
                                )
                            } else {
                                Row(
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.glassSurface)
                                        .border(0.8.dp, colors.glassBorderSubtle, RoundedCornerShape(12.dp))
                                        .clickable { onCopyName() }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = brandPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (isArabic) "نسخ الاسم" else "Copy Name",
                                        color = colors.textSecondary,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                            }

                            Row(
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (copiedAllRecently) EmeraldNeon.copy(alpha = 0.20f)
                                        else brandPrimary.copy(alpha = 0.14f)
                                    )
                                    .border(
                                        0.8.dp,
                                        if (copiedAllRecently) EmeraldNeon.copy(alpha = 0.55f)
                                        else brandPrimary.copy(alpha = 0.38f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        copiedAllRecently = true
                                        onCopyAll()
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (copiedAllRecently) Icons.Default.Check else Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    tint = if (copiedAllRecently) EmeraldNeon else brandPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (copiedAllRecently) {
                                        if (isArabic) "تم النسخ ✓" else "Copied ✓"
                                    } else {
                                        if (isArabic) "نسخ الكل" else "Copy All"
                                    },
                                    color = if (copiedAllRecently) EmeraldNeon else brandPrimary,
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

@Composable
private fun AddOrEditCcpAccountDialog(
    existing: CcpAccountEntry?,
    secureScreenshots: Boolean,
    isAtCapacity: Boolean,
    shimmerPhase: Float,
    pulsePhase: Float,
    onDismiss: () -> Unit,
    onSave: (
        firstName: String,
        lastName: String,
        ccpNumber: String,
        ccpKey: String,
        ripNumber: String,
        agencyNote: String,
        contactName: String,
        contactUri: String,
        contactPhone: String,
        isPinned: Boolean,
        appPresetId: String
    ) -> Unit
) {
    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors
    val isArabic = strings is ArabicStrings
    val context = LocalContext.current

    val initialPreset = remember(existing) {
        if (existing != null) VaultAppPresets.resolveForCcpEntry(existing).id else "algerie_poste"
    }
    var selectedPresetId by remember(existing) { mutableStateOf(initialPreset) }
    var showAppPickerModal by remember { mutableStateOf(false) }

    val activePreset = remember(selectedPresetId) { VaultAppPresets.findById(selectedPresetId) }
    val presetAccent = activePreset.primaryColor()

    var firstName by remember(existing) { mutableStateOf(existing?.firstName ?: "") }
    var lastName by remember(existing) { mutableStateOf(existing?.lastName ?: "") }
    var ccpNumber by remember(existing) { mutableStateOf(existing?.ccpNumber ?: "") }
    var ccpKey by remember(existing) { mutableStateOf(existing?.ccpKey ?: "") }
    var ripNumber by remember(existing) { mutableStateOf(existing?.ripFull ?: "") }
    var agencyNote by remember(existing) { mutableStateOf(existing?.agencyNote ?: "") }
    var contactName by remember(existing) { mutableStateOf(existing?.contactName ?: "") }
    var contactUri by remember(existing) { mutableStateOf(existing?.contactUri ?: "") }
    var contactPhone by remember(existing) { mutableStateOf(existing?.contactPhone ?: "") }
    var isPinned by remember(existing) { mutableStateOf(existing?.isPinned ?: false) }

    var lastAutoFilledContactName by remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        onDispose {
            firstName = ""
            lastName = ""
            ccpNumber = ""
            ccpKey = ""
            ripNumber = ""
            agencyNote = ""
            contactName = ""
            contactUri = ""
            contactPhone = ""
            lastAutoFilledContactName = ""
        }
    }

    if (showAppPickerModal) {
        AppPresetPickerDialog(
            selectedPresetId = selectedPresetId,
            isArabic = isArabic,
            onSelect = { chosen ->
                selectedPresetId = chosen.id
                showAppPickerModal = false
            },
            onDismiss = { showAppPickerModal = false }
        )
    }

    fun applyResolvedContact(name: String, phone: String, uriStr: String) {
        contactUri = uriStr
        if (name.isNotBlank()) {
            contactName = name
            val (fn, ln) = HubVaultCodec.splitFullName(name)
            val prevSplit = if (lastAutoFilledContactName.isNotBlank()) {
                HubVaultCodec.splitFullName(lastAutoFilledContactName)
            } else {
                "" to ""
            }
            if (firstName.isBlank() || (lastAutoFilledContactName.isNotBlank() && firstName == prevSplit.first)) {
                firstName = fn
            }
            if (lastName.isBlank() || (lastAutoFilledContactName.isNotBlank() && lastName == prevSplit.second)) {
                lastName = ln
            }
            lastAutoFilledContactName = name
        }
        if (phone.isNotBlank()) {
            contactPhone = phone
        }
    }

    val contactPicker = rememberContactPickerFlow(
        secureScreenshots = secureScreenshots
    ) { resolvedName, resolvedPhone, resolvedUriStr ->
        applyResolvedContact(resolvedName, resolvedPhone, resolvedUriStr)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            securePolicy = if (secureScreenshots) SecureFlagPolicy.SecureOn else SecureFlagPolicy.SecureOff
        )
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
                // Live Brand Transformation Preview Banner matching KeysScreen Dialog
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
                                    if (isArabic) "إضافة ملف CCP جديد" else "Add CCP Person Profile"
                                } else {
                                    if (isArabic) "تعديل ملف CCP" else "Edit CCP Profile"
                                },
                                color = colors.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = if (isArabic) "نمط البطاقة: ${activePreset.nameAr}"
                                else "Card theme: ${activePreset.nameEn}",
                                color = presetAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Prominent Style / App Selector Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(presetAccent.copy(alpha = 0.14f))
                        .border(1.dp, presetAccent.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                        .clickable { showAppPickerModal = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .testTag("select_ccp_preset_button"),
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
                                text = if (isArabic) "اختيار شكل ولون الوعاء" else "Select Card Style / Theme",
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

                // Quick Popular Style Chips Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val quickIds = listOf("algerie_poste", "baridimob", "redotpay", "whatsapp", "bybit", "binance", "custom")
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
                                .clickable { selectedPresetId = p.id }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            VaultAppEmblemBadge(preset = p, size = 20.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = p.nameEn.substringBefore(" /"),
                                color = if (selected) pColor else colors.textSecondary,
                                fontSize = 11.5.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 1. Link Contact Card Button
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (contactName.isNotBlank() || contactUri.isNotBlank()) {
                                EmeraldNeon.copy(alpha = 0.14f)
                            } else {
                                CyanNeon.copy(alpha = 0.12f)
                            }
                        )
                        .border(
                            1.dp,
                            if (contactName.isNotBlank() || contactUri.isNotBlank()) {
                                EmeraldNeon.copy(alpha = 0.45f)
                            } else {
                                CyanNeon.copy(alpha = 0.35f)
                            },
                            RoundedCornerShape(14.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { contactPicker.openPicker() }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .testTag("link_ccp_contact_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContactPhone,
                            contentDescription = null,
                            tint = if (contactName.isNotBlank()) EmeraldNeon else CyanNeon,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (contactName.isNotBlank()) {
                                    if (isArabic) "مرتبط: $contactName" else "Linked: $contactName"
                                } else {
                                    if (isArabic) "ربط الملف من جهات الاتصال" else "Link Profile from Contacts"
                                },
                                color = colors.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (contactPhone.isNotBlank()) {
                                    contactPhone
                                } else {
                                    if (isArabic) "اضغط لاختيار جهة اتصال وتعبئة الاسم تلقائياً" else "Tap to select a phone contact"
                                },
                                color = colors.textMuted,
                                fontSize = 10.5.sp
                            )
                        }
                        if (contactName.isNotBlank() || contactUri.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    contactName = ""
                                    contactUri = ""
                                    contactPhone = ""
                                    lastAutoFilledContactName = ""
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = if (isArabic) "إلغاء الربط" else "Unlink",
                                    tint = RoseNeon,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    if (contactPicker.permissionDeniedHint && !contactPicker.hasPermission) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(RoseNeon.copy(alpha = 0.10f))
                                .clickable { openAppContactsPermissionSettings(context) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isArabic) "إذن جهات الاتصال مرفوض — اضغط لفتح الإعدادات وتفعيله"
                                else "Contacts permission denied — tap to enable in Settings",
                                color = RoseNeon,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isArabic) "الإعدادات" else "Settings",
                                color = CyanNeon,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. First Name (الاسم) & Last Name (اللقب)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        CcpFieldBox(
                            label = if (isArabic) "الاسم" else "First Name",
                            value = firstName,
                            onValueChange = { firstName = it },
                            placeholder = if (isArabic) "مثال: أمين" else "e.g. Amine"
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        CcpFieldBox(
                            label = if (isArabic) "اللقب" else "Last Name",
                            value = lastName,
                            onValueChange = { lastName = it },
                            placeholder = if (isArabic) "مثال: بن علي" else "e.g. Benali"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. CCP Number & Clé (رقم الحساب البريدي CCP والمفتاح Clé)
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(modifier = Modifier.weight(1.75f)) {
                            CcpFieldBox(
                                label = if (isArabic) "رقم الحساب البريدي CCP" else "CCP Number",
                                value = ccpNumber,
                                onValueChange = { ccpNumber = it.filter { ch -> ch.isDigit() }.take(10) },
                                isNumeric = true,
                                placeholder = "0012345678"
                            )
                        }
                        Box(modifier = Modifier.weight(0.85f)) {
                            CcpFieldBox(
                                label = if (isArabic) "المفتاح Clé" else "Clé",
                                value = ccpKey,
                                onValueChange = { ccpKey = it.filter { ch -> ch.isDigit() }.take(2) },
                                isNumeric = true,
                                placeholder = "42"
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 4. Directly Below: Manual RIP Number Field
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    CcpFieldBox(
                        label = if (isArabic) "رقم الـ RIP (اختياري)" else "RIP Number (optional)",
                        value = ripNumber,
                        onValueChange = { ripNumber = it.filter { ch -> ch.isDigit() }.take(20) },
                        isNumeric = true,
                        placeholder = "00799999001234567897"
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 5. Directly Below RIP: Note (الملاحظة)
                CcpFieldBox(
                    label = if (isArabic) "الملاحظة (اختياري)" else "Note (optional)",
                    value = agencyNote,
                    onValueChange = { agencyNote = it },
                    placeholder = if (isArabic) "الولاية، الصفة، أو تفاصيل إضافية..." else "Wilaya, relationship, or note..."
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
                            text = if (isArabic) "تثبيت الملف في الأعلى" else "Pin profile to top",
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

                val canSave = (firstName.isNotBlank() || lastName.isNotBlank()) &&
                    (ccpNumber.isNotBlank() || ripNumber.isNotBlank()) &&
                    !isAtCapacity

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
                        enabled = canSave,
                        containerColor = presetAccent,
                        contentColor = Color(0xFF04202C),
                        modifier = Modifier.weight(1.2f),
                        onClick = {
                            onSave(
                                firstName,
                                lastName,
                                ccpNumber,
                                ccpKey,
                                ripNumber,
                                agencyNote,
                                contactName,
                                contactUri,
                                contactPhone,
                                isPinned,
                                selectedPresetId
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CcpFieldBox(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isNumeric: Boolean = false,
    placeholder: String = ""
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
        Box(modifier = Modifier.fillMaxWidth()) {
            if (value.isEmpty() && placeholder.isNotEmpty()) {
                Text(
                    text = placeholder,
                    color = colors.textMuted.copy(alpha = 0.55f),
                    fontSize = 13.5.sp,
                    fontFamily = if (isNumeric) FontFamily.Monospace else FontFamily.Default,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = if (isNumeric) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
                textStyle = TextStyle(
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = if (isNumeric) FontFamily.Monospace else FontFamily.Default
                ),
                cursorBrush = SolidColor(CyanNeon),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

