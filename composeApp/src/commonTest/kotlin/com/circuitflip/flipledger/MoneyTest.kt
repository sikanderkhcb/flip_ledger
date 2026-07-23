package com.circuitflip.flipledger

import com.circuitflip.flipledger.domain.util.Money
import com.circuitflip.flipledger.domain.util.toPercentLabel
import kotlin.test.Test
import kotlin.test.assertEquals

class MoneyTest {

    @Test
    fun parsesPlainDollars() {
        assertEquals(56000L, Money.parseToCents("560"))
    }

    @Test
    fun parsesWithCommasAndDollarSign() {
        assertEquals(105050L, Money.parseToCents("$1,050.50"))
    }

    @Test
    fun parsesGarbageAsZero() {
        assertEquals(0L, Money.parseToCents("abc"))
        assertEquals(0L, Money.parseToCents(""))
    }

    @Test
    fun rejectsMalformedOrOverPreciseAmounts() {
        assertEquals(null, Money.parseToCentsOrNull("1..2"))
        assertEquals(null, Money.parseToCentsOrNull("1.234"))
        assertEquals(123L, Money.parseToCentsOrNull("1.23"))
    }

    @Test
    fun formatsWholeDollarsWithThousands() {
        assertEquals("$1,050", Money.format(105000L))
        assertEquals("$0", Money.format(0L))
    }

    @Test
    fun formatsNegativeWithLeadingSign() {
        assertEquals("-$43", Money.format(-4300L))
    }

    @Test
    fun formatSignedPrefixesPlusForGains() {
        assertEquals("+$200", Money.formatSigned(20000L))
        assertEquals("-$5", Money.formatSigned(-500L))
    }

    @Test
    fun percentLabels() {
        assertEquals("32%", 0.324.toPercentLabel())
        assertEquals("18%", 18.toPercentLabel())
    }
}
