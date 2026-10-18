package com.karolina.cieslik.kafe.feature.menu.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

/**
 * KAFE-6: Wejście do ekranu menu z grafu nawigacji – podpina [MenuViewModel] do bezstanowego [MenuScreen]
 * i zamienia jednorazowe [MenuEvent] na nawigację.
 *
 * @param onNavigateToProduct wywoływane z id produktu po stuknięciu karty lub „+” (event [MenuEvent.NavigateToProduct]).
 * @param onNavigateToCafePicker wywoływane po wybraniu „Zmień kawiarnię” (event [MenuEvent.NavigateToCafePicker]).
 * @param viewModel domyślnie z Koina; parametr ułatwia podmianę w testach.
 */
@Composable
fun MenuRoute(
    onNavigateToProduct: (productId: String) -> Unit,
    onNavigateToCafePicker: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MenuViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnNavigateToProduct by rememberUpdatedState(onNavigateToProduct)
    val currentOnNavigateToCafePicker by rememberUpdatedState(onNavigateToCafePicker)

    // Zbieranie eventów startuje raz na instancję ViewModelu – zmiana lambd nawigacji go nie restartuje,
    // bo wywołujemy je przez rememberUpdatedState (zawsze najnowsze).
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is MenuEvent.NavigateToProduct -> currentOnNavigateToProduct(event.productId)
                MenuEvent.NavigateToCafePicker -> currentOnNavigateToCafePicker()
            }
        }
    }

    MenuScreen(
        state = state,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}
