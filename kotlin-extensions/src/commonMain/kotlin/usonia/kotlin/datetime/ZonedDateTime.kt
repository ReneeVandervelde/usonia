package usonia.kotlin.datetime

import kotlinx.datetime.*
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

/**
 * An instant of time paired with a timezone.
 *
 * This makes the best of both worlds between Instant and LocalDateTime.
 * Someday this class will probably be added to KotlinX.DateTime, but for now
 * it's here.
 */
@OptIn(ExperimentalTime::class)
data class ZonedDateTime(
    val instant: Instant,
    val zone: TimeZone
): Comparable<ZonedDateTime> {
    val localDateTime by lazy { instant.toLocalDateTime(zone) }
    val localDate by lazy { localDateTime.date }

    val epochSeconds: Long get() = instant.epochSeconds
    val nanosecondsOfSecond: Int get() = instant.nanosecondsOfSecond
    val year: Int get() = localDateTime.year
    val monthNumber: Int get() = localDateTime.monthNumber
    val month: Month get() = localDateTime.month
    val dayOfMonth: Int get() = localDateTime.dayOfMonth
    val dayOfWeek: DayOfWeek get() = localDateTime.dayOfWeek
    val dayOfYear: Int get() = localDateTime.dayOfYear

    val hour: Int get() = localDateTime.hour
    val minute: Int get() = localDateTime.minute
    val second: Int get() = localDateTime.second
    val nanosecond: Int get() = localDateTime.nanosecond

    override fun compareTo(other: ZonedDateTime): Int = instant.compareTo(other.instant)
    operator fun plus(time: Duration) = ZonedDateTime(instant + time, zone)
    operator fun minus(time: Duration) = ZonedDateTime(instant - time, zone)
    operator fun minus(other: ZonedDateTime) = instant - other.instant
    operator fun minus(other: Instant) = instant - other
    override fun toString(): String = "$localDateTime ${zone.id}"
}

/**
 * Create a Zoned DateTime from a local time and its zone.
 */
fun LocalDateTime.withZone(zone: TimeZone) = ZonedDateTime(toInstant(zone), zone)

/**
 * Create a Zoned DateTime from an Instant and its zone.
 */
fun Instant.withZone(zone: TimeZone) = ZonedDateTime(this, zone)
