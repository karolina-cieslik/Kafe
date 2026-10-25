package com.karolina.cieslik.kafe.feature.cart.domain

import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.Money
import com.karolina.cieslik.kafe.core.domain.Result
import com.karolina.cieslik.kafe.feature.menu.domain.SizeCode
import com.karolina.cieslik.kafe.feature.product.domain.ConfigurationIssue
import com.karolina.cieslik.kafe.feature.product.domain.PriceCalculator
import com.karolina.cieslik.kafe.feature.product.domain.ProductConfiguration
import com.karolina.cieslik.kafe.feature.product.domain.details
import com.karolina.cieslik.kafe.feature.product.domain.milkGroup
import com.karolina.cieslik.kafe.feature.product.domain.shotsGroup
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Walidacja, cena jednej sztuki i zapis pozycji przy dodaniu do koszyka. */
class AddToCartTest {

    private val repository = FakeCartRepository()
    private val addToCart = AddToCart(repository, PriceCalculator())

    private fun configuration(
        size: SizeCode = SizeCode.M,
        selections: Map<String, Map<String, Int>> = mapOf("milk" to mapOf("cow" to 1)),
        quantity: Int = 1,
    ) = ProductConfiguration(size, selections, quantity)

    @Test
    fun validConfigurationIsAddedWithSnapshotOfNameAndUnitPrice() = runTest {
        val configuration = configuration(
            selections = mapOf("milk" to mapOf("oat" to 1), "extras" to mapOf("shot" to 2)),
            quantity = 3,
        )

        val result = addToCart(details(), configuration)

        assertIs<Result.Success<AddToCartOutcome>>(result)
        assertEquals(
            NewCartItem(
                productId = "latte",
                productName = "Caffè latte",
                size = SizeCode.M,
                selections = listOf(
                    CartSelection("milk", "oat", "Owsiane", Money(200), 1),
                    CartSelection("extras", "shot", "Dodatkowe espresso", Money(300), 2),
                ),
                quantity = 3,
                unitPrice = Money(2_200),
            ),
            repository.added.single(),
        )
    }

    @Test
    fun outcomeFromRepositoryIsReturned() = runTest {
        val capped = AddToCartOutcome.CappedAtLimit(cartItem(quantity = ProductConfiguration.MAX_QUANTITY))
        repository.addResult = Result.Success(capped)

        val result = addToCart(details(), configuration())

        assertEquals(Result.Success(capped), result)
    }

    @Test
    fun invalidConfigurationFailsWithoutWriting() = runTest {
        val result = addToCart(details(), configuration(selections = emptyMap()))

        val failure = assertIs<Result.Failure<AddToCartError>>(result)
        assertEquals(AddToCartError.InvalidConfiguration(ConfigurationIssue.MissingRequired(milkGroup)), failure.error)
        assertTrue(repository.added.isEmpty())
    }

    @Test
    fun unavailableProductFailsWithoutWriting() = runTest {
        val result = addToCart(details(isAvailable = false), configuration())

        val failure = assertIs<Result.Failure<AddToCartError>>(result)
        assertEquals(AddToCartError.InvalidConfiguration(ConfigurationIssue.ProductUnavailable), failure.error)
        assertTrue(repository.added.isEmpty())
    }

    @Test
    fun storageErrorIsWrapped() = runTest {
        repository.addResult = Result.Failure(DataError.Local.DISK_FULL)

        val result = addToCart(details(), configuration())

        assertEquals(Result.Failure(AddToCartError.Storage(DataError.Local.DISK_FULL)), result)
    }

    @Test
    fun selectionsOutsideProductGroupsAndEmptyGroupsAreNotStored() = runTest {
        val configuration = configuration(
            selections = mapOf(
                "milk" to mapOf("cow" to 1, "removed" to 1),
                "syrup" to emptyMap(),
                "extras" to mapOf("shot" to 0),
                "gone" to mapOf("x" to 1),
            ),
        )

        addToCart(details(groups = listOf(milkGroup, shotsGroup)), configuration)

        val added = repository.added.single()
        assertEquals(listOf(CartSelection("milk", "cow", "Krowie", Money.ZERO, 1, isDefault = true)), added.selections)
        assertEquals(Money(1_400), added.unitPrice)
    }

    @Test
    fun onlyDefaultOptionOfGroupIsMarkedAsDefault() = runTest {
        addToCart(details(), configuration(selections = mapOf("milk" to mapOf("cow" to 1))))
        addToCart(details(), configuration(selections = mapOf("milk" to mapOf("oat" to 1))))

        assertEquals(listOf(true, false), repository.added.map { it.selections.single().isDefault })
    }

    @Test
    fun optionOutsideProductGroupsDoesNotChangeConfigurationKey() = runTest {
        addToCart(details(), configuration(selections = mapOf("milk" to mapOf("cow" to 1))))
        addToCart(details(), configuration(selections = mapOf("milk" to mapOf("cow" to 1), "gone" to mapOf("x" to 1))))

        val (first, second) = repository.added
        assertEquals(first.configurationKey, second.configurationKey)
    }

    @Test
    fun quantityOutsideLimitIsClampedBeforeWriting() = runTest {
        addToCart(details(), configuration(quantity = 25))
        addToCart(details(), configuration(quantity = 0))

        assertEquals(
            listOf(ProductConfiguration.MAX_QUANTITY, ProductConfiguration.MIN_QUANTITY),
            repository.added.map { it.quantity },
        )
    }

    private fun cartItem(quantity: Int) = CartItem(
        id = 7,
        productId = "latte",
        productName = "Caffè latte",
        size = SizeCode.M,
        selections = listOf(CartSelection("milk", "cow", "Krowie", Money.ZERO, 1)),
        quantity = quantity,
        unitPrice = Money(1_400),
    )
}
