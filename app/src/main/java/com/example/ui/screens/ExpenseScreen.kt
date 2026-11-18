package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpenseScreen(
    transactions: List<TransactionEntity>,
    autoExpenseEnabled: Boolean = false,
    onToggleAutoExpense: (Boolean) -> Unit = {},
    onAddExpenseClick: () -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val smsPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.READ_SMS] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
                PackageManager.PERMISSION_GRANTED
        if (granted) {
            onToggleAutoExpense(true)
        }
    }

    fun handleAutoExpenseToggle(next: Boolean) {
        if (!next) {
            onToggleAutoExpense(false)
            return
        }
        val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED
        if (hasPerm) {
            onToggleAutoExpense(true)
        } else {
            smsPermLauncher.launch(
                arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
            )
        }
    }
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
                    AutoExpenseAlgPostToggleCard(
                        enabled = autoExpenseEnabled,
                        strings = strings,
                        dimens = dimens,
                        typo = typo,
                        onToggle = ::handleAutoExpenseToggle
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
                    strings = strings,
                    dimens = dimens,
                    typo = typo,
                    onAddExpenseClick = onAddExpenseClick
                )
            }

            item {
                AutoExpenseAlgPostToggleCard(
                    enabled = autoExpenseEnabled,
                    strings = strings,
                    dimens = dimens,
                    typo = typo,
                    onToggle = ::handleAutoExpenseToggle
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
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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

@Composable
private fun AutoExpenseAlgPostToggleCard(
    enabled: Boolean,
    strings: StringsBundle,
    dimens: com.example.ui.theme.AdaptiveDimens,
    typo: com.example.ui.theme.AdaptiveTypography,
    onToggle: (Boolean) -> Unit
) {
    val colors = LiquidGlassTheme.colors
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!enabled) },
        backgroundColor = if (enabled) RoseNeon.copy(alpha = 0.12f) else colors.glassCard,
        shape = RoundedCornerShape(dimens.cornerRadiusMd)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.cardPadding, vertical = dimens.spacingSm + 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(dimens.iconSizeXl)
                    .clip(CircleShape)
                    .background(
                        if (enabled) RoseNeon.copy(alpha = 0.22f)
                        else colors.glassSurface
                    )
                    .border(
                        1.dp,
                        if (enabled) RoseNeon.copy(alpha = 0.55f) else colors.glassBorderSubtle,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Sms,
                    contentDescription = strings.autoExpensesFromAlgPost,
                    tint = if (enabled) RoseNeon else colors.textSecondary,
                    modifier = Modifier.size(dimens.iconSizeSm)
                )
            }
            Spacer(modifier = Modifier.width(dimens.spacingMd))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.autoExpensesFromAlgPost,
                    color = colors.textPrimary,
                    fontSize = typo.titleSm,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = strings.autoExpensesFromAlgPostDesc,
                    color = colors.textSecondary,
                    fontSize = typo.caption,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(dimens.spacingSm))
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                modifier = Modifier.testTag("auto_expense_alg_post_switch"),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = RoseNeon,
                    uncheckedThumbColor = colors.textMuted,
                    uncheckedTrackColor = colors.glassSurface
                )
            )
        }
    }
}
