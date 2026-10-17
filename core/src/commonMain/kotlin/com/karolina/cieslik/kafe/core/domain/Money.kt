package com.karolina.cieslik.kafe.core.domain

import kotlin.jvm.JvmInline

// KAFE-6: Kwota w PLN trzymana w groszach – bez Double, żeby uniknąć błędów zaokrągleń przy sumowaniu koszyka.
@JvmInline
value class Money(val grosze: Long) : Comparable<Money> {

    operator fun plus(other: Money): Money = Money(grosze + other.grosze)

    operator fun minus(other: Money): Money = Money(grosze - other.grosze)

    operator fun times(quantity: Int): Money = Money(grosze * quantity)

    override fun compareTo(other: Money): Int = grosze.compareTo(other.grosze)

    /** Format polski zgodny z kontraktem UI: „12,00 zł”, „1 234,50 zł” (zwykłe spacje). */
    fun format(): String {
        val absolute = if (grosze < 0) -grosze else grosze
        val zlote = (absolute / GROSZE_IN_ZLOTY).toString()
            .reversed()
            .chunked(3)
            .joinToString(" ")
            .reversed()
        val groszePart = (absolute % GROSZE_IN_ZLOTY).toString().padStart(2, '0')
        val sign = if (grosze < 0) "-" else ""
        return "$sign$zlote,$groszePart zł"
    }

    companion object {
        private const val GROSZE_IN_ZLOTY = 100L

        val ZERO = Money(0)

        fun ofZlote(zlote: Long, grosze: Long = 0): Money = Money(zlote * GROSZE_IN_ZLOTY + grosze)
    }
}
