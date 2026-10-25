package com.karolina.cieslik.kafe.feature.menu.domain

import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.EmptyResult
import com.karolina.cieslik.kafe.core.domain.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Wspólne menu do testów domeny: osobny strumień na lokal, [emit] podmienia menu lokalu (null = niezapamiętane),
 * a [refresh] zwraca [refreshResult].
 */
internal class FakeMenuRepository(menus: Map<String, Menu?> = emptyMap()) : MenuRepository {
    private val flows = menus.mapValues { (_, menu) -> MutableStateFlow(menu) }.toMutableMap()
    var refreshResult: EmptyResult<DataError> = Result.Success(Unit)

    fun emit(cafeId: String, menu: Menu?) {
        flow(cafeId).value = menu
    }

    override fun observeMenu(cafeId: String): Flow<Menu?> = flow(cafeId)

    override suspend fun refresh(cafeId: String): EmptyResult<DataError> = refreshResult

    private fun flow(cafeId: String) = flows.getOrPut(cafeId) { MutableStateFlow(null) }
}
