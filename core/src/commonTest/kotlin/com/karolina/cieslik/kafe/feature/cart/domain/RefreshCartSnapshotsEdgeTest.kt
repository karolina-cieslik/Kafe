package com.karolina.cieslik.kafe.feature.cart.domain

import com.karolina.cieslik.kafe.core.domain.Money
import com.karolina.cieslik.kafe.core.domain.Result
import com.karolina.cieslik.kafe.feature.cart.domain.CartCheckTestData.item
import com.karolina.cieslik.kafe.feature.cart.domain.CartCheckTestData.menu
import com.karolina.cieslik.kafe.feature.menu.domain.FakeMenuRepository
import com.karolina.cieslik.kafe.feature.menu.domain.ProductSize
import com.karolina.cieslik.kafe.feature.menu.domain.SizeCode
import com.karolina.cieslik.kafe.feature.product.domain.PriceCalculator
import com.karolina.cieslik.kafe.feature.product.domain.ProductTestData
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Zapis nowej migawki – nazwa i cena naraz, koszyk mieszany i pusty, menu zmienione w trakcie. */
class RefreshCartSnapshotsEdgeTest {

    private val cart = FakeCartRepository()
    private val menus = FakeMenuRepository()
    private val refresh = RefreshCartSnapshots(cart, menus, PriceCalculator())

    private fun latteAt(mPrice: Money, name: String = ProductTestData.latte.name) =
        ProductTestData.latte.copy(name = name, sizes = listOf(ProductSize(SizeCode.M, 350, mPrice)))

    @Test
    fun renamedAndRepricedItemSavesNameAndPriceAndReportsPrice() = runTest {
        cart.cart.value = Cart(listOf(item()))
        menus.emit("cafe", menu(products = listOf(latteAt(Money(1_700), name = "Latte"))))

        val result = refresh("cafe")

        assertEquals(Result.Success(listOf(PriceUpdate(1, Money(2_400), Money(2_500)))), result)
        val saved = cart.snapshotUpdates.single().single()
        assertEquals("Latte", saved.productName)
        assertEquals(Money(2_500), saved.unitPrice)
    }

    @Test
    fun onlyChangedAvailableItemsAreSavedInMixedCart() = runTest {
        val mocha = ProductTestData.latte.copy(id = "mocha", name = "Mocha")
        cart.cart.value = Cart(
            listOf(
                item(id = 1),
                item(id = 2, productId = "mocha", name = "Mocha", unitPrice = Money(2_000)),
                item(id = 3, productId = "gone", name = "Usunięty", unitPrice = Money(9_900)),
            ),
        )
        menus.emit("cafe", menu(products = listOf(ProductTestData.latte, mocha)))

        val result = refresh("cafe")

        assertEquals(Result.Success(listOf(PriceUpdate(2, Money(2_000), Money(2_400)))), result)
        assertEquals(listOf(2L), cart.snapshotUpdates.single().map { it.item.id })
    }

    @Test
    fun emptyCartWithSavedMenuWritesNothing() = runTest {
        menus.emit("cafe", menu())

        assertEquals(Result.Success(emptyList()), refresh("cafe"))
        assertTrue(cart.snapshotUpdates.isEmpty())
    }

    @Test
    fun menuChangedWhileCartIsReadIsNotMixedIntoSavedSnapshots() = runTest {
        val menuChangesOnCartRead = object : CartRepository by cart {
            override fun observeCart() = cart.cart.also {
                menus.emit("cafe", menu(products = listOf(latteAt(Money(1_900)))))
            }
        }
        cart.cart.value = Cart(listOf(item(id = 1), item(id = 2, quantity = 20)))
        menus.emit("cafe", menu(products = listOf(latteAt(Money(1_700)))))

        val result = RefreshCartSnapshots(menuChangesOnCartRead, menus, PriceCalculator())("cafe")

        val expected = listOf(PriceUpdate(1, Money(2_400), Money(2_500)), PriceUpdate(2, Money(2_400), Money(2_500)))
        assertEquals(Result.Success(expected), result)
        assertEquals(listOf(Money(2_500)), cart.snapshotUpdates.single().map { it.unitPrice }.distinct())
    }
}
