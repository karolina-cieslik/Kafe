package com.karolina.cieslik.kafe

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.EmptyResult
import com.karolina.cieslik.kafe.core.domain.Result
import com.karolina.cieslik.kafe.core.testing.ROBOLECTRIC_SDK
import com.karolina.cieslik.kafe.feature.cafe.domain.Cafe
import com.karolina.cieslik.kafe.feature.cafe.domain.CafeRepository
import com.karolina.cieslik.kafe.feature.cafe.presentation.CafePickerTestTags
import com.karolina.cieslik.kafe.feature.cafe.presentation.CafePickerViewModel
import com.karolina.cieslik.kafe.feature.menu.domain.GetMenuForCafe
import com.karolina.cieslik.kafe.feature.menu.presentation.FakeMenuRepository
import com.karolina.cieslik.kafe.feature.menu.presentation.MenuTestTags
import com.karolina.cieslik.kafe.feature.menu.presentation.MenuViewModel
import com.karolina.cieslik.kafe.feature.menu.presentation.cafe
import com.karolina.cieslik.kafe.feature.menu.presentation.category
import com.karolina.cieslik.kafe.feature.menu.presentation.menu
import com.karolina.cieslik.kafe.feature.menu.presentation.product
import com.karolina.cieslik.kafe.feature.start.presentation.AppStartViewModel
import com.karolina.cieslik.kafe.feature.start.presentation.KafeIntroTestTags
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.TimeZone
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.robolectric.annotation.Config
import kotlin.time.Clock
import kotlin.time.Instant

// KAFE-42 / KAFE-31: Po intro widoczny ekran docelowy – CafePicker bez zapamiętanego lokalu, Menu z wyborem.
// Intro znika dopiero, gdy skończy się animacja ORAZ AppStartViewModel poda ekran startowy.
@RunWith(AndroidJUnit4::class)
@Config(sdk = [ROBOLECTRIC_SDK], qualifiers = "w411dp-h891dp")
class AppStartUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val krakow = cafe("krakow", name = "Kafe Rynek")
    private val cafes = AppStartFakeCafeRepository(listOf(krakow))
    private val sampleMenu = menu(listOf(category("classic", 1)), listOf(product("espresso", "classic", 1, "Espresso")))

    @Before
    fun startDi() {
        val menus = FakeMenuRepository(cached = mapOf("krakow" to sampleMenu))
        val clock = object : Clock {
            override fun now() = Instant.parse("2026-10-19T08:00:00Z")
        }
        startKoin {
            modules(
                module {
                    viewModel { MenuViewModel(cafes, menus, GetMenuForCafe(menus), Dispatchers.Main) }
                    viewModel { CafePickerViewModel(cafes, clock, TimeZone.of("Europe/Warsaw"), Dispatchers.Main) }
                },
            )
        }
    }

    @After
    fun stopDi() {
        stopKoin()
    }

    private fun setApp() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { App(appStartViewModel = AppStartViewModel(cafes, Dispatchers.Main)) }
    }

    /** Intro (≈1300 ms) + crossfade (300 ms); potem intro z nieskończoną animacją znika i można wrócić do auto. */
    private fun finishIntroAndCrossfade() {
        composeRule.mainClock.advanceTimeBy(1_400 + 300 + 100)
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
    }

    @Test
    fun withoutStoredCafeIntroIsFollowedByCafePickerWelcome() {
        cafes.selectedId.value = null
        setApp()

        finishIntroAndCrossfade()

        composeRule.onNodeWithTag(KafeIntroTestTags.INTRO).assertDoesNotExist()
        composeRule.onNodeWithTag(CafePickerTestTags.WELCOME).assertIsDisplayed()
    }

    @Test
    fun withStoredCafeIntroIsFollowedByMenu() {
        cafes.selectedId.value = "krakow"
        setApp()

        finishIntroAndCrossfade()

        composeRule.onNodeWithTag(KafeIntroTestTags.INTRO).assertDoesNotExist()
        composeRule.onNodeWithTag(MenuTestTags.HEADER).assertIsDisplayed()
        composeRule.onNodeWithTag(MenuTestTags.product("espresso")).assertIsDisplayed()
    }

    @Test
    fun introStaysUntilAnimationEndsEvenWhenStartIsReady() {
        cafes.selectedId.value = "krakow"
        setApp()

        composeRule.mainClock.advanceTimeBy(800)

        composeRule.onNodeWithTag(KafeIntroTestTags.INTRO).assertIsDisplayed()
        composeRule.onNodeWithTag(MenuTestTags.HEADER).assertDoesNotExist()
    }

    @Test
    fun introStaysWhileStartDestinationIsUnknown() {
        cafes.selectedId.value = "krakow"
        cafes.blockSelected()
        setApp()

        composeRule.mainClock.advanceTimeBy(3_000)

        composeRule.onNodeWithTag(KafeIntroTestTags.INTRO).assertIsDisplayed()
        composeRule.onNodeWithTag(MenuTestTags.HEADER).assertDoesNotExist()

        cafes.unblockSelected()
        finishIntroAndCrossfade()

        composeRule.onNodeWithTag(MenuTestTags.HEADER).assertIsDisplayed()
    }
}

// Lokale z pamięci; observeSelectedCafe można zablokować, żeby AppStartViewModel został w Loading.
private class AppStartFakeCafeRepository(cafes: List<Cafe>) : CafeRepository {
    private val cafes = MutableStateFlow(cafes)
    val selectedId = MutableStateFlow<String?>(null)
    private var gate: CompletableDeferred<Unit>? = null

    fun blockSelected() {
        gate = CompletableDeferred()
    }

    fun unblockSelected() {
        gate?.complete(Unit)
    }

    override fun observeCafes(): Flow<List<Cafe>> = cafes

    override fun observeSelectedCafe(): Flow<Cafe?> = flow {
        gate?.await()
        combine(cafes, selectedId) { list, id -> list.firstOrNull { it.id == id } }.collect { emit(it) }
    }

    override suspend fun selectCafe(cafeId: String) {
        selectedId.value = cafeId
    }

    override suspend fun refresh(): EmptyResult<DataError> = Result.Success(Unit)
}
