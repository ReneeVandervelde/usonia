package usonia.rules.lights

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import usonia.core.state.ConfigurationAccess
import usonia.core.state.getSite
import usonia.foundation.Room
import usonia.foundation.unit.ColorTemperature
import usonia.kotlin.unit.Percentage
import usonia.kotlin.unit.percent
import usonia.weather.WeatherAccess
import kotlin.time.ExperimentalTime
import kotlin.time.hours
import kotlin.time.minutes

private const val DAYLIGHT = "circadian.color.daylight"
private const val NIGHTLIGHT = "circadian.color.nightlight"
private const val NIGHT_BRIGHTNESS = "circadian.color.night.brightness"
private const val TRANSITION_PERIOD = "circadian.color.period"
internal val DEFAULT_NIGHTLIGHT = ColorTemperature(2800)
internal val DEFAULT_DAYLIGHT = ColorTemperature(6200)
internal val DEFAULT_NIGHT_BRIGHTNESS = 50.percent
@ExperimentalTime
internal val DEFAULT_PERIOD = 2.hours

/**
 * Determine colors based on sunrise/sunset.
 */
@OptIn(ExperimentalTime::class)
internal class CircadianColors(
    private val configurationAccess: ConfigurationAccess,
    private val weather: WeatherAccess,
    private val clock: Clock = Clock.System,
    private val logger: KimchiLogger = EmptyLogger,
): ColorPicker {
    override suspend fun getRoomColor(room: Room): LightSettings {
        val forecast = weather.forecast.first()
        val site = configurationAccess.getSite()
        val daylightColor = site.parameters[DAYLIGHT]
            ?.let { ColorTemperature(it.toInt()) }
            ?: DEFAULT_DAYLIGHT
        val nightColor = site.parameters[NIGHTLIGHT]
            ?.let { ColorTemperature(it.toInt()) }
            ?: DEFAULT_NIGHTLIGHT
        val nightBrightness = site.parameters[NIGHT_BRIGHTNESS]
            ?.toInt()
            ?.percent
            ?: DEFAULT_NIGHT_BRIGHTNESS
        val period = site.parameters[TRANSITION_PERIOD]
            ?.toInt()
            ?.minutes
            ?: DEFAULT_PERIOD
        val now = clock.now()

        when {
            now >= forecast.sunrise.minus(period) && now <= forecast.sunrise -> {
                logger.trace("In morning blue hour")
                val position = ((now - forecast.sunrise.minus(period)).inMinutes / period.inMinutes).toFloat()
                return LightSettings(
                    temperature = (nightColor..daylightColor).transition(position),
                    brightness = (nightBrightness..100.percent).transition(position),
                )
            }
            now > forecast.sunrise && now < forecast.sunset -> {
                logger.trace("In daytime")
                return LightSettings(
                    temperature = daylightColor,
                    brightness = 100.percent,
                )
            }
            now >= forecast.sunset -> {
                logger.trace("In evening blue hour")
                val position = ((now - forecast.sunset).inMinutes / period.inMinutes).toFloat()
                return LightSettings(
                    temperature = (daylightColor..nightColor).transition(position),
                    brightness = (100.percent..nightBrightness).transition(position),
                )
            }
            now < forecast.sunrise || now > forecast.sunset.plus(period) -> {
                logger.trace("In nighttime")
                return LightSettings(
                    temperature = nightColor,
                    brightness = nightBrightness,
                )
            }
            else -> throw IllegalStateException("Time not covered in color conditions.")
        }
    }

    private fun IntRange.transition(position: Float) = (start + ((endInclusive - start) * kotlin.math.max(0f, kotlin.math.min(1f, position)))).toInt()
    private fun ClosedRange<ColorTemperature>.transition(position: Float) = (start.kelvinValue..endInclusive.kelvinValue).transition(position).let(::ColorTemperature)
    private fun ClosedRange<Percentage>.transition(position: Float) = (start.percent..endInclusive.percent).transition(position).percent
}
