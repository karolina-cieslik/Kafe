package com.karolina.cieslik.kafe.feature.cafe.presentation

// KAFE-29 / KAFE-31: Kontrakt ekranu wyboru lokalu (uzgodniony z ui).
// Lista alfabetyczna, bez odległości (lokalizacja – KAFE-32); etykiety są już sformatowane.
sealed interface CafePickerUiState {

    data object Loading : CafePickerUiState

    data class Error(val message: String) : CafePickerUiState

    data class Content(
        val cafes: List<CafeUi>,
        /** Brak zapamiętanego wyboru – ekran startowy aplikacji, bez przycisku „wstecz” (KAFE-31). */
        val isFirstLaunch: Boolean,
    ) : CafePickerUiState
}

data class CafeUi(
    val id: String,
    val name: String,
    val address: String,
    /** Godziny na dziś, np. „Dziś 7:00–20:00” albo „Dziś zamknięte”. */
    val openingHoursLabel: String,
    /** Liczone w strefie Europe/Warsaw. */
    val isOpenNow: Boolean,
    val isSelected: Boolean,
)

sealed interface CafePickerAction {
    data class CafeSelected(val id: String) : CafePickerAction
    data object Retry : CafePickerAction
    data object BackClicked : CafePickerAction
}

sealed interface CafePickerEvent {
    /** Wybór zapisany – nawigacja do menu (przy pierwszym uruchomieniu z wyczyszczeniem stosu). */
    data object CafeChosen : CafePickerEvent
    data object NavigateBack : CafePickerEvent
}
