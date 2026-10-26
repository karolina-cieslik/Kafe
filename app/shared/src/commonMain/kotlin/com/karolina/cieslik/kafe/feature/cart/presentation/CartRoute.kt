package com.karolina.cieslik.kafe.feature.cart.presentation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

/**
 * Wejście do ekranu koszyka z grafu nawigacji – podpina [CartViewModel] do bezstanowego [CartScreen] i obsługuje
 * jednorazowe [CartEvent]: snackbary („Usunięto · Cofnij”, limit, błąd) przez [showCartEventSnackbar],
 * [CartEvent.UndoExpired] zamyka „Cofnij”, a zdarzenia nawigacji wywołują [onNavigateToMenu] / [onNavigateBack].
 *
 * @param onNavigateToMenu wywoływane po „Przejdź do menu” w pustym koszyku.
 * @param onNavigateBack wywoływane po strzałce wstecz.
 * @param viewModel domyślnie z Koina; parametr ułatwia podmianę w testach.
 */
@Composable
fun CartRoute(
    onNavigateToMenu: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CartViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val currentOnNavigateToMenu by rememberUpdatedState(onNavigateToMenu)
    val currentOnNavigateBack by rememberUpdatedState(onNavigateBack)
    // Nawigacja tylko z ekranu w stanie RESUMED: po pierwszym „wstecz” wpis koszyka schodzi do STARTED na czas
    // animacji wyjścia, a drugie szybkie stuknięcie zdjęłoby też ekran pod spodem (np. produkt).
    val navigateToMenu = dropUnlessResumed { currentOnNavigateToMenu() }
    val navigateBack = dropUnlessResumed { currentOnNavigateBack() }

    // Zbieranie eventów startuje raz na instancję ViewModelu – zmiana lambd nawigacji go nie restartuje,
    // bo wywołujemy je przez rememberUpdatedState (zawsze najnowsze).
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                // Osobna korutyna: showSnackbar zawiesza się do zamknięcia komunikatu (przy „Cofnij” ok. 10 s)
                // i nie może blokować kolejnych zdarzeń. Nowy komunikat zastępuje poprzedni.
                is CartEvent.ItemRemoved, is CartEvent.RestoredAtLimit, CartEvent.ActionFailed -> snackbarScope.launch {
                    snackbarHostState.showCartEventSnackbar(event) { viewModel.onAction(CartAction.UndoClicked) }
                }
                CartEvent.UndoExpired -> snackbarHostState.dismissUndoSnackbar()
                CartEvent.NavigateToMenu -> navigateToMenu()
                CartEvent.NavigateBack -> navigateBack()
            }
        }
    }

    CartScreen(
        state = state,
        onAction = viewModel::onAction,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}
