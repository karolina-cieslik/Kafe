package com.karolina.cieslik.kafe.feature.menu.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * Kategoria menu sieci w tabeli `menu_categories`. Menu jest zawsze zastępowane w całości, więc kategorie
 * i produkty usunięte z menu znikają po odświeżeniu.
 *
 * @property id identyfikator z API.
 * @property name nazwa wyświetlana.
 * @property sortOrder pozycja w menu ustalona przez sieć.
 */
@Entity(tableName = "menu_categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sortOrder: Int,
)

/**
 * Produkt menu sieci w tabeli `menu_products`; rozmiary są w osobnej tabeli.
 *
 * @property id identyfikator z API.
 * @property categoryId kategoria produktu; indeks pod filtrowanie po kategorii.
 * @property name nazwa wyświetlana.
 * @property description opis; null albo pusty, gdy brak (pusty zamieniany na null przy odczycie).
 * @property imageUrl adres zdjęcia; null albo pusty, gdy brak.
 * @property sortOrder pozycja w kategorii ustalona przez sieć.
 * @property isActive false dla produktów poza ofertą (np. sezonowych poza sezonem).
 */
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

/**
 * Rozmiar produktu w tabeli `menu_product_sizes` – jeden wiersz na produkt i kod rozmiaru.
 *
 * @property productId produkt, którego dotyczy rozmiar.
 * @property code nazwa SizeCode wielkimi literami (S/M/L); nieznany kod jest pomijany przy odczycie.
 * @property volumeMl pojemność w mililitrach; null dla produktów bez pojemności.
 * @property priceGrosze pełna cena w groszach (nie dopłata); ujemna jest pomijana przy odczycie.
 */
@Entity(tableName = "menu_product_sizes", primaryKeys = ["productId", "code"])
data class ProductSizeEntity(
    val productId: String,
    val code: String,
    val volumeMl: Int?,
    val priceGrosze: Long,
)

/**
 * Dostępność produktu w lokalu w tabeli `menu_product_availability`. Produkt bez wiersza jest dostępny.
 *
 * @property cafeId lokal.
 * @property productId produkt.
 * @property isAvailable false, gdy produkt jest chwilowo niedostępny w lokalu.
 */
@Entity(tableName = "menu_product_availability", primaryKeys = ["cafeId", "productId"])
data class ProductAvailabilityEntity(
    val cafeId: String,
    val productId: String,
    val isAvailable: Boolean,
)

/**
 * Wynik relacji Room: produkt z rozmiarami.
 *
 * @property product wiersz produktu.
 * @property sizes rozmiary produktu, w dowolnej kolejności.
 */
data class ProductWithSizes(
    @Embedded val product: ProductEntity,
    @Relation(parentColumn = "id", entityColumn = "productId")
    val sizes: List<ProductSizeEntity>,
)
