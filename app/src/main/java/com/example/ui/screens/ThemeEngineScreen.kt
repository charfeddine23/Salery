package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPill
import com.example.ui.components.LocalEdgeFadeOuterPadding
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.ArabicStrings
import com.example.ui.theme.AppThemeSpec
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.LocalAdaptiveDimens
import com.example.ui.theme.LocalAdaptiveLayoutInfo
import com.example.ui.theme.LocalAdaptiveTypography
import com.example.ui.theme.ThemeCategory
import com.example.ui.theme.ThemeEngineCatalog
import com.example.ui.theme.ThemeSurfaceEffect

@Composable
fun ThemeEngineScreen(
    currentDesignStyle: String,
    currentThemeMode: String,
    onSelectDesignStyle: (String) -> Unit,
    onSelectThemeMode: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current
    val isArabic = strings is ArabicStrings
    val colors = LiquidGlassTheme.colors
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current
    val layoutInfo = LocalAdaptiveLayoutInfo.current

    var selectedCategory by remember { mutableStateOf(ThemeCategory.ALL) }
    var searchQuery by remember { mutableStateOf("") }

    val activeSpec = remember(currentDesignStyle) {
        ThemeEngineCatalog.findById(currentDesignStyle)
    }

    val filteredThemes = remember(selectedCategory, searchQuery) {
        val query = searchQuery.trim().lowercase()
        ThemeEngineCatalog.allThemes.filter { spec ->
            val matchesCategory = selectedCategory == ThemeCategory.ALL || spec.category == selectedCategory
            val matchesQuery = query.isEmpty() ||
                spec.nameEn.lowercase().contains(query) ||
                spec.nameAr.lowercase().contains(query) ||
                spec.taglineEn.lowercase().contains(query) ||
                spec.taglineAr.lowercase().contains(query)
            matchesCategory && matchesQuery
        }
    }

    val gridCols = layoutInfo.gridColumns.coerceAtLeast(1)
    val chunkedRows = remember(filteredThemes, gridCols) {
        filteredThemes.chunked(gridCols)
    }

    val outerEdgePadding = LocalEdgeFadeOuterPadding.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("theme_engine_screen"),
        contentPadding = PaddingValues(
            start = dimens.screenPaddingH,
            end = dimens.screenPaddingH,
            top = outerEdgePadding.calculateTopPadding() + 12.dp,
            bottom = outerEdgePadding.calculateBottomPadding() + 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Active Theme Live Showcase Hero Card
        item {
            ActiveThemeHeroCard(
                activeSpec = activeSpec,
                isDarkPreview = colors.isDark,
                currentThemeMode = currentThemeMode,
                isArabic = isArabic,
                onSelectThemeMode = onSelectThemeMode
            )
        }

        // Search Bar
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                backgroundColor = colors.glassSurface,
                breathingGlow = false
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = colors.primaryNeon,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = if (isArabic) "ابحث من بين 32 ثيماً..." else "Search 32 studio themes...",
                                color = colors.textMuted,
                                fontSize = 14.sp
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = colors.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(colors.primaryNeon),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("theme_search_input")
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = if (isArabic) "مسح البحث" else "Clear search",
                                tint = colors.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Category Filter Pills
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(ThemeCategory.entries) { category ->
                    val count = remember(category) {
                        if (category == ThemeCategory.ALL) ThemeEngineCatalog.allThemes.size
                        else ThemeEngineCatalog.allThemes.count { it.category == category }
                    }
                    val label = if (isArabic) {
                        "${category.labelAr} ($count)"
                    } else {
                        "${category.labelEn} ($count)"
                    }
                    GlassPill(
                        text = label,
                        isSelected = selectedCategory == category,
                        onClick = { selectedCategory = category }
                    )
                }
            }
        }

        // Section count caption
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                    Text(
                        text = if (isArabic) {
                            "معرض الثيمات (${filteredThemes.size} من ${ThemeEngineCatalog.allThemes.size})"
                        } else {
                            "Theme Gallery (${filteredThemes.size} of ${ThemeEngineCatalog.allThemes.size})"
                        },
                        color = colors.textSecondary,
                        fontSize = typo.bodySm,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isArabic) "معاينة فورية" else "Instant Live Apply",
                        color = colors.primaryNeon,
                        fontSize = typo.bodyXs,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }

            if (chunkedRows.isEmpty()) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(dimens.cornerRadiusLg)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(dimens.cardPaddingLarge),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = colors.primaryNeon,
                                modifier = Modifier.size(dimens.iconSizeXl)
                            )
                            Spacer(modifier = Modifier.height(dimens.spacingSm))
                            Text(
                                text = if (isArabic) "لم يتم العثور على ثيم مطابق" else "No matching themes found",
                                color = colors.textPrimary,
                                fontSize = typo.bodyLg,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                items(
                    items = chunkedRows,
                    key = { row -> "${gridCols}_${row.joinToString("_") { it.id }}" }
                ) { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(dimens.spacingMd)
                    ) {
                        rowItems.forEach { spec ->
                            ThemeEngineGridCard(
                                spec = spec,
                                isSelected = spec.id == activeSpec.id,
                                isDarkPreview = colors.isDark,
                                isArabic = isArabic,
                                onSelect = { onSelectDesignStyle(spec.id) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        val emptySlots = (gridCols - rowItems.size).coerceAtLeast(0)
                        repeat(emptySlots) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveThemeHeroCard(
    activeSpec: AppThemeSpec,
    isDarkPreview: Boolean,
    currentThemeMode: String,
    isArabic: Boolean,
    onSelectThemeMode: (String) -> Unit
) {
    val colors = LiquidGlassTheme.colors
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimens.cornerRadiusXl),
        borderColor = colors.primaryNeon.copy(alpha = 0.65f),
        backgroundColor = colors.glassCard
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimens.cardPadding),
            verticalArrangement = Arrangement.spacedBy(dimens.spacingMd)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(dimens.cornerRadiusMd))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(colors.primaryNeon, colors.secondaryNeon)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val iconTint = if (colors.primaryNeon.luminance() > 0.48f) Color(0xFF09111E) else Color.White
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(dimens.iconSizeMd)
                    )
                }
                Spacer(modifier = Modifier.width(dimens.spacingMd))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isArabic) activeSpec.nameAr else activeSpec.nameEn,
                        color = colors.textPrimary,
                        fontSize = typo.title,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isArabic) activeSpec.category.labelAr else activeSpec.nameAr,
                        color = colors.primaryNeon,
                        fontSize = typo.bodySm,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(dimens.spacingSm))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(dimens.cornerRadiusSm))
                        .background(colors.primaryNeon.copy(alpha = 0.18f))
                        .border(1.dp, colors.primaryNeon.copy(alpha = 0.5f), RoundedCornerShape(dimens.cornerRadiusSm))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "${ThemeEngineCatalog.allThemes.size} ${if (isArabic) "ثيم" else "Themes"}",
                        color = colors.primaryNeon,
                        fontSize = typo.bodyXs,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }

            Text(
                text = if (isArabic) activeSpec.taglineAr else activeSpec.taglineEn,
                color = colors.textSecondary,
                fontSize = typo.body
            )

            // Palette & Quick Mode Switch FlowRow (prevents overlap on compact phones)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PaletteSwatchDot(colors.bgCenter, colors.glassBorder)
                    PaletteSwatchDot(colors.glassCard, colors.glassBorder)
                    PaletteSwatchDot(colors.primaryNeon, colors.primaryNeon)
                    PaletteSwatchDot(colors.secondaryNeon, colors.secondaryNeon)
                    PaletteSwatchDot(colors.successColor, colors.successColor)
                }

                // Quick Dark / Light / Auto toggle buttons for instant testing
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ModeMiniChip(
                        icon = Icons.Default.DarkMode,
                        label = if (isArabic) "داكن" else "Dark",
                        isSelected = currentThemeMode == "DARK" || (currentThemeMode == "SYSTEM" && isDarkPreview),
                        onClick = { onSelectThemeMode("DARK") }
                    )
                    ModeMiniChip(
                        icon = Icons.Default.LightMode,
                        label = if (isArabic) "فاتح" else "Light",
                        isSelected = currentThemeMode == "LIGHT" || (currentThemeMode == "SYSTEM" && !isDarkPreview),
                        onClick = { onSelectThemeMode("LIGHT") }
                    )
                    ModeMiniChip(
                        icon = Icons.Default.SettingsBrightness,
                        label = if (isArabic) "تلقائي" else "Auto",
                        isSelected = currentThemeMode == "SYSTEM",
                        onClick = { onSelectThemeMode("SYSTEM") }
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeMiniChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = LiquidGlassTheme.colors
    val typo = LocalAdaptiveTypography.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) colors.primaryNeon.copy(alpha = 0.22f)
                else colors.glassSurface
            )
            .border(
                1.dp,
                if (isSelected) colors.primaryNeon else colors.glassBorderSubtle,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) colors.primaryNeon else colors.textMuted,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = label,
            color = if (isSelected) colors.primaryNeon else colors.textSecondary,
            fontSize = typo.bodyXs,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun PaletteSwatchDot(color: Color, border: Color) {
    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, border.copy(alpha = 0.65f), CircleShape)
    )
}

@Composable
private fun ThemeEngineGridCard(
    spec: AppThemeSpec,
    isSelected: Boolean,
    isDarkPreview: Boolean,
    isArabic: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appColors = LiquidGlassTheme.colors
    val preview = remember(spec, isDarkPreview) { spec.colors(isDarkPreview) }
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) appColors.primaryNeon else appColors.glassBorderSubtle,
        animationSpec = tween(220),
        label = "theme_card_border"
    )
    val cardRadius = if (spec.surfaceEffect == ThemeSurfaceEffect.BRUTALIST_LUXURY) 10.dp else 20.dp

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(cardRadius))
            .background(appColors.glassCard)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(cardRadius)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = preview.primaryNeon),
                onClick = onSelect
            )
            .testTag("theme_card_${spec.id}")
            .padding(10.dp)
    ) {
        // Live Miniature Theme Canvas Preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(preview.bgStart, preview.bgCenter, preview.bgEnd)
                    )
                )
                .border(1.dp, preview.glassBorder.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
        ) {
            val isRtlPreview = LocalLayoutDirection.current == LayoutDirection.Rtl
            Canvas(modifier = Modifier.fillMaxSize()) {
                fun mx(fraction: Float): Float = if (isRtlPreview) size.width * (1f - fraction) else size.width * fraction
                when (spec.surfaceEffect) {
                    ThemeSurfaceEffect.BLUEPRINT_GRID -> {
                        val step = 16.dp.toPx()
                        var x = 0f
                        while (x < size.width) {
                            drawLine(
                                color = preview.primaryNeon.copy(alpha = 0.18f),
                                start = Offset(x, 0f),
                                end = Offset(x, size.height),
                                strokeWidth = 0.8f
                            )
                            x += step
                        }
                        var y = 0f
                        while (y < size.height) {
                            drawLine(
                                color = preview.primaryNeon.copy(alpha = 0.18f),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 0.8f
                            )
                            y += step
                        }
                    }
                    ThemeSurfaceEffect.NEUMORPHIC -> {
                        // Subtle highlight/shadow accents
                        drawCircle(
                            color = Color.White.copy(alpha = if (preview.isDark) 0.06f else 0.45f),
                            radius = size.minDimension * 0.45f,
                            center = Offset(mx(0.2f), size.height * 0.25f)
                        )
                    }
                    else -> {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    preview.primaryNeon.copy(alpha = 0.34f),
                                    Color.Transparent
                                ),
                                center = Offset(mx(0.22f), size.height * 0.24f),
                                radius = size.minDimension * 0.72f
                            )
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    preview.secondaryNeon.copy(alpha = 0.28f),
                                    Color.Transparent
                                ),
                                center = Offset(mx(0.85f), size.height * 0.8f),
                                radius = size.minDimension * 0.65f
                            )
                        )
                    }
                }
            }

            // Mini UI card inside the preview
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(preview.primaryNeon)
                        )
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(preview.secondaryNeon)
                        )
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(preview.successColor)
                        )
                    }
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(preview.primaryNeon),
                            contentAlignment = Alignment.Center
                        ) {
                            val checkTint = if (preview.primaryNeon.luminance() > 0.45f) Color(0xFF0A1220) else Color.White
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = if (isArabic) "مفعّل" else "Active",
                                tint = checkTint,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    } else if (spec.isExistingClassic) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(preview.glassSurface)
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isArabic) "أساسي" else "Core",
                                color = preview.textSecondary,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Mini card bar mockup
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .clip(RoundedCornerShape(if (spec.surfaceEffect == ThemeSurfaceEffect.BRUTALIST_LUXURY) 5.dp else 10.dp))
                        .background(preview.glassCard)
                        .border(
                            BorderStroke(1.dp, preview.glassBorder.copy(alpha = 0.75f)),
                            RoundedCornerShape(if (spec.surfaceEffect == ThemeSurfaceEffect.BRUTALIST_LUXURY) 5.dp else 10.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .width(46.dp)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(preview.textPrimary.copy(alpha = 0.9f))
                            )
                            Box(
                                modifier = Modifier
                                    .width(28.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(preview.textSecondary.copy(alpha = 0.55f))
                            )
                        }
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(preview.primaryNeon)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(9.dp))

        // Theme Title (Primary locale)
        val typo = LocalAdaptiveTypography.current
        Text(
            text = if (isArabic) spec.nameAr else spec.nameEn,
            color = appColors.textPrimary,
            fontSize = typo.body,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Secondary name + active check
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (isArabic) spec.category.labelAr else spec.nameAr,
                color = if (isSelected) appColors.primaryNeon else appColors.textMuted,
                fontSize = typo.bodyXs,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = if (isArabic) "مفعّل" else "Active",
                    tint = appColors.primaryNeon,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
