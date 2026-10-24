package com.karolina.cieslik.kafe.feature.menu.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.karolina.cieslik.kafe.core.presentation.designsystem.theme.KafeTheme

/** Dane fake do previewów ekranu menu. */
internal object MenuPreviewData {
    private val coffee = CategoryUi(
        id = "coffee",
        name = "Kawa",
        products = listOf(
            ProductUi("cappuccino", "Cappuccino", "Espresso, spienione mleko", "12,00 zł", null, true),
            ProductUi("latte", "Latte", "Z mlekiem owsianym", "13,50 zł", null, true),
            ProductUi("flat-white", "Flat white", "Podwójne espresso", "14,00 zł", null, false),
            ProductUi("americano", "Americano", null, "10,00 zł", null, true),
        ),
    )
    private val tea = CategoryUi(
        id = "tea",
        name = "Herbata",
        products = listOf(
            ProductUi("matcha", "Matcha latte", "Japońska matcha", "16,00 zł", null, true),
            ProductUi("chai", "Chai latte", "Korzenne przyprawy", "15,00 zł", null, true),
        ),
    )
    private val desserts = CategoryUi(
        id = "desserts",
        name = "Desery",
        products = listOf(
            ProductUi("sernik", "Sernik baskijski", null, "18,00 zł", null, true),
        ),
    )

    val content = MenuUiState.Content(
        cafeName = "Kafe Kazimierz, ul. Józefa 12, Kraków",
        categories = listOf(coffee, tea, desserts),
        selectedCategoryId = coffee.id,
        isRefreshing = false,
        cartItemCount = 3,
    )

    val search = content.copy(
        searchQuery = "latte",
        categories = listOf(
            coffee.copy(products = coffee.products.filter { "latte" in it.name.lowercase() }),
            tea.copy(products = tea.products.filter { "latte" in it.name.lowercase() }),
        ),
    )
}

@Composable
private fun MenuPreview(state: MenuUiState) {
    KafeTheme { MenuScreen(state = state, onAction = {}) }
}

@PreviewLightDark
@Composable
private fun MenuContentPreview() = MenuPreview(MenuPreviewData.content)

@Preview
@Composable
private fun MenuRefreshingPreview() = MenuPreview(MenuPreviewData.content.copy(isRefreshing = true))

@Preview
@Composable
private fun MenuCartOverflowPreview() = MenuPreview(MenuPreviewData.content.copy(cartItemCount = 120))

@PreviewLightDark
@Composable
private fun MenuSearchPreview() = MenuPreview(MenuPreviewData.search)

@Preview
@Composable
private fun MenuSearchEmptyPreview() =
    MenuPreview(MenuPreviewData.content.copy(searchQuery = "mocha", categories = emptyList()))

@Preview
@Composable
private fun MenuEmptyPreview() = MenuPreview(MenuPreviewData.content.copy(categories = emptyList()))

@PreviewLightDark
@Composable
private fun MenuLoadingPreview() = MenuPreview(MenuUiState.Loading)

@PreviewLightDark
@Composable
private fun MenuErrorPreview() =
    MenuPreview(MenuUiState.Error("Brak połączenia z internetem. Sprawdź sieć i spróbuj ponownie."))
