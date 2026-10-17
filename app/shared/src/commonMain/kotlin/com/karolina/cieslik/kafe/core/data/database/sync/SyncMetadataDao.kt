package com.karolina.cieslik.kafe.core.data.database.sync

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

// KAFE-2
@Dao
interface SyncMetadataDao {

    @Upsert
    suspend fun upsert(metadata: SyncMetadataEntity)

    @Query("SELECT * FROM sync_metadata WHERE resourceKey = :resourceKey")
    suspend fun get(resourceKey: String): SyncMetadataEntity?

    @Query("SELECT * FROM sync_metadata WHERE resourceKey = :resourceKey")
    fun observe(resourceKey: String): Flow<SyncMetadataEntity?>

    @Query("DELETE FROM sync_metadata")
    suspend fun clear()
}
