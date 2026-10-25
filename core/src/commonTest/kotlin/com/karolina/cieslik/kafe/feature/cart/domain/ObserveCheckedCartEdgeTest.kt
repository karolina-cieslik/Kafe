package com.karolina.cieslik.kafe.feature.cart.domain

import com.karolina.cieslik.kafe.core.domain.Money
import com.karolina.cieslik.kafe.feature.cart.domain.CartCheckTestData.item
import com.karolina.cieslik.kafe.feature.cart.domain.CartCheckTestData.menu
import com.karolina.cieslik.kafe.feature.menu.domain.FakeMenuRepository
import com.karolina.cieslik.kafe.feature.menu.domain.Menu
import com.karolina.cieslik.kafe.feature.menu.domain.ModifierOption
import com.karolina.cieslik.kafe.feature.menu.domain.ProductSize
import com.karolina.cieslik.kafe.feature.menu.domain.SizeCode
import com.karolina.cieslik.kafe.feature.product.domain.PriceCalculator
import com.karolina.cieslik.kafe.feature.product.domain.ProductTestData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Sprawdzenie koszyka – przypadki brzegowe: kilka powodów niedostępności naraz, zmiana nazwy i ceny, sumy. */
class ObserveCheckedCartEdgeTest {

    private val cart = FakeCartRepository()
    private val menus = FakeMenuRepository()
    private val observe = ObserveCheckedCart(cart, menus, PriceCalculator())

    private val latteWithoutM = ProductTestData.latte.copy(
        sizes = ProductTestData.latte.sizes.filterNot { it.code == SizeCode.M },
    )
    private val milkWithoutOat =
        ProductTestData.milk.copy(options = ProductTestData.milk.options.filterNot { it.id == "milk-oat" })

    private suspend fun checked(menu: Menu?, vararg items: CartItem): CheckedCart {
        cart.cart.value = Cart(items.toList())
        menus.emit("cafe", menu)
        return observe("cafe").first()
    }

    private suspend fun statusOf(menu: Menu): CartItemStatus = checked(menu, item()).items.single().status

    private fun unavailable(reason: UnavailableReason) = CartItemStatus.Unavailable(reason)

    private fun latteAt(mPrice: Money, name: String = ProductTestData.latte.name) =
        ProductTestData.latte.copy(name = name, sizes = listOf(ProductSize(SizeCode.M, 350, mPrice)))

    @Test
    fun inactiveProductUnavailableInCafeIsRemoved() = runTest {
        val inactive = ProductTestData.latte.copy(isActive = false)

        assertEquals(
            unavailable(UnavailableReason.PRODUCT_REMOVED),
            statusOf(menu(products = listOf(inactive), unavailableProductIds = setOf("latte"))),
        )
    }

    @Test
    fun productUnavailableInCafeWinsOverRemovedSizeAndOption() = runTest {
        val menu = menu(
            products = listOf(latteWithoutM),
            groups = listOf(milkWithoutOat, ProductTestData.shots),
            unavailableProductIds = setOf("latte"),
        )

        assertEquals(unavailable(UnavailableReason.PRODUCT_UNAVAILABLE), statusOf(menu))
    }

    @Test
    fun removedSizeWinsOverRemovedOptionAndLoweredLimit() = runTest {
        val menu = menu(
            products = listOf(latteWithoutM),
            groups = listOf(milkWithoutOat, ProductTestData.shots.copy(maxQuantity = 1)),
        )

        assertEquals(unavailable(UnavailableReason.SIZE_UNAVAILABLE), statusOf(menu))
    }

    @Test
    fun removedOptionWinsOverLoweredLimit() = runTest {
        val menu = menu(groups = listOf(milkWithoutOat, ProductTestData.shots.copy(maxQuantity = 1)))

        assertEquals(unavailable(UnavailableReason.OPTION_UNAVAILABLE), statusOf(menu))
    }

    @Test
    fun renamedAndRepricedItemIsPriceChangedAndKeepsSnapshotName() = runTest {
        val checked = checked(menu(products = listOf(latteAt(Money(1_700), name = "Latte"))), item()).items.single()

        assertEquals(CartItemStatus.PriceChanged(previousUnitPrice = Money(2_400)), checked.status)
        assertEquals(Money(2_500), checked.currentUnitPrice)
        assertEquals("Caffè latte", checked.item.productName)
    }

    @Test
    fun renamedProductAndOptionAtSamePriceStayAvailable() = runTest {
        val milk = ProductTestData.milk.copy(
            options = listOf(
                ModifierOption("milk-cow", "Krowie", Money.ZERO),
                ModifierOption("milk-oat", "Napój owsiany", Money(200)),
            ),
        )
        val menu = menu(
            products = listOf(latteAt(Money(1_600), name = "Latte")),
            groups = listOf(milk, ProductTestData.shots),
        )

        val checked = checked(menu, item()).items.single()

        assertEquals(CartItemStatus.Available, checked.status)
        assertEquals(Money(2_400), checked.currentUnitPrice)
    }

    @Test
    fun lowerMenuPriceIsAlsoPriceChanged() = runTest {
        val checked = checked(menu(products = listOf(latteAt(Money(1_500)))), item(quantity = 3))

        assertEquals(CartItemStatus.PriceChanged(Money(2_400)), checked.items.single().status)
        assertEquals(Money(6_900), checked.payableTotal)
    }

    @Test
    fun onlyUnavailableItemsGiveZeroCountAndTotal() = runTest {
        val checked = checked(menu(products = emptyList()), item(id = 1, quantity = 5), item(id = 2, quantity = 20))

        assertTrue(checked.isVerified)
        assertEquals(0, checked.itemCount)
        assertEquals(Money.ZERO, checked.payableTotal)
        assertFalse(checked.canProceed)
    }

    @Test
    fun unverifiedItemsCountAtSnapshotPriceButCannotProceed() = runTest {
        val checked = checked(null, item(id = 1, quantity = 2), item(id = 2, quantity = 3, unitPrice = Money(1_000)))

        assertEquals(5, checked.itemCount)
        assertEquals(Money(7_800), checked.payableTotal)
        assertFalse(checked.canProceed)
    }

    @Test
    fun twentyPiecesOfVeryExpensiveItemsSumBeyondIntRange() = runTest {
        val expensive = latteAt(Money(99_999_999))
        val unitPrice = Money(99_999_999 + 200 + 600)
        val items = (1L..3L).map { item(id = it, quantity = 20, unitPrice = unitPrice) }.toTypedArray()

        val checked = checked(menu(products = listOf(expensive)), *items)

        assertEquals(listOf(CartItemStatus.Available), checked.items.map { it.status }.distinct())
        assertEquals(60, checked.itemCount)
        assertEquals(Money(6_000_047_940), checked.payableTotal)
        assertEquals("60 000 479,40 zł", checked.payableTotal.format())
        assertTrue(checked.canProceed)
    }

    @Test
    fun repricedItemAtLimitUsesMenuPriceForAllPieces() = runTest {
        val checked = checked(
            menu(products = listOf(latteAt(Money(1_700)))),
            item(id = 1, quantity = 20),
            item(id = 2, productId = "gone", quantity = 4),
        )

        assertEquals(20, checked.itemCount)
        assertEquals(Money(50_000), checked.payableTotal)
        assertFalse(checked.canProceed)
    }
}
