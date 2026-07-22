package com.circuitflip.flipledger.presentation.screens.setup

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
    val currency: Currency = Currency.USD,
    val splitYou: Int = 60,
    val categoryPref: String = "mixed",
    val saved: Boolean = false,
)

/** Onboarding steps 04–06: workspace type, business name, business preferences. */
class SetupViewModel(private val profileRepository: ProfileRepository) : BaseViewModel() {

    private val _state = MutableStateFlow(SetupUiState())
    val state = _state.asStateFlow()

    init { scope.launch { _state.update { it.copy(businessName = profileRepository.getProfile().businessName) } } }

    fun setWorkspace(type: WorkspaceType) = _state.update { it.copy(workspaceType = type) }
    fun setBusinessName(v: String) = _state.update { it.copy(businessName = v) }
    fun setCurrency(c: Currency) = _state.update { it.copy(currency = c) }
    fun setSplit(v: Int) = _state.update { it.copy(splitYou = v) }
    fun setCategoryPref(id: String) = _state.update { it.copy(categoryPref = id) }

    fun finish() {
        val s = _state.value
        scope.launch {
            profileRepository.updateProfile(
                BusinessProfile(
                    businessName = s.businessName.ifBlank { "Circuit Flip Co." },
                    workspaceType = s.workspaceType,
                    currency = s.currency,
                    splitYou = s.splitYou,
                    categoryPref = s.categoryPref,
                ),
            )
            profileRepository.setOnboarded()
            _state.update { it.copy(saved = true) }
        }
    }
}
