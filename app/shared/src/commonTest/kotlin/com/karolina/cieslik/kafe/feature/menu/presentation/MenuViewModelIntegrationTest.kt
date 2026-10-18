package com.karolina.cieslik.kafe.feature.menu.presentation

import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.EmptyResult
import com.karolina.cieslik.kafe.core.domain.Result
import com.karolina.cieslik.kafe.core.testing.ViewModelTestContext
import com.karolina.cieslik.kafe.core.testing.runViewModelTest
import com.karolina.cieslik.kafe.feature.cafe.data.OfflineFirstCafeRepository
import com.karolina.cieslik.kafe.feature.cafe.data.local.CafeLocalDataSource
import com.karolina.cieslik.kafe.feature.cafe.data.remote.CafeDto
import com.karolina.cieslik.kafe.feature.cafe.data.remote.CafeRemoteDataSource
import com.karolina.cieslik.kafe.feature.cafe.data.remote.SeedCafeRemoteDataSource
import com.karolina.cieslik.kafe.feature.cafe.domain.Cafe
import com.karolina.cieslik.kafe.feature.menu.data.OfflineFirstMenuRepository
import com.karolina.cieslik.kafe.feature.menu.data.local.MenuLocalDataSource
import com.karolina.cieslik.kafe.feature.menu.data.local.ModifierGroupWithOptions
import com.karolina.cieslik.kafe.feature.menu.data.local.toMenu
import com.karolina.cieslik.kafe.feature.menu.data.local.toSnapshot
import com.karolina.cieslik.kafe.feature.menu.data.local.ProductWithSizes
import com.karolina.cieslik.kafe.feature.menu.data.remote.AvailabilityDto
import com.karolina.cieslik.kafe.feature.menu.data.remote.MenuDto
import com.karolina.cieslik.kafe.feature.menu.data.remote.SeedMenuRemoteDataSource
import com.karolina.cieslik.kafe.feature.menu.domain.GetMenuForCafe
import com.karolina.cieslik.kafe.feature.menu.domain.Menu
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.time.Duration

/** MenuViewModel na repozytoriach offline-first z lokalnymi źródłami w pamięci i danymi seed, end-to-end bez UI. */
class MenuViewModelIntegrationTest {

    private val offlineCafes = object : CafeRemoteDataSource {
        override suspend fun fetchCafes(): Result<List<CafeDto>, DataError.Remote> =
            Result.Failure(DataError.Remote.NO_INTERNET)
    }

    private data class Setup(
        val viewModel: MenuViewModel,
        val cafeRepository: OfflineFirstCafeRepository,
        val cafeRemote: SwitchableCafeRemote,
    )

    private fun ViewModelTestContext.createViewModel(cafeRemote: CafeRemoteDataSource = SeedCafeRemoteDataSource()): Setup {
        val switchable = SwitchableCafeRemote(cafeRemote)
        val cafeRepository = OfflineFirstCafeRepository(InMemoryCafeLocal(), switchable)
        val menuRepository = OfflineFirstMenuRepository(InMemoryMenuLocal(), SeedMenuRemoteDataSource(latency = Duration.ZERO))
        val viewModel = track(
            MenuViewModel(
                cafeRepository = cafeRepository,
                menuRepository = menuRepository,
                getMenuForCafe = GetMenuForCafe(menuRepository),
                dispatcher = dispatcher,
            ),
        )
        return Setup(viewModel, cafeRepository, switchable)
    }

    private fun TestScope.observe(viewModel: MenuViewModel) {
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()
    }

    @Test
    fun seedMenuShowsCategoriesInNetworkOrderWithoutEmptyCategory() = runViewModelTest { context ->
        val (viewModel) = context.createViewModel()

        observe(viewModel)

        val content = assertIs<MenuUiState.Content>(viewModel.state.value)
        assertEquals("Kafe Rynek", content.cafeName)
        assertEquals(listOf("Kawy klasyczne", "Przelewy", "Sezonowe", "Przekąski"), content.categories.map { it.name })
    }

    @Test
    fun seedSeasonalCategoryShowsOnlyActiveProducts() = runViewModelTest { context ->
        val (viewModel) = context.createViewModel()

        observe(viewModel)

        val seasonal = assertIs<MenuUiState.Content>(viewModel.state.value).categories.first { it.name == "Sezonowe" }
        assertEquals(listOf("pumpkin-latte"), seasonal.products.map { it.id })
    }

    @Test
    fun seedProductsShowCheapestSizePrice() = runViewModelTest { context ->
        val (viewModel) = context.createViewModel()

        observe(viewModel)

        val products = assertIs<MenuUiState.Content>(viewModel.state.value).categories.flatMap { it.products }.associateBy { it.id }
        assertEquals("13,00 zł", products.getValue("cappuccino").priceLabel)
        assertEquals("18,00 zł", products.getValue("v60").priceLabel)
        assertEquals("9,50 zł", products.getValue("croissant").priceLabel)
    }

    @Test
    fun switchingToGdanskMarksItsMissingProductsUnavailable() = runViewModelTest { context ->
        val setup = context.createViewModel()
        observe(setup.viewModel)

        setup.cafeRepository.selectCafe("gdansk-dluga")
        advanceUntilIdle()

        val content = assertIs<MenuUiState.Content>(setup.viewModel.state.value)
        assertEquals("Kafe Długa", content.cafeName)
        val unavailable = content.categories.flatMap { it.products }.filterNot { it.isAvailable }.map { it.id }.toSet()
        assertEquals(setOf("cappuccino", "chemex", "cheesecake", "flat-white"), unavailable)
        assertFalse(content.isRefreshing)
    }

    @Test
    fun cafeListFailureWithoutCacheShowsConnectionError() = runViewModelTest { context ->
        val (viewModel) = context.createViewModel(cafeRemote = offlineCafes)

        observe(viewModel)

        assertEquals(MenuUiState.Error(MENU_ERROR_NO_CONNECTION), viewModel.state.value)
    }

    @Test
    fun retryAfterCafeListFailureLoadsCafesAndMenu() = runViewModelTest { context ->
        val setup = context.createViewModel(cafeRemote = offlineCafes)
        observe(setup.viewModel)
        assertIs<MenuUiState.Error>(setup.viewModel.state.value)

        setup.cafeRemote.delegate = SeedCafeRemoteDataSource()
        setup.viewModel.onAction(MenuAction.Retry)
        advanceUntilIdle()

        val content = assertIs<MenuUiState.Content>(setup.viewModel.state.value)
        assertEquals("Kafe Rynek", content.cafeName)
        assertEquals(4, content.categories.size)
    }
}

/** Zdalne źródło lokali z podmienialnym [delegate] – symuluje odzyskanie sieci między próbami (Retry). */
private class SwitchableCafeRemote(var delegate: CafeRemoteDataSource) : CafeRemoteDataSource {
    override suspend fun fetchCafes(): Result<List<CafeDto>, DataError.Remote> = delegate.fetchCafes()
}

/** Lokalne źródło lokali w pamięci zamiast Room; `null` do pierwszego zapisu (lokale nigdy nie pobrane). */
private class InMemoryCafeLocal : CafeLocalDataSource {
    private val cafes = MutableStateFlow<List<Cafe>?>(null)

    override fun observeCafes(): Flow<List<Cafe>?> = cafes

    override suspend fun saveCafes(cafes: List<CafeDto>, syncedAtEpochMillis: Long): EmptyResult<DataError.Local> {
        this.cafes.value = cafes.map { Cafe(it.id, it.name, it.address, it.lat, it.lng, emptyList()) }
        return Result.Success(Unit)
    }
}

/**
 * Lokalne źródło menu w pamięci zamiast Room. Zapis idzie przez te same mappery encji co
 * RoomMenuLocalDataSource, więc test integracyjny sprawdza też mapowanie DTO -> encje -> domena.
 */
private class InMemoryMenuLocal : MenuLocalDataSource {
    private val menus = MutableStateFlow<Map<String, Menu>>(emptyMap())

    override fun observeMenu(cafeId: String): Flow<Menu?> = menus.map { it[cafeId] }

    override suspend fun saveMenu(
        cafeId: String,
        menu: MenuDto,
        availability: AvailabilityDto,
        syncedAtEpochMillis: Long,
    ): EmptyResult<DataError.Local> {
        val snapshot = menu.toSnapshot(cafeId, availability)
        val sizesByProduct = snapshot.sizes.groupBy { it.productId }
        val groupsByProduct = snapshot.productModifierGroups.groupBy { it.productId }
        val optionsByGroup = snapshot.modifierOptions.groupBy { it.groupId }
        menus.value += cafeId to toMenu(
            categories = snapshot.categories,
            products = snapshot.products.map {
                ProductWithSizes(it, sizesByProduct[it.id].orEmpty(), groupsByProduct[it.id].orEmpty())
            },
            groups = snapshot.modifierGroups.map { ModifierGroupWithOptions(it, optionsByGroup[it.id].orEmpty()) },
            availability = snapshot.availability,
        )
        return Result.Success(Unit)
    }
}
