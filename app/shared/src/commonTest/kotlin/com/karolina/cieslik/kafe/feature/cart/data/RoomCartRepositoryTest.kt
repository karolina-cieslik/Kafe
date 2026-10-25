package com.karolina.cieslik.kafe.feature.cart.data

import app.cash.turbine.test
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

/**
 * Scalanie, limit 20, „Cofnij” i odczyt koszyka – logika DAO na tabeli w pamięci (Room testuje CartDaoTest
 * na urządzeniu).
 */
class RoomCartRepositoryTest {

    private val dao = InMemoryCartDao()
    private val repository = RoomCartRepository(dao)

    private val oat = CartSelection("milk", "oat", "Owsiane", Money(200), 1)

    private fun newItem(
        productId: String = "latte",
        quantity: Int = 1,
        selections: List<CartSelection> = listOf(oat),
        name: String = "Caffè latte",
        unitPrice: Money = Money(1_600),
    ) = NewCartItem(productId, name, SizeCode.M, selections, quantity, unitPrice)

    private suspend fun addedItem(item: NewCartItem): CartItem = repository.add(item).outcome().item

    private fun Result<AddToCartOutcome, DataError.Local>.outcome(): AddToCartOutcome =
        assertIs<Result.Success<AddToCartOutcome>>(this).data

    private fun Result<RestoreOutcome, DataError.Local>.restored(): RestoreOutcome =
        assertIs<Result.Success<RestoreOutcome>>(this).data

    private fun snapshotOf(item: CartItem, name: String, price: Money, oatName: String, oatSurcharge: Money) =
        CartSnapshotUpdate(
            item = item,
            productName = name,
            unitPrice = price,
            options = mapOf(OptionRef("milk", "oat") to OptionSnapshot(oatName, oatSurcharge)),
        )

    @Test
    fun emptyCartEmitsEmpty() = runTest {
        assertEquals(Cart.EMPTY, repository.observeCart().first())
    }

    @Test
    fun newConfigurationIsAddedAtTheEnd() = runTest {
        addedItem(newItem(productId = "latte"))

        val result = repository.add(newItem(productId = "flat-white", quantity = 2))

        val added = assertIs<AddToCartOutcome.Added>(result.outcome())
        assertEquals(2, added.item.quantity)
        assertEquals(listOf("latte", "flat-white"), repository.observeCart().first().items.map { it.productId })
    }

    @Test
    fun identicalConfigurationIncreasesQuantityAndRefreshesSnapshot() = runTest {
        val first = addedItem(newItem(quantity = 2))

        val result = repository.add(
            newItem(
                quantity = 3,
                name = "Latte",
                unitPrice = Money(1_700),
                selections = listOf(oat.copy(surcharge = Money(250))),
            ),
        )

        val increased = assertIs<AddToCartOutcome.QuantityIncreased>(result.outcome())
        assertEquals(first.id, increased.item.id)
        assertEquals(5, increased.item.quantity)
        assertEquals("Latte", increased.item.productName)
        assertEquals(Money(1_700), increased.item.unitPrice)
        assertEquals(Money(250), increased.item.selections.single().surcharge)
        assertEquals(1, repository.observeCart().first().items.size)
    }

    @Test
    fun mergeAboveLimitIsCappedAtTwenty() = runTest {
        addedItem(newItem(quantity = 15))

        val result = repository.add(newItem(quantity = 10))

        val capped = assertIs<AddToCartOutcome.CappedAtLimit>(result.outcome())
        assertEquals(20, capped.item.quantity)
    }

    @Test
    fun mergeExactlyToLimitIsNotCapped() = runTest {
        addedItem(newItem(quantity = 15))

        val result = repository.add(newItem(quantity = 5))

        assertIs<AddToCartOutcome.QuantityIncreased>(result.outcome())
    }

    @Test
    fun setQuantityClampsFromAboveOnly() = runTest {
        val item = addedItem(newItem())

        assertEquals(Result.Success(Unit), repository.setQuantity(item.id, 25))
        assertEquals(20, repository.observeCart().first().items.single().quantity)
        assertEquals(Result.Success(Unit), repository.setQuantity(item.id, 1))
        assertEquals(1, repository.observeCart().first().items.single().quantity)
    }

    @Test
    fun setQuantityBelowOneIsCallerError() = runTest {
        val item = addedItem(newItem())

        assertFailsWith<IllegalArgumentException> { repository.setQuantity(item.id, 0) }
    }

    @Test
    fun setQuantityOfMissingItemFailsWithNotFound() = runTest {
        assertEquals(Result.Failure(DataError.Local.NOT_FOUND), repository.setQuantity(42, 2))
    }

    @Test
    fun removeReturnsRemovedItemAndMissingFailsWithNotFound() = runTest {
        val item = addedItem(newItem())

        assertEquals(Result.Success(item), repository.remove(item.id))
        assertEquals(Cart.EMPTY, repository.observeCart().first())
        assertEquals(Result.Failure(DataError.Local.NOT_FOUND), repository.remove(item.id))
    }

    @Test
    fun restoreBringsItemBackToItsPlace() = runTest {
        val first = addedItem(newItem(productId = "a"))
        val second = addedItem(newItem(productId = "b"))
        addedItem(newItem(productId = "c"))
        repository.remove(second.id)

        val result = repository.restore(second)

        assertEquals(RestoreOutcome.Restored(second), result.restored())
        val items = repository.observeCart().first().items
        assertEquals(listOf("a", "b", "c"), items.map { it.productId })
        assertEquals(listOf(first.id, second.id), items.take(2).map { it.id })
    }

    @Test
    fun restoreMergesIntoItemAddedInTheMeantime() = runTest {
        val removed = addedItem(newItem(quantity = 4))
        repository.remove(removed.id)
        val readded = addedItem(newItem(quantity = 2, unitPrice = Money(1_800)))

        val result = repository.restore(removed)

        val merged = assertIs<RestoreOutcome.Merged>(result.restored())
        assertEquals(false, merged.capped)
        assertEquals(readded.id, merged.item.id)
        assertEquals(6, merged.item.quantity)
        assertEquals(Money(1_800), merged.item.unitPrice)
        assertEquals(1, repository.observeCart().first().items.size)
    }

    @Test
    fun restoreMergeAboveLimitIsCapped() = runTest {
        val removed = addedItem(newItem(quantity = 12))
        repository.remove(removed.id)
        addedItem(newItem(quantity = 12))

        val result = repository.restore(removed)

        val merged = assertIs<RestoreOutcome.Merged>(result.restored())
        assertEquals(true, merged.capped)
        assertEquals(20, merged.item.quantity)
    }

    @Test
    fun restoreOfItemAlreadyInCartChangesNothing() = runTest {
        val item = addedItem(newItem(quantity = 3))
        repository.remove(item.id)
        repository.restore(item)

        val result = repository.restore(item)

        assertEquals(Result.Success(RestoreOutcome.AlreadyInCart(item)), result)
        assertEquals(listOf(item), repository.observeCart().first().items)
    }

    @Test
    fun increaseAddsOneUpToTwenty() = runTest {
        val item = addedItem(newItem(quantity = 19))

        assertEquals(20, assertIs<Result.Success<CartItem>>(repository.increase(item.id)).data.quantity)
        assertEquals(20, assertIs<Result.Success<CartItem>>(repository.increase(item.id)).data.quantity)
    }

    @Test
    fun decreaseAboveOneDecreasesAndAtOneRemoves() = runTest {
        val item = addedItem(newItem(quantity = 2))

        val decreased = repository.decreaseOrRemove(item.id)
        val removed = repository.decreaseOrRemove(item.id)

        assertEquals(Result.Success(DecreaseOutcome.Decreased(item.copy(quantity = 1))), decreased)
        assertEquals(Result.Success(DecreaseOutcome.Removed(item.copy(quantity = 1))), removed)
        assertEquals(Cart.EMPTY, repository.observeCart().first())
    }

    @Test
    fun increaseAndDecreaseOfMissingItemAreNotFound() = runTest {
        assertEquals(Result.Failure(DataError.Local.NOT_FOUND), repository.increase(42))
        assertEquals(Result.Failure(DataError.Local.NOT_FOUND), repository.decreaseOrRemove(42))
    }

    @Test
    fun updateSnapshotsChangesNamesAndPricesButKeepsQuantityAndKey() = runTest {
        val item = addedItem(newItem(quantity = 3))
        repository.increase(item.id)
        val update = snapshotOf(item, "Latte", Money(1_700), "Owsiane bio", Money(300))

        val result = repository.updateSnapshots(listOf(update))

        assertEquals(Result.Success(Unit), result)
        assertEquals(
            item.copy(
                productName = "Latte",
                unitPrice = Money(1_700),
                quantity = 4,
                selections = listOf(oat.copy(optionName = "Owsiane bio", surcharge = Money(300))),
            ),
            repository.observeCart().first().items.single(),
        )
        assertEquals(item.configurationKey, dao.rowOf(item).configurationKey)
    }

    @Test
    fun updateSnapshotsWithDifferentOptionsIsRejectedWithoutWriting() = runTest {
        val item = addedItem(newItem())
        val wrong = snapshotOf(item, "Latte", Money(1_700), "Owsiane", Money(200))
            .copy(options = mapOf(OptionRef("milk", "soy") to OptionSnapshot("Sojowe", Money(200))))

        assertFailsWith<IllegalArgumentException> { repository.updateSnapshots(listOf(wrong)) }
        assertEquals(listOf(item), repository.observeCart().first().items)
    }

    @Test
    fun updateSnapshotsSkipsItemRemovedInTheMeantime() = runTest {
        val removed = addedItem(newItem(productId = "a"))
        val kept = addedItem(newItem(productId = "b"))
        repository.remove(removed.id)

        val result = repository.updateSnapshots(
            listOf(
                snapshotOf(removed, "A", Money(1_000), "Owsiane", Money(200)),
                snapshotOf(kept, "B", Money(1_100), "Owsiane", Money(200)),
            ),
        )

        assertEquals(Result.Success(Unit), result)
        assertEquals(listOf("B"), repository.observeCart().first().items.map { it.productName })
    }

    @Test
    fun removeUnreadableDeletesOnlyUnreadableRows() = runTest {
        val good = addedItem(newItem())
        dao.seed(dao.rowOf(good).copy(id = 100, sizeCode = "XL", configurationKey = "x"))
        dao.seed(dao.rowOf(good).copy(id = 101, selectionsJson = "{nie json", configurationKey = "y"))

        assertEquals(Result.Success(2), repository.removeUnreadable())
        assertEquals(listOf(good.id), dao.observeItems().first().map { it.id })
    }

    @Test
    fun removeUnreadableKeepsRowsWhenAllAreUnreadable() = runTest {
        val good = addedItem(newItem())
        val broken = dao.rowOf(good).copy(sizeCode = "XL")
        repository.clear()
        dao.seed(broken)

        assertEquals(Result.Failure(DataError.Local.UNKNOWN), repository.removeUnreadable())
        assertEquals(listOf(broken), dao.observeItems().first())
    }

    @Test
    fun removeUnreadableOnEmptyCartRemovesNothing() = runTest {
        assertEquals(Result.Success(0), repository.removeUnreadable())
    }

    @Test
    fun defaultFlagOfSelectionIsStoredExplicitlyAndReadBack() = runTest {
        val cow = CartSelection("milk", "cow", "Krowie", Money.ZERO, 1, isDefault = true)
        val item = addedItem(newItem(selections = listOf(cow, oat.copy(groupId = "extra"))))

        val stored = repository.observeCart().first().items.single()
        assertEquals(listOf(true, false), stored.selections.map { it.isDefault })
        assertEquals(2, Regex("\"isDefault\":").findAll(dao.rowOf(item).selectionsJson).count())
    }

    @Test
    fun clearRemovesAllItems() = runTest {
        addedItem(newItem(productId = "a"))
        addedItem(newItem(productId = "b"))

        assertEquals(Result.Success(Unit), repository.clear())
        assertEquals(Cart.EMPTY, repository.observeCart().first())
    }

    @Test
    fun unreadableRowsAreSkipped() = runTest {
        val good = addedItem(newItem())
        dao.seed(dao.rowOf(good).copy(id = 100, sizeCode = "XL", configurationKey = "x"))
        dao.seed(dao.rowOf(good).copy(id = 101, selectionsJson = "{nie json", configurationKey = "y"))
        dao.seed(dao.rowOf(good).copy(id = 102, selectionsJson = """[{"groupId":"milk"}]""", configurationKey = "z"))

        assertEquals(listOf(good), repository.observeCart().first().items)
    }

    @Test
    fun observeCartEmitsAfterEveryChange() = runTest {
        repository.observeCart().map { it.itemCount }.test {
            assertEquals(0, awaitItem())
            val item = addedItem(newItem(quantity = 2))
            assertEquals(2, awaitItem())
            repository.setQuantity(item.id, 5)
            assertEquals(5, awaitItem())
            repository.remove(item.id)
            assertEquals(0, awaitItem())
        }
    }

    @Test
    fun storageErrorIsMappedToDataError() = runTest {
        dao.failWith = IllegalStateException("disk")

        assertEquals(Result.Failure(DataError.Local.UNKNOWN), repository.add(newItem()))
    }

    /** Tabela `cart_items` w pamięci: AUTOINCREMENT bez ponownego użycia id i unikalny klucz konfiguracji. */
    private class InMemoryCartDao : CartDao() {
        private val rows = MutableStateFlow<List<CartItemEntity>>(emptyList())
        private var lastId = 0L
        var failWith: Exception? = null

        fun seed(row: CartItemEntity) = insertRow(row)

        fun rowOf(item: CartItem): CartItemEntity = rows.value.first { it.id == item.id }

        override fun observeItems(): Flow<List<CartItemEntity>> = rows.map { list -> list.sortedBy { it.id } }

        override suspend fun updateQuantity(id: Long, quantity: Int): Int {
            val row = rows.value.firstOrNull { it.id == id } ?: return 0
            update(row.copy(quantity = quantity))
            return 1
        }

        override suspend fun deleteAll() {
            rows.value = emptyList()
        }

        override suspend fun findByKey(configurationKey: String): CartItemEntity? {
            failWith?.let { throw it }
            return rows.value.firstOrNull { it.configurationKey == configurationKey }
        }

        override suspend fun findById(id: Long): CartItemEntity? = rows.value.firstOrNull { it.id == id }

        override suspend fun insert(item: CartItemEntity): Long = insertRow(item)

        override suspend fun update(item: CartItemEntity) {
            rows.value = rows.value.map { if (it.id == item.id) item else it }
        }

        override suspend fun deleteById(id: Long) {
            rows.value = rows.value.filterNot { it.id == id }
        }

        override suspend fun incrementQuantity(id: Long, max: Int): Int {
            val row = rows.value.firstOrNull { it.id == id } ?: return 0
            update(row.copy(quantity = minOf(row.quantity + 1, max)))
            return 1
        }

        override suspend fun updateSnapshot(
            id: Long,
            configurationKey: String,
            productName: String,
            selectionsJson: String,
            unitPriceGrosze: Long,
        ) {
            val row = rows.value.firstOrNull { it.id == id && it.configurationKey == configurationKey } ?: return
            update(
                row.copy(productName = productName, selectionsJson = selectionsJson, unitPriceGrosze = unitPriceGrosze),
            )
        }

        override suspend fun getAll(): List<CartItemEntity> = rows.value

        private fun insertRow(item: CartItemEntity): Long {
            val id = if (item.id == 0L) lastId + 1 else item.id
            check(rows.value.none { it.id == id || it.configurationKey == item.configurationKey }) {
                "UNIQUE constraint failed"
            }
            lastId = maxOf(lastId, id)
            rows.value = rows.value + item.copy(id = id)
            return id
        }
    }
}
