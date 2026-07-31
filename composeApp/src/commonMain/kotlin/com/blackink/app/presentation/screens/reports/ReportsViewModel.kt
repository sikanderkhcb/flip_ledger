package com.blackink.app.presentation.screens.reports

import com.blackink.app.domain.model.Device
import com.blackink.app.domain.model.ReportMetric
import com.blackink.app.domain.model.Sale
import com.blackink.app.domain.usecase.GetReportMetricsUseCase
import com.blackink.app.domain.usecase.ObserveInventoryUseCase
import com.blackink.app.domain.usecase.ObserveSalesUseCase
import com.blackink.app.domain.repository.ProfileRepository
import com.blackink.app.domain.model.WorkspaceType
import com.blackink.app.domain.util.Dates
import com.blackink.app.presentation.BaseViewModel
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
    profileRepository: ProfileRepository,
) : BaseViewModel() {
    private val _metrics = MutableStateFlow<List<ReportMetric>>(emptyList())
    val metrics = _metrics.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private var devices: List<Device> = emptyList()
    private var sales: List<Sale> = emptyList()
    private var workspaceType = WorkspaceType.SOLO
    private var partnerSplit = 0
    private val period = Dates.today()
    val periodLabel: String = Dates.monthLabel(period.year, period.monthNumber)

    init {
        combine(observeInventory(), observeSales(), profileRepository.observeProfile()) { inv, s, profile ->
            devices = inv
            sales = s
            workspaceType = profile.workspaceType
            partnerSplit = profile.splitPartner
            getReportMetrics(inv, salesInPeriod(s))
        }.onEach { _metrics.value = it }.launchIn(scope)
        combine(observeInventory.error, observeSales.error) { inventoryError, salesError ->
            (inventoryError ?: salesError)?.userMessage()
        }.onEach { _error.value = it }.launchIn(scope)
    }

    /** Builds a CSV of every recorded sale and every active-inventory cost. */
    fun buildCsv(): String = buildString {
        appendLine("SALES")
        appendLine("device_name,buy_price,sale_price,expenses,partner_settlement,profit,date_sold,date_bought,fees,channel,customer_name,customer_email,days_held")
        salesInPeriod(sales).forEach { s ->
            appendLine(
                listOf(
                    s.model, money(s.purchasePriceCents), money(s.revenueCents), money(s.expensesCents),
                    money(partnerSettlement(s)), money(s.netProfitCents), s.soldDate, s.purchaseDate,
                    money(s.feesCents), s.channel?.label ?: "", s.customerName, s.customerEmail, s.daysHeld.toString(),
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

    private fun partnerSettlement(sale: Sale): Long =
        if (workspaceType == WorkspaceType.PARTNER) (sale.netProfitCents * partnerSplit / 100.0).toLong() else 0L

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
