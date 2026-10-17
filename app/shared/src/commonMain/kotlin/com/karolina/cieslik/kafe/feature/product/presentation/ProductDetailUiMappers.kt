package com.karolina.cieslik.kafe.feature.product.presentation

import com.karolina.cieslik.kafe.core.domain.Money
import com.karolina.cieslik.kafe.feature.menu.domain.ModifierGroup
import com.karolina.cieslik.kafe.feature.menu.domain.ModifierGroupType
import com.karolina.cieslik.kafe.feature.menu.domain.SizeCode
import com.karolina.cieslik.kafe.feature.product.domain.ConfigurationIssue
import com.karolina.cieslik.kafe.feature.product.domain.PriceCalculator
import com.karolina.cieslik.kafe.feature.product.domain.ProductConfiguration
import com.karolina.cieslik.kafe.feature.product.domain.ProductDetails

// KAFE-9 / KAFE-10 / KAFE-11: Mapowanie szczegółów produktu i konfiguracji na stan ekranu.
internal fun ProductDetails.toContent(
    configuration: ProductConfiguration,
    calculator: PriceCalculator,
    deselectedOptions: List<String> = emptyList(),
): ProductDetailUiState.Content {
    val issue = configuration.validate(this)
    return ProductDetailUiState.Content(
        name = product.name,
        description = product.description,
        imageUrl = product.imageUrl,
        isAvailable = isAvailable,
        sizes = product.sizes.map { size ->
            ProductSizeUi(
                code = size.code.name,
                label = size.code.label(),
                volumeLabel = size.volumeMl?.let { "$it ml" },
                priceLabel = size.price.format(),
                isAvailable = isSizeAvailable(size.code),
                isSelected = size.code == configuration.size,
            )
        },
        modifierGroups = modifierGroups.map { it.toUi(this, configuration) },
        quantity = configuration.quantity,
        canIncreaseQuantity = configuration.quantity < ProductConfiguration.MAX_QUANTITY,
        canDecreaseQuantity = configuration.quantity > ProductConfiguration.MIN_QUANTITY,
        totalPriceLabel = configuration.totalPrice(this, calculator).format(),
        canAddToCart = issue == null,
        // Blokada dodania ważniejsza niż informacja o odznaczeniu.
        infoMessage = issue?.toMessage() ?: deselectedOptions.toDeselectedMessage(),
    )
}

private fun ModifierGroup.toUi(details: ProductDetails, configuration: ProductConfiguration): ModifierGroupUi {
    val groupTotal = configuration.totalIn(id)
    return ModifierGroupUi(
        id = id,
        name = name,
        type = when (type) {
            ModifierGroupType.SINGLE -> ModifierGroupTypeUi.SINGLE
            ModifierGroupType.QUANTITY -> ModifierGroupTypeUi.QUANTITY
        },
        isRequired = isRequired,
        limitLabel = if (type == ModifierGroupType.QUANTITY) "maks. $maxQuantity" else null,
        options = options.map { option ->
            val quantity = configuration.quantityOf(id, option.id)
            val isAvailable = details.isOptionAvailable(option.id)
            ModifierOptionUi(
                id = option.id,
                name = option.name,
                surchargeLabel = option.surcharge.toSurchargeLabel(),
                isAvailable = isAvailable,
                isSelected = quantity > 0,
                quantity = quantity,
                canIncrease = when (type) {
                    ModifierGroupType.SINGLE -> isAvailable && quantity == 0
                    ModifierGroupType.QUANTITY -> isAvailable && groupTotal < maxQuantity
                },
                canDecrease = when (type) {
                    ModifierGroupType.SINGLE -> quantity > 0 && !isRequired
                    ModifierGroupType.QUANTITY -> quantity > 0
                },
            )
        },
    )
}

internal fun Money.toSurchargeLabel(): String = if (this == Money.ZERO) SURCHARGE_NONE else "+${format()}"

internal fun SizeCode.label(): String = when (this) {
    SizeCode.S -> "Mały"
    SizeCode.M -> "Średni"
    SizeCode.L -> "Duży"
}

internal fun ConfigurationIssue.toMessage(): String = when (this) {
    ConfigurationIssue.ProductUnavailable -> "Produkt jest niedostępny w wybranym lokalu."
    ConfigurationIssue.SizeUnavailable -> "Wybrany rozmiar jest niedostępny w tym lokalu."
    is ConfigurationIssue.MissingRequired -> "Wybierz opcję: ${group.name}."
    is ConfigurationIssue.OptionUnavailable -> "Opcja „$optionName” jest niedostępna w tym lokalu."
}

internal fun List<String>.toDeselectedMessage(): String? = when (size) {
    0 -> null
    1 -> "Opcja „${first()}” jest już niedostępna i została odznaczona."
    else -> "Opcje ${joinToString { "„$it”" }} są już niedostępne i zostały odznaczone."
}

// Docelowo do zasobów stringów (composeResources), gdy dojdzie lokalizacja.
internal const val SURCHARGE_NONE = "bez dopłaty"
internal const val PRODUCT_ERROR_NOT_FOUND = "Tego produktu nie ma już w menu."
internal const val PRODUCT_ERROR_NO_CAFE = "Nie wybrano lokalu."
internal const val PRODUCT_ERROR_NO_CONNECTION = "Nie udało się pobrać produktu. Sprawdź połączenie."
internal const val PRODUCT_ERROR_GENERIC = "Nie udało się pobrać produktu. Spróbuj ponownie później."
