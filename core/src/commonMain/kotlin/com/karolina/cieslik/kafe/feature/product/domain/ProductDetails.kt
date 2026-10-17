package com.karolina.cieslik.kafe.feature.product.domain

import com.karolina.cieslik.kafe.feature.menu.domain.ModifierGroup
import com.karolina.cieslik.kafe.feature.menu.domain.Product
import com.karolina.cieslik.kafe.feature.menu.domain.SizeCode

// KAFE-9 / KAFE-10: Produkt z dostępnością w wybranym lokalu – wszystko, czego potrzeba do konfiguracji.
data class ProductDetails(
    val product: Product,
    val isAvailable: Boolean,
    /** Grupy przypisane do produktu, w kolejności z [Product.modifierGroupIds]. */
    val modifierGroups: List<ModifierGroup>,
    val unavailableSizes: Set<SizeCode>,
    val unavailableOptionIds: Set<String>,
) {
    fun isSizeAvailable(code: SizeCode): Boolean = code !in unavailableSizes && product.sizes.any { it.code == code }

    fun isOptionAvailable(optionId: String): Boolean = optionId !in unavailableOptionIds
}

sealed interface ProductLookup {
    /** Menu lokalu nie zostało jeszcze pobrane. */
    data object MenuNotLoaded : ProductLookup

    /** Produktu nie ma w menu albo jest nieaktywny / bez rozmiarów. */
    data object NotFound : ProductLookup

    data class Found(val details: ProductDetails) : ProductLookup
}
