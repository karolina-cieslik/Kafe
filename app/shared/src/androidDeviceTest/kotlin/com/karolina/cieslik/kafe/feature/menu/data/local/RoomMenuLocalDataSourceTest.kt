package com.karolina.cieslik.kafe.feature.menu.data.local

import androidx.room.Room
import androidx.room.execSQL
import androidx.room.useWriterConnection
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
import com.karolina.cieslik.kafe.feature.menu.domain.Menu
import com.karolina.cieslik.kafe.feature.menu.domain.ModifierGroupType
import com.karolina.cieslik.kafe.feature.menu.domain.SizeCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** Lokalne źródło menu na prawdziwym SQLite (baza w pamięci). */
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
    fun observerNeverSeesHalfWrittenMenu() = runTest {
        val menuB = MenuDto(
            categories = listOf(CategoryDto("d", "Herbaty", 1)),
            products = listOf(
                ProductDto(
                    "tea", "d", "Herbata",
                    sizes = listOf(ProductSizeDto("M", 300, 1100)),
                    modifierGroupIds = listOf("milk"),
                ),
            ),
            modifierGroups = listOf(
                ModifierGroupDto("milk", "Mleko", "SINGLE", options = listOf(ModifierOptionDto("cow", "Krowie", 0))),
            ),
        )
        val availabilityA = AvailabilityDto(products = mapOf("espresso" to false))
        val availabilityB = AvailabilityDto(modifierOptions = mapOf("cow" to false))
        dataSource.saveMenu("cafe", menuB, availabilityB, syncedAtEpochMillis = 1)
        val expectedB = assertNotNull(dataSource.observeMenu("cafe").first())
        dataSource.saveMenu("cafe", menu, availabilityA, syncedAtEpochMillis = 2)
        val expectedA = assertNotNull(dataSource.observeMenu("cafe").first())
        val emissions = MutableStateFlow<List<Menu?>>(emptyList())
        val collector = launch(Dispatchers.IO) {
            dataSource.observeMenu("cafe").collect { menu -> emissions.update { it + menu } }
        }

        repeat(SAVE_ROUNDS) { round ->
            val (dto, availability) = if (round % 2 == 0) menuB to availabilityB else menu to availabilityA
            dataSource.saveMenu("cafe", dto, availability, syncedAtEpochMillis = round + 3L)
        }
        val last = if ((SAVE_ROUNDS - 1) % 2 == 0) expectedB else expectedA
        withContext(Dispatchers.IO) { withTimeout(5_000) { emissions.first { it.lastOrNull() == last } } }
        collector.cancel()

        assertEquals(emptyList(), emissions.value.filterNot { it == expectedA || it == expectedB })
    }

    @Test
    fun priceChangeWithSameRowCountIsEmitted() = runTest {
        dataSource.saveMenu("cafe", menu, AvailabilityDto(), syncedAtEpochMillis = 1)
        val repriced = menu.copy(
            products = menu.products.map { product ->
                if (product.id != "latte") return@map product
                product.copy(sizes = product.sizes.map { it.copy(priceGrosze = it.priceGrosze + 100) })
            },
        )

        val emissions = collectWhile(dataSource.observeMenu("cafe")) {
            dataSource.saveMenu("cafe", repriced, AvailabilityDto(), syncedAtEpochMillis = 2)
        }

        assertEquals(listOf(1500L, 1700L), emissions.last()?.latteSizePrices())
    }

    @Test
    fun changeOnlyInRelationTableIsEmitted() = runTest {
        val groups = listOf(
            ModifierGroupDto("milk", "Mleko", "SINGLE", options = listOf(ModifierOptionDto("cow", "Krowie", 0))),
            ModifierGroupDto(
                "syrup", "Syrop", "SINGLE",
                options = listOf(ModifierOptionDto("vanilla", "Waniliowy", 150)),
            ),
        )
        fun withLatteGroup(groupId: String) = menu.copy(
            products = menu.products.map { if (it.id == "latte") it.copy(modifierGroupIds = listOf(groupId)) else it },
            modifierGroups = groups,
        )
        dataSource.saveMenu("cafe", withLatteGroup("milk"), AvailabilityDto(), syncedAtEpochMillis = 1)

        // Zapis tylko do tabeli relacji – saveMenu przepisuje wszystkie tabele, więc nie sprawdziłby wyzwalacza.
        val emissions = collectWhile(dataSource.observeMenu("cafe")) {
            database.useWriterConnection { connection ->
                connection.execSQL(
                    "UPDATE menu_product_modifier_groups SET groupId = 'syrup' WHERE productId = 'latte'",
                )
            }
        }

        assertEquals(listOf("syrup"), emissions.last()?.products?.single { it.id == "latte" }?.modifierGroupIds)
    }

    /**
     * Zbiera emisje [flow] w tle, wykonuje [write] i czeka, aż strumień się uspokoi (brak nowych emisji przez
     * [QUIET_MILLIS]) – zwraca wszystkie zebrane wartości.
     */
    private suspend fun <T> TestScope.collectWhile(flow: Flow<T>, write: suspend () -> Unit): List<T> {
        val emissions = MutableStateFlow<List<T>>(emptyList())
        val collector = launch(Dispatchers.IO) { flow.collect { value -> emissions.update { it + value } } }
        withContext(Dispatchers.IO) { withTimeout(5_000) { emissions.first { it.isNotEmpty() } } }
        write()
        withContext(Dispatchers.IO) {
            withTimeout(5_000) { emissions.first { it.size >= 2 } }
            delay(QUIET_MILLIS)
        }
        collector.cancel()
        return emissions.value
    }

    private fun Menu.latteSizePrices() = products.single { it.id == "latte" }.sizes.map { it.price.grosze }

    @Test
    fun savingOneCafeKeepsAvailabilityOfAnother() = runTest {
        dataSource.saveMenu("a", menu, AvailabilityDto(products = mapOf("latte" to false)), syncedAtEpochMillis = 1)
        dataSource.saveMenu("b", menu, AvailabilityDto(), syncedAtEpochMillis = 2)

        assertEquals(setOf("latte"), assertNotNull(dataSource.observeMenu("a").first()).unavailableProductIds)
    }

    /** Modyfikatory, domyślny rozmiar i dostępność rozmiarów oraz opcji (schemat v3). */
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

    private companion object {
        const val SAVE_ROUNDS = 20
        const val QUIET_MILLIS = 300L
    }
}
