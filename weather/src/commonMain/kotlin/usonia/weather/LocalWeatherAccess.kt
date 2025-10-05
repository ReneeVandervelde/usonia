package usonia.weather

import com.inkapplications.coroutines.ongoing.OngoingFlow
import com.inkapplications.coroutines.ongoing.combinePair
import com.inkapplications.coroutines.ongoing.filterNotNull
import com.inkapplications.coroutines.ongoing.first
import com.inkapplications.coroutines.ongoing.map

/**
 * Provides access to weather information updates.
 */
interface LocalWeatherAccess
{
    val forecast: OngoingFlow<Forecast?>
    val conditions: OngoingFlow<Conditions?>
}

suspend fun LocalWeatherAccess.getLatestConditions(): Conditions? = conditions.first()
suspend fun LocalWeatherAccess.getLatestForecast(): Forecast? = forecast.first()
suspend fun LocalWeatherAccess.awaitConditions(): Conditions = conditions.filterNotNull().first()
suspend fun LocalWeatherAccess.awaitForecast(): Forecast = forecast.filterNotNull().first()

/**
 * Combine current conditions with forecast data.
 */
val LocalWeatherAccess.combinedData get() = conditions.combinePair(forecast)
    .map { WeatherSnapshot(it.first, it.second) }

data class WeatherSnapshot(
    val conditions: Conditions?,
    val forecast: Forecast?,
)
