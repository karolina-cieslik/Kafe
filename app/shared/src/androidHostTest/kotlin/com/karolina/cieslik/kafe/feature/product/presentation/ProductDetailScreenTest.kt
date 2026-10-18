package com.karolina.cieslik.kafe.feature.product.presentation

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.karolina.cieslik.kafe.core.presentation.designsystem.theme.KafeTheme
import com.karolina.cieslik.kafe.core.testing.ROBOLECTRIC_SDK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** ProductDetailScreen: rozmiary z pełną ceną, modyfikatory i limity, liczba sztuk, cena i akcje. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [ROBOLECTRIC_SDK], qualifiers = "w411dp-h891dp")
class ProductDetailScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val actions = mutableListOf<ProductDetailAction>()

    private fun setScreen(state: ProductDetailUiState) {
        composeRule.setContent {
            KafeTheme(darkTheme = false) {
                ProductDetailScreen(state = state, onAction = { actions += it })
            }
        }
    }

    private fun clickContentDescription(description: String) {
        composeRule.onNodeWithContentDescription(description).scrollAboveBottomBar().performClick()
    }

    /**
     * Treść przewija się pod przypiętym paskiem ceny (edge-to-edge), więc samo performScrollTo zostawia element
     * pod paskiem. Dosuwamy go nad górną krawędź paska – tak jak zrobiłby to użytkownik.
     */
    private fun SemanticsNodeInteraction.scrollAboveBottomBar(): SemanticsNodeInteraction {
        performScrollTo()
        val barTop = composeRule.onNodeWithText("Razem").fetchSemanticsNode().boundsInRoot.top
        val overlap = fetchSemanticsNode().boundsInRoot.bottom - barTop + BAR_MARGIN_PX
        if (overlap > 0) {
            composeRule.onNodeWithTag(ProductDetailTestTags.CONTENT)
                .performSemanticsAction(SemanticsActions.ScrollBy) { scrollBy -> scrollBy(0f, overlap) }
        }
        return this
    }

    @Test
    fun loadingShowsLoadingTag() {
        setScreen(ProductDetailUiState.Loading)

        composeRule.onNodeWithTag(ProductDetailTestTags.LOADING).assertIsDisplayed()
        composeRule.onNodeWithTag(ProductDetailTestTags.CONTENT).assertDoesNotExist()
    }

    @Test
    fun errorRetrySendsRetry() {
        setScreen(ProductDetailUiState.Error("Nie udało się pobrać produktu."))

        composeRule.onNodeWithText("Nie udało się pobrać produktu.").assertIsDisplayed()
        composeRule.onNodeWithText("Spróbuj ponownie").performClick()

        assertEquals(listOf<ProductDetailAction>(ProductDetailAction.Retry), actions)
    }

    @Test
    fun contentShowsNameAndDescription() {
        setScreen(ProductDetailPreviewData.content)

        composeRule.onNodeWithTag(ProductDetailTestTags.CONTENT).assertIsDisplayed()
        composeRule.onNodeWithText("Cappuccino").assertIsDisplayed()
        composeRule.onNodeWithText("Klasyka włoskiej kawiarni", substring = true).assertIsDisplayed()
    }

    @Test
    fun sizesShowVolumeAndFullPrice() {
        setScreen(ProductDetailPreviewData.content)

        composeRule.onNodeWithTag(ProductDetailTestTags.size("S")).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("250 ml").assertIsDisplayed()
        composeRule.onNodeWithText("12,00 zł").assertIsDisplayed()
        composeRule.onNodeWithText("350 ml").assertIsDisplayed()
        composeRule.onNodeWithText("14,00 zł").assertIsDisplayed()
    }

    @Test
    fun defaultSizeFromStateIsSelected() {
        setScreen(ProductDetailPreviewData.content)

        composeRule.onNodeWithTag(ProductDetailTestTags.size("M")).performScrollTo().assertIsSelected()
        composeRule.onNodeWithTag(ProductDetailTestTags.size("S")).assertIsNotSelected()
    }

    @Test
    fun clickOnSizeSendsSizeSelected() {
        setScreen(ProductDetailPreviewData.content)

        composeRule.onNodeWithTag(ProductDetailTestTags.size("S")).scrollAboveBottomBar().performClick()

        assertEquals(listOf<ProductDetailAction>(ProductDetailAction.SizeSelected("S")), actions)
    }

    @Test
    fun unavailableSizeIsDisabledAndCannotBeSelected() {
        setScreen(ProductDetailPreviewData.content)

        composeRule.onNodeWithTag(ProductDetailTestTags.size("L")).performScrollTo().assertIsNotEnabled().performClick()

        assertTrue(actions.isEmpty())
    }

    @Test
    fun singleSizeProductHidesSizeSelector() {
        val single = ProductDetailPreviewData.content.copy(
            sizes = listOf(ProductSizeUi("S", "S", null, "9,50 zł", isAvailable = true, isSelected = true)),
        )
        setScreen(single)

        composeRule.onNodeWithTag(ProductDetailTestTags.size("S")).assertDoesNotExist()
    }

    @Test
    fun milkOptionsShowSurchargeAndDefaultCowMilkIsSelected() {
        setScreen(ProductDetailPreviewData.content)

        composeRule.onNodeWithTag(ProductDetailTestTags.option("milk", "cow")).performScrollTo().assertIsSelected()
        composeRule.onNodeWithTag(ProductDetailTestTags.option("milk", "oat")).assertIsNotSelected()
        composeRule.onNodeWithText("bez dopłaty").assertIsDisplayed()
    }

    @Test
    fun clickOnSingleOptionSendsOptionSelected() {
        setScreen(ProductDetailPreviewData.content)

        composeRule.onNodeWithTag(ProductDetailTestTags.option("milk", "oat")).scrollAboveBottomBar().performClick()

        assertEquals(listOf<ProductDetailAction>(ProductDetailAction.OptionSelected("milk", "oat")), actions)
    }

    @Test
    fun unavailableOptionIsDisabledAndCannotBeSelected() {
        setScreen(ProductDetailPreviewData.content)

        composeRule.onNodeWithTag(ProductDetailTestTags.option("milk", "soy")).performScrollTo().assertIsNotEnabled().performClick()

        assertTrue(actions.isEmpty())
    }

    @Test
    fun selectedSyrupCanBeClickedAgainToDeselect() {
        setScreen(ProductDetailPreviewData.content)

        composeRule.onNodeWithTag(ProductDetailTestTags.option("syrup", "vanilla")).scrollAboveBottomBar().assertIsSelected().performClick()

        assertEquals(listOf<ProductDetailAction>(ProductDetailAction.OptionSelected("syrup", "vanilla")), actions)
    }

    @Test
    fun shotStepperSendsQuantityChanges() {
        setScreen(ProductDetailPreviewData.content)

        composeRule.onNodeWithContentDescription("Dodatkowe espresso: 1").performScrollTo().assertIsDisplayed()
        clickContentDescription("Zwiększ: Dodatkowe espresso")
        clickContentDescription("Zmniejsz: Dodatkowe espresso")

        assertEquals(
            listOf<ProductDetailAction>(
                ProductDetailAction.OptionQuantityChanged("extras", "shot", +1),
                ProductDetailAction.OptionQuantityChanged("extras", "shot", -1),
            ),
            actions,
        )
    }

    @Test
    fun shotsAtLimitDisableIncreaseAndShowLimit() {
        setScreen(ProductDetailPreviewData.shotsAtLimit)

        composeRule.onNodeWithContentDescription("Dodatkowe espresso: 4").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Zwiększ: Dodatkowe espresso").assertIsNotEnabled().performClick()
        composeRule.onNodeWithContentDescription("Zmniejsz: Dodatkowe espresso").assertIsEnabled()
        composeRule.onNodeWithText("maks. 4").performScrollTo().assertIsDisplayed()

        assertTrue(actions.isEmpty())
    }

    @Test
    fun quantityStepperSendsQuantityChanged() {
        setScreen(ProductDetailPreviewData.shotsAtLimit)

        composeRule.onNodeWithContentDescription("Liczba sztuk: 2").performScrollTo().assertIsDisplayed()
        clickContentDescription("Zwiększ: Liczba sztuk")
        clickContentDescription("Zmniejsz: Liczba sztuk")

        assertEquals(
            listOf<ProductDetailAction>(ProductDetailAction.QuantityChanged(+1), ProductDetailAction.QuantityChanged(-1)),
            actions,
        )
    }

    @Test
    fun lastRowCanBeScrolledAboveBottomBar() {
        setScreen(ProductDetailPreviewData.content)

        composeRule.onNodeWithTag(ProductDetailTestTags.CONTENT)
            .performSemanticsAction(SemanticsActions.ScrollBy) { scrollBy -> scrollBy(0f, 100_000f) }

        // Na końcu przewijania ostatni wiersz (liczba sztuk) nie może być zasłonięty paskiem ceny.
        val barTop = composeRule.onNodeWithText("Razem").fetchSemanticsNode().boundsInRoot.top
        val lastRowBottom = composeRule.onNodeWithContentDescription("Zmniejsz: Liczba sztuk").fetchSemanticsNode().boundsInRoot.bottom
        assertTrue("Ostatni wiersz ($lastRowBottom) wchodzi pod pasek ceny ($barTop)", lastRowBottom <= barTop)
    }

    @Test
    fun quantityCannotBeDecreasedBelowOne() {
        setScreen(ProductDetailPreviewData.content)

        composeRule.onNodeWithContentDescription("Liczba sztuk: 1").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Zmniejsz: Liczba sztuk").assertIsNotEnabled()
    }

    @Test
    fun totalPriceIsVisibleWithoutScrolling() {
        setScreen(ProductDetailPreviewData.content)

        composeRule.onNodeWithText("Razem").assertIsDisplayed()
        composeRule.onNodeWithText("18,50 zł").assertIsDisplayed()
        composeRule.onNodeWithTag(ProductDetailTestTags.ADD_TO_CART).assertIsDisplayed()
    }

    @Test
    fun totalPriceStaysVisibleAfterScrollingToBottom() {
        setScreen(ProductDetailPreviewData.content)

        composeRule.onNodeWithTag(ProductDetailTestTags.CONTENT)
            .performSemanticsAction(SemanticsActions.ScrollBy) { scrollBy -> scrollBy(0f, 100_000f) }

        composeRule.onNodeWithText("18,50 zł").assertIsDisplayed()
        composeRule.onNodeWithTag(ProductDetailTestTags.ADD_TO_CART).assertIsDisplayed()
    }

    @Test
    fun addToCartSendsAddToCartClicked() {
        setScreen(ProductDetailPreviewData.content)

        composeRule.onNodeWithTag(ProductDetailTestTags.ADD_TO_CART).assertIsEnabled().performClick()

        assertEquals(listOf<ProductDetailAction>(ProductDetailAction.AddToCartClicked), actions)
    }

    @Test
    fun addToCartIsDisabledWhenCannotAdd() {
        setScreen(ProductDetailPreviewData.content.copy(canAddToCart = false))

        composeRule.onNodeWithTag(ProductDetailTestTags.ADD_TO_CART).assertIsNotEnabled().performClick()

        assertTrue(actions.isEmpty())
    }

    @Test
    fun unavailableProductShowsBadgeInfoAndDisablesOptions() {
        setScreen(ProductDetailPreviewData.unavailable)

        composeRule.onNodeWithText("Chwilowo niedostępny").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(ProductDetailTestTags.INFO).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Ten produkt jest chwilowo niedostępny w Kafe Rynek.").assertIsDisplayed()
        composeRule.onNodeWithTag(ProductDetailTestTags.option("milk", "oat")).performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Zwiększ: Dodatkowe espresso").performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithTag(ProductDetailTestTags.ADD_TO_CART).assertIsNotEnabled()
    }

    @Test
    fun backArrowSendsBackClicked() {
        setScreen(ProductDetailPreviewData.content)

        composeRule.onNodeWithContentDescription("Wstecz").performClick()

        assertEquals(listOf<ProductDetailAction>(ProductDetailAction.BackClicked), actions)
    }

    /** Pomiar przepełnienia tekstu wymaga natywnej grafiki – w trybie LEGACY Robolectric nie mierzy tekstu. */
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test
    fun longDescriptionCanBeExpandedAndCollapsed() {
        setScreen(ProductDetailPreviewData.content.copy(description = "Klasyka włoskiej kawiarni. ".repeat(20)))

        composeRule.onNodeWithText("Czytaj więcej").performScrollTo().performClick()
        composeRule.onNodeWithText("Zwiń").performScrollTo().assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Czytaj więcej").performScrollTo().assertIsDisplayed()

        assertTrue(actions.isEmpty())
    }
}

private const val BAR_MARGIN_PX = 8f
