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
import com.karolina.cieslik.kafe.feature.cart.domain.NewCartItem
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

        assertIs<AddToCartOutcome.Added>(result.outcome())
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

        val increased = assertIs<AddToCartOutcome.QuantityIncreased>(result.outcome())
        assertEquals(readded.id, increased.item.id)
        assertEquals(6, increased.item.quantity)
        assertEquals(Money(1_800), increased.item.unitPrice)
        assertEquals(1, repository.observeCart().first().items.size)
    }

    @Test
    fun restoreMergeAboveLimitIsCapped() = runTest {
        val removed = addedItem(newItem(quantity = 12))
        repository.remove(removed.id)
        addedItem(newItem(quantity = 12))

        val result = repository.restore(removed)

        val capped = assertIs<AddToCartOutcome.CappedAtLimit>(result.outcome())
        assertEquals(20, capped.item.quantity)
    }

    @Test
    fun restoreOfItemAlreadyInCartChangesNothing() = runTest {
        val item = addedItem(newItem(quantity = 3))
        repository.remove(item.id)
        repository.restore(item)

        val result = repository.restore(item)

        assertEquals(Result.Success(AddToCartOutcome.QuantityIncreased(item)), result)
        assertEquals(listOf(item), repository.observeCart().first().items)
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
