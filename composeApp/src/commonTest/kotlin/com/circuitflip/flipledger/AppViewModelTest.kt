package com.circuitflip.flipledger

import app.cash.turbine.test
import com.circuitflip.flipledger.core.DataResult
import com.circuitflip.flipledger.domain.model.AuthDraft
import com.circuitflip.flipledger.domain.repository.AuthRepository
import com.circuitflip.flipledger.domain.repository.ThemeRepository
import com.circuitflip.flipledger.presentation.AppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** Fakes let us drive the ViewModel's inputs without any real DB/network. */
private class FakeThemeRepository(initial: Boolean = false) : ThemeRepository {
    val dark = MutableStateFlow(initial)
    override val isDark: Flow<Boolean> get() = dark
    override suspend fun setDark(dark: Boolean) { this.dark.value = dark }
}

private class FakeAuthRepository(initial: Boolean = false) : AuthRepository {
    val authed = MutableStateFlow(initial)
    override val isAuthenticated: Flow<Boolean> get() = authed
    override suspend fun signIn(email: String, password: String): DataResult<Unit> = DataResult.Success(Unit)
    override suspend fun signUp(draft: AuthDraft): DataResult<Unit> = DataResult.Success(Unit)
    override suspend fun signInWithApple(identityToken: String): DataResult<Unit> = DataResult.Success(Unit)
    override suspend fun signOut() { authed.value = false }
}

class AppViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    @Test
    fun reflectsThemeAndAuthChanges() = runTest(dispatcher) {
        val theme = FakeThemeRepository(initial = false)
        val auth = FakeAuthRepository(initial = false)
        val vm = AppViewModel(theme, auth)

        vm.state.test {
            // Initial emission
            assertEquals(false, awaitItem().isDark)

            theme.dark.value = true
            assertEquals(true, awaitItem().isDark)

            auth.authed.value = true
            assertEquals(true, awaitItem().isAuthenticated)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
