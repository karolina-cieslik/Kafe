package com.karolina.cieslik.kafe.feature.product.presentation

// KAFE-9 / KAFE-10 / KAFE-11: Kontrakt ekranu szczegółów produktu (uzgodniony z ui).
// Wszystkie etykiety (ceny, pojemności, limity) są już sformatowane – UI ich nie liczy.
sealed interface ProductDetailUiState {

    data object Loading : ProductDetailUiState

    data class Error(val message: String) : ProductDetailUiState

    data class Content(
        val name: String,
        val description: String?,
        val imageUrl: String?,
        /** Produkt dostępny w wybranym lokalu (KAFE-7). */
        val isAvailable: Boolean,
        /** Jeden element = produkt jednorozmiarowy, UI ukrywa wybór rozmiaru. */
        val sizes: List<ProductSizeUi>,
        val modifierGroups: List<ModifierGroupUi>,
        val quantity: Int,
        /** Cena za wszystkie sztuki z dopłatami, np. „27,00 zł” (KAFE-11). */
        val totalPriceLabel: String,
        val canAddToCart: Boolean,
        /** Np. powód, dla którego nie da się dodać do koszyka; null = brak komunikatu. */
        val infoMessage: String? = null,
    ) : ProductDetailUiState
}

data class ProductSizeUi(
    /** Kod rozmiaru, np. "S" / "M" / "L" – wraca w [ProductDetailAction.SizeSelected]. */
    val code: String,
    val label: String,
    /** Np. „350 ml”; null, gdy produkt nie ma pojemności. */
    val volumeLabel: String?,
    val priceLabel: String,
    val isAvailable: Boolean,
    val isSelected: Boolean,
)

enum class ModifierGroupTypeUi {
    /** Wybór jednej opcji (np. mleko, syrop). */
    SINGLE,

    /** Opcje z licznikiem (np. dodatkowe shoty espresso). */
    QUANTITY,
}

data class ModifierGroupUi(
    val id: String,
    val name: String,
    val type: ModifierGroupTypeUi,
    val isRequired: Boolean,
    /** Np. „maks. 4”; null, gdy nie ma czego pokazać. */
    val limitLabel: String?,
    val options: List<ModifierOptionUi>,
)

data class ModifierOptionUi(
    val id: String,
    val name: String,
    /** „+2,00 zł” albo „bez dopłaty”. */
    val surchargeLabel: String,
    val isAvailable: Boolean,
    /** SINGLE: opcja wybrana; QUANTITY: quantity > 0. */
    val isSelected: Boolean,
    /** SINGLE: 0 albo 1. */
    val quantity: Int,
    val canIncrease: Boolean,
    val canDecrease: Boolean,
)

sealed interface ProductDetailAction {
    data class SizeSelected(val code: String) : ProductDetailAction

    /** SINGLE: wybór opcji; w grupie opcjonalnej ponowny wybór zaznaczonej opcji ją odznacza. */
    data class OptionSelected(val groupId: String, val optionId: String) : ProductDetailAction

    /** QUANTITY: delta +1 / -1. */
    data class OptionQuantityChanged(val groupId: String, val optionId: String, val delta: Int) : ProductDetailAction

    data class QuantityChanged(val delta: Int) : ProductDetailAction
    data object AddToCartClicked : ProductDetailAction
    data object BackClicked : ProductDetailAction
    data object Retry : ProductDetailAction
}

sealed interface ProductDetailEvent {
    data class AddedToCart(val productName: String) : ProductDetailEvent
    data object NavigateBack : ProductDetailEvent
}
