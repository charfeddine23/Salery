package com.example.ui.components

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import com.example.cloud.GoogleAuthUiState
import com.example.data.UserSettings
import com.example.ui.i18n.APP_NAME
import com.example.ui.i18n.APP_VERSION
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.ArabicStrings
import com.example.ui.i18n.DEVELOPER_CONTACT_TITLE
import com.example.ui.i18n.DEVELOPER_CONTACT_URL
import com.example.ui.i18n.DEVELOPER_NAME
import com.example.ui.i18n.isolateLtr
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.LocalAdaptiveDimens
import com.example.ui.theme.RoseNeon
import com.example.update.AppUpdateUiState

@Composable
fun SidebarDrawer(
    selectedRoute: String,
    onNavigate: (String) -> Unit,
    userSettings: UserSettings,
    pendingSmsCount: Int = 0,
    updateUiState: AppUpdateUiState = AppUpdateUiState.Idle,
    onCheckForUpdates: () -> Unit = {},
    googleAuthState: GoogleAuthUiState = GoogleAuthUiState.SignedOut,
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors
    val context = LocalContext.current
    val dimens = LocalAdaptiveDimens.current
    val isArabic = strings is ArabicStrings
    val signedIn = googleAuthState as? GoogleAuthUiState.SignedIn

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

            // 1. App Branding Header Card (kept as-is)
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = colors.glassSurface,
                shape = RoundedCornerShape(22.dp)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VaultixLogoContainer(
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

            // 2. Main 4 Navigation Pages (kept as-is)
            val isInsideCard = selectedRoute in listOf(
                "dashboard_details", "income", "expenses", "debts", "baridimob_sync", "insights", "assets_vault", "gold_asset_editor"
            )

            DrawerNavItem(
                icon = Icons.Default.CreditCard,
                title = if (isInsideCard) strings.navDashboard else if (isArabic) "مركز البطاقات" else "Card Hub",
                isSelected = selectedRoute == "cards" || selectedRoute == "visa_coming_soon",
                onClick = { onNavigate("cards") }
            )

            if (!isInsideCard) {
                DrawerNavItem(
                    icon = Icons.Default.VpnKey,
                    title = if (isArabic) "المفاتيح" else "Keys",
                    iconTint = AmberNeon,
                    isSelected = selectedRoute == "keys",
                    onClick = { onNavigate("keys") }
                )
                DrawerNavItem(
                    icon = Icons.Default.AccountBalance,
                    title = if (isArabic) "الحسابات البريدية" else "CCP's",
                    iconTint = EmeraldNeon,
                    isSelected = selectedRoute == "ccps",
                    onClick = { onNavigate("ccps") }
                )
                DrawerNavItem(
                    icon = Icons.Default.CurrencyExchange,
                    title = if (isArabic) "السكوار" else "Square",
                    iconTint = AmberNeon,
                    isSelected = selectedRoute == "square",
                    onClick = { onNavigate("square") }
                )
            } else {
                DrawerNavItem(
                    icon = Icons.Default.Dashboard,
                    title = if (isArabic) "لوحة التحكم" else "Dashboard",
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
                    title = if (isArabic) "مزامنة بريدي موب" else "BaridiMob SMS Sync",
                    iconTint = Color(0xFF818CF8),
                    isSelected = selectedRoute == "baridimob_sync",
                    badgeCount = pendingSmsCount,
                    onClick = { onNavigate("baridimob_sync") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = colors.glassBorderSubtle, thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // 3. Reorganized Settings Category Buttons
            Text(
                text = strings.settingsTitle,
                color = colors.textMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Button 1: Account
                DrawerCategoryButton(
                    icon = Icons.Default.ManageAccounts,
                    title = if (isArabic) "الحساب (Account)" else "Account",
                    iconTint = if (signedIn != null) EmeraldNeon else colors.primaryNeon,
                    isSelected = selectedRoute == "settings_account",
                    testTag = "settings_category_account",
                    onClick = { onNavigate("settings_account") }
                )

                // Button 2: Authorisation
                DrawerCategoryButton(
                    icon = Icons.Default.AdminPanelSettings,
                    title = if (isArabic) "التراخيص (Authorisation)" else "Authorisation",
                    iconTint = AmberNeon,
                    isSelected = selectedRoute == "settings_authorisation" || selectedRoute == "access_permissions",
                    testTag = "settings_category_authorisation",
                    onClick = { onNavigate("settings_authorisation") }
                )

                // Button 3: Appearance
                DrawerCategoryButton(
                    icon = Icons.Default.Palette,
                    title = if (isArabic) "المظهر (Appearance)" else "Appearance",
                    iconTint = colors.secondaryNeon,
                    isSelected = selectedRoute == "settings_appearance" || selectedRoute == "theme_engine",
                    testTag = "settings_category_appearance",
                    onClick = { onNavigate("settings_appearance") }
                )

                // Button 4: Security
                DrawerCategoryButton(
                    icon = Icons.Default.Shield,
                    title = if (isArabic) "الأمان (Security)" else "Security",
                    iconTint = EmeraldNeon,
                    isSelected = selectedRoute == "settings_security",
                    testTag = "settings_category_security",
                    onClick = { onNavigate("settings_security") }
                )

                // Button 5: Language
                DrawerCategoryButton(
                    icon = Icons.Default.Translate,
                    title = if (isArabic) "اللغة (Language)" else "Language",
                    iconTint = CyanNeon,
                    isSelected = selectedRoute == "settings_language",
                    testTag = "settings_category_language",
                    onClick = { onNavigate("settings_language") }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Check for Updates (kept as-is below settings buttons)
            val hasPendingUpdate = updateUiState is AppUpdateUiState.Available ||
                updateUiState is AppUpdateUiState.Downloading ||
                updateUiState is AppUpdateUiState.ReadyToInstall
            val updateSubtitle = when (updateUiState) {
                is AppUpdateUiState.Available -> "${strings.updateBadgeLabel} • ${isolateLtr(updateUiState.release.tagName)}"
                is AppUpdateUiState.Downloading -> strings.downloadingUpdate
                is AppUpdateUiState.ReadyToInstall -> strings.installUpdateButton
                else -> if (isArabic) "${isolateLtr("v$APP_VERSION")} • إصدارات جيت هب" else "v$APP_VERSION • GitHub Releases"
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

            // 5. Dynamic Developer Footer & App Info Card
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag("drawer_developer_card"),
                backgroundColor = colors.glassSurface,
                shape = RoundedCornerShape(18.dp),
                onClick = { onNavigate("settings_developer") }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        VaultixLogoContainer(
                            size = 42.dp,
                            cornerRadius = 13.dp,
                            padding = 4.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = EmeraldNeon,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = isolateLtr("Developed by: $DEVELOPER_NAME"),
                                    color = colors.textPrimary,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = isolateLtr("v$APP_VERSION"),
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
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = colors.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(12.dp))
                            .background(EmeraldNeon.copy(alpha = 0.13f))
                            .border(1.dp, EmeraldNeon.copy(alpha = 0.42f), RoundedCornerShape(12.dp))
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
                            .padding(horizontal = 12.dp, vertical = 9.dp)
                            .testTag("drawer_developer_whatsapp_link"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = null,
                            tint = EmeraldNeon,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = isolateLtr(DEVELOPER_CONTACT_TITLE),
                                color = EmeraldNeon,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = isolateLtr(DEVELOPER_CONTACT_URL),
                                color = colors.textSecondary,
                                fontSize = 10.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = EmeraldNeon,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerCategoryButton(
    icon: ImageVector,
    title: String,
    iconTint: Color,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val colors = LiquidGlassTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) colors.primaryNeon.copy(alpha = 0.16f)
                else colors.glassCard
            )
            .border(
                width = if (isSelected) 1.2.dp else 0.9.dp,
                color = if (isSelected) colors.primaryNeon.copy(alpha = 0.55f) else colors.glassBorderSubtle,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            color = if (isSelected) colors.primaryNeon else colors.textPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = if (isSelected) colors.primaryNeon else colors.textMuted,
            modifier = Modifier.size(19.dp)
        )
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
