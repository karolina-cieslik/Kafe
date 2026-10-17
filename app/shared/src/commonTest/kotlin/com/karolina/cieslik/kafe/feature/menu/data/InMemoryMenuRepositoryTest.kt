package com.karolina.cieslik.kafe.feature.menu.data

import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.Result
import com.karolina.cieslik.kafe.feature.menu.data.remote.CategoryDto
import com.karolina.cieslik.kafe.feature.menu.data.remote.MenuDto
import com.karolina.cieslik.kafe.feature.menu.data.remote.MenuRemoteDataSource
import com.karolina.cieslik.kafe.feature.menu.data.remote.SeedMenuRemoteDataSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration

// KAFE-6: Testy tymczasowego repozytorium menu w pamięci.
class InMemoryMenuRepositoryTest {

    private class FakeRemote(
        var menu: Result<MenuDto, DataError.Remote> = Result.Success(MenuDto(categories = listOf(CategoryDto("c", "Kawy")))),
        var availability: Result<Map<String, Boolean>, DataError.Remote> = Result.Success(mapOf("p" to false)),
    ) : MenuRemoteDataSource {
        override suspend fun fetchMenu() = menu
        override suspend fun fetchAvailability(cafeId: String) = availability
    }

    @Test
    fun menuIsNullBeforeFirstRefresh() = runTest {
        val repository = InMemoryMenuRepository(FakeRemote())

        assertNull(repository.observeMenu("cafe").first())
    }

    @Test
    fun successfulRefreshStoresMenuWithCafeAvailability() = runTest {
        val repository = InMemoryMenuRepository(FakeRemote())

        assertEquals(Result.Success(Unit), repository.refresh("cafe"))

        val menu = assertNotNull(repository.observeMenu("cafe").first())
        assertEquals(listOf("c"), menu.categories.map { it.id })
        assertEquals(setOf("p"), menu.unavailableProductIds)
    }

    @Test
    fun menuIsNotAvailableForCafeWhoseAvailabilityWasNotFetched() = runTest {
        val repository = InMemoryMenuRepository(FakeRemote())

        repository.refresh("cafe")

        assertNull(repository.observeMenu("other-cafe").first())
    }

    @Test
    fun failedAvailabilityFetchReturnsErrorAndStoresNothing() = runTest {
        val remote = FakeRemote(availability = Result.Failure(DataError.Remote.NO_INTERNET))
        val repository = InMemoryMenuRepository(remote)

        assertEquals(Result.Failure(DataError.Remote.NO_INTERNET), repository.refresh("cafe"))
        assertNull(repository.observeMenu("cafe").first())
    }

    @Test
    fun failedRefreshKeepsPreviouslyCachedMenu() = runTest {
        val remote = FakeRemote()
        val repository = InMemoryMenuRepository(remote)
        repository.refresh("cafe")

        remote.menu = Result.Failure(DataError.Remote.SERVER)
        repository.refresh("cafe")

        assertNotNull(repository.observeMenu("cafe").first())
    }

    @Test
    fun seedDataSourceRejectsUnknownCafe() = runTest {
        val seed = SeedMenuRemoteDataSource(latency = Duration.ZERO)

        assertEquals(Result.Failure(DataError.Remote.NOT_FOUND), seed.fetchAvailability("unknown"))
    }
}
