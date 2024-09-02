package usonia.weather

import usonia.kotlin.OngoingFlow
import usonia.kotlin.combineToPair

/**
 * Provides access to weather information updates.
 */
interface LocalWeatherAccess {
    val forecast: OngoingFlow<Forecast>
    val conditions: OngoingFlow<Conditions>
    val currentConditions: Conditions
    val currentForecast: Forecast
}

/**
 * Combine current conditions with forecast data.
 */
val LocalWeatherAccess.combinedData get() = conditions.combineToPair(forecast)
