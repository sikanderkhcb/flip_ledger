package com.blackink.app.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.blackink.app.core.ui.UiBanner
import com.blackink.app.presentation.theme.FlipTheme

/**
 * Feeds a screen's top-level error into the global [UiBanner]. Drop this anywhere in a screen
 * that has a top-level `state.error` — it's invisible; it just mirrors the error into the pinned
 * banner and clears it when the screen resolves the error. Field errors stay inline.
 */
@Composable
fun UiErrorEffect(error: String?) {
    LaunchedEffect(error) {
        if (error != null) UiBanner.show(error) else UiBanner.dismiss()
    }
}

/**
 * The pinned top error banner. Rendered once at the app root, above all screen content, so an
 * error is visible immediately regardless of where the user is scrolled. Stays until dismissed
 * (close button), on navigation, or when the originating screen clears its error.
 */
@Composable
fun ErrorBannerHost(modifier: Modifier = Modifier) {
    val message by UiBanner.message.collectAsState()
    // Keep the last text so it stays rendered through the slide-out animation.
    var lastMessage by remember { mutableStateOf("") }
    LaunchedEffect(message) { message?.let { lastMessage = it } }

    val colors = FlipTheme.colors
    AnimatedVisibility(
        visible = message != null,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(colors.error)
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                lastMessage,
                style = FlipTheme.typography.bodyM,
                color = colors.textInverse,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.size(12.dp))
            Text(
                "✕",
                style = FlipTheme.typography.headingS,
                color = colors.textInverse,
                modifier = Modifier.clickable { UiBanner.dismiss() },
            )
        }
    }
}
