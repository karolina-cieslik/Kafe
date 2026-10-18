package com.karolina.cieslik.kafe.feature.menu.data.remote

import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.Result

/** Zdalne źródło menu. Na razie implementacja seed w pamięci; docelowo Firebase przez Ktor (REST). */
interface MenuRemoteDataSource {

    /** Menu sieci z modyfikatorami; [DataError.Remote] przy błędzie sieci. */
    suspend fun fetchMenu(): Result<MenuDto, DataError.Remote>

    /**
     * Braki produktów, rozmiarów i opcji modyfikatorów w lokalu [cafeId]. Brak wpisu oznacza „dostępne”.
     * [DataError.Remote.NOT_FOUND], gdy lokal nie istnieje, inne [DataError.Remote] przy błędzie sieci.
     */
    suspend fun fetchAvailability(cafeId: String): Result<AvailabilityDto, DataError.Remote>
}
