package com.karolina.cieslik.kafe.feature.cart.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.Money
import com.karolina.cieslik.kafe.core.domain.Result
import com.karolina.cieslik.kafe.feature.cafe.domain.Cafe
import com.karolina.cieslik.kafe.feature.cafe.domain.CafeRepository
import com.karolina.cieslik.kafe.feature.cart.domain.CartItem
import com.karolina.cieslik.kafe.feature.cart.domain.CartItemStatus
import com.karolina.cieslik.kafe.feature.cart.domain.CartRepository
import com.karolina.cieslik.kafe.feature.cart.domain.CheckedCart
import com.karolina.cieslik.kafe.feature.cart.domain.CheckedCartItem
import com.karolina.cieslik.kafe.feature.cart.domain.DecreaseOutcome
import com.karolina.cieslik.kafe.feature.cart.domain.ObserveCheckedCart
import com.karolina.cieslik.kafe.feature.cart.domain.RefreshCartSnapshots
import com.karolina.cieslik.kafe.feature.cart.domain.RestoreOutcome
import com.karolina.cieslik.kafe.feature.menu.domain.MenuRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Ekran koszyka: pozycje sprawdzone z menu wybranego lokalu, zmiany ilości, „Cofnij” i wyczyszczenie.
 *
 * Przy pierwszej obserwacji lokalu VM raz pobiera jego menu w tle; po udanym pobraniu zapisuje nowe migawki cen
 * ([RefreshCartSnapshots]) i do końca życia VM oznacza zmienione pozycje „Cena została zaktualizowana”. Raz na VM
 * usuwa też nieczytelne wiersze koszyka (błąd bezpiecznika nie jest pokazywany – i tak są pominięte).
 *
 * Stan tylko w pamięci VM (dialog wyczyszczenia, ostatnia usunięta pozycja, oznaczenia cen): przetrwa obrót
 * ekranu, a po śmierci procesu ekran startuje bez dialogu, bez „Cofnij” i bez oznaczeń – koszyk jest w bazie.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CartViewModel(
    cafeRepository: CafeRepository,
    private val menuRepository: MenuRepository,
    private val cartRepository: CartRepository,
    private val observeCheckedCart: ObserveCheckedCart,
    private val refreshCartSnapshots: RefreshCartSnapshots,
) : ViewModel() {

    private val refreshes = MutableStateFlow<Map<String, MenuRefresh>>(emptyMap())
    private val previousPrices = MutableStateFlow<Map<Long, Money>>(emptyMap())
    private val showClearConfirmation = MutableStateFlow(false)

    /** Ostatnia usunięta pozycja do „Cofnij”; dostęp tylko pod [writes]. */
    private var lastRemoved: CartItem? = null

    /** Zapisy po kolei, w kolejności kliknięć (Mutex jest sprawiedliwy) – szybkie „+ + +” niczego nie gubi. */
    private val writes = Mutex()

    private val eventChannel = Channel<CartEvent>(Channel.BUFFERED)

    /** Jednorazowe zdarzenia (snackbary, nawigacja); każde trafia do jednego kolektora. */
    val events: Flow<CartEvent> = eventChannel.receiveAsFlow()

    private val cartOfCafe: Flow<CartOfCafe> = cafeRepository.observeSelectedCafe()
        .distinctUntilChanged()
        .onEach { cafe -> cafe?.let { startRefreshOnce(it.id) } }
        .flatMapLatest { cafe ->
            if (cafe == null) {
                cartRepository.observeCart().map { cart ->
                    val items = cart.items.map { CheckedCartItem(it, CartItemStatus.Unverified, it.unitPrice) }
                    CartOfCafe(null, CheckedCart(items, isVerified = false), emptyMap())
                }
            } else {
                combine(observeCheckedCart(cafe.id), menuRepository.observeMenu(cafe.id)) { checked, menu ->
                    val images = menu?.products.orEmpty().mapNotNull { p -> p.imageUrl?.let { p.id to it } }.toMap()
                    CartOfCafe(cafe, checked, images)
                }
            }
        }

    /**
     * Stan ekranu. Zaczyna od [CartUiState.Loading]; aktualizuje się po każdej zmianie koszyka, menu, lokalu,
     * wyniku pobrania menu i dialogu. Obserwacja trwa jeszcze 5 s po zniknięciu kolektora (obrót ekranu).
     */
    val state: StateFlow<CartUiState> =
        combine(cartOfCafe, refreshes, previousPrices, showClearConfirmation, ::buildState)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CartUiState.Loading)

    init {
        viewModelScope.launch { cartRepository.removeUnreadable() }
    }

    /**
     * Obsługuje akcję użytkownika. Zapisy idą po kolei w kolejności kliknięć i kończą się także po zamknięciu
     * ekranu (bez zdarzenia). Akcja na pozycji, której już nie ma (np. drugie „−” na 1 sztuce), jest pomijana bez
     * komunikatu; inny błąd zapisu daje [CartEvent.ActionFailed]. Usunięcie (kosz albo „−” na 1) emituje
     * [CartEvent.ItemRemoved] i zapamiętuje pozycję do „Cofnij”; wyczyszczenie koszyka ją zapomina
     * ([CartEvent.UndoExpired]).
     */
    fun onAction(action: CartAction) {
        when (action) {
            is CartAction.IncreaseClicked -> write { cartRepository.increase(action.itemId).failureEvent() }
            is CartAction.DecreaseClicked -> write { decrease(action.itemId) }
            is CartAction.RemoveClicked -> write { remove(action.itemId) }
            CartAction.UndoClicked -> write { undo() }
            CartAction.ClearClicked -> showClearConfirmation.value = true
            CartAction.ClearDismissed -> showClearConfirmation.value = false
            CartAction.ClearConfirmed -> {
                showClearConfirmation.value = false
                write { clearCart() }
            }
            CartAction.GoToMenuClicked -> send(CartEvent.NavigateToMenu)
            CartAction.BackClicked -> send(CartEvent.NavigateBack)
        }
    }

    private suspend fun decrease(itemId: Long): CartEvent? =
        when (val result = cartRepository.decreaseOrRemove(itemId)) {
            is Result.Failure -> result.failureEvent()
            is Result.Success -> when (val outcome = result.data) {
                is DecreaseOutcome.Decreased -> null
                is DecreaseOutcome.Removed -> removed(outcome.item)
            }
        }

    private suspend fun remove(itemId: Long): CartEvent? = when (val result = cartRepository.remove(itemId)) {
        is Result.Failure -> result.failureEvent()
        is Result.Success -> removed(result.data)
    }

    private fun removed(item: CartItem): CartEvent? {
        lastRemoved = item
        return CartEvent.ItemRemoved(item.productName)
    }

    private suspend fun undo(): CartEvent? {
        val item = lastRemoved ?: return null
        lastRemoved = null
        return when (val result = cartRepository.restore(item)) {
            is Result.Failure -> {
                lastRemoved = item
                CartEvent.ActionFailed
            }
            is Result.Success -> when (val outcome = result.data) {
                is RestoreOutcome.Merged -> CartEvent.RestoredAtLimit(item.productName).takeIf { outcome.capped }
                is RestoreOutcome.Restored, is RestoreOutcome.AlreadyInCart -> null
            }
        }
    }

    private suspend fun clearCart(): CartEvent? {
        lastRemoved = null
        send(CartEvent.UndoExpired)
        return (cartRepository.clear() as? Result.Failure)?.let { CartEvent.ActionFailed }
    }

    /** Zapis pod [writes], odporny na zamknięcie ekranu; zdarzenie wysyłane tylko, gdy VM jeszcze żyje. */
    private fun write(block: suspend () -> CartEvent?) {
        viewModelScope.launch {
            val result = withContext(NonCancellable) { writes.withLock { block() } }
            // send do buforowanego kanału nie zawiesza się, więc sam nie zauważy anulowania VM.
            ensureActive()
            result?.let { eventChannel.send(it) }
        }
    }

    private fun send(event: CartEvent) {
        viewModelScope.launch { eventChannel.send(event) }
    }

    /** Pobranie menu lokalu raz na VM; po sukcesie nowa migawka cen i zapamiętanie poprzednich. */
    private fun startRefreshOnce(cafeId: String) {
        if (cafeId in refreshes.value) return
        refreshes.update { it + (cafeId to MenuRefresh.IN_PROGRESS) }
        viewModelScope.launch {
            val refreshed = menuRepository.refresh(cafeId) is Result.Success
            refreshes.update { it + (cafeId to if (refreshed) MenuRefresh.DONE else MenuRefresh.FAILED) }
            if (!refreshed) return@launch
            val updates = (refreshCartSnapshots(cafeId) as? Result.Success)?.data.orEmpty()
            previousPrices.update { prices -> prices + updates.associate { it.itemId to it.previousUnitPrice } }
        }
    }

    private fun buildState(
        cart: CartOfCafe,
        refreshes: Map<String, MenuRefresh>,
        previousPrices: Map<Long, Money>,
        showClearConfirmation: Boolean,
    ): CartUiState {
        val cafe = cart.cafe
        val pricesUnverified = cafe == null || (!cart.checked.isVerified && refreshes[cafe.id] == MenuRefresh.FAILED)
        return cart.checked.toUiState(
            cafeName = cafe?.name,
            pricesUnverified = pricesUnverified,
            showClearConfirmation = showClearConfirmation,
            previousPrices = previousPrices,
            imageUrls = cart.imageUrls,
        )
    }

    /** Zdarzenie po błędzie zapisu; NOT_FOUND (pozycji już nie ma) bez komunikatu. */
    private fun Result<*, DataError.Local>.failureEvent(): CartEvent? = when (this) {
        is Result.Success -> null
        is Result.Failure -> CartEvent.ActionFailed.takeIf { error != DataError.Local.NOT_FOUND }
    }

    private data class CartOfCafe(val cafe: Cafe?, val checked: CheckedCart, val imageUrls: Map<String, String>)

    private enum class MenuRefresh { IN_PROGRESS, DONE, FAILED }
}
