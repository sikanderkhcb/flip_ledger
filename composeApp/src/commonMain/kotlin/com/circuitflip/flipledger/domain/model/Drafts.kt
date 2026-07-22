package com.circuitflip.flipledger.domain.model

/**
 * Mutable in-progress form models used by the multi-step wizards. They hold raw string
 * input (matching text-field values) and are only converted to domain objects on submit.
 */
data class DeviceDraft(
    val category: DeviceCategory? = null,
    val model: String = "",
    val price: String = "",
    val date: String = "Jul 12, 2026",
    val source: AcquisitionSource? = null,
    val condition: DeviceCondition? = null,
    val identifierLast4: String = "",   // "Last 4 digits are enough"
    val storage: String = "",
    val lock: LockStatus? = null,
)

data class CostDraft(
    val type: CostType? = null,
    val amount: String = "",
    val paidBy: PaidBy = PaidBy.YOU,
    val date: String = "Jul 12, 2026",
    val note: String = "",
)

data class SaleDraft(
    val price: String = "",
    val date: String = "Jul 12, 2026",
    val channel: SalesChannel? = null,
    val platformFee: String = "",
    val paymentFee: String = "",
    val shipping: String = "",
    val packaging: String = "",
    val otherFee: String = "",
)

/** Onboarding / auth form model. */
data class AuthDraft(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val phone: String = "",
    val businessName: String = "",
)
