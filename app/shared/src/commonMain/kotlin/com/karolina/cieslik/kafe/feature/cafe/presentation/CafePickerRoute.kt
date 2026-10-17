package com.karolina.cieslik.kafe.feature.cafe.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

// KAFE-29: Podpina CafePickerViewModel do bezstanowego CafePickerScreen i zamienia CafePickerEvent na nawigację.
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
