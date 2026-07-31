package com.blackink.app.data.repository

import com.blackink.app.core.AppError
import com.blackink.app.core.DataResult
import com.blackink.app.core.runCatchingResult
import com.blackink.app.core.telemetry.Track
import com.blackink.app.data.remote.dto.CostDto
import com.blackink.app.data.remote.dto.DeviceDto
import com.blackink.app.data.remote.dto.toDomain
import com.blackink.app.data.remote.dto.toDto
import com.blackink.app.domain.model.Cost
import com.blackink.app.domain.model.Device
import com.blackink.app.domain.model.DeviceStatus
import com.blackink.app.domain.model.DeviceCareDraft
import com.blackink.app.domain.repository.InventoryRepository
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
 * Inventory backed by the Supabase `devices` + `costs` tables. A shared [MutableStateFlow]
 * caches the current list; it re-fetches when a new collector starts (via [onStart]) and
 * after every mutation. Sold devices are deleted from `devices`, so the inventory naturally
 * excludes them. Row Level Security scopes all reads/writes to the signed-in user.
 */
class InventoryRepositoryImpl(
    private val client: SupabaseClient,
    private val io: CoroutineDispatcher,
) : InventoryRepository {

    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    private val _error = MutableStateFlow<AppError?>(null)
    override val error = _error
    private val scope = CoroutineScope(SupervisorJob() + io)
    private var cacheGeneration: Long = 0

    // Emit the cached list immediately and refresh in the background, so opening a screen
    // never blocks on the network (the cached device shows instantly, then updates).
    override fun observeInventory(): Flow<List<Device>> =
        _devices.onStart { scope.launch { refreshSafely() } }

    override fun observeDevice(id: String): Flow<Device?> =
        observeInventory().map { list -> list.firstOrNull { it.id == id } }

    override suspend fun getDevice(id: String): Device? {
        if (_devices.value.none { it.id == id }) refreshSafely()
        return _devices.value.firstOrNull { it.id == id }
    }

    override suspend fun addDevice(device: Device): DataResult<Device> = withContext(io) {
        val generation = cacheGeneration
        val result = runCatchingResult {
            client.postgrest.rpc(
                function = "add_device",
                parameters = buildJsonObject {
                    put("p_device", INVENTORY_RPC_JSON.encodeToJsonElement(device.toDto()))
                    put(
                        "p_costs",
                        INVENTORY_RPC_JSON.encodeToJsonElement(device.costs.map { it.toDto(device.id) }),
                    )
                },
            )
            device
        }
        if (generation == cacheGeneration && result is DataResult.Success) {
            _devices.value = listOf(result.data) + _devices.value.filterNot { it.id == result.data.id }
        }
        refreshSafely()
        result.track("device_added", mapOf("cost_count" to device.costs.size.toString()))
    }

    override suspend fun updateStatus(deviceId: String, status: DeviceStatus): DataResult<Unit> = withContext(io) {
        val generation = cacheGeneration
        val result = runCatchingResult {
            client.from("devices").update({ set("status", status.label) }) { filter { eq("id", deviceId) } }
            Unit
        }
        if (generation == cacheGeneration && result is DataResult.Success) {
            _devices.value = _devices.value.map {
                if (it.id == deviceId) it.copy(status = status) else it
            }
        }
        refreshSafely()
        result
    }

    override suspend fun updateDeviceCare(deviceId: String, care: DeviceCareDraft): DataResult<Unit> = withContext(io) {
        val generation = cacheGeneration
        val result = runCatchingResult {
            client.from("devices").update({
                set("repair_issue", care.repairIssue.trim())
                set("repair_provider", care.repairProvider.trim())
                set("repair_started_on", care.repairStartedOn.trim().ifBlank { null })
                set("repair_completed_on", care.repairCompletedOn.trim().ifBlank { null })
                set("warranty_provider", care.warrantyProvider.trim())
                set("warranty_expires_on", care.warrantyExpiresOn.trim().ifBlank { null })
            }) { filter { eq("id", deviceId) } }
            Unit
        }
        if (generation == cacheGeneration && result is DataResult.Success) {
            _devices.value = _devices.value.map {
                if (it.id == deviceId) it.copy(
                    repairIssue = care.repairIssue.trim(),
                    repairProvider = care.repairProvider.trim(),
                    repairStartedOn = care.repairStartedOn.trim().ifBlank { null },
                    repairCompletedOn = care.repairCompletedOn.trim().ifBlank { null },
                    warrantyProvider = care.warrantyProvider.trim(),
                    warrantyExpiresOn = care.warrantyExpiresOn.trim().ifBlank { null },
                ) else it
            }
        }
        refreshSafely()
        result
    }

    override suspend fun addCost(deviceId: String, cost: Cost): DataResult<Unit> = withContext(io) {
        val generation = cacheGeneration
        val result = runCatchingResult {
            client.from("costs").insert(cost.toDto(deviceId))
            Unit
        }
        if (generation == cacheGeneration && result is DataResult.Success) {
            _devices.value = _devices.value.map {
                if (it.id == deviceId) it.copy(costs = it.costs + cost) else it
            }
        }
        refreshSafely()
        result.track("cost_added")
    }

    override suspend fun deleteDevice(deviceId: String): DataResult<Unit> = withContext(io) {
        val generation = cacheGeneration
        val result = runCatchingResult {
            client.from("devices").delete { filter { eq("id", deviceId) } }
            Unit
        }
        if (generation == cacheGeneration && result is DataResult.Success) removeCachedDevice(deviceId)
        refreshSafely()
        result
    }

    override fun clearCache() {
        cacheGeneration += 1
        _devices.value = emptyList()
        _error.value = null
    }

    override fun removeCachedDevice(deviceId: String) {
        _devices.value = _devices.value.filterNot { it.id == deviceId }
    }

    /** Reloads devices + their costs into [_devices]. Costs are grouped by device id. */
    private suspend fun refresh() = withContext(io) {
        val generation = cacheGeneration
        val devices = client.from("devices")
            .select { order("created_at", Order.DESCENDING) }
            .decodeList<DeviceDto>()
        val costs = client.from("costs").select().decodeList<CostDto>()
        val byDevice = costs.groupBy { it.deviceId }
        if (generation == cacheGeneration) {
            _devices.value = devices.map { d ->
                d.toDomain(byDevice[d.id].orEmpty().map { it.toDomain() })
            }
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

    /**
     * Logs an analytics [event] on success, or reports the underlying cause to Crashlytics on
     * failure (with a breadcrumb naming the error type). Returns the receiver unchanged.
     */
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

private val INVENTORY_RPC_JSON = Json { encodeDefaults = true }
