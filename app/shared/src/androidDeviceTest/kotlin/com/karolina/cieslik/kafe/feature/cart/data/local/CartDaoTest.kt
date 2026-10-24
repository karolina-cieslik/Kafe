package com.karolina.cieslik.kafe.feature.cart.data.local

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.karolina.cieslik.kafe.core.data.database.KafeDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** DAO koszyka na prawdziwym SQLite: transakcje, unikalny klucz konfiguracji i AUTOINCREMENT. */
@RunWith(AndroidJUnit4::class)
class CartDaoTest {

    private lateinit var database: KafeDatabase
    private lateinit var dao: CartDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder<KafeDatabase>(context)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        dao = database.cartDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun row(key: String, quantity: Int = 1, id: Long = 0) = CartItemEntity(
        id = id,
        productId = key,
        productName = "Caffè latte",
        sizeCode = "M",
        selectionsJson = "[]",
        configurationKey = key,
        quantity = quantity,
        unitPriceGrosze = 1_400,
    )

    @Test
    fun parallelAddsOfSameConfigurationCreateOneItem() = runTest {
        (1..10).map { async(Dispatchers.IO) { dao.add(row("latte")) } }.awaitAll()

        val items = dao.observeItems().first()
        assertEquals(1, items.size)
        assertEquals(10, items.single().quantity)
    }

    @Test
    fun addMergesAndCapsAtTwenty() = runTest {
        dao.add(row("latte", quantity = 15))

        val write = dao.add(row("latte", quantity = 10))

        assertEquals(CartWriteKind.CAPPED, write.kind)
        assertEquals(20, dao.observeItems().first().single().quantity)
    }

    @Test
    fun removedIdIsNotReusedAndRestoreKeepsPlace() = runTest {
        val first = dao.add(row("a")).item
        val second = dao.add(row("b")).item
        dao.remove(second.id)
        val third = dao.add(row("c")).item

        dao.restore(second)

        assertEquals(second.id + 1, third.id)
        assertEquals(listOf(first.id, second.id, third.id), dao.observeItems().first().map { it.id })
    }

    @Test
    fun restoreOfItemAlreadyInCartChangesNothing() = runTest {
        val item = dao.add(row("latte", quantity = 3)).item
        dao.remove(item.id)
        dao.restore(item)

        val write = dao.restore(item)

        assertEquals(CartWrite(item, CartWriteKind.INCREASED), write)
        assertEquals(listOf(item), dao.observeItems().first())
    }

    @Test
    fun restoreMergesIntoItemAddedInTheMeantime() = runTest {
        val removed = dao.add(row("latte", quantity = 4)).item
        dao.remove(removed.id)
        val readded = dao.add(row("latte", quantity = 2)).item

        val write = dao.restore(removed)

        assertEquals(CartWrite(readded.copy(quantity = 6), CartWriteKind.INCREASED), write)
        assertEquals(listOf(readded.copy(quantity = 6)), dao.observeItems().first())
    }

    @Test
    fun removeOfMissingItemReturnsNull() = runTest {
        assertNull(dao.remove(42))
        assertEquals(0, dao.updateQuantity(42, 3))
    }
}
