package com.karolina.cieslik.kafe.core.presentation.designsystem.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Tekst licznika na ikonie koszyka: brak przy zerze, liczba do 99, potem „99+”. */
class CountBadgeLabelTest {

    @Test
    fun zeroHasNoLabel() {
        assertNull(countBadgeLabel(0))
    }

    @Test
    fun negativeCountHasNoLabel() {
        assertNull(countBadgeLabel(-1))
    }

    @Test
    fun countUpToMaxIsShownAsIs() {
        assertEquals("1", countBadgeLabel(1))
        assertEquals("99", countBadgeLabel(99))
    }

    @Test
    fun countAboveMaxIsShownAsMaxPlus() {
        assertEquals("99+", countBadgeLabel(100))
        assertEquals("99+", countBadgeLabel(1000))
    }
}
