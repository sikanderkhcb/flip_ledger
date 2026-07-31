package com.blackink.app.android

import android.app.Activity
import android.content.Context
import com.blackink.app.core.review.ReviewPrompter
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Android-side [ReviewPrompter] backed by the Play In-App Review API. The prompt is an overlay the
 * Play Store shows in place (no navigation away); the API decides whether to actually show it
 * (per-user quotas), so a no-show is normal and not an error.
 *
 * `launchReviewFlow` needs the foreground [Activity], which the app-level provider doesn't hold —
 * so [currentActivity] supplies whichever activity is currently resumed (tracked by
 * BlackInkApplication). If none is resumed, we simply skip.
 */
class PlayReviewPrompter(
    context: Context,
    private val currentActivity: () -> Activity?,
) : ReviewPrompter {

    private val manager = ReviewManagerFactory.create(context.applicationContext)

    override fun requestReview() {
        val activity = currentActivity() ?: return
        manager.requestReviewFlow().addOnCompleteListener { request ->
            if (request.isSuccessful) {
                // Ignore the launch result: whether shown or quota-skipped, there's nothing to do.
                manager.launchReviewFlow(activity, request.result)
            }
        }
    }
}
