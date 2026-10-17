package com.karolina.cieslik.kafe.feature.menu.data.remote

import kotlinx.serialization.Serializable

// KAFE-6: DTO menu w kształcie planowanej struktury Firebase Realtime DB:
// /menu (wspólne dla sieci) oraz /availability/{cafeId} (braki w lokalu).
@Serializable
data class MenuDto(
    val categories: List<CategoryDto> = emptyList(),
    val products: List<ProductDto> = emptyList(),
    /** KAFE-10: grupy modyfikatorów wspólne dla sieci. */
    val modifierGroups: List<ModifierGroupDto> = emptyList(),
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
    /** KAFE-9 AC3: kod rozmiaru domyślnego (S/M/L). */
    val defaultSize: String? = null,
    /** KAFE-10: id grup modyfikatorów w kolejności wyświetlania. */
    val modifierGroupIds: List<String> = emptyList(),
)

@Serializable
data class ProductSizeDto(
    val code: String,
    val volumeMl: Int? = null,
    val priceGrosze: Long,
)

// KAFE-10: type = "SINGLE" | "QUANTITY"; nieznany typ jest pomijany przy odczycie.
@Serializable
data class ModifierGroupDto(
    val id: String,
    val name: String,
    val type: String,
    val required: Boolean = false,
    val maxQuantity: Int = 1,
    val defaultOptionId: String? = null,
    val options: List<ModifierOptionDto> = emptyList(),
)

@Serializable
data class ModifierOptionDto(
    val id: String,
    val name: String,
    val surchargeGrosze: Long = 0,
    val sortOrder: Int = 0,
)

/**
 * Dostępność w lokalu; brak wpisu = dostępne.
 * products: productId -> dostępność, sizes: productId -> (kod rozmiaru -> dostępność) (KAFE-9 AC6),
 * modifierOptions: id opcji -> dostępność (KAFE-10).
 */
@Serializable
data class AvailabilityDto(
    val products: Map<String, Boolean> = emptyMap(),
    val sizes: Map<String, Map<String, Boolean>> = emptyMap(),
    val modifierOptions: Map<String, Boolean> = emptyMap(),
)
