package com.karolina.cieslik.kafe.feature.menu.data.remote

import kotlinx.serialization.Serializable

/**
 * Menu sieci z API w kształcie planowanej struktury Firebase Realtime DB: węzeł `/menu`, wspólny dla sieci.
 * Dostępność w lokalach to osobny węzeł `/availability/{cafeId}/{productId}: Boolean`.
 *
 * @property categories kategorie; brak pola = pusta lista.
 * @property products produkty; brak pola = pusta lista.
 */
@Serializable
data class MenuDto(
    val categories: List<CategoryDto> = emptyList(),
    val products: List<ProductDto> = emptyList(),
)

/**
 * Kategoria menu.
 *
 * @property id identyfikator, np. `classic`.
 * @property name nazwa wyświetlana.
 * @property sortOrder pozycja w menu, rosnąco; brak pola = 0.
 */
@Serializable
data class CategoryDto(
    val id: String,
    val name: String,
    val sortOrder: Int = 0,
)

/**
 * Produkt menu.
 *
 * @property id identyfikator, np. `latte`.
 * @property categoryId kategoria; produkt z nieznaną kategorią nie trafia do żadnej sekcji.
 * @property name nazwa wyświetlana.
 * @property description opis; null albo brak pola = brak opisu.
 * @property imageUrl adres zdjęcia; null albo brak pola = brak zdjęcia.
 * @property sizes rozmiary z cenami; brak pola = pusta lista, a produkt bez rozmiarów nie jest pokazywany.
 * @property sortOrder pozycja w kategorii, rosnąco; brak pola = 0.
 * @property active false dla produktów poza ofertą; brak pola = aktywny.
 */
@Serializable
data class ProductDto(
    val id: String,
    val categoryId: String,
    val name: String,
    val description: String? = null,
    val imageUrl: String? = null,
    val sizes: List<ProductSizeDto> = emptyList(),
    val sortOrder: Int = 0,
    val active: Boolean = true,
)

/**
 * Rozmiar produktu.
 *
 * @property code kod rozmiaru S/M/L (wielkość liter bez znaczenia); nieznany kod jest pomijany.
 * @property volumeMl pojemność w mililitrach; null albo brak pola dla produktów bez pojemności.
 * @property priceGrosze PEŁNA cena w groszach (nie dopłata do najmniejszego rozmiaru).
 */
@Serializable
data class ProductSizeDto(
    val code: String,
    val volumeMl: Int? = null,
    val priceGrosze: Long,
)
