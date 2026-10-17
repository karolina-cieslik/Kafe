package com.karolina.cieslik.kafe.feature.menu.data

import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.EmptyResult
import com.karolina.cieslik.kafe.core.domain.Money
import com.karolina.cieslik.kafe.core.domain.Result
import com.karolina.cieslik.kafe.feature.menu.data.local.MenuLocalDataSource
import com.karolina.cieslik.kafe.feature.menu.data.local.ProductEntity
import com.karolina.cieslik.kafe.feature.menu.data.local.ProductSizeEntity
import com.karolina.cieslik.kafe.feature.menu.data.local.ProductWithSizes
import com.karolina.cieslik.kafe.feature.menu.data.local.toDomain
import com.karolina.cieslik.kafe.feature.menu.data.local.toDomainOrNull
import com.karolina.cieslik.kafe.feature.menu.data.remote.CategoryDto
import com.karolina.cieslik.kafe.feature.menu.data.remote.MenuDto
import com.karolina.cieslik.kafe.feature.menu.data.remote.MenuRemoteDataSource
import com.karolina.cieslik.kafe.feature.menu.data.remote.SeedMenuRemoteDataSource
import com.karolina.cieslik.kafe.feature.menu.domain.Category
import com.karolina.cieslik.kafe.feature.menu.domain.Menu
import com.karolina.cieslik.kafe.feature.menu.domain.ProductSize
import com.karolina.cieslik.kafe.feature.menu.domain.SizeCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

// KAFE-6: Testy repozytorium menu offline-first (lokalne źródło zastąpione fake'iem w pamięci).
class OfflineFirstMenuRepositoryTest {

    private class FakeRemote(
        var menu: Result<MenuDto, DataError.Remote> = Result.Success(MenuDto(categories = listOf(CategoryDto("c", "Kawy", 1)))),
        var availability: Result<Map<String, Boolean>, DataError.Remote> = Result.Success(mapOf("p" to false)),
    ) : MenuRemoteDataSource {
        override suspend fun fetchMenu() = menu
        override suspend fun fetchAvailability(cafeId: String) = availability
    }

    private class FakeLocal : MenuLocalDataSource {
        val saved = MutableStateFlow<Map<String, Menu>>(emptyMap())
        var lastSyncedAt: Long? = null
        var saveResult: EmptyResult<DataError.Local> = Result.Success(Unit)

        override fun observeMenu(cafeId: String): Flow<Menu?> = saved.map { it[cafeId] }

        override suspend fun saveMenu(
            cafeId: String,
            menu: MenuDto,
            availability: Map<String, Boolean>,
            syncedAtEpochMillis: Long,
        ): EmptyResult<DataError.Local> {
            if (saveResult is Result.Failure) return saveResult
            lastSyncedAt = syncedAtEpochMillis
            saved.value += cafeId to Menu(
                categories = menu.categories.map { Category(it.id, it.name, it.sortOrder) },
                products = emptyList(),
                unavailableProductIds = availability.filterValues { !it }.keys,
            )
            return saveResult
        }
    }

    private val fixedClock = object : Clock {
        override fun now() = Instant.fromEpochMilliseconds(1_234)
    }

    @Test
    fun menuIsNullBeforeFirstRefresh() = runTest {
        val repository = OfflineFirstMenuRepository(FakeLocal(), FakeRemote(), fixedClock)

        assertNull(repository.observeMenu("cafe").first())
    }

    @Test
    fun successfulRefreshSavesMenuWithAvailabilityAndSyncTime() = runTest {
        val local = FakeLocal()
        val repository = OfflineFirstMenuRepository(local, FakeRemote(), fixedClock)

        assertEquals(Result.Success(Unit), repository.refresh("cafe"))

        val menu = assertNotNull(repository.observeMenu("cafe").first())
        assertEquals(listOf("c"), menu.categories.map { it.id })
        assertEquals(setOf("p"), menu.unavailableProductIds)
        assertEquals(1_234, local.lastSyncedAt)
    }

    @Test
    fun failedAvailabilityFetchReturnsErrorAndSavesNothing() = runTest {
        val local = FakeLocal()
        val remote = FakeRemote(availability = Result.Failure(DataError.Remote.NO_INTERNET))
        val repository = OfflineFirstMenuRepository(local, remote, fixedClock)

        assertEquals(Result.Failure(DataError.Remote.NO_INTERNET), repository.refresh("cafe"))
        assertNull(local.lastSyncedAt)
    }

    @Test
    fun failedRefreshKeepsPreviouslySavedMenu() = runTest {
        val remote = FakeRemote()
        val repository = OfflineFirstMenuRepository(FakeLocal(), remote, fixedClock)
        repository.refresh("cafe")

        remote.menu = Result.Failure(DataError.Remote.SERVER)

        assertEquals(Result.Failure(DataError.Remote.SERVER), repository.refresh("cafe"))
        assertNotNull(repository.observeMenu("cafe").first())
    }

    @Test
    fun localSaveErrorIsReturned() = runTest {
        val local = FakeLocal().apply { saveResult = Result.Failure(DataError.Local.DISK_FULL) }
        val repository = OfflineFirstMenuRepository(local, FakeRemote(), fixedClock)

        assertEquals(Result.Failure(DataError.Local.DISK_FULL), repository.refresh("cafe"))
    }

    @Test
    fun seedDataSourceRejectsUnknownCafe() = runTest {
        val seed = SeedMenuRemoteDataSource(latency = Duration.ZERO)

        assertEquals(Result.Failure(DataError.Remote.NOT_FOUND), seed.fetchAvailability("unknown"))
    }

    @Test
    fun sizeEntityWithUnknownCodeOrNegativePriceIsDropped() {
        assertEquals(ProductSize(SizeCode.M, 350, Money(1500)), ProductSizeEntity("p", "m", 350, 1500).toDomainOrNull())
        assertNull(ProductSizeEntity("p", "XL", 350, 1500).toDomainOrNull())
        assertNull(ProductSizeEntity("p", "S", null, -1).toDomainOrNull())
    }

    @Test
    fun productEntityMapsSizesInSizeOrderAndBlankTextsToNull() {
        val product = ProductWithSizes(
            product = ProductEntity("p", "c", "Latte", description = " ", imageUrl = "", sortOrder = 1, isActive = true),
            sizes = listOf(ProductSizeEntity("p", "L", 450, 1800), ProductSizeEntity("p", "S", 250, 1400)),
        ).toDomain()

        assertEquals(listOf(SizeCode.S, SizeCode.L), product.sizes.map { it.code })
        assertNull(product.description)
        assertNull(product.imageUrl)
    }
}
