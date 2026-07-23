package com.circuitflip.flipledger.domain.repository

import com.circuitflip.flipledger.core.DataResult
import com.circuitflip.flipledger.domain.model.AuthDraft
import kotlinx.coroutines.flow.Flow

/** Coarse session status used to pick the app's start destination on launch. */
enum class SessionState { LOADING, AUTHENTICATED, UNAUTHENTICATED }

/** Authentication + session state. */
interface AuthRepository {
    val isAuthenticated: Flow<Boolean>

    /** Live session status, including the initial LOADING while it's restored from storage. */
    val sessionState: Flow<SessionState>

    suspend fun signIn(email: String, password: String): DataResult<Unit>
    suspend fun signUp(draft: AuthDraft): DataResult<Unit>
    suspend fun signInWithApple(identityToken: String): DataResult<Unit>
    suspend fun signInWithGoogle(identityToken: String): DataResult<Unit>
    suspend fun signOut(): DataResult<Unit>

    /** Refreshes the profile's owner name from the signed-in user (metadata / email). */
    suspend fun syncProfileName()
}
