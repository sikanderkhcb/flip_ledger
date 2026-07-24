package com.circuitflip.flipledger

import com.circuitflip.flipledger.domain.model.SubscriptionAccess
import com.circuitflip.flipledger.domain.model.SubscriptionStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SubscriptionAccessTest {

    @Test
    fun freeAccountCanAddOnlyFirstTenLifetimeDevices() {
        val beforeLimit = SubscriptionAccess(lifetimeDevicesCreated = 9)
        val atLimit = SubscriptionAccess(lifetimeDevicesCreated = 10)

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
            status = SubscriptionStatus.CANCELED,
            lifetimeDevicesCreated = 30,
        )

        assertFalse(access.isUnlimited)
        assertFalse(access.canAddDevice)
    }
}
