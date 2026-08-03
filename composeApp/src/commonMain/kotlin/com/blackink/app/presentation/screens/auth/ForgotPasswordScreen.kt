package com.blackink.app.presentation.screens.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.blackink.app.presentation.components.FlipTextField
import com.blackink.app.presentation.components.FlipTopBar
import com.blackink.app.presentation.components.LinkButton
import com.blackink.app.presentation.components.PrimaryButton
import com.blackink.app.presentation.components.ScreenScaffold
import com.blackink.app.presentation.components.UiErrorEffect
import com.blackink.app.presentation.rememberViewModel
import com.blackink.app.presentation.theme.FlipTheme

@Composable
fun ForgotPasswordScreen(onBack: () -> Unit) {
    val vm = rememberViewModel<ForgotPasswordViewModel>()
    val state by vm.state.collectAsState()
    val colors = FlipTheme.colors

    ScreenScaffold {
        FlipTopBar(title = "Forgot password", onBack = onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        ) {
            Text("Reset your password", style = FlipTheme.typography.displayM, color = colors.textDefault)
            Spacer(Modifier.height(8.dp))
            Text(
                "Enter your email and we'll send you a secure reset link.",
                style = FlipTheme.typography.bodyL,
                color = colors.textWeaker,
            )
            Spacer(Modifier.height(28.dp))
            FlipTextField(
                state.email,
                vm::onEmail,
                "Email",
                placeholder = "you@business.com",
                keyboardType = KeyboardType.Email,
                error = state.fieldError,
            )
            UiErrorEffect(state.error)
            if (state.sent) {
                Spacer(Modifier.height(20.dp))
                Text(
                    "Check your inbox for a password reset link. If you don't see it, check spam.",
                    style = FlipTheme.typography.bodyM,
                    color = colors.success,
                )
            }
        }
        Column(Modifier.padding(24.dp)) {
            PrimaryButton("Send reset link", vm::submit, loading = state.loading)
            Spacer(Modifier.height(8.dp))
            LinkButton("Back to sign in", onClick = onBack)
        }
    }
}
