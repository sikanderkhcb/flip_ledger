package com.circuitflip.flipledger.presentation.screens.reports

import com.circuitflip.flipledger.domain.model.Device
import com.circuitflip.flipledger.domain.model.ReportMetric
import com.circuitflip.flipledger.domain.model.Sale
import com.circuitflip.flipledger.domain.usecase.GetReportMetricsUseCase
import com.circuitflip.flipledger.domain.usecase.ObserveInventoryUseCase
import com.circuitflip.flipledger.domain.usecase.ObserveSalesUseCase
import com.circuitflip.flipledger.domain.util.Dates
import com.circuitflip.flipledger.presentation.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.math.abs

class ReportsViewModel(
    observeInventory: ObserveInventoryUseCase,
    observeSales: ObserveSalesUseCase,
    getReportMetrics: GetReportMetricsUseCase,
) : BaseViewModel() {
    private val _metrics = MutableStateFlow<List<ReportMetric>>(emptyList())
    val metrics = _metrics.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private var devices: List<Device> = emptyList()
    private var sales: List<Sale> = emptyList()
    private val period = Dates.today()
    val periodLabel: String = Dates.monthLabel(period.year, period.monthNumber)

    init {
        combine(observeInventory(), observeSales()) { inv, s ->
            devices = inv
            sales = s
            getReportMetrics(inv, salesInPeriod(s))
        }.onEach { _metrics.value = it }.launchIn(scope)
        combine(observeInventory.error, observeSales.error) { inventoryError, salesError ->
            (inventoryError ?: salesError)?.userMessage()
        }.onEach { _error.value = it }.launchIn(scope)
    }

    /** Builds a CSV of every recorded sale and every active-inventory cost. */
    fun buildCsv(): String = buildString {
        appendLine("SALES")
        appendLine("id,model,sold_date,channel,revenue,cost,fees,net_profit,margin_percent,days_held")
        salesInPeriod(sales).forEach { s ->
            appendLine(
                listOf(
                    s.id, s.model, s.soldDate, s.channel?.label ?: "",
                    money(s.revenueCents), money(s.costCents), money(s.feesCents),
                    money(s.netProfitCents), (s.margin * 100).toInt().toString(), s.daysHeld.toString(),
                ).joinToString(",") { csv(it) },
            )
        }
        appendLine()
        appendLine("COSTS (active inventory)")
        appendLine("device_id,model,type,amount,paid_by,date,note")
        devices.forEach { d ->
            d.costs.filter { Dates.isInMonth(it.date, period.year, period.monthNumber) }.forEach { c ->
                appendLine(
                    listOf(
                        d.id, d.model, c.type.label, money(c.amountCents), c.paidBy.label, c.date, c.note,
                ).joinToString(",") { csv(it) },
                )
            }
        }
    }

    /** cents → plain decimal string with no symbol/grouping, safe for CSV. */
    private fun money(cents: Long): String {
        val sign = if (cents < 0) "-" else ""
        val a = abs(cents)
        return "$sign${a / 100}.${(a % 100).toString().padStart(2, '0')}"
    }

    /** Quote a field and escape embedded quotes so commas/newlines don't break columns. */
    private fun csv(value: String): String {
        val trimmed = value.trimStart()
        val dangerous = trimmed.startsWith("=") ||
            trimmed.startsWith("+") ||
            trimmed.startsWith("@") ||
            (trimmed.startsWith("-") && trimmed.drop(1).firstOrNull()?.isDigit() == false)
        val safe = if (dangerous) "'$value" else value
        return "\"" + safe.replace("\"", "\"\"") + "\""
    }

    private fun salesInPeriod(source: List<Sale>): List<Sale> = source.filter {
        Dates.isInMonth(it.soldDate, period.year, period.monthNumber) ||
            (Dates.parseIso(it.soldDate) == null &&
                Dates.monthIndexFromTimestamp(it.createdAt) == Dates.monthIndex(period))
    }
}
