package com.circuitflip.flipledger.domain.usecase

import com.circuitflip.flipledger.core.DataResult
import com.circuitflip.flipledger.domain.model.Device
import com.circuitflip.flipledger.domain.model.Sale
import com.circuitflip.flipledger.domain.model.SaleDraft
import com.circuitflip.flipledger.domain.repository.SalesRepository
import com.circuitflip.flipledger.domain.util.Dates
import com.circuitflip.flipledger.domain.util.FormValidation
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
        FormValidation.firstError(FormValidation.sale(device, draft))?.let {
            return DataResult.Failure(it)
        }
        val revenue = requireNotNull(Money.parseToCentsOrNull(draft.price))
        val soldDate = requireNotNull(Dates.parseIso(draft.date))
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
            purchasePriceCents = device.purchasePriceCents,
            purchaseDate = device.purchaseDate,
            customerName = draft.customerName.trim(),
            customerEmail = draft.customerEmail.trim(),
            customerPhone = draft.customerPhone.trim(),
            customerAddress = draft.customerAddress.trim(),
        )
        return repo.recordSale(sale, soldDeviceId = device.id)
    }
}
