package usonia.rules.indicator

import com.github.ajalt.colormath.RGB
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import usonia.core.Daemon
import usonia.core.state.*
import usonia.foundation.*
import usonia.kotlin.neverEnding
import usonia.kotlin.unit.percent
import usonia.weather.Conditions
import usonia.weather.Forecast
import usonia.weather.WeatherAccess
import usonia.weather.combinedData
import kotlin.math.max
import kotlin.math.min

/**
 * Updates a light based on weather data.
 */
internal class Indicator(
    private val weatherAccess: WeatherAccess,
    private val configurationAccess: ConfigurationAccess,
    private val eventAccess: EventAccess,
    private val actionPublisher: ActionPublisher,
    private val logger: KimchiLogger = EmptyLogger,
): Daemon {
    private val hotIndicator = RGB(255, 0 , 0)
    private val coldIndicator = RGB(0, 0 , 255)
    private val snowColor = RGB(255, 255 , 255)
    private val rainColor = RGB(0, 255 , 255)

    override suspend fun start(): Nothing = neverEnding {
        configurationAccess.site.collectLatest { site ->
            coroutineScope {
                launch { bindColorUpdates(site) }
                launch { bindAwayBrightness(site) }
            }
        }
    }

    private suspend fun bindColorUpdates(site: Site) {
        weatherAccess.combinedData
            .onEach { logger.debug("Updating indicator with new data: <$it>") }
            .map { (conditions, forecast) ->
                site.findDevicesBy { it.fixture == Fixture.Indicator }.map { device ->
                    Action.ColorChange(
                        target = device.id,
                        color = getColor(forecast, conditions),
                    )
                }
            }
            .onEach { logger.debug("Mapped Indicator update to ${it.size} device actions") }
            .collect { actions ->
                actions.forEach {
                    actionPublisher.publishAction(it)
                }
            }
    }

    private suspend fun bindAwayBrightness(site: Site) {
        eventAccess.events
            .filterIsInstance<Event.Presence>()
            .mapLatest {
                if (eventAccess.allAway(site.users)) 5.percent else 100.percent
            }
            .collectLatest { level ->
                site.findDevicesBy { it.fixture == Fixture.Indicator }
                    .map { Action.Dim(it.id, level) }
                    .forEach { actionPublisher.publishAction(it) }
            }
    }

    private fun getColor(forecast: Forecast, conditions: Conditions): RGB {
        return when {
            forecast.snowChance > 20.percent -> snowColor
            forecast.rainChance > 20.percent -> rainColor
            else -> colorTransition(coldIndicator, hotIndicator, conditions.temperature / 100f)
        }
    }

    /**
     * Linearly transition from one color to another.
     *
     * @param start The color at the start of the transition range
     * @param end The color at the end of the transition range
     * @param position The current transition position from 0.0 to 1.0
     * @return A color between the start and end colors, relative to the position.
     */
    private fun colorTransition(start: RGB, end: RGB, position: Float): RGB {
        val constrainedPosition = max(0f, min(1f, position))

        return RGB(
            r = (start.r + (constrainedPosition * (end.r - start.r))).toInt(),
            g = (start.g + (constrainedPosition * (end.g - start.g))).toInt(),
            b = (start.b + (constrainedPosition * (end.b - start.b))).toInt()
        )
    }
}