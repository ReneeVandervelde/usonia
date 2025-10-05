package usonia.weather

import com.inkapplications.coroutines.ongoing.OngoingFlow
import com.inkapplications.coroutines.ongoing.asOngoing
import inkapplications.spondee.spatial.GeoCoordinates
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDate

/**
 * A weather access implementation that provides a single data set.
 */
class FixedWeather(
    initialConditions: Conditions?,
    initialForecast: Forecast?,
): LocalWeatherAccess, LocationWeatherAccess {
    private val mutableForecast = MutableStateFlow(initialForecast)
    private val mutableConditions = MutableStateFlow(initialConditions)
    override val forecast: OngoingFlow<Forecast?> = mutableForecast.asOngoing()
    override val conditions: OngoingFlow<Conditions?> = mutableConditions.asOngoing()

    fun updateConditions(conditions: Conditions) {
        mutableConditions.value = conditions
    }
    fun updateForecast(forecast: Forecast) {
        mutableForecast.value = forecast
    }

    override suspend fun getWeatherForLocation(
        location: GeoCoordinates,
        date: LocalDate,
        type: ForecastType,
    ): Forecast? = mutableForecast.value
}
