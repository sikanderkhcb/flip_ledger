package com.circuitflip.flipledger.presentation.screens.dashboard

import com.circuitflip.flipledger.domain.model.AttentionItem
import com.circuitflip.flipledger.domain.model.BusinessProfile
import com.circuitflip.flipledger.domain.model.DashboardMetrics
import com.circuitflip.flipledger.domain.model.Sale
import com.circuitflip.flipledger.domain.repository.ProfileRepository
import com.circuitflip.flipledger.domain.usecase.GetAttentionItemsUseCase
import com.circuitflip.flipledger.domain.usecase.GetDashboardMetricsUseCase
import com.circuitflip.flipledger.domain.usecase.ObserveInventoryUseCase
import com.circuitflip.flipledger.domain.usecase.ObserveSalesUseCase
import com.circuitflip.flipledger.presentation.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

data class DashboardUiState(
    val loading: Boolean = true,
    val profile: BusinessProfile = BusinessProfile(),
    val metrics: DashboardMetrics? = null,
    val attention: List<AttentionItem> = emptyList(),
    val recentSales: List<Sale> = emptyList(),
)

class DashboardViewModel(
    observeInventory: ObserveInventoryUseCase,
    observeSales: ObserveSalesUseCase,
    profileRepository: ProfileRepository,
    private val getMetrics: GetDashboardMetricsUseCase,
    private val getAttention: GetAttentionItemsUseCase,
) : BaseViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state = _state.asStateFlow()

    init {
        combine(
            observeInventory(),
            observeSales(),
            profileRepository.observeProfile(),
        ) { inventory, sales, profile ->
            DashboardUiState(
                loading = false,
                profile = profile,
                metrics = getMetrics(inventory, sales),
                attention = getAttention(inventory),
                recentSales = sales.take(3),
            )
        }.onEach { _state.value = it }.launchIn(scope)
    }
}
