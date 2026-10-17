package com.karolina.cieslik.kafe.feature.menu.data.local

import com.karolina.cieslik.kafe.core.data.database.safeDbCall
import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.EmptyResult
import com.karolina.cieslik.kafe.core.domain.Money
import com.karolina.cieslik.kafe.feature.menu.data.remote.AvailabilityDto
import com.karolina.cieslik.kafe.feature.menu.data.remote.MenuDto
import com.karolina.cieslik.kafe.feature.menu.domain.Category
import com.karolina.cieslik.kafe.feature.menu.domain.Menu
import com.karolina.cieslik.kafe.feature.menu.domain.ModifierGroup
import com.karolina.cieslik.kafe.feature.menu.domain.ModifierGroupType
import com.karolina.cieslik.kafe.feature.menu.domain.ModifierOption
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
        availability: AvailabilityDto,
        syncedAtEpochMillis: Long,
    ): EmptyResult<DataError.Local>
}

class RoomMenuLocalDataSource(private val dao: MenuDao) : MenuLocalDataSource {

    override fun observeMenu(cafeId: String): Flow<Menu?> {
        val network = combine(dao.observeCategories(), dao.observeProducts(), dao.observeModifierGroups(), ::Triple)
        val availability = combine(
            dao.observeAvailability(cafeId),
            dao.observeSizeAvailability(cafeId),
            dao.observeOptionAvailability(cafeId),
            ::CafeAvailabilityRows,
        )
        return combine(
            dao.observeSyncedCount(listOf(MenuSyncKeys.MENU, MenuSyncKeys.availability(cafeId))),
            network,
            availability,
        ) { syncedCount, (categories, products, groups), cafeAvailability ->
            if (syncedCount < 2) null else toMenu(categories, products, groups, cafeAvailability)
        }.distinctUntilChanged()
    }

    override suspend fun saveMenu(
        cafeId: String,
        menu: MenuDto,
        availability: AvailabilityDto,
        syncedAtEpochMillis: Long,
    ): EmptyResult<DataError.Local> = safeDbCall {
        dao.replaceMenuForCafe(cafeId, menu.toSnapshot(cafeId, availability), syncedAtEpochMillis)
    }
}

internal fun MenuDto.toSnapshot(cafeId: String, availability: AvailabilityDto): MenuSnapshot {
    // Duplikaty id złamałyby klucze główne – wszędzie zostaje pierwszy wpis.
    val groups = modifierGroups.distinctBy { it.id }
    return MenuSnapshot(
        categories = categories.map { CategoryEntity(it.id, it.name, it.sortOrder) },
        products = products.map {
            ProductEntity(
                id = it.id,
                categoryId = it.categoryId,
                name = it.name,
                description = it.description,
                imageUrl = it.imageUrl,
                sortOrder = it.sortOrder,
                isActive = it.active,
                defaultSizeCode = it.defaultSize?.uppercase(),
            )
        },
        sizes = products.flatMap { product ->
            product.sizes.distinctBy { it.code.uppercase() }.map {
                ProductSizeEntity(product.id, it.code.uppercase(), it.volumeMl, it.priceGrosze)
            }
        },
        modifierGroups = groups.map {
            ModifierGroupEntity(it.id, it.name, it.type.uppercase(), it.required, it.maxQuantity, it.defaultOptionId)
        },
        modifierOptions = groups.flatMap { group ->
            group.options.map { ModifierOptionEntity(it.id, group.id, it.name, it.surchargeGrosze, it.sortOrder) }
        }.distinctBy { it.id },
        productModifierGroups = products.flatMap { product ->
            product.modifierGroupIds.distinct().mapIndexed { index, groupId ->
                ProductModifierGroupEntity(product.id, groupId, index)
            }
        },
        availability = CafeAvailabilityRows(
            products = availability.products.map { (productId, isAvailable) ->
                ProductAvailabilityEntity(cafeId = cafeId, productId = productId, isAvailable = isAvailable)
            },
            sizes = availability.sizes.flatMap { (productId, sizes) ->
                sizes.entries.distinctBy { it.key.uppercase() }.map { (code, isAvailable) ->
                    SizeAvailabilityEntity(cafeId, productId, code.uppercase(), isAvailable)
                }
            },
            options = availability.modifierOptions.map { (optionId, isAvailable) ->
                ModifierOptionAvailabilityEntity(cafeId, optionId, isAvailable)
            },
        ),
    )
}

internal fun toMenu(
    categories: List<CategoryEntity>,
    products: List<ProductWithSizes>,
    groups: List<ModifierGroupWithOptions>,
    availability: CafeAvailabilityRows,
) = Menu(
    categories = categories.map { it.toDomain() },
    products = products.map { it.toDomain() },
    unavailableProductIds = availability.products.filterNot { it.isAvailable }.mapTo(mutableSetOf()) { it.productId },
    modifierGroups = groups.mapNotNull { it.toDomainOrNull() },
    unavailableSizes = availability.sizes
        .filterNot { it.isAvailable }
        .mapNotNull { entity -> entity.code.toSizeCodeOrNull()?.let { entity.productId to it } }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, codes) -> codes.toSet() },
    unavailableOptionIds = availability.options.filterNot { it.isAvailable }.mapTo(mutableSetOf()) { it.optionId },
)

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
    defaultSize = product.defaultSizeCode?.toSizeCodeOrNull(),
    modifierGroupIds = modifierGroups.sortedBy { it.sortOrder }.map { it.groupId },
)

/** Nieznane kody rozmiarów (np. z nowszej wersji API) i ujemne ceny są pomijane zamiast wywracać całe menu. */
internal fun ProductSizeEntity.toDomainOrNull(): ProductSize? {
    val sizeCode = code.toSizeCodeOrNull() ?: return null
    if (priceGrosze < 0) return null
    return ProductSize(code = sizeCode, volumeMl = volumeMl, price = Money(priceGrosze))
}

/** KAFE-10: grupa z nieznanym typem albo bez poprawnych opcji jest pomijana; ujemne dopłaty są odrzucane. */
internal fun ModifierGroupWithOptions.toDomainOrNull(): ModifierGroup? {
    val type = ModifierGroupType.entries.firstOrNull { it.name.equals(group.type, ignoreCase = true) } ?: return null
    val options = options
        .filter { it.surchargeGrosze >= 0 }
        .sortedBy { it.sortOrder }
        .map { ModifierOption(id = it.id, name = it.name, surcharge = Money(it.surchargeGrosze)) }
        .ifEmpty { return null }
    return ModifierGroup(
        id = group.id,
        name = group.name,
        type = type,
        isRequired = group.isRequired,
        maxQuantity = if (type == ModifierGroupType.SINGLE) 1 else group.maxQuantity.coerceAtLeast(1),
        options = options,
        defaultOptionId = group.defaultOptionId?.takeIf { id -> options.any { it.id == id } },
    )
}

private fun String.toSizeCodeOrNull(): SizeCode? = SizeCode.entries.firstOrNull { it.name.equals(this, ignoreCase = true) }
