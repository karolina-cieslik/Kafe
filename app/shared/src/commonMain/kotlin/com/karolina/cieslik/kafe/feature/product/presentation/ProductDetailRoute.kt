package com.karolina.cieslik.kafe.feature.product.presentation

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
import kafe.app.shared.generated.resources.product_detail_added_to_cart
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Wejście do ekranu szczegółów produktu z grafu nawigacji – podpina [ProductDetailViewModel] do bezstanowego
 * [ProductDetailScreen] i obsługuje jednorazowe [ProductDetailEvent]: [ProductDetailEvent.AddedToCart] pokazuje
 * snackbar „Dodano do koszyka: …”, a [ProductDetailEvent.NavigateBack] wywołuje [onNavigateBack].
 *
 * @param productId id produktu z trasy; trafia do ViewModelu jako parametr Koina.
 * @param onNavigateBack wywoływane po strzałce wstecz.
 * @param viewModel domyślnie z Koina dla [productId]; parametr ułatwia podmianę w testach.
 */
@Composable
fun ProductDetailRoute(
    productId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProductDetailViewModel = koinViewModel { parametersOf(productId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val currentOnNavigateBack by rememberUpdatedState(onNavigateBack)

    // Zbieranie eventów startuje raz na instancję ViewModelu – zmiana lambdy nawigacji go nie restartuje,
    // bo wywołujemy ją przez rememberUpdatedState (zawsze najnowszą).
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                // Osobna korutyna: showSnackbar zawiesza się do zamknięcia komunikatu i nie może blokować
                // kolejnych zdarzeń (np. NavigateBack). Nowe dodanie zastępuje poprzedni komunikat.
                is ProductDetailEvent.AddedToCart -> snackbarScope.launch {
                    val message = getString(Res.string.product_detail_added_to_cart, event.productName)
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(message)
                }
                // TODO(KAFE-12): snackbar błędu zapisu i komunikat o limicie 20 szt. – ekran w PR ui.
                ProductDetailEvent.AddToCartFailed -> Unit
                ProductDetailEvent.NavigateBack -> currentOnNavigateBack()
            }
        }
    }

    ProductDetailScreen(
        state = state,
        onAction = viewModel::onAction,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}
