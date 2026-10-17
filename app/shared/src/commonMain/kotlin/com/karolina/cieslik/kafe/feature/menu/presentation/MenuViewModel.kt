package com.karolina.cieslik.kafe.feature.menu.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.Result
import com.karolina.cieslik.kafe.feature.cafe.domain.Cafe
import com.karolina.cieslik.kafe.feature.cafe.domain.CafeRepository
import com.karolina.cieslik.kafe.feature.menu.domain.CafeMenu
import com.karolina.cieslik.kafe.feature.menu.domain.GetMenuForCafe
import com.karolina.cieslik.kafe.feature.menu.domain.MenuRepository
import kotlinx.coroutines.CoroutineDispatcher
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
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// KAFE-6: Menu wybranego lokalu. Offline-first: zapamiętane menu od razu, odświeżenie w tle przy każdym
// (ponownym) rozpoczęciu obserwacji lokalu. Błąd odświeżenia przy istniejącym menu nie blokuje ekranu.
@OptIn(ExperimentalCoroutinesApi::class)
class MenuViewModel(
    cafeRepository: CafeRepository,
    private val menuRepository: MenuRepository,
    getMenuForCafe: GetMenuForCafe,
    private val dispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val selectedCategoryId = MutableStateFlow<String?>(null)
    private val searchQuery = MutableStateFlow("")
    private val refreshStatus = MutableStateFlow<RefreshStatus>(RefreshStatus.Idle)

    // Dostęp tylko z wątku Main (viewModelScope / onAction).
    private var lastCafeId: String? = null
    private var refreshJob: Job? = null

    private val eventChannel = Channel<MenuEvent>(Channel.BUFFERED)
    val events: Flow<MenuEvent> = eventChannel.receiveAsFlow()

    private val selectedCafe = cafeRepository.observeSelectedCafe()
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), replay = 1)

    // Menu zmienia się tylko przy zmianie id lokalu; cafeId w wyniku pozwala odrzucić menu poprzedniego lokalu.
    private val menuOfSelectedCafe: Flow<MenuOfCafe?> = selectedCafe
        .map { it?.id }
        .distinctUntilChanged()
        .flatMapLatest { cafeId ->
            if (cafeId == null) {
                flowOf(null)
            } else {
                getMenuForCafe(cafeId)
                    .map { MenuOfCafe(cafeId, it) }
                    .flowOn(dispatcher)
                    .onStart { startRefresh(cafeId) }
            }
        }

    val state: StateFlow<MenuUiState> = combine(
        selectedCafe,
        menuOfSelectedCafe,
        refreshStatus,
        selectedCategoryId,
        searchQuery,
        ::buildState,
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MenuUiState.Loading)

    fun onAction(action: MenuAction) {
        when (action) {
            is MenuAction.CategorySelected -> selectedCategoryId.value = action.id
            is MenuAction.SearchQueryChanged -> searchQuery.value = action.query
            is MenuAction.ProductClicked -> sendEvent(MenuEvent.NavigateToProduct(action.id))
            MenuAction.ChangeCafeClicked -> sendEvent(MenuEvent.NavigateToCafePicker)
            MenuAction.Retry, MenuAction.Refresh -> lastCafeId?.let { cafeId ->
                if (refreshJob?.isActive != true) startRefresh(cafeId)
            }
        }
    }

    private fun startRefresh(cafeId: String) {
        lastCafeId = cafeId
        refreshJob?.cancel()
        // Status ustawiany synchronicznie, żeby pierwszy stan bez menu był Loading, a nie Error poprzedniej próby.
        refreshStatus.value = RefreshStatus.InProgress(cafeId)
        refreshJob = viewModelScope.launch(dispatcher) {
            val newStatus = when (val result = menuRepository.refresh(cafeId)) {
                is Result.Success -> RefreshStatus.Idle
                is Result.Failure -> RefreshStatus.Failed(cafeId, result.error)
            }
            // Wynik spóźnionego odświeżenia innego lokalu nie może nadpisać bieżącego statusu.
            refreshStatus.update { current -> if (current.cafeId == cafeId) newStatus else current }
        }
    }

    private fun sendEvent(event: MenuEvent) {
        viewModelScope.launch { eventChannel.send(event) }
    }

    private fun buildState(
        cafe: Cafe?,
        menuOfCafe: MenuOfCafe?,
        refresh: RefreshStatus,
        selectedCategoryId: String?,
        query: String,
    ): MenuUiState {
        if (cafe == null) return MenuUiState.Loading
        val menu = menuOfCafe?.takeIf { it.cafeId == cafe.id }?.menu
        val status = refresh.takeIf { it.cafeId == cafe.id } ?: RefreshStatus.Idle
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
        )
    }

    private data class MenuOfCafe(val cafeId: String, val menu: CafeMenu?)

    private sealed interface RefreshStatus {
        val cafeId: String?

        data object Idle : RefreshStatus {
            override val cafeId: String? = null
        }

        data class InProgress(override val cafeId: String) : RefreshStatus

        data class Failed(override val cafeId: String, val error: DataError) : RefreshStatus
    }
}
