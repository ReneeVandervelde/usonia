package usonia.weather

import inkapplications.spondee.measure.Length
import inkapplications.spondee.scalar.Percentage
import kotlinx.datetime.Instant

/**
 * Info about current weather conditions.
 */
data class Conditions(
    val timestamp: Instant,
    val cloudCover: Percentage,
    val temperature: Int,
    val rainInLast6Hours: Length,
    val isRaining: Boolean,
)
