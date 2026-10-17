package com.karolina.cieslik.kafe.feature.menu.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// KAFE-6: Menu gotowe do wyświetlenia – sekcje w kolejności sieci, bez pustych kategorii, z dostępnością w lokalu.
data class CafeMenu(val sections: List<MenuSection>)

data class MenuSection(val category: Category, val items: List<MenuItem>)

data class MenuItem(val product: Product, val isAvailable: Boolean)

class GetMenuForCafe(private val menuRepository: MenuRepository) {

    operator fun invoke(cafeId: String): Flow<CafeMenu?> =
        menuRepository.observeMenu(cafeId).map { menu -> menu?.toCafeMenu() }
}

/**
 * Reguły AC KAFE-6:
 * - AC 3: kategorie i produkty sortowane po sortOrder (stabilnie – przy remisie zostaje kolejność ze źródła),
 * - AC 4: kategoria bez produktów jest pomijana,
 * - AC 5: nieaktywne produkty są pomijane, więc np. „Sezonowe” bez aktywnych produktów znika.
 * Produkt niedostępny w lokalu zostaje na liście (wyszarzony w UI), więc nie ukrywa kategorii.
 * Produkty bez rozmiarów (bez ceny) traktujemy jak błędne dane i pomijamy.
 */
internal fun Menu.toCafeMenu(): CafeMenu {
    val visibleProductsByCategory = products
        .asSequence()
        .filter { it.isActive && it.sizes.isNotEmpty() }
        .sortedBy { it.sortOrder }
        .groupBy { it.categoryId }

    val sections = categories
        .sortedBy { it.sortOrder }
        .mapNotNull { category ->
            visibleProductsByCategory[category.id]
                ?.takeIf { it.isNotEmpty() }
                ?.let { products ->
                    MenuSection(
                        category = category,
                        items = products.map { MenuItem(product = it, isAvailable = isAvailable(it)) },
                    )
                }
        }
    return CafeMenu(sections)
}

/** KAFE-9: produkt ze wszystkimi rozmiarami niedostępnymi w lokalu jest niedostępny (spójnie ze szczegółami). */
internal fun Menu.isAvailable(product: Product): Boolean {
    val unavailable = unavailableSizes[product.id].orEmpty()
    return product.id !in unavailableProductIds && product.sizes.any { it.code !in unavailable }
}
