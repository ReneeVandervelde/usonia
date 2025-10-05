package usonia.weather

import inkapplications.spondee.measure.Length
import inkapplications.spondee.measure.Temperature
import inkapplications.spondee.scalar.Percentage
import kotlinx.datetime.Instant

/**
 * Info about current weather conditions.
 */
data class Conditions(
    val timestamp: Instant,
    val cloudCover: Percentage?,
    val temperature: Temperature?,
    val rainInLast6Hours: Length?,
    val isRaining: Boolean?,
)
