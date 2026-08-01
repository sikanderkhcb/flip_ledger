package com.blackink.app.presentation.screens.addcost

import com.blackink.app.core.AppError
import com.blackink.app.core.onFailure
import com.blackink.app.core.onSuccess
import com.blackink.app.domain.model.CostDraft
import com.blackink.app.domain.model.CostType
import com.blackink.app.domain.model.PaidBy
import com.blackink.app.domain.usecase.AddCostUseCase
import com.blackink.app.domain.util.FormValidation
import com.blackink.app.presentation.BaseViewModel
import com.blackink.app.presentation.WizardStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddCostViewModel(
    private val store: WizardStore,
    private val addCost: AddCostUseCase,
) : BaseViewModel() {

    val draft: StateFlow<CostDraft> = store.costDraft

    private val _saved = MutableStateFlow(false)
    val saved = _saved.asStateFlow()

    private val _submitting = MutableStateFlow(false)
    val submitting = _submitting.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _fieldErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val fieldErrors = _fieldErrors.asStateFlow()

    fun start() {
        store.resetCostDraft()
        _saved.value = false
        _submitting.value = false
        _error.value = null
        _fieldErrors.value = emptyMap()
    }

    fun setType(t: CostType) = updateField("type") { it.copy(type = t) }
    fun setAmount(v: String) =
        updateField("amount") { it.copy(amount = v.filter { c -> c.isDigit() || c == '.' }) }
    fun setPaidBy(p: PaidBy) = updateField("paidBy") { it.copy(paidBy = p) }
    fun setDate(v: String) = updateField("date") { it.copy(date = v) }
    fun setNote(v: String) = updateField("note") { it.copy(note = v) }

    fun save() {
        if (_submitting.value) return
        val fieldErrors = FormValidation.cost(store.costDraft.value)
        if (fieldErrors.isNotEmpty()) {
            _fieldErrors.value = fieldErrors
            _error.value = null
            return
        }
        val id = store.selectedDeviceId
        if (id == null) {
            _error.value = "Choose a device before adding an expense."
            return
        }
        _submitting.value = true
        _error.value = null
        _fieldErrors.value = emptyMap()
        scope.launch {
            addCost(id, store.costDraft.value)
                .onSuccess { _saved.value = true }
                .onFailure(::showError)
            _submitting.value = false
        }
    }

    private fun updateField(field: String, update: (CostDraft) -> CostDraft) {
        store.updateCost(update)
        _fieldErrors.value = _fieldErrors.value - field
        _error.value = null
    }

    private fun showError(error: AppError) {
        if (error is AppError.Validation) {
            _fieldErrors.value = _fieldErrors.value + (error.field to error.message)
            _error.value = null
        } else {
            _error.value = error.userMessage()
        }
    }
}
