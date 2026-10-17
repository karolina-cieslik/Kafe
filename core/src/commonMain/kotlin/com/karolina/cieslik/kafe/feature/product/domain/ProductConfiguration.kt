package com.karolina.cieslik.kafe.feature.product.domain

import com.karolina.cieslik.kafe.core.domain.Money
import com.karolina.cieslik.kafe.feature.menu.domain.ModifierGroup
import com.karolina.cieslik.kafe.feature.menu.domain.ModifierGroupType
import com.karolina.cieslik.kafe.feature.menu.domain.SizeCode

// KAFE-9 / KAFE-10 / KAFE-11: Wybór klienta (rozmiar, modyfikatory, liczba sztuk) i reguły jego zmiany.
// Niedozwolone zmiany (niedostępna opcja, przekroczony limit) są ignorowane – zwracana jest ta sama konfiguracja.
data class ProductConfiguration(
    val size: SizeCode,
    /** groupId -> (optionId -> ilość > 0). SINGLE ma co najwyżej jeden wpis z ilością 1. */
    val selections: Map<String, Map<String, Int>>,
    val quantity: Int,
) {

    fun quantityOf(groupId: String, optionId: String): Int = selections[groupId]?.get(optionId) ?: 0

    fun totalIn(groupId: String): Int = selections[groupId]?.values?.sum() ?: 0

    fun selectSize(code: SizeCode, details: ProductDetails): ProductConfiguration =
        if (details.isSizeAvailable(code)) copy(size = code) else this

    /** SINGLE: wybór opcji; w grupie opcjonalnej ponowny wybór zaznaczonej opcji ją odznacza. */
    fun selectOption(groupId: String, optionId: String, details: ProductDetails): ProductConfiguration {
        val group = details.group(groupId)?.takeIf { it.type == ModifierGroupType.SINGLE } ?: return this
        if (group.options.none { it.id == optionId }) return this
        val isSelected = quantityOf(groupId, optionId) > 0
        return when {
            isSelected && !group.isRequired -> withGroup(groupId, emptyMap())
            isSelected -> this
            !details.isOptionAvailable(optionId) -> this
            else -> withGroup(groupId, mapOf(optionId to 1))
        }
    }

    /** QUANTITY: zmiana ilości o [delta]; suma w grupie w zakresie 0..maxQuantity, niedostępną opcję można tylko zmniejszać. */
    fun changeOptionQuantity(groupId: String, optionId: String, delta: Int, details: ProductDetails): ProductConfiguration {
        val group = details.group(groupId)?.takeIf { it.type == ModifierGroupType.QUANTITY } ?: return this
        if (delta == 0 || group.options.none { it.id == optionId }) return this
        if (delta > 0 && !details.isOptionAvailable(optionId)) return this
        val current = quantityOf(groupId, optionId)
        val othersTotal = totalIn(groupId) - current
        val newQuantity = (current + delta).coerceIn(0, (group.maxQuantity - othersTotal).coerceAtLeast(0))
        if (newQuantity == current) return this
        val updated = selections[groupId].orEmpty() + (optionId to newQuantity)
        return withGroup(groupId, updated.filterValues { it > 0 })
    }

    fun changeQuantity(delta: Int): ProductConfiguration =
        copy(quantity = (quantity + delta).coerceIn(MIN_QUANTITY, MAX_QUANTITY))

    /** Pierwszy powód, dla którego nie można dodać do koszyka; null = konfiguracja poprawna. */
    fun validate(details: ProductDetails): ConfigurationIssue? {
        if (!details.isAvailable) return ConfigurationIssue.ProductUnavailable
        if (!details.isSizeAvailable(size)) return ConfigurationIssue.SizeUnavailable
        details.modifierGroups.forEach { group ->
            // Liczą się tylko opcje, które wciąż są w grupie – usunięte z menu traktujemy jak niewybrane.
            val selected = group.options.filter { quantityOf(group.id, it.id) > 0 }
            selected.firstOrNull { !details.isOptionAvailable(it.id) }?.let { option ->
                return ConfigurationIssue.OptionUnavailable(group, option.name)
            }
            if (group.isRequired && selected.isEmpty()) return ConfigurationIssue.MissingRequired(group)
        }
        return null
    }

    /**
     * KAFE-10 (przypadek brzegowy): dopasowanie wyboru do menu po odświeżeniu. Opcje usunięte z grupy znikają,
     * niedostępne są odznaczane (zwracane w [Normalized.deselectedOptions], żeby UI mogło o tym poinformować),
     * limit QUANTITY jest przycinany, a wymagana grupa bez wyboru dostaje opcję startową. Rozmiar spoza produktu
     * wraca do startowego; niedostępny rozmiar zostaje (walidacja blokuje dodanie, jak w KAFE-9 AC6).
     */
    fun normalizedFor(details: ProductDetails): Normalized {
        val start by lazy { initial(details) }
        val deselected = mutableListOf<String>()
        val normalizedSelections = details.modifierGroups.mapNotNull { group ->
            var remaining = group.maxQuantity
            val kept = buildMap {
                group.options.forEach { option ->
                    val quantity = quantityOf(group.id, option.id)
                    if (quantity <= 0) return@forEach
                    if (!details.isOptionAvailable(option.id)) {
                        deselected += option.name
                        return@forEach
                    }
                    val allowed = minOf(quantity, remaining)
                    if (allowed > 0) put(option.id, allowed)
                    remaining -= allowed
                }
            }
            val selection = kept.ifEmpty { if (group.isRequired) start.selections[group.id].orEmpty() else emptyMap() }
            selection.takeIf { it.isNotEmpty() }?.let { group.id to it }
        }.toMap()
        val normalizedSize = size.takeIf { code -> details.product.sizes.any { it.code == code } } ?: start.size
        val configuration = copy(size = normalizedSize, selections = normalizedSelections)
        return Normalized(configuration, deselected)
    }

    data class Normalized(val configuration: ProductConfiguration, val deselectedOptions: List<String>)

    fun totalPrice(details: ProductDetails, calculator: PriceCalculator): Money {
        val sizePrice = (details.product.sizes.firstOrNull { it.code == size } ?: details.product.sizes.first()).price
        val options = details.modifierGroups.flatMap { group ->
            group.options.mapNotNull { option ->
                quantityOf(group.id, option.id).takeIf { it > 0 }?.let { PricedOption(option.surcharge, it) }
            }
        }
        return calculator.totalPrice(sizePrice, options, quantity)
    }

    private fun withGroup(groupId: String, options: Map<String, Int>) = copy(selections = selections + (groupId to options))

    companion object {
        const val MIN_QUANTITY = 1
        const val MAX_QUANTITY = 20

        /**
         * Konfiguracja startowa (KAFE-9 AC3, KAFE-10): domyślny rozmiar, jeśli dostępny, w przeciwnym razie pierwszy
         * dostępny (S → L); gdy żaden – domyślny / pierwszy. Grupy: opcja domyślna, jeśli dostępna; wymagana grupa
         * bez dostępnej opcji domyślnej – pierwsza dostępna; QUANTITY – zero.
         */
        fun initial(details: ProductDetails): ProductConfiguration {
            val sizes = details.product.sizes
            val default = details.product.defaultSize?.takeIf { code -> sizes.any { it.code == code } }
            val size = default?.takeIf { details.isSizeAvailable(it) }
                ?: sizes.firstOrNull { details.isSizeAvailable(it.code) }?.code
                ?: default
                ?: sizes.first().code
            val selections = details.modifierGroups
                .mapNotNull { group -> group.initialOption(details)?.let { group.id to mapOf(it to 1) } }
                .toMap()
            return ProductConfiguration(size = size, selections = selections, quantity = MIN_QUANTITY)
        }

        private fun ModifierGroup.initialOption(details: ProductDetails): String? {
            if (type != ModifierGroupType.SINGLE) return null
            val default = defaultOptionId?.takeIf { id -> options.any { it.id == id } && details.isOptionAvailable(id) }
            return default ?: if (isRequired) options.firstOrNull { details.isOptionAvailable(it.id) }?.id else null
        }
    }
}

sealed interface ConfigurationIssue {
    data object ProductUnavailable : ConfigurationIssue
    data object SizeUnavailable : ConfigurationIssue
    data class MissingRequired(val group: ModifierGroup) : ConfigurationIssue
    data class OptionUnavailable(val group: ModifierGroup, val optionName: String) : ConfigurationIssue
}

private fun ProductDetails.group(groupId: String): ModifierGroup? = modifierGroups.firstOrNull { it.id == groupId }
