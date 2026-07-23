package com.circuitflip.flipledger.presentation.screens.adddevice

import com.circuitflip.flipledger.core.onFailure
import com.circuitflip.flipledger.core.onSuccess
import com.circuitflip.flipledger.domain.model.AcquisitionSource
import com.circuitflip.flipledger.domain.model.DeviceCategory
import com.circuitflip.flipledger.domain.model.DeviceCondition
import com.circuitflip.flipledger.domain.model.DeviceDraft
import com.circuitflip.flipledger.domain.model.LockStatus
import com.circuitflip.flipledger.domain.usecase.AddDeviceUseCase
import com.circuitflip.flipledger.presentation.BaseViewModel
import com.circuitflip.flipledger.presentation.WizardStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Drives the 4-step Add Device wizard. Draft lives in [WizardStore] so any step can read it
 * (the review step shows every field). On submit the device is persisted and its id stored
 * for the success + detail screens.
 */
class AddDeviceViewModel(
    private val store: WizardStore,
    private val addDevice: AddDeviceUseCase,
) : BaseViewModel() {

    val draft: StateFlow<DeviceDraft> = store.deviceDraft

    private val _submitted = MutableStateFlow(false)
    val submitted = _submitted.asStateFlow()

    /** True while the device is being saved to the backend, to drive the button spinner. */
    private val _submitting = MutableStateFlow(false)
    val submitting = _submitting.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun start() {
        store.resetDeviceDraft()
        _submitted.value = false
        _submitting.value = false
        _error.value = null
    }

    fun setCategory(c: DeviceCategory) = store.updateDevice { it.copy(category = c) }
    fun setModel(v: String) = store.updateDevice { it.copy(model = v) }
    fun setPrice(v: String) = store.updateDevice { it.copy(price = v.filter { ch -> ch.isDigit() || ch == '.' }) }
    fun setDate(v: String) = store.updateDevice { it.copy(date = v) }
    fun setSource(s: AcquisitionSource) = store.updateDevice { it.copy(source = s) }
    fun setCondition(c: DeviceCondition) = store.updateDevice { it.copy(condition = c) }
    fun setIdentifier(v: String) = store.updateDevice { it.copy(identifierLast4 = v) }
    fun setStorage(v: String) = store.updateDevice { it.copy(storage = v) }
    fun setLock(l: LockStatus) = store.updateDevice { it.copy(lock = l) }

    fun submit() {
        if (_submitting.value) return
        _submitting.value = true
        _error.value = null
        scope.launch {
            addDevice(store.deviceDraft.value)
                .onSuccess { device ->
                    store.lastAddedDeviceId = device.id
                    store.selectedDeviceId = device.id
                    _submitted.value = true
                }
                .onFailure { _error.value = it.userMessage() }
            _submitting.value = false
        }
    }
}
