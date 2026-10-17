package com.karolina.cieslik.kafe.core.data.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.karolina.cieslik.kafe.core.data.database.sync.SyncMetadataDao
import com.karolina.cieslik.kafe.core.data.database.sync.SyncMetadataEntity

// KAFE-2: Lokalna baza Room KMP – źródło prawdy w podejściu Offline-First.
@Database(
    entities = [SyncMetadataEntity::class],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(KafeDatabaseConstructor::class)
abstract class KafeDatabase : RoomDatabase() {

    abstract fun syncMetadataDao(): SyncMetadataDao

    companion object {
        const val DATABASE_NAME = "kafe.db"
    }
}

// Implementację (actual) generuje kompilator Room przez KSP dla każdej platformy.
@Suppress("KotlinNoActualForExpect")
expect object KafeDatabaseConstructor : RoomDatabaseConstructor<KafeDatabase> {
    override fun initialize(): KafeDatabase
}
