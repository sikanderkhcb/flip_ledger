package com.circuitflip.flipledger.presentation.screens.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.circuitflip.flipledger.presentation.components.ScreenScaffold
import com.circuitflip.flipledger.presentation.theme.FlipTheme
import flipledger.composeapp.generated.resources.Res
import flipledger.composeapp.generated.resources.flip_ledger_logo_transparent
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource

/** Branded startup state shown while the persisted session is restored. */
@Composable
fun SplashScreen(
    isReady: Boolean,
    onFinished: () -> Unit,
) {
    var minimumDisplayElapsed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(650)
        minimumDisplayElapsed = true
    }
    LaunchedEffect(isReady, minimumDisplayElapsed) {
        if (isReady && minimumDisplayElapsed) onFinished()
    }

    ScreenScaffold {
        Box(Modifier.fillMaxSize()) {
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
            ) {
                Image(
                    painter = painterResource(Res.drawable.flip_ledger_logo_transparent),
                    contentDescription = null,
                    modifier = Modifier.size(104.dp),
                )
                Spacer(Modifier.height(24.dp))
                Text("FlipLedger", style = FlipTheme.typography.displayM, color = FlipTheme.colors.textDefault)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Inventory and profit, clearly tracked.",
                    style = FlipTheme.typography.bodyM,
                    color = FlipTheme.colors.textWeaker,
                    textAlign = TextAlign.Center,
                )
            }

            CircularProgressIndicator(
                color = FlipTheme.colors.textWeakest,
                strokeWidth = 2.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 40.dp)
                    .size(20.dp)
                    .semantics { contentDescription = "Starting FlipLedger" },
            )
        }
    }
}
