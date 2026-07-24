package com.circuitflip.flipledger.domain.repository

import com.circuitflip.flipledger.core.DataResult
import com.circuitflip.flipledger.domain.model.SubscriptionAccess
import kotlinx.coroutines.flow.Flow

interface SubscriptionRepository {
    fun observeAccess(): Flow<SubscriptionAccess>
    suspend fun refresh(): DataResult<SubscriptionAccess>
    suspend fun createCheckoutSession(): DataResult<String>
    suspend fun createPortalSession(): DataResult<String>
    fun recordDeviceAdded()
    fun clearCache()
}
