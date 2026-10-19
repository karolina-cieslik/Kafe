package com.karolina.cieslik.kafe.feature.product.domain

import com.karolina.cieslik.kafe.feature.menu.domain.Menu
import com.karolina.cieslik.kafe.feature.menu.domain.MenuRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Przypadek użycia: szczegóły produktu w lokalu na podstawie zapamiętanego menu (bez osobnego zapytania do sieci). */
class GetProductDetails(private val menuRepository: MenuRepository) {

    /**
     * Obserwuje produkt [productId] w menu lokalu [cafeId].
     *
     * Zimny Flow: emituje od razu stan z zapamiętanego menu i ponownie po każdym zapisie menu, ale tylko gdy
     * wynik się zmienił. [ProductLookup.MenuNotLoaded], gdy menu lokalu nie zostało jeszcze pobrane.
     */
    operator fun invoke(cafeId: String, productId: String): Flow<ProductLookup> =
        menuRepository.observeMenu(cafeId)
            .map { menu -> menu?.findProduct(productId) ?: ProductLookup.MenuNotLoaded }
            .distinctUntilChanged()
}

internal fun Menu.findProduct(productId: String): ProductLookup {
    val product = products.firstOrNull { it.id == productId }
        ?.takeIf { it.isActive && it.sizes.isNotEmpty() }
        ?: return ProductLookup.NotFound
    val groupsById = modifierGroups.associateBy { it.id }
    val productUnavailableSizes = unavailableSizes[product.id].orEmpty()
    return ProductLookup.Found(
        ProductDetails(
            product = product,
            // Wszystkie rozmiary niedostępne = produkt niedostępny w lokalu.
            isAvailable = product.id !in unavailableProductIds && product.sizes.any { it.code !in productUnavailableSizes },
            // Nieznane id grup i grupy bez opcji są pomijane – wymagana pusta grupa zablokowałaby dodanie.
            modifierGroups = product.modifierGroupIds.distinct()
                .mapNotNull { groupsById[it] }
                .filter { it.options.isNotEmpty() },
            unavailableSizes = productUnavailableSizes,
            unavailableOptionIds = unavailableOptionIds,
        ),
    )
}
