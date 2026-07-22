package com.circuitflip.flipledger.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.circuitflip.flipledger.presentation.theme.FlipTheme

/**
 * Page container using the subtle "warm stone" canvas from the design. Applies safe-drawing
 * insets so content clears the status bar / home indicator on both platforms.
 */
@Composable
fun ScreenScaffold(
    modifier: Modifier = Modifier,
    applyInsets: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(Modifier.fillMaxSize().background(FlipTheme.colors.backgroundSubtle)) {
        Column(
            modifier = (if (applyInsets) modifier.windowInsetsPadding(WindowInsets.safeDrawing) else modifier)
                .fillMaxSize(),
            content = content,
        )
    }
}
