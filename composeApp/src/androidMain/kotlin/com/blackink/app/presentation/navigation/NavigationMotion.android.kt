package com.blackink.app.presentation.navigation

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

internal actual val navigationPlatform: NavigationPlatform = NavigationPlatform.ANDROID

@Composable
internal actual fun prefersReducedMotion(): Boolean {
    val context = LocalContext.current
    return Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    ) == 0f
}
