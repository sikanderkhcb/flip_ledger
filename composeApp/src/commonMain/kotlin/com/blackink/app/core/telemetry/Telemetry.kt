package com.blackink.app.core.telemetry

/**
 * Platform-agnostic analytics + crash reporting. The concrete implementation is provided
 * natively per platform (Firebase Analytics + Crashlytics): Android installs it from
 * `BlackInkApplication`, iOS installs it from Swift after `FirebaseApp.configure()`.
 *
 * Shared code never touches Firebase directly — it goes through [Track].
 */
interface Telemetry {
    /** Product-analytics event. [name] and keys must be <=40 chars, letters/digits/underscore. */
    fun logEvent(name: String, params: Map<String, String> = emptyMap())

    /** Report a non-fatal (or fatal) error to Crashlytics. */
    fun recordError(throwable: Throwable, fatal: Boolean = false)

    /** Breadcrumb attached to the next crash report — useful for tracing what led to a crash. */
    fun log(message: String)

    /** Associate subsequent events and crashes with a user id (null clears it, e.g. on sign out). */
    fun setUser(id: String?)
}

/** Default until a platform installs the real implementation; also used when Firebase is absent. */
object NoOpTelemetry : Telemetry {
    override fun logEvent(name: String, params: Map<String, String>) {}
    override fun recordError(throwable: Throwable, fatal: Boolean) {}
    override fun log(message: String) {}
    override fun setUser(id: String?) {}
}

/**
 * Mutable holder the platform installs at startup. A holder (rather than DI) keeps this
 * callable from both Kotlin and Swift, and lets iOS wire its Firebase-backed impl in from
 * the Swift side after Koin has already started.
 */
object TelemetryProvider {
    var instance: Telemetry = NoOpTelemetry
}

/** Convenience entry point used throughout shared code so calls stay one line. */
object Track {
    fun event(name: String, params: Map<String, String> = emptyMap()) =
        TelemetryProvider.instance.logEvent(name, params)

    fun error(t: Throwable, fatal: Boolean = false) =
        TelemetryProvider.instance.recordError(t, fatal)

    fun breadcrumb(message: String) = TelemetryProvider.instance.log(message)

    fun user(id: String?) = TelemetryProvider.instance.setUser(id)
}
