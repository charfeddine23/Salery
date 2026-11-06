package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.RoseNeon

import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.security.BiometricAuthManager

@Composable
fun PatternLockDialog(
    isCurrentlyEnabled: Boolean,
    isBiometricEnabled: Boolean = false,
    onDismiss: () -> Unit,
    onSavePattern: (List<Int>) -> Unit,
    onDisablePattern: () -> Unit,
    onToggleBiometric: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors
    val isBiometricAvailable = remember { BiometricAuthManager.isBiometricAvailable(context) }

    var firstPattern by remember { mutableStateOf<List<Int>?>(null) }
    var patternState by remember { mutableStateOf(PatternState.INPUT) }
    var instructionText by remember { mutableStateOf(strings.drawPattern) }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(26.dp),
            backgroundColor = colors.dialogBackground
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = colors.primaryNeon,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = strings.patternLock,
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

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = instructionText,
                    color = when (patternState) {
                        PatternState.INPUT -> colors.textSecondary
                        PatternState.SUCCESS -> EmeraldNeon
                        PatternState.ERROR -> RoseNeon
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                PatternLockView(
                    modifier = Modifier.size(260.dp),
                    patternState = patternState,
                    onPatternComplete = { pattern ->
                        if (pattern.size < 4) {
                            patternState = PatternState.ERROR
                            instructionText = strings.patternTooShort
                            return@PatternLockView
                        }

                        if (firstPattern == null) {
                            // Step 1: Record first pattern and ask to confirm
                            firstPattern = pattern
                            patternState = PatternState.INPUT
                            instructionText = strings.confirmPattern
                        } else {
                            // Step 2: Compare with first pattern
                            if (pattern == firstPattern) {
                                patternState = PatternState.SUCCESS
                                instructionText = strings.patternSaved
                                onSavePattern(pattern)
                                onDismiss()
                            } else {
                                patternState = PatternState.ERROR
                                instructionText = strings.patternMismatch
                                firstPattern = null
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Biometric Unlock Option (Fingerprint / Face ID)
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = colors.glassCard
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = if (isBiometricAvailable) colors.primaryNeon else colors.textMuted,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.size(10.dp))
                            Column {
                                Text(
                                    text = strings.biometricUnlock,
                                    color = colors.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isBiometricAvailable) strings.biometricUnlockDesc else strings.biometricNotAvailable,
                                    color = colors.textMuted,
                                    fontSize = 10.5.sp,
                                    lineHeight = 13.sp
                                )
                            }
                        }
                        Switch(
                            checked = isBiometricEnabled && isBiometricAvailable,
                            onCheckedChange = { onToggleBiometric(it) },
                            enabled = isBiometricAvailable,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = colors.primaryNeon,
                                uncheckedThumbColor = colors.textSecondary,
                                uncheckedTrackColor = colors.glassSurface
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (isCurrentlyEnabled) {
                    GlassButton(
                        text = "Turn Off Pattern Lock",
                        icon = Icons.Default.LockOpen,
                        containerColor = RoseNeon.copy(alpha = 0.8f),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            onDisablePattern()
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}
