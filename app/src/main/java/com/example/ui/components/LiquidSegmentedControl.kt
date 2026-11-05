package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassTheme

/**
 * Modern, fluid, animated segmented control for Theme Appearance:
 * Supports 3 dynamic states:
 * - "DARK" -> Dark Glass
 * - "LIGHT" -> Light Crystal
 * - "SYSTEM" -> System Default (dynamically tracks OS system theme)
 *
 * Engineered with non-overlapping geometry, fluid sliding pill, and crisp contrast.
 */
@Composable
fun LiquidThemeSegmentedControl(
    currentThemeMode: String,
    onSelectThemeMode: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors
    val isDark = colors.isDark

    val normalizedMode = currentThemeMode.uppercase()
    val selectedIndex = when (normalizedMode) {
        "LIGHT" -> 1
        "SYSTEM" -> 2
        else -> 0 // "DARK"
    }

    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "ThemeSegmentSlide"
    )

    val activeIndicatorBrush = if (isDark) {
        Brush.horizontalGradient(
            listOf(
                colors.primaryNeon.copy(alpha = 0.38f),
                colors.secondaryNeon.copy(alpha = 0.28f)
            )
        )
    } else {
        Brush.horizontalGradient(
            listOf(
                Color(0xFF0284C7).copy(alpha = 0.24f),
                Color(0xFF2563EB).copy(alpha = 0.18f)
            )
        )
    }

    val activeBorderColor = if (isDark) {
        colors.primaryNeon.copy(alpha = 0.65f)
    } else {
        Color(0xFF0284C7).copy(alpha = 0.55f)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.glassCard.copy(alpha = if (isDark) 0.65f else 0.90f))
            .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(14.dp))
            .padding(3.dp)
    ) {
        val segmentWidth = maxWidth / 3

        // Animated Sliding Indicator Pill
        Box(
            modifier = Modifier
                .offset(x = segmentWidth * animatedIndex)
                .width(segmentWidth)
                .fillMaxHeight()
                .shadow(elevation = 3.dp, shape = RoundedCornerShape(11.dp))
                .clip(RoundedCornerShape(11.dp))
                .background(activeIndicatorBrush)
                .border(1.2.dp, activeBorderColor, RoundedCornerShape(11.dp))
        )

        // 3 Segment Items
        Row(modifier = Modifier.fillMaxSize()) {
            ThemeSegmentOption(
                text = strings.themeDark,
                icon = Icons.Default.DarkMode,
                isSelected = selectedIndex == 0,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = { onSelectThemeMode("DARK") }
            )
            ThemeSegmentOption(
                text = strings.themeLight,
                icon = Icons.Default.LightMode,
                isSelected = selectedIndex == 1,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = { onSelectThemeMode("LIGHT") }
            )
            ThemeSegmentOption(
                text = strings.themeSystem,
                icon = Icons.Default.BrightnessAuto,
                isSelected = selectedIndex == 2,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = { onSelectThemeMode("SYSTEM") }
            )
        }
    }
}

@Composable
private fun ThemeSegmentOption(
    text: String,
    icon: ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = LiquidGlassTheme.colors
    val textColor by animateColorAsState(
        targetValue = if (isSelected) colors.textPrimary else colors.textMuted,
        animationSpec = tween(durationMillis = 200),
        label = "ThemeTextColor"
    )
    val iconTint by animateColorAsState(
        targetValue = if (isSelected) colors.primaryNeon else colors.textMuted,
        animationSpec = tween(durationMillis = 200),
        label = "ThemeIconColor"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = colors.primaryNeon),
                onClick = onClick
            )
            .padding(horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                color = textColor,
                fontSize = 11.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Modern, fluid, animated segmented control for Language (English / العربية).
 * Provides responsive sliding indicators, dynamic highlight styling, and tactile feedback.
 */
@Composable
fun LiquidLanguageSegmentedControl(
    currentLanguage: String,
    onSelectLanguage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LiquidGlassTheme.colors
    val isDark = colors.isDark
    val isArabic = currentLanguage == "ar"
    val selectedIndex = if (isArabic) 1 else 0

    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "LangSegmentSlide"
    )

    val activeIndicatorBrush = if (isDark) {
        Brush.horizontalGradient(
            listOf(
                EmeraldNeon.copy(alpha = 0.35f),
                colors.primaryNeon.copy(alpha = 0.24f)
            )
        )
    } else {
        Brush.horizontalGradient(
            listOf(
                Color(0xFF059669).copy(alpha = 0.22f),
                Color(0xFF0284C7).copy(alpha = 0.16f)
            )
        )
    }

    val activeBorderColor = if (isDark) {
        EmeraldNeon.copy(alpha = 0.60f)
    } else {
        Color(0xFF059669).copy(alpha = 0.50f)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.glassCard.copy(alpha = if (isDark) 0.65f else 0.90f))
            .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(14.dp))
            .padding(3.dp)
    ) {
        val segmentWidth = maxWidth / 2

        // Animated Sliding Indicator Pill
        Box(
            modifier = Modifier
                .offset(x = segmentWidth * animatedIndex)
                .width(segmentWidth)
                .fillMaxHeight()
                .shadow(elevation = 3.dp, shape = RoundedCornerShape(11.dp))
                .clip(RoundedCornerShape(11.dp))
                .background(activeIndicatorBrush)
                .border(1.2.dp, activeBorderColor, RoundedCornerShape(11.dp))
        )

        // Language Options Row
        Row(modifier = Modifier.fillMaxSize()) {
            LanguageSegmentItem(
                label = "English",
                subLabel = "EN",
                isSelected = !isArabic,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = { onSelectLanguage("en") }
            )
            LanguageSegmentItem(
                label = "العربية",
                subLabel = "AR",
                isSelected = isArabic,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = { onSelectLanguage("ar") }
            )
        }
    }
}

@Composable
private fun LanguageSegmentItem(
    label: String,
    subLabel: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = LiquidGlassTheme.colors
    val textColor by animateColorAsState(
        targetValue = if (isSelected) colors.textPrimary else colors.textMuted,
        animationSpec = tween(durationMillis = 200),
        label = "LangTextColor"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = EmeraldNeon),
                onClick = onClick
            )
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                color = textColor,
                fontSize = 12.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) EmeraldNeon.copy(alpha = 0.22f) else colors.glassBorderSubtle.copy(alpha = 0.35f))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = subLabel,
                    color = if (isSelected) EmeraldNeon else colors.textMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
