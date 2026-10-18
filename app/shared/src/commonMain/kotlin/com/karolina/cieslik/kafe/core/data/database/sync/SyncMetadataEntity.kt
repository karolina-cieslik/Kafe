package com.karolina.cieslik.kafe.core.data.database.sync

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Znacznik ostatniej udanej synchronizacji zasobu z API.
 *
 * Obecność wiersza oznacza, że zasób był choć raz zapisany – na tej podstawie źródła lokalne odróżniają
 * „jeszcze nie pobrano” (null) od „pobrano pustą listę”. Sam czas synchronizacji jest na razie tylko zapisywany
 * i nic go nie czyta.
 *
 * @property resourceKey klucz zasobu, np. `menu`, `menu_availability:{cafeId}`, `cafes`.
 * @property lastSyncedAtEpochMillis czas zapisu w milisekundach od epoki Unix (UTC).
 */
@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey val resourceKey: String,
    val lastSyncedAtEpochMillis: Long,
)
