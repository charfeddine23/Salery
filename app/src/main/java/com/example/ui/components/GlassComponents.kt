package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.NeuDarkHighlight
import com.example.ui.theme.neumorphicDepth

@Composable
fun GlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = LiquidGlassTheme.colors

    if (colors.isNeumorphic) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(colors.bgStart),
            content = content
        )
    } else {
        // Subtle breathing pulse for liquid orbs
        val transition = rememberInfiniteTransition(label = "liquid_glass_bg")
        val pulse by transition.animateFloat(
            initialValue = 0.85f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(4000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(colors.bgStart, colors.bgCenter, colors.bgEnd)
                    )
                )
                .drawBehind {
                    // Liquid glowing diffuse orbs in the background
                    val orb1Radius = size.width * 0.45f * pulse
                    val orb2Radius = size.width * 0.38f * (2f - pulse)

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                colors.primaryNeon.copy(alpha = if (colors.isDark) 0.18f else 0.12f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.2f, size.height * 0.15f),
                            radius = orb1Radius
                        )
                    )

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                colors.secondaryNeon.copy(alpha = if (colors.isDark) 0.14f else 0.08f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.85f, size.height * 0.45f),
                            radius = orb2Radius
                        )
                    )

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                colors.successColor.copy(alpha = if (colors.isDark) 0.12f else 0.06f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.35f, size.height * 0.8f),
                            radius = orb1Radius * 0.9f
                        )
                    )
                },
            content = content
        )
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    cornerRadius: Dp? = null,
    borderColor: Color = LiquidGlassTheme.colors.glassBorder,
    backgroundColor: Color = LiquidGlassTheme.colors.glassCard,
    borderWidth: Dp = 1.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = LiquidGlassTheme.colors
    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(color = colors.primaryNeon),
            onClick = onClick
        )
    } else Modifier

    val density = LocalDensity.current
    val effectiveCornerRadius = cornerRadius ?: if (shape is RoundedCornerShape) {
        with(density) { shape.topStart.toPx(androidx.compose.ui.geometry.Size.Unspecified, this).toDp() }
    } else {
        20.dp
    }

    if (colors.isNeumorphic) {
        Box(
            modifier = modifier
                .neumorphicDepth(
                    cornerRadius = effectiveCornerRadius,
                    elevation = 4.dp,
                    isDark = colors.isDark,
                    backgroundColor = backgroundColor
                )
                .then(clickModifier)
        ) {
            // Inner crisp border matching stroke width and corner curvature
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape)
                    .border(
                        border = BorderStroke(
                            width = borderWidth,
                            color = if (colors.isDark) NeuDarkHighlight.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.60f)
                        ),
                        shape = shape
                    )
            )
            // Fully clipped content container (uniform smooth clipping on all 4 corners)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
            ) {
                content()
            }
        }
    } else {
        Box(
            modifier = modifier
                .clip(shape)
                .border(
                    border = BorderStroke(
                        width = borderWidth,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                borderColor,
                                borderColor.copy(alpha = 0.2f),
                                Color.Transparent,
                                borderColor.copy(alpha = 0.4f)
                            )
                        )
                    ),
                    shape = shape
                )
                .background(backgroundColor, shape)
                .then(clickModifier),
            content = content
        )
    }
}

@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    containerColor: Color = LiquidGlassTheme.colors.primaryNeon,
    contentColor: Color = if (LiquidGlassTheme.colors.isDark) Color(0xFF021B27) else Color.White,
    shape: Shape = RoundedCornerShape(16.dp),
    enabled: Boolean = true
) {
    val colors = LiquidGlassTheme.colors

    if (colors.isNeumorphic) {
        Box(
            modifier = modifier
                .height(50.dp)
                .neumorphicDepth(
                    cornerRadius = 16.dp,
                    elevation = 4.dp,
                    isDark = colors.isDark,
                    backgroundColor = if (enabled) containerColor else (if (colors.isDark) Color(0xFF232731) else Color(0xFFD5DDE7))
                )
                .clip(shape)
                .clickable(
                    enabled = enabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = Color.White),
                    onClick = onClick
                )
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    color = contentColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    } else {
        Box(
            modifier = modifier
                .height(50.dp)
                .clip(shape)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = if (enabled) {
                            listOf(containerColor, containerColor.copy(alpha = 0.8f))
                        } else {
                            listOf(Color.Gray.copy(alpha = 0.4f), Color.Gray.copy(alpha = 0.3f))
                        }
                    ),
                    shape = shape
                )
                .clickable(
                    enabled = enabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = Color.White),
                    onClick = onClick
                )
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    color = contentColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun QuickActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    brush: Brush,
    borderColor: Color = Color(0x33FFFFFF),
    contentColor: Color = Color.White
) {
    val colors = LiquidGlassTheme.colors

    if (colors.isNeumorphic) {
        Box(
            modifier = modifier
                .height(44.dp)
                .neumorphicDepth(
                    cornerRadius = 14.dp,
                    elevation = 3.dp,
                    isDark = colors.isDark
                )
                .clip(RoundedCornerShape(14.dp))
                .background(brush)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = Color.White),
                    onClick = onClick
                )
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = text,
                    color = contentColor,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
    } else {
        Box(
            modifier = modifier
                .height(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(14.dp))
                .background(brush, RoundedCornerShape(14.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = Color.White),
                    onClick = onClick
                )
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = text,
                    color = contentColor,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun GlassPill(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val colors = LiquidGlassTheme.colors
    val bg = if (isSelected) colors.primaryNeon.copy(alpha = 0.25f) else colors.glassSurface
    val borderCol = if (isSelected) colors.primaryNeon else colors.glassBorderSubtle
    val txtCol = if (isSelected) colors.primaryNeon else colors.textSecondary

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .border(BorderStroke(1.dp, borderCol), RoundedCornerShape(24.dp))
            .background(bg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = colors.primaryNeon),
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = txtCol,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                color = txtCol,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
