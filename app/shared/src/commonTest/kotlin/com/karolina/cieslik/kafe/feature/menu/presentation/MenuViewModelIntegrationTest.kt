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
import com.karolina.cieslik.kafe.feature.menu.data.local.CategoryEntity
import com.karolina.cieslik.kafe.feature.menu.data.local.MenuLocalDataSource
import com.karolina.cieslik.kafe.feature.menu.data.local.ProductEntity
import com.karolina.cieslik.kafe.feature.menu.data.local.ProductSizeEntity
import com.karolina.cieslik.kafe.feature.menu.data.local.ProductWithSizes
import com.karolina.cieslik.kafe.feature.menu.data.local.toDomain
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

// KAFE-6: MenuViewModel na repozytoriach offline-first z lokalnymi źródłami w pamięci i danymi seed – AC 1–5, 7 end-to-end bez UI.
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
        // AC 3: kolejność sieci; AC 4: „Herbaty” bez produktów ukryte.
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
        assertEquals(setOf("chemex", "cheesecake", "flat-white"), unavailable)
        assertFalse(content.isRefreshing)
    }

    // AC 7: pierwsze uruchomienie bez sieci – nie da się pobrać lokali, więc nie ma też menu.
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
        availability: Map<String, Boolean>,
        syncedAtEpochMillis: Long,
    ): EmptyResult<DataError.Local> {
        val domain = Menu(
            categories = menu.categories.map { CategoryEntity(it.id, it.name, it.sortOrder).toDomain() },
            products = menu.products.map { product ->
                ProductWithSizes(
                    product = ProductEntity(
                        id = product.id,
                        categoryId = product.categoryId,
                        name = product.name,
                        description = product.description,
                        imageUrl = product.imageUrl,
                        sortOrder = product.sortOrder,
                        isActive = product.active,
                    ),
                    sizes = product.sizes.map { ProductSizeEntity(product.id, it.code.uppercase(), it.volumeMl, it.priceGrosze) },
                ).toDomain()
            },
            unavailableProductIds = availability.filterValues { !it }.keys,
        )
        menus.value += cafeId to domain
        return Result.Success(Unit)
    }
}
