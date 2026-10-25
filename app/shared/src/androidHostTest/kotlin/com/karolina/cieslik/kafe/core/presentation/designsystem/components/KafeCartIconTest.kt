package com.karolina.cieslik.kafe.core.presentation.designsystem.components

import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.karolina.cieslik.kafe.core.presentation.designsystem.theme.KafeTheme
import com.karolina.cieslik.kafe.core.testing.ROBOLECTRIC_SDK
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** KafeCartIcon: opis dla TalkBack z liczbą sztuk (polska odmiana – stąd język „pl”), brak kliknięcia, obszar 48dp. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [ROBOLECTRIC_SDK], qualifiers = "pl")
class KafeCartIconTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setIcon(itemCount: Int) {
        composeRule.setContent {
            KafeTheme(darkTheme = false) {
                KafeCartIcon(itemCount = itemCount)
            }
        }
    }

    @Test
    fun emptyCartIsDescribedAsEmpty() {
        setIcon(0)

        composeRule.onNodeWithContentDescription("Koszyk jest pusty").assertIsDisplayed()
    }

    @Test
    fun oneItemUsesSingularForm() {
        setIcon(1)

        composeRule.onNodeWithContentDescription("Koszyk, 1 sztuka").assertIsDisplayed()
    }

    @Test
    fun fewItemsUseFewForm() {
        setIcon(3)

        composeRule.onNodeWithContentDescription("Koszyk, 3 sztuki").assertIsDisplayed()
    }

    @Test
    fun manyItemsUseManyForm() {
        setIcon(5)

        composeRule.onNodeWithContentDescription("Koszyk, 5 sztuk").assertIsDisplayed()
    }

    @Test
    fun countAboveBadgeMaxIsStillReadInFull() {
        setIcon(120)

        composeRule.onNodeWithContentDescription("Koszyk, 120 sztuk").assertIsDisplayed()
    }

    @Test
    fun iconIsNotClickableAndKeepsTouchTargetSize() {
        setIcon(3)

        composeRule.onNodeWithContentDescription("Koszyk, 3 sztuki")
            .assertHasNoClickAction()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
    }
}
