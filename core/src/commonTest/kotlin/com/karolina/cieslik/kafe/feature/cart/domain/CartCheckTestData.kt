package com.karolina.cieslik.kafe.feature.cart.domain

import com.karolina.cieslik.kafe.core.domain.Money
import com.karolina.cieslik.kafe.feature.menu.domain.Menu
import com.karolina.cieslik.kafe.feature.menu.domain.ModifierGroup
import com.karolina.cieslik.kafe.feature.menu.domain.Product
import com.karolina.cieslik.kafe.feature.menu.domain.SizeCode
import com.karolina.cieslik.kafe.feature.product.domain.ProductTestData

/** Dane do testów sprawdzania koszyka: latte z [ProductTestData] w menu lokalu i pozycja w koszyku. */
internal object CartCheckTestData {

    val oat = CartSelection("milk", "milk-oat", "Owsiane", Money(200), 1)
    val twoShots = CartSelection("shots", "shot", "Shot espresso", Money(300), 2)

    fun menu(
        products: List<Product> = listOf(ProductTestData.latte),
        groups: List<ModifierGroup> = listOf(ProductTestData.milk, ProductTestData.shots, ProductTestData.syrup),
        unavailableProductIds: Set<String> = emptySet(),
        unavailableSizes: Map<String, Set<SizeCode>> = emptyMap(),
        unavailableOptionIds: Set<String> = emptySet(),
    ) = Menu(
        categories = emptyList(),
        products = products,
        unavailableProductIds = unavailableProductIds,
        modifierGroups = groups,
        unavailableSizes = unavailableSizes,
        unavailableOptionIds = unavailableOptionIds,
    )

    fun item(
        id: Long = 1,
        productId: String = "latte",
        size: SizeCode = SizeCode.M,
        selections: List<CartSelection> = listOf(oat, twoShots),
        quantity: Int = 1,
        unitPrice: Money = Money(2_400),
        name: String = "Caffè latte",
    ) = CartItem(id, productId, name, size, selections, quantity, unitPrice)
}
