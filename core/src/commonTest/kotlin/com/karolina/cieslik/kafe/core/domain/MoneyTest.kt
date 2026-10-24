package com.karolina.cieslik.kafe.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Formatowanie i arytmetyka Money (cena w formacie „12,00 zł”). */
class MoneyTest {

    @Test
    fun zeroIsFormattedWithTwoDecimals() {
        assertEquals("0,00 zł", Money.ZERO.format())
    }

    @Test
    fun groszeBelowOneZlotyArePaddedToTwoDigits() {
        assertEquals("0,05 zł", Money(5).format())
        assertEquals("0,50 zł", Money(50).format())
    }

    @Test
    fun wholeZlotyAmountIsFormattedWithZeroGrosze() {
        assertEquals("12,00 zł", Money(1_200).format())
    }

    @Test
    fun thousandsAreSeparatedWithPlainSpace() {
        assertEquals("1 234,50 zł", Money(123_450).format())
        assertEquals("1 000,00 zł", Money(100_000).format())
        assertEquals("1 234 567,89 zł", Money(123_456_789).format())
    }

    @Test
    fun amountBelowThousandHasNoSeparator() {
        assertEquals("999,99 zł", Money(99_999).format())
    }

    @Test
    fun negativeAmountHasLeadingMinus() {
        assertEquals("-5,00 zł", Money(-500).format())
        assertEquals("-0,05 zł", Money(-5).format())
        assertEquals("-1 234,50 zł", Money(-123_450).format())
    }

    @Test
    fun formattedAmountUsesOnlyPlainSpaces() {
        val formatted = Money(123_450).format()

        assertTrue(formatted.none { it == ' ' || it == ' ' })
    }

    @Test
    fun ofZloteCombinesZlotyAndGrosze() {
        assertEquals(Money(1_250), Money.ofZlote(12, 50))
        assertEquals(Money(1_200), Money.ofZlote(12))
    }

    @Test
    fun arithmeticOperatorsWorkOnGrosze() {
        assertEquals(Money(1_750), Money(1_200) + Money(550))
        assertEquals(Money(650), Money(1_200) - Money(550))
        assertEquals(Money(3_600), Money(1_200) * 3)
        assertEquals(Money.ZERO, Money(1_200) * 0)
    }

    @Test
    fun subtractionBelowZeroGivesNegativeAmount() {
        assertEquals("-3,50 zł", (Money(500) - Money(850)).format())
    }

    @Test
    fun moneyIsComparedByAmount() {
        assertTrue(Money(1_200) > Money(1_199))
        assertTrue(Money(-1) < Money.ZERO)
        assertEquals(Money(900), listOf(Money(1_400), Money(900), Money(1_200)).min())
    }
}
