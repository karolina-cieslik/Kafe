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
@Dao
abstract class MenuDao {

    @Query("SELECT * FROM menu_categories")
    abstract fun observeCategories(): Flow<List<CategoryEntity>>

    @Transaction
    @Query("SELECT * FROM menu_products")
    abstract fun observeProducts(): Flow<List<ProductWithSizes>>

    @Query("SELECT * FROM menu_product_availability WHERE cafeId = :cafeId")
    abstract fun observeAvailability(cafeId: String): Flow<List<ProductAvailabilityEntity>>

    /** Liczba zsynchronizowanych zasobów z podanych kluczy – menu jest „zapamiętane”, gdy są oba. */
    @Query("SELECT COUNT(*) FROM sync_metadata WHERE resourceKey IN (:resourceKeys)")
    abstract fun observeSyncedCount(resourceKeys: List<String>): Flow<Int>

    @Transaction
    open suspend fun replaceMenuForCafe(
        cafeId: String,
        categories: List<CategoryEntity>,
        products: List<ProductEntity>,
        sizes: List<ProductSizeEntity>,
        availability: List<ProductAvailabilityEntity>,
        syncedAtEpochMillis: Long,
    ) {
        deleteAllSizes()
        deleteAllProducts()
        deleteAllCategories()
        insertCategories(categories)
        insertProducts(products)
        insertSizes(sizes)
        deleteAvailability(cafeId)
        insertAvailability(availability)
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
