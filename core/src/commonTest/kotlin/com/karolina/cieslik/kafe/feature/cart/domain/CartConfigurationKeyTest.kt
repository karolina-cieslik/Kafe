package com.karolina.cieslik.kafe.feature.cart.domain

import com.karolina.cieslik.kafe.feature.menu.domain.SizeCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/** Klucz konfiguracji decydujący o scalaniu pozycji koszyka. */
class CartConfigurationKeyTest {

    @Test
    fun keyHasReadableFormatWithSortedGroupsAndOptions() {
        val key = cartConfigurationKey(
            "latte",
            SizeCode.M,
            mapOf("milk" to mapOf("oat" to 1), "extras" to mapOf("shot" to 2, "decaf" to 1)),
        )

        assertEquals("latte|M|extras:decaf=1,shot=2;milk:oat=1", key)
    }

    @Test
    fun keyDoesNotDependOnMapOrder() {
        val first = cartConfigurationKey(
            "latte",
            SizeCode.M,
            linkedMapOf("milk" to linkedMapOf("oat" to 1), "extras" to linkedMapOf("shot" to 2, "decaf" to 1)),
        )
        val second = cartConfigurationKey(
            "latte",
            SizeCode.M,
            linkedMapOf("extras" to linkedMapOf("decaf" to 1, "shot" to 2), "milk" to linkedMapOf("oat" to 1)),
        )

        assertEquals(first, second)
    }

    @Test
    fun emptyGroupsAndZeroQuantitiesAreIgnored() {
        val plain = cartConfigurationKey("latte", SizeCode.M, mapOf("milk" to mapOf("oat" to 1)))
        val withEmpty = cartConfigurationKey(
            "latte",
            SizeCode.M,
            mapOf("milk" to mapOf("oat" to 1, "cow" to 0), "syrup" to emptyMap(), "extras" to mapOf("shot" to 0)),
        )

        assertEquals(plain, withEmpty)
    }

    @Test
    fun differentProductSizeOptionOrOptionQuantityGiveDifferentKeys() {
        val base = cartConfigurationKey("latte", SizeCode.M, mapOf("extras" to mapOf("shot" to 1)))

        assertNotEquals(base, cartConfigurationKey("flat-white", SizeCode.M, mapOf("extras" to mapOf("shot" to 1))))
        assertNotEquals(base, cartConfigurationKey("latte", SizeCode.L, mapOf("extras" to mapOf("shot" to 1))))
        assertNotEquals(base, cartConfigurationKey("latte", SizeCode.M, mapOf("extras" to mapOf("shot" to 2))))
        assertNotEquals(base, cartConfigurationKey("latte", SizeCode.M, mapOf("extras" to mapOf("decaf" to 1))))
        assertNotEquals(base, cartConfigurationKey("latte", SizeCode.M, emptyMap()))
    }

    @Test
    fun separatorsInIdsDoNotMakeDifferentConfigurationsEqual() {
        val oneOptionWithComma = cartConfigurationKey("latte", SizeCode.M, mapOf("g" to mapOf("a=1,b" to 1)))
        val twoOptions = cartConfigurationKey("latte", SizeCode.M, mapOf("g" to mapOf("a" to 1, "b" to 1)))
        val productWithPipe = cartConfigurationKey("latte|M|", SizeCode.M, emptyMap())
        val plainProduct = cartConfigurationKey("latte", SizeCode.M, emptyMap())

        assertNotEquals(oneOptionWithComma, twoOptions)
        assertNotEquals(productWithPipe, plainProduct)
        assertEquals("latte\\|M\\||M|", productWithPipe)
    }
}
