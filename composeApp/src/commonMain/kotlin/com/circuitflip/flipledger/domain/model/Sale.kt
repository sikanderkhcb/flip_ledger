package com.circuitflip.flipledger.domain.model

/**
 * A completed sale. Once a device is sold it is removed from active inventory and a
 * [Sale] record is created (see the sale flow use cases).
 */
data class Sale(
    val id: String,
    val model: String,
    /** Date label, e.g. "Jul 9". */
    val soldDate: String,
    val channel: SalesChannel?,
    val revenueCents: Long,
    /** Total invested (purchase + device costs) at time of sale. */
    val costCents: Long,
    /** Selling fees (platform, payment, shipping, packaging, other). */
    val feesCents: Long,
    val daysHeld: Int,
    val purchasePriceCents: Long = 0,
    val purchaseDate: String = "",
    val customerName: String = "",
    val customerEmail: String = "",
    val customerPhone: String = "",
    val customerAddress: String = "",
    /** Server timestamp (ISO-8601) of when the sale was recorded; null for not-yet-persisted sales. */
    val createdAt: String? = null,
) {
    val netProfitCents: Long get() = revenueCents - costCents - feesCents
    val expensesCents: Long get() = (costCents - purchasePriceCents).coerceAtLeast(0)

    /** Margin as a fraction (0.0..1.0). Guards divide-by-zero. */
    val margin: Double get() = if (revenueCents > 0) netProfitCents.toDouble() / revenueCents else 0.0
}
