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
import com.karolina.cieslik.kafe.feature.menu.data.local.ModifierGroupEntity
import com.karolina.cieslik.kafe.feature.menu.data.local.ModifierOptionAvailabilityEntity
import com.karolina.cieslik.kafe.feature.menu.data.local.ModifierOptionEntity
import com.karolina.cieslik.kafe.feature.menu.data.local.ProductAvailabilityEntity
import com.karolina.cieslik.kafe.feature.menu.data.local.ProductEntity
import com.karolina.cieslik.kafe.feature.menu.data.local.ProductModifierGroupEntity
import com.karolina.cieslik.kafe.feature.menu.data.local.ProductSizeEntity
import com.karolina.cieslik.kafe.feature.menu.data.local.SizeAvailabilityEntity

// KAFE-2: Lokalna baza Room KMP – źródło prawdy w podejściu Offline-First.
// KAFE-6: v2 – menu, dostępność per lokal i lokale (same nowe tabele, więc AutoMigration).
// KAFE-9 / KAFE-10: v3 – modyfikatory, dostępność rozmiarów i opcji, domyślny rozmiar produktu
// (nowe tabele + kolumna z NULL, więc nadal AutoMigration).
@Database(
    entities = [
        SyncMetadataEntity::class,
        CategoryEntity::class,
        ProductEntity::class,
        ProductSizeEntity::class,
        ProductAvailabilityEntity::class,
        CafeEntity::class,
        OpeningHoursEntity::class,
        ModifierGroupEntity::class,
        ModifierOptionEntity::class,
        ProductModifierGroupEntity::class,
        SizeAvailabilityEntity::class,
        ModifierOptionAvailabilityEntity::class,
    ],
    version = 3,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
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
