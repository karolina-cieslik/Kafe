package com.karolina.cieslik.kafe.feature.menu.data

import com.karolina.cieslik.kafe.core.domain.Money
import com.karolina.cieslik.kafe.feature.menu.data.remote.MenuDto
import com.karolina.cieslik.kafe.feature.menu.data.remote.ProductDto
import com.karolina.cieslik.kafe.feature.menu.data.remote.ProductSizeDto
import com.karolina.cieslik.kafe.feature.menu.domain.ProductSize
import com.karolina.cieslik.kafe.feature.menu.domain.SizeCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// KAFE-6: Testy mapowania DTO menu na domenę.
class MenuMappersTest {

    @Test
    fun sizeCodeIsMappedCaseInsensitively() {
        assertEquals(
            ProductSize(SizeCode.M, 350, Money(1500)),
            ProductSizeDto(code = "m", volumeMl = 350, priceGrosze = 1500).toDomainOrNull(),
        )
    }

    @Test
    fun unknownSizeCodeAndNegativePriceAreDropped() {
        assertNull(ProductSizeDto(code = "XL", priceGrosze = 1500).toDomainOrNull())
        assertNull(ProductSizeDto(code = "S", priceGrosze = -1).toDomainOrNull())
    }

    @Test
    fun blankDescriptionAndImageUrlBecomeNull() {
        val product = ProductDto(id = "p", categoryId = "c", name = "Latte", description = " ", imageUrl = "").toDomain()

        assertNull(product.description)
        assertNull(product.imageUrl)
    }

    @Test
    fun onlyExplicitlyUnavailableProductsAreMarkedUnavailable() {
        val menu = MenuDto().toDomain(availability = mapOf("a" to false, "b" to true))

        assertEquals(setOf("a"), menu.unavailableProductIds)
    }
}
