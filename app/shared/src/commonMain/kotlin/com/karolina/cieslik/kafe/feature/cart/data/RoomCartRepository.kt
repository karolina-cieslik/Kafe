package com.karolina.cieslik.kafe.feature.cart.data

import com.karolina.cieslik.kafe.core.data.database.safeDbCall
import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.EmptyResult
import com.karolina.cieslik.kafe.core.domain.Result
import com.karolina.cieslik.kafe.core.domain.map
import com.karolina.cieslik.kafe.feature.cart.data.local.CartDao
import com.karolina.cieslik.kafe.feature.cart.data.local.CartItemEntity
import com.karolina.cieslik.kafe.feature.cart.data.local.CartSnapshotRow
import com.karolina.cieslik.kafe.feature.cart.data.local.CartWrite
import com.karolina.cieslik.kafe.feature.cart.data.local.CartWriteKind
import com.karolina.cieslik.kafe.feature.cart.data.local.toDomainOrNull
import com.karolina.cieslik.kafe.feature.cart.data.local.toEntity
import com.karolina.cieslik.kafe.feature.cart.data.local.toJson
import com.karolina.cieslik.kafe.feature.cart.domain.AddToCartOutcome
import com.karolina.cieslik.kafe.feature.cart.domain.Cart
import com.karolina.cieslik.kafe.feature.cart.domain.CartItem
import com.karolina.cieslik.kafe.feature.cart.domain.CartRepository
import com.karolina.cieslik.kafe.feature.cart.domain.CartSnapshotUpdate
import com.karolina.cieslik.kafe.feature.cart.domain.DecreaseOutcome
import com.karolina.cieslik.kafe.feature.cart.domain.NewCartItem
import com.karolina.cieslik.kafe.feature.cart.domain.OptionRef
import com.karolina.cieslik.kafe.feature.cart.domain.RestoreOutcome
import com.karolina.cieslik.kafe.feature.product.domain.ProductConfiguration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * [CartRepository] na Room – koszyk istnieje tylko lokalnie, więc bez źródła zdalnego. Room jest main-safe
 * (zapytania na dispatcherze bazy), dlatego repozytorium nie przełącza wątków.
 *
 * Wiersze, których nie da się odczytać (nieznany rozmiar, błędny JSON opcji), są pomijane w [observeCart].
 */
class RoomCartRepository(private val dao: CartDao) : CartRepository {

    override fun observeCart(): Flow<Cart> =
        dao.observeItems()
            .map { rows -> Cart(rows.mapNotNull { it.toDomainOrNull() }) }
            .distinctUntilChanged()

    override suspend fun add(item: NewCartItem): Result<AddToCartOutcome, DataError.Local> =
        safeDbCall { dao.add(item.toEntity()) }.toOutcome()

    override suspend fun setQuantity(itemId: Long, quantity: Int): EmptyResult<DataError.Local> {
        require(quantity >= ProductConfiguration.MIN_QUANTITY) {
            "Ilość $quantity < 1 – do usuwania pozycji służy remove"
        }
        val capped = quantity.coerceAtMost(ProductConfiguration.MAX_QUANTITY)
        return when (val updated = safeDbCall { dao.updateQuantity(itemId, capped) }) {
            is Result.Failure -> updated
            is Result.Success ->
                if (updated.data == 0) Result.Failure(DataError.Local.NOT_FOUND) else Result.Success(Unit)
        }
    }

    override suspend fun remove(itemId: Long): Result<CartItem, DataError.Local> =
        when (val removed = safeDbCall { dao.remove(itemId) }) {
            is Result.Failure -> removed
            is Result.Success -> when (val row = removed.data) {
                null -> Result.Failure(DataError.Local.NOT_FOUND)
                // Wiersz nieczytelny (pomijany w koszyku) został usunięty, ale nie ma czego przywracać.
                else -> row.toDomainOrNull()?.let { Result.Success(it) } ?: Result.Failure(DataError.Local.UNKNOWN)
            }
        }

    override suspend fun increase(itemId: Long): Result<CartItem, DataError.Local> =
        when (val increased = safeDbCall { dao.increase(itemId) }) {
            is Result.Failure -> increased
            is Result.Success -> increased.data.toFoundItem()
        }

    override suspend fun decreaseOrRemove(itemId: Long): Result<DecreaseOutcome, DataError.Local> {
        val write = when (val decreased = safeDbCall { dao.decreaseOrRemove(itemId) }) {
            is Result.Failure -> return decreased
            is Result.Success -> decreased.data
        }
        return write?.item.toFoundItem().map { item ->
            if (write?.removed == true) DecreaseOutcome.Removed(item) else DecreaseOutcome.Decreased(item)
        }
    }

    override suspend fun restore(item: CartItem): Result<RestoreOutcome, DataError.Local> =
        when (val restored = safeDbCall { dao.restore(item.toEntity()) }) {
            is Result.Failure -> restored
            is Result.Success -> {
                val row = restored.data.item.toDomainOrNull() ?: return Result.Failure(DataError.Local.UNKNOWN)
                Result.Success(
                    when (restored.data.kind) {
                        CartWriteKind.INSERTED -> RestoreOutcome.Restored(row)
                        CartWriteKind.INCREASED -> RestoreOutcome.Merged(row, capped = false)
                        CartWriteKind.CAPPED -> RestoreOutcome.Merged(row, capped = true)
                        CartWriteKind.UNCHANGED -> RestoreOutcome.AlreadyInCart(row)
                    },
                )
            }
        }

    override suspend fun updateSnapshots(updates: List<CartSnapshotUpdate>): EmptyResult<DataError.Local> {
        val rows = updates.map { it.toRow() }
        return safeDbCall { dao.updateSnapshots(rows) }
    }

    override suspend fun removeUnreadable(): Result<Int, DataError.Local> =
        when (val removed = safeDbCall { dao.deleteUnreadable { it.toDomainOrNull() != null } }) {
            is Result.Failure -> removed
            is Result.Success -> removed.data?.let { Result.Success(it) } ?: Result.Failure(DataError.Local.UNKNOWN)
        }

    override suspend fun clear(): EmptyResult<DataError.Local> = safeDbCall { dao.deleteAll() }
}

private fun Result<CartWrite, DataError.Local>.toOutcome(): Result<AddToCartOutcome, DataError.Local> =
    when (this) {
        is Result.Failure -> this
        is Result.Success -> {
            val item = data.item.toDomainOrNull() ?: return Result.Failure(DataError.Local.UNKNOWN)
            Result.Success(
                when (data.kind) {
                    CartWriteKind.INSERTED -> AddToCartOutcome.Added(item)
                    CartWriteKind.INCREASED -> AddToCartOutcome.QuantityIncreased(item)
                    CartWriteKind.CAPPED -> AddToCartOutcome.CappedAtLimit(item)
                    CartWriteKind.UNCHANGED -> error("add nie zwraca UNCHANGED")
                },
            )
        }
    }

/** NOT_FOUND dla null; UNKNOWN dla wiersza nieczytelnego (pomijanego w koszyku). */
private fun CartItemEntity?.toFoundItem(): Result<CartItem, DataError.Local> = when (this) {
    null -> Result.Failure(DataError.Local.NOT_FOUND)
    else -> toDomainOrNull()?.let { Result.Success(it) } ?: Result.Failure(DataError.Local.UNKNOWN)
}

/**
 * Wiersz z nową migawką: ten sam zbiór opcji i ilości (więc klucz bez zmian), nowe nazwy, dopłaty i flagi
 * opcji domyślnej.
 *
 * @throws IllegalArgumentException gdy opcje aktualizacji nie są dokładnie opcjami pozycji.
 */
private fun CartSnapshotUpdate.toRow(): CartSnapshotRow {
    val refs = item.selections.map { OptionRef(it.groupId, it.optionId) }
    require(refs.toSet() == options.keys) {
        "Migawka pozycji ${item.id} ma opcje ${options.keys}, a pozycja $refs"
    }
    val selections = item.selections.map { selection ->
        val snapshot = options.getValue(OptionRef(selection.groupId, selection.optionId))
        selection.copy(
            optionName = snapshot.optionName,
            surcharge = snapshot.surcharge,
            isDefault = snapshot.isDefault,
        )
    }
    return CartSnapshotRow(item.id, item.configurationKey, productName, selections.toJson(), unitPrice.grosze)
}
