package com.circuitflip.flipledger.presentation.screens.auth

/** The social sign-in provider surfaced on the current platform. */
enum class SocialAuthProvider(val label: String) {
    GOOGLE("Continue with Google"),
    APPLE("Continue with Apple"),
}

/** Google on Android, Apple on iOS. */
expect val platformSocialAuthProvider: SocialAuthProvider
