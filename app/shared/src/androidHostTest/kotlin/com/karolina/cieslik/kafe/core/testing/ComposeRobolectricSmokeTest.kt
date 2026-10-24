package com.karolina.cieslik.kafe.core.testing

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Sprawdza, że testy Compose działają na JVM przez Robolectric (bez urządzenia, także w CI). */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class ComposeRobolectricSmokeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun clickUpdatesComposableState() {
        composeRule.setContent {
            var clicks by remember { mutableIntStateOf(0) }
            Button(onClick = { clicks++ }) { Text("Kliknięto: $clicks") }
        }

        composeRule.onNodeWithText("Kliknięto: 0").performClick()

        composeRule.onNodeWithText("Kliknięto: 1").assertIsDisplayed()
    }
}
