package com.blackink.app.presentation.screens.auth

import androidx.compose.runtime.Composable

@Composable
actual fun rememberAppleSignInLauncher(
    onIdentityToken: (String) -> Unit,
    onError: (String) -> Unit,
): () -> Unit = { onError("Apple sign-in is currently available on iOS.") }
