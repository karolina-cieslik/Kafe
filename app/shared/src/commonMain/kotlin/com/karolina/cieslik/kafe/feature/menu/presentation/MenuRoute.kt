package com.karolina.cieslik.kafe.feature.menu.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

// KAFE-6: Podpina MenuViewModel do bezstanowego MenuScreen i zamienia MenuEvent na nawigację.
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
