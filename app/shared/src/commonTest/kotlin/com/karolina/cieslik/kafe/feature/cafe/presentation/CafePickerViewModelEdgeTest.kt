package com.karolina.cieslik.kafe.feature.cafe.presentation

import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.EmptyResult
import com.karolina.cieslik.kafe.core.domain.Result
import com.karolina.cieslik.kafe.core.testing.runViewModelTest
import com.karolina.cieslik.kafe.feature.cafe.domain.Cafe
import com.karolina.cieslik.kafe.feature.cafe.domain.CafeRepository
import com.karolina.cieslik.kafe.feature.cafe.domain.OpeningHours
import com.karolina.cieslik.kafe.feature.cafe.domain.Weekday
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * „Otwarte teraz” w strefie Europe/Warsaw (czas zimowy/letni, dzień zmiany czasu), sortowanie po polsku
 * i spójność etykiety godzin ze statusem.
 */
class CafePickerViewModelEdgeTest {

    private val warsaw = TimeZone.of("Europe/Warsaw")

    private fun everyDay(opens: Int, closes: Int) = Weekday.entries.map { OpeningHours(it, opens, closes) }

    private fun cafe(id: String, name: String = "Kafe $id", hours: List<OpeningHours> = everyDay(7 * 60, 20 * 60)) =
        Cafe(id, name, "ul. Testowa 1", 0.0, 0.0, hours)

    private fun TestScope.contentAt(
        track: (CafePickerViewModel) -> CafePickerViewModel,
        instant: String,
        cafes: List<Cafe>,
    ): CafePickerUiState.Content {
        val clock = object : Clock {
            override fun now() = Instant.parse(instant)
        }
        val viewModel = track(CafePickerViewModel(StaticCafeRepository(cafes), clock, warsaw))
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()
        return assertIs<CafePickerUiState.Content>(viewModel.state.value)
    }

    private fun isOpen(content: CafePickerUiState.Content) = content.cafes.single().isOpenNow

    @Test
    fun winterTimeUsesUtcPlusOne() = runViewModelTest { context ->
        val cafes = listOf(cafe("a"))
        // 7 grudnia 2026 (poniedziałek), CET = UTC+1.
        assertFalse(isOpen(contentAt(context::track, "2026-12-07T05:59:00Z", cafes)))
        assertTrue(isOpen(contentAt(context::track, "2026-12-07T06:00:00Z", cafes)))
        assertTrue(isOpen(contentAt(context::track, "2026-12-07T18:59:00Z", cafes)))
        assertFalse(isOpen(contentAt(context::track, "2026-12-07T19:00:00Z", cafes)))
    }

    @Test
    fun summerTimeUsesUtcPlusTwo() = runViewModelTest { context ->
        val cafes = listOf(cafe("a"))
        // 6 lipca 2026 (poniedziałek), CEST = UTC+2.
        assertFalse(isOpen(contentAt(context::track, "2026-07-06T04:59:00Z", cafes)))
        assertTrue(isOpen(contentAt(context::track, "2026-07-06T05:00:00Z", cafes)))
        assertFalse(isOpen(contentAt(context::track, "2026-07-06T18:00:00Z", cafes)))
    }

    @Test
    fun dayOfSpringTimeChangeUsesNewOffsetAfterTwoAm() = runViewModelTest { context ->
        // 29 marca 2026 (niedziela): 2:00 CET -> 3:00 CEST. Lokal otwarty 3:00–4:00.
        val cafes = listOf(cafe("a", hours = everyDay(3 * 60, 4 * 60)))

        assertTrue(isOpen(contentAt(context::track, "2026-03-29T01:30:00Z", cafes)))
        assertFalse(isOpen(contentAt(context::track, "2026-03-29T00:59:00Z", cafes)))
    }

    @Test
    fun weekdayIsTakenFromWarsawDateNotUtcDate() = runViewModelTest { context ->
        // Niedziela 23:30 UTC = poniedziałek 0:30 w Warszawie (zima). Otwarte tylko w poniedziałki od 0:00.
        val mondayOnly = listOf(cafe("a", hours = listOf(OpeningHours(Weekday.MONDAY, 0, 60))))

        val content = contentAt(context::track, "2026-12-06T23:30:00Z", mondayOnly)

        assertTrue(isOpen(content))
        assertEquals("Dziś 0:00–1:00", content.cafes.single().openingHoursLabel)
    }

    @Test
    fun overnightCafeAfterClosingShowsTodaysLabelAndSaturdayNoonShowsFullRange() = runViewModelTest { context ->
        val overnight = listOf(cafe("a", hours = listOf(OpeningHours(Weekday.SATURDAY, 18 * 60, 2 * 60))))

        // Niedziela 2:00 (zima) – już zamknięte, w niedzielę brak godzin.
        val sundayAfterClosing = contentAt(context::track, "2026-12-06T01:00:00Z", overnight).cafes.single()
        assertFalse(sundayAfterClosing.isOpenNow)
        assertEquals(CAFE_CLOSED_TODAY, sundayAfterClosing.openingHoursLabel)

        // Sobota 12:00 – zamknięte, etykieta z pełnym zakresem przez północ.
        val saturdayNoon = contentAt(context::track, "2026-12-05T11:00:00Z", overnight).cafes.single()
        assertFalse(saturdayNoon.isOpenNow)
        assertEquals("Dziś 18:00–2:00", saturdayNoon.openingHoursLabel)
    }

    @Test
    fun cafesAreSortedPolishAlphabeticallyNotByCodePoint() = runViewModelTest { context ->
        val cafes = listOf(
            "Kafe Żoliborz", "Kafe Śródmieście", "Kafe Zabłocie", "Kafe Sopot", "Kafe Łazienki", "Kafe Mokotów",
        )
            .mapIndexed { index, name -> cafe("c$index", name) }

        val content = contentAt(context::track, "2026-10-19T08:00:00Z", cafes)

        assertEquals(
            listOf(
                "Kafe Łazienki", "Kafe Mokotów", "Kafe Sopot", "Kafe Śródmieście", "Kafe Zabłocie", "Kafe Żoliborz",
            ),
            content.cafes.map { it.name },
        )
    }

    @Test
    fun overnightCafeOpenNowIsNotLabelledClosedToday() = runViewModelTest { context ->
        // Sobota 18:00–2:00, w niedzielę zamknięte. Niedziela 1:00 (zima) – lokal wciąż otwarty od soboty.
        val overnight = listOf(cafe("a", hours = listOf(OpeningHours(Weekday.SATURDAY, 18 * 60, 2 * 60))))

        val cafeUi = contentAt(context::track, "2026-12-06T00:00:00Z", overnight).cafes.single()

        assertTrue(cafeUi.isOpenNow)
        assertNotEquals(
            CAFE_CLOSED_TODAY,
            cafeUi.openingHoursLabel,
            "Etykieta „Dziś zamknięte” przy statusie „Otwarte”",
        )
        assertEquals("Do 2:00", cafeUi.openingHoursLabel)
    }
}

private class StaticCafeRepository(cafes: List<Cafe>) : CafeRepository {
    private val cafes = MutableStateFlow(cafes)
    private val selectedId = MutableStateFlow<String?>(null)

    override fun observeCafes(): Flow<List<Cafe>> = cafes

    override fun observeSelectedCafe(): Flow<Cafe?> =
        combine(cafes, selectedId) { list, id -> list.firstOrNull { it.id == id } }

    override suspend fun selectCafe(cafeId: String): EmptyResult<DataError.Local> {
        selectedId.value = cafeId
        return Result.Success(Unit)
    }

    override suspend fun refresh(): EmptyResult<DataError> = Result.Success(Unit)
}
