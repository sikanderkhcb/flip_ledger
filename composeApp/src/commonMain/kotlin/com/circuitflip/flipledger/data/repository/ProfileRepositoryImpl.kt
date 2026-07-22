package com.circuitflip.flipledger.data.repository

import com.circuitflip.flipledger.data.remote.dto.ProfileDto
import com.circuitflip.flipledger.data.remote.dto.toDomain
import com.circuitflip.flipledger.domain.model.BusinessProfile
import com.circuitflip.flipledger.domain.repository.ProfileRepository
import com.russhwolf.settings.Settings
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
    private val settings: Settings,
    private val io: CoroutineDispatcher,
) : ProfileRepository {

    // Seed the owner name from the last cached value so returning users see it instantly on
    // launch (no placeholder/flash) while the network refresh happens in the background.
    private val _profile = MutableStateFlow(
        BusinessProfile(ownerName = settings.getStringOrNull(K_OWNER_NAME) ?: ""),
    )
    private val scope = CoroutineScope(SupervisorJob() + io)

    override fun observeProfile(): Flow<BusinessProfile> =
        _profile.onStart {
            scope.launch { runCatching { getProfile().also { _profile.value = it; cacheOwner(it.ownerName) } } }
        }

    override suspend fun getProfile(): BusinessProfile = withContext(io) {
        val dto = fetchDto() ?: return@withContext BusinessProfile()
        dto.toDomain()
    }

    override suspend fun updateProfile(profile: BusinessProfile) {
        withContext(io) {
            val uid = client.auth.currentUserOrNull()?.id ?: return@withContext
            // Note: owner_name is intentionally NOT written here — it's owned by the auth sync
            // (see [setOwnerName]). Writing it from form defaults would clobber the real name.
            client.from("profiles").update({
                set("business_name", profile.businessName)
                set("workspace_type", profile.workspaceType.id)
                set("split_you", profile.splitYou)
                set("currency", profile.currency.code)
                set("category_pref", profile.categoryPref)
            }) { filter { eq("id", uid) } }
            // Keep the real owner name that the auth sync already put in place.
            _profile.value = profile.copy(ownerName = _profile.value.ownerName)
        }
    }

    override suspend fun setOwnerName(name: String) {
        withContext(io) {
            val uid = client.auth.currentUserOrNull()?.id ?: return@withContext
            client.from("profiles").update({ set("owner_name", name) }) { filter { eq("id", uid) } }
            _profile.value = _profile.value.copy(ownerName = name)
            cacheOwner(name)
        }
    }

    override suspend fun isOnboarded(): Boolean = withContext(io) {
        fetchDto()?.onboarded ?: false
    }

    override suspend fun setOnboarded() {
        withContext(io) {
            val uid = client.auth.currentUserOrNull()?.id ?: return@withContext
            client.from("profiles").update({ set("onboarded", true) }) { filter { eq("id", uid) } }
        }
    }

    private suspend fun fetchDto(): ProfileDto? {
        val uid = client.auth.currentUserOrNull()?.id ?: return null
        return client.from("profiles").select { filter { eq("id", uid) } }.decodeSingleOrNull()
    }

    /** Persists the owner name locally so it can be shown instantly on the next launch. */
    private fun cacheOwner(name: String) {
        if (name.isNotBlank()) settings.putString(K_OWNER_NAME, name)
    }

    private companion object {
        const val K_OWNER_NAME = "profile.ownerName.cache"
    }
}
