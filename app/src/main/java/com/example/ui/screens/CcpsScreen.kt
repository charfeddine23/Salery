package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
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
import androidx.core.content.ContextCompat
import com.example.data.CcpAccountEntry
import com.example.data.HubVaultCodec
import com.example.data.UserSettings
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.LiquidEmptyStateCard
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

@Composable
fun CcpsScreen(
    userSettings: UserSettings,
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
        isPinned: Boolean
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
        isPinned: Boolean
    ) -> Unit,
    onTogglePinCcpAccount: (String) -> Unit,
    onDeleteCcpAccount: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors
    val isArabic = strings is ArabicStrings
    val isStealth = LocalStealthMode.current
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current
    val context = LocalContext.current

    val accounts = remember(userSettings.ccpAccountsJson) {
        HubVaultCodec.parseCcpAccounts(userSettings.ccpAccountsJson)
    }

    var searchQuery by remember { mutableStateOf("") }
    val filteredAccounts by remember(accounts, searchQuery) {
        derivedStateOf {
            val q = searchQuery.trim().lowercase()
            if (q.isEmpty()) {
                accounts
            } else {
                accounts.filter { acc ->
                    acc.displayFullName.lowercase().contains(q) ||
                        acc.ccpNumber.contains(q) ||
                        acc.ripFull.contains(q) ||
                        acc.agencyNote.lowercase().contains(q) ||
                        acc.contactName.lowercase().contains(q) ||
                        acc.contactPhone.contains(q)
                }
            }
        }
    }

    var showAddOrEditDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<CcpAccountEntry?>(null) }

    if (showAddOrEditDialog) {
        AddOrEditCcpAccountDialog(
            existing = editingAccount,
            secureScreenshots = userSettings.screenshotProtectionEnabled,
            isAtCapacity = editingAccount == null && accounts.size >= HubVaultCodec.MAX_CCP_PROFILES,
            onDismiss = {
                showAddOrEditDialog = false
                editingAccount = null
            },
            onSave = { first, last, ccp, key, rip, note, cName, cUri, cPhone, pinned ->
                val target = editingAccount
                if (target == null) {
                    onAddCcpAccount(first, last, ccp, key, rip, note, cName, cUri, cPhone, pinned)
                } else {
                    onUpdateCcpAccount(target.id, first, last, ccp, key, rip, note, cName, cUri, cPhone, pinned)
                }
                showAddOrEditDialog = false
                editingAccount = null
            }
        )
    }

    fun copyText(label: String, text: String, toastMessage: String) {
        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cb.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = dimens.screenPaddingH),
        verticalArrangement = Arrangement.spacedBy(dimens.spacingMd)
    ) {
        // Header + Add Person Button + Capacity Counter (up to 1000)
        item(key = "ccp_header") {
            Spacer(modifier = Modifier.height(dimens.spacingSm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "CCP's",
                            color = colors.textPrimary,
                            fontSize = typo.title,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(CyanNeon.copy(alpha = 0.14f))
                                .border(1.dp, CyanNeon.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${accounts.size} / ${HubVaultCodec.MAX_CCP_PROFILES}",
                                color = CyanNeon,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isArabic) "دليل الحسابات البريدية، جهات الاتصال والنسخ السريع"
                        else "CCP directory, manual RIP, linked contacts & quick copy",
                        color = colors.textMuted,
                        fontSize = typo.caption
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

        // Search Bar (fast filtering across up to 1,000 profiles)
        item(key = "ccp_search") {
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
                                fontSize = 13.sp,
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
                                fontSize = 13.5.sp,
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
            }
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
                        if (isArabic) "أضف حتى 1000 شخص (الاسم واللقب، ربط بجهة اتصال، CCP والمفتاح Clé، و RIP) مع النسخ بنقرة والتثبيت في الأعلى."
                        else "Store up to 1,000 CCP & RIP profiles with linked contacts, top pinning, and one-tap copy."
                    } else {
                        if (isArabic) "جرّب البحث باسم آخر أو برقم CCP مختلف."
                        else "Try searching with a different name or CCP digits."
                    },
                    icon = Icons.Default.AccountBalance,
                    iconColor = CyanNeon
                )
            }
        } else {
            items(filteredAccounts, key = { it.id }) { acc ->
                CcpAccountCard(
                    account = acc,
                    isStealth = isStealth,
                    isArabic = isArabic,
                    onTogglePin = {
                        onTogglePinCcpAccount(acc.id)
                        Toast.makeText(
                            context,
                            if (!acc.isPinned) {
                                if (isArabic) "تم تثبيت الملف في الأعلى 📌" else "Pinned to top 📌"
                            } else {
                                if (isArabic) "تم إلغاء التثبيت" else "Unpinned"
                            },
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onEdit = {
                        editingAccount = acc
                        showAddOrEditDialog = true
                    },
                    onCopyName = {
                        copyText(
                            label = "Name",
                            text = acc.displayFullName,
                            toastMessage = if (isArabic) "تم نسخ الاسم: ${acc.displayFullName}" else "Name copied"
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
                    },
                    onDelete = { onDeleteCcpAccount(acc.id) }
                )
            }
        }

        item(key = "ccp_bottom_spacer") {
            Spacer(modifier = Modifier.height(84.dp))
        }
    }
}

@Composable
private fun CcpAccountCard(
    account: CcpAccountEntry,
    isStealth: Boolean,
    isArabic: Boolean,
    onTogglePin: () -> Unit,
    onEdit: () -> Unit,
    onCopyName: () -> Unit,
    onCopyCcp: () -> Unit,
    onCopyRip: () -> Unit,
    onCopyPhone: () -> Unit,
    onCopyAll: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LiquidGlassTheme.colors
    val cardBorderColor = if (account.isPinned) AmberNeon.copy(alpha = 0.65f) else CyanNeon.copy(alpha = 0.32f)

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = if (account.isPinned) colors.glassSurface else colors.glassCard,
        borderColor = cardBorderColor
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row: Avatar + Full Name + Pinned Badge + Pin / Edit / Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onCopyName() }
                        .padding(vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (account.isPinned) AmberNeon.copy(alpha = 0.18f)
                                else CyanNeon.copy(alpha = 0.16f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (account.hasLinkedContact) Icons.Default.ContactPhone else Icons.Default.Person,
                            contentDescription = null,
                            tint = if (account.isPinned) AmberNeon else CyanNeon,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = account.displayFullName,
                                color = colors.textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (account.isPinned) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(AmberNeon.copy(alpha = 0.18f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isArabic) "مثبت" else "PINNED",
                                        color = AmberNeon,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }

                        if (account.hasLinkedContact) {
                            val contactMeta = buildString {
                                append(if (isArabic) "مرتبط: ${account.contactName}" else "Contact: ${account.contactName}")
                                if (account.contactPhone.isNotBlank()) {
                                    append(" • ${if (isStealth) "••••••" else account.contactPhone}")
                                }
                            }
                            Text(
                                text = contactMeta,
                                color = EmeraldNeon,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = if (account.contactPhone.isNotBlank()) {
                                    Modifier.clickable { onCopyPhone() }
                                } else Modifier
                            )
                        }
                    }
                }

                // Action Icons (>= 48dp touch targets)
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
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = if (isArabic) "تعديل" else "Edit",
                            tint = CyanNeon,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = if (isArabic) "حذف" else "Delete",
                            tint = RoseNeon,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // CCP & Clé One-Tap Copy Row
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                if (account.ccpNumber.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.glassSurface)
                            .border(0.8.dp, colors.glassBorderSubtle, RoundedCornerShape(12.dp))
                            .clickable { onCopyCcp() }
                            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "CCP • CLÉ",
                                color = CyanNeon,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            val ccpDisplay = if (isStealth) "••••••••••" else buildString {
                                append(account.ccpNumber)
                                if (account.ccpKey.isNotBlank()) append("   Clé ${account.ccpKey}")
                            }
                            Text(
                                text = ccpDisplay,
                                color = colors.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        IconButton(onClick = onCopyCcp, modifier = Modifier.size(48.dp)) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy CCP",
                                tint = CyanNeon,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }

                // Manual RIP One-Tap Copy Row
                if (account.ripFull.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.glassSurface)
                            .border(0.8.dp, EmeraldNeon.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                            .clickable { onCopyRip() }
                            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "RIP (ALGÉRIE POSTE)",
                                color = EmeraldNeon,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = if (isStealth) "007 99999 •••••••••• ••" else HubVaultCodec.formatRipGrouped(account.ripFull),
                                color = colors.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = onCopyRip, modifier = Modifier.size(48.dp)) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy RIP",
                                tint = EmeraldNeon,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }

            // Note (الملاحظة) + One-Tap Copy All button
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (account.agencyNote.isNotBlank()) {
                    Text(
                        text = account.agencyNote,
                        color = colors.textSecondary,
                        fontSize = 11.5.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyanNeon.copy(alpha = 0.14f))
                        .border(0.8.dp, CyanNeon.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .clickable { onCopyAll() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        tint = CyanNeon,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isArabic) "نسخ الكل" else "Copy All",
                        color = CyanNeon,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
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
        isPinned: Boolean
    ) -> Unit
) {
    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors
    val isArabic = strings is ArabicStrings
    val context = LocalContext.current

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

    var showInAppContactsPicker by remember { mutableStateOf(false) }
    var contactsPermissionDeniedHint by remember { mutableStateOf(false) }
    var lastAutoFilledContactName by remember { mutableStateOf("") }

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
        contactsPermissionDeniedHint = false
        Toast.makeText(
            context,
            if (isArabic) "تم ربط جهة الاتصال بنجاح" else "Contact linked successfully",
            Toast.LENGTH_SHORT
        ).show()
    }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { pickedUri: Uri? ->
        pickedUri?.let { uri ->
            val (resolvedName, resolvedPhone, resolvedUriStr) = resolvePickedContactSafely(context, uri)
            if (resolvedName.isBlank() && resolvedPhone.isBlank()) {
                if (hasReadContactsPermission(context)) {
                    showInAppContactsPicker = true
                } else {
                    contactsPermissionDeniedHint = true
                    Toast.makeText(
                        context,
                        if (isArabic) "تعذّرت قراءة جهة الاتصال بدون إذن جهات الاتصال"
                        else "Contacts permission is needed to read contact details",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                applyResolvedContact(resolvedName, resolvedPhone, resolvedUriStr)
            }
        }
    }

    fun launchSystemContactPickerWithFallback() {
        try {
            contactPickerLauncher.launch(null)
        } catch (_: Throwable) {
            if (hasReadContactsPermission(context)) {
                showInAppContactsPicker = true
            } else {
                contactsPermissionDeniedHint = true
                Toast.makeText(
                    context,
                    if (isArabic) "يرجى السماح بالوصول إلى جهات الاتصال أو فتح الإعدادات"
                    else "Please allow Contacts access in settings",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    val readContactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            contactsPermissionDeniedHint = false
            showInAppContactsPicker = true
        } else {
            contactsPermissionDeniedHint = true
            // Try system zero-permission PickContact as fallback on standard AOSP
            launchSystemContactPickerWithFallback()
        }
    }

    if (showInAppContactsPicker) {
        InAppContactsPickerDialog(
            secureScreenshots = secureScreenshots,
            onDismiss = { showInAppContactsPicker = false },
            onSelectContact = { item ->
                showInAppContactsPicker = false
                applyResolvedContact(item.displayName, item.phoneNumber, item.contactUri)
            },
            onOpenSystemPicker = {
                showInAppContactsPicker = false
                launchSystemContactPickerWithFallback()
            }
        )
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
            shape = RoundedCornerShape(24.dp),
            backgroundColor = colors.dialogBackground
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (existing == null) {
                        if (isArabic) "إضافة ملف CCP جديد" else "Add CCP Person Profile"
                    } else {
                        if (isArabic) "تعديل ملف CCP" else "Edit CCP Profile"
                    },
                    color = colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

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
                            .clickable {
                                if (hasReadContactsPermission(context)) {
                                    contactsPermissionDeniedHint = false
                                    showInAppContactsPicker = true
                                } else {
                                    readContactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                                }
                            }
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

                    if (contactsPermissionDeniedHint && !hasReadContactsPermission(context)) {
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
                        containerColor = CyanNeon,
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
                                isPinned
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

private data class DeviceContactItem(
    val id: String,
    val displayName: String,
    val phoneNumber: String,
    val contactUri: String
)

@Composable
private fun InAppContactsPickerDialog(
    secureScreenshots: Boolean,
    onDismiss: () -> Unit,
    onSelectContact: (DeviceContactItem) -> Unit,
    onOpenSystemPicker: () -> Unit
) {
    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors
    val isArabic = strings is ArabicStrings
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var contacts by remember { mutableStateOf<List<DeviceContactItem>>(emptyList()) }

    LaunchedEffect(Unit) {
        isLoading = true
        contacts = withContext(Dispatchers.IO) {
            loadDeviceContactsList(context)
        }
        isLoading = false
    }

    val filtered by remember(contacts, searchQuery) {
        derivedStateOf {
            val q = searchQuery.trim().lowercase()
            if (q.isEmpty()) contacts
            else contacts.filter {
                it.displayName.lowercase().contains(q) ||
                    it.phoneNumber.lowercase().contains(q)
            }
        }
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
                .fillMaxWidth()
                .height(500.dp),
            shape = RoundedCornerShape(24.dp),
            backgroundColor = colors.dialogBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
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
                        Icon(
                            imageVector = Icons.Default.ContactPhone,
                            contentDescription = null,
                            tint = CyanNeon,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isArabic) "اختيار جهة اتصال" else "Select Contact",
                            color = colors.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = strings.cancel,
                            tint = colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Search Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.glassCard)
                        .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = if (isArabic) "ابحث بالاسم أو رقم الهاتف..." else "Search by name or phone...",
                                color = colors.textMuted,
                                fontSize = 13.sp
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = colors.textPrimary,
                                fontSize = 13.5.sp
                            ),
                            cursorBrush = SolidColor(CyanNeon),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when {
                        isLoading -> {
                            CircularProgressIndicator(
                                color = CyanNeon,
                                modifier = Modifier
                                    .size(32.dp)
                                    .align(Alignment.Center)
                            )
                        }
                        filtered.isEmpty() -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (isArabic) "لا توجد جهات اتصال مطابقة" else "No matching contacts found",
                                    color = colors.textSecondary,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        else -> {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(filtered, key = { "${it.id}_${it.phoneNumber}" }) { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(colors.glassCard)
                                            .border(0.8.dp, colors.glassBorderSubtle, RoundedCornerShape(14.dp))
                                            .clickable { onSelectContact(item) }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(CyanNeon.copy(alpha = 0.16f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = item.displayName.firstOrNull()?.uppercase() ?: "#",
                                                color = CyanNeon,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.displayName,
                                                color = colors.textPrimary,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (item.phoneNumber.isNotBlank()) {
                                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                                    Text(
                                                        text = item.phoneNumber,
                                                        color = colors.textMuted,
                                                        fontSize = 11.5.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
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

                Spacer(modifier = Modifier.height(10.dp))

                GlassButton(
                    text = if (isArabic) "الفتح عبر تطبيق جهات الاتصال" else "Open System Contacts App",
                    containerColor = CyanNeon.copy(alpha = 0.18f),
                    contentColor = CyanNeon,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onOpenSystemPicker
                )
            }
        }
    }
}

private fun hasReadContactsPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_CONTACTS
    ) == PackageManager.PERMISSION_GRANTED
}

private fun openAppContactsPermissionSettings(context: Context) {
    try {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}")
        )
        if (context.findActivityOrNull() == null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Throwable) {
    }
}

private tailrec fun Context.findActivityOrNull(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivityOrNull()
    else -> null
}

private fun resolvePickedContactSafely(context: Context, uri: Uri): Triple<String, String, String> {
    val resolver = context.contentResolver
    val lookupUri = try {
        ContactsContract.Contacts.lookupContact(resolver, uri) ?: uri
    } catch (_: Throwable) {
        uri
    }

    var resolvedName = ""
    var contactId = ""
    var resolvedPhone = ""

    fun readCursor(targetUri: Uri) {
        try {
            resolver.query(targetUri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    if (resolvedName.isBlank()) {
                        val nameCols = listOf(
                            ContactsContract.Contacts.DISPLAY_NAME,
                            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                            ContactsContract.Contacts.DISPLAY_NAME_ALTERNATIVE
                        )
                        for (col in nameCols) {
                            val idx = cursor.getColumnIndex(col)
                            if (idx != -1) {
                                val v = cursor.getString(idx)?.trim().orEmpty()
                                if (v.isNotBlank()) {
                                    resolvedName = v
                                    break
                                }
                            }
                        }
                    }
                    if (contactId.isBlank()) {
                        val idCols = listOf(
                            ContactsContract.Contacts._ID,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID
                        )
                        for (col in idCols) {
                            val idx = cursor.getColumnIndex(col)
                            if (idx != -1) {
                                val v = cursor.getString(idx)?.trim().orEmpty()
                                if (v.isNotBlank()) {
                                    contactId = v
                                    break
                                }
                            }
                        }
                    }
                    if (resolvedPhone.isBlank()) {
                        val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        if (numIdx != -1) {
                            resolvedPhone = cursor.getString(numIdx)?.trim().orEmpty()
                        }
                    }
                }
            }
        } catch (_: Throwable) {
        }
    }

    readCursor(lookupUri)
    if ((resolvedName.isBlank() || resolvedPhone.isBlank()) && lookupUri != uri) {
        readCursor(uri)
    }

    if (hasReadContactsPermission(context) && contactId.isNotBlank()) {
        if (resolvedPhone.isBlank()) {
            try {
                resolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                    arrayOf(contactId),
                    null
                )?.use { phoneCursor ->
                    while (phoneCursor.moveToNext()) {
                        val numIdx = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        if (numIdx != -1) {
                            val candidate = phoneCursor.getString(numIdx)?.trim().orEmpty()
                            if (candidate.isNotBlank()) {
                                resolvedPhone = candidate
                                break
                            }
                        }
                    }
                }
            } catch (_: Throwable) {
            }
        }
        if (resolvedName.isBlank()) {
            try {
                resolver.query(
                    ContactsContract.Contacts.CONTENT_URI,
                    arrayOf(ContactsContract.Contacts.DISPLAY_NAME),
                    "${ContactsContract.Contacts._ID} = ?",
                    arrayOf(contactId),
                    null
                )?.use { c ->
                    if (c.moveToFirst()) {
                        val idx = c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                        if (idx != -1) {
                            resolvedName = c.getString(idx)?.trim().orEmpty()
                        }
                    }
                }
            } catch (_: Throwable) {
            }
        }
    }

    return Triple(resolvedName, resolvedPhone, lookupUri.toString())
}

private fun loadDeviceContactsList(context: Context): List<DeviceContactItem> {
    if (!hasReadContactsPermission(context)) return emptyList()
    val resolver = context.contentResolver
    val itemsMap = linkedMapOf<String, DeviceContactItem>()

    try {
        resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY
            ),
            null,
            null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val keyIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY)

            while (cursor.moveToNext()) {
                val id = if (idIdx != -1) cursor.getString(idIdx)?.trim().orEmpty() else ""
                val name = if (nameIdx != -1) cursor.getString(nameIdx)?.trim().orEmpty() else ""
                val phone = if (numIdx != -1) cursor.getString(numIdx)?.trim().orEmpty() else ""
                val lookupKey = if (keyIdx != -1) cursor.getString(keyIdx)?.trim().orEmpty() else ""
                if (name.isBlank() && phone.isBlank()) continue

                val uri = if (id.isNotBlank() && lookupKey.isNotBlank()) {
                    ContactsContract.Contacts.getLookupUri(id.toLongOrNull() ?: 0L, lookupKey)?.toString().orEmpty()
                } else if (id.isNotBlank()) {
                    Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, id).toString()
                } else {
                    ""
                }

                val mapKey = "${id}_${phone.filter { it.isDigit() }}"
                if (!itemsMap.containsKey(mapKey)) {
                    itemsMap[mapKey] = DeviceContactItem(
                        id = id.ifBlank { mapKey },
                        displayName = name.ifBlank { phone },
                        phoneNumber = phone,
                        contactUri = uri
                    )
                }
            }
        }
    } catch (_: Throwable) {
    }

    val seenContactIds = itemsMap.values.map { it.id }.toSet()
    try {
        resolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts.LOOKUP_KEY
            ),
            null,
            null,
            "${ContactsContract.Contacts.DISPLAY_NAME} ASC"
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID)
            val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            val keyIdx = cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY)

            while (cursor.moveToNext()) {
                val id = if (idIdx != -1) cursor.getString(idIdx)?.trim().orEmpty() else ""
                val name = if (nameIdx != -1) cursor.getString(nameIdx)?.trim().orEmpty() else ""
                val lookupKey = if (keyIdx != -1) cursor.getString(keyIdx)?.trim().orEmpty() else ""
                if (id.isBlank() || name.isBlank() || seenContactIds.contains(id)) continue

                val uri = if (lookupKey.isNotBlank()) {
                    ContactsContract.Contacts.getLookupUri(id.toLongOrNull() ?: 0L, lookupKey)?.toString().orEmpty()
                } else {
                    Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, id).toString()
                }

                itemsMap["contact_$id"] = DeviceContactItem(
                    id = id,
                    displayName = name,
                    phoneNumber = "",
                    contactUri = uri
                )
            }
        }
    } catch (_: Throwable) {
    }

    return itemsMap.values.sortedBy { it.displayName.lowercase() }
}
