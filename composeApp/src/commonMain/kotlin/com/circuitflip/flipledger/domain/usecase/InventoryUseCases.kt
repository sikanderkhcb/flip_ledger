package com.circuitflip.flipledger.domain.usecase

import com.circuitflip.flipledger.core.DataResult
import com.circuitflip.flipledger.domain.model.Cost
import com.circuitflip.flipledger.domain.model.CostDraft
import com.circuitflip.flipledger.domain.model.Device
import com.circuitflip.flipledger.domain.model.DeviceDraft
import com.circuitflip.flipledger.domain.model.DeviceStatus
import com.circuitflip.flipledger.domain.model.DeviceCareDraft
import com.circuitflip.flipledger.core.AppError
import com.circuitflip.flipledger.domain.model.LockStatus
import com.circuitflip.flipledger.domain.repository.InventoryRepository
import com.circuitflip.flipledger.domain.repository.SubscriptionRepository
import com.circuitflip.flipledger.domain.util.Dates
import com.circuitflip.flipledger.domain.util.FormValidation
import com.circuitflip.flipledger.domain.util.Ids
import com.circuitflip.flipledger.domain.util.Money
import kotlinx.coroutines.flow.Flow

/** Reactive inventory stream, optionally filtered by status and search query. */
class ObserveInventoryUseCase(private val repo: InventoryRepository) {
    operator fun invoke(): Flow<List<Device>> = repo.observeInventory()
    val error get() = repo.error
}

class ObserveDeviceUseCase(private val repo: InventoryRepository) {
    operator fun invoke(id: String): Flow<Device?> = repo.observeDevice(id)
}

/**
 * Converts a completed [DeviceDraft] into a persisted [Device]. Applies the same defaulting
 * rules as the reference design (masked identifier, "Untitled device", etc.).
 */
class AddDeviceUseCase(
    private val repo: InventoryRepository,
    private val subscriptionRepository: SubscriptionRepository,
) {
    suspend operator fun invoke(draft: DeviceDraft): DataResult<Device> {
        FormValidation.firstError(FormValidation.device(draft))?.let {
            return DataResult.Failure(it)
        }
        val category = requireNotNull(draft.category)
        val price = requireNotNull(Money.parseToCentsOrNull(draft.price))
        val purchaseDate = requireNotNull(Dates.parseIso(draft.date))
        val id = Ids.new("d")
        val identifier = if (draft.identifierLast4.isNotBlank()) {
            "IMEI ●●●●${draft.identifierLast4.takeLast(4)}"
        } else {
            "No identifier on file"
        }
        val device = Device(
            id = id,
            category = category,
            model = draft.model.trim(),
            identifier = identifier,
            condition = draft.condition,
            storage = draft.storage.trim().ifBlank { "—" },
            lock = draft.lock ?: LockStatus.NONE,
            purchasePriceCents = price,
            source = draft.source,
            purchaseDate = purchaseDate.toString(),
            costs = emptyList(),
            status = DeviceStatus.PURCHASED,
            daysHeld = Dates.daysBetween(purchaseDate.toString()) ?: 0,
        )
        val result = repo.addDevice(device)
        if (result is DataResult.Success) subscriptionRepository.recordDeviceAdded()
        return result
    }
}

class UpdateDeviceStatusUseCase(private val repo: InventoryRepository) {
    suspend operator fun invoke(deviceId: String, status: DeviceStatus) =
        repo.updateStatus(deviceId, status)
}

class DeleteDeviceUseCase(private val repo: InventoryRepository) {
    suspend operator fun invoke(deviceId: String): DataResult<Unit> = repo.deleteDevice(deviceId)
}

class UpdateDeviceCareUseCase(private val repo: InventoryRepository) {
    suspend operator fun invoke(deviceId: String, draft: DeviceCareDraft): DataResult<Unit> {
        val dates = listOf(
            "repairStartedOn" to draft.repairStartedOn,
            "repairCompletedOn" to draft.repairCompletedOn,
            "warrantyExpiresOn" to draft.warrantyExpiresOn,
        )
        dates.firstOrNull { it.second.isNotBlank() && Dates.parseIso(it.second) == null }?.let {
            return DataResult.Failure(AppError.Validation(it.first, "Use YYYY-MM-DD."))
        }
        return repo.updateDeviceCare(deviceId, draft)
    }
}

/** Validates and persists a cost from a [CostDraft]. */
class AddCostUseCase(private val repo: InventoryRepository) {
    suspend operator fun invoke(deviceId: String, draft: CostDraft): DataResult<Unit> {
        FormValidation.firstError(FormValidation.cost(draft))?.let {
            return DataResult.Failure(it)
        }
        val purchaseDate = repo.getDevice(deviceId)?.purchaseDate
        FormValidation.firstError(FormValidation.cost(draft, purchaseDate))?.let {
            return DataResult.Failure(it)
        }
        val type = requireNotNull(draft.type)
        val cents = requireNotNull(Money.parseToCentsOrNull(draft.amount))
        val date = requireNotNull(Dates.parseIso(draft.date))
        val cost = Cost(
            id = Ids.new("c"),
            type = type,
            amountCents = cents,
            paidBy = draft.paidBy,
            date = date.toString(),
            note = draft.note.trim(),
        )
        return repo.addCost(deviceId, cost)
    }
}
