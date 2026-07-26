package com.circuitflip.flipledger.presentation.screens.welcome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.circuitflip.flipledger.presentation.components.LinkButton
import com.circuitflip.flipledger.presentation.components.PrimaryButton
import com.circuitflip.flipledger.presentation.components.ScreenScaffold
import com.circuitflip.flipledger.presentation.theme.FlipTheme

/** 02 · Welcome — value prop, feature bullets, Get Started / sign-in link. */
@Composable
fun WelcomeScreen(onGetStarted: () -> Unit, onHaveAccount: () -> Unit) {
    val bullets = listOf("Track every device", "Know your true profit", "See cash tied up in inventory")
    ScreenScaffold {
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(24.dp)) {
            Spacer(Modifier.height(40.dp))
            Text("Run your resale business with clarity.", style = FlipTheme.typography.displayM, color = FlipTheme.colors.textDefault)
            Spacer(Modifier.height(12.dp))
            Text(
                "FlipLedger tracks every device you buy, every dollar you spend, and exactly what you earn when it sells.",
                style = FlipTheme.typography.bodyL, color = FlipTheme.colors.textWeaker,
            )
            Spacer(Modifier.height(28.dp))
            bullets.forEach {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = FlipTheme.colors.success, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.size(12.dp))
                    Text(it, style = FlipTheme.typography.bodyL, color = FlipTheme.colors.textDefault)
                }
            }
        }
        Column(Modifier.padding(24.dp)) {
            Text(
                "5 device records free · No credit card required",
                style = FlipTheme.typography.bodyS,
                color = FlipTheme.colors.textWeaker,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            PrimaryButton("Start Free", onGetStarted)
            Spacer(Modifier.height(8.dp))
            LinkButton("I already have an account", onHaveAccount, Modifier.fillMaxWidth())
        }
    }
}
