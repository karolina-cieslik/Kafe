package com.karolina.cieslik.kafe.feature.menu.presentation

import androidx.lifecycle.viewModelScope
import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.EmptyResult
import com.karolina.cieslik.kafe.core.domain.Result
import com.karolina.cieslik.kafe.feature.cafe.domain.Cafe
import com.karolina.cieslik.kafe.feature.cafe.domain.CafeRepository
import com.karolina.cieslik.kafe.feature.menu.domain.Category
import com.karolina.cieslik.kafe.feature.menu.domain.GetMenuForCafe
import com.karolina.cieslik.kafe.feature.menu.domain.Menu
import com.karolina.cieslik.kafe.feature.menu.domain.MenuRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

// KAFE-6: Regresja zgłoszona przez testera – brak lokali bez sieci kończył się wiecznym Loading (AC 7).
@OptIn(ExperimentalCoroutinesApi::class)
class MenuViewModelCafeErrorTest {

    private class FakeCafeRepository : CafeRepository {
        val cafes = MutableStateFlow<List<Cafe>>(emptyList())
        var refreshResult: EmptyResult<DataError> = Result.Failure(DataError.Remote.NO_INTERNET)
        var refreshCount = 0

        override fun observeCafes(): Flow<List<Cafe>> = cafes
        override fun observeSelectedCafe(): Flow<Cafe?> = cafes.map { it.firstOrNull() }
        override suspend fun selectCafe(cafeId: String) = Unit
        override suspend fun refresh(): EmptyResult<DataError> {
            refreshCount++
            if (refreshResult is Result.Success) cafes.value = listOf(Cafe("a", "Kafe A", "ul. Testowa 1", 0.0, 0.0, emptyList()))
            return refreshResult
        }
    }

    private class FakeMenuRepository : MenuRepository {
        private val menu = MutableStateFlow<Menu?>(null)
        override fun observeMenu(cafeId: String): Flow<Menu?> = menu
        override suspend fun refresh(cafeId: String): EmptyResult<DataError> {
            menu.value = Menu(listOf(Category("c", "Kawy", 1)), emptyList(), emptySet())
            return Result.Success(Unit)
        }
    }

    private fun runVmTest(block: suspend TestScope.(MenuViewModel, FakeCafeRepository) -> Unit) = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val cafes = FakeCafeRepository()
        val menu = FakeMenuRepository()
        val viewModel = MenuViewModel(cafes, menu, GetMenuForCafe(menu), dispatcher)
        val collector = launch { viewModel.state.collect {} }
        try {
            block(viewModel, cafes)
        } finally {
            collector.cancel()
            viewModel.viewModelScope.cancel()
            // Anulowanie musi się dokończyć na testowym Main, zanim go zdejmiemy.
            advanceUntilIdle()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun cafeListFailureWithoutCacheShowsConnectionError() = runVmTest { viewModel, _ ->
        advanceUntilIdle()

        assertEquals(MenuUiState.Error(MENU_ERROR_NO_CONNECTION), viewModel.state.value)
    }

    @Test
    fun retryAfterCafeListFailureLoadsCafesAndThenMenu() = runVmTest { viewModel, cafes ->
        advanceUntilIdle()
        cafes.refreshResult = Result.Success(Unit)

        viewModel.onAction(MenuAction.Retry)
        advanceUntilIdle()

        val content = assertIs<MenuUiState.Content>(viewModel.state.value)
        assertEquals("Kafe A", content.cafeName)
        assertEquals(2, cafes.refreshCount)
    }
}
