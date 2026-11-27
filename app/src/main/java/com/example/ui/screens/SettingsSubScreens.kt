package com.example.ui.screens

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import com.example.cloud.CloudSyncUiState
import com.example.cloud.GoogleAuthUiState
import com.example.data.UserSettings
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.LiquidLanguageSegmentedControl
import com.example.ui.components.LiquidThemeSegmentedControl
import com.example.ui.components.VaultixLogoContainer
import com.example.ui.components.edgeFadeVerticalContentPadding
import com.example.ui.i18n.APP_NAME
import com.example.ui.i18n.APP_VERSION
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.ArabicStrings
import com.example.ui.i18n.DEVELOPER_CONTACT_TITLE
import com.example.ui.i18n.DEVELOPER_CONTACT_URL
import com.example.ui.i18n.DEVELOPER_NAME
import com.example.ui.i18n.formatDate
import com.example.ui.i18n.isolateLtr
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.LocalAdaptiveDimens
import com.example.ui.theme.LocalScreenshotProtection
import com.example.ui.theme.RoseNeon
import com.example.ui.theme.ThemeEngineCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private val avatarHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
}

@Composable
fun AccountSettingsScreen(
    userSettings: UserSettings,
    googleAuthState: GoogleAuthUiState,
    cloudSyncState: CloudSyncUiState,
    lastCloudSyncMillis: Long,
    recordsCount: Int = 0,
    onGoogleSignIn: () -> Unit,
    onGoogleSignOut: () -> Unit,
    onCloudSyncNow: () -> Unit,
    onRestoreFromCloud: () -> Unit,
    onOpenBackupRestore: () -> Unit,
    onSignOutToAuthScreen: () -> Unit,
    onUpdateAccountProfile: (firstName: String, lastName: String, username: String, pickedPhotoUri: Uri?, removeCustomPhoto: Boolean) -> Unit,
    onBeforeExternalFlow: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current
    val isArabic = strings is ArabicStrings
    val colors = LiquidGlassTheme.colors
    val dimens = LocalAdaptiveDimens.current

    val fullName = listOf(userSettings.firstName.trim(), userSettings.lastName.trim())
        .filter { it.isNotEmpty() }
        .joinToString(" ")
        .ifBlank { userSettings.localUsername.trim() }
    val signedIn = googleAuthState as? GoogleAuthUiState.SignedIn

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }

    val directPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onUpdateAccountProfile(
                userSettings.firstName,
                userSettings.lastName,
                userSettings.localUsername,
                uri,
                false
            )
        }
    }

    val avatarBitmap by rememberResolvedAvatarBitmap(
        localAvatarPath = userSettings.profileAvatarPath,
        remotePhotoUrl = signedIn?.photoUrl
    )

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
        // 1. Profile Card (exact layout matching reference screenshot)
        item(key = "account_profile_card") {
            val isLinked = signedIn != null
            val accentColor = if (isLinked) EmeraldNeon else colors.primaryNeon
            val profileCardBg = if (colors.isDark) {
                Color(0xFF0A2426).copy(alpha = 0.86f)
            } else {
                colors.glassCard
            }

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("account_profile_card"),
                backgroundColor = profileCardBg,
                borderColor = accentColor.copy(alpha = 0.42f),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = if (colors.isDark) 0.14f else 0.08f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Row: "Profile" label + "Linked" status pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isArabic) "الملف الشخصي" else "Profile",
                            color = colors.textSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )

                        val statusTint = if (isLinked) EmeraldNeon else AmberNeon
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(statusTint.copy(alpha = 0.14f))
                                .border(1.dp, statusTint.copy(alpha = 0.48f), CircleShape)
                                .then(
                                    if (!isLinked) {
                                        Modifier.clickable(onClick = onGoogleSignIn)
                                    } else {
                                        Modifier
                                    }
                                )
                                .padding(horizontal = 11.dp, vertical = 5.dp)
                                .testTag("account_linked_status_pill"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(statusTint)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isLinked) {
                                    if (isArabic) "مرتبط" else "Linked"
                                } else {
                                    if (isArabic) "محلي" else "Local"
                                },
                                color = statusTint,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Centered Avatar with Emerald Glowing Ring & Camera Badge
                    val displayName = fullName.ifBlank {
                        signedIn?.displayName ?: if (isArabic) "حساب Vaultix" else "Vaultix User"
                    }
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clickable {
                                onBeforeExternalFlow()
                                directPhotoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .testTag("account_avatar_picker_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        ProfileAvatarCircle(
                            bitmap = avatarBitmap,
                            displayName = displayName,
                            ringColor = accentColor,
                            size = 92.dp
                        )

                        // Bottom-right camera badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(CyanNeon)
                                .border(
                                    width = 2.dp,
                                    color = if (colors.isDark) Color(0xFF0A2022) else colors.glassSurface,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = if (isArabic) "تغيير الصورة" else "Change photo",
                                tint = Color(0xFF062026),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Display Name + Gold Badge Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = displayName,
                            color = colors.textPrimary,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 220.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val badgeTint = AmberNeon
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(badgeTint.copy(alpha = 0.15f))
                                .border(1.dp, badgeTint.copy(alpha = 0.65f), CircleShape)
                                .padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = badgeTint,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isLinked) {
                                    if (isArabic) "سحابي" else "Cloud"
                                } else {
                                    if (isArabic) "محلي" else "Local"
                                },
                                color = badgeTint,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(5.dp))

                    // Email / Handle Subtitle
                    val emailSubtitle = signedIn?.email?.takeIf { it.isNotBlank() }
                        ?: userSettings.localUsername.takeIf { it.isNotBlank() }?.let { "@$it" }
                        ?: if (isArabic) "حساب محلي غير مرتبط" else "Offline local vault"
                    Text(
                        text = isolateLtr(emailSubtitle),
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Indigo "Edit Photo & Name" Pill Button
                    val editBtnBg = if (colors.isDark) Color(0xFF4F46E5).copy(alpha = 0.24f) else Color(0xFF6366F1).copy(alpha = 0.14f)
                    val editBtnBorder = Color(0xFF818CF8).copy(alpha = 0.45f)
                    val editBtnContent = if (colors.isDark) Color(0xFFC7D2FE) else Color(0xFF4338CA)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(16.dp))
                            .background(editBtnBg)
                            .border(1.dp, editBtnBorder, RoundedCornerShape(16.dp))
                            .clickable { showEditProfileDialog = true }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .testTag("account_edit_profile_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = editBtnContent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isArabic) "تعديل الصورة والاسم" else "Edit Photo & Name",
                                color = editBtnContent,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // 2. Cloud Sync Card (Cloud Sync Active + Sync Now & Sign Out)
        item(key = "account_cloud_sync_card") {
            AccountCloudSyncCard(
                isArabic = isArabic,
                googleAuthState = googleAuthState,
                cloudSyncState = cloudSyncState,
                lastCloudSyncMillis = lastCloudSyncMillis,
                recordsCount = recordsCount,
                onGoogleSignIn = onGoogleSignIn,
                onCloudSyncNow = onCloudSyncNow,
                onOpenSignOutDialog = { showSignOutDialog = true },
                onSwitchToAuthScreen = onSignOutToAuthScreen
            )
        }

        // 3. Google Drive & Backup Card (Save & Restore)
        item(key = "account_backup_card") {
            AccountBackupCard(
                isArabic = isArabic,
                lastBackupTimestamp = userSettings.lastBackupTimestamp,
                backupDriveTitle = strings.backupDriveTitle,
                onSaveClick = onOpenBackupRestore,
                onRestoreClick = onOpenBackupRestore
            )
        }
    }

    if (showEditProfileDialog) {
        EditAccountProfileDialog(
            isArabic = isArabic,
            initialFirstName = userSettings.firstName,
            initialLastName = userSettings.lastName,
            initialUsername = userSettings.localUsername,
            hasCustomPhoto = userSettings.profileAvatarPath.isNotBlank(),
            currentAvatarBitmap = avatarBitmap,
            onBeforeExternalFlow = onBeforeExternalFlow,
            onDismiss = { showEditProfileDialog = false },
            onSave = { first, last, username, pickedUri, removePhoto ->
                showEditProfileDialog = false
                onUpdateAccountProfile(first, last, username, pickedUri, removePhoto)
            }
        )
    }

    if (showSignOutDialog) {
        SignOutAccountDialog(
            isArabic = isArabic,
            isLinked = signedIn != null,
            signedInEmail = signedIn?.email,
            onDismiss = { showSignOutDialog = false },
            onSignOutCloudOnly = {
                showSignOutDialog = false
                onGoogleSignOut()
            },
            onSignOutAndSwitchAccount = {
                showSignOutDialog = false
                if (signedIn != null) {
                    onGoogleSignOut()
                }
                onSignOutToAuthScreen()
            },
            onRestoreFromCloud = {
                showSignOutDialog = false
                onRestoreFromCloud()
            }
        )
    }
}

@Composable
private fun rememberResolvedAvatarBitmap(
    localAvatarPath: String,
    remotePhotoUrl: String?
): androidx.compose.runtime.State<ImageBitmap?> {
    return produceState<ImageBitmap?>(initialValue = null, localAvatarPath, remotePhotoUrl) {
        value = withContext(Dispatchers.IO) {
            if (localAvatarPath.isNotBlank()) {
                val file = File(localAvatarPath)
                if (file.exists()) {
                    runCatching {
                        BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                    }.getOrNull()?.let { return@withContext it }
                }
            }
            if (!remotePhotoUrl.isNullOrBlank()) {
                runCatching {
                    val req = Request.Builder().url(remotePhotoUrl).build()
                    avatarHttpClient.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val bytes = resp.body?.bytes()
                            if (bytes != null && bytes.isNotEmpty()) {
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                            } else null
                        } else null
                    }
                }.getOrNull()?.let { return@withContext it }
            }
            null
        }
    }
}

@Composable
private fun ProfileAvatarCircle(
    bitmap: ImageBitmap?,
    displayName: String,
    ringColor: Color,
    size: Dp
) {
    val colors = LiquidGlassTheme.colors
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(ringColor.copy(alpha = 0.14f))
            .border(2.dp, ringColor.copy(alpha = 0.85f), CircleShape)
            .padding(4.dp)
            .clip(CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        ringColor.copy(alpha = 0.28f),
                        colors.glassSurface
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            val initials = displayName
                .split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .joinToString("") { it.first().uppercaseChar().toString() }
            if (initials.isNotEmpty()) {
                Text(
                    text = initials,
                    color = colors.textPrimary,
                    fontSize = (size.value * 0.30f).sp,
                    fontWeight = FontWeight.ExtraBold
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = displayName,
                    tint = ringColor,
                    modifier = Modifier.size(size * 0.45f)
                )
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
    recordsCount: Int,
    onGoogleSignIn: () -> Unit,
    onCloudSyncNow: () -> Unit,
    onOpenSignOutDialog: () -> Unit,
    onSwitchToAuthScreen: () -> Unit
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

    val timeLabel = remember(lastCloudSyncMillis) {
        if (lastCloudSyncMillis > 0L) {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(lastCloudSyncMillis))
        } else {
            null
        }
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cloud_sync_drawer_card"),
        backgroundColor = colors.glassCard,
        borderColor = accentColor.copy(alpha = 0.42f),
        shape = RoundedCornerShape(22.dp)
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
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accentColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isAuthLoading || isSyncing) {
                        CircularProgressIndicator(
                            color = accentColor,
                            strokeWidth = 2.2.dp,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Icon(
                            imageVector = when {
                                signedIn != null -> Icons.Default.CloudDone
                                authError != null -> Icons.Default.CloudOff
                                else -> Icons.Default.CloudQueue
                            },
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    val title = when {
                        isAuthLoading -> if (isArabic) "جاري تسجيل الدخول..." else "Signing In..."
                        isSyncing -> if (isArabic) "جاري المزامنة السحابية..." else "Cloud Syncing..."
                        signedIn != null && cloudSyncState is CloudSyncUiState.Error ->
                            if (isArabic) "تعذرت المزامنة السحابية" else "Cloud Sync Attention"
                        signedIn != null -> if (isArabic) "المزامنة السحابية نشطة" else "Cloud Sync Active"
                        authError != null -> if (isArabic) "خطأ في المصادقة" else "Authentication Error"
                        else -> if (isArabic) "المزامنة السحابية متوقفة" else "Cloud Sync Paused"
                    }
                    Text(
                        text = title,
                        color = colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))

                    val subtitle = when {
                        signedIn != null && cloudSyncState is CloudSyncUiState.Error -> cloudSyncState.message
                        signedIn != null -> {
                            val totalRecords = when (cloudSyncState) {
                                is CloudSyncUiState.Success -> (cloudSyncState.transactionCount + cloudSyncState.debtCount).coerceAtLeast(recordsCount)
                                else -> recordsCount
                            }
                            val countPart = if (isArabic) "$totalRecords سجل" else "$totalRecords records"
                            if (timeLabel != null) {
                                "$countPart • ${isolateLtr(timeLabel)}"
                            } else {
                                countPart
                            }
                        }
                        authError != null -> authError
                        else -> if (isArabic) {
                            "اربط حساب Google لمزامنة كل بياناتك تلقائياً"
                        } else {
                            "Sign in with Google to sync your vault automatically"
                        }
                    }
                    Text(
                        text = subtitle,
                        color = if ((authError != null && signedIn == null) || cloudSyncState is CloudSyncUiState.Error) RoseNeon else colors.textMuted,
                        fontSize = 12.5.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = colors.glassBorderSubtle, thickness = 0.7.dp)
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (signedIn != null) {
                    // Left Button: Sync Now
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(14.dp))
                            .background(EmeraldNeon.copy(alpha = 0.14f))
                            .border(1.dp, EmeraldNeon.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                            .clickable(enabled = !isSyncing, onClick = onCloudSyncNow)
                            .padding(vertical = 12.dp, horizontal = 10.dp)
                            .testTag("drawer_cloud_sync_now_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                tint = EmeraldNeon,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isArabic) "مزامنة الآن" else "Sync Now",
                                color = EmeraldNeon,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Right Button: Sign Out
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(14.dp))
                            .background(RoseNeon.copy(alpha = 0.13f))
                            .border(1.dp, RoseNeon.copy(alpha = 0.42f), RoundedCornerShape(14.dp))
                            .clickable(onClick = onOpenSignOutDialog)
                            .padding(vertical = 12.dp, horizontal = 10.dp)
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
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isArabic) "تسجيل الخروج" else "Sign Out",
                                color = RoseNeon,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else {
                    // Left Button: Sign In with Google
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(14.dp))
                            .background(EmeraldNeon.copy(alpha = 0.14f))
                            .border(1.dp, EmeraldNeon.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                            .clickable(enabled = !isAuthLoading, onClick = onGoogleSignIn)
                            .padding(vertical = 12.dp, horizontal = 10.dp)
                            .testTag("drawer_google_signin_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = EmeraldNeon,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isArabic) "ربط Google" else "Sign In",
                                color = EmeraldNeon,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Right Button: Switch / Sign Out to Auth Screen
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(14.dp))
                            .background(RoseNeon.copy(alpha = 0.13f))
                            .border(1.dp, RoseNeon.copy(alpha = 0.42f), RoundedCornerShape(14.dp))
                            .clickable(onClick = onSwitchToAuthScreen)
                            .padding(vertical = 12.dp, horizontal = 10.dp)
                            .testTag("open_auth_screen_drawer_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = null,
                                tint = RoseNeon,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isArabic) "تسجيل الخروج" else "Sign Out",
                                color = RoseNeon,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
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

@Composable
private fun AccountBackupCard(
    isArabic: Boolean,
    lastBackupTimestamp: Long,
    backupDriveTitle: String,
    onSaveClick: () -> Unit,
    onRestoreClick: () -> Unit
) {
    val colors = LiquidGlassTheme.colors
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_google_drive_backups_button"),
        backgroundColor = colors.glassCard,
        borderColor = colors.glassBorderSubtle,
        shape = RoundedCornerShape(22.dp)
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
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(EmeraldNeon.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SdStorage,
                        contentDescription = null,
                        tint = EmeraldNeon,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = backupDriveTitle,
                        color = colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val subtitle = if (lastBackupTimestamp > 0L) {
                        val dateLabel = isolateLtr(formatDate(lastBackupTimestamp, isArabic))
                        if (isArabic) "آخر نسخة: $dateLabel" else "Last backup: $dateLabel"
                    } else {
                        if (isArabic) "حفظ واستعادة النسخة على Drive أو الجهاز" else "Save & restore backup on Drive or device"
                    }
                    Text(
                        text = subtitle,
                        color = colors.textMuted,
                        fontSize = 12.5.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = colors.glassBorderSubtle, thickness = 0.7.dp)
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Left Button: Save
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(14.dp))
                        .background(EmeraldNeon.copy(alpha = 0.14f))
                        .border(1.dp, EmeraldNeon.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                        .clickable(onClick = onSaveClick)
                        .padding(vertical = 12.dp, horizontal = 10.dp)
                        .testTag("account_backup_save_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            tint = EmeraldNeon,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isArabic) "حفظ" else "Save",
                            color = EmeraldNeon,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Right Button: Restore
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(14.dp))
                        .background(AmberNeon.copy(alpha = 0.13f))
                        .border(1.dp, AmberNeon.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                        .clickable(onClick = onRestoreClick)
                        .padding(vertical = 12.dp, horizontal = 10.dp)
                        .testTag("account_backup_restore_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = AmberNeon,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isArabic) "استعادة" else "Restore",
                            color = AmberNeon,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditAccountProfileDialog(
    isArabic: Boolean,
    initialFirstName: String,
    initialLastName: String,
    initialUsername: String,
    hasCustomPhoto: Boolean,
    currentAvatarBitmap: ImageBitmap?,
    onBeforeExternalFlow: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (firstName: String, lastName: String, username: String, pickedPhotoUri: Uri?, removeCustomPhoto: Boolean) -> Unit
) {
    val colors = LiquidGlassTheme.colors
    val context = LocalContext.current
    val isScreenshotProtected = LocalScreenshotProtection.current

    var firstName by remember(initialFirstName) { mutableStateOf(initialFirstName) }
    var lastName by remember(initialLastName) { mutableStateOf(initialLastName) }
    var username by remember(initialUsername) { mutableStateOf(initialUsername) }
    var pickedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var removeCustomPhoto by remember { mutableStateOf(false) }

    val previewBitmap by produceState<ImageBitmap?>(
        initialValue = currentAvatarBitmap,
        pickedPhotoUri,
        removeCustomPhoto,
        currentAvatarBitmap
    ) {
        value = when {
            removeCustomPhoto -> null
            pickedPhotoUri != null -> withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(pickedPhotoUri!!)?.use { stream ->
                        BitmapFactory.decodeStream(stream)?.asImageBitmap()
                    }
                }.getOrNull()
            }
            else -> currentAvatarBitmap
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            pickedPhotoUri = uri
            removeCustomPhoto = false
        }
    }

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
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                shape = RoundedCornerShape(26.dp),
                backgroundColor = colors.dialogBackground
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isArabic) "تعديل الصورة والاسم" else "Edit Photo & Name",
                            color = colors.textPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = if (isArabic) "إغلاق" else "Close",
                                tint = colors.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val previewLabel = listOf(firstName.trim(), lastName.trim())
                        .filter { it.isNotEmpty() }
                        .joinToString(" ")
                        .ifBlank { username.ifBlank { "V" } }

                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clickable {
                                onBeforeExternalFlow()
                                photoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        ProfileAvatarCircle(
                            bitmap = previewBitmap,
                            displayName = previewLabel,
                            ringColor = EmeraldNeon,
                            size = 84.dp
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(CyanNeon)
                                .border(2.dp, colors.dialogBackground, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = if (isArabic) "اختيار صورة" else "Choose photo",
                                tint = Color(0xFF062026),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    if (hasCustomPhoto || pickedPhotoUri != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    pickedPhotoUri = null
                                    removeCustomPhoto = true
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = RoseNeon,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isArabic) "إزالة الصورة المخصصة" else "Remove custom photo",
                                color = RoseNeon,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val fieldColors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = colors.primaryNeon,
                        unfocusedBorderColor = colors.glassBorderSubtle,
                        focusedLabelColor = colors.primaryNeon,
                        unfocusedLabelColor = colors.textMuted
                    )

                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        singleLine = true,
                        label = { Text(if (isArabic) "الاسم الأول" else "First Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_profile_first_name_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        singleLine = true,
                        label = { Text(if (isArabic) "اللقب" else "Last Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_profile_last_name_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        singleLine = true,
                        label = { Text(if (isArabic) "اسم المستخدم" else "Username") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_profile_username_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GlassButton(
                            text = if (isArabic) "إلغاء" else "Cancel",
                            containerColor = colors.glassCard,
                            contentColor = colors.textSecondary,
                            modifier = Modifier.weight(1f),
                            onClick = onDismiss
                        )
                        GlassButton(
                            text = if (isArabic) "حفظ" else "Save",
                            icon = Icons.Default.Save,
                            containerColor = EmeraldNeon,
                            contentColor = Color(0xFF062018),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("edit_profile_save_button"),
                            onClick = {
                                onSave(firstName, lastName, username, pickedPhotoUri, removeCustomPhoto)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SignOutAccountDialog(
    isArabic: Boolean,
    isLinked: Boolean,
    signedInEmail: String?,
    onDismiss: () -> Unit,
    onSignOutCloudOnly: () -> Unit,
    onSignOutAndSwitchAccount: () -> Unit,
    onRestoreFromCloud: () -> Unit
) {
    val colors = LiquidGlassTheme.colors
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
                    .widthIn(max = 460.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                shape = RoundedCornerShape(24.dp),
                backgroundColor = colors.dialogBackground
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isArabic) "إدارة الجلسة السحابية" else "Account Session",
                            color = colors.textPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = colors.textSecondary
                            )
                        }
                    }

                    if (!signedInEmail.isNullOrBlank()) {
                        Text(
                            text = if (isArabic) {
                                "الحساب المرتبط: ${isolateLtr(signedInEmail)}"
                            } else {
                                "Connected: $signedInEmail"
                            },
                            color = colors.textSecondary,
                            fontSize = 12.5.sp
                        )
                    }

                    if (isLinked) {
                        GlassButton(
                            text = if (isArabic) "جلب واستعادة من السحابة" else "Pull & Restore from Cloud",
                            icon = Icons.Default.Sync,
                            containerColor = EmeraldNeon.copy(alpha = 0.16f),
                            contentColor = EmeraldNeon,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("drawer_cloud_restore_button"),
                            onClick = onRestoreFromCloud
                        )

                        GlassButton(
                            text = if (isArabic) "فصل الحساب السحابي فقط" else "Sign Out of Cloud Only",
                            icon = Icons.AutoMirrored.Filled.Logout,
                            containerColor = AmberNeon.copy(alpha = 0.14f),
                            contentColor = AmberNeon,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onSignOutCloudOnly
                        )
                    }

                    GlassButton(
                        text = if (isArabic) "تسجيل الخروج لشاشة الدخول" else "Sign Out & Switch Account",
                        icon = Icons.AutoMirrored.Filled.Logout,
                        containerColor = RoseNeon.copy(alpha = 0.15f),
                        contentColor = RoseNeon,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_auth_screen_drawer_button"),
                        onClick = onSignOutAndSwitchAccount
                    )
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

@Composable
fun DeveloperSettingsScreen(
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current
    val isArabic = strings is ArabicStrings
    val colors = LiquidGlassTheme.colors
    val dimens = LocalAdaptiveDimens.current
    val context = LocalContext.current

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
        item(key = "developer_info_card") {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("developer_settings_card"),
                backgroundColor = colors.glassCard,
                borderColor = EmeraldNeon.copy(alpha = 0.42f),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    VaultixLogoContainer(
                        size = 72.dp,
                        cornerRadius = 22.dp,
                        padding = 6.dp
                    )

                    Text(
                        text = APP_NAME,
                        color = colors.textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Text(
                        text = isolateLtr("Developed by: $DEVELOPER_NAME"),
                        color = colors.textPrimary,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "${isolateLtr("v$APP_VERSION")} • ${strings.updatedLabel}: ${strings.buildDate}",
                        color = colors.textMuted,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(16.dp))
                            .background(EmeraldNeon.copy(alpha = 0.14f))
                            .border(1.dp, EmeraldNeon.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                            .clickable {
                                runCatching {
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse(DEVELOPER_CONTACT_URL)
                                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .testTag("developer_whatsapp_link_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(EmeraldNeon.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = null,
                                tint = EmeraldNeon,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = isolateLtr(DEVELOPER_CONTACT_TITLE),
                                color = EmeraldNeon,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = isolateLtr(DEVELOPER_CONTACT_URL),
                                color = colors.textSecondary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = if (isArabic) "فتح الرابط" else "Open link",
                            tint = EmeraldNeon,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

