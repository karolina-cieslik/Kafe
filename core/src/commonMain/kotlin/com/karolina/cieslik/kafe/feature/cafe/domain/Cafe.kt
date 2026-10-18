package com.karolina.cieslik.kafe.feature.cafe.domain

/**
 * Lokal sieci. Menu jest wspólne dla sieci, a dostępność produktów zależy od lokalu.
 *
 * @property id identyfikator lokalu z API (np. `rynek`).
 * @property name nazwa wyświetlana, np. „Kafe Rynek”.
 * @property address adres w jednej linii.
 * @property lat szerokość geograficzna (WGS84, stopnie).
 * @property lng długość geograficzna (WGS84, stopnie).
 * @property openingHours godziny otwarcia; dzień bez wpisu oznacza, że lokal jest wtedy zamknięty.
 */
data class Cafe(
    val id: String,
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val openingHours: List<OpeningHours>,
)

/**
 * Godziny otwarcia w danym dniu; czas jako minuty od północy czasu lokalnego (np. 7:30 = 450),
 * bez zależności od kotlinx-datetime.
 *
 * @property day dzień tygodnia, którego dotyczy wpis.
 * @property opensAtMinute godzina otwarcia w minutach od północy.
 * @property closesAtMinute godzina zamknięcia w minutach od północy.
 */
data class OpeningHours(
    val day: Weekday,
    val opensAtMinute: Int,
    val closesAtMinute: Int,
)

/** Dzień tygodnia; kolejność od poniedziałku jak w polskim kalendarzu. */
enum class Weekday { MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY }
