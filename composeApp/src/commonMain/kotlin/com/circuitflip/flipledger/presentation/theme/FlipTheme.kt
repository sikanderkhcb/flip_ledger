package com.circuitflip.flipledger.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle

/** Access the active FlipLedger color set anywhere in the tree. */
val LocalFlipColors: ProvidableCompositionLocal<FlipColors> =
    staticCompositionLocalOf { LoftColors }

/** Access the active typography scale. */
val LocalFlipTypography: ProvidableCompositionLocal<FlipTypography> =
    staticCompositionLocalOf { flipTypography() }

/**
 * Short accessor object, so screens can write `FlipTheme.colors.primary` /
 * `FlipTheme.typography.headingM` — matching the ergonomics of MaterialTheme.
 */
object FlipTheme {
    val colors: FlipColors
        @Composable @ReadOnlyComposable get() = LocalFlipColors.current
    val typography: FlipTypography
        @Composable @ReadOnlyComposable get() = LocalFlipTypography.current
}

/**
 * Root theme wrapper. Provides both the custom FlipLedger tokens and a Material3
 * ColorScheme (so Material components — ripples, text selection — inherit brand colors).
 */
@Composable
fun FlipLedgerTheme(
    darkTheme: Boolean = false,
    typography: FlipTypography = flipTypography(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LoftColors

    val materialScheme = if (darkTheme) {
        darkColorScheme(
            primary = colors.primary,
            onPrimary = colors.textInverse,
            background = colors.backgroundSubtle,
            surface = colors.backgroundDefault,
            onSurface = colors.textDefault,
            error = colors.error,
        )
    } else {
        lightColorScheme(
            primary = colors.primary,
            onPrimary = colors.textInverse,
            background = colors.backgroundSubtle,
            surface = colors.backgroundDefault,
            onSurface = colors.textDefault,
            error = colors.error,
        )
    }

    CompositionLocalProvider(
        LocalFlipColors provides colors,
        LocalFlipTypography provides typography,
    ) {
        MaterialTheme(colorScheme = materialScheme) {
            content()
        }
    }
}
