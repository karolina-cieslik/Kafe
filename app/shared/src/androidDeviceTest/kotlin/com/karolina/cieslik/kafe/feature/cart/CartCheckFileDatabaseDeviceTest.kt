package com.karolina.cieslik.kafe.feature.cart

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.karolina.cieslik.kafe.core.data.database.KafeDatabase
import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.Money
import com.karolina.cieslik.kafe.core.domain.Result
import com.karolina.cieslik.kafe.feature.cart.data.RoomCartRepository
import com.karolina.cieslik.kafe.feature.cart.domain.CartItem
import com.karolina.cieslik.kafe.feature.cart.domain.CartItemStatus
import com.karolina.cieslik.kafe.feature.cart.domain.CartRepository
import com.karolina.cieslik.kafe.feature.cart.domain.CartSelection
import com.karolina.cieslik.kafe.feature.cart.domain.CartSnapshotUpdate
import com.karolina.cieslik.kafe.feature.cart.domain.CheckedCart
import com.karolina.cieslik.kafe.feature.cart.domain.NewCartItem
import com.karolina.cieslik.kafe.feature.cart.domain.ObserveCheckedCart
import com.karolina.cieslik.kafe.feature.cart.domain.PriceUpdate
import com.karolina.cieslik.kafe.feature.cart.domain.RefreshCartSnapshots
import com.karolina.cieslik.kafe.feature.menu.data.OfflineFirstMenuRepository
import com.karolina.cieslik.kafe.feature.menu.data.local.RoomMenuLocalDataSource
import com.karolina.cieslik.kafe.feature.menu.data.remote.AvailabilityDto
import com.karolina.cieslik.kafe.feature.menu.data.remote.CategoryDto
import com.karolina.cieslik.kafe.feature.menu.data.remote.MenuDto
import com.karolina.cieslik.kafe.feature.menu.data.remote.MenuRemoteDataSource
import com.karolina.cieslik.kafe.feature.menu.data.remote.ModifierGroupDto
import com.karolina.cieslik.kafe.feature.menu.data.remote.ModifierOptionDto
import com.karolina.cieslik.kafe.feature.menu.data.remote.ProductDto
import com.karolina.cieslik.kafe.feature.menu.data.remote.ProductSizeDto
import com.karolina.cieslik.kafe.feature.menu.domain.SizeCode
import com.karolina.cieslik.kafe.feature.product.domain.PriceCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Sprawdzanie koszyka na bazie plikowej w trybie WAL – jak w aplikacji: kilka połączeń, odczyty równolegle z zapisem.
 * Zapis menu w trakcie obserwacji i zmiany koszyka albo menu w trakcie zapisu nowej migawki.
 */
@RunWith(AndroidJUnit4::class)
class CartCheckFileDatabaseDeviceTest {

    private lateinit var dbFile: File
    private lateinit var database: KafeDatabase
    private lateinit var cart: RoomCartRepository
    private lateinit var menus: OfflineFirstMenuRepository
    private val remote = SwitchableRemote()

    private val oat = CartSelection("milk", "oat", "Owsiane", Money(200), 1)

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        dbFile = File(context.cacheDir, "cart-check-wal.db")
        deleteDatabaseFiles()
        database = Room.databaseBuilder<KafeDatabase>(context, dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .build()
        cart = RoomCartRepository(database.cartDao())
        menus = OfflineFirstMenuRepository(RoomMenuLocalDataSource(database.menuDao()), remote)
    }

    @After
    fun tearDown() {
        database.close()
        deleteDatabaseFiles()
    }

    private fun deleteDatabaseFiles() {
        listOf("", "-wal", "-shm", "-journal").forEach { File(dbFile.path + it).delete() }
    }

    private fun menu(sizeMGrosze: Long, oatGrosze: Long, extraProduct: String, fillers: Int = 0) = MenuDto(
        categories = listOf(CategoryDto("coffee", "Kawy", 1)),
        products = listOf(
            ProductDto(
                "latte", "coffee", "Caffè latte",
                sizes = listOf(ProductSizeDto("M", 350, sizeMGrosze)),
                modifierGroupIds = listOf("milk"),
            ),
            ProductDto(extraProduct, "coffee", extraProduct, sizes = listOf(ProductSizeDto("S", 100, 900))),
        ) + (1..fillers).map {
            ProductDto("$extraProduct-$it", "coffee", "Wypełniacz $it", sizes = listOf(ProductSizeDto("S", 100, 900)))
        },
        modifierGroups = listOf(
            ModifierGroupDto(
                "milk", "Mleko", "SINGLE", required = true, defaultOptionId = "cow",
                options = listOf(
                    ModifierOptionDto("cow", "Krowie", 0),
                    ModifierOptionDto("oat", "Owsiane", oatGrosze),
                ),
            ),
        ),
    )

    private suspend fun addLatte(quantity: Int = 2, unitPrice: Money = Money(1_800)): CartItem {
        val added = cart.add(NewCartItem("latte", "Caffè latte", SizeCode.M, listOf(oat), quantity, unitPrice))
        return (added as Result.Success).data.item
    }

    private suspend fun checkedNow(): CheckedCart = ObserveCheckedCart(cart, menus, PriceCalculator())("cafe").first()

    private fun CheckedCart.single() = items.single().let { it.status to it.currentUnitPrice }

    @Test
    fun savingMenuWhileObservingNeverMixesProductsOfOneMenuWithGroupsOfAnother() = runTest {
        // given: w obu menu latte z owsianym kosztuje 18 zł, ale z innego podziału na rozmiar i dopłatę –
        // produkt z jednego zapisu i grupy z drugiego dałyby 17 albo 19 zł
        val menuA = menu(sizeMGrosze = 1_600, oatGrosze = 200, extraProduct = "espresso", fillers = FILLERS)
        val menuB = menu(sizeMGrosze = 1_700, oatGrosze = 100, extraProduct = "tea", fillers = FILLERS)
        remote.menus = listOf(menuA, menuB)
        addLatte()
        menus.refresh("cafe")
        val emissions = MutableStateFlow<List<CheckedCart>>(emptyList())
        val collector = launch(Dispatchers.IO) {
            ObserveCheckedCart(cart, menus, PriceCalculator())("cafe").collect { checked ->
                emissions.update { it + checked }
            }
        }
        withContext(Dispatchers.IO) { withTimeout(TIMEOUT_MILLIS) { emissions.first { it.isNotEmpty() } } }

        // when
        withContext(Dispatchers.IO) { repeat(SAVE_ROUNDS) { menus.refresh("cafe") } }
        withContext(Dispatchers.IO) { delay(QUIET_MILLIS) }
        collector.cancel()

        // then
        val statuses = emissions.value.flatMap { checked -> checked.items.map { it.status to it.currentUnitPrice } }
        assertEquals(listOf(CartItemStatus.Available to Money(1_800)), statuses.distinct())
    }

    @Test
    fun quantityChangedWhileSavingSnapshotsIsKept() = runTest {
        remote.menus = listOf(menu(sizeMGrosze = 1_700, oatGrosze = 200, extraProduct = "espresso"))
        menus.refresh("cafe")
        val latte = addLatte(quantity = 2)
        val increasedMeanwhile = object : CartRepository by cart {
            override suspend fun updateSnapshots(updates: List<CartSnapshotUpdate>) =
                cart.increase(latte.id).let { cart.updateSnapshots(updates) }
        }

        val result = RefreshCartSnapshots(increasedMeanwhile, menus, PriceCalculator())("cafe")

        assertEquals(Result.Success(listOf(PriceUpdate(latte.id, Money(1_800), Money(1_900)))), result)
        val stored = cart.observeCart().first().items.single()
        assertEquals(3 to Money(1_900), stored.quantity to stored.unitPrice)
    }

    @Test
    fun itemRemovedWhileSavingSnapshotsStaysRemoved() = runTest {
        remote.menus = listOf(menu(sizeMGrosze = 1_700, oatGrosze = 200, extraProduct = "espresso"))
        menus.refresh("cafe")
        val latte = addLatte()
        val removedMeanwhile = object : CartRepository by cart {
            override suspend fun updateSnapshots(updates: List<CartSnapshotUpdate>) =
                cart.remove(latte.id).let { cart.updateSnapshots(updates) }
        }

        val result = RefreshCartSnapshots(removedMeanwhile, menus, PriceCalculator())("cafe")

        assertIs<Result.Success<List<PriceUpdate>>>(result)
        assertEquals(emptyList(), cart.observeCart().first().items)
    }

    @Test
    fun snapshotFollowsMenuChangedAfterPreviousRefresh() = runTest {
        remote.menus = listOf(menu(sizeMGrosze = 1_700, oatGrosze = 200, extraProduct = "espresso"))
        menus.refresh("cafe")
        val latte = addLatte()
        val refresh = RefreshCartSnapshots(cart, menus, PriceCalculator())
        refresh("cafe")
        assertEquals(CartItemStatus.Available to Money(1_900), checkedNow().single())

        remote.menus = listOf(menu(sizeMGrosze = 1_500, oatGrosze = 250, extraProduct = "espresso"))
        menus.refresh("cafe")
        assertEquals(CartItemStatus.PriceChanged(Money(1_900)), checkedNow().items.single().status)

        assertEquals(Result.Success(listOf(PriceUpdate(latte.id, Money(1_900), Money(1_750)))), refresh("cafe"))
        val after = checkedNow()
        assertEquals(CartItemStatus.Available to Money(1_750), after.single())
        assertEquals(Money(3_500), after.payableTotal)
    }

    /** Zdalne menu: kolejne pobrania zwracają po kolei [menus] (w kółko); dostępność bez ograniczeń. */
    private class SwitchableRemote : MenuRemoteDataSource {
        var menus: List<MenuDto> = emptyList()
        private var calls = 0

        override suspend fun fetchMenu(): Result<MenuDto, DataError.Remote> =
            Result.Success(menus[calls++ % menus.size])

        override suspend fun fetchAvailability(cafeId: String): Result<AvailabilityDto, DataError.Remote> =
            Result.Success(AvailabilityDto())
    }

    private companion object {
        const val SAVE_ROUNDS = 100
        const val FILLERS = 300
        const val QUIET_MILLIS = 300L
        const val TIMEOUT_MILLIS = 5_000L
    }
}
