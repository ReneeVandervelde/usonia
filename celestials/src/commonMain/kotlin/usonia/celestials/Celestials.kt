package usonia.celestials

import usonia.kotlin.datetime.ZonedDateTime
import kotlin.time.Duration

/**
 * Snapshot of solar events for a single day.
 */
data class Celestials(
    val daylight: ClosedRange<ZonedDateTime>,
    val civilTwilight: ClosedRange<ZonedDateTime>,
) {
    val solarNoon: ZonedDateTime = daylight.start + ((daylight.endInclusive - daylight.start) / 2)
    val dayLength: Duration = daylight.endInclusive - daylight.start
}
