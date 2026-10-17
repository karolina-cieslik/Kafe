package com.karolina.cieslik.kafe.feature.cafe.data.local

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Upsert
import com.karolina.cieslik.kafe.core.data.database.safeDbCall
import com.karolina.cieslik.kafe.core.data.database.sync.SyncMetadataEntity
import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.EmptyResult
import com.karolina.cieslik.kafe.feature.cafe.data.remote.CafeDto
import com.karolina.cieslik.kafe.feature.cafe.data.toDomainOrNull
import com.karolina.cieslik.kafe.feature.cafe.data.remote.OpeningHoursDto
import com.karolina.cieslik.kafe.feature.cafe.domain.Cafe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

// KAFE-6: Encje, DAO i lokalne źródło lokali sieci (Room jako źródło prawdy).
@Entity(tableName = "cafes")
data class CafeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    /** Kolejność lokali ze źródła – pierwszy jest domyślnie wybrany. */
    val position: Int,
)

/** Godziny jak w API („HH:mm”), parsowane przy odczycie tym samym mapperem co DTO. */
@Entity(tableName = "cafe_opening_hours", primaryKeys = ["cafeId", "day"])
data class OpeningHoursEntity(
    val cafeId: String,
    val day: String,
    val opens: String,
    val closes: String,
)

data class CafeWithOpeningHours(
    @Embedded val cafe: CafeEntity,
    @Relation(parentColumn = "id", entityColumn = "cafeId")
    val openingHours: List<OpeningHoursEntity>,
)

@Dao
abstract class CafeDao {

    @Transaction
    @Query("SELECT * FROM cafes ORDER BY position")
    abstract fun observeCafes(): Flow<List<CafeWithOpeningHours>>

    @Query("SELECT COUNT(*) FROM sync_metadata WHERE resourceKey = :resourceKey")
    abstract fun observeSyncedCount(resourceKey: String = CAFES_SYNC_KEY): Flow<Int>

    @Transaction
    open suspend fun replaceCafes(
        cafes: List<CafeEntity>,
        openingHours: List<OpeningHoursEntity>,
        syncedAtEpochMillis: Long,
    ) {
        deleteAllOpeningHours()
        deleteAllCafes()
        insertCafes(cafes)
        insertOpeningHours(openingHours)
        upsertSyncMetadata(SyncMetadataEntity(CAFES_SYNC_KEY, syncedAtEpochMillis))
    }

    @Query("DELETE FROM cafes")
    protected abstract suspend fun deleteAllCafes()

    @Query("DELETE FROM cafe_opening_hours")
    protected abstract suspend fun deleteAllOpeningHours()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertCafes(cafes: List<CafeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertOpeningHours(openingHours: List<OpeningHoursEntity>)

    @Upsert
    protected abstract suspend fun upsertSyncMetadata(metadata: SyncMetadataEntity)

    companion object {
        const val CAFES_SYNC_KEY = "cafes"
    }
}

interface CafeLocalDataSource {

    /** null, gdy lokale nie zostały jeszcze nigdy zapisane. */
    fun observeCafes(): Flow<List<Cafe>?>

    suspend fun saveCafes(cafes: List<CafeDto>, syncedAtEpochMillis: Long): EmptyResult<DataError.Local>
}

class RoomCafeLocalDataSource(private val dao: CafeDao) : CafeLocalDataSource {

    override fun observeCafes(): Flow<List<Cafe>?> =
        combine(dao.observeSyncedCount(), dao.observeCafes()) { syncedCount, cafes ->
            if (syncedCount == 0) null else cafes.map { it.toDomain() }
        }.distinctUntilChanged()

    override suspend fun saveCafes(cafes: List<CafeDto>, syncedAtEpochMillis: Long): EmptyResult<DataError.Local> =
        safeDbCall {
            dao.replaceCafes(
                cafes = cafes.mapIndexed { index, cafe ->
                    CafeEntity(cafe.id, cafe.name, cafe.address, cafe.lat, cafe.lng, position = index)
                },
                openingHours = cafes.flatMap { cafe ->
                    cafe.openingHours
                        .distinctBy { it.day.uppercase() }
                        .map { OpeningHoursEntity(cafe.id, it.day.uppercase(), it.opens, it.closes) }
                },
                syncedAtEpochMillis = syncedAtEpochMillis,
            )
        }
}

internal fun CafeWithOpeningHours.toDomain() = Cafe(
    id = cafe.id,
    name = cafe.name,
    address = cafe.address,
    lat = cafe.lat,
    lng = cafe.lng,
    openingHours = openingHours
        .mapNotNull { OpeningHoursDto(it.day, it.opens, it.closes).toDomainOrNull() }
        .sortedBy { it.day.ordinal },
)
