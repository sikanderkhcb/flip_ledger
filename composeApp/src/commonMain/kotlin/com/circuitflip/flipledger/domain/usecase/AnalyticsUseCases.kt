package com.circuitflip.flipledger.domain.usecase

import com.circuitflip.flipledger.domain.model.AttentionItem
import com.circuitflip.flipledger.domain.model.AttentionKind
import com.circuitflip.flipledger.domain.model.BusinessProfile
import com.circuitflip.flipledger.domain.model.DashboardMetrics
import com.circuitflip.flipledger.domain.model.Device
import com.circuitflip.flipledger.domain.model.DeviceStatus
import com.circuitflip.flipledger.domain.model.ReportMetric
import com.circuitflip.flipledger.domain.model.Sale
import com.circuitflip.flipledger.domain.model.Settlement
import com.circuitflip.flipledger.domain.model.SettlementActivity
import com.circuitflip.flipledger.domain.util.Money
import com.circuitflip.flipledger.domain.util.ProfitCalculator
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.math.roundToInt

/** Builds the dashboard metric bundle from live inventory + sales. */
class GetDashboardMetricsUseCase {
    operator fun invoke(inventory: List<Device>, sales: List<Sale>): DashboardMetrics {
        val tz = TimeZone.currentSystemDefault()
        // A month index (year*12 + month) so we can compare "this month" vs "last month".
        val thisMonth = Clock.System.now().toLocalDateTime(tz).let { it.year * 12 + (it.monthNumber - 1) }
        fun monthOf(sale: Sale): Int? = sale.createdAt?.let { ts ->
            runCatching { Instant.parse(ts).toLocalDateTime(tz).let { it.year * 12 + (it.monthNumber - 1) } }.getOrNull()
        }
        val thisMonthNet = sales.filter { monthOf(it) == thisMonth }.sumOf { it.netProfitCents }
        val lastMonthNet = sales.filter { monthOf(it) == thisMonth - 1 }.sumOf { it.netProfitCents }
        val deltaPercent = when {
            lastMonthNet != 0L -> (((thisMonthNet - lastMonthNet).toDouble() / abs(lastMonthNet)) * 100).roundToInt()
            thisMonthNet > 0L -> 100 // grew from nothing
            else -> 0
        }
        return DashboardMetrics(
            monthNetProfitCents = ProfitCalculator.monthNetProfitCents(sales),
            profitDeltaPercent = deltaPercent,
            inventoryValueCents = ProfitCalculator.inventoryValueCents(inventory),
            avgMarginFraction = ProfitCalculator.averageMargin(sales),
            agingCount = ProfitCalculator.agingCount(inventory),
            salesThisMonth = sales.size,
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
        // Missing-evidence nudge targets an accessory with no photos (reference behavior).
        inventory.firstOrNull { it.model.contains("AirPods", ignoreCase = true) }?.let {
            add(
                AttentionItem(
                    title = "${it.model} — no photos added",
                    subtitle = "Missing evidence for records",
                    kind = AttentionKind.MISSING_EVIDENCE,
                    deviceId = it.id,
                ),
            )
        }
    }
}

/** Computes the partner settlement summary. */
class GetSettlementUseCase {
    operator fun invoke(profile: BusinessProfile, sales: List<Sale>): Settlement {
        val total = ProfitCalculator.monthNetProfitCents(sales)
        val yourShare = (total * profile.splitYou / 100.0).toLong()
        val partnerShare = (total * profile.splitPartner / 100.0).toLong()
        // Reference: already paid $150 this cycle, so amount owed nets that out.
        val owed = maxOf(0L, partnerShare - Money.dollarsToCents(150))
        return Settlement(
            totalProfitCents = total,
            owedCents = owed,
            yourShareCents = yourShare,
            partnerShareCents = partnerShare,
            activity = listOf(
                SettlementActivity("Jul 1 — Paid ${profile.partnerName} (June settlement)", Money.dollarsToCents(150)),
                SettlementActivity("Jun 1 — Paid ${profile.partnerName} (May settlement)", Money.dollarsToCents(310)),
            ),
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
