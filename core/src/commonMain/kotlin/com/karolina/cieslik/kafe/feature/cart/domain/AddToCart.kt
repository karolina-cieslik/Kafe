package com.karolina.cieslik.kafe.feature.cart.domain

import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.Error
import com.karolina.cieslik.kafe.core.domain.Result
import com.karolina.cieslik.kafe.feature.product.domain.ConfigurationIssue
import com.karolina.cieslik.kafe.feature.product.domain.PriceCalculator
import com.karolina.cieslik.kafe.feature.product.domain.PricedOption
import com.karolina.cieslik.kafe.feature.product.domain.ProductConfiguration
import com.karolina.cieslik.kafe.feature.product.domain.ProductDetails

/** Błąd dodania do koszyka. */
sealed interface AddToCartError : Error {

    /**
     * Konfiguracji nie można zamówić (ta sama walidacja co na ekranie produktu).
     *
     * @property issue powód odrzucenia konfiguracji.
     */
    data class InvalidConfiguration(val issue: ConfigurationIssue) : AddToCartError

    /**
     * Zapis w koszyku się nie udał.
     *
     * @property error błąd lokalnej bazy z repozytorium.
     */
    data class Storage(val error: DataError.Local) : AddToCartError
}

/** Przypadek użycia: dodanie skonfigurowanego produktu do koszyka. */
class AddToCart(
    private val cartRepository: CartRepository,
    private val priceCalculator: PriceCalculator,
) {

    /**
     * Waliduje [configuration] względem [details] ([ProductConfiguration.validate]), liczy cenę jednej sztuki
     * z dopłatami ([PriceCalculator]) i dodaje pozycję przez [CartRepository.add]. Do koszyka trafiają tylko
     * opcje z grup produktu – te same, które liczy cena. Liczba sztuk jest przycinana do 1–20.
     * Zakłada konfigurację zbudowaną przez [ProductConfiguration] – limitów w grupach (np. maks. 4 shoty) nie
     * sprawdza ponownie.
     *
     * @return wynik z repozytorium ([AddToCartOutcome]); [AddToCartError.InvalidConfiguration] bez zapisu, gdy
     * konfiguracja jest niepoprawna; [AddToCartError.Storage] przy błędzie bazy.
     */
    suspend operator fun invoke(
        details: ProductDetails,
        configuration: ProductConfiguration,
    ): Result<AddToCartOutcome, AddToCartError> {
        configuration.validate(details)?.let { return Result.Failure(AddToCartError.InvalidConfiguration(it)) }
        val selections = configuration.selectionsIn(details)
        // validate gwarantuje, że wybrany rozmiar istnieje w produkcie.
        val sizePrice = details.product.sizes.first { it.code == configuration.size }.price
        val item = NewCartItem(
            productId = details.product.id,
            productName = details.product.name,
            size = configuration.size,
            selections = selections,
            quantity = configuration.quantity
                .coerceIn(ProductConfiguration.MIN_QUANTITY, ProductConfiguration.MAX_QUANTITY),
            // Cena z tej samej listy co migawka opcji – nie mogą się rozjechać.
            unitPrice = priceCalculator.unitPrice(
                sizePrice,
                selections.map { PricedOption(it.surcharge, it.quantity) },
            ),
        )
        return when (val result = cartRepository.add(item)) {
            is Result.Success -> result
            is Result.Failure -> Result.Failure(AddToCartError.Storage(result.error))
        }
    }

    /** Wybór ograniczony do grup i opcji produktu – opcje usunięte z menu nie wpływają na cenę ani na klucz. */
    private fun ProductConfiguration.selectionsIn(details: ProductDetails): List<CartSelection> =
        details.modifierGroups.flatMap { group ->
            group.options.mapNotNull { option ->
                quantityOf(group.id, option.id).takeIf { it > 0 }?.let { quantity ->
                    CartSelection(
                        group.id, option.id, option.name, option.surcharge, quantity,
                        isDefault = option.id == group.defaultOptionId,
                    )
                }
            }
        }
}
