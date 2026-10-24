package com.karolina.cieslik.kafe.feature.cart.domain

import com.karolina.cieslik.kafe.core.domain.Money
import com.karolina.cieslik.kafe.feature.menu.domain.SizeCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/** Przypadki brzegowe klucza konfiguracji: kolejność, puste i zerowe opcje, znaki specjalne w id. */
class CartConfigurationKeyEdgeTest {

    private fun selection(groupId: String, optionId: String, quantity: Int = 1) =
        CartSelection(groupId, optionId, optionName = "nazwa $optionId", surcharge = Money(100), quantity = quantity)

    private fun key(productId: String = "latte", selections: List<CartSelection> = emptyList()) =
        cartConfigurationKey(productId, SizeCode.M, selections)

    @Test
    fun everyPermutationOfSelectionsGivesSameKey() {
        val selections = listOf(
            selection("milk", "milk-oat"),
            selection("shots", "shot", quantity = 2),
            selection("syrup", "syrup-vanilla"),
            selection("milk", "milk-cow"),
        )
        val expected = key(selections = selections)

        permutations(selections).forEach { permutation ->
            assertEquals(expected, key(selections = permutation), "kolejność: $permutation")
        }
    }

    @Test
    fun optionsAreSortedByGroupFirstThenByOption() {
        val selections = listOf(selection("b", "a"), selection("a", "z"), selection("a", "b"))

        assertEquals("latte|M|a:b=1;a:z=1;b:a=1", key(selections = selections))
    }

    @Test
    fun negativeQuantityOptionIsSameAsNoOption() {
        assertEquals(key(), key(selections = listOf(selection("shots", "shot", quantity = -1))))
    }

    @Test
    fun onlyZeroQuantityOptionsGiveSameKeyAsNoOptions() {
        val zeros = listOf(selection("shots", "shot", quantity = 0), selection("syrup", "syrup-vanilla", quantity = 0))

        assertEquals(key(), key(selections = zeros))
        assertEquals("latte|M|", key(selections = zeros))
    }

    @Test
    fun backslashInIdIsEscapedWithBackslash() {
        assertEquals("a\\\\b|M|g\\\\:o\\\\=1", key(productId = "a\\b", selections = listOf(selection("g\\", "o\\"))))
    }

    @Test
    fun idEndingWithBackslashDoesNotCollideWithIdContainingEscapedSeparator() {
        val trailingBackslash = key(productId = "latte\\", selections = listOf(selection("g", "o")))
        val pipeInId = key(productId = "latte\\|M|g:o=1", selections = emptyList())
        val backslashOption = key(selections = listOf(selection("g\\", "o")))
        val separatorInGroup = key(selections = listOf(selection("g\\:o", "x")))

        assertEquals(4, setOf(trailingBackslash, pipeInId, backslashOption, separatorInGroup).size)
        assertNotEquals(
            key(selections = listOf(selection("g\\", "|o"))),
            key(selections = listOf(selection("g", "\\|o"))),
        )
    }

    @Test
    fun differentProductIdsWithSingleOptionNeverShareKey() {
        val ids = shortIds()
        val configurations = ids.flatMap { productId ->
            ids.flatMap { groupId -> ids.map { optionId -> Triple(productId, groupId, optionId) } }
        }

        val keys = configurations.map { (productId, groupId, optionId) ->
            key(productId = productId, selections = listOf(selection(groupId, optionId)))
        }

        assertEquals(configurations.size, keys.toSet().size)
    }

    @Test
    fun twoOptionsNeverShareKeyWithOtherPairOrSingleOption() {
        val ids = shortIds(maxLength = 1)
        val pairs = ids.flatMap { groupId -> ids.map { optionId -> groupId to optionId } }
        val singleOptions = shortIds().flatMap { groupId -> shortIds().map { optionId -> listOf(groupId to optionId) } }
        val twoOptions = pairs.flatMap { first -> pairs.filter { it != first }.map { second -> listOf(first, second) } }
            .distinctBy { it.toSet() }
        val configurations = singleOptions + twoOptions

        val keys = configurations.map { options -> key(selections = options.map { (g, o) -> selection(g, o) }) }

        assertEquals(configurations.size, keys.toSet().size)
    }

    /** Id z liter i wszystkich separatorów klucza (także `\`) o długości 0..[maxLength]. */
    private fun shortIds(maxLength: Int = 2): List<String> {
        val alphabet = listOf("a", "\\", "|", ";", ":", "=")
        var longest = listOf("")
        val ids = mutableListOf("")
        repeat(maxLength) {
            longest = longest.flatMap { prefix -> alphabet.map { prefix + it } }
            ids += longest
        }
        return ids
    }

    private fun <T> permutations(items: List<T>): List<List<T>> =
        if (items.size <= 1) listOf(items)
        else items.indices.flatMap { i ->
            permutations(items.filterIndexed { j, _ -> j != i }).map { listOf(items[i]) + it }
        }
}
