package com.karolina.cieslik.kafe.feature.cafe.data

import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.EmptyResult
import com.karolina.cieslik.kafe.core.domain.Result
import com.karolina.cieslik.kafe.feature.cafe.data.local.CafeLocalDataSource
import com.karolina.cieslik.kafe.feature.cafe.data.local.SelectedCafeStore
import com.karolina.cieslik.kafe.feature.cafe.data.remote.CafeRemoteDataSource
import com.karolina.cieslik.kafe.feature.cafe.domain.Cafe
import com.karolina.cieslik.kafe.feature.cafe.domain.CafeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

// KAFE-6: Offline-first – Room jest źródłem prawdy, refresh() pobiera lokale i zapisuje je lokalnie.
// KAFE-31: wybór lokalu zapamiętany w SelectedCafeStore (DataStore), bez domyślnego lokalu.
class OfflineFirstCafeRepository(
    private val local: CafeLocalDataSource,
    private val remote: CafeRemoteDataSource,
    private val selectedCafeStore: SelectedCafeStore,
    private val clock: Clock = Clock.System,
) : CafeRepository {

    override fun observeCafes(): Flow<List<Cafe>> = local.observeCafes().map { it.orEmpty() }

    override fun observeSelectedCafe(): Flow<Cafe?> =
        combine(observeCafes(), selectedCafeStore.observeSelectedCafeId()) { cafes, selectedId ->
            cafes.firstOrNull { it.id == selectedId }
        }.distinctUntilChanged()

    override suspend fun selectCafe(cafeId: String) {
        selectedCafeStore.saveSelectedCafeId(cafeId)
    }

    override suspend fun refresh(): EmptyResult<DataError> =
        when (val result = remote.fetchCafes()) {
            is Result.Success -> local.saveCafes(result.data, clock.now().toEpochMilliseconds())
            is Result.Failure -> result
        }
}
