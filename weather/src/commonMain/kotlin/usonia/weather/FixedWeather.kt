package usonia.weather

import kotlinx.coroutines.flow.MutableStateFlow
import usonia.kotlin.OngoingFlow
import usonia.kotlin.asOngoing

/**
 * A weather access implementation that provides a single data set.
 */
class FixedWeather(
    initialConditions: Conditions,
    initialForecast: Forecast,
): WeatherAccess {
    private val mutableForecast = MutableStateFlow(initialForecast)
    private val mutableConditions = MutableStateFlow(initialConditions)
    override val forecast: OngoingFlow<Forecast> = mutableForecast.asOngoing()
    override val conditions: OngoingFlow<Conditions> = mutableConditions.asOngoing()
    override val currentConditions: Conditions = mutableConditions.value
    override val currentForecast: Forecast = mutableForecast.value

    fun updateConditions(conditions: Conditions) {
        mutableConditions.value = conditions
    }
    fun updateForecast(forecast: Forecast) {
        mutableForecast.value = forecast
    }
}
