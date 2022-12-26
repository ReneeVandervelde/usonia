package usonia.rules.lights

import inkapplications.spondee.measure.ColorTemperature
import inkapplications.spondee.measure.metric.kelvin
import inkapplications.spondee.scalar.Percentage
import inkapplications.spondee.scalar.percent
import inkapplications.spondee.scalar.toWholePercentage
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toLocalDateTime
import usonia.core.state.ConfigurationAccess
import usonia.core.state.getSite
import usonia.foundation.Room
import usonia.kotlin.datetime.ZonedClock
import usonia.kotlin.datetime.ZonedSystemClock
import usonia.kotlin.datetime.current
import usonia.kotlin.datetime.withZone
import usonia.kotlin.first
import usonia.weather.WeatherAccess
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
    private val weather: WeatherAccess,
    private val clock: ZonedClock = ZonedSystemClock,
    private val logger: KimchiLogger = EmptyLogger,
): LightSettingsPicker {
    override suspend fun getActiveSettings(room: Room): LightSettings {
        val forecast = weather.forecast.first()
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
        val now = clock.current
        val startOfDay = LocalDateTime(
            year = now.localDate.year,
            month = now.localDate.month,
            dayOfMonth = now.localDate.dayOfMonth,
            hour = 0,
            minute = 0,
            second = 0,
            nanosecond = 0,
        ).withZone(clock.timeZone)
        val nightStartInstant = startOfDay + nightStartMinute.minutes
        val nightExemptRooms = setOf(Room.Type.Office, Room.Type.Storage, Room.Type.Utility)
        val modifiedNightColor = if (room.type in nightExemptRooms) eveningColor else nightColor
        val modifiedNightBrightness = if (room.type in nightExemptRooms) 100.percent else nightBrightness

        when {
            now.instant >= forecast.sunrise.minus(period) && now.instant <= forecast.sunrise -> {
                logger.trace("In morning blue hour")
                val position = ((now - forecast.sunrise.minus(period)).toDouble(DurationUnit.MINUTES) / period.toDouble(DurationUnit.MINUTES)).toFloat()
                return LightSettings.Temperature(
                    temperature = transition(modifiedNightColor, daylightColor, position),
                    brightness = transition(modifiedNightBrightness, 100.percent, position),
                )
            }
            now.instant > forecast.sunrise && now.instant < forecast.sunset -> {
                logger.trace("In daytime")
                return LightSettings.Temperature(
                    temperature = daylightColor,
                    brightness = 100.percent,
                )
            }
            now >= nightStartInstant -> {
                logger.trace("In twilight")

                val eveningTransitionPosition = ((now - forecast.sunset).toDouble(DurationUnit.MINUTES) / period.toDouble(DurationUnit.MINUTES)).toFloat()
                val eveningTransitionColor = transition(daylightColor, eveningColor, eveningTransitionPosition)
                val position = ((now - nightStartInstant).toDouble(DurationUnit.MINUTES) / period.toDouble(DurationUnit.MINUTES)).toFloat()
                return LightSettings.Temperature(
                    temperature = transition(eveningTransitionColor, modifiedNightColor, position),
                    brightness = transition(100.percent, modifiedNightBrightness, position),
                )
            }
            now.instant < forecast.sunrise
                || now > nightStartInstant.plus(period)
                || now.localDate.dayOfYear > forecast.sunset.toLocalDateTime(clock.timeZone).dayOfYear
            -> {
                logger.trace("In nighttime")
                return LightSettings.Temperature(
                    temperature = modifiedNightColor,
                    brightness = modifiedNightBrightness,
                )
            }
            now.instant >= forecast.sunset -> {
                logger.trace("In evening")
                val position = ((now - forecast.sunset).toDouble(DurationUnit.MINUTES) / period.toDouble(DurationUnit.MINUTES)).toFloat()
                return LightSettings.Temperature(
                    temperature = transition(daylightColor, eveningColor, position),
                    brightness = 100.percent,
                )
            }
            else -> throw IllegalStateException("Time not covered in color conditions.")
        }
    }

    private fun IntRange.transition(position: Float) = (start + ((endInclusive - start) * kotlin.math.max(0f, kotlin.math.min(1f, position)))).toInt()
    private fun transition(start: ColorTemperature, end: ColorTemperature, position: Float): ColorTemperature {
        val startKelvin = start.toKelvin().value.toInt()
        val endKelvin = end.toKelvin().value.toInt()

        return (startKelvin..endKelvin).transition(position).kelvin
    }
    private fun transition(start: Percentage, end: Percentage, position: Float): Percentage {
        val startPercentage = start.toWholePercentage().value.toInt()
        val endPercentage = end.toWholePercentage().value.toInt()

        return (startPercentage..endPercentage).transition(position).percent
    }
}
