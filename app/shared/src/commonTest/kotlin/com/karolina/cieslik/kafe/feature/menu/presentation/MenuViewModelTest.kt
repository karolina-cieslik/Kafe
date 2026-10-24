package com.karolina.cieslik.kafe.feature.menu.presentation

import app.cash.turbine.Event
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.Money
import com.karolina.cieslik.kafe.core.domain.Result
import com.karolina.cieslik.kafe.core.testing.ViewModelTestContext
import com.karolina.cieslik.kafe.core.testing.runViewModelTest
import com.karolina.cieslik.kafe.feature.cart.FakeCartRepository
import com.karolina.cieslik.kafe.feature.cart.domain.Cart
import com.karolina.cieslik.kafe.feature.cart.domain.CartItem
import com.karolina.cieslik.kafe.feature.menu.domain.GetMenuForCafe
import com.karolina.cieslik.kafe.feature.menu.domain.SizeCode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

// KAFE-6: MenuViewModel – ładowanie (AC 6), błąd i ponowienie (AC 7), offline-first (AC 8), kategorie, wyszukiwarka, nawigacja (AC 9).
class MenuViewModelTest {

    private val krakow = cafe("krakow", name = "Kafe Rynek")
    private val gdansk = cafe("gdansk", name = "Kafe Długa")

    private val sampleMenu = menu(
        categories = listOf(category("classic", 1, "Kawy klasyczne"), category("snacks", 2, "Przekąski")),
        products = listOf(
            product("espresso", "classic", 1, "Espresso", priceGrosze = 900, description = "Podwójne"),
            product("latte", "classic", 2, "Caffè latte", priceGrosze = 1_400),
            product("croissant", "snacks", 1, "Croissant maślany", priceGrosze = 950),
        ),
    )

    private fun ViewModelTestContext.createViewModel(
        cafeRepository: FakeCafeRepository = FakeCafeRepository(krakow),
        menuRepository: FakeMenuRepository,
        cartRepository: FakeCartRepository = FakeCartRepository(),
    ) = track(
        MenuViewModel(
            cafeRepository = cafeRepository,
            menuRepository = menuRepository,
            getMenuForCafe = GetMenuForCafe(menuRepository),
            cartRepository = cartRepository,
        ),
    )

    private fun cartOf(vararg quantities: Int) = Cart(
        quantities.mapIndexed { index, quantity ->
            CartItem(index + 1L, "latte", "Caffè latte", SizeCode.M, emptyList(), quantity, Money(1_400))
        },
    )

    @Test
    fun emptyCartGivesZeroItemCount() = runViewModelTest { context ->
        val viewModel = context.createViewModel(menuRepository = FakeMenuRepository(mapOf("krakow" to sampleMenu)))
        observe(viewModel)

        assertEquals(0, assertIs<MenuUiState.Content>(viewModel.state.value).cartItemCount)
    }

    @Test
    fun cartItemCountSumsQuantitiesAndFollowsCartChanges() = runViewModelTest { context ->
        val cart = FakeCartRepository(cartOf(2, 3))
        val viewModel = context.createViewModel(
            menuRepository = FakeMenuRepository(mapOf("krakow" to sampleMenu)),
            cartRepository = cart,
        )
        observe(viewModel)
        assertEquals(5, assertIs<MenuUiState.Content>(viewModel.state.value).cartItemCount)

        cart.cart.value = cartOf(2, 3, 20)
        advanceUntilIdle()

        assertEquals(25, assertIs<MenuUiState.Content>(viewModel.state.value).cartItemCount)
    }

    /** Subskrybuje stan w tle (jak UI) i dobiega do bezczynności – do testów stanu końcowego. */
    private fun TestScope.observe(viewModel: MenuViewModel) {
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()
    }

    // --- AC 6: pierwsze ładowanie ---

    @Test
    fun firstLoadWithoutCacheShowsLoadingThenContent() = runViewModelTest { context ->
        val repository = FakeMenuRepository(menuAfterRefresh = mapOf("krakow" to sampleMenu))
        val viewModel = context.createViewModel(menuRepository = repository)

        viewModel.state.test {
            assertEquals(MenuUiState.Loading, awaitItem())
            val content = assertIs<MenuUiState.Content>(expectMostRecentItemAfterIdle(context))
            assertEquals("Kafe Rynek", content.cafeName)
            assertEquals(listOf("classic", "snacks"), content.categories.map { it.id })
            assertFalse(content.isRefreshing)
        }
    }

    @Test
    fun firstLoadNeverShowsEmptyContentBeforeMenuArrives() = runViewModelTest { context ->
        val repository = FakeMenuRepository(menuAfterRefresh = mapOf("krakow" to sampleMenu))
        val viewModel = context.createViewModel(menuRepository = repository)

        viewModel.state.test {
            context.dispatcher.scheduler.advanceUntilIdle()
            val states = cancelAndConsumeRemainingEvents()
                .filterIsInstance<Event.Item<MenuUiState>>()
                .map { it.value }
            assertTrue(states.none { it is MenuUiState.Content && it.categories.isEmpty() })
        }
    }

    @Test
    fun noSelectedCafeKeepsLoading() = runViewModelTest { context ->
        val viewModel = context.createViewModel(
            cafeRepository = FakeCafeRepository(selected = null),
            menuRepository = FakeMenuRepository(menuAfterRefresh = mapOf("krakow" to sampleMenu)),
        )

        observe(viewModel)

        assertEquals(MenuUiState.Loading, viewModel.state.value)
    }

    // --- AC 7: brak sieci i brak cache ---

    @Test
    fun noInternetWithoutCacheShowsConnectionError() = runViewModelTest { context ->
        val repository = FakeMenuRepository(refreshResult = Result.Failure(DataError.Remote.NO_INTERNET))
        val viewModel = context.createViewModel(menuRepository = repository)

        observe(viewModel)

        assertEquals(MenuUiState.Error("Nie udało się pobrać menu. Sprawdź połączenie."), viewModel.state.value)
    }

    @Test
    fun timeoutWithoutCacheShowsConnectionError() = runViewModelTest { context ->
        val repository = FakeMenuRepository(refreshResult = Result.Failure(DataError.Remote.REQUEST_TIMEOUT))
        val viewModel = context.createViewModel(menuRepository = repository)

        observe(viewModel)

        assertEquals(MenuUiState.Error(MENU_ERROR_NO_CONNECTION), viewModel.state.value)
    }

    @Test
    fun serverErrorWithoutCacheShowsGenericError() = runViewModelTest { context ->
        val repository = FakeMenuRepository(refreshResult = Result.Failure(DataError.Remote.SERVER))
        val viewModel = context.createViewModel(menuRepository = repository)

        observe(viewModel)

        assertEquals(MenuUiState.Error(MENU_ERROR_GENERIC), viewModel.state.value)
    }

    @Test
    fun retryAfterErrorShowsLoadingThenContent() = runViewModelTest { context ->
        val repository = FakeMenuRepository(
            menuAfterRefresh = mapOf("krakow" to sampleMenu),
            refreshResult = Result.Failure(DataError.Remote.NO_INTERNET),
        )
        val viewModel = context.createViewModel(menuRepository = repository)

        viewModel.state.test {
            assertEquals(MenuUiState.Loading, awaitItem())
            assertIs<MenuUiState.Error>(awaitItem())

            repository.refreshResult = Result.Success(Unit)
            viewModel.onAction(MenuAction.Retry)

            assertEquals(MenuUiState.Loading, awaitItem())
            assertIs<MenuUiState.Content>(expectMostRecentItemAfterIdle(context))
        }
        assertEquals(listOf("krakow", "krakow"), repository.refreshCalls)
    }

    @Test
    fun retryThatFailsAgainShowsErrorAgain() = runViewModelTest { context ->
        val repository = FakeMenuRepository(refreshResult = Result.Failure(DataError.Remote.NO_INTERNET))
        val viewModel = context.createViewModel(menuRepository = repository)
        observe(viewModel)

        repository.refreshResult = Result.Failure(DataError.Remote.SERVER)
        viewModel.onAction(MenuAction.Retry)
        advanceUntilIdle()

        assertEquals(MenuUiState.Error(MENU_ERROR_GENERIC), viewModel.state.value)
    }

    // --- AC 8: zapamiętane menu od razu, odświeżenie w tle ---

    @Test
    fun cachedMenuIsShownImmediatelyWhileRefreshingInBackground() = runViewModelTest { context ->
        val repository = FakeMenuRepository(cached = mapOf("krakow" to sampleMenu))
        val gate = CompletableDeferred<Unit>().also { repository.refreshGate = it }
        val viewModel = context.createViewModel(menuRepository = repository)
        observe(viewModel)

        val whileRefreshing = assertIs<MenuUiState.Content>(viewModel.state.value)
        assertTrue(whileRefreshing.isRefreshing)
        assertEquals(3, whileRefreshing.categories.sumOf { it.products.size })

        gate.complete(Unit)
        advanceUntilIdle()

        assertFalse(assertIs<MenuUiState.Content>(viewModel.state.value).isRefreshing)
        assertEquals(listOf("krakow"), repository.refreshCalls)
    }

    @Test
    fun refreshErrorWithCachedMenuKeepsContentWithoutBlockingError() = runViewModelTest { context ->
        val repository = FakeMenuRepository(
            cached = mapOf("krakow" to sampleMenu),
            refreshResult = Result.Failure(DataError.Remote.SERVER),
        )
        val viewModel = context.createViewModel(menuRepository = repository)

        observe(viewModel)

        val content = assertIs<MenuUiState.Content>(viewModel.state.value)
        assertFalse(content.isRefreshing)
        assertEquals(listOf("classic", "snacks"), content.categories.map { it.id })
    }

    @Test
    fun productRemovedAfterRefreshDisappears() = runViewModelTest { context ->
        val withoutLatte = menu(sampleMenu.categories, sampleMenu.products.filterNot { it.id == "latte" })
        val repository = FakeMenuRepository(
            cached = mapOf("krakow" to sampleMenu),
            menuAfterRefresh = mapOf("krakow" to withoutLatte),
        )
        val viewModel = context.createViewModel(menuRepository = repository)

        observe(viewModel)

        val classic = assertIs<MenuUiState.Content>(viewModel.state.value).categories.first { it.id == "classic" }
        assertEquals(listOf("espresso"), classic.products.map { it.id })
    }

    @Test
    fun refreshActionStartsAnotherRefresh() = runViewModelTest { context ->
        val repository = FakeMenuRepository(cached = mapOf("krakow" to sampleMenu))
        val viewModel = context.createViewModel(menuRepository = repository)
        observe(viewModel)

        viewModel.onAction(MenuAction.Refresh)
        advanceUntilIdle()

        assertEquals(listOf("krakow", "krakow"), repository.refreshCalls)
    }

    @Test
    fun refreshAndRetryAreIgnoredWhileRefreshIsInProgress() = runViewModelTest { context ->
        val repository = FakeMenuRepository(cached = mapOf("krakow" to sampleMenu))
        val gate = CompletableDeferred<Unit>().also { repository.refreshGate = it }
        val viewModel = context.createViewModel(menuRepository = repository)
        observe(viewModel)

        viewModel.onAction(MenuAction.Refresh)
        viewModel.onAction(MenuAction.Retry)
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(listOf("krakow"), repository.refreshCalls)
    }

    // --- AC 2: mapowanie produktu ---

    @Test
    fun productIsMappedWithFormattedPriceAndSubtitle() = runViewModelTest { context ->
        val viewModel = context.createViewModel(menuRepository = FakeMenuRepository(cached = mapOf("krakow" to sampleMenu)))

        observe(viewModel)

        val espresso = assertIs<MenuUiState.Content>(viewModel.state.value).categories.first().products.first()
        assertEquals(
            ProductUi(id = "espresso", name = "Espresso", subtitle = "Podwójne", priceLabel = "9,00 zł", imageUrl = null, isAvailable = true),
            espresso,
        )
    }

    // --- Kategorie ---

    @Test
    fun firstCategoryIsSelectedByDefault() = runViewModelTest { context ->
        val viewModel = context.createViewModel(menuRepository = FakeMenuRepository(cached = mapOf("krakow" to sampleMenu)))

        observe(viewModel)

        assertEquals("classic", assertIs<MenuUiState.Content>(viewModel.state.value).selectedCategoryId)
    }

    @Test
    fun categorySelectedChangesSelection() = runViewModelTest { context ->
        val viewModel = context.createViewModel(menuRepository = FakeMenuRepository(cached = mapOf("krakow" to sampleMenu)))
        observe(viewModel)

        viewModel.onAction(MenuAction.CategorySelected("snacks"))
        advanceUntilIdle()

        assertEquals("snacks", assertIs<MenuUiState.Content>(viewModel.state.value).selectedCategoryId)
    }

    @Test
    fun unknownCategoryFallsBackToFirst() = runViewModelTest { context ->
        val viewModel = context.createViewModel(menuRepository = FakeMenuRepository(cached = mapOf("krakow" to sampleMenu)))
        observe(viewModel)

        viewModel.onAction(MenuAction.CategorySelected("doesNotExist"))
        advanceUntilIdle()

        assertEquals("classic", assertIs<MenuUiState.Content>(viewModel.state.value).selectedCategoryId)
    }

    // --- Wyszukiwarka ---

    @Test
    fun searchFiltersProductsByNameIgnoringCaseAndWhitespace() = runViewModelTest { context ->
        val viewModel = context.createViewModel(menuRepository = FakeMenuRepository(cached = mapOf("krakow" to sampleMenu)))
        observe(viewModel)

        viewModel.onAction(MenuAction.SearchQueryChanged("  LATTE "))
        advanceUntilIdle()

        val content = assertIs<MenuUiState.Content>(viewModel.state.value)
        assertEquals(listOf("classic"), content.categories.map { it.id })
        assertEquals(listOf("latte"), content.categories.single().products.map { it.id })
        assertEquals("  LATTE ", content.searchQuery)
    }

    @Test
    fun searchHidingSelectedCategoryFallsBackToFirstVisible() = runViewModelTest { context ->
        val viewModel = context.createViewModel(menuRepository = FakeMenuRepository(cached = mapOf("krakow" to sampleMenu)))
        observe(viewModel)

        viewModel.onAction(MenuAction.CategorySelected("classic"))
        viewModel.onAction(MenuAction.SearchQueryChanged("croissant"))
        advanceUntilIdle()

        assertEquals("snacks", assertIs<MenuUiState.Content>(viewModel.state.value).selectedCategoryId)
    }

    @Test
    fun searchWithoutMatchesGivesEmptyContent() = runViewModelTest { context ->
        val viewModel = context.createViewModel(menuRepository = FakeMenuRepository(cached = mapOf("krakow" to sampleMenu)))
        observe(viewModel)

        viewModel.onAction(MenuAction.SearchQueryChanged("matcha"))
        advanceUntilIdle()

        val content = assertIs<MenuUiState.Content>(viewModel.state.value)
        assertTrue(content.categories.isEmpty())
        assertNull(content.selectedCategoryId)
    }

    @Test
    fun clearingSearchRestoresFullMenu() = runViewModelTest { context ->
        val viewModel = context.createViewModel(menuRepository = FakeMenuRepository(cached = mapOf("krakow" to sampleMenu)))
        observe(viewModel)

        viewModel.onAction(MenuAction.SearchQueryChanged("latte"))
        viewModel.onAction(MenuAction.SearchQueryChanged(""))
        advanceUntilIdle()

        assertEquals(3, assertIs<MenuUiState.Content>(viewModel.state.value).categories.sumOf { it.products.size })
    }

    // --- AC 9 i zmiana lokalu: zdarzenia nawigacyjne ---

    @Test
    fun productClickedEmitsNavigateToProduct() = runViewModelTest { context ->
        val viewModel = context.createViewModel(menuRepository = FakeMenuRepository(cached = mapOf("krakow" to sampleMenu)))

        viewModel.events.test {
            viewModel.onAction(MenuAction.ProductClicked("latte"))
            assertEquals(MenuEvent.NavigateToProduct("latte"), awaitItem())
        }
    }

    @Test
    fun changeCafeClickedEmitsNavigateToCafePicker() = runViewModelTest { context ->
        val viewModel = context.createViewModel(menuRepository = FakeMenuRepository(cached = mapOf("krakow" to sampleMenu)))

        viewModel.events.test {
            viewModel.onAction(MenuAction.ChangeCafeClicked)
            assertEquals(MenuEvent.NavigateToCafePicker, awaitItem())
        }
    }

    // --- Dostępność per lokal i zmiana lokalu ---

    @Test
    fun productUnavailableInCafeIsMarkedUnavailable() = runViewModelTest { context ->
        val gdanskMenu = menu(sampleMenu.categories, sampleMenu.products, unavailableProductIds = setOf("latte"))
        val viewModel = context.createViewModel(
            cafeRepository = FakeCafeRepository(gdansk),
            menuRepository = FakeMenuRepository(cached = mapOf("gdansk" to gdanskMenu)),
        )

        observe(viewModel)

        val classic = assertIs<MenuUiState.Content>(viewModel.state.value).categories.first { it.id == "classic" }
        assertEquals(listOf(true, false), classic.products.map { it.isAvailable })
    }

    @Test
    fun changingCafeShowsLoadingThenMenuOfNewCafe() = runViewModelTest { context ->
        val cafes = FakeCafeRepository(krakow)
        val gdanskMenu = menu(sampleMenu.categories, sampleMenu.products, unavailableProductIds = setOf("latte"))
        val repository = FakeMenuRepository(
            cached = mapOf("krakow" to sampleMenu),
            menuAfterRefresh = mapOf("gdansk" to gdanskMenu),
        )
        val viewModel = context.createViewModel(cafeRepository = cafes, menuRepository = repository)

        viewModel.state.test {
            assertEquals("Kafe Rynek", assertIs<MenuUiState.Content>(expectMostRecentItemAfterIdle(context)).cafeName)

            cafes.selectedCafe.value = gdansk

            assertEquals(MenuUiState.Loading, awaitItem())
            val content = assertIs<MenuUiState.Content>(expectMostRecentItemAfterIdle(context))
            assertEquals("Kafe Długa", content.cafeName)
            assertFalse(content.categories.first().products.first { it.id == "latte" }.isAvailable)
        }
        assertEquals(listOf("krakow", "gdansk"), repository.refreshCalls)
    }

    @Test
    fun refreshErrorOfPreviousCafeDoesNotLeakToNewCafe() = runViewModelTest { context ->
        val cafes = FakeCafeRepository(krakow)
        val repository = FakeMenuRepository(refreshResult = Result.Failure(DataError.Remote.NO_INTERNET))
        val viewModel = context.createViewModel(cafeRepository = cafes, menuRepository = repository)
        observe(viewModel)
        assertIs<MenuUiState.Error>(viewModel.state.value)

        repository.refreshResult = Result.Success(Unit)
        repository.menuAfterRefresh = mapOf("gdansk" to sampleMenu)
        cafes.selectedCafe.value = gdansk
        advanceUntilIdle()

        assertEquals("Kafe Długa", assertIs<MenuUiState.Content>(viewModel.state.value).cafeName)
    }

    /** Dobiega do bezczynności schedulera i zwraca ostatni stan (pomija pośrednie emisje, np. isRefreshing=true). */
    private fun ReceiveTurbine<MenuUiState>.expectMostRecentItemAfterIdle(context: ViewModelTestContext): MenuUiState {
        context.dispatcher.scheduler.advanceUntilIdle()
        return expectMostRecentItem()
    }
}
