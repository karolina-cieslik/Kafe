package com.karolina.cieslik.kafe.feature.product.presentation

import androidx.lifecycle.viewModelScope
import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.EmptyResult
import com.karolina.cieslik.kafe.core.domain.Money
import com.karolina.cieslik.kafe.core.domain.Result
import com.karolina.cieslik.kafe.feature.cafe.domain.Cafe
import com.karolina.cieslik.kafe.feature.cafe.domain.CafeRepository
import com.karolina.cieslik.kafe.feature.cart.FakeCartRepository
import com.karolina.cieslik.kafe.feature.cart.domain.AddToCart
import com.karolina.cieslik.kafe.feature.cart.domain.AddToCartOutcome
import com.karolina.cieslik.kafe.feature.cart.domain.CartSelection
import com.karolina.cieslik.kafe.feature.cart.toCartItem
import com.karolina.cieslik.kafe.feature.menu.domain.Menu
import com.karolina.cieslik.kafe.feature.menu.domain.MenuRepository
import com.karolina.cieslik.kafe.feature.menu.domain.ModifierGroup
import com.karolina.cieslik.kafe.feature.menu.domain.ModifierGroupType
import com.karolina.cieslik.kafe.feature.menu.domain.ModifierOption
import com.karolina.cieslik.kafe.feature.menu.domain.Product
import com.karolina.cieslik.kafe.feature.menu.domain.ProductSize
import com.karolina.cieslik.kafe.feature.menu.domain.SizeCode
import com.karolina.cieslik.kafe.feature.product.domain.GetProductDetails
import com.karolina.cieslik.kafe.feature.product.domain.PriceCalculator
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** ProductDetailViewModel na fake'owych repozytoriach; na końcu regresje od testera – odświeżenie menu w trakcie konfiguracji. */
@OptIn(ExperimentalCoroutinesApi::class)
class ProductDetailViewModelTest {

    private class FakeCafeRepository : CafeRepository {
        val selected = MutableStateFlow<Cafe?>(cafe("a"))
        override fun observeCafes(): Flow<List<Cafe>> = selected.map { listOfNotNull(it) }
        override fun observeSelectedCafe(): Flow<Cafe?> = selected
        override suspend fun selectCafe(cafeId: String): EmptyResult<DataError.Local> {
            selected.value = cafe(cafeId)
            return Result.Success(Unit)
        }
        override suspend fun refresh(): EmptyResult<DataError> = Result.Success(Unit)
    }

    private class FakeMenuRepository : MenuRepository {
        val menus = MutableStateFlow<Map<String, Menu>>(emptyMap())
        var remoteMenu: Menu = menu()
        var refreshResult: EmptyResult<DataError> = Result.Success(Unit)
        var refreshCount = 0

        override fun observeMenu(cafeId: String): Flow<Menu?> = menus.map { it[cafeId] }
        override suspend fun refresh(cafeId: String): EmptyResult<DataError> {
            refreshCount++
            if (refreshResult is Result.Success) menus.value += cafeId to remoteMenu
            return refreshResult
        }
    }

    private class Env(
        val viewModel: ProductDetailViewModel,
        val cafes: FakeCafeRepository,
        val menus: FakeMenuRepository,
        val cart: FakeCartRepository,
        val events: MutableList<ProductDetailEvent>,
    ) {
        val content get() = assertIs<ProductDetailUiState.Content>(viewModel.state.value)
    }

    private fun runVmTest(
        productId: String = "latte",
        savedMenu: Menu? = menu(),
        block: suspend TestScope.(Env) -> Unit,
    ) = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val cafes = FakeCafeRepository()
        val menus = FakeMenuRepository().apply { savedMenu?.let { menus.value = mapOf("a" to it) } }
        val cart = FakeCartRepository()
        val viewModel = ProductDetailViewModel(
            productId,
            cafes,
            menus,
            GetProductDetails(menus),
            PriceCalculator(),
            AddToCart(cart, PriceCalculator()),
        )
        val events = mutableListOf<ProductDetailEvent>()
        val collectors = listOf(
            launch { viewModel.state.collect {} },
            launch { viewModel.events.collect { events += it } },
        )
        try {
            advanceUntilIdle()
            block(Env(viewModel, cafes, menus, cart, events))
        } finally {
            collectors.forEach { it.cancel() }
            viewModel.viewModelScope.cancel()
            advanceUntilIdle()
            Dispatchers.resetMain()
        }
    }

    private fun TestScope.act(env: Env, vararg actions: ProductDetailAction) {
        actions.forEach { env.viewModel.onAction(it) }
        advanceUntilIdle()
    }

    @Test
    fun initialStateUsesDefaultsAndSizePrice() = runVmTest { env ->
        val content = env.content

        assertEquals("Caffè latte", content.name)
        assertEquals("M", content.sizes.single { it.isSelected }.code)
        assertEquals(listOf("Mały", "Średni", "Duży"), content.sizes.map { it.label })
        assertEquals("350 ml", content.sizes[1].volumeLabel)
        assertEquals("milk-cow", content.group("milk").options.single { it.isSelected }.id)
        assertEquals(1, content.quantity)
        assertEquals("16,00 zł", content.totalPriceLabel)
        assertTrue(content.canAddToCart)
        assertNull(content.infoMessage)
    }

    @Test
    fun labelsAndLimitsFollowContract() = runVmTest { env ->
        val milk = env.content.group("milk")
        assertEquals(ModifierGroupTypeUi.SINGLE, milk.type)
        assertTrue(milk.isRequired)
        assertNull(milk.limitLabel)
        assertEquals(listOf("bez dopłaty", "+2,00 zł"), milk.options.map { it.surchargeLabel })
        assertFalse(milk.options[0].canDecrease)

        repeat(5) { act(env, ProductDetailAction.OptionQuantityChanged("shots", "shot", +1)) }

        val shot = env.content.group("shots").also { assertEquals("maks. 4", it.limitLabel) }.options.single()
        assertEquals(4, shot.quantity)
        assertFalse(shot.canIncrease)
        assertTrue(shot.canDecrease)
    }

    @Test
    fun actionsUpdateTotalPriceWithKafe11Formula() = runVmTest { env ->
        act(
            env,
            ProductDetailAction.SizeSelected("L"),
            ProductDetailAction.OptionSelected("milk", "milk-oat"),
            ProductDetailAction.OptionQuantityChanged("shots", "shot", +1),
            ProductDetailAction.OptionQuantityChanged("shots", "shot", +1),
            ProductDetailAction.QuantityChanged(+1),
        )

        // (1800 + 200 + 2 * 300) * 2
        assertEquals("52,00 zł", env.content.totalPriceLabel)
        assertEquals(2, env.content.quantity)
    }

    @Test
    fun optionalSyrupTogglesAndQuantityNeverDropsBelowOne() = runVmTest { env ->
        act(env, ProductDetailAction.OptionSelected("syrup", "syrup-vanilla"))
        assertEquals("18,00 zł", env.content.totalPriceLabel)

        act(env, ProductDetailAction.OptionSelected("syrup", "syrup-vanilla"), ProductDetailAction.QuantityChanged(-1))
        assertEquals("16,00 zł", env.content.totalPriceLabel)
        assertEquals(1, env.content.quantity)
    }

    @Test
    fun unavailableDefaultSizeStartsFromFirstAvailableAndCannotBeSelected() = runVmTest(
        savedMenu = menu(unavailableSizes = mapOf("latte" to setOf(SizeCode.M))),
    ) { env ->
        assertEquals("S", env.content.sizes.single { it.isSelected }.code)
        assertFalse(env.content.sizes.single { it.code == "M" }.isAvailable)

        act(env, ProductDetailAction.SizeSelected("M"))

        assertEquals("S", env.content.sizes.single { it.isSelected }.code)
    }

    @Test
    fun productWithAllSizesUnavailableCannotBeAdded() = runVmTest(
        savedMenu = menu(unavailableSizes = mapOf("latte" to SizeCode.entries.toSet())),
    ) { env ->
        assertFalse(env.content.isAvailable)
        assertFalse(env.content.canAddToCart)
        assertEquals("Produkt jest niedostępny w wybranym lokalu.", env.content.infoMessage)

        act(env, ProductDetailAction.AddToCartClicked)

        assertEquals(emptyList(), env.events)
        assertTrue(env.cart.added.isEmpty())
    }

    @Test
    fun unavailableOptionIsMarkedAndCannotBeSelected() = runVmTest(
        savedMenu = menu(unavailableOptionIds = setOf("milk-oat")),
    ) { env ->
        assertFalse(env.content.group("milk").options.single { it.id == "milk-oat" }.isAvailable)

        act(env, ProductDetailAction.OptionSelected("milk", "milk-oat"))

        assertEquals("milk-cow", env.content.group("milk").options.single { it.isSelected }.id)
    }

    @Test
    fun addToCartAndBackEmitEvents() = runVmTest { env ->
        act(env, ProductDetailAction.AddToCartClicked, ProductDetailAction.BackClicked)

        assertEquals(listOf(ProductDetailEvent.AddedToCart("Caffè latte"), ProductDetailEvent.NavigateBack), env.events)
    }

    @Test
    fun addToCartSavesCurrentConfigurationAndKeepsItOnScreen() = runVmTest { env ->
        act(
            env,
            ProductDetailAction.OptionSelected("milk", "milk-oat"),
            ProductDetailAction.OptionQuantityChanged("shots", "shot", +2),
            ProductDetailAction.QuantityChanged(+2),
        )
        val before = env.content

        act(env, ProductDetailAction.AddToCartClicked)

        val added = env.cart.added.single()
        assertEquals("latte", added.productId)
        assertEquals(SizeCode.M, added.size)
        assertEquals(3, added.quantity)
        assertEquals(
            listOf(
                CartSelection("milk", "milk-oat", "Owsiane", Money(200), 1),
                CartSelection("shots", "shot", "Shot espresso", Money(300), 2),
            ),
            added.selections,
        )
        assertEquals(Money(2_400), added.unitPrice)
        assertEquals(before, env.content)
    }

    @Test
    fun addingAboveLimitEmitsLimitReached() = runVmTest { env ->
        env.cart.addResult = { item ->
            Result.Success(AddToCartOutcome.CappedAtLimit(item.toCartItem(id = 1, quantity = 20)))
        }

        act(env, ProductDetailAction.AddToCartClicked)

        assertEquals(
            listOf<ProductDetailEvent>(ProductDetailEvent.AddedToCart("Caffè latte", limitReached = true)),
            env.events,
        )
    }

    @Test
    fun mergingWithinLimitEmitsPlainConfirmation() = runVmTest { env ->
        env.cart.addResult = { item ->
            Result.Success(AddToCartOutcome.QuantityIncreased(item.toCartItem(id = 1, quantity = 5)))
        }

        act(env, ProductDetailAction.AddToCartClicked)

        assertEquals(listOf<ProductDetailEvent>(ProductDetailEvent.AddedToCart("Caffè latte")), env.events)
    }

    @Test
    fun storageFailureEmitsAddToCartFailed() = runVmTest { env ->
        env.cart.addResult = { Result.Failure(DataError.Local.DISK_FULL) }

        act(env, ProductDetailAction.AddToCartClicked)

        assertEquals(listOf<ProductDetailEvent>(ProductDetailEvent.AddToCartFailed), env.events)
    }

    @Test
    fun saveStartedBeforeScreenClosesCompletesWithoutEvent() = runVmTest { env ->
        val gate = CompletableDeferred<Unit>()
        env.cart.addGate = gate

        act(env, ProductDetailAction.AddToCartClicked)
        env.viewModel.viewModelScope.cancel()
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, env.cart.completed.size)
        assertTrue(env.events.isEmpty())
    }

    @Test
    fun clickDuringSaveIsIgnored() = runVmTest { env ->
        val gate = CompletableDeferred<Unit>()
        env.cart.addGate = gate

        act(env, ProductDetailAction.AddToCartClicked, ProductDetailAction.AddToCartClicked)
        gate.complete(Unit)
        advanceUntilIdle()
        act(env, ProductDetailAction.AddToCartClicked)

        assertEquals(2, env.cart.added.size)
        assertEquals(2, env.events.size)
    }

    @Test
    fun missingMenuIsFetchedOnce() = runVmTest(savedMenu = null) { env ->
        assertEquals(1, env.menus.refreshCount)
        assertIs<ProductDetailUiState.Content>(env.viewModel.state.value)
    }

    @Test
    fun menuFetchFailureShowsConnectionErrorAndRetryRecovers() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val cafes = FakeCafeRepository()
        val menus = FakeMenuRepository().apply { refreshResult = Result.Failure(DataError.Remote.NO_INTERNET) }
        val viewModel = ProductDetailViewModel(
            "latte",
            cafes,
            menus,
            GetProductDetails(menus),
            PriceCalculator(),
            AddToCart(FakeCartRepository(), PriceCalculator()),
        )
        val collector = launch { viewModel.state.collect {} }
        try {
            advanceUntilIdle()
            assertEquals(ProductDetailUiState.Error(PRODUCT_ERROR_NO_CONNECTION), viewModel.state.value)

            menus.refreshResult = Result.Success(Unit)
            viewModel.onAction(ProductDetailAction.Retry)
            advanceUntilIdle()

            assertIs<ProductDetailUiState.Content>(viewModel.state.value)
            assertEquals(2, menus.refreshCount)
        } finally {
            collector.cancel()
            viewModel.viewModelScope.cancel()
            advanceUntilIdle()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun unknownProductShowsNotFoundError() = runVmTest(productId = "unknown") { env ->
        assertEquals(ProductDetailUiState.Error(PRODUCT_ERROR_NOT_FOUND), env.viewModel.state.value)
    }

    @Test
    fun changingCafeResetsConfigurationToDefaults() = runVmTest { env ->
        env.menus.menus.value += "b" to menu()
        act(env, ProductDetailAction.SizeSelected("L"))
        assertEquals("L", env.content.sizes.single { it.isSelected }.code)

        env.cafes.selected.value = cafe("b")
        advanceUntilIdle()

        assertEquals("M", env.content.sizes.single { it.isSelected }.code)
    }

    @Test
    fun selectedOptionBecomingUnavailableIsDeselectedWithInfo() = runVmTest { env ->
        act(env, ProductDetailAction.OptionSelected("syrup", "syrup-vanilla"))

        env.menus.menus.value = mapOf("a" to menu(unavailableOptionIds = setOf("syrup-vanilla")))
        advanceUntilIdle()

        val content = env.content
        assertTrue(content.group("syrup").options.none { it.isSelected })
        assertEquals("Opcja „Waniliowy” jest już niedostępna i została odznaczona.", content.infoMessage)
        assertEquals("16,00 zł", content.totalPriceLabel)
        assertTrue(content.canAddToCart)
    }

    @Test
    fun refreshRemovingSelectedRequiredOptionRestoresDefault() = runVmTest { env ->
        act(env, ProductDetailAction.OptionSelected("milk", "milk-oat"))

        env.menus.menus.value = mapOf("a" to menu().copy(modifierGroups = listOf(milk.copy(options = milk.options.take(1)), shots, syrup)))
        advanceUntilIdle()

        assertEquals("milk-cow", env.content.group("milk").options.single { it.isSelected }.id)
        assertEquals("16,00 zł", env.content.totalPriceLabel)
        assertTrue(env.content.canAddToCart)
    }

    @Test
    fun quantityButtonsFollowLimits() = runVmTest { env ->
        assertFalse(env.content.canDecreaseQuantity)
        assertTrue(env.content.canIncreaseQuantity)

        act(env, ProductDetailAction.QuantityChanged(+100))

        assertEquals(20, env.content.quantity)
        assertFalse(env.content.canIncreaseQuantity)
        assertTrue(env.content.canDecreaseQuantity)
    }

    private fun ProductDetailUiState.Content.group(id: String) = modifierGroups.single { it.id == id }

    private companion object {
        fun cafe(id: String) = Cafe(id, "Kafe $id", "ul. Testowa 1", 0.0, 0.0, emptyList())

        val milk = ModifierGroup(
            id = "milk", name = "Mleko", type = ModifierGroupType.SINGLE, isRequired = true, maxQuantity = 1,
            options = listOf(ModifierOption("milk-cow", "Krowie", Money.ZERO), ModifierOption("milk-oat", "Owsiane", Money(200))),
            defaultOptionId = "milk-cow",
        )
        val shots = ModifierGroup(
            id = "shots", name = "Dodatkowe espresso", type = ModifierGroupType.QUANTITY, isRequired = false, maxQuantity = 4,
            options = listOf(ModifierOption("shot", "Shot espresso", Money(300))),
        )
        val syrup = ModifierGroup(
            id = "syrup", name = "Syrop", type = ModifierGroupType.SINGLE, isRequired = false, maxQuantity = 1,
            options = listOf(ModifierOption("syrup-vanilla", "Waniliowy", Money(200))),
        )
        val latte = Product(
            id = "latte", categoryId = "c", name = "Caffè latte", description = "Delikatne", imageUrl = null,
            sizes = listOf(
                ProductSize(SizeCode.S, 250, Money(1400)),
                ProductSize(SizeCode.M, 350, Money(1600)),
                ProductSize(SizeCode.L, 450, Money(1800)),
            ),
            sortOrder = 1, isActive = true, defaultSize = SizeCode.M, modifierGroupIds = listOf("milk", "shots", "syrup"),
        )

        fun menu(
            unavailableSizes: Map<String, Set<SizeCode>> = emptyMap(),
            unavailableOptionIds: Set<String> = emptySet(),
        ) = Menu(
            categories = emptyList(),
            products = listOf(latte),
            unavailableProductIds = emptySet(),
            modifierGroups = listOf(milk, shots, syrup),
            unavailableSizes = unavailableSizes,
            unavailableOptionIds = unavailableOptionIds,
        )
    }
}
