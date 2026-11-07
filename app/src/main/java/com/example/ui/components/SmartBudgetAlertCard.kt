package com.example.ui.components

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.TransactionEntity
import com.example.notification.CategoryBudgetAlert
import com.example.notification.SmartBudgetAlertManager
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.LocalStealthMode
import com.example.ui.i18n.formatDzd
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.NeuBurntCoral
import com.example.ui.theme.RoseNeon

/**
 * Smart Category Budget Threshold Alert & Local Notification Card.
 * Warns users before reaching category spending caps (e.g. 80% of Shopping in November),
 * provides actionable deficit avoidance advice, and dispatches real local Android notifications.
 */
@Composable
fun SmartBudgetAlertCard(
    transactions: List<TransactionEntity>,
    isArabic: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LiquidGlassTheme.colors
    val strings = AppStrings.current
    val isStealth = LocalStealthMode.current

    // Custom caps state
    var customCaps by remember { mutableStateOf(SmartBudgetAlertManager.DEFAULT_CAPS.toMutableMap()) }

    // Calculate real alerts
    val realAlerts = remember(transactions, customCaps) {
        SmartBudgetAlertManager.calculateAlerts(transactions, customCaps)
    }

    // Always have sample/highlight for November Shopping 80% if transactions don't have enough data
    val sampleShopping = remember { SmartBudgetAlertManager.createSampleNovemberShoppingAlert() }

    // Find any alerts reaching >= 80%
    val alertsReachingThreshold = realAlerts.filter { it.percentage >= 80.0 }

    // Currently selected category key to view
    var selectedCategoryKey by remember {
        mutableStateOf(
            alertsReachingThreshold.firstOrNull()?.categoryId ?: "shopping"
        )
    }

    // Selected alert object
    val currentAlert = realAlerts.find { it.categoryId == selectedCategoryKey }
        ?: if (selectedCategoryKey == "shopping") sampleShopping else realAlerts.first()

    // Dialog to adjust category cap
    var showAdjustCapDialog by remember { mutableStateOf(false) }
    var adjustCapInput by remember { mutableStateOf("") }

    // Android 13+ Notification Permission Launcher
    var pendingAlertToNotify by remember { mutableStateOf<CategoryBudgetAlert?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingAlertToNotify?.let { alert ->
                val sent = SmartBudgetAlertManager.sendAlertNotification(context, alert, isArabic)
                if (sent) {
                    Toast.makeText(
                        context,
                        if (isArabic) "تم إرسال إشعار الميزانية الذكي بنجاح 🔔" else "Smart budget alert notification sent 🔔",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            Toast.makeText(
                context,
                if (isArabic) "يرجى منح إذن الإشعارات لتلقي تنبيهات الميزانية" else "Please grant notification permission for budget alerts",
                Toast.LENGTH_SHORT
            ).show()
        }
        pendingAlertToNotify = null
    }

    // Infinite pulse for warning beacon
    val infiniteTransition = rememberInfiniteTransition(label = "alert_pulse")
    val beaconAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beacon_alpha"
    )

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(26.dp),
        backgroundColor = colors.glassCard
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header Row
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
                            .background(
                                Brush.linearGradient(
                                    listOf(AmberNeon.copy(alpha = 0.25f), RoseNeon.copy(alpha = 0.25f))
                                )
                            )
                            .border(1.dp, AmberNeon.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = AmberNeon,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = if (isArabic) "إشعارات الميزانية الذكية وتنبيهات السقف" else "Smart Budget Alerts & Deficit Guard",
                            color = colors.textPrimary,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.2.sp
                        )
                        Text(
                            text = if (isArabic) "تنبيه استباقي عند بلوغ 80% من سقف المصاريف" else "Proactive 80% threshold warning & advice",
                            color = colors.textMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                // Beacon indicator
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            if (currentAlert.percentage >= 80.0) AmberNeon.copy(alpha = beaconAlpha) else EmeraldNeon
                        )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category Selector Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                realAlerts.forEach { alert ->
                    val isSelected = alert.categoryId == selectedCategoryKey
                    val isCritical = alert.percentage >= 80.0
                    val chipBg = when {
                        isSelected && isCritical -> AmberNeon.copy(alpha = 0.22f)
                        isSelected -> colors.primaryNeon.copy(alpha = 0.2f)
                        isCritical -> AmberNeon.copy(alpha = 0.1f)
                        else -> colors.glassSurface.copy(alpha = 0.5f)
                    }
                    val chipBorder = when {
                        isSelected && isCritical -> AmberNeon
                        isSelected -> colors.primaryNeon
                        isCritical -> AmberNeon.copy(alpha = 0.4f)
                        else -> colors.glassBorderSubtle
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(chipBg)
                            .border(1.dp, chipBorder, RoundedCornerShape(12.dp))
                            .clickable { selectedCategoryKey = alert.categoryId }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(alert.categoryId),
                            contentDescription = null,
                            tint = if (isCritical) AmberNeon else colors.textPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isArabic) alert.categoryNameAr else alert.categoryNameEn,
                            color = if (isSelected) colors.textPrimary else colors.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                        if (isCritical) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${alert.percentage.toInt()}%",
                                color = AmberNeon,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Highlight Threshold Alert Box (Banner)
            val isWarning = currentAlert.percentage >= 80.0
            val bannerBorderColor = if (isWarning) AmberNeon.copy(alpha = 0.6f) else EmeraldNeon.copy(alpha = 0.3f)
            val bannerBg = if (isWarning) AmberNeon.copy(alpha = 0.12f) else colors.glassSurface.copy(alpha = 0.4f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(bannerBg)
                    .border(1.dp, bannerBorderColor, RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isWarning) Icons.Default.Warning else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (isWarning) AmberNeon else EmeraldNeon,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isArabic) {
                                    if (isWarning) "تنبيه: اقتراب من سقف الميزانية (80%)" else "الميزانية ضمن الحدود الآمنة"
                                } else {
                                    if (isWarning) "Alert: Approaching 80% Budget Cap" else "Budget Within Safe Limits"
                                },
                                color = if (isWarning) AmberNeon else EmeraldNeon,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Target Month Tag
                        Text(
                            text = if (isArabic) "لشهر ${currentAlert.monthNameAr}" else "For ${currentAlert.monthNameEn}",
                            color = colors.textMuted,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Spending Details
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = if (isArabic) "المصروف الفعلي" else "Actual Spent",
                                color = colors.textMuted,
                                fontSize = 11.sp
                            )
                            Text(
                                text = formatDzd(currentAlert.spentAmount, strings.currencyCode, isStealth),
                                color = if (isWarning) AmberNeon else colors.textPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (isArabic) "السقف المحدد" else "Budget Cap",
                                color = colors.textMuted,
                                fontSize = 11.sp
                            )
                            Text(
                                text = formatDzd(currentAlert.budgetCap, strings.currencyCode, isStealth),
                                color = colors.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Progress Bar
                    val progressFloat = (currentAlert.percentage / 100.0).coerceIn(0.0, 1.0).toFloat()
                    LinearProgressIndicator(
                        progress = { progressFloat },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (currentAlert.percentage >= 100.0) RoseNeon else if (currentAlert.percentage >= 80.0) AmberNeon else EmeraldNeon,
                        trackColor = colors.glassSurface.copy(alpha = 0.5f)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${currentAlert.percentage.toInt()}% " + (if (isArabic) "مستهلك" else "used"),
                            color = if (isWarning) AmberNeon else colors.textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = (if (isArabic) "متبقٍ: " else "Remaining: ") +
                                    formatDzd(currentAlert.remainingAmount, strings.currencyCode, isStealth),
                            color = colors.textMuted,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Proactive Advice Box: نصيحة سريعة لتفادي العجز
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.glassCard)
                            .border(1.dp, AmberNeon.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(AmberNeon.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = AmberNeon,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isArabic) "نصيحة سريعة لتفادي العجز" else "Quick Deficit Prevention Advice",
                                    color = AmberNeon,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = if (isArabic) currentAlert.adviceAr else currentAlert.adviceEn,
                                    color = colors.textPrimary,
                                    fontSize = 11.5.sp,
                                    lineHeight = 16.5.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: Send Local Notification & Adjust Cap
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Button 1: Send Local Android Notification (تنبيه النظام)
                GlassButton(
                    text = if (isArabic) "إرسال إشعار للنظام 🔔" else "Send Local Alert 🔔",
                    icon = Icons.Default.Notifications,
                    containerColor = colors.primaryNeon,
                    contentColor = if (colors.isDark) Color(0xFF0F172A) else Color.White,
                    modifier = Modifier.weight(1.3f),
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            pendingAlertToNotify = currentAlert
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            val sent = SmartBudgetAlertManager.sendAlertNotification(context, currentAlert, isArabic)
                            if (sent) {
                                Toast.makeText(
                                    context,
                                    if (isArabic) "تم إرسال إشعار الميزانية الذكي إلى شريط التنبيهات 🔔" else "Smart budget alert notification sent to status bar 🔔",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                )

                // Button 2: Adjust Budget Cap
                GlassButton(
                    text = if (isArabic) "تعديل السقف" else "Set Cap",
                    icon = Icons.Default.Adjust,
                    containerColor = colors.glassCard,
                    contentColor = colors.textPrimary,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        adjustCapInput = currentAlert.budgetCap.toLong().toString()
                        showAdjustCapDialog = true
                    }
                )
            }
        }
    }

    // Modal to Adjust Category Budget Cap
    if (showAdjustCapDialog) {
        Dialog(onDismissRequest = { showAdjustCapDialog = false }) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(24.dp),
                backgroundColor = colors.dialogBackground
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Text(
                        text = if (isArabic) "تعديل سقف ميزانية ${currentAlert.categoryNameAr}" else "Adjust ${currentAlert.categoryNameEn} Budget Cap",
                        color = colors.textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isArabic) "حدد السقف الشهري بالدينار الجزائري لتلقي تنبيهات الـ 80% والنصائح" else "Set monthly limit in DZD to receive 80% threshold warnings & advice",
                        color = colors.textMuted,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = adjustCapInput,
                        onValueChange = { adjustCapInput = it.filter { char -> char.isDigit() } },
                        label = { Text(if (isArabic) "السقف الشهري (دج)" else "Monthly Cap (DZD)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primaryNeon,
                            unfocusedBorderColor = colors.glassBorderSubtle,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        GlassButton(
                            text = strings.cancel,
                            containerColor = colors.glassCard,
                            contentColor = colors.textSecondary,
                            onClick = { showAdjustCapDialog = false }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        GlassButton(
                            text = strings.save,
                            containerColor = colors.primaryNeon,
                            contentColor = if (colors.isDark) Color(0xFF0F172A) else Color.White,
                            onClick = {
                                val newCap = adjustCapInput.toDoubleOrNull() ?: currentAlert.budgetCap
                                if (newCap > 0) {
                                    val updated = customCaps.toMutableMap()
                                    updated[currentAlert.categoryId] = newCap
                                    customCaps = updated
                                    Toast.makeText(
                                        context,
                                        if (isArabic) "تم تحديث سقف الميزانية بنجاح" else "Budget cap updated successfully",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                showAdjustCapDialog = false
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun getCategoryIcon(categoryId: String): ImageVector {
    return when (categoryId) {
        "shopping" -> Icons.Default.ShoppingCart
        "food" -> Icons.Default.Fastfood
        "bills" -> Icons.Default.Receipt
        "transport" -> Icons.Default.Wifi
        else -> Icons.Default.Receipt
    }
}
