package usonia.weather

import com.inkapplications.coroutines.ongoing.OngoingFlow
import com.inkapplications.coroutines.ongoing.asOngoing
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A weather access implementation that provides a single data set.
 */
class FixedWeather(
    initialConditions: Conditions,
    initialForecast: FullForecast,
): LocalWeatherAccess {
    private val mutableForecast = MutableStateFlow(initialForecast)
    private val mutableConditions = MutableStateFlow(initialConditions)
    override val forecast: OngoingFlow<FullForecast> = mutableForecast.asOngoing()
    override val conditions: OngoingFlow<Conditions> = mutableConditions.asOngoing()
    override val currentConditions: Conditions = mutableConditions.value
    override val currentForecast: FullForecast = mutableForecast.value

    fun updateConditions(conditions: Conditions) {
        mutableConditions.value = conditions
    }
    fun updateForecast(forecast: FullForecast) {
        mutableForecast.value = forecast
    }
}
