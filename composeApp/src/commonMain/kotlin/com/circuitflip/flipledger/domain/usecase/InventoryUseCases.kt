package com.circuitflip.flipledger.domain.usecase

import com.circuitflip.flipledger.core.AppError
import com.circuitflip.flipledger.core.DataResult
import com.circuitflip.flipledger.domain.model.Cost
import com.circuitflip.flipledger.domain.model.CostDraft
import com.circuitflip.flipledger.domain.model.Device
import com.circuitflip.flipledger.domain.model.DeviceDraft
import com.circuitflip.flipledger.domain.model.DeviceStatus
import com.circuitflip.flipledger.domain.model.LockStatus
import com.circuitflip.flipledger.domain.repository.InventoryRepository
import com.circuitflip.flipledger.domain.util.Money
import com.circuitflip.flipledger.domain.util.Dates
import com.circuitflip.flipledger.domain.util.Ids
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
class AddDeviceUseCase(private val repo: InventoryRepository) {
    suspend operator fun invoke(draft: DeviceDraft): DataResult<Device> {
        val category = draft.category
            ?: return DataResult.Failure(AppError.Validation("category", "Choose a device category."))
        if (draft.model.isBlank()) {
            return DataResult.Failure(AppError.Validation("model", "Enter the device model."))
        }
        val price = Money.parseToCentsOrNull(draft.price)
            ?: return DataResult.Failure(AppError.Validation("price", "Enter a valid price with at most two decimal places."))
        if (price <= 0L) {
            return DataResult.Failure(AppError.Validation("price", "Purchase price must be greater than zero."))
        }
        val purchaseDate = Dates.parseIso(draft.date)
            ?: return DataResult.Failure(AppError.Validation("date", "Use a valid date in YYYY-MM-DD format."))
        if (purchaseDate.toEpochDays() > Dates.today().toEpochDays()) {
            return DataResult.Failure(AppError.Validation("date", "Purchase date cannot be in the future."))
        }
        if (draft.identifierLast4.isNotBlank() &&
            (draft.identifierLast4.length != 4 || draft.identifierLast4.any { !it.isDigit() })
        ) {
            return DataResult.Failure(AppError.Validation("identifier", "Enter exactly the last 4 digits, or leave it blank."))
        }
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
            storage = draft.storage.ifBlank { "—" },
            lock = draft.lock ?: LockStatus.NONE,
            purchasePriceCents = price,
            source = draft.source,
            purchaseDate = purchaseDate.toString(),
            costs = emptyList(),
            status = DeviceStatus.PURCHASED,
            daysHeld = Dates.daysBetween(purchaseDate.toString()) ?: 0,
        )
        return repo.addDevice(device)
    }
}

class UpdateDeviceStatusUseCase(private val repo: InventoryRepository) {
    suspend operator fun invoke(deviceId: String, status: DeviceStatus) =
        repo.updateStatus(deviceId, status)
}

/** Validates and persists a cost from a [CostDraft]. */
class AddCostUseCase(private val repo: InventoryRepository) {
    suspend operator fun invoke(deviceId: String, draft: CostDraft): DataResult<Unit> {
        val type = draft.type
            ?: return DataResult.Failure(AppError.Validation("type", "Choose a cost type."))
        val cents = Money.parseToCentsOrNull(draft.amount)
            ?: return DataResult.Failure(AppError.Validation("amount", "Enter a valid amount with at most two decimal places."))
        if (cents <= 0L) {
            return DataResult.Failure(AppError.Validation("amount", "Amount must be greater than zero."))
        }
        val date = Dates.parseIso(draft.date)
            ?: return DataResult.Failure(AppError.Validation("date", "Use a valid date in YYYY-MM-DD format."))
        val cost = Cost(
            id = Ids.new("c"),
            type = type,
            amountCents = cents,
            paidBy = draft.paidBy,
            date = date.toString(),
            note = draft.note,
        )
        return repo.addCost(deviceId, cost)
    }
}
