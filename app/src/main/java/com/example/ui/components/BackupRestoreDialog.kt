package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.formatDate
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.RoseNeon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Clean, explicit, user-triggered Google Drive & Local Storage Backup and Restore dialog.
 * Uses Android Storage Access Framework (SAF) contracts:
 * - CreateDocument for direct Google Drive / Storage saving
 * - OpenDocument for picking JSON files directly from Google Drive or local storage
 */
@Composable
fun BackupRestoreDialog(
    lastBackupTimestamp: Long,
    onDismiss: () -> Unit,
    onExportBackup: suspend () -> String,
    onRestoreBackup: suspend (String) -> Boolean,
    onSyncGoogleDrive: (() -> Unit)? = null
) {
    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isErrorMessage by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showManualPaste by remember { mutableStateOf(false) }
    var manualJsonText by remember { mutableStateOf("") }
    var pendingExportJson by remember { mutableStateOf<String?>(null) }

    // 1. SAF Create Document Launcher: Save backup directly into Google Drive folder or local storage
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
                    statusMessage = strings.backupSuccess
                    isErrorMessage = false
                } catch (e: Exception) {
                    statusMessage = "Write error: ${e.localizedMessage ?: "Failed to save"}"
                    isErrorMessage = true
                } finally {
                    isLoading = false
                    pendingExportJson = null
                }
            }
        } else {
            pendingExportJson = null
        }
    }

    // 2. SAF Open Document Launcher: Pick JSON backup file directly from Google Drive or local files
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            isLoading = true
            scope.launch {
                try {
                    val jsonContent = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            stream.bufferedReader().use { it.readText() }
                        }
                    }
                    if (!jsonContent.isNullOrBlank()) {
                        val success = onRestoreBackup(jsonContent.trim())
                        if (success) {
                            statusMessage = strings.restoreSuccess
                            isErrorMessage = false
                        } else {
                            statusMessage = strings.restoreError
                            isErrorMessage = true
                        }
                    } else {
                        statusMessage = strings.restoreError
                        isErrorMessage = true
                    }
                } catch (e: Exception) {
                    statusMessage = "${strings.restoreError}: ${e.localizedMessage ?: "Failed to read"}"
                    isErrorMessage = true
                } finally {
                    isLoading = false
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
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
                                text = "SaleryPulse Cloud & Local Data",
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

                Spacer(modifier = Modifier.height(18.dp))

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
                            String.format(strings.lastBackupLabel, formatDate(lastBackupTimestamp))
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

                Spacer(modifier = Modifier.height(18.dp))

                // Explicit Manual Action 1: Backup to Google Drive (SAF Document Creator)
                GlassButton(
                    text = strings.backupToDrive,
                    icon = Icons.Default.CloudUpload,
                    containerColor = colors.primaryNeon,
                    contentColor = if (colors.isDark) Color(0xFF0F172A) else Color.White,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        isLoading = true
                        scope.launch {
                            try {
                                val json = onExportBackup()
                                pendingExportJson = json

                                // Copy to clipboard as quick safety backup
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("SaleryPulse Backup", json))

                                // Launch Storage Access Framework Document Creator: user selects Google Drive folder or device storage
                                createDocumentLauncher.launch("SaleryPulse_Backup.json")
                            } catch (e: Exception) {
                                statusMessage = "Backup error: ${e.localizedMessage ?: "Unknown"}"
                                isErrorMessage = true
                                isLoading = false
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Explicit Manual Action 2: Restore from Google Drive (SAF Document Picker filtered for JSON)
                GlassButton(
                    text = strings.restoreFromDrive,
                    icon = Icons.Default.CloudDownload,
                    containerColor = colors.glassCard,
                    contentColor = colors.textPrimary,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        // Open system document picker filtered for JSON documents
                        openDocumentLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Share Intent Action (Direct share to Google Drive app or email)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            scope.launch {
                                try {
                                    val json = onExportBackup()
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, json)
                                        putExtra(Intent.EXTRA_SUBJECT, "SaleryPulse Backup (${formatDate(System.currentTimeMillis())})")
                                        putExtra(Intent.EXTRA_TITLE, "SaleryPulse_Backup.json")
                                        type = "application/json"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Share Backup to Google Drive / Apps")
                                    context.startActivity(shareIntent)
                                } catch (e: Exception) {
                                    statusMessage = e.localizedMessage
                                    isErrorMessage = true
                                }
                            }
                        }
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
                        text = "Or share backup via Google Drive app",
                        color = colors.textSecondary,
                        fontSize = 11.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Loading Indicator
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
                            text = "Processing...",
                            color = colors.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                // Status / Error message banner
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
                            onClick = {
                                isLoading = true
                                scope.launch {
                                    val success = onRestoreBackup(manualJsonText.trim())
                                    if (success) {
                                        statusMessage = strings.restoreSuccess
                                        isErrorMessage = false
                                        manualJsonText = ""
                                        showManualPaste = false
                                    } else {
                                        statusMessage = strings.restoreError
                                        isErrorMessage = true
                                    }
                                    isLoading = false
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
