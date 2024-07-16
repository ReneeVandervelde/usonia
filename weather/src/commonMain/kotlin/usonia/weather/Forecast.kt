package usonia.weather

import inkapplications.spondee.measure.Temperature
import inkapplications.spondee.scalar.Percentage
import kotlinx.datetime.Instant

/**
 * Provides forecasted weather conditions.
 */
data class Forecast(
    val timestamp: Instant,
    val sunrise: Instant,
    val sunset: Instant,
    val rainChance: Percentage,
    val snowChance: Percentage,
    val highTemperature: Temperature,
    val lowTemperature: Temperature,
)
