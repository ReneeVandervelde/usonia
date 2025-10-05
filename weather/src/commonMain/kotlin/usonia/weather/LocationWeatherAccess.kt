package usonia.weather

import inkapplications.spondee.spatial.GeoCoordinates
import kotlinx.datetime.LocalDate

interface LocationWeatherAccess {
    suspend fun getWeatherForLocation(
        location: GeoCoordinates,
        date: LocalDate,
        type: ForecastType = ForecastType.FullDay,
    ): Forecast?
}
