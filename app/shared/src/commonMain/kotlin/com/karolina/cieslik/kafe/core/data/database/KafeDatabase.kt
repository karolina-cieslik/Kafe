package com.karolina.cieslik.kafe.core.data.database

import androidx.room.AutoMigration
import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.karolina.cieslik.kafe.core.data.database.sync.SyncMetadataDao
import com.karolina.cieslik.kafe.core.data.database.sync.SyncMetadataEntity
import com.karolina.cieslik.kafe.feature.cafe.data.local.CafeDao
import com.karolina.cieslik.kafe.feature.cafe.data.local.CafeEntity
import com.karolina.cieslik.kafe.feature.cafe.data.local.OpeningHoursEntity
import com.karolina.cieslik.kafe.feature.menu.data.local.CategoryEntity
import com.karolina.cieslik.kafe.feature.menu.data.local.MenuDao
import com.karolina.cieslik.kafe.feature.menu.data.local.ProductAvailabilityEntity
import com.karolina.cieslik.kafe.feature.menu.data.local.ProductEntity
import com.karolina.cieslik.kafe.feature.menu.data.local.ProductSizeEntity

// KAFE-2: Lokalna baza Room KMP – źródło prawdy w podejściu Offline-First.
// KAFE-6: v2 – menu, dostępność per lokal i lokale (same nowe tabele, więc AutoMigration).
@Database(
    entities = [
        SyncMetadataEntity::class,
        CategoryEntity::class,
        ProductEntity::class,
        ProductSizeEntity::class,
        ProductAvailabilityEntity::class,
        CafeEntity::class,
        OpeningHoursEntity::class,
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
@ConstructedBy(KafeDatabaseConstructor::class)
abstract class KafeDatabase : RoomDatabase() {

    abstract fun syncMetadataDao(): SyncMetadataDao

    abstract fun menuDao(): MenuDao

    abstract fun cafeDao(): CafeDao

    companion object {
        const val DATABASE_NAME = "kafe.db"
    }
}

// Implementację (actual) generuje kompilator Room przez KSP dla każdej platformy.
@Suppress("KotlinNoActualForExpect")
expect object KafeDatabaseConstructor : RoomDatabaseConstructor<KafeDatabase> {
    override fun initialize(): KafeDatabase
}
