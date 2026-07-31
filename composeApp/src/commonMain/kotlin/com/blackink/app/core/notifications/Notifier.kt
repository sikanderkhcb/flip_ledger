package com.blackink.app.core.notifications

/**
 * Platform-agnostic device (system tray) notifications. The concrete implementation is provided
 * natively per platform: Android installs it from `BlackInkApplication` (posts via
 * NotificationManagerCompat), iOS installs it from Swift (posts via UNUserNotificationCenter).
 *
 * These are *local* notifications — fired from the app itself, no push server / FCM required.
 * Shared code never touches platform notification APIs directly — it goes through [Notify].
 */
interface Notifier {
    /**
     * Requests notification permission if needed, then posts a local notification to the device
     * tray. A no-op if the user has denied permission. Fire-and-forget: the platform handles
     * any async permission prompt and threading.
     */
    fun show(title: String, body: String)
}

/** Default until a platform installs the real implementation (and on platforms without one). */
object NoOpNotifier : Notifier {
    override fun show(title: String, body: String) {}
}

/**
 * Mutable holder the platform installs at startup. A holder (rather than DI) keeps this callable
 * from both Kotlin and Swift, and lets iOS wire its UNUserNotificationCenter-backed impl in from
 * the Swift side after Koin has already started.
 */
object NotifierProvider {
    var instance: Notifier = NoOpNotifier
}

/** Convenience entry point used throughout shared code so call sites stay one line. */
object Notify {
    /** Welcomes a newly registered user with a device notification. */
    fun welcome(userName: String?) {
        val name = userName?.trim()?.takeIf { it.isNotBlank() }
        NotifierProvider.instance.show(
            title = if (name != null) "Welcome, $name! 👋" else "Welcome to BlackInk 👋",
            body = "Your account is ready — add your first device to start tracking profit.",
        )
    }
}
