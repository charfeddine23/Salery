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
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.UserSettings
import com.example.ui.i18n.APP_NAME
import com.example.ui.i18n.APP_VERSION
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.DEVELOPER_NAME
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.RoseNeon

@Composable
fun SidebarDrawer(
    selectedRoute: String,
    onNavigate: (String) -> Unit,
    userSettings: UserSettings,
    onOpenSalaryScheduler: () -> Unit,
    onOpenBackupRestore: () -> Unit,
    onOpenPatternLock: () -> Unit,
    onLockAppNow: () -> Unit,
    onToggleTheme: () -> Unit,
    onSelectLanguage: (String) -> Unit,
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
            DrawerNavItem(
                icon = Icons.Default.Dashboard,
                title = strings.navDashboard,
                isSelected = selectedRoute == "dashboard",
                onClick = { onNavigate("dashboard") }
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
                icon = Icons.Default.CreditCard,
                title = strings.navDebts,
                iconTint = CyanNeon,
                isSelected = selectedRoute == "debts",
                onClick = { onNavigate("debts") }
            )

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

            // Smart Salary Scheduler trigger
            DrawerActionItem(
                icon = Icons.Default.Schedule,
                title = strings.salarySchedulerTitle,
                subtitle = "${userSettings.salaryAmount.toLong()} DZD • Day ${userSettings.salaryPayday}",
                onClick = onOpenSalaryScheduler
            )

            // Backup & Restore
            DrawerActionItem(
                icon = Icons.Default.CloudDone,
                title = strings.backupDriveTitle,
                subtitle = "Manual Google Drive & JSON",
                onClick = onOpenBackupRestore
            )

            // Pattern Lock Security
            DrawerActionItem(
                icon = Icons.Default.Lock,
                title = strings.patternLock,
                subtitle = if (userSettings.patternLockEnabled) "Enabled (3x3 Matrix)" else "Disabled",
                onClick = onOpenPatternLock
            )

            if (userSettings.patternLockEnabled) {
                DrawerActionItem(
                    icon = Icons.Default.Lock,
                    title = "Lock SaleryPulse Now",
                    subtitle = "Require pattern unlock immediately",
                    onClick = onLockAppNow
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dynamic Theme & Language Switchers Card
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
                        isDark = colors.isDark,
                        onToggle = onToggleTheme
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

            Spacer(modifier = Modifier.height(20.dp))

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
                                text = "Updated: Nov 5, 2026",
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
