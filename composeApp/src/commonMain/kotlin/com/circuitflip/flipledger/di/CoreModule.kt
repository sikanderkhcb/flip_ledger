package com.circuitflip.flipledger.di

import com.circuitflip.flipledger.data.remote.createFlipLedgerSupabaseClient
import com.circuitflip.flipledger.data.repository.AuthRepositoryImpl
import com.circuitflip.flipledger.data.repository.InventoryRepositoryImpl
import com.circuitflip.flipledger.data.repository.ProfileRepositoryImpl
import com.circuitflip.flipledger.data.repository.SalesRepositoryImpl
import com.circuitflip.flipledger.data.repository.ThemeRepositoryImpl
import com.circuitflip.flipledger.domain.repository.AuthRepository
import com.circuitflip.flipledger.domain.repository.InventoryRepository
import com.circuitflip.flipledger.domain.repository.ProfileRepository
import com.circuitflip.flipledger.domain.repository.SalesRepository
import com.circuitflip.flipledger.domain.repository.ThemeRepository
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.Dispatchers
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** Supabase client + repository wiring shared across platforms. */
val coreModule = module {

    // Dispatcher for IO-bound work (network + decode).
    single(named("io")) { Dispatchers.Default }

    // Supabase (Postgres data API + Auth).
    single<SupabaseClient> { createFlipLedgerSupabaseClient() }

    // Repositories
    single<ProfileRepository> { ProfileRepositoryImpl(get(), get(named("io"))) }
    single<InventoryRepository> { InventoryRepositoryImpl(get(), get(named("io"))) }
    single<SalesRepository> { SalesRepositoryImpl(get(), get(), get(named("io"))) }
    single<ThemeRepository> { ThemeRepositoryImpl(get()) }
    single<AuthRepository> { AuthRepositoryImpl(get(), get(), get(), get()) }
}
