package com.blackink.app.presentation.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.blackink.app.presentation.components.FlipTextField
import com.blackink.app.presentation.components.FlipTopBar
import com.blackink.app.presentation.components.LinkButton
import com.blackink.app.presentation.components.PrimaryButton
import com.blackink.app.presentation.components.ScreenScaffold
import com.blackink.app.presentation.rememberViewModel
import com.blackink.app.presentation.theme.FlipTheme

/**
 * 03c · Email OTP verification. Shown after sign-up when email confirmation is required: the user
 * enters the 6-digit code emailed to them. A valid code establishes the session, and the app's
 * session-state effect routes onward to onboarding/dashboard.
 */
@Composable
fun VerifyOtpScreen(email: String, onBack: () -> Unit, onVerified: () -> Unit) {
    val vm = rememberViewModel<VerifyOtpViewModel>()
    val state by vm.state.collectAsState()

    LaunchedEffect(state.success) { if (state.success) { vm.consumeSuccess(); onVerified() } }

    ScreenScaffold {
        FlipTopBar(title = "", onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Text(
                "Verify your email",
                style = FlipTheme.typography.displayM,
                color = FlipTheme.colors.textDefault,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Enter the 6-digit code we sent to $email.",
                style = FlipTheme.typography.bodyL,
                color = FlipTheme.colors.textWeaker,
            )
            Spacer(Modifier.height(24.dp))
            FlipTextField(
                state.code,
                vm::onCode,
                "Verification code",
                placeholder = "123456",
                keyboardType = KeyboardType.Number,
                error = state.error,
            )
            state.info?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, style = FlipTheme.typography.bodyS, color = FlipTheme.colors.primary)
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Didn't get the code?",
                    style = FlipTheme.typography.bodyM,
                    color = FlipTheme.colors.textWeaker,
                )
                LinkButton("Resend", onClick = { vm.resend(email) })
            }
        }
        Column(Modifier.padding(24.dp)) {
            PrimaryButton("Verify", onClick = { vm.verify(email) }, loading = state.loading)
        }
    }
}
