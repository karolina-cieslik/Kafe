package com.karolina.cieslik.kafe.feature.cafe.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

/**
 * Wejście do ekranu wyboru lokalu z grafu nawigacji – podpina [CafePickerViewModel] do bezstanowego
 * [CafePickerScreen] i zamienia jednorazowe [CafePickerEvent] na nawigację.
 *
 * @param onCafeChosen wywoływane po zapisaniu wyboru ([CafePickerEvent.CafeChosen]); graf decyduje, czy wrócić
 * do menu, czy (przy pierwszym uruchomieniu) otworzyć menu z wyczyszczonym stosem.
 * @param onNavigateBack wywoływane po strzałce wstecz ([CafePickerEvent.NavigateBack]).
 * @param viewModel domyślnie z Koina; parametr ułatwia podmianę w testach.
 */
@Composable
fun CafePickerRoute(
    onCafeChosen: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CafePickerViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnCafeChosen by rememberUpdatedState(onCafeChosen)
    val currentOnNavigateBack by rememberUpdatedState(onNavigateBack)

    // Klucz = ViewModel: zbieranie zdarzeń startuje raz na instancję VM i nie restartuje się przy rekompozycji;
    // aktualne lambdy nawigacji dają rememberUpdatedState.
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                CafePickerEvent.CafeChosen -> currentOnCafeChosen()
                CafePickerEvent.NavigateBack -> currentOnNavigateBack()
            }
        }
    }

    CafePickerScreen(
        state = state,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}
