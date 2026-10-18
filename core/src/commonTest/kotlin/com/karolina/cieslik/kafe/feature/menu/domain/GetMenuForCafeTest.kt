package com.karolina.cieslik.kafe.feature.menu.domain

import app.cash.turbine.test
import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.EmptyResult
import com.karolina.cieslik.kafe.core.domain.Money
import com.karolina.cieslik.kafe.core.domain.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// KAFE-6: Reguły menu – kolejność (AC 3), puste kategorie (AC 4), „Sezonowe” (AC 5), dostępność per lokal, cena bazowa (AC 2).
class GetMenuForCafeTest {

    // --- AC 3: kolejność ustalona przez sieć ---

    @Test
    fun categoriesAreOrderedBySortOrderNotAlphabetically() {
        val menu = menu(
            categories = listOf(category("snacks", sortOrder = 4), category("classic", sortOrder = 1), category("brew", sortOrder = 2)),
            products = listOf(product("p1", "snacks"), product("p2", "classic"), product("p3", "brew")),
        )

        assertEquals(listOf("classic", "brew", "snacks"), menu.toCafeMenu().categoryIds())
    }

    @Test
    fun productsAreOrderedBySortOrderWithinCategory() {
        val menu = menu(
            categories = listOf(category("classic")),
            products = listOf(
                product("latte", "classic", sortOrder = 3),
                product("espresso", "classic", sortOrder = 1),
                product("cappuccino", "classic", sortOrder = 2),
            ),
        )

        assertEquals(listOf("espresso", "cappuccino", "latte"), menu.toCafeMenu().productIds("classic"))
    }

    @Test
    fun equalSortOrderKeepsSourceOrder() {
        val menu = menu(
            categories = listOf(category("b", sortOrder = 1), category("a", sortOrder = 1)),
            products = listOf(product("z", "b"), product("y", "b"), product("x", "a")),
        )

        val cafeMenu = menu.toCafeMenu()

        assertEquals(listOf("b", "a"), cafeMenu.categoryIds())
        assertEquals(listOf("z", "y"), cafeMenu.productIds("b"))
    }

    @Test
    fun productsAreGroupedIntoTheirOwnCategories() {
        val menu = menu(
            categories = listOf(category("classic", 1), category("snacks", 2)),
            products = listOf(product("croissant", "snacks"), product("espresso", "classic"), product("cookie", "snacks")),
        )

        val cafeMenu = menu.toCafeMenu()

        assertEquals(listOf("espresso"), cafeMenu.productIds("classic"))
        assertEquals(listOf("croissant", "cookie"), cafeMenu.productIds("snacks"))
    }

    // --- AC 4: kategoria bez produktów nie jest wyświetlana ---

    @Test
    fun categoryWithoutProductsIsHidden() {
        val menu = menu(
            categories = listOf(category("classic", 1), category("empty", 2)),
            products = listOf(product("espresso", "classic")),
        )

        assertEquals(listOf("classic"), menu.toCafeMenu().categoryIds())
    }

    @Test
    fun categoryWhoseProductsHaveNoSizesIsHidden() {
        val menu = menu(
            categories = listOf(category("classic", 1), category("broken", 2)),
            products = listOf(product("espresso", "classic"), product("noPrice", "broken", sizes = emptyList())),
        )

        assertEquals(listOf("classic"), menu.toCafeMenu().categoryIds())
    }

    @Test
    fun productWithoutSizesIsSkipped() {
        val menu = menu(
            categories = listOf(category("classic")),
            products = listOf(product("espresso", "classic"), product("noPrice", "classic", sizes = emptyList())),
        )

        assertEquals(listOf("espresso"), menu.toCafeMenu().productIds("classic"))
    }

    @Test
    fun productWithUnknownCategoryIsNotShown() {
        val menu = menu(
            categories = listOf(category("classic")),
            products = listOf(product("espresso", "classic"), product("orphan", "missing")),
        )

        val cafeMenu = menu.toCafeMenu()

        assertEquals(listOf("classic"), cafeMenu.categoryIds())
        assertEquals(listOf("espresso"), cafeMenu.productIds("classic"))
    }

    @Test
    fun emptyMenuGivesNoSections() {
        assertTrue(menu(categories = emptyList(), products = emptyList()).toCafeMenu().sections.isEmpty())
    }

    // --- AC 5: „Sezonowe” tylko z co najmniej jednym aktywnym produktem ---

    @Test
    fun seasonalCategoryWithOnlyInactiveProductsIsHidden() {
        val menu = menu(
            categories = listOf(category("classic", 1), category("seasonal", 3, name = "Sezonowe")),
            products = listOf(
                product("espresso", "classic"),
                product("pumpkinLatte", "seasonal", isActive = false),
                product("gingerbreadLatte", "seasonal", isActive = false),
            ),
        )

        assertEquals(listOf("classic"), menu.toCafeMenu().categoryIds())
    }

    @Test
    fun seasonalCategoryWithOneActiveProductIsShownWithOnlyActiveProducts() {
        val menu = menu(
            categories = listOf(category("seasonal", name = "Sezonowe")),
            products = listOf(
                product("pumpkinLatte", "seasonal", isActive = false),
                product("gingerbreadLatte", "seasonal", isActive = true),
            ),
        )

        assertEquals(listOf("gingerbreadLatte"), menu.toCafeMenu().productIds("seasonal"))
    }

    @Test
    fun inactiveProductIsHiddenInAnyCategory() {
        val menu = menu(
            categories = listOf(category("classic")),
            products = listOf(product("espresso", "classic"), product("retired", "classic", isActive = false)),
        )

        assertEquals(listOf("espresso"), menu.toCafeMenu().productIds("classic"))
    }

    // --- Dostępność per lokal ---

    @Test
    fun productUnavailableInCafeStaysVisibleAndIsMarkedUnavailable() {
        val menu = menu(
            categories = listOf(category("classic")),
            products = listOf(product("espresso", "classic", 1), product("flatWhite", "classic", 2)),
            unavailableProductIds = setOf("flatWhite"),
        )

        val items = menu.toCafeMenu().sections.single().items

        assertEquals(listOf("espresso", "flatWhite"), items.map { it.product.id })
        assertTrue(items[0].isAvailable)
        assertFalse(items[1].isAvailable)
    }

    @Test
    fun categoryWithOnlyUnavailableProductsIsStillShown() {
        val menu = menu(
            categories = listOf(category("classic", 1), category("brew", 2)),
            products = listOf(product("espresso", "classic"), product("v60", "brew"), product("chemex", "brew")),
            unavailableProductIds = setOf("v60", "chemex"),
        )

        val cafeMenu = menu.toCafeMenu()

        assertEquals(listOf("classic", "brew"), cafeMenu.categoryIds())
        assertTrue(cafeMenu.sections.last().items.none { it.isAvailable })
    }

    @Test
    fun unavailableIdOfInactiveProductDoesNotResurrectIt() {
        val menu = menu(
            categories = listOf(category("seasonal")),
            products = listOf(product("pumpkinLatte", "seasonal", isActive = false)),
            unavailableProductIds = setOf("pumpkinLatte"),
        )

        assertTrue(menu.toCafeMenu().sections.isEmpty())
    }

    @Test
    fun unknownUnavailableIdIsIgnored() {
        val menu = menu(
            categories = listOf(category("classic")),
            products = listOf(product("espresso", "classic")),
            unavailableProductIds = setOf("doesNotExist"),
        )

        assertTrue(menu.toCafeMenu().sections.single().items.single().isAvailable)
    }

    @Test
    fun sameMenuGivesDifferentAvailabilityPerCafe() = runTest {
        val categories = listOf(category("classic"))
        val products = listOf(product("espresso", "classic"), product("flatWhite", "classic", sortOrder = 1))
        val repository = FakeMenuRepository(
            menus = mapOf(
                "krakow" to menu(categories, products),
                "gdansk" to menu(categories, products, unavailableProductIds = setOf("flatWhite")),
            ),
        )
        val getMenu = GetMenuForCafe(repository)

        getMenu("krakow").test {
            assertTrue(awaitItem()!!.sections.single().items.all { it.isAvailable })
            cancelAndIgnoreRemainingEvents()
        }
        getMenu("gdansk").test {
            val items = awaitItem()!!.sections.single().items
            assertEquals(listOf(true, false), items.map { it.isAvailable })
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- Flow z repozytorium (AC 6–8, edge: produkt usunięty po odświeżeniu) ---

    @Test
    fun missingCachedMenuEmitsNull() = runTest {
        val getMenu = GetMenuForCafe(FakeMenuRepository(menus = emptyMap()))

        getMenu("krakow").test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun productRemovedAfterRefreshDisappearsFromMenu() = runTest {
        val categories = listOf(category("classic"))
        val repository = FakeMenuRepository(
            menus = mapOf("krakow" to menu(categories, listOf(product("espresso", "classic"), product("mocha", "classic", 1)))),
        )
        val getMenu = GetMenuForCafe(repository)

        getMenu("krakow").test {
            assertEquals(listOf("espresso", "mocha"), awaitItem()!!.productIds("classic"))

            repository.emit("krakow", menu(categories, listOf(product("espresso", "classic"))))

            assertEquals(listOf("espresso"), awaitItem()!!.productIds("classic"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- AC 2: cena bazowa ---

    @Test
    fun basePriceIsCheapestSizeRegardlessOfOrder() {
        val latte = product(
            "latte",
            "classic",
            sizes = listOf(size(SizeCode.L, 1_900), size(SizeCode.S, 1_300), size(SizeCode.M, 1_600)),
        )

        assertEquals(Money(1_300), latte.basePrice)
    }

    @Test
    fun basePriceOfSingleSizeProductIsItsPrice() {
        assertEquals(Money(900), product("croissant", "snacks", sizes = listOf(size(grosze = 900, volumeMl = null))).basePrice)
    }

    @Test
    fun basePriceIsNullWithoutSizes() {
        assertNull(product("broken", "classic", sizes = emptyList()).basePrice)
    }
}

private class FakeMenuRepository(menus: Map<String, Menu?>) : MenuRepository {
    private val flows = menus.mapValues { (_, menu) -> MutableStateFlow(menu) }.toMutableMap()

    fun emit(cafeId: String, menu: Menu?) {
        flows.getOrPut(cafeId) { MutableStateFlow(null) }.value = menu
    }

    override fun observeMenu(cafeId: String): Flow<Menu?> = flows.getOrPut(cafeId) { MutableStateFlow(null) }

    override suspend fun refresh(cafeId: String): EmptyResult<DataError> = Result.Success(Unit)
}
