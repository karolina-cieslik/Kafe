package com.karolina.cieslik.kafe.feature.menu.data.remote

import kotlinx.serialization.Serializable

// KAFE-6: DTO menu w kształcie planowanej struktury Firebase Realtime DB:
// /menu (wspólne dla sieci) oraz /availability/{cafeId}/{productId}: Boolean.
@Serializable
data class MenuDto(
    val categories: List<CategoryDto> = emptyList(),
    val products: List<ProductDto> = emptyList(),
)

@Serializable
data class CategoryDto(
    val id: String,
    val name: String,
    val sortOrder: Int = 0,
)

@Serializable
data class ProductDto(
    val id: String,
    val categoryId: String,
    val name: String,
    val description: String? = null,
    val imageUrl: String? = null,
    val sizes: List<ProductSizeDto> = emptyList(),
    val sortOrder: Int = 0,
    val active: Boolean = true,
)

@Serializable
data class ProductSizeDto(
    val code: String,
    val volumeMl: Int? = null,
    val priceGrosze: Long,
)
