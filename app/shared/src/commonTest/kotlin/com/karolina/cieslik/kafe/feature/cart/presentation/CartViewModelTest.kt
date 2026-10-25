package com.karolina.cieslik.kafe.feature.cart.presentation

import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.Money
import com.karolina.cieslik.kafe.core.domain.Result
import com.karolina.cieslik.kafe.core.testing.ViewModelTestContext
import com.karolina.cieslik.kafe.core.testing.runViewModelTest
import com.karolina.cieslik.kafe.feature.cart.FakeCartRepository
import com.karolina.cieslik.kafe.feature.cart.domain.Cart
import com.karolina.cieslik.kafe.feature.cart.domain.CartItem
import com.karolina.cieslik.kafe.feature.cart.domain.CartSelection
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

/** CartViewModel: stan ekranu, sprawdzenie z menu lokalu, kolejka zapisów, „Cofnij” i wyczyszczenie. */
class CartViewModelTest {

    private val krakow = cafe("krakow", name = "Kafe Rynek")
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

    private fun latte(sizePrice: Long = 1_600) = Product(
        id = "latte", categoryId = "coffee", name = "Caffè latte", description = null, imageUrl = "https://img/latte",
        sizes = listOf(ProductSize(SizeCode.M, 350, Money(sizePrice))), sortOrder = 1, isActive = true,
        modifierGroupIds = listOf("milk", "shots"),
    )

    private fun menu(sizePrice: Long = 1_600, unavailable: Set<String> = emptySet()) = Menu(
        categories = emptyList(),
        products = listOf(latte(sizePrice)),
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

    @Test
    fun emptyCartIsEmpty() = runViewModelTest { context ->
        assertEquals(CartUiState.Empty, setUp(context).viewModel.state.value)
    }

    @Test
    fun contentHasCafeLabelsAndSummary() = runViewModelTest { context ->
        val setup = setUp(context, item(1), item(2, selections = listOf(oat), quantity = 2, unitPrice = 1_800))

        assertEquals("Kafe Rynek", setup.content.cafeName)
        assertFalse(setup.content.pricesUnverified)
        assertEquals(
            CartItemUi(
                id = 1, name = "Caffè latte", imageUrl = "https://img/latte", sizeLabel = "Średni",
                modifiersLabel = "2× Shot espresso", quantity = 1, canIncrease = true,
                unitPriceLabel = Money(2_200).format(), totalPriceLabel = Money(2_200).format(),
                status = CartItemStatusUi.Ok,
            ),
            setup.content.items.first(),
        )
        assertEquals("Owsiane", setup.content.items[1].modifiersLabel)
        assertEquals(CartSummaryUi(3, Money(5_800).format()), setup.content.summary)
    }

    @Test
    fun onlyDefaultOptionWithoutSurchargeLeavesNoModifiersLabel() = runViewModelTest { context ->
        val setup = setUp(context, item(1, selections = listOf(cow), unitPrice = 1_600))

        assertNull(setup.content.items.single().modifiersLabel)
    }

    @Test
    fun increaseIsDisabledAtTwentyAndForUnavailableItem() = runViewModelTest { context ->
        val setup = setUp(
            context,
            item(1, quantity = 20),
            item(2, productId = "gone"),
            menus = FakeMenuRepository(cached = mapOf("krakow" to menu())),
        )

        assertEquals(listOf(false, false), setup.content.items.map { it.canIncrease })
        assertEquals(CartItemStatusUi.Unavailable, setup.content.items[1].status)
        assertEquals(20, setup.content.summary.itemCount)
    }

    @Test
    fun withoutSelectedCafeItemsHaveSnapshotPricesBannerAndNoCafeName() = runViewModelTest { context ->
        val setup = setUp(context, item(1, productId = "gone"), cafes = FakeCafeRepository(null))

        assertNull(setup.content.cafeName)
        assertTrue(setup.content.pricesUnverified)
        assertEquals(CartItemStatusUi.Ok, setup.content.items.single().status)
    }

    @Test
    fun bannerIsHiddenDuringFirstMenuDownloadAndShownAfterItFails() = runViewModelTest { context ->
        val gate = CompletableDeferred<Unit>()
        val menus = FakeMenuRepository(refreshResult = Result.Failure(DataError.Remote.NO_INTERNET)).apply {
            refreshGate = gate
        }
        val setup = setUp(context, item(1), menus = menus)

        assertFalse(setup.content.pricesUnverified)
        gate.complete(Unit)
        advanceUntilIdle()
        assertTrue(setup.content.pricesUnverified)
    }

    @Test
    fun failedRefreshWithSavedMenuShowsNoBannerAndWritesNothing() = runViewModelTest { context ->
        val menus = FakeMenuRepository(
            cached = mapOf("krakow" to menu(sizePrice = 1_700)),
            refreshResult = Result.Failure(DataError.Remote.NO_INTERNET),
        )
        val setup = setUp(context, item(1), menus = menus)

        assertFalse(setup.content.pricesUnverified)
        assertTrue(setup.cart.snapshotUpdates.isEmpty())
        assertEquals(CartItemStatusUi.PriceUpdated(Money(2_200).format()), setup.content.items.single().status)
    }

    @Test
    fun successfulRefreshSavesNewPricesOnceAndKeepsPriceMark() = runViewModelTest { context ->
        val menus = FakeMenuRepository(
            cached = mapOf("krakow" to menu()),
            menuAfterRefresh = mapOf("krakow" to menu(sizePrice = 1_700)),
        )
        val saved = setUp(context, item(1), menus = menus)

        assertEquals(1, saved.cart.snapshotUpdates.size)
        assertEquals(Money(2_300).format(), saved.content.items.single().unitPriceLabel)
        assertEquals(CartItemStatusUi.PriceUpdated(Money(2_200).format()), saved.content.items.single().status)
        assertEquals(listOf("krakow"), menus.refreshCalls)
    }

    @Test
    fun priceMarkDisappearsWithRemovedItem() = runViewModelTest { context ->
        val menus = FakeMenuRepository(
            cached = mapOf("krakow" to menu()),
            menuAfterRefresh = mapOf("krakow" to menu(sizePrice = 1_700)),
        )
        val setup = setUp(context, item(1), item(2, selections = listOf(oat), unitPrice = 1_800), menus = menus)

        act(setup, CartAction.RemoveClicked(1))

        assertEquals(listOf(2L), setup.content.items.map { it.id })
    }

    @Test
    fun rapidIncreasesAreAllApplied() = runViewModelTest { context ->
        val setup = setUp(context, item(1))
        val gate = CompletableDeferred<Unit>()
        setup.cart.writeGate = gate

        repeat(5) { setup.viewModel.onAction(CartAction.IncreaseClicked(1)) }
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(6, setup.content.items.single().quantity)
    }

    @Test
    fun rapidDecreaseOnLastPieceRemovesOnceWithoutFailure() = runViewModelTest { context ->
        val setup = setUp(context, item(1))

        act(setup, CartAction.DecreaseClicked(1), CartAction.DecreaseClicked(1))

        assertEquals(listOf<CartEvent>(CartEvent.ItemRemoved("Caffè latte")), setup.events)
        assertEquals(CartUiState.Empty, setup.viewModel.state.value)
    }

    @Test
    fun undoBringsRemovedItemBackToItsPlace() = runViewModelTest { context ->
        val setup = setUp(context, item(1), item(2, selections = listOf(oat), unitPrice = 1_800))

        act(setup, CartAction.RemoveClicked(1), CartAction.UndoClicked)

        assertEquals(listOf(1L, 2L), setup.content.items.map { it.id })
        assertEquals(listOf<CartEvent>(CartEvent.ItemRemoved("Caffè latte")), setup.events)
    }

    @Test
    fun undoAfterClearingCartRestoresNothing() = runViewModelTest { context ->
        val setup = setUp(context, item(1), item(2, selections = listOf(oat), unitPrice = 1_800))

        act(setup, CartAction.RemoveClicked(1), CartAction.ClearConfirmed, CartAction.UndoClicked)

        assertEquals(CartUiState.Empty, setup.viewModel.state.value)
        assertEquals(listOf(CartEvent.ItemRemoved("Caffè latte"), CartEvent.UndoExpired), setup.events)
    }

    @Test
    fun undoMergedAboveLimitReportsLimit() = runViewModelTest { context ->
        val setup = setUp(context, item(1, quantity = 15))
        act(setup, CartAction.RemoveClicked(1))
        setup.cart.cart.value = Cart(listOf(item(5, quantity = 10)))

        act(setup, CartAction.UndoClicked)

        assertEquals(CartEvent.RestoredAtLimit("Caffè latte"), setup.events.last())
        assertEquals(listOf(20), setup.content.items.map { it.quantity })
    }

    @Test
    fun writeErrorReportsActionFailedAndKeepsUndo() = runViewModelTest { context ->
        val setup = setUp(context, item(1))
        act(setup, CartAction.RemoveClicked(1))
        setup.cart.writeError = DataError.Local.DISK_FULL

        act(setup, CartAction.UndoClicked)
        setup.cart.writeError = null
        act(setup, CartAction.UndoClicked)

        assertEquals(CartEvent.ActionFailed, setup.events[1])
        assertEquals(listOf(1L), setup.content.items.map { it.id })
    }

    @Test
    fun clearDialogIsShownDismissedAndConfirmed() = runViewModelTest { context ->
        val setup = setUp(context, item(1))

        act(setup, CartAction.ClearClicked)
        assertTrue(setup.content.showClearConfirmation)
        act(setup, CartAction.ClearDismissed)
        assertFalse(setup.content.showClearConfirmation)
        act(setup, CartAction.ClearClicked, CartAction.ClearConfirmed)

        assertEquals(CartUiState.Empty, setup.viewModel.state.value)
    }

    @Test
    fun unreadableRowsAreRemovedOncePerViewModel() = runViewModelTest { context ->
        val setup = setUp(context, item(1))

        act(setup, CartAction.IncreaseClicked(1), CartAction.ClearClicked)

        assertEquals(1, setup.cart.removeUnreadableCalls)
    }

    @Test
    fun newViewModelAfterProcessDeathStartsWithoutDialogUndoOrPriceMarks() = runViewModelTest { context ->
        val cart = FakeCartRepository(Cart(listOf(item(1), item(2, selections = listOf(oat), unitPrice = 1_800))))
        val menus = FakeMenuRepository(
            cached = mapOf("krakow" to menu()),
            menuAfterRefresh = mapOf("krakow" to menu(sizePrice = 1_700)),
        )
        val before = setUp(context, cart = cart, menus = menus)
        act(before, CartAction.RemoveClicked(1), CartAction.ClearClicked)

        val after = setUp(context, cart = cart, menus = menus)
        act(after, CartAction.UndoClicked)

        assertFalse(after.content.showClearConfirmation)
        assertEquals(listOf(2L), after.content.items.map { it.id })
        assertEquals(CartItemStatusUi.Ok, after.content.items.single().status)
    }

    @Test
    fun navigationActionsEmitEvents() = runViewModelTest { context ->
        val setup = setUp(context)

        act(setup, CartAction.GoToMenuClicked, CartAction.BackClicked)

        assertEquals(listOf(CartEvent.NavigateToMenu, CartEvent.NavigateBack), setup.events)
    }
}
