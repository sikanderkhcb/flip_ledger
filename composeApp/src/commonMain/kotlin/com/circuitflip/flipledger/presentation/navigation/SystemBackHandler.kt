package com.circuitflip.flipledger.presentation.navigation

import androidx.compose.runtime.Composable

/**
 * Routes the platform's system back gesture/button into the in-app [Navigator].
 *
 * On Android this intercepts the hardware/gesture back so it pops the [Navigator]'s
 * stack instead of finishing the Activity. When [enabled] is false (i.e. we're at a
 * root screen with nothing to pop) the platform default applies and back leaves the app.
 * iOS has no system back button, so its actual is a no-op.
 */
@Composable
expect fun SystemBackHandler(enabled: Boolean, onBack: () -> Unit)
