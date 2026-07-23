package com.circuitflip.flipledger.domain.usecase

import com.circuitflip.flipledger.core.AppError
import com.circuitflip.flipledger.core.DataResult
import com.circuitflip.flipledger.domain.model.Device
import com.circuitflip.flipledger.domain.model.Sale
import com.circuitflip.flipledger.domain.model.SaleDraft
import com.circuitflip.flipledger.domain.repository.SalesRepository
import com.circuitflip.flipledger.domain.util.Dates
import com.circuitflip.flipledger.domain.util.Ids
import com.circuitflip.flipledger.domain.util.Money
import com.circuitflip.flipledger.domain.util.ProfitCalculator
import kotlinx.coroutines.flow.Flow

class ObserveSalesUseCase(private val repo: SalesRepository) {
    operator fun invoke(): Flow<List<Sale>> = repo.observeSales()
    val error get() = repo.error
}

/**
 * Finalizes a sale: builds the [Sale] record from the device + draft, records it, and
 * removes the device from inventory. Returns the created [Sale] so the result screen can
 * show the net-profit snapshot.
 */
class CompleteSaleUseCase(private val repo: SalesRepository) {
    suspend operator fun invoke(device: Device, draft: SaleDraft): DataResult<Sale> {
        val revenue = Money.parseToCentsOrNull(draft.price)
            ?: return DataResult.Failure(AppError.Validation("price", "Enter a valid sale price with at most two decimal places."))
        if (revenue <= 0L) {
            return DataResult.Failure(AppError.Validation("price", "Sale price must be greater than zero."))
        }
        val soldDate = Dates.parseIso(draft.date)
            ?: return DataResult.Failure(AppError.Validation("date", "Use a valid date in YYYY-MM-DD format."))
        val purchaseDate = Dates.parseIso(device.purchaseDate)
        if (purchaseDate != null && soldDate.toEpochDays() < purchaseDate.toEpochDays()) {
            return DataResult.Failure(AppError.Validation("date", "Sale date cannot be before the purchase date."))
        }
        if (draft.channel == null) {
            return DataResult.Failure(AppError.Validation("channel", "Choose a sales channel."))
        }
        listOf(
            "platform fee" to draft.platformFee,
            "payment fee" to draft.paymentFee,
            "shipping" to draft.shipping,
            "packaging" to draft.packaging,
            "other fee" to draft.otherFee,
        ).firstOrNull { (_, value) -> value.isNotBlank() && Money.parseToCentsOrNull(value) == null }
            ?.let { (field, _) ->
                return DataResult.Failure(
                    AppError.Validation(field, "Enter valid fees with at most two decimal places."),
                )
            }
        val invested = device.investedCents
        val fees = ProfitCalculator.feesCents(draft)
        val sale = Sale(
            id = Ids.new("s"),
            model = device.model,
            soldDate = soldDate.toString(),
            channel = draft.channel,
            revenueCents = revenue,
            costCents = invested,
            feesCents = fees,
            daysHeld = Dates.daysBetween(device.purchaseDate, soldDate.toString()) ?: device.daysHeld,
        )
        return repo.recordSale(sale, soldDeviceId = device.id)
    }
}
