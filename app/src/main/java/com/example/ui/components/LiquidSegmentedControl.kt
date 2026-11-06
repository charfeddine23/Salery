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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.LiquidGlassTheme

/**
 * Clean, modern, flat segmented control for Theme Appearance:
 * Removed heavy glass/skeuomorphic bevels and harsh borders.
 * Styled with a smooth, neutral track and an elegant floating pill with soft drop-shadow.
 *
 * Supports 3 dynamic states:
 * - "DARK" -> Dark
 * - "LIGHT" -> Light
 * - "SYSTEM" -> System Default
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
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "ThemeSegmentSlide"
    )

    // Flat, clean track colors (no thick glass borders)
    val trackBgColor = if (isDark) Color(0xFF0F172A).copy(alpha = 0.85f) else Color(0xFFF1F5F9)
    val trackBorderColor = if (isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFE2E8F0)

    // Clean, modern floating pill (solid, refined, flat)
    val pillBgColor = if (isDark) Color(0xFF1E293B) else Color.White
    val pillBorderColor = if (isDark) Color(0xFF475569).copy(alpha = 0.6f) else Color(0xFFCBD5E1).copy(alpha = 0.5f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(trackBgColor)
            .border(0.8.dp, trackBorderColor, RoundedCornerShape(12.dp))
            .padding(3.dp)
    ) {
        val segmentWidth = maxWidth / 3

        // Animated Sliding Pill
        Box(
            modifier = Modifier
                .offset(x = segmentWidth * animatedIndex)
                .width(segmentWidth)
                .fillMaxHeight()
                .shadow(
                    elevation = if (isDark) 1.dp else 2.dp,
                    shape = RoundedCornerShape(9.dp),
                    spotColor = Color.Black.copy(alpha = if (isDark) 0.3f else 0.08f)
                )
                .clip(RoundedCornerShape(9.dp))
                .background(pillBgColor)
                .border(0.8.dp, pillBorderColor, RoundedCornerShape(9.dp))
        )

        // 3 Segment Items
        Row(modifier = Modifier.fillMaxSize()) {
            ThemeSegmentOption(
                text = strings.themeDark,
                icon = Icons.Default.DarkMode,
                isSelected = selectedIndex == 0,
                isDarkTheme = isDark,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = { onSelectThemeMode("DARK") }
            )
            ThemeSegmentOption(
                text = strings.themeLight,
                icon = Icons.Default.LightMode,
                isSelected = selectedIndex == 1,
                isDarkTheme = isDark,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = { onSelectThemeMode("LIGHT") }
            )
            ThemeSegmentOption(
                text = strings.themeSystem,
                icon = Icons.Default.BrightnessAuto,
                isSelected = selectedIndex == 2,
                isDarkTheme = isDark,
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
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val selectedTextColor = if (isDarkTheme) Color.White else Color(0xFF0F172A)
    val unselectedTextColor = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
    val selectedIconColor = if (isDarkTheme) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val unselectedIconColor = if (isDarkTheme) Color(0xFF64748B) else Color(0xFF94A3B8)

    val textColor by animateColorAsState(
        targetValue = if (isSelected) selectedTextColor else unselectedTextColor,
        animationSpec = tween(durationMillis = 200),
        label = "ThemeTextColor"
    )
    val iconTint by animateColorAsState(
        targetValue = if (isSelected) selectedIconColor else unselectedIconColor,
        animationSpec = tween(durationMillis = 200),
        label = "ThemeIconColor"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = selectedIconColor.copy(alpha = 0.2f)),
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
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = text,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Clean, modern, flat segmented control for Language (English / العربية):
 * Removed heavy glass/skeuomorphic borders and weird inner nested badges.
 * Presents a refined, clean text switch with smooth sliding pill.
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
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "LangSegmentSlide"
    )

    // Flat, clean track colors (no thick glass borders)
    val trackBgColor = if (isDark) Color(0xFF0F172A).copy(alpha = 0.85f) else Color(0xFFF1F5F9)
    val trackBorderColor = if (isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFE2E8F0)

    // Clean, modern floating pill (solid, refined, flat)
    val pillBgColor = if (isDark) Color(0xFF1E293B) else Color.White
    val pillBorderColor = if (isDark) Color(0xFF475569).copy(alpha = 0.6f) else Color(0xFFCBD5E1).copy(alpha = 0.5f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(trackBgColor)
            .border(0.8.dp, trackBorderColor, RoundedCornerShape(12.dp))
            .padding(3.dp)
    ) {
        val segmentWidth = maxWidth / 2

        // Animated Sliding Pill
        Box(
            modifier = Modifier
                .offset(x = segmentWidth * animatedIndex)
                .width(segmentWidth)
                .fillMaxHeight()
                .shadow(
                    elevation = if (isDark) 1.dp else 2.dp,
                    shape = RoundedCornerShape(9.dp),
                    spotColor = Color.Black.copy(alpha = if (isDark) 0.3f else 0.08f)
                )
                .clip(RoundedCornerShape(9.dp))
                .background(pillBgColor)
                .border(0.8.dp, pillBorderColor, RoundedCornerShape(9.dp))
        )

        // Language Options Row (Clean Typography, no nested chaotic badges)
        Row(modifier = Modifier.fillMaxSize()) {
            LanguageSegmentOption(
                label = "English",
                isSelected = !isArabic,
                isDarkTheme = isDark,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = { onSelectLanguage("en") }
            )
            LanguageSegmentOption(
                label = "العربية",
                isSelected = isArabic,
                isDarkTheme = isDark,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = { onSelectLanguage("ar") }
            )
        }
    }
}

@Composable
private fun LanguageSegmentOption(
    label: String,
    isSelected: Boolean,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val selectedTextColor = if (isDarkTheme) Color.White else Color(0xFF0F172A)
    val unselectedTextColor = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)

    val textColor by animateColorAsState(
        targetValue = if (isSelected) selectedTextColor else unselectedTextColor,
        animationSpec = tween(durationMillis = 200),
        label = "LangTextColor"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = Color(0xFF10B981).copy(alpha = 0.2f)),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Clean segmented control for Design Style:
 * Allows user to toggle between "Liquid Glass" and "Neumorphism".
 */
@Composable
fun LiquidDesignStyleSegmentedControl(
    currentDesignStyle: String,
    onSelectDesignStyle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors
    val isDark = colors.isDark
    val isNeumorphic = currentDesignStyle == "NEUMORPHISM"
    val selectedIndex = if (isNeumorphic) 1 else 0

    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "DesignStyleSlide"
    )

    val trackBgColor = if (isDark) Color(0xFF0F172A).copy(alpha = 0.85f) else Color(0xFFF1F5F9)
    val trackBorderColor = if (isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFE2E8F0)

    val pillBgColor = if (isDark) Color(0xFF1E293B) else Color.White
    val pillBorderColor = if (isDark) Color(0xFF475569).copy(alpha = 0.6f) else Color(0xFFCBD5E1).copy(alpha = 0.5f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(trackBgColor)
            .border(0.8.dp, trackBorderColor, RoundedCornerShape(12.dp))
            .padding(3.dp)
    ) {
        val segmentWidth = maxWidth / 2

        Box(
            modifier = Modifier
                .offset(x = segmentWidth * animatedIndex)
                .width(segmentWidth)
                .fillMaxHeight()
                .shadow(
                    elevation = if (isDark) 1.dp else 2.dp,
                    shape = RoundedCornerShape(9.dp),
                    spotColor = Color.Black.copy(alpha = if (isDark) 0.3f else 0.08f)
                )
                .clip(RoundedCornerShape(9.dp))
                .background(pillBgColor)
                .border(0.8.dp, pillBorderColor, RoundedCornerShape(9.dp))
        )

        Row(modifier = Modifier.fillMaxSize()) {
            DesignStyleOption(
                label = strings.styleLiquidGlass,
                isSelected = !isNeumorphic,
                isDarkTheme = isDark,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = { onSelectDesignStyle("LIQUID_GLASS") }
            )
            DesignStyleOption(
                label = strings.styleNeumorphism,
                isSelected = isNeumorphic,
                isDarkTheme = isDark,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = { onSelectDesignStyle("NEUMORPHISM") }
            )
        }
    }
}

@Composable
private fun DesignStyleOption(
    label: String,
    isSelected: Boolean,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val selectedTextColor = if (isDarkTheme) Color.White else Color(0xFF0F172A)
    val unselectedTextColor = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)

    val textColor by animateColorAsState(
        targetValue = if (isSelected) selectedTextColor else unselectedTextColor,
        animationSpec = tween(durationMillis = 200),
        label = "DesignStyleTextColor"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = Color(0xFF3B82F6).copy(alpha = 0.2f)),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 12.5.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

