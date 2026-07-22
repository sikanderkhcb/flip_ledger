package com.circuitflip.flipledger.data.repository

import com.circuitflip.flipledger.core.DataResult
import com.circuitflip.flipledger.core.runCatchingResult
import com.circuitflip.flipledger.data.remote.dto.SaleDto
import com.circuitflip.flipledger.data.remote.dto.toDomain
import com.circuitflip.flipledger.data.remote.dto.toDto
import com.circuitflip.flipledger.domain.model.Sale
import com.circuitflip.flipledger.domain.repository.SalesRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Sales history backed by the Supabase `sales` table. Recording a sale also deletes the
 * underlying device from `devices` (its `costs` cascade-delete via the FK), removing it from
 * active inventory.
 */
class SalesRepositoryImpl(
    private val client: SupabaseClient,
    private val io: CoroutineDispatcher,
) : SalesRepository {

    private val _sales = MutableStateFlow<List<Sale>>(emptyList())
    private val scope = CoroutineScope(SupervisorJob() + io)

    override fun observeSales(): Flow<List<Sale>> =
        _sales.onStart { scope.launch { runCatching { refresh() } } }

    override suspend fun recordSale(sale: Sale, soldDeviceId: String): DataResult<Sale> = withContext(io) {
        val result = runCatchingResult {
            client.from("sales").insert(sale.toDto())
            client.from("devices").delete { filter { eq("id", soldDeviceId) } }
            sale
        }
        runCatching { refresh() }
        result
    }

    private suspend fun refresh() = withContext(io) {
        _sales.value = client.from("sales")
            .select { order("created_at", Order.DESCENDING) }
            .decodeList<SaleDto>()
            .map { it.toDomain() }
    }
}
