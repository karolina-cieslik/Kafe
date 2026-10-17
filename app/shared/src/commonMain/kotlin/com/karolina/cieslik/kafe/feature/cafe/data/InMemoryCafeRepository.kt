package com.karolina.cieslik.kafe.feature.cafe.data

import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.EmptyResult
import com.karolina.cieslik.kafe.core.domain.Result
import com.karolina.cieslik.kafe.feature.cafe.data.remote.CafeRemoteDataSource
import com.karolina.cieslik.kafe.feature.cafe.domain.Cafe
import com.karolina.cieslik.kafe.feature.cafe.domain.CafeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// KAFE-6: Tymczasowy cache lokali w pamięci; pierwsza obserwacja dociąga lokale ze źródła zdalnego.
// Docelowo Room + zapamiętany wybór lokalu (KAFE-7).
class InMemoryCafeRepository(
    private val remote: CafeRemoteDataSource,
) : CafeRepository {

    private val cachedCafes = MutableStateFlow<List<Cafe>?>(null)
    private val selectedCafeId = MutableStateFlow<String?>(null)
    private val initialLoadMutex = Mutex()

    override fun observeCafes(): Flow<List<Cafe>> =
        cachedCafes
            .onStart { loadIfMissing() }
            .filterNotNull()

    override fun observeSelectedCafe(): Flow<Cafe?> =
        combine(observeCafes(), selectedCafeId) { cafes, selectedId ->
            cafes.firstOrNull { it.id == selectedId } ?: cafes.firstOrNull()
        }.distinctUntilChanged()

    override suspend fun selectCafe(cafeId: String) {
        selectedCafeId.value = cafeId
    }

    override suspend fun refresh(): EmptyResult<DataError> =
        when (val result = remote.fetchCafes()) {
            is Result.Success -> {
                cachedCafes.value = result.data.map { it.toDomain() }
                Result.Success(Unit)
            }
            is Result.Failure -> result
        }

    // Kilku obserwatorów naraz nie powinno odpalać kilku pobrań.
    private suspend fun loadIfMissing() {
        initialLoadMutex.withLock {
            if (cachedCafes.value == null) refresh()
        }
    }
}
