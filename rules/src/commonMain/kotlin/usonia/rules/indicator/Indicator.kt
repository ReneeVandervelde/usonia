package usonia.rules.indicator

import com.github.ajalt.colormath.model.RGB
import com.inkapplications.coroutines.ongoing.collectLatest
import com.inkapplications.coroutines.ongoing.map
import com.inkapplications.coroutines.ongoing.mapLatest
import com.inkapplications.coroutines.ongoing.onEach
import inkapplications.spondee.measure.us.toFahrenheit
import inkapplications.spondee.scalar.percent
import inkapplications.spondee.structure.toFloat
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import regolith.processes.daemon.Daemon
import usonia.foundation.*
import usonia.foundation.unit.compareTo
import usonia.server.client.BackendClient
import usonia.weather.Conditions
import usonia.weather.Forecast
import usonia.weather.FullForecast
import usonia.weather.LocalWeatherAccess
import usonia.weather.combinedData
import kotlin.math.max
import kotlin.math.min

/**
 * Updates a light based on weather data.
 */
internal class Indicator(
    private val client: BackendClient,
    private val weatherAccess: LocalWeatherAccess,
    private val logger: KimchiLogger = EmptyLogger,
): Daemon {
    private val hotIndicator = RGB(255, 0 , 0)
    private val coldIndicator = RGB(0, 0 , 255)
    private val snowColor = RGB(255, 255 , 255)
    private val rainColor = RGB(0, 255 , 255)
    private val unknownColor = RGB(0, 255 , 0)

    override suspend fun startDaemon(): Nothing {
        client.site.collectLatest { site ->
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
            .collectLatest { actions ->
                actions.forEach {
                    client.publishAction(it)
                }
            }
    }

    private suspend fun bindAwayBrightness(site: Site) {
        client.securityState
            .mapLatest {
                if (it == SecurityState.Armed) 1.percent else 80.percent
            }
            .collectLatest { level ->
                site.findDevicesBy { it.fixture == Fixture.Indicator }
                    .map { Action.Dim(it.id, level) }
                    .forEach { client.publishAction(it) }
            }
    }

    private fun getColor(forecast: Forecast?, conditions: Conditions?): RGB {
        val currentTemperature = conditions?.temperature?.toFahrenheit()?.toFloat()
        return when {
            forecast == null || conditions == null || currentTemperature == null -> unknownColor
            forecast.snowChance > 20.percent -> snowColor
            forecast.rainChance > 20.percent -> rainColor
            else -> colorTransition(coldIndicator, hotIndicator, currentTemperature / 100f)
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
