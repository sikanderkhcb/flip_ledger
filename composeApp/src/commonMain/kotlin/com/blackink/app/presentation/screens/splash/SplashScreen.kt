package com.blackink.app.presentation.screens.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import com.blackink.app.presentation.theme.FlipTheme
import kotlinx.coroutines.delay

private const val MINIMUM_SPLASH_DURATION_MS = 1_000L

// Fixed brand palette so the in-app splash matches the (fixed) system splash exactly, regardless
// of the app's light/dark theme — the two then read as one continuous splash.
private val SplashBackground = Color(0xFFF9F7F7)
private val SplashInk = Color(0xFF282829)
private val SplashMuted = Color(0xFF6B6B72)
private val SplashFaint = Color(0xFFB0B0B8)

/**
 * Branded startup screen: the BlackInk diamond mark, wordmark and tagline, shown while the
 * persisted session is restored. The mark is drawn from vector geometry so it stays crisp.
 */
@Composable
fun SplashScreen(
    isReady: Boolean,
    onFinished: () -> Unit,
) {
    var minimumDisplayElapsed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Start the visible-duration clock only after Compose can present this screen.
        withFrameNanos { }
        delay(MINIMUM_SPLASH_DURATION_MS)
        minimumDisplayElapsed = true
    }
    LaunchedEffect(isReady, minimumDisplayElapsed) {
        if (isReady && minimumDisplayElapsed) onFinished()
    }

    Box(Modifier.fillMaxSize().background(SplashBackground)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.align(Alignment.Center).padding(32.dp),
        ) {
            DiamondMark(Modifier.size(96.dp))
            Spacer(Modifier.height(24.dp))
            Text("BlackInk", style = FlipTheme.typography.displayM, color = SplashInk)
            Spacer(Modifier.height(8.dp))
            Text(
                "Inventory and profit, clearly tracked.",
                style = FlipTheme.typography.bodyM,
                color = SplashMuted,
                textAlign = TextAlign.Center,
            )
        }

        CircularProgressIndicator(
            color = SplashFaint,
            strokeWidth = 2.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                // Sit above the system navigation bar (the app draws edge-to-edge).
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 40.dp)
                .size(20.dp)
                .semantics { contentDescription = "Starting BlackInk" },
        )
    }
}

/** The BlackInk faceted diamond (dark on the light splash), filling [modifier]'s bounds. */
@Composable
private fun DiamondMark(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension / 108f
        fun x(v: Float) = v * s + (size.width - 108f * s) / 2f
        fun y(v: Float) = v * s + (size.height - 108f * s) / 2f

        val body = Path().apply {
            moveTo(x(30f), y(20f))
            lineTo(x(78f), y(20f))
            lineTo(x(94.8f), y(37.6f))
            lineTo(x(54f), y(88f))
            lineTo(x(13.2f), y(37.6f))
            close()
        }
        drawPath(body, color = SplashInk)

        val facets = Path().apply {
            moveTo(x(13.2f), y(37.6f)); lineTo(x(94.8f), y(37.6f))
            moveTo(x(30f), y(20f)); lineTo(x(44.4f), y(37.6f))
            moveTo(x(78f), y(20f)); lineTo(x(63.6f), y(37.6f))
            moveTo(x(44.4f), y(37.6f)); lineTo(x(54f), y(88f))
            moveTo(x(63.6f), y(37.6f)); lineTo(x(54f), y(88f))
        }
        drawPath(facets, color = Color.White.copy(alpha = 0.20f), style = Stroke(width = 1.2f * s))
    }
}
