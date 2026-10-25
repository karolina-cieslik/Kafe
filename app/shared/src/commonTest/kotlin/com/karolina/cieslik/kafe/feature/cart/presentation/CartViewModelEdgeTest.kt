package com.karolina.cieslik.kafe.feature.cart.presentation

import androidx.lifecycle.viewModelScope
import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.Money
import com.karolina.cieslik.kafe.core.domain.Result
import com.karolina.cieslik.kafe.core.testing.ViewModelTestContext
import com.karolina.cieslik.kafe.core.testing.runViewModelTest
import com.karolina.cieslik.kafe.feature.cart.FakeCartRepository
import com.karolina.cieslik.kafe.feature.cart.domain.Cart
import com.karolina.cieslik.kafe.feature.cart.domain.CartItem
import com.karolina.cieslik.kafe.feature.cart.domain.CartRepository
import com.karolina.cieslik.kafe.feature.cart.domain.CartSelection
import com.karolina.cieslik.kafe.feature.cart.domain.DecreaseOutcome
import com.karolina.cieslik.kafe.feature.cart.domain.ObserveCheckedCart
import com.karolina.cieslik.kafe.feature.cart.domain.RefreshCartSnapshots
import com.karolina.cieslik.kafe.feature.menu.domain.Menu
import com.karolina.cieslik.kafe.feature.menu.domain.ModifierGroup
import com.karolina.cieslik.kafe.feature.menu.domain.ModifierGroupType
import com.karolina.cieslik.kafe.feature.menu.domain.ModifierOption
import com.karolina.cieslik.kafe.feature.menu.domain.Product
import com.karolina.cieslik.kafe.feature.menu.domain.ProductSize
import com.karolina.cieslik.kafe.feature.menu.domain.SizeCode
import com.karolina.cieslik.kafe.feature.menu.presentation.FakeCafeRepository
import com.karolina.cieslik.kafe.feature.menu.presentation.FakeMenuRepository
import com.karolina.cieslik.kafe.feature.menu.presentation.cafe
import com.karolina.cieslik.kafe.feature.product.domain.PriceCalculator
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * CartViewModel – przypadki brzegowe: kolejność szybkich kliknięć, „Cofnij” po zmianie lokalu i po nowych migawkach,
 * zmiana lokalu w trakcie pobierania menu, oznaczenie ceny przy niedostępności, etykiety opcji i stany początkowe.
 */
class CartViewModelEdgeTest {

    private val krakow = cafe("krakow", name = "Kafe Rynek")
    private val gdansk = cafe("gdansk", name = "Kafe Długa")
    private val milk = ModifierGroup(
        "milk", "Mleko", ModifierGroupType.SINGLE, isRequired = true, maxQuantity = 1,
        options = listOf(ModifierOption("cow", "Krowie", Money.ZERO), ModifierOption("oat", "Owsiane", Money(200))),
        defaultOptionId = "cow",
    )
    private val shots = ModifierGroup(
        "shots", "Dodatkowe espresso", ModifierGroupType.QUANTITY, isRequired = false, maxQuantity = 4,
        options = listOf(ModifierOption("shot", "Shot espresso", Money(300))),
    )
    private val cow = CartSelection("milk", "cow", "Krowie", Money.ZERO, 1, isDefault = true)
    private val oat = CartSelection("milk", "oat", "Owsiane", Money(200), 1)
    private val twoShots = CartSelection("shots", "shot", "Shot espresso", Money(300), 2)

    private fun menu(sizePrice: Long = 1_600, unavailable: Set<String> = emptySet()) = Menu(
        categories = emptyList(),
        products = listOf(
            Product(
                id = "latte", categoryId = "coffee", name = "Caffè latte", description = null, imageUrl = null,
                sizes = listOf(ProductSize(SizeCode.M, 350, Money(sizePrice))), sortOrder = 1, isActive = true,
                modifierGroupIds = listOf("milk", "shots"),
            ),
        ),
        unavailableProductIds = unavailable,
        modifierGroups = listOf(milk, shots),
    )

    private fun item(
        id: Long,
        selections: List<CartSelection> = listOf(cow, twoShots),
        quantity: Int = 1,
        unitPrice: Long = 2_200,
        productId: String = "latte",
    ) = CartItem(id, productId, "Caffè latte", SizeCode.M, selections, quantity, Money(unitPrice))

    private class Setup(
        val viewModel: CartViewModel,
        val cart: FakeCartRepository,
        val menus: FakeMenuRepository,
        val cafes: FakeCafeRepository,
        val events: MutableList<CartEvent>,
    )

    private fun TestScope.setUp(
        context: ViewModelTestContext,
        vararg items: CartItem,
        cafes: FakeCafeRepository = FakeCafeRepository(krakow),
        menus: FakeMenuRepository = FakeMenuRepository(cached = mapOf("krakow" to menu())),
        cart: FakeCartRepository = FakeCartRepository(Cart(items.toList())),
    ): Setup {
        val calculator = PriceCalculator()
        val viewModel = context.track(
            CartViewModel(
                cafes, menus, cart,
                ObserveCheckedCart(cart, menus, calculator),
                RefreshCartSnapshots(cart, menus, calculator),
            ),
        )
        val events = mutableListOf<CartEvent>()
        backgroundScope.launch { viewModel.state.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.collect { events += it } }
        advanceUntilIdle()
        return Setup(viewModel, cart, menus, cafes, events)
    }

    private val Setup.content get() = assertIs<CartUiState.Content>(viewModel.state.value)

    private fun TestScope.act(setup: Setup, vararg actions: CartAction) {
        actions.forEach { setup.viewModel.onAction(it) }
        advanceUntilIdle()
    }

    private fun TestScope.actWhileWritesAreBlocked(setup: Setup, vararg actions: CartAction) {
        val gate = CompletableDeferred<Unit>()
        setup.cart.writeGate = gate
        actions.forEach { setup.viewModel.onAction(it) }
        advanceUntilIdle()
        gate.complete(Unit)
        advanceUntilIdle()
        setup.cart.writeGate = null
    }

    @Test
    fun decreaseDecreaseIncreaseFromTwoRemovesItemAndSkipsLateIncrease() = runViewModelTest { context ->
        val setup = setUp(context, item(1, quantity = 2))

        actWhileWritesAreBlocked(
            setup,
            CartAction.DecreaseClicked(1),
            CartAction.DecreaseClicked(1),
            CartAction.IncreaseClicked(1),
        )

        assertEquals(CartUiState.Empty, setup.viewModel.state.value)
        assertEquals(listOf<CartEvent>(CartEvent.ItemRemoved("Caffè latte")), setup.events)
    }

    @Test
    fun slowFirstWriteIsNotOvertakenByLaterClick() = runViewModelTest { context ->
        val cart = FakeCartRepository(Cart(listOf(item(1))))
        val slowDecrease = object : CartRepository by cart {
            override suspend fun decreaseOrRemove(itemId: Long): Result<DecreaseOutcome, DataError.Local> {
                delay(SLOW_WRITE_MILLIS)
                return cart.decreaseOrRemove(itemId)
            }
        }
        val menus = FakeMenuRepository(cached = mapOf("krakow" to menu()))
        val viewModel = context.track(
            CartViewModel(
                FakeCafeRepository(krakow), menus, slowDecrease,
                ObserveCheckedCart(slowDecrease, menus, PriceCalculator()),
                RefreshCartSnapshots(slowDecrease, menus, PriceCalculator()),
            ),
        )
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.onAction(CartAction.DecreaseClicked(1))
        viewModel.onAction(CartAction.IncreaseClicked(1))
        advanceUntilIdle()

        assertEquals(CartUiState.Empty, viewModel.state.value)
    }

    @Test
    fun longMixedSeriesOfClicksEndsAtSumOfClicks() = runViewModelTest { context ->
        val setup = setUp(context, item(1), item(2, selections = listOf(oat), quantity = 5, unitPrice = 1_800))
        val clicks = listOf(
            CartAction.IncreaseClicked(1), CartAction.DecreaseClicked(2), CartAction.IncreaseClicked(1),
            CartAction.DecreaseClicked(1), CartAction.DecreaseClicked(2), CartAction.IncreaseClicked(1),
            CartAction.IncreaseClicked(2), CartAction.DecreaseClicked(2),
        )

        actWhileWritesAreBlocked(setup, *clicks.toTypedArray())

        assertEquals(listOf(1L to 3, 2L to 3), setup.content.items.map { it.id to it.quantity })
        assertTrue(setup.events.isEmpty())
    }

    @Test
    fun quickRemovalsOfTwoItemsThenUndoRestoresOnlyLastOne() = runViewModelTest { context ->
        val setup = setUp(context, item(1), item(2, selections = listOf(oat), unitPrice = 1_800), item(3))

        actWhileWritesAreBlocked(
            setup,
            CartAction.RemoveClicked(1),
            CartAction.RemoveClicked(2),
            CartAction.UndoClicked,
        )

        assertEquals(listOf(2L, 3L), setup.content.items.map { it.id })
        assertEquals(List<CartEvent>(2) { CartEvent.ItemRemoved("Caffè latte") }, setup.events)
    }

    @Test
    fun undoClickedWhileRemovalIsStillSavingRestoresItem() = runViewModelTest { context ->
        val setup = setUp(context, item(1), item(2, selections = listOf(oat), unitPrice = 1_800))

        actWhileWritesAreBlocked(setup, CartAction.RemoveClicked(1), CartAction.UndoClicked)

        assertEquals(listOf(1L, 2L), setup.content.items.map { it.id })
        assertEquals(listOf<CartEvent>(CartEvent.ItemRemoved("Caffè latte")), setup.events)
    }

    @Test
    fun itemRemovedWhileSnapshotsAreSavedStaysRemovedAndOtherKeepsPriceMark() = runViewModelTest { context ->
        val refreshGate = CompletableDeferred<Unit>()
        val menus = FakeMenuRepository(
            cached = mapOf("krakow" to menu()),
            menuAfterRefresh = mapOf("krakow" to menu(sizePrice = 1_700)),
        ).apply { this.refreshGate = refreshGate }
        val setup = setUp(context, item(1), item(2, selections = listOf(oat), unitPrice = 1_800), menus = menus)
        val writeGate = CompletableDeferred<Unit>()
        setup.cart.writeGate = writeGate

        // given: usunięcie czeka w kolejce zapisów przed nową migawką z odświeżenia
        setup.viewModel.onAction(CartAction.RemoveClicked(1))
        advanceUntilIdle()
        refreshGate.complete(Unit)
        advanceUntilIdle()
        // when
        writeGate.complete(Unit)
        advanceUntilIdle()

        // then
        assertEquals(listOf(listOf(1L, 2L)), setup.cart.snapshotUpdates.map { updates -> updates.map { it.item.id } })
        val remaining = setup.content.items.single()
        assertEquals(2L, remaining.id)
        assertEquals(CartItemStatusUi.PriceUpdated(Money(1_800).format()), remaining.status)
        assertEquals(Money(1_900).format(), remaining.unitPriceLabel)
    }

    @Test
    fun secondUndoAfterSuccessfulUndoDoesNothing() = runViewModelTest { context ->
        val setup = setUp(context, item(1, quantity = 3))

        act(setup, CartAction.RemoveClicked(1), CartAction.UndoClicked, CartAction.UndoClicked)

        assertEquals(listOf(1L to 3), setup.content.items.map { it.id to it.quantity })
        assertEquals(listOf<CartEvent>(CartEvent.ItemRemoved("Caffè latte")), setup.events)
    }

    @Test
    fun undoAfterCafeChangeRestoresItemCheckedWithNewCafe() = runViewModelTest { context ->
        val menus = FakeMenuRepository(
            cached = mapOf("krakow" to menu(), "gdansk" to menu(unavailable = setOf("latte"))),
        )
        val setup = setUp(context, item(1), menus = menus)
        act(setup, CartAction.RemoveClicked(1))

        setup.cafes.selectedCafe.value = gdansk
        advanceUntilIdle()
        act(setup, CartAction.UndoClicked)

        assertEquals("Kafe Długa", setup.content.cafeName)
        assertEquals(listOf(1L to CartItemStatusUi.Unavailable), setup.content.items.map { it.id to it.status })
        assertEquals(CartSummaryUi(0, Money.ZERO.format()), setup.content.summary)
    }

    @Test
    fun undoAfterNewSnapshotsBringsBackOldPriceMarkedAsUpdated() = runViewModelTest { context ->
        val gate = CompletableDeferred<Unit>()
        val menus = FakeMenuRepository(
            cached = mapOf("krakow" to menu()),
            menuAfterRefresh = mapOf("krakow" to menu(sizePrice = 1_700)),
        ).apply { refreshGate = gate }
        val setup = setUp(context, item(1), item(2, selections = listOf(oat), unitPrice = 1_800), menus = menus)
        act(setup, CartAction.RemoveClicked(1))

        gate.complete(Unit)
        advanceUntilIdle()
        act(setup, CartAction.UndoClicked)

        val (restored, other) = setup.content.items
        assertEquals(1L, restored.id)
        assertEquals(CartItemStatusUi.PriceUpdated(Money(2_200).format()), restored.status)
        assertEquals(Money(2_300).format(), restored.unitPriceLabel)
        assertEquals(CartItemStatusUi.PriceUpdated(Money(1_800).format()), other.status)
        assertEquals(listOf(listOf(2L)), setup.cart.snapshotUpdates.map { updates -> updates.map { it.item.id } })
    }

    @Test
    fun cafeChangeDuringMenuDownloadKeepsPriceMarksAndRefreshesEachCafeOnce() = runViewModelTest { context ->
        val gate = CompletableDeferred<Unit>()
        val menus = FakeMenuRepository(
            cached = mapOf("krakow" to menu(), "gdansk" to menu()),
            menuAfterRefresh = mapOf("krakow" to menu(sizePrice = 1_700), "gdansk" to menu(sizePrice = 1_700)),
        ).apply { refreshGate = gate }
        val setup = setUp(context, item(1), menus = menus)

        setup.cafes.selectedCafe.value = gdansk
        advanceUntilIdle()
        gate.complete(Unit)
        advanceUntilIdle()
        setup.cafes.selectedCafe.value = krakow
        advanceUntilIdle()

        assertEquals(listOf("krakow", "gdansk"), menus.refreshCalls)
        assertEquals(1, setup.cart.snapshotUpdates.size)
        val shown = setup.content.items.single()
        assertEquals(CartItemStatusUi.PriceUpdated(Money(2_200).format()), shown.status)
        assertEquals(Money(2_300).format(), shown.unitPriceLabel)
        assertFalse(setup.content.pricesUnverified)
    }

    @Test
    fun switchingToCafeWithoutMenuShowsBannerOnlyAfterItsDownloadFails() = runViewModelTest { context ->
        val gate = CompletableDeferred<Unit>()
        val menus = FakeMenuRepository(cached = mapOf("krakow" to menu()))
        val setup = setUp(context, item(1), menus = menus)
        menus.refreshResult = Result.Failure(DataError.Remote.NO_INTERNET)
        menus.refreshGate = gate

        setup.cafes.selectedCafe.value = gdansk
        advanceUntilIdle()
        assertFalse(setup.content.pricesUnverified)
        assertEquals(CartItemStatusUi.Ok, setup.content.items.single().status)
        gate.complete(Unit)
        advanceUntilIdle()
        assertTrue(setup.content.pricesUnverified)

        setup.cafes.selectedCafe.value = krakow
        advanceUntilIdle()
        assertFalse(setup.content.pricesUnverified)
        assertEquals(listOf("krakow", "gdansk"), menus.refreshCalls)
    }

    @Test
    fun priceMarkGivesWayToUnavailableAndReturnsWhenItemIsBack() = runViewModelTest { context ->
        val menus = FakeMenuRepository(
            cached = mapOf("krakow" to menu()),
            menuAfterRefresh = mapOf("krakow" to menu(sizePrice = 1_700)),
        )
        val setup = setUp(context, item(1, quantity = 2), menus = menus)
        assertEquals(CartItemStatusUi.PriceUpdated(Money(2_200).format()), setup.content.items.single().status)

        menus.setCached("krakow", menu(sizePrice = 1_700, unavailable = setOf("latte")))
        advanceUntilIdle()
        val unavailable = setup.content.items.single()
        assertEquals(CartItemStatusUi.Unavailable, unavailable.status)
        assertFalse(unavailable.canIncrease)
        assertEquals(CartSummaryUi(0, Money.ZERO.format()), setup.content.summary)

        menus.setCached("krakow", menu(sizePrice = 1_700))
        advanceUntilIdle()
        assertEquals(CartItemStatusUi.PriceUpdated(Money(2_200).format()), setup.content.items.single().status)
        assertEquals(CartSummaryUi(2, Money(4_600).format()), setup.content.summary)
    }

    @Test
    fun mixedCartSummaryCountsOnlyPayableItemsAtMenuPrices() = runViewModelTest { context ->
        val menus = FakeMenuRepository(
            cached = mapOf("krakow" to menu(sizePrice = 1_700)),
            refreshResult = Result.Failure(DataError.Remote.NO_INTERNET),
        )
        val setup = setUp(
            context,
            item(1, quantity = 2),
            item(2, productId = "gone", quantity = 5),
            item(3, selections = listOf(oat), quantity = 20, unitPrice = 1_800),
            menus = menus,
        )

        assertEquals(
            listOf(
                CartItemStatusUi.PriceUpdated(Money(2_200).format()),
                CartItemStatusUi.Unavailable,
                CartItemStatusUi.PriceUpdated(Money(1_800).format()),
            ),
            setup.content.items.map { it.status },
        )
        assertEquals(Money(38_000).format(), setup.content.items[2].totalPriceLabel)
        assertEquals(CartSummaryUi(22, Money(42_600).format()), setup.content.summary)
        assertEquals(listOf(true, false, false), setup.content.items.map { it.canIncrease })
    }

    @Test
    fun defaultOptionWithSurchargeIsShown() {
        val defaultOat = oat.copy(isDefault = true)

        assertEquals("Owsiane", listOf(defaultOat).toModifiersLabel())
    }

    @Test
    fun freeOptionThatIsNotDefaultIsShown() {
        assertEquals("Krowie", listOf(cow.copy(isDefault = false)).toModifiersLabel())
    }

    @Test
    fun modifiersKeepOrderAndOnlyQuantitiesAboveOneGetMultiplier() {
        val vanilla = CartSelection("syrup", "vanilla", "Waniliowy", Money(250), 1)

        assertEquals(
            "Owsiane, 2× Shot espresso, Waniliowy",
            listOf(cow, oat, twoShots, vanilla).toModifiersLabel(),
        )
        assertEquals("Shot espresso", listOf(twoShots.copy(quantity = 1)).toModifiersLabel())
        assertNull(emptyList<CartSelection>().toModifiersLabel())
    }

    @Test
    fun stateIsLoadingUntilFirstObservation() = runViewModelTest { context ->
        val cart = FakeCartRepository(Cart(listOf(item(1))))
        val menus = FakeMenuRepository(cached = mapOf("krakow" to menu()))
        val viewModel = context.track(
            CartViewModel(
                FakeCafeRepository(krakow), menus, cart,
                ObserveCheckedCart(cart, menus, PriceCalculator()),
                RefreshCartSnapshots(cart, menus, PriceCalculator()),
            ),
        )
        advanceUntilIdle()

        assertEquals(CartUiState.Loading, viewModel.state.value)
        assertTrue(menus.refreshCalls.isEmpty())
    }

    @Test
    fun emptyCartWithoutSelectedCafeIsEmptyAndDownloadsNothing() = runViewModelTest { context ->
        val menus = FakeMenuRepository()
        val setup = setUp(context, cafes = FakeCafeRepository(null), menus = menus)

        assertEquals(CartUiState.Empty, setup.viewModel.state.value)
        assertTrue(menus.refreshCalls.isEmpty())
    }

    @Test
    fun safetyFuseFailureOfUnreadableCleanupShowsCartWithoutEvent() = runViewModelTest { context ->
        val cart = FakeCartRepository(Cart(listOf(item(1)))).apply {
            removeUnreadableResult = Result.Failure(DataError.Local.UNKNOWN)
        }
        val setup = setUp(context, cart = cart)

        assertEquals(listOf(1L), setup.content.items.map { it.id })
        assertTrue(setup.events.isEmpty())
    }

    @Test
    fun removalQueuedWhenScreenClosesIsSavedWithoutEvent() = runViewModelTest { context ->
        val setup = setUp(context, item(1), item(2, selections = listOf(oat), unitPrice = 1_800))
        val gate = CompletableDeferred<Unit>()
        setup.cart.writeGate = gate

        setup.viewModel.onAction(CartAction.RemoveClicked(1))
        advanceUntilIdle()
        setup.viewModel.viewModelScope.cancel()
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(listOf(2L), setup.cart.cart.value.items.map { it.id })
        assertTrue(setup.events.isEmpty())
    }

    private companion object {
        const val SLOW_WRITE_MILLIS = 100L
    }
}
