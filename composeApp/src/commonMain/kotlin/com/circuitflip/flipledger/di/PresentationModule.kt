package com.circuitflip.flipledger.di

import com.circuitflip.flipledger.presentation.AppViewModel
import com.circuitflip.flipledger.presentation.WizardStore
import com.circuitflip.flipledger.presentation.navigation.Navigator
import com.circuitflip.flipledger.presentation.navigation.Route
import com.circuitflip.flipledger.presentation.screens.addcost.AddCostViewModel
import com.circuitflip.flipledger.presentation.screens.adddevice.AddDeviceViewModel
import com.circuitflip.flipledger.presentation.screens.auth.AuthViewModel
import com.circuitflip.flipledger.presentation.screens.dashboard.DashboardViewModel
import com.circuitflip.flipledger.presentation.screens.devicedetail.DeviceDetailViewModel
import com.circuitflip.flipledger.presentation.screens.inventory.InventoryViewModel
import com.circuitflip.flipledger.presentation.screens.reports.ReportsViewModel
import com.circuitflip.flipledger.presentation.screens.sale.SaleViewModel
import com.circuitflip.flipledger.presentation.screens.saleshistory.SalesHistoryViewModel
import com.circuitflip.flipledger.presentation.screens.settings.SettingsViewModel
import com.circuitflip.flipledger.presentation.screens.settlement.SettlementViewModel
import com.circuitflip.flipledger.presentation.screens.setup.SetupViewModel
import org.koin.dsl.module

/**
 * Presentation wiring. Navigator + WizardStore are singletons (shared nav + wizard state);
 * ViewModels are factories so each screen entry gets a fresh instance with its own scope.
 */
val presentationModule = module {
    single { Navigator(start = Route.Splash) }
    single { WizardStore() }

    factory { AppViewModel(get(), get(), get()) }
    factory { AuthViewModel(get()) }
    factory { SetupViewModel(get()) }
    factory { DashboardViewModel(get(), get(), get(), get(), get()) }
    factory { InventoryViewModel(get()) }
    factory { AddDeviceViewModel(get(), get()) }
    factory { DeviceDetailViewModel(get(), get(), get()) }
    factory { AddCostViewModel(get(), get()) }
    factory { SaleViewModel(get(), get(), get()) }
    factory { SalesHistoryViewModel(get()) }
    factory { SettlementViewModel(get(), get(), get()) }
    factory { ReportsViewModel(get(), get(), get()) }
    factory { SettingsViewModel(get(), get(), get()) }
}
