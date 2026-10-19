package com.karolina.cieslik.kafe.feature.cafe.presentation

/**
 * Stan ekranu wyboru lokalu; kontrakt uzgodniony z ui. Lista alfabetyczna, na razie bez odległości od
 * użytkownika; etykiety są już sformatowane.
 */
sealed interface CafePickerUiState {

    /** Brak zapamiętanych lokali i trwa ich pobieranie. */
    data object Loading : CafePickerUiState

    /**
     * Nie udało się pobrać lokali, a w pamięci nic nie ma; ekran pokazuje „Spróbuj ponownie”.
     *
     * @property message gotowy komunikat dla użytkownika.
     */
    data class Error(val message: String) : CafePickerUiState

    /**
     * Lista lokali.
     *
     * @property cafes lokale alfabetycznie po polsku; pusta lista, gdy serwer nie zwrócił lokali.
     * @property isFirstLaunch brak zapamiętanego wyboru – ekran startowy aplikacji, bez przycisku „wstecz”.
     * Ustalane raz i nie zmienia się po wyborze, żeby przycisk nie mignął przed przejściem do menu.
     */
    data class Content(
        val cafes: List<CafeUi>,
        val isFirstLaunch: Boolean,
    ) : CafePickerUiState
}

/**
 * Lokal na liście wyboru.
 *
 * @property id identyfikator lokalu – wraca w [CafePickerAction.CafeSelected].
 * @property name nazwa lokalu.
 * @property address adres w jednej linii.
 * @property openingHoursLabel godziny na dziś, np. „Dziś 7:00–20:00”, „Dziś zamknięte” albo „Do 2:00”, gdy
 * lokal jest jeszcze otwarty od wczoraj (godziny przez północ).
 * @property isOpenNow lokal otwarty w tej chwili, liczone w strefie Europe/Warsaw.
 * @property isSelected lokal zapamiętany jako wybrany.
 */
data class CafeUi(
    val id: String,
    val name: String,
    val address: String,
    val openingHoursLabel: String,
    val isOpenNow: Boolean,
    val isSelected: Boolean,
)

/** Akcje użytkownika na ekranie wyboru lokalu. */
sealed interface CafePickerAction {
    /** Wybór lokalu [id]; nieznane id jest ignorowane. */
    data class CafeSelected(val id: String) : CafePickerAction

    /** „Spróbuj ponownie” na ekranie błędu. */
    data object Retry : CafePickerAction

    /** Strzałka wstecz (tylko gdy ekran nie jest startowy). */
    data object BackClicked : CafePickerAction
}

/** Jednorazowe zdarzenia ekranu wyboru lokalu – zbiera je route ekranu. */
sealed interface CafePickerEvent {
    /** Wybór zapisany – nawigacja do menu (przy pierwszym uruchomieniu z wyczyszczeniem stosu). */
    data object CafeChosen : CafePickerEvent

    /**
     * Nie udało się zapisać wyboru (błąd pliku preferencji) – zostajemy na liście, poprzedni wybór bez zmian.
     * Ekran pokazuje krótki komunikat (np. snackbar) i pozwala wybrać lokal ponownie.
     */
    data object SelectionFailed : CafePickerEvent

    /** Powrót do poprzedniego ekranu bez zmiany wyboru. */
    data object NavigateBack : CafePickerEvent
}
