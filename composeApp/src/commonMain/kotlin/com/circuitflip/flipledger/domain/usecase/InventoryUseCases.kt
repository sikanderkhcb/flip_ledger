package com.circuitflip.flipledger.domain.usecase

import com.circuitflip.flipledger.core.DataResult
import com.circuitflip.flipledger.domain.model.Cost
import com.circuitflip.flipledger.domain.model.CostDraft
import com.circuitflip.flipledger.domain.model.Device
import com.circuitflip.flipledger.domain.model.DeviceCategory
import com.circuitflip.flipledger.domain.model.DeviceDraft
import com.circuitflip.flipledger.domain.model.DeviceStatus
import com.circuitflip.flipledger.domain.model.LockStatus
import com.circuitflip.flipledger.domain.repository.InventoryRepository
import com.circuitflip.flipledger.domain.util.Money
import kotlinx.coroutines.flow.Flow
import kotlin.random.Random

/** Reactive inventory stream, optionally filtered by status and search query. */
class ObserveInventoryUseCase(private val repo: InventoryRepository) {
    operator fun invoke(): Flow<List<Device>> = repo.observeInventory()
}

class ObserveDeviceUseCase(private val repo: InventoryRepository) {
    operator fun invoke(id: String): Flow<Device?> = repo.observeDevice(id)
}

/**
 * Converts a completed [DeviceDraft] into a persisted [Device]. Applies the same defaulting
 * rules as the reference design (masked identifier, "Untitled device", etc.).
 */
class AddDeviceUseCase(private val repo: InventoryRepository) {
    suspend operator fun invoke(draft: DeviceDraft): DataResult<Device> {
        val id = "d" + Random.nextLong().toString(16).takeLast(10)
        val identifier = if (draft.identifierLast4.isNotBlank()) {
            "IMEI ●●●●${draft.identifierLast4.takeLast(4)}"
        } else {
            "No identifier on file"
        }
        val device = Device(
            id = id,
            category = draft.category ?: DeviceCategory.PHONE,
            model = draft.model.ifBlank { "Untitled device" },
            identifier = identifier,
            condition = draft.condition,
            storage = draft.storage.ifBlank { "—" },
            lock = draft.lock ?: LockStatus.NONE,
            purchasePriceCents = Money.parseToCents(draft.price),
            source = draft.source,
            purchaseDate = draft.date,
            costs = emptyList(),
            status = DeviceStatus.PURCHASED,
            daysHeld = 0,
        )
        return repo.addDevice(device)
    }
}

class UpdateDeviceStatusUseCase(private val repo: InventoryRepository) {
    suspend operator fun invoke(deviceId: String, status: DeviceStatus) =
        repo.updateStatus(deviceId, status)
}

/** Persists a cost from a [CostDraft]. No-op if type/amount missing (matches reference). */
class AddCostUseCase(private val repo: InventoryRepository) {
    suspend operator fun invoke(deviceId: String, draft: CostDraft): DataResult<Unit> {
        val type = draft.type ?: return DataResult.Success(Unit)
        val cents = Money.parseToCents(draft.amount)
        if (cents <= 0L) return DataResult.Success(Unit)
        val cost = Cost(
            id = "c" + Random.nextLong().toString(16).takeLast(8),
            type = type,
            amountCents = cents,
            paidBy = draft.paidBy,
            date = draft.date,
            note = draft.note,
        )
        return repo.addCost(deviceId, cost)
    }
}
