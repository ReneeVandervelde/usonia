package usonia.weather

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Provides access to weather information updates.
 */
interface WeatherAccess {
    val forecast: Flow<Forecast>
    val conditions: Flow<Conditions>
}

/**
 * Combine current conditions with forecast data.
 */
val WeatherAccess.combinedData get() = conditions.combine(forecast) { conditions, forecast -> conditions to forecast }
