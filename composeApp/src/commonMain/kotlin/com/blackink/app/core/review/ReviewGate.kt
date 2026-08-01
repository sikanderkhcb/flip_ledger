package com.blackink.app.core.review

import com.russhwolf.settings.ObservableSettings

/**
 * Decides *when* to surface the native rating prompt. We only ask a user who's shown real
 * engagement — here, after they've added their 2nd device — and we ask at most once ever, so the
 * prompt never nags. State is persisted in [ObservableSettings] (durable across launches), which
 * is what makes "once ever" hold even after the app is killed.
 */
class ReviewGate(private val settings: ObservableSettings) {

    /** Call on each successful device add. Triggers the prompt exactly once, on the 2nd add. */
    fun onDeviceAdded() {
        if (settings.getBoolean(KEY_ASKED, false)) return

        val count = settings.getInt(KEY_ADD_COUNT, 0) + 1
        settings.putInt(KEY_ADD_COUNT, count)

        if (count >= PROMPT_AFTER_ADDS) {
            // Mark asked *before* requesting so a failure/quota-skip still won't re-prompt later.
            settings.putBoolean(KEY_ASKED, true)
            ReviewPrompterProvider.instance.requestReview()
        }
    }

    private companion object {
        const val KEY_ADD_COUNT = "review_device_add_count"
        const val KEY_ASKED = "review_prompt_asked"
        const val PROMPT_AFTER_ADDS = 2
    }
}
