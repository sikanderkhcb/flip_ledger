package com.circuitflip.flipledger.presentation.screens.setup

import com.circuitflip.flipledger.core.AppError
import com.circuitflip.flipledger.domain.model.BusinessProfile
import com.circuitflip.flipledger.domain.model.Currency
import com.circuitflip.flipledger.domain.model.WorkspaceType
import com.circuitflip.flipledger.domain.repository.ProfileRepository
import com.circuitflip.flipledger.domain.util.FormValidation
import com.circuitflip.flipledger.presentation.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SetupUiState(
    val workspaceType: WorkspaceType = WorkspaceType.PARTNER,
    val businessName: String = "",
    val partnerName: String = "Partner",
    val currency: Currency = Currency.USD,
    val splitYou: Int = 60,
    val categoryPref: String = "mixed",
    val saved: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    val fieldErrors: Map<String, String> = emptyMap(),
)

/** Onboarding steps 04–06: workspace type, business name, business preferences. */
class SetupViewModel(private val profileRepository: ProfileRepository) : BaseViewModel() {

    private val _state = MutableStateFlow(SetupUiState())
    val state = _state.asStateFlow()

    fun start() {
        _state.value = SetupUiState()
        scope.launch {
            runCatching { profileRepository.getProfile() }
                .onSuccess { profile ->
                    _state.update {
                        it.copy(
                            businessName = profile.businessName,
                            partnerName = profile.partnerName,
                            workspaceType = profile.workspaceType,
                            currency = profile.currency,
                            splitYou = profile.splitYou,
                            categoryPref = profile.categoryPref,
                        )
                    }
                }
        }
    }

    fun setWorkspace(type: WorkspaceType) =
        _state.update {
            it.copy(
                workspaceType = type,
                error = null,
                fieldErrors = if (type == WorkspaceType.SOLO) it.fieldErrors - "partnerName" else it.fieldErrors,
            )
        }
    fun setBusinessName(v: String) = updateField("businessName") { it.copy(businessName = v) }
    fun setPartnerName(v: String) = updateField("partnerName") { it.copy(partnerName = v) }
    fun setCurrency(c: Currency) = _state.update { it.copy(currency = c, error = null) }
    fun setSplit(v: Int) = updateField("splitYou") { it.copy(splitYou = v) }
    fun setCategoryPref(id: String) = updateField("categoryPref") { it.copy(categoryPref = id) }

    fun validateStep(step: Int): Boolean {
        val s = _state.value
        val fields = when (step) {
            2 -> setOf("businessName")
            3 -> setOf("partnerName", "splitYou", "categoryPref")
            else -> emptySet()
        }
        val errors = when (step) {
            2 -> FormValidation.setupBusinessName(s.businessName)
            3 -> FormValidation.setupPreferences(s.workspaceType, s.partnerName, s.splitYou, s.categoryPref)
            else -> emptyMap()
        }
        _state.update {
            it.copy(
                error = null,
                fieldErrors = it.fieldErrors.filterKeys { key -> key !in fields } + errors,
            )
        }
        return errors.isEmpty()
    }

    fun finish(onSaved: () -> Unit) {
        if (_state.value.loading) return
        val s = _state.value
        val fieldErrors = FormValidation.setupBusinessName(s.businessName) +
            FormValidation.setupPreferences(s.workspaceType, s.partnerName, s.splitYou, s.categoryPref)
        if (fieldErrors.isNotEmpty()) {
            _state.update { it.copy(error = null, fieldErrors = fieldErrors) }
            return
        }
        _state.update { it.copy(loading = true, error = null, fieldErrors = emptyMap()) }
        scope.launch {
            runCatching {
                profileRepository.updateProfile(
                    BusinessProfile(
                        businessName = s.businessName.trim(),
                        partnerName = s.partnerName.trim().ifBlank { "Partner" },
                        workspaceType = s.workspaceType,
                        currency = s.currency,
                        splitYou = s.splitYou,
                        categoryPref = s.categoryPref,
                    ),
                )
                profileRepository.setOnboarded()
            }.onSuccess {
                _state.update { it.copy(saved = true, loading = false) }
                onSaved()
            }.onFailure { throwable ->
                _state.update {
                    it.copy(
                        loading = false,
                        error = AppError.from(throwable).userMessage(),
                    )
                }
            }
        }
    }

    private fun updateField(field: String, update: (SetupUiState) -> SetupUiState) {
        _state.update {
            update(it).copy(
                error = null,
                fieldErrors = it.fieldErrors - field,
            )
        }
    }
}
