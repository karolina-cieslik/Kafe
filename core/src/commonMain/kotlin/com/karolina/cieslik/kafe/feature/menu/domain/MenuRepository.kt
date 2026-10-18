package com.karolina.cieslik.kafe.feature.menu.domain

import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.EmptyResult
import kotlinx.coroutines.flow.Flow

/** Offline-first: [observeMenu] emituje zapamiętane menu, [refresh] dociąga dane z serwera w tle. */
interface MenuRepository {

    /**
     * Zapamiętane menu dla lokalu [cafeId]; null, gdy menu nie zostało jeszcze nigdy pobrane.
     * Emituje od razu stan z bazy i ponownie po każdym zapisie menu lub dostępności.
     */
    fun observeMenu(cafeId: String): Flow<Menu?>

    /**
     * Pobiera menu sieci i dostępność w lokalu [cafeId] z serwera i zapisuje je lokalnie.
     *
     * @return sukces albo [DataError.Remote] przy błędzie sieci lub [DataError.Local] przy błędzie zapisu;
     * przy błędzie zapamiętane menu zostaje bez zmian.
     */
    suspend fun refresh(cafeId: String): EmptyResult<DataError>
}
