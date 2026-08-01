package com.blackink.app.data.repository

import com.blackink.app.core.AppError
import com.blackink.app.core.DataResult
import com.blackink.app.core.runCatchingResult
import com.blackink.app.data.remote.dto.BillingAccountDto
import com.blackink.app.data.remote.dto.BillingUrlResponse
import com.blackink.app.data.remote.dto.CheckoutSessionRequest
import com.blackink.app.data.remote.dto.toDomain
import com.blackink.app.domain.model.SubscriptionAccess
import com.blackink.app.domain.model.SubscriptionTier
import com.blackink.app.domain.repository.SubscriptionRepository
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

    override suspend fun createCheckoutSession(tier: SubscriptionTier): DataResult<String> = withContext(io) {
        invokeBillingFunction(
            name = "create-checkout-session",
            body = CheckoutSessionRequest(tier.name.lowercase()),
        )
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

    private suspend fun invokeBillingFunction(
        name: String,
        body: CheckoutSessionRequest? = null,
    ): DataResult<String> =
        try {
            val httpResponse = if (body == null) {
                client.functions.invoke(name)
            } else {
                client.functions.invoke(name, body)
            }
            val response = httpResponse.body<BillingUrlResponse>()
            DataResult.Success(response.url)
        } catch (t: Throwable) {
            DataResult.Failure(AppError.from(t))
        }
}
