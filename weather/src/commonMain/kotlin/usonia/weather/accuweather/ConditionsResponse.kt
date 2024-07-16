package usonia.weather.accuweather

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ConditionsResponse(
    @SerialName("CloudCover")
    val cloudCover: Int,

    @SerialName("Temperature")
    val temperature: TemperatureTypes,

    @SerialName("PrecipitationSummary")
    val precipitation: PrecipitationSummary,

    @SerialName("HasPrecipitation")
    val hasPrecipitation: Boolean,
) {
    @Serializable
    internal data class PrecipitationSummary(
        @SerialName("Past6Hours")
        val pastSixHours: PrecipitationTypes,
    ) {
        @Serializable
        internal data class PrecipitationTypes(
            @SerialName("Imperial")
            val imperial: PrecipitationData,
        ) {
            @Serializable
            data class PrecipitationData(
                @SerialName("Value")
                val value: Float
            )
        }
    }

    @Serializable
    internal data class TemperatureTypes(
        @SerialName("Imperial")
        val imperial: TemperatureData
    ) {
        @Serializable
        internal data class TemperatureData(
            @SerialName("Value")
            val temperature: Float
        )
    }
}
