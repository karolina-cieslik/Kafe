package com.karolina.cieslik.kafe.feature.cart.domain

import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.EmptyResult
import com.karolina.cieslik.kafe.core.domain.Result
import com.karolina.cieslik.kafe.feature.product.domain.ProductConfiguration
import kotlinx.coroutines.flow.Flow

/**
 * Trwały koszyk (lokalna baza) – działa offline, jeden dla całej aplikacji, niezależnie od lokalu.
 *
 * Ilość każdej pozycji mieści się w [ProductConfiguration.MIN_QUANTITY]..[ProductConfiguration.MAX_QUANTITY]
 * (1–20).
 * Błędy zapisu i odczytu to [DataError.Local] (np. [DataError.Local.DISK_FULL]).
 */
interface CartRepository {

    /**
     * Obserwuje koszyk. Zimny Flow: emituje od razu stan z bazy ([Cart.EMPTY], gdy pusto), potem po każdej
     * zmianie koszyka. Pozycje w kolejności dodania (id rosnąco).
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
     * Ustawia ilość pozycji [itemId] na [quantity], przyciętą od góry do 20.
     *
     * @throws IllegalArgumentException gdy [quantity] < 1 – do usuwania pozycji służy [remove].
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
     * „+”: zwiększa ilość pozycji [itemId] o 1 atomowo w bazie, najwyżej do 20 – szybkie stuknięcia się nie
     * gubią i nie przekroczą limitu.
     *
     * @return pozycję po zmianie (przy 20 – bez zmian); [DataError.Local.NOT_FOUND], gdy jej nie ma;
     * [DataError.Local.UNKNOWN], gdy wiersza nie da się odczytać.
     */
    suspend fun increase(itemId: Long): Result<CartItem, DataError.Local>

    /**
     * „−”: w jednej transakcji zmniejsza ilość pozycji [itemId] o 1, a przy ilości 1 ją usuwa. O usunięciu
     * decyduje stan bazy, nie ilość widoczna w UI.
     *
     * @return [DecreaseOutcome.Decreased] albo [DecreaseOutcome.Removed] (pozycja do „Cofnij” przez [restore]);
     * [DataError.Local.NOT_FOUND], gdy jej nie ma; [DataError.Local.UNKNOWN], gdy wiersza nie da się odczytać
     * (przy ilości 1 jest już wtedy usunięty).
     */
    suspend fun decreaseOrRemove(itemId: Long): Result<DecreaseOutcome, DataError.Local>

    /**
     * „Cofnij” po [remove] albo [decreaseOrRemove]: wstawia [item] z tym samym id, więc wraca na to samo miejsce
     * w kolejności. Gdy w międzyczasie dodano identyczną konfigurację, zostaje istniejąca pozycja (jej id
     * i miejsce) z ilością równą sumie, przyciętą do 20. Gdy pozycja o tym id już jest w koszyku (np. podwójne
     * „Cofnij”), nic się nie zmienia – ilość się nie podwaja.
     *
     * @return [RestoreOutcome.Restored], [RestoreOutcome.Merged] albo [RestoreOutcome.AlreadyInCart];
     * [DataError.Local] przy błędzie bazy.
     */
    suspend fun restore(item: CartItem): Result<RestoreOutcome, DataError.Local>

    /**
     * Zapisuje nowe migawki nazw i cen ([CartSnapshotUpdate]) w jednej transakcji. Zmienia tylko nazwy i ceny –
     * ilość zmieniona w międzyczasie przez „+/−” zostaje. Pozycja usunięta w międzyczasie jest pomijana.
     *
     * @throws IllegalArgumentException gdy [CartSnapshotUpdate.options] nie odpowiadają dokładnie opcjom pozycji.
     * @return [DataError.Local] przy błędzie bazy (wtedy nic nie jest zapisane).
     */
    suspend fun updateSnapshots(updates: List<CartSnapshotUpdate>): EmptyResult<DataError.Local>

    /**
     * Usuwa wiersze, których nie da się odczytać (pomijane w [observeCart], ale blokujące swój klucz
     * konfiguracji). Gdy nieczytelne są wszystkie wiersze, niczego nie usuwa – to raczej błąd odczytu
     * w aplikacji niż zepsute dane, a usunięcie wyczyściłoby koszyk bez możliwości cofnięcia.
     *
     * @return liczbę usuniętych wierszy; [DataError.Local.UNKNOWN], gdy nieczytelne są wszystkie (i jest ich > 0).
     */
    suspend fun removeUnreadable(): Result<Int, DataError.Local>

    /** Usuwa wszystkie pozycje. */
    suspend fun clear(): EmptyResult<DataError.Local>
}
