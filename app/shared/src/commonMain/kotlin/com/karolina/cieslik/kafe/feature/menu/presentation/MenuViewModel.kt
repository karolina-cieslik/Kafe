package com.karolina.cieslik.kafe.feature.menu.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.EmptyResult
import com.karolina.cieslik.kafe.core.domain.Result
import com.karolina.cieslik.kafe.feature.cafe.domain.Cafe
import com.karolina.cieslik.kafe.feature.cafe.domain.CafeRepository
import com.karolina.cieslik.kafe.feature.cart.domain.CartRepository
import com.karolina.cieslik.kafe.feature.menu.domain.CafeMenu
import com.karolina.cieslik.kafe.feature.menu.domain.GetMenuForCafe
import com.karolina.cieslik.kafe.feature.menu.domain.MenuRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Menu wybranego lokalu.
 *
 * Offline-first: zapamiętane dane od razu, a przy pierwszej obserwacji odświeżenie w tle – lokali, a potem
 * menu wybranego lokalu. Błąd odświeżenia przy zapamiętanych danych nie blokuje ekranu; bez zapamiętanych
 * danych kończy się stanem Error z „Spróbuj ponownie”. Licznik sztuk w koszyku pochodzi z [CartRepository].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MenuViewModel(
    private val cafeRepository: CafeRepository,
    private val menuRepository: MenuRepository,
    getMenuForCafe: GetMenuForCafe,
    cartRepository: CartRepository,
) : ViewModel() {

    private val selectedCategoryId = MutableStateFlow<String?>(null)
    private val searchQuery = MutableStateFlow("")
    private val cafesRefresh = MutableStateFlow<RefreshStatus>(RefreshStatus.Idle)
    private val menuRefresh = MutableStateFlow<RefreshStatus>(RefreshStatus.Idle)

    /** Lokal, którego menu jest obserwowane; dostęp tylko z wątku Main (viewModelScope / onAction). */
    private var currentCafeId: String? = null
    private var cafesRefreshJob: Job? = null
    private var menuRefreshJob: Job? = null

    private val eventChannel = Channel<MenuEvent>(Channel.BUFFERED)
    /** Jednorazowe zdarzenia nawigacyjne; każde trafia do jednego kolektora, bufor trzyma je do czasu zebrania. */
    val events: Flow<MenuEvent> = eventChannel.receiveAsFlow()

    /**
     * Lokale, których menu udało się już odświeżyć w tym VM. Odświeżenie w tle jest tylko przy pierwszym wejściu
     * na menu danego lokalu, więc powrót ze szczegółów nie pokazuje wskaźnika; ponownie: pull-to-refresh / Retry.
     * Tylko udane odświeżenia – po błędzie ponowne wejście na lokal znów próbuje pobrać menu.
     */
    private val refreshedMenuCafeIds = MutableStateFlow<Set<String>>(emptySet())

    private val selectedCafe = cafeRepository.observeSelectedCafe()
        .onStart { if (cafesRefreshJob == null) startCafesRefresh() }
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), replay = 1)

    /** Menu zmienia się tylko przy zmianie id lokalu; cafeId w wyniku pozwala odrzucić menu poprzedniego lokalu. */
    private val menuOfSelectedCafe: Flow<MenuOfCafe?> = selectedCafe
        .map { it?.id }
        .distinctUntilChanged()
        .flatMapLatest { cafeId ->
            currentCafeId = cafeId
            if (cafeId == null) {
                flowOf(null)
            } else {
                getMenuForCafe(cafeId)
                    .map { MenuOfCafe(cafeId, it) }
                    .onStart { if (cafeId !in refreshedMenuCafeIds.value) startMenuRefresh(cafeId) }
            }
        }

    private val cartItemCount: Flow<Int> = cartRepository.observeCart()
        .map { it.itemCount }
        .distinctUntilChanged()

    /**
     * Stan ekranu. Zaczyna od [MenuUiState.Loading], aktualizuje się po każdej zmianie lokalu, menu, statusu
     * odświeżania, kategorii, wyszukiwania i liczby sztuk w koszyku. Obserwacja trwa jeszcze 5 s po zniknięciu
     * ostatniego kolektora, żeby obrót ekranu nie restartował odświeżania.
     */
    val state: StateFlow<MenuUiState> = combine(
        selectedCafe,
        menuOfSelectedCafe,
        combine(cafesRefresh, menuRefresh, ::RefreshStatuses),
        selectedCategoryId,
        combine(searchQuery, cartItemCount, ::QueryAndCart),
        ::buildState,
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MenuUiState.Loading)

    /**
     * Obsługuje akcję użytkownika. Retry i Refresh nie startują drugiego odświeżania, gdy jedno już trwa.
     * Kliknięcia produktu, „Zmień lokal” i ikony koszyka emitują zdarzenie w [events].
     */
    fun onAction(action: MenuAction) {
        when (action) {
            is MenuAction.CategorySelected -> selectedCategoryId.value = action.id
            is MenuAction.SearchQueryChanged -> searchQuery.value = action.query
            is MenuAction.ProductClicked -> sendEvent(MenuEvent.NavigateToProduct(action.id))
            MenuAction.ChangeCafeClicked -> sendEvent(MenuEvent.NavigateToCafePicker)
            MenuAction.CartClicked -> sendEvent(MenuEvent.NavigateToCart)
            MenuAction.Retry, MenuAction.Refresh -> {
                // Bez lokalu nie ma czego odświeżać w menu – ponawiamy pobranie lokali (po sukcesie ruszy menu).
                val cafeId = currentCafeId
                if (cafeId == null) {
                    if (cafesRefreshJob?.isActive != true) startCafesRefresh()
                } else {
                    if (menuRefreshJob?.isActive != true) startMenuRefresh(cafeId)
                }
            }
        }
    }

    private fun startCafesRefresh() {
        cafesRefreshJob?.cancel()
        cafesRefresh.value = RefreshStatus.InProgress(CAFES_KEY)
        cafesRefreshJob = launchRefresh(cafesRefresh, CAFES_KEY) { cafeRepository.refresh() }
    }

    private fun startMenuRefresh(cafeId: String) {
        menuRefreshJob?.cancel()
        // Status ustawiany synchronicznie, żeby pierwszy stan bez menu był Loading, a nie Error poprzedniej próby.
        menuRefresh.value = RefreshStatus.InProgress(cafeId)
        menuRefreshJob = launchRefresh(menuRefresh, cafeId) {
            menuRepository.refresh(cafeId).also { result ->
                if (result is Result.Success) refreshedMenuCafeIds.update { it + cafeId }
            }
        }
    }

    private fun launchRefresh(
        status: MutableStateFlow<RefreshStatus>,
        key: String,
        refresh: suspend () -> EmptyResult<DataError>,
    ): Job = viewModelScope.launch {
        val newStatus = when (val result = refresh()) {
            is Result.Success -> RefreshStatus.Idle
            is Result.Failure -> RefreshStatus.Failed(key, result.error)
        }
        // Wynik spóźnionego odświeżenia (np. poprzedniego lokalu) nie może nadpisać bieżącego statusu.
        status.update { current -> if (current.key == key) newStatus else current }
    }

    private fun sendEvent(event: MenuEvent) {
        viewModelScope.launch { eventChannel.send(event) }
    }

    private fun buildState(
        cafe: Cafe?,
        menuOfCafe: MenuOfCafe?,
        refresh: RefreshStatuses,
        selectedCategoryId: String?,
        queryAndCart: QueryAndCart,
    ): MenuUiState {
        val query = queryAndCart.query
        if (cafe == null) {
            // Idle po udanym refresh też daje Loading: Room emituje zapisane lokale chwilę po zakończeniu zapisu.
            val cafes = refresh.cafes
            return if (cafes is RefreshStatus.Failed) {
                MenuUiState.Error(cafes.error.toMenuErrorMessage())
            } else {
                MenuUiState.Loading
            }
        }
        val menu = menuOfCafe?.takeIf { it.cafeId == cafe.id }?.menu
        val status = refresh.menu.takeIf { it.key == cafe.id } ?: RefreshStatus.Idle
        if (menu == null) {
            return if (status is RefreshStatus.Failed) {
                MenuUiState.Error(status.error.toMenuErrorMessage())
            } else {
                MenuUiState.Loading
            }
        }
        val categories = menu.filterByProductName(query).sections.map { it.toCategoryUi() }
        return MenuUiState.Content(
            cafeName = cafe.name,
            categories = categories,
            selectedCategoryId = selectedCategoryId?.takeIf { id -> categories.any { it.id == id } }
                ?: categories.firstOrNull()?.id,
            isRefreshing = status is RefreshStatus.InProgress,
            searchQuery = query,
            cartItemCount = queryAndCart.cartItemCount,
        )
    }

    private data class MenuOfCafe(val cafeId: String, val menu: CafeMenu?)

    private data class RefreshStatuses(val cafes: RefreshStatus, val menu: RefreshStatus)

    private data class QueryAndCart(val query: String, val cartItemCount: Int)

    /** [key] – id lokalu dla menu albo [CAFES_KEY] dla listy lokali. */
    private sealed interface RefreshStatus {
        val key: String?

        data object Idle : RefreshStatus {
            override val key: String? = null
        }

        data class InProgress(override val key: String) : RefreshStatus

        data class Failed(override val key: String, val error: DataError) : RefreshStatus
    }

    private companion object {
        const val CAFES_KEY = "cafes"
    }
}
