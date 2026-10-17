package com.karolina.cieslik.kafe.feature.menu.presentation

// KAFE-6: Kontrakt ekranu menu uzgodniony między dev i ui (MenuScreen(state, onAction) jest bezstanowy).
sealed interface MenuUiState {

    data object Loading : MenuUiState

    /**
     * [categories] są już przefiltrowane wyszukiwaniem; pusta lista = stan Empty.
     * [selectedCategoryId] nie jest null, gdy [categories] są niepuste (domyślnie pierwsza kategoria).
     */
    data class Content(
        val cafeName: String,
        val categories: List<CategoryUi>,
        val selectedCategoryId: String?,
        val isRefreshing: Boolean,
        val searchQuery: String = "",
    ) : MenuUiState

    data class Error(val message: String) : MenuUiState
}

data class CategoryUi(
    val id: String,
    val name: String,
    val products: List<ProductUi>,
)

data class ProductUi(
    val id: String,
    val name: String,
    val subtitle: String?,
    /** Cena bazowa (najtańszy rozmiar) w formacie „12,00 zł”. */
    val priceLabel: String,
    val imageUrl: String?,
    val isAvailable: Boolean,
)

sealed interface MenuAction {
    data class CategorySelected(val id: String) : MenuAction
    data class ProductClicked(val id: String) : MenuAction
    data class SearchQueryChanged(val query: String) : MenuAction
    data object Retry : MenuAction
    data object Refresh : MenuAction
    data object ChangeCafeClicked : MenuAction
}

/** Jednorazowe zdarzenia nawigacyjne – zbiera je route ekranu, nie MenuScreen. */
sealed interface MenuEvent {
    data class NavigateToProduct(val productId: String) : MenuEvent
    data object NavigateToCafePicker : MenuEvent
}
