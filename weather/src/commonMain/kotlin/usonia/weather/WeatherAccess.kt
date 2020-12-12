package usonia.weather

import kotlinx.coroutines.flow.Flow

/**
 * Provides access to weather information updates.
 */
interface WeatherAccess {
    val forecast: Flow<Forecast>
    val conditions: Flow<Conditions>
}
