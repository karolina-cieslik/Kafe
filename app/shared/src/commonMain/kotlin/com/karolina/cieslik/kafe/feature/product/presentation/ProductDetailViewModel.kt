package com.karolina.cieslik.kafe.feature.product.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.Result
import com.karolina.cieslik.kafe.feature.cafe.domain.CafeRepository
import com.karolina.cieslik.kafe.feature.menu.domain.MenuRepository
import com.karolina.cieslik.kafe.feature.menu.domain.SizeCode
import com.karolina.cieslik.kafe.feature.product.domain.GetProductDetails
import com.karolina.cieslik.kafe.feature.product.domain.PriceCalculator
import com.karolina.cieslik.kafe.feature.product.domain.ProductConfiguration
import com.karolina.cieslik.kafe.feature.product.domain.ProductDetails
import com.karolina.cieslik.kafe.feature.product.domain.ProductLookup
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
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// KAFE-9 / KAFE-10 / KAFE-11: Szczegóły i konfiguracja produktu w wybranym lokalu.
// Dane pochodzą z zapamiętanego menu; gdy menu lokalu nie ma (np. wejście spoza menu), VM pobiera je raz,
// a błąd kończy się stanem Error z Retry. Konfiguracja żyje tylko w VM – każde otwarcie startuje od domyślnych.
@OptIn(ExperimentalCoroutinesApi::class)
class ProductDetailViewModel(
    private val productId: String,
    private val cafeRepository: CafeRepository,
    private val menuRepository: MenuRepository,
    getProductDetails: GetProductDetails,
    private val priceCalculator: PriceCalculator,
    private val dispatcher: CoroutineDispatcher,
) : ViewModel() {

    /** Wybór klienta dla konkretnego lokalu – po zmianie lokalu dostępność jest inna, więc start od nowa. */
    private val configuration = MutableStateFlow<CafeConfiguration?>(null)
    private val refreshError = MutableStateFlow<CafeError?>(null)

    // Dostęp tylko z wątku Main (viewModelScope / onAction).
    private var current: LookupOfCafe? = null
    private var refreshJob: Job? = null
    private var refreshedCafeId: String? = null

    private val eventChannel = Channel<ProductDetailEvent>(Channel.BUFFERED)
    val events: Flow<ProductDetailEvent> = eventChannel.receiveAsFlow()

    private val lookup: Flow<LookupOfCafe?> = cafeRepository.observeSelectedCafe()
        .map { it?.id }
        .distinctUntilChanged()
        .flatMapLatest { cafeId ->
            if (cafeId == null) {
                flowOf(null)
            } else {
                getProductDetails(cafeId, productId)
                    .map { LookupOfCafe(cafeId, it) }
                    .flowOn(dispatcher)
            }
        }
        .onEach { result ->
            current = result
            if (result?.lookup == ProductLookup.MenuNotLoaded && refreshedCafeId != result.cafeId) {
                startRefresh(result.cafeId)
            }
        }

    val state: StateFlow<ProductDetailUiState> = combine(lookup, configuration, refreshError, ::buildState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProductDetailUiState.Loading)

    fun onAction(action: ProductDetailAction) {
        when (action) {
            is ProductDetailAction.SizeSelected -> {
                val code = SizeCode.entries.firstOrNull { it.name == action.code } ?: return
                updateConfiguration { details -> selectSize(code, details) }
            }
            is ProductDetailAction.OptionSelected ->
                updateConfiguration { details -> selectOption(action.groupId, action.optionId, details) }
            is ProductDetailAction.OptionQuantityChanged ->
                updateConfiguration { details -> changeOptionQuantity(action.groupId, action.optionId, action.delta, details) }
            is ProductDetailAction.QuantityChanged -> updateConfiguration { changeQuantity(action.delta) }
            ProductDetailAction.AddToCartClicked -> addToCart()
            ProductDetailAction.BackClicked -> sendEvent(ProductDetailEvent.NavigateBack)
            ProductDetailAction.Retry -> retry()
        }
    }

    private fun addToCart() {
        val (cafeId, details) = currentDetails() ?: return
        if (effectiveConfiguration(configuration.value, cafeId, details).validate(details) != null) return
        // KAFE-12: tu trafi zapis do koszyka; na razie tylko potwierdzenie dla UI (snackbar).
        sendEvent(ProductDetailEvent.AddedToCart(details.product.name))
    }

    private fun retry() {
        val result = current
        if (refreshJob?.isActive == true) return
        if (result == null) {
            // Brak wybranego lokalu – ponawiamy pobranie lokali, menu ruszy po wyborze pierwszego.
            refreshError.value = null
            refreshJob = viewModelScope.launch(dispatcher) {
                (cafeRepository.refresh() as? Result.Failure)?.let { refreshError.value = CafeError(null, it.error) }
            }
        } else {
            startRefresh(result.cafeId)
        }
    }

    private fun startRefresh(cafeId: String) {
        refreshJob?.cancel()
        refreshedCafeId = cafeId
        refreshError.value = null
        refreshJob = viewModelScope.launch(dispatcher) {
            (menuRepository.refresh(cafeId) as? Result.Failure)?.let { refreshError.value = CafeError(cafeId, it.error) }
        }
    }

    private fun updateConfiguration(change: ProductConfiguration.(ProductDetails) -> ProductConfiguration) {
        val (cafeId, details) = currentDetails() ?: return
        val updated = effectiveConfiguration(configuration.value, cafeId, details).change(details)
        configuration.value = CafeConfiguration(cafeId, updated)
    }

    private fun currentDetails(): Pair<String, ProductDetails>? {
        val result = current ?: return null
        val found = result.lookup as? ProductLookup.Found ?: return null
        return result.cafeId to found.details
    }

    /** Zapisany wybór, jeśli pasuje do lokalu i produktu (rozmiar istnieje); w przeciwnym razie wartości domyślne. */
    private fun effectiveConfiguration(
        saved: CafeConfiguration?,
        cafeId: String,
        details: ProductDetails,
    ): ProductConfiguration =
        saved
            ?.takeIf { saved -> saved.cafeId == cafeId && details.product.sizes.any { it.code == saved.configuration.size } }
            ?.configuration
            ?: ProductConfiguration.initial(details)

    private fun sendEvent(event: ProductDetailEvent) {
        viewModelScope.launch { eventChannel.send(event) }
    }

    private fun buildState(
        result: LookupOfCafe?,
        saved: CafeConfiguration?,
        error: CafeError?,
    ): ProductDetailUiState {
        if (result == null) {
            return if (error != null && error.cafeId == null) {
                ProductDetailUiState.Error(error.error.toProductErrorMessage())
            } else {
                ProductDetailUiState.Error(PRODUCT_ERROR_NO_CAFE)
            }
        }
        return when (val lookup = result.lookup) {
            ProductLookup.MenuNotLoaded ->
                error?.takeIf { it.cafeId == result.cafeId }
                    ?.let { ProductDetailUiState.Error(it.error.toProductErrorMessage()) }
                    ?: ProductDetailUiState.Loading
            ProductLookup.NotFound -> ProductDetailUiState.Error(PRODUCT_ERROR_NOT_FOUND)
            is ProductLookup.Found ->
                lookup.details.toContent(effectiveConfiguration(saved, result.cafeId, lookup.details), priceCalculator)
        }
    }

    private data class LookupOfCafe(val cafeId: String, val lookup: ProductLookup)

    private data class CafeConfiguration(val cafeId: String, val configuration: ProductConfiguration)

    /** cafeId = null – błąd pobrania listy lokali. */
    private data class CafeError(val cafeId: String?, val error: DataError)
}

internal fun DataError.toProductErrorMessage(): String = when (this) {
    DataError.Remote.NO_INTERNET, DataError.Remote.REQUEST_TIMEOUT -> PRODUCT_ERROR_NO_CONNECTION
    else -> PRODUCT_ERROR_GENERIC
}
