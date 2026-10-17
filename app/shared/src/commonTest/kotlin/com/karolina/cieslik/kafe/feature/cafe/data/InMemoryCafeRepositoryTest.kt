package com.karolina.cieslik.kafe.feature.cafe.data

import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.Result
import com.karolina.cieslik.kafe.feature.cafe.data.remote.CafeDto
import com.karolina.cieslik.kafe.feature.cafe.data.remote.CafeRemoteDataSource
import com.karolina.cieslik.kafe.feature.cafe.data.remote.OpeningHoursDto
import com.karolina.cieslik.kafe.feature.cafe.domain.OpeningHours
import com.karolina.cieslik.kafe.feature.cafe.domain.Weekday
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// KAFE-6: Testy repozytorium lokali w pamięci i mapowania godzin otwarcia.
class InMemoryCafeRepositoryTest {

    private class FakeRemote(private val cafes: List<CafeDto>) : CafeRemoteDataSource {
        var fetchCount = 0
        override suspend fun fetchCafes(): Result<List<CafeDto>, DataError.Remote> {
            fetchCount++
            return Result.Success(cafes)
        }
    }

    private fun cafe(id: String) = CafeDto(id = id, name = "Kafe $id", address = "ul. Testowa 1", lat = 0.0, lng = 0.0)

    @Test
    fun firstCafeIsSelectedByDefault() = runTest {
        val repository = InMemoryCafeRepository(FakeRemote(listOf(cafe("a"), cafe("b"))))

        assertEquals("a", repository.observeSelectedCafe().first()?.id)
    }

    @Test
    fun selectedCafeFollowsSelection() = runTest {
        val repository = InMemoryCafeRepository(FakeRemote(listOf(cafe("a"), cafe("b"))))

        repository.selectCafe("b")

        assertEquals("b", repository.observeSelectedCafe().first()?.id)
    }

    @Test
    fun unknownSelectionFallsBackToFirstCafe() = runTest {
        val repository = InMemoryCafeRepository(FakeRemote(listOf(cafe("a"))))

        repository.selectCafe("missing")

        assertEquals("a", repository.observeSelectedCafe().first()?.id)
    }

    @Test
    fun noCafesMeansNoSelection() = runTest {
        assertNull(InMemoryCafeRepository(FakeRemote(emptyList())).observeSelectedCafe().first())
    }

    @Test
    fun cafesAreFetchedOnlyOnceForManyObservers() = runTest {
        val remote = FakeRemote(listOf(cafe("a")))
        val repository = InMemoryCafeRepository(remote)

        repository.observeCafes().first()
        repository.observeSelectedCafe().first()

        assertEquals(1, remote.fetchCount)
    }

    @Test
    fun openingHoursAreParsedToMinutesOfDay() {
        assertEquals(
            OpeningHours(Weekday.MONDAY, opensAtMinute = 450, closesAtMinute = 1200),
            OpeningHoursDto(day = "monday", opens = "07:30", closes = "20:00").toDomainOrNull(),
        )
    }

    @Test
    fun invalidOpeningHoursAreDropped() {
        assertNull(OpeningHoursDto(day = "FUNDAY", opens = "07:00", closes = "20:00").toDomainOrNull())
        assertNull(OpeningHoursDto(day = "MONDAY", opens = "25:00", closes = "20:00").toDomainOrNull())
        assertNull(OpeningHoursDto(day = "MONDAY", opens = "7", closes = "20:00").toDomainOrNull())
    }
}
