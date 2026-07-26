package com.circuitflip.flipledger

import com.circuitflip.flipledger.domain.model.SubscriptionAccess
import com.circuitflip.flipledger.domain.model.SubscriptionStatus
import com.circuitflip.flipledger.domain.model.SubscriptionTier
import com.circuitflip.flipledger.domain.model.FREE_DEVICE_LIMIT
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SubscriptionAccessTest {

    @Test
    fun freeAccountCanAddOnlyFirstFiveLifetimeDevices() {
        val beforeLimit = SubscriptionAccess(lifetimeDevicesCreated = FREE_DEVICE_LIMIT - 1)
        val atLimit = SubscriptionAccess(lifetimeDevicesCreated = FREE_DEVICE_LIMIT)

        assertTrue(beforeLimit.canAddDevice)
        assertEquals(1, beforeLimit.remainingFreeDevices)
        assertFalse(atLimit.canAddDevice)
        assertEquals(0, atLimit.remainingFreeDevices)
    }

    @Test
    fun activeSubscriptionAllowsDevicesBeyondFreeLimit() {
        val access = SubscriptionAccess(
            status = SubscriptionStatus.ACTIVE,
            lifetimeDevicesCreated = 30,
        )

        assertTrue(access.isUnlimited)
        assertTrue(access.canAddDevice)
    }

    @Test
    fun canceledSubscriptionBlocksOnlyFutureDeviceCreation() {
        val access = SubscriptionAccess(
            tier = SubscriptionTier.PARTNER,
            status = SubscriptionStatus.CANCELED,
            lifetimeDevicesCreated = 30,
        )

        assertEquals(SubscriptionTier.PARTNER, access.tier)
        assertFalse(access.isUnlimited)
        assertFalse(access.canAddDevice)
    }

    @Test
    fun subscriptionTierDecodesServerValuesSafely() {
        assertEquals(SubscriptionTier.SOLO, SubscriptionTier.fromWire("solo"))
        assertEquals(SubscriptionTier.PARTNER, SubscriptionTier.fromWire("PARTNER"))
        assertEquals(SubscriptionTier.FREE, SubscriptionTier.fromWire("unknown"))
    }
}
