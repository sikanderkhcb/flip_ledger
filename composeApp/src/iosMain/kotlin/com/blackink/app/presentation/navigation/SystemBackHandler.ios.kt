package com.blackink.app.presentation.navigation

import androidx.compose.runtime.Composable

@Composable
actual fun SystemBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS has no system back button; on-screen back controls drive navigation.
}
