package com.blackink.app.data.repository

import com.blackink.app.core.AppError
import com.blackink.app.core.DataResult
import com.blackink.app.core.runCatchingResult
import com.blackink.app.core.telemetry.Track
import com.blackink.app.data.remote.dto.SaleDto
import com.blackink.app.data.remote.dto.toDomain
import com.blackink.app.data.remote.dto.toDto
import com.blackink.app.domain.model.Sale
import com.blackink.app.domain.repository.InventoryRepository
import com.blackink.app.domain.repository.SalesRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put

/**
 * Sales history backed by the Supabase `sales` table. Recording a sale also deletes the
 * underlying device from `devices` (its `costs` cascade-delete via the FK), removing it from
 * active inventory.
 */
class SalesRepositoryImpl(
    private val client: SupabaseClient,
    private val inventoryRepository: InventoryRepository,
    private val io: CoroutineDispatcher,
) : SalesRepository {

    private val _sales = MutableStateFlow<List<Sale>>(emptyList())
    private val _error = MutableStateFlow<AppError?>(null)
    override val error = _error
    private val scope = CoroutineScope(SupervisorJob() + io)
    private var cacheGeneration: Long = 0

    override fun observeSales(): Flow<List<Sale>> =
        _sales.onStart { scope.launch { refreshSafely() } }

    override suspend fun recordSale(sale: Sale, soldDeviceId: String): DataResult<Sale> = withContext(io) {
        val generation = cacheGeneration
        val result = runCatchingResult {
            client.postgrest.rpc(
                function = "complete_sale",
                parameters = buildJsonObject {
                    put("p_sale", SALES_RPC_JSON.encodeToJsonElement(sale.toDto()))
                    put("p_device_id", soldDeviceId)
                },
            )
            sale
        }
        if (generation == cacheGeneration && result is DataResult.Success) {
            inventoryRepository.removeCachedDevice(soldDeviceId)
            _sales.value = listOf(sale) + _sales.value.filterNot { it.id == sale.id }
        }
        refreshSafely()
        result.track("sale_completed")
    }

    override fun clearCache() {
        cacheGeneration += 1
        _sales.value = emptyList()
        _error.value = null
    }

    private suspend fun refresh() = withContext(io) {
        val generation = cacheGeneration
        val sales = client.from("sales")
            .select { order("created_at", Order.DESCENDING) }
            .decodeList<SaleDto>()
            .map { it.toDomain() }
        if (generation == cacheGeneration) {
            _sales.value = sales
        }
    }

    private suspend fun refreshSafely() {
        try {
            refresh()
            _error.value = null
        } catch (t: Throwable) {
            _error.value = AppError.from(t)
        }
    }

    /** Logs [event] on success, or reports the underlying cause to Crashlytics on failure. */
    private fun <T> DataResult<T>.track(event: String, params: Map<String, String> = emptyMap()): DataResult<T> {
        when (this) {
            is DataResult.Success -> Track.event(event, params)
            is DataResult.Failure -> {
                Track.breadcrumb("$event failed: ${error::class.simpleName}")
                error.cause?.let { Track.error(it) }
            }
        }
        return this
    }
}

private val SALES_RPC_JSON = Json { encodeDefaults = true }
