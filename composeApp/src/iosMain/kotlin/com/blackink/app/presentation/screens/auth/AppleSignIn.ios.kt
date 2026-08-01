package com.blackink.app.presentation.screens.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AuthenticationServices.ASAuthorization
import platform.AuthenticationServices.ASAuthorizationAppleIDCredential
import platform.AuthenticationServices.ASAuthorizationAppleIDProvider
import platform.AuthenticationServices.ASAuthorizationController
import platform.AuthenticationServices.ASAuthorizationControllerDelegateProtocol
import platform.AuthenticationServices.ASAuthorizationControllerPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASAuthorizationErrorCanceled
import platform.AuthenticationServices.ASPresentationAnchor
import platform.Foundation.NSError
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow
import platform.darwin.NSObject
import platform.posix.memcpy

private var activeAppleDelegate: AppleAuthorizationDelegate? = null

@OptIn(ExperimentalForeignApi::class)
private class AppleAuthorizationDelegate(
    private val onToken: (String) -> Unit,
    private val onError: (String) -> Unit,
) : NSObject(), ASAuthorizationControllerDelegateProtocol,
    ASAuthorizationControllerPresentationContextProvidingProtocol {
    override fun presentationAnchorForAuthorizationController(controller: ASAuthorizationController): ASPresentationAnchor =
        UIApplication.sharedApplication.keyWindow
            ?: (UIApplication.sharedApplication.windows.firstOrNull() as? UIWindow)
            ?: error("Unable to present Apple sign-in.")

    override fun authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithAuthorization: ASAuthorization,
    ) {
        val credential = didCompleteWithAuthorization.credential as? ASAuthorizationAppleIDCredential
        val token = credential?.identityToken?.let { data ->
            data.bytes?.let { bytes ->
                memScoped {
                    val buffer = allocArray<ByteVar>(data.length.toInt() + 1)
                    memcpy(buffer, bytes, data.length)
                    buffer.toKString()
                }
            }
        }
        if (!token.isNullOrBlank()) onToken(token)
        else onError("Apple sign-in did not return an identity token.")
        activeAppleDelegate = null
    }

    override fun authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithError: NSError,
    ) {
        if (didCompleteWithError.code != ASAuthorizationErrorCanceled) {
            onError(didCompleteWithError.localizedDescription ?: "Apple sign-in failed.")
        }
        activeAppleDelegate = null
    }
}

@Composable
@OptIn(ExperimentalForeignApi::class)
actual fun rememberAppleSignInLauncher(
    onIdentityToken: (String) -> Unit,
    onError: (String) -> Unit,
): () -> Unit {
    val tokenCallback = remember(onIdentityToken) { onIdentityToken }
    val errorCallback = remember(onError) { onError }
    return remember(tokenCallback, errorCallback) {
        {
            val request = ASAuthorizationAppleIDProvider().createRequest().apply {
                requestedScopes = listOf(
                    platform.AuthenticationServices.ASAuthorizationScopeFullName,
                    platform.AuthenticationServices.ASAuthorizationScopeEmail,
                )
            }
            val delegate = AppleAuthorizationDelegate(tokenCallback, errorCallback)
            activeAppleDelegate = delegate
            ASAuthorizationController(listOf(request)).apply {
                setDelegate(delegate)
                setPresentationContextProvider(delegate)
                performRequests()
            }
        }
    }
}
