package com.circuitflip.flipledger

import com.circuitflip.flipledger.domain.model.Cost
import com.circuitflip.flipledger.domain.model.CostType
import com.circuitflip.flipledger.domain.model.Device
import com.circuitflip.flipledger.domain.model.DeviceCategory
import com.circuitflip.flipledger.domain.model.DeviceCondition
import com.circuitflip.flipledger.domain.model.DeviceStatus
import com.circuitflip.flipledger.domain.model.LockStatus
import com.circuitflip.flipledger.domain.model.Sale
import com.circuitflip.flipledger.domain.model.SaleDraft
import com.circuitflip.flipledger.domain.model.SalesChannel
import com.circuitflip.flipledger.domain.util.ProfitCalculator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProfitCalculatorTest {

    private fun device(
        purchaseCents: Long,
        costs: List<Cost> = emptyList(),
        daysHeld: Int = 5,
    ) = Device(
        id = "d1",
        category = DeviceCategory.PHONE,
        model = "iPhone 14 Pro",
        identifier = "IMEI ●●●●4821",
        condition = DeviceCondition.EXCELLENT,
        storage = "256GB",
        lock = LockStatus.UNLOCKED,
        purchasePriceCents = purchaseCents,
        source = null,
        purchaseDate = "Jun 24",
        costs = costs,
        status = DeviceStatus.LISTED,
        daysHeld = daysHeld,
    )

    private fun cost(cents: Long) = Cost(id = "c${cents}", type = CostType.PARTS, amountCents = cents)

    @Test
    fun investedIsPurchasePlusCosts() {
        val d = device(56000, listOf(cost(2200), cost(1400)))
        assertEquals(59600L, d.investedCents)
    }

    @Test
    fun previewNetProfitSubtractsCostsAndFees() {
        val d = device(56000, listOf(cost(2200), cost(1400)))
        val draft = SaleDraft(price = "720", platformFee = "43")
        // 72000 - 56000 - 3600 - 4300 = 8100
        assertEquals(8100L, ProfitCalculator.previewNetProfitCents(d, draft))
    }

    @Test
    fun previewMarginGuardsZeroRevenue() {
        val d = device(10000)
        assertEquals(0.0, ProfitCalculator.previewMargin(d, SaleDraft(price = "")))
    }

    @Test
    fun expectedProfitIs32PercentOfInvested() {
        val d = device(50000, listOf(cost(0)))
        assertEquals(16000L, ProfitCalculator.expectedProfitCents(d))
    }

    @Test
    fun agingWhenHeldOverThirtyDays() {
        assertTrue(device(1, daysHeld = 41).isAging)
        assertTrue(!device(1, daysHeld = 18).isAging)
    }

    @Test
    fun monthNetProfitSumsSaleProfits() {
        val sales = listOf(
            Sale("s1", "iPhone 13 Pro", "Jul 9", SalesChannel.EBAY, 72000, 48000, 4300, 22),
            Sale("s2", "MacBook Pro", "Jul 6", SalesChannel.SWAPPA, 105000, 59000, 5000, 15),
        )
        // (72000-48000-4300) + (105000-59000-5000) = 19700 + 41000 = 60700
        assertEquals(60700L, ProfitCalculator.monthNetProfitCents(sales))
    }

    @Test
    fun averageMarginOfEmptyIsZero() {
        assertEquals(0.0, ProfitCalculator.averageMargin(emptyList()))
    }
}
