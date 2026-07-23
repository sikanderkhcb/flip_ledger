package com.circuitflip.flipledger.data.repository

import com.circuitflip.flipledger.domain.repository.ThemeRepository
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.getBooleanFlow
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalSettingsApi::class)
class ThemeRepositoryImpl(private val settings: ObservableSettings) : ThemeRepository {
    override val isDark: Flow<Boolean> = settings.getBooleanFlow(KEY, false)
    override suspend fun setDark(dark: Boolean) = settings.putBoolean(KEY, dark)
    private companion object { const val KEY = "theme.dark" }
}
