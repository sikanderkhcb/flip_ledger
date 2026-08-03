package com.blackink.app.presentation.screens.auth

import com.blackink.app.core.AppError
import com.blackink.app.core.onFailure
import com.blackink.app.core.onSuccess
import com.blackink.app.domain.repository.AuthRepository
import com.blackink.app.presentation.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ForgotPasswordUiState(
    val email: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val fieldError: String? = null,
    val sent: Boolean = false,
)

class ForgotPasswordViewModel(private val authRepository: AuthRepository) : BaseViewModel() {
    private val _state = MutableStateFlow(ForgotPasswordUiState())
    val state = _state.asStateFlow()

    fun onEmail(value: String) = _state.update { it.copy(email = value, fieldError = null, error = null) }

    fun submit() {
        val email = _state.value.email.trim()
        if (!email.contains("@") || !email.substringAfterLast('@').contains('.')) {
            _state.update { it.copy(fieldError = "Enter a valid email address.", error = null) }
            return
        }
        _state.update { it.copy(loading = true, error = null, fieldError = null) }
        scope.launch {
            authRepository.sendPasswordReset(email)
                .onSuccess { _state.update { it.copy(loading = false, sent = true) } }
                .onFailure { error ->
                    _state.update { it.copy(loading = false, error = error.userMessage()) }
                }
        }
    }
}
