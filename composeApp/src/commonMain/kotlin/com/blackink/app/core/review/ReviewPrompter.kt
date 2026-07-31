package com.blackink.app.core.review

/**
 * Platform-agnostic entry point for the OS's native in-app rating prompt. The concrete
 * implementation is provided natively per platform: Android uses the Play In-App Review API
 * (`ReviewManager`), iOS uses StoreKit (`SKStoreReviewController`). Both show the store's own
 * rating overlay in place — neither sends the user out to the store listing.
 *
 * Shared code never touches these APIs directly — it goes through [ReviewGate], which decides
 * *when* to ask; this interface only knows *how* to ask.
 */
interface ReviewPrompter {
    /**
     * Asks the OS to present its rating prompt. The OS may silently ignore it (quota, already
     * rated, etc.) — that's expected and by design; callers must not assume it was shown.
     */
    fun requestReview()
}

/** Default until a platform installs the real implementation (and on platforms without one). */
object NoOpReviewPrompter : ReviewPrompter {
    override fun requestReview() {}
}

/**
 * Mutable holder the platform installs at startup, mirroring TelemetryProvider / NotifierProvider.
 * A holder (rather than DI) keeps it callable from both Kotlin and Swift, and lets iOS wire its
 * StoreKit-backed impl in from the Swift side.
 */
object ReviewPrompterProvider {
    var instance: ReviewPrompter = NoOpReviewPrompter
}
