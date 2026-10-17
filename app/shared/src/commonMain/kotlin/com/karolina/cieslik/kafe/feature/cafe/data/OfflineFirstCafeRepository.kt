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
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock

// KAFE-6: Offline-first – zapamiętane lokale od razu, a raz na życie procesu odświeżenie w tle przy pierwszej
// obserwacji. Wybór lokalu na razie tylko w pamięci (zapamiętanie wyboru i mapa – KAFE-7).
class OfflineFirstCafeRepository(
    private val local: CafeLocalDataSource,
    private val remote: CafeRemoteDataSource,
    private val clock: Clock = Clock.System,
) : CafeRepository {

    private val selectedCafeId = MutableStateFlow<String?>(null)
    private val backgroundRefreshMutex = Mutex()
    private var backgroundRefreshDone = false

    override fun observeCafes(): Flow<List<Cafe>> = channelFlow {
        launch { refreshOncePerProcess() }
        local.observeCafes().filterNotNull().collect { send(it) }
    }

    override fun observeSelectedCafe(): Flow<Cafe?> =
        combine(observeCafes(), selectedCafeId) { cafes, selectedId ->
            cafes.firstOrNull { it.id == selectedId } ?: cafes.firstOrNull()
        }.distinctUntilChanged()

    override suspend fun selectCafe(cafeId: String) {
        selectedCafeId.value = cafeId
    }

    override suspend fun refresh(): EmptyResult<DataError> =
        when (val result = remote.fetchCafes()) {
            is Result.Success -> local.saveCafes(result.data, clock.now().toEpochMilliseconds())
            is Result.Failure -> result
        }

    // Nieudane odświeżenie nie jest zapamiętywane – kolejna obserwacja spróbuje ponownie.
    private suspend fun refreshOncePerProcess() {
        backgroundRefreshMutex.withLock {
            if (backgroundRefreshDone) return
            if (refresh() is Result.Success) backgroundRefreshDone = true
        }
    }
}
