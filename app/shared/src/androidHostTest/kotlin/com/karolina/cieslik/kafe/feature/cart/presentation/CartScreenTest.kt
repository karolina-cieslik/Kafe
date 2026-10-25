package com.karolina.cieslik.kafe.feature.cart.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.karolina.cieslik.kafe.core.presentation.designsystem.theme.KafeTheme
import com.karolina.cieslik.kafe.core.testing.ROBOLECTRIC_SDK
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** CartScreen: stany, oznaczenia pozycji, stepper i kosz, podsumowanie z odmianą liczby i dialog wyczyszczenia. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [ROBOLECTRIC_SDK], qualifiers = "pl-w411dp-h891dp")
class CartScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val actions = mutableListOf<CartAction>()

    private fun setScreen(state: CartUiState) {
        composeRule.setContent {
            KafeTheme(darkTheme = false) {
                CartScreen(state = state, onAction = { actions += it })
            }
        }
    }

    private fun contentWith(vararg items: CartItemUi) = CartPreviewData.content.copy(items = items.toList())

    @Test
    fun loadingShowsLoadingTagWithoutSummaryAndClear() {
        setScreen(CartUiState.Loading)

        composeRule.onNodeWithTag(CartTestTags.LOADING).assertIsDisplayed()
        composeRule.onNodeWithTag(CartTestTags.SUMMARY).assertDoesNotExist()
        composeRule.onNodeWithTag(CartTestTags.CLEAR).assertDoesNotExist()
    }

    @Test
    fun emptyGoToMenuSendsGoToMenuClicked() {
        setScreen(CartUiState.Empty)

        composeRule.onNodeWithText("Twój koszyk jest pusty").assertIsDisplayed()
        composeRule.onNodeWithTag(CartTestTags.CLEAR).assertDoesNotExist()
        composeRule.onNodeWithText("Przejdź do menu").performClick()

        assertEquals(listOf<CartAction>(CartAction.GoToMenuClicked), actions)
    }

    @Test
    fun backArrowSendsBackClicked() {
        setScreen(CartUiState.Empty)

        composeRule.onNodeWithContentDescription("Wstecz").performClick()

        assertEquals(listOf<CartAction>(CartAction.BackClicked), actions)
    }

    @Test
    fun contentShowsPickupCafeWithoutUnverifiedBanner() {
        setScreen(CartPreviewData.content)

        composeRule.onNodeWithText("Odbiór w: Kafe Kazimierz").assertIsDisplayed()
        composeRule.onNodeWithTag(CartTestTags.PRICES_UNVERIFIED).assertDoesNotExist()
    }

    @Test
    fun noCafeHidesPickupLineAndShowsUnverifiedBanner() {
        setScreen(CartPreviewData.unverified)

        composeRule.onNodeWithText("Odbiór w:", substring = true).assertDoesNotExist()
        composeRule.onNodeWithTag(CartTestTags.PRICES_UNVERIFIED).assertIsDisplayed()
        composeRule.onNodeWithText("Nie udało się sprawdzić aktualnych cen", substring = true).assertIsDisplayed()
    }

    @Test
    fun itemShowsSizeModifiersUnitAndTotalPrice() {
        setScreen(contentWith(CartPreviewData.latte))

        composeRule.onNodeWithText("Latte").assertIsDisplayed()
        composeRule.onNodeWithText("Średni · Owsiane, 2× Shot espresso").assertIsDisplayed()
        composeRule.onNodeWithText("22,00 zł / szt.").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Razem za pozycję: 44,00 zł").assertIsDisplayed()
    }

    @Test
    fun singleItemHidesUnitPrice() {
        setScreen(contentWith(CartPreviewData.cappuccino.copy(status = CartItemStatusUi.Ok)))

        composeRule.onNodeWithText("15,00 zł / szt.").assertDoesNotExist()
        composeRule.onNodeWithText("Mały").assertIsDisplayed()
    }

    @Test
    fun stepperSendsIncreaseAndDecreaseWithItemId() {
        setScreen(contentWith(CartPreviewData.latte))

        composeRule.onNodeWithContentDescription("Zwiększ: $LATTE_LABEL").performClick()
        composeRule.onNodeWithContentDescription("Zmniejsz: $LATTE_LABEL").performClick()

        assertEquals(listOf(CartAction.IncreaseClicked(1), CartAction.DecreaseClicked(1)), actions)
    }

    @Test
    fun decreaseStaysEnabledAtOnePiece() {
        setScreen(contentWith(CartPreviewData.cappuccino))

        composeRule.onNodeWithContentDescription("Zmniejsz: Cappuccino, Mały").assertIsEnabled()
    }

    @Test
    fun increaseDisabledAtLimit() {
        setScreen(contentWith(CartPreviewData.espressoAtLimit))

        composeRule.onNodeWithContentDescription("Zwiększ: Espresso, Mały").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Espresso, Mały: 20").assertIsDisplayed()
    }

    @Test
    fun twoConfigurationsOfSameProductHaveDistinctButtonLabels() {
        setScreen(contentWith(CartPreviewData.latte, CartPreviewData.latte.copy(id = 5, modifiersLabel = null)))

        composeRule.onNodeWithContentDescription("Usuń: Latte, Średni").performClick()
        composeRule.onNodeWithContentDescription("Zwiększ: $LATTE_LABEL").performClick()

        assertEquals(listOf(CartAction.RemoveClicked(5), CartAction.IncreaseClicked(1)), actions)
    }

    @Test
    fun removeSendsRemoveClickedWithItemId() {
        setScreen(contentWith(CartPreviewData.latte))

        composeRule.onNodeWithContentDescription("Usuń: $LATTE_LABEL").performClick()

        assertEquals(listOf<CartAction>(CartAction.RemoveClicked(1)), actions)
    }

    @Test
    fun unavailableItemHasLabelAndRemoveButNoStepper() {
        setScreen(contentWith(CartPreviewData.flatWhite))

        composeRule.onNodeWithText("Niedostępne – usuń, aby kontynuować").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Zwiększ: Flat white, Średni").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Zmniejsz: Flat white, Średni").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Usuń: Flat white, Średni").performClick()

        assertEquals(listOf<CartAction>(CartAction.RemoveClicked(3)), actions)
    }

    @Test
    fun priceUpdatedShowsLabelPreviousAndNewUnitPrice() {
        setScreen(contentWith(CartPreviewData.cappuccino))

        composeRule.onNodeWithText("Cena została zaktualizowana").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Poprzednia cena: 14,00 zł").assertIsDisplayed()
        composeRule.onNodeWithText("15,00 zł / szt.").assertIsDisplayed()
    }

    @Test
    fun summaryShowsTotalPluralItemCountAndVat() {
        setScreen(CartPreviewData.content)

        composeRule.onNodeWithTag(CartTestTags.SUMMARY).assertIsDisplayed()
        composeRule.onNodeWithText("Razem do zapłaty", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("59,00 zł", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("3 produkty · Ceny zawierają VAT", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun summaryUsesManyFormForFive() {
        setScreen(CartPreviewData.content.copy(summary = CartSummaryUi(itemCount = 5, payableTotalLabel = "80,00 zł")))

        composeRule.onNodeWithText("5 produktów · Ceny zawierają VAT", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun summaryUsesSingularForOne() {
        setScreen(CartPreviewData.content.copy(summary = CartSummaryUi(itemCount = 1, payableTotalLabel = "15,00 zł")))

        composeRule.onNodeWithText("1 produkt · Ceny zawierają VAT", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun clearSendsClearClicked() {
        setScreen(CartPreviewData.content)

        composeRule.onNodeWithTag(CartTestTags.CLEAR).performClick()

        assertEquals(listOf<CartAction>(CartAction.ClearClicked), actions)
    }

    @Test
    fun clearConfirmationDialogConfirmSendsClearConfirmed() {
        setScreen(CartPreviewData.content.copy(showClearConfirmation = true))

        composeRule.onNodeWithText("Czy na pewno usunąć wszystkie pozycje?").assertIsDisplayed()
        composeRule.onNode(hasText("Wyczyść") and !hasTestTag(CartTestTags.CLEAR)).performClick()

        assertEquals(listOf<CartAction>(CartAction.ClearConfirmed), actions)
    }

    @Test
    fun clearConfirmationDialogCancelSendsClearDismissed() {
        setScreen(CartPreviewData.content.copy(showClearConfirmation = true))

        composeRule.onNodeWithText("Anuluj").performClick()

        assertEquals(listOf<CartAction>(CartAction.ClearDismissed), actions)
    }

    @Test
    fun noDialogWithoutClearConfirmation() {
        setScreen(CartPreviewData.content)

        composeRule.onNodeWithText("Czy na pewno usunąć wszystkie pozycje?").assertDoesNotExist()
    }

    private companion object {
        const val LATTE_LABEL = "Latte, Średni, Owsiane, 2× Shot espresso"
    }
}
