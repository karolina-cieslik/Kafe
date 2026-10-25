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
 * powiadomienie dopiero po zatwierdzeniu całości. `@Suppress`, bo Room wymaga, żeby transakcje i zapytania,
 * z których korzystają, były metodami tej samej klasy DAO.
 */
@Suppress("TooManyFunctions")
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
     * i zwraca [CartWriteKind.UNCHANGED] z istniejącym wierszem.
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
        if (existing.id == item.id) return CartWrite(existing, CartWriteKind.UNCHANGED)
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

    /**
     * „+”: zwiększa ilość pozycji [id] o 1, najwyżej do 20, i zwraca wiersz po zmianie; null, gdy pozycji nie ma.
     * Zwiększenie liczy SQLite na bieżącej wartości (bez odczytu przed zapisem), więc równoległe „+” się nie
     * gubią; odczyt w tej samej transakcji zwraca ilość po tym właśnie zapisie.
     */
    @Transaction
    open suspend fun increase(id: Long): CartItemEntity? {
        if (incrementQuantity(id, MAX_QUANTITY) == 0) return null
        return findById(id)
    }

    /**
     * „−”: zmniejsza ilość pozycji [id] o 1, a przy ilości 1 usuwa wiersz; null, gdy pozycji nie ma. Odczyt
     * i zapis w jednej transakcji – dwa równoległe „−” przy ilości 2 dają jedno zmniejszenie i jedno usunięcie.
     */
    @Transaction
    open suspend fun decreaseOrRemove(id: Long): CartDecrease? {
        val existing = findById(id) ?: return null
        if (existing.quantity <= 1) {
            deleteById(id)
            return CartDecrease(existing, removed = true)
        }
        val decreased = existing.copy(quantity = existing.quantity - 1)
        updateQuantity(id, decreased.quantity)
        return CartDecrease(decreased, removed = false)
    }

    /**
     * Zapisuje nowe migawki [snapshots] w jednej transakcji – obserwatorzy widzą wszystkie albo żadną. Zmienia
     * tylko nazwę, opcje i cenę, a nie cały wiersz, żeby nie nadpisać ilości zmienionej w międzyczasie przez „+/−”.
     * Warunek na klucz konfiguracji pomija pozycję usuniętą w międzyczasie (id nie wraca dla innego klucza).
     */
    @Transaction
    open suspend fun updateSnapshots(snapshots: List<CartSnapshotRow>) {
        snapshots.forEach {
            updateSnapshot(it.id, it.configurationKey, it.productName, it.selectionsJson, it.unitPriceGrosze)
        }
    }

    /**
     * Usuwa wiersze, dla których [isReadable] zwraca false, i zwraca ich liczbę. Gdy nieczytelne są wszystkie
     * wiersze, niczego nie usuwa i zwraca null – to raczej błąd mappera niż danych. Odczyt i usunięcia w jednej
     * transakcji, żeby nie usunąć wiersza, który [add] w międzyczasie nadpisał czytelną migawką.
     */
    @Transaction
    open suspend fun deleteUnreadable(isReadable: (CartItemEntity) -> Boolean): Int? {
        val rows = getAll()
        val unreadable = rows.filterNot(isReadable)
        if (unreadable.isNotEmpty() && unreadable.size == rows.size) return null
        unreadable.forEach { deleteById(it.id) }
        return unreadable.size
    }

    /** Ustawia ilość pozycji [id]; zwraca liczbę zmienionych wierszy (0 = brak pozycji). */
    @Query("UPDATE cart_items SET quantity = :quantity WHERE id = :id")
    abstract suspend fun updateQuantity(id: Long, quantity: Int): Int

    /** Usuwa wszystkie pozycje. */
    @Query("DELETE FROM cart_items")
    abstract suspend fun deleteAll()

    @Query("UPDATE cart_items SET quantity = MIN(quantity + 1, :max) WHERE id = :id")
    protected abstract suspend fun incrementQuantity(id: Long, max: Int): Int

    @Query(
        "UPDATE cart_items SET productName = :productName, selectionsJson = :selectionsJson, " +
            "unitPriceGrosze = :unitPriceGrosze WHERE id = :id AND configurationKey = :configurationKey",
    )
    protected abstract suspend fun updateSnapshot(
        id: Long,
        configurationKey: String,
        productName: String,
        selectionsJson: String,
        unitPriceGrosze: Long,
    )

    @Query("SELECT * FROM cart_items")
    protected abstract suspend fun getAll(): List<CartItemEntity>

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
 * Wynik [CartDao.decreaseOrRemove].
 *
 * @property item wiersz po zmniejszeniu albo – gdy [removed] – usunięty wiersz (do „Cofnij”).
 * @property removed pozycja miała 1 sztukę i została usunięta.
 */
data class CartDecrease(val item: CartItemEntity, val removed: Boolean)

/**
 * Nowa migawka pozycji dla [CartDao.updateSnapshots].
 *
 * @property id id pozycji.
 * @property configurationKey klucz konfiguracji pozycji – zapis tylko, gdy wiersz o [id] nadal ma ten klucz.
 * @property productName nazwa produktu.
 * @property selectionsJson migawka opcji – ten sam zbiór opcji co w wierszu, nowe nazwy i dopłaty.
 * @property unitPriceGrosze cena jednej sztuki z dopłatami w groszach.
 */
data class CartSnapshotRow(
    val id: Long,
    val configurationKey: String,
    val productName: String,
    val selectionsJson: String,
    val unitPriceGrosze: Long,
)

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

    /** Pozycja o tym id już była w koszyku – nic nie zmieniono (tylko [CartDao.restore]). */
    UNCHANGED,
}
