package com.blackink.app.presentation.screens.auth

import com.blackink.app.core.onFailure
import com.blackink.app.core.onSuccess
import com.blackink.app.domain.repository.AuthRepository
import com.blackink.app.presentation.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val OTP_LENGTH = 6

data class VerifyOtpUiState(
    val code: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val info: String? = null,
    val success: Boolean = false,
)

/** Backs 03c · email OTP verification. On a valid code, Supabase establishes the session. */
class VerifyOtpViewModel(private val authRepository: AuthRepository) : BaseViewModel() {

    private val _state = MutableStateFlow(VerifyOtpUiState())
    val state = _state.asStateFlow()

    /** Keeps only digits, capped at the OTP length. */
    fun onCode(value: String) {
        val digits = value.filter(Char::isDigit).take(OTP_LENGTH)
        _state.update { it.copy(code = digits, error = null) }
    }

    fun verify(email: String) {
        val code = _state.value.code
        if (code.length != OTP_LENGTH) {
            _state.update { it.copy(error = "Enter the $OTP_LENGTH-digit code from your email.") }
            return
        }
        _state.update { it.copy(loading = true, error = null, info = null) }
        scope.launch {
            authRepository.verifySignupOtp(email, code)
                .onSuccess { _state.update { it.copy(loading = false, success = true) } }
                .onFailure { err -> _state.update { it.copy(loading = false, error = err.userMessage()) } }
        }
    }

    fun resend(email: String) {
        _state.update { it.copy(loading = true, error = null, info = null) }
        scope.launch {
            authRepository.resendSignupOtp(email)
                .onSuccess {
                    _state.update { it.copy(loading = false, info = "We sent a new code to your email.") }
                }
                .onFailure { err -> _state.update { it.copy(loading = false, error = err.userMessage()) } }
        }
    }

    fun consumeSuccess() = _state.update { it.copy(success = false) }
}
