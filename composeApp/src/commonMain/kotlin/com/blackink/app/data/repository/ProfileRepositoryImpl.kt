package com.blackink.app.data.repository

import com.blackink.app.core.AppError
import com.blackink.app.data.remote.dto.ProfileDto
import com.blackink.app.data.remote.dto.toDomain
import com.blackink.app.domain.model.BusinessProfile
import com.blackink.app.domain.repository.ProfileRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Business profile stored in the Supabase `profiles` table (one row per authenticated user,
 * created automatically by the `handle_new_user` trigger on signup). The observed flow
 * re-fetches whenever a new collector starts and after each [updateProfile].
 *
 * [updateProfile] uses a column-scoped update (not an upsert of the whole DTO) so it never
 * clobbers the server-managed `onboarded` flag, which is written separately by [setOnboarded].
 */
class ProfileRepositoryImpl(
    private val client: SupabaseClient,
    private val io: CoroutineDispatcher,
) : ProfileRepository {

    private val _profile = MutableStateFlow(BusinessProfile())
    private val _error = MutableStateFlow<AppError?>(null)
    override val error = _error
    private val scope = CoroutineScope(SupervisorJob() + io)
    private var cacheGeneration: Long = 0

    override fun observeProfile(): Flow<BusinessProfile> =
        _profile.onStart {
            scope.launch {
                val generation = cacheGeneration
                try {
                    val profile = getProfile()
                    if (generation == cacheGeneration) _profile.value = profile
                    _error.value = null
                } catch (t: Throwable) {
                    _error.value = AppError.from(t)
                }
            }
        }

    override suspend fun getProfile(): BusinessProfile = withContext(io) {
        val dto = fetchDto() ?: return@withContext BusinessProfile()
        dto.toDomain()
    }

    override suspend fun updateProfile(profile: BusinessProfile) {
        withContext(io) {
            val generation = cacheGeneration
            val uid = client.auth.currentUserOrNull()?.id
                ?: throw IllegalStateException("Authentication required.")
            // Note: owner_name is intentionally NOT written here — it's owned by the auth sync
            // (see [setOwnerName]). Writing it from form defaults would clobber the real name.
            client.from("profiles").update({
                set("business_name", profile.businessName)
                set("workspace_type", profile.workspaceType.id)
                set("partner_name", profile.partnerName)
                set("split_you", profile.splitYou)
                set("currency", profile.currency.code)
                set("category_pref", profile.categoryPref)
            }) { filter { eq("id", uid) } }
            // Keep the real owner name that the auth sync already put in place.
            if (generation == cacheGeneration) {
                _profile.value = profile.copy(ownerName = _profile.value.ownerName)
            }
        }
    }

    override suspend fun setOwnerName(name: String) {
        withContext(io) {
            val generation = cacheGeneration
            val uid = client.auth.currentUserOrNull()?.id
                ?: throw IllegalStateException("Authentication required.")
            client.from("profiles").update({ set("owner_name", name) }) { filter { eq("id", uid) } }
            if (generation == cacheGeneration) {
                _profile.value = _profile.value.copy(ownerName = name)
            }
        }
    }

    override suspend fun isOnboarded(): Boolean = withContext(io) {
        fetchDto()?.onboarded ?: false
    }

    override suspend fun setOnboarded() {
        withContext(io) {
            val uid = client.auth.currentUserOrNull()?.id
                ?: throw IllegalStateException("Authentication required.")
            client.from("profiles").update({ set("onboarded", true) }) { filter { eq("id", uid) } }
        }
    }

    private suspend fun fetchDto(): ProfileDto? {
        val uid = client.auth.currentUserOrNull()?.id ?: return null
        return client.from("profiles").select { filter { eq("id", uid) } }.decodeSingleOrNull()
    }

    override fun clearCache() {
        cacheGeneration += 1
        _profile.value = BusinessProfile()
        _error.value = null
    }
}
