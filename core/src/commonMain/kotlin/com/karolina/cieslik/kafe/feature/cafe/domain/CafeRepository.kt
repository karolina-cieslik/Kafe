package com.karolina.cieslik.kafe.feature.cafe.domain

import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.EmptyResult
import kotlinx.coroutines.flow.Flow

/**
 * Źródło lokali sieci oraz aktualnie wybranego lokalu.
 *
 * Obserwacja zwraca tylko zapamiętane dane i niczego nie pobiera – odświeżenie ([refresh]) wywołuje warstwa
 * prezentacji, dzięki czemu może pokazać błąd i ponowić próbę.
 */
interface CafeRepository {

    /**
     * Zapamiętane lokale; pusta lista, gdy lokale nie zostały jeszcze pobrane.
     * Emituje od razu stan z bazy i ponownie po każdym zapisie lokali.
     */
    fun observeCafes(): Flow<List<Cafe>>

    /**
     * Wybrany lokal; gdy użytkownik jeszcze nie wybrał – pierwszy lokal sieci, null gdy brak zapamiętanych lokali.
     * Emituje po każdej zmianie listy lokali albo wyboru.
     */
    fun observeSelectedCafe(): Flow<Cafe?>

    /** Zapamiętuje wybór lokalu [cafeId]; [observeSelectedCafe] wyemituje nowy lokal. */
    suspend fun selectCafe(cafeId: String)

    /**
     * Pobiera lokale z serwera i zapisuje je lokalnie.
     *
     * @return sukces albo [DataError.Remote] przy błędzie sieci lub [DataError.Local] przy błędzie zapisu;
     * przy błędzie zapamiętane dane zostają bez zmian.
     */
    suspend fun refresh(): EmptyResult<DataError>
}
