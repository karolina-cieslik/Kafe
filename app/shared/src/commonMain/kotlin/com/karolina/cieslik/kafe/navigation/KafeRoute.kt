package com.karolina.cieslik.kafe.navigation

import com.karolina.cieslik.kafe.feature.menu.presentation.MenuEvent
import kotlinx.serialization.Serializable

// KAFE-41: Trasy aplikacji jako typy @Serializable – argumenty (productId) przenosi Navigation Compose.
@Serializable
sealed interface KafeRoute {

    @Serializable
    data object Menu : KafeRoute

    @Serializable
    data class ProductDetail(val productId: String) : KafeRoute

    @Serializable
    data object CafePicker : KafeRoute
}

/** KAFE-41: Cel nawigacji dla zdarzenia z menu. */
fun MenuEvent.toRoute(): KafeRoute = when (this) {
    is MenuEvent.NavigateToProduct -> KafeRoute.ProductDetail(productId)
    MenuEvent.NavigateToCafePicker -> KafeRoute.CafePicker
}
