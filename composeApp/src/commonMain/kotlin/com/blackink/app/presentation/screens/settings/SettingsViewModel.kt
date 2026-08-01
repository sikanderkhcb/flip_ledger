package com.blackink.app.presentation.screens.settings

import com.blackink.app.core.onFailure
import com.blackink.app.core.onSuccess
import com.blackink.app.domain.model.BusinessProfile
import com.blackink.app.domain.repository.AuthRepository
import com.blackink.app.domain.repository.ProfileRepository
import com.blackink.app.domain.repository.ThemeRepository
import com.blackink.app.presentation.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class SettingsUiState(
    val profile: BusinessProfile = BusinessProfile(),
    val isDark: Boolean = false,
    val signedOut: Boolean = false,
    val deleting: Boolean = false,
    val error: String? = null,
)

class SettingsViewModel(
    profileRepository: ProfileRepository,
    private val themeRepository: ThemeRepository,
    private val authRepository: AuthRepository,
) : BaseViewModel() {
    private val _state = MutableStateFlow(SettingsUiState())
    val state = _state.asStateFlow()

    init {
        combine(profileRepository.observeProfile(), themeRepository.isDark) { profile, dark ->
            SettingsUiState(
                profile = profile,
                isDark = dark,
                signedOut = _state.value.signedOut,
                deleting = _state.value.deleting,
                error = _state.value.error,
            )
        }.onEach { _state.value = it }.launchIn(scope)
    }

    fun toggleTheme() = scope.launch { themeRepository.setDark(!_state.value.isDark) }
    fun signOut() = scope.launch {
        _state.value = _state.value.copy(error = null)
        authRepository.signOut()
            .onSuccess { _state.value = _state.value.copy(signedOut = true) }
            .onFailure { _state.value = _state.value.copy(error = it.userMessage()) }
    }

    fun deleteAccount() = scope.launch {
        _state.value = _state.value.copy(deleting = true, error = null)
        authRepository.deleteAccount()
            .onSuccess { _state.value = _state.value.copy(deleting = false, signedOut = true) }
            .onFailure { _state.value = _state.value.copy(deleting = false, error = it.userMessage()) }
    }
}
