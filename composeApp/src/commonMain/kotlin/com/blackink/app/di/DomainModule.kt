package com.blackink.app.di

import com.blackink.app.domain.usecase.AddCostUseCase
import com.blackink.app.domain.usecase.AddDeviceUseCase
import com.blackink.app.domain.usecase.CompleteSaleUseCase
import com.blackink.app.domain.usecase.GetAttentionItemsUseCase
import com.blackink.app.domain.usecase.GetDashboardMetricsUseCase
import com.blackink.app.domain.usecase.GetReportMetricsUseCase
import com.blackink.app.domain.usecase.GetSettlementUseCase
import com.blackink.app.domain.usecase.ObserveDeviceUseCase
import com.blackink.app.domain.usecase.ObserveInventoryUseCase
import com.blackink.app.domain.usecase.ObserveSalesUseCase
import com.blackink.app.domain.usecase.UpdateDeviceStatusUseCase
import com.blackink.app.domain.usecase.UpdateDeviceCareUseCase
import com.blackink.app.domain.usecase.DeleteDeviceUseCase
import org.koin.dsl.module

/** Use case factories. Stateless, so plain factories are fine. */
val domainModule = module {
    factory { ObserveInventoryUseCase(get()) }
    factory { ObserveDeviceUseCase(get()) }
    factory { AddDeviceUseCase(get(), get(), get()) }
    factory { UpdateDeviceStatusUseCase(get()) }
    factory { UpdateDeviceCareUseCase(get()) }
    factory { DeleteDeviceUseCase(get()) }
    factory { AddCostUseCase(get()) }
    factory { ObserveSalesUseCase(get()) }
    factory { CompleteSaleUseCase(get()) }
    factory { GetDashboardMetricsUseCase() }
    factory { GetAttentionItemsUseCase() }
    factory { GetSettlementUseCase() }
    factory { GetReportMetricsUseCase() }
}
