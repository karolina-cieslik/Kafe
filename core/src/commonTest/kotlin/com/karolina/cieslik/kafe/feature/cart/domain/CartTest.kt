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
        selections = emptyList(),
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
        val oat = CartSelection("milk", "oat", "Owsiane", Money(200), 1)
        val shots = CartSelection("extras", "shot", "Dodatkowe espresso", Money(300), 2)
        val item = item(1, 1, Money(1_400)).copy(selections = listOf(oat, shots))
        val newItem = NewCartItem(
            productId = "latte",
            productName = "Latte",
            size = SizeCode.M,
            selections = listOf(shots.copy(optionName = "Shot", surcharge = Money(350)), oat),
            quantity = 5,
            unitPrice = Money(1),
        )

        assertEquals(item.configurationKey, newItem.configurationKey)
    }
}
