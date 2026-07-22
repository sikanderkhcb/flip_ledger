package com.circuitflip.flipledger.presentation.screens.addcost

import com.circuitflip.flipledger.domain.model.CostDraft
import com.circuitflip.flipledger.domain.model.CostType
import com.circuitflip.flipledger.domain.model.PaidBy
import com.circuitflip.flipledger.domain.usecase.AddCostUseCase
import com.circuitflip.flipledger.presentation.BaseViewModel
import com.circuitflip.flipledger.presentation.WizardStore
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

    fun start() { store.resetCostDraft(); _saved.value = false; _submitting.value = false }

    fun setType(t: CostType) = store.updateCost { it.copy(type = t) }
    fun setAmount(v: String) = store.updateCost { it.copy(amount = v.filter { c -> c.isDigit() || c == '.' }) }
    fun setPaidBy(p: PaidBy) = store.updateCost { it.copy(paidBy = p) }
    fun setDate(v: String) = store.updateCost { it.copy(date = v) }
    fun setNote(v: String) = store.updateCost { it.copy(note = v) }

    fun save() {
        val id = store.selectedDeviceId ?: return
        if (_submitting.value) return
        _submitting.value = true
        scope.launch {
            addCost(id, store.costDraft.value)
            _saved.value = true
            _submitting.value = false
        }
    }
}
