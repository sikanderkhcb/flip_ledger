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
)

class InventoryViewModel(observeInventory: ObserveInventoryUseCase) : BaseViewModel() {

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow<DeviceStatus?>(null)

    private val _state = MutableStateFlow(InventoryUiState())
    val state = _state.asStateFlow()

    init {
        combine(observeInventory(), query, filter) { all, q, f ->
            val filtered = all
                .filter { f == null || it.status == f }
                .filter { q.isBlank() || it.model.contains(q, ignoreCase = true) }
            InventoryUiState(
                query = q,
                statusFilter = f,
                devices = filtered,
                totalInvestedCents = all.sumOf { it.investedCents },
            )
        }.onEach { _state.value = it }.launchIn(scope)
    }

    fun onQuery(v: String) = query.update { v }
    fun onFilter(status: DeviceStatus?) = filter.update { status }
}
