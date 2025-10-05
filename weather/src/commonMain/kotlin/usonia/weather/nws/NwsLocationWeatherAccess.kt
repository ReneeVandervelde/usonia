package usonia.weather.nws

import com.inkapplications.coroutines.ongoing.OngoingFlow
import com.inkapplications.coroutines.ongoing.asOngoing
import com.inkapplications.coroutines.ongoing.collectLatest
import com.inkapplications.coroutines.ongoing.map
import com.inkapplications.datetime.ZonedClock
import usonia.weather.nws.NwsObservations.Observation.CloudLayer.CloudLayerAmount
import inkapplications.spondee.measure.us.fahrenheit
import inkapplications.spondee.measure.us.inches
import inkapplications.spondee.scalar.percent
import inkapplications.spondee.spatial.GeoCoordinates
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import regolith.processes.cron.CronJob
import regolith.processes.cron.Schedule
import regolith.processes.daemon.Daemon
import usonia.kotlin.RetryStrategy
import usonia.kotlin.runRetryable
import usonia.server.client.BackendClient
import usonia.weather.Conditions
import usonia.weather.Forecast
import usonia.weather.ForecastType
import usonia.weather.LocalWeatherAccess
import usonia.weather.LocationWeatherAccess
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

internal class NwsLocationWeatherAccess(
    private val api: NwsApi,
    private val client: BackendClient,
    private val clock: ZonedClock,
    private val logger: KimchiLogger,
): LocationWeatherAccess, LocalWeatherAccess, CronJob, Daemon {
    private val homeGrid = MutableStateFlow<GridInfo?>(null)
    private val homeStation = MutableStateFlow<StationProperties.StationIdentifier?>(null)
    private val coordinatesCache: MutableMap<GeoCoordinates, GridInfo.GridCoordinate> = mutableMapOf()
    private val forecastCache: MutableMap<GridInfo.GridCoordinate, NwsForecast> = mutableMapOf()
    private val localForecastState = MutableStateFlow<Forecast?>(null)
    private val localConditionsState = MutableStateFlow<Conditions?>(null)

    override val forecast: OngoingFlow<Forecast?> = localForecastState.asOngoing()
    override val conditions: OngoingFlow<Conditions?> = localConditionsState.asOngoing()
    override val schedule: Schedule = Schedule(
        minutes = setOf(0)
    )

    override suspend fun startDaemon(): Nothing
    {
        client.site
            .map { it.location }
            .collectLatest { location ->
                val gridInfo = api.getGridInfo(location)
                homeGrid.value = gridInfo
                homeStation.value = api.getStations(gridInfo.properties.gridId, gridInfo.properties.gridX, gridInfo.properties.gridY)
                    .features
                    .first()
                    .properties
                    .stationIdentifier
            }
    }

    override suspend fun runCron(time: LocalDateTime, zone: TimeZone)
    {
        coroutineScope {
            val forecastUpdate = async {
                runRetryable(
                    strategy = RetryStrategy.Exponential(attempts = 10, initial = 10.seconds, maximum = 5.minutes)
                ) {
                    updateLocalForecast(time)
                }
            }
            val conditionsUpdate = async {
                runRetryable(
                    strategy = RetryStrategy.Exponential(attempts = 10, initial = 10.seconds, maximum = 5.minutes)
                ) {
                    updateLocalConditions()
                }
                }

            forecastUpdate.await()
            conditionsUpdate.await()
        }
    }

    private suspend fun updateLocalForecast(time: LocalDateTime)
    {
        val grid = homeGrid.value ?: run {
            logger.warn("Location weather update run before home grid was set")
            return
        }

        val nwsForecast = api.getForecast(grid.properties.gridId, grid.properties.gridX, grid.properties.gridY)
        val forecast = nwsForecast.toForecast(date = time.date)

        localForecastState.value = forecast
        if (forecast != null) {
            forecastCache[grid.properties.coordinate] = nwsForecast
        }
    }

    private suspend fun updateLocalConditions()
    {
        val station = homeStation.value ?: run {
            logger.warn("Location weather update run before home station was set")
            return
        }
        val conditions = api.getLatestObservations(station)
            .toConditions()

        localConditionsState.value = conditions
    }

    private suspend fun getGridCoordinates(location: GeoCoordinates): GridInfo.GridCoordinate?
    {
        return runRetryable(
            strategy = RetryStrategy.Bracket(
                attempts = 3,
                timeouts = listOf(200.milliseconds, 5.seconds, 20.seconds),
            ),
            onError = { error -> logger.warn("Error getting NWS coordinates", error) },
        ) {
            coordinatesCache
                .getOrElse(location) {
                    api.getGridInfo(location).properties.coordinate
                }
                .also {
                    coordinatesCache[location] = it
                }
        }.getOrNull()
    }

    override suspend fun getWeatherForLocation(
        location: GeoCoordinates,
        date: LocalDate,
        type: ForecastType
    ): Forecast? {
        val coordinates = getGridCoordinates(location) ?: return null
        val cachedForecast = forecastCache[coordinates]

        if (cachedForecast != null && cachedForecast.properties.updateTime > clock.now() - 6.hours) {
            return cachedForecast.toForecast(date, type)
        }

        val nwsForecast = runRetryable(
            strategy = RetryStrategy.Bracket(
                attempts = 3,
                timeouts = listOf(200.milliseconds, 5.seconds, 20.seconds),
            ),
            onError = { error -> logger.warn("Error getting NWS forecast for coordinates $coordinates", error) },
        ) {
            api.getForecast(coordinates.gridId, coordinates.gridX, coordinates.gridY)
        }.getOrNull() ?: return null
        val forecast = nwsForecast.toForecast(date, type)

        if (forecast != null) {
            forecastCache[coordinates] = nwsForecast
        }

        return forecast
    }

    private fun NwsForecast.toForecast(
        date: LocalDate,
        type: ForecastType = ForecastType.FullDay,
    ): Forecast? {
        val relevant = properties.periods
            .filter { it.startTime.toLocalDateTime(clock.zone).date == date }
            .filter {
                when (type) {
                    ForecastType.FullDay -> true
                    ForecastType.Daytime -> it.isDaytime
                    ForecastType.Overnight -> !it.isDaytime
                }
            }

        if (relevant.isEmpty()) {
            return null
        }

        return Forecast(
            timestamp = properties.updateTime,
            precipitation = relevant
                .map { it.probabilityOfPrecipitation }
                .maxOf { it.value ?: 0 }
                .percent,
            rainChance = relevant
                .filter { it.temperature >= 36 }
                .map { it.probabilityOfPrecipitation }
                .maxOfOrNull { it.value ?: 0 }
                ?.percent
                ?: 0.percent,
            snowChance = relevant
                .filter { it.temperature < 36  }
                .map { it.probabilityOfPrecipitation }
                .maxOfOrNull { it.value ?: 0 }
                ?.percent
                ?: 0.percent,
            highTemperature = relevant
                .maxOfOrNull { it.temperature }
                ?.fahrenheit,
            lowTemperature = relevant
                .minOfOrNull { it.temperature }
                ?.fahrenheit,
        )
    }

    private fun NwsObservations.toConditions(): Conditions
    {
        return Conditions(
            timestamp = properties.timestamp,
            cloudCover = when {
                properties.cloudLayers?.any { it.amount == CloudLayerAmount.Overcast } == true -> 100.percent
                properties.cloudLayers?.any { it.amount == CloudLayerAmount.Broken } == true -> 75.percent
                properties.cloudLayers?.any { it.amount == CloudLayerAmount.Scattered } == true -> 50.percent
                properties.cloudLayers?.any { it.amount == CloudLayerAmount.Few } == true -> 25.percent
                properties.cloudLayers?.all { it.amount == CloudLayerAmount.Clear || it.amount == CloudLayerAmount.SkyClear } == true -> 0.percent
                else -> null.also {
                    logger.warn("Unknown cloud cover state: ${properties.cloudLayers}")
                }
            },
            temperature = properties.temperature?.value?.roundToInt()?.fahrenheit,
            rainInLast6Hours = properties.precipitationLast6Hours?.value?.inches,
            isRaining = properties.presentWeather?.any { it.weather == NwsObservations.Observation.Phenomenon.Type.rain },
        )
    }
}
