package com.circuitflip.flipledger.core

import com.circuitflip.flipledger.domain.model.FREE_DEVICE_LIMIT
import kotlinx.io.IOException

/**
 * Domain-level error taxonomy. The presentation layer maps these to user-facing copy;
 * see [userMessage].
 */
sealed class AppError(open val cause: Throwable? = null) {
    data class Network(override val cause: Throwable? = null) : AppError(cause)
    data class Unauthorized(val reason: String? = null) : AppError()
    data class NotFound(val what: String) : AppError()
    data class Validation(val field: String, val message: String) : AppError()
    data object SubscriptionRequired : AppError()
    data class Storage(override val cause: Throwable? = null) : AppError(cause)
    data class Unknown(override val cause: Throwable? = null) : AppError(cause)

    /** Human-friendly message safe to surface directly in the UI. */
    fun userMessage(): String = when (this) {
        is Network -> "No internet connection. Check your connection and try again."
        is Unauthorized -> reason ?: "Your session expired. Please sign in again."
        is NotFound -> "We couldn't find that $what."
        is Validation -> message
        SubscriptionRequired -> "You've used all $FREE_DEVICE_LIMIT free device slots. Upgrade to add more."
        is Storage -> "Something went wrong saving your data. Please try again."
        is Unknown -> "Something went wrong. Please try again."
    }

    companion object {
        fun from(t: Throwable): AppError = when (t) {
            is IOException -> Network(t)
            else -> {
                if (t.message?.contains("FREE_DEVICE_LIMIT_REACHED", ignoreCase = true) == true) {
                    SubscriptionRequired
                } else {
                    Unknown(t)
                }
            }
        }
    }
}
