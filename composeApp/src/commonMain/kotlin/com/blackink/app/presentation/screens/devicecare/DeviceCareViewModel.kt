package com.blackink.app.presentation.screens.devicecare

import com.blackink.app.core.AppError
import com.blackink.app.core.onFailure
import com.blackink.app.core.onSuccess
import com.blackink.app.domain.model.Device
import com.blackink.app.domain.model.DeviceCareDraft
import com.blackink.app.domain.model.DeviceStatus
import com.blackink.app.domain.usecase.ObserveDeviceUseCase
import com.blackink.app.domain.usecase.UpdateDeviceCareUseCase
import com.blackink.app.domain.usecase.UpdateDeviceStatusUseCase
import com.blackink.app.presentation.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class DeviceCareUiState(
    val device: Device? = null,
    val draft: DeviceCareDraft = DeviceCareDraft(),
    val saving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
)

class DeviceCareViewModel(
    private val observeDevice: ObserveDeviceUseCase,
    private val updateCare: UpdateDeviceCareUseCase,
    private val updateStatus: UpdateDeviceStatusUseCase,
) : BaseViewModel() {
    private val _state = MutableStateFlow(DeviceCareUiState())
    val state = _state.asStateFlow()

    fun load(deviceId: String) {
        observeDevice(deviceId).onEach { device ->
            if (device != null) {
                _state.value = _state.value.copy(
                    device = device,
                    draft = DeviceCareDraft(
                        repairIssue = device.repairIssue,
                        repairProvider = device.repairProvider,
                        repairStartedOn = device.repairStartedOn.orEmpty(),
                        repairCompletedOn = device.repairCompletedOn.orEmpty(),
                        warrantyProvider = device.warrantyProvider,
                        warrantyExpiresOn = device.warrantyExpiresOn.orEmpty(),
                    ),
                )
            }
        }.launchIn(scope)
    }

    fun update(update: (DeviceCareDraft) -> DeviceCareDraft) {
        _state.value = _state.value.copy(draft = update(_state.value.draft), error = null, saved = false)
    }

    fun save(deviceId: String) {
        if (_state.value.saving) return
        _state.value = _state.value.copy(saving = true, error = null, saved = false)
        scope.launch {
            updateCare(deviceId, _state.value.draft)
                .onSuccess { _state.value = _state.value.copy(saved = true) }
                .onFailure { error -> _state.value = _state.value.copy(error = error.userMessage()) }
            _state.value = _state.value.copy(saving = false)
        }
    }

    fun toggleRepairStatus(deviceId: String) {
        val target = if (_state.value.device?.status == DeviceStatus.REPAIR) {
            DeviceStatus.READY
        } else {
            DeviceStatus.REPAIR
        }
        if (_state.value.saving) return
        _state.value = _state.value.copy(saving = true, error = null)
        scope.launch {
            updateStatus(deviceId, target)
                .onFailure { error -> _state.value = _state.value.copy(error = error.userMessage()) }
            _state.value = _state.value.copy(saving = false)
        }
    }
}
