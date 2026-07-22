package com.circuitflip.flipledger.presentation.screens.devicedetail

import com.circuitflip.flipledger.domain.model.Device
import com.circuitflip.flipledger.domain.model.DeviceStatus
import com.circuitflip.flipledger.domain.usecase.ObserveDeviceUseCase
import com.circuitflip.flipledger.domain.usecase.UpdateDeviceStatusUseCase
import com.circuitflip.flipledger.domain.util.ProfitCalculator
import com.circuitflip.flipledger.presentation.BaseViewModel
import com.circuitflip.flipledger.presentation.WizardStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class DeviceDetailUiState(
    val device: Device? = null,
    val expectedProfitCents: Long = 0,
)

class DeviceDetailViewModel(
    private val store: WizardStore,
    private val observeDevice: ObserveDeviceUseCase,
    private val updateStatus: UpdateDeviceStatusUseCase,
) : BaseViewModel() {

    private val _state = MutableStateFlow(DeviceDetailUiState())
    val state = _state.asStateFlow()

    /** True while a status change is being written to the backend. */
    private val _submitting = MutableStateFlow(false)
    val submitting = _submitting.asStateFlow()

    fun load(deviceId: String) {
        store.selectedDeviceId = deviceId
        observeDevice(deviceId).onEach { device ->
            _state.value = DeviceDetailUiState(
                device = device,
                expectedProfitCents = device?.let { ProfitCalculator.expectedProfitCents(it) } ?: 0,
            )
        }.launchIn(scope)
    }

    /** Returns the label + action for the status-appropriate primary button. */
    fun primaryActionLabel(status: DeviceStatus): String = when (status) {
        DeviceStatus.PURCHASED, DeviceStatus.REPAIR -> "Mark Ready to List"
        DeviceStatus.READY -> "Mark as Listed"
        DeviceStatus.LISTED, DeviceStatus.SOLD -> "Complete Sale"
    }

    /** @return true if the action starts the sale flow (caller navigates); false if handled here. */
    fun onPrimaryAction(status: DeviceStatus): Boolean = when (status) {
        DeviceStatus.PURCHASED, DeviceStatus.REPAIR -> { changeStatus(DeviceStatus.READY); false }
        DeviceStatus.READY -> { changeStatus(DeviceStatus.LISTED); false }
        DeviceStatus.LISTED, DeviceStatus.SOLD -> true
    }

    private fun changeStatus(status: DeviceStatus) {
        val id = store.selectedDeviceId ?: return
        if (_submitting.value) return
        _submitting.value = true
        scope.launch {
            updateStatus(id, status)
            _submitting.value = false
        }
    }
}
