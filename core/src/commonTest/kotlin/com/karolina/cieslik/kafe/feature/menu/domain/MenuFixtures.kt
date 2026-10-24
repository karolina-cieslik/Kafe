package com.karolina.cieslik.kafe.feature.menu.domain

import com.karolina.cieslik.kafe.core.domain.Money

// Buildery danych testowych menu – domyślne wartości, nadpisujemy tylko to, co istotne dla testu.
/** Kategoria; [sortOrder] decyduje o kolejności, nazwa domyślnie równa id. */
internal fun category(id: String, sortOrder: Int = 0, name: String = id) =
    Category(id = id, name = name, sortOrder = sortOrder)

/** Rozmiar z pełną ceną w groszach (domyślnie S, 12,00 zł, 250 ml); `volumeMl = null` dla przekąsek. */
internal fun size(code: SizeCode = SizeCode.S, grosze: Long = 1_200, volumeMl: Int? = 250) =
    ProductSize(code = code, volumeMl = volumeMl, price = Money(grosze))

/** Produkt (domyślnie aktywny, z jednym rozmiarem z [size]); `sizes = emptyList()` symuluje błędne dane bez ceny. */
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

/** Menu sieci; [unavailableProductIds] – produkty niedostępne w danym lokalu. */
internal fun menu(
    categories: List<Category>,
    products: List<Product>,
    unavailableProductIds: Set<String> = emptySet(),
) = Menu(categories = categories, products = products, unavailableProductIds = unavailableProductIds)

/** Id kategorii widocznych w menu lokalu, w kolejności wyświetlania. */
internal fun CafeMenu.categoryIds(): List<String> = sections.map { it.category.id }

/** Id produktów w sekcji [categoryId], w kolejności wyświetlania; rzuca, gdy sekcji nie ma. */
internal fun CafeMenu.productIds(categoryId: String): List<String> =
    sections.single { it.category.id == categoryId }.items.map { it.product.id }
