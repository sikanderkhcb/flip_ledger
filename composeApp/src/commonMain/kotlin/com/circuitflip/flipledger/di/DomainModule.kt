package com.circuitflip.flipledger.di

import com.circuitflip.flipledger.domain.usecase.AddCostUseCase
import com.circuitflip.flipledger.domain.usecase.AddDeviceUseCase
import com.circuitflip.flipledger.domain.usecase.CompleteSaleUseCase
import com.circuitflip.flipledger.domain.usecase.GetAttentionItemsUseCase
import com.circuitflip.flipledger.domain.usecase.GetDashboardMetricsUseCase
import com.circuitflip.flipledger.domain.usecase.GetReportMetricsUseCase
import com.circuitflip.flipledger.domain.usecase.GetSettlementUseCase
import com.circuitflip.flipledger.domain.usecase.ObserveDeviceUseCase
import com.circuitflip.flipledger.domain.usecase.ObserveInventoryUseCase
import com.circuitflip.flipledger.domain.usecase.ObserveSalesUseCase
import com.circuitflip.flipledger.domain.usecase.UpdateDeviceStatusUseCase
import org.koin.dsl.module

/** Use case factories. Stateless, so plain factories are fine. */
val domainModule = module {
    factory { ObserveInventoryUseCase(get()) }
    factory { ObserveDeviceUseCase(get()) }
    factory { AddDeviceUseCase(get(), get()) }
    factory { UpdateDeviceStatusUseCase(get()) }
    factory { AddCostUseCase(get()) }
    factory { ObserveSalesUseCase(get()) }
    factory { CompleteSaleUseCase(get()) }
    factory { GetDashboardMetricsUseCase() }
    factory { GetAttentionItemsUseCase() }
    factory { GetSettlementUseCase() }
    factory { GetReportMetricsUseCase() }
}
