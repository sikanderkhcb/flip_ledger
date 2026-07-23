package com.circuitflip.flipledger.presentation.screens.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.circuitflip.flipledger.presentation.components.ScreenScaffold
import com.circuitflip.flipledger.presentation.theme.FlipTheme
import flipledger.composeapp.generated.resources.Res
import flipledger.composeapp.generated.resources.flip_ledger_logo
import org.jetbrains.compose.resources.painterResource

/** 01 · Splash — logo, wordmark, tagline, "Tap to continue". */
@Composable
fun SplashScreen(onContinue: () -> Unit) {
    ScreenScaffold {
        Box(Modifier.fillMaxSize().clickable(onClick = onContinue), contentAlignment = Alignment.Center) {
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp),
            ) {
                Image(
                    painter = painterResource(Res.drawable.flip_ledger_logo),
                    contentDescription = "FlipLedger logo",
                    modifier = Modifier.size(96.dp),
                )
                Spacer(Modifier.height(20.dp))
                Text("FlipLedger", style = FlipTheme.typography.displayM, color = FlipTheme.colors.textDefault)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Know what you own. Know what you earned.",
                    style = FlipTheme.typography.bodyM,
                    color = FlipTheme.colors.textWeaker,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(28.dp))
                Text("Tap to continue", style = FlipTheme.typography.bodyS, color = FlipTheme.colors.textWeakest)
            }
        }
    }
}
