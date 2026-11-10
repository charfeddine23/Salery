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
import androidx.compose.material.icons.filled.TrendingUp
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
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPill
import com.example.ui.components.LiquidEmptyStateCard
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.StringsBundle
import com.example.ui.i18n.formatDzd
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.LocalAdaptiveDimens
import com.example.ui.theme.LocalAdaptiveLayoutInfo
import com.example.ui.theme.LocalAdaptiveTypography

@Composable
fun IncomeScreen(
    transactions: List<TransactionEntity>,
    onAddIncomeClick: () -> Unit,
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

    val incomeList = transactions.filter { it.type == TransactionType.INCOME }
    val totalIncome = incomeList.sumOf { it.amount }

    val filteredList = incomeList.filter { item ->
        val matchesSearch = item.title.contains(searchQuery, ignoreCase = true) ||
                item.note.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategoryFilter == null || item.category.equals(selectedCategoryFilter, ignoreCase = true)
        matchesSearch && matchesCategory
    }

    val availableCategories = listOf("salary", "freelance", "investment", "gift", "other")

    if (layoutInfo.columns >= 2) {
        // Adaptive 2-column layout for tablets and landscape
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = dimens.screenPaddingH),
            horizontalArrangement = Arrangement.spacedBy(dimens.spacingLg)
        ) {
            // Left Column: Total Income Hero Card + Category Filters
            LazyColumn(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(dimens.spacingMd)
            ) {
                item { Spacer(modifier = Modifier.height(dimens.spacingXs)) }

                item {
                    TotalIncomeHeroCard(
                        totalIncome = totalIncome,
                        strings = strings,
                        dimens = dimens,
                        typo = typo,
                        onAddIncomeClick = onAddIncomeClick
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
                                onClick = { selectedCategoryFilter = if (selectedCategoryFilter == cat) null else cat }
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(dimens.spacingXl)) }
            }

            // Right Column: Search & Income Transactions List
            LazyColumn(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(dimens.spacingMd)
            ) {
                item { Spacer(modifier = Modifier.height(dimens.spacingXs)) }

                item {
                    IncomeSearchBar(
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
                            title = strings.emptyIncomeTitle,
                            description = strings.emptyIncomeDesc,
                            icon = Icons.Default.TrendingUp,
                            iconColor = EmeraldNeon,
                            actionText = strings.addFirstIncome,
                            onActionClick = onAddIncomeClick
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
                TotalIncomeHeroCard(
                    totalIncome = totalIncome,
                    strings = strings,
                    dimens = dimens,
                    typo = typo,
                    onAddIncomeClick = onAddIncomeClick
                )
            }

            item {
                IncomeSearchBar(
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
                            onClick = { selectedCategoryFilter = if (selectedCategoryFilter == cat) null else cat }
                        )
                    }
                }
            }

            if (filteredList.isEmpty()) {
                item {
                    LiquidEmptyStateCard(
                        title = strings.emptyIncomeTitle,
                        description = strings.emptyIncomeDesc,
                        icon = Icons.Default.TrendingUp,
                        iconColor = EmeraldNeon,
                        actionText = strings.addFirstIncome,
                        onActionClick = onAddIncomeClick
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
private fun TotalIncomeHeroCard(
    totalIncome: Double,
    strings: StringsBundle,
    dimens: com.example.ui.theme.AdaptiveDimens,
    typo: com.example.ui.theme.AdaptiveTypography,
    onAddIncomeClick: () -> Unit
) {
    val colors = LiquidGlassTheme.colors

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = colors.glassCard,
        shape = RoundedCornerShape(dimens.cornerRadiusLg)
    ) {
        Column(modifier = Modifier.padding(dimens.cardPadding)) {
            Text(
                text = strings.monthlyIncome,
                color = colors.textSecondary,
                fontSize = typo.body,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(dimens.spacingXs))
            Text(
                text = formatDzd(totalIncome, strings.currencyCode),
                color = EmeraldNeon,
                fontSize = typo.display,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(dimens.spacingMd))
            GlassButton(
                text = strings.addIncome,
                icon = Icons.Default.Add,
                containerColor = EmeraldNeon,
                modifier = Modifier.fillMaxWidth(),
                onClick = onAddIncomeClick
            )
        }
    }
}

@Composable
private fun IncomeSearchBar(
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
