package com.karolina.cieslik.kafe.feature.menu.presentation

import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.feature.menu.domain.CafeMenu
import com.karolina.cieslik.kafe.feature.menu.domain.MenuItem
import com.karolina.cieslik.kafe.feature.menu.domain.MenuSection

// KAFE-6: Mapowanie domena -> UI oraz filtrowanie wyszukiwarką.
internal fun MenuSection.toCategoryUi() = CategoryUi(
    id = category.id,
    name = category.name,
    products = items.map { it.toProductUi() },
)

internal fun MenuItem.toProductUi() = ProductUi(
    id = product.id,
    name = product.name,
    subtitle = product.description,
    priceLabel = product.basePrice?.format().orEmpty(),
    imageUrl = product.imageUrl,
    isAvailable = isAvailable,
)

/** Filtr po nazwie produktu (bez wielkości liter, z trim); sekcje bez wyników znikają. Pusty filtr = całe menu. */
internal fun CafeMenu.filterByProductName(query: String): CafeMenu {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return this
    return CafeMenu(
        sections = sections.mapNotNull { section ->
            val matching = section.items.filter { it.product.name.contains(normalizedQuery, ignoreCase = true) }
            if (matching.isEmpty()) null else section.copy(items = matching)
        },
    )
}

internal fun DataError.toMenuErrorMessage(): String = when (this) {
    DataError.Remote.NO_INTERNET, DataError.Remote.REQUEST_TIMEOUT -> MENU_ERROR_NO_CONNECTION
    else -> MENU_ERROR_GENERIC
}

// AC 7 KAFE-6. Docelowo do zasobów stringów (composeResources), gdy dojdzie lokalizacja.
internal const val MENU_ERROR_NO_CONNECTION = "Nie udało się pobrać menu. Sprawdź połączenie."
internal const val MENU_ERROR_GENERIC = "Nie udało się pobrać menu. Spróbuj ponownie później."
