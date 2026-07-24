package com.circuitflip.flipledger.presentation.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntOffset

internal enum class NavigationPlatform {
    IOS,
    ANDROID,
}

internal expect val navigationPlatform: NavigationPlatform

@Composable
internal expect fun prefersReducedMotion(): Boolean

internal fun navigationTransform(
    direction: NavigationDirection,
    reduceMotion: Boolean = false,
): ContentTransform {
    if (reduceMotion) {
        return EnterTransition.None togetherWith ExitTransition.None
    }

    return when (navigationPlatform) {
        NavigationPlatform.IOS -> iosTransform(direction)
        NavigationPlatform.ANDROID -> androidTransform(direction)
    }
}

private fun iosTransform(direction: NavigationDirection): ContentTransform {
    val enterSpec = tween<IntOffset>(durationMillis = 320, easing = FastOutSlowInEasing)
    val exitSpec = tween<IntOffset>(durationMillis = 280, easing = FastOutSlowInEasing)

    return when (direction) {
        NavigationDirection.FORWARD -> {
            (
                slideInHorizontally(enterSpec) { width -> width } +
                    fadeIn(tween(durationMillis = 180, delayMillis = 40))
                ) togetherWith (
                slideOutHorizontally(exitSpec) { width -> -width / 4 } +
                    fadeOut(tween(durationMillis = 160))
                )
        }

        NavigationDirection.BACKWARD -> {
            (
                slideInHorizontally(exitSpec) { width -> -width / 4 } +
                    fadeIn(tween(durationMillis = 180))
                ) togetherWith (
                slideOutHorizontally(enterSpec) { width -> width } +
                    fadeOut(tween(durationMillis = 160, delayMillis = 40))
                )
        }

        NavigationDirection.REPLACE ->
            fadeIn(tween(durationMillis = 180)) togetherWith
                fadeOut(tween(durationMillis = 120))
    }
}

private fun androidTransform(direction: NavigationDirection): ContentTransform {
    val enterSpec = tween<IntOffset>(durationMillis = 240, easing = LinearOutSlowInEasing)
    val exitSpec = tween<IntOffset>(durationMillis = 180, easing = FastOutSlowInEasing)

    return when (direction) {
        NavigationDirection.FORWARD -> {
            (
                slideInHorizontally(enterSpec) { width -> width / 6 } +
                    fadeIn(tween(durationMillis = 210)) +
                    scaleIn(tween(durationMillis = 240), initialScale = 0.98f)
                ) togetherWith (
                slideOutHorizontally(exitSpec) { width -> -width / 12 } +
                    fadeOut(tween(durationMillis = 120))
                )
        }

        NavigationDirection.BACKWARD -> {
            (
                slideInHorizontally(enterSpec) { width -> -width / 12 } +
                    fadeIn(tween(durationMillis = 180))
                ) togetherWith (
                slideOutHorizontally(exitSpec) { width -> width / 6 } +
                    fadeOut(tween(durationMillis = 120)) +
                    scaleOut(tween(durationMillis = 180), targetScale = 0.98f)
                )
        }

        NavigationDirection.REPLACE ->
            (
                fadeIn(tween(durationMillis = 220, delayMillis = 60)) +
                    scaleIn(tween(durationMillis = 220), initialScale = 0.98f)
                ) togetherWith fadeOut(tween(durationMillis = 120))
    }
}
