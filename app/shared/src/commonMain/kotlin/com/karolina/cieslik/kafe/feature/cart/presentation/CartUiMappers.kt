package com.karolina.cieslik.kafe.feature.cart.presentation

import com.karolina.cieslik.kafe.core.domain.Money
import com.karolina.cieslik.kafe.feature.cart.domain.CartItemStatus
import com.karolina.cieslik.kafe.feature.cart.domain.CartSelection
import com.karolina.cieslik.kafe.feature.cart.domain.CheckedCart
import com.karolina.cieslik.kafe.feature.cart.domain.CheckedCartItem
import com.karolina.cieslik.kafe.feature.product.domain.ProductConfiguration
import com.karolina.cieslik.kafe.feature.product.presentation.label

/**
 * Stan ekranu ze sprawdzonego koszyka.
 *
 * @param previousPrices poprzednie ceny pozycji zapisanych po odświeżeniu menu (id → cena); oznaczenie
 * „Cena została zaktualizowana” dostają tylko pozycje, które są w koszyku.
 * @param imageUrls zdjęcia produktów z bieżącego menu (productId → url).
 */
internal fun CheckedCart.toUiState(
    cafeName: String?,
    pricesUnverified: Boolean,
    showClearConfirmation: Boolean,
    previousPrices: Map<Long, Money>,
    imageUrls: Map<String, String>,
): CartUiState {
    if (items.isEmpty()) return CartUiState.Empty
    return CartUiState.Content(
        cafeName = cafeName,
        items = items.map { it.toUi(previousPrices[it.item.id], imageUrls[it.item.productId]) },
        summary = CartSummaryUi(itemCount = itemCount, payableTotalLabel = payableTotal.format()),
        pricesUnverified = pricesUnverified,
        showClearConfirmation = showClearConfirmation,
    )
}

private fun CheckedCartItem.toUi(savedPreviousPrice: Money?, imageUrl: String?): CartItemUi {
    val unavailable = status is CartItemStatus.Unavailable
    return CartItemUi(
        id = item.id,
        name = item.productName,
        imageUrl = imageUrl,
        sizeLabel = item.size.label(),
        modifiersLabel = item.selections.toModifiersLabel(),
        quantity = item.quantity,
        canIncrease = !unavailable && item.quantity < ProductConfiguration.MAX_QUANTITY,
        unitPriceLabel = currentUnitPrice.format(),
        totalPriceLabel = (currentUnitPrice * item.quantity).format(),
        status = when (val status = status) {
            is CartItemStatus.Unavailable -> CartItemStatusUi.Unavailable
            // Cena z menu inna niż w migawce – widoczna od razu, także bez zapisu (offline).
            is CartItemStatus.PriceChanged -> CartItemStatusUi.PriceUpdated(status.previousUnitPrice.format())
            CartItemStatus.Available, CartItemStatus.Unverified ->
                savedPreviousPrice?.let { CartItemStatusUi.PriceUpdated(it.format()) } ?: CartItemStatusUi.Ok
        },
    )
}

/** „Owsiane, 2× Shot espresso” – bez domyślnych opcji bez dopłaty (np. „Krowie”); null, gdy nic nie zostaje. */
internal fun List<CartSelection>.toModifiersLabel(): String? =
    filterNot { it.isDefault && it.surcharge == Money.ZERO }
        .joinToString(", ") { if (it.quantity > 1) "${it.quantity}× ${it.optionName}" else it.optionName }
        .ifEmpty { null }
