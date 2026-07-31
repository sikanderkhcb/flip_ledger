package com.blackink.app.domain.usecase

import com.blackink.app.domain.model.AttentionItem
import com.blackink.app.domain.model.AttentionKind
import com.blackink.app.domain.model.BusinessProfile
import com.blackink.app.domain.model.DashboardMetrics
import com.blackink.app.domain.model.Device
import com.blackink.app.domain.model.DeviceStatus
import com.blackink.app.domain.model.ReportMetric
import com.blackink.app.domain.model.Sale
import com.blackink.app.domain.model.Settlement
import com.blackink.app.domain.util.Dates
import com.blackink.app.domain.util.Money
import com.blackink.app.domain.util.ProfitCalculator
import kotlin.math.abs
import kotlin.math.roundToInt

/** Builds the dashboard metric bundle from live inventory + sales. */
class GetDashboardMetricsUseCase {
    operator fun invoke(inventory: List<Device>, sales: List<Sale>): DashboardMetrics {
        val today = Dates.today()
        // A month index (year*12 + month) so we can compare "this month" vs "last month".
        val thisMonth = Dates.monthIndex(today)
        fun monthOf(sale: Sale): Int? =
            Dates.parseIso(sale.soldDate)?.let(Dates::monthIndex)
                ?: Dates.monthIndexFromTimestamp(sale.createdAt)
        val thisMonthSales = sales.filter { monthOf(it) == thisMonth }
        val lastMonthSales = sales.filter { monthOf(it) == thisMonth - 1 }
        val thisMonthNet = thisMonthSales.sumOf { it.netProfitCents }
        val lastMonthNet = lastMonthSales.sumOf { it.netProfitCents }
        val deltaPercent = when {
            lastMonthNet != 0L -> (((thisMonthNet - lastMonthNet).toDouble() / abs(lastMonthNet)) * 100).roundToInt()
            thisMonthNet > 0L -> 100 // grew from nothing
            else -> 0
        }
        return DashboardMetrics(
            monthNetProfitCents = ProfitCalculator.monthNetProfitCents(thisMonthSales),
            profitDeltaPercent = deltaPercent,
            inventoryValueCents = ProfitCalculator.inventoryValueCents(inventory),
            avgMarginFraction = ProfitCalculator.averageMargin(thisMonthSales),
            agingCount = ProfitCalculator.agingCount(inventory),
            salesThisMonth = thisMonthSales.size,
        )
    }
}

/** Builds the dashboard "attention needed" nudges (aging, ready-to-list, missing evidence). */
class GetAttentionItemsUseCase {
    operator fun invoke(inventory: List<Device>): List<AttentionItem> = buildList {
        val aging = inventory.filter { it.isAging }
        if (aging.isNotEmpty()) {
            add(
                AttentionItem(
                    title = "${aging.size} devices older than 30 days",
                    subtitle = aging.joinToString(", ") { it.model },
                    kind = AttentionKind.AGING,
                ),
            )
        }
        val ready = inventory.filter { it.status == DeviceStatus.READY }
        if (ready.isNotEmpty()) {
            add(
                AttentionItem(
                    title = "${ready.size} items ready to list",
                    subtitle = ready.joinToString(", ") { it.model },
                    kind = AttentionKind.READY_TO_LIST,
                ),
            )
        }
    }
}

/** Computes the partner settlement summary. */
class GetSettlementUseCase {
    operator fun invoke(profile: BusinessProfile, sales: List<Sale>): Settlement {
        val today = Dates.today()
        val monthSales = sales.filter {
            Dates.isInMonth(it.soldDate, today.year, today.monthNumber) ||
                (Dates.parseIso(it.soldDate) == null &&
                    Dates.monthIndexFromTimestamp(it.createdAt) == Dates.monthIndex(today))
        }
        val total = ProfitCalculator.monthNetProfitCents(monthSales)
        val yourShare = (total * profile.splitYou / 100.0).toLong()
        val partnerShare = (total * profile.splitPartner / 100.0).toLong()
        return Settlement(
            totalProfitCents = total,
            owedCents = maxOf(0L, partnerShare),
            yourShareCents = yourShare,
            partnerShareCents = partnerShare,
            activity = emptyList(),
        )
    }
}

/** Builds the reports summary rows. */
class GetReportMetricsUseCase {
    operator fun invoke(inventory: List<Device>, sales: List<Sale>): List<ReportMetric> {
        val revenue = ProfitCalculator.revenueTotalCents(sales)
        val net = ProfitCalculator.monthNetProfitCents(sales)
        val costs = ProfitCalculator.costsTotalCents(sales)
        val inventoryValue = ProfitCalculator.inventoryValueCents(inventory)
        return listOf(
            ReportMetric("Revenue", Money.format(revenue)),
            ReportMetric("Net profit", Money.format(net)),
            ReportMetric("Total costs", Money.format(costs)),
            ReportMetric("Active inventory value", Money.format(inventoryValue)),
            ReportMetric("Best category", bestCategory(sales)),
        )
    }

    private fun bestCategory(sales: List<Sale>): String {
        fun guess(m: String) = when {
            Regex("iphone|galaxy|pixel", RegexOption.IGNORE_CASE).containsMatchIn(m) -> "Phones"
            Regex("macbook|laptop", RegexOption.IGNORE_CASE).containsMatchIn(m) -> "Laptops"
            Regex("ipad|tablet", RegexOption.IGNORE_CASE).containsMatchIn(m) -> "Tablets"
            Regex("xbox|switch|playstation|ps5|steam deck", RegexOption.IGNORE_CASE).containsMatchIn(m) -> "Gaming"
            else -> "Accessories"
        }
        return sales.groupBy { guess(it.model) }
            .mapValues { (_, v) -> v.sumOf { it.netProfitCents } }
            .maxByOrNull { it.value }?.key ?: "Phones"
    }
}
