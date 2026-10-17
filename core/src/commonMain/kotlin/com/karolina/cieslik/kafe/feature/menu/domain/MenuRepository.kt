package com.karolina.cieslik.kafe.feature.menu.domain

import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.EmptyResult
import kotlinx.coroutines.flow.Flow

// KAFE-6: Offline-first – observeMenu emituje zapamiętane menu, refresh dociąga dane z serwera w tle.
interface MenuRepository {

    /** Zapamiętane menu dla lokalu; null, gdy menu nie zostało jeszcze nigdy pobrane (AC 6–7). */
    fun observeMenu(cafeId: String): Flow<Menu?>

    suspend fun refresh(cafeId: String): EmptyResult<DataError>
}
