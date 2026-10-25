package com.karolina.cieslik.kafe.feature.menu.presentation

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.karolina.cieslik.kafe.core.presentation.designsystem.theme.KafeTheme
import com.karolina.cieslik.kafe.core.testing.ROBOLECTRIC_SDK
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** MenuScreen – stany Loading / Error + „Spróbuj ponownie” / Content i akcje użytkownika. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [ROBOLECTRIC_SDK], qualifiers = "w411dp-h891dp")
class MenuScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val actions = mutableListOf<MenuAction>()

    private val classic = CategoryUi(
        id = "classic",
        name = "Kawy klasyczne",
        products = listOf(
            ProductUi("espresso", "Espresso", "Podwójne", "9,00 zł", null, isAvailable = true),
            ProductUi("flat-white", "Flat white", "Mocniejsze niż latte", "15,00 zł", null, isAvailable = false),
        ),
    )
    private val snacks = CategoryUi(
        id = "snacks",
        name = "Przekąski",
        products = listOf(ProductUi("croissant", "Croissant maślany", null, "9,50 zł", null, isAvailable = true)),
    )
    private val content = MenuUiState.Content(
        cafeName = "Kafe Rynek",
        categories = listOf(classic, snacks),
        selectedCategoryId = "classic",
        isRefreshing = false,
    )

    private fun setScreen(state: MenuUiState) {
        composeRule.setContent {
            KafeTheme(darkTheme = false) {
                MenuScreen(state = state, onAction = { actions += it })
            }
        }
    }

    // --- Loading ---

    @Test
    fun loadingShowsSkeletonGridWithLoadingDescription() {
        setScreen(MenuUiState.Loading)

        composeRule.onNodeWithTag(MenuTestTags.LOADING)
            .assertIsDisplayed()
            .assertContentDescriptionEquals("Ładowanie…")
        composeRule.onNodeWithTag(MenuTestTags.GRID).assertDoesNotExist()
    }

    // --- Error ---

    @Test
    fun errorShowsMessageAndRetryButton() {
        setScreen(MenuUiState.Error("Nie udało się pobrać menu. Sprawdź połączenie."))

        composeRule.onNodeWithText("Nie udało się pobrać menu. Sprawdź połączenie.").assertIsDisplayed()
        composeRule.onNodeWithText("Spróbuj ponownie").assertIsDisplayed()
    }

    @Test
    fun retryButtonSendsRetry() {
        setScreen(MenuUiState.Error("Nie udało się pobrać menu. Sprawdź połączenie."))

        composeRule.onNodeWithText("Spróbuj ponownie").performClick()

        assertEquals(listOf<MenuAction>(MenuAction.Retry), actions)
    }

    // --- Content ---

    @Test
    fun contentShowsCafeNameChipsAndProductsOfSelectedCategory() {
        setScreen(content)

        composeRule.onNodeWithTag(MenuTestTags.GRID).assertIsDisplayed()
        composeRule.onNodeWithText("Kafe Rynek").assertIsDisplayed()
        composeRule.onNodeWithTag(MenuTestTags.category("classic")).assertIsDisplayed()
        composeRule.onNodeWithTag(MenuTestTags.category("snacks")).assertIsDisplayed()
        composeRule.onNodeWithTag(MenuTestTags.product("espresso")).assertIsDisplayed()
        composeRule.onNodeWithText("9,00 zł").assertIsDisplayed()
        // Bez wyszukiwania widać tylko wybraną kategorię.
        composeRule.onNodeWithTag(MenuTestTags.product("croissant")).assertDoesNotExist()
    }

    @Test
    fun selectedCategoryFromStateDecidesVisibleProducts() {
        setScreen(content.copy(selectedCategoryId = "snacks"))

        composeRule.onNodeWithTag(MenuTestTags.product("croissant")).assertIsDisplayed()
        composeRule.onNodeWithTag(MenuTestTags.product("espresso")).assertDoesNotExist()
    }

    @Test
    fun unavailableProductShowsLabelInGrid() {
        setScreen(content)

        composeRule.onNodeWithTag(MenuTestTags.GRID).performScrollToNode(hasTestTag(MenuTestTags.product("flat-white")))
        composeRule.onNodeWithText("Chwilowo niedostępny").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Dodaj Flat white").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Dodaj Espresso").assertExists()
    }

    // --- Akcje ---

    @Test
    fun categoryChipSendsCategorySelected() {
        setScreen(content)

        composeRule.onNodeWithTag(MenuTestTags.category("snacks")).performClick()

        assertEquals(listOf<MenuAction>(MenuAction.CategorySelected("snacks")), actions)
    }

    @Test
    fun productCardSendsProductClicked() {
        setScreen(content)

        composeRule.onNodeWithTag(MenuTestTags.product("espresso")).performClick()

        assertEquals(listOf<MenuAction>(MenuAction.ProductClicked("espresso")), actions)
    }

    @Test
    fun addButtonSendsProductClicked() {
        setScreen(content)

        composeRule.onNodeWithContentDescription("Dodaj Espresso").performClick()

        assertEquals(listOf<MenuAction>(MenuAction.ProductClicked("espresso")), actions)
    }

    @Test
    fun cafeNameClickSendsChangeCafeClicked() {
        setScreen(content)

        composeRule.onNode(hasClickLabel("Zmień kawiarnię")).performClick()

        assertEquals(listOf<MenuAction>(MenuAction.ChangeCafeClicked), actions)
    }

    @Test
    fun typingInSearchSendsSearchQueryChanged() {
        setScreen(content)

        composeRule.onNode(hasSetTextAction()).performTextInput("latte")

        assertEquals(listOf<MenuAction>(MenuAction.SearchQueryChanged("latte")), actions)
    }

    @Test
    fun clearSearchButtonSendsEmptyQuery() {
        setScreen(content.copy(searchQuery = "esp", categories = listOf(classic)))

        composeRule.onNodeWithContentDescription("Wyczyść wyszukiwanie").performClick()

        assertEquals(listOf<MenuAction>(MenuAction.SearchQueryChanged("")), actions)
    }

    @Test
    fun searchModeHidesChipsAndShowsSectionHeaders() {
        setScreen(content.copy(searchQuery = "a"))

        composeRule.onNodeWithTag(MenuTestTags.category("classic")).assertDoesNotExist()
        composeRule.onNodeWithText("Kawy klasyczne").assertIsDisplayed()
        composeRule.onNodeWithTag(MenuTestTags.GRID).performScrollToNode(hasText("Przekąski"))
        composeRule.onNodeWithText("Przekąski").assertIsDisplayed()
    }

    @Test
    fun emptySearchResultsShowNoResultsTitle() {
        setScreen(content.copy(searchQuery = "matcha", categories = emptyList(), selectedCategoryId = null))

        composeRule.onNodeWithText("Brak wyników").assertIsDisplayed()
        composeRule.onNodeWithText("Pusto w menu").assertDoesNotExist()
    }

    @Test
    fun emptyMenuShowsChangeCafeAction() {
        setScreen(content.copy(categories = emptyList(), selectedCategoryId = null))

        composeRule.onNodeWithText("Pusto w menu").assertIsDisplayed()
        composeRule.onNode(hasText("Zmień kawiarnię") and hasClickAction()).performClick()

        assertEquals(listOf<MenuAction>(MenuAction.ChangeCafeClicked), actions)
    }

    // --- Przypięty nagłówek i chipy ---

    private val longCategory = CategoryUi(
        id = "classic",
        name = "Kawy klasyczne",
        products = (1..12).map { ProductUi("p$it", "Kawa $it", null, "12,00 zł", null, isAvailable = true) },
    )

    @Test
    fun headerAndChipsStayVisibleAfterScrollingGrid() {
        setScreen(content.copy(categories = listOf(longCategory, snacks)))

        composeRule.onNodeWithTag(MenuTestTags.GRID).performScrollToIndex(11)

        composeRule.onNodeWithTag(MenuTestTags.product("p12")).assertIsDisplayed()
        composeRule.onNodeWithTag(MenuTestTags.product("p1")).assertIsNotDisplayedOrAbsent()
        composeRule.onNodeWithTag(MenuTestTags.HEADER).assertIsDisplayed()
        composeRule.onNodeWithTag(MenuTestTags.CATEGORIES).assertIsDisplayed()
        composeRule.onNodeWithTag(MenuTestTags.category("snacks")).assertIsDisplayed()
        composeRule.onNodeWithText("Kafe Rynek").assertIsDisplayed()
    }

    @Test
    fun gridDoesNotContainHeaderOrChips() {
        setScreen(content)

        composeRule.onNode(hasTestTag(MenuTestTags.HEADER) and hasAnyAncestor(hasTestTag(MenuTestTags.GRID))).assertDoesNotExist()
        composeRule.onNode(hasTestTag(MenuTestTags.CATEGORIES) and hasAnyAncestor(hasTestTag(MenuTestTags.GRID))).assertDoesNotExist()
    }

    @Test
    fun loadingShowsHeaderWithDisabledSearch() {
        setScreen(MenuUiState.Loading)

        composeRule.onNodeWithTag(MenuTestTags.HEADER).assertIsDisplayed()
        // Wyłączone pole tekstowe nie wystawia akcji SetText – nie da się w nim pisać.
        composeRule.onNodeWithText("Szukaj kawy").assertIsDisplayed()
        composeRule.onNode(hasSetTextAction()).assertDoesNotExist()
        composeRule.onNodeWithTag(MenuTestTags.CATEGORIES).assertDoesNotExist()
    }

    @Test
    fun errorIsFullScreenWithoutHeader() {
        setScreen(MenuUiState.Error(MENU_ERROR_GENERIC))

        composeRule.onNodeWithTag(MenuTestTags.HEADER).assertDoesNotExist()
    }

    @Test
    fun categoriesRowIsHiddenWhileSearchingAndForEmptyMenu() {
        setScreen(content.copy(searchQuery = "kawa"))

        composeRule.onNodeWithTag(MenuTestTags.HEADER).assertIsDisplayed()
        composeRule.onNodeWithTag(MenuTestTags.CATEGORIES).assertDoesNotExist()
    }

    @Test
    fun categoriesRowIsHiddenForEmptyMenu() {
        setScreen(content.copy(categories = emptyList(), selectedCategoryId = null))

        composeRule.onNodeWithTag(MenuTestTags.CATEGORIES).assertDoesNotExist()
    }

    private fun SemanticsNodeInteraction.assertIsNotDisplayedOrAbsent() {
        if (fetchSemanticsNodes().isNotEmpty()) assertIsNotDisplayed()
    }

    private fun SemanticsNodeInteraction.fetchSemanticsNodes() =
        runCatching { listOf(fetchSemanticsNode()) }.getOrDefault(emptyList())

    private fun hasClickLabel(label: String) = SemanticsMatcher("onClickLabel = $label") { node ->
        node.config.getOrElseNullable(SemanticsActions.OnClick) { null }?.label == label
    }

    private fun hasClickAction() = SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick)

    @Test
    fun headerCartIconDescribesItemCount() {
        setScreen(content.copy(cartItemCount = 3))

        composeRule.onNodeWithTag(MenuTestTags.CART)
            .assertIsDisplayed()
            .assertContentDescriptionEquals("Koszyk, 3 sztuki")
    }

    @Test
    fun headerCartIconWithoutItemsIsDescribedAsEmpty() {
        setScreen(content)

        composeRule.onNodeWithTag(MenuTestTags.CART).assertContentDescriptionEquals("Koszyk jest pusty")
    }

    @Test
    fun loadingHeaderShowsEmptyCartIcon() {
        setScreen(MenuUiState.Loading)

        composeRule.onNodeWithTag(MenuTestTags.CART).assertContentDescriptionEquals("Koszyk jest pusty")
    }

    @Test
    fun headerCartIconHasNoClickActionYet() {
        setScreen(content.copy(cartItemCount = 3))

        composeRule.onNodeWithTag(MenuTestTags.CART).assert(hasClickAction().not())
    }
}
