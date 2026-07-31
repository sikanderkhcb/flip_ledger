package com.blackink.app.domain.util

import com.blackink.app.domain.model.Currency
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Currency + percentage formatting. Mirrors the reference design's `money()` and `pct()`
 * helpers: whole-dollar display with thousands separators and a leading `-$` for losses.
 *
 * Internally the app stores cents; [format] rounds to the nearest dollar for display,
 * matching the source mockup, while keeping full precision in storage.
 */
object Money {

    /** Parse a user-typed dollar string (e.g. "560", "1,050.50") into cents. */
    fun parseToCents(input: String): Long {
        return parseToCentsOrNull(input) ?: 0L
    }

    /** Parses a monetary amount, returning null instead of silently treating malformed input as $0. */
    fun parseToCentsOrNull(input: String): Long? {
        val cleaned = input.trim().replace(",", "").removePrefix("$")
        if (!AMOUNT_PATTERN.matches(cleaned)) return null
        val parts = cleaned.split(".", limit = 2)
        val dollars = parts[0].toLongOrNull() ?: return null
        val fractional = parts.getOrNull(1)?.padEnd(2, '0')?.toLongOrNull() ?: 0L
        if (dollars > (Long.MAX_VALUE - fractional) / 100L) return null
        return dollars * 100L + fractional
    }

    fun dollarsToCents(dollars: Number): Long = (dollars.toDouble() * 100).roundToLong()

    /** Format cents for display, e.g. 105000 -> "$1,050", -4300 -> "-$43". */
    fun format(cents: Long, currency: Currency = Currency.USD): String {
        val whole = (cents / 100.0).roundToLong()
        val sign = if (whole < 0) "-" else ""
        return sign + currency.symbol + groupThousands(abs(whole))
    }

    /** Same as [format] but always prefixes a "+" for non-negative values (used for profit). */
    fun formatSigned(cents: Long, currency: Currency = Currency.USD): String {
        val whole = (cents / 100.0).roundToLong()
        return if (whole >= 0) "+" + format(cents, currency) else format(cents, currency)
    }

    private fun groupThousands(value: Long): String {
        val s = value.toString()
        val sb = StringBuilder()
        var count = 0
        for (i in s.indices.reversed()) {
            sb.append(s[i])
            count++
            if (count % 3 == 0 && i != 0) sb.append(',')
        }
        return sb.reverse().toString()
    }

    private val AMOUNT_PATTERN = Regex("""\d+(?:\.\d{1,2})?""")
}

/** Format a 0.0..1.0 fraction as a whole-number percent, e.g. 0.324 -> "32%". */
fun Double.toPercentLabel(): String = "${(this * 100).roundToLong()}%"

/** Format an integer percent, e.g. 18 -> "18%". */
fun Int.toPercentLabel(): String = "$this%"
