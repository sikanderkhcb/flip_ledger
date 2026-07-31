package com.blackink.app.core.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App-wide top error banner state. Screens surface a *top-level* (non-field) error — a server
 * failure, "Something went wrong", "Incorrect password", etc. — by pushing it here; it renders in
 * a banner pinned to the top of the app (see `ErrorBannerHost`), over any screen and independent
 * of scroll position, so the user never has to scroll to discover an error.
 *
 * Dismissed by the banner's close button, on navigation, or when the screen clears its error.
 * Field-level validation (wrong email format, etc.) stays inline next to its input — only
 * top-level errors belong here.
 */
object UiBanner {
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    fun show(text: String) {
        _message.value = text
    }

    fun dismiss() {
        _message.value = null
    }
}
