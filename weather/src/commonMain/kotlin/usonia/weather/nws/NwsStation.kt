package usonia.weather.nws

import kotlinx.serialization.Serializable

@Serializable
data class NwsStation(
    val stationIdentifier: StationIdentifier
) {
    @JvmInline
    @Serializable
    value class StationIdentifier(val value: String)
}
