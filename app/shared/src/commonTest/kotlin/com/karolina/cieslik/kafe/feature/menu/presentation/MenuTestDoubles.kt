package com.karolina.cieslik.kafe.feature.menu.presentation

import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.EmptyResult
import com.karolina.cieslik.kafe.core.domain.Money
import com.karolina.cieslik.kafe.core.domain.Result
import com.karolina.cieslik.kafe.feature.cafe.domain.Cafe
import com.karolina.cieslik.kafe.feature.cafe.domain.CafeRepository
import com.karolina.cieslik.kafe.feature.menu.domain.Category
import com.karolina.cieslik.kafe.feature.menu.domain.Menu
import com.karolina.cieslik.kafe.feature.menu.domain.MenuRepository
import com.karolina.cieslik.kafe.feature.menu.domain.Product
import com.karolina.cieslik.kafe.feature.menu.domain.ProductSize
import com.karolina.cieslik.kafe.feature.menu.domain.SizeCode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

// KAFE-6: Fake'i i buildery do testów warstwy prezentacji (MenuViewModel, nawigacja, ekrany).

/** Lokal testowy bez godzin otwarcia; nazwa domyślnie „Kafe <id>”. */
internal fun cafe(id: String, name: String = "Kafe $id") =
    Cafe(id = id, name = name, address = "ul. Testowa 1", lat = 0.0, lng = 0.0, openingHours = emptyList())

/** Kategoria menu; [sortOrder] decyduje o kolejności (AC 3 KAFE-6), nazwa domyślnie równa id. */
internal fun category(id: String, sortOrder: Int = 0, name: String = id) =
    Category(id = id, name = name, sortOrder = sortOrder)

/** Aktywny produkt z jednym rozmiarem S (250 ml) w pełnej cenie [priceGrosze] (domyślnie 12,00 zł). */
internal fun product(
    id: String,
    categoryId: String,
    sortOrder: Int = 0,
    name: String = id,
    priceGrosze: Long = 1_200,
    description: String? = null,
) = Product(
    id = id,
    categoryId = categoryId,
    name = name,
    description = description,
    imageUrl = null,
    sizes = listOf(ProductSize(code = SizeCode.S, volumeMl = 250, price = Money(priceGrosze))),
    sortOrder = sortOrder,
    isActive = true,
)

/** Menu sieci w kontekście lokalu; [unavailableProductIds] – produkty niedostępne w tym lokalu. */
internal fun menu(
    categories: List<Category>,
    products: List<Product>,
    unavailableProductIds: Set<String> = emptySet(),
) = Menu(categories = categories, products = products, unavailableProductIds = unavailableProductIds)

/**
 * Repozytorium z jednym lokalem: [selectedCafe] to zarazem wybrany lokal i cała lista (pusta przy `null`).
 * Zmiana [selectedCafe] w teście symuluje zmianę lokalu; `refresh()` zawsze kończy się sukcesem bez zmian.
 */
internal class FakeCafeRepository(selected: Cafe?) : CafeRepository {
    val selectedCafe = MutableStateFlow(selected)

    override fun observeCafes(): Flow<List<Cafe>> = selectedCafe.map { listOfNotNull(it) }

    override fun observeSelectedCafe(): Flow<Cafe?> = selectedCafe

    override suspend fun selectCafe(cafeId: String): EmptyResult<DataError.Local> {
        selectedCafe.value = cafe(cafeId)
        return Result.Success(Unit)
    }

    override suspend fun refresh(): EmptyResult<DataError> = Result.Success(Unit)
}

/**
 * Repozytorium menu sterowane z testu (offline-first jak OfflineFirstMenuRepository, bez sieci).
 *
 * `observeMenu(cafeId)` to gorący StateFlow per lokal: emituje od razu zapamiętane menu (`null` = brak cache),
 * a potem każdą zmianę z `refresh()` lub [setCached].
 *
 * @param cached zapamiętane menu per lokal na start.
 * @property menuAfterRefresh menu zapisywane do cache po udanym `refresh()`.
 * @property refreshResult wynik każdego `refresh()` (np. `Result.Failure(DataError.Remote.NO_INTERNET)`).
 * @property refreshGate gdy ustawiony, `refresh()` czeka na `complete()` – pozwala obserwować stan „w toku”.
 * @property refreshCalls id lokali w kolejności wywołań `refresh()`.
 */
internal class FakeMenuRepository(
    cached: Map<String, Menu?> = emptyMap(),
    var menuAfterRefresh: Map<String, Menu> = emptyMap(),
    var refreshResult: EmptyResult<DataError> = Result.Success(Unit),
) : MenuRepository {
    private val flows = cached.mapValues { (_, menu) -> MutableStateFlow(menu) }.toMutableMap()
    val refreshCalls = mutableListOf<String>()
    var refreshGate: CompletableDeferred<Unit>? = null

    private fun flowFor(cafeId: String) = flows.getOrPut(cafeId) { MutableStateFlow(null) }

    /** Podmienia zapamiętane menu lokalu – symuluje zmianę w bazie (np. odświeżenie z innego ekranu). */
    fun setCached(cafeId: String, menu: Menu?) {
        flowFor(cafeId).value = menu
    }

    override fun observeMenu(cafeId: String): Flow<Menu?> = flowFor(cafeId)

    override suspend fun refresh(cafeId: String): EmptyResult<DataError> {
        refreshCalls += cafeId
        refreshGate?.await()
        val result = refreshResult
        if (result is Result.Success) menuAfterRefresh[cafeId]?.let { flowFor(cafeId).value = it }
        return result
    }
}
