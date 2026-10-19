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

/**
 * Lokalna baza Room KMP – źródło prawdy w podejściu offline-first. Schematy są eksportowane do
 * `app/shared/schemas`, a migracje generuje Room (AutoMigration) na ich podstawie.
 *
 * - Wersja 2 dodała menu, dostępność per lokal i lokale – same nowe tabele.
 * - Wersja 3 dodała modyfikatory: tabele `menu_modifier_groups`, `menu_modifier_options`,
 *   `menu_product_modifier_groups`, dostępność rozmiarów (`menu_size_availability`) i opcji
 *   (`menu_modifier_option_availability`) oraz kolumnę `menu_products.defaultSizeCode` (TEXT, NULL).
 *
 * Migracja 2→3 jest bezpieczna dla danych: tylko tworzy tabele i dodaje kolumnę dopuszczającą NULL,
 * niczego nie usuwa ani nie przepisuje. Zapamiętane menu zostaje, ale bez modyfikatorów i domyślnego
 * rozmiaru – uzupełnią się przy najbliższym odświeżeniu menu.
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

    /** DAO znaczników synchronizacji. */
    abstract fun syncMetadataDao(): SyncMetadataDao

    /** DAO menu sieci, modyfikatorów i dostępności w lokalach. */
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
