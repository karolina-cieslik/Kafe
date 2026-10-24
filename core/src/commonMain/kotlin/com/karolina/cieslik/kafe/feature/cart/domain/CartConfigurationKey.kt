package com.karolina.cieslik.kafe.feature.cart.domain

import com.karolina.cieslik.kafe.feature.menu.domain.SizeCode

/**
 * Klucz konfiguracji pozycji koszyka: dwie pozycje są „identyczne” (scalane), gdy mają ten sam klucz.
 *
 * Format: `productId|size|groupId:optionId=ilość,optionId=ilość;groupId:…`, grupy i opcje posortowane po id,
 * więc wynik nie zależy od kolejności map. Opcje z ilością ≤ 0 i puste grupy są pomijane – „brak syropu” to ta
 * sama konfiguracja niezależnie od tego, czy grupa jest w mapie. Znaki separatorów w id są poprzedzane `\`,
 * żeby różne konfiguracje nie dały tego samego klucza.
 *
 * Klucz jest zapisywany w bazie (unikalny indeks), więc zmiana formatu wymaga migracji danych koszyka.
 */
fun cartConfigurationKey(
    productId: String,
    size: SizeCode,
    selections: Map<String, Map<String, Int>>,
): String {
    val groups = selections
        .mapValues { (_, options) -> options.filterValues { it > 0 } }
        .filterValues { it.isNotEmpty() }
        .entries
        .sortedBy { it.key }
        .map { (groupId, options) ->
            val optionsPart = options.entries.sortedBy { it.key }.joinToString(",") { (optionId, quantity) ->
                "${optionId.escapeKeyPart()}=$quantity"
            }
            "${groupId.escapeKeyPart()}:$optionsPart"
        }
    return "${productId.escapeKeyPart()}|${size.name}|${groups.joinToString(";")}"
}

private val KEY_SEPARATORS = setOf('\\', '|', ';', ':', ',', '=')

private fun String.escapeKeyPart(): String = buildString {
    this@escapeKeyPart.forEach { char ->
        if (char in KEY_SEPARATORS) append('\\')
        append(char)
    }
}
