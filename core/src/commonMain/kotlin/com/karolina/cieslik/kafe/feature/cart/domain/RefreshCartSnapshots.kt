package com.karolina.cieslik.kafe.feature.cart.domain

import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.Money
import com.karolina.cieslik.kafe.core.domain.Result
import com.karolina.cieslik.kafe.core.domain.map
import com.karolina.cieslik.kafe.feature.menu.domain.MenuRepository
import com.karolina.cieslik.kafe.feature.product.domain.PriceCalculator
import kotlinx.coroutines.flow.first

/**
 * Zmiana ceny pozycji zapisana przez [RefreshCartSnapshots] – po zapisie domena zna już tylko nową cenę, więc
 * poprzednią (do „Cena została zaktualizowana”) przechowuje wołający.
 *
 * @property itemId id pozycji.
 * @property previousUnitPrice cena jednej sztuki przed zapisem.
 * @property newUnitPrice cena jednej sztuki po zapisie (z menu).
 */
data class PriceUpdate(val itemId: Long, val previousUnitPrice: Money, val newUnitPrice: Money)

/**
 * Przypadek użycia: zapis nowej migawki nazw i cen pozycji koszyka po **udanym** odświeżeniu menu lokalu. Osobno
 * od [ObserveCheckedCart], żeby zapis nie był efektem ubocznym obserwacji (nie powtarza się przy każdej emisji
 * i nie zapisuje cen z nieaktualnego menu offline).
 */
class RefreshCartSnapshots(
    private val cartRepository: CartRepository,
    private val menuRepository: MenuRepository,
    private val priceCalculator: PriceCalculator,
) {

    /**
     * Czyta bieżący koszyk i zapamiętane menu lokalu [cafeId] i jednym zapisem ([CartRepository.updateSnapshots])
     * aktualizuje migawki pozycji, które można zamówić, a których nazwy, dopłaty, flagi opcji domyślnej albo cena
     * różnią się od menu. Pozycji niedostępnych nie zmienia (zostaje cała stara migawka). Bez zmian albo bez
     * zapamiętanego menu – bez zapisu.
     *
     * @return zmiany cen (tylko pozycje z inną ceną; sama zmiana nazwy nie trafia na listę). Pozycja usunięta
     * między odczytem a zapisem też może się tu znaleźć (zapis ją pomija) – wołający pokazuje zmiany tylko dla
     * pozycji obecnych w koszyku. [DataError.Local] przy błędzie zapisu – wtedy nic nie jest zapisane.
     */
    suspend operator fun invoke(cafeId: String): Result<List<PriceUpdate>, DataError.Local> {
        val menu = menuRepository.observeMenu(cafeId).first() ?: return Result.Success(emptyList())
        val updates = cartRepository.observeCart().first().items
            .mapNotNull { (it.checkWith(menu, priceCalculator) as? CartItemCheck.Valid)?.snapshot }
            .filter { it.differsFromItem() }
        if (updates.isEmpty()) return Result.Success(emptyList())
        return cartRepository.updateSnapshots(updates).map {
            updates.filter { it.unitPrice != it.item.unitPrice }
                .map { PriceUpdate(it.item.id, previousUnitPrice = it.item.unitPrice, newUnitPrice = it.unitPrice) }
        }
    }
}
