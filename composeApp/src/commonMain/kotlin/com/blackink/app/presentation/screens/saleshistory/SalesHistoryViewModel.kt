package com.blackink.app.presentation.screens.saleshistory

import com.blackink.app.domain.model.Sale
import com.blackink.app.domain.usecase.ObserveSalesUseCase
import com.blackink.app.domain.util.ProfitCalculator
import com.blackink.app.domain.util.Dates
import com.blackink.app.presentation.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

data class SalesHistoryUiState(
    val sales: List<Sale> = emptyList(),
    val netProfitCents: Long = 0,
    val avgMargin: Double = 0.0,
    val summarySalesCount: Int = 0,
    val error: String? = null,
)

class SalesHistoryViewModel(observeSales: ObserveSalesUseCase) : BaseViewModel() {
    private val _state = MutableStateFlow(SalesHistoryUiState())
    val state = _state.asStateFlow()

    init {
        combine(observeSales(), observeSales.error) { sales, error -> sales to error }
            .onEach { (sales, error) ->
            val today = Dates.today()
            val monthSales = sales.filter {
                Dates.isInMonth(it.soldDate, today.year, today.monthNumber) ||
                    (Dates.parseIso(it.soldDate) == null &&
                        Dates.monthIndexFromTimestamp(it.createdAt) == Dates.monthIndex(today))
            }
            _state.value = SalesHistoryUiState(
                sales = sales,
                netProfitCents = ProfitCalculator.monthNetProfitCents(monthSales),
                avgMargin = ProfitCalculator.averageMargin(monthSales),
                summarySalesCount = monthSales.size,
                error = error?.userMessage(),
            )
        }.launchIn(scope)
    }
}
