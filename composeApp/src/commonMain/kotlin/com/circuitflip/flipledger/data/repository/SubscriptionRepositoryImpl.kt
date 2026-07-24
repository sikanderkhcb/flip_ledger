package com.circuitflip.flipledger.data.repository

import com.circuitflip.flipledger.core.AppError
import com.circuitflip.flipledger.core.DataResult
import com.circuitflip.flipledger.core.runCatchingResult
import com.circuitflip.flipledger.data.remote.dto.BillingAccountDto
import com.circuitflip.flipledger.data.remote.dto.BillingUrlResponse
import com.circuitflip.flipledger.data.remote.dto.toDomain
import com.circuitflip.flipledger.domain.model.SubscriptionAccess
import com.circuitflip.flipledger.domain.repository.SubscriptionRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import io.ktor.client.call.body
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SubscriptionRepositoryImpl(
    private val client: SupabaseClient,
    private val io: CoroutineDispatcher,
) : SubscriptionRepository {

    private val access = MutableStateFlow(SubscriptionAccess())
    private val scope = CoroutineScope(SupervisorJob() + io)
    private var cacheGeneration: Long = 0

    override fun observeAccess(): Flow<SubscriptionAccess> =
        access.onStart { scope.launch { refresh() } }

    override suspend fun refresh(): DataResult<SubscriptionAccess> = withContext(io) {
        val generation = cacheGeneration
        val result = runCatchingResult {
            client.from("billing_accounts")
                .select()
                .decodeSingleOrNull<BillingAccountDto>()
                ?.toDomain()
                ?: SubscriptionAccess()
        }
        if (generation == cacheGeneration && result is DataResult.Success) {
            access.value = result.data
        }
        result
    }

    override suspend fun createCheckoutSession(): DataResult<String> = withContext(io) {
        invokeBillingFunction("create-checkout-session")
    }

    override suspend fun createPortalSession(): DataResult<String> = withContext(io) {
        invokeBillingFunction("create-portal-session")
    }

    override fun recordDeviceAdded() {
        access.update {
            it.copy(lifetimeDevicesCreated = it.lifetimeDevicesCreated + 1)
        }
    }

    override fun clearCache() {
        cacheGeneration += 1
        access.value = SubscriptionAccess()
    }

    private suspend fun invokeBillingFunction(name: String): DataResult<String> =
        try {
            val response = client.functions.invoke(name).body<BillingUrlResponse>()
            DataResult.Success(response.url)
        } catch (t: Throwable) {
            DataResult.Failure(AppError.from(t))
        }
}
