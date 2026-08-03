package com.blackink.app.presentation.screens.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import com.blackink.app.presentation.components.FlipTextField
import com.blackink.app.presentation.components.FlipTopBar
import com.blackink.app.presentation.components.LinkButton
import com.blackink.app.presentation.components.PrimaryButton
import com.blackink.app.presentation.components.ScreenScaffold
import com.blackink.app.presentation.components.UiErrorEffect
import com.blackink.app.presentation.rememberViewModel
import com.blackink.app.presentation.theme.FlipTheme

/**
 * 03 · Sign In and 03b · Sign Up. Sign Up asks only for account credentials; business
 * details are collected progressively during onboarding.
 */
@Composable
fun AuthScreen(
    signUp: Boolean,
    onBack: () -> Unit,
    onAuthenticated: () -> Unit,
    onNeedsVerification: (String) -> Unit,
    onForgotPassword: () -> Unit,
    onToggleMode: (Boolean) -> Unit,
) {
    val vm = rememberViewModel<AuthViewModel>(key = signUp)
    val state by vm.state.collectAsState()

    val googleSignIn = rememberGoogleSignInLauncher(
        onIdToken = { token -> vm.startLoading(); vm.signInWithGoogleToken(token) },
        onError = { message -> vm.onSocialError(message) },
    )
    val appleSignIn = rememberAppleSignInLauncher(
        onIdentityToken = { token -> vm.startLoading(); vm.signInWithAppleToken(token) },
        onError = { message -> vm.onSocialError(message) },
    )

    LaunchedEffect(signUp) { vm.setSignUp(signUp) }
    LaunchedEffect(state.success) { if (state.success) { vm.consumeSuccess(); onAuthenticated() } }
    LaunchedEffect(state.pendingOtpEmail) {
        state.pendingOtpEmail?.let { email -> vm.consumePendingOtp(); onNeedsVerification(email) }
    }

    ScreenScaffold {
        FlipTopBar(title = "", onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Text(
                if (signUp) "Create your account" else "Welcome back",
                style = FlipTheme.typography.displayM, color = FlipTheme.colors.textDefault,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (signUp) "Track up to 5 device records free. Upgrade to Solo for $10/month when you need it."
                else "Sign in to keep tracking your inventory and profit.",
                style = FlipTheme.typography.bodyL, color = FlipTheme.colors.textWeaker,
            )
            Spacer(Modifier.height(24.dp))

            if (signUp) {
                FlipTextField(
                    state.draft.name,
                    vm::onName,
                    "Full name",
                    placeholder = "Jordan Rivera",
                    error = state.fieldErrors["name"],
                )
                Spacer(Modifier.height(16.dp))
            }
            FlipTextField(
                state.draft.email,
                vm::onEmail,
                "Email",
                placeholder = "you@business.com",
                keyboardType = KeyboardType.Email,
                error = state.fieldErrors["email"],
            )
            Spacer(Modifier.height(16.dp))
            FlipTextField(
                state.draft.password,
                vm::onPassword,
                "Password",
                placeholder = "At least 8 characters",
                isPassword = true,
                error = state.fieldErrors["password"],
            )
            if (!signUp) {
                LinkButton("Forgot password?", onClick = onForgotPassword)
            }
            UiErrorEffect(state.error)
            Spacer(Modifier.height(16.dp))
        }
        Column(Modifier.padding(24.dp)) {
            PrimaryButton(if (signUp) "Create Free Account" else "Sign In", vm::submit, loading = state.loading)
            platformSocialAuthProvider?.let { socialProvider ->
                Spacer(Modifier.height(12.dp))
                val isApple = socialProvider == SocialAuthProvider.APPLE
                Button(
                    onClick = {
                        if (isApple) appleSignIn() else googleSignIn()
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isApple) Color.Black else Color.White,
                        contentColor = if (isApple) Color.White else FlipTheme.colors.textDefault,
                    ),
                    border = if (isApple) null else BorderStroke(1.dp, FlipTheme.colors.borderDefault),
                ) {
                    if (isApple) {
                        Text("", style = FlipTheme.typography.headingS, color = Color.White)
                    } else {
                        Icon(Icons.AutoMirrored.Rounded.Login, contentDescription = null, tint = FlipTheme.colors.textDefault)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(socialProvider.label, style = FlipTheme.typography.headingS)
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text(if (signUp) "Already have an account?" else "Don't have an account?", style = FlipTheme.typography.bodyM, color = FlipTheme.colors.textWeaker)
                LinkButton(if (signUp) "Sign in" else "Sign up", onClick = { onToggleMode(!signUp) })
            }
        }
    }
}
