package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.UserSettings
import com.example.security.BiometricAuthManager
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.ArabicStrings
import com.example.ui.i18n.formatPercent
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.LocalAdaptiveDimens
import com.example.ui.theme.LocalAdaptiveLayoutInfo
import com.example.ui.theme.LocalAdaptiveTypography
import com.example.ui.theme.RoseNeon
import com.example.update.AppUpdateManager

data class AppPermissionsSummary(
    val smsGranted: Boolean,
    val contactsGranted: Boolean,
    val notificationsGranted: Boolean,
    val installUnknownAppsAllowed: Boolean
) {
    val grantedCount: Int
        get() = listOf(
            smsGranted,
            contactsGranted,
            notificationsGranted,
            installUnknownAppsAllowed
        ).count { it }

    val totalCount: Int = 4
    val allEssentialGranted: Boolean get() = grantedCount == totalCount
}

fun readAppPermissionsSummary(context: Context): AppPermissionsSummary {
    val sms = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
        PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) ==
        PackageManager.PERMISSION_GRANTED
    val contacts = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
        PackageManager.PERMISSION_GRANTED
    val notifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
    val installSources = AppUpdateManager.canRequestPackageInstalls(context)
    return AppPermissionsSummary(
        smsGranted = sms,
        contactsGranted = contacts,
        notificationsGranted = notifications,
        installUnknownAppsAllowed = installSources
    )
}

@Composable
fun AccessPermissionsScreen(
    userSettings: UserSettings,
    onRequestInstallSourcesSettings: () -> Unit,
    onToggleBaridiMobSmsSync: (Boolean) -> Unit,
    onToggleBiometric: (Boolean) -> Unit,
    onReplayOnboardingWizard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors
    val isArabic = strings is ArabicStrings
    val layoutInfo = LocalAdaptiveLayoutInfo.current
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current

    var refreshTick by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshTick++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permSummary = remember(refreshTick) { readAppPermissionsSummary(context) }
    val biometricReady = remember(refreshTick) { BiometricAuthManager.isBiometricAvailable(context) }

    val runtimePermissionsToRequest = remember {
        buildList {
            add(Manifest.permission.READ_SMS)
            add(Manifest.permission.RECEIVE_SMS)
            add(Manifest.permission.READ_CONTACTS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }

    val multiPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        refreshTick++
        val smsOk = result[Manifest.permission.READ_SMS] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED
        if (smsOk && !userSettings.baridiMobSmsSyncEnabled) {
            onToggleBaridiMobSmsSync(true)
        }
    }

    val singlePermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        refreshTick++
    }

    fun openAppSystemSettings() {
        try {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Throwable) {
        }
    }

    val progress = permSummary.grantedCount.toFloat() / permSummary.totalCount.toFloat()
    val statusAccent = if (permSummary.allEssentialGranted) EmeraldNeon else AmberNeon

    val readinessHeroCard: @Composable () -> Unit = {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("access_permissions_hero_card"),
            backgroundColor = colors.glassCard,
            borderColor = statusAccent.copy(alpha = 0.45f)
        ) {
            Column(modifier = Modifier.padding(dimens.cardPadding)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(statusAccent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = statusAccent,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isArabic) "جاهزية النظام والأذونات" else "System Readiness & Controls",
                            color = colors.textPrimary,
                            fontSize = typo.titleSm,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isArabic) {
                                "تفعيل كل الصلاحيات اللازمة لعمل الرسائل، جهات الاتصال، التحديثات والتنبيهات بسلاسة"
                            } else {
                                "Manage all required permissions for SMS sync, contacts, alerts & updates"
                            },
                            color = colors.textMuted,
                            fontSize = typo.bodyXs
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isArabic) {
                            "تم تفعيل ${permSummary.grantedCount} من ${permSummary.totalCount} صلاحيات أساسية"
                        } else {
                            "${permSummary.grantedCount} of ${permSummary.totalCount} core permissions active"
                        },
                        color = statusAccent,
                        fontSize = typo.bodySm,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatPercent((progress * 100).toInt(), isArabic),
                        color = statusAccent,
                        fontSize = typo.bodySm,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(50)),
                    color = statusAccent,
                    trackColor = colors.glassSurface
                )

                if (!permSummary.smsGranted || !permSummary.contactsGranted || !permSummary.notificationsGranted) {
                    Spacer(modifier = Modifier.height(14.dp))
                    GlassButton(
                        text = if (isArabic) "تفعيل جميع صلاحيات النظام دفعة واحدة" else "Grant All Runtime Permissions",
                        icon = Icons.Default.VerifiedUser,
                        containerColor = EmeraldNeon,
                        contentColor = Color(0xFF041C24),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("grant_all_permissions_button"),
                        onClick = {
                            multiPermLauncher.launch(runtimePermissionsToRequest)
                        }
                    )
                }
            }
        }
    }

    val systemSettingsCard: @Composable () -> Unit = {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = colors.glassCard
        ) {
            Column(
                modifier = Modifier.padding(dimens.cardPadding),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlassButton(
                    text = if (isArabic) "فتح صفحة أذونات التطبيق في إعدادات النظام" else "Open App Permissions in Android Settings",
                    icon = Icons.Default.Settings,
                    containerColor = CyanNeon.copy(alpha = 0.22f),
                    contentColor = CyanNeon,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_system_app_settings_button"),
                    onClick = { openAppSystemSettings() }
                )

                GlassButton(
                    text = if (isArabic) "عرض نافذة تفعيل الصلاحيات الترحيبية" else "Show Animated Security Setup Wizard",
                    icon = Icons.Default.OpenInNew,
                    containerColor = colors.glassSurface,
                    contentColor = colors.textPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("replay_permissions_wizard_button"),
                    onClick = onReplayOnboardingWizard
                )
            }
        }
    }

    val permissionCards: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingMd)) {
            PermissionControlCard(
                icon = Icons.Default.Sms,
                accent = Color(0xFF818CF8),
                title = if (isArabic) "قراءة واستلام رسائل الهاتف (بريد الجزائر)" else "SMS Read & Receive (ALG Poste)",
                description = if (isArabic) {
                    "مطلوب لمزامنة إشعارات بريدي موب وبريد الجزائر واقتراح المصاريف (-) والدخل (+) تلقائياً"
                } else {
                    "Required for BaridiMob / ALG Poste SMS analysis and Expense/Income suggestions"
                },
                isGranted = permSummary.smsGranted,
                actionLabel = if (permSummary.smsGranted) {
                    if (isArabic) "مفعّلة" else "Granted"
                } else {
                    if (isArabic) "تفعيل صلاحية الرسائل" else "Grant SMS Access"
                },
                onActionClick = {
                    if (permSummary.smsGranted) {
                        openAppSystemSettings()
                    } else {
                        multiPermLauncher.launch(
                            arrayOf(
                                Manifest.permission.READ_SMS,
                                Manifest.permission.RECEIVE_SMS
                            )
                        )
                    }
                },
                footerContent = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.glassSurface)
                            .clickable {
                                if (!permSummary.smsGranted) {
                                    multiPermLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.READ_SMS,
                                            Manifest.permission.RECEIVE_SMS
                                        )
                                    )
                                } else {
                                    onToggleBaridiMobSmsSync(!userSettings.baridiMobSmsSyncEnabled)
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isArabic) "تفعيل المزامنة التلقائية لبريدي موب" else "Auto-Sync BaridiMob SMS",
                            color = colors.textSecondary,
                            fontSize = typo.bodySm,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Switch(
                            checked = userSettings.baridiMobSmsSyncEnabled && permSummary.smsGranted,
                            onCheckedChange = { checked ->
                                if (checked && !permSummary.smsGranted) {
                                    multiPermLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.READ_SMS,
                                            Manifest.permission.RECEIVE_SMS
                                        )
                                    )
                                } else {
                                    onToggleBaridiMobSmsSync(checked)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = EmeraldNeon,
                                checkedTrackColor = EmeraldNeon.copy(alpha = 0.35f)
                            )
                        )
                    }
                }
            )

            PermissionControlCard(
                icon = Icons.Default.ContactPhone,
                accent = CyanNeon,
                title = if (isArabic) "الوصول إلى جهات الاتصال" else "Contacts Access",
                description = if (isArabic) {
                    "مطلوب لربط الحسابات البريدية والأشخاص في الديون مباشرة من قائمة جهات الاتصال بالهاتف"
                } else {
                    "Required to link CCP profiles and Debt contacts directly from your address book"
                },
                isGranted = permSummary.contactsGranted,
                actionLabel = if (permSummary.contactsGranted) {
                    if (isArabic) "مفعّلة" else "Granted"
                } else {
                    if (isArabic) "تفعيل جهات الاتصال" else "Grant Contacts"
                },
                onActionClick = {
                    if (permSummary.contactsGranted) {
                        openAppSystemSettings()
                    } else {
                        singlePermLauncher.launch(Manifest.permission.READ_CONTACTS)
                    }
                }
            )

            PermissionControlCard(
                icon = Icons.Default.NotificationsActive,
                accent = EmeraldNeon,
                title = if (isArabic) "صلاحية الإشعارات والتنبيهات" else "Notifications & Reminders",
                description = if (isArabic) {
                    "مطلوب لتنبيهات مواعيد الديون، إشعارات الراتب واقتراحات رسائل بريد الجزائر الفورية"
                } else {
                    "Required for debt due-date reminders, salary alerts & instant ALG Poste notifications"
                },
                isGranted = permSummary.notificationsGranted,
                actionLabel = if (permSummary.notificationsGranted) {
                    if (isArabic) "مفعّلة" else "Granted"
                } else {
                    if (isArabic) "تفعيل الإشعارات" else "Grant Notifications"
                },
                onActionClick = {
                    if (!permSummary.notificationsGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        singlePermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        openAppSystemSettings()
                    }
                }
            )

            PermissionControlCard(
                icon = Icons.Default.SystemUpdate,
                accent = AmberNeon,
                title = if (isArabic) "التثبيت من مصادر غير معروفة" else "Install Unknown Apps",
                description = if (isArabic) {
                    "مطلوب للسماح بتثبيت التحديثات الرسمية لـ Vaultix مباشرة من داخل التطبيق عبر إعدادات النظام"
                } else {
                    "Required for seamless in-app APK updates from GitHub Releases in System Settings"
                },
                isGranted = permSummary.installUnknownAppsAllowed,
                actionLabel = if (permSummary.installUnknownAppsAllowed) {
                    if (isArabic) "مفعّل • إدارة" else "Allowed • Manage"
                } else {
                    if (isArabic) "فتح إعدادات التثبيت الآن" else "Allow in Settings"
                },
                onActionClick = onRequestInstallSourcesSettings
            )

            PermissionControlCard(
                icon = Icons.Default.Fingerprint,
                accent = Color(0xFFF472B6),
                title = if (isArabic) "الحماية البيومترية (البصمة / الوجه)" else "Biometric Hardware Access",
                description = if (biometricReady) {
                    if (isArabic) "مستشعر البصمة جاهز ومتاح لحماية التطبيق وبياناتك" else "Biometric sensor is ready to lock & protect Vaultix"
                } else {
                    if (isArabic) "يرجى تسجيل بصمة في إعدادات الهاتف لاستخدام القفل البيومتري" else "Enroll fingerprint/face in device Security Settings to enable"
                },
                isGranted = biometricReady && userSettings.biometricEnabled,
                actionLabel = if (!biometricReady) {
                    if (isArabic) "فتح إعدادات الأمان" else "Device Security"
                } else if (userSettings.biometricEnabled) {
                    if (isArabic) "قفل البصمة مفعل" else "Biometric Lock On"
                } else {
                    if (isArabic) "تفعيل قفل البصمة" else "Enable Biometric Lock"
                },
                onActionClick = {
                    if (biometricReady) {
                        onToggleBiometric(!userSettings.biometricEnabled)
                    } else {
                        try {
                            context.startActivity(
                                Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            )
                        } catch (_: Throwable) {
                            openAppSystemSettings()
                        }
                    }
                }
            )
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
                item { Spacer(modifier = Modifier.height(dimens.spacingXs)) }
                item { readinessHeroCard() }
                item { systemSettingsCard() }
                item { Spacer(modifier = Modifier.height(dimens.spacingXl)) }
            }
            LazyColumn(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(dimens.spacingMd)
            ) {
                item { Spacer(modifier = Modifier.height(dimens.spacingXs)) }
                item { permissionCards() }
                item { Spacer(modifier = Modifier.height(dimens.spacingXl)) }
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = dimens.screenPaddingH),
            verticalArrangement = Arrangement.spacedBy(dimens.spacingMd)
        ) {
            item {
                Spacer(modifier = Modifier.height(dimens.spacingXs))
                readinessHeroCard()
            }
            item { permissionCards() }
            item { systemSettingsCard() }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun PermissionControlCard(
    icon: ImageVector,
    accent: Color,
    title: String,
    description: String,
    isGranted: Boolean,
    actionLabel: String,
    onActionClick: () -> Unit,
    footerContent: (@Composable () -> Unit)? = null
) {
    val colors = LiquidGlassTheme.colors
    val isArabic = AppStrings.current is ArabicStrings
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current
    val statusColor = if (isGranted) EmeraldNeon else RoseNeon

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = colors.glassCard,
        borderColor = if (isGranted) EmeraldNeon.copy(alpha = 0.42f) else accent.copy(alpha = 0.38f)
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
                        .background(accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            color = colors.textPrimary,
                            fontSize = typo.body,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(statusColor.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isGranted) {
                                    if (isArabic) "مفعّلة" else "Active"
                                } else {
                                    if (isArabic) "غير مفعّلة" else "Needed"
                                },
                                color = statusColor,
                                fontSize = typo.caption,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = description,
                        color = colors.textMuted,
                        fontSize = typo.bodyXs
                    )
                }
            }

            if (footerContent != null) {
                Spacer(modifier = Modifier.height(10.dp))
                footerContent()
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = dimens.buttonHeight)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isGranted) colors.glassSurface else accent.copy(alpha = 0.20f)
                    )
                    .border(
                        width = 1.dp,
                        color = if (isGranted) colors.glassBorderSubtle else accent.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable(onClick = onActionClick)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = actionLabel,
                    color = if (isGranted) colors.textPrimary else accent,
                    fontSize = typo.bodySm,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
