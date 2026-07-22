package com.circuitflip.flipledger.di

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.ObservableSettings
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults

actual fun platformModule(): Module = module {
    single<ObservableSettings> {
        NSUserDefaultsSettings(NSUserDefaults(suiteName = "flipledger.prefs"))
    }
    single<com.russhwolf.settings.Settings> { get<ObservableSettings>() }
}
