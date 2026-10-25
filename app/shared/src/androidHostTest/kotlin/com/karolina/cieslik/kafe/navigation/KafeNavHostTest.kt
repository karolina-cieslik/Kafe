package com.karolina.cieslik.kafe.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDisplayed
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
import com.karolina.cieslik.kafe.core.domain.Money
import com.karolina.cieslik.kafe.core.domain.Result
import com.karolina.cieslik.kafe.core.presentation.designsystem.theme.KafeTheme
import com.karolina.cieslik.kafe.core.testing.ROBOLECTRIC_SDK
import com.karolina.cieslik.kafe.feature.cafe.domain.Cafe
import com.karolina.cieslik.kafe.feature.cafe.domain.CafeRepository
import com.karolina.cieslik.kafe.feature.cafe.presentation.CafePickerTestTags
import com.karolina.cieslik.kafe.feature.cafe.presentation.CafePickerViewModel
import com.karolina.cieslik.kafe.feature.cart.FakeCartRepository
import com.karolina.cieslik.kafe.feature.cart.domain.AddToCart
import com.karolina.cieslik.kafe.feature.cart.domain.Cart
import com.karolina.cieslik.kafe.feature.cart.domain.CartItem
import com.karolina.cieslik.kafe.feature.cart.domain.ObserveCheckedCart
import com.karolina.cieslik.kafe.feature.cart.domain.RefreshCartSnapshots
import com.karolina.cieslik.kafe.feature.cart.presentation.CartViewModel
import com.karolina.cieslik.kafe.feature.menu.domain.GetMenuForCafe
import com.karolina.cieslik.kafe.feature.menu.domain.SizeCode
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
    private val cart = FakeCartRepository()

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
                    viewModel { MenuViewModel(cafes, menus, GetMenuForCafe(menus), FakeCartRepository()) }
                    viewModel { (productId: String) ->
                        ProductDetailViewModel(
                            productId = productId,
                            cafeRepository = cafes,
                            menuRepository = menus,
                            getProductDetails = GetProductDetails(menus),
                            priceCalculator = PriceCalculator(),
                            addToCart = AddToCart(FakeCartRepository(), PriceCalculator()),
                        )
                    }
                    viewModel { CafePickerViewModel(cafes, fixedClock, TimeZone.of("Europe/Warsaw")) }
                    viewModel {
                        CartViewModel(
                            cafeRepository = cafes,
                            menuRepository = menus,
                            cartRepository = cart,
                            observeCheckedCart = ObserveCheckedCart(cart, menus, PriceCalculator()),
                            refreshCartSnapshots = RefreshCartSnapshots(cart, menus, PriceCalculator()),
                        )
                    }
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

    /**
     * Czeka, aż tekst będzie widoczny. Snackbar pokazuje się w osobnej korutynie po odczycie tekstu przez suspend
     * `getString`, więc `waitForIdle()` po kliknięciu nie gwarantuje, że węzeł jest już ułożony – na wolniejszym CI
     * istniał, ale nie był jeszcze „displayed”.
     */
    private fun waitUntilDisplayed(text: String) {
        composeRule.waitUntil(conditionDescription = "widoczny tekst „$text”", timeoutMillis = SNACKBAR_TIMEOUT_MS) {
            composeRule.onNodeWithText(text).isDisplayed()
        }
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
    fun failedCafeSelectionShowsSnackbarAndStaysOnCafePicker() {
        setNavHost()
        openCafePickerFromMenu()
        cafes.selectError = DataError.Local.DISK_FULL

        click(CafePickerTestTags.cafe("gdansk"))

        assertCurrentRoute<KafeRoute.CafePicker>()
        waitUntilDisplayed("Nie udało się zapisać wyboru lokalu. Spróbuj ponownie.")
        assertEquals("krakow", cafes.selectedId.value)
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

    @Test
    fun firstLaunchOfflineWithoutCafesShowsErrorWithWelcomeAndNoBackArrow() {
        cafes.cafeList.value = emptyList()
        cafes.refreshResult = Result.Failure(DataError.Remote.NO_INTERNET)

        setNavHost(startDestination = KafeRoute.CafePicker, selectedCafeId = null)

        composeRule.onNodeWithText("Spróbuj ponownie").assertIsDisplayed()
        composeRule.onNodeWithTag(CafePickerTestTags.WELCOME).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Wstecz").assertDoesNotExist()
        assertCurrentRoute<KafeRoute.CafePicker>()
    }

    private fun openCart() {
        composeRule.runOnUiThread { navController.navigate(KafeRoute.Cart) { launchSingleTop = true } }
        composeRule.waitForIdle()
    }

    @Test
    fun cartOpenedFromMenuShowsEmptyCartAndBackArrowReturnsToMenu() {
        setNavHost()

        openCart()

        assertCurrentRoute<KafeRoute.Cart>()
        composeRule.onNodeWithText("Twój koszyk jest pusty").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Wstecz").performClick()
        composeRule.waitForIdle()
        assertCurrentRoute<KafeRoute.Menu>()
        assertNull(navController.previousBackStackEntry)
    }

    @Test
    fun systemBackFromCartOpenedOverProductReturnsToProduct() {
        setNavHost()
        click(MenuTestTags.product("espresso"))
        openCart()

        pressSystemBack()

        assertCurrentRoute<KafeRoute.ProductDetail>()
    }

    @Test
    fun goToMenuFromCartOpenedOverProductPopsBackToSingleMenuEntry() {
        setNavHost()
        click(MenuTestTags.product("espresso"))
        openCart()

        composeRule.onNodeWithText("Przejdź do menu").performClick()
        composeRule.waitForIdle()

        assertCurrentRoute<KafeRoute.Menu>()
        assertNull(navController.previousBackStackEntry)
    }

    @Test
    fun openingCartTwiceKeepsSingleCartEntry() {
        setNavHost()

        openCart()
        openCart()
        composeRule.onNodeWithContentDescription("Wstecz").performClick()
        composeRule.waitForIdle()

        assertCurrentRoute<KafeRoute.Menu>()
    }

    @Test
    fun removingItemInCartShowsUndoAndUndoRestoresIt() {
        cart.cart.value = Cart(listOf(CartItem(1, "espresso", "Espresso", SizeCode.S, emptyList(), 1, Money(1_200))))
        setNavHost()
        openCart()

        composeRule.onNodeWithContentDescription("Usuń: Espresso, Mały").performClick()
        waitUntilDisplayed("Usunięto: Espresso")
        composeRule.onNodeWithText("Twój koszyk jest pusty").assertIsDisplayed()
        composeRule.onNodeWithText("Cofnij").performClick()

        waitUntilDisplayed("Espresso")
        assertEquals(listOf(1L), cart.cart.value.items.map { it.id })
    }

    private fun hasClickLabel(label: String) = SemanticsMatcher("onClickLabel = $label") { node ->
        node.config.getOrElseNullable(SemanticsActions.OnClick) { null }?.label == label
    }
}

/** Limit czekania na snackbar; z zapasem dla wolnych maszyn CI, w praktyce pojawia się po kilku klatkach. */
private const val SNACKBAR_TIMEOUT_MS = 5_000L

/** Kilka lokali i zapamiętany wybór (`null` = pierwsze uruchomienie), jak `OfflineFirstCafeRepository` bez sieci. */
private class NavFakeCafeRepository(cafes: List<Cafe>) : CafeRepository {
    val cafeList = MutableStateFlow(cafes)
    val selectedId = MutableStateFlow<String?>(null)
    var selectError: DataError.Local? = null
    var refreshResult: EmptyResult<DataError> = Result.Success(Unit)

    override fun observeCafes(): Flow<List<Cafe>> = cafeList

    override fun observeSelectedCafe(): Flow<Cafe?> =
        combine(cafeList, selectedId) { list, id -> list.firstOrNull { it.id == id } }

    override suspend fun selectCafe(cafeId: String): EmptyResult<DataError.Local> {
        selectError?.let { return Result.Failure(it) }
        selectedId.value = cafeId
        return Result.Success(Unit)
    }

    override suspend fun refresh(): EmptyResult<DataError> = refreshResult
}
