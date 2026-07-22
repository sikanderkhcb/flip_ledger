package com.circuitflip.flipledger.core

import io.github.aakira.napier.Napier

/**
 * Thin logging facade so the rest of the codebase never depends on a concrete logger.
 * Napier is initialized once in [com.circuitflip.flipledger.di.initKoin].
 */
object Log {
    fun d(tag: String, message: String) = Napier.d(message, tag = tag)
    fun i(tag: String, message: String) = Napier.i(message, tag = tag)
    fun w(tag: String, message: String, t: Throwable? = null) = Napier.w(message, t, tag = tag)
    fun e(tag: String, message: String, t: Throwable? = null) = Napier.e(message, t, tag = tag)
}
