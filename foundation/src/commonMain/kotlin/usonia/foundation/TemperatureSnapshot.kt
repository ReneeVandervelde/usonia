package usonia.foundation

import inkapplications.spondee.measure.Temperature
import kotlinx.serialization.Serializable
import kotlin.time.Duration

/**
 * A snapshot of temperature data for a given time.
 *
 * @param timeAgo The length of time since now that this temperature data
 *        is relevant. The granularity of this duration is determined by the
 *        query, but is typically grouped by hours.
 * @param temperature The temperature that was recorded at this given time.
 */
@Serializable
class TemperatureSnapshot(
    @Serializable(with = DurationMillisecondSerializer::class)
    val timeAgo: Duration,
    @Serializable(with = FahrenheitSerializer::class)
    val temperature: Temperature,
)
