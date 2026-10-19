package com.karolina.cieslik.kafe.feature.menu.data

import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.EmptyResult
import com.karolina.cieslik.kafe.core.domain.Result
import com.karolina.cieslik.kafe.feature.menu.data.local.MenuLocalDataSource
import com.karolina.cieslik.kafe.feature.menu.data.remote.MenuRemoteDataSource
import com.karolina.cieslik.kafe.feature.menu.domain.Menu
import com.karolina.cieslik.kafe.feature.menu.domain.MenuRepository
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock

/**
 * Offline-first: Room jest źródłem prawdy, [refresh] pobiera menu i dostępność, a potem zapisuje je w jednej
 * transakcji. Błąd sieci nie rusza zapamiętanego menu.
 *
 * @param clock źródło czasu zapisu synchronizacji; w testach podmieniane.
 */
class OfflineFirstMenuRepository(
    private val local: MenuLocalDataSource,
    private val remote: MenuRemoteDataSource,
    private val clock: Clock = Clock.System,
) : MenuRepository {

    override fun observeMenu(cafeId: String): Flow<Menu?> = local.observeMenu(cafeId)

    /** Zapisuje dopiero, gdy oba zapytania się udały – pierwszy błąd przerywa odświeżanie bez zapisu. */
    override suspend fun refresh(cafeId: String): EmptyResult<DataError> {
        val menu = when (val result = remote.fetchMenu()) {
            is Result.Success -> result.data
            is Result.Failure -> return result
        }
        val availability = when (val result = remote.fetchAvailability(cafeId)) {
            is Result.Success -> result.data
            is Result.Failure -> return result
        }
        return local.saveMenu(
            cafeId = cafeId,
            menu = menu,
            availability = availability,
            syncedAtEpochMillis = clock.now().toEpochMilliseconds(),
        )
    }
}
