package com.karolina.cieslik.kafe.feature.cafe.presentation

import androidx.lifecycle.viewModelScope
import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.EmptyResult
import com.karolina.cieslik.kafe.core.domain.Result
import com.karolina.cieslik.kafe.feature.cafe.domain.Cafe
import com.karolina.cieslik.kafe.feature.cafe.domain.CafeRepository
import com.karolina.cieslik.kafe.feature.cafe.domain.OpeningHours
import com.karolina.cieslik.kafe.feature.cafe.domain.Weekday
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

// KAFE-29 / KAFE-31: CafePickerViewModel ze wstrzykiwanym zegarem i strefą.
@OptIn(ExperimentalCoroutinesApi::class)
class CafePickerViewModelTest {

    private class FakeCafeRepository(cafes: List<Cafe> = emptyList(), selectedId: String? = null) : CafeRepository {
        val cafes = MutableStateFlow(cafes)
        val selectedId = MutableStateFlow(selectedId)
        var remoteCafes: List<Cafe> = seedCafes
        var refreshResult: EmptyResult<DataError> = Result.Success(Unit)
        var refreshCount = 0

        override fun observeCafes(): Flow<List<Cafe>> = cafes
        override fun observeSelectedCafe(): Flow<Cafe?> =
            combine(cafes, selectedId) { list, id -> list.firstOrNull { it.id == id } }
        override suspend fun selectCafe(cafeId: String) {
            selectedId.value = cafeId
        }
        override suspend fun refresh(): EmptyResult<DataError> {
            refreshCount++
            if (refreshResult is Result.Success) cafes.value = remoteCafes
            return refreshResult
        }
    }

    private class Env(
        val viewModel: CafePickerViewModel,
        val repository: FakeCafeRepository,
        val events: MutableList<CafePickerEvent>,
    ) {
        val content get() = assertIs<CafePickerUiState.Content>(viewModel.state.value)
    }

    /** Domyślnie poniedziałek 2026-10-19, 12:30 czasu warszawskiego (10:30 UTC). */
    private fun runVmTest(
        repository: FakeCafeRepository = FakeCafeRepository(),
        now: Instant = Instant.parse("2026-10-19T10:30:00Z"),
        block: suspend TestScope.(Env) -> Unit,
    ) = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val clock = object : Clock {
            override fun now() = now
        }
        val viewModel = CafePickerViewModel(repository, clock, TimeZone.of("Europe/Warsaw"), dispatcher)
        val events = mutableListOf<CafePickerEvent>()
        val collectors = listOf(
            launch { viewModel.state.collect {} },
            launch { viewModel.events.collect { events += it } },
        )
        try {
            advanceUntilIdle()
            block(Env(viewModel, repository, events))
        } finally {
            collectors.forEach { it.cancel() }
            viewModel.viewModelScope.cancel()
            advanceUntilIdle()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun cafesAreSortedAlphabeticallyWithTodayHoursAndOpenStatus() = runVmTest { env ->
        val cafes = env.content.cafes

        assertEquals(listOf("Kafe Długa", "Kafe Łąkowa", "Kafe Rynek"), cafes.map { it.name })
        assertEquals("Dziś 7:00–20:00", cafes.single { it.id == "rynek" }.openingHoursLabel)
        assertTrue(cafes.single { it.id == "rynek" }.isOpenNow)
        // Długa w poniedziałek otwiera się o 13:00.
        assertFalse(cafes.single { it.id == "dluga" }.isOpenNow)
        assertEquals("Dziś zamknięte", cafes.single { it.id == "lakowa" }.openingHoursLabel)
        assertFalse(cafes.single { it.id == "lakowa" }.isOpenNow)
    }

    @Test
    fun openNowUsesWarsawTimeNotUtc() = runVmTest(now = Instant.parse("2026-10-19T18:30:00Z")) { env ->
        // 18:30 UTC = 20:30 w Warszawie (CEST) – Rynek (7:00–20:00) już zamknięty.
        assertFalse(env.content.cafes.single { it.id == "rynek" }.isOpenNow)
    }

    @Test
    fun withoutStoredChoiceItIsFirstLaunchAndNothingIsSelected() = runVmTest { env ->
        assertTrue(env.content.isFirstLaunch)
        assertTrue(env.content.cafes.none { it.isSelected })
    }

    @Test
    fun storedChoiceIsMarkedAndItIsNotFirstLaunch() = runVmTest(FakeCafeRepository(seedCafes, selectedId = "dluga")) { env ->
        assertFalse(env.content.isFirstLaunch)
        assertEquals("dluga", env.content.cafes.single { it.isSelected }.id)
    }

    @Test
    fun selectingCafeStoresChoiceAndEmitsCafeChosen() = runVmTest { env ->
        env.viewModel.onAction(CafePickerAction.CafeSelected("rynek"))
        advanceUntilIdle()

        assertEquals("rynek", env.repository.selectedId.value)
        assertEquals(listOf<CafePickerEvent>(CafePickerEvent.CafeChosen), env.events)
        // Flaga pierwszego uruchomienia nie znika po wyborze.
        assertTrue(env.content.isFirstLaunch)
    }

    @Test
    fun unknownCafeIsIgnored() = runVmTest { env ->
        env.viewModel.onAction(CafePickerAction.CafeSelected("missing"))
        advanceUntilIdle()

        assertEquals(null, env.repository.selectedId.value)
        assertEquals(emptyList(), env.events)
    }

    @Test
    fun backClickedEmitsNavigateBack() = runVmTest { env ->
        env.viewModel.onAction(CafePickerAction.BackClicked)
        advanceUntilIdle()

        assertEquals(listOf<CafePickerEvent>(CafePickerEvent.NavigateBack), env.events)
    }

    @Test
    fun refreshFailureWithoutCacheShowsErrorAndRetryRecovers() = runVmTest(
        FakeCafeRepository().apply { refreshResult = Result.Failure(DataError.Remote.NO_INTERNET) },
    ) { env ->
        assertEquals(CafePickerUiState.Error(CAFE_PICKER_ERROR_NO_CONNECTION), env.viewModel.state.value)

        env.repository.refreshResult = Result.Success(Unit)
        env.viewModel.onAction(CafePickerAction.Retry)
        advanceUntilIdle()

        assertEquals(3, env.content.cafes.size)
        assertEquals(2, env.repository.refreshCount)
    }

    @Test
    fun refreshFailureWithCacheKeepsCachedCafes() = runVmTest(
        FakeCafeRepository(seedCafes.take(1)).apply { refreshResult = Result.Failure(DataError.Remote.SERVER) },
    ) { env ->
        assertEquals(listOf("rynek"), env.content.cafes.map { it.id })
    }

    @Test
    fun emptyListFromServerIsContentNotEndlessLoading() = runVmTest(FakeCafeRepository().apply { remoteCafes = emptyList() }) { env ->
        assertEquals(emptyList(), env.content.cafes)
    }

    private companion object {
        fun hours(day: Weekday, opens: Int, closes: Int) = OpeningHours(day, opens * 60, closes * 60)

        val seedCafes = listOf(
            Cafe("rynek", "Kafe Rynek", "Rynek Główny 1", 0.0, 0.0, Weekday.entries.map { hours(it, 7, 20) }),
            Cafe("lakowa", "Kafe Łąkowa", "ul. Łąkowa 2", 0.0, 0.0, listOf(hours(Weekday.SATURDAY, 9, 17))),
            Cafe("dluga", "Kafe Długa", "ul. Długa 3", 0.0, 0.0, listOf(hours(Weekday.MONDAY, 13, 22))),
        )
    }
}
