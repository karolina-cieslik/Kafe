package com.karolina.cieslik.kafe.feature.cart.domain

import com.karolina.cieslik.kafe.feature.menu.domain.Menu
import com.karolina.cieslik.kafe.feature.menu.domain.ModifierGroup
import com.karolina.cieslik.kafe.feature.menu.domain.ModifierGroupType
import com.karolina.cieslik.kafe.feature.product.domain.ConfigurationIssue
import com.karolina.cieslik.kafe.feature.product.domain.PriceCalculator
import com.karolina.cieslik.kafe.feature.product.domain.PricedOption
import com.karolina.cieslik.kafe.feature.product.domain.ProductConfiguration
import com.karolina.cieslik.kafe.feature.product.domain.ProductDetails
import com.karolina.cieslik.kafe.feature.product.domain.ProductLookup
import com.karolina.cieslik.kafe.feature.product.domain.findProduct

/** Wynik sprawdzenia jednej pozycji z menu – wspólny dla [ObserveCheckedCart] i [RefreshCartSnapshots]. */
internal sealed interface CartItemCheck {

    /** Pozycji nie da się zamówić. */
    data class Unavailable(val reason: UnavailableReason) : CartItemCheck

    /** Pozycję można zamówić; [snapshot] to jej migawka według aktualnego menu. */
    data class Valid(val snapshot: CartSnapshotUpdate) : CartItemCheck
}

/** Sprawdza pozycję z [menu] lokalu; dla pozycji, którą można zamówić, liczy migawkę i cenę jak [AddToCart]. */
internal fun CartItem.checkWith(menu: Menu, priceCalculator: PriceCalculator): CartItemCheck {
    val details = (menu.findProduct(productId) as? ProductLookup.Found)?.details
        ?: return CartItemCheck.Unavailable(UnavailableReason.PRODUCT_REMOVED)
    val reason = unavailableReasonIn(details)
    return reason?.let { CartItemCheck.Unavailable(it) }
        ?: CartItemCheck.Valid(snapshotFrom(details, priceCalculator))
}

/** Migawka pozycji różni się od menu czymkolwiek, co jest w niej zapisane (nazwy, dopłaty, flagi, cena). */
internal fun CartSnapshotUpdate.differsFromItem(): Boolean =
    productName != item.productName || unitPrice != item.unitPrice || item.selections.any { selection ->
        val current = options.getValue(OptionRef(selection.groupId, selection.optionId))
        current.optionName != selection.optionName ||
            current.surcharge != selection.surcharge ||
            current.isDefault != selection.isDefault
    }

/**
 * Pierwszy powód niedostępności; null = można zamówić. Najpierw to, czego [ProductConfiguration.validate] nie
 * widzi, bo zakłada wybór z aktualnego menu: opcje usunięte z grupy (bez tego pozycja wyszłaby po cichu tańsza)
 * i limity grup zmienione po dodaniu. Potem te same reguły co przy dodaniu z ekranu produktu.
 */
private fun CartItem.unavailableReasonIn(details: ProductDetails): UnavailableReason? {
    if (!details.isAvailable) return UnavailableReason.PRODUCT_UNAVAILABLE
    if (details.product.sizes.none { it.code == size }) return UnavailableReason.SIZE_UNAVAILABLE
    // findProduct pomija grupy odpięte od produktu, więc ich opcje też tu nie zostaną znalezione.
    val groups = details.modifierGroups.associateBy { it.id }
    if (selections.any { groups[it.groupId].optionOrNull(it) == null }) return UnavailableReason.OPTION_UNAVAILABLE
    val byGroup = selections.groupBy { it.groupId }
    if (byGroup.any { (groupId, chosen) -> groups.getValue(groupId).breaksLimit(chosen) }) {
        return UnavailableReason.INVALID_CONFIGURATION
    }
    val configuration = ProductConfiguration(
        size = size,
        selections = byGroup.mapValues { (_, chosen) -> chosen.associate { it.optionId to it.quantity } },
        quantity = quantity,
    )
    return configuration.validate(details)?.toReason()
}

/** Migawka według menu; zakłada pozycję bez powodu niedostępności ([unavailableReasonIn] = null). */
private fun CartItem.snapshotFrom(details: ProductDetails, priceCalculator: PriceCalculator): CartSnapshotUpdate {
    val groups = details.modifierGroups.associateBy { it.id }
    val options = selections.associateWith { selection ->
        checkNotNull(groups[selection.groupId].optionOrNull(selection)) { "Brak opcji ${selection.optionId}" }
    }
    return CartSnapshotUpdate(
        item = this,
        productName = details.product.name,
        unitPrice = priceCalculator.unitPrice(
            details.product.sizes.first { it.code == size }.price,
            selections.map { PricedOption(options.getValue(it).surcharge, it.quantity) },
        ),
        options = selections.associate { selection ->
            val option = options.getValue(selection)
            OptionRef(selection.groupId, selection.optionId) to OptionSnapshot(
                optionName = option.name,
                surcharge = option.surcharge,
                isDefault = option.id == groups.getValue(selection.groupId).defaultOptionId,
            )
        },
    )
}

private fun ModifierGroup?.optionOrNull(selection: CartSelection) =
    this?.options?.firstOrNull { it.id == selection.optionId }

/** Suma ilości ponad limit grupy albo w SINGLE więcej niż jedna opcja z ilością 1 (np. po zmianie typu grupy). */
private fun ModifierGroup.breaksLimit(chosen: List<CartSelection>): Boolean =
    chosen.sumOf { it.quantity } > maxQuantity ||
        (type == ModifierGroupType.SINGLE && (chosen.size > 1 || chosen.single().quantity != 1))

private fun ConfigurationIssue.toReason(): UnavailableReason = when (this) {
    ConfigurationIssue.ProductUnavailable -> UnavailableReason.PRODUCT_UNAVAILABLE
    ConfigurationIssue.SizeUnavailable -> UnavailableReason.SIZE_UNAVAILABLE
    is ConfigurationIssue.OptionUnavailable -> UnavailableReason.OPTION_UNAVAILABLE
    is ConfigurationIssue.MissingRequired -> UnavailableReason.INVALID_CONFIGURATION
}
