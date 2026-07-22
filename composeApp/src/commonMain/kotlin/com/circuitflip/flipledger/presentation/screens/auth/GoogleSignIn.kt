package com.circuitflip.flipledger.presentation.screens.auth

import androidx.compose.runtime.Composable

/**
 * Returns a launcher that performs the platform's Google sign-in and calls [onIdToken] with
 * the resulting Google ID token (to be exchanged with Supabase), or [onError] on failure.
 *
 * Implemented on Android via Credential Manager; a no-op elsewhere (iOS uses Apple).
 */
@Composable
expect fun rememberGoogleSignInLauncher(
    onIdToken: (String) -> Unit,
    onError: (String) -> Unit,
): () -> Unit
