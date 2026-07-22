package com.circuitflip.flipledger.domain.model

/**
 * Per-workspace configuration captured in onboarding and editable in Settings.
 */
data class BusinessProfile(
    val businessName: String = "Circuit Flip Co.",
    val ownerName: String = "",
    val partnerName: String = "Marcus",
    val workspaceType: WorkspaceType = WorkspaceType.PARTNER,
    /** Percentage of profit the owner keeps in partner mode (0..100). */
    val splitYou: Int = 60,
    val currency: Currency = Currency.USD,
    val categoryPref: String = "mixed",
) {
    val splitPartner: Int get() = 100 - splitYou
}
