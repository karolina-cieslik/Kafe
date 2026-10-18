package com.karolina.cieslik.kafe.core.domain

import kotlin.jvm.JvmInline
import kotlin.math.absoluteValue

/**
 * Kwota w PLN trzymana w groszach – bez Double, żeby uniknąć błędów zaokrągleń przy sumowaniu koszyka.
 *
 * @property grosze kwota w groszach (1 zł = 100 gr); może być ujemna, np. przy rabacie.
 */
@JvmInline
value class Money(val grosze: Long) : Comparable<Money> {

    /** Suma kwot. */
    operator fun plus(other: Money): Money = Money(grosze + other.grosze)

    /** Różnica kwot; wynik może być ujemny. */
    operator fun minus(other: Money): Money = Money(grosze - other.grosze)

    /** Kwota pomnożona przez liczbę sztuk. */
    operator fun times(quantity: Int): Money = Money(grosze * quantity)

    override fun compareTo(other: Money): Int = grosze.compareTo(other.grosze)

    /** Format polski zgodny z kontraktem UI: „12,00 zł”, „1 234,50 zł” (zwykłe spacje). */
    fun format(): String {
        // Wartość bezwzględna dopiero po dzieleniu – -Long.MIN_VALUE by się przepełniło.
        val zlote = (grosze / GROSZE_IN_ZLOTY).absoluteValue.toString()
            .reversed()
            .chunked(3)
            .joinToString(" ")
            .reversed()
        val groszePart = (grosze % GROSZE_IN_ZLOTY).absoluteValue.toString().padStart(2, '0')
        val sign = if (grosze < 0) "-" else ""
        return "$sign$zlote,$groszePart zł"
    }

    /** Stałe i fabryki kwot. */
    companion object {
        private const val GROSZE_IN_ZLOTY = 100L

        /** Zero złotych. */
        val ZERO = Money(0)

        /** Kwota z pełnych złotych i groszy, np. `ofZlote(12, 50)` = 12,50 zł. */
        fun ofZlote(zlote: Long, grosze: Long = 0): Money = Money(zlote * GROSZE_IN_ZLOTY + grosze)
    }
}
