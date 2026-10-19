package com.karolina.cieslik.kafe.core.presentation.designsystem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.karolina.cieslik.kafe.core.presentation.designsystem.theme.KafeTheme
import com.karolina.cieslik.kafe.core.testing.ROBOLECTRIC_SDK
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val HEADER = "header"
private const val SHORT_TITLE = "Dodatkowe espresso"
private const val LONG_TITLE = "Dodatkowe espresso z ziaren jednorodnych z Etiopii i Kolumbii"

/** KafeGroupHeader: oznaczenie tuż za tytułem, limit przy prawej krawędzi, tytuł zawija się tylko z braku miejsca. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [ROBOLECTRIC_SDK], qualifiers = "w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class KafeGroupHeaderTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setHeader(title: String) {
        composeRule.setContent {
            KafeTheme(darkTheme = false) {
                Box(Modifier.padding(horizontal = 24.dp)) {
                    KafeGroupHeader(
                        title = title,
                        isRequired = false,
                        hint = "maks. 4",
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(HEADER),
                    )
                }
            }
        }
    }

    private fun bounds(text: String): Rect =
        composeRule.onNodeWithText(text).assertIsDisplayed().fetchSemanticsNode().boundsInRoot

    private fun px(value: Float) = with(composeRule.density) { value.dp.toPx() }

    @Test
    fun shortTitleStaysOnOneLineWithLabelRightAfterItAndLimitAtEnd() {
        setHeader(SHORT_TITLE)

        val header = composeRule.onNodeWithTag(HEADER).fetchSemanticsNode().boundsInRoot
        val title = bounds(SHORT_TITLE)
        val label = bounds("Opcjonalne")
        val hint = bounds("maks. 4")

        assertTrue("Tytuł zawinięty: wysokość ${title.height}px", title.height < px(30f))
        assertTrue("Oznaczenie przed tytułem", label.left >= title.right)
        assertTrue("Oznaczenie odsunięte od tytułu o ${label.left - title.right}px", label.left - title.right <= px(9f))
        assertTrue("Limit nachodzi na oznaczenie", hint.left > label.right)
        assertTrue("Limit nie przy prawej krawędzi", header.right - hint.right <= px(1f))
    }

    @Test
    fun longTitleWrapsAndKeepsLabelAndLimitVisible() {
        setHeader(LONG_TITLE)

        val header = composeRule.onNodeWithTag(HEADER).fetchSemanticsNode().boundsInRoot
        val title = bounds(LONG_TITLE)
        val label = bounds("Opcjonalne")
        val hint = bounds("maks. 4")

        assertTrue("Długi tytuł powinien się zawinąć: wysokość ${title.height}px", title.height >= px(30f))
        assertTrue("Oznaczenie przed tytułem", label.left >= title.right)
        assertTrue("Limit nachodzi na oznaczenie", hint.left > label.right)
        assertTrue("Limit nie przy prawej krawędzi", header.right - hint.right <= px(1f))
    }
}
