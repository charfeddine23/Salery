package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.UserSettings
import com.example.ui.i18n.APP_NAME
import com.example.ui.i18n.APP_VERSION
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.ArabicStrings
import com.example.ui.i18n.DEVELOPER_NAME
import com.example.ui.screens.readAppPermissionsSummary
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.RoseNeon
import com.example.ui.theme.ThemeEngineCatalog
import com.example.update.AppUpdateUiState

@Composable
fun SidebarDrawer(
    selectedRoute: String,
    onNavigate: (String) -> Unit,
    userSettings: UserSettings,
    pendingSmsCount: Int = 0,
    onOpenSalaryScheduler: () -> Unit = {},
    onOpenBackupRestore: () -> Unit = {},
    onOpenAccessPermissions: () -> Unit = {},
    onOpenThemeEngine: () -> Unit = {},
    onOpenPatternLock: () -> Unit = {},
    onLockAppNow: () -> Unit = {},
    onToggleStealthMode: () -> Unit = {},
    onToggleBiometric: (Boolean) -> Unit = {},
    onToggleBreathingGlow: (Boolean) -> Unit = {},
    onToggleScreenshotProtection: (Boolean) -> Unit = {},
    onSelectDesignStyle: (String) -> Unit,
    onSelectThemeMode: (String) -> Unit,
    onSelectLanguage: (String) -> Unit,
    updateUiState: AppUpdateUiState = AppUpdateUiState.Idle,
    onCheckForUpdates: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors
    val context = LocalContext.current
    val permSummary = readAppPermissionsSummary(context)

    val dimens = com.example.ui.theme.LocalAdaptiveDimens.current

    Box(
        modifier = modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        colors.bgStart.copy(alpha = 0.96f),
                        colors.bgCenter.copy(alpha = 0.98f),
                        colors.bgEnd
                    )
                )
            )
            .padding(horizontal = dimens.cardPadding, vertical = dimens.cardPaddingLarge)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Liquid Glass Branding Header Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = colors.glassSurface,
                shape = RoundedCornerShape(22.dp)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SpLogoContainer(
                        size = 52.dp,
                        cornerRadius = 16.dp,
                        padding = 5.dp
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = APP_NAME,
                        color = colors.textPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Navigation Items
            val isInsideCard = selectedRoute in listOf(
                "dashboard_details", "income", "expenses", "debts", "baridimob_sync", "insights"
            )

            DrawerNavItem(
                icon = Icons.Default.CreditCard,
                title = if (isInsideCard) strings.navDashboard else "Card Hub",
                isSelected = selectedRoute == "cards" || selectedRoute == "visa_coming_soon",
                onClick = { onNavigate("cards") }
            )

            if (!isInsideCard) {
                DrawerNavItem(
                    icon = Icons.Default.VpnKey,
                    title = "Keys",
                    iconTint = com.example.ui.theme.AmberNeon,
                    isSelected = selectedRoute == "keys",
                    onClick = { onNavigate("keys") }
                )
                DrawerNavItem(
                    icon = Icons.Default.AccountBalance,
                    title = "CCP's",
                    iconTint = EmeraldNeon,
                    isSelected = selectedRoute == "ccps",
                    onClick = { onNavigate("ccps") }
                )
                DrawerNavItem(
                    icon = Icons.Default.CurrencyExchange,
                    title = "Square",
                    iconTint = com.example.ui.theme.AmberNeon,
                    isSelected = selectedRoute == "square",
                    onClick = { onNavigate("square") }
                )
            } else {
                DrawerNavItem(
                    icon = Icons.Default.Dashboard,
                    title = if (strings is ArabicStrings) "لوحة التحكم" else "Dashboard",
                    isSelected = selectedRoute == "dashboard_details",
                    onClick = { onNavigate("dashboard_details") }
                )
                DrawerNavItem(
                    icon = Icons.Default.TrendingUp,
                    title = strings.navIncome,
                    iconTint = EmeraldNeon,
                    isSelected = selectedRoute == "income",
                    onClick = { onNavigate("income") }
                )
                DrawerNavItem(
                    icon = Icons.Default.TrendingDown,
                    title = strings.navExpenses,
                    iconTint = RoseNeon,
                    isSelected = selectedRoute == "expenses",
                    onClick = { onNavigate("expenses") }
                )
                DrawerNavItem(
                    icon = Icons.Default.AccountBalanceWallet,
                    title = strings.navDebts,
                    iconTint = CyanNeon,
                    isSelected = selectedRoute == "debts",
                    onClick = { onNavigate("debts") }
                )
                DrawerNavItem(
                    icon = Icons.Default.Sms,
                    title = if (strings is ArabicStrings) "مزامنة بريدي موب" else "BaridiMob SMS Sync",
                    iconTint = Color(0xFF818CF8),
                    isSelected = selectedRoute == "baridimob_sync",
                    badgeCount = pendingSmsCount,
                    onClick = { onNavigate("baridimob_sync") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = colors.glassBorderSubtle, thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = strings.settingsTitle,
                color = colors.textMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Access Permissions (صلاحيات الوصول)
            val isArabic = strings is ArabicStrings
            DrawerActionItem(
                icon = Icons.Default.AdminPanelSettings,
                title = if (isArabic) "صلاحيات الوصول" else "Access Permissions",
                subtitle = if (permSummary.allEssentialGranted) {
                    if (isArabic) "${permSummary.grantedCount}/${permSummary.totalCount} صلاحيات مفعّلة ✓"
                    else "${permSummary.grantedCount}/${permSummary.totalCount} permissions active ✓"
                } else {
                    val missing = permSummary.totalCount - permSummary.grantedCount
                    if (isArabic) "${permSummary.grantedCount}/${permSummary.totalCount} • يتطلب تفعيل $missing"
                    else "${permSummary.grantedCount}/${permSummary.totalCount} • $missing needed"
                },
                iconTint = if (permSummary.allEssentialGranted) EmeraldNeon else com.example.ui.theme.AmberNeon,
                modifier = Modifier.testTag("access_permissions_setting_row"),
                onClick = onOpenAccessPermissions
            )

            // Backup & Restore
            DrawerActionItem(
                icon = Icons.Default.CloudDone,
                title = strings.backupDriveTitle,
                subtitle = if (isArabic) "حفظ واستعادة النسخ الاحتياطية" else "Google Drive & JSON backup",
                onClick = onOpenBackupRestore
            )

            // Fingerprint Authentication (زر تفعيل وتعطيل ال fingerprint authentication)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onToggleBiometric(!userSettings.biometricEnabled) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.glassCard),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = colors.primaryNeon,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.biometricSettingsTitle,
                        color = colors.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (userSettings.biometricEnabled) strings.biometricActive else strings.biometricDisabled,
                        color = if (userSettings.biometricEnabled) EmeraldNeon else colors.textMuted,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = userSettings.biometricEnabled,
                    onCheckedChange = { onToggleBiometric(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.primaryNeon,
                        checkedTrackColor = colors.primaryNeon.copy(alpha = 0.35f),
                        uncheckedThumbColor = colors.textMuted,
                        uncheckedTrackColor = colors.glassCard
                    )
                )
            }

            // Card Breathing Glow Toggle (تأثير التوهج التنفسي للبطاقات)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onToggleBreathingGlow(!userSettings.breathingGlowEnabled) }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .testTag("breathing_glow_setting_row"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.glassCard),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = if (userSettings.breathingGlowEnabled) colors.primaryNeon else colors.textMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.breathingGlowTitle,
                        color = colors.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (userSettings.breathingGlowEnabled) strings.breathingGlowActive else strings.breathingGlowDisabled,
                        color = if (userSettings.breathingGlowEnabled) colors.primaryNeon else colors.textMuted,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = userSettings.breathingGlowEnabled,
                    onCheckedChange = { onToggleBreathingGlow(it) },
                    modifier = Modifier.testTag("breathing_glow_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.primaryNeon,
                        checkedTrackColor = colors.primaryNeon.copy(alpha = 0.35f),
                        uncheckedThumbColor = colors.textMuted,
                        uncheckedTrackColor = colors.glassCard
                    )
                )
            }

            // No-Screenshot Protection Toggle (زر تفعيل وتعطيل منع التصوير)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onToggleScreenshotProtection(!userSettings.screenshotProtectionEnabled) }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .testTag("screenshot_protection_setting_row"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.glassCard),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = if (userSettings.screenshotProtectionEnabled) EmeraldNeon else colors.textMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.screenshotProtectionTitle,
                        color = colors.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (userSettings.screenshotProtectionEnabled) {
                            strings.screenshotProtectionActive
                        } else {
                            strings.screenshotProtectionDisabled
                        },
                        color = if (userSettings.screenshotProtectionEnabled) EmeraldNeon else colors.textMuted,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = userSettings.screenshotProtectionEnabled,
                    onCheckedChange = { onToggleScreenshotProtection(it) },
                    modifier = Modifier.testTag("screenshot_protection_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = EmeraldNeon,
                        checkedTrackColor = EmeraldNeon.copy(alpha = 0.35f),
                        uncheckedThumbColor = colors.textMuted,
                        uncheckedTrackColor = colors.glassCard
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Theme Engine Button (32 Themes: 2 classic + 30 signature themes)
            val activeThemeSpec = ThemeEngineCatalog.findById(userSettings.designStyle)
            val activeThemeName = if (isArabic) activeThemeSpec.nameAr else activeThemeSpec.nameEn

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_theme_engine_button"),
                backgroundColor = colors.glassCard,
                borderColor = colors.primaryNeon.copy(alpha = 0.55f),
                shape = RoundedCornerShape(18.dp),
                onClick = onOpenThemeEngine
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
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
                            contentDescription = "Theme Engine",
                            tint = colors.primaryNeon,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Theme Engine",
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isArabic) {
                                "$activeThemeName • ${ThemeEngineCatalog.allThemes.size} ثيماً"
                            } else {
                                "$activeThemeName • ${ThemeEngineCatalog.allThemes.size} themes"
                            },
                            color = colors.primaryNeon,
                            fontSize = 11.sp,
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

            Spacer(modifier = Modifier.height(14.dp))

            // Theme Mode & Language Switchers Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = colors.glassCard,
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = strings.themeMode,
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LiquidThemeSegmentedControl(
                        currentThemeMode = userSettings.themeMode,
                        onSelectThemeMode = onSelectThemeMode
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = colors.glassBorderSubtle, thickness = 0.6.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = strings.appLanguage,
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LiquidLanguageSegmentedControl(
                        currentLanguage = userSettings.language,
                        onSelectLanguage = onSelectLanguage
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // In-App Update Check Action Card
            val hasPendingUpdate = updateUiState is AppUpdateUiState.Available ||
                updateUiState is AppUpdateUiState.Downloading ||
                updateUiState is AppUpdateUiState.ReadyToInstall
            val updateSubtitle = when (updateUiState) {
                is AppUpdateUiState.Available -> "${strings.updateBadgeLabel} • ${updateUiState.release.tagName}"
                is AppUpdateUiState.Downloading -> strings.downloadingUpdate
                is AppUpdateUiState.ReadyToInstall -> strings.installUpdateButton
                else -> "v$APP_VERSION • GitHub Releases"
            }

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("check_updates_drawer_button"),
                backgroundColor = if (hasPendingUpdate) RoseNeon.copy(alpha = 0.14f) else colors.glassCard,
                borderColor = if (hasPendingUpdate) RoseNeon.copy(alpha = 0.55f) else colors.glassBorderSubtle,
                shape = RoundedCornerShape(18.dp),
                onClick = onCheckForUpdates
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(
                                if (hasPendingUpdate) RoseNeon.copy(alpha = 0.22f)
                                else colors.primaryNeon.copy(alpha = 0.14f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = if (hasPendingUpdate) RoseNeon else colors.primaryNeon,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.checkForUpdates,
                            color = colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = updateSubtitle,
                            color = if (hasPendingUpdate) RoseNeon else colors.textMuted,
                            fontSize = 11.sp
                        )
                    }
                    if (hasPendingUpdate) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(RoseNeon)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dynamic Developer Footer & App Info Card
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                backgroundColor = colors.glassSurface,
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SpLogoContainer(
                        size = 42.dp,
                        cornerRadius = 13.dp,
                        padding = 4.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = EmeraldNeon,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = DEVELOPER_NAME,
                                color = colors.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "v$APP_VERSION",
                                color = colors.primaryNeon,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "•",
                                color = colors.textMuted,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${strings.updatedLabel}: ${strings.buildDate}",
                                color = colors.textMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerNavItem(
    icon: ImageVector,
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    iconTint: Color = LiquidGlassTheme.colors.primaryNeon,
    badgeCount: Int = 0
) {
    val colors = LiquidGlassTheme.colors
    val bg = if (isSelected) colors.primaryNeon.copy(alpha = 0.18f) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) colors.primaryNeon else iconTint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            color = if (isSelected) colors.primaryNeon else colors.textPrimary,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        if (badgeCount > 0) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(RoseNeon)
                    .padding(horizontal = 7.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badgeCount.toString(),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun DrawerActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    iconTint: Color = LiquidGlassTheme.colors.primaryNeon,
    modifier: Modifier = Modifier
) {
    val colors = LiquidGlassTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.glassCard),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = colors.textMuted,
                fontSize = 11.sp
            )
        }
    }
}
