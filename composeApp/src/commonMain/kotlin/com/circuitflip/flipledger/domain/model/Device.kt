package com.circuitflip.flipledger.domain.model

import com.circuitflip.flipledger.domain.util.Dates

/**
 * A single unit of resale inventory. This is the central aggregate of the app.
 *
 * Money is stored as whole-cent [Long]s to avoid floating-point rounding errors — the
 * source design worked in whole dollars, but cents future-proofs partial fees.
 */
data class Device(
    val id: String,
    val category: DeviceCategory,
    val model: String,
    /** Masked identifier as stored, e.g. "IMEI ●●●●4821" or "No identifier on file". */
    val identifier: String,
    val condition: DeviceCondition?,
    val storage: String,          // e.g. "256GB" or "—"
    val lock: LockStatus,
    val purchasePriceCents: Long,
    val source: AcquisitionSource?,
    /** Human-readable purchase date label, e.g. "Jun 24". Kept as-is from the design. */
    val purchaseDate: String,
    val costs: List<Cost>,
    val status: DeviceStatus,
    val daysHeld: Int,
    val repairIssue: String = "",
    val repairProvider: String = "",
    val repairStartedOn: String? = null,
    val repairCompletedOn: String? = null,
    val warrantyProvider: String = "",
    val warrantyExpiresOn: String? = null,
) {
    /** Total capital tied up in this device: purchase + all logged costs. */
    val investedCents: Long
        get() = purchasePriceCents + costs.sumOf { it.amountCents }

    val isAging: Boolean get() = daysHeld > AGING_THRESHOLD_DAYS

    val warrantyDaysRemaining: Int?
        get() = warrantyExpiresOn?.let { Dates.daysUntil(it) }

    companion object {
        const val AGING_THRESHOLD_DAYS = 30
    }
}

/** An additional cost logged against a device (repair parts, shipping, etc.). */
data class Cost(
    val id: String,
    val type: CostType,
    val amountCents: Long,
    val paidBy: PaidBy = PaidBy.YOU,
    val date: String = "",
    val note: String = "",
)
