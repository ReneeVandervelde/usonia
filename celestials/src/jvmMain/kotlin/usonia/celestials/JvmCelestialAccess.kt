package usonia.celestials

import com.inkapplications.coroutines.ongoing.*
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator
import com.luckycatlabs.sunrisesunset.dto.Location
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toJavaZoneId
import kotlinx.datetime.TimeZone as KotlinTimeZone
import kotlinx.datetime.toKotlinInstant
import usonia.kotlin.datetime.ZonedClock
import usonia.kotlin.datetime.ZonedDateTime
import usonia.kotlin.datetime.current
import usonia.kotlin.datetime.withZone
import usonia.server.client.BackendClient
import java.util.*

/**
 * Adapts the JVM sunrise/sunset calculator to provide celestial data.
 */
internal class JvmCelestialAccess(
    usonia: BackendClient,
    private val clock: ZonedClock,
): CelestialAccess {
    /**
     * Latest location for the configured site.
     */
    private val location = usonia.site
        .map { it.location }
        .map { Location(it.latitude.asDecimal, it.longitude.asDecimal) }

    override val localCelestials: OngoingFlow<UpcomingCelestials> = location
        .map { location ->
            SunriseSunsetCalculator(location, clock.timeZone.toJavaZoneId().id)
        }
        .flatMapLatest { calculator ->
            updatingCelestialsFlow(calculator)
        }

    /**
     * Get a JVM calendar instance from a ZonedClock for the current time.
     */
    private val ZonedClock.currentCalendar: Calendar
        get() {
            return Calendar.getInstance(TimeZone.getTimeZone(timeZone.toJavaZoneId()))
                .apply {
                    time = Date.from(now().toJavaInstant())
                }
        }

    /**
     * Create a flow of celestial data that updates when the data expires.
     */
    private fun updatingCelestialsFlow(calculator: SunriseSunsetCalculator): Flow<UpcomingCelestials>
    {
        return flow {
            while (currentCoroutineContext().isActive) {
                val celestials = currentCelestials(calculator)
                emit(celestials)
                delay(celestials.firstEvent - clock.now())
            }
        }
    }

    /**
     * Create a schedule of two days of celestial events starting with today.
     */
    private fun currentCelestials(calculator: SunriseSunsetCalculator): UpcomingCelestials
    {
        val currentCalendar = clock.currentCalendar
        val tomorrowCalendar = clock.currentCalendar.apply {
            add(Calendar.DATE, 1)
        }
        val now = clock.current

        return UpcomingCelestials(
            timestamp = now,
            today = celestialsForDay(calculator, currentCalendar),
            tomorrow = celestialsForDay(calculator, tomorrowCalendar),
        )
    }

    /**
     * Create celestial data for the specified calendar date.
     */
    private fun celestialsForDay(calculator: SunriseSunsetCalculator, calendar: Calendar): Celestials
    {
        val sunrise = calculator.getOfficialSunriseCalendarForDate(calendar)
        val sunset = calculator.getOfficialSunsetCalendarForDate(calendar)
        val civilTwilightStart = calculator.getCivilSunriseCalendarForDate(calendar)
        val civilTwilightEnd = calculator.getCivilSunsetCalendarForDate(calendar)

        return Celestials(
            daylight = sunrise.toZonedDateTime(
                clock.timeZone
            )..sunset.toZonedDateTime(
                clock.timeZone
            ),
            civilTwilight = civilTwilightStart.toZonedDateTime(
                clock.timeZone
            )..civilTwilightEnd.toZonedDateTime(
                clock.timeZone
            ),
        )
    }

    /**
     * Convert a JVM calendar to a kotlinx ZonedDateTime.
     */
    private fun Calendar.toZonedDateTime(zone: KotlinTimeZone): ZonedDateTime
    {
        return toInstant().toKotlinInstant().withZone(zone)
    }
}