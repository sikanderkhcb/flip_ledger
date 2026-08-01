package com.blackink.app.domain.util

import kotlin.random.Random

/** Generates long client-side identifiers with enough entropy to make collisions negligible. */
object Ids {
    fun new(prefix: String): String =
        prefix + buildString {
            repeat(2) {
                append(Random.nextLong().toString(16).removePrefix("-").padStart(16, '0'))
            }
        }
}
