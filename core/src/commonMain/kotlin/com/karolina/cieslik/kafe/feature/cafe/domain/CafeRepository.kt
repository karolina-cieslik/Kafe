package com.karolina.cieslik.kafe.feature.cafe.domain

import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.EmptyResult
import kotlinx.coroutines.flow.Flow

// KAFE-6: Źródło lokali sieci oraz aktualnie wybranego lokalu (wybór przez mapę/listę – KAFE-7).
interface CafeRepository {

    fun observeCafes(): Flow<List<Cafe>>

    /** Wybrany lokal; gdy użytkownik jeszcze nie wybrał – domyślnie pierwszy lokal sieci, null gdy brak lokali. */
    fun observeSelectedCafe(): Flow<Cafe?>

    suspend fun selectCafe(cafeId: String)

    suspend fun refresh(): EmptyResult<DataError>
}
