package com.karolina.cieslik.kafe.feature.cart

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.karolina.cieslik.kafe.core.data.database.KafeDatabase
import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.Money
import com.karolina.cieslik.kafe.core.domain.Result
import com.karolina.cieslik.kafe.feature.cart.data.RoomCartRepository
import com.karolina.cieslik.kafe.feature.cart.domain.CartItemStatus
import com.karolina.cieslik.kafe.feature.cart.domain.CartSelection
import com.karolina.cieslik.kafe.feature.cart.domain.CheckedCart
import com.karolina.cieslik.kafe.feature.cart.domain.NewCartItem
import com.karolina.cieslik.kafe.feature.cart.domain.ObserveCheckedCart
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
import kotlin.test.Test
import kotlin.test.assertEquals

/** ObserveCheckedCart na prawdziwym Room: zapis menu w trakcie obserwacji nie daje fałszywych statusów pozycji. */
@RunWith(AndroidJUnit4::class)
class ObserveCheckedCartDeviceTest {

    private lateinit var database: KafeDatabase
    private val remote = AlternatingRemote()

    private val milk = ModifierGroupDto(
        "milk", "Mleko", "SINGLE", required = true, defaultOptionId = "cow",
        options = listOf(ModifierOptionDto("cow", "Krowie", 0), ModifierOptionDto("oat", "Owsiane", 200)),
    )
    private val latte = ProductDto(
        "latte", "coffee", "Caffè latte",
        sizes = listOf(ProductSizeDto("S", 250, 1_400), ProductSizeDto("M", 350, 1_600)),
        modifierGroupIds = listOf("milk"),
    )
    private val menuA = MenuDto(
        categories = listOf(CategoryDto("coffee", "Kawy", 1)),
        products = listOf(
            latte,
            ProductDto("espresso", "coffee", "Espresso", sizes = listOf(ProductSizeDto("S", 40, 900))),
        ),
        modifierGroups = listOf(milk),
    )
    private val menuB = MenuDto(
        categories = listOf(CategoryDto("coffee", "Kawy", 1), CategoryDto("tea", "Herbaty", 2)),
        products = listOf(
            latte,
            ProductDto("tea", "tea", "Herbata", sizes = listOf(ProductSizeDto("M", 300, 1_100))),
        ),
        modifierGroups = listOf(
            milk,
            ModifierGroupDto("lemon", "Cytryna", "SINGLE", options = listOf(ModifierOptionDto("slice", "Plaster", 0))),
        ),
    )

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder<KafeDatabase>(context)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun savingMenuWhileObservingNeverMarksUnchangedItemAsChangedOrUnavailable() = runTest {
        val menus = OfflineFirstMenuRepository(RoomMenuLocalDataSource(database.menuDao()), remote)
        val cart = RoomCartRepository(database.cartDao())
        val observe = ObserveCheckedCart(cart, menus, PriceCalculator())
        val oat = CartSelection("milk", "oat", "Owsiane", Money(200), 1)
        cart.add(NewCartItem("latte", "Caffè latte", SizeCode.M, listOf(oat), quantity = 2, unitPrice = Money(1_800)))
        menus.refresh("cafe")
        val emissions = MutableStateFlow<List<CheckedCart>>(emptyList())
        val collector = launch(Dispatchers.IO) {
            observe("cafe").collect { checked -> emissions.update { it + checked } }
        }
        withContext(Dispatchers.IO) { withTimeout(5_000) { emissions.first { it.isNotEmpty() } } }

        repeat(SAVE_ROUNDS) { menus.refresh("cafe") }
        withContext(Dispatchers.IO) { delay(QUIET_MILLIS) }
        collector.cancel()

        val statuses = emissions.value.flatMap { checked -> checked.items.map { it.status to it.currentUnitPrice } }
        assertEquals(listOf(CartItemStatus.Available to Money(1_800)), statuses.distinct())
    }

    /** Zdalne menu na zmianę A i B – latte w obu takie samo, różnią się inne produkty, kategorie i grupy. */
    private inner class AlternatingRemote : MenuRemoteDataSource {
        private var calls = 0

        override suspend fun fetchMenu(): Result<MenuDto, DataError.Remote> =
            Result.Success(if (calls++ % 2 == 0) menuA else menuB)

        override suspend fun fetchAvailability(cafeId: String): Result<AvailabilityDto, DataError.Remote> =
            Result.Success(AvailabilityDto())
    }

    private companion object {
        const val SAVE_ROUNDS = 20
        const val QUIET_MILLIS = 300L
    }
}
