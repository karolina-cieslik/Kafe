package com.karolina.cieslik.kafe.feature.cart.data.local

import com.karolina.cieslik.kafe.core.domain.Money
import com.karolina.cieslik.kafe.feature.cart.domain.CartItem
import com.karolina.cieslik.kafe.feature.cart.domain.CartSelection
import com.karolina.cieslik.kafe.feature.cart.domain.NewCartItem
import com.karolina.cieslik.kafe.feature.menu.domain.SizeCode
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

private val cartJson = Json { ignoreUnknownKeys = true }

internal fun NewCartItem.toEntity() = CartItemEntity(
    id = 0,
    productId = productId,
    productName = productName,
    sizeCode = size.name,
    selectionsJson = selections.toJson(),
    configurationKey = configurationKey,
    quantity = quantity,
    unitPriceGrosze = unitPrice.grosze,
)

internal fun CartItem.toEntity() = CartItemEntity(
    id = id,
    productId = productId,
    productName = productName,
    sizeCode = size.name,
    selectionsJson = selections.toJson(),
    configurationKey = configurationKey,
    quantity = quantity,
    unitPriceGrosze = unitPrice.grosze,
)

/**
 * Null dla wiersza, którego nie da się odczytać (nieznany rozmiar, błędny JSON) – koszyk go pomija zamiast się
 * wysypać.
 */
internal fun CartItemEntity.toDomainOrNull(): CartItem? {
    val size = SizeCode.entries.firstOrNull { it.name == sizeCode } ?: return null
    val selections = try {
        cartJson.decodeFromString<List<CartSelectionJson>>(selectionsJson)
    } catch (_: SerializationException) {
        return null
    } catch (_: IllegalArgumentException) {
        return null
    }
    return CartItem(
        id = id,
        productId = productId,
        productName = productName,
        size = size,
        selections = selections.map {
            CartSelection(it.groupId, it.optionId, it.optionName, Money(it.surchargeGrosze), it.quantity)
        },
        quantity = quantity,
        unitPrice = Money(unitPriceGrosze),
    )
}

private fun List<CartSelection>.toJson(): String = cartJson.encodeToString(
    map { CartSelectionJson(it.groupId, it.optionId, it.optionName, it.surcharge.grosze, it.quantity) },
)
