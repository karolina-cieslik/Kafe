package com.karolina.cieslik.kafe.feature.menu.presentation

import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.Result
import com.karolina.cieslik.kafe.core.testing.ViewModelTestContext
import com.karolina.cieslik.kafe.core.testing.runViewModelTest
import com.karolina.cieslik.kafe.feature.cafe.data.InMemoryCafeRepository
import com.karolina.cieslik.kafe.feature.cafe.data.remote.CafeDto
import com.karolina.cieslik.kafe.feature.cafe.data.remote.CafeRemoteDataSource
import com.karolina.cieslik.kafe.feature.cafe.data.remote.SeedCafeRemoteDataSource
import com.karolina.cieslik.kafe.feature.menu.data.InMemoryMenuRepository
import com.karolina.cieslik.kafe.feature.menu.data.remote.SeedMenuRemoteDataSource
import com.karolina.cieslik.kafe.feature.menu.domain.GetMenuForCafe
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

// KAFE-6: MenuViewModel na repozytoriach w pamięci i danych seed – AC 1–5 end-to-end bez UI.
class MenuViewModelIntegrationTest {

    private fun ViewModelTestContext.createViewModel(
        cafeRemote: CafeRemoteDataSource = SeedCafeRemoteDataSource(),
    ): Pair<MenuViewModel, InMemoryCafeRepository> {
        val cafeRepository = InMemoryCafeRepository(cafeRemote)
        val menuRepository = InMemoryMenuRepository(SeedMenuRemoteDataSource(latency = kotlin.time.Duration.ZERO))
        val viewModel = track(
            MenuViewModel(
                cafeRepository = cafeRepository,
                menuRepository = menuRepository,
                getMenuForCafe = GetMenuForCafe(menuRepository),
                dispatcher = dispatcher,
            ),
        )
        return viewModel to cafeRepository
    }

    private fun TestScope.observe(viewModel: MenuViewModel) {
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()
    }

    @Test
    fun seedMenuShowsCategoriesInNetworkOrderWithoutEmptyCategory() = runViewModelTest { context ->
        val (viewModel, _) = context.createViewModel()

        observe(viewModel)

        val content = assertIs<MenuUiState.Content>(viewModel.state.value)
        assertEquals("Kafe Rynek", content.cafeName)
        // AC 3: kolejność sieci; AC 4: „Herbaty” bez produktów ukryte.
        assertEquals(listOf("Kawy klasyczne", "Przelewy", "Sezonowe", "Przekąski"), content.categories.map { it.name })
    }

    @Test
    fun seedSeasonalCategoryShowsOnlyActiveProducts() = runViewModelTest { context ->
        val (viewModel, _) = context.createViewModel()

        observe(viewModel)

        val seasonal = assertIs<MenuUiState.Content>(viewModel.state.value).categories.first { it.name == "Sezonowe" }
        assertEquals(listOf("pumpkin-latte"), seasonal.products.map { it.id })
    }

    @Test
    fun seedProductsShowCheapestSizePrice() = runViewModelTest { context ->
        val (viewModel, _) = context.createViewModel()

        observe(viewModel)

        val products = assertIs<MenuUiState.Content>(viewModel.state.value).categories.flatMap { it.products }.associateBy { it.id }
        assertEquals("13,00 zł", products.getValue("cappuccino").priceLabel)
        assertEquals("18,00 zł", products.getValue("v60").priceLabel)
        assertEquals("9,50 zł", products.getValue("croissant").priceLabel)
    }

    @Test
    fun switchingToGdanskMarksItsMissingProductsUnavailable() = runViewModelTest { context ->
        val (viewModel, cafes) = context.createViewModel()
        observe(viewModel)

        cafes.selectCafe("gdansk-dluga")
        advanceUntilIdle()

        val content = assertIs<MenuUiState.Content>(viewModel.state.value)
        assertEquals("Kafe Długa", content.cafeName)
        val unavailable = content.categories.flatMap { it.products }.filterNot { it.isAvailable }.map { it.id }.toSet()
        assertEquals(setOf("chemex", "cheesecake", "flat-white"), unavailable)
        assertFalse(content.isRefreshing)
    }

    // KAFE-6 BUG (zgłoszony devowi): gdy pobranie listy lokali padnie (brak sieci przy pierwszym uruchomieniu),
    // InMemoryCafeRepository.observeCafes() nigdy nie emituje, VM zostaje w Loading na zawsze, a Retry nic nie robi
    // (lastCafeId == null). AC 7 wymaga komunikatu „Nie udało się pobrać menu. Sprawdź połączenie.” z „Spróbuj ponownie”.
    // Odznaczyć @Ignore po poprawce.
    @Ignore
    @Test
    fun cafeListFailureWithoutCacheShowsConnectionError() = runViewModelTest { context ->
        val offline = object : CafeRemoteDataSource {
            override suspend fun fetchCafes(): Result<List<CafeDto>, DataError.Remote> =
                Result.Failure(DataError.Remote.NO_INTERNET)
        }
        val (viewModel, _) = context.createViewModel(cafeRemote = offline)

        observe(viewModel)

        assertEquals(MenuUiState.Error(MENU_ERROR_NO_CONNECTION), viewModel.state.value)
    }
}
