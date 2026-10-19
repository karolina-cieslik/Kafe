package com.karolina.cieslik.kafe.feature.start.presentation

import android.provider.Settings
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.karolina.cieslik.kafe.core.presentation.designsystem.theme.KafeTheme
import com.karolina.cieslik.kafe.core.testing.ROBOLECTRIC_SDK
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Intro: oś czasu (onFinished po ok. 1300 ms), dostępność (contentDescription „Kafe”) i wyłączone animacje.
 * Intro ma nieskończoną animację pary, więc zegar jest prowadzony ręcznie (`autoAdvance = false`).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class KafeIntroTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var finishedCount = 0

    private fun setIntro() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            KafeTheme(darkTheme = false) { KafeIntro(onFinished = { finishedCount++ }) }
        }
    }

    private fun setAnimatorScale(scale: Float) {
        val resolver = ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver
        Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, scale)
    }

    @Test
    fun introIsAccessibleAsBrandName() {
        setIntro()
        composeRule.mainClock.advanceTimeByFrame()

        composeRule.onNodeWithTag(KafeIntroTestTags.INTRO)
            .assertIsDisplayed()
            .assertContentDescriptionEquals("Kafe")
    }

    @Test
    fun introDoesNotFinishBeforeAnimationEnds() {
        setIntro()

        composeRule.mainClock.advanceTimeBy(1_000)

        assertEquals(0, finishedCount)
    }

    @Test
    fun introFinishesOnceAfterAnimation() {
        setIntro()

        composeRule.mainClock.advanceTimeBy(1_400)
        assertEquals(1, finishedCount)

        composeRule.mainClock.advanceTimeBy(2_000)
        assertEquals("onFinished nie może przyjść ponownie", 1, finishedCount)
    }

    @Test
    fun reducedMotionFinishesImmediately() {
        setAnimatorScale(0f)
        setIntro()

        composeRule.mainClock.advanceTimeByFrame()

        assertEquals(1, finishedCount)
        composeRule.onNodeWithTag(KafeIntroTestTags.INTRO).assertIsDisplayed()
    }
}
