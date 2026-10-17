package com.karolina.cieslik.kafe.feature.menu.data.local

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.karolina.cieslik.kafe.core.data.database.KafeDatabase
import com.karolina.cieslik.kafe.feature.menu.data.remote.AvailabilityDto
import com.karolina.cieslik.kafe.feature.menu.data.remote.CategoryDto
import com.karolina.cieslik.kafe.feature.menu.data.remote.MenuDto
import com.karolina.cieslik.kafe.feature.menu.data.remote.ModifierGroupDto
import com.karolina.cieslik.kafe.feature.menu.data.remote.ModifierOptionDto
import com.karolina.cieslik.kafe.feature.menu.data.remote.ProductDto
import com.karolina.cieslik.kafe.feature.menu.data.remote.ProductSizeDto
import com.karolina.cieslik.kafe.feature.menu.domain.ModifierGroupType
import com.karolina.cieslik.kafe.feature.menu.domain.SizeCode
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
        dataSource.saveMenu("cafe", menu, AvailabilityDto(products = mapOf("espresso" to false)), syncedAtEpochMillis = 1)

        val saved = assertNotNull(dataSource.observeMenu("cafe").first())
        assertEquals(setOf("latte", "espresso"), saved.products.map { it.id }.toSet())
        assertEquals(2, saved.products.single { it.id == "latte" }.sizes.size)
        assertEquals(setOf("espresso"), saved.unavailableProductIds)
    }

    @Test
    fun menuIsNullForCafeWithoutSavedAvailability() = runTest {
        dataSource.saveMenu("cafe", menu, AvailabilityDto(), syncedAtEpochMillis = 1)

        assertNull(dataSource.observeMenu("other-cafe").first())
    }

    @Test
    fun productRemovedFromMenuDisappearsAfterNextSave() = runTest {
        dataSource.saveMenu("cafe", menu, AvailabilityDto(), syncedAtEpochMillis = 1)
        dataSource.saveMenu("cafe", menu.copy(products = menu.products.take(1)), AvailabilityDto(), syncedAtEpochMillis = 2)

        val saved = assertNotNull(dataSource.observeMenu("cafe").first())
        assertEquals(listOf("latte"), saved.products.map { it.id })
    }

    @Test
    fun savingOneCafeKeepsAvailabilityOfAnother() = runTest {
        dataSource.saveMenu("a", menu, AvailabilityDto(products = mapOf("latte" to false)), syncedAtEpochMillis = 1)
        dataSource.saveMenu("b", menu, AvailabilityDto(), syncedAtEpochMillis = 2)

        assertEquals(setOf("latte"), assertNotNull(dataSource.observeMenu("a").first()).unavailableProductIds)
    }

    // KAFE-9 / KAFE-10: modyfikatory, domyślny rozmiar i dostępność rozmiarów oraz opcji (schemat v3).
    @Test
    fun modifiersDefaultSizeAndSizeAndOptionAvailabilityAreReadBack() = runTest {
        val withModifiers = menu.copy(
            products = listOf(
                ProductDto(
                    "latte", "c", "Latte",
                    sizes = listOf(ProductSizeDto("S", 250, 1400), ProductSizeDto("M", 350, 1600)),
                    defaultSize = "m",
                    modifierGroupIds = listOf("shots", "milk"),
                ),
            ),
            modifierGroups = listOf(
                ModifierGroupDto(
                    "milk", "Mleko", "SINGLE", required = true, defaultOptionId = "cow",
                    options = listOf(ModifierOptionDto("oat", "Owsiane", 200, 2), ModifierOptionDto("cow", "Krowie", 0, 1)),
                ),
                ModifierGroupDto("shots", "Shoty", "QUANTITY", maxQuantity = 4, options = listOf(ModifierOptionDto("shot", "Shot", 300))),
            ),
        )
        val availability = AvailabilityDto(
            sizes = mapOf("latte" to mapOf("s" to false, "M" to true)),
            modifierOptions = mapOf("oat" to false),
        )

        dataSource.saveMenu("cafe", withModifiers, availability, syncedAtEpochMillis = 1)

        val saved = assertNotNull(dataSource.observeMenu("cafe").first())
        val latte = saved.products.single()
        assertEquals(SizeCode.M, latte.defaultSize)
        assertEquals(listOf("shots", "milk"), latte.modifierGroupIds)
        val milk = saved.modifierGroups.single { it.id == "milk" }
        assertEquals(listOf("cow", "oat"), milk.options.map { it.id })
        assertEquals(ModifierGroupType.QUANTITY, saved.modifierGroups.single { it.id == "shots" }.type)
        assertEquals(mapOf("latte" to setOf(SizeCode.S)), saved.unavailableSizes)
        assertEquals(setOf("oat"), saved.unavailableOptionIds)
    }
}
