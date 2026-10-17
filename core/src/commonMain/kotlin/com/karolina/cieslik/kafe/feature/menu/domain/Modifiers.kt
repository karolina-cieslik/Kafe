package com.karolina.cieslik.kafe.feature.menu.domain

import com.karolina.cieslik.kafe.core.domain.Money

// KAFE-10: Modyfikatory produktu (mleko, dodatkowe espresso, syrop) wspólne dla sieci.
enum class ModifierGroupType {
    /** Jedna opcja z grupy (np. mleko, syrop). */
    SINGLE,

    /** Opcje z licznikiem; suma ilości w grupie ≤ [ModifierGroup.maxQuantity] (np. shoty 0–4). */
    QUANTITY,
}

data class ModifierGroup(
    val id: String,
    val name: String,
    val type: ModifierGroupType,
    /** SINGLE: musi być wybrana opcja (np. mleko). QUANTITY: suma ilości ≥ 1. */
    val isRequired: Boolean,
    /** Limit sumy ilości w grupie; dla SINGLE zawsze 1. */
    val maxQuantity: Int,
    val options: List<ModifierOption>,
    /** Opcja wybrana na start (np. mleko krowie); null = brak (dla wymaganej – pierwsza dostępna). */
    val defaultOptionId: String? = null,
)

data class ModifierOption(
    val id: String,
    val name: String,
    /** Dopłata za jedną sztukę opcji; Money.ZERO = „bez dopłaty”. */
    val surcharge: Money,
)
