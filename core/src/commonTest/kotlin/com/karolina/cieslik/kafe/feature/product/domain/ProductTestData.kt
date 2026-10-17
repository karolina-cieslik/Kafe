package com.karolina.cieslik.kafe.feature.product.domain

import com.karolina.cieslik.kafe.core.domain.Money
import com.karolina.cieslik.kafe.feature.menu.domain.ModifierGroup
import com.karolina.cieslik.kafe.feature.menu.domain.ModifierGroupType
import com.karolina.cieslik.kafe.feature.menu.domain.ModifierOption
import com.karolina.cieslik.kafe.feature.menu.domain.Product
import com.karolina.cieslik.kafe.feature.menu.domain.ProductSize
import com.karolina.cieslik.kafe.feature.menu.domain.SizeCode

// KAFE-9 / KAFE-10: Wspólne dane testowe – latte z mlekiem (wymagane), shotami (0–4) i syropem (opcjonalny).
internal object ProductTestData {

    val milk = ModifierGroup(
        id = "milk",
        name = "Mleko",
        type = ModifierGroupType.SINGLE,
        isRequired = true,
        maxQuantity = 1,
        options = listOf(
            ModifierOption("milk-cow", "Krowie", Money.ZERO),
            ModifierOption("milk-oat", "Owsiane", Money(200)),
        ),
        defaultOptionId = "milk-cow",
    )

    val shots = ModifierGroup(
        id = "shots",
        name = "Dodatkowe espresso",
        type = ModifierGroupType.QUANTITY,
        isRequired = false,
        maxQuantity = 4,
        options = listOf(ModifierOption("shot", "Shot espresso", Money(300))),
    )

    val syrup = ModifierGroup(
        id = "syrup",
        name = "Syrop",
        type = ModifierGroupType.SINGLE,
        isRequired = false,
        maxQuantity = 1,
        options = listOf(
            ModifierOption("syrup-vanilla", "Waniliowy", Money(200)),
            ModifierOption("syrup-caramel", "Karmelowy", Money(250)),
        ),
    )

    val latte = Product(
        id = "latte",
        categoryId = "classic",
        name = "Caffè latte",
        description = null,
        imageUrl = null,
        sizes = listOf(
            ProductSize(SizeCode.S, 250, Money(1400)),
            ProductSize(SizeCode.M, 350, Money(1600)),
            ProductSize(SizeCode.L, 450, Money(1800)),
        ),
        sortOrder = 1,
        isActive = true,
        defaultSize = SizeCode.M,
        modifierGroupIds = listOf("milk", "shots", "syrup"),
    )

    fun details(
        product: Product = latte,
        isAvailable: Boolean = true,
        groups: List<ModifierGroup> = listOf(milk, shots, syrup),
        unavailableSizes: Set<SizeCode> = emptySet(),
        unavailableOptionIds: Set<String> = emptySet(),
    ) = ProductDetails(product, isAvailable, groups, unavailableSizes, unavailableOptionIds)
}
