package com.blackink.app.presentation.screens.auth

/** The social sign-in provider surfaced on the current platform. */
enum class SocialAuthProvider(val label: String) {
    GOOGLE("Continue with Google"),
    APPLE("Continue with Apple"),
}

/** Google on Android, Apple on iOS. */
/** Null when a platform's provider has not been configured and must not be advertised. */
expect val platformSocialAuthProvider: SocialAuthProvider?
