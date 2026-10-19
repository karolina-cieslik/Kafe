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
 * DAO menu sieci (kategorie, produkty, rozmiary, modyfikatory) i dostępności w lokalach (produkty, rozmiary,
 * opcje). Klasa abstrakcyjna, żeby zapis wielu tabel mógł być jedną metodą @Transaction.
 * Zapytania `observe*` emitują od razu i ponownie po każdej zmianie swoich tabel.
 */
@Dao
abstract class MenuDao {

    /** Wszystkie kategorie sieci, bez sortowania (sortuje domena). */
    @Query("SELECT * FROM menu_categories")
    abstract fun observeCategories(): Flow<List<CategoryEntity>>

    /** Wszystkie produkty z rozmiarami i przypisanymi grupami; relacje czytane w jednej transakcji. */
    @Transaction
    @Query("SELECT * FROM menu_products")
    abstract fun observeProducts(): Flow<List<ProductWithSizes>>

    /** Wszystkie grupy modyfikatorów z opcjami; relacja czytana w jednej transakcji. */
    @Transaction
    @Query("SELECT * FROM menu_modifier_groups")
    abstract fun observeModifierGroups(): Flow<List<ModifierGroupWithOptions>>

    /** Wpisy dostępności produktów w lokalu [cafeId]; produkt bez wpisu jest dostępny. */
    @Query("SELECT * FROM menu_product_availability WHERE cafeId = :cafeId")
    abstract fun observeAvailability(cafeId: String): Flow<List<ProductAvailabilityEntity>>

    /** Wpisy dostępności rozmiarów w lokalu [cafeId]; rozmiar bez wpisu jest dostępny. */
    @Query("SELECT * FROM menu_size_availability WHERE cafeId = :cafeId")
    abstract fun observeSizeAvailability(cafeId: String): Flow<List<SizeAvailabilityEntity>>

    /** Wpisy dostępności opcji modyfikatorów w lokalu [cafeId]; opcja bez wpisu jest dostępna. */
    @Query("SELECT * FROM menu_modifier_option_availability WHERE cafeId = :cafeId")
    abstract fun observeOptionAvailability(cafeId: String): Flow<List<ModifierOptionAvailabilityEntity>>

    /** Liczba zsynchronizowanych zasobów z podanych kluczy – menu jest „zapamiętane”, gdy są oba. */
    @Query("SELECT COUNT(*) FROM sync_metadata WHERE resourceKey IN (:resourceKeys)")
    abstract fun observeSyncedCount(resourceKeys: List<String>): Flow<Int>

    /**
     * Zastępuje menu sieci w całości i dostępność w lokalu [cafeId] wierszami z [menu], a na końcu zapisuje oba
     * znaczniki synchronizacji z czasem [syncedAtEpochMillis] (ms od epoki, UTC).
     *
     * Jedna transakcja: przerwany zapis jest wycofywany i zostaje poprzednie menu, a obserwatorzy dostają
     * powiadomienie dopiero po zatwierdzeniu całości. Pełne zastąpienie usuwa produkty, grupy i opcje, których
     * nie ma już w menu. Dostępność innych lokali zostaje bez zmian.
     */
    @Transaction
    open suspend fun replaceMenuForCafe(cafeId: String, menu: MenuSnapshot, syncedAtEpochMillis: Long) {
        // 1. Menu sieci: najpierw tabele podrzędne (przypisania grup, opcje, rozmiary), potem nadrzędne. Kluczy
        // obcych nie ma, ale ta kolejność zadziała też po ich dodaniu. Wstawianie w odwrotnej kolejności.
        deleteAllProductModifierGroups()
        deleteAllModifierOptions()
        deleteAllModifierGroups()
        deleteAllSizes()
        deleteAllProducts()
        deleteAllCategories()
        insertCategories(menu.categories)
        insertProducts(menu.products)
        insertSizes(menu.sizes)
        insertModifierGroups(menu.modifierGroups)
        insertModifierOptions(menu.modifierOptions)
        insertProductModifierGroups(menu.productModifierGroups)
        // 2. Dostępność tylko tego lokalu – usunięcie i wstawienie od nowa, żeby zniknęły nieaktualne braki.
        deleteAvailability(cafeId)
        deleteSizeAvailability(cafeId)
        deleteOptionAvailability(cafeId)
        insertAvailability(menu.availability.products)
        insertSizeAvailability(menu.availability.sizes)
        insertOptionAvailability(menu.availability.options)
        // 3. Znaczniki na końcu: observeMenu przestaje zwracać null dopiero, gdy menu i dostępność są zapisane.
        // TODO(KAFE-6): R-1 – zapis jest atomowy, ale observeMenu łączy siedem osobnych zapytań, które po
        // transakcji wykonują się ponownie każde osobno, więc może wyemitować stan pośredni (np. nowe produkty
        // bez grup albo nowe menu ze starą dostępnością). Poprawka: jeden wyzwalacz i odczyt w jednej transakcji.
        upsertSyncMetadata(SyncMetadataEntity(MenuSyncKeys.MENU, syncedAtEpochMillis))
        upsertSyncMetadata(SyncMetadataEntity(MenuSyncKeys.availability(cafeId), syncedAtEpochMillis))
    }

    @Query("DELETE FROM menu_categories")
    protected abstract suspend fun deleteAllCategories()

    @Query("DELETE FROM menu_products")
    protected abstract suspend fun deleteAllProducts()

    @Query("DELETE FROM menu_product_sizes")
    protected abstract suspend fun deleteAllSizes()

    @Query("DELETE FROM menu_modifier_groups")
    protected abstract suspend fun deleteAllModifierGroups()

    @Query("DELETE FROM menu_modifier_options")
    protected abstract suspend fun deleteAllModifierOptions()

    @Query("DELETE FROM menu_product_modifier_groups")
    protected abstract suspend fun deleteAllProductModifierGroups()

    @Query("DELETE FROM menu_product_availability WHERE cafeId = :cafeId")
    protected abstract suspend fun deleteAvailability(cafeId: String)

    @Query("DELETE FROM menu_size_availability WHERE cafeId = :cafeId")
    protected abstract suspend fun deleteSizeAvailability(cafeId: String)

    @Query("DELETE FROM menu_modifier_option_availability WHERE cafeId = :cafeId")
    protected abstract suspend fun deleteOptionAvailability(cafeId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertCategories(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertProducts(products: List<ProductEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertSizes(sizes: List<ProductSizeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertModifierGroups(groups: List<ModifierGroupEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertModifierOptions(options: List<ModifierOptionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertProductModifierGroups(links: List<ProductModifierGroupEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertAvailability(availability: List<ProductAvailabilityEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertSizeAvailability(availability: List<SizeAvailabilityEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertOptionAvailability(availability: List<ModifierOptionAvailabilityEntity>)

    @Upsert
    protected abstract suspend fun upsertSyncMetadata(metadata: SyncMetadataEntity)
}

/**
 * Komplet wierszy do zapisania w [MenuDao.replaceMenuForCafe].
 *
 * @property categories kategorie sieci.
 * @property products produkty sieci.
 * @property sizes rozmiary wszystkich produktów.
 * @property modifierGroups grupy modyfikatorów sieci.
 * @property modifierOptions opcje wszystkich grup.
 * @property productModifierGroups przypisania grup do produktów z kolejnością.
 * @property availability braki w lokalu, którego dotyczy zapis.
 */
data class MenuSnapshot(
    val categories: List<CategoryEntity>,
    val products: List<ProductEntity>,
    val sizes: List<ProductSizeEntity>,
    val modifierGroups: List<ModifierGroupEntity>,
    val modifierOptions: List<ModifierOptionEntity>,
    val productModifierGroups: List<ProductModifierGroupEntity>,
    val availability: CafeAvailabilityRows,
)

/**
 * Wiersze dostępności jednego lokalu.
 *
 * @property products dostępność produktów.
 * @property sizes dostępność rozmiarów produktów.
 * @property options dostępność opcji modyfikatorów.
 */
data class CafeAvailabilityRows(
    val products: List<ProductAvailabilityEntity>,
    val sizes: List<SizeAvailabilityEntity>,
    val options: List<ModifierOptionAvailabilityEntity>,
)

internal object MenuSyncKeys {
    const val MENU = "menu"
    fun availability(cafeId: String) = "menu_availability:$cafeId"
}
