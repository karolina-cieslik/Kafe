package com.karolina.cieslik.kafe.feature.product.domain

import com.karolina.cieslik.kafe.core.domain.Money
import com.karolina.cieslik.kafe.feature.menu.domain.ModifierGroup
import com.karolina.cieslik.kafe.feature.menu.domain.ModifierGroupType
import com.karolina.cieslik.kafe.feature.menu.domain.ModifierOption
import com.karolina.cieslik.kafe.feature.menu.domain.Product
import com.karolina.cieslik.kafe.feature.menu.domain.ProductSize
import com.karolina.cieslik.kafe.feature.menu.domain.SizeCode

// KAFE-9 / KAFE-10: Dane testowe produktu z rozmiarami S/M/L i grupami jak w AC KAFE-10.
internal val milkGroup = ModifierGroup(
    id = "milk",
    name = "Mleko",
    type = ModifierGroupType.SINGLE,
    isRequired = true,
    maxQuantity = 1,
    options = listOf(
        ModifierOption("cow", "Krowie", Money.ZERO),
        ModifierOption("oat", "Owsiane", Money(200)),
        ModifierOption("almond", "Migdałowe", Money(250)),
    ),
    defaultOptionId = "cow",
)

internal val shotsGroup = ModifierGroup(
    id = "extras",
    name = "Espresso",
    type = ModifierGroupType.QUANTITY,
    isRequired = false,
    maxQuantity = 4,
    options = listOf(ModifierOption("shot", "Dodatkowe espresso", Money(300))),
)

internal val syrupGroup = ModifierGroup(
    id = "syrup",
    name = "Syropy",
    type = ModifierGroupType.SINGLE,
    isRequired = false,
    maxQuantity = 1,
    options = listOf(
        ModifierOption("vanilla", "Waniliowy", Money(150)),
        ModifierOption("caramel", "Karmelowy", Money(150)),
    ),
)

internal fun latte(defaultSize: SizeCode? = SizeCode.M) = Product(
    id = "latte",
    categoryId = "classic",
    name = "Caffè latte",
    description = null,
    imageUrl = null,
    sizes = listOf(
        ProductSize(SizeCode.S, 250, Money(1_200)),
        ProductSize(SizeCode.M, 350, Money(1_400)),
        ProductSize(SizeCode.L, 450, Money(1_600)),
    ),
    sortOrder = 1,
    isActive = true,
    defaultSize = defaultSize,
    modifierGroupIds = listOf("milk", "extras", "syrup"),
)

internal fun details(
    product: Product = latte(),
    isAvailable: Boolean = true,
    groups: List<ModifierGroup> = listOf(milkGroup, shotsGroup, syrupGroup),
    unavailableSizes: Set<SizeCode> = emptySet(),
    unavailableOptionIds: Set<String> = emptySet(),
) = ProductDetails(
    product = product,
    isAvailable = isAvailable,
    modifierGroups = groups,
    unavailableSizes = unavailableSizes,
    unavailableOptionIds = unavailableOptionIds,
)
