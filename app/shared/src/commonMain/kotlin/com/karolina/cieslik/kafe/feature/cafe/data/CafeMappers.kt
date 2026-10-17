package com.karolina.cieslik.kafe.feature.cafe.data

import com.karolina.cieslik.kafe.feature.cafe.data.remote.OpeningHoursDto
import com.karolina.cieslik.kafe.feature.cafe.domain.OpeningHours
import com.karolina.cieslik.kafe.feature.cafe.domain.Weekday

// KAFE-6: Parsowanie godzin otwarcia (wspólne dla DTO i encji Room). Błędne wpisy są pomijane.
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
