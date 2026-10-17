package com.karolina.cieslik.kafe.feature.menu.domain

import com.karolina.cieslik.kafe.core.domain.Money

// KAFE-6: Buildery danych testowych menu – domyślne wartości, nadpisujemy tylko to, co istotne dla testu.
internal fun category(id: String, sortOrder: Int = 0, name: String = id) =
    Category(id = id, name = name, sortOrder = sortOrder)

internal fun size(code: SizeCode = SizeCode.S, grosze: Long = 1_200, volumeMl: Int? = 250) =
    ProductSize(code = code, volumeMl = volumeMl, price = Money(grosze))

internal fun product(
    id: String,
    categoryId: String,
    sortOrder: Int = 0,
    isActive: Boolean = true,
    sizes: List<ProductSize> = listOf(size()),
    name: String = id,
) = Product(
    id = id,
    categoryId = categoryId,
    name = name,
    description = null,
    imageUrl = null,
    sizes = sizes,
    sortOrder = sortOrder,
    isActive = isActive,
)

internal fun menu(
    categories: List<Category>,
    products: List<Product>,
    unavailableProductIds: Set<String> = emptySet(),
) = Menu(categories = categories, products = products, unavailableProductIds = unavailableProductIds)

internal fun CafeMenu.categoryIds(): List<String> = sections.map { it.category.id }

internal fun CafeMenu.productIds(categoryId: String): List<String> =
    sections.single { it.category.id == categoryId }.items.map { it.product.id }
