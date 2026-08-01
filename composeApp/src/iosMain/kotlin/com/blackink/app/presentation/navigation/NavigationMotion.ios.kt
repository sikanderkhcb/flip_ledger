package com.blackink.app.presentation.navigation

import androidx.compose.runtime.Composable
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled

internal actual val navigationPlatform: NavigationPlatform = NavigationPlatform.IOS

@Composable
internal actual fun prefersReducedMotion(): Boolean =
    UIAccessibilityIsReduceMotionEnabled()
