package com.karolina.cieslik.kafe.feature.product.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.karolina.cieslik.kafe.core.presentation.designsystem.theme.KafeTheme

/** Dane fake do previewów szczegółów produktu. */
internal object ProductDetailPreviewData {
    private val milk = ModifierGroupUi(
        id = "milk",
        name = "Mleko",
        type = ModifierGroupTypeUi.SINGLE,
        isRequired = true,
        limitLabel = null,
        options = listOf(
            ModifierOptionUi("cow", "Krowie", "bez dopłaty", true, true, 0, false, false),
            ModifierOptionUi("oat", "Owsiane", "+2,00 zł", true, false, 0, false, false),
            ModifierOptionUi("soy", "Sojowe", "+2,00 zł", false, false, 0, false, false),
        ),
    )
    private val syrup = ModifierGroupUi(
        id = "syrup",
        name = "Syrop",
        type = ModifierGroupTypeUi.SINGLE,
        isRequired = false,
        limitLabel = null,
        options = listOf(
            ModifierOptionUi("vanilla", "Waniliowy", "+1,50 zł", true, true, 0, false, false),
            ModifierOptionUi("caramel", "Karmelowy", "+1,50 zł", true, false, 0, false, false),
        ),
    )
    private val shots = ModifierGroupUi(
        id = "extras",
        name = "Dodatki",
        type = ModifierGroupTypeUi.QUANTITY,
        isRequired = false,
        limitLabel = "maks. 4",
        options = listOf(
            ModifierOptionUi("shot", "Dodatkowe espresso", "+3,00 zł", true, true, 1, true, true),
        ),
    )

    val content = ProductDetailUiState.Content(
        name = "Cappuccino",
        description = "Klasyka włoskiej kawiarni: podwójne espresso z ziaren z Etiopii, gorące mleko i aksamitna " +
            "pianka. Najlepiej smakuje od razu, zanim pianka opadnie – podajemy w porcelanowej filiżance.",
        imageUrl = null,
        isAvailable = true,
        sizes = listOf(
            ProductSizeUi("S", "S", "250 ml", "12,00 zł", isAvailable = true, isSelected = false),
            ProductSizeUi("M", "M", "350 ml", "14,00 zł", isAvailable = true, isSelected = true),
            ProductSizeUi("L", "L", "450 ml", "16,00 zł", isAvailable = false, isSelected = false),
        ),
        modifierGroups = listOf(milk, syrup, shots),
        quantity = 1,
        canDecreaseQuantity = false,
        totalPriceLabel = "18,50 zł",
        canAddToCart = true,
        infoMessage = null,
    )

    val shotsAtLimit = content.copy(
        modifierGroups = listOf(
            milk,
            syrup,
            shots.copy(options = shots.options.map { it.copy(quantity = 4, canIncrease = false) }),
        ),
        quantity = 2,
        canDecreaseQuantity = true,
        totalPriceLabel = "61,00 zł",
    )

    val unavailable = content.copy(
        isAvailable = false,
        canAddToCart = false,
        infoMessage = "Ten produkt jest chwilowo niedostępny w Kafe Rynek.",
    )
}

/** Ekran w motywie Kafe dla podanego stanu – wspólne opakowanie previewów poniżej. */
@Composable
private fun ProductDetailPreview(state: ProductDetailUiState) {
    KafeTheme { ProductDetailScreen(state = state, onAction = {}) }
}

@PreviewLightDark
@Composable
private fun ProductDetailContentPreview() = ProductDetailPreview(ProductDetailPreviewData.content)

@Preview(heightDp = 1400)
@Composable
private fun ProductDetailShotsAtLimitPreview() = ProductDetailPreview(ProductDetailPreviewData.shotsAtLimit)

@PreviewLightDark
@Composable
private fun ProductDetailUnavailablePreview() = ProductDetailPreview(ProductDetailPreviewData.unavailable)

@Preview
@Composable
private fun ProductDetailLoadingPreview() = ProductDetailPreview(ProductDetailUiState.Loading)

@PreviewLightDark
@Composable
private fun ProductDetailErrorPreview() =
    ProductDetailPreview(ProductDetailUiState.Error("Nie udało się wczytać produktu. Sprawdź połączenie."))
