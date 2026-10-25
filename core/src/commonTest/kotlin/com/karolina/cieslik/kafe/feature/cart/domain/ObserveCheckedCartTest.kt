package com.karolina.cieslik.kafe.feature.cart.domain

import com.karolina.cieslik.kafe.core.domain.Money
import com.karolina.cieslik.kafe.feature.cart.domain.CartCheckTestData.item
import com.karolina.cieslik.kafe.feature.cart.domain.CartCheckTestData.menu
import com.karolina.cieslik.kafe.feature.cart.domain.CartCheckTestData.oat
import com.karolina.cieslik.kafe.feature.cart.domain.CartCheckTestData.twoShots
import com.karolina.cieslik.kafe.feature.menu.domain.FakeMenuRepository
import com.karolina.cieslik.kafe.feature.menu.domain.Menu
import com.karolina.cieslik.kafe.feature.menu.domain.ModifierGroupType
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

/** Sprawdzenie koszyka z menu lokalu: statusy pozycji, ceny, podsumowanie i stan bez zapamiętanego menu. */
class ObserveCheckedCartTest {

    private val cart = FakeCartRepository()
    private val menus = FakeMenuRepository()
    private val observe = ObserveCheckedCart(cart, menus, PriceCalculator())

    private suspend fun checked(menu: Menu?, vararg items: CartItem): CheckedCart {
        cart.cart.value = Cart(items.toList())
        menus.emit("cafe", menu)
        return observe("cafe").first()
    }

    private suspend fun statusOf(menu: Menu?, item: CartItem = item()): CartItemStatus =
        checked(menu, item).items.single().status

    private fun unavailable(reason: UnavailableReason) = CartItemStatus.Unavailable(reason)

    @Test
    fun itemMatchingMenuIsAvailableAtSnapshotPrice() = runTest {
        val checked = checked(menu(), item()).items.single()

        assertEquals(CartItemStatus.Available, checked.status)
        assertEquals(Money(2_400), checked.currentUnitPrice)
    }

    @Test
    fun changedSizePriceGivesPriceChangedWithMenuPrice() = runTest {
        val latte = ProductTestData.latte.copy(sizes = listOf(ProductSize(SizeCode.M, 350, Money(1_700))))

        val checked = checked(menu(products = listOf(latte)), item()).items.single()

        assertEquals(CartItemStatus.PriceChanged(previousUnitPrice = Money(2_400)), checked.status)
        assertEquals(Money(2_500), checked.currentUnitPrice)
    }

    @Test
    fun changedSurchargeGivesPriceChanged() = runTest {
        val milk = ProductTestData.milk.copy(
            options = listOf(
                ModifierOption("milk-cow", "Krowie", Money.ZERO),
                ModifierOption("milk-oat", "Owsiane", Money(250)),
            ),
        )

        val checked = checked(menu(groups = listOf(milk, ProductTestData.shots)), item()).items.single()

        assertEquals(CartItemStatus.PriceChanged(Money(2_400)), checked.status)
        assertEquals(Money(2_450), checked.currentUnitPrice)
    }

    @Test
    fun productMissingOrInactiveIsRemoved() = runTest {
        val inactive = ProductTestData.latte.copy(isActive = false)

        assertEquals(unavailable(UnavailableReason.PRODUCT_REMOVED), statusOf(menu(products = emptyList())))
        assertEquals(unavailable(UnavailableReason.PRODUCT_REMOVED), statusOf(menu(products = listOf(inactive))))
    }

    @Test
    fun productUnavailableInCafeIsUnavailable() = runTest {
        assertEquals(
            unavailable(UnavailableReason.PRODUCT_UNAVAILABLE),
            statusOf(menu(unavailableProductIds = setOf("latte"))),
        )
    }

    @Test
    fun sizeRemovedFromProductOrUnavailableInCafeIsSizeUnavailable() = runTest {
        val withoutM = ProductTestData.latte.copy(
            sizes = ProductTestData.latte.sizes.filterNot { it.code == SizeCode.M },
        )

        assertEquals(unavailable(UnavailableReason.SIZE_UNAVAILABLE), statusOf(menu(products = listOf(withoutM))))
        assertEquals(
            unavailable(UnavailableReason.SIZE_UNAVAILABLE),
            statusOf(menu(unavailableSizes = mapOf("latte" to setOf(SizeCode.M)))),
        )
    }

    @Test
    fun optionRemovedFromGroupIsUnavailableAndDoesNotLowerPrice() = runTest {
        val milk = ProductTestData.milk.copy(options = ProductTestData.milk.options.filterNot { it.id == "milk-oat" })

        val checked = checked(menu(groups = listOf(milk, ProductTestData.shots)), item()).items.single()

        assertEquals(unavailable(UnavailableReason.OPTION_UNAVAILABLE), checked.status)
        assertEquals(Money(2_400), checked.currentUnitPrice)
    }

    @Test
    fun groupDetachedFromProductMakesItsOptionUnavailable() = runTest {
        val latte = ProductTestData.latte.copy(modifierGroupIds = listOf("milk", "syrup"))

        assertEquals(unavailable(UnavailableReason.OPTION_UNAVAILABLE), statusOf(menu(products = listOf(latte))))
    }

    @Test
    fun optionUnavailableInCafeIsOptionUnavailable() = runTest {
        assertEquals(
            unavailable(UnavailableReason.OPTION_UNAVAILABLE),
            statusOf(menu(unavailableOptionIds = setOf("milk-oat"))),
        )
    }

    @Test
    fun quantityAboveLoweredGroupLimitIsInvalid() = runTest {
        val shots = ProductTestData.shots.copy(maxQuantity = 1)

        assertEquals(
            unavailable(UnavailableReason.INVALID_CONFIGURATION),
            statusOf(menu(groups = listOf(ProductTestData.milk, shots, ProductTestData.syrup))),
        )
    }

    @Test
    fun twoOptionsInSingleGroupAreInvalid() = runTest {
        val shotsAsSingle = ProductTestData.shots.copy(
            type = ModifierGroupType.SINGLE,
            maxQuantity = 1,
            options = listOf(
                ModifierOption("shot", "Shot espresso", Money(300)),
                ModifierOption("decaf", "Bezkofeinowe", Money(300)),
            ),
        )
        val decaf = CartSelection("shots", "decaf", "Bezkofeinowe", Money(300), 1)
        val both = item(selections = listOf(oat, twoShots.copy(quantity = 1), decaf))

        assertEquals(
            unavailable(UnavailableReason.INVALID_CONFIGURATION),
            statusOf(menu(groups = listOf(ProductTestData.milk, shotsAsSingle)), both),
        )
    }

    @Test
    fun requiredGroupWithoutChoiceIsInvalid() = runTest {
        assertEquals(
            unavailable(UnavailableReason.INVALID_CONFIGURATION),
            statusOf(menu(), item(selections = listOf(twoShots), unitPrice = Money(2_200))),
        )
    }

    @Test
    fun withoutSavedMenuAllItemsAreUnverifiedAtSnapshotPrice() = runTest {
        val checked = checked(null, item(id = 1), item(id = 2, quantity = 3))

        assertFalse(checked.isVerified)
        assertEquals(listOf(CartItemStatus.Unverified, CartItemStatus.Unverified), checked.items.map { it.status })
        assertEquals(Money(9_600), checked.payableTotal)
        assertFalse(checked.canProceed)
    }

    @Test
    fun countAndTotalSkipUnavailableItemsAndBlockProceeding() = runTest {
        val checked = checked(
            menu(),
            item(id = 1, quantity = 2),
            item(id = 2, productId = "gone", quantity = 5),
        )

        assertEquals(2, checked.itemCount)
        assertEquals(Money(4_800), checked.payableTotal)
        assertFalse(checked.canProceed)
    }

    @Test
    fun totalUsesMenuPriceForChangedItems() = runTest {
        val latte = ProductTestData.latte.copy(sizes = listOf(ProductSize(SizeCode.M, 350, Money(1_700))))

        val checked = checked(menu(products = listOf(latte)), item(quantity = 2))

        assertEquals(Money(5_000), checked.payableTotal)
        assertTrue(checked.canProceed)
    }

    @Test
    fun emptyCartCannotProceed() = runTest {
        assertFalse(checked(menu()).canProceed)
    }

    @Test
    fun itemsKeepCartOrder() = runTest {
        val checked = checked(menu(), item(id = 3), item(id = 1, productId = "gone"), item(id = 2))

        assertEquals(listOf(3L, 1L, 2L), checked.items.map { it.item.id })
    }

    @Test
    fun menuChangeIsEmitted() = runTest {
        checked(menu(), item())

        menus.emit("cafe", menu(unavailableProductIds = setOf("latte")))

        assertEquals(unavailable(UnavailableReason.PRODUCT_UNAVAILABLE), observe("cafe").first().items.single().status)
    }
}
