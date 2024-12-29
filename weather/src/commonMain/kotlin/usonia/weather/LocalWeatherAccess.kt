package usonia.weather

import com.inkapplications.coroutines.ongoing.OngoingFlow
import com.inkapplications.coroutines.ongoing.combinePair

/**
 * Provides access to weather information updates.
 */
interface LocalWeatherAccess {
    val forecast: OngoingFlow<FullForecast>
    val conditions: OngoingFlow<Conditions>
    val currentConditions: Conditions
    val currentForecast: FullForecast
}

/**
 * Combine current conditions with forecast data.
 */
val LocalWeatherAccess.combinedData get() = conditions.combinePair(forecast)
