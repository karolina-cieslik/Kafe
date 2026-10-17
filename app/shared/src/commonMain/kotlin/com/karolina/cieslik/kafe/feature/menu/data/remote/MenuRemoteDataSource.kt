package com.karolina.cieslik.kafe.feature.menu.data.remote

import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.Result

// KAFE-6: Zdalne źródło menu. Na razie implementacja seed w pamięci; docelowo Firebase przez Ktor (REST).
interface MenuRemoteDataSource {

    suspend fun fetchMenu(): Result<MenuDto, DataError.Remote>

    /** Braki produktów, rozmiarów i opcji modyfikatorów w lokalu. Brak wpisu oznacza „dostępne”. */
    suspend fun fetchAvailability(cafeId: String): Result<AvailabilityDto, DataError.Remote>
}
