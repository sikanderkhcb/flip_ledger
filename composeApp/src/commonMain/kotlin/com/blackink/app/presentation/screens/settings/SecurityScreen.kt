package com.blackink.app.presentation.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.blackink.app.presentation.components.FlipTextField
import com.blackink.app.presentation.components.FlipTopBar
import com.blackink.app.presentation.components.PrimaryButton
import com.blackink.app.presentation.components.ScreenScaffold
import com.blackink.app.presentation.components.UiErrorEffect
import com.blackink.app.presentation.rememberViewModel
import com.blackink.app.presentation.theme.FlipTheme

@Composable
fun SecurityScreen(onBack: () -> Unit) {
    val vm = rememberViewModel<SecurityViewModel>()
    val state by vm.state.collectAsState()
    val colors = FlipTheme.colors

    LaunchedEffect(state.saved) { if (state.saved) onBack() }

    ScreenScaffold {
        FlipTopBar(title = "Security", onBack = onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        ) {
            Text("Change password", style = FlipTheme.typography.displayM, color = colors.textDefault)
            Spacer(Modifier.height(8.dp))
            Text(
                "Choose a new password for your BlackInk account.",
                style = FlipTheme.typography.bodyL,
                color = colors.textWeaker,
            )
            Spacer(Modifier.height(28.dp))
            FlipTextField(
                state.password,
                vm::onPassword,
                "New password",
                placeholder = "At least 8 characters",
                keyboardType = KeyboardType.Password,
                isPassword = true,
                error = state.passwordError,
            )
            Spacer(Modifier.height(16.dp))
            FlipTextField(
                state.confirmation,
                vm::onConfirmation,
                "Confirm new password",
                placeholder = "Enter it again",
                keyboardType = KeyboardType.Password,
                isPassword = true,
                error = state.confirmationError,
            )
            UiErrorEffect(state.error)
        }
        Column(Modifier.padding(24.dp)) {
            PrimaryButton("Update password", vm::save, loading = state.loading)
        }
    }
}
