package com.blackink.app.di

import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.SharedPreferencesSettings
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<ObservableSettings> {
        SharedPreferencesSettings(
            androidContext().getSharedPreferences("blackink.prefs", android.content.Context.MODE_PRIVATE),
        )
    }
    single<com.russhwolf.settings.Settings> { get<ObservableSettings>() }
}
