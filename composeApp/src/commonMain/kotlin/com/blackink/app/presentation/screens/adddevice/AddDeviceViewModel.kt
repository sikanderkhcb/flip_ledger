package com.blackink.app.presentation.screens.adddevice

import com.blackink.app.core.AppError
import com.blackink.app.core.onFailure
import com.blackink.app.core.onSuccess
import com.blackink.app.domain.model.AcquisitionSource
import com.blackink.app.domain.model.DeviceCategory
import com.blackink.app.domain.model.DeviceCondition
import com.blackink.app.domain.model.DeviceDraft
import com.blackink.app.domain.model.LockStatus
import com.blackink.app.domain.usecase.AddDeviceUseCase
import com.blackink.app.domain.util.FormValidation
import com.blackink.app.presentation.BaseViewModel
import com.blackink.app.presentation.WizardStore
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

    private val _fieldErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val fieldErrors = _fieldErrors.asStateFlow()

    fun start() {
        store.resetDeviceDraft()
        _submitted.value = false
        _submitting.value = false
        _error.value = null
        _fieldErrors.value = emptyMap()
    }

    fun setCategory(c: DeviceCategory) = updateField("category") { it.copy(category = c) }
    fun setModel(v: String) = updateField("model") { it.copy(model = v) }
    fun setPrice(v: String) =
        updateField("price") { it.copy(price = v.filter { ch -> ch.isDigit() || ch == '.' }) }
    fun setDate(v: String) = updateField("date") { it.copy(date = v) }
    fun setSource(s: AcquisitionSource) = updateField("source") { it.copy(source = s) }
    fun setCondition(c: DeviceCondition) = updateField("condition") { it.copy(condition = c) }
    fun setIdentifier(v: String) = updateField("identifier") { it.copy(identifierLast4 = v) }
    fun setStorage(v: String) = updateField("storage") { it.copy(storage = v) }
    fun setLock(l: LockStatus) = updateField("lock") { it.copy(lock = l) }

    fun validateStep(step: Int): Boolean {
        val errors = when (step) {
            1 -> FormValidation.deviceStep1(draft.value)
            2 -> FormValidation.deviceStep2(draft.value)
            3 -> FormValidation.deviceStep3(draft.value)
            else -> FormValidation.device(draft.value)
        }
        _fieldErrors.value = _fieldErrors.value
            .filterKeys { key -> key !in fieldsForStep(step) } + errors
        _error.value = null
        return errors.isEmpty()
    }

    fun submit() {
        if (_submitting.value) return
        val fieldErrors = FormValidation.device(store.deviceDraft.value)
        if (fieldErrors.isNotEmpty()) {
            _fieldErrors.value = fieldErrors
            _error.value = fieldErrors.values.first()
            return
        }
        _submitting.value = true
        _error.value = null
        _fieldErrors.value = emptyMap()
        scope.launch {
            addDevice(store.deviceDraft.value)
                .onSuccess { device ->
                    store.lastAddedDeviceId = device.id
                    store.selectedDeviceId = device.id
                    _submitted.value = true
                }
                .onFailure(::showError)
            _submitting.value = false
        }
    }

    private fun updateField(field: String, update: (DeviceDraft) -> DeviceDraft) {
        store.updateDevice(update)
        _fieldErrors.value = _fieldErrors.value - field
        _error.value = null
    }

    private fun showError(error: AppError) {
        if (error is AppError.Validation) {
            _fieldErrors.value = _fieldErrors.value + (error.field to error.message)
            _error.value = error.message
        } else {
            _error.value = error.userMessage()
        }
    }

    private fun fieldsForStep(step: Int): Set<String> = when (step) {
        1 -> setOf("category", "model")
        2 -> setOf("price", "date", "source")
        3 -> setOf("condition", "identifier", "storage", "lock")
        else -> _fieldErrors.value.keys
    }
}
