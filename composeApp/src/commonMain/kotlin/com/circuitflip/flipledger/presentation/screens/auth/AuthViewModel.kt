package com.circuitflip.flipledger.presentation.screens.auth

import com.circuitflip.flipledger.core.AppError
import com.circuitflip.flipledger.core.onFailure
import com.circuitflip.flipledger.core.onSuccess
import com.circuitflip.flipledger.domain.model.AuthDraft
import com.circuitflip.flipledger.domain.repository.AuthRepository
import com.circuitflip.flipledger.domain.util.FormValidation
import com.circuitflip.flipledger.presentation.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val draft: AuthDraft = AuthDraft(),
    val isSignUp: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    val fieldErrors: Map<String, String> = emptyMap(),
    val success: Boolean = false,
)

/** Backs both Sign In (03) and Sign Up (03b) — the layout differs, the logic is shared. */
class AuthViewModel(private val authRepository: AuthRepository) : BaseViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state = _state.asStateFlow()

    fun setSignUp(signUp: Boolean) =
        _state.update { it.copy(isSignUp = signUp, error = null, fieldErrors = emptyMap()) }

    fun onName(v: String) = updateField("name") { it.copy(name = v) }
    fun onEmail(v: String) = updateField("email") { it.copy(email = v) }
    fun onPassword(v: String) = updateField("password") { it.copy(password = v) }

    fun submit() {
        val s = _state.value
        val fieldErrors = FormValidation.auth(s.draft, s.isSignUp)
        if (fieldErrors.isNotEmpty()) {
            _state.update { it.copy(error = null, fieldErrors = fieldErrors) }
            return
        }
        _state.update { it.copy(loading = true, error = null, fieldErrors = emptyMap()) }
        scope.launch {
            val result = if (s.isSignUp) authRepository.signUp(s.draft)
            else authRepository.signIn(s.draft.email, s.draft.password)
            result
                .onSuccess { _state.update { it.copy(loading = false, success = true) } }
                .onFailure(::showError)
        }
    }

    /** Exchanges a real Google ID token (from Credential Manager) for a Supabase session. */
    fun signInWithGoogleToken(idToken: String) {
        _state.update { it.copy(loading = true) }
        scope.launch {
            authRepository.signInWithGoogle(idToken)
                .onSuccess { _state.update { it.copy(loading = false, success = true) } }
                .onFailure { err -> _state.update { it.copy(loading = false, error = err.userMessage()) } }
        }
    }

    fun onSocialError(message: String) = _state.update { it.copy(loading = false, error = message) }

    fun startLoading() = _state.update { it.copy(loading = true, error = null) }

    fun consumeSuccess() = _state.update { it.copy(success = false) }

    private fun updateField(field: String, update: (AuthDraft) -> AuthDraft) {
        _state.update {
            it.copy(
                draft = update(it.draft),
                error = null,
                fieldErrors = it.fieldErrors - field,
            )
        }
    }

    private fun showError(error: AppError) {
        _state.update {
            if (error is AppError.Validation) {
                it.copy(
                    loading = false,
                    error = null,
                    fieldErrors = it.fieldErrors + (error.field to error.message),
                )
            } else {
                it.copy(loading = false, error = error.userMessage())
            }
        }
    }
}
