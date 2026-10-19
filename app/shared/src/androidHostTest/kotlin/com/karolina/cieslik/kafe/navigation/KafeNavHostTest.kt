package com.karolina.cieslik.kafe.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.EmptyResult
import com.karolina.cieslik.kafe.core.domain.Result
import com.karolina.cieslik.kafe.core.presentation.designsystem.theme.KafeTheme
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
import com.karolina.cieslik.kafe.feature.product.domain.GetProductDetails
import com.karolina.cieslik.kafe.feature.product.domain.PriceCalculator
import com.karolina.cieslik.kafe.feature.product.presentation.ProductDetailTestTags
import com.karolina.cieslik.kafe.feature.product.presentation.ProductDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

/** Nawigacja na prawdziwych ekranach: Menu → ProductDetail(productId), Menu → CafePicker, powrót z zachowanym stanem i wybór lokalu (z menu i przy pierwszym uruchomieniu). */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [ROBOLECTRIC_SDK], qualifiers = "w411dp-h891dp")
class KafeNavHostTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var navController: NavHostController

    private val krakow = cafe("krakow", name = "Kafe Rynek")
    private val gdansk = cafe("gdansk", name = "Kafe Długa")
    private val sampleMenu = menu(
        categories = listOf(category("classic", 1, "Kawy klasyczne"), category("snacks", 2, "Przekąski")),
        products = listOf(
            product("espresso", "classic", 1, "Espresso"),
            product("croissant", "snacks", 1, "Croissant maślany"),
        ),
    )
    private val cafes = NavFakeCafeRepository(listOf(krakow, gdansk))

    /** Poniedziałek 10:00 czasu warszawskiego – stały zegar dla „otwarte teraz”. */
    private val fixedClock = object : Clock {
        override fun now() = Instant.parse("2026-10-19T08:00:00Z")
    }

    @Before
    fun startDi() {
        val menus = FakeMenuRepository(cached = mapOf("krakow" to sampleMenu, "gdansk" to sampleMenu))
        startKoin {
            modules(
                module {
                    viewModel { MenuViewModel(cafes, menus, GetMenuForCafe(menus), Dispatchers.Main) }
                    viewModel { (productId: String) ->
                        ProductDetailViewModel(
                            productId = productId,
                            cafeRepository = cafes,
                            menuRepository = menus,
                            getProductDetails = GetProductDetails(menus),
                            priceCalculator = PriceCalculator(),
                            dispatcher = Dispatchers.Main,
                        )
                    }
                    viewModel { CafePickerViewModel(cafes, fixedClock, TimeZone.of("Europe/Warsaw"), Dispatchers.Main) }
                },
            )
        }
    }

    @After
    fun stopDi() {
        stopKoin()
    }

    private fun setNavHost(startDestination: KafeRoute = KafeRoute.Menu, selectedCafeId: String? = "krakow") {
        cafes.selectedId.value = selectedCafeId
        composeRule.setContent {
            KafeTheme(darkTheme = false) {
                navController = rememberNavController()
                KafeNavHost(navController = navController, startDestination = startDestination)
            }
        }
        composeRule.waitForIdle()
    }

    private inline fun <reified T : Any> assertCurrentRoute() {
        val destination = navController.currentBackStackEntry?.destination
        assertTrue("Oczekiwana trasa ${T::class.simpleName}, jest ${destination?.route}", destination?.hasRoute<T>() == true)
    }

    private fun pressSystemBack() {
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
    }

    private fun click(tag: String) {
        composeRule.onNodeWithTag(tag).performClick()
        composeRule.waitForIdle()
    }

    private fun openCafePickerFromMenu() {
        composeRule.onNode(hasClickLabel("Zmień kawiarnię")).performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun startsOnMenu() {
        setNavHost()

        assertCurrentRoute<KafeRoute.Menu>()
        composeRule.onNodeWithTag(MenuTestTags.product("espresso")).assertIsDisplayed()
    }

    @Test
    fun tapOnProductOpensProductDetailWithItsId() {
        setNavHost()

        click(MenuTestTags.product("espresso"))

        assertCurrentRoute<KafeRoute.ProductDetail>()
        assertEquals("espresso", navController.currentBackStackEntry!!.toRoute<KafeRoute.ProductDetail>().productId)
        composeRule.onNodeWithTag(ProductDetailTestTags.CONTENT).assertIsDisplayed()
        composeRule.onNodeWithText("Espresso").assertIsDisplayed()
    }

    @Test
    fun backArrowFromProductDetailReturnsToMenuWithPreservedState() {
        setNavHost()
        click(MenuTestTags.category("snacks"))
        click(MenuTestTags.product("croissant"))

        composeRule.onNodeWithContentDescription("Wstecz").performClick()
        composeRule.waitForIdle()

        assertCurrentRoute<KafeRoute.Menu>()
        // Stan VM menu (wybrana kategoria) przetrwał nawigację.
        composeRule.onNodeWithTag(MenuTestTags.product("croissant")).assertIsDisplayed()
        composeRule.onNodeWithTag(MenuTestTags.product("espresso")).assertDoesNotExist()
    }

    @Test
    fun systemBackFromProductDetailReturnsToSingleMenuEntry() {
        setNavHost()
        click(MenuTestTags.product("espresso"))

        pressSystemBack()

        assertCurrentRoute<KafeRoute.Menu>()
        assertNull(navController.previousBackStackEntry)
        composeRule.onNodeWithTag(MenuTestTags.GRID).assertIsDisplayed()
    }

    @Test
    fun changeCafeOpensCafePickerWithBackArrow() {
        setNavHost()

        openCafePickerFromMenu()

        assertCurrentRoute<KafeRoute.CafePicker>()
        composeRule.onNodeWithTag(CafePickerTestTags.cafe("gdansk")).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Wstecz").assertIsDisplayed()
    }

    @Test
    fun systemBackFromCafePickerReturnsToMenuWithoutChangingCafe() {
        setNavHost()
        openCafePickerFromMenu()

        pressSystemBack()

        assertCurrentRoute<KafeRoute.Menu>()
        composeRule.onNodeWithText("Kafe Rynek").assertIsDisplayed()
        assertEquals("krakow", cafes.selectedId.value)
    }

    @Test
    fun choosingCafeFromMenuReturnsToMenuOfNewCafe() {
        setNavHost()
        openCafePickerFromMenu()

        click(CafePickerTestTags.cafe("gdansk"))

        assertCurrentRoute<KafeRoute.Menu>()
        assertNull(navController.previousBackStackEntry)
        assertEquals("gdansk", cafes.selectedId.value)
        composeRule.onNodeWithText("Kafe Długa").assertIsDisplayed()
    }

    @Test
    fun firstLaunchStartsOnCafePickerWithWelcomeAndNoBackArrow() {
        setNavHost(startDestination = KafeRoute.CafePicker, selectedCafeId = null)

        assertCurrentRoute<KafeRoute.CafePicker>()
        composeRule.onNodeWithTag(CafePickerTestTags.WELCOME).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Wstecz").assertDoesNotExist()
    }

    @Test
    fun choosingCafeOnFirstLaunchOpensMenuWithoutPickerOnBackStack() {
        setNavHost(startDestination = KafeRoute.CafePicker, selectedCafeId = null)

        click(CafePickerTestTags.cafe("krakow"))

        assertCurrentRoute<KafeRoute.Menu>()
        assertNull("Picker nie powinien zostać pod menu", navController.previousBackStackEntry)
        composeRule.onNodeWithText("Kafe Rynek").assertIsDisplayed()
        assertEquals("krakow", cafes.selectedId.value)
    }

    private fun hasClickLabel(label: String) = SemanticsMatcher("onClickLabel = $label") { node ->
        node.config.getOrElseNullable(SemanticsActions.OnClick) { null }?.label == label
    }
}

/** Kilka lokali i zapamiętany wybór (`null` = pierwsze uruchomienie), jak `OfflineFirstCafeRepository` bez sieci. */
private class NavFakeCafeRepository(cafes: List<Cafe>) : CafeRepository {
    private val cafes = MutableStateFlow(cafes)
    val selectedId = MutableStateFlow<String?>(null)

    override fun observeCafes(): Flow<List<Cafe>> = cafes

    override fun observeSelectedCafe(): Flow<Cafe?> =
        combine(cafes, selectedId) { list, id -> list.firstOrNull { it.id == id } }

    override suspend fun selectCafe(cafeId: String) {
        selectedId.value = cafeId
    }

    override suspend fun refresh(): EmptyResult<DataError> = Result.Success(Unit)
}
