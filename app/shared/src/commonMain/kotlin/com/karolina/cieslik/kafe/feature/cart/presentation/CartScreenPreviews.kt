package com.karolina.cieslik.kafe.feature.cart.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.karolina.cieslik.kafe.core.presentation.designsystem.theme.KafeTheme

@PreviewLightDark
@Composable
private fun CartContentPreview() {
    KafeTheme {
        CartScreen(state = CartPreviewData.content, onAction = {})
    }
}

@PreviewLightDark
@Composable
private fun CartUnverifiedPreview() {
    KafeTheme {
        CartScreen(state = CartPreviewData.unverified, onAction = {})
    }
}

@PreviewLightDark
@Composable
private fun CartClearConfirmationPreview() {
    KafeTheme {
        CartScreen(state = CartPreviewData.content.copy(showClearConfirmation = true), onAction = {})
    }
}

@PreviewLightDark
@Composable
private fun CartEmptyPreview() {
    KafeTheme {
        CartScreen(state = CartUiState.Empty, onAction = {})
    }
}

@PreviewLightDark
@Composable
private fun CartLoadingPreview() {
    KafeTheme {
        CartScreen(state = CartUiState.Loading, onAction = {})
    }
}
