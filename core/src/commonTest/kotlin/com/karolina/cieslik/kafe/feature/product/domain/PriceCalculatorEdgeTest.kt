package com.karolina.cieslik.kafe.feature.product.domain

import com.karolina.cieslik.kafe.core.domain.Money
import kotlin.test.Test
import kotlin.test.assertEquals

/** Przypadki brzegowe ceny: grosze bez błędów zaokrągleń, format PLN i maksymalne ilości. */
class PriceCalculatorEdgeTest {

    private val calculator = PriceCalculator()

    @Test
    fun groszeAreSummedWithoutRoundingErrors() {
        // Odpowiednik 0,10 + 0,20 zł – w Double dałoby 0.30000000000000004.
        val total = calculator.totalPrice(Money(10), listOf(PricedOption(Money(20), 1)), quantity = 3)

        assertEquals(Money(90), total)
        assertEquals("0,90 zł", total.format())
    }

    @Test
    fun oddGroszeAreFormattedWithPolishSeparator() {
        val total = calculator.totalPrice(Money(1_250), listOf(PricedOption(Money(600), 1)), quantity = 1)

        assertEquals("18,50 zł", total.format())
    }

    @Test
    fun unitPriceIsIndependentOfItemQuantity() {
        val options = listOf(PricedOption(Money(300), 2))

        assertEquals(Money(2_000), calculator.unitPrice(Money(1_400), options))
        assertEquals(Money(2_000) * 3, calculator.totalPrice(Money(1_400), options, quantity = 3))
    }

    @Test
    fun maximumShotsAndItemQuantityGiveExactTotal() {
        val total = calculator.totalPrice(Money(2_600), listOf(PricedOption(Money(300), 4)), quantity = 20)

        assertEquals("760,00 zł", total.format())
    }
}
