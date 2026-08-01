package com.blackink.app.presentation.screens.auth

import androidx.compose.runtime.Composable

/** Starts native Apple Sign in and returns the Apple identity token for Supabase. */
@Composable
expect fun rememberAppleSignInLauncher(
    onIdentityToken: (String) -> Unit,
    onError: (String) -> Unit,
): () -> Unit
