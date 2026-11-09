package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.DebtType
import com.example.data.TransactionType
import com.example.ui.components.AddDebtDialog
import com.example.ui.components.AddTransactionDialog
import com.example.ui.components.BackupRestoreDialog
import com.example.ui.components.DynamicMoneyBag
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.BreathingSpLogo
import com.example.ui.components.PatternLockDialog
import com.example.ui.components.SalarySchedulerDialog
import com.example.ui.components.SidebarDrawer
import com.example.ui.components.SpLogoContainer
import com.example.ui.i18n.APP_NAME
import com.example.ui.i18n.APP_VERSION
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.ArabicStrings
import com.example.ui.i18n.EnglishStrings
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.RoseNeon
import com.example.viewmodel.SaleryPelseViewModel
import kotlinx.coroutines.launch

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.input.pointer.pointerInput
import com.example.sensor.RememberShakeDetector
import com.example.ui.i18n.LocalStealthMode

@Composable
fun MainAppScreen(
    viewModel: SaleryPelseViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }

    val userSettings by viewModel.settings.collectAsState()
    val isAppLocked by viewModel.isAppLocked.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val debts by viewModel.allDebts.collectAsState()
    val summary by viewModel.financialSummary.collectAsState()

    // Shake to Hide / Stealth Mode Sensor Listener
    RememberShakeDetector(enabled = !isAppLocked) {
        viewModel.toggleStealthMode()
    }

    // Determine current localized strings bundle
    val isArabic = userSettings.language == "ar"
    val activeStrings = if (isArabic) ArabicStrings else EnglishStrings
    // Strict LTR rule: Force Left-to-Right layout direction regardless of language (never flip or mirror)
    val layoutDirection = LayoutDirection.Ltr

    // Active bottom navigation route
    var currentRoute by remember { mutableStateOf("dashboard") }

    // Dialog States
    var showAddTransactionDialog by remember { mutableStateOf(false) }
    var addTransactionType by remember { mutableStateOf(TransactionType.EXPENSE) }

    var showAddDebtDialog by remember { mutableStateOf(false) }
    var addDebtType by remember { mutableStateOf(DebtType.OWED_BY_ME) }

    var showSalarySchedulerDialog by remember { mutableStateOf(false) }
    var showBackupRestoreDialog by remember { mutableStateOf(false) }
    var showPatternLockDialog by remember { mutableStateOf(false) }

    // Toast Events collector
    LaunchedEffect(viewModel) {
        viewModel.toastEvent.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            snackbarHostState.showSnackbar(msg)
        }
    }

    CompositionLocalProvider(
        LocalStrings provides activeStrings,
        LocalLayoutDirection provides layoutDirection,
        LocalStealthMode provides userSettings.stealthMode
    ) {
        if (isAppLocked) {
            LockScreen(
                onUnlockAttempt = { pattern ->
                    viewModel.unlockApp(pattern)
                },
                onBiometricUnlock = {
                    viewModel.unlockAppByBiometrics()
                },
                isBiometricEnabled = userSettings.biometricEnabled
            )
        } else {
            // Sidebar / Navigation Drawer is strictly locked to the Left side (LTR) in both English & Arabic
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet(
                            drawerContainerColor = Color.Transparent,
                            modifier = Modifier.width(320.dp)
                        ) {
                            SidebarDrawer(
                                selectedRoute = currentRoute,
                                onNavigate = { route ->
                                    currentRoute = route
                                    scope.launch { drawerState.close() }
                                },
                                userSettings = userSettings,
                                onOpenSalaryScheduler = {
                                    scope.launch { drawerState.close() }
                                    showSalarySchedulerDialog = true
                                },
                                onOpenBackupRestore = {
                                    scope.launch { drawerState.close() }
                                    showBackupRestoreDialog = true
                                },
                                onOpenPatternLock = {
                                    scope.launch { drawerState.close() }
                                    showPatternLockDialog = true
                                },
                                onLockAppNow = {
                                    scope.launch { drawerState.close() }
                                    viewModel.lockApp()
                                },
                                onToggleStealthMode = {
                                    viewModel.toggleStealthMode()
                                },
                                onSelectDesignStyle = { style ->
                                    viewModel.setDesignStyle(style)
                                },
                                onSelectThemeMode = { mode ->
                                    viewModel.setThemeMode(mode)
                                },
                                onSelectLanguage = { lang ->
                                    viewModel.setLanguage(lang)
                                }
                            )
                        }
                    }
                ) {
                    // Main App Content adopts the chosen layoutDirection (RTL in Arabic, LTR in English)
                    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                        GlassBackground {
                            Scaffold(
                        containerColor = Color.Transparent,
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        topBar = {
                            MainTopAppBar(
                                title = when (currentRoute) {
                                    "dashboard" -> APP_NAME
                                    "income" -> activeStrings.navIncome
                                    "expenses" -> activeStrings.navExpenses
                                    "debts" -> activeStrings.navDebts
                                    "insights" -> if (isArabic) "الادخار" else "Savings"
                                    else -> APP_NAME
                                },
                                onMenuClick = {
                                    scope.launch {
                                        if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                    }
                                },
                                totalBalance = summary.totalBalance,
                                onOpenInsights = { currentRoute = "insights" },
                                isStealthMode = userSettings.stealthMode,
                                onToggleStealth = { viewModel.toggleStealthMode() }
                            )
                        },
                        bottomBar = {
                            LiquidGlassBottomBar(
                                currentRoute = currentRoute,
                                onRouteSelect = { currentRoute = it }
                            )
                        },
                        floatingActionButton = {
                            FloatingActionButton(
                                onClick = {
                                    when (currentRoute) {
                                        "income" -> {
                                            addTransactionType = TransactionType.INCOME
                                            showAddTransactionDialog = true
                                        }
                                        "expenses" -> {
                                            addTransactionType = TransactionType.EXPENSE
                                            showAddTransactionDialog = true
                                        }
                                        "debts" -> {
                                            addDebtType = DebtType.OWED_BY_ME
                                            showAddDebtDialog = true
                                        }
                                        else -> {
                                            addTransactionType = TransactionType.EXPENSE
                                            showAddTransactionDialog = true
                                        }
                                    }
                                },
                                containerColor = LiquidGlassTheme.colors.primaryNeon,
                                contentColor = if (LiquidGlassTheme.colors.isDark) Color(0xFF0D1B2A) else Color.White,
                                shape = CircleShape,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add",
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (currentRoute) {
                                "dashboard" -> DashboardScreen(
                                    summary = summary,
                                    transactions = transactions,
                                    debts = debts,
                                    userSettings = userSettings,
                                    onAddIncomeClick = {
                                        addTransactionType = TransactionType.INCOME
                                        showAddTransactionDialog = true
                                    },
                                    onAddExpenseClick = {
                                        addTransactionType = TransactionType.EXPENSE
                                        showAddTransactionDialog = true
                                    },
                                    onAddDebtClick = {
                                        addDebtType = DebtType.OWED_BY_ME
                                        showAddDebtDialog = true
                                    },
                                    onAddTransactionClick = {
                                        addTransactionType = TransactionType.EXPENSE
                                        showAddTransactionDialog = true
                                    },
                                    onSeeAllClick = { currentRoute = "expenses" },
                                    onOpenSalaryScheduler = { showSalarySchedulerDialog = true },
                                    onDeleteTransaction = { viewModel.deleteTransaction(it) },
                                    onLockToVault = { viewModel.lockToVault(it) },
                                    onUnlockFromVault = { viewModel.unlockFromVault(it) }
                                )
                                "income" -> IncomeScreen(
                                    transactions = transactions,
                                    onAddIncomeClick = {
                                        addTransactionType = TransactionType.INCOME
                                        showAddTransactionDialog = true
                                    },
                                    onDeleteTransaction = { viewModel.deleteTransaction(it) }
                                )
                                "expenses" -> ExpenseScreen(
                                    transactions = transactions,
                                    onAddExpenseClick = {
                                        addTransactionType = TransactionType.EXPENSE
                                        showAddTransactionDialog = true
                                    },
                                    onDeleteTransaction = { viewModel.deleteTransaction(it) }
                                )
                                "debts" -> DebtScreen(
                                    debts = debts,
                                    onAddDebtClick = { type ->
                                        addDebtType = type
                                        showAddDebtDialog = true
                                    },
                                    onEditDebt = { viewModel.updateDebt(it) },
                                    onPartialPay = { debt, amount, recordInWallet ->
                                        viewModel.makePartialPayment(debt, amount, recordInWallet)
                                    },
                                    onPayDebtNow = { viewModel.markDebtAsPaid(it) },
                                    onDeleteDebt = { viewModel.deleteDebt(it) },
                                    onClearSettledDebts = { viewModel.clearSettledDebts() }
                                )
                                "insights" -> FinancialInsightsScreen(
                                    summary = summary,
                                    transactions = transactions,
                                    debts = debts,
                                    userSettings = userSettings,
                                    onBackClick = { currentRoute = "dashboard" },
                                    onTransferToSavings = { viewModel.transferToSavings(it) },
                                    onWithdrawFromSavings = { viewModel.withdrawFromSavings(it) },
                                    onSpendFromSavings = { amount, title, category, note ->
                                        viewModel.spendFromSavings(amount, title, category, note)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

            // Dialogs
            if (showAddTransactionDialog) {
                AddTransactionDialog(
                    initialType = addTransactionType,
                    onDismiss = { showAddTransactionDialog = false },
                    onSave = { type, amount, category, title, note, dateMillis ->
                        viewModel.addTransaction(type, amount, category, title, note, dateMillis)
                    }
                )
            }

            if (showAddDebtDialog) {
                AddDebtDialog(
                    initialType = addDebtType,
                    onDismiss = { showAddDebtDialog = false },
                    onSave = { type, person, amount, note, dueDateMillis, autoPay, isRecurring, monthlyInstallment, installmentDay, paidAmount ->
                        viewModel.addDebt(
                            type = type,
                            personName = person,
                            amount = amount,
                            note = note,
                            dueDateMillis = dueDateMillis,
                            autoPay = autoPay,
                            isRecurring = isRecurring,
                            monthlyInstallment = monthlyInstallment,
                            installmentDay = installmentDay,
                            initialPaidAmount = paidAmount
                        )
                    }
                )
            }

            if (showSalarySchedulerDialog) {
                SalarySchedulerDialog(
                    currentAmount = userSettings.salaryAmount,
                    currentPayday = userSettings.salaryPayday,
                    currentEmployer = userSettings.salaryEmployer,
                    currentAutoCredit = userSettings.salaryAutoCredit,
                    onDismiss = { showSalarySchedulerDialog = false },
                    onSave = { amount, payday, employer, autoCredit ->
                        viewModel.updateSalarySettings(amount, payday, employer, autoCredit)
                    },
                    onDepositNow = {
                        viewModel.depositSalaryNowManually()
                    }
                )
            }

            if (showBackupRestoreDialog) {
                BackupRestoreDialog(
                    lastBackupTimestamp = userSettings.lastBackupTimestamp,
                    onDismiss = { showBackupRestoreDialog = false },
                    onExportBackup = { viewModel.exportBackupJson() },
                    onRestoreBackup = { json -> viewModel.restoreFromJson(json) }
                )
            }

            if (showPatternLockDialog) {
                PatternLockDialog(
                    isCurrentlyEnabled = userSettings.patternLockEnabled,
                    isBiometricEnabled = userSettings.biometricEnabled,
                    onDismiss = { showPatternLockDialog = false },
                    onSavePattern = { pattern -> viewModel.saveNewPattern(pattern) },
                    onDisablePattern = { viewModel.disablePatternLock() },
                    onToggleBiometric = { enabled -> viewModel.setBiometricEnabled(enabled) }
                )
            }
        }
    }
}

@Composable
fun MainTopAppBar(
    title: String,
    onMenuClick: () -> Unit,
    totalBalance: Double = 0.0,
    onOpenInsights: () -> Unit = {},
    isStealthMode: Boolean = false,
    onToggleStealth: () -> Unit = {}
) {
    val colors = LiquidGlassTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Menu Drawer Button (fixed 44dp anchored to left)
        GlassCard(
            modifier = Modifier.size(44.dp),
            shape = RoundedCornerShape(14.dp),
            backgroundColor = colors.glassCard,
            onClick = onMenuClick
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Fixed Icon Positioning with Double-Tap to Toggle Stealth Mode & Breathing Glow
        Box(
            modifier = Modifier.pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onToggleStealth() }
                )
            }
        ) {
            BreathingSpLogo(
                size = 36.dp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Screen Title anchored directly next to the fixed icon
        Text(
            text = title,
            color = colors.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.3.sp,
            maxLines = 1
        )

        Spacer(modifier = Modifier.weight(1f))

        // Dynamic 90fps Money Bag (placed directly in front of the Stealth Mode eye icon)
        DynamicMoneyBag(
            totalBalance = totalBalance,
            onClick = onOpenInsights,
            size = 40.dp
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Stealth Mode / Privacy Toggle Action Button
        GlassCard(
            modifier = Modifier.size(42.dp),
            shape = RoundedCornerShape(13.dp),
            backgroundColor = if (isStealthMode) colors.primaryNeon.copy(alpha = 0.22f) else colors.glassCard,
            onClick = onToggleStealth
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isStealthMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = "Stealth Mode Toggle",
                    tint = if (isStealthMode) colors.primaryNeon else colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun LiquidGlassBottomBar(
    currentRoute: String,
    onRouteSelect: (String) -> Unit
) {
    val colors = LiquidGlassTheme.colors
    val strings = AppStrings.current

    val items = listOf(
        NavigationItem("dashboard", strings.navDashboard, Icons.Default.Dashboard),
        NavigationItem("income", strings.navIncome, Icons.Default.TrendingUp),
        NavigationItem("expenses", strings.navExpenses, Icons.Default.TrendingDown),
        NavigationItem("debts", strings.navDebts, Icons.Default.CreditCard)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            backgroundColor = colors.glassCard
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = currentRoute == item.route
                    val activeColor = when (item.route) {
                        "income" -> EmeraldNeon
                        "expenses" -> RoseNeon
                        "debts" -> AmberNeon
                        else -> colors.primaryNeon
                    }

                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onRouteSelect(item.route) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (isSelected) activeColor else colors.textMuted,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.label,
                            color = if (isSelected) activeColor else colors.textMuted,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

private data class NavigationItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)
