package com.karolina.cieslik.kafe.feature.menu.data

import com.karolina.cieslik.kafe.core.domain.Money
import com.karolina.cieslik.kafe.feature.menu.data.remote.CategoryDto
import com.karolina.cieslik.kafe.feature.menu.data.remote.MenuDto
import com.karolina.cieslik.kafe.feature.menu.data.remote.ProductDto
import com.karolina.cieslik.kafe.feature.menu.data.remote.ProductSizeDto
import com.karolina.cieslik.kafe.feature.menu.domain.Category
import com.karolina.cieslik.kafe.feature.menu.domain.Menu
import com.karolina.cieslik.kafe.feature.menu.domain.Product
import com.karolina.cieslik.kafe.feature.menu.domain.ProductSize
import com.karolina.cieslik.kafe.feature.menu.domain.SizeCode

// KAFE-6: Mapowanie DTO -> domena. Nieznane kody rozmiarów są pomijane zamiast wywracać całe menu.
internal fun MenuDto.toDomain(availability: Map<String, Boolean>): Menu = Menu(
    categories = categories.map { it.toDomain() },
    products = products.map { it.toDomain() },
    unavailableProductIds = availability.filterValues { available -> !available }.keys,
)

internal fun CategoryDto.toDomain() = Category(id = id, name = name, sortOrder = sortOrder)

internal fun ProductDto.toDomain() = Product(
    id = id,
    categoryId = categoryId,
    name = name,
    description = description?.takeIf { it.isNotBlank() },
    imageUrl = imageUrl?.takeIf { it.isNotBlank() },
    sizes = sizes.mapNotNull { it.toDomainOrNull() },
    sortOrder = sortOrder,
    isActive = active,
)

internal fun ProductSizeDto.toDomainOrNull(): ProductSize? {
    val sizeCode = SizeCode.entries.firstOrNull { it.name.equals(code, ignoreCase = true) } ?: return null
    if (priceGrosze < 0) return null
    return ProductSize(code = sizeCode, volumeMl = volumeMl, price = Money(priceGrosze))
}
