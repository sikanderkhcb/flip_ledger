package com.circuitflip.flipledger

import com.circuitflip.flipledger.domain.model.BusinessProfile
import com.circuitflip.flipledger.domain.model.Sale
import com.circuitflip.flipledger.domain.model.SalesChannel
import com.circuitflip.flipledger.domain.usecase.GetDashboardMetricsUseCase
import com.circuitflip.flipledger.domain.usecase.GetSettlementUseCase
import com.circuitflip.flipledger.domain.util.Dates
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnalyticsUseCasesTest {
    private val today = Dates.today()

    @Test
    fun dashboardUsesOnlyCurrentMonthForMonthlyMetrics() {
        val current = sale("current", dateInMonth(Dates.monthIndex(today)), profitCents = 5_000)
        val previous = sale("previous", dateInMonth(Dates.monthIndex(today) - 1), profitCents = 20_000)

        val metrics = GetDashboardMetricsUseCase()(emptyList(), listOf(current, previous))

        assertEquals(5_000L, metrics.monthNetProfitCents)
        assertEquals(1, metrics.salesThisMonth)
        assertEquals(current.margin, metrics.avgMarginFraction)
    }

    @Test
    fun settlementDoesNotUseOldSalesOrInventPayments() {
        val current = sale("current", dateInMonth(Dates.monthIndex(today)), profitCents = 10_000)
        val previous = sale("previous", dateInMonth(Dates.monthIndex(today) - 1), profitCents = 50_000)
        val profile = BusinessProfile(splitYou = 60)

        val settlement = GetSettlementUseCase()(profile, listOf(current, previous))

        assertEquals(10_000L, settlement.totalProfitCents)
        assertEquals(6_000L, settlement.yourShareCents)
        assertEquals(4_000L, settlement.partnerShareCents)
        assertEquals(4_000L, settlement.owedCents)
        assertTrue(settlement.activity.isEmpty())
    }

    private fun sale(id: String, soldDate: String, profitCents: Long): Sale {
        val cost = 10_000L
        return Sale(
            id = id,
            model = "Test device",
            soldDate = soldDate,
            channel = SalesChannel.IN_PERSON,
            revenueCents = cost + profitCents,
            costCents = cost,
            feesCents = 0,
            daysHeld = 1,
        )
    }

    private fun dateInMonth(monthIndex: Int): String {
        val year = monthIndex / 12
        val month = monthIndex % 12 + 1
        return LocalDate(year, month, 1).toString()
    }
}
