package com.blackink.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.blackink.app.domain.util.Money
import com.blackink.app.presentation.AppViewModel
import com.blackink.app.presentation.AuthRecovery
import com.blackink.app.domain.repository.AuthRepository
import com.blackink.app.presentation.StartDestination
import com.blackink.app.presentation.WizardStore
import com.blackink.app.core.ui.UiBanner
import com.blackink.app.presentation.components.BottomTab
import com.blackink.app.presentation.components.ErrorBannerHost
import com.blackink.app.presentation.components.FlipBottomBar
import com.blackink.app.presentation.navigation.Navigator
import com.blackink.app.presentation.navigation.Route
import com.blackink.app.presentation.navigation.SystemBackHandler
import com.blackink.app.presentation.navigation.navigationTransform
import com.blackink.app.presentation.navigation.prefersReducedMotion
import com.blackink.app.presentation.rememberViewModel
import com.blackink.app.presentation.screens.addcost.AddCostScreen
import com.blackink.app.presentation.screens.addcost.AddCostViewModel
import com.blackink.app.presentation.screens.adddevice.AddDevice1Screen
import com.blackink.app.presentation.screens.adddevice.AddDevice2Screen
import com.blackink.app.presentation.screens.adddevice.AddDevice3Screen
import com.blackink.app.presentation.screens.adddevice.AddDevice4Screen
import com.blackink.app.presentation.screens.adddevice.AddDeviceViewModel
import com.blackink.app.presentation.screens.adddevice.DeviceAddedScreen
import com.blackink.app.presentation.screens.auth.AuthScreen
import com.blackink.app.presentation.screens.auth.VerifyOtpScreen
import com.blackink.app.presentation.screens.auth.ForgotPasswordScreen
import com.blackink.app.presentation.screens.dashboard.DashboardScreen
import com.blackink.app.presentation.screens.devicedetail.DeviceDetailScreen
import com.blackink.app.presentation.screens.devicecare.DeviceCareScreen
import com.blackink.app.presentation.screens.inventory.InventoryScreen
import com.blackink.app.presentation.screens.reports.ReportsScreen
import com.blackink.app.presentation.screens.sale.Sale1Screen
import com.blackink.app.presentation.screens.sale.Sale2Screen
import com.blackink.app.presentation.screens.sale.Sale3Screen
import com.blackink.app.presentation.screens.sale.SaleCompleteScreen
import com.blackink.app.presentation.screens.sale.SaleViewModel
import com.blackink.app.presentation.screens.invoice.InvoiceScreen
import com.blackink.app.presentation.screens.saleshistory.SalesHistoryScreen
import com.blackink.app.presentation.screens.settings.SettingsScreen
import com.blackink.app.presentation.screens.settings.SecurityScreen
import com.blackink.app.presentation.screens.settlement.SettlementScreen
import com.blackink.app.presentation.screens.setup.Setup1Screen
import com.blackink.app.presentation.screens.setup.Setup2Screen
import com.blackink.app.presentation.screens.setup.Setup3Screen
import com.blackink.app.presentation.screens.setup.SetupViewModel
import com.blackink.app.presentation.screens.splash.SplashScreen
import com.blackink.app.presentation.screens.subscription.SubscriptionScreen
import com.blackink.app.presentation.screens.subscription.SubscriptionViewModel
import com.blackink.app.presentation.screens.welcome.WelcomeScreen
import com.blackink.app.presentation.theme.BlackInkTheme
import com.blackink.app.presentation.theme.FlipTheme
import org.koin.compose.getKoin

/**
 * Root of the shared UI. Reads theme + auth state, then hosts a single [Navigator] whose
 * current [Route] is rendered by [AppNavHost]. This is the one composable both platforms call.
 */
@Composable
fun App() {
    val appVm = rememberViewModel<AppViewModel>()
    val appState by appVm.state.collectAsState()

    BlackInkTheme(darkTheme = appState.isDark) {
        Box(Modifier.fillMaxSize().background(FlipTheme.colors.backgroundSubtle)) {
            AppNavHost(appState.start)
            // Pinned top error banner, above every screen — always visible regardless of scroll.
            ErrorBannerHost(Modifier.align(Alignment.TopCenter))
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
    val reduceMotion = prefersReducedMotion()

    // Shared flow ViewModels — one instance for the whole session.
    val setupVm = rememberViewModel<SetupViewModel>()
    val addDeviceVm = rememberViewModel<AddDeviceViewModel>()
    val saleVm = rememberViewModel<SaleViewModel>()
    val addCostVm = rememberViewModel<AddCostViewModel>()
    val subscriptionVm = rememberViewModel<SubscriptionViewModel>()
    val subscriptionState by subscriptionVm.state.collectAsState()
    val passwordResetUrl by AuthRecovery.requested.collectAsState()

    LaunchedEffect(passwordResetUrl) {
        passwordResetUrl?.let { url ->
            AuthRecovery.consume()
            koin.get<AuthRepository>().handlePasswordResetUrl(url)
            if (navigator.current != Route.Security) navigator.push(Route.Security)
        }
    }

    val openAddDevice: () -> Unit = {
        subscriptionVm.requestAddDevice(
            onAllowed = {
                addDeviceVm.start()
                navigator.push(Route.AddDevice1)
            },
            onLimitReached = {
                if (navigator.current != Route.Subscription) {
                    navigator.push(Route.Subscription)
                }
            },
        )
    }

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
            navigator.current is Route.Auth ||
            navigator.current is Route.VerifyOtp
        when (start) {
            StartDestination.AUTH -> {
                store.clearSession()
                saleVm.reset()
                if (!atEntry) navigator.resetTo(Route.Welcome)
            }
            StartDestination.HOME -> {
                if (atEntry && navigator.current !is Route.Splash) {
                    navigator.resetTo(Route.Dashboard)
                }
            }
            StartDestination.ONBOARDING -> {
                if (atEntry && navigator.current !is Route.Splash) {
                    setupVm.start()
                    navigator.resetTo(Route.Setup1)
                }
            }
            StartDestination.LOADING -> Unit
        }
    }

    // Clear any top error banner when the route changes, so an error never lingers onto an
    // unrelated screen.
    LaunchedEffect(navigator.current) { UiBanner.dismiss() }

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
        AnimatedContent(
            targetState = navigator.current,
            transitionSpec = {
                navigationTransform(
                    direction = navigator.direction,
                    reduceMotion = reduceMotion,
                )
            },
            modifier = Modifier.fillMaxSize(),
            label = "Screen navigation",
        ) { route ->
          when (route) {
        Route.Splash -> SplashScreen(
            isReady = start != StartDestination.LOADING,
            onFinished = {
                when (start) {
                    StartDestination.AUTH -> navigator.replace(Route.Welcome)
                    StartDestination.HOME -> navigator.resetTo(Route.Dashboard)
                    StartDestination.ONBOARDING -> {
                        setupVm.start()
                        navigator.resetTo(Route.Setup1)
                    }
                    StartDestination.LOADING -> Unit
                }
            },
        )

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
            onNeedsVerification = { email -> navigator.push(Route.VerifyOtp(email)) },
            onForgotPassword = { navigator.push(Route.ForgotPassword) },
            onToggleMode = { signUp -> navigator.replace(Route.Auth(signUp = signUp)) },
        )

        is Route.VerifyOtp -> VerifyOtpScreen(
            email = route.email,
            onBack = { navigator.back() },
            // A verified OTP establishes the session; the onboarding-aware effect routes onward.
            onVerified = {},
        )

        Route.ForgotPassword -> ForgotPasswordScreen(onBack = { navigator.back() })

        Route.Setup1 -> Setup1Screen(setupVm, onContinue = { navigator.push(Route.Setup2) }, onBack = { navigator.back() })
        Route.Setup2 -> Setup2Screen(
            setupVm,
            onContinue = { if (setupVm.validateStep(2)) navigator.push(Route.Setup3) },
            onBack = { navigator.back() },
        )
        Route.Setup3 -> Setup3Screen(setupVm, onFinish = { navigator.resetTo(Route.Dashboard) }, onBack = { navigator.back() })

        Route.Dashboard -> DashboardScreen(
            subscriptionAccess = subscriptionState.access,
            onAddDevice = openAddDevice,
            onSeeAllSales = { navigator.push(Route.SalesHistory) },
            onOpenDevice = { id -> navigator.push(Route.DeviceDetail(id)) },
            onOpenSettings = { navigator.push(Route.Settings) },
            onOpenSubscription = { navigator.push(Route.Subscription) },
        )

        Route.Inventory -> InventoryScreen(
            subscriptionAccess = subscriptionState.access,
            onAddDevice = openAddDevice,
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
                onAddAnother = {
                    navigator.resetTo(Route.Dashboard)
                    openAddDevice()
                },
            )
        }

        is Route.DeviceDetail -> DeviceDetailScreen(
            deviceId = route.deviceId,
            onBack = { navigator.back() },
            onAddCost = { addCostVm.start(); navigator.push(Route.AddCost) },
            onOpenCare = { navigator.push(Route.DeviceCare(route.deviceId)) },
            onStartSale = { saleVm.start(); navigator.push(Route.Sale1) },
        )

        is Route.DeviceCare -> DeviceCareScreen(route.deviceId, onBack = { navigator.back() })

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
            onAddAnother = {
                navigator.resetTo(Route.Dashboard)
                openAddDevice()
            },
            onDashboard = { navigator.resetTo(Route.Dashboard) },
            onInvoice = { if (store.lastSale != null) navigator.push(Route.Invoice) },
        )

        Route.Invoice -> store.lastSale?.let { InvoiceScreen(it, onBack = { navigator.back() }) }

        Route.SalesHistory -> SalesHistoryScreen(
            onBack = if (navigator.canGoBack) navigator::back else null,
            onOpenSettlement = { navigator.push(Route.Settlement) },
        )

        Route.Settlement -> SettlementScreen(onBack = { navigator.back() })

        Route.Reports -> ReportsScreen(onBack = { navigator.back() })

        Route.Subscription -> SubscriptionScreen(
            vm = subscriptionVm,
            onBack = { navigator.back() },
        )

        Route.Settings -> SettingsScreen(
            onBack = if (navigator.canGoBack) navigator::back else null,
            onOpenSettlement = { navigator.push(Route.Settlement) },
            onOpenReports = { navigator.push(Route.Reports) },
            onOpenSubscription = { navigator.push(Route.Subscription) },
            onOpenSecurity = { navigator.push(Route.Security) },
            onSignedOut = { navigator.resetTo(Route.Welcome) },
        )

        Route.Security -> SecurityScreen(onBack = { navigator.back() })
          }
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
