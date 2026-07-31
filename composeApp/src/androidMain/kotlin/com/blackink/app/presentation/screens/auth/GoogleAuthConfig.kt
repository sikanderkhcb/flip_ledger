package com.blackink.app.presentation.screens.auth

/**
 * Google sign-in configuration.
 *
 * [WEB_CLIENT_ID] must be the **Web application** OAuth client ID from Google Cloud Console
 * (the same one added to Supabase → Auth → Providers → Google). Credential Manager uses it as
 * the server client id, and Supabase validates the returned ID token against it. It is a public
 * identifier (safe to ship in the app).
 */
object GoogleAuthConfig {
    const val WEB_CLIENT_ID = "284835645600-59p60q6ebvm7pkg6un9d1u8bsph9l78p.apps.googleusercontent.com"
}
