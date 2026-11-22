package com.example.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.market.SquareMarketCatalog
import com.example.market.SquareMarketSourceSpec
import com.example.market.SquareSourceCategory
import com.example.ui.i18n.isolateLtr
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassColors
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.RoseNeon

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SquarePriceWebsitesSelectorPanel(
    selectedIds: Set<String>,
    minOneWarning: Boolean,
    isArabic: Boolean,
    onToggleSource: (String) -> Unit,
    onSelectAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LiquidGlassTheme.colors
    val allSources = SquareMarketCatalog.availableSources
    val allIds = SquareMarketCatalog.ALL_SOURCE_IDS

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("square_sources_panel")
    ) {
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

        Spacer(modifier = Modifier.height(10.dp))

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
                    .defaultMinSize(minHeight = 40.dp)
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
                    .clickable(onClick = onSelectAll)
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

        Spacer(modifier = Modifier.height(12.dp))

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

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                            onToggle = { onToggleSource(spec.id) }
                        )
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
