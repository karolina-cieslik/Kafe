package com.karolina.cieslik.kafe.feature.cart.presentation

/** Stan ekranu koszyka. */
sealed interface CartUiState {

    /** Pierwszy odczyt koszyka jeszcze trwa. */
    data object Loading : CartUiState

    /** Koszyk jest pusty – „Twój koszyk jest pusty” i „Przejdź do menu”. */
    data object Empty : CartUiState

    /**
     * Koszyk z pozycjami.
     *
     * @property cafeName nazwa wybranego lokalu („Odbiór w: …”); null, gdy lokalu nie wybrano albo zapamiętany
     * lokal zniknął – wtedy ceny są z migawki, bez oznaczeń.
     * @property items pozycje w kolejności dodania.
     * @property summary podsumowanie przypięte do dołu ekranu.
     * @property pricesUnverified pokazać baner „Nie udało się sprawdzić aktualnych cen” (tekst w zasobach UI):
     * brak lokalu albo nieudane pobranie menu lokalu, którego nie ma w pamięci. W trakcie pierwszego pobierania –
     * false.
     * @property showClearConfirmation pokazać dialog „Czy na pewno usunąć wszystkie pozycje?”; w stanie, żeby
     * przetrwał obrót ekranu.
     */
    data class Content(
        val cafeName: String?,
        val items: List<CartItemUi>,
        val summary: CartSummaryUi,
        val pricesUnverified: Boolean,
        val showClearConfirmation: Boolean,
    ) : CartUiState
}

/**
 * Pozycja na liście koszyka.
 *
 * @property id id pozycji – wraca w akcjach [CartAction].
 * @property name nazwa produktu z migawki pozycji.
 * @property imageUrl zdjęcie produktu z bieżącego menu; null = grafika zastępcza (także gdy produktu nie ma
 * w menu).
 * @property sizeLabel rozmiar, np. „Średni”.
 * @property modifiersLabel modyfikatory w jednej linii, np. „Owsiane, 2× Shot espresso”; bez domyślnych opcji
 * bez dopłaty (np. „Krowie”); null = nic do pokazania.
 * @property quantity liczba sztuk (tylko w stepperze).
 * @property canIncrease „+” aktywny – false przy 20 sztukach.
 * @property unitPriceLabel cena jednej sztuki, np. „16,00 zł”.
 * @property totalPriceLabel cena wszystkich sztuk.
 * @property status oznaczenie pozycji; [CartItemStatusUi.Unavailable] = bez steppera, tylko kosz.
 */
data class CartItemUi(
    val id: Long,
    val name: String,
    val imageUrl: String?,
    val sizeLabel: String,
    val modifiersLabel: String?,
    val quantity: Int,
    val canIncrease: Boolean,
    val unitPriceLabel: String,
    val totalPriceLabel: String,
    val status: CartItemStatusUi,
)

/** Oznaczenie pozycji koszyka. */
sealed interface CartItemStatusUi {

    /** Bez oznaczenia. */
    data object Ok : CartItemStatusUi

    /**
     * „Cena została zaktualizowana” – widoczne do wyjścia z ekranu koszyka.
     *
     * @property previousUnitPriceLabel poprzednia cena jednej sztuki.
     */
    data class PriceUpdated(val previousUnitPriceLabel: String) : CartItemStatusUi

    /** „Niedostępne – usuń, aby kontynuować”; pozycja bez steppera. */
    data object Unavailable : CartItemStatusUi
}

/**
 * Podsumowanie koszyka – obie wartości bez pozycji niedostępnych.
 *
 * @property itemCount liczba sztuk.
 * @property payableTotalLabel „Razem do zapłaty”, np. „32,00 zł” (ceny brutto, „Ceny zawierają VAT” w UI).
 */
data class CartSummaryUi(val itemCount: Int, val payableTotalLabel: String)

/** Akcje użytkownika na ekranie koszyka. */
sealed interface CartAction {

    /** „+” na pozycji [itemId]; szybkie stuknięcia są kolejkowane, nie gubione. */
    data class IncreaseClicked(val itemId: Long) : CartAction

    /** „−” na pozycji [itemId]; przy 1 sztuce usuwa pozycję (z „Cofnij”). */
    data class DecreaseClicked(val itemId: Long) : CartAction

    /** Ikona kosza na pozycji [itemId] (z „Cofnij”). */
    data class RemoveClicked(val itemId: Long) : CartAction

    /** „Cofnij” w snackbarze po usunięciu. */
    data object UndoClicked : CartAction

    /** „Wyczyść koszyk” – otwiera dialog potwierdzenia. */
    data object ClearClicked : CartAction

    /** Potwierdzenie w dialogu – usuwa wszystkie pozycje, bez „Cofnij”. */
    data object ClearConfirmed : CartAction

    /** Anulowanie albo zamknięcie dialogu. */
    data object ClearDismissed : CartAction

    /** „Przejdź do menu” w pustym koszyku. */
    data object GoToMenuClicked : CartAction

    /** Strzałka wstecz. */
    data object BackClicked : CartAction
}

/** Jednorazowe zdarzenia ekranu koszyka. */
sealed interface CartEvent {

    /**
     * Snackbar „Usunięto: {nazwa}” z akcją „Cofnij” (`SnackbarDuration.Long`); nowy zastępuje poprzedni.
     *
     * @property productName nazwa usuniętej pozycji.
     */
    data class ItemRemoved(val productName: String) : CartEvent

    /** Zamknąć snackbar „Cofnij” – po wyczyszczeniu koszyka nie ma już czego przywracać. */
    data object UndoExpired : CartEvent

    /**
     * „Cofnij” scaliło pozycję z dodaną w międzyczasie i suma doszła do limitu: „W koszyku jest już maks. 20 szt.
     * tej pozycji”.
     *
     * @property productName nazwa przywróconej pozycji.
     */
    data class RestoredAtLimit(val productName: String) : CartEvent

    /** Zapis się nie udał: „Nie udało się zmienić koszyka. Spróbuj ponownie.” */
    data object ActionFailed : CartEvent

    /** Powrót do menu (z pustego koszyka). */
    data object NavigateToMenu : CartEvent

    /** Powrót do poprzedniego ekranu. */
    data object NavigateBack : CartEvent
}
