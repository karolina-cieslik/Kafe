package com.karolina.cieslik.kafe.feature.cafe.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.Result
import com.karolina.cieslik.kafe.feature.cafe.domain.Cafe
import com.karolina.cieslik.kafe.feature.cafe.domain.CafeRepository
import com.karolina.cieslik.kafe.feature.cafe.domain.PolishAlphabeticalOrder
import com.karolina.cieslik.kafe.feature.cafe.domain.Weekday
import com.karolina.cieslik.kafe.feature.cafe.domain.isOpenAt
import com.karolina.cieslik.kafe.feature.cafe.domain.openingHoursOn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

// KAFE-29 / KAFE-31: Lista lokali alfabetycznie (bez odległości – KAFE-32) z „otwarte teraz” w strefie lokali
// (Europe/Warsaw). Zapamiętane lokale od razu, odświeżenie w tle przy otwarciu ekranu.
// „Otwarte teraz” liczone przy każdej zmianie danych, bez cyklicznego zegara.
class CafePickerViewModel(
    private val cafeRepository: CafeRepository,
    private val clock: Clock,
    private val timeZone: TimeZone,
    private val dispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val refreshStatus = MutableStateFlow<RefreshStatus>(RefreshStatus.Idle)

    // Dostęp tylko z wątku Main (viewModelScope / onAction).
    private var refreshJob: Job? = null
    private var selectJob: Job? = null
    /** Raz ustalone „pierwsze uruchomienie” nie znika po wyborze – UI nie mignie przyciskiem „wstecz”. */
    private var isFirstLaunch: Boolean? = null

    private val eventChannel = Channel<CafePickerEvent>(Channel.BUFFERED)
    val events: Flow<CafePickerEvent> = eventChannel.receiveAsFlow()

    val state: StateFlow<CafePickerUiState> = combine(
        // Odświeżenie raz na życie VM (powrót z tła go nie powtarza); ponownie tylko przez Retry.
        cafeRepository.observeCafes().onStart { if (refreshJob == null) startRefresh() },
        cafeRepository.observeSelectedCafe(),
        refreshStatus,
        ::buildState,
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CafePickerUiState.Loading)

    fun onAction(action: CafePickerAction) {
        when (action) {
            is CafePickerAction.CafeSelected -> select(action.id)
            CafePickerAction.Retry -> if (refreshJob?.isActive != true) startRefresh()
            CafePickerAction.BackClicked -> sendEvent(CafePickerEvent.NavigateBack)
        }
    }

    private fun select(cafeId: String) {
        if (selectJob?.isActive == true) return
        val content = state.value as? CafePickerUiState.Content ?: return
        if (content.cafes.none { it.id == cafeId }) return
        selectJob = viewModelScope.launch {
            cafeRepository.selectCafe(cafeId)
            eventChannel.send(CafePickerEvent.CafeChosen)
        }
    }

    private fun startRefresh() {
        refreshJob?.cancel()
        refreshStatus.value = RefreshStatus.InProgress
        refreshJob = viewModelScope.launch(dispatcher) {
            refreshStatus.value = when (val result = cafeRepository.refresh()) {
                is Result.Success -> RefreshStatus.Done
                is Result.Failure -> RefreshStatus.Failed(result.error)
            }
        }
    }

    private fun sendEvent(event: CafePickerEvent) {
        viewModelScope.launch { eventChannel.send(event) }
    }

    private fun buildState(cafes: List<Cafe>, selected: Cafe?, status: RefreshStatus): CafePickerUiState {
        if (cafes.isEmpty()) {
            return when (status) {
                is RefreshStatus.Failed -> CafePickerUiState.Error(status.error.toCafePickerErrorMessage())
                // Pusta lista z serwera to poprawny wynik; Idle/InProgress = jeszcze czekamy na dane.
                RefreshStatus.Done -> CafePickerUiState.Content(emptyList(), isFirstLaunch(selected))
                else -> CafePickerUiState.Loading
            }
        }
        val now = clock.now().toLocalDateTime(timeZone)
        val today = Weekday.valueOf(now.dayOfWeek.name)
        val minuteOfDay = now.hour * 60 + now.minute
        return CafePickerUiState.Content(
            cafes = cafes.sortedWith(compareBy(PolishAlphabeticalOrder) { it.name }).map { cafe ->
                CafeUi(
                    id = cafe.id,
                    name = cafe.name,
                    address = cafe.address,
                    openingHoursLabel = cafe.todayLabel(today),
                    isOpenNow = cafe.isOpenAt(today, minuteOfDay),
                    isSelected = cafe.id == selected?.id,
                )
            },
            isFirstLaunch = isFirstLaunch(selected),
        )
    }

    private fun isFirstLaunch(selected: Cafe?): Boolean =
        isFirstLaunch ?: (selected == null).also { isFirstLaunch = it }

    private sealed interface RefreshStatus {
        data object Idle : RefreshStatus
        data object InProgress : RefreshStatus
        data object Done : RefreshStatus
        data class Failed(val error: DataError) : RefreshStatus
    }
}

internal fun Cafe.todayLabel(today: Weekday): String {
    val hours = openingHoursOn(today) ?: return CAFE_CLOSED_TODAY
    return "Dziś ${hours.opensAtMinute.toClockLabel()}–${hours.closesAtMinute.toClockLabel()}"
}

internal fun Int.toClockLabel(): String = "${this / 60}:${(this % 60).toString().padStart(2, '0')}"

internal fun DataError.toCafePickerErrorMessage(): String = when (this) {
    DataError.Remote.NO_INTERNET, DataError.Remote.REQUEST_TIMEOUT -> CAFE_PICKER_ERROR_NO_CONNECTION
    else -> CAFE_PICKER_ERROR_GENERIC
}

// Docelowo do zasobów stringów (composeResources), gdy dojdzie lokalizacja.
internal const val CAFE_CLOSED_TODAY = "Dziś zamknięte"
internal const val CAFE_PICKER_ERROR_NO_CONNECTION = "Nie udało się pobrać listy lokali. Sprawdź połączenie."
internal const val CAFE_PICKER_ERROR_GENERIC = "Nie udało się pobrać listy lokali. Spróbuj ponownie później."
