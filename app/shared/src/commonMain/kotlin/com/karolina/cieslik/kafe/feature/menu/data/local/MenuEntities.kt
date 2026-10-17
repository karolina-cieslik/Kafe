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

data class ProductWithSizes(
    @Embedded val product: ProductEntity,
    @Relation(parentColumn = "id", entityColumn = "productId")
    val sizes: List<ProductSizeEntity>,
)
