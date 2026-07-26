package com.circuitflip.flipledger.presentation.screens.dashboard

import com.circuitflip.flipledger.domain.model.AttentionItem
import com.circuitflip.flipledger.domain.model.BusinessProfile
import com.circuitflip.flipledger.domain.model.DashboardMetrics
import com.circuitflip.flipledger.domain.model.Sale
import com.circuitflip.flipledger.domain.model.CategoryBars
import com.circuitflip.flipledger.domain.model.CategoryCount
import com.circuitflip.flipledger.domain.model.DeviceCategory
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
    val categoryCounts: List<CategoryCount> = emptyList(),
    val categoryBars: List<CategoryBars> = emptyList(),
    val error: String? = null,
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
            observeInventory.error,
            observeSales.error,
        ) { inventory, sales, profile, inventoryError, salesError ->
            DashboardUiState(
                loading = false,
                profile = profile,
                metrics = getMetrics(inventory, sales),
                attention = getAttention(inventory),
                recentSales = sales.take(3),
                categoryCounts = categoryCounts(inventory, sales),
                categoryBars = categoryBars(inventory, sales),
                error = (inventoryError ?: salesError)?.userMessage(),
            )
        }.onEach { _state.value = it }.launchIn(scope)
    }

    private fun categoryCounts(inventory: List<com.circuitflip.flipledger.domain.model.Device>, sales: List<Sale>): List<CategoryCount> =
        DeviceCategory.entries.map { category ->
            CategoryCount(category.label, inventory.count { it.category == category } + sales.count { categoryFor(it.model) == category })
        }.filter { it.count > 0 }

    private fun categoryBars(inventory: List<com.circuitflip.flipledger.domain.model.Device>, sales: List<Sale>): List<CategoryBars> =
        DeviceCategory.entries.map { category ->
            CategoryBars(
                category.label,
                inventory.count { it.category == category } + sales.count { categoryFor(it.model) == category },
                sales.count { categoryFor(it.model) == category },
            )
        }.filter { it.bought > 0 || it.sold > 0 }

    private fun categoryFor(model: String): DeviceCategory = when {
        Regex("iphone|galaxy|pixel|phone", RegexOption.IGNORE_CASE).containsMatchIn(model) -> DeviceCategory.PHONE
        Regex("macbook|laptop|thinkpad", RegexOption.IGNORE_CASE).containsMatchIn(model) -> DeviceCategory.LAPTOP
        Regex("ipad|tablet", RegexOption.IGNORE_CASE).containsMatchIn(model) -> DeviceCategory.TABLET
        Regex("xbox|switch|playstation|ps5|steam deck", RegexOption.IGNORE_CASE).containsMatchIn(model) -> DeviceCategory.GAMING
        else -> DeviceCategory.ACCESSORY
    }
}
