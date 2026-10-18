package com.karolina.cieslik.kafe.feature.cafe.data

import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.EmptyResult
import com.karolina.cieslik.kafe.core.domain.Result
import com.karolina.cieslik.kafe.feature.cafe.data.local.CafeLocalDataSource
import com.karolina.cieslik.kafe.feature.cafe.data.remote.CafeRemoteDataSource
import com.karolina.cieslik.kafe.feature.cafe.domain.Cafe
import com.karolina.cieslik.kafe.feature.cafe.domain.CafeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

/**
 * Offline-first: Room jest źródłem prawdy, [refresh] pobiera lokale i zapisuje je lokalnie.
 *
 * Wybór lokalu żyje tylko w pamięci tej instancji – po restarcie aplikacji wraca pierwszy lokal sieci.
 *
 * @param clock źródło czasu zapisu synchronizacji; w testach podmieniane.
 */
class OfflineFirstCafeRepository(
    private val local: CafeLocalDataSource,
    private val remote: CafeRemoteDataSource,
    private val clock: Clock = Clock.System,
) : CafeRepository {

    // TODO(KAFE-31): zapamiętanie wybranego lokalu między uruchomieniami.
    private val selectedCafeId = MutableStateFlow<String?>(null)

    override fun observeCafes(): Flow<List<Cafe>> = local.observeCafes().map { it.orEmpty() }

    override fun observeSelectedCafe(): Flow<Cafe?> =
        combine(observeCafes(), selectedCafeId) { cafes, selectedId ->
            cafes.firstOrNull { it.id == selectedId } ?: cafes.firstOrNull()
        }.distinctUntilChanged()

    override suspend fun selectCafe(cafeId: String) {
        selectedCafeId.value = cafeId
    }

    /** Błąd sieci zwraca bez zapisu, więc zapamiętane lokale zostają bez zmian. */
    override suspend fun refresh(): EmptyResult<DataError> =
        when (val result = remote.fetchCafes()) {
            is Result.Success -> local.saveCafes(result.data, clock.now().toEpochMilliseconds())
            is Result.Failure -> result
        }
}
