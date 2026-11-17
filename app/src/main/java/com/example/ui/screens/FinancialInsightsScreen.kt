package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ai.FinancialInsightsEngine
import com.example.data.DebtEntity
import com.example.data.SavingsMovementEntry
import com.example.data.TransactionEntity
import com.example.data.UserSettings
import com.example.ui.components.BalanceProjectionChart
import com.example.ui.components.FinancialInsightsCard
import com.example.ui.components.InstantSavingsTransferStudio
import com.example.ui.components.SavingsActivityLedgerCard
import com.example.ui.components.SavingsGoalDialog
import com.example.ui.components.SavingsPulseSection
import com.example.ui.components.SovereignSavingsHeroCard
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.LocalStealthMode
import com.example.ui.theme.LocalAdaptiveDimens
import com.example.ui.theme.LocalAdaptiveLayoutInfo
import com.example.viewmodel.FinancialSummary

/**
 * Redesigned Luxurious Savings & Wealth Reserve Screen.
 * Features a breathing sovereign vault hero card, an effortless inline quick-transfer studio
 * (Deposit / Withdraw / Spend), live savings movement ledger, and adaptive wealth forecasting.
 */
@Composable
fun FinancialInsightsScreen(
    summary: FinancialSummary,
    transactions: List<TransactionEntity>,
    debts: List<DebtEntity>,
    userSettings: UserSettings,
    savingsHistory: List<SavingsMovementEntry> = emptyList(),
    onBackClick: () -> Unit,
    onTransferToSavings: (Double) -> Unit = {},
    onWithdrawFromSavings: (Double) -> Unit = {},
    onSpendFromSavings: (Double, String, String, String) -> Unit = { _, _, _, _ -> },
    onSetSavingsGoal: (Double) -> Unit = {},
    onClearSavingsHistory: () -> Unit = {},
    onLockToVault: (Double) -> Unit = onTransferToSavings,
    onUnlockFromVault: (Double) -> Unit = onWithdrawFromSavings,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)

    val strings = AppStrings.current
    val layoutInfo = LocalAdaptiveLayoutInfo.current
    val dimens = LocalAdaptiveDimens.current
    val isArabic = userSettings.language == "ar"
    val isStealth = LocalStealthMode.current

    var showGoalDialog by remember { mutableStateOf(false) }

    val insights = remember(transactions, debts, summary, userSettings, isArabic) {
        FinancialInsightsEngine.generateInsights(
            transactions = transactions,
            debts = debts,
            summary = summary,
            userSettings = userSettings,
            isArabic = isArabic
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (layoutInfo.columns >= 2) {
            // Adaptive 2-Column Tablet / Landscape Layout
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = dimens.screenPaddingH),
                horizontalArrangement = Arrangement.spacedBy(dimens.spacingLg)
            ) {
                // Left Column: Sovereign Vault Hero + Instant Quick-Transfer Studio
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(dimens.spacingMd)
                ) {
                    item { Spacer(modifier = Modifier.height(dimens.spacingXs)) }

                    item {
                        SovereignSavingsHeroCard(
                            savingsBalance = summary.savingsBalance,
                            availableBalance = summary.totalBalance,
                            savingsGoalAmount = userSettings.savingsGoalAmount,
                            isArabic = isArabic,
                            isStealth = isStealth,
                            onEditGoalClick = { showGoalDialog = true }
                        )
                    }

                    item {
                        InstantSavingsTransferStudio(
                            availableBalance = summary.totalBalance,
                            savingsBalance = summary.savingsBalance,
                            isArabic = isArabic,
                            isStealth = isStealth,
                            onDeposit = onTransferToSavings,
                            onWithdraw = onWithdrawFromSavings,
                            onSpend = onSpendFromSavings
                        )
                    }

                    item { Spacer(modifier = Modifier.height(dimens.spacingXl)) }
                }

                // Right Column: Activity Ledger, Liquidity Pulse, Projection Chart & AI Insights
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(dimens.spacingMd)
                ) {
                    item { Spacer(modifier = Modifier.height(dimens.spacingXs)) }

                    item {
                        SavingsActivityLedgerCard(
                            entries = savingsHistory,
                            currencyCode = strings.currencyCode,
                            isArabic = isArabic,
                            isStealth = isStealth,
                            onClearHistory = onClearSavingsHistory
                        )
                    }

                    item {
                        SavingsPulseSection(
                            summary = summary,
                            userSettings = userSettings,
                            isArabic = isArabic,
                            isStealth = isStealth
                        )
                    }

                    item {
                        BalanceProjectionChart(
                            currentBalance = summary.totalBalance,
                            transactions = transactions,
                            debts = debts,
                            userSettings = userSettings,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        FinancialInsightsCard(
                            insights = insights,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item { Spacer(modifier = Modifier.height(dimens.spacingXl)) }
                }
            }
        } else {
            // Single-Column Mobile Flow
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = dimens.screenPaddingH),
                verticalArrangement = Arrangement.spacedBy(dimens.spacingMd)
            ) {
                item { Spacer(modifier = Modifier.height(dimens.spacingXs)) }

                item {
                    SovereignSavingsHeroCard(
                        savingsBalance = summary.savingsBalance,
                        availableBalance = summary.totalBalance,
                        savingsGoalAmount = userSettings.savingsGoalAmount,
                        isArabic = isArabic,
                        isStealth = isStealth,
                        onEditGoalClick = { showGoalDialog = true }
                    )
                }

                item {
                    InstantSavingsTransferStudio(
                        availableBalance = summary.totalBalance,
                        savingsBalance = summary.savingsBalance,
                        isArabic = isArabic,
                        isStealth = isStealth,
                        onDeposit = onTransferToSavings,
                        onWithdraw = onWithdrawFromSavings,
                        onSpend = onSpendFromSavings
                    )
                }

                item {
                    SavingsActivityLedgerCard(
                        entries = savingsHistory,
                        currencyCode = strings.currencyCode,
                        isArabic = isArabic,
                        isStealth = isStealth,
                        onClearHistory = onClearSavingsHistory
                    )
                }

                item {
                    SavingsPulseSection(
                        summary = summary,
                        userSettings = userSettings,
                        isArabic = isArabic,
                        isStealth = isStealth
                    )
                }

                item {
                    BalanceProjectionChart(
                        currentBalance = summary.totalBalance,
                        transactions = transactions,
                        debts = debts,
                        userSettings = userSettings,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    FinancialInsightsCard(
                        insights = insights,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item { Spacer(modifier = Modifier.height(dimens.spacingXl)) }
            }
        }

        if (showGoalDialog) {
            SavingsGoalDialog(
                currentGoal = userSettings.savingsGoalAmount,
                isArabic = isArabic,
                onDismiss = { showGoalDialog = false },
                onConfirm = { goal ->
                    onSetSavingsGoal(goal)
                    showGoalDialog = false
                }
            )
        }
    }
}
