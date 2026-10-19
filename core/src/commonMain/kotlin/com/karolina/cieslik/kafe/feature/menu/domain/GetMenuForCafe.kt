package com.karolina.cieslik.kafe.feature.menu.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Menu gotowe do wyświetlenia – sekcje w kolejności sieci, bez pustych kategorii, z dostępnością w lokalu.
 *
 * @property sections niepuste kategorie w kolejności sieci.
 */
data class CafeMenu(val sections: List<MenuSection>)

/**
 * Kategoria menu z widocznymi produktami.
 *
 * @property category kategoria sieci.
 * @property items co najmniej jeden produkt, w kolejności sieci.
 */
data class MenuSection(val category: Category, val items: List<MenuItem>)

/**
 * Produkt na liście menu.
 *
 * @property product produkt z menu sieci.
 * @property isAvailable false, gdy produkt jest chwilowo niedostępny w wybranym lokalu (UI go wyszarza).
 */
data class MenuItem(val product: Product, val isAvailable: Boolean)

/** Przypadek użycia: menu sieci przefiltrowane i posortowane do wyświetlenia w lokalu. */
class GetMenuForCafe(private val menuRepository: MenuRepository) {

    /**
     * Obserwuje menu dla lokalu [cafeId].
     *
     * Zimny Flow: emituje przy każdej emisji [MenuRepository.observeMenu], czyli od razu stan z bazy
     * i ponownie po każdym zapisie menu. Emituje null, gdy menu nie zostało jeszcze nigdy pobrane.
     */
    operator fun invoke(cafeId: String): Flow<CafeMenu?> =
        menuRepository.observeMenu(cafeId).map { menu -> menu?.toCafeMenu() }
}

/**
 * Reguły wyświetlania menu:
 * - kategorie i produkty sortowane po sortOrder (stabilnie – przy remisie zostaje kolejność ze źródła),
 * - kategoria bez produktów jest pomijana,
 * - nieaktywne produkty są pomijane, więc np. „Sezonowe” bez aktywnych produktów znika.
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

/**
 * Produkt jest dostępny w lokalu, gdy nie ma go na liście braków i ma co najmniej jeden dostępny rozmiar.
 * Wszystkie rozmiary niedostępne = produkt niedostępny; ta sama reguła obowiązuje na liście i w szczegółach.
 */
internal fun Menu.isAvailable(product: Product): Boolean {
    val unavailable = unavailableSizes[product.id].orEmpty()
    return product.id !in unavailableProductIds && product.sizes.any { it.code !in unavailable }
}
