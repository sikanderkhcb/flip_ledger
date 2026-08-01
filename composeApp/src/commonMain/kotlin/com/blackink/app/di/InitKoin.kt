package com.blackink.app.di

import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

/**
 * Single entry point for DI. Called from the Android Application and the iOS app delegate.
 * [appDeclaration] lets Android inject the androidContext.
 */
fun initKoin(appDeclaration: KoinAppDeclaration = {}): KoinApplication {
    Napier.base(DebugAntilog())
    return startKoin {
        appDeclaration()
        modules(platformModule(), coreModule, domainModule, presentationModule)
    }
}

/** Convenience for iOS, which cannot pass a trailing lambda easily. */
fun initKoinIos() = initKoin()
