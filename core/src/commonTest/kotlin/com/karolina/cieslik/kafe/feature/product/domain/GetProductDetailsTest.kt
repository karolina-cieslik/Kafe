package com.karolina.cieslik.kafe.feature.product.domain

import com.karolina.cieslik.kafe.feature.menu.domain.Menu
import com.karolina.cieslik.kafe.feature.menu.domain.ModifierGroup
import com.karolina.cieslik.kafe.feature.menu.domain.Product
import com.karolina.cieslik.kafe.feature.menu.domain.SizeCode
import com.karolina.cieslik.kafe.feature.product.domain.ProductTestData.latte
import com.karolina.cieslik.kafe.feature.product.domain.ProductTestData.milk
import com.karolina.cieslik.kafe.feature.product.domain.ProductTestData.shots
import com.karolina.cieslik.kafe.feature.product.domain.ProductTestData.syrup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

// KAFE-9: Wyszukanie produktu w menu lokalu.
class GetProductDetailsTest {

    private fun menu(
        vararg products: Product,
        unavailableProductIds: Set<String> = emptySet(),
        unavailableSizes: Map<String, Set<SizeCode>> = emptyMap(),
    ) = Menu(
        categories = emptyList(),
        products = products.toList(),
        unavailableProductIds = unavailableProductIds,
        modifierGroups = listOf(syrup, milk, shots, ModifierGroup("empty", "Pusta", milk.type, true, 1, emptyList())),
        unavailableSizes = unavailableSizes,
        unavailableOptionIds = setOf("milk-oat"),
    )

    @Test
    fun foundProductCarriesAvailabilityAndGroupsInProductOrder() {
        val product = latte.copy(modifierGroupIds = listOf("milk", "missing", "empty", "shots", "milk"))
        val menu = menu(product, unavailableSizes = mapOf("latte" to setOf(SizeCode.L), "other" to setOf(SizeCode.S)))

        val found = assertIs<ProductLookup.Found>(menu.findProduct("latte"))

        assertEquals(true, found.details.isAvailable)
        assertEquals(listOf("milk", "shots"), found.details.modifierGroups.map { it.id })
        assertEquals(setOf(SizeCode.L), found.details.unavailableSizes)
        assertEquals(false, found.details.isOptionAvailable("milk-oat"))
    }

    @Test
    fun productMarkedUnavailableInCafeIsUnavailable() {
        val found = assertIs<ProductLookup.Found>(menu(latte, unavailableProductIds = setOf("latte")).findProduct("latte"))

        assertEquals(false, found.details.isAvailable)
    }

    @Test
    fun productWithAllSizesUnavailableIsUnavailable() {
        val menu = menu(latte, unavailableSizes = mapOf("latte" to SizeCode.entries.toSet()))

        val found = assertIs<ProductLookup.Found>(menu.findProduct("latte"))

        assertEquals(false, found.details.isAvailable)
    }

    @Test
    fun missingInactiveOrSizelessProductIsNotFound() {
        assertEquals(ProductLookup.NotFound, menu(latte).findProduct("unknown"))
        assertEquals(ProductLookup.NotFound, menu(latte.copy(isActive = false)).findProduct("latte"))
        assertEquals(ProductLookup.NotFound, menu(latte.copy(sizes = emptyList())).findProduct("latte"))
    }
}
