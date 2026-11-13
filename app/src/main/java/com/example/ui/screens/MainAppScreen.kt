package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
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
import androidx.compose.foundation.layout.widthIn
import com.example.ui.theme.LocalAdaptiveDimens
import com.example.ui.theme.LocalAdaptiveLayoutInfo
import com.example.ui.theme.LocalAdaptiveTypography
import com.example.ui.theme.adaptiveContentWidth
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
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.ui.theme.LocalBreathingGlowEnabled
import com.example.ui.theme.RoseNeon
import com.example.viewmodel.SaleryPelseViewModel
import kotlinx.coroutines.launch

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.input.pointer.pointerInput
import com.example.sensor.RememberShakeDetector
import com.example.ui.components.AppUpdateDialog
import com.example.ui.i18n.LocalStealthMode
import com.example.update.AppUpdateManager
import com.example.update.AppUpdateUiState

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
    val updateUiState by viewModel.updateUiState.collectAsState()
    val showUpdateDialog by viewModel.showUpdateDialog.collectAsState()

    var canInstallPackages by remember {
        mutableStateOf(AppUpdateManager.canRequestPackageInstalls(context))
    }
    var lastAutoLaunchedApkPath by remember { mutableStateOf<String?>(null) }
    val installPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        val allowed = AppUpdateManager.canRequestPackageInstalls(context)
        canInstallPackages = allowed
        if (allowed) {
            viewModel.installReadyUpdate(context)
        }
    }

    // Shake to Hide / Stealth Mode Sensor Listener
    RememberShakeDetector(enabled = !isAppLocked) {
        viewModel.toggleStealthMode()
    }

    // Determine current localized strings bundle
    val isArabic = userSettings.language == "ar"
    val activeStrings = if (isArabic) ArabicStrings else EnglishStrings
    // Responsive layout direction: Full RTL when Arabic is selected, LTR for English
    val layoutDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

    // Active route ("cards" = Cards Hub outside card; cardInnerRoutes = inside Classic Blue card)
    var currentRoute by remember { mutableStateOf("cards") }
    val cardInnerRoutes = listOf("dashboard_details", "income", "expenses", "debts", "insights")
    val isInsideCard = currentRoute in cardInnerRoutes

    // Intercept system back gestures: exit sub-tabs back to Dashboard, or exit Card back to Cards Hub
    if (currentRoute != "cards") {
        BackHandler {
            currentRoute = if (currentRoute in listOf("income", "expenses", "debts", "insights")) {
                "dashboard_details"
            } else {
                "cards"
            }
        }
    }

    // Dialog States
    var showAddTransactionDialog by remember { mutableStateOf(false) }
    var addTransactionType by remember { mutableStateOf(TransactionType.EXPENSE) }

    var showAddDebtDialog by remember { mutableStateOf(false) }
    var addDebtType by remember { mutableStateOf(DebtType.OWED_BY_ME) }

    var showSalarySchedulerDialog by remember { mutableStateOf(false) }
    var showBackupRestoreDialog by remember { mutableStateOf(false) }

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
        LocalStealthMode provides userSettings.stealthMode,
        LocalBreathingGlowEnabled provides userSettings.breathingGlowEnabled
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
            // Respect the active language's layout direction for the entire interface and drawer
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
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
                                onOpenBackupRestore = {
                                    scope.launch { drawerState.close() }
                                    showBackupRestoreDialog = true
                                },
                                onToggleBiometric = { enabled ->
                                    viewModel.setBiometricEnabled(enabled)
                                },
                                onToggleBreathingGlow = { enabled ->
                                    viewModel.setBreathingGlowEnabled(enabled)
                                },
                                onSelectDesignStyle = { style ->
                                    viewModel.setDesignStyle(style)
                                },
                                onSelectThemeMode = { mode ->
                                    viewModel.setThemeMode(mode)
                                },
                                onSelectLanguage = { lang ->
                                    viewModel.setLanguage(lang)
                                },
                                updateUiState = updateUiState,
                                onCheckForUpdates = {
                                    scope.launch { drawerState.close() }
                                    viewModel.checkForUpdates(manual = true)
                                }
                            )
                        }
                    }
                ) {
                    // Main App Content adopts the chosen layoutDirection (RTL in Arabic, LTR in English)
                    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                        val dimens = LocalAdaptiveDimens.current
                        val typo = LocalAdaptiveTypography.current

                        GlassBackground {
                            Scaffold(
                        containerColor = Color.Transparent,
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        topBar = {
                            val isSubScreen = currentRoute != "cards"
                            MainTopAppBar(
                                title = when (currentRoute) {
                                    "cards" -> activeStrings.cardsHubTitle
                                    "dashboard_details" -> if (isArabic) "لوحة التحكم" else "Dashboard"
                                    "visa_coming_soon" -> activeStrings.visaCardTitle
                                    "income" -> activeStrings.navIncome
                                    "expenses" -> activeStrings.navExpenses
                                    "debts" -> activeStrings.navDebts
                                    "insights" -> if (isArabic) "الادخار" else "Savings"
                                    else -> APP_NAME
                                },
                                canGoBack = isSubScreen,
                                onBackClick = {
                                    currentRoute = if (currentRoute in listOf("income", "expenses", "debts", "insights")) {
                                        "dashboard_details"
                                    } else {
                                        "cards"
                                    }
                                },
                                onMenuClick = {
                                    scope.launch {
                                        if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                    }
                                },
                                totalBalance = summary.totalBalance,
                                showMoneyBag = isInsideCard,
                                onOpenInsights = { currentRoute = "insights" },
                                isStealthMode = userSettings.stealthMode,
                                onToggleStealth = { viewModel.toggleStealthMode() }
                            )
                        },
                        bottomBar = {
                            AnimatedVisibility(
                                visible = isInsideCard,
                                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
                            ) {
                                LiquidGlassBottomBar(
                                    currentRoute = currentRoute,
                                    onRouteSelect = { currentRoute = it }
                                )
                            }
                        },
                        floatingActionButton = {
                            if (isInsideCard) {
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
                                    modifier = Modifier.size(dimens.fabSize)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add",
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .adaptiveContentWidth(dimens.contentMaxWidth),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            when (currentRoute) {
                                "cards" -> CardsHubScreen(
                                    summary = summary,
                                    userSettings = userSettings,
                                    onOpenDashboard = { currentRoute = "dashboard_details" },
                                    onOpenVisaDetails = { currentRoute = "visa_coming_soon" },
                                    onUpdateClassicCard = { name, number, expiry, cvc ->
                                        viewModel.updateClassicCardDetails(name, number, expiry, cvc)
                                    }
                                )
                                "dashboard_details" -> DashboardScreen(
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
                                "visa_coming_soon" -> VisaComingSoonScreen(
                                    onBackClick = { currentRoute = "cards" }
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
                                    onBackClick = { currentRoute = "cards" },
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

            if (showUpdateDialog) {
                LaunchedEffect(updateUiState) {
                    val allowed = AppUpdateManager.canRequestPackageInstalls(context)
                    canInstallPackages = allowed
                    val readyState = updateUiState as? AppUpdateUiState.ReadyToInstall
                    if (readyState != null && allowed && lastAutoLaunchedApkPath != readyState.apkFile.absolutePath) {
                        lastAutoLaunchedApkPath = readyState.apkFile.absolutePath
                        viewModel.installReadyUpdate(context)
                    }
                }
                AppUpdateDialog(
                    state = updateUiState,
                    canInstallPackages = canInstallPackages,
                    onDismiss = { viewModel.dismissUpdateDialog() },
                    onStartDownload = {
                        lastAutoLaunchedApkPath = null
                        viewModel.startUpdateDownload()
                    },
                    onInstallApk = {
                        canInstallPackages = AppUpdateManager.canRequestPackageInstalls(context)
                        if (canInstallPackages) {
                            viewModel.installReadyUpdate(context)
                        } else {
                            try {
                                installPermissionLauncher.launch(
                                    AppUpdateManager.buildManageUnknownAppSourcesIntent(context)
                                )
                            } catch (_: Throwable) {
                                AppUpdateManager.openReleaseInBrowser(context)
                            }
                        }
                    },
                    onRequestInstallPermission = {
                        try {
                            installPermissionLauncher.launch(
                                AppUpdateManager.buildManageUnknownAppSourcesIntent(context)
                            )
                        } catch (_: Throwable) {
                            AppUpdateManager.openReleaseInBrowser(context)
                        }
                    },
                    onRetryCheck = { viewModel.checkForUpdates(manual = true) },
                    onOpenReleaseBrowser = { url ->
                        AppUpdateManager.openReleaseInBrowser(context, url)
                    }
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
    showMoneyBag: Boolean = true,
    onOpenInsights: () -> Unit = {},
    isStealthMode: Boolean = false,
    onToggleStealth: () -> Unit = {},
    canGoBack: Boolean = false,
    onBackClick: () -> Unit = {}
) {
    val colors = LiquidGlassTheme.colors
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .adaptiveContentWidth(dimens.contentMaxWidth)
                .padding(horizontal = dimens.screenPaddingH, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Menu / Back Button (min touch target 48dp)
            GlassCard(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(14.dp),
                backgroundColor = colors.glassCard,
                breathingGlow = false,
                onClick = if (canGoBack) onBackClick else onMenuClick
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (canGoBack) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Menu,
                        contentDescription = if (canGoBack) "Back" else "Menu",
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
                    size = 38.dp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Screen Title anchored directly next to the fixed icon
            Text(
                text = title,
                color = colors.textPrimary,
                fontSize = typo.title,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp,
                maxLines = 1
            )

            Spacer(modifier = Modifier.weight(1f))

            if (showMoneyBag) {
                // Dynamic 90fps Money Bag (placed directly in front of the Stealth Mode eye icon inside the card)
                DynamicMoneyBag(
                    totalBalance = totalBalance,
                    onClick = onOpenInsights,
                    size = 44.dp
                )

                Spacer(modifier = Modifier.width(8.dp))
            }

            // Stealth Mode / Privacy Toggle Action Button (min touch target 48dp)
            GlassCard(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(14.dp),
                backgroundColor = if (isStealthMode) colors.primaryNeon.copy(alpha = 0.22f) else colors.glassCard,
                breathingGlow = false,
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
}

@Composable
fun LiquidGlassBottomBar(
    currentRoute: String,
    onRouteSelect: (String) -> Unit
) {
    val colors = LiquidGlassTheme.colors
    val strings = AppStrings.current
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current
    val isArabic = strings is ArabicStrings

    val items = listOf(
        NavigationItem("dashboard_details", if (isArabic) "لوحة التحكم" else "Dashboard", Icons.Default.Dashboard),
        NavigationItem("income", strings.navIncome, Icons.Default.TrendingUp),
        NavigationItem("expenses", strings.navExpenses, Icons.Default.TrendingDown),
        NavigationItem("debts", strings.navDebts, Icons.Default.AccountBalanceWallet)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = dimens.screenPaddingH, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            backgroundColor = colors.glassCard
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
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
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onRouteSelect(item.route) }
                            .padding(horizontal = 4.dp, vertical = 6.dp),
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
                            fontSize = typo.caption,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
