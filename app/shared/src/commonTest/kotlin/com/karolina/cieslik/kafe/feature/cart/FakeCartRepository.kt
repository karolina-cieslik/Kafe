package com.karolina.cieslik.kafe.feature.cart

import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.EmptyResult
import com.karolina.cieslik.kafe.core.domain.Result
import com.karolina.cieslik.kafe.feature.cart.domain.AddToCartOutcome
import com.karolina.cieslik.kafe.feature.cart.domain.Cart
import com.karolina.cieslik.kafe.feature.cart.domain.CartItem
import com.karolina.cieslik.kafe.feature.cart.domain.CartRepository
import com.karolina.cieslik.kafe.feature.cart.domain.CartSnapshotUpdate
import com.karolina.cieslik.kafe.feature.cart.domain.DecreaseOutcome
import com.karolina.cieslik.kafe.feature.cart.domain.NewCartItem
import com.karolina.cieslik.kafe.feature.cart.domain.RestoreOutcome
import com.karolina.cieslik.kafe.feature.product.domain.ProductConfiguration
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Koszyk do testów prezentacji: [cart] steruje tym, co emituje [observeCart], a operacje zmieniają go tak jak
 * prawdziwe repozytorium (limit 20, „Cofnij” na to samo miejsce, scalanie po kluczu). [add] zapisuje wywołania
 * i zwraca [addResult] (domyślnie nowa pozycja zbudowana z dodawanej); [addGate] pozwala zatrzymać zapis
 * w trakcie, a [completed] zawiera tylko zapisy, które przeszły bramkę. [writeError] sprawia, że następne
 * zapisy kończą się tym błędem bez zmian; [writeGate] wstrzymuje zapisy koszyka do `complete()`.
 * [removeUnreadableResult] to wynik `removeUnreadable` (domyślnie nic do usunięcia).
 */
internal class FakeCartRepository(initial: Cart = Cart.EMPTY) : CartRepository {
    val cart = MutableStateFlow(initial)
    val added = mutableListOf<NewCartItem>()
    val completed = mutableListOf<NewCartItem>()
    var addResult: ((NewCartItem) -> Result<AddToCartOutcome, DataError.Local>)? = null
    var addGate: CompletableDeferred<Unit>? = null
    var writeError: DataError.Local? = null
    var writeGate: CompletableDeferred<Unit>? = null
    val snapshotUpdates = mutableListOf<List<CartSnapshotUpdate>>()
    var removeUnreadableCalls = 0
    var removeUnreadableResult: Result<Int, DataError.Local> = Result.Success(0)

    private val items get() = cart.value.items

    override fun observeCart(): Flow<Cart> = cart

    override suspend fun add(item: NewCartItem): Result<AddToCartOutcome, DataError.Local> {
        added += item
        addGate?.await()
        completed += item
        return addResult?.invoke(item)
            ?: Result.Success(AddToCartOutcome.Added(item.toCartItem(id = added.size.toLong())))
    }

    override suspend fun setQuantity(itemId: Long, quantity: Int): EmptyResult<DataError.Local> =
        error("nieużywane")

    override suspend fun increase(itemId: Long): Result<CartItem, DataError.Local> = write {
        val item = items.find(itemId) ?: return@write Result.Failure(DataError.Local.NOT_FOUND)
        val increased = item.copy(quantity = minOf(item.quantity + 1, ProductConfiguration.MAX_QUANTITY))
        replace(increased)
        Result.Success(increased)
    }

    override suspend fun decreaseOrRemove(itemId: Long): Result<DecreaseOutcome, DataError.Local> = write {
        val item = items.find(itemId) ?: return@write Result.Failure(DataError.Local.NOT_FOUND)
        if (item.quantity <= 1) {
            setItems(items - item)
            Result.Success(DecreaseOutcome.Removed(item))
        } else {
            val decreased = item.copy(quantity = item.quantity - 1)
            replace(decreased)
            Result.Success(DecreaseOutcome.Decreased(decreased))
        }
    }

    override suspend fun remove(itemId: Long): Result<CartItem, DataError.Local> = write {
        val item = items.find(itemId) ?: return@write Result.Failure(DataError.Local.NOT_FOUND)
        setItems(items - item)
        Result.Success(item)
    }

    override suspend fun restore(item: CartItem): Result<RestoreOutcome, DataError.Local> = write {
        items.find(item.id)?.let { return@write Result.Success(RestoreOutcome.AlreadyInCart(it)) }
        val same = items.firstOrNull { it.configurationKey == item.configurationKey }
        if (same == null) {
            setItems(items + item)
            Result.Success(RestoreOutcome.Restored(item))
        } else {
            val sum = same.quantity + item.quantity
            val merged = same.copy(quantity = minOf(sum, ProductConfiguration.MAX_QUANTITY))
            replace(merged)
            Result.Success(RestoreOutcome.Merged(merged, capped = sum > ProductConfiguration.MAX_QUANTITY))
        }
    }

    override suspend fun updateSnapshots(updates: List<CartSnapshotUpdate>): EmptyResult<DataError.Local> = write {
        snapshotUpdates += updates
        updates.forEach { update ->
            val item = items.find(update.item.id) ?: return@forEach
            replace(item.copy(productName = update.productName, unitPrice = update.unitPrice))
        }
        Result.Success(Unit)
    }

    override suspend fun removeUnreadable(): Result<Int, DataError.Local> {
        removeUnreadableCalls++
        return removeUnreadableResult
    }

    override suspend fun clear(): EmptyResult<DataError.Local> = write {
        setItems(emptyList())
        Result.Success(Unit)
    }

    private suspend fun <T> write(block: () -> Result<T, DataError.Local>): Result<T, DataError.Local> {
        writeGate?.await()
        return writeError?.let { Result.Failure(it) } ?: block()
    }

    private fun List<CartItem>.find(id: Long) = firstOrNull { it.id == id }

    private fun replace(item: CartItem) = setItems(items.map { if (it.id == item.id) item else it })

    private fun setItems(items: List<CartItem>) {
        cart.value = Cart(items.sortedBy { it.id })
    }
}

internal fun NewCartItem.toCartItem(id: Long, quantity: Int = this.quantity) =
    CartItem(id, productId, productName, size, selections, quantity, unitPrice)
