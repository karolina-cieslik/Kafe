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
 * Produkt menu sieci w tabeli `menu_products`; rozmiary i przypisania grup są w osobnych tabelach.
 *
 * @property id identyfikator z API.
 * @property categoryId kategoria produktu; indeks pod filtrowanie po kategorii.
 * @property name nazwa wyświetlana.
 * @property description opis; null albo pusty, gdy brak (pusty zamieniany na null przy odczycie).
 * @property imageUrl adres zdjęcia; null albo pusty, gdy brak.
 * @property sortOrder pozycja w kategorii ustalona przez sieć.
 * @property isActive false dla produktów poza ofertą (np. sezonowych poza sezonem).
 * @property defaultSizeCode kod domyślnego rozmiaru wielkimi literami (S/M/L); null = brak (także dla wierszy
 * sprzed migracji do wersji 3).
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
    val defaultSizeCode: String? = null,
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
 * Grupa modyfikatorów sieci w tabeli `menu_modifier_groups` (od wersji 3 bazy).
 *
 * @property id identyfikator grupy z API.
 * @property name nazwa wyświetlana.
 * @property type nazwa ModifierGroupType wielkimi literami (SINGLE/QUANTITY); grupa z nieznanym typem jest
 * pomijana przy odczycie.
 * @property isRequired grupa wymagana.
 * @property maxQuantity limit sumy ilości w grupie; przy odczycie dla SINGLE zawsze 1, dla QUANTITY co najmniej 1.
 * @property defaultOptionId opcja wybrana na start; null = brak, a id spoza grupy jest ignorowane przy odczycie.
 */
@Entity(tableName = "menu_modifier_groups")
data class ModifierGroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val isRequired: Boolean,
    val maxQuantity: Int,
    val defaultOptionId: String?,
)

/**
 * Opcja modyfikatora w tabeli `menu_modifier_options` (od wersji 3 bazy).
 *
 * @property id identyfikator opcji; unikalny w całej sieci.
 * @property groupId grupa, do której należy opcja; indeks pod relację z grupą.
 * @property name nazwa wyświetlana.
 * @property surchargeGrosze dopłata za jedną sztukę opcji w groszach; ujemna jest pomijana przy odczycie.
 * @property sortOrder pozycja w grupie, rosnąco.
 */
@Entity(tableName = "menu_modifier_options", indices = [Index("groupId")])
data class ModifierOptionEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val name: String,
    val surchargeGrosze: Long,
    val sortOrder: Int,
)

/**
 * Przypisanie grupy modyfikatorów do produktu w tabeli `menu_product_modifier_groups` (od wersji 3 bazy).
 *
 * @property productId produkt.
 * @property groupId grupa; nieznane id jest pomijane w domenie.
 * @property sortOrder pozycja grupy na ekranie produktu, od 0.
 */
@Entity(tableName = "menu_product_modifier_groups", primaryKeys = ["productId", "groupId"])
data class ProductModifierGroupEntity(
    val productId: String,
    val groupId: String,
    val sortOrder: Int,
)

/**
 * Dostępność rozmiaru produktu w lokalu w tabeli `menu_size_availability` (od wersji 3 bazy).
 * Rozmiar bez wiersza jest dostępny.
 *
 * @property cafeId lokal.
 * @property productId produkt.
 * @property code kod rozmiaru wielkimi literami (S/M/L); nieznany kod jest pomijany przy odczycie.
 * @property isAvailable false, gdy rozmiar jest chwilowo niedostępny w lokalu.
 */
@Entity(tableName = "menu_size_availability", primaryKeys = ["cafeId", "productId", "code"])
data class SizeAvailabilityEntity(
    val cafeId: String,
    val productId: String,
    val code: String,
    val isAvailable: Boolean,
)

/**
 * Dostępność opcji modyfikatora w lokalu w tabeli `menu_modifier_option_availability` (od wersji 3 bazy).
 * Opcja bez wiersza jest dostępna.
 *
 * @property cafeId lokal.
 * @property optionId opcja.
 * @property isAvailable false, gdy opcja jest chwilowo niedostępna w lokalu (np. brak mleka migdałowego).
 */
@Entity(tableName = "menu_modifier_option_availability", primaryKeys = ["cafeId", "optionId"])
data class ModifierOptionAvailabilityEntity(
    val cafeId: String,
    val optionId: String,
    val isAvailable: Boolean,
)

/**
 * Wynik relacji Room: produkt z rozmiarami i przypisanymi grupami modyfikatorów.
 *
 * @property product wiersz produktu.
 * @property sizes rozmiary produktu, w dowolnej kolejności.
 * @property modifierGroups przypisania grup, w dowolnej kolejności (sortowane po sortOrder przy odczycie).
 */
data class ProductWithSizes(
    @Embedded val product: ProductEntity,
    @Relation(parentColumn = "id", entityColumn = "productId")
    val sizes: List<ProductSizeEntity>,
    @Relation(parentColumn = "id", entityColumn = "productId")
    val modifierGroups: List<ProductModifierGroupEntity> = emptyList(),
)

/**
 * Wynik relacji Room: grupa modyfikatorów z opcjami.
 *
 * @property group wiersz grupy.
 * @property options opcje grupy, w dowolnej kolejności (sortowane po sortOrder przy odczycie).
 */
data class ModifierGroupWithOptions(
    @Embedded val group: ModifierGroupEntity,
    @Relation(parentColumn = "id", entityColumn = "groupId")
    val options: List<ModifierOptionEntity>,
)
