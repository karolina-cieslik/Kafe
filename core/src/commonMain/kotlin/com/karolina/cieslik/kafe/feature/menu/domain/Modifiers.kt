package com.karolina.cieslik.kafe.feature.menu.domain

import com.karolina.cieslik.kafe.core.domain.Money

/** Rodzaj grupy modyfikatorów produktu (mleko, dodatkowe espresso, syrop); grupy są wspólne dla sieci. */
enum class ModifierGroupType {
    /** Jedna opcja z grupy (np. mleko, syrop). */
    SINGLE,

    /** Opcje z licznikiem; suma ilości w grupie od 0 do [ModifierGroup.maxQuantity] (np. shoty 0–4 przy limicie 4). */
    QUANTITY,
}

/**
 * Grupa modyfikatorów, np. „Mleko” albo „Dodatkowe espresso”.
 *
 * @property id identyfikator grupy z API.
 * @property name nazwa wyświetlana.
 * @property type sposób wyboru opcji.
 * @property isRequired SINGLE: musi być wybrana opcja (np. mleko). QUANTITY: suma ilości ≥ 1.
 * @property maxQuantity limit sumy ilości w grupie (np. 4 dla shotów); dla SINGLE zawsze 1.
 * @property options opcje w kolejności wyświetlania.
 * @property defaultOptionId opcja wybrana na start (np. mleko krowie); null = brak, a w grupie wymaganej
 * wybierana jest wtedy pierwsza dostępna.
 */
data class ModifierGroup(
    val id: String,
    val name: String,
    val type: ModifierGroupType,
    val isRequired: Boolean,
    val maxQuantity: Int,
    val options: List<ModifierOption>,
    val defaultOptionId: String? = null,
)

/**
 * Opcja w grupie modyfikatorów, np. „Mleko owsiane”.
 *
 * @property id identyfikator opcji z API; unikalny w całej sieci (dostępność w lokalu jest po samym id).
 * @property name nazwa wyświetlana.
 * @property surcharge dopłata za jedną sztukę opcji; Money.ZERO = „bez dopłaty”.
 */
data class ModifierOption(
    val id: String,
    val name: String,
    val surcharge: Money,
)
