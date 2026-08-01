package com.blackink.app.di

import com.blackink.app.presentation.AppViewModel
import com.blackink.app.presentation.WizardStore
import com.blackink.app.presentation.navigation.Navigator
import com.blackink.app.presentation.navigation.Route
import com.blackink.app.presentation.screens.addcost.AddCostViewModel
import com.blackink.app.presentation.screens.adddevice.AddDeviceViewModel
import com.blackink.app.presentation.screens.auth.AuthViewModel
import com.blackink.app.presentation.screens.auth.VerifyOtpViewModel
import com.blackink.app.presentation.screens.dashboard.DashboardViewModel
import com.blackink.app.presentation.screens.devicedetail.DeviceDetailViewModel
import com.blackink.app.presentation.screens.devicecare.DeviceCareViewModel
import com.blackink.app.presentation.screens.inventory.InventoryViewModel
import com.blackink.app.presentation.screens.invoice.InvoiceViewModel
import com.blackink.app.presentation.screens.reports.ReportsViewModel
import com.blackink.app.presentation.screens.sale.SaleViewModel
import com.blackink.app.presentation.screens.saleshistory.SalesHistoryViewModel
import com.blackink.app.presentation.screens.settings.SettingsViewModel
import com.blackink.app.presentation.screens.settlement.SettlementViewModel
import com.blackink.app.presentation.screens.setup.SetupViewModel
import com.blackink.app.presentation.screens.subscription.SubscriptionViewModel
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
    factory { VerifyOtpViewModel(get()) }
    factory { SetupViewModel(get()) }
    factory { DashboardViewModel(get(), get(), get(), get(), get()) }
    factory { InventoryViewModel(get()) }
    factory { AddDeviceViewModel(get(), get()) }
    factory { DeviceDetailViewModel(get(), get(), get(), get()) }
    factory { DeviceCareViewModel(get(), get(), get()) }
    factory { AddCostViewModel(get(), get()) }
    factory { SaleViewModel(get(), get(), get()) }
    factory { SalesHistoryViewModel(get()) }
    factory { SettlementViewModel(get(), get(), get()) }
    factory { ReportsViewModel(get(), get(), get(), get()) }
    factory { SettingsViewModel(get(), get(), get()) }
    factory { SubscriptionViewModel(get()) }
    factory { InvoiceViewModel(get()) }
}
