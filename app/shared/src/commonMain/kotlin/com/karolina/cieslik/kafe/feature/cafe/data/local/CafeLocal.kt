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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Lokal sieci w tabeli `cafes`.
 *
 * @property id identyfikator lokalu z API.
 * @property name nazwa wyświetlana.
 * @property address adres w jednej linii.
 * @property lat szerokość geograficzna (WGS84, stopnie).
 * @property lng długość geograficzna (WGS84, stopnie).
 * @property position kolejność lokali ze źródła, od 0.
 */
@Entity(tableName = "cafes")
data class CafeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val position: Int,
)

/**
 * Godziny otwarcia lokalu w tabeli `cafe_opening_hours` – jeden wiersz na dzień.
 * Trzymane jak w API („HH:mm”) i parsowane przy odczycie tym samym mapperem co DTO.
 *
 * @property cafeId lokal, którego dotyczą godziny.
 * @property day dzień tygodnia wielkimi literami: MONDAY…SUNDAY.
 * @property opens godzina otwarcia „HH:mm” czasu lokalnego.
 * @property closes godzina zamknięcia „HH:mm” czasu lokalnego.
 */
@Entity(tableName = "cafe_opening_hours", primaryKeys = ["cafeId", "day"])
data class OpeningHoursEntity(
    val cafeId: String,
    val day: String,
    val opens: String,
    val closes: String,
)

/**
 * Wynik relacji Room: lokal z godzinami otwarcia.
 *
 * @property cafe wiersz lokalu.
 * @property openingHours godziny otwarcia lokalu, w dowolnej kolejności.
 */
data class CafeWithOpeningHours(
    @Embedded val cafe: CafeEntity,
    @Relation(parentColumn = "id", entityColumn = "cafeId")
    val openingHours: List<OpeningHoursEntity>,
)

/** DAO lokali; klasa abstrakcyjna, żeby zapis wielu tabel mógł być jedną metodą @Transaction. */
@Dao
abstract class CafeDao {

    /**
     * Wyzwalacz zmian: emituje od razu i ponownie po każdym zatwierdzonym zapisie tabel `cafes`,
     * `cafe_opening_hours` lub `sync_metadata`. Wartość nie ma znaczenia – dane czyta [loadCafes]. Lista tabel
     * musi odpowiadać tabelom czytanym przez [loadCafes], także przez relację (`cafe_opening_hours`).
     */
    @Query(
        "SELECT (SELECT COUNT(*) FROM cafes) + (SELECT COUNT(*) FROM cafe_opening_hours) + " +
            "(SELECT COUNT(*) FROM sync_metadata)",
    )
    abstract fun observeCafeTablesChanged(): Flow<Int>

    /**
     * Lokale z godzinami w kolejności ze źródła, odczytane razem ze znacznikiem synchronizacji w jednej
     * transakcji – nigdy w połowie [replaceCafes]. Null, gdy lokale nie były jeszcze nigdy zapisane.
     */
    @Transaction
    open suspend fun loadCafes(): List<CafeWithOpeningHours>? =
        if (getSyncedCount(CAFES_SYNC_KEY) == 0) null else getCafes()

    @Transaction
    @Query("SELECT * FROM cafes ORDER BY position")
    protected abstract suspend fun getCafes(): List<CafeWithOpeningHours>

    @Query("SELECT COUNT(*) FROM sync_metadata WHERE resourceKey = :resourceKey")
    protected abstract suspend fun getSyncedCount(resourceKey: String): Int

    /**
     * Zastępuje wszystkie lokale nowymi i zapisuje znacznik synchronizacji.
     *
     * Jedna transakcja: przerwany zapis jest wycofywany i zostają stare lokale, a obserwatorzy dostają
     * powiadomienie dopiero po zatwierdzeniu całości.
     */
    @Transaction
    open suspend fun replaceCafes(
        cafes: List<CafeEntity>,
        openingHours: List<OpeningHoursEntity>,
        syncedAtEpochMillis: Long,
    ) {
        // Najpierw tabela podrzędna, potem lokale. Kluczy obcych nie ma, ale ta kolejność zadziała też po ich
        // dodaniu. Pełne zastąpienie usuwa lokale, których nie ma już w źródle.
        deleteAllOpeningHours()
        deleteAllCafes()
        insertCafes(cafes)
        insertOpeningHours(openingHours)
        // Znacznik na końcu: observeCafes przestaje zwracać null dopiero, gdy dane są już zapisane.
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

    /** Stałe DAO lokali. */
    companion object {
        /** Klucz znacznika synchronizacji lokali w `sync_metadata`. */
        const val CAFES_SYNC_KEY = "cafes"
    }
}

/** Lokalne źródło lokali (źródło prawdy offline-first). Interfejs pozwala testować repozytorium na JVM. */
interface CafeLocalDataSource {

    /**
     * Zapamiętane lokale; null, gdy nie zostały jeszcze nigdy zapisane, pusta lista – gdy źródło nie miało lokali.
     * Emituje od razu i ponownie po każdym zapisie; kolejne identyczne wartości są pomijane.
     */
    fun observeCafes(): Flow<List<Cafe>?>

    /**
     * Zastępuje zapamiętane lokale [cafes]; [syncedAtEpochMillis] to czas zapisu w ms od epoki (UTC).
     *
     * @return sukces albo [DataError.Local] przy błędzie bazy; wtedy zostają poprzednie lokale.
     */
    suspend fun saveCafes(cafes: List<CafeDto>, syncedAtEpochMillis: Long): EmptyResult<DataError.Local>
}

/** [CafeLocalDataSource] na Room. */
class RoomCafeLocalDataSource(private val dao: CafeDao) : CafeLocalDataSource {

    /** Jeden wyzwalacz i odczyt w transakcji ([CafeDao.loadCafes]) – bez stanu pośredniego po zapisie. */
    override fun observeCafes(): Flow<List<Cafe>?> =
        dao.observeCafeTablesChanged()
            .map { dao.loadCafes()?.map { it.toDomain() } }
            .distinctUntilChanged()

    override suspend fun saveCafes(cafes: List<CafeDto>, syncedAtEpochMillis: Long): EmptyResult<DataError.Local> =
        safeDbCall {
            dao.replaceCafes(
                cafes = cafes.mapIndexed { index, cafe ->
                    CafeEntity(cafe.id, cafe.name, cafe.address, cafe.lat, cafe.lng, position = index)
                },
                // Jeden wpis na dzień (klucz główny cafeId + day); przy duplikatach zostaje pierwszy.
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
