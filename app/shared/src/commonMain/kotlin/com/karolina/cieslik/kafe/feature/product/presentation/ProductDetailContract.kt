package com.karolina.cieslik.kafe.feature.product.presentation

/**
 * Stan ekranu szczegółów produktu; kontrakt uzgodniony z ui. Wszystkie etykiety (ceny, pojemności, limity)
 * są już sformatowane – UI ich nie liczy.
 */
sealed interface ProductDetailUiState {

    /** Menu lokalu nie zostało jeszcze pobrane. */
    data object Loading : ProductDetailUiState

    /**
     * Produktu nie da się pokazać (np. zniknął z menu).
     *
     * @property message gotowy komunikat dla użytkownika.
     */
    data class Error(val message: String) : ProductDetailUiState

    /**
     * Produkt z bieżącą konfiguracją.
     *
     * @property name nazwa produktu.
     * @property description opis; null, gdy brak.
     * @property imageUrl adres zdjęcia; null = grafika zastępcza.
     * @property isAvailable produkt dostępny w wybranym lokalu.
     * @property sizes rozmiary; jeden element = produkt jednorozmiarowy, UI ukrywa wybór rozmiaru.
     * @property modifierGroups grupy modyfikatorów w kolejności wyświetlania.
     * @property quantity liczba sztuk.
     * @property canIncreaseQuantity przycisk „+” przy liczbie sztuk aktywny – poniżej limitu 20 sztuk;
     * domyślnie true dla zgodności wstecz z pierwszą wersją kontraktu.
     * @property canDecreaseQuantity przycisk „−” przy liczbie sztuk aktywny – powyżej 1 sztuki; domyślnie true.
     * @property totalPriceLabel cena za wszystkie sztuki z dopłatami, np. „27,00 zł”.
     * @property canAddToCart konfiguracja jest poprawna i produkt można dodać do koszyka.
     * @property infoMessage np. powód, dla którego nie da się dodać do koszyka; null = brak komunikatu.
     */
    data class Content(
        val name: String,
        val description: String?,
        val imageUrl: String?,
        val isAvailable: Boolean,
        val sizes: List<ProductSizeUi>,
        val modifierGroups: List<ModifierGroupUi>,
        val quantity: Int,
        val canIncreaseQuantity: Boolean = true,
        val canDecreaseQuantity: Boolean = true,
        val totalPriceLabel: String,
        val canAddToCart: Boolean,
        val infoMessage: String? = null,
    ) : ProductDetailUiState
}

/**
 * Rozmiar na ekranie produktu.
 *
 * @property code kod rozmiaru, np. "S" / "M" / "L" – wraca w [ProductDetailAction.SizeSelected].
 * @property label nazwa rozmiaru, np. „Średni”.
 * @property volumeLabel np. „350 ml”; null, gdy produkt nie ma pojemności.
 * @property priceLabel pełna cena produktu w tym rozmiarze, np. „14,00 zł”.
 * @property isAvailable rozmiar dostępny w lokalu.
 * @property isSelected rozmiar wybrany.
 */
data class ProductSizeUi(
    val code: String,
    val label: String,
    val volumeLabel: String?,
    val priceLabel: String,
    val isAvailable: Boolean,
    val isSelected: Boolean,
)

/** Sposób wyboru opcji w grupie – decyduje o kontrolce w UI. */
enum class ModifierGroupTypeUi {
    /** Wybór jednej opcji (np. mleko, syrop). */
    SINGLE,

    /** Opcje z licznikiem (np. dodatkowe shoty espresso). */
    QUANTITY,
}

/**
 * Grupa modyfikatorów na ekranie produktu.
 *
 * @property id identyfikator grupy – wraca w akcjach opcji.
 * @property name nazwa grupy, np. „Mleko”.
 * @property type sposób wyboru opcji.
 * @property isRequired grupa wymagana (UI może oznaczyć ją jako obowiązkową).
 * @property limitLabel np. „maks. 4”; null, gdy nie ma czego pokazać.
 * @property options opcje w kolejności wyświetlania.
 */
data class ModifierGroupUi(
    val id: String,
    val name: String,
    val type: ModifierGroupTypeUi,
    val isRequired: Boolean,
    val limitLabel: String?,
    val options: List<ModifierOptionUi>,
)

/**
 * Opcja modyfikatora na ekranie produktu.
 *
 * @property id identyfikator opcji – wraca w akcjach.
 * @property name nazwa opcji, np. „Owsiane”.
 * @property surchargeLabel „+2,00 zł” albo „bez dopłaty” – dopłata za jedną sztukę opcji.
 * @property isAvailable opcja dostępna w lokalu.
 * @property isSelected SINGLE: opcja wybrana; QUANTITY: quantity > 0.
 * @property quantity ilość opcji; dla SINGLE 0 albo 1.
 * @property canIncrease QUANTITY: przycisk „+” aktywny – limit grupy nieosiągnięty i opcja dostępna.
 * SINGLE: opcję można zaznaczyć – jest dostępna i jeszcze niewybrana.
 * @property canDecrease QUANTITY: przycisk „−” aktywny – ilość > 0. SINGLE: opcję można odznaczyć – jest wybrana,
 * a grupa jest opcjonalna.
 */
data class ModifierOptionUi(
    val id: String,
    val name: String,
    val surchargeLabel: String,
    val isAvailable: Boolean,
    val isSelected: Boolean,
    val quantity: Int,
    val canIncrease: Boolean,
    val canDecrease: Boolean,
)

/** Akcje użytkownika na ekranie produktu. */
sealed interface ProductDetailAction {
    /** Wybór rozmiaru [code] (z [ProductSizeUi.code]). */
    data class SizeSelected(val code: String) : ProductDetailAction

    /**
     * SINGLE: wybór opcji; w grupie opcjonalnej ponowny wybór zaznaczonej opcji ją odznacza.
     *
     * @property groupId id grupy modyfikatorów (z [ModifierGroupUi.id]).
     * @property optionId id wybranej opcji w tej grupie.
     */
    data class OptionSelected(val groupId: String, val optionId: String) : ProductDetailAction

    /**
     * QUANTITY: zmiana ilości opcji o [delta] (+1 / -1).
     *
     * @property groupId id grupy modyfikatorów (z [ModifierGroupUi.id]).
     * @property optionId id opcji, której ilość się zmienia.
     * @property delta zmiana ilości: +1 albo -1.
     */
    data class OptionQuantityChanged(
        val groupId: String,
        val optionId: String,
        val delta: Int,
    ) : ProductDetailAction

    /** Zmiana liczby sztuk o [delta] (+1 / -1). */
    data class QuantityChanged(val delta: Int) : ProductDetailAction

    /** „Dodaj do koszyka”. */
    data object AddToCartClicked : ProductDetailAction

    /** Strzałka wstecz. */
    data object BackClicked : ProductDetailAction

    /** „Zobacz koszyk” w snackbarze po dodaniu do koszyka. */
    data object ViewCartClicked : ProductDetailAction

    /** „Spróbuj ponownie” na ekranie błędu. */
    data object Retry : ProductDetailAction
}

/** Jednorazowe zdarzenia ekranu produktu – zbiera je route ekranu. */
sealed interface ProductDetailEvent {
    /**
     * Produkt dodany do koszyka (nowa pozycja albo zwiększona ilość identycznej) – UI pokazuje potwierdzenie.
     *
     * @property productName nazwa produktu do komunikatu „Dodano do koszyka: …”.
     * @property limitReached pozycja w koszyku doszła do limitu 20 sztuk i nadmiar nie został dodany – UI
     * pokazuje „W koszyku jest już maks. 20 szt. tej pozycji” zamiast zwykłego potwierdzenia.
     */
    data class AddedToCart(val productName: String, val limitReached: Boolean = false) : ProductDetailEvent

    /** Zapis w koszyku się nie udał (błąd bazy) – UI pokazuje „Nie udało się dodać do koszyka. Spróbuj ponownie.”. */
    data object AddToCartFailed : ProductDetailEvent

    /** Powrót do poprzedniego ekranu. */
    data object NavigateBack : ProductDetailEvent

    /** Przejście do koszyka. */
    data object NavigateToCart : ProductDetailEvent
}
