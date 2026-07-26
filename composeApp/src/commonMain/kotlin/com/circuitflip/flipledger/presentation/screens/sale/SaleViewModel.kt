package com.circuitflip.flipledger.presentation.screens.sale

import com.circuitflip.flipledger.core.AppError
import com.circuitflip.flipledger.core.onFailure
import com.circuitflip.flipledger.core.onSuccess
import com.circuitflip.flipledger.domain.model.Device
import com.circuitflip.flipledger.domain.model.SaleDraft
import com.circuitflip.flipledger.domain.model.SalesChannel
import com.circuitflip.flipledger.domain.repository.InventoryRepository
import com.circuitflip.flipledger.domain.usecase.CompleteSaleUseCase
import com.circuitflip.flipledger.domain.util.FormValidation
import com.circuitflip.flipledger.domain.util.ProfitCalculator
import com.circuitflip.flipledger.presentation.BaseViewModel
import com.circuitflip.flipledger.presentation.WizardStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class SaleUiState(
    val device: Device? = null,
    val draft: SaleDraft = SaleDraft(),
    val previewNetProfitCents: Long = 0,
    val previewMargin: Double = 0.0,
    val completed: Boolean = false,
    val error: String? = null,
    val fieldErrors: Map<String, String> = emptyMap(),
)

/** Drives the 3-step Sale flow + live profit preview + completion. */
class SaleViewModel(
    private val store: WizardStore,
    private val inventoryRepository: InventoryRepository,
    private val completeSale: CompleteSaleUseCase,
) : BaseViewModel() {

    private val _state = MutableStateFlow(SaleUiState())
    val state = _state.asStateFlow()
    private var deviceJob: Job? = null

    fun start() {
        deviceJob?.cancel()
        store.resetSaleDraft()
        store.lastSale = null
        _state.value = SaleUiState()
        deviceJob = combine(
            inventoryRepositoryDeviceFlow(),
            store.saleDraft,
        ) { device, draft ->
            SaleUiState(
                device = device,
                draft = draft,
                previewNetProfitCents = device?.let { ProfitCalculator.previewNetProfitCents(it, draft) } ?: 0,
                previewMargin = device?.let { ProfitCalculator.previewMargin(it, draft) } ?: 0.0,
                completed = false,
                error = _state.value.error,
                fieldErrors = _state.value.fieldErrors,
            )
        }.onEach { _state.value = it }.launchIn(scope)
    }

    fun reset() {
        deviceJob?.cancel()
        deviceJob = null
        _state.value = SaleUiState()
    }

    private fun inventoryRepositoryDeviceFlow() =
        inventoryRepository.observeDevice(store.selectedDeviceId ?: "")

    fun setPrice(v: String) =
        updateField("price") { it.copy(price = v.filter { c -> c.isDigit() || c == '.' }) }
    fun setDate(v: String) = updateField("date") { it.copy(date = v) }
    fun setChannel(c: SalesChannel) = updateField("channel") { it.copy(channel = c) }
    fun setCustomerName(v: String) = updateField("customerName") { it.copy(customerName = v) }
    fun setCustomerEmail(v: String) = updateField("customerEmail") { it.copy(customerEmail = v) }
    fun setCustomerPhone(v: String) = updateField("customerPhone") { it.copy(customerPhone = v) }
    fun setCustomerAddress(v: String) = updateField("customerAddress") { it.copy(customerAddress = v) }
    fun setPlatformFee(v: String) = updateField("platformFee") { it.copy(platformFee = money(v)) }
    fun setPaymentFee(v: String) = updateField("paymentFee") { it.copy(paymentFee = money(v)) }
    fun setShipping(v: String) = updateField("shipping") { it.copy(shipping = money(v)) }
    fun setPackaging(v: String) = updateField("packaging") { it.copy(packaging = money(v)) }
    fun setOtherFee(v: String) = updateField("otherFee") { it.copy(otherFee = money(v)) }
    private fun money(v: String) = v.filter { it.isDigit() || it == '.' }

    private val _submitting = MutableStateFlow(false)
    val submitting = _submitting.asStateFlow()

    fun validateStep(step: Int): Boolean {
        val device = _state.value.device
        if (device == null) {
            _state.value = _state.value.copy(error = "The selected device could not be loaded.")
            return false
        }
        val allErrors = FormValidation.sale(device, store.saleDraft.value)
        val fields = when (step) {
            1 -> setOf("price", "date", "channel")
            2 -> setOf("platformFee", "paymentFee", "shipping", "packaging", "otherFee")
            else -> allErrors.keys
        }
        val errors = allErrors.filterKeys { it in fields }
        _state.value = _state.value.copy(
            error = null,
            fieldErrors = _state.value.fieldErrors.filterKeys { it !in fields } + errors,
        )
        return errors.isEmpty()
    }

    fun complete() {
        if (_submitting.value) return
        val device = _state.value.device
        if (device == null) {
            _state.value = _state.value.copy(error = "The selected device could not be loaded.")
            return
        }
        val fieldErrors = FormValidation.sale(device, store.saleDraft.value)
        if (fieldErrors.isNotEmpty()) {
            _state.value = _state.value.copy(error = null, fieldErrors = fieldErrors)
            return
        }
        _submitting.value = true
        _state.value = _state.value.copy(error = null, fieldErrors = emptyMap())
        scope.launch {
            completeSale(device, store.saleDraft.value)
                .onSuccess { sale ->
                    store.lastSale = sale
                    _state.value = _state.value.copy(completed = true)
                }
                .onFailure(::showError)
            _submitting.value = false
        }
    }

    private fun updateField(field: String, update: (SaleDraft) -> SaleDraft) {
        store.updateSale(update)
        _state.value = _state.value.copy(
            error = null,
            fieldErrors = _state.value.fieldErrors - field,
        )
    }

    private fun showError(error: AppError) {
        _state.value = if (error is AppError.Validation) {
            _state.value.copy(
                error = null,
                fieldErrors = _state.value.fieldErrors + (error.field to error.message),
            )
        } else {
            _state.value.copy(error = error.userMessage())
        }
    }
}
