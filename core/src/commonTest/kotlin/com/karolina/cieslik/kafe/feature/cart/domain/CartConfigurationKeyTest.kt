package com.karolina.cieslik.kafe.feature.cart.domain

import com.karolina.cieslik.kafe.core.domain.Money
import com.karolina.cieslik.kafe.feature.menu.domain.SizeCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/** Klucz konfiguracji decydujący o scalaniu pozycji koszyka. */
class CartConfigurationKeyTest {

    private fun selection(groupId: String, optionId: String, quantity: Int = 1) =
        CartSelection(groupId, optionId, optionName = optionId, surcharge = Money(100), quantity = quantity)

    private val oat = selection("milk", "oat")
    private val shots = selection("extras", "shot", quantity = 2)
    private val decaf = selection("extras", "decaf")

    @Test
    fun keyHasReadableFormatWithOptionsSortedByGroupAndOption() {
        val key = cartConfigurationKey("latte", SizeCode.M, listOf(oat, shots, decaf))

        assertEquals("latte|M|extras:decaf=1;extras:shot=2;milk:oat=1", key)
    }

    @Test
    fun keyDoesNotDependOnSelectionOrder() {
        assertEquals(
            cartConfigurationKey("latte", SizeCode.M, listOf(oat, shots, decaf)),
            cartConfigurationKey("latte", SizeCode.M, listOf(decaf, oat, shots)),
        )
    }

    @Test
    fun zeroQuantityOptionIsSameAsNoOption() {
        assertEquals(
            cartConfigurationKey("latte", SizeCode.M, listOf(oat)),
            cartConfigurationKey("latte", SizeCode.M, listOf(oat, selection("syrup", "vanilla", quantity = 0))),
        )
    }

    @Test
    fun optionNameAndSurchargeDoNotAffectKey() {
        assertEquals(
            cartConfigurationKey("latte", SizeCode.M, listOf(oat)),
            cartConfigurationKey("latte", SizeCode.M, listOf(oat.copy(optionName = "Owsiane", surcharge = Money(250)))),
        )
    }

    @Test
    fun differentProductSizeOptionOrOptionQuantityGiveDifferentKeys() {
        val base = cartConfigurationKey("latte", SizeCode.M, listOf(shots))

        assertNotEquals(base, cartConfigurationKey("flat-white", SizeCode.M, listOf(shots)))
        assertNotEquals(base, cartConfigurationKey("latte", SizeCode.L, listOf(shots)))
        assertNotEquals(base, cartConfigurationKey("latte", SizeCode.M, listOf(shots.copy(quantity = 3))))
        assertNotEquals(base, cartConfigurationKey("latte", SizeCode.M, listOf(decaf)))
        assertNotEquals(base, cartConfigurationKey("latte", SizeCode.M, emptyList()))
    }

    @Test
    fun separatorsInIdsDoNotMakeDifferentConfigurationsEqual() {
        val oneOptionWithSeparators = cartConfigurationKey("latte", SizeCode.M, listOf(selection("g", "a=1;g:b")))
        val twoOptions = cartConfigurationKey("latte", SizeCode.M, listOf(selection("g", "a"), selection("g", "b")))
        val productWithPipe = cartConfigurationKey("latte|M|", SizeCode.M, emptyList())
        val plainProduct = cartConfigurationKey("latte", SizeCode.M, emptyList())

        assertNotEquals(oneOptionWithSeparators, twoOptions)
        assertNotEquals(productWithPipe, plainProduct)
        assertEquals("latte\\|M\\||M|", productWithPipe)
    }
}
