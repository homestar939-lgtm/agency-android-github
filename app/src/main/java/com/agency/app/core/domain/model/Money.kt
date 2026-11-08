package com.agency.app.core.domain.model

/**
 * جميع المبالغ المالية في النظام تُخزَّن كـ [Long] بالسنت (cents)
 * لتجنّب أي خطأ تقريب عائم في حسابات الذمم والعمولات.
 */
@JvmInline
value class Money(val cents: Long) : Comparable<Money> {

    val isZero: Boolean get() = cents == 0L
    val isPositive: Boolean get() = cents > 0L
    val isNegative: Boolean get() = cents < 0L

    operator fun plus(other: Money) = Money(cents + other.cents)
    operator fun minus(other: Money) = Money(cents - other.cents)
    operator fun times(factor: Int) = Money(cents * factor)
    operator fun unaryMinus() = Money(-cents)

    /** عمولة بنسبة مئوية (بالمئة الكاملة أي 5 تعني 5%). */
    fun percent(percent: Int): Money =
        Money((cents * percent) / 100L)

    override fun compareTo(other: Money): Int = cents.compareTo(other.cents)

    override fun toString(): String {
        val negative = cents < 0
        val abs = if (negative) -cents else cents
        val whole = abs / 100
        val frac = abs % 100
        return buildString {
            if (negative) append('-')
            append(whole)
            append('.')
            if (frac < 10) append('0')
            append(frac)
        }
    }

    companion object {
        val ZERO = Money(0L)
        fun fromMajor(amount: Double): Money = Money(Math.round(amount * 100.0))
        fun fromMajor(amount: Long): Money = Money(amount * 100L)
        fun fromString(raw: String): Money? {
            val cleaned = raw.trim().replace(",", ".")
            if (cleaned.isEmpty()) return null
            val value = cleaned.toDoubleOrNull() ?: return null
            if (value < 0) return null
            return fromMajor(value)
        }
    }
}
