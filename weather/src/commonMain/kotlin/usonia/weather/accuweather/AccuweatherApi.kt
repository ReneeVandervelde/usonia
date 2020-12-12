package usonia.weather.accuweather

/**
 * Fetch weather details from the accuweather API.
 *
 * See: https://developer.accuweather.com/apis
 */
internal interface AccuweatherApi {
    suspend fun getForecast(
        locationId: String,
        apiKey: String,
    ): ForecastResponse

    suspend fun getConditions(
        locationId: String,
        apiKey: String,
    ): ConditionsResponse
}
