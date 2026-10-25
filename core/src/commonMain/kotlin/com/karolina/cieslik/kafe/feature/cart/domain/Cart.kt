package com.karolina.cieslik.kafe.feature.cart.domain

import com.karolina.cieslik.kafe.core.domain.Money
import com.karolina.cieslik.kafe.feature.menu.domain.SizeCode
import com.karolina.cieslik.kafe.feature.product.domain.ProductConfiguration

/**
 * Pozycja w koszyku: produkt w konkretnej konfiguracji (rozmiar + modyfikatory) i liczba sztuk.
 *
 * Nazwa i cena to migawka z chwili dodania (aktualizowana po odświeżeniu menu,
 * [CartRepository.updateSnapshots]) – pozycja przetrwa zmianę albo zniknięcie produktu z menu.
 *
 * @property id identyfikator pozycji nadany przez bazę; stały także po „Cofnij”.
 * @property productId id produktu z menu sieci.
 * @property productName nazwa produktu z chwili dodania.
 * @property size wybrany rozmiar.
 * @property selections migawka wybranych opcji, w kolejności grup i opcji produktu.
 * @property quantity liczba sztuk, od [ProductConfiguration.MIN_QUANTITY] do [ProductConfiguration.MAX_QUANTITY].
 * @property unitPrice cena jednej sztuki z dopłatami z chwili dodania (przy scaleniu – z ostatniego dodania).
 */
data class CartItem(
    val id: Long,
    val productId: String,
    val productName: String,
    val size: SizeCode,
    val selections: List<CartSelection>,
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
 * @property items pozycje od najstarszej do najnowszej (id rosnąco).
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
 * @property selections migawka wybranych opcji – tylko opcje z grup produktu z ilością > 0.
 * @property quantity liczba dodawanych sztuk, 1–20.
 * @property unitPrice cena jednej sztuki z dopłatami.
 */
data class NewCartItem(
    val productId: String,
    val productName: String,
    val size: SizeCode,
    val selections: List<CartSelection>,
    val quantity: Int,
    val unitPrice: Money,
) {
    /** Klucz konfiguracji – po nim repozytorium szuka identycznej pozycji ([cartConfigurationKey]). */
    val configurationKey: String get() = cartConfigurationKey(productId, size, selections)
}

/**
 * Migawka wybranej opcji z chwili dodania – ekran koszyka opisze pozycję także wtedy, gdy opcja zniknie z menu.
 *
 * @property groupId id grupy modyfikatorów.
 * @property optionId id opcji w grupie.
 * @property optionName nazwa opcji w chwili dodania.
 * @property surcharge dopłata za jedną sztukę opcji w chwili dodania.
 * @property quantity ilość opcji w jednej sztuce produktu, > 0 (dla SINGLE zawsze 1).
 * @property isDefault opcja była domyślną opcją grupy w chwili dodania – koszyk nie wypisuje domyślnych opcji
 * bez dopłaty (np. „Krowie”). Nie wchodzi do klucza konfiguracji.
 */
data class CartSelection(
    val groupId: String,
    val optionId: String,
    val optionName: String,
    val surcharge: Money,
    val quantity: Int,
    val isDefault: Boolean = false,
)

/** Wynik „−” na pozycji ([CartRepository.decreaseOrRemove]). */
sealed interface DecreaseOutcome {

    /** Ilość zmniejszona o 1; [item] po zmianie. */
    data class Decreased(val item: CartItem) : DecreaseOutcome

    /** Pozycja miała 1 sztukę i została usunięta; [item] sprzed usunięcia – do „Cofnij”. */
    data class Removed(val item: CartItem) : DecreaseOutcome
}

/** Wynik „Cofnij” po usunięciu pozycji ([CartRepository.restore]). */
sealed interface RestoreOutcome {

    /** Pozycja w koszyku po operacji. */
    val item: CartItem

    /** Pozycja wróciła z tym samym id na swoje miejsce. */
    data class Restored(override val item: CartItem) : RestoreOutcome

    /**
     * W międzyczasie dodano tę samą konfigurację – [item] to tamta pozycja z sumą ilości.
     *
     * @property capped suma przekroczyła [ProductConfiguration.MAX_QUANTITY] i została przycięta.
     */
    data class Merged(override val item: CartItem, val capped: Boolean) : RestoreOutcome

    /** Pozycja już jest w koszyku (np. podwójne „Cofnij”) – nic się nie zmieniło. */
    data class AlreadyInCart(override val item: CartItem) : RestoreOutcome
}

/** Wynik dodania do koszyka ([CartRepository.add]). */
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
