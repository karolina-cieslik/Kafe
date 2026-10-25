package com.karolina.cieslik.kafe.feature.cart.domain

import com.karolina.cieslik.kafe.core.domain.Money

/**
 * Nowa migawka nazw i cen jednej pozycji z aktualnego menu ([CartRepository.updateSnapshots]). Zmienia tylko
 * nazwy i kwoty – rozmiar, zbiór opcji i ich ilości zostają, więc klucz konfiguracji też.
 *
 * @property item pozycja z koszyka, której migawka jest aktualizowana.
 * @property productName aktualna nazwa produktu.
 * @property unitPrice aktualna cena jednej sztuki z dopłatami.
 * @property options aktualne nazwy i dopłaty – klucze to dokładnie opcje z [CartItem.selections].
 */
data class CartSnapshotUpdate(
    val item: CartItem,
    val productName: String,
    val unitPrice: Money,
    val options: Map<OptionRef, OptionSnapshot>,
)

/**
 * Opcja w grupie modyfikatorów.
 *
 * @property groupId id grupy.
 * @property optionId id opcji w grupie.
 */
data class OptionRef(val groupId: String, val optionId: String)

/**
 * Aktualna nazwa i dopłata opcji.
 *
 * @property optionName nazwa opcji.
 * @property surcharge dopłata za jedną sztukę opcji.
 */
data class OptionSnapshot(val optionName: String, val surcharge: Money)
