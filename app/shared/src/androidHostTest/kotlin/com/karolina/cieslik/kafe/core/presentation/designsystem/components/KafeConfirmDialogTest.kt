package com.karolina.cieslik.kafe.core.presentation.designsystem.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
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

/** KafeConfirmDialog: treść, domyślne „Anuluj” i wywołania obu przycisków. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class KafeConfirmDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val calls = mutableListOf<String>()

    private fun setDialog(title: String? = null) {
        composeRule.setContent {
            KafeTheme(darkTheme = false) {
                KafeConfirmDialog(
                    title = title,
                    message = "Czy na pewno usunąć wszystkie pozycje?",
                    confirmText = "Wyczyść",
                    onConfirm = { calls += "confirm" },
                    onDismiss = { calls += "dismiss" },
                    destructive = true,
                )
            }
        }
    }

    @Test
    fun showsTitleMessageAndDefaultCancel() {
        setDialog(title = "Wyczyścić koszyk?")

        composeRule.onNodeWithText("Wyczyścić koszyk?").assertIsDisplayed()
        composeRule.onNodeWithText("Czy na pewno usunąć wszystkie pozycje?").assertIsDisplayed()
        composeRule.onNodeWithText("Anuluj").assertIsDisplayed()
    }

    @Test
    fun confirmCallsOnConfirmOnly() {
        setDialog()

        composeRule.onNodeWithText("Wyczyść").performClick()

        assertEquals(listOf("confirm"), calls)
    }

    @Test
    fun cancelCallsOnDismissOnly() {
        setDialog()

        composeRule.onNodeWithText("Anuluj").performClick()

        assertEquals(listOf("dismiss"), calls)
    }
}
