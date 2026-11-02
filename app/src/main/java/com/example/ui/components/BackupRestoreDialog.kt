package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.formatDate
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassTheme
import kotlinx.coroutines.launch

@Composable
fun BackupRestoreDialog(
    lastBackupTimestamp: Long,
    onDismiss: () -> Unit,
    onExportBackup: suspend () -> String,
    onRestoreBackup: suspend (String) -> Boolean,
    onSyncGoogleDrive: () -> Unit
) {
    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var restoreJsonText by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isRestoreMode by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(26.dp),
            backgroundColor = colors.dialogBackground
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = colors.primaryNeon,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = strings.backupDriveTitle,
                            color = colors.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
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

                Spacer(modifier = Modifier.height(14.dp))

                // Cloud Status Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.glassCard)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = EmeraldNeon,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    Column {
                        Text(
                            text = strings.googleDriveSynced,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        val lastSyncStr = if (lastBackupTimestamp > 0) {
                            "Last sync: ${formatDate(lastBackupTimestamp)}"
                        } else {
                            "Ready to sync"
                        }
                        Text(
                            text = lastSyncStr,
                            color = colors.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Google Drive Sync Button
                GlassButton(
                    text = strings.syncWithDrive,
                    icon = Icons.Default.CloudSync,
                    containerColor = colors.primaryNeon,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onSyncGoogleDrive()
                        statusMessage = "Google Drive sync initiated!"
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Export Button (Share/Drive Intent)
                GlassButton(
                    text = strings.exportBackup,
                    icon = Icons.Default.FileUpload,
                    containerColor = EmeraldNeon,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            val json = onExportBackup()
                            // Copy to clipboard
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("SaleryPelse Backup", json))

                            // Share to Google Drive or other apps
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, json)
                                putExtra(Intent.EXTRA_TITLE, "SaleryPelse_Backup.json")
                                type = "application/json"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Save Backup to Google Drive / Files")
                            context.startActivity(shareIntent)
                            statusMessage = strings.backupSuccess
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Restore Section Toggle
                if (!isRestoreMode) {
                    GlassButton(
                        text = strings.restoreBackup,
                        icon = Icons.Default.FileDownload,
                        containerColor = colors.glassCard,
                        contentColor = colors.textPrimary,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { isRestoreMode = true }
                    )
                } else {
                    OutlinedTextField(
                        value = restoreJsonText,
                        onValueChange = { restoreJsonText = it },
                        label = { Text("Paste Backup JSON") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        shape = RoundedCornerShape(16.dp),
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
                        containerColor = colors.primaryNeon,
                        enabled = restoreJsonText.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            scope.launch {
                                val success = onRestoreBackup(restoreJsonText.trim())
                                if (success) {
                                    statusMessage = strings.restoreSuccess
                                    isRestoreMode = false
                                } else {
                                    statusMessage = strings.restoreError
                                }
                            }
                        }
                    )
                }

                if (statusMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = statusMessage!!,
                        color = EmeraldNeon,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
