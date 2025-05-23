package usonia.celestials

import com.inkapplications.datetime.ZonedDateTime
import com.inkapplications.datetime.atZone
import com.inkapplications.datetime.toZonedDateTime
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
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

val FakeCelestials = Celestials(
    daylight = Instant.DISTANT_PAST.toZonedDateTime(TimeZone.UTC)..Instant.DISTANT_FUTURE.toZonedDateTime(TimeZone.UTC),
    civilTwilight = Instant.DISTANT_PAST.toZonedDateTime(TimeZone.UTC)..Instant.DISTANT_FUTURE.toZonedDateTime(TimeZone.UTC),
)
