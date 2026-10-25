package com.karolina.cieslik.kafe.feature.cart.domain

import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.Money
import com.karolina.cieslik.kafe.core.domain.Result
import com.karolina.cieslik.kafe.feature.cart.domain.CartCheckTestData.item
import com.karolina.cieslik.kafe.feature.cart.domain.CartCheckTestData.menu
import com.karolina.cieslik.kafe.feature.cart.domain.CartCheckTestData.oat
import com.karolina.cieslik.kafe.feature.cart.domain.CartCheckTestData.twoShots
import com.karolina.cieslik.kafe.feature.menu.domain.FakeMenuRepository
import com.karolina.cieslik.kafe.feature.menu.domain.Menu
import com.karolina.cieslik.kafe.feature.menu.domain.ProductSize
import com.karolina.cieslik.kafe.feature.menu.domain.SizeCode
import com.karolina.cieslik.kafe.feature.product.domain.PriceCalculator
import com.karolina.cieslik.kafe.feature.product.domain.ProductTestData
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Zapis nowej migawki koszyka po odświeżeniu menu: co jest zapisywane, ile razy i co wraca jako zmiana ceny. */
class RefreshCartSnapshotsTest {

    private val cart = FakeCartRepository()
    private val menus = FakeMenuRepository()
    private val refresh = RefreshCartSnapshots(cart, menus, PriceCalculator())

    private val repriced = ProductTestData.latte.copy(sizes = listOf(ProductSize(SizeCode.M, 350, Money(1_700))))

    private suspend fun refreshWith(menu: Menu?, vararg items: CartItem): Result<List<PriceUpdate>, DataError.Local> {
        cart.cart.value = Cart(items.toList())
        menus.emit("cafe", menu)
        return refresh("cafe")
    }

    @Test
    fun changedPricesAreSavedInOneWriteAndReported() = runTest {
        val mocha = ProductTestData.latte.copy(id = "mocha", name = "Mocha")

        val result = refreshWith(
            menu(products = listOf(repriced, mocha)),
            item(id = 1),
            item(id = 2, productId = "mocha", name = "Mocha"),
        )

        assertEquals(Result.Success(listOf(PriceUpdate(1, Money(2_400), Money(2_500)))), result)
        assertEquals(listOf(listOf(1L)), cart.snapshotUpdates.map { updates -> updates.map { it.item.id } })
        assertEquals(Money(2_500), cart.snapshotUpdates.single().single().unitPrice)
    }

    @Test
    fun nothingIsWrittenWhenSnapshotsMatchMenu() = runTest {
        assertEquals(Result.Success(emptyList()), refreshWith(menu(), item()))
        assertTrue(cart.snapshotUpdates.isEmpty())
    }

    @Test
    fun nothingIsWrittenWithoutSavedMenu() = runTest {
        assertEquals(Result.Success(emptyList()), refreshWith(null, item(unitPrice = Money(1))))
        assertTrue(cart.snapshotUpdates.isEmpty())
    }

    @Test
    fun renameWithSamePriceIsSavedButNotReportedAsPriceUpdate() = runTest {
        val renamed = ProductTestData.latte.copy(name = "Latte")

        val result = refreshWith(menu(products = listOf(renamed)), item())

        assertEquals(Result.Success(emptyList()), result)
        assertEquals("Latte", cart.snapshotUpdates.single().single().productName)
    }

    @Test
    fun unavailableItemIsNotUpdated() = runTest {
        val milk = ProductTestData.milk.copy(options = ProductTestData.milk.options.filterNot { it.id == "milk-oat" })

        val menu = menu(products = listOf(repriced), groups = listOf(milk, ProductTestData.shots))

        val result = refreshWith(menu, item())

        assertEquals(Result.Success(emptyList()), result)
        assertTrue(cart.snapshotUpdates.isEmpty())
    }

    @Test
    fun defaultOptionFlagFollowsMenu() = runTest {
        val oatDefault = ProductTestData.milk.copy(defaultOptionId = "milk-oat")

        refreshWith(menu(groups = listOf(oatDefault, ProductTestData.shots)), item(selections = listOf(oat, twoShots)))

        val options = cart.snapshotUpdates.single().single().options
        assertEquals(true, options.getValue(OptionRef("milk", "milk-oat")).isDefault)
        assertEquals(false, options.getValue(OptionRef("shots", "shot")).isDefault)
    }

    @Test
    fun storageErrorIsReturned() = runTest {
        cart.updateResult = Result.Failure(DataError.Local.DISK_FULL)

        assertEquals(Result.Failure(DataError.Local.DISK_FULL), refreshWith(menu(products = listOf(repriced)), item()))
    }
}
