package com.karolina.cieslik.kafe.feature.cart.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.karolina.cieslik.kafe.core.data.database.KafeDatabase
import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.Money
import com.karolina.cieslik.kafe.core.domain.Result
import com.karolina.cieslik.kafe.feature.cart.data.local.CartDao
import com.karolina.cieslik.kafe.feature.cart.data.local.CartItemEntity
import com.karolina.cieslik.kafe.feature.cart.domain.AddToCartOutcome
import com.karolina.cieslik.kafe.feature.cart.domain.Cart
import com.karolina.cieslik.kafe.feature.cart.domain.CartItem
import com.karolina.cieslik.kafe.feature.cart.domain.CartSelection
import com.karolina.cieslik.kafe.feature.cart.domain.CartSnapshotUpdate
import com.karolina.cieslik.kafe.feature.cart.domain.DecreaseOutcome
import com.karolina.cieslik.kafe.feature.cart.domain.NewCartItem
import com.karolina.cieslik.kafe.feature.cart.domain.OptionRef
import com.karolina.cieslik.kafe.feature.cart.domain.OptionSnapshot
import com.karolina.cieslik.kafe.feature.cart.domain.RestoreOutcome
import com.karolina.cieslik.kafe.feature.menu.domain.SizeCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * RoomCartRepository na prawdziwym SQLite: id po usunięciach, „Cofnij”, równoległe zapisy, odporny odczyt, nowe
 * migawki i usuwanie nieczytelnych wierszy (także na pustej bazie i z wierszami z poprzedniego formatu).
 */
@RunWith(AndroidJUnit4::class)
class RoomCartRepositoryDeviceTest {

    private lateinit var database: KafeDatabase
    private lateinit var dao: CartDao
    private lateinit var repository: RoomCartRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder<KafeDatabase>(context)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        dao = database.cartDao()
        repository = RoomCartRepository(dao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun newItem(productId: String = "latte", quantity: Int = 1, unitPrice: Money = Money(1_400)) = NewCartItem(
        productId = productId,
        productName = "Caffè latte",
        size = SizeCode.M,
        selections = listOf(CartSelection("milk", "milk-cow", "Krowie", Money.ZERO, 1)),
        quantity = quantity,
        unitPrice = unitPrice,
    )

    private suspend fun add(item: NewCartItem): CartItem =
        (repository.add(item) as Result.Success).data.item

    private suspend fun items(): List<CartItem> = repository.observeCart().first().items

    private fun row(productId: String, sizeCode: String = "M", selectionsJson: String = "[]") = CartItemEntity(
        id = 0,
        productId = productId,
        productName = productId,
        sizeCode = sizeCode,
        selectionsJson = selectionsJson,
        configurationKey = "$productId|$sizeCode|$selectionsJson",
        quantity = 1,
        unitPriceGrosze = 1_500,
    )

    private fun renamed(item: CartItem, name: String) = CartSnapshotUpdate(
        item = item,
        productName = name,
        unitPrice = Money(1_700),
        options = item.selections.associate {
            OptionRef(it.groupId, it.optionId) to OptionSnapshot("${it.optionName} $name", Money(50), it.isDefault)
        },
    )

    @Test
    fun clearDoesNotResetIdsSoNewItemsComeAfterRemovedOnes() = runTest {
        val first = add(newItem("latte"))
        val second = add(newItem("mocha"))
        repository.clear()

        val afterClear = add(newItem("flat-white"))

        assertTrue(afterClear.id > second.id && second.id > first.id)
    }

    @Test
    fun restoreAfterClearBringsItemBackBeforeNewerItems() = runTest {
        val latte = add(newItem("latte"))
        val removed = (repository.remove(latte.id) as Result.Success).data
        repository.clear()
        val mocha = add(newItem("mocha"))

        repository.restore(removed)

        assertEquals(listOf(latte.id, mocha.id), items().map { it.id })
    }

    @Test
    fun restoreAfterSetQuantityBringsBackChangedQuantityAtSamePlace() = runTest {
        val latte = add(newItem("latte"))
        val mocha = add(newItem("mocha"))
        repository.setQuantity(latte.id, 5)
        val removed = (repository.remove(latte.id) as Result.Success).data

        val outcome = repository.restore(removed)

        assertEquals(5, removed.quantity)
        assertIs<RestoreOutcome.Restored>((outcome as Result.Success).data)
        assertEquals(listOf(latte.id to 5, mocha.id to 1), items().map { it.id to it.quantity })
    }

    @Test
    fun restoreIntoNewerIdenticalItemKeepsNewerIdPlaceAndPrice() = runTest {
        val old = add(newItem("latte", quantity = 3, unitPrice = Money(1_400)))
        val removed = (repository.remove(old.id) as Result.Success).data
        val mocha = add(newItem("mocha"))
        val newer = add(newItem("latte", quantity = 2, unitPrice = Money(1_600)))

        val outcome = repository.restore(removed)

        val merged = assertIs<RestoreOutcome.Merged>((outcome as Result.Success).data).item
        assertEquals(newer.id, merged.id)
        assertEquals(5, merged.quantity)
        assertEquals(Money(1_600), merged.unitPrice)
        assertEquals(listOf(mocha.id, newer.id), items().map { it.id })
    }

    @Test
    fun restoreIntoNewerIdenticalItemAboveLimitIsCapped() = runTest {
        val old = add(newItem("latte", quantity = 15))
        val removed = (repository.remove(old.id) as Result.Success).data
        add(newItem("latte", quantity = 10))

        val outcome = repository.restore(removed)

        val merged = assertIs<RestoreOutcome.Merged>((outcome as Result.Success).data)
        assertEquals(true, merged.capped)
        assertEquals(20, merged.item.quantity)
        assertEquals(1, items().size)
    }

    @Test
    fun parallelAddsAboveLimitStopAtTwentyAndReportEachStepOnce() = runTest {
        val outcomes = (1..25).map { async(Dispatchers.IO) { repository.add(newItem()) } }.awaitAll()
            .map { (it as Result.Success).data }

        assertEquals(1, outcomes.count { it is AddToCartOutcome.Added })
        assertEquals(19, outcomes.count { it is AddToCartOutcome.QuantityIncreased })
        assertEquals(5, outcomes.count { it is AddToCartOutcome.CappedAtLimit })
        assertEquals(listOf(20), items().map { it.quantity })
    }

    @Test
    fun parallelRestoreAndAddOfSameConfigurationLeaveOneItem() = runTest {
        val removed = (repository.remove(add(newItem(quantity = 2)).id) as Result.Success).data

        listOf(
            async(Dispatchers.IO) { repository.restore(removed) },
            async(Dispatchers.IO) { repository.add(newItem(quantity = 3)) },
        ).awaitAll()

        assertEquals(listOf(5), items().map { it.quantity })
    }

    @Test
    fun productIdsDifferingOnlyInCaseAreSeparateItems() = runTest {
        add(newItem("latte"))
        add(newItem("Latte"))

        assertEquals(2, items().size)
    }

    @Test
    fun unreadableRowIsSkippedAndItsRemovalFailsWithUnknownError() = runTest {
        val good = add(newItem("latte"))
        val broken = CartItemEntity(
            id = 0,
            productId = "mocha",
            productName = "Mocha",
            sizeCode = "XL",
            selectionsJson = "[]",
            configurationKey = "mocha|XL|",
            quantity = 1,
            unitPriceGrosze = 1_500,
        )
        val brokenId = dao.add(broken).item.id

        assertEquals(listOf(good.id), items().map { it.id })
        assertEquals(Result.Failure(DataError.Local.UNKNOWN), repository.remove(brokenId))
        assertEquals(listOf(good.id), items().map { it.id })
    }

    @Test
    fun specialCharactersAndLongIdsSurviveRealDatabase() = runTest {
        val tricky = "Cytryna \"bio\" \\ ' ; : = | \n – ąęł 😀" + "x".repeat(3_000)
        val item = NewCartItem(
            productId = tricky,
            productName = tricky,
            size = SizeCode.L,
            selections = listOf(CartSelection(tricky, tricky, tricky, Money(250), 2)),
            quantity = 1,
            unitPrice = Money(2_300),
        )

        val added = add(item)
        add(item)

        val stored = items().single()
        assertEquals(added.copy(quantity = 2), stored)
    }

    @Test
    fun removeUnreadableOnFreshDatabaseRemovesNothing() = runTest {
        assertEquals(Result.Success(0), repository.removeUnreadable())
        assertEquals(emptyList(), items())
    }

    @Test
    fun removeUnreadableKeepsRowsFromPreviousFormatWithoutDefaultFlag() = runTest {
        val oldFormat = """[{"groupId":"milk","optionId":"milk-cow","optionName":"Krowie",""" +
            """"surchargeGrosze":0,"quantity":1}]"""
        val oldId = dao.add(row("latte", selectionsJson = oldFormat)).item.id
        dao.add(row("mocha", sizeCode = "XL"))

        assertEquals(Result.Success(1), repository.removeUnreadable())

        val kept = items().single()
        assertEquals(oldId, kept.id)
        assertEquals(
            listOf(CartSelection("milk", "milk-cow", "Krowie", Money.ZERO, 1, isDefault = false)),
            kept.selections,
        )
    }

    @Test
    fun removeUnreadableWhenAllRowsAreUnreadableFailsAndKeepsThem() = runTest {
        dao.add(row("mocha", sizeCode = "XL"))
        dao.add(row("latte", selectionsJson = "{}"))

        assertEquals(Result.Failure(DataError.Local.UNKNOWN), repository.removeUnreadable())
        assertEquals(Result.Failure(DataError.Local.UNKNOWN), repository.removeUnreadable())
        assertEquals(2, dao.observeItems().first().size)
    }

    @Test
    fun decreaseAtOneThenRestoreBringsItemBackToItsPlace() = runTest {
        val latte = add(newItem("latte"))
        val mocha = add(newItem("mocha"))
        val flatWhite = add(newItem("flat-white"))

        val decreased = (repository.decreaseOrRemove(mocha.id) as Result.Success).data
        val removed = assertIs<DecreaseOutcome.Removed>(decreased).item
        assertEquals(listOf(latte.id, flatWhite.id), items().map { it.id })

        val outcome = repository.restore(removed)

        assertEquals(RestoreOutcome.Restored(mocha), (outcome as Result.Success).data)
        assertEquals(listOf(latte.id, mocha.id, flatWhite.id), items().map { it.id })
    }

    @Test
    fun restoreMergeExactlyToLimitIsNotCapped() = runTest {
        val old = add(newItem("latte", quantity = 12))
        val removed = (repository.remove(old.id) as Result.Success).data
        add(newItem("latte", quantity = 8))

        val merged = assertIs<RestoreOutcome.Merged>((repository.restore(removed) as Result.Success).data)

        assertEquals(false, merged.capped)
        assertEquals(20, merged.item.quantity)
    }

    @Test
    fun updateSnapshotsKeepsDefaultFlagsOptionQuantitiesAndKey() = runTest {
        val item = add(
            newItem("latte", quantity = 3).copy(
                selections = listOf(
                    CartSelection("milk", "milk-cow", "Krowie", Money.ZERO, 1, isDefault = true),
                    CartSelection("extra", "shot", "Shot", Money(300), 2, isDefault = false),
                ),
            ),
        )

        assertEquals(Result.Success(Unit), repository.updateSnapshots(listOf(renamed(item, "nowa"))))

        val stored = items().single()
        assertEquals(
            item.copy(
                productName = "nowa",
                unitPrice = Money(1_700),
                selections = listOf(
                    CartSelection("milk", "milk-cow", "Krowie nowa", Money(50), 1, isDefault = true),
                    CartSelection("extra", "shot", "Shot nowa", Money(50), 2, isDefault = false),
                ),
            ),
            stored,
        )
        assertEquals(item.configurationKey, stored.configurationKey)
    }

    @Test
    fun updateSnapshotsWithEmptyListChangesNothing() = runTest {
        val item = add(newItem("latte"))

        assertEquals(Result.Success(Unit), repository.updateSnapshots(emptyList()))
        assertEquals(listOf(item), items())
    }

    @Test
    fun observersSeeNewSnapshotsOfAllItemsAtOnce() = runTest {
        val latte = add(newItem("latte"))
        val mocha = add(newItem("mocha"))
        val seen = MutableStateFlow(emptyList<Cart>())
        val collector = launch(Dispatchers.IO) {
            repository.observeCart().collect { cart -> seen.update { it + cart } }
        }
        withContext(Dispatchers.IO) { withTimeout(5_000) { seen.first { it.isNotEmpty() } } }

        repository.updateSnapshots(listOf(renamed(latte, "nowa"), renamed(mocha, "nowa")))
        withContext(Dispatchers.IO) {
            withTimeout(5_000) { seen.first { carts -> carts.last().items.all { it.productName == "nowa" } } }
        }
        collector.cancel()

        assertEquals(emptyList(), seen.value.filter { cart -> cart.items.count { it.productName == "nowa" } == 1 })
    }
}
