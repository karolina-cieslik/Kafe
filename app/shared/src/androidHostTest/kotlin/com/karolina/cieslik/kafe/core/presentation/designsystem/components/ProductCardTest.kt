package com.karolina.cieslik.kafe.core.presentation.designsystem.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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

// KAFE-25 / KAFE-6: ProductCard – dane produktu (AC 2), stan „Chwilowo niedostępny”, kliknięcia.
@RunWith(AndroidJUnit4::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class ProductCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var cardClicks = 0
    private var addClicks = 0

    private fun setCard(isAvailable: Boolean, subtitle: String? = "Z mleczną pianką") {
        composeRule.setContent {
            KafeTheme(darkTheme = false) {
                ProductCard(
                    name = "Cappuccino",
                    subtitle = subtitle,
                    priceLabel = "13,00 zł",
                    imageUrl = null,
                    isAvailable = isAvailable,
                    onClick = { cardClicks++ },
                    onAddClick = { addClicks++ },
                )
            }
        }
    }

    @Test
    fun availableCardShowsNameSubtitleAndPrice() {
        setCard(isAvailable = true)

        composeRule.onNodeWithText("Cappuccino").assertIsDisplayed()
        composeRule.onNodeWithText("Z mleczną pianką").assertIsDisplayed()
        composeRule.onNodeWithText("13,00 zł").assertIsDisplayed()
    }

    @Test
    fun availableCardHasAddButtonAndNoUnavailableLabel() {
        setCard(isAvailable = true)

        composeRule.onNodeWithContentDescription("Dodaj Cappuccino").assertIsDisplayed()
        composeRule.onNodeWithText("Chwilowo niedostępny").assertDoesNotExist()
    }

    @Test
    fun unavailableCardShowsLabelAndHasNoAddButton() {
        setCard(isAvailable = false)

        composeRule.onNodeWithText("Chwilowo niedostępny").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Dodaj Cappuccino").assertDoesNotExist()
    }

    @Test
    fun unavailableCardStillShowsNameAndPrice() {
        setCard(isAvailable = false)

        composeRule.onNodeWithText("Cappuccino").assertIsDisplayed()
        composeRule.onNodeWithText("13,00 zł").assertIsDisplayed()
    }

    @Test
    fun cardWithoutSubtitleRendersWithoutCrash() {
        setCard(isAvailable = true, subtitle = null)

        composeRule.onNodeWithText("Cappuccino").assertIsDisplayed()
    }

    @Test
    fun clickOnCardInvokesOnClick() {
        setCard(isAvailable = true)

        composeRule.onNodeWithText("Cappuccino").performClick()

        assertEquals(1, cardClicks)
        assertEquals(0, addClicks)
    }

    @Test
    fun clickOnAddButtonInvokesOnAddClick() {
        setCard(isAvailable = true)

        composeRule.onNodeWithContentDescription("Dodaj Cappuccino").performClick()

        assertEquals(1, addClicks)
    }
}
