package com.karolina.cieslik.kafe.feature.cart.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.karolina.cieslik.kafe.core.presentation.designsystem.theme.KafeTheme

/** Dane fake do previewów i testów ekranu koszyka. */
internal object CartPreviewData {
    val latte = CartItemUi(
        id = 1,
        name = "Latte",
        imageUrl = null,
        sizeLabel = "Średni",
        modifiersLabel = "Owsiane, 2× Shot espresso",
        quantity = 2,
        canIncrease = true,
        unitPriceLabel = "22,00 zł",
        totalPriceLabel = "44,00 zł",
        status = CartItemStatusUi.Ok,
    )
    val cappuccino = CartItemUi(
        id = 2,
        name = "Cappuccino",
        imageUrl = null,
        sizeLabel = "Mały",
        modifiersLabel = null,
        quantity = 1,
        canIncrease = true,
        unitPriceLabel = "15,00 zł",
        totalPriceLabel = "15,00 zł",
        status = CartItemStatusUi.PriceUpdated(previousUnitPriceLabel = "14,00 zł"),
    )
    val flatWhite = CartItemUi(
        id = 3,
        name = "Flat white",
        imageUrl = null,
        sizeLabel = "Średni",
        modifiersLabel = null,
        quantity = 1,
        canIncrease = true,
        unitPriceLabel = "17,00 zł",
        totalPriceLabel = "17,00 zł",
        status = CartItemStatusUi.Unavailable,
    )
    val espressoAtLimit = CartItemUi(
        id = 4,
        name = "Espresso",
        imageUrl = null,
        sizeLabel = "Mały",
        modifiersLabel = null,
        quantity = 20,
        canIncrease = false,
        unitPriceLabel = "9,00 zł",
        totalPriceLabel = "180,00 zł",
        status = CartItemStatusUi.Ok,
    )

    val content = CartUiState.Content(
        cafeName = "Kafe Kazimierz",
        items = listOf(latte, cappuccino, flatWhite),
        summary = CartSummaryUi(itemCount = 3, payableTotalLabel = "59,00 zł"),
        pricesUnverified = false,
        showClearConfirmation = false,
    )

    val unverified = CartUiState.Content(
        cafeName = null,
        items = listOf(latte.copy(status = CartItemStatusUi.Ok), espressoAtLimit),
        summary = CartSummaryUi(itemCount = 22, payableTotalLabel = "224,00 zł"),
        pricesUnverified = true,
        showClearConfirmation = false,
    )
}

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
