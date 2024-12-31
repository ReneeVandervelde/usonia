package usonia.celestials

import kotlinx.datetime.atTime
import usonia.celestials.doubles.KnownCelestials
import usonia.kotlin.datetime.withZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes

class UpcomingCelestialsTest
{
    @Test
    fun testEarliestSchedule()
    {
        val now = KnownCelestials.FIRST_DATE.atTime(1, 0).withZone(KnownCelestials.ZONE)

        val upcoming = UpcomingCelestials(
            timestamp = now,
            today = KnownCelestials.FIRST,
            tomorrow = KnownCelestials.SECOND,
        )

        assertEquals(
            KnownCelestials.FIRST.civilTwilight.start,
            upcoming.firstEvent,
            "First Event is today's Twilight Start"
        )
        assertEquals(
            KnownCelestials.FIRST.civilTwilight.start,
            upcoming.nextTwilightStart,
            "Next Twilight Start is today's Twilight Start"
        )
        assertEquals(
            KnownCelestials.FIRST.daylight.start,
            upcoming.nextSunrise,
            "Next Sunrise is today's Sunrise"
        )
        assertEquals(
            KnownCelestials.FIRST.daylight.endInclusive,
            upcoming.nextSunset,
            "Next Sunset is today's Sunset"
        )
        assertEquals(
            KnownCelestials.FIRST.civilTwilight.endInclusive,
            upcoming.nextTwilightEnd,
            "Next Twilight End is today's Twilight End"
        )
    }

    @Test
    fun testAfterTwilightStart()
    {
        val now = KnownCelestials.FIRST_DATE
            .atTime(KnownCelestials.FIRST.civilTwilight.start.plus(1.minutes).localDateTime.time)
            .withZone(KnownCelestials.ZONE)

        val upcoming = UpcomingCelestials(
            timestamp = now,
            today = KnownCelestials.FIRST,
            tomorrow = KnownCelestials.SECOND,
        )

        assertEquals(
            KnownCelestials.FIRST.daylight.start,
            upcoming.firstEvent,
            "First Event is today's Sunrise"
        )
        assertEquals(
            KnownCelestials.SECOND.civilTwilight.start,
            upcoming.nextTwilightStart,
            "Next Twilight Start is tomorrow's Twilight Start"
        )
        assertEquals(
            KnownCelestials.FIRST.daylight.start,
            upcoming.nextSunrise,
            "Next Sunrise is today's Sunrise"
        )
        assertEquals(
            KnownCelestials.FIRST.daylight.endInclusive,
            upcoming.nextSunset,
            "Next Sunset is today's Sunset"
        )
        assertEquals(
            KnownCelestials.FIRST.civilTwilight.endInclusive,
            upcoming.nextTwilightEnd,
            "Next Twilight End is today's Twilight End"
        )
    }

    @Test
    fun testAfterSunrise()
    {
        val now = KnownCelestials.FIRST_DATE
            .atTime(KnownCelestials.FIRST.daylight.start.plus(1.minutes).localDateTime.time)
            .withZone(KnownCelestials.ZONE)

        val upcoming = UpcomingCelestials(
            timestamp = now,
            today = KnownCelestials.FIRST,
            tomorrow = KnownCelestials.SECOND,
        )

        assertEquals(
            KnownCelestials.FIRST.daylight.endInclusive,
            upcoming.firstEvent,
            "First Event is today's Sunset"
        )
        assertEquals(
            KnownCelestials.SECOND.civilTwilight.start,
            upcoming.nextTwilightStart,
            "Next Twilight Start is tomorrow's Twilight Start"
        )
        assertEquals(
            KnownCelestials.SECOND.daylight.start,
            upcoming.nextSunrise,
            "Next Sunrise is tomorrow's Sunrise"
        )
        assertEquals(
            KnownCelestials.FIRST.daylight.endInclusive,
            upcoming.nextSunset,
            "Next Sunset is today's Sunset"
        )
        assertEquals(
            KnownCelestials.FIRST.civilTwilight.endInclusive,
            upcoming.nextTwilightEnd,
            "Next Twilight End is today's Twilight End"
        )
    }

    @Test
    fun testAfterSunset()
    {
        val now = KnownCelestials.FIRST_DATE
            .atTime(KnownCelestials.FIRST.daylight.endInclusive.plus(1.minutes).localDateTime.time)
            .withZone(KnownCelestials.ZONE)

        val upcoming = UpcomingCelestials(
            timestamp = now,
            today = KnownCelestials.FIRST,
            tomorrow = KnownCelestials.SECOND,
        )

        assertEquals(
            KnownCelestials.FIRST.civilTwilight.endInclusive,
            upcoming.firstEvent,
            "First Event is today's Twilight End"
        )
        assertEquals(
            KnownCelestials.SECOND.civilTwilight.start,
            upcoming.nextTwilightStart,
            "Next Twilight Start is tomorrow's Twilight Start"
        )
        assertEquals(
            KnownCelestials.SECOND.daylight.start,
            upcoming.nextSunrise,
            "Next Sunrise is tomorrow's Sunrise"
        )
        assertEquals(
            KnownCelestials.SECOND.daylight.endInclusive,
            upcoming.nextSunset,
            "Next Sunset is tomorrow's Sunset"
        )
        assertEquals(
            KnownCelestials.FIRST.civilTwilight.endInclusive,
            upcoming.nextTwilightEnd,
            "Next Twilight End is today's Twilight End"
        )
    }

    @Test
    fun testAfterTwilightEnd()
    {
        val now = KnownCelestials.FIRST_DATE
            .atTime(KnownCelestials.FIRST.civilTwilight.endInclusive.plus(1.minutes).localDateTime.time)
            .withZone(KnownCelestials.ZONE)

        val upcoming = UpcomingCelestials(
            timestamp = now,
            today = KnownCelestials.FIRST,
            tomorrow = KnownCelestials.SECOND,
        )

        assertEquals(
            KnownCelestials.SECOND.civilTwilight.start,
            upcoming.firstEvent,
            "First Event is tomorrow's Twilight Start"
        )
        assertEquals(
            KnownCelestials.SECOND.civilTwilight.start,
            upcoming.nextTwilightStart,
            "Next Twilight Start is tomorrow's Twilight Start"
        )
        assertEquals(
            KnownCelestials.SECOND.daylight.start,
            upcoming.nextSunrise,
            "Next Sunrise is tomorrow's Sunrise"
        )
        assertEquals(
            KnownCelestials.SECOND.daylight.endInclusive,
            upcoming.nextSunset,
            "Next Sunset is tomorrow's Sunset"
        )
        assertEquals(
            KnownCelestials.SECOND.civilTwilight.endInclusive,
            upcoming.nextTwilightEnd,
            "Next Twilight End is tomorrow's Twilight End"
        )
    }
}

