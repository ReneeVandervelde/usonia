package usonia.weather

import inkapplications.spondee.measure.Temperature
import inkapplications.spondee.scalar.Percentage
import kotlinx.datetime.Instant

data class Forecast(
    val timestamp: Instant,
    val precipitation: Percentage,
    val temperature: Temperature,
)
