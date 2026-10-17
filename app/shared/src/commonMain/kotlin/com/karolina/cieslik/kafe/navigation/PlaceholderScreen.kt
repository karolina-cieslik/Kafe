package com.karolina.cieslik.kafe.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

// KAFE-41: Tymczasowy ekran dla tras, których ekrany jeszcze powstają (KAFE-9, KAFE-29).
@Composable
internal fun PlaceholderScreen(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().safeDrawingPadding().testTag(PLACEHOLDER_TEST_TAG),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title)
        TextButton(onClick = onBack) { Text("Wróć") }
    }
}

internal const val PLACEHOLDER_TEST_TAG = "placeholder_screen"
