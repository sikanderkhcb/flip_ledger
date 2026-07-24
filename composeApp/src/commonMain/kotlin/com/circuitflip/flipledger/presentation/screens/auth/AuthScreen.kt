package com.circuitflip.flipledger.presentation.screens.auth

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
import com.circuitflip.flipledger.presentation.components.FlipTextField
import com.circuitflip.flipledger.presentation.components.FlipTopBar
import com.circuitflip.flipledger.presentation.components.LinkButton
import com.circuitflip.flipledger.presentation.components.PrimaryButton
import com.circuitflip.flipledger.presentation.components.ScreenScaffold
import com.circuitflip.flipledger.presentation.rememberViewModel
import com.circuitflip.flipledger.presentation.theme.FlipTheme

/**
 * 03 · Sign In and 03b · Sign Up. Sign Up asks only for account credentials; business
 * details are collected progressively during onboarding.
 */
@Composable
fun AuthScreen(signUp: Boolean, onBack: () -> Unit, onAuthenticated: () -> Unit, onToggleMode: (Boolean) -> Unit) {
    val vm = rememberViewModel<AuthViewModel>(key = signUp)
    val state by vm.state.collectAsState()

    LaunchedEffect(signUp) { vm.setSignUp(signUp) }
    LaunchedEffect(state.success) { if (state.success) { vm.consumeSuccess(); onAuthenticated() } }

    ScreenScaffold {
        FlipTopBar(title = "", onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Text(
                if (signUp) "Create your account" else "Welcome back",
                style = FlipTheme.typography.displayM, color = FlipTheme.colors.textDefault,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (signUp) "Track up to 10 device records free. Upgrade to unlimited for $10/month when you need it."
                else "Sign in to keep tracking your inventory and profit.",
                style = FlipTheme.typography.bodyL, color = FlipTheme.colors.textWeaker,
            )
            Spacer(Modifier.height(24.dp))

            platformSocialAuthProvider?.let { socialProvider ->
                val googleSignIn = rememberGoogleSignInLauncher(
                    onIdToken = { token -> vm.startLoading(); vm.signInWithGoogleToken(token) },
                    onError = { message -> vm.onSocialError(message) },
                )
                OutlinedButton(
                    onClick = {
                        when (socialProvider) {
                            SocialAuthProvider.GOOGLE -> googleSignIn()
                            SocialAuthProvider.APPLE -> vm.onSocialError("Apple sign-in is not configured.")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, FlipTheme.colors.borderDefault),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Login, contentDescription = null, tint = FlipTheme.colors.textDefault)
                    Spacer(Modifier.width(8.dp))
                    Text(socialProvider.label, style = FlipTheme.typography.headingS, color = FlipTheme.colors.textDefault)
                }

                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HorizontalDivider(Modifier.weight(1f), color = FlipTheme.colors.borderDefault)
                    Text("OR", style = FlipTheme.typography.caption, color = FlipTheme.colors.textWeakest, modifier = Modifier.padding(horizontal = 12.dp))
                    HorizontalDivider(Modifier.weight(1f), color = FlipTheme.colors.borderDefault)
                }
                Spacer(Modifier.height(20.dp))
            }

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
            state.error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, style = FlipTheme.typography.bodyS, color = FlipTheme.colors.error)
            }
            Spacer(Modifier.height(16.dp))
        }
        Column(Modifier.padding(24.dp)) {
            PrimaryButton(if (signUp) "Create Free Account" else "Sign In", vm::submit, loading = state.loading)
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text(if (signUp) "Already have an account?" else "Don't have an account?", style = FlipTheme.typography.bodyM, color = FlipTheme.colors.textWeaker)
                LinkButton(if (signUp) "Sign in" else "Sign up", onClick = { onToggleMode(!signUp) })
            }
        }
    }
}
