package com.circuitflip.flipledger.domain.repository

import com.circuitflip.flipledger.core.AppError
import com.circuitflip.flipledger.domain.model.BusinessProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ProfileRepository {
    val error: StateFlow<AppError?>

    fun observeProfile(): Flow<BusinessProfile>
    suspend fun getProfile(): BusinessProfile
    suspend fun updateProfile(profile: BusinessProfile)

    /** Sets only the owner's display name (sourced from auth, kept separate from form edits). */
    suspend fun setOwnerName(name: String)

    /** True once the user has completed onboarding (Setup). Persisted on the profile row. */
    suspend fun isOnboarded(): Boolean

    /** Marks onboarding complete so future launches skip straight to the dashboard. */
    suspend fun setOnboarded()

    /** Removes in-memory profile data when the authenticated session changes. */
    fun clearCache()
}
