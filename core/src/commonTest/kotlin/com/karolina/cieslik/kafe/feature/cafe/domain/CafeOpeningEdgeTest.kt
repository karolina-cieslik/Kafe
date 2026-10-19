package com.karolina.cieslik.kafe.feature.cafe.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// KAFE-29: Przypadki brzegowe „otwarte teraz” i polskiego sortowania – uzupełnienie CafeOpeningTest deva.
class CafeOpeningEdgeTest {

    private fun cafe(vararg hours: OpeningHours) = Cafe("a", "Kafe", "ul. Testowa 1", 0.0, 0.0, hours.toList())

    @Test
    fun overnightHoursCoverMidnightExactly() {
        val cafe = cafe(OpeningHours(Weekday.SATURDAY, 18 * 60, 2 * 60))

        assertTrue(cafe.isOpenAt(Weekday.SATURDAY, 23 * 60 + 59))
        assertTrue(cafe.isOpenAt(Weekday.SUNDAY, 0))
        assertTrue(cafe.isOpenAt(Weekday.SUNDAY, 2 * 60 - 1))
        assertFalse(cafe.isOpenAt(Weekday.SATURDAY, 18 * 60 - 1))
    }

    @Test
    fun overnightFromPreviousDayAndOwnMorningHoursBothCount() {
        val cafe = cafe(
            OpeningHours(Weekday.FRIDAY, 18 * 60, 2 * 60),
            OpeningHours(Weekday.SATURDAY, 9 * 60, 21 * 60),
        )

        assertTrue(cafe.isOpenAt(Weekday.SATURDAY, 60))
        assertFalse(cafe.isOpenAt(Weekday.SATURDAY, 5 * 60))
        assertTrue(cafe.isOpenAt(Weekday.SATURDAY, 9 * 60))
    }

    @Test
    fun sameOpeningAndClosingTimeMeansOpenAllDay() {
        // 0:00–0:00 – zamknięcie ≤ otwarcie, więc traktowane jak całodobowe.
        val cafe = cafe(OpeningHours(Weekday.WEDNESDAY, 0, 0))

        assertTrue(cafe.isOpenAt(Weekday.WEDNESDAY, 0))
        assertTrue(cafe.isOpenAt(Weekday.WEDNESDAY, 12 * 60))
        assertTrue(cafe.isOpenAt(Weekday.WEDNESDAY, MINUTES_IN_DAY - 1))
        assertFalse(cafe.isOpenAt(Weekday.THURSDAY, 0))
    }

    @Test
    fun cafeWithoutAnyHoursIsAlwaysClosed() {
        val cafe = cafe()

        assertTrue(Weekday.entries.none { cafe.isOpenAt(it, 12 * 60) })
    }

    @Test
    fun previousDayWrapsAroundTheWeek() {
        assertEquals(Weekday.SATURDAY, Weekday.SUNDAY.previous())
        assertEquals(Weekday.MONDAY, Weekday.TUESDAY.previous())
    }

    @Test
    fun polishOrderHandlesAllDiacriticPairs() {
        val names = listOf("Żory", "Zamość", "Źródło", "Łódź", "Lublin", "Ćmielów", "Częstochowa", "Ślęza", "Sopot", "Opole", "Ostrów")

        assertEquals(
            listOf("Częstochowa", "Ćmielów", "Lublin", "Łódź", "Opole", "Ostrów", "Sopot", "Ślęza", "Zamość", "Źródło", "Żory"),
            names.sortedWith(PolishAlphabeticalOrder),
        )
    }

    @Test
    fun polishOrderIgnoresCaseAndPutsPrefixFirst() {
        assertEquals(listOf("kafe", "Kafe Długa", "KAFE RYNEK"), listOf("KAFE RYNEK", "Kafe Długa", "kafe").sortedWith(PolishAlphabeticalOrder))
    }
}
