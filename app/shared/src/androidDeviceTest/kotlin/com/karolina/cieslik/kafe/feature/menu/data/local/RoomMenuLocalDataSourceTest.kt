package com.karolina.cieslik.kafe.feature.menu.data.local

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.karolina.cieslik.kafe.core.data.database.KafeDatabase
import com.karolina.cieslik.kafe.feature.menu.data.remote.CategoryDto
import com.karolina.cieslik.kafe.feature.menu.data.remote.MenuDto
import com.karolina.cieslik.kafe.feature.menu.data.remote.ProductDto
import com.karolina.cieslik.kafe.feature.menu.data.remote.ProductSizeDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

// KAFE-6: Lokalne źródło menu na prawdziwym SQLite (baza w pamięci).
@RunWith(AndroidJUnit4::class)
class RoomMenuLocalDataSourceTest {

    private lateinit var database: KafeDatabase
    private lateinit var dataSource: RoomMenuLocalDataSource

    private val menu = MenuDto(
        categories = listOf(CategoryDto("c", "Kawy", 1)),
        products = listOf(
            ProductDto("latte", "c", "Latte", sizes = listOf(ProductSizeDto("S", 250, 1400), ProductSizeDto("M", 350, 1600))),
            ProductDto("espresso", "c", "Espresso", sizes = listOf(ProductSizeDto("S", 40, 900))),
        ),
    )

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder<KafeDatabase>(context)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        dataSource = RoomMenuLocalDataSource(database.menuDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun menuIsNullBeforeAnySave() = runTest {
        assertNull(dataSource.observeMenu("cafe").first())
    }

    @Test
    fun savedMenuIsReadBackWithSizesAndAvailability() = runTest {
        dataSource.saveMenu("cafe", menu, mapOf("espresso" to false), syncedAtEpochMillis = 1)

        val saved = assertNotNull(dataSource.observeMenu("cafe").first())
        assertEquals(setOf("latte", "espresso"), saved.products.map { it.id }.toSet())
        assertEquals(2, saved.products.single { it.id == "latte" }.sizes.size)
        assertEquals(setOf("espresso"), saved.unavailableProductIds)
    }

    @Test
    fun menuIsNullForCafeWithoutSavedAvailability() = runTest {
        dataSource.saveMenu("cafe", menu, emptyMap(), syncedAtEpochMillis = 1)

        assertNull(dataSource.observeMenu("other-cafe").first())
    }

    @Test
    fun productRemovedFromMenuDisappearsAfterNextSave() = runTest {
        dataSource.saveMenu("cafe", menu, emptyMap(), syncedAtEpochMillis = 1)
        dataSource.saveMenu("cafe", menu.copy(products = menu.products.take(1)), emptyMap(), syncedAtEpochMillis = 2)

        val saved = assertNotNull(dataSource.observeMenu("cafe").first())
        assertEquals(listOf("latte"), saved.products.map { it.id })
    }

    @Test
    fun savingOneCafeKeepsAvailabilityOfAnother() = runTest {
        dataSource.saveMenu("a", menu, mapOf("latte" to false), syncedAtEpochMillis = 1)
        dataSource.saveMenu("b", menu, emptyMap(), syncedAtEpochMillis = 2)

        assertEquals(setOf("latte"), assertNotNull(dataSource.observeMenu("a").first()).unavailableProductIds)
    }
}
