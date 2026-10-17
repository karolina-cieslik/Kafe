package com.karolina.cieslik.kafe.feature.menu.data.remote

import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.Result

// KAFE-6: Zdalne źródło menu. Na razie implementacja seed w pamięci; docelowo Firebase przez Ktor (REST).
interface MenuRemoteDataSource {

    suspend fun fetchMenu(): Result<MenuDto, DataError.Remote>

    /** Mapa productId -> dostępność w lokalu. Brak wpisu oznacza produkt dostępny. */
    suspend fun fetchAvailability(cafeId: String): Result<Map<String, Boolean>, DataError.Remote>
}
