package usonia.weather.nws

import kotlinx.serialization.Serializable

@Serializable
data class StationList(
    val features: List<NwsStation>,
)

@Serializable
data class NwsStation(
    val properties: StationProperties,
)

@Serializable
data class StationProperties(
    val stationIdentifier: StationIdentifier
) {
    @JvmInline
    @Serializable
    value class StationIdentifier(val value: String)
}
