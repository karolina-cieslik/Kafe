package com.karolina.cieslik.kafe.feature.product.domain

import com.karolina.cieslik.kafe.core.domain.Money

/**
 * Cena pozycji, liczona w groszach ([Money]).
 *
 * Cena jednej sztuki to pełna cena wybranego rozmiaru plus suma dopłat wszystkich wybranych opcji, gdzie
 * dopłata każdej opcji jest mnożona przez jej ilość (np. 2 shoty po 2,00 zł = 4,00 zł). Cena pozycji to cena
 * jednej sztuki pomnożona przez liczbę sztuk. Przykład: rozmiar 14,00 zł + mleko owsiane 2,00 zł + 2 shoty
 * po 3,00 zł = 22,00 zł za sztukę, a 2 sztuki = 44,00 zł.
 *
 * Czyste funkcje bez zależności, żeby koszyk liczył tak samo jak ekran produktu.
 */
class PriceCalculator {

    /** Cena jednej sztuki: [sizePrice] + suma (dopłata opcji × jej ilość). */
    fun unitPrice(sizePrice: Money, options: List<PricedOption>): Money =
        options.fold(sizePrice) { total, option -> total + option.surcharge * option.quantity }

    /** Cena [quantity] sztuk: [unitPrice] × [quantity]. */
    fun totalPrice(sizePrice: Money, options: List<PricedOption>, quantity: Int): Money =
        unitPrice(sizePrice, options) * quantity
}

/**
 * Wybrana opcja w obliczeniu ceny.
 *
 * @property surcharge dopłata za jedną sztukę opcji.
 * @property quantity ilość opcji w jednej sztuce produktu (dla SINGLE zawsze 1).
 */
data class PricedOption(val surcharge: Money, val quantity: Int)
