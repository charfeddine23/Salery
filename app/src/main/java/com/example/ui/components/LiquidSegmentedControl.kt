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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassTheme

/**
 * Modern, fluid, animated segmented control for Theme (Dark / Light).
 * Features a dynamic sliding capsule, glowing reflections, and smooth transitions.
 */
@Composable
fun LiquidThemeSegmentedControl(
    isDark: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LiquidGlassTheme.colors
    val selectedIndex = if (isDark) 0 else 1

    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "ThemeSegmentSlide"
    )

    val activeIndicatorBrush = if (isDark) {
        Brush.horizontalGradient(
            listOf(
                colors.primaryNeon.copy(alpha = 0.35f),
                colors.secondaryNeon.copy(alpha = 0.25f)
            )
        )
    } else {
        Brush.horizontalGradient(
            listOf(
                Color(0xFF0284C7).copy(alpha = 0.22f),
                Color(0xFF2563EB).copy(alpha = 0.16f)
            )
        )
    }

    val activeBorderColor = if (isDark) colors.primaryNeon.copy(alpha = 0.6f) else Color(0xFF0284C7).copy(alpha = 0.5f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.glassCard.copy(alpha = if (isDark) 0.6f else 0.85f))
            .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(14.dp))
            .padding(4.dp)
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
                .border(1.dp, activeBorderColor, RoundedCornerShape(11.dp))
        )

        // Segment Options Row
        Row(modifier = Modifier.fillMaxSize()) {
            ThemeSegmentItem(
                text = "Dark Glass",
                icon = Icons.Default.DarkMode,
                isSelected = isDark,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = { if (!isDark) onToggle() }
            )
            ThemeSegmentItem(
                text = "Light Crystal",
                icon = Icons.Default.LightMode,
                isSelected = !isDark,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = { if (isDark) onToggle() }
            )
        }
    }
}

@Composable
private fun ThemeSegmentItem(
    text: String,
    icon: ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = LiquidGlassTheme.colors
    val textColor by animateColorAsState(
        targetValue = if (isSelected) colors.textPrimary else colors.textMuted,
        animationSpec = tween(durationMillis = 250),
        label = "ThemeTextColor"
    )
    val iconTint by animateColorAsState(
        targetValue = if (isSelected) colors.primaryNeon else colors.textMuted,
        animationSpec = tween(durationMillis = 250),
        label = "ThemeIconColor"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = colors.primaryNeon),
                onClick = onClick
            ),
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
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = text,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
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
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "LangSegmentSlide"
    )

    val activeIndicatorBrush = if (isDark) {
        Brush.horizontalGradient(
            listOf(
                EmeraldNeon.copy(alpha = 0.32f),
                colors.primaryNeon.copy(alpha = 0.22f)
            )
        )
    } else {
        Brush.horizontalGradient(
            listOf(
                Color(0xFF059669).copy(alpha = 0.20f),
                Color(0xFF0284C7).copy(alpha = 0.15f)
            )
        )
    }

    val activeBorderColor = if (isDark) EmeraldNeon.copy(alpha = 0.55f) else Color(0xFF059669).copy(alpha = 0.45f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.glassCard.copy(alpha = if (isDark) 0.6f else 0.85f))
            .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(14.dp))
            .padding(4.dp)
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
                .border(1.dp, activeBorderColor, RoundedCornerShape(11.dp))
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
        animationSpec = tween(durationMillis = 250),
        label = "LangTextColor"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = EmeraldNeon),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                color = textColor,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) EmeraldNeon.copy(alpha = 0.2f) else colors.glassBorderSubtle.copy(alpha = 0.3f))
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
