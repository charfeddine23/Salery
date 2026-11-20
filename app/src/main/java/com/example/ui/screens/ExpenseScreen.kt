package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ExpenseCategories
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPill
import com.example.ui.components.LiquidEmptyStateCard
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.ArabicStrings
import com.example.ui.i18n.StringsBundle
import com.example.ui.i18n.formatDzd
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.LocalAdaptiveDimens
import com.example.ui.theme.LocalAdaptiveLayoutInfo
import com.example.ui.theme.LocalAdaptiveTypography
import com.example.ui.theme.RoseNeon
import com.example.ui.theme.neumorphicDepth

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpenseScreen(
    transactions: List<TransactionEntity>,
    onAddExpenseClick: () -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    unloggedTaxesCount: Int = 0,
    onAutoAddTaxes: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current
    val isArabic = strings is ArabicStrings
    val colors = LiquidGlassTheme.colors
    val layoutInfo = LocalAdaptiveLayoutInfo.current
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }

    val expenseList = transactions.filter { it.type == TransactionType.EXPENSE }
    val totalExpense = expenseList.sumOf { it.amount }

    val availableCategories = remember {
        ExpenseCategories.getExpenseCategories().map { it.id }
    }

    val filteredList = expenseList.filter { item ->
        val matchesSearch = item.title.contains(searchQuery, ignoreCase = true) ||
                item.note.contains(searchQuery, ignoreCase = true)
        val normalizedItemCat = ExpenseCategories.findCategory(item.category).id
        val matchesCategory = selectedCategoryFilter == null ||
                item.category.equals(selectedCategoryFilter, ignoreCase = true) ||
                normalizedItemCat.equals(selectedCategoryFilter, ignoreCase = true)
        matchesSearch && matchesCategory
    }

    if (layoutInfo.columns >= 2) {
        // Adaptive 2-column layout for tablets and landscape
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = dimens.screenPaddingH),
            horizontalArrangement = Arrangement.spacedBy(dimens.spacingLg)
        ) {
            // Left Column: Total Expense Hero Card + Category Filters
            LazyColumn(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(dimens.spacingMd)
            ) {
                item { Spacer(modifier = Modifier.height(dimens.spacingXs)) }

                item {
                    TotalExpenseHeroCard(
                        totalExpense = totalExpense,
                        unloggedTaxesCount = unloggedTaxesCount,
                        isArabic = isArabic,
                        strings = strings,
                        dimens = dimens,
                        typo = typo,
                        onAddExpenseClick = onAddExpenseClick,
                        onAutoAddTaxes = onAutoAddTaxes
                    )
                }

                item {
                    Text(
                        text = strings.category,
                        color = colors.textPrimary,
                        fontSize = typo.titleSm,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = dimens.spacingSm)
                    )
                }

                item {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(dimens.spacingSm),
                        verticalArrangement = Arrangement.spacedBy(dimens.spacingSm),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        GlassPill(
                            text = strings.allFilter,
                            isSelected = selectedCategoryFilter == null,
                            onClick = { selectedCategoryFilter = null }
                        )
                        availableCategories.forEach { cat ->
                            GlassPill(
                                text = strings.categoryName(cat),
                                isSelected = selectedCategoryFilter == cat,
                                onClick = {
                                    selectedCategoryFilter = if (selectedCategoryFilter == cat) null else cat
                                }
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(dimens.spacingXl)) }
            }

            // Right Column: Search & Expense Transactions List
            LazyColumn(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(dimens.spacingMd)
            ) {
                item { Spacer(modifier = Modifier.height(dimens.spacingXs)) }

                item {
                    ExpenseSearchBar(
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        strings = strings,
                        dimens = dimens,
                        typo = typo
                    )
                }

                if (filteredList.isEmpty()) {
                    item {
                        LiquidEmptyStateCard(
                            title = strings.emptyExpensesTitle,
                            description = strings.emptyExpensesDesc,
                            icon = Icons.Default.TrendingDown,
                            iconColor = RoseNeon,
                            actionText = strings.addFirstExpense,
                            onActionClick = onAddExpenseClick
                        )
                    }
                } else {
                    items(filteredList, key = { it.id }) { item ->
                        TransactionRowItem(
                            transaction = item,
                            currencyCode = strings.currencyCode,
                            onDelete = { onDeleteTransaction(item) }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(dimens.spacingXl)) }
            }
        }
    } else {
        // Single-column smooth vertical flow for Phones
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = dimens.screenPaddingH),
            verticalArrangement = Arrangement.spacedBy(dimens.spacingMd)
        ) {
            item { Spacer(modifier = Modifier.height(dimens.spacingXs)) }

            item {
                TotalExpenseHeroCard(
                    totalExpense = totalExpense,
                    unloggedTaxesCount = unloggedTaxesCount,
                    isArabic = isArabic,
                    strings = strings,
                    dimens = dimens,
                    typo = typo,
                    onAddExpenseClick = onAddExpenseClick,
                    onAutoAddTaxes = onAutoAddTaxes
                )
            }

            item {
                ExpenseSearchBar(
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    strings = strings,
                    dimens = dimens,
                    typo = typo
                )
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(dimens.spacingSm),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        GlassPill(
                            text = strings.allFilter,
                            isSelected = selectedCategoryFilter == null,
                            onClick = { selectedCategoryFilter = null }
                        )
                    }
                    items(availableCategories) { cat ->
                        GlassPill(
                            text = strings.categoryName(cat),
                            isSelected = selectedCategoryFilter == cat,
                            onClick = {
                                selectedCategoryFilter = if (selectedCategoryFilter == cat) null else cat
                            }
                        )
                    }
                }
            }

            if (filteredList.isEmpty()) {
                item {
                    LiquidEmptyStateCard(
                        title = strings.emptyExpensesTitle,
                        description = strings.emptyExpensesDesc,
                        icon = Icons.Default.TrendingDown,
                        iconColor = RoseNeon,
                        actionText = strings.addFirstExpense,
                        onActionClick = onAddExpenseClick
                    )
                }
            } else {
                items(filteredList, key = { it.id }) { item ->
                    TransactionRowItem(
                        transaction = item,
                        currencyCode = strings.currencyCode,
                        onDelete = { onDeleteTransaction(item) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(dimens.spacingXl)) }
        }
    }
}

@Composable
private fun TotalExpenseHeroCard(
    totalExpense: Double,
    unloggedTaxesCount: Int,
    isArabic: Boolean,
    strings: StringsBundle,
    dimens: com.example.ui.theme.AdaptiveDimens,
    typo: com.example.ui.theme.AdaptiveTypography,
    onAddExpenseClick: () -> Unit,
    onAutoAddTaxes: () -> Unit
) {
    val colors = LiquidGlassTheme.colors

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = colors.glassCard,
        shape = RoundedCornerShape(dimens.cornerRadiusLg)
    ) {
        Column(modifier = Modifier.padding(dimens.cardPadding)) {
            Text(
                text = strings.monthlyExpense,
                color = colors.textSecondary,
                fontSize = typo.body,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(dimens.spacingXs))
            Text(
                text = formatDzd(totalExpense, strings.currencyCode),
                color = RoseNeon,
                fontSize = typo.display,
                fontWeight = FontWeight.ExtraBold,
                style = TextStyle(fontFeatureSettings = "tnum"),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(dimens.spacingMd))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimens.spacingSm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassButton(
                    text = strings.addExpense,
                    icon = Icons.Default.Add,
                    containerColor = RoseNeon,
                    modifier = Modifier.weight(1f),
                    onClick = onAddExpenseClick
                )

                AutoAddTaxesActionButton(
                    unloggedCount = unloggedTaxesCount,
                    isArabic = isArabic,
                    dimens = dimens,
                    typo = typo,
                    onClick = onAutoAddTaxes,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun AutoAddTaxesActionButton(
    unloggedCount: Int,
    isArabic: Boolean,
    dimens: com.example.ui.theme.AdaptiveDimens,
    typo: com.example.ui.theme.AdaptiveTypography,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LiquidGlassTheme.colors
    val shape = RoundedCornerShape(16.dp)
    val accent = AmberNeon
    val bgBrush = if (colors.isDark) {
        Brush.horizontalGradient(
            listOf(
                accent.copy(alpha = 0.22f),
                accent.copy(alpha = 0.14f)
            )
        )
    } else {
        Brush.horizontalGradient(
            listOf(
                accent.copy(alpha = 0.18f),
                accent.copy(alpha = 0.10f)
            )
        )
    }
    val textColor = if (colors.isDark) Color(0xFFFDE68A) else Color(0xFF92400E)

    val depthModifier = if (colors.isNeumorphic) {
        modifier
            .defaultMinSize(minHeight = dimens.buttonHeight)
            .neumorphicDepth(
                cornerRadius = 16.dp,
                elevation = 3.dp,
                isDark = colors.isDark,
                backgroundColor = colors.glassSurface
            )
            .clip(shape)
            .border(BorderStroke(1.dp, accent.copy(alpha = if (unloggedCount > 0) 0.55f else 0.35f)), shape)
    } else {
        modifier
            .defaultMinSize(minHeight = dimens.buttonHeight)
            .clip(shape)
            .background(bgBrush, shape)
            .border(BorderStroke(1.dp, accent.copy(alpha = if (unloggedCount > 0) 0.60f else 0.38f)), shape)
    }

    Box(
        modifier = depthModifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = accent),
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 10.dp)
            .testTag("auto_add_taxes_button"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoFixHigh,
                contentDescription = if (isArabic) "إضافة الضرائب تلقائياً" else "Auto Add Taxes",
                tint = textColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isArabic) "إضافة الضرائب" else "Auto Add Taxes",
                color = textColor,
                fontSize = typo.bodySm,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (unloggedCount > 0) {
                Spacer(modifier = Modifier.width(5.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(accent)
                        .padding(horizontal = 6.dp, vertical = 1.5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = unloggedCount.toString(),
                        color = Color(0xFF0A1220),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        style = TextStyle(fontFeatureSettings = "tnum")
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpenseSearchBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    strings: StringsBundle,
    dimens: com.example.ui.theme.AdaptiveDimens,
    typo: com.example.ui.theme.AdaptiveTypography
) {
    val colors = LiquidGlassTheme.colors

    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchChange,
        placeholder = { Text(strings.searchPlaceholder, color = colors.textMuted, fontSize = typo.body) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(dimens.iconSizeSm)
            )
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimens.cornerRadiusMd),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary,
            focusedBorderColor = colors.primaryNeon,
            unfocusedBorderColor = colors.glassBorderSubtle
        ),
        singleLine = true
    )
}
