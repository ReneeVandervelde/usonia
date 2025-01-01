package usonia.celestials

import com.inkapplications.datetime.ZonedDateTime

/**
 * A schedule of celestial events relevant to a given time.
 *
 * This is used to calculate the next/upcoming events, such as the next
 * sunrise, in addition to the current day's events.
 */
data class UpcomingCelestials(
    /**
     * The timestamp that this schedule is based on.
     *
     * This is used to calculate the next relevant event for certain ranges.
     */
    val timestamp: ZonedDateTime,

    /**
     * Today's celestial evens
     */
    val today: Celestials,

    /**
     * Tomorrow's celestial events
     */
    val tomorrow: Celestials,
) {
    /**
     * The start next morning's civil twilight.
     */
    val nextTwilightStart: ZonedDateTime = today.civilTwilight.start.unlessPassed(tomorrow.civilTwilight.start)

    /**
     * The next sunrise event to occur.
     */
    val nextSunrise: ZonedDateTime = today.daylight.start.unlessPassed(tomorrow.daylight.start)

    /**
     * The next sunset event to occur.
     */
    val nextSunset: ZonedDateTime = today.daylight.endInclusive.unlessPassed(tomorrow.daylight.endInclusive)

    /**
     * The end of the next evening's civil twilight.
     */
    val nextTwilightEnd: ZonedDateTime = today.civilTwilight.endInclusive.unlessPassed(tomorrow.civilTwilight.endInclusive)

    /**
     * The first event to occur in the schedule.
     */
    val firstEvent: ZonedDateTime = listOf(
        nextSunrise,
        nextTwilightStart,
        nextTwilightEnd,
        nextSunset,
    ).min()

    /**
     * Use this date if not passed, otherwise use the provided date.
     */
    private fun ZonedDateTime.unlessPassed(otherwise: ZonedDateTime): ZonedDateTime {
        return if (this < timestamp) otherwise else this
    }
}
