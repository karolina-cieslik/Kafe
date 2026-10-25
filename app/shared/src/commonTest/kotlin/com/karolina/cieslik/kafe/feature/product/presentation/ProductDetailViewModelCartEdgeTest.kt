package com.karolina.cieslik.kafe.feature.product.presentation

import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.Money
import com.karolina.cieslik.kafe.core.domain.Result
import com.karolina.cieslik.kafe.core.testing.ViewModelTestContext
import com.karolina.cieslik.kafe.core.testing.runViewModelTest
import com.karolina.cieslik.kafe.feature.cart.FakeCartRepository
import com.karolina.cieslik.kafe.feature.cart.domain.AddToCart
import com.karolina.cieslik.kafe.feature.cart.domain.AddToCartOutcome
import com.karolina.cieslik.kafe.feature.cart.toCartItem
import com.karolina.cieslik.kafe.feature.menu.domain.Category
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
import com.karolina.cieslik.kafe.feature.product.domain.GetProductDetails
import com.karolina.cieslik.kafe.feature.product.domain.PriceCalculator
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Dodanie do koszyka z ekranu produktu: kolejne kliknięcia, ponowienie po błędzie, zdarzenia przy rotacji. */
class ProductDetailViewModelCartEdgeTest {

    private val milk = ModifierGroup(
        id = "milk", name = "Mleko", type = ModifierGroupType.SINGLE, isRequired = true, maxQuantity = 1,
        options = listOf(
            ModifierOption("cow", "Krowie", Money.ZERO),
            ModifierOption("oat", "Owsiane", Money(200)),
        ),
        defaultOptionId = "cow",
    )
    private val syrup = ModifierGroup(
        id = "syrup", name = "Syropy", type = ModifierGroupType.SINGLE, isRequired = false, maxQuantity = 1,
        options = listOf(ModifierOption("vanilla", "Waniliowy", Money(150))),
    )
    private val latte = Product(
        id = "latte", categoryId = "classic", name = "Caffè latte", description = null, imageUrl = null,
        sizes = listOf(ProductSize(SizeCode.S, 250, Money(1_200)), ProductSize(SizeCode.M, 350, Money(1_400))),
        sortOrder = 1, isActive = true, defaultSize = SizeCode.M, modifierGroupIds = listOf("milk", "syrup"),
    )

    private fun menu(
        unavailableOptionIds: Set<String> = emptySet(),
        unavailableProductIds: Set<String> = emptySet(),
    ) = Menu(
        categories = listOf(Category("classic", "Kawy klasyczne", 1)),
        products = listOf(latte),
        unavailableProductIds = unavailableProductIds,
        modifierGroups = listOf(milk, syrup),
        unavailableOptionIds = unavailableOptionIds,
    )

    private class Env(
        val viewModel: ProductDetailViewModel,
        val menus: FakeMenuRepository,
        val cart: FakeCartRepository,
    )

    private fun TestScope.createViewModel(context: ViewModelTestContext): Env {
        val menus = FakeMenuRepository(cached = mapOf("krakow" to menu()))
        val cart = FakeCartRepository()
        val viewModel = context.track(
            ProductDetailViewModel(
                productId = "latte",
                cafeRepository = FakeCafeRepository(cafe("krakow")),
                menuRepository = menus,
                getProductDetails = GetProductDetails(menus),
                priceCalculator = PriceCalculator(),
                addToCart = AddToCart(cart, PriceCalculator()),
            ),
        )
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()
        return Env(viewModel, menus, cart)
    }

    /**
     * Kolektor zdarzeń jak ekran. Unconfined, bo na StandardTestDispatcher w `backgroundScope` zdarzenie docierało
     * dopiero przy następnej akcji, a nie po `advanceUntilIdle`.
     */
    private fun TestScope.collectEvents(viewModel: ProductDetailViewModel): MutableList<ProductDetailEvent> {
        val events = mutableListOf<ProductDetailEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.collect { events += it } }
        advanceUntilIdle()
        return events
    }

    private fun TestScope.click(env: Env) {
        env.viewModel.onAction(ProductDetailAction.AddToCartClicked)
        advanceUntilIdle()
    }

    @Test
    fun secondClickAfterFinishedSaveAddsSecondPiece() = runViewModelTest { context ->
        val env = createViewModel(context)
        val events = collectEvents(env.viewModel)
        env.cart.addResult = { item ->
            val merged = env.cart.added.size > 1
            val saved = item.toCartItem(id = 1, quantity = env.cart.added.size)
            Result.Success(if (merged) AddToCartOutcome.QuantityIncreased(saved) else AddToCartOutcome.Added(saved))
        }

        click(env)
        click(env)

        assertEquals(2, env.cart.added.size)
        assertEquals(env.cart.added[0], env.cart.added[1])
        assertEquals(List<ProductDetailEvent>(2) { ProductDetailEvent.AddedToCart("Caffè latte") }, events)
    }

    @Test
    fun clickAfterFailedSaveTriesAgain() = runViewModelTest { context ->
        val env = createViewModel(context)
        val events = collectEvents(env.viewModel)
        env.cart.addResult = { Result.Failure(DataError.Local.DISK_FULL) }
        click(env)

        env.cart.addResult = null
        click(env)

        assertEquals(2, env.cart.added.size)
        assertEquals(
            listOf(ProductDetailEvent.AddToCartFailed, ProductDetailEvent.AddedToCart("Caffè latte")),
            events,
        )
    }

    @Test
    fun confirmationFinishedDuringRotationReachesNewScreenOnce() = runViewModelTest { context ->
        val env = createViewModel(context)
        val gate = CompletableDeferred<Unit>()
        env.cart.addGate = gate
        val beforeRotation = mutableListOf<ProductDetailEvent>()
        val oldScreen = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            env.viewModel.events.collect { beforeRotation += it }
        }
        advanceUntilIdle()
        click(env)

        oldScreen.cancel()
        gate.complete(Unit)
        advanceUntilIdle()
        val afterRotation = collectEvents(env.viewModel)

        assertTrue(beforeRotation.isEmpty())
        assertEquals(listOf<ProductDetailEvent>(ProductDetailEvent.AddedToCart("Caffè latte")), afterRotation)
    }

    @Test
    fun clickDuringSaveRemainsIgnoredAcrossRotation() = runViewModelTest { context ->
        val env = createViewModel(context)
        val gate = CompletableDeferred<Unit>()
        env.cart.addGate = gate
        click(env)

        val events = collectEvents(env.viewModel)
        click(env)
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, env.cart.added.size)
        assertEquals(1, events.size)
    }

    @Test
    fun optionThatBecameUnavailableIsNotAddedToCart() = runViewModelTest { context ->
        val env = createViewModel(context)
        val events = collectEvents(env.viewModel)
        env.viewModel.onAction(ProductDetailAction.OptionSelected("syrup", "vanilla"))
        advanceUntilIdle()
        env.menus.setCached("krakow", menu(unavailableOptionIds = setOf("vanilla")))
        advanceUntilIdle()

        click(env)

        assertEquals(listOf("cow"), env.cart.added.single().selections.map { it.optionId })
        assertEquals(Money(1_400), env.cart.added.single().unitPrice)
        assertEquals(listOf<ProductDetailEvent>(ProductDetailEvent.AddedToCart("Caffè latte")), events)
    }

    @Test
    fun productThatBecameUnavailableIsNotAddedAndEmitsNothing() = runViewModelTest { context ->
        val env = createViewModel(context)
        val events = collectEvents(env.viewModel)
        env.menus.setCached("krakow", menu(unavailableProductIds = setOf("latte")))
        advanceUntilIdle()

        click(env)

        assertTrue(env.cart.added.isEmpty())
        assertTrue(events.isEmpty())
    }
}
