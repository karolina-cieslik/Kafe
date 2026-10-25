package com.karolina.cieslik.kafe.navigation

import com.karolina.cieslik.kafe.feature.menu.presentation.MenuEvent
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/** Trasy i mapowanie zdarzeń menu na nawigację. */
class KafeRouteTest {

    @Test
    fun productClickNavigatesToProductDetailWithId() {
        assertEquals(KafeRoute.ProductDetail("flat-white"), MenuEvent.NavigateToProduct("flat-white").toRoute())
    }

    @Test
    fun cartIconNavigatesToCart() {
        assertEquals(KafeRoute.Cart, MenuEvent.NavigateToCart.toRoute())
    }

    @Test
    fun changeCafeNavigatesToCafePicker() {
        assertEquals(KafeRoute.CafePicker, MenuEvent.NavigateToCafePicker.toRoute())
    }

    @Test
    fun routesSurviveSerializationRoundTrip() {
        val routes = listOf(KafeRoute.Menu, KafeRoute.ProductDetail("café-ząb/1 ?&"), KafeRoute.CafePicker)

        routes.forEach { route ->
            val encoded = Json.encodeToString(KafeRoute.serializer(), route)
            assertEquals(route, Json.decodeFromString(KafeRoute.serializer(), encoded))
        }
    }
}
