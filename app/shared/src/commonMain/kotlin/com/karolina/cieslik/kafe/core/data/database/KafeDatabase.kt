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

/**
 * Lokalna baza Room KMP – źródło prawdy w podejściu offline-first.
 *
 * Wersja 2 dodała menu, dostępność per lokal i lokale; to same nowe tabele, więc wystarcza AutoMigration.
 * Schematy są eksportowane do `app/shared/schemas`.
 */
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

    /** DAO znaczników synchronizacji. */
    abstract fun syncMetadataDao(): SyncMetadataDao

    /** DAO menu sieci i dostępności w lokalach. */
    abstract fun menuDao(): MenuDao

    /** DAO lokali sieci. */
    abstract fun cafeDao(): CafeDao

    /** Stałe bazy. */
    companion object {
        /** Nazwa pliku bazy na urządzeniu. */
        const val DATABASE_NAME = "kafe.db"
    }
}

/**
 * Konstruktor bazy dla Room KMP. Implementację (actual) generuje Room przez KSP dla każdej platformy,
 * dlatego `@Suppress` – IDE nie widzi wygenerowanego kodu i zgłasza brak actual.
 */
@Suppress("KotlinNoActualForExpect")
expect object KafeDatabaseConstructor : RoomDatabaseConstructor<KafeDatabase> {
    /** Tworzy instancję wygenerowanej implementacji [KafeDatabase]. */
    override fun initialize(): KafeDatabase
}
