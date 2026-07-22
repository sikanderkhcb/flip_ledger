package com.circuitflip.flipledger.presentation.navigation

/**
 * Every destination in FlipLedger. Sealed so navigation is exhaustive and type-safe.
 * Routes carrying arguments (device id) are data classes; the rest are objects.
 *
 * Screen numbering matches the reference design (01 Splash … 24 Settings).
 */
sealed interface Route {
    data object Splash : Route              // 01
    data object Welcome : Route             // 02
    data class Auth(val signUp: Boolean = false) : Route  // 03 / 03b
    data object Setup1 : Route              // 04
    data object Setup2 : Route              // 05
    data object Setup3 : Route              // 06
    data object Dashboard : Route           // 07
    data object Inventory : Route           // 08
    data object AddDevice1 : Route          // 09
    data object AddDevice2 : Route          // 10
    data object AddDevice3 : Route          // 11
    data object AddDevice4 : Route          // 12
    data object DeviceAdded : Route         // 13
    data class DeviceDetail(val deviceId: String) : Route  // 14
    data object AddCost : Route             // 15
    data object Sale1 : Route               // 16
    data object Sale2 : Route               // 17
    data object Sale3 : Route               // 18
    data object SaleComplete : Route        // 19
    data object SalesHistory : Route        // 20
    data object Settlement : Route          // 21
    data object Reports : Route             // 22
    data object Subscription : Route        // 23
    data object Settings : Route            // 24
}
