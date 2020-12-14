package usonia.weather.accuweather

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import usonia.core.cron.CronJob
import usonia.core.cron.Schedule
import usonia.core.state.ConfigurationAccess
import usonia.kotlin.unit.percent
import usonia.weather.Conditions
import usonia.weather.Forecast
import usonia.weather.WeatherAccess
import kotlin.time.ExperimentalTime
import kotlin.time.hours

private const val SERVICE = "accuweather"
private const val LOCATION = "location"
private const val TOKEN = "token"

/**
 * Provide access to weather data with pre-cached information.
 *
 * Fetches weather conditions at startup and updates once every hour to
 * rate limit requests. All data provided is pre-cached.
 * Forecast is updated only after it has expired. Subsequently, sunrise
 * times may not reflect the next day until the previous forecast has expired.
 */
@OptIn(ExperimentalTime::class)
internal class AccuweatherAccess(
    private val api: AccuweatherApi,
    private val config: ConfigurationAccess,
    private val clock: Clock = Clock.System,
    private val logger: KimchiLogger = EmptyLogger,
): WeatherAccess, CronJob {
    private val forecastFlow = MutableSharedFlow<Forecast>(replay = 1)
    private val conditionsFlow = MutableSharedFlow<Conditions>(replay = 1)
    override val forecast: Flow<Forecast> = forecastFlow
    override val conditions: Flow<Conditions> = conditionsFlow

    override val schedule: Schedule = Schedule(
        minutes = setOf(0)
    )

    override suspend fun run(time: LocalDateTime) {
        val (location, token) = getConfig() ?: return

        coroutineScope {
            val newConditions = async { getFreshConditions(location, token) }
            val currentForecast = forecast.first()
            if (clock.now() - currentForecast.timestamp > 4.hours) {
                logger.info("Forecast is expired. Updating.")
                forecastFlow.emit(getFreshForecast(location, token))
            }

            conditionsFlow.emit(newConditions.await())
        }
    }

    override suspend fun start() {
        val (location, token) = getConfig() ?: return

        coroutineScope {
            val newConditions = async { getFreshConditions(location, token) }
            val newForecast = async { getFreshForecast(location, token) }

            conditionsFlow.emit(newConditions.await())
            forecastFlow.emit(newForecast.await())
        }
    }

    private suspend fun getConfig(): Pair<String, String>? {
        val bridge = config.site.first().bridges.singleOrNull { it.service == SERVICE } ?: run {
            logger.warn("Accuweather Bridge not configured. Create bridge parameters for service <$SERVICE>")
            return null
        }
        val location = bridge.parameters[LOCATION] ?: run {
            logger.error("No Accuweather location configured. Set it in bridge parameters: <$LOCATION>")
            return null
        }
        val token = bridge.parameters[TOKEN] ?: run {
            logger.error("No Accuweather token configured. Set it in bridge parameters: <$TOKEN>")
            return null
        }

        return location to token
    }

    private suspend fun getFreshConditions(
        location: String,
        token: String,
    ): Conditions {
        logger.debug("Refreshing Conditions")
        val conditionsResponse = api.getConditions(location, token)

        return Conditions(
            timestamp = clock.now(),
            cloudCover = conditionsResponse.cloudCover.percent,
            temperature = conditionsResponse.temperature.imperial.temperature.toInt(),
        ).also {
            logger.debug("New Conditions: <$it>")
        }
    }

    private suspend fun getFreshForecast(
        location: String,
        token: String,
    ): Forecast {
        logger.debug("Refreshing Conditions")
        val forecastResponse = api.getForecast(location, token)

        return Forecast(
            timestamp = clock.now(),
            sunrise = forecastResponse.daily.single().sun.rise.let(Instant.Companion::fromEpochSeconds),
            sunset = forecastResponse.daily.single().sun.set.let(Instant.Companion::fromEpochSeconds),
        ).also {
            logger.debug("New Forecast: <$it>")
        }
    }
}
