package usonia.rules.lights

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.flow.first
import kotlinx.datetime.*
import usonia.core.state.ConfigurationAccess
import usonia.core.state.getSite
import usonia.foundation.Room
import usonia.foundation.unit.ColorTemperature
import usonia.kotlin.datetime.*
import usonia.kotlin.unit.Percentage
import usonia.kotlin.unit.percent
import usonia.weather.WeatherAccess
import kotlin.time.ExperimentalTime
import kotlin.time.hours
import kotlin.time.minutes

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
            ?.let { ColorTemperature(it.toInt()) }
            ?: DEFAULT_DAYLIGHT
        val nightColor = site.parameters[NIGHTLIGHT]
            ?.let { ColorTemperature(it.toInt()) }
            ?: DEFAULT_NIGHTLIGHT
        val eveningColor = site.parameters[EVENING]
            ?.let { ColorTemperature(it.toInt()) }
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

        when {
            now.instant >= forecast.sunrise.minus(period) && now.instant <= forecast.sunrise -> {
                logger.trace("In morning blue hour")
                val position = ((now - forecast.sunrise.minus(period)).inMinutes / period.inMinutes).toFloat()
                return LightSettings.Temperature(
                    temperature = (nightColor..daylightColor).transition(position),
                    brightness = (nightBrightness..100.percent).transition(position),
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

                val eveningTransitionPosition = ((now - forecast.sunset).inMinutes / period.inMinutes).toFloat()
                val eveningTransitionColor = (daylightColor..eveningColor).transition(eveningTransitionPosition)
                val position = ((now - nightStartInstant).inMinutes / period.inMinutes).toFloat()
                return LightSettings.Temperature(
                    temperature = (eveningTransitionColor..nightColor).transition(position),
                    brightness = (100.percent..nightBrightness).transition(position),
                )
            }
            now.instant < forecast.sunrise
                || now > nightStartInstant.plus(period)
                || now.localDate.dayOfYear > forecast.sunset.toLocalDateTime(clock.timeZone).dayOfYear
            -> {
                logger.trace("In nighttime")
                return LightSettings.Temperature(
                    temperature = nightColor,
                    brightness = nightBrightness,
                )
            }
            now.instant >= forecast.sunset -> {
                logger.trace("In evening")
                val position = ((now - forecast.sunset).inMinutes / period.inMinutes).toFloat()
                return LightSettings.Temperature(
                    temperature = (daylightColor..eveningColor).transition(position),
                    brightness = 100.percent,
                )
            }
            else -> throw IllegalStateException("Time not covered in color conditions.")
        }
    }

    private fun IntRange.transition(position: Float) = (start + ((endInclusive - start) * kotlin.math.max(0f, kotlin.math.min(1f, position)))).toInt()
    private fun ClosedRange<ColorTemperature>.transition(position: Float) = (start.kelvinValue..endInclusive.kelvinValue).transition(position).let(::ColorTemperature)
    private fun ClosedRange<Percentage>.transition(position: Float) = (start.percent..endInclusive.percent).transition(position).percent
}
