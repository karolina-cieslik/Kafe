package com.karolina.cieslik.kafe.feature.cart.domain

import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.EmptyResult
import com.karolina.cieslik.kafe.core.domain.Result
import com.karolina.cieslik.kafe.feature.product.domain.ProductConfiguration
import kotlinx.coroutines.flow.Flow

/**
 * Trwały koszyk (lokalna baza) – działa offline, jeden dla całej aplikacji, niezależnie od lokalu.
 *
 * Ilość każdej pozycji mieści się w [ProductConfiguration.MIN_QUANTITY]..[ProductConfiguration.MAX_QUANTITY] (1–20).
 * Błędy zapisu i odczytu to [DataError.Local] (np. [DataError.Local.DISK_FULL]).
 */
interface CartRepository {

    /**
     * Obserwuje koszyk. Zimny Flow: emituje od razu stan z bazy ([Cart.EMPTY], gdy pusto), potem po każdej
     * zmianie koszyka. Pozycje w kolejności dodania (od najstarszej).
     */
    fun observeCart(): Flow<Cart>

    /**
     * Dodaje [item] atomowo: gdy w koszyku jest pozycja o tym samym [NewCartItem.configurationKey], zwiększa jej
     * ilość (najwyżej do 20) i nadpisuje migawkę nazwy i ceny; w przeciwnym razie wstawia nową pozycję na koniec.
     *
     * @return [AddToCartOutcome.Added], [AddToCartOutcome.QuantityIncreased] albo [AddToCartOutcome.CappedAtLimit]
     * z pozycją po zapisie; [DataError.Local] przy błędzie bazy.
     */
    suspend fun add(item: NewCartItem): Result<AddToCartOutcome, DataError.Local>

    /**
     * Ustawia ilość pozycji [itemId] na [quantity] przyciętą do 1–20 (usuwanie to [remove]).
     *
     * @return [DataError.Local.NOT_FOUND], gdy pozycji nie ma w koszyku.
     */
    suspend fun setQuantity(itemId: Long, quantity: Int): EmptyResult<DataError.Local>

    /**
     * Usuwa pozycję [itemId].
     *
     * @return usuniętą pozycję (do „Cofnij” przez [restore]); [DataError.Local.NOT_FOUND], gdy jej nie ma.
     */
    suspend fun remove(itemId: Long): Result<CartItem, DataError.Local>

    /**
     * „Cofnij” po [remove]: przywraca [item] z tym samym id i miejscem w kolejności. Gdy w międzyczasie dodano
     * identyczną konfigurację, scala ilości (najwyżej do 20) w jedną pozycję.
     */
    suspend fun restore(item: CartItem): EmptyResult<DataError.Local>

    /** Usuwa wszystkie pozycje. */
    suspend fun clear(): EmptyResult<DataError.Local>
}
