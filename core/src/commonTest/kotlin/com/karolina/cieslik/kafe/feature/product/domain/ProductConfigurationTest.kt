package com.karolina.cieslik.kafe.feature.product.domain

import com.karolina.cieslik.kafe.core.domain.Money
import com.karolina.cieslik.kafe.feature.menu.domain.SizeCode
import com.karolina.cieslik.kafe.feature.product.domain.ProductTestData.details
import com.karolina.cieslik.kafe.feature.product.domain.ProductTestData.latte
import com.karolina.cieslik.kafe.feature.product.domain.ProductTestData.milk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/** Reguły konfiguracji produktu; na końcu regresje od testera – odświeżenie menu w trakcie konfiguracji. */
class ProductConfigurationTest {

    private val calculator = PriceCalculator()

    @Test
    fun initialUsesDefaultSizeDefaultMilkAndNoOptionalExtras() {
        val config = ProductConfiguration.initial(details())

        assertEquals(SizeCode.M, config.size)
        assertEquals(mapOf("milk" to mapOf("milk-cow" to 1)), config.selections)
        assertEquals(1, config.quantity)
        assertNull(config.validate(details()))
        assertEquals(Money(1600), config.totalPrice(details(), calculator))
    }

    @Test
    fun unavailableDefaultSizeFallsBackToFirstAvailable() {
        val details = details(unavailableSizes = setOf(SizeCode.M, SizeCode.S))

        assertEquals(SizeCode.L, ProductConfiguration.initial(details).size)
    }

    @Test
    fun withoutDefaultSizeFirstAvailableIsChosen() {
        val details = details(product = latte.copy(defaultSize = null), unavailableSizes = setOf(SizeCode.S))

        assertEquals(SizeCode.M, ProductConfiguration.initial(details).size)
    }

    @Test
    fun allSizesUnavailableKeepsDefaultAndBlocksAddToCart() {
        val details = details(unavailableSizes = SizeCode.entries.toSet())
        val config = ProductConfiguration.initial(details)

        assertEquals(SizeCode.M, config.size)
        assertEquals(ConfigurationIssue.SizeUnavailable, config.validate(details))
    }

    @Test
    fun unavailableSizeCannotBeSelected() {
        val details = details(unavailableSizes = setOf(SizeCode.L))
        val config = ProductConfiguration.initial(details)

        assertEquals(SizeCode.M, config.selectSize(SizeCode.L, details).size)
        assertEquals(SizeCode.S, config.selectSize(SizeCode.S, details).size)
    }

    @Test
    fun unavailableDefaultMilkFallsBackToFirstAvailableOption() {
        val details = details(unavailableOptionIds = setOf("milk-cow"))

        assertEquals(mapOf("milk-oat" to 1), ProductConfiguration.initial(details).selections["milk"])
    }

    @Test
    fun requiredGroupWithoutAvailableOptionsBlocksAddToCart() {
        val details = details(unavailableOptionIds = setOf("milk-cow", "milk-oat"))

        assertEquals(ConfigurationIssue.MissingRequired(milk), ProductConfiguration.initial(details).validate(details))
    }

    @Test
    fun requiredSingleOptionCannotBeDeselectedButCanBeSwitched() {
        val details = details()
        val config = ProductConfiguration.initial(details)

        assertEquals(config, config.selectOption("milk", "milk-cow", details))
        assertEquals(mapOf("milk-oat" to 1), config.selectOption("milk", "milk-oat", details).selections["milk"])
    }

    @Test
    fun optionalSingleOptionTogglesOff() {
        val details = details()
        val withSyrup = ProductConfiguration.initial(details).selectOption("syrup", "syrup-vanilla", details)

        assertEquals(mapOf("syrup-vanilla" to 1), withSyrup.selections["syrup"])
        assertEquals(0, withSyrup.selectOption("syrup", "syrup-vanilla", details).totalIn("syrup"))
    }

    @Test
    fun unavailableOptionCannotBeSelected() {
        val details = details(unavailableOptionIds = setOf("syrup-caramel"))
        val config = ProductConfiguration.initial(details)

        assertEquals(config, config.selectOption("syrup", "syrup-caramel", details))
    }

    @Test
    fun shotsAreLimitedToZeroUntilFour() {
        val details = details()
        var config = ProductConfiguration.initial(details)

        repeat(6) { config = config.changeOptionQuantity("shots", "shot", +1, details) }
        assertEquals(4, config.quantityOf("shots", "shot"))

        repeat(6) { config = config.changeOptionQuantity("shots", "shot", -1, details) }
        assertEquals(0, config.quantityOf("shots", "shot"))
        assertNull(config.selections["shots"]?.get("shot"))
    }

    @Test
    fun quantityChangeOnSingleGroupIsIgnored() {
        val details = details()
        val config = ProductConfiguration.initial(details)

        assertEquals(config, config.changeOptionQuantity("syrup", "syrup-vanilla", +1, details))
        assertEquals(config, config.selectOption("shots", "shot", details))
    }

    @Test
    fun itemQuantityIsClampedBetweenOneAndTwenty() {
        val config = ProductConfiguration.initial(details())

        assertEquals(1, config.changeQuantity(-1).quantity)
        assertEquals(20, config.changeQuantity(+100).quantity)
    }

    @Test
    fun totalPriceFollowsKafe11Formula() {
        val details = details()
        val config = ProductConfiguration.initial(details)
            .selectSize(SizeCode.L, details)
            .selectOption("milk", "milk-oat", details)
            .changeOptionQuantity("shots", "shot", +2, details)
            .selectOption("syrup", "syrup-caramel", details)
            .changeQuantity(+1)

        // (1800 + 200 + 2 * 300 + 250) * 2
        assertEquals(Money(5700), config.totalPrice(details, calculator))
    }

    @Test
    fun selectedOptionThatBecameUnavailableBlocksAddToCart() {
        val config = ProductConfiguration.initial(details()).selectOption("syrup", "syrup-vanilla", details())
        val later = details(unavailableOptionIds = setOf("syrup-vanilla"))

        val issue = assertIs<ConfigurationIssue.OptionUnavailable>(config.validate(later))
        assertEquals("Waniliowy", issue.optionName)
        // Niedostępną opcję da się odznaczyć.
        assertNull(config.selectOption("syrup", "syrup-vanilla", later).validate(later))
    }

    @Test
    fun validateDoesNotCrashWhenSelectedOptionWasRemovedFromGroup() {
        val config = ProductConfiguration.initial(details()).selectOption("syrup", "syrup-vanilla", details())
        val later = details(
            groups = listOf(milk, ProductTestData.shots, ProductTestData.syrup.copy(options = ProductTestData.syrup.options.drop(1))),
            unavailableOptionIds = setOf("syrup-vanilla"),
        )

        assertNull(config.validate(later))
    }

    @Test
    fun requiredGroupWithOnlyRemovedOptionSelectedIsMissingAndNormalizationRestoresDefault() {
        val config = ProductConfiguration.initial(details()).selectOption("milk", "milk-oat", details())
        val later = details(groups = listOf(milk.copy(options = milk.options.take(1)), ProductTestData.shots, ProductTestData.syrup))

        assertEquals(ConfigurationIssue.MissingRequired(later.modifierGroups[0]), config.validate(later))
        val normalized = config.normalizedFor(later)
        assertEquals(mapOf("milk-cow" to 1), normalized.configuration.selections["milk"])
        assertEquals(emptyList(), normalized.deselectedOptions)
        assertNull(normalized.configuration.validate(later))
    }

    @Test
    fun normalizationDeselectsOptionThatBecameUnavailable() {
        val config = ProductConfiguration.initial(details()).selectOption("syrup", "syrup-vanilla", details())
        val later = details(unavailableOptionIds = setOf("syrup-vanilla"))

        val normalized = config.normalizedFor(later)

        assertEquals(listOf("Waniliowy"), normalized.deselectedOptions)
        assertNull(normalized.configuration.selections["syrup"])
        assertNull(normalized.configuration.validate(later))
        assertEquals(Money(1600), normalized.configuration.totalPrice(later, calculator))
    }

    @Test
    fun normalizationClampsQuantityToNewLimit() {
        var config = ProductConfiguration.initial(details())
        repeat(4) { config = config.changeOptionQuantity("shots", "shot", +1, details()) }
        val later = details(groups = listOf(milk, ProductTestData.shots.copy(maxQuantity = 2), ProductTestData.syrup))

        assertEquals(2, config.normalizedFor(later).configuration.quantityOf("shots", "shot"))
    }

    @Test
    fun unavailableProductBlocksAddToCart() {
        val details = details(isAvailable = false)

        assertEquals(ConfigurationIssue.ProductUnavailable, ProductConfiguration.initial(details).validate(details))
    }
}
