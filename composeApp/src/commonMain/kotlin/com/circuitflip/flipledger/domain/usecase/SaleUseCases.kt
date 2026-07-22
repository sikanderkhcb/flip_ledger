package com.circuitflip.flipledger.domain.usecase

import com.circuitflip.flipledger.core.DataResult
import com.circuitflip.flipledger.domain.model.Device
import com.circuitflip.flipledger.domain.model.Sale
import com.circuitflip.flipledger.domain.model.SaleDraft
import com.circuitflip.flipledger.domain.repository.SalesRepository
import com.circuitflip.flipledger.domain.util.Money
import com.circuitflip.flipledger.domain.util.ProfitCalculator
import kotlinx.coroutines.flow.Flow
import kotlin.random.Random

class ObserveSalesUseCase(private val repo: SalesRepository) {
    operator fun invoke(): Flow<List<Sale>> = repo.observeSales()
}

/**
 * Finalizes a sale: builds the [Sale] record from the device + draft, records it, and
 * removes the device from inventory. Returns the created [Sale] so the result screen can
 * show the net-profit snapshot.
 */
class CompleteSaleUseCase(private val repo: SalesRepository) {
    suspend operator fun invoke(device: Device, draft: SaleDraft): DataResult<Sale> {
        val invested = device.investedCents
        val fees = ProfitCalculator.feesCents(draft)
        val revenue = Money.parseToCents(draft.price)
        val sale = Sale(
            id = "s" + Random.nextLong().toString(16).takeLast(10),
            model = device.model,
            soldDate = "Jul 12",
            channel = draft.channel,
            revenueCents = revenue,
            costCents = invested,
            feesCents = fees,
            daysHeld = device.daysHeld,
        )
        return repo.recordSale(sale, soldDeviceId = device.id)
    }
}
