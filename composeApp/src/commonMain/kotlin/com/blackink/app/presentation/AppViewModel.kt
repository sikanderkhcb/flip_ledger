package com.blackink.app.presentation

import com.blackink.app.domain.repository.AuthRepository
import com.blackink.app.domain.repository.ProfileRepository
import com.blackink.app.domain.repository.SessionState
import com.blackink.app.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/** Where the app should open on launch, decided from the restored session + onboarding flag. */
enum class StartDestination { LOADING, AUTH, ONBOARDING, HOME }

data class AppUiState(val isDark: Boolean = false, val start: StartDestination = StartDestination.LOADING)

/** Root-level state: theme + the launch destination (skips login/onboarding for returning users). */
class AppViewModel(
    themeRepository: ThemeRepository,
    authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
) : BaseViewModel() {
    private val _state = MutableStateFlow(AppUiState())
    val state = _state.asStateFlow()

    init {
        themeRepository.isDark.onEach { _state.value = _state.value.copy(isDark = it) }.launchIn(scope)

        // A persisted Supabase session means the user is already logged in — skip straight to
        // the dashboard (or to onboarding if they never finished it). Otherwise start at auth.
        authRepository.sessionState.onEach { session ->
            val start = when (session) {
                SessionState.LOADING -> StartDestination.LOADING
                SessionState.UNAUTHENTICATED -> StartDestination.AUTH
                SessionState.AUTHENTICATED -> {
                    // Keep the greeting/name correct even for a restored session (which never
                    // goes through the sign-in path).
                    runCatching { authRepository.syncProfileName() }
                    // On a transient failure, don't force a returning user back through onboarding.
                    val onboarded = runCatching { profileRepository.isOnboarded() }.getOrDefault(true)
                    if (onboarded) StartDestination.HOME else StartDestination.ONBOARDING
                }
            }
            _state.value = _state.value.copy(start = start)
        }.launchIn(scope)
    }
}
