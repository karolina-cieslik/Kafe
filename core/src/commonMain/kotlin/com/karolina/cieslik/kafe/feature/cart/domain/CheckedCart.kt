package com.karolina.cieslik.kafe.feature.cart.domain

import com.karolina.cieslik.kafe.core.domain.Money

/**
 * Pozycja koszyka sprawdzona z aktualnym menu lokalu.
 *
 * @property item pozycja z koszyka (migawka nazw i cen z bazy).
 * @property status wynik sprawdzenia.
 * @property currentUnitPrice cena jednej sztuki do zapłaty: z menu dla [CartItemStatus.Available]
 * i [CartItemStatus.PriceChanged], z migawki w pozostałych przypadkach.
 */
data class CheckedCartItem(
    val item: CartItem,
    val status: CartItemStatus,
    val currentUnitPrice: Money,
)

/** Wynik sprawdzenia pozycji z menu lokalu. */
sealed interface CartItemStatus {

    /** Pozycję można zamówić w cenie z migawki. */
    data object Available : CartItemStatus

    /**
     * Pozycję można zamówić, ale cena w menu jest inna niż w migawce.
     *
     * @property previousUnitPrice cena jednej sztuki z migawki.
     */
    data class PriceChanged(val previousUnitPrice: Money) : CartItemStatus

    /**
     * Pozycji nie da się zamówić w tym lokalu.
     *
     * @property reason pierwszy wykryty powód; przy kilku naraz kolejność sprawdzania nie jest częścią kontraktu.
     */
    data class Unavailable(val reason: UnavailableReason) : CartItemStatus

    /** Nie wiadomo – menu lokalu nie zostało jeszcze pobrane (np. offline przy pierwszym wejściu). */
    data object Unverified : CartItemStatus
}

/**
 * Powód, dla którego pozycji koszyka nie da się zamówić w lokalu. Pozycja dostaje jeden – pierwszy wykryty;
 * kolejność sprawdzania nie jest częścią kontraktu (UI pokazuje jedno „Niedostępne”).
 */
enum class UnavailableReason {
    /** Produktu nie ma w menu, jest nieaktywny albo nie ma rozmiarów. */
    PRODUCT_REMOVED,

    /** Produkt jest niedostępny w lokalu (także gdy niedostępne są wszystkie jego rozmiary). */
    PRODUCT_UNAVAILABLE,

    /** Wybranego rozmiaru nie ma już w produkcie albo jest niedostępny w lokalu. */
    SIZE_UNAVAILABLE,

    /** Wybranej opcji nie ma już w grupie, grupa została odpięta od produktu albo opcja jest niedostępna w lokalu. */
    OPTION_UNAVAILABLE,

    /** Wybór nie spełnia reguł menu po zmianie: brak wymaganej opcji, przekroczony limit grupy. */
    INVALID_CONFIGURATION,
}

/**
 * Koszyk sprawdzony z menu lokalu.
 *
 * @property items pozycje w kolejności koszyka.
 * @property isVerified false, gdy menu lokalu nie zostało jeszcze pobrane – wtedy wszystkie pozycje są
 * [CartItemStatus.Unverified].
 */
data class CheckedCart(
    val items: List<CheckedCartItem>,
    val isVerified: Boolean,
) {
    private val payable get() = items.filterNot { it.status is CartItemStatus.Unavailable }

    /** Suma sztuk do zapłaty – bez pozycji niedostępnych. */
    val itemCount: Int get() = payable.sumOf { it.item.quantity }

    /** „Razem do zapłaty”: suma [CheckedCartItem.currentUnitPrice] × ilość, bez pozycji niedostępnych. */
    val payableTotal: Money
        get() = payable.fold(Money.ZERO) { sum, checked -> sum + checked.currentUnitPrice * checked.item.quantity }

    /** Można przejść do zamówienia: koszyk niepusty, sprawdzony i bez pozycji niedostępnych. */
    val canProceed: Boolean get() = items.isNotEmpty() && isVerified && payable.size == items.size
}
