package com.karolina.cieslik.kafe.feature.menu.data

import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.EmptyResult
import com.karolina.cieslik.kafe.core.domain.Result
import com.karolina.cieslik.kafe.feature.menu.data.remote.MenuDto
import com.karolina.cieslik.kafe.feature.menu.data.remote.MenuRemoteDataSource
import com.karolina.cieslik.kafe.feature.menu.domain.Menu
import com.karolina.cieslik.kafe.feature.menu.domain.MenuRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update

// KAFE-6: Tymczasowy cache w pamięci (żyje tyle co proces). Docelowo Room jako źródło prawdy (offline-first).
class InMemoryMenuRepository(
    private val remote: MenuRemoteDataSource,
) : MenuRepository {

    private val cachedMenu = MutableStateFlow<MenuDto?>(null)
    private val cachedAvailability = MutableStateFlow<Map<String, Map<String, Boolean>>>(emptyMap())

    // Menu dla lokalu jest „zapamiętane” dopiero, gdy mamy i menu sieci, i dostępność w tym lokalu –
    // inaczej pokazalibyśmy niedostępne produkty jako dostępne.
    override fun observeMenu(cafeId: String): Flow<Menu?> =
        combine(cachedMenu, cachedAvailability) { menu, availability ->
            val cafeAvailability = availability[cafeId]
            if (menu == null || cafeAvailability == null) null else menu.toDomain(cafeAvailability)
        }.distinctUntilChanged()

    override suspend fun refresh(cafeId: String): EmptyResult<DataError> {
        val menu = when (val result = remote.fetchMenu()) {
            is Result.Success -> result.data
            is Result.Failure -> return result
        }
        val availability = when (val result = remote.fetchAvailability(cafeId)) {
            is Result.Success -> result.data
            is Result.Failure -> return result
        }
        // Zapis po pobraniu obu części, żeby obserwatorzy nie dostali niespójnego stanu.
        cachedMenu.value = menu
        cachedAvailability.update { it + (cafeId to availability) }
        return Result.Success(Unit)
    }
}
