package com.karolina.cieslik.kafe.feature.product.domain

import com.karolina.cieslik.kafe.core.domain.Money
import com.karolina.cieslik.kafe.feature.menu.domain.ModifierGroup
import com.karolina.cieslik.kafe.feature.menu.domain.SizeCode
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

// KAFE-9 / KAFE-10 / KAFE-11: Przypadki brzegowe konfiguracji, uzupełniające ProductConfigurationTest deva.
class ProductConfigurationEdgeTest {

    private val calculator = PriceCalculator()
    private val details = details()
    private val initial = ProductConfiguration.initial(details)

    private val oatRemovedGroups = listOf(milkGroup.copy(options = milkGroup.options.filterNot { it.id == "oat" }), shotsGroup, syrupGroup)

    @Test
    fun unavailableDefaultAndSmallFallBackToLarge() {
        val config = ProductConfiguration.initial(details(unavailableSizes = setOf(SizeCode.S, SizeCode.M)))

        assertEquals(SizeCode.L, config.size)
    }

    @Test
    fun changingSizeChangesPriceToFullSizePriceNotSurcharge() {
        assertEquals(Money(1_200), initial.selectSize(SizeCode.S, details).totalPrice(details, calculator))
        assertEquals(Money(1_600), initial.selectSize(SizeCode.L, details).totalPrice(details, calculator))
    }

    @Test
    fun selectingSecondSyrupReplacesFirst() {
        val config = initial.selectOption("syrup", "vanilla", details).selectOption("syrup", "caramel", details)

        assertEquals(mapOf("caramel" to 1), config.selections["syrup"])
    }

    @Test
    fun decreasingShotToZeroRemovesItFromPrice() {
        val config = initial.changeOptionQuantity("extras", "shot", +1, details).changeOptionQuantity("extras", "shot", -1, details)

        assertEquals(0, config.totalIn("extras"))
        assertEquals(Money(1_400), config.totalPrice(details, calculator))
    }

    @Test
    fun deltaLargerThanLimitIsClampedToLimit() {
        assertEquals(4, initial.changeOptionQuantity("extras", "shot", +10, details).quantityOf("extras", "shot"))
    }

    @Test
    fun unavailableShotCanOnlyBeDecreased() {
        val two = initial.changeOptionQuantity("extras", "shot", +2, details)
        val noShots = details(unavailableOptionIds = setOf("shot"))

        assertSame(two, two.changeOptionQuantity("extras", "shot", +1, noShots))
        assertEquals(1, two.changeOptionQuantity("extras", "shot", -1, noShots).quantityOf("extras", "shot"))
    }

    @Test
    fun unknownGroupOrOptionIsIgnored() {
        assertSame(initial, initial.selectOption("unknown", "x", details))
        assertSame(initial, initial.selectOption("milk", "unknown", details))
        assertSame(initial, initial.changeOptionQuantity("extras", "unknown", +1, details))
    }

    @Test
    fun priceIsRecalculatedWhenPriceListChanges() {
        val config = initial.selectOption("milk", "oat", details)
        val cheaperOat = details(groups = listOf(milkGroup.withSurcharge("oat", Money(100)), shotsGroup, syrupGroup))

        assertEquals(Money(1_500), config.totalPrice(cheaperOat, calculator))
    }

    @Test
    fun selectedSizeThatBecameUnavailableIsReported() {
        val config = initial.selectSize(SizeCode.L, details)

        assertEquals(ConfigurationIssue.SizeUnavailable, config.validate(details(unavailableSizes = setOf(SizeCode.L))))
    }

    // BUG (zgłoszony devowi): po odświeżeniu menu wybrana opcja zniknęła z grupy, a jej id jest na liście niedostępnych –
    // validate() robi group.options.first { it.id == optionId } i rzuca NoSuchElementException.
    @Test
    fun validateDoesNotCrashWhenSelectedOptionWasRemovedFromGroup() {
        val config = initial.selectOption("milk", "oat", details)
        val oatRemoved = details(groups = oatRemovedGroups, unavailableOptionIds = setOf("oat"))

        assertIs<ConfigurationIssue>(config.validate(oatRemoved))
    }

    // BUG (zgłoszony devowi): wymagana grupa przechodzi walidację „wyborem-sierotą”, którego nie ma już wśród opcji –
    // UI nie pokazuje żadnego zaznaczonego mleka, a „Dodaj do koszyka” jest aktywne.
    @Test
    fun requiredGroupWithOnlyRemovedOptionSelectedIsMissing() {
        val config = initial.selectOption("milk", "oat", details)
        val oatRemoved = details(groups = oatRemovedGroups)

        assertEquals(ConfigurationIssue.MissingRequired(oatRemoved.modifierGroups.first()), config.validate(oatRemoved))
    }
}

private fun ModifierGroup.withSurcharge(optionId: String, surcharge: Money) =
    copy(options = options.map { if (it.id == optionId) it.copy(surcharge = surcharge) else it })
