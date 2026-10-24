package com.karolina.cieslik.kafe.feature.cart

import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.EmptyResult
import com.karolina.cieslik.kafe.core.domain.Result
import com.karolina.cieslik.kafe.feature.cart.domain.AddToCartOutcome
import com.karolina.cieslik.kafe.feature.cart.domain.Cart
import com.karolina.cieslik.kafe.feature.cart.domain.CartItem
import com.karolina.cieslik.kafe.feature.cart.domain.CartRepository
import com.karolina.cieslik.kafe.feature.cart.domain.NewCartItem
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Koszyk do testów prezentacji: [cart] steruje tym, co emituje [observeCart], a [add] zapisuje wywołania
 * i zwraca [addResult] (domyślnie nowa pozycja zbudowana z dodawanej). [addGate] pozwala zatrzymać zapis
 * w trakcie, np. żeby sprawdzić podwójne kliknięcie.
 */
internal class FakeCartRepository(initial: Cart = Cart.EMPTY) : CartRepository {
    val cart = MutableStateFlow(initial)
    val added = mutableListOf<NewCartItem>()
    var addResult: ((NewCartItem) -> Result<AddToCartOutcome, DataError.Local>)? = null
    var addGate: CompletableDeferred<Unit>? = null

    override fun observeCart(): Flow<Cart> = cart

    override suspend fun add(item: NewCartItem): Result<AddToCartOutcome, DataError.Local> {
        added += item
        addGate?.await()
        return addResult?.invoke(item)
            ?: Result.Success(AddToCartOutcome.Added(item.toCartItem(id = added.size.toLong())))
    }

    override suspend fun setQuantity(itemId: Long, quantity: Int): EmptyResult<DataError.Local> =
        error("nieużywane")

    override suspend fun remove(itemId: Long): Result<CartItem, DataError.Local> = error("nieużywane")

    override suspend fun restore(item: CartItem): Result<AddToCartOutcome, DataError.Local> = error("nieużywane")

    override suspend fun clear(): EmptyResult<DataError.Local> = error("nieużywane")
}

internal fun NewCartItem.toCartItem(id: Long, quantity: Int = this.quantity) =
    CartItem(id, productId, productName, size, selections, quantity, unitPrice)
