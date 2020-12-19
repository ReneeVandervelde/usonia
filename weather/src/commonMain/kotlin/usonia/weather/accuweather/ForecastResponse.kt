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
        @SerialName("Day")
        val day: DayConditions,
    )

    @Serializable
    internal data class SunSchedule(
        @SerialName("EpochRise")
        val rise: Long,
        @SerialName("EpochSet")
        val set: Long,
    )

    @Serializable
    internal data class DayConditions(
        @SerialName("SnowProbability")
        val snowProbability: Int,
        @SerialName("RainProbability")
        val rainProbability: Int,
    )
}
