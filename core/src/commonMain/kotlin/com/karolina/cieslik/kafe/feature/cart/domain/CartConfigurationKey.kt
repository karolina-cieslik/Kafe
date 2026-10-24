package com.karolina.cieslik.kafe.feature.cart.domain

import com.karolina.cieslik.kafe.feature.menu.domain.SizeCode

/**
 * Klucz konfiguracji pozycji koszyka: dwie pozycje są „identyczne” (scalane), gdy mają ten sam klucz.
 *
 * Do klucza wchodzą tylko id produktu, rozmiar oraz pary (groupId, optionId) z ilością – nazwy i dopłaty nie, bo
 * to migawka, która może się zmienić między dodaniami. Format: `productId|size|groupId:optionId=ilość;…`, opcje
 * posortowane po (groupId, optionId), więc wynik nie zależy od kolejności listy. Opcje z ilością ≤ 0 są pomijane.
 * Znaki separatorów w id są poprzedzane `\`, żeby różne konfiguracje nie dały tego samego klucza.
 *
 * Format zapisany w bazie – zmiana wymaga migracji.
 */
fun cartConfigurationKey(productId: String, size: SizeCode, selections: List<CartSelection>): String {
    val options = selections
        .filter { it.quantity > 0 }
        .sortedWith(compareBy({ it.groupId }, { it.optionId }))
        .joinToString(";") { "${it.groupId.escapeKeyPart()}:${it.optionId.escapeKeyPart()}=${it.quantity}" }
    return "${productId.escapeKeyPart()}|${size.name}|$options"
}

private val KEY_SEPARATORS = setOf('\\', '|', ';', ':', '=')

private fun String.escapeKeyPart(): String = buildString {
    this@escapeKeyPart.forEach { char ->
        if (char in KEY_SEPARATORS) append('\\')
        append(char)
    }
}
