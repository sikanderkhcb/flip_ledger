package com.circuitflip.flipledger.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Lightweight multiplatform ViewModel base. Provides a [scope] tied to the main dispatcher
 * with a supervisor job so one failing coroutine doesn't cancel siblings. [clear] must be
 * called when the owning screen leaves composition (handled by AppNavHost).
 *
 * We intentionally avoid the AndroidX lifecycle-viewmodel dependency to keep the shared
 * module lean and fully platform-neutral; the contract is identical.
 */
abstract class BaseViewModel {
    protected val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    open fun clear() {
        scope.cancel()
    }
}
