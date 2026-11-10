package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ExpenseCategories
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPill
import com.example.ui.components.LiquidEmptyStateCard
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.StringsBundle
import com.example.ui.i18n.formatDzd
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.LocalAdaptiveDimens
import com.example.ui.theme.LocalAdaptiveLayoutInfo
import com.example.ui.theme.LocalAdaptiveTypography
import com.example.ui.theme.RoseNeon

@Composable
fun ExpenseScreen(
    transactions: List<TransactionEntity>,
    onAddExpenseClick: () -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current
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
                        strings = strings,
                        dimens = dimens,
                        typo = typo,
                        onAddExpenseClick = onAddExpenseClick
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
                    strings = strings,
                    dimens = dimens,
                    typo = typo,
                    onAddExpenseClick = onAddExpenseClick
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
    strings: StringsBundle,
    dimens: com.example.ui.theme.AdaptiveDimens,
    typo: com.example.ui.theme.AdaptiveTypography,
    onAddExpenseClick: () -> Unit
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
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(dimens.spacingMd))
            GlassButton(
                text = strings.addExpense,
                icon = Icons.Default.Add,
                containerColor = RoseNeon,
                modifier = Modifier.fillMaxWidth(),
                onClick = onAddExpenseClick
            )
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
