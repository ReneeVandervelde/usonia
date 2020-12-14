package usonia.weather

import kotlinx.datetime.Instant

/**
 * Provides forecasted weather conditions.
 */
data class Forecast(
    val timestamp: Instant,
    val sunrise: Instant,
    val sunset: Instant,
)
