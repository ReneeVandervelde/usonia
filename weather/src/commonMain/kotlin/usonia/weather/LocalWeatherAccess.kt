package usonia.weather

import usonia.kotlin.OngoingFlow
import usonia.kotlin.combineToPair

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
val LocalWeatherAccess.combinedData get() = conditions.combineToPair(forecast)
