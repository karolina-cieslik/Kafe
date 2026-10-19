package com.karolina.cieslik.kafe.feature.cafe.presentation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kafe.app.shared.generated.resources.Res
import kafe.app.shared.generated.resources.cafe_picker_selection_failed
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.koin.compose.viewmodel.koinViewModel

/**
 * Wejście do ekranu wyboru lokalu z grafu nawigacji – podpina [CafePickerViewModel] do bezstanowego
 * [CafePickerScreen] i zamienia jednorazowe [CafePickerEvent] na nawigację; [CafePickerEvent.SelectionFailed] pokazuje
 * snackbar „Nie udało się zapisać wyboru lokalu…” i zostawia użytkownika na liście.
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
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val currentOnCafeChosen by rememberUpdatedState(onCafeChosen)
    val currentOnNavigateBack by rememberUpdatedState(onNavigateBack)

    // Klucz = ViewModel: zbieranie zdarzeń startuje raz na instancję VM i nie restartuje się przy rekompozycji;
    // aktualne lambdy nawigacji dają rememberUpdatedState.
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                CafePickerEvent.CafeChosen -> currentOnCafeChosen()
                // Osobna korutyna: showSnackbar zawiesza się do zamknięcia komunikatu i nie może blokować kolejnych
                // zdarzeń (np. udanego ponownego wyboru). Kolejny błąd zastępuje poprzedni komunikat.
                CafePickerEvent.SelectionFailed -> snackbarScope.launch {
                    val message = getString(Res.string.cafe_picker_selection_failed)
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(message)
                }
                CafePickerEvent.NavigateBack -> currentOnNavigateBack()
            }
        }
    }

    CafePickerScreen(
        state = state,
        onAction = viewModel::onAction,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}
