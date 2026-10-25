package com.karolina.cieslik.kafe.feature.cart.presentation

/** Stabilne tagi ekranu koszyka dla testów UI (Robolectric i instrumentacyjnych). */
object CartTestTags {
    /** Wskaźnik w stanie ładowania. */
    const val LOADING = "cartLoading"

    /** Lista pozycji w stanie treści. */
    const val LIST = "cartList"

    /** „Wyczyść” w górnym pasku. */
    const val CLEAR = "cartClear"

    /** Przypięte podsumowanie na dole ekranu. */
    const val SUMMARY = "cartSummary"

    /** Baner „Nie udało się sprawdzić aktualnych cen”. */
    const val PRICES_UNVERIFIED = "cartPricesUnverified"

    /** Wiersz pozycji o danym id. */
    fun item(id: Long) = "cartItem_$id"
}
