package com.karolina.cieslik.kafe.feature.cafe.presentation

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
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

/**
 * Ekran wyboru lokalu: lista z adresem, godzinami i „otwarte/zamknięte”, wybrany lokal, nagłówek powitalny albo
 * pasek ze strzałką wstecz we wszystkich stanach, stany Loading / pusta lista / Error z „Spróbuj ponownie”.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [ROBOLECTRIC_SDK], qualifiers = "w411dp-h891dp")
class CafePickerScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val actions = mutableListOf<CafePickerAction>()

    private val dluga = CafeUi("dluga", "Kafe Długa", "ul. Długa 45, Gdańsk", "Dziś 8:00–19:00", isOpenNow = true, isSelected = false)
    private val rynek = CafeUi("rynek", "Kafe Rynek", "Rynek Główny 12, Kraków", "Dziś zamknięte", isOpenNow = false, isSelected = true)

    private fun setScreen(state: CafePickerUiState, canNavigateBack: Boolean = true) {
        composeRule.setContent {
            KafeTheme(darkTheme = false) {
                CafePickerScreen(state = state, onAction = { actions += it }, canNavigateBack = canNavigateBack)
            }
        }
    }

    @Test
    fun loadingShowsLoadingTag() {
        setScreen(CafePickerUiState.Loading)

        composeRule.onNodeWithTag(CafePickerTestTags.LOADING).assertIsDisplayed()
        composeRule.onNodeWithTag(CafePickerTestTags.LIST).assertDoesNotExist()
    }

    @Test
    fun errorShowsMessageAndRetrySendsRetry() {
        setScreen(CafePickerUiState.Error(CAFE_PICKER_ERROR_NO_CONNECTION))

        composeRule.onNodeWithText("Nie udało się pobrać listy lokali. Sprawdź połączenie.").assertIsDisplayed()
        composeRule.onNodeWithText("Spróbuj ponownie").performClick()

        assertEquals(listOf<CafePickerAction>(CafePickerAction.Retry), actions)
    }

    @Test
    fun emptyListShowsEmptyState() {
        setScreen(CafePickerUiState.Content(cafes = emptyList(), isFirstLaunch = true))

        composeRule.onNodeWithText("Brak kawiarni").assertIsDisplayed()
        composeRule.onNodeWithTag(CafePickerTestTags.LIST).assertDoesNotExist()
    }

    @Test
    fun eachCafeShowsNameAddressHoursAndOpenStatus() {
        setScreen(CafePickerUiState.Content(cafes = listOf(dluga, rynek), isFirstLaunch = false))

        composeRule.onNodeWithText("Kafe Długa").assertIsDisplayed()
        composeRule.onNodeWithText("ul. Długa 45, Gdańsk").assertIsDisplayed()
        composeRule.onNodeWithText("Dziś 8:00–19:00").assertIsDisplayed()
        composeRule.onNodeWithText("Otwarte").assertIsDisplayed()
        composeRule.onNodeWithText("Rynek Główny 12, Kraków").assertIsDisplayed()
        composeRule.onNodeWithText("Dziś zamknięte").assertIsDisplayed()
        composeRule.onNodeWithText("Zamknięte").assertIsDisplayed()
    }

    @Test
    fun cafesAreShownInStateOrder() {
        setScreen(CafePickerUiState.Content(cafes = listOf(dluga, rynek), isFirstLaunch = false))

        val top = { id: String -> composeRule.onNodeWithTag(CafePickerTestTags.cafe(id)).fetchSemanticsNode().boundsInRoot.top }
        assertEquals(true, top("dluga") < top("rynek"))
    }

    @Test
    fun selectedCafeIsMarkedAsSelectedRadio() {
        setScreen(CafePickerUiState.Content(cafes = listOf(dluga, rynek), isFirstLaunch = false))

        composeRule.onNodeWithTag(CafePickerTestTags.cafe("rynek"))
            .assertIsSelected()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Wybrana"))
        composeRule.onNodeWithTag(CafePickerTestTags.cafe("dluga")).assertIsNotSelected()
    }

    @Test
    fun clickOnCafeSendsCafeSelected() {
        setScreen(CafePickerUiState.Content(cafes = listOf(dluga, rynek), isFirstLaunch = false))

        composeRule.onNodeWithTag(CafePickerTestTags.cafe("dluga")).performClick()

        assertEquals(listOf<CafePickerAction>(CafePickerAction.CafeSelected("dluga")), actions)
    }

    @Test
    fun firstLaunchShowsWelcomeWithoutBackArrow() {
        setScreen(CafePickerUiState.Content(cafes = listOf(dluga), isFirstLaunch = true), canNavigateBack = false)

        composeRule.onNodeWithTag(CafePickerTestTags.WELCOME).assertIsDisplayed()
        composeRule.onNode(hasText("Wybierz kawiarnię")).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Wstecz").assertDoesNotExist()
    }

    @Test
    fun changingCafeShowsBackArrowThatSendsBackClicked() {
        setScreen(CafePickerUiState.Content(cafes = listOf(dluga, rynek), isFirstLaunch = false))

        composeRule.onNodeWithTag(CafePickerTestTags.WELCOME).assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Wstecz").performClick()

        assertEquals(listOf<CafePickerAction>(CafePickerAction.BackClicked), actions)
    }

    @Test
    fun startDestinationInLoadingShowsWelcomeWithoutBackArrow() {
        setScreen(CafePickerUiState.Loading, canNavigateBack = false)

        composeRule.onNodeWithTag(CafePickerTestTags.LOADING).assertIsDisplayed()
        composeRule.onNodeWithTag(CafePickerTestTags.WELCOME).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Wstecz").assertDoesNotExist()
    }

    @Test
    fun startDestinationInErrorShowsWelcomeWithoutBackArrow() {
        setScreen(CafePickerUiState.Error("Brak połączenia z internetem."), canNavigateBack = false)

        composeRule.onNodeWithText("Brak połączenia z internetem.").assertIsDisplayed()
        composeRule.onNodeWithTag(CafePickerTestTags.WELCOME).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Wstecz").assertDoesNotExist()
    }

    @Test
    fun openedFromMenuInLoadingShowsBackArrowThatSendsBackClicked() {
        setScreen(CafePickerUiState.Loading)

        composeRule.onNodeWithTag(CafePickerTestTags.WELCOME).assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Wstecz").performClick()

        assertEquals(listOf<CafePickerAction>(CafePickerAction.BackClicked), actions)
    }

    @Test
    fun emptyCafeListRetrySendsRetry() {
        setScreen(CafePickerUiState.Content(cafes = emptyList(), isFirstLaunch = true), canNavigateBack = false)

        composeRule.onNodeWithText("Brak kawiarni").assertIsDisplayed()
        composeRule.onNodeWithText("Spróbuj ponownie").performClick()

        assertEquals(listOf<CafePickerAction>(CafePickerAction.Retry), actions)
    }
}
