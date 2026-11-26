package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cloud.CloudSyncUiState
import com.example.cloud.GoogleAuthUiState
import com.example.data.UserSettings
import com.example.ui.components.GlassCard
import com.example.ui.components.LiquidLanguageSegmentedControl
import com.example.ui.components.LiquidThemeSegmentedControl
import com.example.ui.components.edgeFadeVerticalContentPadding
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.ArabicStrings
import com.example.ui.i18n.formatDate
import com.example.ui.i18n.isolateLtr
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.LocalAdaptiveDimens
import com.example.ui.theme.LocalAdaptiveTypography
import com.example.ui.theme.RoseNeon
import com.example.ui.theme.ThemeEngineCatalog

@Composable
fun AccountSettingsScreen(
    userSettings: UserSettings,
    googleAuthState: GoogleAuthUiState,
    cloudSyncState: CloudSyncUiState,
    lastCloudSyncMillis: Long,
    onGoogleSignIn: () -> Unit,
    onGoogleSignOut: () -> Unit,
    onCloudSyncNow: () -> Unit,
    onRestoreFromCloud: () -> Unit,
    onOpenBackupRestore: () -> Unit,
    onSignOutToAuthScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current
    val isArabic = strings is ArabicStrings
    val colors = LiquidGlassTheme.colors
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current

    val fullName = listOf(userSettings.firstName.trim(), userSettings.lastName.trim())
        .filter { it.isNotEmpty() }
        .joinToString(" ")
        .ifBlank { userSettings.localUsername.trim() }
    val signedIn = googleAuthState as? GoogleAuthUiState.SignedIn

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = dimens.screenPaddingH),
        contentPadding = edgeFadeVerticalContentPadding(
            extraTop = 8.dp,
            extraBottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Profile summary header
        item(key = "account_profile_header") {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = colors.glassSurface,
                shape = RoundedCornerShape(22.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.primaryNeon.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = colors.primaryNeon,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = fullName.ifBlank {
                                signedIn?.displayName ?: if (isArabic) "حساب Vaultix" else "Vaultix Account"
                            },
                            color = colors.textPrimary,
                            fontSize = typo.titleSm,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val subtitle = signedIn?.email?.takeIf { it.isNotBlank() }
                            ?: userSettings.localUsername.takeIf { it.isNotBlank() }?.let { "@$it" }
                            ?: if (isArabic) "حساب محلي نشط" else "Local vault profile active"
                        Text(
                            text = subtitle,
                            color = colors.textMuted,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // 1. Account Sync (مزامنة الحساب)
        item(key = "account_cloud_sync") {
            AccountCloudSyncCard(
                isArabic = isArabic,
                googleAuthState = googleAuthState,
                cloudSyncState = cloudSyncState,
                lastCloudSyncMillis = lastCloudSyncMillis,
                onGoogleSignIn = onGoogleSignIn,
                onCloudSyncNow = onCloudSyncNow,
                onRestoreFromCloud = onRestoreFromCloud
            )
        }

        // 2. Google Drive Backups
        item(key = "account_google_drive_backups") {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_google_drive_backups_button"),
                backgroundColor = colors.glassCard,
                borderColor = colors.primaryNeon.copy(alpha = 0.42f),
                shape = RoundedCornerShape(20.dp),
                onClick = onOpenBackupRestore
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.primaryNeon.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = colors.primaryNeon,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.backupDriveTitle,
                            color = colors.textPrimary,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isArabic) "حفظ واستعادة النسخ الاحتياطية المشفرة" else "Export & restore encrypted backups",
                            color = colors.textMuted,
                            fontSize = 11.5.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 3. Sign Out Controls (زر تسجيل الخروج)
        item(key = "account_sign_out_section") {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = colors.glassCard,
                borderColor = RoseNeon.copy(alpha = 0.35f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (signedIn != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(14.dp))
                                .background(RoseNeon.copy(alpha = 0.12f))
                                .border(1.dp, RoseNeon.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                .clickable(onClick = onGoogleSignOut)
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                .testTag("drawer_google_signout_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = null,
                                    tint = RoseNeon,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isArabic) "تسجيل الخروج من الحساب السحابي" else "Sign out of Cloud Account",
                                    color = RoseNeon,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(14.dp))
                            .background(EmeraldNeon.copy(alpha = 0.12f))
                            .border(1.dp, EmeraldNeon.copy(alpha = 0.32f), RoundedCornerShape(14.dp))
                            .clickable(onClick = onSignOutToAuthScreen)
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .testTag("open_auth_screen_drawer_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = null,
                                tint = EmeraldNeon,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isArabic) "تسجيل الخروج إلى شاشة الدخول" else "Sign Out / Switch Account",
                                color = EmeraldNeon,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountCloudSyncCard(
    isArabic: Boolean,
    googleAuthState: GoogleAuthUiState,
    cloudSyncState: CloudSyncUiState,
    lastCloudSyncMillis: Long,
    onGoogleSignIn: () -> Unit,
    onCloudSyncNow: () -> Unit,
    onRestoreFromCloud: () -> Unit
) {
    val colors = LiquidGlassTheme.colors
    val signedIn = googleAuthState as? GoogleAuthUiState.SignedIn
    val isAuthLoading = googleAuthState is GoogleAuthUiState.Loading
    val authError = (googleAuthState as? GoogleAuthUiState.Error)?.message
    val isSyncing = cloudSyncState is CloudSyncUiState.Syncing

    val accentColor = when {
        signedIn != null && cloudSyncState is CloudSyncUiState.Error -> RoseNeon
        signedIn != null -> EmeraldNeon
        authError != null -> RoseNeon
        else -> colors.primaryNeon
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cloud_sync_drawer_card"),
        backgroundColor = colors.glassCard,
        borderColor = accentColor.copy(alpha = 0.45f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isAuthLoading || isSyncing) {
                        CircularProgressIndicator(
                            color = accentColor,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = when {
                                signedIn != null -> Icons.Default.CloudSync
                                authError != null -> Icons.Default.CloudOff
                                else -> Icons.Default.CloudQueue
                            },
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isArabic) "مزامنة الحساب السحابية" else "Account Cloud Sync",
                        color = colors.textPrimary,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val subtitle = when {
                        isAuthLoading -> if (isArabic) "جاري تسجيل الدخول..." else "Signing in..."
                        signedIn != null -> signedIn.email.ifBlank {
                            if (isArabic) "متصل بـ Firebase" else "Connected to Firebase"
                        }
                        authError != null -> authError
                        else -> if (isArabic) {
                            "سجّل الدخول لمزامنة كل البيانات والإعدادات تلقائياً"
                        } else {
                            "Sign in to sync all data & settings automatically"
                        }
                    }
                    Text(
                        text = subtitle,
                        color = if (authError != null && signedIn == null) RoseNeon else colors.textMuted,
                        fontSize = 11.5.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (signedIn != null) {
                Spacer(modifier = Modifier.height(12.dp))
                val statusText = when (cloudSyncState) {
                    is CloudSyncUiState.Syncing -> if (isArabic) "جاري المزامنة مع Firestore..." else "Syncing with Firestore..."
                    is CloudSyncUiState.Success -> if (isArabic) {
                        "متزامن • ${cloudSyncState.transactionCount} معاملة • ${cloudSyncState.debtCount} ديون"
                    } else {
                        "Synced • ${cloudSyncState.transactionCount} tx • ${cloudSyncState.debtCount} debts"
                    }
                    is CloudSyncUiState.Error -> cloudSyncState.message
                    is CloudSyncUiState.Idle -> if (lastCloudSyncMillis > 0L) {
                        val dateLabel = isolateLtr(formatDate(lastCloudSyncMillis, isArabic))
                        if (isArabic) "آخر مزامنة: $dateLabel" else "Last sync: $dateLabel"
                    } else {
                        if (isArabic) "المخزن المحلي جاهز للمزامنة الفورية" else "Offline-first vault ready to sync"
                    }
                }
                val statusColor = when (cloudSyncState) {
                    is CloudSyncUiState.Error -> RoseNeon
                    is CloudSyncUiState.Success -> EmeraldNeon
                    is CloudSyncUiState.Syncing -> colors.primaryNeon
                    is CloudSyncUiState.Idle -> colors.textSecondary
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.glassSurface.copy(alpha = 0.72f))
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (cloudSyncState is CloudSyncUiState.Error) Icons.Default.ErrorOutline else Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.primaryNeon.copy(alpha = 0.18f))
                            .border(1.dp, colors.primaryNeon.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                            .clickable(enabled = !isSyncing, onClick = onCloudSyncNow)
                            .padding(vertical = 11.dp, horizontal = 10.dp)
                            .testTag("drawer_cloud_sync_now_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = colors.primaryNeon,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isArabic) "مزامنة الآن" else "Sync Now",
                                color = colors.primaryNeon,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(12.dp))
                            .background(EmeraldNeon.copy(alpha = 0.14f))
                            .border(1.dp, EmeraldNeon.copy(alpha = 0.40f), RoundedCornerShape(12.dp))
                            .clickable(enabled = !isSyncing, onClick = onRestoreFromCloud)
                            .padding(vertical = 11.dp, horizontal = 10.dp)
                            .testTag("drawer_cloud_restore_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                tint = EmeraldNeon,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isArabic) "استعادة" else "Restore",
                                color = EmeraldNeon,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.primaryNeon.copy(alpha = 0.18f))
                        .border(1.dp, colors.primaryNeon.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                        .clickable(enabled = !isAuthLoading, onClick = onGoogleSignIn)
                        .padding(vertical = 12.dp, horizontal = 12.dp)
                        .testTag("drawer_google_signin_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = colors.primaryNeon,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isAuthLoading) {
                                if (isArabic) "جاري الاتصال..." else "Connecting..."
                            } else if (isArabic) {
                                "ربط حساب Google للمزامنة"
                            } else {
                                "Sign in with Google to Sync"
                            },
                            color = colors.primaryNeon,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AuthorisationSettingsScreen(
    onOpenAccessPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current
    val isArabic = strings is ArabicStrings
    val colors = LiquidGlassTheme.colors
    val dimens = LocalAdaptiveDimens.current
    val context = LocalContext.current
    val permSummary = readAppPermissionsSummary(context)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = dimens.screenPaddingH),
        contentPadding = edgeFadeVerticalContentPadding(
            extraTop = 8.dp,
            extraBottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "authorisation_access_permissions_card") {
            val statusColor = if (permSummary.allEssentialGranted) EmeraldNeon else AmberNeon
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("access_permissions_setting_row"),
                backgroundColor = colors.glassCard,
                borderColor = statusColor.copy(alpha = 0.45f),
                shape = RoundedCornerShape(22.dp),
                onClick = onOpenAccessPermissions
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(statusColor.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(23.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isArabic) "صلاحيات الوصول (Access Permissions)" else "Access Permissions",
                            color = colors.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = if (permSummary.allEssentialGranted) {
                                if (isArabic) "${permSummary.grantedCount}/${permSummary.totalCount} صلاحيات مفعّلة بالكامل ✓"
                                else "${permSummary.grantedCount}/${permSummary.totalCount} permissions active ✓"
                            } else {
                                val missing = permSummary.totalCount - permSummary.grantedCount
                                if (isArabic) "${permSummary.grantedCount}/${permSummary.totalCount} • يتطلب تفعيل $missing صلاحية"
                                else "${permSummary.grantedCount}/${permSummary.totalCount} • $missing permissions needed"
                            },
                            color = statusColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        item(key = "authorisation_overview_list") {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = colors.glassSurface,
                shape = RoundedCornerShape(20.dp),
                onClick = onOpenAccessPermissions
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PermissionPreviewRow(
                        icon = Icons.Default.Sms,
                        title = if (isArabic) "رسائل SMS (بريدي موب)" else "BaridiMob SMS Sync",
                        granted = permSummary.smsGranted,
                        isArabic = isArabic
                    )
                    HorizontalDivider(color = colors.glassBorderSubtle, thickness = 0.6.dp)
                    PermissionPreviewRow(
                        icon = Icons.Default.ContactPhone,
                        title = if (isArabic) "جهات الاتصال" else "Contacts Access",
                        granted = permSummary.contactsGranted,
                        isArabic = isArabic
                    )
                    HorizontalDivider(color = colors.glassBorderSubtle, thickness = 0.6.dp)
                    PermissionPreviewRow(
                        icon = Icons.Default.NotificationsActive,
                        title = if (isArabic) "الإشعارات" else "Notifications",
                        granted = permSummary.notificationsGranted,
                        isArabic = isArabic
                    )
                    HorizontalDivider(color = colors.glassBorderSubtle, thickness = 0.6.dp)
                    PermissionPreviewRow(
                        icon = Icons.Default.SystemUpdate,
                        title = if (isArabic) "تثبيت التحديثات" else "Install App Updates",
                        granted = permSummary.installUnknownAppsAllowed,
                        isArabic = isArabic
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionPreviewRow(
    icon: ImageVector,
    title: String,
    granted: Boolean,
    isArabic: Boolean
) {
    val colors = LiquidGlassTheme.colors
    val tint = if (granted) EmeraldNeon else AmberNeon
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(19.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            color = colors.textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = if (granted) {
                if (isArabic) "مفعّل" else "Granted"
            } else {
                if (isArabic) "غير مفعّل" else "Needed"
            },
            color = tint,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AppearanceSettingsScreen(
    userSettings: UserSettings,
    onToggleBreathingGlow: (Boolean) -> Unit,
    onOpenThemeEngine: () -> Unit,
    onSelectThemeMode: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current
    val isArabic = strings is ArabicStrings
    val colors = LiquidGlassTheme.colors
    val dimens = LocalAdaptiveDimens.current

    val activeThemeSpec = ThemeEngineCatalog.findById(userSettings.designStyle)
    val activeThemeName = if (isArabic) activeThemeSpec.nameAr else activeThemeSpec.nameEn

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = dimens.screenPaddingH),
        contentPadding = edgeFadeVerticalContentPadding(
            extraTop = 8.dp,
            extraBottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Card Breathing Glow
        item(key = "appearance_breathing_glow") {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("breathing_glow_setting_row"),
                backgroundColor = colors.glassCard,
                borderColor = if (userSettings.breathingGlowEnabled) {
                    colors.primaryNeon.copy(alpha = 0.45f)
                } else {
                    colors.glassBorderSubtle
                },
                shape = RoundedCornerShape(20.dp),
                onClick = { onToggleBreathingGlow(!userSettings.breathingGlowEnabled) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.glassSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (userSettings.breathingGlowEnabled) colors.primaryNeon else colors.textMuted,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.breathingGlowTitle,
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (userSettings.breathingGlowEnabled) {
                                strings.breathingGlowActive
                            } else {
                                strings.breathingGlowDisabled
                            },
                            color = if (userSettings.breathingGlowEnabled) colors.primaryNeon else colors.textMuted,
                            fontSize = 11.5.sp
                        )
                    }
                    Switch(
                        checked = userSettings.breathingGlowEnabled,
                        onCheckedChange = onToggleBreathingGlow,
                        modifier = Modifier.testTag("breathing_glow_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.primaryNeon,
                            checkedTrackColor = colors.primaryNeon.copy(alpha = 0.35f),
                            uncheckedThumbColor = colors.textMuted,
                            uncheckedTrackColor = colors.glassSurface
                        )
                    )
                }
            }
        }

        // 2. Theme Engine
        item(key = "appearance_theme_engine") {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_theme_engine_button"),
                backgroundColor = colors.glassCard,
                borderColor = colors.primaryNeon.copy(alpha = 0.55f),
                shape = RoundedCornerShape(20.dp),
                onClick = onOpenThemeEngine
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .padding(horizontal = 16.dp, vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        colors.primaryNeon.copy(alpha = 0.28f),
                                        colors.secondaryNeon.copy(alpha = 0.24f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = if (isArabic) "محرك الثيمات" else "Theme Engine",
                            tint = colors.primaryNeon,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isArabic) "محرك الثيمات (Theme Engine)" else "Theme Engine",
                            color = colors.textPrimary,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isArabic) {
                                "$activeThemeName • ${ThemeEngineCatalog.allThemes.size} ثيماً"
                            } else {
                                "$activeThemeName • ${ThemeEngineCatalog.allThemes.size} themes"
                            },
                            color = colors.primaryNeon,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(colors.primaryNeon)
                        )
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(colors.secondaryNeon)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = colors.textMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 3. Mode (Dark / Light / System)
        item(key = "appearance_mode") {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = colors.glassCard,
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = strings.themeMode,
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LiquidThemeSegmentedControl(
                        currentThemeMode = userSettings.themeMode,
                        onSelectThemeMode = onSelectThemeMode
                    )
                }
            }
        }
    }
}

@Composable
fun SecuritySettingsScreen(
    userSettings: UserSettings,
    onToggleScreenshotProtection: (Boolean) -> Unit,
    onToggleBiometric: (Boolean) -> Unit,
    onSelectAutoLockTimeoutSeconds: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current
    val isArabic = strings is ArabicStrings
    val colors = LiquidGlassTheme.colors
    val dimens = LocalAdaptiveDimens.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = dimens.screenPaddingH),
        contentPadding = edgeFadeVerticalContentPadding(
            extraTop = 8.dp,
            extraBottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. No-Screenshot Mode
        item(key = "security_no_screenshot") {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("screenshot_protection_setting_row"),
                backgroundColor = colors.glassCard,
                borderColor = if (userSettings.screenshotProtectionEnabled) {
                    EmeraldNeon.copy(alpha = 0.45f)
                } else {
                    colors.glassBorderSubtle
                },
                shape = RoundedCornerShape(20.dp),
                onClick = { onToggleScreenshotProtection(!userSettings.screenshotProtectionEnabled) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.glassSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (userSettings.screenshotProtectionEnabled) EmeraldNeon else colors.textMuted,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.screenshotProtectionTitle,
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (userSettings.screenshotProtectionEnabled) {
                                strings.screenshotProtectionActive
                            } else {
                                strings.screenshotProtectionDisabled
                            },
                            color = if (userSettings.screenshotProtectionEnabled) EmeraldNeon else colors.textMuted,
                            fontSize = 11.5.sp
                        )
                    }
                    Switch(
                        checked = userSettings.screenshotProtectionEnabled,
                        onCheckedChange = onToggleScreenshotProtection,
                        modifier = Modifier.testTag("screenshot_protection_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = EmeraldNeon,
                            checkedTrackColor = EmeraldNeon.copy(alpha = 0.35f),
                            uncheckedThumbColor = colors.textMuted,
                            uncheckedTrackColor = colors.glassSurface
                        )
                    )
                }
            }
        }

        // 2. Fingerprint Authentication + Auto-Lock Timeout
        item(key = "security_fingerprint_auth") {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("biometric_setting_row"),
                backgroundColor = colors.glassCard,
                borderColor = if (userSettings.biometricEnabled) {
                    colors.primaryNeon.copy(alpha = 0.45f)
                } else {
                    colors.glassBorderSubtle
                },
                shape = RoundedCornerShape(20.dp),
                onClick = { onToggleBiometric(!userSettings.biometricEnabled) }
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.glassSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = colors.primaryNeon,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.biometricSettingsTitle,
                                color = colors.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (userSettings.biometricEnabled) strings.biometricActive else strings.biometricDisabled,
                                color = if (userSettings.biometricEnabled) EmeraldNeon else colors.textMuted,
                                fontSize = 11.5.sp
                            )
                        }
                        Switch(
                            checked = userSettings.biometricEnabled,
                            onCheckedChange = onToggleBiometric,
                            modifier = Modifier.testTag("biometric_auth_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.primaryNeon,
                                checkedTrackColor = colors.primaryNeon.copy(alpha = 0.35f),
                                uncheckedThumbColor = colors.textMuted,
                                uncheckedTrackColor = colors.glassSurface
                            )
                        )
                    }

                    AnimatedVisibility(visible = userSettings.biometricEnabled) {
                        val timeoutOptions = listOf(
                            0 to (if (isArabic) "فوري" else "Instant"),
                            30 to (if (isArabic) "30 ث" else "30s"),
                            60 to (if (isArabic) "1 د" else "1m"),
                            300 to (if (isArabic) "5 د" else "5m"),
                            -1 to (if (isArabic) "إيقاف" else "Off")
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.glassSurface.copy(alpha = 0.72f))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = if (isArabic) "القفل التلقائي عند الخلفية" else "Auto-Lock Timeout",
                                color = colors.textSecondary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                timeoutOptions.forEach { (sec, label) ->
                                    val selected = userSettings.autoLockTimeoutSeconds == sec
                                    Box(
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (selected) colors.primaryNeon.copy(alpha = 0.22f)
                                                else colors.glassCard
                                            )
                                            .border(
                                                1.dp,
                                                if (selected) colors.primaryNeon else colors.glassBorderSubtle,
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clickable { onSelectAutoLockTimeoutSeconds(sec) }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                            .testTag("auto_lock_option_$sec"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (selected) colors.primaryNeon else colors.textSecondary,
                                            fontSize = 11.5.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
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
}

@Composable
fun LanguageSettingsScreen(
    currentLanguage: String,
    onSelectLanguage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current
    val isArabic = strings is ArabicStrings
    val colors = LiquidGlassTheme.colors
    val dimens = LocalAdaptiveDimens.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = dimens.screenPaddingH),
        contentPadding = edgeFadeVerticalContentPadding(
            extraTop = 8.dp,
            extraBottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "language_selector_card") {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = colors.glassCard,
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.primaryNeon.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = null,
                                tint = colors.primaryNeon,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = strings.appLanguage,
                                color = colors.textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isArabic) "اختر لغة العرض واتجاه الواجهة" else "Choose display language & layout direction",
                                color = colors.textMuted,
                                fontSize = 11.5.sp
                            )
                        }
                    }

                    LiquidLanguageSegmentedControl(
                        currentLanguage = currentLanguage,
                        onSelectLanguage = onSelectLanguage
                    )

                    LanguageOptionRow(
                        title = "English",
                        subtitle = "Left-to-Right (LTR) interface",
                        selected = currentLanguage != "ar",
                        onClick = { onSelectLanguage("en") },
                        tag = "settings_language_en_option"
                    )

                    LanguageOptionRow(
                        title = "العربية",
                        subtitle = "الواجهة العربية الكاملة",
                        selected = currentLanguage == "ar",
                        onClick = { onSelectLanguage("ar") },
                        tag = "settings_language_ar_option"
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageOptionRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    val colors = LiquidGlassTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) colors.primaryNeon.copy(alpha = 0.14f)
                else colors.glassSurface.copy(alpha = 0.65f)
            )
            .border(
                width = if (selected) 1.3.dp else 1.dp,
                color = if (selected) colors.primaryNeon.copy(alpha = 0.65f) else colors.glassBorderSubtle,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (selected) colors.primaryNeon else colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = colors.textMuted,
                fontSize = 11.5.sp
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = colors.primaryNeon,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
