package com.circuitflip.flipledger.presentation.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class NavigationDirection {
    FORWARD,
    BACKWARD,
    REPLACE,
}

/**
 * Minimal stack-based navigator, deliberately modeled on the reference design's own
 * `goTo(screen)` / `back()` behavior. Held as a single instance and observed by [AppNavHost].
 *
 * - [push] navigates forward, remembering the previous screen.
 * - [replace] swaps the top without growing the stack (used after auth/setup completion).
 * - [popTo] unwinds to a specific route (used by "Go to dashboard" success actions).
 */
class Navigator(start: Route = Route.Splash) {
    private val backStack = mutableStateListOf(start)

    var current by mutableStateOf(start)
        private set

    var direction by mutableStateOf(NavigationDirection.REPLACE)
        private set

    val canGoBack: Boolean get() = backStack.size > 1

    fun push(route: Route) {
        if (route == current) return
        backStack.add(route)
        direction = NavigationDirection.FORWARD
        current = route
    }

    fun replace(route: Route) {
        if (route == current) return
        if (backStack.isNotEmpty()) backStack[backStack.lastIndex] = route
        direction = NavigationDirection.REPLACE
        current = route
    }

    fun back() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
            direction = NavigationDirection.BACKWARD
            current = backStack.last()
        }
    }

    fun popTo(route: Route, inclusive: Boolean = false) {
        val previous = current
        val index = backStack.indexOfLast { it::class == route::class }
        if (index >= 0) {
            val target = if (inclusive) index - 1 else index
            while (backStack.size - 1 > target && backStack.size > 1) {
                backStack.removeAt(backStack.lastIndex)
            }
        }
        val destination = backStack.last()
        if (destination != previous) {
            direction = NavigationDirection.BACKWARD
            current = destination
        }
    }

    /** Clears the whole stack and starts fresh (used on sign-out). */
    fun resetTo(route: Route) {
        val changed = current != route
        backStack.clear()
        backStack.add(route)
        if (changed) {
            direction = NavigationDirection.REPLACE
            current = route
        }
    }
}
