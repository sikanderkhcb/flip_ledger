package com.blackink.app.presentation.screens.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@Composable
actual fun rememberGoogleSignInLauncher(
    onIdToken: (String) -> Unit,
    onError: (String) -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return {
        scope.launch {
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setServerClientId(GoogleAuthConfig.WEB_CLIENT_ID)
                    // Show all Google accounts, not just previously-authorized ones.
                    .setFilterByAuthorizedAccounts(false)
                    .build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = CredentialManager.create(context).getCredential(context, request)
                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    onIdToken(googleCredential.idToken)
                } else {
                    onError("Unexpected credential type from Google.")
                }
            } catch (_: GetCredentialCancellationException) {
                // User dismissed the account chooser — not an error.
            } catch (t: Throwable) {
                onError(t.message ?: "Google sign-in failed.")
            }
        }
    }
}
