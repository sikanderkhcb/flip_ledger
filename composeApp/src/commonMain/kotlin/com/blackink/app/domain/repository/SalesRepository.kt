package com.blackink.app.domain.repository

import com.blackink.app.core.AppError
import com.blackink.app.core.DataResult
import com.blackink.app.domain.model.Sale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface SalesRepository {
    val error: StateFlow<AppError?>

    fun observeSales(): Flow<List<Sale>>

    /** Records a sale and removes the underlying device from active inventory. */
    suspend fun recordSale(sale: Sale, soldDeviceId: String): DataResult<Sale>

    /** Removes in-memory data when the authenticated session changes. */
    fun clearCache()
}
