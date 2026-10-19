package com.karolina.cieslik.kafe.feature.cafe.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
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

/** Ekran wyboru lokalu: nagłówek powitalny albo pasek ze strzałką wstecz we wszystkich stanach i „Spróbuj ponownie” przy pustej liście. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [ROBOLECTRIC_SDK], qualifiers = "w411dp-h891dp")
class CafePickerScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val actions = mutableListOf<CafePickerAction>()

    private fun setScreen(state: CafePickerUiState, canNavigateBack: Boolean) {
        composeRule.setContent {
            KafeTheme(darkTheme = false) {
                CafePickerScreen(state = state, onAction = { actions += it }, canNavigateBack = canNavigateBack)
            }
        }
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
        setScreen(CafePickerUiState.Loading, canNavigateBack = true)

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
