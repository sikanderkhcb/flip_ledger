package com.blackink.app.domain.util

import com.blackink.app.domain.model.Cost
import com.blackink.app.domain.model.Device
import com.blackink.app.domain.model.Sale
import com.blackink.app.domain.model.SaleDraft

/**
 * Pure, side-effect-free financial calculations. Kept separate from repositories so the
 * math is trivially unit-testable and reused identically across dashboard, device detail,
 * and the sale wizard.
 *
 * Every figure here is derived exactly from the reference design's logic.
 */
object ProfitCalculator {

    /** Sum of extra costs (excludes purchase price). */
    fun extraCostsCents(costs: List<Cost>): Long = costs.sumOf { it.amountCents }

    /** Total capital invested in a device (purchase + costs). */
    fun investedCents(device: Device): Long = device.investedCents

    /** Total selling fees from a sale draft. */
    fun feesCents(draft: SaleDraft): Long =
        Money.parseToCents(draft.platformFee) +
            Money.parseToCents(draft.paymentFee) +
            Money.parseToCents(draft.shipping) +
            Money.parseToCents(draft.packaging) +
            Money.parseToCents(draft.otherFee)

    /**
     * Live net-profit preview for the sale wizard: revenue - purchase - extra costs - fees.
     */
    fun previewNetProfitCents(device: Device, draft: SaleDraft): Long {
        val revenue = Money.parseToCents(draft.price)
        return revenue - device.purchasePriceCents - extraCostsCents(device.costs) - feesCents(draft)
    }

    fun previewMargin(device: Device, draft: SaleDraft): Double {
        val revenue = Money.parseToCents(draft.price)
        val net = previewNetProfitCents(device, draft)
        return if (revenue > 0) net.toDouble() / revenue else 0.0
    }

    /**
     * Rough expected-profit estimate shown on the device detail card before a real sale
     * is entered. The reference uses 32% of invested capital.
     */
    fun expectedProfitCents(device: Device): Long = (device.investedCents * 0.32).toLong()

    // ---- Portfolio-level roll-ups (dashboard / reports) ----

    fun monthNetProfitCents(sales: List<Sale>): Long = sales.sumOf { it.netProfitCents }

    fun inventoryValueCents(inventory: List<Device>): Long = inventory.sumOf { it.investedCents }

    fun averageMargin(sales: List<Sale>): Double =
        if (sales.isEmpty()) 0.0 else sales.sumOf { it.margin } / sales.size

    fun agingCount(inventory: List<Device>): Int = inventory.count { it.isAging }

    fun revenueTotalCents(sales: List<Sale>): Long = sales.sumOf { it.revenueCents }

    fun costsTotalCents(sales: List<Sale>): Long = sales.sumOf { it.costCents + it.feesCents }
}
