package com.circuitflip.flipledger.presentation.screens.setup

import com.circuitflip.flipledger.core.AppError
import com.circuitflip.flipledger.domain.model.BusinessProfile
import com.circuitflip.flipledger.domain.model.Currency
import com.circuitflip.flipledger.domain.model.WorkspaceType
import com.circuitflip.flipledger.domain.repository.ProfileRepository
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

    fun setWorkspace(type: WorkspaceType) = _state.update { it.copy(workspaceType = type) }
    fun setBusinessName(v: String) = _state.update { it.copy(businessName = v) }
    fun setPartnerName(v: String) = _state.update { it.copy(partnerName = v) }
    fun setCurrency(c: Currency) = _state.update { it.copy(currency = c) }
    fun setSplit(v: Int) = _state.update { it.copy(splitYou = v) }
    fun setCategoryPref(id: String) = _state.update { it.copy(categoryPref = id) }

    fun finish(onSaved: () -> Unit) {
        if (_state.value.loading) return
        val s = _state.value
        _state.update { it.copy(loading = true, error = null) }
        scope.launch {
            runCatching {
                profileRepository.updateProfile(
                    BusinessProfile(
                        businessName = s.businessName.ifBlank { "My Resale Business" },
                        partnerName = s.partnerName.ifBlank { "Partner" },
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
}
