package com.blackink.app.presentation.screens.auth

import androidx.compose.runtime.Composable

@Composable
actual fun rememberGoogleSignInLauncher(
    onIdToken: (String) -> Unit,
    onError: (String) -> Unit,
): () -> Unit = {
    // iOS surfaces "Continue with Apple" instead, so this is never invoked there.
    onError("Google sign-in is not available on iOS.")
}
