package com.blackink.app.di

import com.blackink.app.core.review.ReviewGate
import com.blackink.app.data.remote.createBlackInkSupabaseClient
import com.blackink.app.data.repository.AuthRepositoryImpl
import com.blackink.app.data.repository.InventoryRepositoryImpl
import com.blackink.app.data.repository.ProfileRepositoryImpl
import com.blackink.app.data.repository.SalesRepositoryImpl
import com.blackink.app.data.repository.SubscriptionRepositoryImpl
import com.blackink.app.data.repository.ThemeRepositoryImpl
import com.blackink.app.domain.repository.AuthRepository
import com.blackink.app.domain.repository.InventoryRepository
import com.blackink.app.domain.repository.ProfileRepository
import com.blackink.app.domain.repository.SalesRepository
import com.blackink.app.domain.repository.SubscriptionRepository
import com.blackink.app.domain.repository.ThemeRepository
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.Dispatchers
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** Supabase client + repository wiring shared across platforms. */
val coreModule = module {

    // Dispatcher for IO-bound work (network + decode).
    single(named("io")) { Dispatchers.Default }

    // Supabase (Postgres data API + Auth).
    single<SupabaseClient> { createBlackInkSupabaseClient() }

    // Repositories
    single<ProfileRepository> { ProfileRepositoryImpl(get(), get(named("io"))) }
    single<InventoryRepository> { InventoryRepositoryImpl(get(), get(named("io"))) }
    single<SalesRepository> { SalesRepositoryImpl(get(), get(), get(named("io"))) }
    single<SubscriptionRepository> { SubscriptionRepositoryImpl(get(), get(named("io"))) }
    single<ThemeRepository> { ThemeRepositoryImpl(get()) }
    single<AuthRepository> { AuthRepositoryImpl(get(), get(), get(), get(), get()) }

    // Decides when to show the native rating prompt (durable state via ObservableSettings).
    single { ReviewGate(get()) }
}
