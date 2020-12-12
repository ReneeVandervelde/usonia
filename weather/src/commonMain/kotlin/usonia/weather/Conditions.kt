package usonia.weather

import kotlinx.datetime.Instant
import usonia.kotlin.unit.Percentage

/**
 * Info about current weather conditions.
 */
data class Conditions(
    val timestamp: Instant,
    val cloudCover: Percentage,
    val temperature: Int
)
