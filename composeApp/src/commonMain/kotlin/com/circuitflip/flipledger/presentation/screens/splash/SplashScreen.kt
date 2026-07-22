package com.circuitflip.flipledger.presentation.screens.splash

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.circuitflip.flipledger.presentation.components.ScreenScaffold
import com.circuitflip.flipledger.presentation.theme.FlipTheme

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
                Box(
                    Modifier.size(76.dp).clip(RoundedCornerShape(22.dp)).background(FlipTheme.colors.primary),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Bolt, contentDescription = null, tint = FlipTheme.colors.textInverse, modifier = Modifier.size(38.dp)) }
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
