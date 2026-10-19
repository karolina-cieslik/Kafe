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
import com.karolina.cieslik.kafe.feature.cafe.domain.previous
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

/**
 * Lista lokali alfabetycznie (na razie bez odległości) z „otwarte teraz” w strefie lokali.
 *
 * Zapamiętane lokale od razu, odświeżenie w tle raz na życie VM przy pierwszej obserwacji; ponownie tylko
 * przez Retry. „Otwarte teraz” liczone przy każdej zmianie danych, bez cyklicznego zegara – ekran otwarty
 * o 19:59 nie przełączy się sam na „zamknięte”.
 *
 * @param clock źródło bieżącego czasu; w testach stały.
 * @param timeZone strefa godzin otwarcia lokali (Europe/Warsaw).
 * @param dispatcher dispatcher odświeżania; w testach testowy.
 */
class CafePickerViewModel(
    private val cafeRepository: CafeRepository,
    private val clock: Clock,
    private val timeZone: TimeZone,
    private val dispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val refreshStatus = MutableStateFlow<RefreshStatus>(RefreshStatus.Idle)

    /** Bieżące odświeżenie; null = jeszcze nie startowało. Dostęp tylko z wątku Main (viewModelScope / onAction). */
    private var refreshJob: Job? = null
    private var selectJob: Job? = null
    /** Raz ustalone „pierwsze uruchomienie” nie znika po wyborze – UI nie mignie przyciskiem „wstecz”. */
    private var isFirstLaunch: Boolean? = null

    private val eventChannel = Channel<CafePickerEvent>(Channel.BUFFERED)
    /** Jednorazowe zdarzenia (wybór zapisany albo nieudany, powrót); każde trafia do jednego kolektora. */
    val events: Flow<CafePickerEvent> = eventChannel.receiveAsFlow()

    /**
     * Stan ekranu. Zaczyna od [CafePickerUiState.Loading], aktualizuje się po każdej zmianie lokali, wyboru
     * i statusu odświeżenia. Obserwacja trwa jeszcze 5 s po zniknięciu ostatniego kolektora.
     */
    val state: StateFlow<CafePickerUiState> = combine(
        // Odświeżenie raz na życie VM (powrót z tła go nie powtarza); ponownie tylko przez Retry.
        cafeRepository.observeCafes().onStart { if (refreshJob == null) startRefresh() },
        cafeRepository.observeSelectedCafe(),
        refreshStatus,
        ::buildState,
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CafePickerUiState.Loading)

    /**
     * Obsługuje akcję użytkownika. Wybór lokalu zapisuje go i emituje [CafePickerEvent.CafeChosen], a gdy zapis
     * się nie uda – [CafePickerEvent.SelectionFailed] (ekran zostaje, można wybrać ponownie). Drugi wybór
     * w trakcie zapisu i nieznane id są ignorowane. Retry nie startuje drugiego odświeżania, gdy jedno trwa.
     */
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
            val event = when (cafeRepository.selectCafe(cafeId)) {
                is Result.Success -> CafePickerEvent.CafeChosen
                is Result.Failure -> CafePickerEvent.SelectionFailed
            }
            eventChannel.send(event)
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
                    openingHoursLabel = cafe.todayLabel(today, minuteOfDay),
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

/** Otwarte jeszcze z wczoraj (godziny przez północ) – „Do 2:00”, żeby etykieta nie przeczyła „otwarte teraz”. */
internal fun Cafe.todayLabel(today: Weekday, minuteOfDay: Int): String {
    val yesterday = openingHoursOn(today.previous())
    if (yesterday != null && yesterday.closesAtMinute <= yesterday.opensAtMinute && minuteOfDay < yesterday.closesAtMinute) {
        return "Do ${yesterday.closesAtMinute.toClockLabel()}"
    }
    val hours = openingHoursOn(today) ?: return CAFE_CLOSED_TODAY
    return "Dziś ${hours.opensAtMinute.toClockLabel()}–${hours.closesAtMinute.toClockLabel()}"
}

/** Minuty od północy jako godzina, np. 450 → „7:30”. */
internal fun Int.toClockLabel(): String = "${this / 60}:${(this % 60).toString().padStart(2, '0')}"

/** Komunikat błędu: osobny dla braku sieci i timeoutu, ogólny dla pozostałych błędów. */
internal fun DataError.toCafePickerErrorMessage(): String = when (this) {
    DataError.Remote.NO_INTERNET, DataError.Remote.REQUEST_TIMEOUT -> CAFE_PICKER_ERROR_NO_CONNECTION
    else -> CAFE_PICKER_ERROR_GENERIC
}

/** Etykieta dnia bez godzin otwarcia. Teksty docelowo w zasobach stringów (composeResources), gdy dojdzie lokalizacja. */
internal const val CAFE_CLOSED_TODAY = "Dziś zamknięte"
/** Brak połączenia przy pobieraniu lokali. */
internal const val CAFE_PICKER_ERROR_NO_CONNECTION = "Nie udało się pobrać listy lokali. Sprawdź połączenie."
/** Pozostałe błędy pobierania lokali. */
internal const val CAFE_PICKER_ERROR_GENERIC = "Nie udało się pobrać listy lokali. Spróbuj ponownie później."
