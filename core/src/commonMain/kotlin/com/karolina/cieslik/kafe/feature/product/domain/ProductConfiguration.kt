package com.karolina.cieslik.kafe.feature.product.domain

import com.karolina.cieslik.kafe.core.domain.Money
import com.karolina.cieslik.kafe.feature.menu.domain.ModifierGroup
import com.karolina.cieslik.kafe.feature.menu.domain.ModifierGroupType
import com.karolina.cieslik.kafe.feature.menu.domain.SizeCode

/**
 * Wybór klienta (rozmiar, modyfikatory, liczba sztuk) i reguły jego zmiany. Konfiguracja nie jest zapamiętywana
 * między wejściami na ekran.
 *
 * Niedozwolone zmiany (niedostępna opcja, przekroczony limit) są ignorowane – zwracana jest ta sama konfiguracja.
 *
 * @property size wybrany rozmiar.
 * @property selections groupId -> (optionId -> ilość > 0). SINGLE ma co najwyżej jeden wpis z ilością 1.
 * @property quantity liczba sztuk, od [MIN_QUANTITY] do [MAX_QUANTITY].
 */
data class ProductConfiguration(
    val size: SizeCode,
    val selections: Map<String, Map<String, Int>>,
    val quantity: Int,
) {

    /** Ilość opcji [optionId] w grupie [groupId]; 0, gdy nie jest wybrana. */
    fun quantityOf(groupId: String, optionId: String): Int = selections[groupId]?.get(optionId) ?: 0

    /** Suma ilości wszystkich opcji w grupie [groupId]. */
    fun totalIn(groupId: String): Int = selections[groupId]?.values?.sum() ?: 0

    /** Zmienia rozmiar na [code]; niedostępny w lokalu albo nieistniejący rozmiar jest ignorowany. */
    fun selectSize(code: SizeCode, details: ProductDetails): ProductConfiguration =
        if (details.isSizeAvailable(code)) copy(size = code) else this

    /**
     * SINGLE: wybór opcji – zastępuje poprzednią opcję w grupie. W grupie opcjonalnej ponowny wybór zaznaczonej
     * opcji ją odznacza; w wymaganej nic nie zmienia. Niedostępnej opcji nie da się wybrać.
     */
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

    /**
     * QUANTITY: zmiana ilości opcji o [delta]. Suma w grupie zostaje w zakresie 0..maxQuantity (np. shoty 0–4),
     * a przekroczenie jest przycinane do limitu. Niedostępną opcję można tylko zmniejszać.
     */
    fun changeOptionQuantity(
        groupId: String,
        optionId: String,
        delta: Int,
        details: ProductDetails,
    ): ProductConfiguration {
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

    /** Zmienia liczbę sztuk o [delta], przycinając do zakresu [MIN_QUANTITY]..[MAX_QUANTITY] (1–20). */
    fun changeQuantity(delta: Int): ProductConfiguration =
        copy(quantity = (quantity + delta).coerceIn(MIN_QUANTITY, MAX_QUANTITY))

    /**
     * Pierwszy powód, dla którego nie można dodać do koszyka; null = konfiguracja poprawna.
     * Kolejność sprawdzania: produkt, rozmiar, potem grupy w kolejności wyświetlania (niedostępna opcja przed
     * brakiem wymaganej).
     */
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
     * Dopasowanie wyboru do menu po odświeżeniu. Opcje usunięte z grupy znikają, niedostępne są odznaczane
     * (zwracane w [Normalized.deselectedOptions], żeby UI mogło o tym poinformować), limit QUANTITY jest
     * przycinany (opcje w kolejności grupy). Wymagana grupa SINGLE bez wyboru dostaje opcję startową, a wymagana
     * grupa QUANTITY zostaje pusta i blokuje dodanie ([validate]). Grupy, których produkt już nie ma, znikają.
     * Rozmiar spoza produktu wraca do startowego; niedostępny rozmiar zostaje, a dodanie blokuje [validate].
     * Liczba sztuk się nie zmienia.
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

    /**
     * Wynik [normalizedFor].
     *
     * @property configuration konfiguracja dopasowana do menu.
     * @property deselectedOptions nazwy opcji odznaczonych, bo stały się niedostępne; pusta lista = bez zmian.
     */
    data class Normalized(val configuration: ProductConfiguration, val deselectedOptions: List<String>)

    /** Cena wszystkich sztuk z dopłatami ([PriceCalculator]); liczy tylko opcje istniejące w grupach produktu. */
    fun totalPrice(details: ProductDetails, calculator: PriceCalculator): Money {
        val sizePrice = (details.product.sizes.firstOrNull { it.code == size } ?: details.product.sizes.first()).price
        val options = details.modifierGroups.flatMap { group ->
            group.options.mapNotNull { option ->
                quantityOf(group.id, option.id).takeIf { it > 0 }?.let { PricedOption(option.surcharge, it) }
            }
        }
        return calculator.totalPrice(sizePrice, options, quantity)
    }

    private fun withGroup(groupId: String, options: Map<String, Int>) =
        copy(selections = selections + (groupId to options))

    /** Limity i konfiguracja startowa. */
    companion object {
        /** Najmniejsza liczba sztuk w pozycji. */
        const val MIN_QUANTITY = 1

        /** Największa liczba sztuk w jednej pozycji – limit 1–20 sztuk na pozycję. */
        const val MAX_QUANTITY = 20

        /**
         * Konfiguracja startowa, 1 sztuka.
         *
         * Rozmiar: domyślny rozmiar produktu, jeśli jest dostępny w lokalu; w przeciwnym razie pierwszy dostępny
         * w kolejności S → M → L (tę kolejność rozmiarów zapewnia mapper danych); gdy żaden nie jest dostępny –
         * domyślny, a bez domyślnego pierwszy z listy (dodanie i tak zablokuje [validate]).
         * Grupy SINGLE: opcja domyślna, jeśli jest dostępna; grupa wymagana bez dostępnej opcji domyślnej –
         * pierwsza dostępna; grupa opcjonalna – nic. Grupy QUANTITY startują od zera.
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

/** Powód, dla którego konfiguracji nie można dodać do koszyka; UI zamienia go na komunikat. */
sealed interface ConfigurationIssue {
    /** Produkt jest niedostępny w lokalu. */
    data object ProductUnavailable : ConfigurationIssue

    /** Wybrany rozmiar jest niedostępny w lokalu. */
    data object SizeUnavailable : ConfigurationIssue

    /** W wymaganej grupie [group] nic nie wybrano. */
    data class MissingRequired(val group: ModifierGroup) : ConfigurationIssue

    /** W grupie [group] wybrano opcję [optionName], która jest niedostępna w lokalu. */
    data class OptionUnavailable(val group: ModifierGroup, val optionName: String) : ConfigurationIssue
}

private fun ProductDetails.group(groupId: String): ModifierGroup? = modifierGroups.firstOrNull { it.id == groupId }
