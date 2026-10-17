package com.karolina.cieslik.kafe.feature.product.presentation

import com.karolina.cieslik.kafe.core.domain.Money
import com.karolina.cieslik.kafe.core.testing.ViewModelTestContext
import com.karolina.cieslik.kafe.core.testing.runViewModelTest
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// KAFE-10 / KAFE-11: Przypadki brzegowe – menu odświeża się w trakcie konfiguracji produktu.
class ProductDetailViewModelRefreshTest {

    private val milk = ModifierGroup(
        id = "milk", name = "Mleko", type = ModifierGroupType.SINGLE, isRequired = true, maxQuantity = 1,
        options = listOf(ModifierOption("cow", "Krowie", Money.ZERO), ModifierOption("oat", "Owsiane", Money(200))),
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
        groups: List<ModifierGroup> = listOf(milk, syrup),
        product: Product = latte,
        unavailableOptionIds: Set<String> = emptySet(),
    ) = Menu(
        categories = listOf(Category("classic", "Kawy klasyczne", 1)),
        products = listOf(product),
        unavailableProductIds = emptySet(),
        modifierGroups = groups,
        unavailableOptionIds = unavailableOptionIds,
    )

    private class Env(val viewModel: ProductDetailViewModel, val menus: FakeMenuRepository) {
        val content get() = assertIs<ProductDetailUiState.Content>(viewModel.state.value)
    }

    private fun TestScope.createViewModel(context: ViewModelTestContext, initialMenu: Menu): Env {
        val menus = FakeMenuRepository(cached = mapOf("krakow" to initialMenu))
        val viewModel = context.track(
            ProductDetailViewModel(
                productId = "latte",
                cafeRepository = FakeCafeRepository(cafe("krakow")),
                menuRepository = menus,
                getProductDetails = GetProductDetails(menus),
                priceCalculator = PriceCalculator(),
                dispatcher = context.dispatcher,
            ),
        )
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()
        return Env(viewModel, menus)
    }

    private fun TestScope.act(env: Env, action: ProductDetailAction) {
        env.viewModel.onAction(action)
        advanceUntilIdle()
    }

    // KAFE-11 edge: zmiana cennika po odświeżeniu menu w trakcie konfiguracji – cena przelicza się automatycznie.
    @Test
    fun priceIsRecalculatedWhenPriceListChangesDuringConfiguration() = runViewModelTest { context ->
        val env = createViewModel(context, menu())
        act(env, ProductDetailAction.OptionSelected("milk", "oat"))
        assertEquals("16,00 zł", env.content.totalPriceLabel)

        val cheaperOat = milk.copy(options = listOf(milk.options[0], milk.options[1].copy(surcharge = Money(100))))
        env.menus.setCached("krakow", menu(groups = listOf(cheaperOat, syrup)))
        advanceUntilIdle()

        assertEquals("15,00 zł", env.content.totalPriceLabel)
        assertTrue(env.content.modifierGroups.first().options.first { it.id == "oat" }.isSelected)
    }

    // KAFE-10 edge: wybrany modyfikator staje się niedostępny po odświeżeniu – jest automatycznie odznaczany,
    // a użytkownik dostaje krótką informację.
    @Test
    fun selectedOptionBecomingUnavailableIsDeselectedWithInfo() = runViewModelTest { context ->
        val env = createViewModel(context, menu())
        act(env, ProductDetailAction.OptionSelected("syrup", "vanilla"))

        env.menus.setCached("krakow", menu(unavailableOptionIds = setOf("vanilla")))
        advanceUntilIdle()

        val vanilla = env.content.modifierGroups.first { it.id == "syrup" }.options.single()
        assertFalse(vanilla.isSelected, "Niedostępny syrop powinien zostać automatycznie odznaczony")
        assertNotNull(env.content.infoMessage)
        assertEquals("14,00 zł", env.content.totalPriceLabel)
        assertTrue(env.content.canAddToCart)
    }

    // Wybrana opcja zniknęła z grupy po odświeżeniu (i jest na liście niedostępnych) – ekran nie może się wywrócić.
    @Test
    fun refreshRemovingSelectedOptionDoesNotCrash() = runViewModelTest { context ->
        val env = createViewModel(context, menu())
        act(env, ProductDetailAction.OptionSelected("milk", "oat"))

        val milkWithoutOat = milk.copy(options = listOf(milk.options[0]))
        env.menus.setCached("krakow", menu(groups = listOf(milkWithoutOat, syrup), unavailableOptionIds = setOf("oat")))
        advanceUntilIdle()

        assertIs<ProductDetailUiState.Content>(env.viewModel.state.value)
    }

    // Wybrana opcja wymaganej grupy zniknęła z menu – nie można dodać do koszyka bez widocznego wyboru mleka.
    @Test
    fun refreshRemovingSelectedRequiredOptionBlocksAddToCartOrRestoresDefault() = runViewModelTest { context ->
        val env = createViewModel(context, menu())
        act(env, ProductDetailAction.OptionSelected("milk", "oat"))

        env.menus.setCached("krakow", menu(groups = listOf(milk.copy(options = listOf(milk.options[0])), syrup)))
        advanceUntilIdle()

        val milkUi = env.content.modifierGroups.first { it.id == "milk" }
        val anyMilkSelected = milkUi.options.any { it.isSelected }
        assertTrue(anyMilkSelected || !env.content.canAddToCart, "Brak widocznego wyboru mleka, a „Dodaj do koszyka” jest aktywne")
    }
}
