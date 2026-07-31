package com.blackink.app.android

import android.content.Context
import android.os.Bundle
import com.blackink.app.core.telemetry.Telemetry
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Android-side [Telemetry] backed by Firebase Analytics + Crashlytics. Firebase auto-initializes
 * via the google-services plugin, so we only grab the singletons here.
 */
class FirebaseTelemetry(context: Context) : Telemetry {
    private val analytics = FirebaseAnalytics.getInstance(context)
    private val crashlytics = FirebaseCrashlytics.getInstance()

    override fun logEvent(name: String, params: Map<String, String>) {
        val bundle = Bundle().apply {
            params.forEach { (key, value) -> putString(sanitize(key), value.take(100)) }
        }
        analytics.logEvent(sanitize(name), bundle)
    }

    override fun recordError(throwable: Throwable, fatal: Boolean) {
        // Crashlytics has one API for logged (non-fatal) exceptions; `fatal` is recorded as a key
        // so it is visible on the issue without changing the reporting path.
        if (fatal) crashlytics.setCustomKey("fatal", true)
        crashlytics.recordException(throwable)
    }

    override fun log(message: String) = crashlytics.log(message)

    override fun setUser(id: String?) {
        analytics.setUserId(id)
        crashlytics.setUserId(id.orEmpty())
    }

    /** Firebase names must be <=40 chars and contain only letters, digits, and underscores. */
    private fun sanitize(raw: String): String =
        raw.take(40).map { if (it.isLetterOrDigit() || it == '_') it else '_' }.joinToString("")
}
