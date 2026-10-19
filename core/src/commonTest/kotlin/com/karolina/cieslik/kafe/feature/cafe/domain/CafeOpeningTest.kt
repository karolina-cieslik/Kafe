package com.karolina.cieslik.kafe.feature.cafe.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** „Otwarte teraz” i sortowanie lokali. */
class CafeOpeningTest {

    private fun cafe(vararg hours: OpeningHours) = Cafe("a", "Kafe", "ul. Testowa 1", 0.0, 0.0, hours.toList())

    @Test
    fun openBetweenOpeningAndClosingTimeExclusive() {
        val cafe = cafe(OpeningHours(Weekday.MONDAY, 7 * 60, 20 * 60))

        assertFalse(cafe.isOpenAt(Weekday.MONDAY, 7 * 60 - 1))
        assertTrue(cafe.isOpenAt(Weekday.MONDAY, 7 * 60))
        assertTrue(cafe.isOpenAt(Weekday.MONDAY, 20 * 60 - 1))
        assertFalse(cafe.isOpenAt(Weekday.MONDAY, 20 * 60))
    }

    @Test
    fun dayWithoutHoursIsClosed() {
        val cafe = cafe(OpeningHours(Weekday.MONDAY, 7 * 60, 20 * 60))

        assertFalse(cafe.isOpenAt(Weekday.TUESDAY, 12 * 60))
    }

    @Test
    fun hoursPastMidnightSpillIntoNextDay() {
        val cafe = cafe(OpeningHours(Weekday.SUNDAY, 18 * 60, 2 * 60))

        assertTrue(cafe.isOpenAt(Weekday.SUNDAY, 23 * 60))
        assertTrue(cafe.isOpenAt(Weekday.MONDAY, 60))
        assertFalse(cafe.isOpenAt(Weekday.MONDAY, 2 * 60))
        assertFalse(cafe.isOpenAt(Weekday.SUNDAY, 60))
    }

    @Test
    fun previousOfMondayIsSunday() {
        assertEquals(Weekday.SUNDAY, Weekday.MONDAY.previous())
        assertEquals(Weekday.MONDAY, Weekday.TUESDAY.previous())
    }

    @Test
    fun polishOrderPutsDiacriticsAfterBaseLetter() {
        val names = listOf("Kafe Żoliborz", "kafe Rynek", "Kafe Łódź", "Kafe Lublin", "Kafe Śródmieście", "Kafe Sopot", "Kafe Zakopane")

        assertEquals(
            listOf("Kafe Lublin", "Kafe Łódź", "kafe Rynek", "Kafe Sopot", "Kafe Śródmieście", "Kafe Zakopane", "Kafe Żoliborz"),
            names.sortedWith(PolishAlphabeticalOrder),
        )
    }
}
