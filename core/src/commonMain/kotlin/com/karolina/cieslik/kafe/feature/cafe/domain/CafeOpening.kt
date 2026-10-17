package com.karolina.cieslik.kafe.feature.cafe.domain

// KAFE-29: „Otwarte teraz” liczone z godzin otwarcia. Czas lokalny (dzień + minuta) podaje warstwa prezentacji
// ze wstrzykiwanego zegara i strefy Europe/Warsaw, więc domena nie zależy od kotlinx-datetime.
const val MINUTES_IN_DAY = 24 * 60

/** Godziny w danym dniu; null = zamknięte. */
fun Cafe.openingHoursOn(day: Weekday): OpeningHours? = openingHours.firstOrNull { it.day == day }

/**
 * Przedział jest prawostronnie otwarty: 7:00–20:00 oznacza otwarte od 7:00 do 19:59.
 * Godziny „przez północ” (zamknięcie ≤ otwarcie, np. 18:00–2:00) obejmują też początek następnego dnia.
 */
fun Cafe.isOpenAt(day: Weekday, minuteOfDay: Int): Boolean {
    val today = openingHoursOn(day)
    if (today != null) {
        val closesSameDay = today.closesAtMinute > today.opensAtMinute
        if (closesSameDay && minuteOfDay in today.opensAtMinute until today.closesAtMinute) return true
        if (!closesSameDay && minuteOfDay >= today.opensAtMinute) return true
    }
    val yesterday = openingHoursOn(day.previous()) ?: return false
    return yesterday.closesAtMinute <= yesterday.opensAtMinute && minuteOfDay < yesterday.closesAtMinute
}

fun Weekday.previous(): Weekday = Weekday.entries[(ordinal + Weekday.entries.size - 1) % Weekday.entries.size]

/** Porządek alfabetyczny po polsku (ą po a, ł po l, ś po s…), bez rozróżniania wielkości liter. */
val PolishAlphabeticalOrder: Comparator<String> = Comparator { a, b ->
    val left = a.lowercase()
    val right = b.lowercase()
    for (i in 0 until minOf(left.length, right.length)) {
        val diff = left[i].polishRank().compareTo(right[i].polishRank())
        if (diff != 0) return@Comparator diff
    }
    left.length.compareTo(right.length)
}

private const val POLISH_ALPHABET = "aąbcćdeęfghijklłmnńoóprsśtuvwxyzźż"

// Litery spoza alfabetu (cyfry, spacje, obce znaki) przed literami, w kolejności kodów.
private fun Char.polishRank(): Int {
    val index = POLISH_ALPHABET.indexOf(this)
    return if (index >= 0) Char.MAX_VALUE.code + index else code
}
