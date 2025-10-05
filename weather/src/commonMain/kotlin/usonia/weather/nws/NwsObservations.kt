package usonia.weather.nws

import kotlinx.datetime.Instant
import kotlinx.datetime.serializers.InstantIso8601Serializer
import kotlinx.serialization.Serializable

@Serializable
data class NwsObservations(
    val properties: Observation
) {
    @Serializable
    data class Observation(
        @Serializable(with = InstantIso8601Serializer::class)
        val timestamp: Instant,
        val temperature: Temperature?,
        val presentWeather: List<Phenomenon>?,
        val precipitationLastHour: PrecipitationMeasurement?,
        val precipitationLast3Hours: PrecipitationMeasurement?,
        val precipitationLast6Hours: PrecipitationMeasurement?,
        val cloudLayers: List<CloudLayer>?,
    ) {
        @Serializable
        data class Temperature(
            val value: Double?,
        )

        @Serializable
        data class PrecipitationMeasurement(
            val value: Double?,
        )

        @Serializable
        data class CloudLayer(
            val amount: CloudLayerAmount,
        ) {
            @Serializable
            @JvmInline
            value class CloudLayerAmount(val key: String)
            {
                companion object
                {
                    val Overcast = CloudLayerAmount("OVC")
                    val Broken = CloudLayerAmount("BKN")
                    val Scattered = CloudLayerAmount("SCT")
                    val Few = CloudLayerAmount("FEW")
                    val SkyClear = CloudLayerAmount("SKC")
                    val Clear = CloudLayerAmount("CLR")
                    val VerticalVisibility = CloudLayerAmount("VV")
                }
            }
        }

        @Serializable
        data class Phenomenon(
            val weather: Type,
        ) {
            @JvmInline
            @Serializable
            value class Type(val key: String)
            {
                companion object
                {
                    val fogMist = Type("fog_mist")
                    val dustStorm = Type("dust_storm")
                    val dust = Type("dust")
                    val drizzle = Type("drizzle")
                    val funnelCloud = Type("funnel_cloud")
                    val fog = Type("fog")
                    val smoke = Type("smoke")
                    val hail = Type("hail")
                    val snowPellets = Type("snow_pellets")
                    val haze = Type("haze")
                    val iceCrystals = Type("ice_crystals")
                    val icePellets = Type("ice_pellets")
                    val dustWhirls = Type("dust_whirls")
                    val spray = Type("spray")
                    val rain = Type("rain")
                    val sand = Type("sand")
                    val snowGrains = Type("snow_grains")
                    val snow = Type("snow")
                    val squalls = Type("squalls")
                    val sandStorm = Type("sand_storm")
                    val thunderstorms = Type("thunderstorms")
                    val unknown = Type("unknown")
                    val volcanicAsh = Type("volcanic_ash")
                }
            }
        }
    }
}
