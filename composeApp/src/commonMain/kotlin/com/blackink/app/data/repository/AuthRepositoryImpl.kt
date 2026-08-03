package com.blackink.app.data.repository

import com.blackink.app.core.AppError
import com.blackink.app.core.DataResult
import com.blackink.app.core.notifications.Notify
import com.blackink.app.core.telemetry.Track
import com.blackink.app.domain.model.AuthDraft
import com.blackink.app.domain.repository.AuthRepository
import com.blackink.app.domain.repository.InventoryRepository
import com.blackink.app.domain.repository.ProfileRepository
import com.blackink.app.domain.repository.SalesRepository
import com.blackink.app.domain.repository.SessionState
import com.blackink.app.domain.repository.SignUpOutcome
import com.blackink.app.domain.repository.SubscriptionRepository
import com.blackink.app.domain.util.FormValidation
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.auth.providers.Apple
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Authentication backed by Supabase Auth. Sessions (and their refresh) are managed by the
 * SDK and persisted per platform; [isAuthenticated] mirrors the live session status.
 * Client-side validation stays so the UI can show field errors without a round-trip.
 */
class AuthRepositoryImpl(
    private val client: SupabaseClient,
    private val profileRepository: ProfileRepository,
    private val inventoryRepository: InventoryRepository,
    private val salesRepository: SalesRepository,
    private val subscriptionRepository: SubscriptionRepository,
) : AuthRepository {

    private companion object {
        // Register this exact URL in Supabase Auth > URL Configuration > Redirect URLs.
        const val PASSWORD_RESET_REDIRECT = "blackink://password-reset"
    }

    override val isAuthenticated: Flow<Boolean> =
        client.auth.sessionStatus.map { it is SessionStatus.Authenticated }

    override val sessionState: Flow<SessionState> =
        client.auth.sessionStatus.onEach {
            if (it is SessionStatus.NotAuthenticated) clearSessionData()
        }.map {
            when (it) {
                is SessionStatus.Authenticated -> SessionState.AUTHENTICATED
                is SessionStatus.NotAuthenticated -> SessionState.UNAUTHENTICATED
                else -> SessionState.LOADING // Initializing / refresh in progress
            }
        }

    override suspend fun signIn(email: String, password: String): DataResult<Unit> {
        val draft = AuthDraft(email = email, password = password)
        FormValidation.firstError(FormValidation.auth(draft, isSignUp = false))?.let {
            return DataResult.Failure(it)
        }
        return try {
            client.auth.signInWith(Email) {
                this.email = email.trim().lowercase()
                this.password = password
            }
            syncProfileName()
            trackSignedIn("password")
            DataResult.Success(Unit)
        } catch (t: Throwable) {
            DataResult.Failure(mapAuthError(t))
        }
    }

    override suspend fun signUp(draft: AuthDraft): DataResult<SignUpOutcome> {
        FormValidation.firstError(FormValidation.auth(draft, isSignUp = true))?.let {
            return DataResult.Failure(it)
        }
        val name = draft.name.trim()
        val email = draft.email.trim().lowercase()
        val businessName = draft.businessName.trim()
        return try {
            client.auth.signUpWith(Email) {
                this.email = email
                this.password = draft.password
                // Persist the name on the auth user so every future login can recover it.
                data = buildJsonObject {
                    put("full_name", name)
                    if (draft.phone.isNotBlank()) put("phone", draft.phone.trim())
                    if (businessName.isNotBlank()) put("business_name", businessName)
                }
            }
            if (client.auth.currentUserOrNull() == null) {
                // Email confirmation is required: no session yet. The user must enter the OTP
                // we just emailed; verifySignupOtp completes the sign-in.
                return DataResult.Success(SignUpOutcome.NeedsVerification(email))
            }
            // Confirmation disabled: signed in immediately — hydrate the profile and welcome.
            hydrateNewUserProfile(fallbackName = name, fallbackBusinessName = businessName)
            DataResult.Success(SignUpOutcome.SignedIn)
        } catch (t: Throwable) {
            DataResult.Failure(mapAuthError(t))
        }
    }

    override suspend fun verifySignupOtp(email: String, code: String): DataResult<Unit> = try {
        client.auth.verifyEmailOtp(
            type = OtpType.Email.SIGNUP,
            email = email.trim().lowercase(),
            token = code.trim(),
        )
        // A valid OTP establishes the session — hydrate from the metadata stored at sign-up.
        hydrateNewUserProfile()
        DataResult.Success(Unit)
    } catch (t: Throwable) {
        DataResult.Failure(mapAuthError(t))
    }

    override suspend fun resendSignupOtp(email: String): DataResult<Unit> = try {
        client.auth.resendEmail(type = OtpType.Email.SIGNUP, email = email.trim().lowercase())
        DataResult.Success(Unit)
    } catch (t: Throwable) {
        DataResult.Failure(mapAuthError(t))
    }

    override suspend fun sendPasswordReset(email: String): DataResult<Unit> = try {
        client.auth.resetPasswordForEmail(
            email = email.trim().lowercase(),
            redirectUrl = PASSWORD_RESET_REDIRECT,
        )
        DataResult.Success(Unit)
    } catch (t: Throwable) {
        DataResult.Failure(mapAuthError(t))
    }

    override suspend fun updatePassword(password: String): DataResult<Unit> = try {
        client.auth.updateUser { this.password = password }
        DataResult.Success(Unit)
    } catch (t: Throwable) {
        DataResult.Failure(mapAuthError(t))
    }

    override suspend fun handlePasswordResetUrl(url: String): DataResult<Unit> = try {
        val fragment = url.substringAfter('#', missingDelimiterValue = "")
        val values = fragment.split('&').mapNotNull { part ->
            part.split('=', limit = 2).takeIf { it.size == 2 }?.let { it[0] to it[1] }
        }.toMap()
        val accessToken = values["access_token"] ?: error("Missing recovery access token")
        val refreshToken = values["refresh_token"] ?: error("Missing recovery refresh token")
        val expiresIn = values["expires_in"]?.toLongOrNull() ?: 3600L
        client.auth.importSession(
            UserSession(
                accessToken = accessToken,
                refreshToken = refreshToken,
                expiresIn = expiresIn,
                tokenType = values["token_type"] ?: "bearer",
                user = null,
            ),
        )
        DataResult.Success(Unit)
    } catch (t: Throwable) {
        DataResult.Failure(mapAuthError(t))
    }

    /**
     * Post-authentication setup shared by the immediate-sign-in and OTP-verified paths: sets the
     * owner name (via the dedicated owner-name path so Setup/Settings can't clobber it), applies
     * the business name, records the sign-up, and fires the welcome notification. Values fall back
     * to the auth user's metadata (persisted at sign-up) when not passed directly.
     */
    private suspend fun hydrateNewUserProfile(
        fallbackName: String? = null,
        fallbackBusinessName: String? = null,
    ) {
        syncProfileName()
        val meta = client.auth.currentUserOrNull()?.userMetadata
        val businessName = fallbackBusinessName?.takeIf { it.isNotBlank() }
            ?: meta?.get("business_name")?.jsonPrimitive?.contentOrNull?.trim()
        if (!businessName.isNullOrBlank()) {
            profileRepository.updateProfile(
                profileRepository.getProfile().copy(businessName = businessName),
            )
        }
        trackSignedIn("password", isNew = true)
        val name = fallbackName?.takeIf { it.isNotBlank() }
            ?: meta?.get("full_name")?.jsonPrimitive?.contentOrNull?.trim()
        // Welcome the new user with a device notification (local; no push server needed).
        Notify.welcome(name)
    }

    override suspend fun signInWithApple(identityToken: String): DataResult<Unit> = try {
        client.auth.signInWith(IDToken) {
            idToken = identityToken
            provider = Apple
        }
        syncProfileName()
        trackSignedIn("apple")
        DataResult.Success(Unit)
    } catch (t: Throwable) {
        DataResult.Failure(mapAuthError(t))
    }

    override suspend fun signInWithGoogle(identityToken: String): DataResult<Unit> = try {
        client.auth.signInWith(IDToken) {
            idToken = identityToken
            provider = Google
        }
        syncProfileName()
        trackSignedIn("google")
        DataResult.Success(Unit)
    } catch (t: Throwable) {
        DataResult.Failure(mapAuthError(t))
    }

    /**
     * Keeps the profile's owner name in sync with the authenticated user, regardless of how they
     * signed in: prefers the display name in auth metadata (set at email sign-up, or provided by
     * Google/Apple), and falls back to the email's local part so it's never the placeholder.
     */
    override suspend fun syncProfileName() {
        runCatching {
            val user = client.auth.currentUserOrNull() ?: return
            val meta = user.userMetadata
            val name = listOf("full_name", "name")
                .firstNotNullOfOrNull { key -> meta?.get(key)?.jsonPrimitive?.contentOrNull }
                ?.takeIf { it.isNotBlank() }
                ?: user.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
                ?: return
            if (profileRepository.getProfile().ownerName != name) {
                profileRepository.setOwnerName(name)
            }
        }
    }

    override suspend fun signOut(): DataResult<Unit> = try {
        client.auth.signOut()
        clearSessionData()
        Track.event("sign_out")
        Track.user(null)
        DataResult.Success(Unit)
    } catch (t: Throwable) {
        DataResult.Failure(mapAuthError(t))
    }

    override suspend fun deleteAccount(): DataResult<Unit> = try {
        // Edge function verifies the JWT and deletes the auth user; DB cascades remove all data.
        client.functions.invoke("delete-account")
        Track.event("account_deleted")
        Track.user(null)
        // The account is gone; the JWT is now invalid, so sign out locally (best-effort) to flip
        // the session to unauthenticated and clear cached data.
        runCatching { client.auth.signOut() }
        clearSessionData()
        DataResult.Success(Unit)
    } catch (t: Throwable) {
        DataResult.Failure(mapAuthError(t))
    }

    /** Associates the crash/analytics session with the signed-in user and logs the entry event. */
    private fun trackSignedIn(method: String, isNew: Boolean = false) {
        Track.user(client.auth.currentUserOrNull()?.id)
        Track.event(if (isNew) "sign_up" else "login", mapOf("method" to method))
    }

    private fun clearSessionData() {
        profileRepository.clearCache()
        inventoryRepository.clearCache()
        salesRepository.clearCache()
        subscriptionRepository.clearCache()
    }

    private fun mapAuthError(t: Throwable): AppError {
        val msg = t.message?.lowercase() ?: ""
        return when {
            "invalid login" in msg || "credential" in msg ->
                AppError.Unauthorized("Incorrect email or password.")
            "already registered" in msg || "already been registered" in msg || "user already" in msg ->
                AppError.Validation("email", "That email is already registered. Try signing in.")
            "rate limit" in msg || "too many" in msg ->
                // Supabase caps confirmation emails; surface a clear message instead of a crash.
                AppError.Unauthorized("Too many attempts right now. Please wait a minute and try again.")
            "otp" in msg || "token has expired" in msg || "invalid otp" in msg || "expired" in msg ->
                AppError.Unauthorized("That code is invalid or expired. Request a new one.")
            else -> {
                Track.error(t) // unexpected auth failure — capture for Crashlytics
                AppError.Unknown(t)
            }
        }
    }

}
