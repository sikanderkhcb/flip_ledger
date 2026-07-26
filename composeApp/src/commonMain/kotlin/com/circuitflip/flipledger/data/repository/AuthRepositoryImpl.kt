package com.circuitflip.flipledger.data.repository

import com.circuitflip.flipledger.core.AppError
import com.circuitflip.flipledger.core.DataResult
import com.circuitflip.flipledger.domain.model.AuthDraft
import com.circuitflip.flipledger.domain.repository.AuthRepository
import com.circuitflip.flipledger.domain.repository.InventoryRepository
import com.circuitflip.flipledger.domain.repository.ProfileRepository
import com.circuitflip.flipledger.domain.repository.SalesRepository
import com.circuitflip.flipledger.domain.repository.SessionState
import com.circuitflip.flipledger.domain.repository.SubscriptionRepository
import com.circuitflip.flipledger.domain.util.FormValidation
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Apple
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
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
            DataResult.Success(Unit)
        } catch (t: Throwable) {
            DataResult.Failure(mapAuthError(t))
        }
    }

    override suspend fun signUp(draft: AuthDraft): DataResult<Unit> {
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
                return DataResult.Failure(
                    AppError.Unauthorized(
                        "Account created. Check your email to confirm it before signing in.",
                    ),
                )
            }
            // Set the owner name from the entered name (via the dedicated owner-name path so it
            // isn't later clobbered by the Setup/Settings form), and the business name if given.
            syncProfileName()
            if (businessName.isNotBlank()) {
                profileRepository.updateProfile(
                    profileRepository.getProfile().copy(businessName = businessName),
                )
            }
            DataResult.Success(Unit)
        } catch (t: Throwable) {
            DataResult.Failure(mapAuthError(t))
        }
    }

    override suspend fun signInWithApple(identityToken: String): DataResult<Unit> = try {
        client.auth.signInWith(IDToken) {
            idToken = identityToken
            provider = Apple
        }
        syncProfileName()
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
        DataResult.Success(Unit)
    } catch (t: Throwable) {
        DataResult.Failure(mapAuthError(t))
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
            else -> AppError.Unknown(t)
        }
    }

}
