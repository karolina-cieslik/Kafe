package com.karolina.cieslik.kafe.navigation

import com.karolina.cieslik.kafe.feature.menu.presentation.MenuEvent
import kotlinx.serialization.Serializable

/** Trasy aplikacji jako typy @Serializable – argumenty (np. productId) przenosi i odtwarza Navigation Compose. */
@Serializable
sealed interface KafeRoute {

    /** Menu wybranego lokalu – domyślny ekran startowy. */
    @Serializable
    data object Menu : KafeRoute

    /**
     * Szczegóły produktu.
     *
     * @property productId identyfikator produktu z menu.
     */
    @Serializable
    data class ProductDetail(val productId: String) : KafeRoute

    /** Wybór lokalu. */
    @Serializable
    data object CafePicker : KafeRoute

    /** Koszyk – jeden w aplikacji, sprawdzany z menu wybranego lokalu. */
    @Serializable
    data object Cart : KafeRoute
}

/** Cel nawigacji dla zdarzenia z menu. */
fun MenuEvent.toRoute(): KafeRoute = when (this) {
    is MenuEvent.NavigateToProduct -> KafeRoute.ProductDetail(productId)
    MenuEvent.NavigateToCafePicker -> KafeRoute.CafePicker
    MenuEvent.NavigateToCart -> KafeRoute.Cart
}
