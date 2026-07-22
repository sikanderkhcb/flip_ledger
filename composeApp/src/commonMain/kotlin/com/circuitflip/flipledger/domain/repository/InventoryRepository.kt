package com.circuitflip.flipledger.domain.repository

import com.circuitflip.flipledger.core.DataResult
import com.circuitflip.flipledger.domain.model.Cost
import com.circuitflip.flipledger.domain.model.Device
import com.circuitflip.flipledger.domain.model.DeviceStatus
import kotlinx.coroutines.flow.Flow

/** Contract for reading/writing devices. Backed by SQLDelight with an offline-first policy. */
interface InventoryRepository {
    /** Reactive stream of the full inventory (excludes sold devices). */
    fun observeInventory(): Flow<List<Device>>

    fun observeDevice(id: String): Flow<Device?>

    suspend fun getDevice(id: String): Device?

    suspend fun addDevice(device: Device): DataResult<Device>

    suspend fun updateStatus(deviceId: String, status: DeviceStatus): DataResult<Unit>

    suspend fun addCost(deviceId: String, cost: Cost): DataResult<Unit>

    suspend fun deleteDevice(deviceId: String): DataResult<Unit>
}
