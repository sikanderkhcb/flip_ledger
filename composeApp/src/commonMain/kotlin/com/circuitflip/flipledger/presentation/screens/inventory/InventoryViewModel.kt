package com.circuitflip.flipledger.presentation.screens.inventory

import com.circuitflip.flipledger.domain.model.Device
import com.circuitflip.flipledger.domain.model.DeviceStatus
import com.circuitflip.flipledger.domain.usecase.ObserveInventoryUseCase
import com.circuitflip.flipledger.presentation.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

data class InventoryUiState(
    val query: String = "",
    val statusFilter: DeviceStatus? = null,   // null == "All"
    val devices: List<Device> = emptyList(),
    val totalInvestedCents: Long = 0,
    val error: String? = null,
)

class InventoryViewModel(observeInventory: ObserveInventoryUseCase) : BaseViewModel() {

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow<DeviceStatus?>(null)

    private val _state = MutableStateFlow(InventoryUiState())
    val state = _state.asStateFlow()

    init {
        combine(observeInventory(), query, filter, observeInventory.error) { all, q, f, error ->
            val filtered = all
                .filter { f == null || it.status == f }
                .filter { q.isBlank() || it.model.contains(q, ignoreCase = true) }
            InventoryUiState(
                query = q,
                statusFilter = f,
                devices = filtered,
                totalInvestedCents = all.sumOf { it.investedCents },
                error = error?.userMessage(),
            )
        }.onEach { _state.value = it }.launchIn(scope)
    }

    // Search is transient rather than persisted, so cap it instead of showing a form error.
    fun onQuery(v: String) = query.update { v.take(MAX_SEARCH_LENGTH) }
    fun onFilter(status: DeviceStatus?) = filter.update { status }

    private companion object {
        const val MAX_SEARCH_LENGTH = 100
    }
}
