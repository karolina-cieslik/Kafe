package com.karolina.cieslik.kafe.feature.start.presentation

import androidx.lifecycle.viewModelScope
import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.EmptyResult
import com.karolina.cieslik.kafe.core.domain.Result
import com.karolina.cieslik.kafe.feature.cafe.domain.Cafe
import com.karolina.cieslik.kafe.feature.cafe.domain.CafeRepository
import com.karolina.cieslik.kafe.navigation.KafeRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals

/** Ekran startowy zależy od zapamiętanego wyboru lokalu. */
@OptIn(ExperimentalCoroutinesApi::class)
class AppStartViewModelTest {

    private class FakeCafeRepository(private val selected: Cafe?) : CafeRepository {
        var refreshCount = 0
        override fun observeCafes(): Flow<List<Cafe>> = flowOf(listOfNotNull(selected))
        override fun observeSelectedCafe(): Flow<Cafe?> = flowOf(selected)
        override suspend fun selectCafe(cafeId: String) = Unit
        override suspend fun refresh(): EmptyResult<DataError> {
            refreshCount++
            return Result.Success(Unit)
        }
    }

    private fun startDestinationFor(selected: Cafe?): Pair<AppStartState, Int> {
        var result: Pair<AppStartState, Int>? = null
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val repository = FakeCafeRepository(selected)
            val viewModel = AppStartViewModel(repository, dispatcher)
            try {
                assertEquals(AppStartState.Loading, viewModel.state.value)
                advanceUntilIdle()
                result = viewModel.state.value to repository.refreshCount
            } finally {
                viewModel.viewModelScope.cancel()
                advanceUntilIdle()
                Dispatchers.resetMain()
            }
        }
        return result!!
    }

    @Test
    fun firstLaunchWithoutChoiceStartsOnCafePicker() {
        assertEquals(AppStartState.Ready(KafeRoute.CafePicker) to 0, startDestinationFor(selected = null))
    }

    @Test
    fun storedChoiceStartsOnMenuWithoutNetwork() {
        val cafe = Cafe("a", "Kafe A", "ul. Testowa 1", 0.0, 0.0, emptyList())

        assertEquals(AppStartState.Ready(KafeRoute.Menu) to 0, startDestinationFor(selected = cafe))
    }
}
