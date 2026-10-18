package com.karolina.cieslik.kafe.feature.menu.domain

import com.karolina.cieslik.kafe.core.domain.Money

/**
 * Kategoria menu wspólnego dla sieci.
 *
 * @property id identyfikator z API.
 * @property name nazwa wyświetlana, np. „Kawy”.
 * @property sortOrder pozycja w menu ustalona przez sieć; rosnąco.
 */
data class Category(
    val id: String,
    val name: String,
    val sortOrder: Int,
)

/**
 * Produkt z menu sieci; ten sam we wszystkich lokalach.
 *
 * @property id identyfikator z API.
 * @property categoryId kategoria, do której należy produkt.
 * @property name nazwa wyświetlana.
 * @property description opis; null, gdy sieć go nie podała.
 * @property imageUrl adres zdjęcia; null, gdy produkt nie ma zdjęcia (UI pokazuje zastępczą grafikę).
 * @property sizes dostępne rozmiary z pełnymi cenami; pusta lista oznacza błędne dane.
 * @property sortOrder pozycja w kategorii ustalona przez sieć; rosnąco.
 * @property defaultSize rozmiar wybrany na start konfiguracji; null = pierwszy dostępny w lokalu.
 * @property modifierGroupIds grupy modyfikatorów produktu w kolejności wyświetlania (id z [Menu.modifierGroups]).
 */
data class Product(
    val id: String,
    val categoryId: String,
    val name: String,
    val description: String?,
    val imageUrl: String?,
    val sizes: List<ProductSize>,
    val sortOrder: Int,
    /** Produkt aktywny w menu sieci (np. sezonowy poza sezonem = false). Nieaktywne nie są pokazywane. */
    val isActive: Boolean,
    val defaultSize: SizeCode? = null,
    val modifierGroupIds: List<String> = emptyList(),
) {
    /** Cena bazowa na liście = najtańszy rozmiar; null tylko dla błędnych danych bez rozmiarów. */
    val basePrice: Money? get() = sizes.minOfOrNull { it.price }
}

/**
 * Rozmiar ma PEŁNĄ cenę, nie dopłatę. Produkty jednorozmiarowe (np. przekąski) mają jeden wpis.
 *
 * @property code kod rozmiaru.
 * @property volumeMl pojemność w mililitrach; null dla produktów bez pojemności (np. przekąsek).
 * @property price pełna cena produktu w tym rozmiarze.
 */
data class ProductSize(
    val code: SizeCode,
    val volumeMl: Int?,
    val price: Money,
)

/** Kod rozmiaru: mały (S), średni (M), duży (L). */
enum class SizeCode { S, M, L }

/**
 * Menu sieci w kontekście lokalu: kategorie, produkty i modyfikatory są wspólne, a zbiory „unavailable”
 * opisują braki w tym lokalu (domyślnie wszystko jest dostępne).
 *
 * @property categories wszystkie kategorie sieci, także puste.
 * @property products wszystkie produkty sieci, także nieaktywne.
 * @property unavailableProductIds identyfikatory produktów niedostępnych w lokalu.
 * @property modifierGroups wszystkie grupy modyfikatorów sieci.
 * @property unavailableSizes rozmiary niedostępne w lokalu: productId -> kody rozmiarów.
 * @property unavailableOptionIds opcje modyfikatorów niedostępne w lokalu (id opcji).
 */
data class Menu(
    val categories: List<Category>,
    val products: List<Product>,
    val unavailableProductIds: Set<String>,
    val modifierGroups: List<ModifierGroup> = emptyList(),
    val unavailableSizes: Map<String, Set<SizeCode>> = emptyMap(),
    val unavailableOptionIds: Set<String> = emptySet(),
)
