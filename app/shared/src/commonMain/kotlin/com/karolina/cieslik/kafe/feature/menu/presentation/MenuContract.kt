package com.karolina.cieslik.kafe.feature.menu.presentation

/** Stan ekranu menu; kontrakt uzgodniony między dev i ui (MenuScreen(state, onAction) jest bezstanowy). */
sealed interface MenuUiState {

    /** Brak zapamiętanych danych i trwa ich pobieranie. */
    data object Loading : MenuUiState

    /**
     * Menu wybranego lokalu gotowe do wyświetlenia.
     *
     * @property cafeName nazwa wybranego lokalu do nagłówka.
     * @property categories kategorie przefiltrowane wyszukiwaniem, każda z co najmniej jednym produktem;
     * pusta lista = stan Empty.
     * @property selectedCategoryId wybrany chip kategorii; nie jest null, gdy [categories] są niepuste
     * (domyślnie pierwsza kategoria).
     * @property isRefreshing trwa odświeżanie menu w tle – dane na ekranie są z pamięci.
     * @property searchQuery tekst wyszukiwarki dokładnie tak, jak go wpisano.
     * @property cartItemCount suma sztuk wszystkich pozycji w koszyku (licznik na ikonie koszyka); 0 = pusty.
     */
    data class Content(
        val cafeName: String,
        val categories: List<CategoryUi>,
        val selectedCategoryId: String?,
        val isRefreshing: Boolean,
        val searchQuery: String = "",
        val cartItemCount: Int = 0,
    ) : MenuUiState

    /**
     * Nie udało się pobrać danych, a w pamięci nic nie ma; ekran pokazuje „Spróbuj ponownie”.
     *
     * @property message gotowy komunikat dla użytkownika.
     */
    data class Error(val message: String) : MenuUiState
}

/**
 * Kategoria na ekranie menu.
 *
 * @property id identyfikator kategorii.
 * @property name nazwa na chipie i w nagłówku sekcji.
 * @property products produkty w kolejności sieci.
 */
data class CategoryUi(
    val id: String,
    val name: String,
    val products: List<ProductUi>,
)

/**
 * Karta produktu na ekranie menu.
 *
 * @property id identyfikator produktu, przekazywany przy kliknięciu.
 * @property name nazwa produktu.
 * @property subtitle opis pod nazwą; null, gdy produkt nie ma opisu.
 * @property priceLabel cena bazowa (najtańszy rozmiar) w formacie „12,00 zł”.
 * @property imageUrl adres zdjęcia; null = grafika zastępcza.
 * @property isAvailable false, gdy produkt jest chwilowo niedostępny w lokalu (karta wyszarzona).
 */
data class ProductUi(
    val id: String,
    val name: String,
    val subtitle: String?,
    val priceLabel: String,
    val imageUrl: String?,
    val isAvailable: Boolean,
)

/** Akcje użytkownika na ekranie menu. */
sealed interface MenuAction {
    /** Wybór chipa kategorii [id]. */
    data class CategorySelected(val id: String) : MenuAction
    /** Kliknięcie produktu [id] – otwiera szczegóły. */
    data class ProductClicked(val id: String) : MenuAction
    /** Zmiana tekstu wyszukiwarki na [query]. */
    data class SearchQueryChanged(val query: String) : MenuAction
    /** „Spróbuj ponownie” na ekranie błędu. */
    data object Retry : MenuAction
    /** Ręczne odświeżenie (pull-to-refresh). */
    data object Refresh : MenuAction
    /** Kliknięcie „Zmień lokal”. */
    data object ChangeCafeClicked : MenuAction
    /** Kliknięcie ikony koszyka – otwiera koszyk. */
    data object CartClicked : MenuAction
}

/** Jednorazowe zdarzenia nawigacyjne – zbiera je route ekranu, nie MenuScreen. */
sealed interface MenuEvent {
    /** Przejście do szczegółów produktu [productId]. */
    data class NavigateToProduct(val productId: String) : MenuEvent
    /** Przejście do wyboru lokalu. */
    data object NavigateToCafePicker : MenuEvent
    /** Przejście do koszyka. */
    data object NavigateToCart : MenuEvent
}
