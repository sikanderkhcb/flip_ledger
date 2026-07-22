package com.circuitflip.flipledger.presentation.screens.saleshistory

import com.circuitflip.flipledger.domain.model.Sale
import com.circuitflip.flipledger.domain.usecase.ObserveSalesUseCase
import com.circuitflip.flipledger.domain.util.ProfitCalculator
import com.circuitflip.flipledger.presentation.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

data class SalesHistoryUiState(
    val sales: List<Sale> = emptyList(),
    val netProfitCents: Long = 0,
    val avgMargin: Double = 0.0,
)

class SalesHistoryViewModel(observeSales: ObserveSalesUseCase) : BaseViewModel() {
    private val _state = MutableStateFlow(SalesHistoryUiState())
    val state = _state.asStateFlow()

    init {
        observeSales().onEach { sales ->
            _state.value = SalesHistoryUiState(
                sales = sales,
                netProfitCents = ProfitCalculator.monthNetProfitCents(sales),
                avgMargin = ProfitCalculator.averageMargin(sales),
            )
        }.launchIn(scope)
    }
}
