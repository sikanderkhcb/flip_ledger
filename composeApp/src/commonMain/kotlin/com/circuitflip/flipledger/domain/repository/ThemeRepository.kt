package com.circuitflip.flipledger.domain.repository

import kotlinx.coroutines.flow.Flow

/** Persisted light/dark preference (loft vs rp-new-dark themes). */
interface ThemeRepository {
    val isDark: Flow<Boolean>
    suspend fun setDark(dark: Boolean)
}
