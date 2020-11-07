package usonia.foundation.unit

/**
 * Color temperature units.
 *
 * @param kelvinValue Color temperature in kelvin. Base unit to reduce rounding errors.
 */
inline class ColorTemperature(val kelvinValue: Int): Comparable<ColorTemperature> {
    /**
     * Value in Mireds. Base unit of the Hue API.
     */
    val miredValue: Int get() = 1_000_000 / kelvinValue

    override fun compareTo(other: ColorTemperature): Int = kelvinValue.compareTo(other.kelvinValue)
}
