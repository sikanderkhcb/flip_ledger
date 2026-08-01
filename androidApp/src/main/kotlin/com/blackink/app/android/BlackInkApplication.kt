package com.blackink.app.android

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.blackink.app.core.notifications.NoOpNotifier
import com.blackink.app.core.notifications.NotifierProvider
import com.blackink.app.core.review.ReviewPrompterProvider
import com.blackink.app.core.telemetry.NoOpTelemetry
import com.blackink.app.core.telemetry.TelemetryProvider
import com.blackink.app.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.logger.Level

/** Boots dependency injection once for the whole process, injecting the Android context. */
class BlackInkApplication : Application() {

    // The currently resumed activity, so the Play review flow (which needs an Activity) can launch.
    @Volatile
    private var currentActivity: Activity? = null

    override fun onCreate() {
        super.onCreate()
        // Install Firebase-backed telemetry before anything else can log. Guarded so a build
        // without google-services.json (Firebase not initialized) simply stays no-op.
        TelemetryProvider.instance = runCatching { FirebaseTelemetry(this) }.getOrDefault(NoOpTelemetry)
        // Local device notifications (e.g. the sign-up welcome) are posted through this facade.
        NotifierProvider.instance = runCatching { AndroidNotifier(this) }.getOrDefault(NoOpNotifier)
        // Native Play In-App Review prompt, driven by ReviewGate from shared code.
        trackCurrentActivity()
        ReviewPrompterProvider.instance = PlayReviewPrompter(this) { currentActivity }
        initKoin {
            androidLogger(Level.ERROR)
            androidContext(this@BlackInkApplication)
        }
    }

    private fun trackCurrentActivity() {
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) { currentActivity = activity }
            override fun onActivityPaused(activity: Activity) {
                if (currentActivity === activity) currentActivity = null
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}
