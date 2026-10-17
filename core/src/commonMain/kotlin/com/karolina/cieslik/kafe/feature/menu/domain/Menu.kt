package com.karolina.cieslik.kafe.feature.menu.domain

import com.karolina.cieslik.kafe.core.domain.Money

// KAFE-6: Modele menu wspólnego dla sieci. Kolejność wyznacza sortOrder ustalony przez sieć (AC 3).
data class Category(
    val id: String,
    val name: String,
    val sortOrder: Int,
)

data class Product(
    val id: String,
    val categoryId: String,
    val name: String,
    val description: String?,
    val imageUrl: String?,
    val sizes: List<ProductSize>,
    val sortOrder: Int,
    /** Produkt aktywny w menu sieci (np. sezonowy poza sezonem = false). Nieaktywne nie są pokazywane (AC 5). */
    val isActive: Boolean,
) {
    /** Cena bazowa na liście = najtańszy rozmiar (AC 2); null tylko dla błędnych danych bez rozmiarów. */
    val basePrice: Money? get() = sizes.minOfOrNull { it.price }
}

/** Rozmiar ma PEŁNĄ cenę, nie dopłatę. Produkty jednorozmiarowe (np. przekąski) mają jeden wpis. */
data class ProductSize(
    val code: SizeCode,
    val volumeMl: Int?,
    val price: Money,
)

enum class SizeCode { S, M, L }

/**
 * Menu sieci w kontekście lokalu: kategorie i produkty są wspólne,
 * [unavailableProductIds] to produkty chwilowo niedostępne w tym lokalu (domyślnie produkt jest dostępny).
 */
data class Menu(
    val categories: List<Category>,
    val products: List<Product>,
    val unavailableProductIds: Set<String>,
)
