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
import com.karolina.cieslik.kafe.feature.product.domain.ProductConfiguration
import kafe.app.shared.generated.resources.cart_limit_reached
import kafe.app.shared.generated.resources.product_detail_add_to_cart_failed
import kafe.app.shared.generated.resources.product_detail_added_to_cart
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Wejście do ekranu szczegółów produktu z grafu nawigacji – podpina [ProductDetailViewModel] do bezstanowego
 * [ProductDetailScreen] i obsługuje jednorazowe [ProductDetailEvent]: [ProductDetailEvent.AddedToCart] i
 * [ProductDetailEvent.AddToCartFailed] pokazują snackbar (treść z [snackbarMessage]), a
 * [ProductDetailEvent.NavigateBack] wywołuje [onNavigateBack].
 *
 * @param productId id produktu z trasy; trafia do ViewModelu jako parametr Koina.
 * @param onNavigateBack wywoływane po strzałce wstecz.
 * @param onNavigateToCart wywoływane po „Zobacz koszyk”.
 * @param viewModel domyślnie z Koina dla [productId]; parametr ułatwia podmianę w testach.
 */
@Composable
fun ProductDetailRoute(
    productId: String,
    onNavigateBack: () -> Unit,
    onNavigateToCart: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProductDetailViewModel = koinViewModel { parametersOf(productId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val currentOnNavigateBack by rememberUpdatedState(onNavigateBack)
    val currentOnNavigateToCart by rememberUpdatedState(onNavigateToCart)

    // Zbieranie eventów startuje raz na instancję ViewModelu – zmiana lambdy nawigacji go nie restartuje,
    // bo wywołujemy ją przez rememberUpdatedState (zawsze najnowszą).
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                // Osobna korutyna: showSnackbar zawiesza się do zamknięcia komunikatu i nie może blokować
                // kolejnych zdarzeń (np. NavigateBack). Nowy komunikat zastępuje poprzedni.
                is ProductDetailEvent.AddedToCart, ProductDetailEvent.AddToCartFailed -> snackbarScope.launch {
                    val message = event.snackbarMessage() ?: return@launch
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(message)
                }
                ProductDetailEvent.NavigateBack -> currentOnNavigateBack()
                ProductDetailEvent.NavigateToCart -> currentOnNavigateToCart()
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

/**
 * Treść snackbara dla zdarzenia koszyka: „Dodano do koszyka: {nazwa}”, przy osiągniętym limicie
 * „W koszyku jest już maks. 20 szt. tej pozycji”, przy błędzie zapisu prośba o ponowienie.
 * Limit w tekście pochodzi z [ProductConfiguration.MAX_QUANTITY]. Zwraca `null` dla zdarzeń bez snackbara
 * (nawigacja).
 */
internal suspend fun ProductDetailEvent.snackbarMessage(): String? = when (this) {
    is ProductDetailEvent.AddedToCart -> if (limitReached) {
        getString(Res.string.cart_limit_reached, ProductConfiguration.MAX_QUANTITY)
    } else {
        getString(Res.string.product_detail_added_to_cart, productName)
    }
    ProductDetailEvent.AddToCartFailed -> getString(Res.string.product_detail_add_to_cart_failed)
    ProductDetailEvent.NavigateBack, ProductDetailEvent.NavigateToCart -> null
}
