package com.karolina.cieslik.kafe.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.karolina.cieslik.kafe.core.presentation.designsystem.theme.KafeTheme
import com.karolina.cieslik.kafe.core.testing.ROBOLECTRIC_SDK
import com.karolina.cieslik.kafe.feature.menu.domain.GetMenuForCafe
import com.karolina.cieslik.kafe.feature.menu.presentation.FakeCafeRepository
import com.karolina.cieslik.kafe.feature.menu.presentation.FakeMenuRepository
import com.karolina.cieslik.kafe.feature.menu.presentation.MenuTestTags
import com.karolina.cieslik.kafe.feature.menu.presentation.MenuViewModel
import com.karolina.cieslik.kafe.feature.menu.presentation.cafe
import com.karolina.cieslik.kafe.feature.menu.presentation.category
import com.karolina.cieslik.kafe.feature.menu.presentation.menu
import com.karolina.cieslik.kafe.feature.menu.presentation.product
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
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

// KAFE-41: Nawigacja Menu -> ProductDetail(productId) i Menu -> CafePicker oraz powrót z zachowanym stanem menu (Robolectric).
@RunWith(AndroidJUnit4::class)
@Config(sdk = [ROBOLECTRIC_SDK], qualifiers = "w411dp-h891dp")
class KafeNavHostTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var navController: NavHostController

    private val sampleMenu = menu(
        categories = listOf(category("classic", 1, "Kawy klasyczne"), category("snacks", 2, "Przekąski")),
        products = listOf(
            product("espresso", "classic", 1, "Espresso"),
            product("croissant", "snacks", 1, "Croissant maślany"),
        ),
    )

    @Before
    fun startDi() {
        val cafes = FakeCafeRepository(cafe("krakow", name = "Kafe Rynek"))
        val menus = FakeMenuRepository(cached = mapOf("krakow" to sampleMenu))
        startKoin {
            modules(
                module {
                    viewModel { MenuViewModel(cafes, menus, GetMenuForCafe(menus), Dispatchers.Main) }
                },
            )
        }
    }

    @After
    fun stopDi() {
        stopKoin()
    }

    private fun setNavHost(startDestination: KafeRoute = KafeRoute.Menu) {
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

    @Test
    fun startsOnMenu() {
        setNavHost()

        assertCurrentRoute<KafeRoute.Menu>()
        composeRule.onNodeWithTag(MenuTestTags.product("espresso")).assertIsDisplayed()
    }

    @Test
    fun tapOnProductOpensProductDetailWithItsId() {
        setNavHost()

        composeRule.onNodeWithTag(MenuTestTags.product("espresso")).performClick()
        composeRule.waitForIdle()

        assertCurrentRoute<KafeRoute.ProductDetail>()
        assertEquals("espresso", navController.currentBackStackEntry!!.toRoute<KafeRoute.ProductDetail>().productId)
        composeRule.onNodeWithText("Produkt: espresso").assertIsDisplayed()
    }

    @Test
    fun changeCafeOpensCafePicker() {
        setNavHost()

        composeRule.onNode(hasClickLabel("Zmień kawiarnię")).performClick()
        composeRule.waitForIdle()

        assertCurrentRoute<KafeRoute.CafePicker>()
        composeRule.onNodeWithText("Wybór lokalu").assertIsDisplayed()
    }

    @Test
    fun backButtonFromProductDetailReturnsToMenuWithPreservedState() {
        setNavHost()
        composeRule.onNodeWithTag(MenuTestTags.category("snacks")).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(MenuTestTags.product("croissant")).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Wróć").performClick()
        composeRule.waitForIdle()

        assertCurrentRoute<KafeRoute.Menu>()
        // Stan VM (wybrana kategoria) przetrwał nawigację.
        composeRule.onNodeWithTag(MenuTestTags.product("croissant")).assertIsDisplayed()
        composeRule.onNodeWithTag(MenuTestTags.product("espresso")).assertDoesNotExist()
    }

    @Test
    fun systemBackFromProductDetailReturnsToMenu() {
        setNavHost()
        composeRule.onNodeWithTag(MenuTestTags.product("espresso")).performClick()
        composeRule.waitForIdle()

        pressSystemBack()

        assertCurrentRoute<KafeRoute.Menu>()
        composeRule.onNodeWithTag(MenuTestTags.GRID).assertIsDisplayed()
    }

    @Test
    fun systemBackFromCafePickerReturnsToMenu() {
        setNavHost()
        composeRule.onNode(hasClickLabel("Zmień kawiarnię")).performClick()
        composeRule.waitForIdle()

        pressSystemBack()

        assertCurrentRoute<KafeRoute.Menu>()
        composeRule.onNodeWithText("Kafe Rynek").assertIsDisplayed()
    }

    @Test
    fun productDetailIsOpenedOnceAndBackLeavesSingleMenuEntry() {
        setNavHost()
        composeRule.onNodeWithTag(MenuTestTags.product("espresso")).performClick()
        composeRule.waitForIdle()

        pressSystemBack()

        assertCurrentRoute<KafeRoute.Menu>()
        assertEquals(null, navController.previousBackStackEntry)
    }

    // KAFE-31: pierwsze uruchomienie bez wybranego lokalu zaczyna od wyboru lokalu.
    @Test
    fun cafePickerAsStartDestinationIsShownFirst() {
        setNavHost(startDestination = KafeRoute.CafePicker)

        assertCurrentRoute<KafeRoute.CafePicker>()
        composeRule.onNodeWithText("Wybór lokalu").assertIsDisplayed()
    }

    private fun hasClickLabel(label: String) = SemanticsMatcher("onClickLabel = $label") { node ->
        node.config.getOrElseNullable(SemanticsActions.OnClick) { null }?.label == label
    }
}
