package com.karolina.cieslik.kafe.feature.cafe.data

import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.EmptyResult
import com.karolina.cieslik.kafe.core.domain.Result
import com.karolina.cieslik.kafe.feature.cafe.data.local.CafeEntity
import com.karolina.cieslik.kafe.feature.cafe.data.local.CafeLocalDataSource
import com.karolina.cieslik.kafe.feature.cafe.data.local.CafeWithOpeningHours
import com.karolina.cieslik.kafe.feature.cafe.data.local.OpeningHoursEntity
import com.karolina.cieslik.kafe.feature.cafe.data.local.toDomain
import com.karolina.cieslik.kafe.feature.cafe.data.remote.CafeDto
import com.karolina.cieslik.kafe.feature.cafe.data.remote.CafeRemoteDataSource
import com.karolina.cieslik.kafe.feature.cafe.data.remote.OpeningHoursDto
import com.karolina.cieslik.kafe.feature.cafe.domain.Cafe
import com.karolina.cieslik.kafe.feature.cafe.domain.OpeningHours
import com.karolina.cieslik.kafe.feature.cafe.domain.Weekday
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// KAFE-6: Testy repozytorium lokali offline-first i mapowania godzin otwarcia.
class OfflineFirstCafeRepositoryTest {

    private class FakeRemote(
        private val cafes: List<CafeDto>,
        var error: DataError.Remote? = null,
    ) : CafeRemoteDataSource {
        var fetchCount = 0
        override suspend fun fetchCafes(): Result<List<CafeDto>, DataError.Remote> {
            fetchCount++
            return error?.let { Result.Failure(it) } ?: Result.Success(cafes)
        }
    }

    private class FakeLocal(initial: List<Cafe>? = null) : CafeLocalDataSource {
        val cafes = MutableStateFlow(initial)
        override fun observeCafes(): Flow<List<Cafe>?> = cafes
        override suspend fun saveCafes(cafes: List<CafeDto>, syncedAtEpochMillis: Long): EmptyResult<DataError.Local> {
            this.cafes.value = cafes.map { Cafe(it.id, it.name, it.address, it.lat, it.lng, emptyList()) }
            return Result.Success(Unit)
        }
    }

    private fun cafeDto(id: String) = CafeDto(id = id, name = "Kafe $id", address = "ul. Testowa 1", lat = 0.0, lng = 0.0)

    private fun cafe(id: String) = Cafe(id, "Zapamiętana $id", "ul. Testowa 1", 0.0, 0.0, emptyList())

    @Test
    fun firstCafeIsSelectedByDefault() = runTest {
        val repository = OfflineFirstCafeRepository(FakeLocal(), FakeRemote(listOf(cafeDto("a"), cafeDto("b"))))

        assertEquals("a", repository.observeSelectedCafe().first()?.id)
    }

    @Test
    fun selectedCafeFollowsSelectionAndFallsBackToFirst() = runTest {
        val repository = OfflineFirstCafeRepository(FakeLocal(), FakeRemote(listOf(cafeDto("a"), cafeDto("b"))))

        repository.selectCafe("b")
        assertEquals("b", repository.observeSelectedCafe().first()?.id)

        repository.selectCafe("missing")
        assertEquals("a", repository.observeSelectedCafe().first()?.id)
    }

    @Test
    fun noCafesMeansNoSelection() = runTest {
        assertNull(OfflineFirstCafeRepository(FakeLocal(), FakeRemote(emptyList())).observeSelectedCafe().first())
    }

    @Test
    fun cachedCafesAreEmittedWhenRefreshFails() = runTest {
        val remote = FakeRemote(emptyList(), error = DataError.Remote.NO_INTERNET)
        val repository = OfflineFirstCafeRepository(FakeLocal(initial = listOf(cafe("a"))), remote)

        assertEquals("Zapamiętana a", repository.observeCafes().first().single().name)
    }

    @Test
    fun backgroundRefreshRunsOncePerProcessAfterSuccess() = runTest {
        val remote = FakeRemote(listOf(cafeDto("a")))
        val repository = OfflineFirstCafeRepository(FakeLocal(), remote)

        repository.observeCafes().first()
        repository.observeSelectedCafe().first()

        assertEquals(1, remote.fetchCount)
    }

    @Test
    fun cafeEntityMapsOpeningHoursSortedAndDropsInvalid() {
        val cafe = CafeWithOpeningHours(
            cafe = CafeEntity("a", "Kafe", "ul. Testowa 1", 1.0, 2.0, position = 0),
            openingHours = listOf(
                OpeningHoursEntity("a", "SUNDAY", "09:00", "18:00"),
                OpeningHoursEntity("a", "MONDAY", "07:30", "20:00"),
                OpeningHoursEntity("a", "TUESDAY", "7", "20:00"),
            ),
        ).toDomain()

        assertEquals(
            listOf(OpeningHours(Weekday.MONDAY, 450, 1200), OpeningHours(Weekday.SUNDAY, 540, 1080)),
            cafe.openingHours,
        )
    }

    @Test
    fun invalidOpeningHoursDtoIsDropped() {
        assertNull(OpeningHoursDto(day = "FUNDAY", opens = "07:00", closes = "20:00").toDomainOrNull())
        assertNull(OpeningHoursDto(day = "MONDAY", opens = "25:00", closes = "20:00").toDomainOrNull())
    }
}
