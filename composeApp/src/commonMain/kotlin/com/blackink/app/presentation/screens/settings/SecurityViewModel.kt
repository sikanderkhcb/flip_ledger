package com.blackink.app.presentation.screens.settings

import com.blackink.app.core.onFailure
import com.blackink.app.core.onSuccess
import com.blackink.app.domain.repository.AuthRepository
import com.blackink.app.presentation.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SecurityUiState(
    val password: String = "",
    val confirmation: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val passwordError: String? = null,
    val confirmationError: String? = null,
    val saved: Boolean = false,
)

class SecurityViewModel(private val authRepository: AuthRepository) : BaseViewModel() {
    private val _state = MutableStateFlow(SecurityUiState())
    val state = _state.asStateFlow()

    fun onPassword(value: String) = _state.update { it.copy(password = value, passwordError = null, error = null) }
    fun onConfirmation(value: String) = _state.update { it.copy(confirmation = value, confirmationError = null, error = null) }

    fun save() {
        val current = _state.value
        val passwordError = when {
            current.password.length < 8 -> "Password must be at least 8 characters."
            current.password.length > 128 -> "Password must be 128 characters or fewer."
            current.password.none(Char::isLetter) -> "Password must include at least one letter."
            current.password.none(Char::isDigit) -> "Password must include at least one number."
            else -> null
        }
        val confirmationError = if (current.confirmation != current.password) "Passwords do not match." else null
        if (passwordError != null || confirmationError != null) {
            _state.update { it.copy(passwordError = passwordError, confirmationError = confirmationError) }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        scope.launch {
            authRepository.updatePassword(current.password)
                .onSuccess { _state.update { it.copy(loading = false, saved = true) } }
                .onFailure { error -> _state.update { it.copy(loading = false, error = error.userMessage()) } }
        }
    }
}
