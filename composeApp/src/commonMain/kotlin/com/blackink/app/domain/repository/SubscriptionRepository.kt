package com.blackink.app.domain.repository

import com.blackink.app.core.DataResult
import com.blackink.app.domain.model.SubscriptionAccess
import com.blackink.app.domain.model.SubscriptionTier
import kotlinx.coroutines.flow.Flow

interface SubscriptionRepository {
    fun observeAccess(): Flow<SubscriptionAccess>
    suspend fun refresh(): DataResult<SubscriptionAccess>
    suspend fun createCheckoutSession(tier: SubscriptionTier): DataResult<String>
    suspend fun createPortalSession(): DataResult<String>
    fun recordDeviceAdded()
    fun clearCache()
}
