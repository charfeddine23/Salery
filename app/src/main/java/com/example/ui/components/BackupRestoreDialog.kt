package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import com.example.security.StepUpAuthHelper
import com.example.security.VaultCryptoManager
import com.example.ui.i18n.APP_NAME
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.ArabicStrings
import com.example.ui.i18n.formatDate
import com.example.ui.i18n.isolateLtr
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.LocalScreenshotProtection
import com.example.ui.theme.RoseNeon
import com.example.viewmodel.BackupRestoreOutcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Clean, explicit, user-triggered Google Drive & Local Storage Backup and Restore dialog.
 * Supports optional AES-256-GCM passphrase encryption & automatic encrypted envelope decryption.
 */
@Composable
fun BackupRestoreDialog(
    lastBackupTimestamp: Long,
    requireStepUpAuth: Boolean = false,
    onBeforeExternalFlow: () -> Unit = {},
    onDismiss: () -> Unit,
    onExportBackup: suspend (passphrase: String) -> String,
    onRestoreBackup: suspend (rawContent: String, passphrase: String) -> BackupRestoreOutcome
) {
    val strings = AppStrings.current
    val isArabic = strings is ArabicStrings
    val colors = LiquidGlassTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isErrorMessage by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showManualPaste by remember { mutableStateOf(false) }
    var manualJsonText by remember { mutableStateOf("") }
    var pendingExportJson by remember { mutableStateOf<String?>(null) }

    // Optional AES-256 backup encryption state
    var encryptBackupWithPassword by remember { mutableStateOf(false) }
    var exportPassphrase by remember { mutableStateOf("") }
    var showExportPassphrase by remember { mutableStateOf(false) }

    // Encrypted backup restore prompt state
    var pendingEncryptedRestoreContent by remember { mutableStateOf<String?>(null) }
    var restorePassphrase by remember { mutableStateOf("") }
    var showRestorePassphrase by remember { mutableStateOf(false) }

    fun handleRestoreInput(rawContent: String, passphrase: String = "") {
        val trimmed = rawContent.trim()
        if (trimmed.isBlank()) {
            statusMessage = strings.restoreError
            isErrorMessage = true
            return
        }
        if (VaultCryptoManager.isEncryptedBackupEnvelope(trimmed) && passphrase.isBlank()) {
            pendingEncryptedRestoreContent = trimmed
            restorePassphrase = ""
            statusMessage = if (isArabic) {
                "هذه النسخة مشفّرة. أدخل كلمة المرور لفك التشفير والاستعادة."
            } else {
                "This backup is encrypted. Enter its password to decrypt & restore."
            }
            isErrorMessage = false
            return
        }

        isLoading = true
        scope.launch {
            try {
                when (onRestoreBackup(trimmed, passphrase)) {
                    BackupRestoreOutcome.SUCCESS -> {
                        statusMessage = strings.restoreSuccess
                        isErrorMessage = false
                        pendingEncryptedRestoreContent = null
                        restorePassphrase = ""
                        manualJsonText = ""
                        showManualPaste = false
                    }
                    BackupRestoreOutcome.WRONG_PASSWORD_OR_TAMPERED -> {
                        statusMessage = if (isArabic) {
                            "كلمة مرور النسخة الاحتياطية غير صحيحة أو الملف معدّل."
                        } else {
                            "Incorrect backup password or file integrity check failed."
                        }
                        isErrorMessage = true
                    }
                    BackupRestoreOutcome.INVALID_FORMAT -> {
                        statusMessage = strings.restoreError
                        isErrorMessage = true
                        pendingEncryptedRestoreContent = null
                    }
                }
            } catch (e: Exception) {
                statusMessage = "${strings.restoreError}: ${e.localizedMessage ?: "Failed"}"
                isErrorMessage = true
            } finally {
                isLoading = false
            }
        }
    }

    // 1. SAF Create Document Launcher
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null && pendingExportJson != null) {
            isLoading = true
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { output ->
                            output.write(pendingExportJson!!.toByteArray(Charsets.UTF_8))
                        }
                    }
                    statusMessage = if (encryptBackupWithPassword) {
                        if (isArabic) "تم حفظ النسخة المشفرة بأمان ✓"
                        else "Encrypted backup saved securely (AES-256) ✓"
                    } else {
                        strings.backupSuccess
                    }
                    isErrorMessage = false
                } catch (e: Exception) {
                    statusMessage = if (isArabic) {
                        "خطأ في الكتابة: ${e.localizedMessage ?: "تعذر الحفظ"}"
                    } else {
                        "Write error: ${e.localizedMessage ?: "Failed to save"}"
                    }
                    isErrorMessage = true
                } finally {
                    isLoading = false
                    pendingExportJson = null
                }
            }
        } else {
            isLoading = false
            pendingExportJson = null
        }
    }

    // 2. SAF Open Document Launcher
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            isLoading = true
            scope.launch {
                try {
                    val content = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            stream.bufferedReader().use { it.readText() }
                        }
                    }
                    isLoading = false
                    if (!content.isNullOrBlank()) {
                        handleRestoreInput(content)
                    } else {
                        statusMessage = strings.restoreError
                        isErrorMessage = true
                    }
                } catch (e: Exception) {
                    isLoading = false
                    statusMessage = "${strings.restoreError}: ${e.localizedMessage ?: "Failed to read"}"
                    isErrorMessage = true
                }
            }
        }
    }

    fun executeVerifiedExport(shareMode: Boolean) {
        if (encryptBackupWithPassword && exportPassphrase.length < 4) {
            statusMessage = if (isArabic) {
                "أدخل كلمة مرور من 4 أحرف على الأقل لتشفير النسخة."
            } else {
                "Enter at least a 4-character password to encrypt this backup."
            }
            isErrorMessage = true
            return
        }

        StepUpAuthHelper.verifyIfEnabled(
            context = context,
            requireBiometric = requireStepUpAuth,
            title = if (isArabic) "تأكيد الهوية لتصدير النسخة" else "Verify Identity to Export",
            subtitle = if (isArabic) "تحقق بالبصمة قبل تصدير بياناتك المالية" else "Confirm biometrics before exporting your vault",
            cancelText = strings.cancel,
            onVerified = {
                isLoading = true
                scope.launch {
                    try {
                        val activePass = if (encryptBackupWithPassword) exportPassphrase else ""
                        val exported = onExportBackup(activePass)
                        if (shareMode) {
                            isLoading = false
                            onBeforeExternalFlow()
                            val suffix = if (encryptBackupWithPassword) "Encrypted.json" else "Backup.json"
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, exported)
                                putExtra(
                                    Intent.EXTRA_SUBJECT,
                                    if (isArabic) {
                                        "نسخة احتياطية $APP_NAME (${formatDate(System.currentTimeMillis(), true)})"
                                    } else {
                                        "$APP_NAME Backup (${formatDate(System.currentTimeMillis(), false)})"
                                    }
                                )
                                putExtra(Intent.EXTRA_TITLE, "Vaultix_$suffix")
                                type = "application/json"
                            }
                            context.startActivity(
                                Intent.createChooser(
                                    sendIntent,
                                    if (isArabic) "مشاركة النسخة عبر جوجل درايف / التطبيقات" else "Share Backup to Google Drive / Apps"
                                )
                            )
                        } else {
                            pendingExportJson = exported
                            onBeforeExternalFlow()
                            val filename = if (encryptBackupWithPassword) {
                                "Vaultix_Encrypted_Backup.json"
                            } else {
                                "Vaultix_Backup.json"
                            }
                            createDocumentLauncher.launch(filename)
                        }
                    } catch (e: Exception) {
                        isLoading = false
                        statusMessage = if (isArabic) {
                            "خطأ في النسخ الاحتياطي: ${e.localizedMessage ?: "خطأ غير معروف"}"
                        } else {
                            "Backup error: ${e.localizedMessage ?: "Unknown"}"
                        }
                        isErrorMessage = true
                    }
                }
            },
            onRejected = { err ->
                if (!err.isNullOrBlank()) {
                    statusMessage = err
                    isErrorMessage = true
                }
            }
        )
    }

    val isScreenshotProtected = LocalScreenshotProtection.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            securePolicy = if (isScreenshotProtected) SecureFlagPolicy.SecureOn else SecureFlagPolicy.SecureOff
        )
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr
        ) {
        GlassCard(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            shape = RoundedCornerShape(26.dp),
            backgroundColor = colors.dialogBackground
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header with SP Brand Accent
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.primaryNeon.copy(alpha = 0.16f))
                                .border(1.dp, colors.primaryNeon.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cloud,
                                contentDescription = null,
                                tint = colors.primaryNeon,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = strings.backupDriveTitle,
                                color = colors.textPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isArabic) "بيانات $APP_NAME السحابية والمحلية" else "$APP_NAME Cloud & Local Data",
                                color = colors.textMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = strings.cancel,
                            tint = colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Last Backup Status Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.glassCard)
                        .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (lastBackupTimestamp > 0) EmeraldNeon else colors.textMuted)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        val lastSyncStr = if (lastBackupTimestamp > 0) {
                            String.format(strings.lastBackupLabel, isolateLtr(formatDate(lastBackupTimestamp, isArabic)))
                        } else {
                            strings.neverBackedUp
                        }
                        Text(
                            text = lastSyncStr,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = strings.backupInfo,
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Optional AES-256 Password Encryption Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (encryptBackupWithPassword) EmeraldNeon.copy(alpha = 0.08f) else colors.glassCard
                        )
                        .border(
                            1.dp,
                            if (encryptBackupWithPassword) EmeraldNeon.copy(alpha = 0.45f) else colors.glassBorderSubtle,
                            RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { encryptBackupWithPassword = !encryptBackupWithPassword },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.EnhancedEncryption,
                            contentDescription = null,
                            tint = if (encryptBackupWithPassword) EmeraldNeon else colors.textMuted,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isArabic) "تشفير النسخة الاحتياطية بكلمة مرور" else "Encrypt backup with password",
                                color = colors.textPrimary,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isArabic) {
                                    "يحفظ جميع البيانات وتفاصيل البطاقة مشفرة بالكامل"
                                } else {
                                    "Protects full records & card details via PBKDF2 + AES-256"
                                },
                                color = colors.textMuted,
                                fontSize = 10.5.sp
                            )
                        }
                        Switch(
                            checked = encryptBackupWithPassword,
                            onCheckedChange = { encryptBackupWithPassword = it },
                            modifier = Modifier.testTag("encrypt_backup_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = EmeraldNeon,
                                checkedTrackColor = EmeraldNeon.copy(alpha = 0.35f),
                                uncheckedThumbColor = colors.textMuted,
                                uncheckedTrackColor = colors.glassCard
                            )
                        )
                    }

                    AnimatedVisibility(visible = encryptBackupWithPassword) {
                        OutlinedTextField(
                            value = exportPassphrase,
                            onValueChange = { exportPassphrase = it },
                            singleLine = true,
                            label = {
                                Text(
                                    text = if (isArabic) "كلمة مرور التشفير" else "Encryption password",
                                    fontSize = 12.sp
                                )
                            },
                            visualTransformation = if (showExportPassphrase) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showExportPassphrase = !showExportPassphrase }) {
                                    Icon(
                                        imageVector = if (showExportPassphrase) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = colors.textSecondary
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .testTag("export_passphrase_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary,
                                focusedBorderColor = EmeraldNeon,
                                unfocusedBorderColor = colors.glassBorderSubtle
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Explicit Manual Action 1: Backup to Google Drive (SAF Document Creator)
                GlassButton(
                    text = strings.backupToDrive,
                    icon = Icons.Default.CloudUpload,
                    containerColor = colors.primaryNeon,
                    contentColor = if (colors.isDark) Color(0xFF0F172A) else Color.White,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    onClick = { executeVerifiedExport(shareMode = false) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Explicit Manual Action 2: Restore from Google Drive
                GlassButton(
                    text = strings.restoreFromDrive,
                    icon = Icons.Default.CloudDownload,
                    containerColor = colors.glassCard,
                    contentColor = colors.textPrimary,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    onClick = {
                        onBeforeExternalFlow()
                        openDocumentLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                    }
                )

                // Encrypted Restore Passphrase Prompt (shown automatically if selected/pasted file is encrypted)
                AnimatedVisibility(visible = pendingEncryptedRestoreContent != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(AmberNeon.copy(alpha = 0.09f))
                            .border(1.dp, AmberNeon.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = AmberNeon,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isArabic) "مطلوب كلمة مرور فك التشفير" else "Decryption Password Required",
                                color = colors.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = restorePassphrase,
                            onValueChange = { restorePassphrase = it },
                            singleLine = true,
                            placeholder = {
                                Text(
                                    text = if (isArabic) "أدخل كلمة مرور النسخة المشفرة..." else "Enter backup password...",
                                    fontSize = 12.sp
                                )
                            },
                            visualTransformation = if (showRestorePassphrase) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showRestorePassphrase = !showRestorePassphrase }) {
                                    Icon(
                                        imageVector = if (showRestorePassphrase) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = colors.textSecondary
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("restore_passphrase_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary,
                                focusedBorderColor = AmberNeon,
                                unfocusedBorderColor = colors.glassBorderSubtle
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        GlassButton(
                            text = if (isArabic) "فك التشفير والاستعادة" else "Decrypt & Restore",
                            icon = Icons.Default.LockOpen,
                            containerColor = AmberNeon,
                            contentColor = Color(0xFF0F172A),
                            enabled = restorePassphrase.isNotBlank() && !isLoading,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                pendingEncryptedRestoreContent?.let { raw ->
                                    handleRestoreInput(raw, restorePassphrase)
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Share Intent Action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = !isLoading) { executeVerifiedExport(shareMode = true) }
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isArabic) "أو مشاركة النسخة عبر جوجل درايف / التطبيقات"
                        else "Or share backup via Google Drive app",
                        color = colors.textSecondary,
                        fontSize = 11.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (isLoading) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = colors.primaryNeon,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isArabic) "جاري المعالجة..." else "Processing...",
                            color = colors.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                if (statusMessage != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isErrorMessage) RoseNeon.copy(alpha = 0.12f)
                                else EmeraldNeon.copy(alpha = 0.12f)
                            )
                            .border(
                                1.dp,
                                if (isErrorMessage) RoseNeon.copy(alpha = 0.35f)
                                else EmeraldNeon.copy(alpha = 0.35f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isErrorMessage) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isErrorMessage) RoseNeon else EmeraldNeon,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = statusMessage!!,
                            color = if (isErrorMessage) RoseNeon else EmeraldNeon,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Collapsible Manual JSON Paste Fallback
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showManualPaste = !showManualPaste }
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = null,
                            tint = colors.textMuted,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.pasteJsonFallback,
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Icon(
                        imageVector = if (showManualPaste) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                AnimatedVisibility(
                    visible = showManualPaste,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        OutlinedTextField(
                            value = manualJsonText,
                            onValueChange = { manualJsonText = it },
                            placeholder = {
                                Text(
                                    text = "{\"exportTimestamp\": ...}",
                                    color = colors.textMuted,
                                    fontSize = 11.sp
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary,
                                focusedBorderColor = colors.primaryNeon,
                                unfocusedBorderColor = colors.glassBorderSubtle
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        GlassButton(
                            text = strings.restoreBackup,
                            icon = Icons.Default.FileDownload,
                            containerColor = colors.primaryNeon,
                            contentColor = if (colors.isDark) Color(0xFF0F172A) else Color.White,
                            enabled = manualJsonText.isNotBlank() && !isLoading,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { handleRestoreInput(manualJsonText) }
                        )
                    }
                }
            }
        }
        }
    }
}
