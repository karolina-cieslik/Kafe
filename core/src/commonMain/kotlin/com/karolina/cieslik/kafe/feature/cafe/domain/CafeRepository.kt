package com.karolina.cieslik.kafe.feature.cafe.domain

import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.EmptyResult
import kotlinx.coroutines.flow.Flow

// KAFE-6: Źródło lokali sieci oraz aktualnie wybranego lokalu (wybór i zapamiętanie między sesjami – KAFE-31).
// Obserwacja zwraca tylko zapamiętane dane i niczego nie pobiera – odświeżenie (refresh) wywołuje warstwa
// prezentacji, dzięki czemu może pokazać błąd i ponowić próbę.
interface CafeRepository {

    /** Zapamiętane lokale; pusta lista, gdy lokale nie zostały jeszcze pobrane. */
    fun observeCafes(): Flow<List<Cafe>>

    /**
     * KAFE-31: Zapamiętany wybór użytkownika; null, gdy jeszcze nic nie wybrał (bez domyślnego lokalu)
     * albo wybranego lokalu nie ma wśród zapamiętanych.
     */
    fun observeSelectedCafe(): Flow<Cafe?>

    /** KAFE-31: Zapisuje wybór na stałe (przetrwa restart aplikacji). */
    suspend fun selectCafe(cafeId: String)

    suspend fun refresh(): EmptyResult<DataError>
}
