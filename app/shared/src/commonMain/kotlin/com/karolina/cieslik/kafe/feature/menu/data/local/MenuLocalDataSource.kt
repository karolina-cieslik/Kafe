package com.karolina.cieslik.kafe.feature.menu.data.local

import com.karolina.cieslik.kafe.core.data.database.safeDbCall
import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.EmptyResult
import com.karolina.cieslik.kafe.core.domain.Money
import com.karolina.cieslik.kafe.feature.menu.data.remote.MenuDto
import com.karolina.cieslik.kafe.feature.menu.domain.Category
import com.karolina.cieslik.kafe.feature.menu.domain.Menu
import com.karolina.cieslik.kafe.feature.menu.domain.Product
import com.karolina.cieslik.kafe.feature.menu.domain.ProductSize
import com.karolina.cieslik.kafe.feature.menu.domain.SizeCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

// KAFE-6: Lokalne źródło menu (źródło prawdy offline-first). Interfejs pozwala testować repozytorium na JVM.
interface MenuLocalDataSource {

    /** null, gdy menu sieci lub dostępność w lokalu nie zostały jeszcze zapisane. */
    fun observeMenu(cafeId: String): Flow<Menu?>

    suspend fun saveMenu(
        cafeId: String,
        menu: MenuDto,
        availability: Map<String, Boolean>,
        syncedAtEpochMillis: Long,
    ): EmptyResult<DataError.Local>
}

class RoomMenuLocalDataSource(private val dao: MenuDao) : MenuLocalDataSource {

    override fun observeMenu(cafeId: String): Flow<Menu?> = combine(
        dao.observeSyncedCount(listOf(MenuSyncKeys.MENU, MenuSyncKeys.availability(cafeId))),
        dao.observeCategories(),
        dao.observeProducts(),
        dao.observeAvailability(cafeId),
    ) { syncedCount, categories, products, availability ->
        if (syncedCount < 2) {
            null
        } else {
            Menu(
                categories = categories.map { it.toDomain() },
                products = products.map { it.toDomain() },
                unavailableProductIds = availability.filterNot { it.isAvailable }.mapTo(mutableSetOf()) { it.productId },
            )
        }
    }.distinctUntilChanged()

    override suspend fun saveMenu(
        cafeId: String,
        menu: MenuDto,
        availability: Map<String, Boolean>,
        syncedAtEpochMillis: Long,
    ): EmptyResult<DataError.Local> = safeDbCall {
        dao.replaceMenuForCafe(
            cafeId = cafeId,
            categories = menu.categories.map { CategoryEntity(it.id, it.name, it.sortOrder) },
            products = menu.products.map {
                ProductEntity(
                    id = it.id,
                    categoryId = it.categoryId,
                    name = it.name,
                    description = it.description,
                    imageUrl = it.imageUrl,
                    sortOrder = it.sortOrder,
                    isActive = it.active,
                )
            },
            sizes = menu.products.flatMap { product ->
                // Duplikaty kodu w jednym produkcie złamałyby klucz główny – zostaje pierwszy wpis.
                product.sizes.distinctBy { it.code.uppercase() }.map {
                    ProductSizeEntity(product.id, it.code.uppercase(), it.volumeMl, it.priceGrosze)
                }
            },
            availability = availability.map { (productId, isAvailable) ->
                ProductAvailabilityEntity(cafeId = cafeId, productId = productId, isAvailable = isAvailable)
            },
            syncedAtEpochMillis = syncedAtEpochMillis,
        )
    }
}

internal fun CategoryEntity.toDomain() = Category(id = id, name = name, sortOrder = sortOrder)

internal fun ProductWithSizes.toDomain() = Product(
    id = product.id,
    categoryId = product.categoryId,
    name = product.name,
    description = product.description?.takeIf { it.isNotBlank() },
    imageUrl = product.imageUrl?.takeIf { it.isNotBlank() },
    sizes = sizes.mapNotNull { it.toDomainOrNull() }.sortedBy { it.code.ordinal },
    sortOrder = product.sortOrder,
    isActive = product.isActive,
)

/** Nieznane kody rozmiarów (np. z nowszej wersji API) i ujemne ceny są pomijane zamiast wywracać całe menu. */
internal fun ProductSizeEntity.toDomainOrNull(): ProductSize? {
    val sizeCode = SizeCode.entries.firstOrNull { it.name.equals(code, ignoreCase = true) } ?: return null
    if (priceGrosze < 0) return null
    return ProductSize(code = sizeCode, volumeMl = volumeMl, price = Money(priceGrosze))
}
