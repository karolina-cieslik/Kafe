package com.karolina.cieslik.kafe.feature.cafe.data

import com.karolina.cieslik.kafe.feature.cafe.data.remote.CafeDto
import com.karolina.cieslik.kafe.feature.cafe.data.remote.OpeningHoursDto
import com.karolina.cieslik.kafe.feature.cafe.domain.Cafe
import com.karolina.cieslik.kafe.feature.cafe.domain.OpeningHours
import com.karolina.cieslik.kafe.feature.cafe.domain.Weekday

// KAFE-6: Mapowanie DTO -> domena. Błędne wpisy godzin otwarcia są pomijane.
internal fun CafeDto.toDomain() = Cafe(
    id = id,
    name = name,
    address = address,
    lat = lat,
    lng = lng,
    openingHours = openingHours.mapNotNull { it.toDomainOrNull() },
)

internal fun OpeningHoursDto.toDomainOrNull(): OpeningHours? {
    val weekday = Weekday.entries.firstOrNull { it.name.equals(day, ignoreCase = true) } ?: return null
    val opensAt = opens.toMinuteOfDayOrNull() ?: return null
    val closesAt = closes.toMinuteOfDayOrNull() ?: return null
    return OpeningHours(day = weekday, opensAtMinute = opensAt, closesAtMinute = closesAt)
}

/** „HH:mm” -> minuty od północy; null dla niepoprawnego formatu. */
internal fun String.toMinuteOfDayOrNull(): Int? {
    val parts = trim().split(':')
    if (parts.size != 2) return null
    val hours = parts[0].toIntOrNull()?.takeIf { it in 0..23 } ?: return null
    val minutes = parts[1].toIntOrNull()?.takeIf { it in 0..59 } ?: return null
    return hours * 60 + minutes
}
