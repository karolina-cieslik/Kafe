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

/** Tymczasowy ekran dla tras, których właściwe ekrany jeszcze powstają: tytuł i przycisk „Wróć”. */
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

/** Tag testowy ekranu zastępczego. */
internal const val PLACEHOLDER_TEST_TAG = "placeholder_screen"
