package com.karolina.cieslik.kafe.core.data.database.sync

import androidx.room.Entity
import androidx.room.PrimaryKey

// KAFE-2: Znacznik ostatniej udanej synchronizacji zasobu z API (np. "menu") – decyduje, czy odświeżyć dane w tle.
@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey val resourceKey: String,
    val lastSyncedAtEpochMillis: Long,
)
