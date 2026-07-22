package com.circuitflip.flipledger.domain.repository

import com.circuitflip.flipledger.core.DataResult
import com.circuitflip.flipledger.domain.model.Sale
import kotlinx.coroutines.flow.Flow

interface SalesRepository {
    fun observeSales(): Flow<List<Sale>>

    /** Records a sale and removes the underlying device from active inventory. */
    suspend fun recordSale(sale: Sale, soldDeviceId: String): DataResult<Sale>
}
