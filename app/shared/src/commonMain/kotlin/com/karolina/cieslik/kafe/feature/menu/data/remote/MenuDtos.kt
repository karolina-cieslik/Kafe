package com.karolina.cieslik.kafe.feature.menu.data.remote

import kotlinx.serialization.Serializable

/**
 * Menu sieci z API w kształcie planowanej struktury Firebase Realtime DB: węzeł `/menu`, wspólny dla sieci.
 * Braki w lokalach to osobny węzeł `/availability/{cafeId}` ([AvailabilityDto]).
 *
 * @property categories kategorie; brak pola = pusta lista.
 * @property products produkty; brak pola = pusta lista.
 * @property modifierGroups grupy modyfikatorów wspólne dla sieci; brak pola = brak modyfikatorów.
 */
@Serializable
data class MenuDto(
    val categories: List<CategoryDto> = emptyList(),
    val products: List<ProductDto> = emptyList(),
    val modifierGroups: List<ModifierGroupDto> = emptyList(),
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
 * @property defaultSize kod rozmiaru domyślnego (S/M/L, wielkość liter bez znaczenia); null = pierwszy dostępny.
 * @property modifierGroupIds id grup modyfikatorów w kolejności wyświetlania; duplikaty są pomijane.
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
    val defaultSize: String? = null,
    val modifierGroupIds: List<String> = emptyList(),
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

/**
 * Grupa modyfikatorów, np. mleko albo dodatkowe espresso.
 *
 * @property id identyfikator grupy, np. `milk`.
 * @property name nazwa wyświetlana.
 * @property type "SINGLE" (jedna opcja) albo "QUANTITY" (opcje z licznikiem); nieznany typ – grupa pominięta.
 * @property required grupa wymagana; brak pola = opcjonalna.
 * @property maxQuantity limit sumy ilości w grupie QUANTITY (np. 4 shoty); brak pola = 1; dla SINGLE ignorowany.
 * @property defaultOptionId opcja wybrana na start; null albo brak pola = brak.
 * @property options opcje grupy; grupa bez poprawnych opcji jest pomijana.
 */
@Serializable
data class ModifierGroupDto(
    val id: String,
    val name: String,
    val type: String,
    val required: Boolean = false,
    val maxQuantity: Int = 1,
    val defaultOptionId: String? = null,
    val options: List<ModifierOptionDto> = emptyList(),
)

/**
 * Opcja modyfikatora.
 *
 * @property id identyfikator opcji, unikalny w całej sieci, np. `milk-oat`.
 * @property name nazwa wyświetlana.
 * @property surchargeGrosze dopłata za jedną sztukę w groszach; brak pola = 0 (bez dopłaty), ujemna – opcja
 * pominięta.
 * @property sortOrder pozycja w grupie, rosnąco; brak pola = 0.
 */
@Serializable
data class ModifierOptionDto(
    val id: String,
    val name: String,
    val surchargeGrosze: Long = 0,
    val sortOrder: Int = 0,
)

/**
 * Dostępność w lokalu, węzeł `/availability/{cafeId}`; brak wpisu = dostępne, brak pola = pusta mapa.
 *
 * @property products productId -> dostępność produktu.
 * @property sizes productId -> (kod rozmiaru S/M/L -> dostępność); wszystkie rozmiary niedostępne = produkt
 * niedostępny.
 * @property modifierOptions id opcji -> dostępność opcji modyfikatora.
 */
@Serializable
data class AvailabilityDto(
    val products: Map<String, Boolean> = emptyMap(),
    val sizes: Map<String, Map<String, Boolean>> = emptyMap(),
    val modifierOptions: Map<String, Boolean> = emptyMap(),
)
