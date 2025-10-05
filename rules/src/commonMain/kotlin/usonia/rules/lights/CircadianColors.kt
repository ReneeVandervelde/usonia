package usonia.rules.lights

import com.inkapplications.coroutines.ongoing.first
import com.inkapplications.datetime.ZonedClock
import com.inkapplications.datetime.atZone
import inkapplications.spondee.measure.metric.kelvin
import inkapplications.spondee.scalar.percent
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.datetime.LocalDateTime
import usonia.celestials.CelestialAccess
import usonia.core.state.ConfigurationAccess
import usonia.core.state.getSite
import usonia.foundation.Room
import usonia.foundation.unit.interpolate
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime

private const val DAYLIGHT = "circadian.color.daylight"
private const val EVENING = "circadian.color.evening"
private const val NIGHTLIGHT = "circadian.color.nightlight"
private const val NIGHT_BRIGHTNESS = "circadian.color.night.brightness"
private const val NIGHT_START = "circadian.color.night.start"
private const val TRANSITION_PERIOD = "circadian.color.period"
internal val DEFAULT_NIGHTLIGHT = Colors.Warm
internal val DEFAULT_EVENING = Colors.Medium
internal val DEFAULT_DAYLIGHT = Colors.Daylight
internal val DEFAULT_NIGHT_BRIGHTNESS = 50.percent
internal val DEFAULT_NIGHT_START = 20 * 60
@ExperimentalTime
internal val DEFAULT_PERIOD = 2.hours

/**
 * Determine colors based on sunrise/sunset.
 */
@OptIn(ExperimentalTime::class)
internal class CircadianColors(
    private val configurationAccess: ConfigurationAccess,
    private val celestialAccess: CelestialAccess,
    private val clock: ZonedClock = ZonedClock.System,
    private val logger: KimchiLogger = EmptyLogger,
): LightSettingsPicker {
    override suspend fun getActiveSettings(room: Room): LightSettings {
        val site = configurationAccess.getSite()
        val daylightColor = site.parameters[DAYLIGHT]
            ?.toInt()
            ?.kelvin
            ?: DEFAULT_DAYLIGHT
        val nightColor = site.parameters[NIGHTLIGHT]
            ?.toInt()
            ?.kelvin
            ?: DEFAULT_NIGHTLIGHT
        val eveningColor = site.parameters[EVENING]
            ?.toInt()
            ?.kelvin
            ?: DEFAULT_EVENING
        val nightStartMinute = site.parameters[NIGHT_START]
            ?.toInt()
            ?: DEFAULT_NIGHT_START
        val nightBrightness = site.parameters[NIGHT_BRIGHTNESS]
            ?.toInt()
            ?.percent
            ?: DEFAULT_NIGHT_BRIGHTNESS
        val period = site.parameters[TRANSITION_PERIOD]
            ?.toInt()
            ?.minutes
            ?: DEFAULT_PERIOD
        val now = clock.zonedDateTime()
        val startOfDay = LocalDateTime(
            year = now.localDate.year,
            month = now.localDate.month,
            dayOfMonth = now.localDate.dayOfMonth,
            hour = 0,
            minute = 0,
            second = 0,
            nanosecond = 0,
        ).atZone(clock.zone)
        val nightStartInstant = startOfDay + nightStartMinute.minutes
        val nightExemptRooms = setOf(Room.Type.Office, Room.Type.Storage, Room.Type.Utility)
        val modifiedNightColor = if (room.type in nightExemptRooms) eveningColor else nightColor
        val modifiedNightBrightness = if (room.type in nightExemptRooms) 100.percent else nightBrightness
        val celestials = celestialAccess.localCelestials.first()
        val sunriseToday = celestials.today.daylight.start
        val sunsetToday = celestials.today.daylight.endInclusive

        when {
            now >= sunriseToday.minus(period) && now <= sunriseToday -> {
                logger.trace("In morning blue hour")
                val position = ((now.instant - sunriseToday.instant.minus(period)).toDouble(DurationUnit.MINUTES) / period.toDouble(DurationUnit.MINUTES)).toFloat()
                return LightSettings.Temperature(
                    temperature = interpolate(modifiedNightColor, daylightColor, position),
                    brightness = interpolate(modifiedNightBrightness, 100.percent, position),
                )
            }
            now > sunriseToday && now < sunsetToday -> {
                logger.trace("In daytime")
                return LightSettings.Temperature(
                    temperature = daylightColor,
                    brightness = 100.percent,
                )
            }
            now >= nightStartInstant -> {
                logger.trace("In twilight")

                val eveningTransitionPosition = ((now.instant - sunsetToday.instant).toDouble(DurationUnit.MINUTES) / period.toDouble(DurationUnit.MINUTES)).toFloat()
                val eveningTransitionColor = interpolate(daylightColor, eveningColor, eveningTransitionPosition)
                val position = ((now.instant - nightStartInstant.instant).toDouble(DurationUnit.MINUTES) / period.toDouble(DurationUnit.MINUTES)).toFloat()
                return LightSettings.Temperature(
                    temperature = interpolate(eveningTransitionColor, modifiedNightColor, position),
                    brightness = interpolate(100.percent, modifiedNightBrightness, position),
                )
            }
            now < sunriseToday
                || now > nightStartInstant.plus(period)
                || now.localDate.dayOfYear > sunsetToday.localDateTime.dayOfYear
            -> {
                logger.trace("In nighttime")
                return LightSettings.Temperature(
                    temperature = modifiedNightColor,
                    brightness = modifiedNightBrightness,
                )
            }
            now >= sunsetToday -> {
                logger.trace("In evening")
                val position = ((now.instant - sunsetToday.instant).toDouble(DurationUnit.MINUTES) / period.toDouble(DurationUnit.MINUTES)).toFloat()
                return LightSettings.Temperature(
                    temperature = interpolate(daylightColor, eveningColor, position),
                    brightness = 100.percent,
                )
            }
            else -> throw IllegalStateException("Time not covered in color conditions.")
        }
    }
}
