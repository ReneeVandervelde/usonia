package usonia.weather.nws

import com.inkapplications.standard.throwCancels
import inkapplications.spondee.measure.us.fahrenheit
import inkapplications.spondee.scalar.percent
import inkapplications.spondee.spatial.GeoCoordinates
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kimchi.logger.KimchiLogger
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.serializers.InstantIso8601Serializer
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import usonia.kotlin.RetryStrategy
import usonia.kotlin.datetime.ZonedClock
import usonia.kotlin.runRetryable
import usonia.weather.Forecast
import usonia.weather.FullForecast
import usonia.weather.LocationWeatherAccess
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

interface NwsApi {
    suspend fun getGridInfo(geoCoordinates: GeoCoordinates): GridInfo
    suspend fun getForecast(
        gridId: GridInfo.GridId,
        gridX: GridInfo.GridX,
        gridY: GridInfo.GridY,
    ): NwsForecast
}

@Serializable
data class NwsForecast(
    val properties: Forecast,
) {
    @Serializable
    data class Forecast(
        @Serializable(with = InstantIso8601Serializer::class)
        val updateTime: Instant,
        val periods: List<Period>,
    )

    @Serializable
    data class Period(
        val number: Int,
        val name: String,

        @Serializable(with = InstantIso8601Serializer::class)
        val startTime: Instant,
        @Serializable(with = InstantIso8601Serializer::class)
        val endTime: Instant,

        val isDaytime: Boolean,

        val temperature: Int,
        val temperatureUnit: String,
        val temperatureTrend: String? = null,

        val windSpeed: String,
        val windDirection: String,
        val shortForecast: String,

        val detailedForecast: String,

        val probabilityOfPrecipitation: ProbabilityOfPrecipitation,
    )

    @Serializable
    data class ProbabilityOfPrecipitation(
        val unit: String? = null,
        val value: Int?,
    )
}

private const val BASEURL = "https://api.weather.gov/"

internal class NwsApiClient: NwsApi {
    private val client = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 20.seconds.inWholeMilliseconds
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }

    override suspend fun getGridInfo(geoCoordinates: GeoCoordinates): GridInfo {
        return client.get("${BASEURL}/points/${geoCoordinates.latitude},${geoCoordinates.longitude}")
            .body()
    }

    override suspend fun getForecast(gridId: GridInfo.GridId, gridX: GridInfo.GridX, gridY: GridInfo.GridY): NwsForecast {
        return client.get("${BASEURL}/gridpoints/${gridId.value}/${gridX.value},${gridY.value}/forecast")
            .body()
    }
}

internal class NwsLocationWeatherAccess(
    private val api: NwsApi,
    private val clock: ZonedClock,
    private val logger: KimchiLogger,
): LocationWeatherAccess {
    private val retryStrategy = RetryStrategy.Bracket(
        attempts = 3,
        timeouts = listOf(200.milliseconds, 5.seconds, 20.seconds),
    )
    private val timeout = 60.seconds
    private val coordinatesCache: MutableMap<GeoCoordinates, GridInfo.GridCoordinate> = mutableMapOf()
    private val forecastCache: MutableMap<GridInfo.GridCoordinate, NwsForecast> = mutableMapOf()

    private suspend fun getGridCoordinates(location: GeoCoordinates): GridInfo.GridCoordinate? {
        return runRetryable(
            strategy = retryStrategy,
            attemptTimeout = timeout,
            onError = { error -> logger.warn("Error getting NWS coordinates", error) },
        ) {
            coordinatesCache
                .getOrElse(location) {
                    api.getGridInfo(location).properties.let { properties ->
                        GridInfo.GridCoordinate(
                            gridId = properties.gridId,
                            gridX = properties.gridX,
                            gridY = properties.gridY,
                        )
                    }
                }
                .also {
                    coordinatesCache[location] = it
                }
        }.getOrNull()
    }

    override suspend fun getWeatherForLocation(
        location: GeoCoordinates,
        date: LocalDate,
        daytime: Boolean
    ): Forecast? {
        val coordinates = getGridCoordinates(location) ?: return null
        val cachedForecast = forecastCache[coordinates]

        if (cachedForecast != null && cachedForecast.properties.updateTime > clock.now() - 6.hours) {
            return cachedForecast.toForecast(date, daytime)
        }

        val nwsForecast = runRetryable(
            strategy = retryStrategy,
            attemptTimeout = timeout,
            onError = { error -> logger.warn("Error getting NWS forecast for coordinates $coordinates", error) },
        ) {
            api.getForecast(coordinates.gridId, coordinates.gridX, coordinates.gridY)
        }.getOrNull() ?: return null

        forecastCache[coordinates] = nwsForecast

        return nwsForecast.toForecast(date, daytime)
    }

    private fun NwsForecast.toForecast(
        date: LocalDate,
        daytime: Boolean
    ): Forecast? {
        val period = properties.periods.firstOrNull {
            it.startTime.toLocalDateTime(clock.timeZone).date == date && it.isDaytime == daytime
        } ?: return null
        return Forecast(
            timestamp = properties.updateTime,
            precipitation = period.probabilityOfPrecipitation.value?.percent ?: 0.percent,
            temperature = period.temperature.fahrenheit,
        )
    }
}

@Serializable
data class GridInfo(
    val properties: Properties,
) {
    @Serializable
    data class Properties(
        val gridId: GridId,
        val gridX: GridX,
        val gridY: GridY,
    )

    @JvmInline
    @Serializable
    value class GridId(val value: String)

    @JvmInline
    @Serializable
    value class GridX(val value: Int)

    @JvmInline
    @Serializable
    value class GridY(val value: Int)

    data class GridCoordinate(
        val gridId: GridId,
        val gridX: GridX,
        val gridY: GridY,
    )
}
