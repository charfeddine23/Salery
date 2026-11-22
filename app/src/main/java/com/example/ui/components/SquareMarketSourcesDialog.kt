package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import com.example.market.SquareMarketCatalog
import com.example.market.SquareMarketSourceSpec
import com.example.market.SquareSourceCategory
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.isolateLtr
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassColors
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.LocalAdaptiveDimens
import com.example.ui.theme.LocalAdaptiveTypography
import com.example.ui.theme.LocalScreenshotProtection
import com.example.ui.theme.RoseNeon

@Composable
fun SquarePriceSourcesBottomButton(
    enabledCount: Int,
    totalCount: Int,
    syncStatusFooter: String,
    isArabic: Boolean,
    colors: LiquidGlassColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 58.dp)
            .clip(RoundedCornerShape(dimens.cornerRadiusLg))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = EmeraldNeon),
                role = Role.Button,
                onClick = onClick
            )
            .testTag("square_sources_button"),
        backgroundColor = colors.glassCard,
        borderColor = EmeraldNeon.copy(alpha = 0.36f),
        shape = RoundedCornerShape(dimens.cornerRadiusLg),
        cornerRadius = dimens.cornerRadiusLg,
        breathingGlow = false
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                EmeraldNeon.copy(alpha = 0.22f),
                                CyanNeon.copy(alpha = 0.14f)
                            )
                        )
                    )
                    .border(0.9.dp, EmeraldNeon.copy(alpha = 0.42f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Public,
                    contentDescription = null,
                    tint = EmeraldNeon,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isArabic) "مواقع تحديث الأسعار" else "Price Update Websites",
                    color = colors.textPrimary,
                    fontSize = typo.bodyLg,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                val activeText = if (isArabic) {
                    "${isolateLtr("$enabledCount/$totalCount")} مواقع مفعّلة"
                } else {
                    "$enabledCount/$totalCount active sources"
                }
                val subline = if (syncStatusFooter.isNotBlank()) {
                    "$activeText • $syncStatusFooter"
                } else {
                    activeText
                }
                Text(
                    text = subline,
                    color = colors.textSecondary,
                    fontSize = typo.bodyXs,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(EmeraldNeon.copy(alpha = 0.14f))
                    .border(0.8.dp, EmeraldNeon.copy(alpha = 0.36f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = EmeraldNeon,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = if (isArabic) "اختيار" else "Sources",
                    color = EmeraldNeon,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = EmeraldNeon,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SquareMarketSourcesDialog(
    initialSelectedIds: Set<String>,
    intervalLabel: String,
    isArabic: Boolean,
    onSaveSelectedIds: (Set<String>) -> Unit,
    onOpenSyncIntervalSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors
    val isDark = colors.isDark
    val allSources = SquareMarketCatalog.availableSources
    val allIds = SquareMarketCatalog.ALL_SOURCE_IDS

    var selectedIds by remember(initialSelectedIds) {
        mutableStateOf(
            initialSelectedIds.filter { it in allIds }.toSet().ifEmpty { allIds }
        )
    }
    var minOneWarning by remember { mutableStateOf(false) }
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }

    val cardScale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.92f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "square_sources_dialog_scale"
    )

    val toggleSource: (String) -> Unit = { id ->
        minOneWarning = false
        if (id in selectedIds) {
            if (selectedIds.size <= 1) {
                minOneWarning = true
            } else {
                selectedIds = selectedIds - id
            }
        } else {
            selectedIds = selectedIds + id
        }
    }

    val isScreenshotProtected = LocalScreenshotProtection.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            securePolicy = if (isScreenshotProtected) SecureFlagPolicy.SecureOn else SecureFlagPolicy.SecureOff
        )
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 460.dp)
                        .heightIn(max = 720.dp)
                        .scale(cardScale)
                        .testTag("square_sources_dialog"),
                    shape = RoundedCornerShape(28.dp),
                    backgroundColor = if (isDark) Color(0xFF0E1522).copy(alpha = 0.97f) else colors.dialogBackground,
                    borderColor = EmeraldNeon.copy(alpha = 0.48f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                val orbX = if (isArabic) size.width * 0.2f else size.width * 0.8f
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            EmeraldNeon.copy(alpha = if (isDark) 0.14f else 0.08f),
                                            SquareGoldAccent.copy(alpha = if (isDark) 0.08f else 0.05f),
                                            Color.Transparent
                                        ),
                                        center = Offset(orbX, 0f),
                                        radius = size.width * 0.72f
                                    )
                                )
                            }
                            .padding(horizontal = 18.dp, vertical = 16.dp)
                    ) {
                        // Top bar: Close + Emblem + Sync Timer shortcut
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("square_sources_close_button")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(colors.glassCard)
                                        .border(0.8.dp, colors.glassBorderSubtle, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = strings.cancel,
                                        tint = colors.textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                EmeraldNeon.copy(alpha = 0.22f),
                                                SquareGoldAccent.copy(alpha = 0.16f)
                                            )
                                        )
                                    )
                                    .border(1.dp, EmeraldNeon.copy(alpha = 0.45f), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = null,
                                    tint = EmeraldNeon,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(SquareGoldAccent.copy(alpha = 0.14f))
                                    .border(0.9.dp, SquareGoldAccent.copy(alpha = 0.42f), RoundedCornerShape(50))
                                    .clickable {
                                        onDismiss()
                                        onOpenSyncIntervalSettings()
                                    }
                                    .padding(horizontal = 11.dp, vertical = 8.dp)
                                    .testTag("square_sources_open_timer_button"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = SquareGoldAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = intervalLabel,
                                    color = SquareGoldAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (isArabic) "مواقع تحديث الأسعار" else "Price Update Websites",
                            color = colors.textPrimary,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (isArabic) {
                                "اختر المواقع التي يتم تحديث أسعار الذهب والعملات والسكوار منها"
                            } else {
                                "Select which websites update Gold, Bank FX & Square rates"
                            },
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Summary & Select All bar
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(colors.glassSurface.copy(alpha = 0.65f))
                                    .border(0.8.dp, colors.glassBorderSubtle, RoundedCornerShape(50))
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (isArabic) {
                                        "المفعّل: ${isolateLtr("${selectedIds.size}/${allSources.size}")}"
                                    } else {
                                        "Active: ${selectedIds.size}/${allSources.size}"
                                    },
                                    color = colors.textPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            val allSelected = selectedIds.size == allIds.size
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (allSelected) EmeraldNeon.copy(alpha = 0.16f)
                                        else CyanNeon.copy(alpha = 0.14f)
                                    )
                                    .border(
                                        0.9.dp,
                                        if (allSelected) EmeraldNeon.copy(alpha = 0.45f)
                                        else CyanNeon.copy(alpha = 0.42f),
                                        RoundedCornerShape(50)
                                    )
                                    .clickable {
                                        minOneWarning = false
                                        selectedIds = allIds
                                    }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                                    .testTag("square_sources_select_all_button"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = null,
                                    tint = if (allSelected) EmeraldNeon else CyanNeon,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (isArabic) "تحديد الكل" else "Select All",
                                    color = if (allSelected) EmeraldNeon else CyanNeon,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (minOneWarning) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isArabic) "يجب إبقاء موقع واحد على الأقل مفعّلاً" else "At least 1 website must stay enabled",
                                color = RoseNeon,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Scrollable grouped sources list
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val grouped = listOf(
                                Triple(
                                    SquareSourceCategory.PARALLEL,
                                    if (isArabic) "مواقع السكوار والسوق الموازي" else "Parallel & Square Market",
                                    EmeraldNeon
                                ),
                                Triple(
                                    SquareSourceCategory.GOLD,
                                    if (isArabic) "مواقع أسعار الذهب العالمية" else "Global Gold Spot",
                                    SquareGoldAccent
                                ),
                                Triple(
                                    SquareSourceCategory.BANK,
                                    if (isArabic) "مواقع البنوك والعملات الرسمية" else "Official Bank & FX",
                                    CyanNeon
                                )
                            )

                            grouped.forEach { (category, headerTitle, accent) ->
                                val categoryItems = allSources.filter { it.category == category }
                                val activeInCat = categoryItems.count { it.id in selectedIds }

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(7.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(accent)
                                            )
                                            Text(
                                                text = headerTitle,
                                                color = accent,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                        Text(
                                            text = isolateLtr("$activeInCat/${categoryItems.size}"),
                                            color = colors.textMuted,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    categoryItems.forEach { spec ->
                                        SquareSourceToggleRow(
                                            spec = spec,
                                            isSelected = spec.id in selectedIds,
                                            accentColor = accent,
                                            isArabic = isArabic,
                                            colors = colors,
                                            onToggle = { toggleSource(spec.id) }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Bottom action bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(0.9f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(colors.glassCard)
                                    .border(0.9.dp, colors.glassBorderSubtle, RoundedCornerShape(50))
                                    .clickable(onClick = onDismiss),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = strings.cancel,
                                    color = colors.textSecondary,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1.35f)
                                    .height(48.dp)
                                    .shadow(
                                        elevation = 8.dp,
                                        shape = RoundedCornerShape(50),
                                        spotColor = EmeraldNeon.copy(alpha = 0.45f)
                                    )
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(EmeraldNeon, SquareGoldAccent)
                                        )
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(color = Color.White),
                                        onClick = {
                                            onSaveSelectedIds(selectedIds)
                                            onDismiss()
                                        }
                                    )
                                    .testTag("square_sources_save_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF0A1220),
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Text(
                                        text = if (isArabic) "حفظ وتحديث" else "Save & Sync",
                                        color = Color(0xFF0A1220),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold
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

@Composable
private fun SquareSourceToggleRow(
    spec: SquareMarketSourceSpec,
    isSelected: Boolean,
    accentColor: Color,
    isArabic: Boolean,
    colors: LiquidGlassColors,
    onToggle: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) accentColor.copy(alpha = 0.12f) else colors.glassCard,
        label = "source_row_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) accentColor.copy(alpha = 0.50f) else colors.glassBorderSubtle,
        label = "source_row_border"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(width = if (isSelected) 1.2.dp else 0.8.dp, color = borderColor, shape = RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = accentColor),
                role = Role.Switch,
                onClick = onToggle
            )
            .padding(horizontal = 12.dp, vertical = 9.dp)
            .testTag("square_source_row_${spec.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (isArabic) spec.nameAr else spec.nameEn,
                    color = colors.textPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (spec.isNewMajor) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AmberNeon.copy(alpha = 0.20f))
                            .border(0.7.dp, AmberNeon.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isArabic) "جديد" else "NEW",
                            color = AmberNeon,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                    }
                }

                val secondaryTag = if (isArabic) spec.secondaryTagAr else spec.secondaryTagEn
                if (secondaryTag.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.glassSurface.copy(alpha = 0.65f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = secondaryTag,
                            color = colors.textSecondary,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = isolateLtr(spec.domain),
                color = colors.textMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Switch(
            checked = isSelected,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF0A1220),
                checkedTrackColor = accentColor,
                uncheckedThumbColor = colors.textMuted,
                uncheckedTrackColor = colors.glassSurface
            )
        )
    }
}
