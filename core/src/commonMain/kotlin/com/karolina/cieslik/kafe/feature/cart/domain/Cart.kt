package com.karolina.cieslik.kafe.feature.cart.domain

import com.karolina.cieslik.kafe.core.domain.Money
import com.karolina.cieslik.kafe.feature.menu.domain.SizeCode
import com.karolina.cieslik.kafe.feature.product.domain.ProductConfiguration

/**
 * Pozycja w koszyku: produkt w konkretnej konfiguracji (rozmiar + modyfikatory) i liczba sztuk.
 *
 * Nazwa i cena to migawka z chwili dodania – pozycja przetrwa zmianę albo zniknięcie produktu z menu.
 *
 * @property id identyfikator pozycji nadany przez bazę; stały także po „Cofnij”.
 * @property productId id produktu z menu sieci.
 * @property productName nazwa produktu z chwili dodania.
 * @property size wybrany rozmiar.
 * @property selections groupId -> (optionId -> ilość > 0), jak w [ProductConfiguration]; bez pustych grup.
 * @property quantity liczba sztuk, od [ProductConfiguration.MIN_QUANTITY] do [ProductConfiguration.MAX_QUANTITY].
 * @property unitPrice cena jednej sztuki z dopłatami z chwili dodania (przy scaleniu – z ostatniego dodania).
 */
data class CartItem(
    val id: Long,
    val productId: String,
    val productName: String,
    val size: SizeCode,
    val selections: Map<String, Map<String, Int>>,
    val quantity: Int,
    val unitPrice: Money,
) {
    /** Cena wszystkich sztuk pozycji: [unitPrice] × [quantity]. */
    val totalPrice: Money get() = unitPrice * quantity

    /** Klucz konfiguracji – pozycje z tym samym kluczem są scalane ([cartConfigurationKey]). */
    val configurationKey: String get() = cartConfigurationKey(productId, size, selections)
}

/**
 * Koszyk – jeden w aplikacji, niezależny od wybranego lokalu.
 *
 * @property items pozycje od najstarszej do najnowszej.
 */
data class Cart(val items: List<CartItem>) {

    /** Suma sztuk wszystkich pozycji – licznik na ikonie koszyka. */
    val itemCount: Int get() = items.sumOf { it.quantity }

    /** Suma cen wszystkich pozycji. */
    val total: Money get() = items.fold(Money.ZERO) { sum, item -> sum + item.totalPrice }

    /** Stałe koszyka. */
    companion object {
        /** Pusty koszyk. */
        val EMPTY = Cart(emptyList())
    }
}

/**
 * Pozycja do dodania do koszyka, wyliczona przez [AddToCart] z produktu i konfiguracji.
 *
 * @property productId id produktu z menu sieci.
 * @property productName nazwa produktu w chwili dodania.
 * @property size wybrany rozmiar.
 * @property selections groupId -> (optionId -> ilość > 0); tylko opcje z grup produktu, bez pustych grup.
 * @property quantity liczba dodawanych sztuk, 1–20.
 * @property unitPrice cena jednej sztuki z dopłatami.
 */
data class NewCartItem(
    val productId: String,
    val productName: String,
    val size: SizeCode,
    val selections: Map<String, Map<String, Int>>,
    val quantity: Int,
    val unitPrice: Money,
) {
    /** Klucz konfiguracji – po nim repozytorium szuka identycznej pozycji ([cartConfigurationKey]). */
    val configurationKey: String get() = cartConfigurationKey(productId, size, selections)
}

/** Wynik dodania do koszyka. */
sealed interface AddToCartOutcome {

    /** Pozycja w koszyku po operacji. */
    val item: CartItem

    /** Dodano nową pozycję [item]. */
    data class Added(override val item: CartItem) : AddToCartOutcome

    /** Scalono z pozycją o identycznej konfiguracji; [item] ma zwiększoną ilość. */
    data class QuantityIncreased(override val item: CartItem) : AddToCartOutcome

    /** Scalono, ale suma przekroczyła [ProductConfiguration.MAX_QUANTITY]; [item] ma ilość przyciętą do limitu. */
    data class CappedAtLimit(override val item: CartItem) : AddToCartOutcome
}
