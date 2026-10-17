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

    /** Wybrany lokal; gdy użytkownik jeszcze nie wybrał – pierwszy lokal sieci, null gdy brak zapamiętanych lokali. */
    fun observeSelectedCafe(): Flow<Cafe?>

    suspend fun selectCafe(cafeId: String)

    suspend fun refresh(): EmptyResult<DataError>
}
