package com.blackink.app.presentation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Small platform bridge for password-reset links opened by the OS. */
object AuthRecovery {
    private val _requested = MutableStateFlow<String?>(null)
    val requested = _requested.asStateFlow()

    fun markRequested(url: String) { _requested.value = url }
    fun consume() { _requested.value = null }
}

fun markPasswordResetRequested(url: String) = AuthRecovery.markRequested(url)
