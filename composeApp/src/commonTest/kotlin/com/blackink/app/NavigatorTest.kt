package com.blackink.app

import com.blackink.app.presentation.navigation.NavigationDirection
import com.blackink.app.presentation.navigation.Navigator
import com.blackink.app.presentation.navigation.Route
import kotlin.test.Test
import kotlin.test.assertEquals

class NavigatorTest {

    @Test
    fun pushAndBackExposeOppositeMotionDirections() {
        val navigator = Navigator(Route.Dashboard)

        navigator.push(Route.Inventory)
        assertEquals(Route.Inventory, navigator.current)
        assertEquals(NavigationDirection.FORWARD, navigator.direction)

        navigator.back()
        assertEquals(Route.Dashboard, navigator.current)
        assertEquals(NavigationDirection.BACKWARD, navigator.direction)
    }

    @Test
    fun replacingOrSwitchingRootUsesNonDirectionalMotion() {
        val navigator = Navigator(Route.Welcome)

        navigator.replace(Route.Auth())
        assertEquals(NavigationDirection.REPLACE, navigator.direction)

        navigator.resetTo(Route.Dashboard)
        assertEquals(Route.Dashboard, navigator.current)
        assertEquals(NavigationDirection.REPLACE, navigator.direction)
    }
}
