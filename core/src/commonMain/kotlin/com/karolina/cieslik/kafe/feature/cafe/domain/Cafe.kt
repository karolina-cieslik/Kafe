package com.karolina.cieslik.kafe.feature.cafe.domain

// KAFE-6: Lokal sieci – menu jest wspólne, a dostępność produktów zależy od lokalu (KAFE-7, KAFE-31).
data class Cafe(
    val id: String,
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val openingHours: List<OpeningHours>,
)

/** Godziny otwarcia w danym dniu; czas jako minuty od północy (np. 7:30 = 450), bez zależności od kotlinx-datetime. */
data class OpeningHours(
    val day: Weekday,
    val opensAtMinute: Int,
    val closesAtMinute: Int,
)

enum class Weekday { MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY }
