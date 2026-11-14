package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.data.VaultKeyEntry
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPill
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

private enum class KeysFilter {
    ALL,
    WEAK_OR_REUSED,
    STRONG
}

@Composable
fun KeysScreen(
    userSettings: UserSettings,
    onAddKey: (label: String, username: String, secretValue: String, category: String, note: String) -> Unit,
    onUpdateKey: (keyId: String, label: String, username: String, secretValue: String, category: String, note: String) -> Unit,
    onDeleteKey: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors
    val isArabic = strings is ArabicStrings
    val isStealth = LocalStealthMode.current
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current
    val context = LocalContext.current

    val keys = remember(userSettings.vaultKeysJson) {
        HubVaultCodec.parseVaultKeys(userSettings.vaultKeysJson)
    }

    // Count duplicate passwords across the entire vault for smart reuse detection
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

    val strongCount = remember(analyzedItems) {
        analyzedItems.count { it.second.level == PasswordStrengthLevel.STRONG || it.second.level == PasswordStrengthLevel.GOOD }
    }
    val weakCount = remember(analyzedItems) {
        analyzedItems.count { it.second.level == PasswordStrengthLevel.WEAK || it.second.level == PasswordStrengthLevel.FAIR }
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
                    entry.note.lowercase().contains(q)
                val matchesFilter = when (activeFilter) {
                    KeysFilter.ALL -> true
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

    val revealedMap = remember { mutableStateMapOf<String, Boolean>() }
    var showAddOrEditDialog by remember { mutableStateOf(false) }
    var editingKey by remember { mutableStateOf<VaultKeyEntry?>(null) }

    if (showAddOrEditDialog) {
        AddOrEditVaultKeyDialog(
            existing = editingKey,
            secureScreenshots = userSettings.screenshotProtectionEnabled,
            onDismiss = {
                showAddOrEditDialog = false
                editingKey = null
            },
            onSave = { label, username, secret, category, note ->
                val target = editingKey
                if (target == null) {
                    onAddKey(label, username, secret, category, note)
                } else {
                    onUpdateKey(target.id, label, username, secret, category, note)
                }
                showAddOrEditDialog = false
                editingKey = null
            }
        )
    }

    fun copyToClipboard(label: String, text: String, msg: String) {
        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cb.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = dimens.screenPaddingH),
        verticalArrangement = Arrangement.spacedBy(dimens.spacingMd)
    ) {
        // Header + Add Key Button
        item(key = "keys_header") {
            Spacer(modifier = Modifier.height(dimens.spacingSm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Keys",
                        color = colors.textPrimary,
                        fontSize = typo.title,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isArabic) "خزنة كلمات السر وأسماء المستخدمين مع التقييم الذكي"
                        else "Password & username vault with smart strength analyzer",
                        color = colors.textMuted,
                        fontSize = typo.caption
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

        // Smart Password Audit Summary Card
        item(key = "keys_smart_audit") {
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
                Column(modifier = Modifier.padding(16.dp)) {
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
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (keys.isEmpty()) {
                                    if (isArabic) "أضف حساباتك لتقييم قوة كلمات السر واكتشاف التكرار"
                                    else "Add credentials to evaluate strength & detect reuse"
                                } else {
                                    if (isArabic) "${keys.size} حساب محفوظ • معدل الأمان العام $averageScore%"
                                    else "${keys.size} saved accounts • Overall security score $averageScore%"
                                },
                                color = colors.textMuted,
                                fontSize = 11.5.sp
                            )
                        }
                        if (keys.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(auditAccent.copy(alpha = 0.16f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "$averageScore%",
                                    color = auditAccent,
                                    fontSize = 13.sp,
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

        // Search + Filter Chips
        if (keys.isNotEmpty()) {
            item(key = "keys_search_filter") {
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
                                        text = if (isArabic) "ابحث باسم الخدمة، اسم المستخدم أو الملاحظة..."
                                        else "Search service, username, or note...",
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
                                    cursorBrush = SolidColor(AmberNeon),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = if (isArabic) "مسح" else "Clear",
                                        tint = colors.textMuted,
                                        modifier = Modifier.size(16.dp)
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
                            text = if (isArabic) "ضعيفة ومكررة ($weakCount)" else "Weak / Reused ($weakCount)",
                            isSelected = activeFilter == KeysFilter.WEAK_OR_REUSED,
                            onClick = { activeFilter = KeysFilter.WEAK_OR_REUSED }
                        )
                        GlassPill(
                            text = if (isArabic) "قوية ($strongCount)" else "Strong ($strongCount)",
                            isSelected = activeFilter == KeysFilter.STRONG,
                            onClick = { activeFilter = KeysFilter.STRONG }
                        )
                    }
                }
            }
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
                        if (isArabic) "احفظ أسماء المستخدمين وكلمات السر لحساباتك (BaridiMob، ECCP، البريد، التطبيقات) مع تحليل ذكي فوري لقوة كل كلمة سر."
                        else "Store usernames & passwords locally with instant smart strength analysis, reuse detection, and strong password generation."
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
                val isVisible = !isStealth && (revealedMap[entry.id] == true)
                VaultKeyRowCard(
                    entry = entry,
                    analysis = analysis,
                    isVisible = isVisible,
                    isStealth = isStealth,
                    isArabic = isArabic,
                    onToggleReveal = {
                        revealedMap[entry.id] = !(revealedMap[entry.id] ?: false)
                    },
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
                    },
                    onEdit = {
                        editingKey = entry
                        showAddOrEditDialog = true
                    },
                    onDelete = { onDeleteKey(entry.id) }
                )
            }
        }

        item(key = "keys_bottom_spacer") {
            Spacer(modifier = Modifier.height(84.dp))
        }
    }
}

@Composable
private fun AuditStatChip(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
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
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun VaultKeyRowCard(
    entry: VaultKeyEntry,
    analysis: PasswordAnalysis,
    isVisible: Boolean,
    isStealth: Boolean,
    isArabic: Boolean,
    onToggleReveal: () -> Unit,
    onCopyUsername: () -> Unit,
    onCopySecret: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LiquidGlassTheme.colors
    val badgeColor = when (entry.category.uppercase()) {
        "BANK", "BARIDIMOB", "ECCP" -> CyanNeon
        "EMAIL" -> EmeraldNeon
        "PIN" -> AmberNeon
        else -> Color(0xFF818CF8)
    }

    val (strengthColor, strengthLabel) = strengthMeta(analysis.level, isArabic)

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = colors.glassCard,
        borderColor = strengthColor.copy(alpha = 0.38f)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Service Name + Strength Badge + Category + Edit/Delete
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
                            .clip(CircleShape)
                            .background(badgeColor.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.label,
                            color = colors.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(badgeColor.copy(alpha = 0.14f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = entry.category,
                                    color = badgeColor,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(strengthColor.copy(alpha = 0.16f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$strengthLabel ${analysis.score}%",
                                    color = strengthColor,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            if (analysis.isReused) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(RoseNeon.copy(alpha = 0.16f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isArabic) "مكررة" else "REUSED",
                                        color = RoseNeon,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = if (isArabic) "تعديل" else "Edit",
                            tint = CyanNeon,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = if (isArabic) "حذف" else "Delete",
                            tint = RoseNeon,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Username Row (if present) with one-tap copy
            if (entry.username.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.glassSurface)
                        .border(0.8.dp, colors.glassBorderSubtle, RoundedCornerShape(12.dp))
                        .clickable { onCopyUsername() }
                        .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
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
                            tint = CyanNeon,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isStealth) "••••••••" else entry.username,
                            color = colors.textPrimary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = onCopyUsername, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = if (isArabic) "نسخ اسم المستخدم" else "Copy username",
                            tint = CyanNeon,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            // Password / Secret Row with Reveal & Copy
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.glassSurface)
                    .border(0.8.dp, colors.glassBorderSubtle, RoundedCornerShape(12.dp))
                    .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isVisible) entry.secretValue else "•".repeat(entry.secretValue.length.coerceIn(6, 14)),
                    color = colors.textPrimary,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleReveal, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isArabic) "إظهار/إخفاء" else "Reveal/Hide",
                            tint = colors.primaryNeon,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onCopySecret, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = if (isArabic) "نسخ كلمة السر" else "Copy password",
                            tint = EmeraldNeon,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Smart Evaluation Insight Tip (shown clearly when weak/fair/reused or note exists)
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
                            .background(strengthColor.copy(alpha = 0.10f))
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
                    color = colors.textMuted,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun AddOrEditVaultKeyDialog(
    existing: VaultKeyEntry?,
    secureScreenshots: Boolean,
    onDismiss: () -> Unit,
    onSave: (label: String, username: String, secret: String, category: String, note: String) -> Unit
) {
    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors
    val isArabic = strings is ArabicStrings

    var label by remember(existing) { mutableStateOf(existing?.label ?: "") }
    var username by remember(existing) { mutableStateOf(existing?.username ?: "") }
    var secret by remember(existing) { mutableStateOf(existing?.secretValue ?: "") }
    var category by remember(existing) { mutableStateOf(existing?.category ?: "APP") }
    var note by remember(existing) { mutableStateOf(existing?.note ?: "") }
    var showPasswordInDialog by remember { mutableStateOf(true) }

    val liveAnalysis = remember(secret, username, label) {
        PasswordStrengthAnalyzer.analyze(
            password = secret,
            username = username,
            label = label,
            duplicateCount = 1
        )
    }
    val (strengthColor, strengthLabel) = strengthMeta(liveAnalysis.level, isArabic)

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
                        if (isArabic) "إضافة حساب وكلمة سر" else "Add Account & Password"
                    } else {
                        if (isArabic) "تعديل الحساب" else "Edit Account"
                    },
                    color = colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("APP", "BANK", "EMAIL", "PIN").forEach { cat ->
                        GlassPill(
                            text = cat,
                            isSelected = category == cat,
                            onClick = { category = cat }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                KeyFieldBox(
                    label = if (isArabic) "اسم التطبيق / الخدمة (مثال: BaridiMob)" else "Account / Service Name",
                    value = label,
                    onValueChange = { label = it }
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
                        .border(1.dp, strengthColor.copy(alpha = if (secret.isEmpty()) 0.25f else 0.55f), RoundedCornerShape(14.dp))
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
                                text = "${liveAnalysis.score}%",
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
                        containerColor = AmberNeon,
                        contentColor = Color(0xFF0F172A),
                        modifier = Modifier.weight(1.2f),
                        onClick = { onSave(label, username, secret, category, note) }
                    )
                }
            }
        }
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
