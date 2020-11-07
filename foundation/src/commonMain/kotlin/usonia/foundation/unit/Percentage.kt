package usonia.foundation.unit

/**
 * Fractional representation of a percentage.
 *
 * @param fraction The percent as a fraction where 1 is 100%
 */
inline class Percentage(val fraction: Float) {
    /**
     * Percentage in hundredths.
     */
    val percent: Int get() = fraction.times(100).toInt()
}

/**
 * Use the current number as a fraction to represent a percentage.
 *
 * eg.
 *    .55.asPercentage // this is 55%
 */
val Number.asPercentage get() = Percentage(toFloat())

/**
 * Convert the current number to a percentage as read, by hundredths.
 *
 * eg.
 *    55.percent // this is 55%
 */
val Number.percent get() = toFloat().div(100).asPercentage
