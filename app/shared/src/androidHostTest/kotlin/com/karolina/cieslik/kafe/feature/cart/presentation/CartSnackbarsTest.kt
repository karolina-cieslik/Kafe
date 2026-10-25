package com.karolina.cieslik.kafe.feature.cart.presentation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.karolina.cieslik.kafe.core.presentation.designsystem.theme.KafeTheme
import com.karolina.cieslik.kafe.core.testing.ROBOLECTRIC_SDK
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Snackbary ekranu koszyka: „Usunięto · Cofnij”, zastępowanie, limit, błąd i zamykanie „Cofnij” po wyczyszczeniu. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [ROBOLECTRIC_SDK], qualifiers = "pl-w411dp-h891dp")
class CartSnackbarsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var hostState: SnackbarHostState
    private lateinit var scope: CoroutineScope
    private var undoCount = 0

    private fun setScreen() {
        composeRule.setContent {
            hostState = remember { SnackbarHostState() }
            scope = rememberCoroutineScope()
            KafeTheme(darkTheme = false) {
                CartScreen(state = CartPreviewData.content, onAction = {}, snackbarHostState = hostState)
            }
        }
    }

    private fun send(event: CartEvent) {
        composeRule.runOnIdle {
            scope.launch { hostState.showCartEventSnackbar(event) { undoCount++ } }
        }
    }

    private fun waitUntilDisplayed(text: String) {
        composeRule.waitUntil(conditionDescription = "widoczny tekst „$text”", timeoutMillis = SNACKBAR_TIMEOUT_MS) {
            composeRule.onNodeWithText(text).isDisplayed()
        }
    }

    private fun waitUntilGone(text: String) {
        composeRule.waitUntil(conditionDescription = "zniknął tekst „$text”", timeoutMillis = SNACKBAR_TIMEOUT_MS) {
            countNodesWithText(text) == 0
        }
    }

    private fun countNodesWithText(text: String) = composeRule.onAllNodes(hasText(text)).fetchSemanticsNodes().size

    @Test
    fun itemRemovedShowsUndoAndUndoCallsOnUndo() {
        setScreen()

        send(CartEvent.ItemRemoved("Latte"))
        waitUntilDisplayed("Usunięto: Latte")
        composeRule.onNodeWithText("Cofnij").performClick()

        waitUntilGone("Usunięto: Latte")
        assertEquals(1, undoCount)
    }

    @Test
    fun newRemovalReplacesPreviousUndo() {
        setScreen()

        send(CartEvent.ItemRemoved("Latte"))
        waitUntilDisplayed("Usunięto: Latte")
        send(CartEvent.ItemRemoved("Cappuccino"))

        waitUntilDisplayed("Usunięto: Cappuccino")
        waitUntilGone("Usunięto: Latte")
        assertEquals(0, undoCount)
    }

    @Test
    fun restoredAtLimitShowsSharedLimitTextWithoutUndo() {
        setScreen()

        send(CartEvent.RestoredAtLimit("Espresso"))

        waitUntilDisplayed("W koszyku jest już maks. 20 szt. tej pozycji")
        assertEquals(0, countNodesWithText("Cofnij"))
    }

    @Test
    fun actionFailedShowsRetryMessage() {
        setScreen()

        send(CartEvent.ActionFailed)

        waitUntilDisplayed("Nie udało się zmienić koszyka. Spróbuj ponownie.")
    }

    @Test
    fun dismissUndoSnackbarClosesUndoWithoutCallingOnUndo() {
        setScreen()
        send(CartEvent.ItemRemoved("Latte"))
        waitUntilDisplayed("Usunięto: Latte")

        composeRule.runOnIdle { hostState.dismissUndoSnackbar() }

        waitUntilGone("Usunięto: Latte")
        assertEquals(0, undoCount)
    }

    @Test
    fun dismissUndoSnackbarKeepsErrorMessage() {
        setScreen()
        send(CartEvent.ActionFailed)
        waitUntilDisplayed("Nie udało się zmienić koszyka. Spróbuj ponownie.")

        composeRule.runOnIdle { hostState.dismissUndoSnackbar() }

        composeRule.waitForIdle()
        assertEquals(1, countNodesWithText("Nie udało się zmienić koszyka. Spróbuj ponownie."))
    }

    @Test
    fun navigationAndUndoExpiredEventsKeepCurrentSnackbar() {
        setScreen()
        send(CartEvent.ItemRemoved("Latte"))
        waitUntilDisplayed("Usunięto: Latte")

        send(CartEvent.NavigateBack)
        send(CartEvent.NavigateToMenu)
        send(CartEvent.UndoExpired)

        composeRule.waitForIdle()
        assertEquals(1, countNodesWithText("Usunięto: Latte"))
    }

    private companion object {
        const val SNACKBAR_TIMEOUT_MS = 5_000L
    }
}
