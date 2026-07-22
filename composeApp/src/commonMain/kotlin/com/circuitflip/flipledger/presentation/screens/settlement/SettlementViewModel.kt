package com.circuitflip.flipledger.presentation.screens.settlement

import com.circuitflip.flipledger.domain.model.BusinessProfile
import com.circuitflip.flipledger.domain.model.Settlement
import com.circuitflip.flipledger.domain.repository.ProfileRepository
import com.circuitflip.flipledger.domain.usecase.GetSettlementUseCase
import com.circuitflip.flipledger.domain.usecase.ObserveSalesUseCase
import com.circuitflip.flipledger.presentation.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

data class SettlementUiState(
    val profile: BusinessProfile = BusinessProfile(),
    val settlement: Settlement? = null,
)

class SettlementViewModel(
    observeSales: ObserveSalesUseCase,
    profileRepository: ProfileRepository,
    getSettlement: GetSettlementUseCase,
) : BaseViewModel() {
    private val _state = MutableStateFlow(SettlementUiState())
    val state = _state.asStateFlow()

    init {
        combine(observeSales(), profileRepository.observeProfile()) { sales, profile ->
            SettlementUiState(profile = profile, settlement = getSettlement(profile, sales))
        }.onEach { _state.value = it }.launchIn(scope)
    }
}
