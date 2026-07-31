package com.blackink.app.domain.repository

import com.blackink.app.core.DataResult
import com.blackink.app.domain.model.AuthDraft
import kotlinx.coroutines.flow.Flow

/** Coarse session status used to pick the app's start destination on launch. */
enum class SessionState { LOADING, AUTHENTICATED, UNAUTHENTICATED }

/**
 * Outcome of a successful sign-up call. Either a session was created immediately (email
 * confirmation off), or the user must enter the OTP emailed to them before a session exists.
 */
sealed interface SignUpOutcome {
    data object SignedIn : SignUpOutcome
    data class NeedsVerification(val email: String) : SignUpOutcome
}

/** Authentication + session state. */
interface AuthRepository {
    val isAuthenticated: Flow<Boolean>

    /** Live session status, including the initial LOADING while it's restored from storage. */
    val sessionState: Flow<SessionState>

    suspend fun signIn(email: String, password: String): DataResult<Unit>
    suspend fun signUp(draft: AuthDraft): DataResult<SignUpOutcome>

    /** Verifies the 6-digit sign-up OTP emailed to [email]; on success a session is established. */
    suspend fun verifySignupOtp(email: String, code: String): DataResult<Unit>

    /** Re-sends the sign-up OTP to [email]. */
    suspend fun resendSignupOtp(email: String): DataResult<Unit>

    suspend fun signInWithApple(identityToken: String): DataResult<Unit>
    suspend fun signInWithGoogle(identityToken: String): DataResult<Unit>
    suspend fun signOut(): DataResult<Unit>

    /** Refreshes the profile's owner name from the signed-in user (metadata / email). */
    suspend fun syncProfileName()
}
