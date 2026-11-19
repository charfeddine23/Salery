package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.ripple
import androidx.compose.foundation.layout.widthIn
import com.example.ui.theme.LocalAdaptiveDimens
import com.example.ui.theme.LocalAdaptiveLayoutInfo
import com.example.ui.theme.LocalAdaptiveTypography
import com.example.ui.theme.adaptiveContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.example.ui.components.SalarySchedulerDialog
import com.example.ui.components.ScreenshotProtectionDialog
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
import com.example.sms.AlgPosteSmsInboxScanner
import com.example.ui.components.AppUpdateDialog
import com.example.ui.components.PermissionsOnboardingDialog
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

    val userSettings by viewModel.settings.collectAsState()
    val isAppLocked by viewModel.isAppLocked.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val debts by viewModel.allDebts.collectAsState()
    val summary by viewModel.financialSummary.collectAsState()
    val updateUiState by viewModel.updateUiState.collectAsState()
    val showUpdateDialog by viewModel.showUpdateDialog.collectAsState()
    val pendingSmsSuggestions by viewModel.pendingSmsSuggestions.collectAsState()
    val detectedSmsTaxes by viewModel.detectedSmsTaxes.collectAsState()
    val smsBalanceHistory by viewModel.smsBalanceHistory.collectAsState()
    val archivedAlgPosteSms by viewModel.archivedAlgPosteSms.collectAsState()
    val archiveStudyReport by viewModel.archiveStudyReport.collectAsState()
    val expandedKeyIds by viewModel.expandedKeyIds.collectAsState()
    val revealedKeyIds by viewModel.revealedKeyIds.collectAsState()
    val expandedCcpIds by viewModel.expandedCcpIds.collectAsState()
    val hiddenCcpIds by viewModel.hiddenCcpIds.collectAsState()
    val binanceHoldings by viewModel.binanceHoldings.collectAsState()
    val binancePrices by viewModel.binancePrices.collectAsState()
    val isRefreshingCryptoPrices by viewModel.isRefreshingCryptoPrices.collectAsState()
    val cryptoPriceError by viewModel.cryptoPriceError.collectAsState()
    val squareSnapshot by viewModel.squareSnapshot.collectAsState()
    val savingsHistory by viewModel.savingsHistory.collectAsState()
    val isRefreshingSquareMarket by viewModel.isRefreshingSquareMarket.collectAsState()
    val squareMarketError by viewModel.squareMarketError.collectAsState()
    val binancePortfolioTotalUsd = remember(binanceHoldings, binancePrices) {
        com.example.crypto.BinanceCryptoCatalog.calculatePortfolioTotalUsd(binanceHoldings, binancePrices)
    }

    var canInstallPackages by remember {
        mutableStateOf(AppUpdateManager.canRequestPackageInstalls(context))
    }
    var lastAutoLaunchedApkPath by remember { mutableStateOf<String?>(null) }
    var lastAutoRequestedInstallPermApkPath by remember { mutableStateOf<String?>(null) }
    var pendingActionAfterInstallPerm by remember { mutableStateOf<String?>(null) }
    var forceShowPermissionsOnboarding by remember { mutableStateOf(false) }
    var allowOnboardingWindow by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(180L)
        allowOnboardingWindow = true
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context, userSettings.baridiMobSmsSyncEnabled) {
        var isInitialResume = true
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                if (!context.isHostActivityChangingConfigurations()) {
                    viewModel.collapseAllVaultContainersForSecurity()
                }
            } else if (event == Lifecycle.Event.ON_RESUME) {
                val allowed = AppUpdateManager.canRequestPackageInstalls(context)
                canInstallPackages = allowed
                if (allowed) {
                    when (pendingActionAfterInstallPerm) {
                        "download" -> {
                            pendingActionAfterInstallPerm = null
                            viewModel.startUpdateDownload()
                        }
                        "install" -> {
                            pendingActionAfterInstallPerm = null
                            viewModel.installReadyUpdate(context)
                        }
                        "onboarding", "permissions_settings" -> {
                            pendingActionAfterInstallPerm = null
                        }
                    }
                }
                if (isInitialResume) {
                    isInitialResume = false
                    return@LifecycleEventObserver
                }
                if (AlgPosteSmsInboxScanner.hasSmsPermissions(context)) {
                    if (userSettings.baridiMobSmsSyncEnabled) {
                        viewModel.scanAlgPosteSmsInbox(context, showFeedback = false)
                    } else {
                        viewModel.refreshCurrentBalanceOnlyFromInbox(context, showFeedback = false)
                    }
                }
                viewModel.checkForUpdatesIfDueOnResume()
                viewModel.refreshCryptoMarketPrices(force = false, showFeedback = false)
                viewModel.refreshSquareMarketData(force = false, showFeedback = false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val installPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        val allowed = AppUpdateManager.canRequestPackageInstalls(context)
        canInstallPackages = allowed
        val pending = pendingActionAfterInstallPerm
        pendingActionAfterInstallPerm = null
        if (allowed) {
            when (pending) {
                "download" -> viewModel.startUpdateDownload()
                "install" -> if (updateUiState is AppUpdateUiState.ReadyToInstall) {
                    viewModel.installReadyUpdate(context)
                }
                else -> Unit
            }
        }
    }

    fun openInstallUnknownAppsSettings(pendingAction: String) {
        pendingActionAfterInstallPerm = pendingAction
        try {
            installPermissionLauncher.launch(
                AppUpdateManager.buildManageUnknownAppSourcesIntent(context)
            )
        } catch (_: Throwable) {
            pendingActionAfterInstallPerm = null
            if (pendingAction == "download" || pendingAction == "install") {
                viewModel.reportInstallPermissionUnavailable()
            }
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

    // Active route (hubRoutes = 4-page bottom banner outside cards; cardInnerRoutes = inside Classic Blue card)
    var currentRoute by remember { mutableStateOf("cards") }
    val hubRoutes = listOf("cards", "keys", "ccps", "square")
    val isOnHubTabs = currentRoute in hubRoutes
    val cardInnerRoutes = listOf("dashboard_details", "income", "expenses", "debts", "baridimob_sync", "insights")
    val isInsideCard = currentRoute in cardInnerRoutes

    // Intercept system back gestures: exit sub-tabs back to Dashboard, or exit Card back to Cards Hub
    if (currentRoute != "cards") {
        BackHandler {
            currentRoute = if (currentRoute in listOf("income", "expenses", "debts", "baridimob_sync", "insights")) {
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
    var showScreenshotShieldDialog by remember { mutableStateOf(false) }

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
                            modifier = Modifier
                                .fillMaxWidth(0.84f)
                                .widthIn(min = 272.dp, max = 336.dp)
                        ) {
                            SidebarDrawer(
                                selectedRoute = currentRoute,
                                onNavigate = { route ->
                                    currentRoute = route
                                    scope.launch { drawerState.close() }
                                },
                                userSettings = userSettings,
                                pendingSmsCount = pendingSmsSuggestions.size,
                                onOpenBackupRestore = {
                                    scope.launch { drawerState.close() }
                                    showBackupRestoreDialog = true
                                },
                                onOpenAccessPermissions = {
                                    currentRoute = "access_permissions"
                                    scope.launch { drawerState.close() }
                                },
                                onOpenThemeEngine = {
                                    currentRoute = "theme_engine"
                                    scope.launch { drawerState.close() }
                                },
                                onToggleBiometric = { enabled ->
                                    viewModel.setBiometricEnabled(enabled)
                                },
                                onToggleBreathingGlow = { enabled ->
                                    viewModel.setBreathingGlowEnabled(enabled)
                                },
                                onToggleScreenshotProtection = { enabled ->
                                    viewModel.setScreenshotProtectionEnabled(enabled)
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
                        val layoutInfo = LocalAdaptiveLayoutInfo.current
                        val showNavRail = layoutInfo.useNavRail && (isOnHubTabs || isInsideCard)

                        GlassBackground {
                            Row(modifier = Modifier.fillMaxSize()) {
                                if (showNavRail) {
                                    LiquidGlassNavigationRail(
                                        currentRoute = currentRoute,
                                        isInsideCard = isInsideCard,
                                        pendingSmsCount = pendingSmsSuggestions.size,
                                        onRouteSelect = { currentRoute = it }
                                    )
                                }

                                Scaffold(
                                    modifier = Modifier.weight(1f),
                                    containerColor = Color.Transparent,
                                    topBar = {
                                        val isSubScreen = !isOnHubTabs
                                        MainTopAppBar(
                                            title = when (currentRoute) {
                                                "cards" -> activeStrings.cardsHubTitle
                                                "keys" -> "Keys"
                                                "ccps" -> "CCP's"
                                                "square" -> "Square"
                                                "baridimob_sync" -> if (isArabic) "مزامنة بريدي موب" else "BaridiMob SMS Sync"
                                                "access_permissions" -> if (isArabic) "صلاحيات الوصول" else "Access Permissions"
                                                "theme_engine" -> "Theme Engine"
                                                "dashboard_details" -> if (isArabic) "لوحة التحكم" else "Dashboard"
                                                "visa_coming_soon" -> activeStrings.visaCardTitle
                                                "binance_wallet" -> if (isArabic) "محفظة Binance" else "Binance Wallet"
                                                "income" -> activeStrings.navIncome
                                                "expenses" -> activeStrings.navExpenses
                                                "debts" -> activeStrings.navDebts
                                                "insights" -> if (isArabic) "الادخار" else "Savings"
                                                else -> APP_NAME
                                            },
                                            canGoBack = isSubScreen,
                                            onBackClick = {
                                                currentRoute = if (currentRoute in listOf("income", "expenses", "debts", "baridimob_sync", "insights")) {
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
                                            showScreenshotShield = currentRoute == "cards",
                                            isScreenshotProtected = userSettings.screenshotProtectionEnabled,
                                            onShieldClick = { showScreenshotShieldDialog = true },
                                            isStealthMode = userSettings.stealthMode,
                                            onToggleStealth = { viewModel.toggleStealthMode() }
                                        )
                                    },
                                    bottomBar = {
                                        if (!showNavRail) {
                                            Box(modifier = Modifier.fillMaxWidth()) {
                                                androidx.compose.animation.AnimatedVisibility(
                                                    visible = isOnHubTabs,
                                                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                                                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
                                                ) {
                                                    HubBottomBar(
                                                        currentRoute = currentRoute,
                                                        onRouteSelect = { currentRoute = it }
                                                    )
                                                }
                                                androidx.compose.animation.AnimatedVisibility(
                                                    visible = isInsideCard,
                                                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                                                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
                                                ) {
                                                    LiquidGlassBottomBar(
                                                        currentRoute = currentRoute,
                                                        pendingSmsCount = pendingSmsSuggestions.size,
                                                        onRouteSelect = { currentRoute = it }
                                                    )
                                                }
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
                                    binancePortfolioTotalUsd = binancePortfolioTotalUsd,
                                    binanceHoldingsCount = binanceHoldings.size,
                                    onOpenDashboard = { currentRoute = "dashboard_details" },
                                    onOpenVisaDetails = { currentRoute = "visa_coming_soon" },
                                    onOpenBinanceWallet = { currentRoute = "binance_wallet" },
                                    onUpdateClassicCard = { name, number, expiry, cvc, bankStyle ->
                                        viewModel.updateClassicCardDetails(name, number, expiry, cvc, bankStyle)
                                    },
                                    onUpdateVisaCard = { provider, name, number, expiry, cvc ->
                                        viewModel.updateVisaCardDetails(provider, name, number, expiry, cvc)
                                    },
                                    onUpdateThirdCard = { provider, name, number, expiry, cvc ->
                                        viewModel.updateThirdCardDetails(provider, name, number, expiry, cvc)
                                    },
                                    onRemoveThirdCard = {
                                        viewModel.removeThirdCard()
                                    }
                                )
                                "keys" -> KeysScreen(
                                    userSettings = userSettings,
                                    expandedKeyIds = expandedKeyIds,
                                    revealedKeyIds = revealedKeyIds,
                                    onToggleExpandKey = { viewModel.toggleKeyExpanded(it) },
                                    onToggleRevealKey = { viewModel.toggleKeyRevealed(it) },
                                    onCollapseAllKeys = { viewModel.collapseAllKeys() },
                                    onAddKey = { label, username, secret, category, note, pinned, appPresetId ->
                                        viewModel.addVaultKey(label, username, secret, category, note, pinned, appPresetId)
                                    },
                                    onUpdateKey = { id, label, username, secret, category, note, pinned, appPresetId ->
                                        viewModel.updateVaultKey(id, label, username, secret, category, note, pinned, appPresetId)
                                    },
                                    onTogglePinKey = { id ->
                                        viewModel.togglePinVaultKey(id)
                                    },
                                    onDeleteKey = { id ->
                                        viewModel.deleteVaultKey(id)
                                    }
                                )
                                "ccps" -> CcpsScreen(
                                    userSettings = userSettings,
                                    expandedCcpIds = expandedCcpIds,
                                    hiddenCcpIds = hiddenCcpIds,
                                    onToggleExpandCcp = { viewModel.toggleCcpExpanded(it) },
                                    onToggleHideCcp = { viewModel.toggleCcpHidden(it) },
                                    onCollapseAllCcps = { viewModel.collapseAllCcps() },
                                    onAddCcpAccount = { first, last, ccp, key, rip, note, cName, cUri, cPhone, pinned, appPresetId ->
                                        viewModel.addCcpAccount(first, last, ccp, key, rip, note, cName, cUri, cPhone, pinned, appPresetId)
                                    },
                                    onUpdateCcpAccount = { id, first, last, ccp, key, rip, note, cName, cUri, cPhone, pinned, appPresetId ->
                                        viewModel.updateCcpAccount(id, first, last, ccp, key, rip, note, cName, cUri, cPhone, pinned, appPresetId)
                                    },
                                    onTogglePinCcpAccount = { id ->
                                        viewModel.togglePinCcpAccount(id)
                                    },
                                    onDeleteCcpAccount = { id ->
                                        viewModel.deleteCcpAccount(id)
                                    }
                                )
                                "square" -> SquareScreen(
                                    snapshot = squareSnapshot,
                                    lastSyncMillis = userSettings.squareLastPriceSyncMillis,
                                    syncIntervalMinutes = userSettings.squareSyncIntervalMinutes,
                                    pinnedIdsJson = userSettings.squarePinnedIdsJson,
                                    isRefreshing = isRefreshingSquareMarket,
                                    errorMessage = squareMarketError,
                                    onRefresh = { force ->
                                        viewModel.refreshSquareMarketData(force = force, showFeedback = force)
                                    },
                                    onUpdateSyncIntervalMinutes = { mins ->
                                        viewModel.updateSquareSyncIntervalMinutes(mins)
                                    },
                                    onTogglePin = { tickerId ->
                                        viewModel.toggleSquarePinnedId(tickerId)
                                    }
                                )
                                "baridimob_sync" -> BaridiMobSmsSyncScreen(
                                    userSettings = userSettings,
                                    pendingSuggestions = pendingSmsSuggestions,
                                    detectedTaxes = detectedSmsTaxes,
                                    balanceHistory = smsBalanceHistory,
                                    archivedSmsList = archivedAlgPosteSms,
                                    archiveStudyReport = archiveStudyReport,
                                    onToggleSyncEnabled = { enabled ->
                                        viewModel.setBaridiMobSmsSyncEnabled(enabled)
                                    },
                                    onScanInboxNow = {
                                        viewModel.scanAlgPosteSmsInbox(context, showFeedback = true)
                                    },
                                    onRefreshCurrentBalanceOnly = { showFeedback ->
                                        viewModel.refreshCurrentBalanceOnlyFromInbox(context, showFeedback)
                                    },
                                    onClearBalanceHistory = {
                                        viewModel.clearSmsBalanceHistory()
                                    },
                                    onReanalyzePermanentArchive = {
                                        viewModel.reanalyzePermanentSmsArchive(showFeedback = true)
                                    },
                                    onSkipOldSmsAndStartFresh = {
                                        viewModel.skipOldSmsAndStartFresh()
                                    },
                                    onResetBaselineAndRescan = {
                                        viewModel.resetSmsBaselineAndRescan(context)
                                    },
                                    onAnalyzeManualSms = { sender, body ->
                                        viewModel.analyzeManualSmsSample(sender, body)
                                    },
                                    onAcceptSuggestion = { id, overrideType ->
                                        viewModel.acceptSmsSuggestion(id, overrideType)
                                    },
                                    onDismissSuggestion = { id ->
                                        viewModel.dismissSmsSuggestion(id)
                                    },
                                    onClearAllSuggestions = {
                                        viewModel.clearAllSmsSuggestions()
                                    },
                                    onRecordTaxAsExpense = { anomalyId ->
                                        viewModel.recordTaxAnomalyAsExpense(anomalyId)
                                    },
                                    onDismissTaxAnomaly = { anomalyId ->
                                        viewModel.dismissTaxAnomaly(anomalyId)
                                    },
                                    onClearAllTaxes = {
                                        viewModel.clearAllTaxAnomalies()
                                    }
                                )
                                "access_permissions" -> AccessPermissionsScreen(
                                    userSettings = userSettings,
                                    onRequestInstallSourcesSettings = {
                                        openInstallUnknownAppsSettings("permissions_settings")
                                    },
                                    onToggleBaridiMobSmsSync = { enabled ->
                                        viewModel.setBaridiMobSmsSyncEnabled(enabled)
                                    },
                                    onToggleBiometric = { enabled ->
                                        viewModel.setBiometricEnabled(enabled)
                                    },
                                    onReplayOnboardingWizard = {
                                        forceShowPermissionsOnboarding = true
                                    }
                                )
                                "theme_engine" -> ThemeEngineScreen(
                                    currentDesignStyle = userSettings.designStyle,
                                    currentThemeMode = userSettings.themeMode,
                                    onSelectDesignStyle = { style ->
                                        viewModel.setDesignStyle(style)
                                    },
                                    onSelectThemeMode = { mode ->
                                        viewModel.setThemeMode(mode)
                                    }
                                )
                                "dashboard_details" -> DashboardScreen(
                                    summary = summary,
                                    transactions = transactions,
                                    debts = debts,
                                    userSettings = userSettings,
                                    pendingSmsSuggestions = pendingSmsSuggestions,
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
                                    onUnlockFromVault = { viewModel.unlockFromVault(it) },
                                    onAcceptSmsSuggestion = { id, overrideType ->
                                        viewModel.acceptSmsSuggestion(id, overrideType)
                                    },
                                    onDismissSmsSuggestion = { id ->
                                        viewModel.dismissSmsSuggestion(id)
                                    },
                                    onOpenBaridiMobSync = { currentRoute = "baridimob_sync" },
                                    onRefreshCcpBalanceFromSms = { showFeedback ->
                                        if (userSettings.baridiMobSmsSyncEnabled) {
                                            viewModel.scanAlgPosteSmsInbox(
                                                context = context,
                                                showFeedback = showFeedback
                                            )
                                        } else {
                                            viewModel.refreshCurrentBalanceOnlyFromInbox(
                                                context = context,
                                                showFeedback = showFeedback
                                            )
                                        }
                                    }
                                )
                                "visa_coming_soon" -> VisaComingSoonScreen(
                                    onBackClick = { currentRoute = "cards" }
                                )
                                "binance_wallet" -> BinanceWalletScreen(
                                    userSettings = userSettings,
                                    holdings = binanceHoldings,
                                    prices = binancePrices,
                                    isRefreshingPrices = isRefreshingCryptoPrices,
                                    priceError = cryptoPriceError,
                                    onBackClick = { currentRoute = "cards" },
                                    onRefreshPrices = { force ->
                                        viewModel.refreshCryptoMarketPrices(force = force, showFeedback = force)
                                    },
                                    onUpsertHolding = { symbol, qty ->
                                        viewModel.upsertCryptoHolding(symbol, qty)
                                    },
                                    onRemoveHolding = { symbol ->
                                        viewModel.removeCryptoHolding(symbol)
                                    }
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
                                    savingsHistory = savingsHistory,
                                    onBackClick = { currentRoute = "cards" },
                                    onTransferToSavings = { viewModel.transferToSavings(it) },
                                    onWithdrawFromSavings = { viewModel.withdrawFromSavings(it) },
                                    onSpendFromSavings = { amount, title, category, note ->
                                        viewModel.spendFromSavings(amount, title, category, note)
                                    },
                                    onSetSavingsGoal = { viewModel.setSavingsGoal(it) },
                                    onClearSavingsHistory = { viewModel.clearSavingsHistory() }
                                )
                                        }
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

            val updateDialogVisible = showUpdateDialog
            if (updateDialogVisible) {
                LaunchedEffect(updateUiState) {
                    val allowed = AppUpdateManager.canRequestPackageInstalls(context)
                    canInstallPackages = allowed
                    val readyState = updateUiState as? AppUpdateUiState.ReadyToInstall
                    if (readyState != null) {
                        val apkPath = readyState.apkFile.absolutePath
                        if (allowed && lastAutoLaunchedApkPath != apkPath) {
                            lastAutoLaunchedApkPath = apkPath
                            viewModel.installReadyUpdate(context)
                        } else if (!allowed && lastAutoRequestedInstallPermApkPath != apkPath) {
                            lastAutoRequestedInstallPermApkPath = apkPath
                            openInstallUnknownAppsSettings("install")
                        }
                    }
                }
                AppUpdateDialog(
                    state = updateUiState,
                    canInstallPackages = canInstallPackages,
                    onDismiss = { viewModel.dismissUpdateDialog() },
                    onStartDownload = {
                        lastAutoLaunchedApkPath = null
                        lastAutoRequestedInstallPermApkPath = null
                        val allowed = AppUpdateManager.canRequestPackageInstalls(context)
                        canInstallPackages = allowed
                        if (!allowed) {
                            openInstallUnknownAppsSettings("download")
                        } else {
                            viewModel.startUpdateDownload()
                        }
                    },
                    onInstallApk = {
                        val allowed = AppUpdateManager.canRequestPackageInstalls(context)
                        canInstallPackages = allowed
                        if (allowed) {
                            viewModel.installReadyUpdate(context)
                        } else {
                            openInstallUnknownAppsSettings("install")
                        }
                    },
                    onRequestInstallPermission = {
                        openInstallUnknownAppsSettings("install")
                    },
                    onRetryCheck = { viewModel.checkForUpdates(manual = true) }
                )
            }

            if (showScreenshotShieldDialog && !updateDialogVisible) {
                ScreenshotProtectionDialog(
                    isProtected = userSettings.screenshotProtectionEnabled,
                    onToggleProtection = { enabled ->
                        viewModel.setScreenshotProtectionEnabled(enabled)
                    },
                    onDismiss = { showScreenshotShieldDialog = false }
                )
            }

            if (allowOnboardingWindow &&
                !isAppLocked &&
                !updateDialogVisible &&
                (!userSettings.hasCompletedPermissionsOnboarding || forceShowPermissionsOnboarding)
            ) {
                PermissionsOnboardingDialog(
                    onRequestInstallSourcesSettings = {
                        openInstallUnknownAppsSettings("onboarding")
                    },
                    onCompleted = { enableSms ->
                        forceShowPermissionsOnboarding = false
                        viewModel.completePermissionsOnboarding(enableSms)
                    },
                    onSkip = {
                        forceShowPermissionsOnboarding = false
                        viewModel.completePermissionsOnboarding(false)
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
    showScreenshotShield: Boolean = false,
    isScreenshotProtected: Boolean = false,
    onShieldClick: () -> Unit = {},
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
                .padding(horizontal = dimens.screenPaddingH, vertical = dimens.spacingSm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Menu / Back Button (min touch target 48dp)
            GlassCard(
                modifier = Modifier.size(dimens.buttonHeight.coerceAtLeast(48.dp)),
                shape = RoundedCornerShape(dimens.cornerRadiusMd),
                backgroundColor = colors.glassCard,
                breathingGlow = false,
                onClick = if (canGoBack) onBackClick else onMenuClick
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (canGoBack) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Menu,
                        contentDescription = if (canGoBack) "Back" else "Menu",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(dimens.iconSizeMd)
                    )
                }
            }

            Spacer(modifier = Modifier.width(dimens.spacingSm + 2.dp))

            // Screen Title (weighted + ellipsis so it never overlaps right-side action buttons)
            Text(
                text = title,
                color = colors.textPrimary,
                fontSize = typo.title,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.2.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(dimens.spacingSm))

            if (showMoneyBag) {
                DynamicMoneyBag(
                    totalBalance = totalBalance,
                    onClick = onOpenInsights,
                    size = (dimens.buttonHeight - 4.dp).coerceAtLeast(44.dp)
                )

                Spacer(modifier = Modifier.width(dimens.spacingXs + 2.dp))
            }

            if (showScreenshotShield) {
                ScreenshotShieldTopButton(
                    isProtected = isScreenshotProtected,
                    onClick = onShieldClick
                )

                Spacer(modifier = Modifier.width(dimens.spacingXs + 2.dp))
            }

            // Stealth Mode / Privacy Toggle Action Button (min touch target 48dp)
            GlassCard(
                modifier = Modifier.size(dimens.buttonHeight.coerceAtLeast(48.dp)),
                shape = RoundedCornerShape(dimens.cornerRadiusMd),
                backgroundColor = if (isStealthMode) colors.primaryNeon.copy(alpha = 0.22f) else colors.glassCard,
                breathingGlow = false,
                onClick = onToggleStealth
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isStealthMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Stealth Mode Toggle",
                        tint = if (isStealthMode) colors.primaryNeon else colors.textSecondary,
                        modifier = Modifier.size(dimens.iconSizeMd - 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ScreenshotShieldTopButton(
    isProtected: Boolean,
    onClick: () -> Unit
) {
    val colors = LiquidGlassTheme.colors
    val infinite = rememberInfiniteTransition(label = "top_shield_glow")
    val pulse by infinite.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shield_glow_pulse"
    )
    val orbitAngle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(7200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shield_light_orbit"
    )

    Box(
        modifier = Modifier
            .size(48.dp)
            .semantics {
                contentDescription = if (isProtected) {
                    "No-Screenshot Protection Active"
                } else {
                    "No-Screenshot Protection Disabled"
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 24.dp, color = CyanNeon),
                onClick = onClick
            )
            .testTag("cards_hub_shield_top_button"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(32.dp)) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f

            // 1. Multi-layered glowing lights behind the shield (NO outer box or container)
            if (isProtected) {
                val glowAlpha = if (colors.isDark) 1f else 0.78f
                // Wide soft cosmic halo radiating behind the shield
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            CyanNeon.copy(alpha = 0.58f * pulse * glowAlpha),
                            EmeraldNeon.copy(alpha = 0.34f * pulse * glowAlpha),
                            Color(0xFF6366F1).copy(alpha = 0.22f * pulse * glowAlpha),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = w * (1.25f + 0.14f * pulse)
                    ),
                    radius = w * (1.25f + 0.14f * pulse),
                    center = Offset(cx, cy)
                )

                // Orbiting twin aurora light lobes behind the shield edges
                rotate(degrees = orbitAngle, pivot = Offset(cx, cy)) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                EmeraldNeon.copy(alpha = 0.62f * pulse * glowAlpha),
                                Color.Transparent
                            ),
                            center = Offset(cx - w * 0.24f, cy - h * 0.14f),
                            radius = w * 0.72f
                        ),
                        radius = w * 0.72f,
                        center = Offset(cx - w * 0.24f, cy - h * 0.14f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF818CF8).copy(alpha = 0.56f * pulse * glowAlpha),
                                Color.Transparent
                            ),
                            center = Offset(cx + w * 0.24f, cy + h * 0.16f),
                            radius = w * 0.72f
                        ),
                        radius = w * 0.72f,
                        center = Offset(cx + w * 0.24f, cy + h * 0.16f)
                    )
                }
            }

            // 2. Sculpted Heraldic Shield Silhouette
            val shieldPath = Path().apply {
                moveTo(cx, h * 0.08f)
                lineTo(w * 0.87f, h * 0.22f)
                cubicTo(
                    w * 0.87f, h * 0.54f,
                    w * 0.74f, h * 0.77f,
                    cx, h * 0.93f
                )
                cubicTo(
                    w * 0.26f, h * 0.77f,
                    w * 0.13f, h * 0.54f,
                    w * 0.13f, h * 0.22f
                )
                close()
            }

            val innerFacetPath = Path().apply {
                moveTo(cx, h * 0.14f)
                lineTo(w * 0.80f, h * 0.26f)
                cubicTo(
                    w * 0.79f, h * 0.52f,
                    w * 0.68f, h * 0.71f,
                    cx, h * 0.85f
                )
                lineTo(cx, h * 0.14f)
                close()
            }

            if (isProtected) {
                // Luminous metallic neon body fill
                drawPath(
                    path = shieldPath,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF34D399),
                            CyanNeon,
                            Color(0xFF6366F1)
                        ),
                        start = Offset(w * 0.15f, h * 0.10f),
                        end = Offset(w * 0.85f, h * 0.92f)
                    )
                )

                // Right-half glossy faceted specular highlight
                drawPath(
                    path = innerFacetPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.28f),
                            Color.White.copy(alpha = 0.06f)
                        )
                    )
                )

                // Crisp luminous shield rim stroke
                drawPath(
                    path = shieldPath,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.95f),
                            CyanNeon,
                            EmeraldNeon
                        )
                    ),
                    style = Stroke(width = 1.6.dp.toPx(), join = StrokeJoin.Round)
                )

                // Center crisp security lock / check emblem inside shield
                val checkPath = Path().apply {
                    moveTo(cx - w * 0.16f, cy + h * 0.02f)
                    lineTo(cx - w * 0.04f, cy + h * 0.14f)
                    lineTo(cx + w * 0.18f, cy - h * 0.10f)
                }
                drawPath(
                    path = checkPath,
                    color = Color(0xFF04202C),
                    style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawPath(
                    path = checkPath,
                    color = Color.White,
                    style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            } else {
                // Calm, sleek disabled shield without glow or outer container
                drawPath(
                    path = shieldPath,
                    color = colors.textSecondary.copy(alpha = if (colors.isDark) 0.16f else 0.12f)
                )
                drawPath(
                    path = shieldPath,
                    color = colors.textSecondary.copy(alpha = 0.82f),
                    style = Stroke(width = 1.7.dp.toPx(), join = StrokeJoin.Round)
                )
                // Subtle diagonal slash indicating protection is off
                drawLine(
                    color = colors.textSecondary.copy(alpha = 0.85f),
                    start = Offset(w * 0.25f, h * 0.72f),
                    end = Offset(w * 0.75f, h * 0.24f),
                    strokeWidth = 1.7.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
fun LiquidGlassBottomBar(
    currentRoute: String,
    pendingSmsCount: Int = 0,
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
        NavigationItem("debts", strings.navDebts, Icons.Default.AccountBalanceWallet),
        NavigationItem("baridimob_sync", if (isArabic) "الرسائل" else "SMS", Icons.Default.Sms)
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
                .widthIn(max = 580.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            backgroundColor = colors.glassCard
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = currentRoute == item.route
                    val activeColor = when (item.route) {
                        "income" -> EmeraldNeon
                        "expenses" -> RoseNeon
                        "debts" -> AmberNeon
                        "baridimob_sync" -> Color(0xFF818CF8)
                        else -> colors.primaryNeon
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onRouteSelect(item.route) }
                            .padding(horizontal = 2.dp, vertical = 6.dp)
                            .testTag("card_tab_${item.route}"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = if (isSelected) activeColor else colors.textMuted,
                                modifier = Modifier.size(22.dp)
                            )
                            if (item.route == "baridimob_sync" && pendingSmsCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(RoseNeon)
                                )
                            }
                        }
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

@Composable
fun HubBottomBar(
    currentRoute: String,
    onRouteSelect: (String) -> Unit
) {
    val colors = LiquidGlassTheme.colors
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current

    val items = listOf(
        NavigationItem("cards", "Card Hub", Icons.Default.CreditCard),
        NavigationItem("keys", "Keys", Icons.Default.VpnKey),
        NavigationItem("ccps", "CCP's", Icons.Default.AccountBalance),
        NavigationItem("square", "Square", Icons.Default.CurrencyExchange)
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
                .fillMaxWidth()
                .testTag("hub_bottom_banner"),
            shape = RoundedCornerShape(26.dp),
            backgroundColor = colors.glassCard
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = currentRoute == item.route
                    val activeColor = when (item.route) {
                        "keys" -> AmberNeon
                        "ccps" -> EmeraldNeon
                        "square" -> AmberNeon
                        else -> CyanNeon
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) activeColor.copy(alpha = 0.14f) else Color.Transparent
                            )
                            .clickable { onRouteSelect(item.route) }
                            .padding(horizontal = 4.dp, vertical = 8.dp)
                            .testTag("hub_tab_${item.route}"),
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

@Composable
fun LiquidGlassNavigationRail(
    currentRoute: String,
    isInsideCard: Boolean,
    pendingSmsCount: Int = 0,
    onRouteSelect: (String) -> Unit
) {
    val colors = LiquidGlassTheme.colors
    val strings = AppStrings.current
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current
    val isArabic = strings is ArabicStrings

    val items = if (isInsideCard) {
        listOf(
            NavigationItem("dashboard_details", if (isArabic) "لوحة التحكم" else "Dashboard", Icons.Default.Dashboard),
            NavigationItem("income", strings.navIncome, Icons.Default.TrendingUp),
            NavigationItem("expenses", strings.navExpenses, Icons.Default.TrendingDown),
            NavigationItem("debts", strings.navDebts, Icons.Default.AccountBalanceWallet),
            NavigationItem("baridimob_sync", if (isArabic) "الرسائل" else "SMS", Icons.Default.Sms)
        )
    } else {
        listOf(
            NavigationItem("cards", "Card Hub", Icons.Default.CreditCard),
            NavigationItem("keys", "Keys", Icons.Default.VpnKey),
            NavigationItem("ccps", "CCP's", Icons.Default.AccountBalance),
            NavigationItem("square", "Square", Icons.Default.CurrencyExchange)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(start = dimens.spacingMd, top = dimens.spacingSm, bottom = dimens.spacingSm),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(
            modifier = Modifier
                .width(88.dp)
                .fillMaxHeight()
                .testTag(if (isInsideCard) "card_nav_rail" else "hub_nav_rail"),
            shape = RoundedCornerShape(dimens.cornerRadiusXl),
            cornerRadius = dimens.cornerRadiusXl,
            backgroundColor = colors.glassCard
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = dimens.spacingLg, horizontal = dimens.spacingXs),
                verticalArrangement = Arrangement.spacedBy(dimens.spacingSm, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items.forEach { item ->
                    val isSelected = currentRoute == item.route
                    val activeColor = when (item.route) {
                        "income" -> EmeraldNeon
                        "expenses" -> RoseNeon
                        "debts" -> AmberNeon
                        "baridimob_sync" -> Color(0xFF818CF8)
                        "keys" -> AmberNeon
                        "ccps" -> EmeraldNeon
                        "square" -> AmberNeon
                        else -> colors.primaryNeon
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(dimens.cornerRadiusMd))
                            .background(if (isSelected) activeColor.copy(alpha = 0.16f) else Color.Transparent)
                            .clickable { onRouteSelect(item.route) }
                            .padding(horizontal = 4.dp, vertical = 12.dp)
                            .testTag((if (isInsideCard) "card_tab_" else "hub_tab_") + item.route),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = if (isSelected) activeColor else colors.textMuted,
                                modifier = Modifier.size(dimens.iconSizeMd)
                            )
                            if (item.route == "baridimob_sync" && pendingSmsCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(RoseNeon)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.label,
                            color = if (isSelected) activeColor else colors.textMuted,
                            fontSize = typo.caption,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
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

private tailrec fun Context.isHostActivityChangingConfigurations(): Boolean = when (this) {
    is Activity -> isChangingConfigurations
    is ContextWrapper -> baseContext.isHostActivityChangingConfigurations()
    else -> false
}
