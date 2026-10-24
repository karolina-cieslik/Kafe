package com.karolina.cieslik.kafe.feature.cart.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.karolina.cieslik.kafe.feature.product.domain.ProductConfiguration
import kotlinx.coroutines.flow.Flow

/**
 * DAO koszyka. Klasa abstrakcyjna, żeby „znajdź identyczną pozycję, potem zwiększ albo wstaw” było jedną
 * metodą @Transaction – podwójne kliknięcie „Dodaj” nie utworzy dwóch pozycji, a obserwatorzy dostają
 * powiadomienie dopiero po zatwierdzeniu całości.
 */
@Dao
abstract class CartDao {

    /** Wszystkie pozycje w kolejności dodania (id rosnąco); emituje od razu i po każdej zmianie tabeli. */
    @Query("SELECT * FROM cart_items ORDER BY id")
    abstract fun observeItems(): Flow<List<CartItemEntity>>

    /**
     * Dodaje [item] (jego id jest ignorowane). Pozycja o tym samym kluczu konfiguracji dostaje sumę ilości
     * przyciętą do 20 i migawkę nazwy, opcji i ceny z [item] (nowszą); bez takiej pozycji wstawiany jest nowy
     * wiersz na koniec, także z ilością przyciętą do 20.
     *
     * Odczyt i zapis w jednej transakcji: przerwana operacja niczego nie zmienia, a równoległe dodanie tej samej
     * konfiguracji czeka na zatwierdzenie i trafia w gałąź scalenia (unikalny indeks klucza jest drugą barierą).
     */
    @Transaction
    open suspend fun add(item: CartItemEntity): CartWrite {
        val existing = findByKey(item.configurationKey)
        if (existing == null) {
            val row = item.copy(id = 0, quantity = item.quantity.coerceAtMost(MAX_QUANTITY))
            return CartWrite(row.copy(id = insert(row)), CartWriteKind.INSERTED)
        }
        val sum = existing.quantity + item.quantity
        val merged = item.copy(id = existing.id, quantity = sum.coerceAtMost(MAX_QUANTITY))
        update(merged)
        return CartWrite(merged, if (sum > MAX_QUANTITY) CartWriteKind.CAPPED else CartWriteKind.INCREASED)
    }

    /**
     * „Cofnij” usunięcia: wstawia [item] z jego id, więc wraca na swoje miejsce w kolejności. Gdy
     * w międzyczasie dodano tę samą konfigurację, zostaje istniejąca pozycja (id, miejsce i migawka – nowsza)
     * z sumą ilości przyciętą do 20. Ponowne przywrócenie pozycji, która już jest w koszyku, niczego nie zmienia
     * i zwraca [CartWriteKind.INCREASED] z istniejącym wierszem.
     *
     * Jedna transakcja z tych samych powodów co [add].
     */
    @Transaction
    open suspend fun restore(item: CartItemEntity): CartWrite {
        val existing = findByKey(item.configurationKey)
        if (existing == null) {
            insert(item)
            return CartWrite(item, CartWriteKind.INSERTED)
        }
        // Pozycja jest już w koszyku (np. podwójne „Cofnij”) – bez zmian, żeby ilość się nie podwoiła.
        if (existing.id == item.id) return CartWrite(existing, CartWriteKind.INCREASED)
        val sum = existing.quantity + item.quantity
        val merged = existing.copy(quantity = sum.coerceAtMost(MAX_QUANTITY))
        update(merged)
        return CartWrite(merged, if (sum > MAX_QUANTITY) CartWriteKind.CAPPED else CartWriteKind.INCREASED)
    }

    /** Usuwa pozycję [id] i zwraca ją; null, gdy jej nie ma. Odczyt i usunięcie w jednej transakcji. */
    @Transaction
    open suspend fun remove(id: Long): CartItemEntity? {
        val existing = findById(id) ?: return null
        deleteById(id)
        return existing
    }

    /** Ustawia ilość pozycji [id]; zwraca liczbę zmienionych wierszy (0 = brak pozycji). */
    @Query("UPDATE cart_items SET quantity = :quantity WHERE id = :id")
    abstract suspend fun updateQuantity(id: Long, quantity: Int): Int

    /** Usuwa wszystkie pozycje. */
    @Query("DELETE FROM cart_items")
    abstract suspend fun deleteAll()

    @Query("SELECT * FROM cart_items WHERE configurationKey = :configurationKey")
    protected abstract suspend fun findByKey(configurationKey: String): CartItemEntity?

    @Query("SELECT * FROM cart_items WHERE id = :id")
    protected abstract suspend fun findById(id: Long): CartItemEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insert(item: CartItemEntity): Long

    @Update
    protected abstract suspend fun update(item: CartItemEntity)

    @Query("DELETE FROM cart_items WHERE id = :id")
    protected abstract suspend fun deleteById(id: Long)

    private companion object {
        const val MAX_QUANTITY = ProductConfiguration.MAX_QUANTITY
    }
}

/**
 * Wynik zapisu w [CartDao.add] / [CartDao.restore].
 *
 * @property item wiersz po zapisie (z id nadanym przez bazę).
 * @property kind co się stało z pozycją.
 */
data class CartWrite(val item: CartItemEntity, val kind: CartWriteKind)

/** Rodzaj zapisu pozycji koszyka. */
enum class CartWriteKind {
    /** Wstawiono nowy wiersz. */
    INSERTED,

    /** Scalono z istniejącą pozycją, suma ilości mieści się w limicie. */
    INCREASED,

    /** Scalono z istniejącą pozycją, a suma ilości przekroczyła 20 i została przycięta. */
    CAPPED,
}
