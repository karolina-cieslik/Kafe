package com.karolina.cieslik.kafe.feature.cart.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Pozycja koszyka w tabeli `cart_items`. Bez kluczy obcych do tabel menu – menu jest nadpisywane przy każdym
 * odświeżeniu, a pozycja ma przetrwać zniknięcie produktu. Kolejność pozycji w koszyku to [id] rosnąco.
 *
 * @property id autoincrement (SQLite AUTOINCREMENT) – id usuniętej pozycji nie jest użyte ponownie, więc
 * „Cofnij” może wstawić ją z powrotem z tym samym id i na to samo miejsce.
 * @property productId id produktu z menu sieci.
 * @property productName nazwa produktu z chwili dodania albo ostatniej aktualizacji migawki.
 * @property sizeCode nazwa SizeCode wielkimi literami (S/M/L); wiersz z nieznanym kodem jest pomijany przy odczycie.
 * @property selectionsJson migawka opcji jako JSON – lista [CartSelectionJson]; wiersz z błędnym JSON-em jest
 * pomijany przy odczycie.
 * @property configurationKey klucz konfiguracji z domeny (`cartConfigurationKey`); unikalny, bo identyczna
 * konfiguracja jest zawsze scalana w jedną pozycję.
 * @property quantity liczba sztuk, 1–20.
 * @property unitPriceGrosze cena jednej sztuki z dopłatami w groszach, z chwili ostatniego dodania albo
 * aktualizacji migawki.
 */
@Entity(tableName = "cart_items", indices = [Index(value = ["configurationKey"], unique = true)])
data class CartItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val productId: String,
    val productName: String,
    val sizeCode: String,
    val selectionsJson: String,
    val configurationKey: String,
    val quantity: Int,
    val unitPriceGrosze: Long,
)

/**
 * Element JSON-a w [CartItemEntity.selectionsJson]. Format zapisany w bazie – zmiana pól wymaga migracji danych
 * albo wartości domyślnych dla starych wierszy (nowe pole z wartością domyślną nie wymaga migracji).
 *
 * @property groupId id grupy modyfikatorów.
 * @property optionId id opcji.
 * @property optionName nazwa opcji z chwili dodania.
 * @property surchargeGrosze dopłata za jedną sztukę opcji w groszach.
 * @property quantity ilość opcji w jednej sztuce produktu, > 0.
 * @property isDefault opcja domyślna grupy w chwili dodania; wiersze sprzed dodania pola czytają się jako false
 * (opcja pokazana w koszyku).
 */
@Serializable
data class CartSelectionJson(
    val groupId: String,
    val optionId: String,
    val optionName: String,
    val surchargeGrosze: Long,
    val quantity: Int,
    val isDefault: Boolean = false,
)
