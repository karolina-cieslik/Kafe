package com.karolina.cieslik.kafe.feature.cart.domain

import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.Money
import com.karolina.cieslik.kafe.core.domain.Result
import com.karolina.cieslik.kafe.feature.menu.domain.ModifierOption
import com.karolina.cieslik.kafe.feature.menu.domain.SizeCode
import com.karolina.cieslik.kafe.feature.product.domain.ConfigurationIssue
import com.karolina.cieslik.kafe.feature.product.domain.PriceCalculator
import com.karolina.cieslik.kafe.feature.product.domain.ProductConfiguration
import com.karolina.cieslik.kafe.feature.product.domain.ProductTestData
import com.karolina.cieslik.kafe.feature.product.domain.ProductTestData.details
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** Przypadki brzegowe AddToCart: scalanie po kluczu, migawka ceny i opcji, walidacja, limity, błędy zapisu. */
class AddToCartEdgeTest {

    private val repository = FakeCartRepository()
    private val addToCart = AddToCart(repository, PriceCalculator())

    private fun configuration(
        size: SizeCode = SizeCode.M,
        selections: Map<String, Map<String, Int>> = mapOf("milk" to mapOf("milk-cow" to 1)),
        quantity: Int = 1,
    ) = ProductConfiguration(size, selections, quantity)

    @Test
    fun sameChoiceInDifferentMapOrderGivesEqualItemsAndKeys() = runTest {
        val forward = mapOf(
            "milk" to mapOf("milk-oat" to 1),
            "shots" to mapOf("shot" to 2),
            "syrup" to mapOf("syrup-caramel" to 1),
        )
        val reversed = mapOf(
            "syrup" to mapOf("syrup-caramel" to 1),
            "shots" to mapOf("shot" to 2),
            "milk" to mapOf("milk-oat" to 1),
        )

        addToCart(details(), configuration(selections = forward))
        addToCart(details(), configuration(selections = reversed))

        val (first, second) = repository.added
        assertEquals(first, second)
        assertEquals(first.configurationKey, second.configurationKey)
    }

    @Test
    fun selectionsFollowProductGroupOrderNotMapOrder() = runTest {
        val selections = mapOf(
            "syrup" to mapOf("syrup-vanilla" to 1),
            "shots" to mapOf("shot" to 1),
            "milk" to mapOf("milk-oat" to 1),
        )

        addToCart(details(), configuration(selections = selections))

        assertEquals(listOf("milk", "shots", "syrup"), repository.added.single().selections.map { it.groupId })
    }

    @Test
    fun differentSizeOrOptionQuantityGivesDifferentKey() = runTest {
        val base = mapOf("milk" to mapOf("milk-cow" to 1), "shots" to mapOf("shot" to 1))

        addToCart(details(), configuration(selections = base))
        addToCart(details(), configuration(size = SizeCode.L, selections = base))
        addToCart(details(), configuration(selections = base + ("shots" to mapOf("shot" to 2))))
        addToCart(details(), configuration(selections = base + ("milk" to mapOf("milk-oat" to 1))))

        assertEquals(4, repository.added.map { it.configurationKey }.toSet().size)
    }

    @Test
    fun quantityDoesNotChangeKeySoSameChoiceIsMerged() = runTest {
        addToCart(details(), configuration(quantity = 1))
        addToCart(details(), configuration(quantity = 7))

        val (first, second) = repository.added
        assertEquals(first.configurationKey, second.configurationKey)
    }

    @Test
    fun unitPriceIsPriceOfOnePieceWhateverTheQuantity() = runTest {
        val selections = mapOf("milk" to mapOf("milk-oat" to 1), "shots" to mapOf("shot" to 2))

        addToCart(details(), configuration(selections = selections, quantity = 1))
        addToCart(details(), configuration(selections = selections, quantity = ProductConfiguration.MAX_QUANTITY))

        assertEquals(listOf(Money(2_400), Money(2_400)), repository.added.map { it.unitPrice })
    }

    @Test
    fun unitPriceAndNamesAreTakenFromMenuAtTimeOfAdding() = runTest {
        val cheaperOat = ProductTestData.milk.copy(
            options = listOf(
                ModifierOption("milk-cow", "Krowie", Money.ZERO),
                ModifierOption("milk-oat", "Owsiane bio", Money(150)),
            ),
        )
        val renamed = ProductTestData.latte.copy(name = "Latte jesienne")
        val selections = mapOf("milk" to mapOf("milk-oat" to 1))

        addToCart(details(), configuration(selections = selections))
        addToCart(
            details(product = renamed, groups = listOf(cheaperOat, ProductTestData.shots, ProductTestData.syrup)),
            configuration(selections = selections),
        )

        val (before, after) = repository.added
        assertEquals(Money(1_800), before.unitPrice)
        assertEquals(Money(1_750), after.unitPrice)
        assertEquals("Latte jesienne", after.productName)
        assertEquals(CartSelection("milk", "milk-oat", "Owsiane bio", Money(150), 1), after.selections.single())
        assertEquals(before.configurationKey, after.configurationKey)
    }

    @Test
    fun sizeUnavailableInCafeFailsWithoutWriting() = runTest {
        val result = addToCart(details(unavailableSizes = setOf(SizeCode.L)), configuration(size = SizeCode.L))

        assertEquals(Result.Failure(AddToCartError.InvalidConfiguration(ConfigurationIssue.SizeUnavailable)), result)
        assertTrue(repository.added.isEmpty())
    }

    @Test
    fun sizeNotOfferedByProductFailsWithoutWriting() = runTest {
        val onlySmallAndMedium = ProductTestData.latte.copy(
            sizes = ProductTestData.latte.sizes.filter { it.code != SizeCode.L },
        )

        val result = addToCart(details(product = onlySmallAndMedium), configuration(size = SizeCode.L))

        assertEquals(Result.Failure(AddToCartError.InvalidConfiguration(ConfigurationIssue.SizeUnavailable)), result)
        assertTrue(repository.added.isEmpty())
    }

    @Test
    fun selectedUnavailableOptionFailsWithoutWriting() = runTest {
        val result = addToCart(
            details(unavailableOptionIds = setOf("syrup-caramel")),
            configuration(selections = mapOf("milk" to mapOf("milk-cow" to 1), "syrup" to mapOf("syrup-caramel" to 1))),
        )

        val expected = ConfigurationIssue.OptionUnavailable(ProductTestData.syrup, "Karmelowy")
        assertEquals(Result.Failure(AddToCartError.InvalidConfiguration(expected)), result)
        assertTrue(repository.added.isEmpty())
    }

    @Test
    fun unavailableOptionThatIsNotSelectedDoesNotBlockAdding() = runTest {
        val result = addToCart(details(unavailableOptionIds = setOf("syrup-caramel")), configuration())

        assertTrue(result is Result.Success)
        assertEquals(1, repository.added.size)
    }

    @Test
    fun requiredGroupWithOnlyRemovedOptionFailsAsMissing() = runTest {
        val result = addToCart(details(), configuration(selections = mapOf("milk" to mapOf("milk-goat" to 1))))

        val expected = ConfigurationIssue.MissingRequired(ProductTestData.milk)
        assertEquals(Result.Failure(AddToCartError.InvalidConfiguration(expected)), result)
        assertTrue(repository.added.isEmpty())
    }

    @Test
    fun negativeOptionQuantityIsNotStored() = runTest {
        val selections = mapOf("milk" to mapOf("milk-cow" to 1), "shots" to mapOf("shot" to -2))

        addToCart(details(), configuration(selections = selections))

        val added = repository.added.single()
        assertEquals(listOf("milk-cow"), added.selections.map { it.optionId })
        assertEquals(Money(1_600), added.unitPrice)
    }

    @Test
    fun quantityAtLimitsIsKeptAndNegativeIsRaisedToMinimum() = runTest {
        listOf(ProductConfiguration.MIN_QUANTITY, ProductConfiguration.MAX_QUANTITY, -5, 21).forEach { quantity ->
            addToCart(details(), configuration(quantity = quantity))
        }

        assertEquals(listOf(1, 20, 1, 20), repository.added.map { it.quantity })
    }

    @Test
    fun everyLocalErrorIsReturnedAsStorageError() = runTest {
        DataError.Local.entries.forEach { error ->
            repository.addResult = Result.Failure(error)

            assertEquals(Result.Failure(AddToCartError.Storage(error)), addToCart(details(), configuration()))
        }
    }

    @Test
    fun quantityIncreasedOutcomeIsPassedThrough() = runTest {
        val merged = AddToCartOutcome.QuantityIncreased(cartItem(quantity = 3))
        repository.addResult = Result.Success(merged)

        assertEquals(Result.Success(merged), addToCart(details(), configuration()))
    }

    @Test
    fun differentProductsWithSameChoiceAreDifferentPositions() = runTest {
        val mocha = ProductTestData.latte.copy(id = "mocha", name = "Mocha")

        addToCart(details(), configuration())
        addToCart(details(product = mocha), configuration())

        val (latte, other) = repository.added
        assertNotEquals(latte.configurationKey, other.configurationKey)
    }

    private fun cartItem(quantity: Int) = CartItem(
        id = 3,
        productId = "latte",
        productName = "Caffè latte",
        size = SizeCode.M,
        selections = listOf(CartSelection("milk", "milk-cow", "Krowie", Money.ZERO, 1)),
        quantity = quantity,
        unitPrice = Money(1_600),
    )
}
