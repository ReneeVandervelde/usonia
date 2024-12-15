package usonia.weather.accuweather

import com.inkapplications.standard.throwCancels
import inkapplications.spondee.measure.us.fahrenheit
import inkapplications.spondee.measure.us.inches
import inkapplications.spondee.scalar.percent
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import regolith.init.Initializer
import regolith.init.TargetManager
import regolith.processes.cron.CronJob
import regolith.processes.cron.Schedule
import usonia.core.state.findBridgeByServiceTag
import usonia.kotlin.*
import usonia.server.client.BackendClient
import usonia.weather.Conditions
import usonia.weather.FullForecast
import usonia.weather.LocalWeatherAccess
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

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
internal class AccuweatherAccess(
    private val api: AccuweatherApi,
    private val client: BackendClient,
    private val clock: Clock = Clock.System,
    private val logger: KimchiLogger = EmptyLogger,
): LocalWeatherAccess, CronJob, Initializer {
    private val retryStrategy = RetryStrategy.Bracket(
        attempts = 4,
        timeouts = listOf(200.milliseconds, 1.seconds, 5.seconds),
    )
    private val timeout = 60.seconds
    private val forecastFlow = MutableStateFlow(
        FullForecast(
            timestamp = Instant.DISTANT_PAST,
            sunset = Instant.DISTANT_FUTURE,
            sunrise = Instant.DISTANT_PAST,
            rainChance = 0.percent,
            snowChance = 0.percent,
            highTemperature = 32.fahrenheit,
            lowTemperature = 32.fahrenheit,
        )
    )
    private val conditionsFlow = MutableStateFlow(
        Conditions(
            timestamp = Instant.DISTANT_PAST,
            cloudCover = 100.percent,
            temperature = 32,
            rainInLast6Hours = 0.inches,
            isRaining = false,
        )
    )
    override val forecast: OngoingFlow<FullForecast> = forecastFlow.asOngoing()
    override val conditions: OngoingFlow<Conditions> = conditionsFlow.asOngoing()

    override val schedule: Schedule = Schedule(
        minutes = setOf(0)
    )
    override val currentConditions: Conditions get() = conditionsFlow.value
    override val currentForecast: FullForecast get() = forecastFlow.value

    override suspend fun runCron(time: LocalDateTime, zone: TimeZone) {
        val (location, token) = getConfig() ?: return

        coroutineScope {
            val newConditions = async { getFreshConditions(location, token) }
            val currentForecast = forecast.first()
            if (clock.now() - currentForecast.timestamp > 2.hours) {
                logger.info("Forecast is expired. Updating.")
                forecastFlow.value = getFreshForecast(location, token)
            }

            conditionsFlow.value = newConditions.await()
        }
    }

    override suspend fun initialize(targetManager: TargetManager) {
        val (location, token) = getConfig() ?: return

        coroutineScope {
            val newConditions = async { getFreshConditions(location, token) }
            val newForecast = async { getFreshForecast(location, token) }

            conditionsFlow.value = newConditions.await()
            forecastFlow.value = newForecast.await()
        }
    }

    private suspend fun getConfig(): Pair<String, String>? {
        val bridge = client.findBridgeByServiceTag(SERVICE) ?: run {
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
        val conditionsResult = runRetryable(
            strategy = retryStrategy,
            attemptTimeout = timeout,
            onError = { error -> logger.warn("Error getting fresh conditions", error) }
        ) {
            api.getConditions(location, token)
        }.throwCancels()

        conditionsResult.onSuccess {  conditionsResponse ->
            return Conditions(
                timestamp = clock.now(),
                cloudCover = conditionsResponse.cloudCover.percent,
                temperature = conditionsResponse.temperature.imperial.temperature.toInt(),
                rainInLast6Hours = conditionsResponse.precipitation.pastSixHours.imperial.value.inches,
                isRaining = conditionsResponse.hasPrecipitation,
            ).also {
                logger.debug("New Conditions: <$it>")
            }
        }

        conditionsResult.onFailure { error ->
            logger.error("Failed to get fresh conditions.", error)
        }

        return conditionsFlow.value
    }

    private suspend fun getFreshForecast(
        location: String,
        token: String,
    ): FullForecast {
        logger.debug("Refreshing Conditions")

        val forecastResult = runRetryable(
            strategy = retryStrategy,
            attemptTimeout = timeout,
            onError = { error -> logger.warn("Error getting forecast", error) },
        ) {
            api.getForecast(location, token)
        }.throwCancels()

        forecastResult.onSuccess { forecastResponse ->
            return FullForecast(
                timestamp = clock.now(),
                sunrise = forecastResponse.daily.single().sun.rise.let(Instant.Companion::fromEpochSeconds),
                sunset = forecastResponse.daily.single().sun.set.let(Instant.Companion::fromEpochSeconds),
                rainChance = forecastResponse.daily.single().day.rainProbability.percent,
                snowChance = forecastResponse.daily.single().day.snowProbability.percent,
                highTemperature = forecastResponse.daily.single().temperature.max.value.fahrenheit,
                lowTemperature = forecastResponse.daily.single().temperature.min.value.fahrenheit,
            ).also {
                logger.debug("New Forecast: <$it>")
            }
        }

        forecastResult.onFailure { error ->
            logger.error("Failed to get fresh forecast.", error)
        }

        return forecastFlow.value
    }
}
