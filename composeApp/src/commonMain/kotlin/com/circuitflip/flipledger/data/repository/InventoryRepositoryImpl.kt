package com.circuitflip.flipledger.data.repository

import com.circuitflip.flipledger.core.DataResult
import com.circuitflip.flipledger.core.runCatchingResult
import com.circuitflip.flipledger.data.remote.dto.CostDto
import com.circuitflip.flipledger.data.remote.dto.DeviceDto
import com.circuitflip.flipledger.data.remote.dto.toDomain
import com.circuitflip.flipledger.data.remote.dto.toDto
import com.circuitflip.flipledger.domain.model.Cost
import com.circuitflip.flipledger.domain.model.Device
import com.circuitflip.flipledger.domain.model.DeviceStatus
import com.circuitflip.flipledger.domain.repository.InventoryRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
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
    private val scope = CoroutineScope(SupervisorJob() + io)

    // Emit the cached list immediately and refresh in the background, so opening a screen
    // never blocks on the network (the cached device shows instantly, then updates).
    override fun observeInventory(): Flow<List<Device>> =
        _devices.onStart { scope.launch { runCatching { refresh() } } }

    override fun observeDevice(id: String): Flow<Device?> =
        observeInventory().map { list -> list.firstOrNull { it.id == id } }

    override suspend fun getDevice(id: String): Device? {
        if (_devices.value.none { it.id == id }) runCatching { refresh() }
        return _devices.value.firstOrNull { it.id == id }
    }

    override suspend fun addDevice(device: Device): DataResult<Device> = withContext(io) {
        val result = runCatchingResult {
            client.from("devices").insert(device.toDto())
            if (device.costs.isNotEmpty()) {
                client.from("costs").insert(device.costs.map { it.toDto(device.id) })
            }
            device
        }
        runCatching { refresh() }
        result
    }

    override suspend fun updateStatus(deviceId: String, status: DeviceStatus): DataResult<Unit> = withContext(io) {
        val result = runCatchingResult {
            client.from("devices").update({ set("status", status.label) }) { filter { eq("id", deviceId) } }
            Unit
        }
        runCatching { refresh() }
        result
    }

    override suspend fun addCost(deviceId: String, cost: Cost): DataResult<Unit> = withContext(io) {
        val result = runCatchingResult {
            client.from("costs").insert(cost.toDto(deviceId))
            Unit
        }
        runCatching { refresh() }
        result
    }

    override suspend fun deleteDevice(deviceId: String): DataResult<Unit> = withContext(io) {
        val result = runCatchingResult {
            client.from("devices").delete { filter { eq("id", deviceId) } }
            Unit
        }
        runCatching { refresh() }
        result
    }

    /** Reloads devices + their costs into [_devices]. Costs are grouped by device id. */
    private suspend fun refresh() = withContext(io) {
        val devices = client.from("devices")
            .select { order("created_at", Order.DESCENDING) }
            .decodeList<DeviceDto>()
        val costs = client.from("costs").select().decodeList<CostDto>()
        val byDevice = costs.groupBy { it.deviceId }
        _devices.value = devices.map { d -> d.toDomain(byDevice[d.id].orEmpty().map { it.toDomain() }) }
    }
}
