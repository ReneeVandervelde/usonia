package usonia.weather.nws

import kotlinx.datetime.Instant
import kotlinx.datetime.serializers.InstantIso8601Serializer
import kotlinx.serialization.Serializable

@Serializable
data class NwsForecast(
    val properties: Forecast,
) {
    @Serializable
    data class Forecast(
        @Serializable(with = InstantIso8601Serializer::class)
        val updateTime: Instant,
        val periods: List<Period>,
    )

    @Serializable
    data class Period(
        val number: Int,
        val name: String,

        @Serializable(with = InstantIso8601Serializer::class)
        val startTime: Instant,
        @Serializable(with = InstantIso8601Serializer::class)
        val endTime: Instant,

        val isDaytime: Boolean,

        val temperature: Int,
        val temperatureUnit: String,
        val temperatureTrend: String? = null,

        val windSpeed: String,
        val windDirection: String,
        val shortForecast: String,

        val detailedForecast: String,

        val probabilityOfPrecipitation: ProbabilityOfPrecipitation,
    )

    @Serializable
    data class ProbabilityOfPrecipitation(
        val unit: String? = null,
        val value: Int?,
    )
}
