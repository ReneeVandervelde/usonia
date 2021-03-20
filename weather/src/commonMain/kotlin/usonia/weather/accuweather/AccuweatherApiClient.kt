package usonia.weather.accuweather

import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

/**
 * Ktor client implementation of the accuweather API.
 */
@OptIn(ExperimentalTime::class)
internal class AccuweatherApiClient: AccuweatherApi {
    private val client = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 20.seconds.toLongMilliseconds()
        }
        install(JsonFeature) {
            serializer = KotlinxSerializer(kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
            })
        }
    }

    override suspend fun getConditions(
        locationId: String,
        apiKey: String,
    ): ConditionsResponse {
        return client.get<List<ConditionsResponse>>("https://dataservice.accuweather.com/currentconditions/v1/$locationId") {
            parameter("apikey", apiKey)
            parameter("details", true)
        }.single()
    }

    override suspend fun getForecast(
        locationId: String,
        apiKey: String,
    ): ForecastResponse {
        return client.get("http://dataservice.accuweather.com/forecasts/v1/daily/1day/$locationId") {
            parameter("apikey", apiKey)
            parameter("details", true)
        }
    }
}
