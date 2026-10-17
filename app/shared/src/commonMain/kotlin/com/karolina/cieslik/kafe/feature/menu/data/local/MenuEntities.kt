package com.karolina.cieslik.kafe.feature.menu.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

// KAFE-6: Encje Room menu sieci i dostępności per lokal. Menu jest zawsze zastępowane w całości,
// więc produkty usunięte z menu po odświeżeniu znikają (przypadek brzegowy z KAFE-6).
@Entity(tableName = "menu_categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sortOrder: Int,
)

@Entity(tableName = "menu_products", indices = [Index("categoryId")])
data class ProductEntity(
    @PrimaryKey val id: String,
    val categoryId: String,
    val name: String,
    val description: String?,
    val imageUrl: String?,
    val sortOrder: Int,
    val isActive: Boolean,
    /** KAFE-9 (v3): kod domyślnego rozmiaru. */
    val defaultSizeCode: String? = null,
)

/** code = nazwa SizeCode (S/M/L); cena pełna w groszach. */
@Entity(tableName = "menu_product_sizes", primaryKeys = ["productId", "code"])
data class ProductSizeEntity(
    val productId: String,
    val code: String,
    val volumeMl: Int?,
    val priceGrosze: Long,
)

@Entity(tableName = "menu_product_availability", primaryKeys = ["cafeId", "productId"])
data class ProductAvailabilityEntity(
    val cafeId: String,
    val productId: String,
    val isAvailable: Boolean,
)

// KAFE-10 (v3): Modyfikatory sieci, przypisanie do produktów i dostępność per lokal.
/** type = nazwa ModifierGroupType (SINGLE/QUANTITY). */
@Entity(tableName = "menu_modifier_groups")
data class ModifierGroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val isRequired: Boolean,
    val maxQuantity: Int,
    val defaultOptionId: String?,
)

@Entity(tableName = "menu_modifier_options", indices = [Index("groupId")])
data class ModifierOptionEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val name: String,
    val surchargeGrosze: Long,
    val sortOrder: Int,
)

@Entity(tableName = "menu_product_modifier_groups", primaryKeys = ["productId", "groupId"])
data class ProductModifierGroupEntity(
    val productId: String,
    val groupId: String,
    val sortOrder: Int,
)

/** KAFE-9 AC6: dostępność rozmiaru w lokalu. */
@Entity(tableName = "menu_size_availability", primaryKeys = ["cafeId", "productId", "code"])
data class SizeAvailabilityEntity(
    val cafeId: String,
    val productId: String,
    val code: String,
    val isAvailable: Boolean,
)

@Entity(tableName = "menu_modifier_option_availability", primaryKeys = ["cafeId", "optionId"])
data class ModifierOptionAvailabilityEntity(
    val cafeId: String,
    val optionId: String,
    val isAvailable: Boolean,
)

data class ProductWithSizes(
    @Embedded val product: ProductEntity,
    @Relation(parentColumn = "id", entityColumn = "productId")
    val sizes: List<ProductSizeEntity>,
    @Relation(parentColumn = "id", entityColumn = "productId")
    val modifierGroups: List<ProductModifierGroupEntity> = emptyList(),
)

data class ModifierGroupWithOptions(
    @Embedded val group: ModifierGroupEntity,
    @Relation(parentColumn = "id", entityColumn = "groupId")
    val options: List<ModifierOptionEntity>,
)
