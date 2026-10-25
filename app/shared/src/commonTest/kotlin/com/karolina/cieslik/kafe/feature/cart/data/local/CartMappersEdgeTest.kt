package com.karolina.cieslik.kafe.feature.cart.data.local

import com.karolina.cieslik.kafe.core.domain.Money
import com.karolina.cieslik.kafe.feature.cart.domain.CartItem
import com.karolina.cieslik.kafe.feature.cart.domain.CartSelection
import com.karolina.cieslik.kafe.feature.menu.domain.SizeCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Zapis i odczyt pozycji koszyka z wiersza bazy: wszystkie rozmiary, flaga opcji domyślnej, znaki specjalne, długie
 * id, uszkodzone, starsze i przyszłe formaty.
 */
class CartMappersEdgeTest {

    private fun item(
        productId: String = "latte",
        productName: String = "Caffè latte",
        size: SizeCode = SizeCode.L,
        selections: List<CartSelection> = listOf(CartSelection("milk", "milk-oat", "Owsiane", Money(200), 1)),
    ) = CartItem(
        id = 4,
        productId = productId,
        productName = productName,
        size = size,
        selections = selections,
        quantity = 3,
        unitPrice = Money(2_150),
    )

    private val readableRow = item().toEntity()

    @Test
    fun itemSurvivesRoundTripThroughRow() {
        val original = item()

        assertEquals(original, original.toEntity().toDomainOrNull())
    }

    @Test
    fun everySizeWithDefaultAndNonDefaultOptionsSurvivesRoundTrip() {
        val selections = listOf(
            CartSelection("milk", "milk-cow", "Krowie", Money.ZERO, 1, isDefault = true),
            CartSelection("extra", "shot", "Shot espresso", Money(300), 2, isDefault = false),
            CartSelection("syrup", "vanilla", "Waniliowy", Money(250), 1, isDefault = true),
        )

        SizeCode.entries.forEach { size ->
            val original = item(size = size, selections = selections)

            assertEquals(original, original.toEntity().toDomainOrNull(), "Rozmiar: $size")
        }
    }

    @Test
    fun rowFromPreviousFormatWithoutDefaultFlagIsReadableAndNotDefault() {
        val json = """[{"groupId":"milk","optionId":"milk-cow","optionName":"Krowie",""" +
            """"surchargeGrosze":0,"quantity":1},""" +
            """{"groupId":"extra","optionId":"shot","optionName":"Shot espresso","surchargeGrosze":300,"quantity":2}]"""

        val item = assertNotNull(readableRow.copy(selectionsJson = json).toDomainOrNull())

        assertEquals(
            listOf(
                CartSelection("milk", "milk-cow", "Krowie", Money.ZERO, 1, isDefault = false),
                CartSelection("extra", "shot", "Shot espresso", Money(300), 2, isDefault = false),
            ),
            item.selections,
        )
    }

    @Test
    fun defaultFlagIsWrittenEvenWhenFalse() {
        val row = item(selections = listOf(CartSelection("milk", "milk-oat", "Owsiane", Money(200), 1))).toEntity()

        assertTrue("\"isDefault\":false" in row.selectionsJson, row.selectionsJson)
    }

    @Test
    fun defaultFlagIsNotPartOfConfigurationKey() {
        val asDefault = item(selections = listOf(CartSelection("milk", "milk-cow", "Krowie", Money.ZERO, 1, true)))
        val notDefault = item(selections = listOf(CartSelection("milk", "milk-cow", "Krowie", Money.ZERO, 1, false)))

        assertEquals(asDefault.toEntity().configurationKey, notDefault.toEntity().configurationKey)
        assertNotEquals(asDefault.toEntity().selectionsJson, notDefault.toEntity().selectionsJson)
    }

    @Test
    fun specialCharactersInNamesAndIdsSurviveRoundTrip() {
        val tricky = "Cytryna \"bio\" \\ ' ; : = | {}[] \n\t – ąęłóśźżć 😀"
        val original = item(
            productId = "latte|$tricky",
            productName = tricky,
            selections = listOf(CartSelection("g:$tricky", "o=$tricky", tricky, Money(1), 2)),
        )

        val row = original.toEntity()

        assertEquals(original, row.toDomainOrNull())
        assertEquals(original.configurationKey, row.configurationKey)
    }

    @Test
    fun veryLongIdsSurviveRoundTrip() {
        val longId = "x".repeat(5_000)
        val original = item(
            productId = longId,
            selections = listOf(CartSelection(longId, longId, "Opcja", Money.ZERO, 1)),
        )

        assertEquals(original, original.toEntity().toDomainOrNull())
    }

    @Test
    fun itemWithoutOptionsIsStoredAsEmptyList() {
        val row = item(selections = emptyList()).toEntity()

        assertEquals("[]", row.selectionsJson)
        assertEquals(emptyList(), assertNotNull(row.toDomainOrNull()).selections)
    }

    @Test
    fun sizeCodeIsCaseSensitive() {
        assertNull(readableRow.copy(sizeCode = "l").toDomainOrNull())
        assertNull(readableRow.copy(sizeCode = "").toDomainOrNull())
    }

    @Test
    fun selectionsThatAreNotJsonListMakeRowUnreadable() {
        listOf("", "null", "{}", "\"milk\"", "[null]", "[1]").forEach { json ->
            assertNull(readableRow.copy(selectionsJson = json).toDomainOrNull(), "JSON: $json")
        }
    }

    @Test
    fun selectionWithWrongFieldTypeMakesRowUnreadable() {
        val json = """[{"groupId":"milk","optionId":"oat","optionName":"Owsiane",""" +
            """"surchargeGrosze":"dwieście","quantity":1}]"""

        assertNull(readableRow.copy(selectionsJson = json).toDomainOrNull())
    }

    @Test
    fun unknownFieldsFromNewerFormatAreIgnored() {
        val json = """[{"groupId":"milk","optionId":"oat","optionName":"Owsiane",""" +
            """"surchargeGrosze":200,"quantity":1,"note":"nowe"}]"""

        val item = assertNotNull(readableRow.copy(selectionsJson = json).toDomainOrNull())

        assertEquals(listOf(CartSelection("milk", "oat", "Owsiane", Money(200), 1)), item.selections)
    }
}
