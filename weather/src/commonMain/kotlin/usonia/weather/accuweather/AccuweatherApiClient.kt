package usonia.weather.accuweather

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

/**
 * Ktor client implementation of the accuweather API.
 */
@OptIn(ExperimentalTime::class)
internal class AccuweatherApiClient: AccuweatherApi {
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

    override suspend fun getConditions(
        locationId: String,
        apiKey: String,
    ): ConditionsResponse {
        return client.get("https://dataservice.accuweather.com/currentconditions/v1/$locationId") {
            parameter("apikey", apiKey)
            parameter("details", true)
        }.body<List<ConditionsResponse>>().single()
    }

    override suspend fun getForecast(
        locationId: String,
        apiKey: String,
    ): ForecastResponse {
        return client.get("http://dataservice.accuweather.com/forecasts/v1/daily/1day/$locationId") {
            parameter("apikey", apiKey)
            parameter("details", true)
        }.body()
    }
}
