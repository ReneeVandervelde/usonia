package usonia.weather

import kotlinx.datetime.Instant
import usonia.kotlin.unit.Percentage

/**
 * Provides forecasted weather conditions.
 */
data class Forecast(
    val timestamp: Instant,
    val sunrise: Instant,
    val sunset: Instant,
    val rainChance: Percentage,
    val snowChance: Percentage,
)
