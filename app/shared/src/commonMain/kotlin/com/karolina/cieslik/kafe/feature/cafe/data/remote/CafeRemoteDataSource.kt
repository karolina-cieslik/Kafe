package com.karolina.cieslik.kafe.feature.cafe.data.remote

import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.Result
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlin.time.Duration

// KAFE-6: DTO i zdalne źródło lokali (docelowo /cafes w Firebase Realtime DB).
@Serializable
data class CafeDto(
    val id: String,
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val openingHours: List<OpeningHoursDto> = emptyList(),
)

/** day: MONDAY…SUNDAY, opens/closes: „HH:mm”. */
@Serializable
data class OpeningHoursDto(
    val day: String,
    val opens: String,
    val closes: String,
)

interface CafeRemoteDataSource {
    suspend fun fetchCafes(): Result<List<CafeDto>, DataError.Remote>
}

class SeedCafeRemoteDataSource(
    private val latency: Duration = Duration.ZERO,
) : CafeRemoteDataSource {

    override suspend fun fetchCafes(): Result<List<CafeDto>, DataError.Remote> {
        delay(latency)
        return Result.Success(SeedCafes.cafes)
    }
}

internal object SeedCafes {

    const val KRAKOW = "krakow-rynek"
    const val WARSZAWA = "warszawa-nowy-swiat"
    const val GDANSK = "gdansk-dluga"

    val cafes = listOf(
        CafeDto(
            id = KRAKOW,
            name = "Kafe Rynek",
            address = "Rynek Główny 12, 31-042 Kraków",
            lat = 50.0617,
            lng = 19.9373,
            openingHours = week(weekdays = "07:00" to "20:00", weekend = "09:00" to "21:00"),
        ),
        CafeDto(
            id = WARSZAWA,
            name = "Kafe Nowy Świat",
            address = "ul. Nowy Świat 22, 00-373 Warszawa",
            lat = 52.2338,
            lng = 21.0182,
            openingHours = week(weekdays = "06:30" to "21:00", weekend = "08:00" to "21:00"),
        ),
        CafeDto(
            id = GDANSK,
            name = "Kafe Długa",
            address = "ul. Długa 45, 80-831 Gdańsk",
            lat = 54.3489,
            lng = 18.6512,
            openingHours = week(weekdays = "08:00" to "19:00", weekend = "09:00" to "18:00"),
        ),
    )

    private fun week(weekdays: Pair<String, String>, weekend: Pair<String, String>): List<OpeningHoursDto> =
        listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY").map { OpeningHoursDto(it, weekdays.first, weekdays.second) } +
            listOf("SATURDAY", "SUNDAY").map { OpeningHoursDto(it, weekend.first, weekend.second) }
}
