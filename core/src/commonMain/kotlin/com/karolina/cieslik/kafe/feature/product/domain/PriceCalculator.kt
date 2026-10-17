package com.karolina.cieslik.kafe.feature.product.domain

import com.karolina.cieslik.kafe.core.domain.Money

// KAFE-11: Cena pozycji w groszach: (pełna cena rozmiaru + Σ dopłata opcji × jej ilość) × liczba sztuk.
// Czysta funkcja – bez zależności, żeby koszyk (KAFE-12) liczył tak samo jak ekran produktu.
class PriceCalculator {

    fun unitPrice(sizePrice: Money, options: List<PricedOption>): Money =
        options.fold(sizePrice) { total, option -> total + option.surcharge * option.quantity }

    fun totalPrice(sizePrice: Money, options: List<PricedOption>, quantity: Int): Money =
        unitPrice(sizePrice, options) * quantity
}

data class PricedOption(val surcharge: Money, val quantity: Int)
