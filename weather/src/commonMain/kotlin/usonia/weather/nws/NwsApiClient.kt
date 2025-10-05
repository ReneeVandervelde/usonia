package usonia.weather.nws

import inkapplications.spondee.spatial.GeoCoordinates
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

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

    override suspend fun getStations(
        gridId: GridInfo.GridId,
        gridX: GridInfo.GridX,
        gridY: GridInfo.GridY,
    ): List<NwsStation> {
        return client.get("${BASEURL}/gridpoints/${gridId.value}/${gridX.value},${gridY.value}/stations")
            .body()
    }

    override suspend fun getLatestObservations(stationId: NwsStation.StationIdentifier): NwsObservations {
        return client.get("${BASEURL}/stations/${stationId.value}/observations/latest")
            .body()
    }
}
