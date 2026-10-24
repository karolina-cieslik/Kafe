package com.karolina.cieslik.kafe.feature.menu.presentation

import app.cash.turbine.test
import com.karolina.cieslik.kafe.core.domain.Money
import com.karolina.cieslik.kafe.core.testing.ViewModelTestContext
import com.karolina.cieslik.kafe.core.testing.runViewModelTest
import com.karolina.cieslik.kafe.feature.cart.FakeCartRepository
import com.karolina.cieslik.kafe.feature.cart.domain.Cart
import com.karolina.cieslik.kafe.feature.cart.domain.CartItem
import com.karolina.cieslik.kafe.feature.menu.domain.GetMenuForCafe
import com.karolina.cieslik.kafe.feature.menu.domain.SizeCode
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/** Licznik koszyka w menu: niezależny od lokalu, wyszukiwania i chwili, w której przychodzi menu. */
class MenuViewModelCartEdgeTest {

    private val krakow = cafe("krakow", name = "Kafe Rynek")
    private val sampleMenu = menu(
        categories = listOf(category("classic", sortOrder = 1)),
        products = listOf(product("latte", "classic", name = "Caffè latte")),
    )

    private fun cartOf(vararg quantities: Int) = Cart(
        quantities.mapIndexed { index, quantity ->
            CartItem(index + 1L, "p$index", "Produkt $index", SizeCode.M, emptyList(), quantity, Money(1_400))
        },
    )

    private fun ViewModelTestContext.createViewModel(
        cafes: FakeCafeRepository = FakeCafeRepository(krakow),
        menus: FakeMenuRepository = FakeMenuRepository(mapOf("krakow" to sampleMenu, "gdansk" to sampleMenu)),
        cart: FakeCartRepository,
    ) = track(MenuViewModel(cafes, menus, GetMenuForCafe(menus), cart))

    private fun TestScope.observe(viewModel: MenuViewModel) {
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()
    }

    private val MenuViewModel.content get() = assertIs<MenuUiState.Content>(state.value)

    @Test
    fun cartItemCountStaysAfterChangingCafe() = runViewModelTest { context ->
        val cafes = FakeCafeRepository(krakow)
        val viewModel = context.createViewModel(cafes = cafes, cart = FakeCartRepository(cartOf(2, 1)))
        observe(viewModel)

        cafes.selectCafe("gdansk")
        advanceUntilIdle()

        assertEquals("Kafe gdansk", viewModel.content.cafeName)
        assertEquals(3, viewModel.content.cartItemCount)
    }

    @Test
    fun cartChangedWhileMenuIsLoadingIsShownWhenMenuArrives() = runViewModelTest { context ->
        val menus = FakeMenuRepository()
        val cart = FakeCartRepository()
        val viewModel = context.createViewModel(menus = menus, cart = cart)
        observe(viewModel)
        assertIs<MenuUiState.Loading>(viewModel.state.value)

        cart.cart.value = cartOf(4)
        advanceUntilIdle()
        menus.setCached("krakow", sampleMenu)
        advanceUntilIdle()

        assertEquals(4, viewModel.content.cartItemCount)
    }

    @Test
    fun cartItemCountIsKeptWhileSearchingAndChangingCategory() = runViewModelTest { context ->
        val viewModel = context.createViewModel(cart = FakeCartRepository(cartOf(7)))
        observe(viewModel)

        viewModel.onAction(MenuAction.SearchQueryChanged("lat"))
        viewModel.onAction(MenuAction.CategorySelected("classic"))
        advanceUntilIdle()

        assertEquals("lat", viewModel.content.searchQuery)
        assertEquals(7, viewModel.content.cartItemCount)
    }

    @Test
    fun cartChangeWithSameItemCountDoesNotEmitNewState() = runViewModelTest { context ->
        val cart = FakeCartRepository(cartOf(2, 3))
        val viewModel = context.createViewModel(cart = cart)

        viewModel.state.test {
            advanceUntilIdle()
            val content = assertIs<MenuUiState.Content>(expectMostRecentItem())

            cart.cart.value = cartOf(5)
            advanceUntilIdle()

            expectNoEvents()
            assertEquals(5, content.cartItemCount)
        }
    }

    @Test
    fun emptyingCartBringsCountBackToZero() = runViewModelTest { context ->
        val cart = FakeCartRepository(cartOf(20, 20))
        val viewModel = context.createViewModel(cart = cart)
        observe(viewModel)
        assertEquals(40, viewModel.content.cartItemCount)

        cart.cart.value = Cart.EMPTY
        advanceUntilIdle()

        assertEquals(0, viewModel.content.cartItemCount)
    }
}
