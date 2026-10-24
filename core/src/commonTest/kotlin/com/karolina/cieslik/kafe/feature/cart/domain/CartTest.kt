package com.karolina.cieslik.kafe.feature.cart.domain

import com.karolina.cieslik.kafe.core.domain.Money
import com.karolina.cieslik.kafe.feature.menu.domain.SizeCode
import kotlin.test.Test
import kotlin.test.assertEquals

/** Liczniki i sumy koszyka oraz cena pozycji. */
class CartTest {

    private fun item(id: Long, quantity: Int, unitPrice: Money) = CartItem(
        id = id,
        productId = "latte",
        productName = "Caffè latte",
        size = SizeCode.M,
        selections = emptyMap(),
        quantity = quantity,
        unitPrice = unitPrice,
    )

    @Test
    fun itemTotalPriceIsUnitPriceTimesQuantity() {
        assertEquals(Money(6_600), item(1, 3, Money(2_200)).totalPrice)
    }

    @Test
    fun emptyCartHasNoItemsAndZeroTotal() {
        assertEquals(0, Cart.EMPTY.itemCount)
        assertEquals(Money.ZERO, Cart.EMPTY.total)
    }

    @Test
    fun itemCountSumsQuantitiesAndTotalSumsItemPrices() {
        val cart = Cart(listOf(item(1, 2, Money(1_400)), item(2, 3, Money(1_000))))

        assertEquals(5, cart.itemCount)
        assertEquals(Money(5_800), cart.total)
    }

    @Test
    fun itemAndNewItemWithSameConfigurationHaveSameKey() {
        val selections = mapOf("milk" to mapOf("oat" to 1), "extras" to mapOf("shot" to 2))
        val item = item(1, 1, Money(1_400)).copy(selections = selections)
        val newItem = NewCartItem("latte", "Latte", SizeCode.M, selections.entries.reversed().associate { it.toPair() }, 5, Money(1))

        assertEquals(item.configurationKey, newItem.configurationKey)
    }
}
