package com.karolina.cieslik.kafe.feature.menu.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.karolina.cieslik.kafe.core.data.database.sync.SyncMetadataEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO menu sieci i dostępności w lokalach; klasa abstrakcyjna, żeby zapis wielu tabel mógł być jedną metodą
 * @Transaction. Zapytania `observe*` emitują od razu i ponownie po każdej zmianie swojej tabeli.
 */
@Dao
abstract class MenuDao {

    /** Wszystkie kategorie sieci, bez sortowania (sortuje domena). */
    @Query("SELECT * FROM menu_categories")
    abstract fun observeCategories(): Flow<List<CategoryEntity>>

    /** Wszystkie produkty z rozmiarami; relacja czytana w jednej transakcji. */
    @Transaction
    @Query("SELECT * FROM menu_products")
    abstract fun observeProducts(): Flow<List<ProductWithSizes>>

    /** Wpisy dostępności w lokalu [cafeId]; produkt bez wpisu jest dostępny. */
    @Query("SELECT * FROM menu_product_availability WHERE cafeId = :cafeId")
    abstract fun observeAvailability(cafeId: String): Flow<List<ProductAvailabilityEntity>>

    /** Liczba zsynchronizowanych zasobów z podanych kluczy – menu jest „zapamiętane”, gdy są oba. */
    @Query("SELECT COUNT(*) FROM sync_metadata WHERE resourceKey IN (:resourceKeys)")
    abstract fun observeSyncedCount(resourceKeys: List<String>): Flow<Int>

    /**
     * Zastępuje menu sieci w całości i dostępność w lokalu [cafeId], a na końcu zapisuje oba znaczniki
     * synchronizacji.
     *
     * Jedna transakcja: przerwany zapis jest wycofywany i zostaje poprzednie menu, a obserwatorzy dostają
     * powiadomienie dopiero po zatwierdzeniu całości. Pełne zastąpienie usuwa produkty, których nie ma już
     * w menu. Dostępność innych lokali zostaje bez zmian.
     */
    @Transaction
    open suspend fun replaceMenuForCafe(
        cafeId: String,
        categories: List<CategoryEntity>,
        products: List<ProductEntity>,
        sizes: List<ProductSizeEntity>,
        availability: List<ProductAvailabilityEntity>,
        syncedAtEpochMillis: Long,
    ) {
        // Najpierw tabele podrzędne (rozmiary, produkty), potem kategorie. Kluczy obcych nie ma, ale ta kolejność
        // zadziała też po ich dodaniu.
        deleteAllSizes()
        deleteAllProducts()
        deleteAllCategories()
        insertCategories(categories)
        insertProducts(products)
        insertSizes(sizes)
        deleteAvailability(cafeId)
        insertAvailability(availability)
        // Znaczniki na końcu: observeMenu przestaje zwracać null dopiero, gdy menu i dostępność są zapisane.
        // TODO(KAFE-6): R-1 – odczyt nie jest spójny z zapisem: observeMenu łączy (combine) cztery osobne zapytania,
        // które po transakcji wykonują się ponownie każde osobno, więc może wyemitować stan pośredni (np. nowe kategorie
        // bez produktów albo nowe menu ze starą dostępnością). Poprawka: jeden przepływ-wyzwalacz i odczyt wszystkich
        // tabel w jednej transakcji.
        upsertSyncMetadata(SyncMetadataEntity(MenuSyncKeys.MENU, syncedAtEpochMillis))
        upsertSyncMetadata(SyncMetadataEntity(MenuSyncKeys.availability(cafeId), syncedAtEpochMillis))
    }

    @Query("DELETE FROM menu_categories")
    protected abstract suspend fun deleteAllCategories()

    @Query("DELETE FROM menu_products")
    protected abstract suspend fun deleteAllProducts()

    @Query("DELETE FROM menu_product_sizes")
    protected abstract suspend fun deleteAllSizes()

    @Query("DELETE FROM menu_product_availability WHERE cafeId = :cafeId")
    protected abstract suspend fun deleteAvailability(cafeId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertCategories(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertProducts(products: List<ProductEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertSizes(sizes: List<ProductSizeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertAvailability(availability: List<ProductAvailabilityEntity>)

    @Upsert
    protected abstract suspend fun upsertSyncMetadata(metadata: SyncMetadataEntity)
}

internal object MenuSyncKeys {
    const val MENU = "menu"
    fun availability(cafeId: String) = "menu_availability:$cafeId"
}
