package usonia.celestials

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.atTime
import usonia.celestials.doubles.KnownCelestials.ZONE
import usonia.kotlin.datetime.withZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class CelestialsTest
{
    @Test
    fun testCalculations()
    {
        val date = LocalDate(2024, 3, 17)
        val celestials = Celestials(
            daylight = date.atTime(LocalTime(7, 3, 0)).withZone(ZONE)..date.atTime(19, 5, 0).withZone(ZONE),
            civilTwilight = date.atTime(6, 36, 0).withZone(ZONE)..date.atTime(19, 33, 0).withZone(ZONE),
        )

        assertEquals(12.hours + 2.minutes, celestials.dayLength)
        assertEquals(date.atTime(13, 4, 0).withZone(ZONE), celestials.solarNoon)
    }
}
