package com.karolina.cieslik.kafe

import android.content.Context
import android.provider.Settings
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.EmptyResult
import com.karolina.cieslik.kafe.core.domain.Money
import com.karolina.cieslik.kafe.core.domain.Result
import com.karolina.cieslik.kafe.di.IoDispatcher
import com.karolina.cieslik.kafe.feature.cafe.domain.Cafe
import com.karolina.cieslik.kafe.feature.cafe.domain.CafeRepository
import com.karolina.cieslik.kafe.feature.cafe.presentation.CafePickerTestTags
import com.karolina.cieslik.kafe.feature.menu.domain.Category
import com.karolina.cieslik.kafe.feature.menu.domain.Menu
import com.karolina.cieslik.kafe.feature.menu.domain.MenuRepository
import com.karolina.cieslik.kafe.feature.menu.domain.Product
import com.karolina.cieslik.kafe.feature.menu.domain.ProductSize
import com.karolina.cieslik.kafe.feature.menu.domain.SizeCode
import com.karolina.cieslik.kafe.feature.menu.presentation.MenuTestTags
import com.karolina.cieslik.kafe.feature.start.presentation.KafeIntroTestTags
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.annotation.Config

/** Po odtworzeniu MainActivity (np. po zmianie motywu w tle) od pierwszej klatki widoczny jest ekran docelowy, a nie intro. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp")
class MainActivityRecreationTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val krakow = Cafe(
        id = "krakow",
        name = "Kafe Rynek",
        address = "Rynek Główny 1",
        lat = 0.0,
        lng = 0.0,
        openingHours = emptyList(),
    )
    private val cafes = RecreationFakeCafeRepository(listOf(krakow))
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        val resolver = ApplicationProvider.getApplicationContext<Context>().contentResolver
        Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        loadKoinModules(
            module {
                single<CoroutineDispatcher>(IoDispatcher) { Dispatchers.Unconfined }
                single<CafeRepository> { cafes }
                single<MenuRepository> { RecreationFakeMenuRepository(sampleMenu()) }
            },
        )
    }

    @After
    fun tearDown() {
        scenario?.close()
        stopKoin()
    }

    @Test
    fun menuIsShownAfterRecreationInForeground() {
        cafes.selectedId.value = "krakow"
        launchAndFinishIntro()
        composeRule.onNodeWithTag(MenuTestTags.HEADER).assertIsDisplayed()

        scenario!!.recreate()

        assertTargetScreenInFirstFrame(MenuTestTags.HEADER)
    }

    @Test
    fun menuIsShownAfterRecreationReturningFromBackground() {
        cafes.selectedId.value = "krakow"
        launchAndFinishIntro()

        recreateAfterReturningFromBackground()

        assertTargetScreenInFirstFrame(MenuTestTags.HEADER)
    }

    @Test
    fun cafePickerIsShownAfterRecreationReturningFromBackground() {
        cafes.selectedId.value = null
        launchAndFinishIntro()
        composeRule.onNodeWithTag(CafePickerTestTags.WELCOME).assertIsDisplayed()

        recreateAfterReturningFromBackground()

        assertTargetScreenInFirstFrame(CafePickerTestTags.WELCOME)
    }

    @Test
    fun menuIsShownAfterSecondRecreation() {
        cafes.selectedId.value = "krakow"
        launchAndFinishIntro()

        recreateAfterReturningFromBackground()
        recreateAfterReturningFromBackground()

        assertTargetScreenInFirstFrame(MenuTestTags.HEADER)
    }

    private fun launchAndFinishIntro() {
        composeRule.mainClock.autoAdvance = false
        scenario = ActivityScenario.launch(MainActivity::class.java)
        composeRule.onNodeWithTag(KafeIntroTestTags.INTRO).assertIsDisplayed()
        composeRule.mainClock.advanceTimeBy(CROSSFADE_MS)
        composeRule.onNodeWithTag(KafeIntroTestTags.INTRO).assertDoesNotExist()
    }

    private fun recreateAfterReturningFromBackground() {
        scenario!!.moveToState(Lifecycle.State.CREATED)
        scenario!!.moveToState(Lifecycle.State.RESUMED)
        scenario!!.recreate()
    }

    private fun assertTargetScreenInFirstFrame(targetTag: String) {
        composeRule.onNodeWithTag(KafeIntroTestTags.INTRO).assertDoesNotExist()
        composeRule.onNodeWithTag(targetTag).assertIsDisplayed()
    }

    private fun sampleMenu() = Menu(
        categories = listOf(Category(id = "classic", name = "Klasyki", sortOrder = 1)),
        products = listOf(
            Product(
                id = "espresso",
                categoryId = "classic",
                name = "Espresso",
                description = null,
                imageUrl = null,
                sizes = listOf(ProductSize(code = SizeCode.S, volumeMl = 30, price = Money(900))),
                sortOrder = 1,
                isActive = true,
            ),
        ),
        unavailableProductIds = emptySet(),
    )

    private companion object {
        const val CROSSFADE_MS = 400L
    }
}

/** Lokale z pamięci; wybrany lokal ustawia test przez [selectedId]. */
private class RecreationFakeCafeRepository(private val cafes: List<Cafe>) : CafeRepository {
    val selectedId = MutableStateFlow<String?>(null)

    override fun observeCafes(): Flow<List<Cafe>> = flowOf(cafes)

    override fun observeSelectedCafe(): Flow<Cafe?> = selectedId.map { id -> cafes.firstOrNull { it.id == id } }

    override suspend fun selectCafe(cafeId: String) {
        selectedId.value = cafeId
    }

    override suspend fun refresh(): EmptyResult<DataError> = Result.Success(Unit)
}

/** To samo zapamiętane menu dla każdego lokalu, odświeżenie zawsze udane. */
private class RecreationFakeMenuRepository(private val menu: Menu) : MenuRepository {

    override fun observeMenu(cafeId: String): Flow<Menu?> = flowOf(menu)

    override suspend fun refresh(cafeId: String): EmptyResult<DataError> = Result.Success(Unit)
}
