package com.karolina.cieslik.kafe.feature.start.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karolina.cieslik.kafe.feature.cafe.domain.CafeRepository
import com.karolina.cieslik.kafe.navigation.KafeRoute
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Ekran startowy z zapamiętanego wyboru – tylko odczyt lokalny (Room + DataStore), więc Ready przychodzi od
 * razu, także offline. Pobieranie danych robią ekrany docelowe (z własnym Error/Retry), żeby nie dublować
 * odświeżeń. Zapamiętany lokal, którego nie ma już wśród lokali, traktujemy jak brak wyboru.
 */
class AppStartViewModel(
    private val cafeRepository: CafeRepository,
    private val dispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _state = MutableStateFlow<AppStartState>(AppStartState.Loading)
    /** [AppStartState.Loading], a po pierwszym odczycie wyboru jednorazowo [AppStartState.Ready]; potem bez zmian. */
    val state: StateFlow<AppStartState> = _state.asStateFlow()

    init {
        viewModelScope.launch(dispatcher) {
            val selectedCafe = cafeRepository.observeSelectedCafe().first()
            _state.value = AppStartState.Ready(if (selectedCafe == null) KafeRoute.CafePicker else KafeRoute.Menu)
        }
    }
}
