package com.blackink.app

import com.blackink.app.core.AppError
import com.blackink.app.core.DataResult
import com.blackink.app.domain.model.AuthDraft
import com.blackink.app.domain.model.BusinessProfile
import com.blackink.app.domain.repository.AuthRepository
import com.blackink.app.domain.repository.ProfileRepository
import com.blackink.app.domain.repository.SessionState
import com.blackink.app.domain.repository.ThemeRepository
import com.blackink.app.presentation.AppViewModel
import com.blackink.app.presentation.StartDestination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

private class FakeThemeRepository(initial: Boolean = false) : ThemeRepository {
    val dark = MutableStateFlow(initial)
    override val isDark: Flow<Boolean> get() = dark
    override suspend fun setDark(dark: Boolean) { this.dark.value = dark }
}

private class FakeAuthRepository(initial: SessionState) : AuthRepository {
    val session = MutableStateFlow(initial)
    override val sessionState: Flow<SessionState> get() = session
    override val isAuthenticated: Flow<Boolean> =
        MutableStateFlow(initial == SessionState.AUTHENTICATED)

    override suspend fun signIn(email: String, password: String) = DataResult.Success(Unit)
    override suspend fun signUp(draft: AuthDraft) = DataResult.Success(Unit)
    override suspend fun signInWithApple(identityToken: String) = DataResult.Success(Unit)
    override suspend fun signInWithGoogle(identityToken: String) = DataResult.Success(Unit)
    override suspend fun signOut() = DataResult.Success(Unit)
    override suspend fun syncProfileName() = Unit
}

private class FakeProfileRepository(
    private val onboarded: Boolean,
) : ProfileRepository {
    private val profile = MutableStateFlow(BusinessProfile())
    override val error: StateFlow<AppError?> = MutableStateFlow(null)
    override fun observeProfile(): Flow<BusinessProfile> = profile
    override suspend fun getProfile(): BusinessProfile = profile.value
    override suspend fun updateProfile(profile: BusinessProfile) { this.profile.value = profile }
    override suspend fun setOwnerName(name: String) {
        profile.value = profile.value.copy(ownerName = name)
    }
    override suspend fun isOnboarded(): Boolean = onboarded
    override suspend fun setOnboarded() = Unit
    override fun clearCache() { profile.value = BusinessProfile() }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun routesUnauthenticatedSessionToAuthAndReflectsTheme() = runTest(dispatcher) {
        val theme = FakeThemeRepository()
        val auth = FakeAuthRepository(SessionState.UNAUTHENTICATED)
        val vm = AppViewModel(theme, auth, FakeProfileRepository(onboarded = true))

        advanceUntilIdle()
        assertEquals(StartDestination.AUTH, vm.state.value.start)

        theme.dark.value = true
        advanceUntilIdle()
        assertEquals(true, vm.state.value.isDark)
        vm.clear()
    }

    @Test
    fun routesAuthenticatedSessionByOnboardingState() = runTest(dispatcher) {
        val homeVm = AppViewModel(
            FakeThemeRepository(),
            FakeAuthRepository(SessionState.AUTHENTICATED),
            FakeProfileRepository(onboarded = true),
        )
        val onboardingVm = AppViewModel(
            FakeThemeRepository(),
            FakeAuthRepository(SessionState.AUTHENTICATED),
            FakeProfileRepository(onboarded = false),
        )

        advanceUntilIdle()
        assertEquals(StartDestination.HOME, homeVm.state.value.start)
        assertEquals(StartDestination.ONBOARDING, onboardingVm.state.value.start)
        homeVm.clear()
        onboardingVm.clear()
    }
}
