package com.karolina.cieslik.kafe.core.data.database.sync

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** DAO znaczników synchronizacji; zapisy menu i lokali upsertują znaczniki we własnych transakcjach. */
@Dao
interface SyncMetadataDao {

    /** Wstawia albo nadpisuje znacznik zasobu. */
    @Upsert
    suspend fun upsert(metadata: SyncMetadataEntity)

    /** Znacznik zasobu [resourceKey]; null, gdy zasób nie był jeszcze synchronizowany. */
    @Query("SELECT * FROM sync_metadata WHERE resourceKey = :resourceKey")
    suspend fun get(resourceKey: String): SyncMetadataEntity?

    /** Jak [get], ale emituje od razu i ponownie po każdej zmianie tabeli `sync_metadata`. */
    @Query("SELECT * FROM sync_metadata WHERE resourceKey = :resourceKey")
    fun observe(resourceKey: String): Flow<SyncMetadataEntity?>

    /** Usuwa wszystkie znaczniki – zapamiętane menu i lokale będą traktowane jak niepobrane. */
    @Query("DELETE FROM sync_metadata")
    suspend fun clear()
}
