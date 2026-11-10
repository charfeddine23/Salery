package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.ExpenseCategories
import com.example.data.TransactionType
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.RoseNeon

@Composable
fun AddTransactionDialog(
    initialType: TransactionType = TransactionType.EXPENSE,
    onDismiss: () -> Unit,
    onSave: (type: TransactionType, amount: Double, category: String, title: String, note: String, dateMillis: Long) -> Unit
) {
    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors

    var selectedType by remember { mutableStateOf(initialType) }
    var amountText by remember { mutableStateOf("") }
    var titleText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }

    val currentCategories = if (selectedType == TransactionType.INCOME) {
        ExpenseCategories.getIncomeCategories()
    } else {
        ExpenseCategories.getExpenseCategories()
    }

    var selectedCategory by remember {
        mutableStateOf(if (initialType == TransactionType.INCOME) "salary" else "food")
    }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(26.dp),
            backgroundColor = colors.dialogBackground
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedType == TransactionType.INCOME) strings.addIncome else strings.addExpense,
                        color = colors.textPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = strings.cancel,
                            tint = colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Type Toggle (Income / Expense)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.glassCard)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selectedType == TransactionType.EXPENSE) RoseNeon.copy(alpha = 0.25f)
                                else Color.Transparent
                            )
                            .clickable {
                                val prevName = strings.categoryName(selectedCategory)
                                selectedType = TransactionType.EXPENSE
                                selectedCategory = "food"
                                if (titleText.isBlank() || titleText == prevName) {
                                    titleText = strings.categoryName("food")
                                }
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.navExpenses,
                            color = if (selectedType == TransactionType.EXPENSE) RoseNeon else colors.textMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selectedType == TransactionType.INCOME) EmeraldNeon.copy(alpha = 0.25f)
                                else Color.Transparent
                            )
                            .clickable {
                                val prevName = strings.categoryName(selectedCategory)
                                selectedType = TransactionType.INCOME
                                selectedCategory = "salary"
                                if (titleText.isBlank() || titleText == prevName) {
                                    titleText = strings.categoryName("salary")
                                }
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.navIncome,
                            color = if (selectedType == TransactionType.INCOME) EmeraldNeon else colors.textMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Amount Input
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("${strings.amount} (${strings.currencyCode})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = colors.primaryNeon,
                        unfocusedBorderColor = colors.glassBorderSubtle,
                        focusedLabelColor = colors.primaryNeon,
                        unfocusedLabelColor = colors.textSecondary
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Title Input
                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text(if (selectedType == TransactionType.EXPENSE) strings.expenseDescription else strings.incomeSource) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = colors.primaryNeon,
                        unfocusedBorderColor = colors.glassBorderSubtle,
                        focusedLabelColor = colors.primaryNeon,
                        unfocusedLabelColor = colors.textSecondary
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Pre-defined Category Selection Header & Active Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.category,
                        color = colors.textPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )

                    val activeCatItem = ExpenseCategories.findCategory(selectedCategory)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(activeCatItem.color.copy(alpha = 0.16f))
                            .border(1.dp, activeCatItem.color.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = activeCatItem.icon,
                            contentDescription = null,
                            tint = activeCatItem.color,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = strings.categoryName(selectedCategory),
                            color = activeCatItem.color,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Pre-defined Category Chips with Icons & Colors
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(currentCategories) { cat ->
                        val isSelected = selectedCategory == cat.id
                        val chipBg = if (isSelected) cat.color.copy(alpha = 0.22f) else colors.glassCard
                        val chipBorder = if (isSelected) cat.color.copy(alpha = 0.85f) else colors.glassBorderSubtle
                        val contentColor = if (isSelected) cat.color else colors.textPrimary

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(chipBg)
                                .border(1.5.dp, chipBorder, RoundedCornerShape(14.dp))
                                .clickable {
                                    val previousCatName = strings.categoryName(selectedCategory)
                                    selectedCategory = cat.id
                                    if (titleText.isBlank() || titleText == previousCatName) {
                                        titleText = strings.categoryName(cat.id)
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) cat.color.copy(alpha = 0.25f) else colors.glassSurface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = null,
                                    tint = contentColor,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(7.dp))
                            Text(
                                text = strings.categoryName(cat.id),
                                color = contentColor,
                                fontSize = 12.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Note Input
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text(strings.note) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = colors.primaryNeon,
                        unfocusedBorderColor = colors.glassBorderSubtle,
                        focusedLabelColor = colors.primaryNeon,
                        unfocusedLabelColor = colors.textSecondary
                    ),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(22.dp))

                // Save Button
                val isValid = (amountText.toDoubleOrNull() ?: 0.0) > 0.0 && titleText.isNotBlank()
                GlassButton(
                    text = strings.save,
                    enabled = isValid,
                    containerColor = if (selectedType == TransactionType.INCOME) EmeraldNeon else colors.primaryNeon,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val amount = amountText.toDoubleOrNull() ?: 0.0
                        onSave(selectedType, amount, selectedCategory, titleText.trim(), noteText.trim(), System.currentTimeMillis())
                        onDismiss()
                    }
                )
            }
        }
    }
}
