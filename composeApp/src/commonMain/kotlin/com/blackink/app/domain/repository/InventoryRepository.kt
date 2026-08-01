package com.blackink.app.domain.repository

import com.blackink.app.core.AppError
import com.blackink.app.core.DataResult
import com.blackink.app.domain.model.Cost
import com.blackink.app.domain.model.Device
import com.blackink.app.domain.model.DeviceStatus
import com.blackink.app.domain.model.DeviceCareDraft
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Contract for reading/writing the authenticated user's devices. */
interface InventoryRepository {
    val error: StateFlow<AppError?>

    /** Reactive stream of the full inventory (excludes sold devices). */
    fun observeInventory(): Flow<List<Device>>

    fun observeDevice(id: String): Flow<Device?>

    suspend fun getDevice(id: String): Device?

    suspend fun addDevice(device: Device): DataResult<Device>

    suspend fun updateStatus(deviceId: String, status: DeviceStatus): DataResult<Unit>

    suspend fun updateDeviceCare(deviceId: String, care: DeviceCareDraft): DataResult<Unit>

    suspend fun addCost(deviceId: String, cost: Cost): DataResult<Unit>

    suspend fun deleteDevice(deviceId: String): DataResult<Unit>

    /** Removes in-memory data when the authenticated session changes. */
    fun clearCache()

    /** Removes a device from the local view after an atomic server-side sale completes. */
    fun removeCachedDevice(deviceId: String)
}
