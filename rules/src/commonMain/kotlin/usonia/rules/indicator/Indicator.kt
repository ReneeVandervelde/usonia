package usonia.rules.indicator

import com.github.ajalt.colormath.RGB
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.flow.*
import usonia.core.Daemon
import usonia.core.state.ActionPublisher
import usonia.core.state.ConfigurationAccess
import usonia.core.state.findDeviceBy
import usonia.foundation.Action
import usonia.foundation.Device
import usonia.foundation.Fixture
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
    private val actionPublisher: ActionPublisher,
    private val logger: KimchiLogger = EmptyLogger,
): Daemon {
    private val hotIndicator = RGB(255, 0 , 0)
    private val coldIndicator = RGB(0, 0 , 255)
    private val snowColor = RGB(255, 255 , 255)
    private val rainColor = RGB(0, 255 , 255)

    override suspend fun start(): Nothing = neverEnding {
        configurationAccess.site
            .map { it.findDeviceBy { it.fixture == Fixture.Indicator } }
            .onEach { logger.debug("Binding ${it.size} Indicators") }
            .collectLatest { indicators -> updateIndicators(indicators) }
    }

    private suspend fun updateIndicators(indicators: Set<Device>) {
        weatherAccess.combinedData
            .onEach { logger.debug("Updating indicator with new data: <$it>") }
            .map { (conditions, forecast) ->
                indicators.map { device ->
                    Action.ColorChange(
                        target = device.id,
                        color = getColor(forecast, conditions),
                        level = 100.percent
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
