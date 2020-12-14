package usonia.weather.accuweather

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ForecastResponse(
    @SerialName("DailyForecasts")
    val daily: List<Daily>,
) {
    @Serializable
    internal data class Daily(
        @SerialName("Sun")
        val sun: SunSchedule,
    )

    @Serializable
    internal data class SunSchedule(
        @SerialName("EpochRise")
        val rise: Long,
        @SerialName("EpochSet")
        val set: Long,
    )
}
