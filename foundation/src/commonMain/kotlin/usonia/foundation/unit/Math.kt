package usonia.foundation.unit

import inkapplications.spondee.measure.ColorTemperature
import inkapplications.spondee.measure.metric.kelvin
import inkapplications.spondee.measure.us.Fahrenheit
import inkapplications.spondee.measure.us.fahrenheit
import inkapplications.spondee.scalar.Percentage
import inkapplications.spondee.scalar.percent
import inkapplications.spondee.scalar.toWholePercentage
import inkapplications.spondee.structure.toDouble

fun interpolate(start: ColorTemperature, end: ColorTemperature, position: Percentage) = interpolate(start, end, position.toDecimal().toDouble())
fun interpolate(start: ColorTemperature, end: ColorTemperature, position: Float) = interpolate(start, end, position.toDouble())
fun interpolate(start: ColorTemperature, end: ColorTemperature, position: Double): ColorTemperature {
    val clamped = position.coerceIn(0.0, 1.0)
    val startKelvin = start.toKelvin().value.toInt()
    val endKelvin = end.toKelvin().value.toInt()

    return (startKelvin + ((endKelvin - startKelvin) * clamped)).kelvin
}

fun interpolate(start: Percentage, end: Percentage, position: Percentage) = interpolate(start, end, position.toDecimal().toDouble())
fun interpolate(start: Percentage, end: Percentage, position: Float) = interpolate(start, end, position.toDouble())
private fun interpolate(start: Percentage, end: Percentage, position: Double): Percentage {
    val clamped = position.coerceIn(0.0, 1.0)
    val startPercentage = start.toWholePercentage().value.toInt()
    val endPercentage = end.toWholePercentage().value.toInt()

    return (startPercentage + ((endPercentage - startPercentage) * clamped)).percent
}
