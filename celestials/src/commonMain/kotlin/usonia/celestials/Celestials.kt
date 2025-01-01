package usonia.celestials

import com.inkapplications.datetime.ZonedDateTime
import kotlin.time.Duration

/**
 * Snapshot of solar events for a single day.
 */
data class Celestials(
    val daylight: ClosedRange<ZonedDateTime>,
    val civilTwilight: ClosedRange<ZonedDateTime>,
) {
    val solarNoon: ZonedDateTime = daylight.start + ((daylight.endInclusive.instant - daylight.start.instant) / 2)
    val dayLength: Duration = daylight.endInclusive.instant - daylight.start.instant
}
