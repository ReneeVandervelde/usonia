package usonia.kotlin.datetime

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone

/**
 * A Timezone-aware clock.
 */
interface ZonedClock: Clock {
    val timeZone: TimeZone
    override fun now(): Instant
}

/**
 * Combined clock+zone delegates.
 */
private class CompositeZonedClock(
    clock: Clock,
    override val timeZone: TimeZone,
): ZonedClock, Clock by clock

/**
 * The the local date/time at the clock's set timezone.
 */
val ZonedClock.current get() = ZonedDateTime(now(), timeZone)

/**
 * Create a ZonedClock from this at a specified timezone.
 */
fun Clock.withTimeZone(zone: TimeZone): ZonedClock = CompositeZonedClock(this, zone)

/**
 * Parse an ISO String as a local datetime in the current clock's zone.
 */
fun ZonedClock.parseLocalDateTime(isoString: String) = LocalDateTime.parse(isoString).withZone(timeZone)

/**
 * Zoned Clock with system defaults.
 */
val ZonedSystemClock: ZonedClock get() = Clock.System.withTimeZone(TimeZone.currentSystemDefault())

/**
 * Zoned Clock with system defaults.
 */
val UtcClock: ZonedClock get() = Clock.System.withTimeZone(TimeZone.UTC)
