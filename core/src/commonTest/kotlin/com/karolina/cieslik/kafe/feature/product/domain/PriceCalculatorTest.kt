package com.karolina.cieslik.kafe.feature.product.domain

import com.karolina.cieslik.kafe.core.domain.Money
import kotlin.test.Test
import kotlin.test.assertEquals

// KAFE-11: Wzór ceny pozycji.
class PriceCalculatorTest {

    private val calculator = PriceCalculator()

    @Test
    fun withoutOptionsTotalIsSizePriceTimesQuantity() {
        assertEquals(Money(4800), calculator.totalPrice(Money(1600), emptyList(), quantity = 3))
    }

    @Test
    fun surchargesAreMultipliedByOptionQuantityAndThenByItemQuantity() {
        val options = listOf(PricedOption(Money(200), 1), PricedOption(Money(300), 2))

        assertEquals(Money(1600 + 200 + 600), calculator.unitPrice(Money(1600), options))
        assertEquals(Money((1600 + 200 + 600) * 2L), calculator.totalPrice(Money(1600), options, quantity = 2))
    }

    @Test
    fun zeroSurchargeDoesNotChangePrice() {
        assertEquals(Money(1600), calculator.unitPrice(Money(1600), listOf(PricedOption(Money.ZERO, 1))))
    }
}
