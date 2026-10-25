package com.karolina.cieslik.kafe.feature.cart.domain

import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.EmptyResult
import com.karolina.cieslik.kafe.core.domain.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Wspólny koszyk do testów domeny: [cart] steruje tym, co emituje [observeCart]; [add] zapisuje wywołania
 * w [added] i zwraca [addResult] (domyślnie nowa pozycja zbudowana z dodawanej); [updateSnapshots] zapisuje
 * wywołania w [snapshotUpdates] i zwraca [updateResult]. Pozostałe operacje nie są używane w testach domeny.
 */
internal class FakeCartRepository(initial: Cart = Cart.EMPTY) : CartRepository {
    val cart = MutableStateFlow(initial)
    val added = mutableListOf<NewCartItem>()
    var addResult: Result<AddToCartOutcome, DataError.Local>? = null
    val snapshotUpdates = mutableListOf<List<CartSnapshotUpdate>>()
    var updateResult: EmptyResult<DataError.Local> = Result.Success(Unit)

    override fun observeCart(): Flow<Cart> = cart

    override suspend fun add(item: NewCartItem): Result<AddToCartOutcome, DataError.Local> {
        added += item
        return addResult ?: Result.Success(AddToCartOutcome.Added(item.toCartItem(id = added.size.toLong())))
    }

    override suspend fun updateSnapshots(updates: List<CartSnapshotUpdate>): EmptyResult<DataError.Local> {
        snapshotUpdates += updates
        return updateResult
    }

    override suspend fun setQuantity(itemId: Long, quantity: Int): EmptyResult<DataError.Local> =
        error("nieużywane")

    override suspend fun remove(itemId: Long): Result<CartItem, DataError.Local> = error("nieużywane")

    override suspend fun increase(itemId: Long): Result<CartItem, DataError.Local> = error("nieużywane")

    override suspend fun decreaseOrRemove(itemId: Long): Result<DecreaseOutcome, DataError.Local> =
        error("nieużywane")

    override suspend fun restore(item: CartItem): Result<RestoreOutcome, DataError.Local> = error("nieużywane")

    override suspend fun removeUnreadable(): Result<Int, DataError.Local> = error("nieużywane")

    override suspend fun clear(): EmptyResult<DataError.Local> = error("nieużywane")
}

internal fun NewCartItem.toCartItem(id: Long, quantity: Int = this.quantity) =
    CartItem(id, productId, productName, size, selections, quantity, unitPrice)
