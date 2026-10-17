package com.karolina.cieslik.kafe.feature.menu.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.karolina.cieslik.kafe.core.data.database.sync.SyncMetadataEntity
import kotlinx.coroutines.flow.Flow

// KAFE-6: DAO menu. Zapis menu sieci, dostępności lokalu i znaczników synchronizacji w jednej transakcji,
// żeby obserwatorzy nie zobaczyli nowego menu ze starą dostępnością.
// KAFE-9 / KAFE-10: także modyfikatory oraz dostępność rozmiarów i opcji.
@Dao
abstract class MenuDao {

    @Query("SELECT * FROM menu_categories")
    abstract fun observeCategories(): Flow<List<CategoryEntity>>

    @Transaction
    @Query("SELECT * FROM menu_products")
    abstract fun observeProducts(): Flow<List<ProductWithSizes>>

    @Transaction
    @Query("SELECT * FROM menu_modifier_groups")
    abstract fun observeModifierGroups(): Flow<List<ModifierGroupWithOptions>>

    @Query("SELECT * FROM menu_product_availability WHERE cafeId = :cafeId")
    abstract fun observeAvailability(cafeId: String): Flow<List<ProductAvailabilityEntity>>

    @Query("SELECT * FROM menu_size_availability WHERE cafeId = :cafeId")
    abstract fun observeSizeAvailability(cafeId: String): Flow<List<SizeAvailabilityEntity>>

    @Query("SELECT * FROM menu_modifier_option_availability WHERE cafeId = :cafeId")
    abstract fun observeOptionAvailability(cafeId: String): Flow<List<ModifierOptionAvailabilityEntity>>

    /** Liczba zsynchronizowanych zasobów z podanych kluczy – menu jest „zapamiętane”, gdy są oba. */
    @Query("SELECT COUNT(*) FROM sync_metadata WHERE resourceKey IN (:resourceKeys)")
    abstract fun observeSyncedCount(resourceKeys: List<String>): Flow<Int>

    @Transaction
    open suspend fun replaceMenuForCafe(cafeId: String, menu: MenuSnapshot, syncedAtEpochMillis: Long) {
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
        deleteAvailability(cafeId)
        deleteSizeAvailability(cafeId)
        deleteOptionAvailability(cafeId)
        insertAvailability(menu.availability.products)
        insertSizeAvailability(menu.availability.sizes)
        insertOptionAvailability(menu.availability.options)
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

/** Komplet wierszy do zapisania w [MenuDao.replaceMenuForCafe]. */
data class MenuSnapshot(
    val categories: List<CategoryEntity>,
    val products: List<ProductEntity>,
    val sizes: List<ProductSizeEntity>,
    val modifierGroups: List<ModifierGroupEntity>,
    val modifierOptions: List<ModifierOptionEntity>,
    val productModifierGroups: List<ProductModifierGroupEntity>,
    val availability: CafeAvailabilityRows,
)

data class CafeAvailabilityRows(
    val products: List<ProductAvailabilityEntity>,
    val sizes: List<SizeAvailabilityEntity>,
    val options: List<ModifierOptionAvailabilityEntity>,
)

internal object MenuSyncKeys {
    const val MENU = "menu"
    fun availability(cafeId: String) = "menu_availability:$cafeId"
}
