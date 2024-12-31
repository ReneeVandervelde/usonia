package usonia.celestials.doubles

import inkapplications.spondee.spatial.GeoCoordinates
import inkapplications.spondee.spatial.latitude
import inkapplications.spondee.spatial.longitude
import kotlinx.datetime.*
import usonia.celestials.Celestials
import usonia.kotlin.datetime.withZone

/**
 * Fixed test data. A known sunrise/sunset schedule for a day in NYC.
 */
object KnownCelestials
{
    val LOCATION = GeoCoordinates(40.730610.latitude, (-73.935242).longitude)
    val FIRST_DATE = LocalDate(2024, 3, 17)
    val SECOND_DATE = LocalDate(2024, 3, 18)
    val ZONE = TimeZone.of("America/New_York")
    val FIRST = Celestials(
        daylight = FIRST_DATE.atTime(LocalTime(7, 3, 0)).withZone(ZONE)..FIRST_DATE.atTime(19, 5, 0).withZone(ZONE),
        civilTwilight = FIRST_DATE.atTime(6, 36, 0).withZone(ZONE)..FIRST_DATE.atTime(19, 33, 0).withZone(ZONE),
    )
    val SECOND = Celestials(
        daylight = SECOND_DATE.atTime(7, 1, 0).withZone(ZONE)..SECOND_DATE.atTime(19, 7, 0).withZone(ZONE),
        civilTwilight = SECOND_DATE.atTime(6, 34, 0).withZone(ZONE)..SECOND_DATE.atTime(19, 34, 0).withZone(ZONE),
    )
}
