package com.circuitflip.flipledger.domain.model

const val FREE_DEVICE_LIMIT = 10

enum class SubscriptionStatus {
    FREE,
    INCOMPLETE,
    INCOMPLETE_EXPIRED,
    TRIALING,
    ACTIVE,
    PAST_DUE,
    CANCELED,
    UNPAID,
    PAUSED;

    companion object {
        fun fromWire(value: String): SubscriptionStatus =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: FREE
    }
}

data class SubscriptionAccess(
    val status: SubscriptionStatus = SubscriptionStatus.FREE,
    val lifetimeDevicesCreated: Int = 0,
    val currentPeriodEnd: String? = null,
    val cancelAtPeriodEnd: Boolean = false,
) {
    val isUnlimited: Boolean
        get() = status == SubscriptionStatus.ACTIVE ||
            status == SubscriptionStatus.TRIALING

    val remainingFreeDevices: Int
        get() = (FREE_DEVICE_LIMIT - lifetimeDevicesCreated).coerceAtLeast(0)

    val canAddDevice: Boolean
        get() = isUnlimited || remainingFreeDevices > 0
}
