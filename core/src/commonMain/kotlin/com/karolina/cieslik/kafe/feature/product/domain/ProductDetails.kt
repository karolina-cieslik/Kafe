package com.karolina.cieslik.kafe.feature.product.domain

import com.karolina.cieslik.kafe.feature.menu.domain.ModifierGroup
import com.karolina.cieslik.kafe.feature.menu.domain.Product
import com.karolina.cieslik.kafe.feature.menu.domain.SizeCode

/**
 * Produkt z dostępnością w wybranym lokalu – wszystko, czego potrzeba do konfiguracji.
 *
 * @property product produkt z menu sieci.
 * @property isAvailable false, gdy produkt jest niedostępny w lokalu albo niedostępne są wszystkie jego rozmiary.
 * @property modifierGroups grupy przypisane do produktu, w kolejności z [Product.modifierGroupIds];
 * bez nieznanych i pustych grup.
 * @property unavailableSizes rozmiary tego produktu niedostępne w lokalu.
 * @property unavailableOptionIds opcje modyfikatorów niedostępne w lokalu (wszystkie, nie tylko tego produktu).
 */
data class ProductDetails(
    val product: Product,
    val isAvailable: Boolean,
    val modifierGroups: List<ModifierGroup>,
    val unavailableSizes: Set<SizeCode>,
    val unavailableOptionIds: Set<String>,
) {
    /** Rozmiar [code] istnieje w produkcie i jest dostępny w lokalu. */
    fun isSizeAvailable(code: SizeCode): Boolean = code !in unavailableSizes && product.sizes.any { it.code == code }

    /** Opcja [optionId] jest dostępna w lokalu (nie sprawdza, czy należy do produktu). */
    fun isOptionAvailable(optionId: String): Boolean = optionId !in unavailableOptionIds
}

/** Wynik wyszukania produktu w zapamiętanym menu lokalu. */
sealed interface ProductLookup {
    /** Menu lokalu nie zostało jeszcze pobrane. */
    data object MenuNotLoaded : ProductLookup

    /** Produktu nie ma w menu albo jest nieaktywny / bez rozmiarów. */
    data object NotFound : ProductLookup

    /** Produkt znaleziony; [details] opisuje go w kontekście lokalu. */
    data class Found(val details: ProductDetails) : ProductLookup
}
