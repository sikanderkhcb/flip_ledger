package com.circuitflip.flipledger.presentation.screens.devicedetail

import com.circuitflip.flipledger.core.onFailure
import com.circuitflip.flipledger.core.onSuccess
import com.circuitflip.flipledger.domain.model.Device
import com.circuitflip.flipledger.domain.model.DeviceStatus
import com.circuitflip.flipledger.domain.usecase.ObserveDeviceUseCase
import com.circuitflip.flipledger.domain.usecase.UpdateDeviceStatusUseCase
import com.circuitflip.flipledger.domain.usecase.DeleteDeviceUseCase
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
    val error: String? = null,
)

class DeviceDetailViewModel(
    private val store: WizardStore,
    private val observeDevice: ObserveDeviceUseCase,
    private val updateStatus: UpdateDeviceStatusUseCase,
    private val deleteDevice: DeleteDeviceUseCase,
) : BaseViewModel() {

    private val _state = MutableStateFlow(DeviceDetailUiState())
    val state = _state.asStateFlow()

    /** True while a status change is being written to the backend. */
    private val _submitting = MutableStateFlow(false)
    val submitting = _submitting.asStateFlow()

    private val _deleted = MutableStateFlow(false)
    val deleted = _deleted.asStateFlow()

    fun load(deviceId: String) {
        _deleted.value = false
        store.selectedDeviceId = deviceId
        observeDevice(deviceId).onEach { device ->
            _state.value = DeviceDetailUiState(
                device = device,
                expectedProfitCents = device?.let { ProfitCalculator.expectedProfitCents(it) } ?: 0,
                error = _state.value.error,
            )
        }.launchIn(scope)
    }

    fun delete(deviceId: String) {
        if (_submitting.value) return
        _submitting.value = true
        _state.value = _state.value.copy(error = null)
        scope.launch {
            deleteDevice(deviceId)
                .onSuccess { _deleted.value = true }
                .onFailure { _state.value = _state.value.copy(error = it.userMessage()) }
            _submitting.value = false
        }
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
        _state.value = _state.value.copy(error = null)
        scope.launch {
            updateStatus(id, status)
                .onFailure { _state.value = _state.value.copy(error = it.userMessage()) }
            _submitting.value = false
        }
    }
}
