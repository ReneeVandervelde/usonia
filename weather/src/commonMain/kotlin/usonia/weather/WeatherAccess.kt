package usonia.weather

import usonia.kotlin.OngoingFlow
import usonia.kotlin.combineToPair

/**
 * Provides access to weather information updates.
 */
interface WeatherAccess {
    val forecast: OngoingFlow<Forecast>
    val conditions: OngoingFlow<Conditions>
    val currentConditions: Conditions
    val currentForecast: Forecast
}

/**
 * Combine current conditions with forecast data.
 */
val WeatherAccess.combinedData get() = conditions.combineToPair(forecast)
