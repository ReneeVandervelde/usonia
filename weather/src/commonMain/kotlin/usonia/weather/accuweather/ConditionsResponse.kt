package usonia.weather.accuweather

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ConditionsResponse(
    @SerialName("CloudCover")
    val cloudCover: Int,

    @SerialName("Temperature")
    val temperature: TemperatureTypes
) {
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
