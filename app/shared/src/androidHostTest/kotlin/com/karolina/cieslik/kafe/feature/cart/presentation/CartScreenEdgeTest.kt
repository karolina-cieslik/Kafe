package com.karolina.cieslik.kafe.feature.cart.presentation

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.karolina.cieslik.kafe.core.domain.Money
import com.karolina.cieslik.kafe.core.presentation.designsystem.theme.KafeTheme
import com.karolina.cieslik.kafe.core.testing.ROBOLECTRIC_SDK
import com.karolina.cieslik.kafe.feature.cart.FakeCartRepository
import com.karolina.cieslik.kafe.feature.cart.domain.Cart
import com.karolina.cieslik.kafe.feature.cart.domain.CartItem
import com.karolina.cieslik.kafe.feature.cart.domain.ObserveCheckedCart
import com.karolina.cieslik.kafe.feature.cart.domain.RefreshCartSnapshots
import com.karolina.cieslik.kafe.feature.menu.domain.SizeCode
import com.karolina.cieslik.kafe.feature.menu.presentation.FakeCafeRepository
import com.karolina.cieslik.kafe.feature.menu.presentation.FakeMenuRepository
import com.karolina.cieslik.kafe.feature.menu.presentation.cafe
import com.karolina.cieslik.kafe.feature.product.domain.PriceCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * CartScreen – przypadki brzegowe: długie teksty i duże kwoty, długa lista z przypiętym podsumowaniem, semantyka
 * dla TalkBack i dialog po odtworzeniu ekranu. Natywna grafika, bo w trybie LEGACY Robolectric nie mierzy tekstu.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [ROBOLECTRIC_SDK], qualifiers = "pl-w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CartScreenEdgeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val actions = mutableListOf<CartAction>()

    /** Do pomiaru ceny łącznej – jej węzeł ma tylko opis dla TalkBack, bez wyniku układu tekstu. */
    private lateinit var textMeasurer: TextMeasurer
    private lateinit var totalPriceStyle: TextStyle

    private val longItem = CartItemUi(
        id = 1,
        name = "Caffè latte na napoju owsianym z podwójnym syropem karmelowo-waniliowym i bitą śmietaną",
        imageUrl = null,
        sizeLabel = "Duży",
        modifiersLabel = "Napój owsiany barista, 4× Shot espresso, Syrop karmelowy, Syrop waniliowy, Bita śmietana",
        quantity = 20,
        canIncrease = false,
        unitPriceLabel = "1 000 007,99 zł",
        totalPriceLabel = "20 000 159,80 zł",
        status = CartItemStatusUi.PriceUpdated(previousUnitPriceLabel = "999 999,99 zł"),
    )

    private fun setScreen(state: CartUiState) {
        composeRule.setContent {
            KafeTheme(darkTheme = false) {
                textMeasurer = rememberTextMeasurer()
                totalPriceStyle = KafeTheme.typography.titleMedium
                CartScreen(state = state, onAction = { actions += it })
            }
        }
    }

    private fun content(items: List<CartItemUi>, summary: CartSummaryUi = CartSummaryUi(3, "59,00 zł")) =
        CartPreviewData.content.copy(items = items, summary = summary)

    private fun SemanticsNodeInteraction.textLayout(): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        return results.single()
    }

    private fun SemanticsNodeInteraction.bounds(): Rect = fetchSemanticsNode().boundsInRoot

    private fun text(value: String) = composeRule.onNodeWithText(value, useUnmergedTree = true)

    private fun textIn(containerTag: String, value: String) =
        composeRule.onNode(hasText(value) and hasAnyAncestor(hasTestTag(containerTag)), useUnmergedTree = true)

    /**
     * Tekst w jednej linii mieści się w całości. `hasVisualOverflow` się tu nie nadaje – zgłasza przepełnienie
     * także dla tekstu węższego niż dostępne miejsce, gdy węzeł ma szerokość treści.
     */
    private fun assertFitsInOneLine(node: SemanticsNodeInteraction, what: String) {
        val needed = node.textLayout().multiParagraph.intrinsics.maxIntrinsicWidth
        val width = node.bounds().width
        assertTrue("$what ucięte: potrzeba $needed px, jest $width px", needed <= width + 1f)
    }

    /** Etykieta pozycji w przyciskach dla TalkBack: nazwa, rozmiar i modyfikatory. */
    private val CartItemUi.spoken get() = listOfNotNull(name, sizeLabel, modifiersLabel).joinToString(", ")

    private fun assertRowPriceFits(item: CartItemUi) {
        val row = composeRule.onNodeWithTag(CartTestTags.item(item.id)).bounds()
        val total = composeRule
            .onNodeWithContentDescription("Razem za pozycję: ${item.totalPriceLabel}", useUnmergedTree = true)
            .bounds()
        val increase = composeRule.onNodeWithContentDescription("Zwiększ: ${item.spoken}").bounds()
        val needed = textMeasurer.measure(item.totalPriceLabel, totalPriceStyle).size.width
        assertTrue("cena łączna ucięta: potrzeba $needed px, jest ${total.width} px", needed <= total.width + 1f)
        assertTrue("cena łączna wychodzi poza wiersz", total.right <= row.right)
        assertTrue("cena łączna nachodzi na stepper", total.left >= increase.right)
    }

    @Test
    @Config(qualifiers = "pl-w320dp-h640dp")
    fun longNameAndModifiersWrapWithoutCuttingPriceOnNarrowScreen() {
        val item = longItem.copy(unitPriceLabel = "58,00 zł", totalPriceLabel = "1 160,00 zł")
        setScreen(content(listOf(item), CartSummaryUi(20, "1 160,00 zł")))

        val screen = composeRule.onRoot().bounds()
        val remove = composeRule.onNodeWithContentDescription("Usuń: ${item.spoken}").bounds()

        assertRowPriceFits(item)
        assertTrue("kosz poza ekranem: $remove", remove.right <= screen.right)
        assertEquals(2, text(item.name).textLayout().lineCount)
        val modifiers = text("Duży · ${item.modifiersLabel}").textLayout()
        assertTrue("modyfikatory w jednej linii", modifiers.lineCount > 1)
        assertFalse("modyfikatory ucięte w pionie", modifiers.didOverflowHeight)
    }

    @Test
    fun hugeTotalsFitInRowAndSummary() {
        setScreen(content(listOf(longItem), CartSummaryUi(20, "20 000 159,80 zł")))

        assertRowPriceFits(longItem)
        assertFitsInOneLine(textIn(CartTestTags.SUMMARY, "20 000 159,80 zł"), "kwota w podsumowaniu")
    }

    @Test
    fun twentyPiecesShowLimitUnitPriceAndPreviousPrice() {
        val item = longItem.copy(name = "Latte", modifiersLabel = null)
        setScreen(content(listOf(item), CartSummaryUi(20, "20 000 159,80 zł")))

        composeRule.onNodeWithContentDescription("Zwiększ: Latte, Duży").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Latte, Duży: 20", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Razem za pozycję: 20 000 159,80 zł", useUnmergedTree = true)
            .assertIsDisplayed()
        text("1 000 007,99 zł / szt.").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Poprzednia cena: 999 999,99 zł").assertIsDisplayed()
        text("20 produktów · Ceny zawierają VAT").assertIsDisplayed()
    }

    @Test
    fun lastOfManyItemsScrollsAboveSummaryAndCanBeRemoved() {
        val items = (1L..15L).map { id ->
            CartPreviewData.latte.copy(id = id, name = "Pozycja $id", modifiersLabel = null)
        }
        setScreen(content(items))

        composeRule.onNodeWithTag(CartTestTags.LIST).performScrollToIndex(items.size)
        composeRule.waitForIdle()

        val last = composeRule.onNodeWithTag(CartTestTags.item(15)).bounds()
        val summary = composeRule.onNodeWithTag(CartTestTags.SUMMARY).bounds()
        assertTrue("ostatnia pozycja pod podsumowaniem: $last / $summary", last.bottom <= summary.top)
        composeRule.onNodeWithContentDescription("Usuń: Pozycja 15, Średni").assertIsDisplayed().performClick()
        assertEquals(listOf<CartAction>(CartAction.RemoveClicked(15)), actions)
    }

    @Test
    fun twoConfigurationsOfSameProductHaveDistinctButtonLabels() {
        val oat = CartPreviewData.latte
        val cow = CartPreviewData.latte.copy(id = 2, modifiersLabel = null, sizeLabel = "Duży")
        setScreen(content(listOf(oat, cow)))

        listOf("Zmniejsz", "Zwiększ", "Usuń").forEach { verb ->
            composeRule.onNodeWithContentDescription("$verb: Latte, Średni, Owsiane, 2× Shot espresso")
                .assertHasClickAction()
            composeRule.onNodeWithContentDescription("$verb: Latte, Duży").assertHasClickAction()
        }
        composeRule.onNodeWithContentDescription("Usuń: Latte, Duży").performClick()
        assertEquals(listOf<CartAction>(CartAction.RemoveClicked(2)), actions)
    }

    @Test
    fun summaryIsReadAsOneElementWithTotalCountAndVat() {
        setScreen(CartPreviewData.content)

        val merged = composeRule.onAllNodes(hasText("Razem do zapłaty")).fetchSemanticsNodes()

        assertEquals(1, merged.size)
        assertEquals(
            listOf("Razem do zapłaty", "3 produkty · Ceny zawierają VAT", "59,00 zł"),
            merged.single().config[SemanticsProperties.Text].map { it.text },
        )
    }

    @Test
    fun crossedOutPreviousPriceIsReadOnlyAsDescription() {
        setScreen(content(listOf(CartPreviewData.cappuccino)))

        assertEquals(0, composeRule.onAllNodes(hasText("14,00 zł / szt."), true).fetchSemanticsNodes().size)
        composeRule.onNodeWithContentDescription("Poprzednia cena: 14,00 zł").assertIsDisplayed()
    }

    @Test
    fun clearDialogAndCartSurviveScreenRecreation() {
        val viewModel = cartViewModel()
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent {
            KafeTheme(darkTheme = false) {
                CartRoute(onNavigateToMenu = {}, onNavigateBack = {}, viewModel = viewModel)
            }
        }
        composeRule.onNodeWithTag(CartTestTags.CLEAR).performClick()
        composeRule.onNodeWithText("Wyczyścić koszyk?").assertIsDisplayed()

        restoration.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithText("Wyczyścić koszyk?").assertIsDisplayed()
        composeRule.onNodeWithText("Anuluj").performClick()
        composeRule.onNodeWithText("Wyczyścić koszyk?").assertDoesNotExist()
        composeRule.onNodeWithTag(CartTestTags.item(1)).assertIsDisplayed()
    }

    private fun cartViewModel(): CartViewModel {
        val espresso = CartItem(1, "espresso", "Espresso", SizeCode.S, emptyList(), 1, Money(900))
        val cart = FakeCartRepository(Cart(listOf(espresso)))
        val menus = FakeMenuRepository()
        return CartViewModel(
            cafeRepository = FakeCafeRepository(cafe("krakow")),
            menuRepository = menus,
            cartRepository = cart,
            observeCheckedCart = ObserveCheckedCart(cart, menus, PriceCalculator()),
            refreshCartSnapshots = RefreshCartSnapshots(cart, menus, PriceCalculator()),
        )
    }
}
