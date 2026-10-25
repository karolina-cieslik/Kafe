package com.karolina.cieslik.kafe.feature.cart.domain

import com.karolina.cieslik.kafe.feature.menu.domain.Menu
import com.karolina.cieslik.kafe.feature.menu.domain.MenuRepository
import com.karolina.cieslik.kafe.feature.product.domain.PriceCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Przypadek użycia: koszyk sprawdzony z zapamiętanym menu lokalu (ceny sieci z ostatniego pobrania + dostępność
 * lokalu). Tylko czyta – nową migawkę cen zapisuje [RefreshCartSnapshots].
 */
class ObserveCheckedCart(
    private val cartRepository: CartRepository,
    private val menuRepository: MenuRepository,
    private val priceCalculator: PriceCalculator,
) {

    /**
     * Obserwuje koszyk sprawdzony z menu lokalu [cafeId]. Zimny Flow: emituje od razu, potem po każdej zmianie
     * koszyka albo menu, tylko gdy wynik się zmienił. Bez zapamiętanego menu lokalu – wszystkie pozycje
     * [CartItemStatus.Unverified] i [CheckedCart.isVerified] = false.
     */
    operator fun invoke(cafeId: String): Flow<CheckedCart> =
        combine(cartRepository.observeCart(), menuRepository.observeMenu(cafeId)) { cart, menu -> check(cart, menu) }
            .distinctUntilChanged()

    private fun check(cart: Cart, menu: Menu?): CheckedCart {
        if (menu == null) {
            return CheckedCart(
                items = cart.items.map { CheckedCartItem(it, CartItemStatus.Unverified, it.unitPrice) },
                isVerified = false,
            )
        }
        val items = cart.items.map { item ->
            when (val check = item.checkWith(menu, priceCalculator)) {
                is CartItemCheck.Unavailable ->
                    CheckedCartItem(item, CartItemStatus.Unavailable(check.reason), item.unitPrice)
                is CartItemCheck.Valid -> {
                    val price = check.snapshot.unitPrice
                    val status = if (price == item.unitPrice) {
                        CartItemStatus.Available
                    } else {
                        CartItemStatus.PriceChanged(previousUnitPrice = item.unitPrice)
                    }
                    CheckedCartItem(item, status, price)
                }
            }
        }
        return CheckedCart(items, isVerified = true)
    }
}
