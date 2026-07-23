package com.circuitflip.flipledger

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.circuitflip.flipledger.domain.util.Money
import com.circuitflip.flipledger.presentation.AppViewModel
import com.circuitflip.flipledger.presentation.StartDestination
import com.circuitflip.flipledger.presentation.WizardStore
import com.circuitflip.flipledger.presentation.components.BottomTab
import com.circuitflip.flipledger.presentation.components.FlipBottomBar
import com.circuitflip.flipledger.presentation.navigation.Navigator
import com.circuitflip.flipledger.presentation.navigation.Route
import com.circuitflip.flipledger.presentation.navigation.SystemBackHandler
import com.circuitflip.flipledger.presentation.rememberViewModel
import com.circuitflip.flipledger.presentation.screens.addcost.AddCostScreen
import com.circuitflip.flipledger.presentation.screens.addcost.AddCostViewModel
import com.circuitflip.flipledger.presentation.screens.adddevice.AddDevice1Screen
import com.circuitflip.flipledger.presentation.screens.adddevice.AddDevice2Screen
import com.circuitflip.flipledger.presentation.screens.adddevice.AddDevice3Screen
import com.circuitflip.flipledger.presentation.screens.adddevice.AddDevice4Screen
import com.circuitflip.flipledger.presentation.screens.adddevice.AddDeviceViewModel
import com.circuitflip.flipledger.presentation.screens.adddevice.DeviceAddedScreen
import com.circuitflip.flipledger.presentation.screens.auth.AuthScreen
import com.circuitflip.flipledger.presentation.screens.dashboard.DashboardScreen
import com.circuitflip.flipledger.presentation.screens.devicedetail.DeviceDetailScreen
import com.circuitflip.flipledger.presentation.screens.inventory.InventoryScreen
import com.circuitflip.flipledger.presentation.screens.reports.ReportsScreen
import com.circuitflip.flipledger.presentation.screens.sale.Sale1Screen
import com.circuitflip.flipledger.presentation.screens.sale.Sale2Screen
import com.circuitflip.flipledger.presentation.screens.sale.Sale3Screen
import com.circuitflip.flipledger.presentation.screens.sale.SaleCompleteScreen
import com.circuitflip.flipledger.presentation.screens.sale.SaleViewModel
import com.circuitflip.flipledger.presentation.screens.saleshistory.SalesHistoryScreen
import com.circuitflip.flipledger.presentation.screens.settings.SettingsScreen
import com.circuitflip.flipledger.presentation.screens.settlement.SettlementScreen
import com.circuitflip.flipledger.presentation.screens.setup.Setup1Screen
import com.circuitflip.flipledger.presentation.screens.setup.Setup2Screen
import com.circuitflip.flipledger.presentation.screens.setup.Setup3Screen
import com.circuitflip.flipledger.presentation.screens.setup.SetupViewModel
import com.circuitflip.flipledger.presentation.screens.splash.SplashScreen
import com.circuitflip.flipledger.presentation.screens.subscription.SubscriptionScreen
import com.circuitflip.flipledger.presentation.screens.welcome.WelcomeScreen
import com.circuitflip.flipledger.presentation.theme.FlipLedgerTheme
import com.circuitflip.flipledger.presentation.theme.FlipTheme
import org.koin.compose.getKoin

/**
 * Root of the shared UI. Reads theme + auth state, then hosts a single [Navigator] whose
 * current [Route] is rendered by [AppNavHost]. This is the one composable both platforms call.
 */
@Composable
fun App() {
    val appVm = rememberViewModel<AppViewModel>()
    val appState by appVm.state.collectAsState()

    FlipLedgerTheme(darkTheme = appState.isDark) {
        Box(Modifier.fillMaxSize().background(FlipTheme.colors.backgroundSubtle)) {
            AppNavHost(appState.start)
        }
    }
}

/**
 * The single source of truth for navigation. A `when` over the current route renders the
 * matching screen and wires each screen's callbacks to navigator operations. Multi-step flows
 * (Setup, Add Device, Sale, Add Cost) share one ViewModel instance obtained here so progress
 * persists across their steps; those flows read the injected [WizardStore] for shared drafts.
 */
@Composable
private fun AppNavHost(start: StartDestination) {
    val koin = getKoin()
    val navigator = remember { Navigator(start = Route.Splash) }
    val store = remember { koin.get<WizardStore>() }

    // Shared flow ViewModels — one instance for the whole session.
    val setupVm = rememberViewModel<SetupViewModel>()
    val addDeviceVm = rememberViewModel<AddDeviceViewModel>()
    val saleVm = rememberViewModel<SaleViewModel>()
    val addCostVm = rememberViewModel<AddCostViewModel>()

    // Add-device submit and sale completion resolve asynchronously; advance when they land.
    val added by addDeviceVm.submitted.collectAsState()
    LaunchedEffect(added) { if (added && navigator.current is Route.AddDevice4) navigator.push(Route.DeviceAdded) }

    val saleState by saleVm.state.collectAsState()
    LaunchedEffect(saleState.completed) { if (saleState.completed && navigator.current is Route.Sale3) navigator.push(Route.SaleComplete) }

    // Once authenticated, route by onboarding status: already-onboarded users go straight to
    // the dashboard, first-time users go to setup. This covers both a restored session (from
    // the splash) and a fresh sign-in (from Welcome/Auth), so returning users never re-onboard.
    // Unauthenticated/loading users stay on the entry screens (splash → welcome → auth).
    LaunchedEffect(start) {
        val atEntry = navigator.current is Route.Splash ||
            navigator.current is Route.Welcome ||
            navigator.current is Route.Auth
        when (start) {
            StartDestination.AUTH -> {
                store.clearSession()
                saleVm.reset()
                if (!atEntry) navigator.resetTo(Route.Welcome)
            }
            StartDestination.HOME -> {
                if (atEntry) navigator.resetTo(Route.Dashboard)
            }
            StartDestination.ONBOARDING -> {
                if (atEntry) {
                    setupVm.start()
                    navigator.resetTo(Route.Setup1)
                }
            }
            StartDestination.LOADING -> Unit
        }
    }

    // Route the system back gesture/button into the navigator stack; at a root screen
    // (nothing to pop) this is disabled so back leaves the app as expected.
    SystemBackHandler(enabled = navigator.canGoBack) { navigator.back() }

    // The bottom navigation bar is shown only on the four primary destinations.
    val activeTab = when (navigator.current) {
        Route.Dashboard -> BottomTab.HOME
        Route.Inventory -> BottomTab.INVENTORY
        Route.SalesHistory -> BottomTab.SALES
        Route.Settings -> BottomTab.MORE
        else -> null
    }

    Column(Modifier.fillMaxSize()) {
      Box(Modifier.weight(1f)) {
        when (val route = navigator.current) {
        Route.Splash -> SplashScreen(onContinue = { navigator.replace(Route.Welcome) })

        Route.Welcome -> WelcomeScreen(
            onGetStarted = { navigator.push(Route.Auth(signUp = true)) },
            onHaveAccount = { navigator.push(Route.Auth(signUp = false)) },
        )

        is Route.Auth -> AuthScreen(
            signUp = route.signUp,
            onBack = { navigator.back() },
            // Navigation after auth is driven by the onboarding-aware effect above:
            // onboarded users → Dashboard, first-time users → Setup.
            onAuthenticated = {},
            onToggleMode = { signUp -> navigator.replace(Route.Auth(signUp = signUp)) },
        )

        Route.Setup1 -> Setup1Screen(setupVm, onContinue = { navigator.push(Route.Setup2) }, onBack = { navigator.back() })
        Route.Setup2 -> Setup2Screen(
            setupVm,
            onContinue = { if (setupVm.validateStep(2)) navigator.push(Route.Setup3) },
            onBack = { navigator.back() },
        )
        Route.Setup3 -> Setup3Screen(setupVm, onFinish = { navigator.resetTo(Route.Dashboard) }, onBack = { navigator.back() })

        Route.Dashboard -> DashboardScreen(
            onAddDevice = { addDeviceVm.start(); navigator.push(Route.AddDevice1) },
            onSeeAllSales = { navigator.push(Route.SalesHistory) },
            onOpenDevice = { id -> navigator.push(Route.DeviceDetail(id)) },
            onOpenSettings = { navigator.push(Route.Settings) },
        )

        Route.Inventory -> InventoryScreen(
            onAddDevice = { addDeviceVm.start(); navigator.push(Route.AddDevice1) },
            onOpenDevice = { id -> navigator.push(Route.DeviceDetail(id)) },
        )

        Route.AddDevice1 -> AddDevice1Screen(
            addDeviceVm,
            onContinue = { if (addDeviceVm.validateStep(1)) navigator.push(Route.AddDevice2) },
            onBack = { navigator.back() },
        )
        Route.AddDevice2 -> AddDevice2Screen(
            addDeviceVm,
            onContinue = { if (addDeviceVm.validateStep(2)) navigator.push(Route.AddDevice3) },
            onBack = { navigator.back() },
        )
        Route.AddDevice3 -> AddDevice3Screen(
            addDeviceVm,
            onContinue = { if (addDeviceVm.validateStep(3)) navigator.push(Route.AddDevice4) },
            onBack = { navigator.back() },
        )
        Route.AddDevice4 -> AddDevice4Screen(addDeviceVm, onEdit = { navigator.popTo(Route.AddDevice1) }, onBack = { navigator.back() })

        Route.DeviceAdded -> {
            val d = store.deviceDraft.value
            val summary = buildString {
                append("Invested ")
                append(Money.format(Money.parseToCents(d.price)))
                d.condition?.let { append(" · ${it.label}") }
            }
            DeviceAddedScreen(
                deviceSummary = summary,
                onGoToInventory = { navigator.resetTo(Route.Dashboard); navigator.push(Route.Inventory) },
                onAddRepairCost = {
                    val id = store.lastAddedDeviceId
                    if (id != null) { navigator.resetTo(Route.Dashboard); navigator.push(Route.DeviceDetail(id)); addCostVm.start(); navigator.push(Route.AddCost) }
                    else navigator.resetTo(Route.Dashboard)
                },
                onAddAnother = { addDeviceVm.start(); navigator.resetTo(Route.Dashboard); navigator.push(Route.AddDevice1) },
            )
        }

        is Route.DeviceDetail -> DeviceDetailScreen(
            deviceId = route.deviceId,
            onBack = { navigator.back() },
            onAddCost = { addCostVm.start(); navigator.push(Route.AddCost) },
            onStartSale = { saleVm.start(); navigator.push(Route.Sale1) },
        )

        Route.AddCost -> AddCostScreen(addCostVm, onBack = { navigator.back() }, onSaved = { navigator.back() })

        Route.Sale1 -> Sale1Screen(
            saleVm,
            onContinue = { if (saleVm.validateStep(1)) navigator.push(Route.Sale2) },
            onBack = { navigator.back() },
        )
        Route.Sale2 -> Sale2Screen(
            saleVm,
            onContinue = { if (saleVm.validateStep(2)) navigator.push(Route.Sale3) },
            onBack = { navigator.back() },
        )
        Route.Sale3 -> Sale3Screen(saleVm, onBack = { navigator.back() })

        Route.SaleComplete -> SaleCompleteScreen(
            sale = store.lastSale,
            onViewSales = { navigator.resetTo(Route.Dashboard); navigator.push(Route.SalesHistory) },
            onAddAnother = { addDeviceVm.start(); navigator.resetTo(Route.Dashboard); navigator.push(Route.AddDevice1) },
            onDashboard = { navigator.resetTo(Route.Dashboard) },
        )

        Route.SalesHistory -> SalesHistoryScreen(
            onBack = { navigator.back() },
            onOpenSettlement = { navigator.push(Route.Settlement) },
        )

        Route.Settlement -> SettlementScreen(onBack = { navigator.back() })

        Route.Reports -> ReportsScreen(onBack = { navigator.back() })

        Route.Subscription -> SubscriptionScreen(
            onBack = { navigator.back() },
        )

        Route.Settings -> SettingsScreen(
            onBack = { navigator.back() },
            onOpenSettlement = { navigator.push(Route.Settlement) },
            onOpenReports = { navigator.push(Route.Reports) },
            onOpenSubscription = { navigator.push(Route.Subscription) },
            onSignedOut = { navigator.resetTo(Route.Welcome) },
        )
        }
      }
      activeTab?.let { tab ->
          FlipBottomBar(active = tab, onSelect = { selected ->
              val target = when (selected) {
                  BottomTab.HOME -> Route.Dashboard
                  BottomTab.INVENTORY -> Route.Inventory
                  BottomTab.SALES -> Route.SalesHistory
                  BottomTab.MORE -> Route.Settings
              }
              if (navigator.current != target) navigator.resetTo(target)
          })
      }
    }
}
