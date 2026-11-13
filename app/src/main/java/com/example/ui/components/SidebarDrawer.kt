package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.RoseNeon
import com.example.update.AppUpdateUiState

@Composable
fun SidebarDrawer(
    selectedRoute: String,
    onNavigate: (String) -> Unit,
    userSettings: UserSettings,
    onOpenSalaryScheduler: () -> Unit = {},
    onOpenBackupRestore: () -> Unit = {},
    onOpenPatternLock: () -> Unit = {},
    onLockAppNow: () -> Unit = {},
    onToggleStealthMode: () -> Unit = {},
    onToggleBiometric: (Boolean) -> Unit = {},
    onToggleBreathingGlow: (Boolean) -> Unit = {},
    onSelectDesignStyle: (String) -> Unit,
    onSelectThemeMode: (String) -> Unit,
    onSelectLanguage: (String) -> Unit,
    updateUiState: AppUpdateUiState = AppUpdateUiState.Idle,
    onCheckForUpdates: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(320.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        colors.bgStart.copy(alpha = 0.96f),
                        colors.bgCenter.copy(alpha = 0.98f),
                        colors.bgEnd
                    )
                )
            )
            .padding(horizontal = 20.dp, vertical = 24.dp)
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
            val isInsideCard = selectedRoute in listOf("dashboard_details", "income", "expenses", "debts", "insights")

            DrawerNavItem(
                icon = Icons.Default.CreditCard,
                title = strings.navDashboard,
                isSelected = selectedRoute == "cards" || selectedRoute == "visa_coming_soon",
                onClick = { onNavigate("cards") }
            )

            if (isInsideCard) {
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

            // Backup & Restore
            val isArabic = strings is ArabicStrings
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

            Spacer(modifier = Modifier.height(14.dp))

            // Dynamic Design Style, Theme & Language Switchers Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = colors.glassCard,
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = strings.designStyleTitle,
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LiquidDesignStyleSegmentedControl(
                        currentDesignStyle = userSettings.designStyle,
                        onSelectDesignStyle = onSelectDesignStyle
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = colors.glassBorderSubtle, thickness = 0.6.dp)
                    Spacer(modifier = Modifier.height(14.dp))

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
    iconTint: Color = LiquidGlassTheme.colors.primaryNeon
) {
    val colors = LiquidGlassTheme.colors
    val bg = if (isSelected) colors.primaryNeon.copy(alpha = 0.18f) else Color.Transparent
    val borderCol = if (isSelected) colors.primaryNeon.copy(alpha = 0.6f) else Color.Transparent

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
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun DrawerActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val colors = LiquidGlassTheme.colors

    Row(
        modifier = Modifier
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
                tint = colors.primaryNeon,
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
