package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.crypto.BinanceCryptoCatalog
import com.example.crypto.CryptoHoldingEntry
import com.example.crypto.CryptoMarketPriceEntry
import com.example.data.DebtEntity
import com.example.data.SavingsMovementEntry
import com.example.data.TransactionEntity
import com.example.data.UserSettings
import com.example.market.GoldAssetHolding
import com.example.market.GoldValuationEngine
import com.example.market.SquareMarketSnapshot
import com.example.ui.components.AssetsLauncherCard
import com.example.ui.components.SavingsGoalDialog
import com.example.ui.components.SavingsLedgerPulseSection
import com.example.ui.components.SavingsQuickActionDock
import com.example.ui.components.SavingsTransferDialog
import com.example.ui.components.SavingsTransferMode
import com.example.ui.components.SovereignSavingsHeroCard
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.LocalStealthMode
import com.example.ui.theme.LocalAdaptiveDimens
import com.example.ui.theme.LocalAdaptiveLayoutInfo
import com.example.viewmodel.FinancialSummary

/**
 * Assets & Saving Screen — Calm, Luxury Vault Layout:
 * 1. Sovereign Savings Hero Card (total savings + optional assets toggle + hairline goal bar).
 * 2. Quick-Action Dock (Deposit / Withdraw / Spend launching a clean focused sheet) + Assets Portal Card.
 * 3. Tabbed Bottom Workspace (Activity Ledger | Monthly Liquidity Pulse).
 */
@Composable
fun FinancialInsightsScreen(
    summary: FinancialSummary,
    transactions: List<TransactionEntity>,
    debts: List<DebtEntity>,
    userSettings: UserSettings,
    savingsHistory: List<SavingsMovementEntry> = emptyList(),
    goldHoldings: List<GoldAssetHolding> = emptyList(),
    squareSnapshot: SquareMarketSnapshot? = null,
    binanceHoldings: List<CryptoHoldingEntry> = emptyList(),
    binancePrices: Map<String, CryptoMarketPriceEntry> = emptyMap(),
    onBackClick: () -> Unit,
    onTransferToSavings: (Double) -> Unit = {},
    onWithdrawFromSavings: (Double) -> Unit = {},
    onSpendFromSavings: (Double, String, String, String) -> Unit = { _, _, _, _ -> },
    onSetSavingsGoal: (Double) -> Unit = {},
    onClearSavingsHistory: () -> Unit = {},
    onToggleIncludeAssets: (Boolean) -> Unit = {},
    onOpenAssetsScreen: () -> Unit = {},
    onSyncAssetsPricesNow: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)

    val strings = AppStrings.current
    val layoutInfo = LocalAdaptiveLayoutInfo.current
    val dimens = LocalAdaptiveDimens.current
    val isArabic = userSettings.language == "ar"
    val isStealth = LocalStealthMode.current

    var showGoalDialog by remember { mutableStateOf(false) }
    var activeTransferModalMode by remember { mutableStateOf<SavingsTransferMode?>(null) }

    val goldSummary = remember(goldHoldings, squareSnapshot) {
        GoldValuationEngine.evaluatePortfolio(goldHoldings, squareSnapshot)
    }
    val cryptoTotalUsd = remember(binanceHoldings, binancePrices) {
        BinanceCryptoCatalog.calculatePortfolioTotalUsd(binanceHoldings, binancePrices)
    }
    val cryptoDzdApprox = remember(cryptoTotalUsd, squareSnapshot) {
        GoldValuationEngine.convertUsdToSquareParallelDzd(cryptoTotalUsd, squareSnapshot) ?: 0.0
    }
    val combinedAssetsDzd = goldSummary.totalValueDzdParallel + cryptoDzdApprox

    // Automatically refresh stale Square & Binance prices whenever combined total is active or holdings exist
    LaunchedEffect(userSettings.includeAssetsInSavingsTotal) {
        if (userSettings.includeAssetsInSavingsTotal || goldHoldings.isNotEmpty() || binanceHoldings.isNotEmpty()) {
            onSyncAssetsPricesNow(false)
        }
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
                // Left Column: Hero Vault + Quick Actions + Assets Portal
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
                            onEditGoalClick = { showGoalDialog = true },
                            assetsTotalDzd = combinedAssetsDzd,
                            goldValueDzd = goldSummary.totalValueDzdParallel,
                            cryptoValueDzd = cryptoDzdApprox,
                            includeAssetsInTotal = userSettings.includeAssetsInSavingsTotal,
                            onToggleIncludeAssets = onToggleIncludeAssets
                        )
                    }

                    item {
                        SavingsQuickActionDock(
                            isArabic = isArabic,
                            onActionClick = { mode -> activeTransferModalMode = mode }
                        )
                    }

                    item {
                        AssetsLauncherCard(
                            goldHoldings = goldHoldings,
                            squareSnapshot = squareSnapshot,
                            binanceHoldings = binanceHoldings,
                            binancePrices = binancePrices,
                            isArabic = isArabic,
                            isStealth = isStealth,
                            onOpenAssetsScreen = onOpenAssetsScreen
                        )
                    }

                    item { Spacer(modifier = Modifier.height(dimens.spacingXl)) }
                }

                // Right Column: Tabbed Activity Ledger & Monthly Pulse
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(dimens.spacingMd)
                ) {
                    item { Spacer(modifier = Modifier.height(dimens.spacingXs)) }

                    item {
                        SavingsLedgerPulseSection(
                            entries = savingsHistory,
                            summary = summary,
                            userSettings = userSettings,
                            currencyCode = strings.currencyCode,
                            isArabic = isArabic,
                            isStealth = isStealth,
                            onClearHistory = onClearSavingsHistory
                        )
                    }

                    item { Spacer(modifier = Modifier.height(dimens.spacingXl)) }
                }
            }
        } else {
            // Calm 3-Tier Mobile Layout
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
                        onEditGoalClick = { showGoalDialog = true },
                        assetsTotalDzd = combinedAssetsDzd,
                        goldValueDzd = goldSummary.totalValueDzdParallel,
                        cryptoValueDzd = cryptoDzdApprox,
                        includeAssetsInTotal = userSettings.includeAssetsInSavingsTotal,
                        onToggleIncludeAssets = onToggleIncludeAssets
                    )
                }

                item {
                    SavingsQuickActionDock(
                        isArabic = isArabic,
                        onActionClick = { mode -> activeTransferModalMode = mode }
                    )
                }

                item {
                    AssetsLauncherCard(
                        goldHoldings = goldHoldings,
                        squareSnapshot = squareSnapshot,
                        binanceHoldings = binanceHoldings,
                        binancePrices = binancePrices,
                        isArabic = isArabic,
                        isStealth = isStealth,
                        onOpenAssetsScreen = onOpenAssetsScreen
                    )
                }

                item {
                    SavingsLedgerPulseSection(
                        entries = savingsHistory,
                        summary = summary,
                        userSettings = userSettings,
                        currencyCode = strings.currencyCode,
                        isArabic = isArabic,
                        isStealth = isStealth,
                        onClearHistory = onClearSavingsHistory
                    )
                }

                item { Spacer(modifier = Modifier.height(dimens.spacingXl)) }
            }
        }

        val pendingMode = activeTransferModalMode
        if (pendingMode != null) {
            SavingsTransferDialog(
                initialMode = pendingMode,
                availableBalance = summary.totalBalance,
                savingsBalance = summary.savingsBalance,
                isArabic = isArabic,
                isStealth = isStealth,
                onDismiss = { activeTransferModalMode = null },
                onDeposit = onTransferToSavings,
                onWithdraw = onWithdrawFromSavings,
                onSpend = onSpendFromSavings
            )
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
